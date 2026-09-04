package com.hbm_m.client.loader;

import java.util.HashMap;
import java.util.Set;

import com.google.gson.JsonObject;
import com.hbm_m.client.model.MachineChemicalFactoryBakedModel;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

/**
 * Лоадер OBJ-модели Chemical Factory (части Base/Frame/Fan1/Fan2 оригинального
 * {@code chemical_factory.obj}; мёртвая в 1.7.10 часть Plane.001 не выпекается).
 */
public class MachineChemicalFactoryModelLoader extends AbstractObjPartModelLoader<MachineChemicalFactoryBakedModel> {

    private static final Set<String> PART_NAMES = Set.of(
        "Base", "Frame", "Fan1", "Fan2"
    );

    @Override
    protected Set<String> getPartNames(JsonObject jsonObject) {
        return PART_NAMES;
    }

    @Override
    protected MachineChemicalFactoryBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts, ItemTransforms transforms, ResourceLocation modelLocation) {
        return new MachineChemicalFactoryBakedModel(bakedParts, transforms);
    }
}
