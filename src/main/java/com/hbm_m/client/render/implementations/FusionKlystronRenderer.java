package com.hbm_m.client.render.implementations;

import java.util.List;
import java.util.Map;

import com.hbm_m.blockentity.machines.fusion.FusionKlystronBlockEntity;
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
 * 1:1-Port von {@code RenderFusionKlystron} (1.7.10): das Gehaeuse steckt im Blockmodell,
 * hier laeuft nur der Rotor - er dreht sich um die lokale X-Achse auf Hoehe 2,5.
 */
public class FusionKlystronRenderer implements BlockEntityRenderer<FusionKlystronBlockEntity> {

    private static final String OBJ = "models/block/machines/klystron.obj";

    public FusionKlystronRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public boolean shouldRenderOffScreen(FusionKlystronBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public void render(FusionKlystronBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {

        Map<String, List<float[]>> obj = RBMKColumnRenderer.getObj(OBJ);
        List<float[]> rotor = obj.get("Rotor");
        if (rotor == null) return;

        TextureAtlasSprite sprite = RBMKColumnRenderer.sprite(RefStrings.MODID, "block/machine/klystron");

        pose.pushPose();
        pose.translate(0.5D, 0D, 0.5D);
        FusionTorusRenderer.applyFacing(be.getBlockState(), pose);
        // Original: glTranslated(-1, 0, 0) nach der Ausrichtungsdrehung.
        pose.translate(-1D, 0D, 0D);

        float rot = Mth.lerp(partialTick, be.prevFan, be.fan);
        pose.translate(0D, 2.5D, 0D);
        pose.mulPose(Axis.XP.rotationDegrees(rot));
        pose.translate(0D, -2.5D, 0D);

        RBMKColumnRenderer.renderObjGroup(buffer.getBuffer(RenderType.cutout()), pose.last().pose(),
                rotor, sprite, 1F, 1F, 1F, packedLight, packedOverlay);

        pose.popPose();
    }
}
