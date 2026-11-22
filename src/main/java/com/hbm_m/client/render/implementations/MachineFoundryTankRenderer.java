package com.hbm_m.client.render.implementations;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineFoundryTankBlockEntity;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderHook;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.RenderHooks;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import org.joml.Matrix4f;

/**
 * Port of the metal part of {@code RenderFoundryTank} (1.7.10, ISBRH) onto the
 * {@link MachineRenderers} factory -- the {@code MachineFoundryRenderer} pattern:
 * the tank shell is the regular chunk-mesh block model (the spec declares no
 * parts) and everything dynamic goes through a {@link MachineRenderHook}:
 * <ul>
 *   <li>the melt in the tank is drawn as fullbright quads (the original used
 *       setBrightness(240)), colored {@code 255-(255-c)*0.7} ("screen-style"
 *       brightening toward white) from the molten color of the held material;</li>
 *   <li>fill height 0.75 (+0.125 per tank below/above in the column);</li>
 *   <li>side quads are drawn only where a neighboring tank stands (solid
 *       column) -- the melt is invisible against touching tanks.</li>
 * </ul>
 * The spec uses a custom, rotation-free block transform (T(0.5,0,0.5) only):
 * the tank block has no facing and the hook quads are in world-axis directions,
 * so the default R_y(90) basis would rotate them away from the neighbor tests.
 * Inside the hook, legacy 0..1 block-local coordinates are shifted by
 * (-0.5, 0, -0.5) onto the centered basis.
 */
public final class MachineFoundryTankRenderer {

    private MachineFoundryTankRenderer() {}

    public static void register() {
        MachineRenderers.machine("foundry_tank", com.hbm_m.blockentity.ModBlockEntities.FOUNDRY_TANK_BE.get(),
                MachineFoundryTankBlockEntity.class)
            .viewDistance(256)
            .blockTransform(MachineFoundryTankRenderer::applyBlockTransform)
            .hook(new TankHook())
            .register();
    }

    /** Translation-only block transform: no facing, world-axis hook geometry. */
    private static void applyBlockTransform(MachineFoundryTankBlockEntity be, LegacyAnimator animator) {
        animator.translate(0.5, 0.0, 0.5);
    }

    /** Immediate content: melt surface + column side quads. */
    private static final class TankHook implements MachineRenderHook<MachineFoundryTankBlockEntity> {

        @Override
        public void render(MachineFoundryTankBlockEntity be, float partialTick, PoseStack poseStack,
                           MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                           MachineRenderApi api) {

            float fill = be.getCapacity() > 0 ? (float) be.amount / be.getCapacity() : 0f;
            if (fill <= 0f) return;
            Level level = be.getLevel();
            if (level == null) return;

            var pos = be.getBlockPos();
            Block self = ModBlocks.FOUNDRY_TANK.get();
            boolean conNegY = level.getBlockState(pos.below()).getBlock() == self;
            boolean conPosY = level.getBlockState(pos.above()).getBlock() == self;
            boolean conPosX = level.getBlockState(pos.east()).getBlock() == self;
            boolean conNegX = level.getBlockState(pos.west()).getBlock() == self;
            boolean conPosZ = level.getBlockState(pos.south()).getBlock() == self;
            boolean conNegZ = level.getBlockState(pos.north()).getBlock() == self;

            float max = 0.75f + (conNegY ? 0.125f : 0f) + (conPosY ? 0.125f : 0f);
            float levelY = fill * max;

            // Original: 255-(255-c)*0.7 -- "screen-style" brightening toward white.
            int argb = be.type != null ? (0xFF000000 | be.type.moltenColor) : 0xFFC18336;
            int raw = argb & 0xFFFFFF;
            int cr = 255 - (int) ((255 - (raw >> 16 & 0xFF)) * 0.7f);
            int cg = 255 - (int) ((255 - (raw >> 8 & 0xFF)) * 0.7f);
            int cb = 255 - (int) ((255 - (raw & 0xFF)) * 0.7f);

            Matrix4f m = poseStack.last().pose();
            int fullbright = 0xF000F0;

            VertexConsumer vc = bufferSource.getBuffer(MeltRenderTypes.MELT);

            // Top surface of the melt (centered basis: legacy 0..1 minus 0.5).
            hQuad(vc, m, -0.5f, 0.5f, -0.5f, 0.5f, levelY, cr, cg, cb, fullbright);

            // Column side quads -- only where connected to neighboring tanks.
            if (conPosX) vQuad(vc, m, 0.5f, true, -0.5f, 0.5f, levelY, cr, cg, cb, fullbright);
            if (conNegX) vQuad(vc, m, -0.5f, true, -0.5f, 0.5f, levelY, cr, cg, cb, fullbright);
            if (conPosZ) vQuad(vc, m, 0.5f, false, -0.5f, 0.5f, levelY, cr, cg, cb, fullbright);
            if (conNegZ) vQuad(vc, m, -0.5f, false, -0.5f, 0.5f, levelY, cr, cg, cb, fullbright);
        }

