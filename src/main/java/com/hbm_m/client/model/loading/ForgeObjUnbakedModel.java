//? if fabric {
package com.hbm_m.client.model.loading;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hbm_m.main.MainRegistry;
import com.mojang.math.Transformation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;

@Environment(EnvType.CLIENT)
public final class ForgeObjUnbakedModel implements ForgeLikeUnbakedModel {

    private final ResourceLocation id;
    private final ResourceLocation objModel;
    private final boolean flipV;
    private final boolean shadeQuads;
    private final boolean automaticCulling;
    private final Map<String, ResourceLocation> textures;
    private final Map<String, Boolean> visibility;
    private final Transformation rootTransform;
    private final ObjModelData objData;
    private final ItemTransforms itemTransforms;

    private ForgeObjUnbakedModel(ResourceLocation id, ResourceLocation objModel, boolean flipV, boolean shadeQuads, boolean automaticCulling, Map<String, ResourceLocation> textures, Map<String, Boolean> visibility, Transformation rootTransform, ObjModelData objData, ItemTransforms itemTransforms) {
        this.id = id; this.objModel = objModel; this.flipV = flipV; this.shadeQuads = shadeQuads; this.automaticCulling = automaticCulling; this.textures = textures; this.visibility = visibility; this.rootTransform = rootTransform; this.objData = objData; this.itemTransforms = itemTransforms;
    }

    public static ForgeObjUnbakedModel fromJson(ResourceLocation id, JsonObject json, ResourceManager rm, Gson gson) {
        String modelStr = GsonHelper.getAsString(json, "model");
        ResourceLocation objLoc = ResourceLocation.tryParse(modelStr);
        if (objLoc == null) throw new IllegalArgumentException("Invalid OBJ model location: " + modelStr);
        boolean automaticCulling = GsonHelper.getAsBoolean(json, "automatic_culling", true);
        boolean shadeQuads = GsonHelper.getAsBoolean(json, "shade_quads", true);
        boolean flipV = GsonHelper.getAsBoolean(json, "flip_v", false);
        Map<String, ResourceLocation> textures = parseTextures(json);
        Map<String, Boolean> visibility = parseVisibility(json);
        Transformation rootTransform = JsonModelTransforms.parseRootTransform(json.getAsJsonObject("transform"));
        ItemTransforms itemTransforms = JsonModelTransforms.parseItemTransforms(json.get("display"), gson);
        ObjModelData data = ObjModelData.load(rm, objLoc);
        return new ForgeObjUnbakedModel(id, objLoc, flipV, shadeQuads, automaticCulling, textures, visibility, rootTransform, data, itemTransforms);
    }

    @Override public Map<String, ResourceLocation> textures() { return textures; }
    @Override public Collection<ResourceLocation> getDependencies() { return Collections.emptyList(); }
    @Override public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter) {}

    @Override
    public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ResourceLocation modelLocation) {
        TextureAtlasSprite particle = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, textures.getOrDefault("particle", MissingTextureAtlasSprite.getLocation())));
        if (particle == null) particle = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, MissingTextureAtlasSprite.getLocation()));

        ObjBakedModel baked = new ObjBakedModel(bakeQuads(spriteGetter, modelState, modelLocation), particle, shadeQuads, automaticCulling, itemTransforms, ItemOverrides.EMPTY);
        MainRegistry.LOGGER.debug("Baked forge-like OBJ model {} from {}", id, objModel);
        return ModelDebugDumper.wrapIfEnabled(modelLocation, baked);
    }

    private Map<Direction, List<net.minecraft.client.renderer.block.model.BakedQuad>> bakeQuads(Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ResourceLocation modelLocation) {
        Map<Direction, List<net.minecraft.client.renderer.block.model.BakedQuad>> out = new HashMap<>();
        for (Direction d : Direction.values()) out.put(d, new ArrayList<>());
        out.put(null, new ArrayList<>());

        Transformation pivotedRot = ModelStateTransforms.resolveAndPivot(modelState);
        org.joml.Matrix4f combinedM = new org.joml.Matrix4f(pivotedRot.getMatrix());
        if (rootTransform != null && !rootTransform.equals(Transformation.identity())) {
            combinedM.mul(new org.joml.Matrix4f(rootTransform.getMatrix()));
        }
        Transformation combined = new Transformation(combinedM);

        Set<String> hidden = new HashSet<>();
        for (var e : visibility.entrySet()) if (Boolean.FALSE.equals(e.getValue())) hidden.add(e.getKey());

        ObjQuadBaker.ObjQuadBakerState.MODEL = objData;
        try {
            for (ObjModelData.Face f : objData.faces()) {
                String objName = f.objectName();
                if (objName != null && hidden.contains(objName)) continue;
                if (objName != null && visibility.containsKey(objName) && Boolean.FALSE.equals(visibility.get(objName))) continue;
                ResourceLocation tex = resolveTextureForFace(f);
                TextureAtlasSprite sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, tex));
                if (sprite == null) sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, MissingTextureAtlasSprite.getLocation()));

                List<net.minecraft.client.renderer.block.model.BakedQuad> quads = ObjQuadBaker.bakeFaceToQuads(f, sprite, flipV, combined, shadeQuads);
                for (net.minecraft.client.renderer.block.model.BakedQuad q : quads) ObjQuadBaker.addQuadWithCulling(q, out, automaticCulling);
            }
        } finally {
            ObjQuadBaker.ObjQuadBakerState.MODEL = null;
        }
        return out;
    }

    private ResourceLocation resolveTextureForFace(ObjModelData.Face face) {
        String mat = face.materialName();
        if (mat != null) {
            String mtlTex = objData.materialTexture(mat);
            if (mtlTex != null) {
                if (mtlTex.startsWith("#")) {
                    ResourceLocation rl = textures.get(mtlTex.substring(1));
                    if (rl != null) return rl;
                } else {
                    ResourceLocation parsed = ResourceLocation.tryParse(mtlTex);
                    if (parsed != null) return parsed;
                }
            }
        }
        return textures.getOrDefault("default", textures.getOrDefault("particle", MissingTextureAtlasSprite.getLocation()));
    }

    private static Map<String, ResourceLocation> parseTextures(JsonObject json) {
        if (!json.has("textures")) return Map.of();
        JsonObject tex = json.getAsJsonObject("textures");
        Map<String, ResourceLocation> out = new HashMap<>();
        for (var e : tex.entrySet()) {
            if (!e.getValue().isJsonPrimitive()) continue;
            ResourceLocation rl = ResourceLocation.tryParse(e.getValue().getAsString());
            if (rl != null) out.put(e.getKey(), rl);
        }
        return out;
    }

    private static Map<String, Boolean> parseVisibility(JsonObject json) {
        if (!json.has("visibility")) return Map.of();
        JsonObject vis = json.getAsJsonObject("visibility");
        Map<String, Boolean> out = new HashMap<>();
        for (var e : vis.entrySet()) {
            if (!e.getValue().isJsonPrimitive()) continue;
            out.put(e.getKey(), e.getValue().getAsBoolean());
        }
        return out;
    }
}
//?}

