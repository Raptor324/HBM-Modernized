package com.hbm_m.client.loader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm_m.client.model.CargoElevatorBakedModel;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Загрузчик модели для CargoElevator: один OBJ (elevator.obj)
 * с частями Base, Platform, Piston, Guides.
 * JSON: {@code "loader": "hbm_m:cargo_elevator"}.
 */
public class CargoElevatorModelLoader extends AbstractObjPartModelLoader<CargoElevatorBakedModel> {

    private static final Set<String> DEFAULT_PART_NAMES = Set.of(
        "Base", "Platform", "Piston", "Guides"
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
    protected CargoElevatorBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts,
                                                        ItemTransforms transforms,
                                                        ResourceLocation modelLocation) {
        return new CargoElevatorBakedModel(bakedParts, transforms);
    }
}