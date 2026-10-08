package com.hbm_m.client.render.legacy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** 1:1 {@code ModelB92} (Techne, 21 Quader, Textur 64x64, alle gespiegelt); {@code renderAnim} schiebt die Pumpe um {@code tran} Bloecke. */
public class ModelB92 {

    private static final String[] NAMES = { "Muzzle1", "Barrel1", "Barrel2", "Grip", "Front1", "Front2", "Body", "Top", "GripBottom", "Handle", "HandleBack", "Frame1", "Frame2", "Frame3", "Trigger", "BackPlate1", "Back", "BackPlate2", "Pump1", "Pump2", "BodyPlate" };
    private final ModelPart[] parts = new ModelPart[NAMES.length];
    private final ModelPart pump1, pump2;

    public ModelB92() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("Muzzle1", CubeListBuilder.create().texOffs(22, 36).mirror().addBox(0.0F, 0.0F, 0.0F, 2.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(-24.0F, 0.5F, -1.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("Barrel1", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0.0F, 0.0F, 0.0F, 24.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(-24.0F, 1.0F, -1.5F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("Barrel2", CubeListBuilder.create().texOffs(0, 5).mirror().addBox(0.0F, 0.0F, 0.0F, 22.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-22.0F, 0.5F, -1.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("Grip", CubeListBuilder.create().texOffs(0, 8).mirror().addBox(0.0F, 0.0F, 0.0F, 20.0F, 3.0F, 4.0F), PartPose.offsetAndRotation(-20.0F, 3.0F, -2.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("Front1", CubeListBuilder.create().texOffs(10, 36).mirror().addBox(0.0F, 0.0F, 0.0F, 2.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(-22.0F, 0.5F, -2.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("Front2", CubeListBuilder.create().texOffs(0, 36).mirror().addBox(0.0F, 0.0F, 0.0F, 2.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(-22.0F, 0.0F, -1.5F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("Body", CubeListBuilder.create().texOffs(0, 15).mirror().addBox(0.0F, 0.0F, 0.0F, 15.0F, 7.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.5F, -2.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("Top", CubeListBuilder.create().texOffs(28, 60).mirror().addBox(0.0F, 0.0F, 0.0F, 15.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -1.5F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("GripBottom", CubeListBuilder.create().texOffs(24, 43).mirror().addBox(0.0F, 0.0F, 0.0F, 18.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-18.0F, 5.5F, -1.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("Handle", CubeListBuilder.create().texOffs(0, 45).mirror().addBox(0.0F, 0.0F, 0.0F, 6.0F, 15.0F, 4.0F), PartPose.offsetAndRotation(6.0F, 7.0F, -2.0F, 0.0F, 0.0F, -0.2268928F));
        root.addOrReplaceChild("HandleBack", CubeListBuilder.create().texOffs(20, 46).mirror().addBox(5.5F, 0.0F, 0.0F, 1.0F, 15.0F, 3.0F), PartPose.offsetAndRotation(6.0F, 7.0F, -1.5F, 0.0F, 0.0F, -0.2268928F));
        root.addOrReplaceChild("Frame1", CubeListBuilder.create().texOffs(28, 57).mirror().addBox(0.0F, 0.0F, 0.0F, 7.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.5F, 11.0F, -1.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("Frame2", CubeListBuilder.create().texOffs(28, 51).mirror().addBox(0.0F, 0.0F, 0.0F, 2.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(-2.0F, 6.5F, -1.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("Frame3", CubeListBuilder.create().texOffs(46, 57).mirror().addBox(0.0F, -1.0F, 0.0F, 3.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(-2.0F, 10.5F, -1.0F, 0.0F, 0.0F, 0.5235988F));
        root.addOrReplaceChild("Trigger", CubeListBuilder.create().texOffs(36, 53).mirror().addBox(0.0F, 0.0F, 0.0F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(4.0F, 7.0F, -0.5F, 0.0F, 0.0F, 0.1919862F));
        root.addOrReplaceChild("BackPlate1", CubeListBuilder.create().texOffs(56, 53).mirror().addBox(-1.0F, 0.0F, 0.0F, 1.0F, 4.0F, 3.0F), PartPose.offsetAndRotation(15.0F, 0.0F, -1.5F, 0.0F, 0.0F, -0.5235988F));
        root.addOrReplaceChild("Back", CubeListBuilder.create().texOffs(42, 49).mirror().addBox(0.0F, 0.0F, 0.0F, 2.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(15.0F, 3.5F, -2.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("BackPlate2", CubeListBuilder.create().texOffs(48, 5).mirror().addBox(-2.0F, 0.0F, 0.0F, 2.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(15.0F, 0.5F, -2.0F, 0.0F, 0.0F, -0.4886922F));
        root.addOrReplaceChild("Pump1", CubeListBuilder.create().texOffs(46, 29).mirror().addBox(0.0F, 0.0F, 0.0F, 7.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(10.0F, 1.0F, -1.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("Pump2", CubeListBuilder.create().texOffs(44, 33).mirror().addBox(0.0F, 0.0F, 0.0F, 3.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(17.0F, 0.5F, -3.5F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("BodyPlate", CubeListBuilder.create().texOffs(0, 26).mirror().addBox(0.0F, 0.0F, 0.0F, 14.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(1.5F, 2.0F, -2.5F, 0.0F, 0.0F, 0.0F));
        ModelPart baked = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        for (int i = 0; i < NAMES.length; i++) parts[i] = baked.getChild(NAMES[i]);
        pump1 = baked.getChild("Pump1");
        pump2 = baked.getChild("Pump2");
    }

    /** Original renderAnim(..., 0.0625F, tran): offsetX in Bloecken = 16 Pixel je Block. */
    public void renderAnim(PoseStack ps, VertexConsumer vc, int light, int overlay, float tran) {
        float px = tran * 16F;
        pump1.x += px;
        pump2.x += px;
        for (ModelPart p : parts) p.render(ps, vc, light, overlay);
        pump1.x -= px;
        pump2.x -= px;
    }
}
