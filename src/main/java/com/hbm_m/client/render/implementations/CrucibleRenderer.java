package com.hbm_m.client.render.implementations;

import com.hbm_m.block.entity.machines.MachineCrucibleBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
@OnlyIn(Dist.CLIENT)
public class CrucibleRenderer implements BlockEntityRenderer<MachineCrucibleBlockEntity> {

    /** lava surface texture — re-uses the existing block/fluids/lava.png */
    private static final ResourceLocation LAVA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/fluids/lava.png");

    /** Y of the bowl base plate top face (4 / 16) */
    private static final float BOWL_BASE = 4f / 16f;

    /** Inner wall inset from block edge (2 / 16) */
    private static final float INNER = 2f / 16f;
    private static final float INNER_MAX = 1f - INNER; // 14/16

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

        float surfaceY = BOWL_BASE + fill * (1f - BOWL_BASE);

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
        vc.vertex(m, INNER,     surfaceY, INNER    ).color(r, g, b, a).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullbright).normal(0, 1, 0).endVertex();
        vc.vertex(m, INNER,     surfaceY, INNER_MAX).color(r, g, b, a).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullbright).normal(0, 1, 0).endVertex();
        vc.vertex(m, INNER_MAX, surfaceY, INNER_MAX).color(r, g, b, a).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullbright).normal(0, 1, 0).endVertex();
        vc.vertex(m, INNER_MAX, surfaceY, INNER    ).color(r, g, b, a).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(fullbright).normal(0, 1, 0).endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(MachineCrucibleBlockEntity blockEntity) {
        return false;
    }
}

