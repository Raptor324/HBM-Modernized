package com.hbm_m.client.loader;

import com.google.gson.JsonObject;
import com.hbm_m.client.model.MachineFluidTankBakedModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Set;

public class MachineFluidTankModelLoader extends AbstractObjPartModelLoader<MachineFluidTankBakedModel> {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("hbm_m", "tank");

    @Override
    protected Set<String> getPartNames(JsonObject jsonObject) {
        return Set.of("Frame", "Tank");
    }

    @Override
    protected MachineFluidTankBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts, ItemTransforms transforms, ResourceLocation modelLocation) {
        return new MachineFluidTankBakedModel(bakedParts, transforms);
    }
}