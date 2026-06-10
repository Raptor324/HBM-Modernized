package com.hbm_m.client.loader;

import com.google.gson.JsonObject;
import com.hbm_m.client.model.MachineFluidTankBakedModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Set;

public class MachineFluidTankModelLoader extends AbstractObjPartModelLoader<MachineFluidTankBakedModel> {

    //? if fabric && < 1.21.1 {
    /*public static final ResourceLocation ID = new ResourceLocation("hbm_m", "tank");
    *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("hbm_m", "tank");
    //?}


    protected Set<String> getPartNames(JsonObject jsonObject) {
        return Set.of("Frame", "Tank");
    }

    protected MachineFluidTankBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts, ItemTransforms transforms, ResourceLocation modelLocation) {
        return new MachineFluidTankBakedModel(bakedParts, transforms);
    }
}