package com.hbm_m.client.render.entity;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.missile.SatellitePodEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderDropship}: Kapsel plus vier Landebeine, die beim Ausfahren um 150 Grad herunterklappen. */
public class SatellitePodRenderer extends EntityRenderer<SatellitePodEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/dropship.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/dropship.png");

    public SatellitePodRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(SatellitePodEntity pod, float yaw, float interp, PoseStack ps, MultiBufferSource buf, int light) {
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(TEX));
        MODEL.renderPart("Pod", ps, vc, light);

        float legs = pod.prevLegs + (pod.legs - pod.prevLegs) * interp;
        for (int i = 0; i < 4; i++) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(45 + 90 * i));
            ps.translate(0.5, 1.75, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(150 * (1F - legs)));
            ps.translate(-0.5, -1.75, 0);
            MODEL.renderPart("Leg", ps, vc, light);
            ps.popPose();
        }
        super.render(pod, yaw, interp, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SatellitePodEntity pod) {
        return TEX;
    }
}