        @Override
        public boolean mayDrawContent(MachineFoundryTankBlockEntity be) {
            // Always true (the MachineFoundryRenderer precedent): the melt level is
            // continuous fluid state with no animation epoch, so a false here could
            // let the roster fast-assert away a frame where melt just appeared.
            return true;
        }

        private static void hQuad(VertexConsumer vc, Matrix4f m,
                                  float x0, float x1, float z0, float z1, float y,
                                  int cr, int cg, int cb, int light) {
            RenderHooks.vertexColorUvLightNormal(vc, m, x0, y, z0, cr, cg, cb, 0xFF, 0, 0, light, 0, 1, 0);
            RenderHooks.vertexColorUvLightNormal(vc, m, x0, y, z1, cr, cg, cb, 0xFF, 0, 1, light, 0, 1, 0);
            RenderHooks.vertexColorUvLightNormal(vc, m, x1, y, z1, cr, cg, cb, 0xFF, 1, 1, light, 0, 1, 0);
            RenderHooks.vertexColorUvLightNormal(vc, m, x1, y, z0, cr, cg, cb, 0xFF, 1, 0, light, 0, 1, 0);
        }

        /**
         * Vertical quad of the melt column, height 0..y.
         * axisX=true -- the plane X=fixed (east/west face), otherwise Z=fixed (north/south).
         */
        private static void vQuad(VertexConsumer vc, Matrix4f m,
                                  float fixed, boolean axisX, float u0, float u1, float y,
                                  int cr, int cg, int cb, int light) {
            if (axisX) {
                RenderHooks.vertexColorUvLightNormal(vc, m, fixed, 0f, u0, cr, cg, cb, 0xFF, 0, 1, light, 1, 0, 0);
                RenderHooks.vertexColorUvLightNormal(vc, m, fixed, y,  u0, cr, cg, cb, 0xFF, 0, 0, light, 1, 0, 0);
                RenderHooks.vertexColorUvLightNormal(vc, m, fixed, y,  u1, cr, cg, cb, 0xFF, 1, 0, light, 1, 0, 0);
                RenderHooks.vertexColorUvLightNormal(vc, m, fixed, 0f, u1, cr, cg, cb, 0xFF, 1, 1, light, 1, 0, 0);
            } else {
                RenderHooks.vertexColorUvLightNormal(vc, m, u0, 0f, fixed, cr, cg, cb, 0xFF, 0, 1, light, 0, 0, 1);
                RenderHooks.vertexColorUvLightNormal(vc, m, u0, y,  fixed, cr, cg, cb, 0xFF, 0, 0, light, 0, 0, 1);
                RenderHooks.vertexColorUvLightNormal(vc, m, u1, y,  fixed, cr, cg, cb, 0xFF, 1, 0, light, 0, 0, 1);
                RenderHooks.vertexColorUvLightNormal(vc, m, u1, 0f, fixed, cr, cg, cb, 0xFF, 1, 1, light, 0, 0, 1);
            }
        }
    }

    /** Melt layer: translucent, no backface culling (the original was double-sided). */
    private static final class MeltRenderTypes extends RenderType {
        private static final ResourceLocation LAVA_TEXTURE =
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/fluids/lava_gray.png");

        private static final RenderType MELT = create("hbm_m_foundry_tank_melt",
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

        private MeltRenderTypes(String s, VertexFormat v, VertexFormat.Mode m,
                                int i, boolean b, boolean b2, Runnable r, Runnable r2) { super(s, v, m, i, b, b2, r, r2); }
    }
}
