package com.hbm_m.powerarmor.layer;

import com.hbm_m.main.MainRegistry;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public class ModModelLayers {

    /**
     * Generic, shared model layer for power armor "no-op" armor model.
     * Actual power armor geometry is rendered via OBJ layers (see powerarmor package).
     */
    public static final ModelLayerLocation POWER_ARMOR = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "power_armor"), "main");
}