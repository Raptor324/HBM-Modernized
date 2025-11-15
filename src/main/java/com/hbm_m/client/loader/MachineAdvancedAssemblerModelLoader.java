package com.hbm_m.client.loader;

import com.google.gson.JsonObject;
import com.hbm_m.client.model.MachineAdvancedAssemblerBakedModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Set;

public class MachineAdvancedAssemblerModelLoader extends AbstractObjPartModelLoader<MachineAdvancedAssemblerBakedModel> {

    private static final Set<String> PART_NAMES = Set.of(
        "Base", "Frame", "Ring", "ArmLower1", "ArmUpper1", "Head1", "Spike1",
        "ArmLower2", "ArmUpper2", "Head2", "Spike2"
    );

    @Override
    protected Set<String> getPartNames(JsonObject jsonObject) {
        return PART_NAMES;
    }

    @Override
    protected MachineAdvancedAssemblerBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts, ItemTransforms transforms, ResourceLocation modelLocation) {
        return new MachineAdvancedAssemblerBakedModel(bakedParts, transforms);
    }
}
