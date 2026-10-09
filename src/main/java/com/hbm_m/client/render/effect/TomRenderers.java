package com.hbm_m.client.render.effect;

import java.util.Random;

import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.UvShiftConsumer;
import com.hbm_m.entity.effect.EntityCloudTom;
import com.hbm_m.entity.projectile.TomEntity;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code RenderTom} (mit {@code TomPronter.prontTom}) und {@code RenderCloudTom}. */
public final class TomRenderers {

    private TomRenderers() { }

    private static ResourceLocation rl(String p) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, p);
    }

    private static final SimpleObjModel TOM_MAIN = new SimpleObjModel(rl("models/weapons/tom_main.obj"));
    private static final SimpleObjModel TOM_FLAME = new SimpleObjModel(rl("models/weapons/tom_flame.obj"));
    private static final ResourceLocation TOM_MAIN_TEX = rl("textures/models/weapons/tom_main.png");
    private static final ResourceLocation TOM_FLAME_TEX = rl("textures/models/weapons/tom_flame.png");
    private static final ResourceLocation TOMBLAST = rl("textures/models/explosion/tomblast.png");

    /** {@code RenderTom}: Tom 50 Bloecke unter der Entity, 100-fach, mit 20 rotierenden Flammenschalen. */
    public static class Tom extends EntityRenderer<TomEntity> {

        public Tom(EntityRendererProvider.Context ctx) {
            super(ctx);
        }

        @Override
        public void render(@NotNull TomEntity entity, float yaw, float f1, PoseStack ps, MultiBufferSource buffers, int light) {
            ps.pushPose();
            ps.translate(0, -50, 0);
            prontTom(ps, buffers);
            ps.popPose();
        }

        @Override
        public boolean shouldRender(@NotNull TomEntity entity, @NotNull Frustum frustum, double x, double y, double z) {
            return true;
        }

        @Override
        public @NotNull ResourceLocation getTextureLocation(@NotNull TomEntity entity) {
            return TOM_MAIN_TEX;
        }
    }

    /** {@code TomPronter.prontTom}: HMF-Flammen mit Texturlauf {@code (ms % 50000) / 2500}, additiv. */
    public static void prontTom(PoseStack ps, MultiBufferSource buffers) {
        ps.pushPose();
        ps.scale(100F, 100F, 100F);

        TOM_MAIN.renderAll(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(TOM_MAIN_TEX)), LightTexture.FULL_BRIGHT);

        float animOffset = (float) ((System.currentTimeMillis() % 50000D) / 2500D);
        VertexConsumer flame = new UvShiftConsumer(buffers.getBuffer(RenderType.eyes(TOM_FLAME_TEX)), 0F, -animOffset);

        float rot = -System.currentTimeMillis() / 10 % 360;
        ps.scale(0.8F, 5F, 0.8F);

        Random rand = new Random(0);

        for (int i = 0; i < 20; i++) {
            int r = rand.nextInt(90);

            ps.mulPose(Axis.YP.rotationDegrees(rot + r));
            TOM_FLAME.renderAll(ps, flame, LightTexture.FULL_BRIGHT);
            ps.mulPose(Axis.YN.rotationDegrees(rot));

            ps.scale(-1.015F, 0.9F, 1.015F);
        }

        ps.popPose();
    }

    /** {@code RenderCloudTom}: 16 Segmente x 5 Lagen, nach oben ausblendende Feuerwand mit laufender Textur. */
    public static class CloudTom extends EntityRenderer<EntityCloudTom> {

        public CloudTom(EntityRendererProvider.Context ctx) {
            super(ctx);
        }

        @Override
        public void render(@NotNull EntityCloudTom blast, float yaw, float f0, PoseStack ps, MultiBufferSource buffers, int light) {
            ps.pushPose();

            double scale = blast.age + f0;

            int segments = 16;
            float angle = (float) Math.toRadians(360D / segments);
            int height = 20;
            int depth = 20;

            float movement = Minecraft.getInstance().player == null ? 0F : -(Minecraft.getInstance().player.tickCount + f0) * 0.005F * 10;
            VertexConsumer vc = new UvShiftConsumer(buffers.getBuffer(RenderType.entityTranslucentEmissive(TOMBLAST, false)), 0F, movement);
            Matrix4f m = ps.last().pose();

            for (int i = 0; i < segments; i++) {

                for (int j = 0; j < 5; j++) {

                    double mod = 1 - j * 0.025;
                    double h = height + j * 10;
                    double off = 1D / j;

                    Vec3 vec = new Vec3(scale, 0, 0).yRot(angle * i);
                    float x0 = (float) (vec.x * mod);
                    float z0 = (float) (vec.z * mod);

                    vertex(vc, m, x0, (float) h, z0, 0, (float) (1 + off), 0F);
                    vertex(vc, m, x0, -depth, z0, 0, (float) (0 + off), 1F);

                    vec = vec.yRot(angle);
                    x0 = (float) (vec.x * mod);
                    z0 = (float) (vec.z * mod);

                    vertex(vc, m, x0, -depth, z0, 1, (float) (0 + off), 1F);
                    vertex(vc, m, x0, (float) h, z0, 1, (float) (1 + off), 0F);
                }
            }

            ps.popPose();
        }

        private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, float a) {
            //? if < 1.21.1 {
            vc.vertex(m, x, y, z).color(1F, 1F, 1F, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(0, 1, 0).endVertex();
            //?} else {
            /*vc.addVertex(m, x, y, z).setColor(1F, 1F, 1F, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
            *///?}
        }

        @Override
        public boolean shouldRender(@NotNull EntityCloudTom entity, @NotNull Frustum frustum, double x, double y, double z) {
            return true;
        }

        @Override
        public @NotNull ResourceLocation getTextureLocation(@NotNull EntityCloudTom entity) {
            return TOMBLAST;
        }
    }
}
