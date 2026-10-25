package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.IRenderFoundry;
import com.hbm_m.blockentity.machines.MachineFoundryBasinBlockEntity;
import com.hbm_m.blockentity.machines.MachineFoundryCastingBaseBlockEntity;
import com.hbm_m.blockentity.machines.MachineFoundryMoldBlockEntity;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import org.joml.Matrix4f;

/**
 * Port of {@code RenderFoundry} (1.7.10) onto the {@link MachineRenderers} factory --
 * the shared Nucleus renderer for the basin and the mold. The block body is the
 * regular chunk-mesh model (plain blockstate JSON -- no parts model), the spec
 * declares no parts and everything dynamic goes through a {@link MachineRenderHook}
 * (the {@code MachineSolidifierRenderer} pattern):
 * <ul>
 *   <li>melt surface: the gray {@code lava_gray} texture tinted with moltenColor,
 *       full brightness (lightmap 240/240) plus a second additive pass (alpha 0.3,
 *       SRC_ALPHA/ONE, depthMask off) -- the glow of hot metal; double-sided
 *       (the original called glDisable(GL_CULL_FACE));</li>
 *   <li>the installed mold (slot 0) -- a flat item at moldHeight (0.13);</li>
 *   <li>the cast item (slot 1) -- a flat item at outHeight, while block casts are
 *       drawn as a real baked block model (ItemRenderer, as in the inventory),
 *       fitted into the basin's cavity.</li>
 * </ul>
 * The hook works against the shared base {@link MachineFoundryCastingBaseBlockEntity}
 * (public {@code slots}) and {@link IRenderFoundry} heights, so one hook serves both
 * BE types ({@code #FOUNDRY_BASIN_BE} and {@code #FOUNDRY_MOLD_BE}).
 * The hook pose already carries the block transform (center + facing), so all legacy
 * 0..1 block-local coordinates are shifted by (-0.5, 0, -0.5).
 */
public final class MachineFoundryRenderer {

    private MachineFoundryRenderer() {}

    public static void register() {
        MachineRenderers.machine("foundry_basin", ModBlockEntities.FOUNDRY_BASIN_BE.get(),
                MachineFoundryBasinBlockEntity.class)
            .viewDistance(256)
            .hook(new FoundryHook<>())
            .register();

        MachineRenderers.machine("foundry_mold", ModBlockEntities.FOUNDRY_MOLD_BE.get(),
                MachineFoundryMoldBlockEntity.class)
            .viewDistance(256)
            .hook(new FoundryHook<>())
            .register();
    }

