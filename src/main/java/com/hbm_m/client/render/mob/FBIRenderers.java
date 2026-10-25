package com.hbm_m.client.render.mob;

import java.util.Random;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.entity.mob.EntityFBI;
import com.hbm_m.entity.mob.EntityFBIDrone;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/** 1:1 {@code RenderFBI} (Biped, Arme stets im Bogenanschlag) und {@code RenderDrone} (Quadcopter, Zufallsdrehung). */
public final class FBIRenderers {

    private FBIRenderers() { }

    public static class FBI extends HumanoidMobRenderer<EntityFBI, HumanoidModel<EntityFBI>> {

        private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/fbi.png");

        public FBI(EntityRendererProvider.Context ctx) {
            super(ctx, new HumanoidModel<>(LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 32).bakeRoot()), 0.5F);
            this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                    new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
        }

        @Override
        public void render(@NotNull EntityFBI entity, float yaw, float partialTicks, @NotNull PoseStack ps, @NotNull MultiBufferSource buffers, int light) {
            this.model.rightArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
            this.model.leftArmPose = HumanoidModel.ArmPose.BOW_AND_ARROW;
            super.render(entity, yaw, partialTicks, ps, buffers, light);
        }

        @Override
        public @NotNull ResourceLocation getTextureLocation(@NotNull EntityFBI entity) {
            return TEX;
        }
    }

    public static class Drone extends EntityRenderer<EntityFBIDrone> {

        private static final SimpleObjModel MODEL = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/mobs/quadcopter.obj"));
        private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/entity/quadcopter.png");

        public Drone(EntityRendererProvider.Context ctx) {
            super(ctx);
        }

        @Override
        public void render(@NotNull EntityFBIDrone entity, float yaw, float f1, PoseStack ps, MultiBufferSource buffers, int light) {
            ps.pushPose();
            ps.translate(0, 0.25, 0);
            Random rand = new Random(entity.getId());
            ps.mulPose(Axis.YP.rotationDegrees((float) (rand.nextDouble() * 360D)));
            MODEL.renderAll(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(TEX)), light);
            ps.popPose();
            super.render(entity, yaw, f1, ps, buffers, light);
        }

        @Override
        public @NotNull ResourceLocation getTextureLocation(@NotNull EntityFBIDrone entity) {
            return TEX;
        }
    }
}
