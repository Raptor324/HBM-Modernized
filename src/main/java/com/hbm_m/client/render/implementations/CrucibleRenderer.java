package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineCrucibleBlockEntity;
import com.hbm_m.client.render.shader.ShaderCompatibilityDetector;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import org.joml.Matrix4f;

/**
 * Renderer for the Crucible BlockEntity.
 *
 * Renders a flat "molten metal" surface quad inside the crucible bowl whose
 * height depends on {@link MachineCrucibleBlockEntity#getFillLevel()}.
 *
 * Legacy equivalent: RenderCrucible (1.7.10 TileEntitySpecialRenderer)
 *
 * Coordinate frame matches the bowl geometry defined in MachineCrucibleBlock:
 *   - bowl base at y = 4/16 = 0.25
 *   - inner X/Z range: 2/16 .. 14/16  (2 px walls on every side)
 *   - lava surface Y  = BOWL_BASE + fillLevel * (1.0 - BOWL_BASE)
 *
 * Once MaterialStack / Mats is ported:
 *   1. Set fillLevel on the BlockEntity from the actual stack data.
 *   2. Set fillColor from the dominant material's moltenColor.
 *   3. The quad will automatically pick up both values.
 */

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class CrucibleRenderer implements BlockEntityRenderer<MachineCrucibleBlockEntity> {

    /** lava surface texture — re-uses the existing block/fluids/lava.png */
    private static final ResourceLocation LAVA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/fluids/lava.png");

    /** The crucible OBJ model spans 3×3 blocks around the block position; its
     *  interior (the baked "Lava" plane) covers x/z -0.5 .. 1.5 with the melt
     *  base at y = 0.5. Original surface height: y + 0.5 + fill * 0.875. */
    private static final float BOWL_BASE = 0.51f; // slightly above the baked plane to avoid z-fighting

    private static final float INNER = -0.48f;
    private static final float INNER_MAX = 1.48f;

    public CrucibleRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(MachineCrucibleBlockEntity blockEntity,
                       float partialTick,
                       PoseStack poseStack,
                       MultiBufferSource bufferSource,
                       int packedLight,
                       int packedOverlay) {

        float fill = blockEntity.getFillLevel();
        if (fill <= 0f) return; // nothing to render yet (MaterialStack not ported)

        float surfaceY = BOWL_BASE + fill * 0.875f;

        // Decompose ARGB color
        int argb  = blockEntity.getFillColor();
        float a   = ((argb >> 24) & 0xFF) / 255f;
        float r   = ((argb >> 16) & 0xFF) / 255f;
        float g   = ((argb >>  8) & 0xFF) / 255f;
        float b   =  (argb        & 0xFF) / 255f;

        // Use translucent render type with the lava texture; fullbright (legacy parity)
        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(LAVA_TEXTURE));
        Matrix4f m = poseStack.last().pose();

        // Full-bright packed light (legacy used OpenGlHelper.setLightmapTextureCoords(... 240F, 240F))
        int fullbright = 0xF000F0;

        // Flat quad: top face (normal Y+), counter-clockwise from south-west
        // (INNER, surfaceY, INNER) → (INNER, surfaceY, INNER_MAX)
        // → (INNER_MAX, surfaceY, INNER_MAX) → (INNER_MAX, surfaceY, INNER)
        //? if < 1.21.1 {
        vc.vertex(m, INNER,     surfaceY, INNER    ).color(r, g, b, a).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullbright).normal(0, 1, 0).endVertex();
        vc.vertex(m, INNER,     surfaceY, INNER_MAX).color(r, g, b, a).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullbright).normal(0, 1, 0).endVertex();
        vc.vertex(m, INNER_MAX, surfaceY, INNER_MAX).color(r, g, b, a).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullbright).normal(0, 1, 0).endVertex();
        vc.vertex(m, INNER_MAX, surfaceY, INNER    ).color(r, g, b, a).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullbright).normal(0, 1, 0).endVertex();
        //?} else {
        /*vc.addVertex(m, INNER,     surfaceY, INNER    ).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullbright).setNormal(0, 1, 0);
        vc.addVertex(m, INNER,     surfaceY, INNER_MAX).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullbright).setNormal(0, 1, 0);
        vc.addVertex(m, INNER_MAX, surfaceY, INNER_MAX).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullbright).setNormal(0, 1, 0);
        vc.addVertex(m, INNER_MAX, surfaceY, INNER    ).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(fullbright).setNormal(0, 1, 0);
        *///?}
    }

    @Override
    public boolean shouldRenderOffScreen(MachineCrucibleBlockEntity blockEntity) {
        return ShaderCompatibilityDetector.shouldRenderBlockEntityOffScreen();
    }
}

