package com.hbm_m.powerarmor.layer;

import org.jetbrains.annotations.NotNull;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * A "no-op" armor model.
 *
 * The real geometry for power armor is rendered via {@link AbstractObjArmorLayer} implementations
 * (Forge OBJ pipeline). This model exists only to satisfy vanilla's armor layer expectations.
 */
public class PowerArmorEmptyModel extends HumanoidModel<LivingEntity> {

    public PowerArmorEmptyModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    /** Sichtbarkeit je Slot und Pose des Originalmodells uebernehmen. */
    public static PowerArmorEmptyModel prepare(PowerArmorEmptyModel model, net.minecraft.world.entity.EquipmentSlot slot, HumanoidModel<?> original) {
        model.setAllVisible(false);
        switch (slot) {
            case HEAD -> model.head.visible = true;
            case CHEST -> {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
            }
            case LEGS, FEET -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            default -> {}
        }
        model.crouching = original.crouching;
        model.riding = original.riding;
        model.young = original.young;
        model.head.copyFrom(original.head);
        model.body.copyFrom(original.body);
        model.rightArm.copyFrom(original.rightArm);
        model.leftArm.copyFrom(original.leftArm);
        model.rightLeg.copyFrom(original.rightLeg);
        model.leftLeg.copyFrom(original.leftLeg);
        return model;
    }

    //? if < 1.21.1 {
    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        // Do nothing. The actual model is rendered via AbstractObjArmorLayer to use Forge's OBJ loader.
    }
    //?} else {
    /*@Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer buffer, int packedLight,
                               int packedOverlay, int color) {
        // Do nothing. The actual model is rendered via AbstractObjArmorLayer to use Forge's OBJ loader.
    }
    *///?}
}