package com.hbm_m.client.loader;

import java.util.HashMap;
import java.util.Set;

import com.google.gson.JsonObject;
import com.hbm_m.client.model.MachineChemicalPlantBakedModel;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

public class MachineChemicalPlantModelLoader extends AbstractObjPartModelLoader<MachineChemicalPlantBakedModel> {

    private static final Set<String> PART_NAMES = Set.of(
        "Base", "Frame", "Slider", "Spinner", "Fluid"
    );

    protected Set<String> getPartNames(JsonObject jsonObject) {
        return PART_NAMES;
    }

    protected MachineChemicalPlantBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts, ItemTransforms transforms, ResourceLocation modelLocation) {
        return new MachineChemicalPlantBakedModel(bakedParts, transforms);
    }
}
