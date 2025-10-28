package com.hbm_m.client.loader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm_m.client.model.DoorBakedModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

public class DoorModelLoader extends AbstractObjPartModelLoader<DoorBakedModel> {

    private static final Set<String> DEFAULT_PART_NAMES = Set.of(
        "frame", "doorLeft", "doorRight"
    );

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
    protected DoorBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts, ItemTransforms transforms) {
        return new DoorBakedModel(bakedParts, transforms);
    }
}
