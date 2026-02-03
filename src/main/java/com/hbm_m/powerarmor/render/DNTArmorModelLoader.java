package com.hbm_m.powerarmor.render;

import java.util.HashMap;
import java.util.Set;

import com.google.gson.JsonObject;
import com.hbm_m.client.loader.AbstractObjPartModelLoader;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

/**
 * Model loader for DNT power armor OBJ parts.
 * Part names must match the "o &lt;name&gt;" groups in dnt.obj.
 */
public class DNTArmorModelLoader extends AbstractObjPartModelLoader<DNTArmorBakedModel> {

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
    protected DNTArmorBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts,
                                                  ItemTransforms transforms,
                                                  ResourceLocation modelLocation) {
        return new DNTArmorBakedModel(bakedParts, transforms);
    }

    @Override
    protected boolean flipV() {
        // Same convention as T51/AJR/Bismuth OBJ models.
        return true;
    }
}

