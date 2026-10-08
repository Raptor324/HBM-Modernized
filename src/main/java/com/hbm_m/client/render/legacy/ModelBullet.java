package com.hbm_m.client.render.legacy;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** 1:1 {@code ModelBullet}. */
public class ModelBullet {

    public static final int TEX_W = 8, TEX_H = 4;

    public final ModelPart root;

    public ModelBullet() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        r.addOrReplaceChild("bullet", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 2F, 1F, 1F), PartPose.offsetAndRotation(1F, -0.5F, -0.5F, 0F, 0F, 0F));
        this.root = LayerDefinition.create(mesh, TEX_W, TEX_H).bakeRoot();
    }

    public ModelPart part(String name) {
        return root.getChild(name);
    }
}
