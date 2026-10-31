package com.hbm_m.client.loader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.hbm_m.client.model.nmesh.NMeshData;
import com.hbm_m.client.model.nmesh.NMeshLoader;
import com.hbm_m.client.model.nmesh.NMeshPartBakedModel;
import com.hbm_m.main.MainRegistry;
import com.mojang.math.Transformation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

/**
 * Whole-mesh {@code .nmesh}-backed replacement for the vanilla Forge/NeoForge {@code forge:obj} loader
 * (Milestone M3). Raw {@code .obj}/{@code .mtl} assets are excluded from the packaged jar, so the stock
 * ObjLoader cannot serve the ~800 static block/item models; this loader bakes ALL sub-meshes of the
 * compiled {@link NMeshData} binary (via {@link NMeshLoader#load}, with transparent {@code .obj → .nmesh}
 * remapping and a resourcepack {@code .obj} fallback) into ONE {@link NMeshPartBakedModel}.
 *
 * <p>Model JSON contract (superset of Forge {@code ObjModel.Loader}):
 * <ul>
 *   <li>{@code "model"} — resource location of the {@code .obj} (remapped to {@code .nmesh}).</li>
 *   <li>{@code "flip_v"} — flip V texture coordinate (default {@code true}; every migrated JSON sets it).</li>
 *   <li>{@code "shade_quads"} — BakedQuad shade flag (default {@code true}).</li>
 *   <li>{@code "automatic_culling"} — when true (default), quads are bucketed per dominant Direction so the
 *       chunk renderer can cull them against opaque neighbours; when false all quads render unculled.</li>
 *   <li>{@code "ao"} — optional explicit ambient-occlusion override; defaults to the model's
 *       {@code ambientocclusion} value ({@code context.useAmbientOcclusion()}).</li>
 *   <li>{@code "emissive_ambient"} — Forge ObjModel setting (default {@code true}); when on, each
 *       sub-mesh's MTL Ka drives a baked fake lightmap (full-bright effect) and quad shading is
 *       disabled; MTL Kd tints every vertex colour.</li>
 *   <li>{@code "render_type"} — handled by Forge ({@code context.getRenderTypeHint()}); resolved here into
 *       chunk/item render types on the baked model so composite children and standalone models both layer correctly.</li>
 *   <li>{@code "visibility"} — handled by Forge; sub-meshes whose root object/group is hidden are skipped.</li>
 *   <li>{@code "texture"} — optional single-texture override bound to every sub-mesh (same key as
 *       {@link BatteryPackModelLoader}); used by the pipe families whose compiled {@code map_Kd} points at
 *       {@code #base} while the variant needs {@code #overlay}.</li>
 *   <li>{@code "tint_index"} — optional BakedQuad tint index (default {@code -1}); replaces the offline-ignored
 *       {@code forge_TintIndex} MTL directive for the tinted pipe overlays.</li>
 *   <li>{@code "mtl_override"} — accepted but inert: raw {@code .mtl} libraries are excluded from the jar, so
 *       the override cannot be parsed at runtime. Its binding/tint information is carried by the
 *       {@code "texture"} / {@code "tint_index"} keys instead (every migrated consumer has been updated).</li>
 * </ul>
 *
 * <p>Sprite binding follows Forge's {@code UnbakedGeometryHelper.resolveDirtyMaterial} semantics on the
 * {@code map_Kd} value persisted in the binary: {@code "#ref"} → JSON texture key, direct path →
 * block-atlas material, else fall back to the MTL material name, then {@code "default"}/{@code "texture"}.
 */
@OnlyIn(Dist.CLIENT)
public class NMeshObjModelLoader implements IGeometryLoader<NMeshObjModelLoader.Geometry> {

