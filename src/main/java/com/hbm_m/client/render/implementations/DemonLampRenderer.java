package com.hbm_m.client.render.implementations;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
 *///?}

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import org.joml.Matrix4f;

import com.hbm_m.block.decorations.DemonLampBlock;
import com.hbm_m.blockentity.decorations.DemonLampBlockEntity;
import com.hbm_m.client.render.PlainBufferSource;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.platform.RenderHooks;

@OnlyIn(Dist.CLIENT)
/**
 * Demon Core Lamp rays: 16 additive quads per ring, two rings (slightly below /
 * above the block center) forming a fan of light rays that shoot outward along
 * the block's facing axis, plus a bright crossed-quad core. The lamp sphere
 * itself is the chunk-meshed OBJ block model; only the rays/core are drawn here.
 *
 * <p>Rays and core use vanilla {@link RenderType#beaconBeam} (POSITION_TEX with
 * the type's own texture, LIGHTNING additive blend, no cull, no depth write),
 * drawn through an isolated {@link PlainBufferSource}. That is the only stable
 * combination under shaderpacks, established over four iterations:
 * <ul>
 *   <li>custom render types are unusable - Iris masks vanilla shaders on them and
 *       its ExtendedShader applies the PACK program's BlendModeOverride/alpha test
 *       (hard-cut rings; Complementary "spokes" from flat glColor);</li>
 *   <li>vertex formats Iris extends (NEW_ENTITY, BLOCK, ...) underfill in a bare
 *       builder outside Iris's own pipelines ("Not filled all elements");</li>
 *   <li>lightning() (POSITION_COLOR) has no UVs - pack fallback programs sample
 *       gtexture at undefined coordinates (Photon static/dither). beaconBeam binds
 *       its own texture and carries real UVs, so the smooth radial fade lives in
 *       ray_fade.png and renders identically everywhere; the color lives in the
 *       texture too (beacon_beam has no vertex color).</li>
 * </ul>
 *
 * <p>Shaderpack soul-flame note: the blue soul-torch treatment (emissive, colored
 * voxel light) is keyed by Photon/Complementary/BSL on the <em>terrain</em> block
 * id from the pack's own {@code block.properties}. ShaderBlockIdDisguises
 * registers our block with soul_torch's id in the loaded pack's id map (polled
 * per frame, in-memory only).
 */
public class DemonLampRenderer implements BlockEntityRenderer<DemonLampBlockEntity> {

    public static final int RAYS = 16;
    public static final double NEAR = 0.375D;

    /** Blue ray color (0, 0.75, 1). */
    private static final int COLOR_R = 0;
    private static final int COLOR_G = 191;
    private static final int COLOR_B = 255;

    /** Emissive core: crossed quads at the lamp center, bright for pack bloom. */
    private static final int CORE_R = 190;
    private static final int CORE_G = 230;
    private static final int CORE_B = 255;
    private static final int CORE_ALPHA = 230;
    private static final float CORE_HALF = 0.22F;

    private static final ResourceLocation WHITE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");

    public DemonLampRenderer(BlockEntityRendererProvider.Context context) {}

    /**
     * Rays MUST be drawn through an isolated {@link PlainBufferSource}
     * (ParticleEngineNT.buffer() rule): immediate draw at the right point of the
     * BE pass, and no interaction with the shared render-pass source (which
     * Oculus replaces with its batched-entity-rendering machinery).
     */
    private static MultiBufferSource.BufferSource raysSource;

    private static synchronized MultiBufferSource.BufferSource raysSource() {
        if (raysSource == null) {
            //? if < 1.21.1 {
            raysSource = new PlainBufferSource(
                    new com.mojang.blaze3d.vertex.BufferBuilder(256));
            //?} else {
            /*raysSource = new PlainBufferSource(
                    new com.mojang.blaze3d.vertex.ByteBufferBuilder(256));
             *///?}
        }
        return raysSource;
    }

    @Override
    public void render(DemonLampBlockEntity lamp, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
        // Thin additive quads cast hard angular shadows in shadow-mapped packs -
        // the rays are pure light, they must not appear in shadow maps at all.
        if (ShaderCompatibilityDetector.isRenderingShadowPass()) {
            return;
        }
        Direction facing = lamp.getBlockState().getValue(DemonLampBlock.FACING);

        pose.pushPose();
        pose.translate(0.5D, 0.5D, 0.5D);
        orient(pose, facing);
        pose.translate(0.0D, -0.5D, 0.0D);

        Matrix4f matrix = pose.last().pose();

        // Rings: additive POSITION_COLOR, split into four brightness segments per
        // ray. Packs whose fallback declares flat glColor cannot interpolate vertex
        // color at all - the stepped segments fake the radial fade there; packs
        // with varying interpolation render them as a piecewise-smooth fade.
        MultiBufferSource.BufferSource source = raysSource();
        VertexConsumer buf = source.getBuffer(RenderType.lightning());
        emitRays(matrix, buf);
        // White in the shader sampler: pack fallback programs for lightning sample
        // gtexture at undefined UVs; white makes that a no-op. Vanilla lightning
        // ignores the sampler. Textured types rebind their own sampler later.
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, WHITE_TEXTURE);
        source.endBatch();

