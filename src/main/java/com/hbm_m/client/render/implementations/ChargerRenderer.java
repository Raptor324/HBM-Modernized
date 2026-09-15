package com.hbm_m.client.render.implementations;

import java.util.List;
import java.util.Map;

import com.hbm_m.blockentity.machines.ChargerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * 1:1-Port von {@code RenderCharger} (1.7.10): die Platte ({@code Base}) steckt im Blockmodell,
 * hier fahren die beiden Arme aus (erste Haelfte der Zeit) und klappen auf (zweite Haelfte),
 * dazu der Schlitten und die immer volle helle Leuchte.
 */
public class ChargerRenderer implements com.hbm_m.client.render.HbmBerBounds<ChargerBlockEntity> {

    public ChargerRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(ChargerBlockEntity be, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Map<String, List<float[]>> obj = ObjMachines.obj("charger");
        if (obj.isEmpty()) return;
        TextureAtlasSprite sprite = ObjMachines.sprite("charger");
        VertexConsumer vc = buffer.getBuffer(RenderType.cutout());

        pose.pushPose();
        ObjMachines.begin(pose, be.getBlockState(), 90F);

        double time = be.getArmProgress(partialTick);
        double extend = Math.min(1D, time * 2D);
        double swivel = Math.max(0D, (time - 0.5D) * 2D);

        // Arms: tilted by ten degrees around the base hinge, pushed out, then swivelled apart.
        pose.pushPose();
        armOffset(pose, extend);
        pose.pushPose();
        pose.translate(0D, 0.28D, 0D);
        pose.mulPose(Axis.XP.rotationDegrees((float) (30D * swivel)));
        pose.translate(0D, -0.28D, 0D);
        ObjMachines.group(vc, pose, obj, "Left", sprite, packedLight, packedOverlay);
        pose.popPose();
        pose.pushPose();
        pose.translate(0D, 0.28D, 0D);
        pose.mulPose(Axis.XP.rotationDegrees((float) (-30D * swivel)));
        pose.translate(0D, -0.28D, 0D);
        ObjMachines.group(vc, pose, obj, "Right", sprite, packedLight, packedOverlay);
        pose.popPose();
        pose.popPose();

        // Original: the light is untextured orange at full brightness; the slide rides with the arms.
        ObjMachines.group(vc, pose, obj, "Light", sprite, 1F, 0.75F, 0F, LightTexture.FULL_BRIGHT, packedOverlay);
        pose.pushPose();
        armOffset(pose, extend);
        ObjMachines.group(vc, pose, obj, "Slide", sprite, packedLight, packedOverlay);
        pose.popPose();

        pose.popPose();
    }

    private static void armOffset(PoseStack pose, double extend) {
        pose.translate(-0.34375D, 0.25D, 0D);
        pose.mulPose(Axis.ZP.rotationDegrees(10F));
        pose.translate(0.34375D, -0.25D, 0D);
        pose.translate(0D, -0.25D * extend, 0D);
    }
}
