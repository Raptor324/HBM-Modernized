package com.hbm_m.client.loader;

import java.util.HashMap;
import java.util.Set;

import com.google.gson.JsonObject;
import com.hbm_m.client.model.ChemicalPlantBakedModel;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

public class ChemicalPlantModelLoader extends AbstractObjPartModelLoader<ChemicalPlantBakedModel> {

    private static final Set<String> PART_NAMES = Set.of(
        "Base", "Frame", "Slider", "Spinner", "Fluid"
    );

    @Override
    protected Set<String> getPartNames(JsonObject jsonObject) {
        return PART_NAMES;
    }

    @Override
    protected ChemicalPlantBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts, ItemTransforms transforms, ResourceLocation modelLocation) {
        return new ChemicalPlantBakedModel(bakedParts, transforms);
    }
}