        // Emissive core: bright crossed quads at the block center - pack bloom and
        // the vanilla additive blend both pick these up as the light source itself.
        VertexConsumer coreBuf = source.getBuffer(RenderType.lightning());
        emitCore(matrix, coreBuf);
        source.endBatch();

        pose.popPose();
    }

    /** Rotates local space so the +Y ray fan points along {@code facing}. */
    public static void orient(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.XP.rotationDegrees(180));
            case UP -> {}
            case NORTH -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(180));
            }
            case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
            case WEST -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(90));
            }
            case EAST -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(270));
            }
        }
    }

    /** Ray brightness at the segment boundaries, ring -> tip. Vertex alpha is
     * constant per segment (flat-glColor packs pick the provoking vertex), but
     * interpolated smoothly by vanilla and varying-glColor fallbacks. */
    private static final int[] SEG_ALPHA = {100, 66, 40, 18, 4};

    private static void emitRays(Matrix4f matrix, VertexConsumer buf) {
        float turn = (float) (Math.PI * 2D / RAYS);
        float cos = Mth.cos(turn);
        float sin = Mth.sin(turn);
        float far = (float) DemonLampBlockEntity.RENDER_RANGE;
        double x = 1D, z = 0D;
        for (int j = 0; j < 2; j++) {
            float y0 = (float) (0.5D + j * 0.125D);
            float y1 = (float) (0.5D + j * 0.125D + (j == 0 ? -0.5D : 0.5D));
            for (int i = 0; i < RAYS; i++) {
                float nx = (float) (x * cos + z * sin);
                float nz = (float) (z * cos - x * sin);
                for (int s = 0; s < 4; s++) {
                    float t0 = s / 4F, t1 = (s + 1) / 4F;
                    float r0 = (float) (NEAR + (far - NEAR) * t0);
                    float r1 = (float) (NEAR + (far - NEAR) * t1);
                    float ya0 = y0 + (y1 - y0) * t0;
                    float ya1 = y0 + (y1 - y0) * t1;
                    int c0 = SEG_ALPHA[s], c1 = SEG_ALPHA[s + 1];
                    RenderHooks.vertexColor(buf, matrix, (float) (x * r0), ya0, (float) (z * r0),
                            COLOR_R, COLOR_G, COLOR_B, c0);
                    RenderHooks.vertexColor(buf, matrix, (float) (x * r1), ya1, (float) (z * r1),
                            COLOR_R, COLOR_G, COLOR_B, c1);
                    RenderHooks.vertexColor(buf, matrix, nx * r1, ya1, nz * r1,
                            COLOR_R, COLOR_G, COLOR_B, c1);
                    RenderHooks.vertexColor(buf, matrix, (float) (nx * r0), ya0, (float) (nz * r0),
                            COLOR_R, COLOR_G, COLOR_B, c0);
                }
                double nd = x * cos + z * sin;
                z = z * cos - x * sin;
                x = nd;
            }
        }
    }

    /** Crossed billboard quads at the block center (local ring frame: center y = 0.5). */
    private static void emitCore(Matrix4f matrix, VertexConsumer buf) {
        float c = 0.5F, h = CORE_HALF;
        coreQuad(buf, matrix, c - h, c - h, 0, c + h, c - h, 0, c + h, c + h, 0, c - h, c + h, 0);
        coreQuad(buf, matrix, 0, c - h, c - h, 0, c - h, c + h, 0, c + h, c + h, 0, c + h, c - h);
    }

    private static void coreQuad(VertexConsumer buf, Matrix4f matrix,
            float ax, float ay, float az, float bx, float by, float bz,
            float cx, float cy, float cz, float dx, float dy, float dz) {
        RenderHooks.vertexColor(buf, matrix, ax, ay, az, CORE_R, CORE_G, CORE_B, CORE_ALPHA);
        RenderHooks.vertexColor(buf, matrix, bx, by, bz, CORE_R, CORE_G, CORE_B, CORE_ALPHA);
        RenderHooks.vertexColor(buf, matrix, cx, cy, cz, CORE_R, CORE_G, CORE_B, CORE_ALPHA);
        RenderHooks.vertexColor(buf, matrix, dx, dy, dz, CORE_R, CORE_G, CORE_B, CORE_ALPHA);
    }

    @Override
    public boolean shouldRenderOffScreen(DemonLampBlockEntity lamp) {
        // Under a pack the rays must draw outside the frustum so shader shadow
        // maps keep them; vanilla culling via the BE render bbox is enough otherwise.
        return ShaderCompatibilityDetector.shouldRenderBlockEntityOffScreen();
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
