package com.hbm_m.datagen.assets;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.generators.CustomLoaderBuilder;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * Emits {@code hbm_m:missile_loader} item model JSON (OBJ hull + per-variant texture).
 */
public class MissileItemModelBuilder extends CustomLoaderBuilder<ItemModelBuilder> {

    private final ResourceLocation objModel;
    private final String[] parts;
    private final ResourceLocation texture;

    public MissileItemModelBuilder(ItemModelBuilder parent, ExistingFileHelper existingFileHelper,
                                   MissileItemModelDefinitions.Definition definition) {
        super(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "missile_loader"), parent, existingFileHelper);
        this.objModel = definition.hull().getObjModel();
        this.parts = definition.hull().getPartNames().toArray(String[]::new);
        this.texture = definition.texture();
    }

    @Override
    public JsonObject toJson(JsonObject json) {
        super.toJson(json);
        json.addProperty("model", objModel.toString());
        json.addProperty("flip_v", true);

        JsonArray partsArray = new JsonArray();
        for (String part : parts) {
            partsArray.add(part);
        }
        json.add("parts", partsArray);

        JsonObject textures = new JsonObject();
        String tex = texture.toString();
        textures.addProperty("default", tex);
        textures.addProperty("particle", tex);
        json.add("textures", textures);

        JsonObject display = new JsonObject();
        display.add("gui", transform(30, 225, 0, 0, -1.5F, 0, 0.65F));
        display.add("ground", transform(0, 0, 0, 0, 4, 0, 0.5F));
        display.add("fixed", transform(0, 0, 0, 0, 0, 0, 0.78F));
        display.add("thirdperson_righthand", transform(75, 225, 0, 0, 3, 1, 0.38F));
        display.add("thirdperson_lefthand", transform(75, 45, 0, 0, 3, 1, 0.38F));
        display.add("firstperson_righthand", transform(0, 225, 0, 1.13F, 3.2F, 1.13F, 0.45F));
        display.add("firstperson_lefthand", transform(0, 45, 0, 1.13F, 3.2F, 1.13F, 0.45F));
        json.add("display", display);

        return json;
    }

    private static JsonObject transform(float rotX, float rotY, float rotZ,
                                        float tx, float ty, float tz, float scale) {
        JsonObject node = new JsonObject();
        JsonArray rotation = new JsonArray();
        rotation.add(rotX);
        rotation.add(rotY);
        rotation.add(rotZ);
        node.add("rotation", rotation);

        JsonArray translation = new JsonArray();
        translation.add(tx);
        translation.add(ty);
        translation.add(tz);
        node.add("translation", translation);

        JsonArray scaleArr = new JsonArray();
        scaleArr.add(scale);
        scaleArr.add(scale);
        scaleArr.add(scale);
        node.add("scale", scaleArr);
        return node;
    }
}
