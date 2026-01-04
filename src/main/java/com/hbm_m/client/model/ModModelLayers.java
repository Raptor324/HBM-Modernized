package com.hbm_m.client.model;

import com.hbm_m.main.MainRegistry; // Убедись, что это твой главный класс мода с MOD_ID
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;

public class ModModelLayers {

    // Это уникальный ключ для твоей брони T51.
    // "main" означает основной слой модели.
    public static final ModelLayerLocation T51_ARMOR = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, "t51_armor"), "main");

}