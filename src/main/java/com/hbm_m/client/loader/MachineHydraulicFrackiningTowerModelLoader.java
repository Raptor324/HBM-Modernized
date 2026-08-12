package com.hbm_m.client.loader;

import java.util.HashMap;
import java.util.Set;

import com.google.gson.JsonObject;
import com.hbm_m.client.model.MachineHydraulicFrackiningTowerBakedModel;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

public class MachineHydraulicFrackiningTowerModelLoader extends AbstractObjPartModelLoader<MachineHydraulicFrackiningTowerBakedModel> {

    // Для статической башни используем одну часть "Base".
    // Если в OBJ есть группы, можно добавить их имена сюда.
    private static final Set<String> PART_NAMES = Set.of("Cube_Cube.001");

    @Override
    protected Set<String> getPartNames(JsonObject jsonObject) {
        return PART_NAMES;
    }

    @Override
    protected MachineHydraulicFrackiningTowerBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts, ItemTransforms transforms, ResourceLocation modelLocation) {
        return new MachineHydraulicFrackiningTowerBakedModel(bakedParts, transforms);
    }
}