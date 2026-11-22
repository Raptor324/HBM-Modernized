package com.hbm_m.client.loader;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.hbm_m.client.render.weapon.WeaponMultipartBakedModel;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Nucleus OBJ loader for handheld weapons, firearms, and melee models.
 * Registered under {@code "weapon_obj_parts"} (and alias {@code "weapon_parts_loader"}).
 */
@OnlyIn(Dist.CLIENT)
public class WeaponPartsModelLoader extends AbstractObjPartModelLoader<WeaponMultipartBakedModel> {

    @Override
    public ObjPartGeometry<WeaponMultipartBakedModel> read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
        if (!jsonObject.has("model")) {
            throw new JsonParseException("Weapon model definition missing required 'model' property!");
        }

        String modelStr = GsonHelper.getAsString(jsonObject, "model");
        ResourceLocation model = modelStr.contains(":")
                ? ResourceLocation.tryParse(modelStr)
                : ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, modelStr);

        if (model == null) {
            throw new JsonParseException("Invalid 'model' ResourceLocation in weapon model definition: " + modelStr);
        }

        Set<String> partNames = getPartNames(jsonObject);
        boolean flipV = GsonHelper.getAsBoolean(jsonObject, "flip_v", true);

        MainRegistry.LOGGER.debug("WeaponPartsModelLoader: registered geometry spec for model='{}', flipV={}, parts={}",
                model, flipV, partNames.isEmpty() ? "AUTO" : partNames);

        return new ObjPartGeometry<>(model, partNames, flipV, this);
    }

    @Override
    protected Set<String> getPartNames(JsonObject jsonObject) {
        if (jsonObject.has("parts") && jsonObject.get("parts").isJsonArray()) {
            Set<String> parts = new LinkedHashSet<>();
            for (JsonElement elem : jsonObject.getAsJsonArray("parts")) {
                if (elem.isJsonPrimitive()) {
                    parts.add(elem.getAsString());
                }
            }
            return parts;
        }
        // Auto-mode: return empty set so AbstractObjPartModelLoader discovers all root components of the OBJ model
        return Set.of();
    }

    @Override
    protected WeaponMultipartBakedModel createBakedModel(HashMap<String, BakedModel> bakedParts,
                                                         ItemTransforms transforms,
                                                         ResourceLocation modelLocation) {
        return new WeaponMultipartBakedModel(bakedParts, transforms, modelLocation);
    }

    @Override
    protected ResourceLocation mapAtlasForTexture(ResourceLocation texture) {
        return null;
    }
}
