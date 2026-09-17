package com.hbm_m.client.loader;

import java.util.LinkedHashMap;
import java.util.Map;

import org.joml.Vector3f;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.hbm_m.client.model.PressBakedModel;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/**
 * Лоадер пресса: две части (Base/Head) из отдельных OBJ + параметры анимации
 * головы ({@code head_transform}). Части описываются картой
 * {@code "base_model"}/{@code "head_model"}; текстуры частей разрешаются
 * контекстом (MTL/«textures» JSON), как и у остальных машин.
 */
public class PressModelLoader extends MachinePartsModelLoader<PressBakedModel> {

    @Override
    public ObjPartGeometry<PressBakedModel> read(JsonObject json, JsonDeserializationContext ctx) {
        Map<String, PartDef> defs = new LinkedHashMap<>();
        defs.put("Base", new PartDef(
                ResourceLocation.tryParse(GsonHelper.getAsString(json, "base_model")), null));
        defs.put("Head", new PartDef(
                ResourceLocation.tryParse(GsonHelper.getAsString(json, "head_model")), null));

        Vector3f headTranslation = new Vector3f(0.0F, 0.0F, 0.0F);
        float headTravel = 0.8F;
        if (json.has("head_transform")) {
            JsonObject headTransform = GsonHelper.getAsJsonObject(json, "head_transform");
            headTranslation = parseTranslation(headTransform);
            if (headTransform.has("travel")) {
                headTravel = headTransform.get("travel").getAsFloat();
            }
        }
        final Vector3f headRestOffset = headTranslation;
        final float travel = headTravel;

        return new MultiObjGeometry<>(defs, GsonHelper.getAsBoolean(json, "flip_v", true),
                (parts, transforms, loc) -> new PressBakedModel(parts, transforms, headRestOffset, travel));
    }

    private static Vector3f parseTranslation(JsonObject headTransform) {
        if (!headTransform.has("translation")) {
            return new Vector3f(0.0F, 0.0F, 0.0F);
        }
        var array = GsonHelper.getAsJsonArray(headTransform, "translation");
        float x = array.size() > 0 ? array.get(0).getAsFloat() : 0.0F;
        float y = array.size() > 1 ? array.get(1).getAsFloat() : 0.0F;
        float z = array.size() > 2 ? array.get(2).getAsFloat() : 0.0F;
        return new Vector3f(x, y, z);
    }
}
