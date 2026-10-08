package com.hbm_m.client.render.mob;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.mob.glyphid.EntityGlyphid;
import com.hbm_m.entity.mob.glyphid.EntityGlyphidNuclear;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 1:1 {@code RenderGlyphid} / {@code RenderGlyphidNuclear} mit {@code ModelGlyphid}: das OBJ-Modell mit Panzerplatten
 * nach den Panzerbits, Bein-, Arm- und Kieferanimation aus Laufzyklus und Biss, befallene Glyphiden mit
 * durchscheinender Befallsschicht. Johnson traegt eine Mini-Nuke, blaeht sich beim Tod auf und blitzt weiss.
 */
public class GlyphidRenderer<T extends EntityGlyphid> extends MobRenderer<T, GlyphidRenderer.ModelGlyphid<T>> {

    public static final SimpleObjModel GLYPHID = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/mobs/glyphid.obj"));
    public static final SimpleObjModel PROJECTILES = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/projectiles.obj"));
    public static final ResourceLocation glyphid_infested_tex = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/glyphid_infestation.png");
    public static final ResourceLocation mini_nuke_tex = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/models/projectiles/mini_nuke.png");

    public GlyphidRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new ModelGlyphid<>(), 1.0F);
        this.shadowStrength = 0.0F;
        this.addLayer(new InfestationLayer<>(this));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(T entity) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/" + entity.getSkinName() + ".png");
    }

    @Override
    public void render(T entity, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        this.model.buffers = buf;
        super.render(entity, yaw, pt, ps, buf, light);
    }

    /** {@code RenderGlyphidNuclear.preRenderCallback}: Aufblaehen beim Todeszaehler. */
    @Override
    protected void scale(T entity, PoseStack ps, float interp) {
        if (entity instanceof EntityGlyphidNuclear nuclear) {
            float swell = (float) (nuclear.deathTicks + interp) / 95F;
            float flash = 1.0F + Mth.sin(swell * 100.0F) * swell * 0.01F;

            if (swell < 0.0F) swell = 0.0F;
            if (swell > 1.0F) swell = 1.0F;

            swell *= swell;
            swell *= swell;

            float scaleHorizontal = (1.0F + swell * 0.4F) * flash;
            float scaleVertical = (1.0F + swell * 0.1F) / flash;
            ps.scale(scaleHorizontal, scaleVertical, scaleHorizontal);
        }
    }

    /** {@code RenderGlyphidNuclear.getColorMultiplier}: weisser Blitz im Takt. */
    @Override
    protected float getWhiteOverlayProgress(T entity, float interp) {
        if (!(entity instanceof EntityGlyphidNuclear nuclear) || nuclear.deathTicks <= 0) return 0F;
        float swell = (float) (nuclear.deathTicks + interp) / 20F;
        int a = (int) (swell * 0.2F * 255.0F);
        if ((int) (swell * 10.0F) % 4 < 2) return 0F;
        if (a < 0) a = 0;
        if (a > 255) a = 255;
        return a / 255F;
    }

    /** Original {@code shouldRenderPass}: zweiter Durchgang mit der Befallstextur, durchscheinend. */
    public static class InfestationLayer<T extends EntityGlyphid> extends RenderLayer<T, ModelGlyphid<T>> {

        public InfestationLayer(RenderLayerParent<T, ModelGlyphid<T>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buf, int light, T entity, float limbSwing, float limbSwingAmount, float pt, float ageInTicks, float netHeadYaw, float headPitch) {
            if (entity.getSubtype() != EntityGlyphid.TYPE_INFECTED || entity instanceof EntityGlyphidNuclear) return;
            VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(glyphid_infested_tex));
            this.getParentModel().renderToBuffer(ps, vc, light, LivingEntityOverlay.overlay(entity), 1F, 1F, 1F, 1F);
        }
    }

    private static final class LivingEntityOverlay {
        static int overlay(EntityGlyphid e) {
            return OverlayTexture.pack(OverlayTexture.u(0F), OverlayTexture.v(e.hurtTime > 0 || e.deathTime > 0));
        }
    }

    public static class ModelGlyphid<T extends EntityGlyphid> extends EntityModel<T> {

        double bite = 0;
        float limbSwing;
        double scale = 1D;
        byte armor = 0b11111;
        boolean nuclear;
        MultiBufferSource buffers;

        @Override
        public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float interp) {
            bite = entity.getGlyphidSwingProgress(interp);
        }

        @Override
        public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            this.limbSwing = limbSwing;
            this.scale = entity.getGlyphidScale();
            this.armor = entity.getArmorBits();
            this.nuclear = entity instanceof EntityGlyphidNuclear;
        }

        @Override
        public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
            ps.pushPose();

            ps.mulPose(Axis.XP.rotationDegrees(180));
            ps.translate(0, -1.5F, 0);

            this.renderModel(ps, vc, light, overlay, r, g, b, a);

            ps.popPose();
        }

        private void part(String name, PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
            GLYPHID.renderPartEntity(name, ps, vc, light, overlay, r, g, b, a);
        }

        public void renderModel(PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {

            ps.pushPose();

            double s = scale;
            ps.scale((float) s, (float) s, (float) s);

            double walkCycle = limbSwing;

            double cy0 = Math.sin(walkCycle % (Math.PI * 2));
            double cy1 = Math.sin(walkCycle % (Math.PI * 2) - Math.PI * 0.5);
            double cy2 = Math.sin(walkCycle % (Math.PI * 2) - Math.PI);
            double cy3 = Math.sin(walkCycle % (Math.PI * 2) - Math.PI * 0.75);

            double bite = Mth.clamp(Math.sin(this.bite * Math.PI * 2 - Math.PI * 0.5), 0, 1) * 20;
            double headTilt = Math.sin(this.bite * Math.PI) * 30;

            part("Body", ps, vc, light, overlay, r, g, b, a);
            if ((armor & (1 << 0)) > 0) part("ArmorFront", ps, vc, light, overlay, r, g, b, a);
            if ((armor & (1 << 1)) > 0) part("ArmorLeft", ps, vc, light, overlay, r, g, b, a);
            if ((armor & (1 << 2)) > 0) part("ArmorRight", ps, vc, light, overlay, r, g, b, a);

            /// LEFT ARM ///
            ps.pushPose();
            ps.translate(0.25, 0.625, 0.0625);
            ps.mulPose(Axis.YP.rotationDegrees(10));
            ps.mulPose(Axis.XP.rotationDegrees((float) (35 + cy1 * 20)));
            ps.translate(-0.25, -0.625, -0.0625);
            part("ArmLeftUpper", ps, vc, light, overlay, r, g, b, a);
            ps.translate(0.25, 0.625, 0.4375);
            ps.mulPose(Axis.XP.rotationDegrees((float) (-75 - cy1 * 20 + cy0 * 20)));
            ps.translate(-0.25, -0.625, -0.4375);
            part("ArmLeftMid", ps, vc, light, overlay, r, g, b, a);
            ps.translate(0.25, 0.625, 0.9375);
            ps.mulPose(Axis.XP.rotationDegrees((float) (90 - cy0 * 45)));
            ps.translate(-0.25, -0.625, -0.9375);
            part("ArmLeftLower", ps, vc, light, overlay, r, g, b, a);
            if ((armor & (1 << 3)) > 0) part("ArmLeftArmor", ps, vc, light, overlay, r, g, b, a);
            ps.popPose();

            /// RIGHT ARM ///
            ps.pushPose();
            ps.translate(-0.25, 0.625, 0.0625);
            ps.mulPose(Axis.YP.rotationDegrees(-10));
            ps.mulPose(Axis.XP.rotationDegrees((float) (35 + cy2 * 20)));
            ps.translate(0.25, -0.625, -0.0625);
            part("ArmRightUpper", ps, vc, light, overlay, r, g, b, a);
            ps.translate(-0.25, 0.625, 0.4375);
            ps.mulPose(Axis.XP.rotationDegrees((float) (-75 - cy2 * 20 + cy3 * 20)));
            ps.translate(0.25, -0.625, -0.4375);
            part("ArmRightMid", ps, vc, light, overlay, r, g, b, a);
            ps.translate(-0.25, 0.625, 0.9375);
            ps.mulPose(Axis.XP.rotationDegrees((float) (90 - cy3 * 45)));
            ps.translate(0.25, -0.625, -0.9375);
            part("ArmRightLower", ps, vc, light, overlay, r, g, b, a);
            if ((armor & (1 << 4)) > 0) part("ArmRightArmor", ps, vc, light, overlay, r, g, b, a);
            ps.popPose();

            ps.pushPose();

            ps.translate(0, 0.5, 0.25);
            ps.mulPose(Axis.ZP.rotationDegrees((float) headTilt));
            ps.translate(0, -0.5, -0.25);

            ps.pushPose();
            ps.translate(0, 0.5, 0.25);
            ps.mulPose(Axis.XP.rotationDegrees((float) -bite));
            ps.translate(0, -0.5, -0.25);
            part("JawTop", ps, vc, light, overlay, r, g, b, a);
            ps.popPose();

            ps.pushPose();
            ps.translate(0, 0.5, 0.25);
            ps.mulPose(Axis.YP.rotationDegrees((float) bite));
            ps.mulPose(Axis.XP.rotationDegrees((float) bite));
            ps.translate(0, -0.5, -0.25);
            part("JawLeft", ps, vc, light, overlay, r, g, b, a);
            ps.popPose();

            ps.pushPose();
            ps.translate(0, 0.5, 0.25);
            ps.mulPose(Axis.YP.rotationDegrees((float) -bite));
            ps.mulPose(Axis.XP.rotationDegrees((float) bite));
            ps.translate(0, -0.5, -0.25);
            part("JawRight", ps, vc, light, overlay, r, g, b, a);
            ps.popPose();
            ps.popPose();

            double steppy = 15;
            double bend = 60;

            for (int i = 0; i < 3; i++) {

                double c0 = cy0 * (i == 1 ? -1 : 1);
                double c1 = cy1 * (i == 1 ? -1 : 1);

                ps.pushPose();
                ps.translate(0, 0.25, 0);
                ps.mulPose(Axis.YP.rotationDegrees((float) (i * 30 - 15 + c0 * 7.5)));
                ps.mulPose(Axis.ZP.rotationDegrees((float) (steppy + c1 * steppy)));
                ps.translate(0, -0.25, 0);
                part("LegLeftUpper", ps, vc, light, overlay, r, g, b, a);
                ps.translate(0.5625, 0.25, 0);
                ps.mulPose(Axis.ZP.rotationDegrees((float) (-bend - c1 * steppy)));
                ps.translate(-0.5625, -0.25, 0);
                part("LegLeftLower", ps, vc, light, overlay, r, g, b, a);
                ps.popPose();

                ps.pushPose();
                ps.translate(0, 0.25, 0);
                ps.mulPose(Axis.YP.rotationDegrees((float) (i * 30 - 45 + c0 * 7.5)));
                ps.mulPose(Axis.ZP.rotationDegrees((float) (-steppy + c1 * steppy)));
                ps.translate(0, -0.25, 0);
                part("LegRightUpper", ps, vc, light, overlay, r, g, b, a);
                ps.translate(-0.5625, 0.25, 0);
                ps.mulPose(Axis.ZP.rotationDegrees((float) (bend - c1 * steppy)));
                ps.translate(0.5625, -0.25, 0);
                part("LegRightLower", ps, vc, light, overlay, r, g, b, a);
                ps.popPose();
            }

            if (nuclear && buffers != null) {
                ps.translate(0, 1, 0);
                ps.mulPose(Axis.XP.rotationDegrees(90));
                VertexConsumer nuke = buffers.getBuffer(RenderType.entityCutoutNoCull(mini_nuke_tex));
                PROJECTILES.renderPartEntity("MiniNuke", ps, nuke, light, overlay, r, g, b, a);
            }

            ps.popPose();
        }
    }
}
