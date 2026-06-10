package com.hbm_m.particle.nt;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Расширяющееся циановое облако Fleija. Порт {@code EntityCloudFleija} + {@code RenderCloudFleija}.
 */
public class FleijaCloudParticle extends ParticleNT {

    //? if fabric && < 1.21.1 {
    /*private static final ResourceLocation TEXTURE = new ResourceLocation(RefStrings.MODID, "textures/particle/flare.png");
    *///?} else {
        private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/particle/flare.png");
    //?}

    private final int maxAge;

    public FleijaCloudParticle(ClientLevel level, double x, double y, double z, int maxAge) {
        super(level, x, y, z);
        this.maxAge = Math.max(1, maxAge);
        this.lifetime = this.maxAge;
        this.noClip = true;
    }

    @Override
    public void tick() {
        this.age++;
        this.level.setSkyFlashTime(2);
        if (this.age >= this.maxAge) {
            this.dead = true;
        }
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTicks, PoseStack levelPoseStack) {
        float ageF = this.age + partialTicks;
        double baseScale = ageF * 2.0;
        float ageScale = (float) (baseScale / this.maxAge);

        Vec3 camPos = camera.getPosition();
        PoseStack poseStack = new PoseStack();
        poseStack.translate(this.x - camPos.x, this.y - camPos.y, this.z - camPos.z);

        Vector3f left = new Vector3f(camera.getLeftVector());
        Vector3f up = new Vector3f(camera.getUpVector());
        int light = LightTexture.FULL_BRIGHT;
        int overlay = OverlayTexture.NO_OVERLAY;

        renderBillboard(poseStack, consumer, left, up, light, overlay, ageScale, baseScale, 0.0F, 1.0F, 1.0F, 0.85F);
        renderBillboard(poseStack, consumer, left, up, light, overlay, ageScale, baseScale, 0.0F, 0.125F, 0.125F, 0.65F);

        float shockTint = (1.0F - ageScale) * 0.75F;
        renderShockwave(poseStack, consumer, left, up, light, overlay, (float) (5.0 * baseScale), shockTint);
    }

    private static void renderBillboard(PoseStack poseStack, VertexConsumer consumer,
                                        Vector3f left, Vector3f up, int light, int overlay,
                                        float ageScale, double baseScale,
                                        float r, float g, float b, float alphaMul) {
        float scale = ageScale * 1.2F;
        if (scale > 1.0F) {
            scale = Math.max(1.0F - (scale - 1.0F) * 5.0F, 0.0F);
        }
        scale *= (float) (2.0 * baseScale);

        for (int layer = 0; layer < 4; layer++) {
            float layerScale = scale * (float) Math.pow(1.05, layer);
            drawQuad(poseStack, consumer, left, up, light, overlay, layerScale, r, g, b, alphaMul);
        }
    }

    private static void renderShockwave(PoseStack poseStack, VertexConsumer consumer,
                                        Vector3f left, Vector3f up, int light, int overlay,
                                        float scale, float tint) {
        drawQuad(poseStack, consumer, left, up, light, overlay, scale, tint, tint, tint, 0.5F);
    }

    private static void drawQuad(PoseStack poseStack, VertexConsumer consumer,
                                 Vector3f left, Vector3f up, int light, int overlay,
                                 float scale, float r, float g, float b, float alpha) {
        Matrix4f matrix = poseStack.last().pose();
        Vector3f l = new Vector3f(left).mul(scale);
        Vector3f u = new Vector3f(up).mul(scale);

        consumer.vertex(matrix, -l.x - u.x, -l.y - u.y, -l.z - u.z).color(r, g, b, alpha).uv(1, 1).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, -l.x + u.x, -l.y + u.y, -l.z + u.z).color(r, g, b, alpha).uv(1, 0).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, l.x + u.x, l.y + u.y, l.z + u.z).color(r, g, b, alpha).uv(0, 0).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
        consumer.vertex(matrix, l.x - u.x, l.y - u.y, l.z - u.z).color(r, g, b, alpha).uv(0, 1).overlayCoords(overlay).uv2(light).normal(0, 1, 0).endVertex();
    }

    @Override
    public RenderType getRenderType() {
        return ClientRenderHandler.CustomRenderTypes.NUKE_CLOUDS.apply(TEXTURE);
    }
}
