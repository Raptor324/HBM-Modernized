//? if forge {
package com.hbm_m.client.loader;

import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;
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
 */
public class MissileModelLoader extends AbstractObjPartModelLoader<MissileBakedModel> {

    @Override
    protected Set<String> getPartNames(JsonObject jsonObject) {
        if (!jsonObject.has("parts")) {
            return Set.of();
        }
        return jsonObject.getAsJsonArray("parts").asList().stream()
                .map(JsonElement::getAsString)
                .collect(Collectors.toSet());
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
//?}

//? if fabric {
/*package com.hbm_m.client.loader;

public class MissileModelLoader extends AbstractObjPartModelLoader<Object> {
}
*///?}
