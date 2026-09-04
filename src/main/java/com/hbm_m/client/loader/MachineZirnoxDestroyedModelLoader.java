package com.hbm_m.client.loader;

import java.util.HashMap;
import java.util.Set;

import com.google.gson.JsonObject;
import com.hbm_m.client.model.MachineZirnoxDestroyedBakedModel;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

/**
 * Лоадер OBJ-модели разрушенного ZIRNOX (порт 1.7.10 {@code ResourceManager.zirnox_destroyed}).
 * Модель — единственная группа "Plane" ({@code o Plane} в zirnox_destroyed.obj).
 */
public class MachineZirnoxDestroyedModelLoader extends AbstractObjPartModelLoader<MachineZirnoxDestroyedBakedModel> {

    private static final Set<String> PART_NAMES = Set.of("Plane");

    @Override
    protected Set<String> getPartNames(JsonObject jsonObject) {
        return PART_NAMES;
    }

    @Override
    protected MachineZirnoxDestroyedBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts, ItemTransforms transforms, ResourceLocation modelLocation) {
        return new MachineZirnoxDestroyedBakedModel(bakedParts, transforms);
    }
}
