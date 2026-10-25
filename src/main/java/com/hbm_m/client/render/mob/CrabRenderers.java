package com.hbm_m.client.render.mob;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.render.legacy.ModelCrab;
import com.hbm_m.client.render.util.BeamPronter;
import com.hbm_m.entity.mob.EntityCyberCrab;
import com.hbm_m.entity.mob.EntityTaintCrab;
import com.hbm_m.entity.mob.EntityTeslaCrab;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code RenderCyberCrab}, {@code RenderTeslaCrab}, {@code RenderTaintCrab} samt {@code ModelTeslaCrab/ModelTaintCrab}. */
public final class CrabRenderers {

    private CrabRenderers() { }

    private static ResourceLocation tex(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path);
    }

    /** {@code RenderCyberCrab}: Quadermodell, Schatten 1 ohne Deckkraft. */
    public static class Cyber extends MobRenderer<EntityCyberCrab, ModelCrab<EntityCyberCrab>> {

        private static final ResourceLocation TEX = tex("textures/entity/crab.png");

        public Cyber(EntityRendererProvider.Context ctx) {
            super(ctx, new ModelCrab<>(), 1.0F);
            this.shadowStrength = 0.0F;
        }

        @Override
        public @NotNull ResourceLocation getTextureLocation(@NotNull EntityCyberCrab entity) {
            return TEX;
        }
    }

    /** {@code ModelTeslaCrab}/{@code ModelTaintCrab}: OBJ-Teile, Beine schwenken um Y. */
    public static class ObjCrabModel<T extends EntityCyberCrab> extends EntityModel<T> {

        private final SimpleObjModel model;
        private final boolean taint;
        private float limbSwing, limbSwingAmount;

        public ObjCrabModel(SimpleObjModel model, boolean taint) {
            this.model = model;
            this.taint = taint;
        }

        @Override
        public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            this.limbSwing = limbSwing;
            this.limbSwingAmount = limbSwingAmount;
        }

        @Override
        //? if < 1.21.1 {
        public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        //?} else {
        /*public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, int hbmColor) {
            float r = net.minecraft.util.FastColor.ARGB32.red(hbmColor) / 255F, g = net.minecraft.util.FastColor.ARGB32.green(hbmColor) / 255F;
            float b = net.minecraft.util.FastColor.ARGB32.blue(hbmColor) / 255F, a = net.minecraft.util.FastColor.ARGB32.alpha(hbmColor) / 255F;
        *///?}
            ps.pushPose();
            if (taint) ps.mulPose(Axis.YN.rotationDegrees(90));
            ps.mulPose(Axis.ZP.rotationDegrees(180));
            ps.translate(0, -1.5F, 0);
            float rot = -(Mth.cos(limbSwing * 0.6662F * 2.0F + 0.0F) * 0.4F) * limbSwingAmount * 57.3F;

            model.renderPartEntity("Body", ps, vc, light, overlay, r, g, b, a);

            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(rot));
            model.renderPartEntity(taint ? "Legs1" : "Front", ps, vc, light, overlay, r, g, b, a);
            ps.popPose();

            ps.pushPose();
            ps.mulPose(Axis.YN.rotationDegrees(rot));
            model.renderPartEntity(taint ? "Legs2" : "Back", ps, vc, light, overlay, r, g, b, a);
            ps.popPose();

            ps.popPose();
        }
    }

    private static final SimpleObjModel TESLA_OBJ = new SimpleObjModel(tex("models/mobs/teslacrab.obj"));
    private static final SimpleObjModel TAINT_OBJ = new SimpleObjModel(tex("models/mobs/taintcrab.obj"));

    /** Blitze zu den {@code zap}-Zielen ab Hoehe {@code dy} (Original 0x404040, RANDOM/SOLID). */
    static void beams(EntityCyberCrab entity, List<Vec3> targets, double dy, PoseStack ps, MultiBufferSource buffers) {
        ps.pushPose();
        ps.translate(0, dy, 0);
        double sx = entity.getX();
        double sy = entity.getY() + dy;
        double sz = entity.getZ();
        for (Vec3 target : targets) {
            double length = Math.sqrt(Math.pow(target.x - sx, 2) + Math.pow(target.y - sy, 2) + Math.pow(target.z - sz, 2));
            BeamPronter.prontBeam(ps, buffers, new Vec3(target.x - sx, target.y - sy, target.z - sz), BeamPronter.EnumWaveType.RANDOM, BeamPronter.EnumBeamType.SOLID,
                    0x404040, 0x404040, (int) (entity.level().getGameTime() % 1000 + 1), (int) (length * 5), 0.125F, 2, 0.03125F);
        }
        ps.popPose();
    }

    public static class Tesla extends MobRenderer<EntityTeslaCrab, ObjCrabModel<EntityTeslaCrab>> {

        private static final ResourceLocation TEX = tex("textures/entity/teslacrab.png");

        public Tesla(EntityRendererProvider.Context ctx) {
            super(ctx, new ObjCrabModel<>(TESLA_OBJ, false), 1.0F);
            this.shadowStrength = 0.0F;
        }

        @Override
        public void render(@NotNull EntityTeslaCrab entity, float yaw, float partialTicks, @NotNull PoseStack ps, @NotNull MultiBufferSource buffers, int light) {
            beams(entity, entity.targets, 1, ps, buffers);
            super.render(entity, yaw, partialTicks, ps, buffers, light);
        }

        @Override
        public @NotNull ResourceLocation getTextureLocation(@NotNull EntityTeslaCrab entity) {
            return TEX;
        }
    }

    public static class Taint extends MobRenderer<EntityTaintCrab, ObjCrabModel<EntityTaintCrab>> {

        private static final ResourceLocation TEX = tex("textures/entity/taintcrab.png");

        public Taint(EntityRendererProvider.Context ctx) {
            super(ctx, new ObjCrabModel<>(TAINT_OBJ, true), 1.0F);
            this.shadowStrength = 0.0F;
        }

        @Override
        public void render(@NotNull EntityTaintCrab entity, float yaw, float partialTicks, @NotNull PoseStack ps, @NotNull MultiBufferSource buffers, int light) {
            beams(entity, entity.targets, 1.25, ps, buffers);
            super.render(entity, yaw, partialTicks, ps, buffers, light);
        }

        @Override
        public @NotNull ResourceLocation getTextureLocation(@NotNull EntityTaintCrab entity) {
            return TEX;
        }
    }
}
