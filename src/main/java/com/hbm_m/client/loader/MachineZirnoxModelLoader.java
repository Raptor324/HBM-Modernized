package com.hbm_m.client.loader;

import java.util.HashMap;
import java.util.Set;

import com.google.gson.JsonObject;
import com.hbm_m.client.model.MachineZirnoxBakedModel;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

/**
 * Лоадер OBJ-модели ZIRNOX (порт 1.7.10 {@code ResourceManager.zirnox}).
 * Модель — единственная группа "Plane" ({@code o Plane} в zirnox.obj).
 */
public class MachineZirnoxModelLoader extends AbstractObjPartModelLoader<MachineZirnoxBakedModel> {

    private static final Set<String> PART_NAMES = Set.of("Plane");

    @Override
    protected Set<String> getPartNames(JsonObject jsonObject) {
        return PART_NAMES;
    }

    @Override
    protected MachineZirnoxBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts, ItemTransforms transforms, ResourceLocation modelLocation) {
        return new MachineZirnoxBakedModel(bakedParts, transforms);
    }
}
