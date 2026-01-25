package com.hbm_m.powerarmor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;

public class T51ArmorModel extends HumanoidModel<LivingEntity> {

    private EquipmentSlot renderSlot = EquipmentSlot.CHEST;

    public T51ArmorModel(ModelPart root) {
        super(root);
        // No manual OBJ loading here. Rendering is handled by T51PowerArmorLayer.
    }

    public void setRenderSlot(EquipmentSlot slot) {
        this.renderSlot = slot;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void renderToBuffer(@Nonnull PoseStack poseStack, @Nonnull VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        // Do nothing. The actual model is rendered via T51PowerArmorLayer to use Forge's OBJ loader.
    }
}



