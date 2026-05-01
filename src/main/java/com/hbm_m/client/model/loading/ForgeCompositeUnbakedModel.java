//? if fabric {
package com.hbm_m.client.model.loading;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.joml.Matrix4f;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm_m.main.MainRegistry;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;

@Environment(EnvType.CLIENT)
public final class ForgeCompositeUnbakedModel implements ForgeLikeUnbakedModel {

    private final ResourceLocation id;
    private final Map<String, ForgeLikeUnbakedModel> children;
    private final Map<String, Boolean> visibility;
    private final Matrix4f transform;
    private final Map<String, ResourceLocation> textures;
    private final ItemTransforms itemTransforms;

    private ForgeCompositeUnbakedModel(ResourceLocation id,
                                       Map<String, ForgeLikeUnbakedModel> children,
                                       Map<String, Boolean> visibility,
                                       Matrix4f transform,
                                       Map<String, ResourceLocation> textures,
                                       ItemTransforms itemTransforms) {
        this.id = id;
        this.children = children;
        this.visibility = visibility;
        this.transform = transform;
        this.textures = textures;
        this.itemTransforms = itemTransforms;
    }

    public static ForgeCompositeUnbakedModel fromJson(ResourceLocation id, JsonObject json, ResourceManager rm, Gson gson) {
        Map<String, ResourceLocation> textures = new HashMap<>();
        if (json.has("textures")) {
            JsonObject t = json.getAsJsonObject("textures");
            for (var e : t.entrySet()) {
                if (!e.getValue().isJsonPrimitive()) continue;
                ResourceLocation rl = ResourceLocation.tryParse(e.getValue().getAsString());
                if (rl != null) textures.put(e.getKey(), rl);
            }
        }

        Map<String, ForgeLikeUnbakedModel> children = new HashMap<>();
        JsonObject kids = GsonHelper.getAsJsonObject(json, "children", new JsonObject());
        for (var e : kids.entrySet()) {
            String name = e.getKey();
            JsonElement v = e.getValue();
            if (!v.isJsonObject()) continue;
            JsonObject childJson = v.getAsJsonObject();
            String loader = childJson.has("loader") ? childJson.get("loader").getAsString() : null;
            if (loader == null) continue;
            ForgeLikeUnbakedModel child = ForgeLikeUnbakedModel.parse(new ResourceLocation(id.getNamespace(), id.getPath() + "/" + name), loader, childJson, rm, gson);
            if (child != null) children.put(name, child);
        }

        Map<String, Boolean> visibility = new HashMap<>();
        if (json.has("visibility")) {
            JsonObject vis = json.getAsJsonObject("visibility");
            for (var e : vis.entrySet()) visibility.put(e.getKey(), e.getValue().getAsBoolean());
        }

        Matrix4f transform = JsonModelTransforms.parseRootTransformMatrix(json.getAsJsonObject("transform"));
        ItemTransforms itemTransforms = JsonModelTransforms.parseItemTransforms(json.get("display"), gson);

        return new ForgeCompositeUnbakedModel(id, children, visibility, transform, textures, itemTransforms);
    }

    @Override
    public Map<String, ResourceLocation> textures() { return textures; }

    @Override
    public Collection<ResourceLocation> getDependencies() { return Collections.emptyList(); }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter) {}

    @Override
    public BakedModel bake(ModelBaker baker,
                           Function<Material, TextureAtlasSprite> spriteGetter,
                           ModelState modelState,
                           ResourceLocation modelLocation) {
        TextureAtlasSprite particle = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, textures.getOrDefault("particle", MissingTextureAtlasSprite.getLocation())));
        if (particle == null) particle = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, MissingTextureAtlasSprite.getLocation()));

        // Forge CompositeModel: composeRootTransformIntoModelState(modelState, rootTransform)
        // which does: rootTransform = rootTransform.applyOrigin(-0.5, -0.5, -0.5)
        //             newModelState.rotation = modelState.getRotation().compose(rootTransform)
        // The children then get baked with this combined modelState, which will in turn
        // call blockCenterToCorner() inside their own makeQuad/bakeQuads.
        //
        // Since our children (OBJ models) call composeForObjBaking() which already does
        // blockCenterToCorner(), we need to pass a modelState that has the combined rotation
        // WITHOUT the blockCenterToCorner applied yet.
        //
        // Forge's composeRootTransformIntoModelState:
        //   rootTransform' = rootTransform.applyOrigin(new Vector3f(-0.5, -0.5, -0.5))
        //   newRot = modelState.getRotation().compose(rootTransform')
        com.mojang.math.Transformation modelRot = ModelStateTransforms.getModelStateRotation(modelState);
        com.mojang.math.Transformation compositeRootTransform = transform != null
                ? new com.mojang.math.Transformation(transform)
                : com.mojang.math.Transformation.identity();

        com.mojang.math.Transformation combinedRot;
        if (ModelStateTransforms.isIdentity(compositeRootTransform)) {
            combinedRot = modelRot;
        } else {
            // Forge: rootTransform.applyOrigin(-0.5, -0.5, -0.5)
            com.mojang.math.Transformation adjustedRoot = ModelStateTransforms.applyOrigin(
                    compositeRootTransform, new org.joml.Vector3f(-0.5f, -0.5f, -0.5f));
            if (ModelStateTransforms.isIdentity(modelRot)) {
                combinedRot = adjustedRoot;
            } else {
                // modelState.getRotation().compose(adjustedRoot)
                Matrix4f m = new Matrix4f(modelRot.getMatrix());
                m.mul(adjustedRoot.getMatrix());
                combinedRot = new com.mojang.math.Transformation(m);
            }
        }

        ModelState childState = new ModelStateTransforms.PivotedModelState(combinedRot,
                modelState != null && modelState.isUvLocked());

        List<BakedModel> bakedKids = new ArrayList<>();
        for (var e : children.entrySet()) {
            String name = e.getKey();
            if (Boolean.FALSE.equals(visibility.get(name))) continue;
            BakedModel kid = e.getValue().bake(baker, spriteGetter, childState, modelLocation);
            if (kid != null) bakedKids.add(kid);
        }

        MainRegistry.LOGGER.debug("Baked forge-like composite model {} (children={})", id, bakedKids.size());
        return new CompositeBakedModel(bakedKids, particle, itemTransforms, ItemOverrides.EMPTY, null);
    }
}
//?}

