package com.hbm_m.client.render.armor;

import com.hbm_m.main.MainRegistry;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/** Die Quader-Modelle {@code ModelGoggles} und {@code ModelJetPack} (1.7.10) als Layer-Definitionen. */
public final class ArmorAccessoryModels {

    private ArmorAccessoryModels() {}

    public static final ModelLayerLocation GOGGLES = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "goggles"), "main");
    public static final ModelLayerLocation JETPACK = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "jetpack"), "main");

    private static void box(PartDefinition parent, String name, int u, int v, float w, float h, float d, float px, float py, float pz) {
        parent.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).mirror().addBox(0F, 0F, 0F, w, h, d), PartPose.offset(px, py, pz));
    }

    /** ModelGoggles: Kinder relativ zum Kopf-Gelenkpunkt. */
    public static LayerDefinition createGoggles() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition google = mesh.getRoot().addOrReplaceChild("google", CubeListBuilder.create(), PartPose.ZERO);
        box(google, "shape1", 0, 0, 9, 3, 1, -4.5F, -3F - 2, -4.5F);
        box(google, "shape2", 0, 4, 9, 2, 5, -4.5F, -3F - 2, -3.5F);
        box(google, "shape5", 26, 0, 2, 2, 1, 1F, -2.5F - 2, -5F);
        box(google, "shape6", 20, 0, 2, 2, 1, -3F, -2.5F - 2, -5F);
        box(google, "shape7", 0, 11, 9, 1, 4, -4.5F, -3F - 2, 0.5F);
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** ModelJetPack: Wurzel bei (0,0,-2), die Kinder per convertToChild relativ dazu; beim Rendern liegt die Wurzel im Rumpf-Gelenkpunkt. */
    public static LayerDefinition createJetpack() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition jet = mesh.getRoot().addOrReplaceChild("jetpack", CubeListBuilder.create(), PartPose.ZERO);
        float z = 2F; // -(-2)
        box(jet, "pack", 12, 10, 4, 6, 1, -2F, 3F, 0F + z);
        box(jet, "tank1", 0, 0, 3, 8, 3, 0.5F, 2F, 0.5F + z);
        box(jet, "tank2", 0, 11, 3, 8, 3, -3.5F, 2F, 0.5F + z);
        box(jet, "tip1", 0, 22, 2, 1, 2, 1F, 1F, 1F + z);
        box(jet, "tip2", 0, 25, 2, 1, 2, -3F, 1F, 1F + z);
        box(jet, "duct1", 8, 22, 2, 1, 2, 1F, 9.5F, 1F + z);
        box(jet, "duct2", 8, 25, 2, 1, 2, -3F, 9.5F, 1F + z);
        box(jet, "thruster1", 12, 0, 3, 2, 3, 0.5F, 10.5F, 0.5F + z);
        box(jet, "thruster2", 12, 5, 3, 2, 3, -3.5F, 10.5F, 0.5F + z);
        return LayerDefinition.create(mesh, 32, 32);
    }
}
