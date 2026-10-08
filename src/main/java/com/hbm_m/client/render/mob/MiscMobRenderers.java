package com.hbm_m.client.render.mob;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.mob.EntityBlockSpider;
import com.hbm_m.entity.mob.EntityDummy;
import com.hbm_m.entity.mob.EntityGhost;
import com.hbm_m.entity.mob.EntityPigeon;
import com.hbm_m.entity.mob.EntityPlasticBag;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Chicken;

/** 1:1 {@code RenderPigeon}, {@code RenderPlasticBag}, {@code RenderQuacc}, {@code RenderDummy}, {@code RenderGhost}, {@code RenderBlockSpider}. */
public final class MiscMobRenderers {

    private MiscMobRenderers() { }

    private static ResourceLocation tex(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path);
    }

    /** Alter 64x32-Biped ({@code ModelBiped(0.0F)}). */
    private static <T extends Mob> HumanoidModel<T> biped() {
        return new HumanoidModel<>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 32).bakeRoot());
    }

    // ─── Taube ───────────────────────────────────────────────────────────────

    /** 1:1 {@code ModelPigeon}: Fluegel haengen an beiden Rumpfvarianten (schlank/dick). */
    public static class ModelPigeon extends EntityModel<EntityPigeon> {

        private final ModelPart head, beak, body, bodyFat, leftLeg, rightLeg, ass, feathers;
        private final ModelPart[] leftWings = new ModelPart[2], rightWings = new ModelPart[2];
        private boolean fat;

        public ModelPigeon() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition r = mesh.getRoot();
            r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2F, -6F, -2F, 4, 6, 4), PartPose.offset(0F, 16F, -2F));
            r.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(14, 0).addBox(-1F, -4F, -4F, 2, 2, 2), PartPose.offset(0F, 16F, -2F));
            PartDefinition body = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 10).addBox(-3F, -3F, -4F, 6, 6, 8), PartPose.offset(0F, 17F, 0F));
            PartDefinition bodyFat = r.addOrReplaceChild("bodyFat", CubeListBuilder.create().texOffs(0, 10).addBox(-3F, -3F, -4F, 6, 6, 8, new CubeDeformation(1)), PartPose.offset(0F, 17F, 0F));
            r.addOrReplaceChild("ass", CubeListBuilder.create().texOffs(0, 24).addBox(-2F, -2F, -2F, 4, 4, 4), PartPose.offset(0F, 20F, 4F));
            r.addOrReplaceChild("feathers", CubeListBuilder.create().texOffs(16, 24).addBox(-1F, -0.5F, -2F, 2, 1, 4), PartPose.offset(0F, 21.5F, 7.5F));
            r.addOrReplaceChild("leftLeg", CubeListBuilder.create().texOffs(20, 0).addBox(-1F, 0F, 0F, 2, 4, 2), PartPose.offset(1F, 20F, -1F));
            r.addOrReplaceChild("rightLeg", CubeListBuilder.create().texOffs(20, 0).addBox(-1F, 0F, 0F, 2, 4, 2), PartPose.offset(-1F, 20F, -1F));
            for (PartDefinition b : new PartDefinition[] { body, bodyFat }) {
                b.addOrReplaceChild("leftWing", CubeListBuilder.create().texOffs(28, 0).addBox(0F, 0F, -3F, 1, 4, 6), PartPose.offset(3F, -2F, 0F));
                b.addOrReplaceChild("rightWing", CubeListBuilder.create().texOffs(28, 10).addBox(-1F, 0F, -3F, 1, 4, 6), PartPose.offset(-3F, -2F, 0F));
            }
            ModelPart root = LayerDefinition.create(mesh, 64, 32).bakeRoot();
            head = root.getChild("head");
            beak = root.getChild("beak");
            this.body = root.getChild("body");
            this.bodyFat = root.getChild("bodyFat");
            ass = root.getChild("ass");
            feathers = root.getChild("feathers");
            leftLeg = root.getChild("leftLeg");
            rightLeg = root.getChild("rightLeg");
            leftWings[0] = this.body.getChild("leftWing");
            leftWings[1] = this.bodyFat.getChild("leftWing");
            rightWings[0] = this.body.getChild("rightWing");
            rightWings[1] = this.bodyFat.getChild("rightWing");
        }

        @Override
        public void setupAnim(EntityPigeon entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            this.head.xRot = this.beak.xRot = headPitch / (180F / (float) Math.PI);
            this.head.yRot = this.beak.yRot = netHeadYaw / (180F / (float) Math.PI);
            this.body.xRot = this.bodyFat.xRot = this.ass.xRot = -((float) Math.PI / 4F);
            this.feathers.xRot = -((float) Math.PI / 8F);
            this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
            this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;
            for (ModelPart w : rightWings) w.zRot = ageInTicks;
            for (ModelPart w : leftWings) w.zRot = -ageInTicks;

            this.fat = entity.isFat();
            this.head.z = this.beak.z = fat ? -4F : -2F;
            this.ass.z = fat ? 5F : 4F;
            this.feathers.z = fat ? 8.5F : 7.5F;
            for (ModelPart w : leftWings) w.x = fat ? 4F : 3F;
            for (ModelPart w : rightWings) w.x = fat ? -4F : -3F;
        }

        @Override
        public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
            head.render(ps, vc, light, overlay, r, g, b, a);
            beak.render(ps, vc, light, overlay, r, g, b, a);
            (fat ? bodyFat : body).render(ps, vc, light, overlay, r, g, b, a);
            rightLeg.render(ps, vc, light, overlay, r, g, b, a);
            leftLeg.render(ps, vc, light, overlay, r, g, b, a);
            ass.render(ps, vc, light, overlay, r, g, b, a);
            feathers.render(ps, vc, light, overlay, r, g, b, a);
        }
    }

    public static class Pigeon extends MobRenderer<EntityPigeon, ModelPigeon> {

        private static final ResourceLocation TEX = tex("textures/entity/pigeon.png");

        public Pigeon(EntityRendererProvider.Context ctx) {
            super(ctx, new ModelPigeon(), 0.3F);
        }

        /** Original {@code handleRotationFloat}: Fluegelschlag aus fallTime/dest. */
        @Override
        protected float getBob(@NotNull EntityPigeon entity, float interp) {
            float f1 = entity.prevFallTime + (entity.fallTime - entity.prevFallTime) * interp;
            float f2 = entity.prevDest + (entity.dest - entity.prevDest) * interp;
            return (Mth.sin(f1) + 1.0F) * f2;
        }

        @Override
        public @NotNull ResourceLocation getTextureLocation(@NotNull EntityPigeon entity) {
            return TEX;
        }
    }

    // ─── Plastiktuete ────────────────────────────────────────────────────────

    public static class PlasticBag extends EntityRenderer<EntityPlasticBag> {

        private static final SimpleObjModel MODEL = new SimpleObjModel(tex("models/mobs/plasticbag.obj"));
        private static final ResourceLocation TEX = tex("textures/entity/plasticbag.png");

        public PlasticBag(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowStrength = 0.0F;
        }

        @Override
        public void render(@NotNull EntityPlasticBag entity, float yaw, float f1, PoseStack ps, MultiBufferSource buffers, int light) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(f1, entity.yRotO, entity.getYRot()) + 90.0F));
            ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(f1, entity.xRotO, entity.getXRot()) - 90));
            MODEL.renderAll(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(TEX)), light);
            ps.popPose();
            super.render(entity, yaw, f1, ps, buffers, light);
        }

        @Override
        public @NotNull ResourceLocation getTextureLocation(@NotNull EntityPlasticBag entity) {
            return TEX;
        }
    }

    // ─── Quackos ─────────────────────────────────────────────────────────────

    /** {@code RenderQuacc}: Huhnmodell mit Entenhaut, 25-fach, Schatten 7.5. */
    public static class Quackos extends ChickenRenderer {

        public Quackos(EntityRendererProvider.Context ctx) {
            super(ctx);
            this.shadowRadius = 7.5F;
        }

        @Override
        protected void scale(@NotNull Chicken entity, PoseStack ps, float interp) {
            ps.scale(25, 25, 25);
        }

        @Override
        public @NotNull ResourceLocation getTextureLocation(@NotNull Chicken entity) {
            return DuckRenderer.DUCC;
        }
    }

    // ─── Testpuppe und Geist ─────────────────────────────────────────────────

    public static class Dummy extends HumanoidMobRenderer<EntityDummy, HumanoidModel<EntityDummy>> {

        private static final ResourceLocation TEX = tex("textures/entity/dummy.png");

        public Dummy(EntityRendererProvider.Context ctx) {
            super(ctx, biped(), 0.5F);
            this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                    new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
        }

        @Override
        public @NotNull ResourceLocation getTextureLocation(@NotNull EntityDummy entity) {
            return TEX;
        }
    }

    /** {@code RenderGhost}: Biped mit Alphablending (Alpha-Schwelle 0). */
    public static class Ghost extends HumanoidMobRenderer<EntityGhost, HumanoidModel<EntityGhost>> {

        private static final ResourceLocation TEX = tex("textures/entity/ghost.png");

        public Ghost(EntityRendererProvider.Context ctx) {
            super(ctx, biped(), 0.5F);
        }

        @Override
        protected RenderType getRenderType(@NotNull EntityGhost entity, boolean visible, boolean translucent, boolean glowing) {
            return RenderType.entityTranslucent(TEX);
        }

        @Override
        public @NotNull ResourceLocation getTextureLocation(@NotNull EntityGhost entity) {
            return TEX;
        }
    }

    // ─── Blockspinne ─────────────────────────────────────────────────────────

    private static final SimpleObjModel SPIDER = new SimpleObjModel(tex("models/mobs/blockspider.obj"));

    /** 1:1 {@code ModelBlockSpider}: Beinpaare schwingen gegenlaeufig, der Block sitzt 0.75 hoeher (siehe Schicht). */
    public static class ModelBlockSpider extends EntityModel<EntityBlockSpider> {

        float rot;

        @Override
        public void setupAnim(EntityBlockSpider entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
            this.rot = -(Mth.cos(limbSwing * 0.6662F * 2.0F + 0.0F) * 0.4F) * limbSwingAmount * 57.3F;
        }

        static void base(PoseStack ps) {
            ps.mulPose(Axis.YN.rotationDegrees(90));
            ps.mulPose(Axis.ZP.rotationDegrees(180));
            ps.translate(0, -1.5F, 0);
        }

        @Override
        public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
            ps.pushPose();
            base(ps);

            ps.pushPose();
            ps.translate(0, rot * 0.005, 0);
            ps.mulPose(Axis.YP.rotationDegrees(rot));
            for (String leg : new String[] { "Leg1", "Leg3", "Leg5", "Leg7" }) SPIDER.renderPartEntity(leg, ps, vc, light, overlay, r, g, b, a);
            ps.popPose();

            ps.pushPose();
            ps.translate(0, rot * -0.005, 0);
            ps.mulPose(Axis.YN.rotationDegrees(rot));
            for (String leg : new String[] { "Leg2", "Leg4", "Leg6", "Leg8" }) SPIDER.renderPartEntity(leg, ps, vc, light, overlay, r, g, b, a);
            ps.popPose();

            ps.popPose();
        }
    }

    public static class BlockSpider extends MobRenderer<EntityBlockSpider, ModelBlockSpider> {

        private static final ResourceLocation TEX = tex("textures/entity/blockspider.png");

        public BlockSpider(EntityRendererProvider.Context ctx) {
            super(ctx, new ModelBlockSpider(), 1.0F);
            this.addLayer(new RenderLayer<>(this) {
                @Override
                public void render(@NotNull PoseStack ps, @NotNull MultiBufferSource buffers, int light, @NotNull EntityBlockSpider entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                    var block = entity.getBlock();
                    if (block == null || block.isAir()) return;
                    ps.pushPose();
                    ModelBlockSpider.base(ps);
                    ps.translate(0, 0.75, 0);
                    ps.translate(-0.5, -0.5, -0.5);
                    Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block, ps, buffers, light, OverlayTexture.NO_OVERLAY);
                    ps.popPose();
                }
            });
        }

        @Override
        public @NotNull ResourceLocation getTextureLocation(@NotNull EntityBlockSpider entity) {
            return TEX;
        }
    }
}
