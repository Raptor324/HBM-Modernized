package com.hbm_m.client.render.legacy;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** 1:1 {@code ModelHunterChopper} (Techne), automatisch uebersetzt; Geschuetzteile wie im Original nicht gerendert. */
public class ModelHunterChopper {

    public static final int TEX_W = 256, TEX_H = 128;

    public final ModelPart root;

    public ModelHunterChopper() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        r.addOrReplaceChild("RotorPivotStem", CubeListBuilder.create().texOffs(40, 22).addBox(0F, 0F, 0F, 1F, 4F, 1F), PartPose.offsetAndRotation(-0.5F, 0F, -0.5F, 0F, 0F, 0F));
        r.addOrReplaceChild("RotorPivotTop", CubeListBuilder.create().texOffs(40, 27).addBox(0F, 0F, 0F, 3F, 1F, 3F), PartPose.offsetAndRotation(-1.5F, -1F, -1.5F, 0F, 0F, 0F));
        r.addOrReplaceChild("RotorPivotPlate", CubeListBuilder.create().texOffs(40, 31).addBox(0F, 0F, 0F, 6F, 1F, 6F), PartPose.offsetAndRotation(-3F, 1.5F, -3F, 0F, 0F, 0F));
        r.addOrReplaceChild("TorsoBaseCenter", CubeListBuilder.create().texOffs(70, 0).addBox(0F, 0F, 0F, 14F, 4F, 2F), PartPose.offsetAndRotation(-8F, 4F, -1F, 0F, 0F, 0F));
        r.addOrReplaceChild("TorsoPlateLeft", CubeListBuilder.create().texOffs(70, 6).addBox(0F, -4F, 0F, 14F, 4F, 1F), PartPose.offsetAndRotation(-8F, 8F, -2F, -0.2268928F, 0F, 0F));
        r.addOrReplaceChild("TorsoBaseBottom", CubeListBuilder.create().texOffs(70, 11).addBox(0F, 0F, 0F, 7F, 2F, 4F), PartPose.offsetAndRotation(-4F, 8F, -2F, 0F, 0F, 0F));
        r.addOrReplaceChild("TorsoPlateRight", CubeListBuilder.create().texOffs(70, 17).addBox(0F, -4F, -1F, 14F, 4F, 1F), PartPose.offsetAndRotation(-8F, 8F, 2F, 0.2268928F, 0F, 0F));
        r.addOrReplaceChild("TorsoPlateBottom", CubeListBuilder.create().texOffs(70, 22).addBox(-5F, -2F, 0F, 5F, 2F, 4F), PartPose.offsetAndRotation(-4F, 10F, -2F, 0F, 0F, 0.2094395F));
        r.addOrReplaceChild("WingLeftPlate", CubeListBuilder.create().texOffs(110, 0).addBox(0F, -3F, 0F, 9F, 3F, 1F), PartPose.offsetAndRotation(-8F, 9F, -3F, -0.2268928F, 0F, 0F));
        r.addOrReplaceChild("WingRightPlate", CubeListBuilder.create().texOffs(130, 0).addBox(0F, -3F, 0F, 9F, 3F, 1F), PartPose.offsetAndRotation(-8F, 9F, 2F, 0.2268928F, 0F, 0F));
        r.addOrReplaceChild("WingLeft", CubeListBuilder.create().texOffs(110, 4).addBox(0F, 0F, 0F, 3F, 1F, 6F), PartPose.offsetAndRotation(-3F, 10F, -8F, 0.3490659F, 0F, 0F));
        r.addOrReplaceChild("WingLeftFront", CubeListBuilder.create().texOffs(110, 11).addBox(0F, 0F, 0F, 2F, 1F, 7F), PartPose.offsetAndRotation(-3F, 10F, -8F, 0.3490659F, -0.3490659F, -0.1745329F));
        r.addOrReplaceChild("WingLeftTip", CubeListBuilder.create().texOffs(110, 19).addBox(0F, 0F, 0F, 5F, 2F, 1F), PartPose.offsetAndRotation(-4F, 9F, -8F, 0F, 0F, 0F));
        r.addOrReplaceChild("WingRight", CubeListBuilder.create().texOffs(130, 4).addBox(0F, 0F, -6F, 3F, 1F, 6F), PartPose.offsetAndRotation(-3F, 10F, 8F, -0.3490659F, 0F, 0F));
        r.addOrReplaceChild("WingRightFront", CubeListBuilder.create().texOffs(130, 11).addBox(0F, 0F, -7F, 2F, 1F, 7F), PartPose.offsetAndRotation(-3F, 10F, 8F, -0.3490659F, 0.3490659F, -0.1745329F));
        r.addOrReplaceChild("WingRightTip", CubeListBuilder.create().texOffs(130, 19).addBox(0F, 0F, 0F, 5F, 2F, 1F), PartPose.offsetAndRotation(-4F, 9F, 7F, 0F, 0F, 0F));
        r.addOrReplaceChild("TorsoBaseBack", CubeListBuilder.create().texOffs(70, 28).addBox(0F, 0F, 0F, 3F, 2F, 3F), PartPose.offsetAndRotation(3F, 7.5F, -1.5F, 0F, 0F, 0F));
        r.addOrReplaceChild("TorsoBoxBottom", CubeListBuilder.create().texOffs(70, 33).addBox(0F, -2F, 0F, 7F, 2F, 2F), PartPose.offsetAndRotation(-3F, 10F, -1F, 0F, 0F, 0.1570796F));
        r.addOrReplaceChild("TorsoPlateBack", CubeListBuilder.create().texOffs(70, 37).addBox(0F, 0F, 0F, 3F, 1F, 2F), PartPose.offsetAndRotation(6F, 4F, -1F, 0F, 0F, 0.2268928F));
        r.addOrReplaceChild("TorsoBoxBack", CubeListBuilder.create().texOffs(70, 40).addBox(0F, 0F, 0F, 2F, 4F, 2F), PartPose.offsetAndRotation(6F, 5F, -1F, 0F, 0F, 0F));
        r.addOrReplaceChild("TorsoPlateLeftBack", CubeListBuilder.create().texOffs(70, 46).addBox(0F, -4F, -1F, 3F, 4F, 1F), PartPose.offsetAndRotation(6F, 8.5F, -1F, -0.2268928F, 0F, 0F));
        r.addOrReplaceChild("TorsoPlateRightBack", CubeListBuilder.create().texOffs(70, 51).addBox(0F, -4F, 0F, 3F, 4F, 1F), PartPose.offsetAndRotation(6F, 8.5F, 1F, 0.2268928F, 0F, 0F));
        r.addOrReplaceChild("TailFrontBase", CubeListBuilder.create().texOffs(24, 54).addBox(0F, 0F, 0F, 5F, 2F, 2F), PartPose.offsetAndRotation(8F, 6F, -1F, 0F, 0F, 0F));
        r.addOrReplaceChild("TailFrontPlate", CubeListBuilder.create().texOffs(24, 58).addBox(-5F, 0F, 0F, 5F, 1F, 2F), PartPose.offsetAndRotation(13F, 6F, -1F, 0F, 0F, 0.2268928F));
        r.addOrReplaceChild("TailBackBase", CubeListBuilder.create().texOffs(24, 61).addBox(0F, 0F, 0F, 4F, 2F, 1F), PartPose.offsetAndRotation(13F, 6F, -0.5F, 0F, 0F, 0F));
        r.addOrReplaceChild("TailRotorFront", CubeListBuilder.create().texOffs(24, 64).addBox(0F, 0F, 0F, 1F, 3F, 1F), PartPose.offsetAndRotation(15.5F, 8F, -0.5F, 0F, 0F, -0.2268928F));
        r.addOrReplaceChild("TailRotorTop", CubeListBuilder.create().texOffs(24, 68).addBox(0F, 0F, 0F, 3F, 1F, 1F), PartPose.offsetAndRotation(17F, 6F, -0.5F, 0F, 0F, 0F));
        r.addOrReplaceChild("TailRotorBack", CubeListBuilder.create().texOffs(24, 70).addBox(0F, 0F, 0F, 1F, 4F, 1F), PartPose.offsetAndRotation(20F, 6F, -0.5F, 0F, 0F, 0F));
        r.addOrReplaceChild("TailRotorBottom", CubeListBuilder.create().texOffs(24, 75).addBox(0F, 0F, 0F, 3F, 1F, 1F), PartPose.offsetAndRotation(18F, 10F, -0.5F, 0F, 0F, 0F));
        r.addOrReplaceChild("TailRotorBlades", CubeListBuilder.create().texOffs(120, 120).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 0F), PartPose.offsetAndRotation(17F + 1.5F, 7F + 1.5F, 0F, 0F, 0F, 0F));
        r.addOrReplaceChild("TailRotorPivot", CubeListBuilder.create().texOffs(24, 77).addBox(0F, 0F, 0F, 1F, 2F, 1F), PartPose.offsetAndRotation(18F, 8F, -0.5F, 0F, 0F, 0F));
        r.addOrReplaceChild("HeadNeck", CubeListBuilder.create().texOffs(0, 40).addBox(-1F, 0F, 0F, 1F, 6F, 3F), PartPose.offsetAndRotation(-7F, 4F, -1.5F, 0F, 0F, 0.2268928F));
        r.addOrReplaceChild("HeadBack", CubeListBuilder.create().texOffs(0, 49).addBox(0F, 0F, 0F, 1F, 7F, 4F), PartPose.offsetAndRotation(-8.5F, 3.5F, -2F, 0F, 0F, 0.2268928F));
        r.addOrReplaceChild("HeadBase", CubeListBuilder.create().texOffs(0, 60).addBox(-2F, 1F, 0F, 2F, 6F, 4F), PartPose.offsetAndRotation(-8.5F, 3.5F, -2F, 0F, 0F, 0.2268928F));
        r.addOrReplaceChild("HeadTop", CubeListBuilder.create().texOffs(0, 70).addBox(-2F, 0F, 0F, 2F, 2F, 4F), PartPose.offsetAndRotation(-8.5F, 3.5F, -2F, 0F, 0F, -0.2268928F));
        r.addOrReplaceChild("HeadFront", CubeListBuilder.create().texOffs(0, 76).addBox(0F, 0F, 0F, 2F, 4F, 2F), PartPose.offsetAndRotation(-13F, 5F, -1F, 0F, 0F, 0F));
        r.addOrReplaceChild("HeadLeft", CubeListBuilder.create().texOffs(0, 82).addBox(-3F, 0F, 0F, 3F, 4F, 1F), PartPose.offsetAndRotation(-10F, 5F, -2F, 0F, 0.3490659F, 0F));
        r.addOrReplaceChild("HeadRight", CubeListBuilder.create().texOffs(0, 87).addBox(-3F, 0F, -1F, 3F, 4F, 1F), PartPose.offsetAndRotation(-10F, 5F, 2F, 0F, -0.3490659F, 0F));
        r.addOrReplaceChild("HeadFrontTop", CubeListBuilder.create().texOffs(0, 92).addBox(-3F, 0F, 0F, 3F, 1F, 2F), PartPose.offsetAndRotation(-10.5F, 4F, -1F, 0F, 0F, -0.3490659F));
        r.addOrReplaceChild("TorsoRotorBottom", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 3F, 1F, 1F), PartPose.offsetAndRotation(-7F, 11.5F, -0.5F, 0F, 0F, 0F));
        r.addOrReplaceChild("TorsoRotorFront", CubeListBuilder.create().texOffs(0, 2).addBox(0F, 0F, 0F, 1F, 3F, 1F), PartPose.offsetAndRotation(-8F, 9F, -0.5F, 0F, 0F, 0F));
        r.addOrReplaceChild("TorsoRotorBack", CubeListBuilder.create().texOffs(0, 6).addBox(0F, 0F, 0F, 1F, 2F, 1F), PartPose.offsetAndRotation(-4F, 10F, -0.5F, 0F, 0F, 0F));
        r.addOrReplaceChild("TorsoRotorBlades", CubeListBuilder.create().texOffs(112, 120).addBox(-1.5F, -1.5F, 0F, 3F, 3F, 0F), PartPose.offsetAndRotation(-7F + 1.5F, 8.5F + 1.5F, 0F, 0F, 0F, 0F));
        r.addOrReplaceChild("TorsoRotorPivot", CubeListBuilder.create().texOffs(0, 9).addBox(0F, 0F, 0F, 1F, 2F, 1F), PartPose.offsetAndRotation(-6F, 8.5F, -0.5F, 0F, 0F, 0F));
        r.addOrReplaceChild("RotorBlades", CubeListBuilder.create().texOffs(76, 68).addBox(-30F, 0F, -30F, 60F, 0F, 60F), PartPose.offsetAndRotation(0F, 1.5F, 0F, 0F, 0F, 0F));
        r.addOrReplaceChild("Antenna1", CubeListBuilder.create().texOffs(0, 95).addBox(0F, 0F, 0F, 4F, 1F, 1F), PartPose.offsetAndRotation(-14F, 4F, 0.5F, 0F, 0F, 0F));
        r.addOrReplaceChild("Antenna2", CubeListBuilder.create().texOffs(0, 97).addBox(0F, 0F, 0F, 2F, 1F, 1F), PartPose.offsetAndRotation(-15F, 7F, 0F, 0F, 0F, 0F));
        this.root = LayerDefinition.create(mesh, TEX_W, TEX_H).bakeRoot();
    }

    public ModelPart part(String name) {
        return root.getChild(name);
    }
}
