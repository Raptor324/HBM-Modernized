package com.hbm_m.client.render.implementations;

import java.util.List;
import java.util.Map;

import com.hbm_m.blockentity.machines.pile.PileControlBlockEntity;
import com.hbm_m.blockentity.machines.pile.PileLoaderBlockEntity;
import com.hbm_m.blockentity.machines.pile.PileVentBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;

/**
 * 1:1-Ports von {@code RenderPileVent}, {@code RenderPileLoader} und {@code RenderPileControl}
 * (1.7.10). Das Gehaeuse jedes Geraets steckt im Blockmodell, hier laufen nur die beweglichen
 * Teile: der Luefter, Hebel/Schlitten/Stab des Laders und der Stab der Steuerung.
 */
public final class PileDeviceRenderers {

    private PileDeviceRenderers() {}

    /** Original: {@code RenderPileVent} - der Luefter dreht sich um die Hochachse. */
    public static class Vent implements com.hbm_m.client.render.HbmBerBounds<PileVentBlockEntity> {
        public Vent(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public void render(PileVentBlockEntity be, float partialTick, PoseStack pose,
                           MultiBufferSource buffer, int packedLight, int packedOverlay) {
            Map<String, List<float[]>> obj = ObjMachines.obj("pile_vent");
            if (obj.isEmpty()) return;
            TextureAtlasSprite sprite = ObjMachines.sprite("pile_vent");
            VertexConsumer vc = buffer.getBuffer(RenderType.cutout());
            pose.pushPose();
            ObjMachines.begin(pose, be.getBlockState(), 90F);
            pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, be.getLastFan(), be.getFan())));
            ObjMachines.group(vc, pose, obj, "Fan", sprite, packedLight, packedOverlay);
            pose.popPose();
        }
    }

    /**
     * Original: {@code RenderPileLoader} - der Hebel kippt um bis zu 90 Grad, der Schlitten faehrt
     * einen halben Block vor, der Stab ist nur zu sehen, wenn einer geladen ist.
     */
    public static class Loader implements com.hbm_m.client.render.HbmBerBounds<PileLoaderBlockEntity> {
        public Loader(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public void render(PileLoaderBlockEntity be, float partialTick, PoseStack pose,
                           MultiBufferSource buffer, int packedLight, int packedOverlay) {
            Map<String, List<float[]>> obj = ObjMachines.obj("pile_loader");
            if (obj.isEmpty()) return;
            TextureAtlasSprite sprite = ObjMachines.sprite("pile_loader");
            VertexConsumer vc = buffer.getBuffer(RenderType.cutout());
            double position = be.getExtension();

            pose.pushPose();
            ObjMachines.begin(pose, be.getBlockState(), 90F);

            pose.pushPose();
            pose.translate(-0.1875D, 0.5D, 0D);
            pose.mulPose(Axis.ZP.rotationDegrees((float) (position * 90D)));
            pose.translate(0.1875D, -0.5D, 0D);
            ObjMachines.group(vc, pose, obj, "Lever", sprite, packedLight, packedOverlay);
            pose.popPose();

            pose.translate(position * -0.5D, 0D, 0D);
            ObjMachines.group(vc, pose, obj, "Slider", sprite, packedLight, packedOverlay);
            if (!be.getStack().isEmpty()) {
                ObjMachines.group(vc, pose, obj, "Rod", sprite, packedLight, packedOverlay);
            }
            pose.popPose();
        }
    }

    /** Original: {@code RenderPileControl} - der Stab steht je nach Stellung bis zu 3/4 Block hoeher. */
    public static class Control implements com.hbm_m.client.render.HbmBerBounds<PileControlBlockEntity> {
        public Control(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public void render(PileControlBlockEntity be, float partialTick, PoseStack pose,
                           MultiBufferSource buffer, int packedLight, int packedOverlay) {
            Map<String, List<float[]>> obj = ObjMachines.obj("pile_control");
            if (obj.isEmpty()) return;
            TextureAtlasSprite sprite = ObjMachines.sprite("pile_control");
            VertexConsumer vc = buffer.getBuffer(RenderType.cutout());
            pose.pushPose();
            ObjMachines.begin(pose, be.getBlockState(), 90F);
            pose.translate(0D, be.getExtension() * 0.75D, 0D);
            ObjMachines.group(vc, pose, obj, "Rod", sprite, packedLight, packedOverlay);
            pose.popPose();
        }
    }
}