    @Override
    public Geometry read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) {
        if (!jsonObject.has("model")) {
            throw new JsonParseException("hbm_m:nmesh_obj loader requires a 'model' key pointing to a valid .obj/.nmesh model.");
        }
        ResourceLocation model = ResourceLocation.tryParse(GsonHelper.getAsString(jsonObject, "model"));
        if (model == null) {
            throw new JsonParseException("hbm_m:nmesh_obj loader: invalid 'model' resource location.");
        }
        boolean flipV = GsonHelper.getAsBoolean(jsonObject, "flip_v", true);
        boolean shadeQuads = GsonHelper.getAsBoolean(jsonObject, "shade_quads", true);
        boolean automaticCulling = GsonHelper.getAsBoolean(jsonObject, "automatic_culling", true);
        boolean emissiveAmbient = GsonHelper.getAsBoolean(jsonObject, "emissive_ambient", true);
        Boolean aoOverride = jsonObject.has("ao") ? GsonHelper.getAsBoolean(jsonObject, "ao") : null;
        int tintIndex = GsonHelper.getAsInt(jsonObject, "tint_index", -1);
        ResourceLocation textureOverride = null;
        if (jsonObject.has("texture") && !jsonObject.get("texture").isJsonNull()) {
            textureOverride = ResourceLocation.tryParse(GsonHelper.getAsString(jsonObject, "texture"));
            if (textureOverride == null) {
                throw new JsonParseException("hbm_m:nmesh_obj loader: invalid 'texture' resource location.");
            }
        }
        return new Geometry(model, flipV, shadeQuads, automaticCulling, emissiveAmbient, aoOverride, tintIndex, textureOverride);
    }

    public static final class Geometry implements IUnbakedGeometry<Geometry> {
        private final ResourceLocation modelLocation;
        private final boolean flipV;
        private final boolean shadeQuads;
        private final boolean automaticCulling;
        private final boolean emissiveAmbient;
        @Nullable
        private final Boolean aoOverride;
        private final int tintIndex;
        @Nullable
        private final ResourceLocation textureOverride;

        public Geometry(ResourceLocation modelLocation, boolean flipV, boolean shadeQuads, boolean automaticCulling,
                        boolean emissiveAmbient, @Nullable Boolean aoOverride, int tintIndex, @Nullable ResourceLocation textureOverride) {
            this.modelLocation = modelLocation;
            this.flipV = flipV;
            this.shadeQuads = shadeQuads;
            this.automaticCulling = automaticCulling;
            this.emissiveAmbient = emissiveAmbient;
            this.aoOverride = aoOverride;
            this.tintIndex = tintIndex;
            this.textureOverride = textureOverride;
        }

        //? if < 1.21.1 {
        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelName) {
            return doBake(context, spriteGetter, modelState);
        }
        //?} else {
        /*@Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
            return doBake(context, spriteGetter, modelState);
        }
        *///?}

        private BakedModel doBake(IGeometryBakingContext context, Function<Material, TextureAtlasSprite> spriteGetter,
                                  ModelState modelState) {
            NMeshData nmesh = NMeshLoader.load(modelLocation);
            if (nmesh == null) {
                throw new RuntimeException("hbm_m:nmesh_obj: could not load model " + modelLocation
                        + " (no compiled .nmesh in the jar and no fallback .obj resource)");
            }

            Material overrideMaterial = textureOverride != null
                    ? new Material(TextureAtlas.LOCATION_BLOCKS, textureOverride)
                    : null;

            List<NMeshPartBakedModel.BoundSubMesh> bound = new ArrayList<>(nmesh.subMeshes().size());
            for (NMeshData.SubMesh sm : nmesh.subMeshes()) {
                String root = !sm.objectName.isEmpty() ? sm.objectName
                        : (!sm.groupName.isEmpty() ? sm.groupName : "default");
                if (!context.isComponentVisible(root, true)) {
                    continue;
                }
                Material mat = overrideMaterial != null ? overrideMaterial : resolveSprite(context, sm);
                TextureAtlasSprite sprite = mat != null ? spriteGetter.apply(mat) : null;
                bound.add(new NMeshPartBakedModel.BoundSubMesh(sm, sprite));
            }
            if (bound.isEmpty()) {
                MainRegistry.LOGGER.warn("hbm_m:nmesh_obj: model {} baked with 0 visible sub-meshes", modelLocation);
            }

            boolean ao = aoOverride != null ? aoOverride : context.useAmbientOcclusion();
            RenderType blockRt = null;
            RenderType itemRt = null;
            ResourceLocation hint = context.getRenderTypeHint();
            if (hint != null) {
                // RenderTypeGroup is used without an import to stay clear of loader-package rewrites.
                var group = context.getRenderType(hint);
                blockRt = group.block();
                itemRt = group.entity();
            }

            // Forge ObjModel.addQuads order (ObjModel.java:631): modelState rotation first,
            // then the JSON root transform; NMeshPartBakedModel re-bases center->corner at bake.
            Transformation rootTransform = modelState.getRotation().compose(context.getRootTransform());
            return new NMeshPartBakedModel(bound, flipV, rootTransform, context.getTransforms(),
                    shadeQuads, ao, automaticCulling, tintIndex, blockRt, itemRt, emissiveAmbient);
        }

        /**
         * Forge {@code resolveDirtyMaterial}-style binding on the persisted {@code map_Kd} value,
         * falling back to the MTL material name and then the conventional JSON texture keys.
         */
        @Nullable
        private Material resolveSprite(IGeometryBakingContext context, NMeshData.SubMesh sm) {
            String mapKd = sm.mapKdTexture;
            if (!mapKd.isEmpty() && mapKd.charAt(0) == '#') {
                String ref = mapKd.substring(1);
                if (!ref.isEmpty() && context.hasMaterial(ref)) {
                    return context.getMaterial(ref);
                }
            } else if (!mapKd.isEmpty()) {
                ResourceLocation tex = ResourceLocation.tryParse(mapKd);
                if (tex != null) {
                    return new Material(TextureAtlas.LOCATION_BLOCKS, tex);
                }
            }
            if (context.hasMaterial(sm.materialName)) {
                return context.getMaterial(sm.materialName);
            }
            if (context.hasMaterial("default")) {
                return context.getMaterial("default");
            }
            if (context.hasMaterial("texture")) {
                return context.getMaterial("texture");
            }
            return null;
        }
    }
}
