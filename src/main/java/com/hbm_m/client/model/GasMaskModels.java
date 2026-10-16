package com.hbm_m.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Java-модели противогазов. Порты Techne-моделей {@link com.hbm.render.model.ModelGasMask}
 * и {@link com.hbm.render.model.ModelM65} (1.7.10): координаты кубов переносятся 1:1,
 * корневая часть "mask"/"filter" наследует позу головы игрока (см. GasMaskLayer).
 */
public final class GasMaskModels {

    private GasMaskModels() {
    }

    // ── Полная маска (gas_mask), текстура 64x32 ──────────────────────────────

    public static LayerDefinition createGasMask() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition mask = root.addOrReplaceChild("mask", CubeListBuilder.create(), PartPose.ZERO);

        mask.addOrReplaceChild("shape1",
                CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0F, 0F, 0F, 8, 8, 3),
                PartPose.offset(-4F, -8F + 0.075F / 2, -4F));
        mask.addOrReplaceChild("shape2",
                CubeListBuilder.create().texOffs(22, 0).mirror().addBox(0F, 0F, 0F, 2, 2, 1),
                PartPose.offset(1F - 4, 3F - 8 + 0.075F / 2, -0.5333334F - 4));
        mask.addOrReplaceChild("shape3",
                CubeListBuilder.create().texOffs(22, 0).mirror().addBox(0F, 0F, 0F, 2, 2, 1),
                PartPose.offset(5F - 4, 3F - 8 + 0.075F / 2, -0.5F - 4));
        mask.addOrReplaceChild("shape4",
                CubeListBuilder.create().texOffs(0, 11).mirror().addBox(0F, 0F, 0F, 2, 2, 2),
                PartPose.offsetAndRotation(3F - 4, 5F - 8 + 0.075F / 2, 0F - 4, -0.7853982F, 0F, 0F));
        mask.addOrReplaceChild("shape5",
                CubeListBuilder.create().texOffs(0, 15).mirror().addBox(0F, 2F, -0.5F, 3, 4, 3),
                PartPose.offsetAndRotation(2.5F - 4, 5F - 8 + 0.075F / 2, 0F - 4, -0.7853982F, 0F, 0F));
        mask.addOrReplaceChild("shape6",
                CubeListBuilder.create().texOffs(0, 22).mirror().addBox(0F, 0F, 0F, 8, 1, 5),
                PartPose.offset(0F - 4, 3F - 8 + 0.075F / 2, 3F - 4));

        return LayerDefinition.create(mesh, 64, 32);
    }

    // ── M65 (m65 / olde / mono / маски-тряпки), текстура 32x32 ───────────────

    public static LayerDefinition createM65() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float yOffset = 0.5F;

        PartDefinition mask = root.addOrReplaceChild("mask", CubeListBuilder.create(), PartPose.ZERO);
        mask.addOrReplaceChild("maskHead",
                CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0F, 0F, 0F, 8, 8, 8),
                PartPose.offset(-4F, -8F + yOffset, -4F));
        mask.addOrReplaceChild("nose",
                CubeListBuilder.create().texOffs(0, 16).mirror().addBox(0F, 0F, 0F, 3, 3, 1),
                PartPose.offset(-1.5F, -3.5F + yOffset, -5F));
        mask.addOrReplaceChild("outlet",
                CubeListBuilder.create().texOffs(0, 20).mirror().addBox(0F, -2F, 0F, 2, 2, 1),
                PartPose.offsetAndRotation(-1F, -3.5F + yOffset, -5F, -0.4799655F, 0F, 0F));
        mask.addOrReplaceChild("noseSlope",
                CubeListBuilder.create().texOffs(8, 16).mirror().addBox(0F, 0F, -2F, 3, 2, 2),
                PartPose.offsetAndRotation(-1.5F, -2F + yOffset, -4F, 0.6108652F, 0F, 0F));
        mask.addOrReplaceChild("eye1",
                CubeListBuilder.create().texOffs(0, 23).mirror().addBox(0F, 0F, 0F, 3, 3, 0),
                PartPose.offset(-3.5F, -6F + yOffset, -4.2F));
        mask.addOrReplaceChild("eye2",
                CubeListBuilder.create().texOffs(0, 26).mirror().addBox(0F, 0F, 0F, 3, 3, 0),
                PartPose.offset(0.5F, -6F + yOffset, -4.2F));
        mask.addOrReplaceChild("iForgot",
                CubeListBuilder.create().texOffs(6, 20).mirror().addBox(0F, 0F, 0F, 2, 2, 1),
                PartPose.offset(-1F, -3.2F + yOffset, -6F));

        PartDefinition filter = root.addOrReplaceChild("filter", CubeListBuilder.create(), PartPose.ZERO);
        filter.addOrReplaceChild("filterConnector",
                CubeListBuilder.create().texOffs(6, 23).mirror().addBox(0F, 0F, -3F, 2, 2, 1),
                PartPose.offsetAndRotation(-1F, -2F + yOffset, -4F, 0.6108652F, 0F, 0F));
        filter.addOrReplaceChild("filter1",
                CubeListBuilder.create().texOffs(18, 21).mirror().addBox(0F, -1F, -5F, 3, 4, 2),
                PartPose.offsetAndRotation(-1.5F, -2F + yOffset, -4F, 0.6108652F, 0F, 0F));
        filter.addOrReplaceChild("filter2",
                CubeListBuilder.create().texOffs(18, 16).mirror().addBox(0F, -0.5F, -5F, 4, 3, 2),
                PartPose.offsetAndRotation(-2F, -2F + yOffset, -4F, 0.6108652F, 0F, 0F));

        return LayerDefinition.create(mesh, 32, 32);
    }
}
