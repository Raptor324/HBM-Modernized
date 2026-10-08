package com.hbm_m.client.render.legacy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** 1:1 {@code ModelCrab} (Flan's Toolbox, 20 Quader): Beine schwingen um Y, Modell um -90 Grad gedreht. */
public class ModelCrab<T extends Entity> extends EntityModel<T> {

    private final ModelPart[] crabModel = new ModelPart[20];

    public ModelCrab() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        r.addOrReplaceChild("b0", CubeListBuilder.create().texOffs(1, 1).addBox(0F, 0F, 0F, 4F, 1F, 4F), PartPose.offsetAndRotation(-2F, -3F, -2F, 0F, 0F, 0F));
        r.addOrReplaceChild("b1", CubeListBuilder.create().texOffs(17, 1).addBox(0F, 0F, 0F, 4F, 1F, 6F), PartPose.offsetAndRotation(-2F, -4F, -3F, 0F, 0F, 0F));
        r.addOrReplaceChild("b2", CubeListBuilder.create().texOffs(33, 1).addBox(0F, 0F, 0F, 3F, 1F, 3F), PartPose.offsetAndRotation(-1.5F, -5F, -1.5F, 0F, 0F, 0F));
        r.addOrReplaceChild("b3", CubeListBuilder.create().texOffs(49, 1).addBox(0F, 0F, 0F, 4F, 1F, 2F), PartPose.offsetAndRotation(-2F, -4.5F, -1F, 0F, 0F, 0F));
        r.addOrReplaceChild("b4", CubeListBuilder.create().texOffs(1, 9).addBox(0F, 0F, 0F, 6F, 1F, 4F), PartPose.offsetAndRotation(-3F, -4F, -2F, 0F, 0F, 0F));
        r.addOrReplaceChild("b5", CubeListBuilder.create().texOffs(25, 9).addBox(-0.5F, 0F, 2F, 1F, 1F, 3F), PartPose.offsetAndRotation(0F, -3F, 0F, -0.17453293F, 0.78539816F, 0F));
        r.addOrReplaceChild("b6", CubeListBuilder.create().texOffs(41, 9).addBox(-0.5F, 0F, 2F, 1F, 1F, 3F), PartPose.offsetAndRotation(0F, -3F, 0F, -0.17453293F, -0.78539816F, 0F));
        r.addOrReplaceChild("b7", CubeListBuilder.create().texOffs(1, 17).addBox(-0.5F, 0F, 2F, 1F, 1F, 3F), PartPose.offsetAndRotation(0F, -3F, 0F, -0.17453293F, -2.35619449F, 0F));
        r.addOrReplaceChild("b8", CubeListBuilder.create().texOffs(17, 17).addBox(-0.5F, 0F, 2F, 1F, 1F, 3F), PartPose.offsetAndRotation(0F, -3F, 0F, -0.17453293F, 2.35619449F, 0F));
        r.addOrReplaceChild("b9", CubeListBuilder.create().texOffs(57, 9).addBox(-0.5F, 1F, 4F, 1F, 3F, 1F), PartPose.offsetAndRotation(0F, -3F, 0F, 0.17453293F, -0.78539816F, 0F));
        r.addOrReplaceChild("b10", CubeListBuilder.create().texOffs(33, 17).addBox(-0.5F, 1F, 4F, 1F, 3F, 1F), PartPose.offsetAndRotation(0F, -3F, 0F, 0.17453293F, 0.78539816F, 0F));
        r.addOrReplaceChild("b11", CubeListBuilder.create().texOffs(41, 17).addBox(-0.5F, 1F, 4F, 1F, 3F, 1F), PartPose.offsetAndRotation(0F, -3F, 0F, 0.17453293F, -2.35619449F, 0F));
        r.addOrReplaceChild("b12", CubeListBuilder.create().texOffs(49, 17).addBox(-0.5F, 1F, 4F, 1F, 3F, 1F), PartPose.offsetAndRotation(0F, -3F, 0F, 0.17453293F, 2.35619449F, 0F));
        r.addOrReplaceChild("b13", CubeListBuilder.create().texOffs(17, 1).addBox(-0.5F, 0F, 1.5F, 1F, 1F, 1F), PartPose.offsetAndRotation(0F, -3F, 0F, -0.43633231F, -0.6981317F, 0F));
        r.addOrReplaceChild("b14", CubeListBuilder.create().texOffs(33, 9).addBox(-0.5F, 0F, 1.5F, 1F, 1F, 1F), PartPose.offsetAndRotation(0F, -3F, 0F, -0.43633231F, 0.87266463F, 0F));
        r.addOrReplaceChild("b15", CubeListBuilder.create().texOffs(49, 9).addBox(-0.5F, 0F, 1.5F, 1F, 1F, 1F), PartPose.offsetAndRotation(0F, -3F, 0F, -0.43633231F, -2.26892803F, 0F));
        r.addOrReplaceChild("b16", CubeListBuilder.create().texOffs(9, 17).addBox(-0.5F, 0F, 1.5F, 1F, 1F, 1F), PartPose.offsetAndRotation(0F, -3F, 0F, -0.43633231F, 2.44346095F, 0F));
        r.addOrReplaceChild("b17", CubeListBuilder.create().texOffs(1, 25).addBox(0F, 0F, 0F, 2F, 1F, 4F), PartPose.offsetAndRotation(-1F, -4.5F, -2F, 0F, 0F, 0F));
        r.addOrReplaceChild("b18", CubeListBuilder.create().texOffs(17, 25).addBox(0F, 0F, 0F, 5F, 1F, 3F), PartPose.offsetAndRotation(-2.5F, -3.5F, -1.5F, 0F, 0F, 0F));
        r.addOrReplaceChild("b19", CubeListBuilder.create().texOffs(33, 25).addBox(0F, 0F, 0F, 3F, 1F, 5F), PartPose.offsetAndRotation(-1.5F, -3.5F, -2.5F, 0F, 0F, 0F));
        ModelPart root = LayerDefinition.create(mesh, 64, 32).bakeRoot();
        for (int i = 0; i < 20; i++) crabModel[i] = root.getChild("b" + i);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.crabModel[10].yRot = 0.78539816F;
        this.crabModel[9].yRot = -0.78539816F;
        this.crabModel[11].yRot = -2.35619449F;
        this.crabModel[12].yRot = 2.35619449F;
        float f9 = (-(Mth.cos(limbSwing * 0.6662F * 2.0F + 0.0F) * 0.4F) * limbSwingAmount) * 1.5F;
        this.crabModel[10].yRot += f9;
        this.crabModel[9].yRot -= f9;
        this.crabModel[11].yRot -= f9;
        this.crabModel[12].yRot += f9;
        this.crabModel[5].yRot = this.crabModel[10].yRot;
        this.crabModel[6].yRot = this.crabModel[9].yRot;
        this.crabModel[7].yRot = this.crabModel[11].yRot;
        this.crabModel[8].yRot = this.crabModel[12].yRot;
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        ps.pushPose();
        ps.translate(0, 1.5F, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-90));
        for (int i = 0; i < 20; i++) this.crabModel[i].render(ps, vc, light, overlay, r, g, b, a);
        ps.popPose();
    }
}
