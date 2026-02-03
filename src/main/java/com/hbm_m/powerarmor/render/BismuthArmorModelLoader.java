package com.hbm_m.powerarmor.render;

import com.google.gson.JsonObject;
import com.hbm_m.client.loader.AbstractObjPartModelLoader;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Set;

/**
 * Model loader for Bismuth armor OBJ parts.
 * Names MUST match "o <name>" in {@code bismuth.obj}.
 */
public class BismuthArmorModelLoader extends AbstractObjPartModelLoader<BismuthArmorBakedModel> {

    private static final Set<String> PART_NAMES = Set.of(
            "Helmet",
            "Chest",
            "LeftArm",
            "RightArm",
            "LeftLeg",
            "RightLeg",
            "LeftBoot",
            "RightBoot"
    );

    @Override
    protected Set<String> getPartNames(JsonObject jsonObject) {
        return PART_NAMES;
    }

    @Override
    protected BismuthArmorBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts,
                                                      ItemTransforms transforms,
                                                      ResourceLocation modelLocation) {
        return new BismuthArmorBakedModel(bakedParts, transforms);
    }

    @Override
    protected boolean flipV() {
        // Same convention as T51/AJR.
        return true;
    }
}

