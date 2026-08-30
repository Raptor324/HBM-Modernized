package com.hbm_m.client.loader;

import java.util.HashMap;
import java.util.Set;

import com.google.gson.JsonObject;
import com.hbm_m.client.model.MachineBatterySocketBakedModel;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import com.hbm_m.lib.RefStrings;

public class MachineBatterySocketModelLoader extends AbstractObjPartModelLoader<MachineBatterySocketBakedModel> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "battery_socket");


    @Override
    protected Set<String> getPartNames(JsonObject jsonObject) {
        return Set.of("Socket", "Battery");
    }

    @Override
    protected MachineBatterySocketBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts, ItemTransforms transforms, ResourceLocation modelLocation) {
        return new MachineBatterySocketBakedModel(bakedParts, transforms);
    }
}