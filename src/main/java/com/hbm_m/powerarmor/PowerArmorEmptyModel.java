package com.hbm_m.powerarmor;

import com.hbm_m.powerarmor.layer.AbstractObjArmorLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nonnull;

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

    @Override
    public void renderToBuffer(@Nonnull PoseStack poseStack, @Nonnull VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        // Do nothing. The actual model is rendered via AbstractObjArmorLayer to use Forge's OBJ loader.
    }
}

