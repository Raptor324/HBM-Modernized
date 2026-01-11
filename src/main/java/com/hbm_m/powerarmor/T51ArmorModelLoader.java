package com.hbm_m.powerarmor;

import com.google.gson.JsonObject;
import com.hbm_m.client.loader.AbstractObjPartModelLoader;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Set;

public class T51ArmorModelLoader extends AbstractObjPartModelLoader<T51ArmorBakedModel> {

    // Имена должны совпадать с "o <name>" в t51.obj. [file:26]
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
    protected T51ArmorBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts,
                                                  ItemTransforms transforms,
                                                  ResourceLocation modelLocation) {
        return new T51ArmorBakedModel(bakedParts, transforms);
    }

    @Override
    protected boolean flipV() {
        // Для Minecraft/Forge OBJ-бейка ожидается инверсия V (vanilla convention).
        // Это также соответствует текущему t51.obj (иначе UV "съезжают").
        return true;
    }

    // mapAtlasForTexture() не переопределен - используем стандартный BLOCK_ATLAS
    // Кастомный атлас не используется, так как при entity рендеринге Material создается
    // заново с BLOCK_ATLAS в T51PowerArmorLayer.T51Config
}
