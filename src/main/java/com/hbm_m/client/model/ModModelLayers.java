package com.hbm_m.client.model;

import com.hbm_m.main.MainRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public class ModModelLayers {

    /**
     * Generic, shared model layer for power armor "no-op" armor model.
     * Actual power armor geometry is rendered via OBJ layers (see powerarmor package). TODO: Check t51, ajr layers
     */
    public static final ModelLayerLocation POWER_ARMOR = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "power_armor"), "main");

    // Legacy: kept for compatibility with older code paths.
    public static final ModelLayerLocation T51_ARMOR = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "t51_armor"), "main");

}