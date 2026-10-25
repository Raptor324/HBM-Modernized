package com.hbm_m.client.render.entity;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.LegoClient;
import com.hbm_m.entity.missile.LambdaRocketEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code RenderLambdaRocket}: Raketenmodell plus Triebwerks-Flare, um einen Block zur Kamera hin versetzt, damit
 * es nicht im Modell steckt.
 */
public class LambdaRocketRenderer extends EntityRenderer<LambdaRocketEntity> {

    public static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/lambda_rocket.obj"));
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/lambda_rocket.png");

    public LambdaRocketRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(LambdaRocketEntity entity, float yaw, float interp, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutout(TEX)), light);

        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Vec3 rel = entity.getPosition(interp).subtract(cam);
        if (rel.lengthSqr() > 1.0E-6) {
            Vec3 toCam = rel.normalize().scale(-1);
            ps.translate(toCam.x, toCam.y, toCam.z);
        }
        ps.scale(2F, 2F, 2F);
        GunGL.begin(ps, buf, light);
        LegoClient.renderFlare(entity, interp, 1F, 0.75F, 0.5F);
        GunGL.end();
        ps.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(LambdaRocketEntity entity) {
        return TEX;
    }

    @Override
    public boolean shouldRender(LambdaRocketEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true;
    }
}