    /** Immediate content: melt surface + installed mold + cast item. */
    private static final class FoundryHook<T extends MachineFoundryCastingBaseBlockEntity & IRenderFoundry>
            implements MachineRenderHook<T> {

        @Override
        public void render(T be, float partialTick, PoseStack poseStack,
                           MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                           MachineRenderApi api) {

            IRenderFoundry foundry = be;

            // -- Installed mold (flat item, original drawItem) --
            ItemStack mold = be.slots[0];
            if (mold != null && !mold.isEmpty()) {
                drawFlatItem(be, bufferSource, poseStack, mold, (float) foundry.moldHeight(),
                        packedLight, packedOverlay);
            }

            // -- Cast item --
            ItemStack out = be.slots[1];
            if (out != null && !out.isEmpty()) {
                if (out.getItem() instanceof BlockItem) {
                    drawSolidBlock(be, bufferSource, poseStack, out, (float) foundry.outHeight(),
                            packedLight, packedOverlay);
                } else {
                    drawFlatItem(be, bufferSource, poseStack, out, (float) foundry.outHeight(),
                            packedLight, packedOverlay);
                }
            }

            // -- Melt surface --
            if (!foundry.shouldRender() || be.getCapacity() <= 0 || be.amount <= 0) return;

            float surfaceY = (float) foundry.getMoltenLevel();

            int argb = be.getMat() != null ? (0xFF000000 | be.getMat().moltenColor) : 0xFFC18336;
            float r = ((argb >> 16) & 0xFF) / 255f;
            float g = ((argb >>  8) & 0xFF) / 255f;
            float b = ( argb        & 0xFF) / 255f;

            Matrix4f m = poseStack.last().pose();
            int fullbright = 0xF000F0;

            // Pass 1: tinted metal (full brightness, double-sided).
            VertexConsumer base = bufferSource.getBuffer(MetalRenderTypes.METAL);
            metalQuad(base, m, surfaceY, r, g, b, 1f, fullbright);

            // Pass 2: additive glow in the metal's color (the original added white at 0.3,
            // but on the fullbright base that clipped to pure white -- the tint preserves the hue).
            VertexConsumer glow = bufferSource.getBuffer(MetalRenderTypes.GLOW);
            metalQuad(glow, m, surfaceY, r, g, b, 0.3f, fullbright);
        }

        @Override
        public boolean mayDrawContent(T be) {
            return true;
        }

        /** Converts a {@code float} in 0..1 to a byte value for the hooks' int color. */
        private static int color(float c) {
            return (int) (c * 255.0f + 0.5f);
        }

        private static void metalQuad(VertexConsumer vc, Matrix4f m, float y, float r, float g, float b, float a, int light) {
            int cr = color(r), cg = color(g), cb = color(b), ca = color(a);
            RenderHooks.vertexColorUvLight(vc, m, -0.375f, y, -0.375f, cr, cg, cb, ca, 0, 0, light);
            RenderHooks.vertexColorUvLight(vc, m, -0.375f, y,  0.375f, cr, cg, cb, ca, 0, 1, light);
            RenderHooks.vertexColorUvLight(vc, m,  0.375f, y,  0.375f, cr, cg, cb, ca, 1, 1, light);
            RenderHooks.vertexColorUvLight(vc, m,  0.375f, y, -0.375f, cr, cg, cb, ca, 1, 0, light);
        }

        /**
         * Positions a flat item (mold or cast) at the center of the basin.
         * Item models are centered around (0,0,0), so a 0.75 scale centers the item
         * exactly in the basin's cavity (0.125..0.875 in block space, centered here).
         * The -90 degree X rotation lays the item face-up without mirroring the texture.
         */
        private static void drawFlatItem(MachineFoundryCastingBaseBlockEntity be, MultiBufferSource bufferSource,
                                         PoseStack poseStack, ItemStack stack, float height,
                                         int packedLight, int packedOverlay) {
            poseStack.pushPose();
            poseStack.translate(0.0D, height + 0.001D, 0.0D);
            float scale = 0.75F;
            poseStack.scale(scale, scale, scale);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90F));
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    stack, net.minecraft.world.item.ItemDisplayContext.NONE,
                    packedLight, packedOverlay, poseStack, bufferSource, be.getLevel(), 0);
            poseStack.popPose();
        }

        /**
         * Cast BLOCK: render the real baked block model (as in the inventory) through
         * ItemRenderer -- the same path as the ingot (drawFlatItem).
         * Original (drawBlock): footprint 0.125..0.875 (scale 0.75), block top at outHeight.
         * ItemRenderer.renderStatic centers the model itself via an internal
         * translate(-0.5), so no extra compensation is needed.
         */
        private static final float BLOCK_SCALE = 0.75F;

        private static void drawSolidBlock(MachineFoundryCastingBaseBlockEntity be, MultiBufferSource bufferSource,
                                           PoseStack poseStack, ItemStack stack, float outHeight,
                                           int packedLight, int packedOverlay) {
            float baseY = outHeight - BLOCK_SCALE + 0.001f;
            poseStack.pushPose();
            poseStack.translate(0.0D, baseY, 0.0D);
            poseStack.scale(BLOCK_SCALE, BLOCK_SCALE, BLOCK_SCALE);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    stack, net.minecraft.world.item.ItemDisplayContext.NONE,
                            packedLight, packedOverlay, poseStack, bufferSource, be.getLevel(), 0);
            poseStack.popPose();
        }
    }

    /** Custom layers: metal (translucent no-cull), glow (additive, no depth). */
    private static final class MetalRenderTypes extends RenderType {
        private static final ResourceLocation LAVA_TEXTURE =
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/fluids/lava_gray.png");

        /** Base pass: translucent, no backface culling (the original was double-sided). */
        private static final RenderType METAL = create("hbm_m_foundry_metal",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS, 256, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(LAVA_TEXTURE, false, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .createCompositeState(false));

        /** Additive glow pass: alpha 0.3, SRC_ALPHA/ONE, no depth write. */
        private static final RenderType GLOW = create("hbm_m_foundry_glow",
                DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                VertexFormat.Mode.QUADS, 256, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(LAVA_TEXTURE, false, false))
                        .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .createCompositeState(false));

        private MetalRenderTypes(String s, VertexFormat v, VertexFormat.Mode m,
                                 int i, boolean b, boolean b2, Runnable r, Runnable r2) { super(s, v, m, i, b, b2, r, r2); }
    }
}
