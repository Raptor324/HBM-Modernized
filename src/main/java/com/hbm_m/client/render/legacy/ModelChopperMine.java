package com.hbm_m.client.render.legacy;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** 1:1 {@code ModelChopperMine}. */
public class ModelChopperMine {

    public static final int TEX_W = 32, TEX_H = 16;

    public final ModelPart root;

    public ModelChopperMine() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition r = mesh.getRoot();
        r.addOrReplaceChild("bullet", CubeListBuilder.create().texOffs(0, 0).addBox(0F, 0F, 0F, 8F, 8F, 8F), PartPose.offsetAndRotation(-4F, -4F, -4F, 0F, 0F, 0F));
        this.root = LayerDefinition.create(mesh, TEX_W, TEX_H).bakeRoot();
    }

    public ModelPart part(String name) {
        return root.getChild(name);
    }
}
