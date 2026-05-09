package com.hbm_m.client.loader;

import java.util.HashMap;
import java.util.Set;

import com.google.gson.JsonObject;
import com.hbm_m.client.model.MachineBatterySocketBakedModel;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

public class MachineBatterySocketModelLoader extends AbstractObjPartModelLoader<MachineBatterySocketBakedModel> {

    //? if fabric && < 1.21.1 {
    public static final ResourceLocation ID = new ResourceLocation("hbm_m", "battery_socket");
    //?} else {
        /*public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("hbm_m", "battery_socket");
    *///?}


    protected Set<String> getPartNames(JsonObject jsonObject) {
        return Set.of("Socket", "Battery");
    }

    protected MachineBatterySocketBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts, ItemTransforms transforms, ResourceLocation modelLocation) {
        return new MachineBatterySocketBakedModel(bakedParts, transforms);
    }
}
