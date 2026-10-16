package com.hbm_m.client.render.implementations;

import com.hbm_m.blockentity.machines.MachineFoundryBasinBlockEntity;
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
 * Renders the molten metal surface inside the foundry basin.
 * Original level formula (TileEntityFoundryBasin.getLevel):
 * 0.125 + amount * 0.75 / capacity
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public class FoundryBasinRenderer implements com.hbm_m.client.render.HbmBerBounds<MachineFoundryBasinBlockEntity> {

    private static final ResourceLocation LAVA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/block/fluids/lava.png");

    private static final float INNER = 2f / 16f;
    private static final float INNER_MAX = 1f - INNER;

    public FoundryBasinRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(MachineFoundryBasinBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        float fill = be.getFillLevel();
        if (fill <= 0f) return;

        float surfaceY = 0.125f + fill * 0.75f;

        int argb = be.getFillColor();
        float a = ((argb >> 24) & 0xFF) / 255f;
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >>  8) & 0xFF) / 255f;
        float b = ( argb        & 0xFF) / 255f;

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucent(LAVA_TEXTURE));
        Matrix4f m = poseStack.last().pose();
        int fullbright = 0xF000F0;

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
    public boolean shouldRenderOffScreen(MachineFoundryBasinBlockEntity blockEntity) {
        return ShaderCompatibilityDetector.shouldRenderBlockEntityOffScreen();
    }
}
