package com.hbm_m.client.render.implementations;

import java.util.List;
import java.util.Map;

import com.hbm_m.blockentity.machines.fusion.FusionMhdtBlockEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;

/**
 * 1:1-Port von {@code RenderFusionMHDT} (1.7.10): das Turbinengehaeuse steckt im Blockmodell,
 * hier laufen nur die Spulen - Drehung um die lokale X-Achse auf Hoehe 1,5, der Winkel wird wie
 * im Original modulo 15 genommen (die Spulen wiederholen sich alle 15 Grad).
 */
public class FusionMhdtRenderer implements BlockEntityRenderer<FusionMhdtBlockEntity> {

    private static final String OBJ = "models/block/machines/mhdt.obj";

    public FusionMhdtRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public boolean shouldRenderOffScreen(FusionMhdtBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public void render(FusionMhdtBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(OBJ);
        List<float[]> coils = obj.get("Coils");
        if (coils == null) return;

        TextureAtlasSprite sprite = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/machine/mhdt");

        pose.pushPose();
        pose.translate(0.5D, 0D, 0.5D);
        FusionTorusRenderer.applyFacing(be.getBlockState(), pose);

        float rot = Mth.lerp(partialTick, be.prevRotor, be.rotor) % 15F;
        pose.translate(0D, 1.5D, 0D);
        pose.mulPose(Axis.XP.rotationDegrees(rot));
        pose.translate(0D, -1.5D, 0D);

        RBMKColumnRenderer.renderObjGroup(buffer.getBuffer(RenderType.cutout()), pose.last().pose(),
                coils, sprite, 1F, 1F, 1F, packedLight, packedOverlay);

        pose.popPose();
    }
}
