package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineFoundryBasinBlockEntity;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
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
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import org.joml.Matrix4f;

/**
 * Port of {@code RenderFoundry} (1.7.10) -- the shared TESR for the basin and the mold:
 *  - melt surface: the gray {@code lava_gray} texture tinted with moltenColor,
 *    full brightness (original: lightmap 240/240, lighting off) plus a second additive
 *    pass (white, alpha 0.3, SRC_ALPHA/ONE, depthMask off) -- the glow of hot metal;
 *    the surface is double-sided (the original called glDisable(GL_CULL_FACE));
 *  - the installed mold (slot 0) -- as a flat item at moldHeight (0.13);
 *  - the cast item (slot 1) -- as a flat item at outHeight, while block casts are drawn
 *    as a real baked block model (ItemRenderer, as in the inventory), fitted
 *    into the basin's cavity.
 * Heights are 1:1 via {@link MachineFoundryBasinBlockEntity} (basin 0.75/0.875,
 * mold 0.25/0.25).
 */
//? if < 1.21.1 {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} else {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class FoundryBasinRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineFoundryBasinBlockEntity> {

    private static final ResourceLocation LAVA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/fluids/lava_gray.png");

    private static final float INNER = 2f / 16f;
    private static final float INNER_MAX = 1f - INNER;

    /** Base pass: translucent, no backface culling (the original was double-sided). */
    private static final RenderType METAL_RT = MetalRenderTypes.METAL;
    /** Additive glow pass: white, alpha 0.3, SRC_ALPHA/ONE, no depth write. */
    private static final RenderType GLOW_RT = MetalRenderTypes.GLOW;
    public FoundryBasinRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(MachineFoundryBasinBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        // -- Installed mold (flat item, original drawItem) --
        ItemStack mold = be.getMoldSlot();
        if (!mold.isEmpty()) {
            drawFlatItem(be, bufferSource, poseStack, mold, be.getMoldHeight(), packedLight, packedOverlay);
        }

        // -- Cast item --
        ItemStack out = be.getOutputSlot();
        if (!out.isEmpty()) {
            if (out.getItem() instanceof BlockItem) {
                drawSolidBlock(be, bufferSource, poseStack, out, packedLight, packedOverlay);
            } else {
                drawFlatItem(be, bufferSource, poseStack, out, be.getOutputHeight(), packedLight, packedOverlay);
            }
        }

        // -- Melt surface --
        float fill = be.getFillLevel();
        if (fill <= 0f) return;

        float surfaceY = 0.125f + fill * be.getSurfaceRange();

        int argb = be.getFillColor();
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >>  8) & 0xFF) / 255f;
        float b = ( argb        & 0xFF) / 255f;

        Matrix4f m = poseStack.last().pose();
        int fullbright = 0xF000F0;

        // Pass 1: tinted metal (full brightness, double-sided).
        VertexConsumer base = bufferSource.getBuffer(METAL_RT);
        metalQuad(base, m, surfaceY, r, g, b, 1f, fullbright);

        // Pass 2: additive glow in the metal's color (the original added white at 0.3,
        // but on the fullbright base that clipped to pure white -- the tint preserves the hue).
        VertexConsumer glow = bufferSource.getBuffer(GLOW_RT);
        metalQuad(glow, m, surfaceY, r, g, b, 0.3f, fullbright);
    }


    /** Converts a {@code float} in 0..1 to a byte value for the hooks' int color. */
    private static int color(float c) {
        return (int) (c * 255.0f + 0.5f);
    }

    private static void metalQuad(VertexConsumer vc, Matrix4f m, float y, float r, float g, float b, float a, int light) {
        int cr = color(r), cg = color(g), cb = color(b), ca = color(a);
        RenderHooks.vertexColorUvLight(vc, m, INNER,     y, INNER,     cr, cg, cb, ca, 0, 0, light);
        RenderHooks.vertexColorUvLight(vc, m, INNER,     y, INNER_MAX, cr, cg, cb, ca, 0, 1, light);
        RenderHooks.vertexColorUvLight(vc, m, INNER_MAX, y, INNER_MAX, cr, cg, cb, ca, 1, 1, light);
        RenderHooks.vertexColorUvLight(vc, m, INNER_MAX, y, INNER,     cr, cg, cb, ca, 1, 0, light);
    }

    /**
     * Positions a flat item (mold or cast) at the center of the basin.
     * Item models are centered around (0,0,0), so translating to (0.5, h, 0.5)
     * with a 0.75 scale centers the item exactly in the basin's cavity (0.125..0.875).
     * The -90 degree X rotation lays the item face-up without mirroring the texture.
     */
    private static void drawFlatItem(MachineFoundryBasinBlockEntity be, MultiBufferSource bufferSource,
                                     PoseStack poseStack, ItemStack stack, float height,
                                     int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5D, height + 0.001D, 0.5D);
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
     * Original (drawBlock): footprint 0.125..0.875 (scale 0.75), block top at outHeight
     * (0.875). ItemRenderer.renderStatic centers the model itself via an internal
     * translate(-0.5), so no extra compensation is needed -- a double shift pushed the
     * block half a block sideways and sank it.
     */
    private static final float BLOCK_SCALE = 0.75F;

    private static void drawSolidBlock(MachineFoundryBasinBlockEntity be, MultiBufferSource bufferSource,
                                       PoseStack poseStack, ItemStack stack,
                                       int packedLight, int packedOverlay) {
        float baseY = be.getOutputHeight() - BLOCK_SCALE + 0.001f;
        poseStack.pushPose();
        poseStack.translate(0.5D, baseY, 0.5D);
        poseStack.scale(BLOCK_SCALE, BLOCK_SCALE, BLOCK_SCALE);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, net.minecraft.world.item.ItemDisplayContext.NONE,
                packedLight, packedOverlay, poseStack, bufferSource, be.getLevel(), 0);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(MachineFoundryBasinBlockEntity blockEntity) {
        return ShaderCompatibilityDetector.shouldRenderBlockEntityOffScreen();
    }

    /** Custom layers: metal (translucent no-cull), glow (additive, no depth), block ghost. */
    private static final class MetalRenderTypes extends RenderType {
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
