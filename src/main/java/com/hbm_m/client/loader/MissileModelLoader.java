package com.hbm_m.client.loader;

import java.util.HashMap;
import java.util.Set;

import com.google.gson.JsonObject;
import com.hbm_m.client.model.MissileBakedModel;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

/**
 * OBJ loader for missile models (single or multi-part). Textures use {@code models/missile/} sprites
 * in the block atlas ({@code textures/models/missile/}).
 * Части — все корневые группы OBJ (авто), ручные списки не нужны.
 */
public class MissileModelLoader extends AbstractObjPartModelLoader<MissileBakedModel> {

    @Override
    protected Set<String> getPartNames(JsonObject jsonObject) {
        return Set.of(); // АВТО: все корневые группы OBJ
    }

    @Override
    protected MissileBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts,
                                                 ItemTransforms transforms,
                                                 ResourceLocation modelLocation) {
        return new MissileBakedModel(bakedParts, transforms, modelLocation);
    }

    @Override
    protected ResourceLocation mapAtlasForTexture(ResourceLocation texture) {
        if (texture == null) {
            return null;
        }
        if (RefStrings.MODID.equals(texture.getNamespace())
                && texture.getPath().startsWith("models/missile/")) {
            return TextureAtlas.LOCATION_BLOCKS;
        }
        return null;
    }
}