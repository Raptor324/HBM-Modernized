package com.hbm_m.client.loader;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm_m.client.model.MachineRadarBakedModel;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

public class MachineRadarModelLoader extends AbstractObjPartModelLoader<MachineRadarBakedModel> {

    private static final Set<String> DEFAULT_PART_NAMES = Set.of("Base", "Dish");

    @Override
    protected Set<String> getPartNames(JsonObject jsonObject) {
        if (jsonObject.has("parts")) {
            return jsonObject.getAsJsonArray("parts")
                    .asList()
                    .stream()
                    .map(JsonElement::getAsString)
                    .collect(Collectors.toSet());
        }
        return DEFAULT_PART_NAMES;
    }

    @Override
    protected MachineRadarBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts,
                                                      ItemTransforms transforms,
                                                      ResourceLocation modelLocation) {
        return new MachineRadarBakedModel(bakedParts, transforms);
    }
}
