//? if fabric {
package com.hbm_m.client.model.loading;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hbm_m.client.model.DoorBakedModel;
import com.hbm_m.client.model.HeatingOvenBakedModel;
import com.hbm_m.client.model.MachineAdvancedAssemblerBakedModel;
import com.hbm_m.client.model.MachineAssemblerBakedModel;
import com.hbm_m.client.model.MachineBatterySocketBakedModel;
import com.hbm_m.client.model.MachineChemicalPlantBakedModel;
import com.hbm_m.client.model.MachineFluidTankBakedModel;
import com.hbm_m.client.model.MachineHydraulicFrackiningTowerBakedModel;
import com.hbm_m.client.model.PressBakedModel;
import com.hbm_m.main.MainRegistry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.model.BakedQuad;
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
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
final class HbmLoaderAdapters {

    private HbmLoaderAdapters() {}

    static ForgeLikeUnbakedModel press(ResourceLocation id, JsonObject json, ResourceManager rm, Gson gson) {
        ResourceLocation base = ResourceLocation.tryParse(GsonHelper.getAsString(json, "base_model"));
        ResourceLocation head = ResourceLocation.tryParse(GsonHelper.getAsString(json, "head_model"));
        boolean flipV = GsonHelper.getAsBoolean(json, "flip_v", true);
        boolean automaticCulling = GsonHelper.getAsBoolean(json, "automatic_culling", true);
        com.mojang.math.Transformation rootTransform = JsonModelTransforms.parseRootTransform(json.getAsJsonObject("transform"));
        ItemTransforms transforms = JsonModelTransforms.parseItemTransforms(json.get("display"), gson);
        Map<String, ResourceLocation> textures = parseTextures(json);
        JsonObject headTransform = json.has("head_transform") && json.get("head_transform").isJsonObject()
                ? json.getAsJsonObject("head_transform")
                : null;
        Vector3f headRestOffset = parseVector3(headTransform, "translation", new Vector3f(0.0F, 0.8F, 0.0F));
        float headTravelDistance = headTransform != null
                ? GsonHelper.getAsFloat(headTransform, "travel", 0.8F)
                : 0.8F;

        return new MultiObjUnbakedModel(id, Map.of("Base", base, "Head", head), flipV, automaticCulling, rootTransform, textures, transforms, rm, parts -> new PressBakedModel(parts, transforms, headRestOffset, headTravelDistance));
    }

    static ForgeLikeUnbakedModel heatingOven(ResourceLocation id, JsonObject json, ResourceManager rm, Gson gson) {
        return objParts(id, json, rm, gson, Set.of(HeatingOvenBakedModel.MAIN, HeatingOvenBakedModel.DOOR, HeatingOvenBakedModel.INNER, HeatingOvenBakedModel.INNER_BURNING), (parts, t, loc) -> new HeatingOvenBakedModel(parts, t));
    }

    static ForgeLikeUnbakedModel advancedAssembler(ResourceLocation id, JsonObject json, ResourceManager rm, Gson gson) {
        Set<String> parts = Set.of("Base", "Frame", "Ring", "ArmLower1", "ArmUpper1", "Head1", "Spike1", "ArmLower2", "ArmUpper2", "Head2", "Spike2");
        return objParts(id, json, rm, gson, parts, (p, t, loc) -> new MachineAdvancedAssemblerBakedModel(p, t));
    }

    static ForgeLikeUnbakedModel chemicalPlant(ResourceLocation id, JsonObject json, ResourceManager rm, Gson gson) {
        return objParts(id, json, rm, gson, Set.of("Base", "Frame", "Slider", "Spinner", "Fluid"), (p, t, loc) -> new MachineChemicalPlantBakedModel(p, t));
    }

    static ForgeLikeUnbakedModel hydraulicTower(ResourceLocation id, JsonObject json, ResourceManager rm, Gson gson) {
        return objParts(id, json, rm, gson, Set.of("Cube_Cube.001"), (p, t, loc) -> new MachineHydraulicFrackiningTowerBakedModel(p, t));
    }

    static ForgeLikeUnbakedModel fluidTank(ResourceLocation id, JsonObject json, ResourceManager rm, Gson gson) {
        return objParts(id, json, rm, gson, Set.of("Frame", "Tank"), (p, t, loc) -> new MachineFluidTankBakedModel(p, t));
    }

    static ForgeLikeUnbakedModel batterySocket(ResourceLocation id, JsonObject json, ResourceManager rm, Gson gson) {
        return objParts(id, json, rm, gson, Set.of("Socket", "Battery"), (p, t, loc) -> new MachineBatterySocketBakedModel(p, t));
    }

    static ForgeLikeUnbakedModel door(ResourceLocation id, JsonObject json, ResourceManager rm, Gson gson) {
        Set<String> parts = Set.of("frame", "doorLeft", "doorRight");
        if (json.has("parts")) {
            java.util.HashSet<String> parsed = new java.util.HashSet<>();
            json.getAsJsonArray("parts").forEach(e -> parsed.add(e.getAsString()));
            parts = parsed;
        }
        return objParts(id, json, rm, gson, parts, (p, t, loc) -> new DoorBakedModel(p, t, loc));
    }

    static ForgeLikeUnbakedModel machineAssembler(ResourceLocation id, JsonObject json, ResourceManager rm, Gson gson) {
        JsonObject parts = GsonHelper.getAsJsonObject(json, "parts");
        Map<String, PartObjDef> partDefs = new java.util.LinkedHashMap<>();
        for (var entry : parts.entrySet()) {
            JsonObject partJson = entry.getValue().getAsJsonObject();
            ResourceLocation model = ResourceLocation.tryParse(GsonHelper.getAsString(partJson, "model"));
            ResourceLocation texture = ResourceLocation.tryParse(GsonHelper.getAsString(partJson, "texture"));
            partDefs.put(entry.getKey(), new PartObjDef(model, texture));
        }

        boolean flipV = GsonHelper.getAsBoolean(json, "flip_v", true);
        boolean automaticCulling = GsonHelper.getAsBoolean(json, "automatic_culling", true);
        com.mojang.math.Transformation rootTransform = JsonModelTransforms.parseRootTransform(json.getAsJsonObject("transform"));
        ItemTransforms transforms = JsonModelTransforms.parseItemTransforms(json.get("display"), gson);
        Map<String, ResourceLocation> textures = parseTextures(json);
        return new PerPartObjUnbakedModel(id, partDefs, flipV, automaticCulling, rootTransform, textures, transforms, rm,
                partsMap -> new MachineAssemblerBakedModel(partsMap, transforms));
    }

    private record PartObjDef(ResourceLocation model, ResourceLocation texture) {}

    private interface MultipartFactory<T extends BakedModel> {
        T create(HashMap<String, BakedModel> bakedParts, ItemTransforms transforms, ResourceLocation modelLocation);
    }

    private static <T extends BakedModel> ForgeLikeUnbakedModel objParts(ResourceLocation id, JsonObject json, ResourceManager rm, Gson gson, Set<String> partNames, MultipartFactory<T> factory) {
        ResourceLocation objLoc = ResourceLocation.tryParse(GsonHelper.getAsString(json, "model"));
        boolean flipV = GsonHelper.getAsBoolean(json, "flip_v", true);
        boolean automaticCulling = GsonHelper.getAsBoolean(json, "automatic_culling", true);
        com.mojang.math.Transformation rootTransform = JsonModelTransforms.parseRootTransform(json.getAsJsonObject("transform"));
        ItemTransforms transforms = JsonModelTransforms.parseItemTransforms(json.get("display"), gson);
        ObjModelData data = ObjModelData.load(rm, objLoc);
        Map<String, ResourceLocation> textures = parseTextures(json);
        return new ObjPartsUnbakedModel<>(id, data, textures, partNames, flipV, automaticCulling, rootTransform, transforms, factory);
    }

    private static final class ObjPartsUnbakedModel<T extends BakedModel> implements ForgeLikeUnbakedModel {
        private final ResourceLocation id; private final ObjModelData data; private final Map<String, ResourceLocation> textures; private final Set<String> partNames; private final boolean flipV; private final boolean automaticCulling; private final com.mojang.math.Transformation rootTransform; private final ItemTransforms itemTransforms; private final MultipartFactory<T> factory;

        private ObjPartsUnbakedModel(ResourceLocation id, ObjModelData data, Map<String, ResourceLocation> textures, Set<String> partNames, boolean flipV, boolean automaticCulling, com.mojang.math.Transformation rootTransform, ItemTransforms itemTransforms, MultipartFactory<T> factory) {
            this.id = id; this.data = data; this.textures = textures; this.partNames = partNames; this.flipV = flipV; this.automaticCulling = automaticCulling; this.rootTransform = rootTransform; this.itemTransforms = itemTransforms; this.factory = factory;
        }

        @Override public Map<String, ResourceLocation> textures() { return textures; }
        @Override public java.util.Collection<ResourceLocation> getDependencies() { return List.of(); }
        @Override public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter) {}

        @Override
        public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ResourceLocation modelLocation) {
            HashMap<String, BakedModel> bakedParts = new HashMap<>();
            TextureAtlasSprite particle = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, textures.getOrDefault("particle", MissingTextureAtlasSprite.getLocation())));

            // On Forge, AbstractObjPartModelLoader bakes parts with IDENTITY modelState
            // (the BakedModel handles facing rotation at runtime via transformQuadsByFacing).
            // We only apply rootTransform here, with blockCenterToCorner for proper centering.
            com.mojang.math.Transformation combined = ModelStateTransforms.isIdentity(rootTransform)
                    ? com.mojang.math.Transformation.identity()
                    : ModelStateTransforms.blockCenterToCorner(rootTransform);

            ObjQuadBaker.ObjQuadBakerState.MODEL = data;
            try {
                for (String part : partNames) {
                    Map<Direction, List<BakedQuad>> quads = new HashMap<>();
                    for (Direction d : Direction.values()) quads.put(d, new java.util.ArrayList<>());
                    quads.put(null, new java.util.ArrayList<>());

                    String partLower = part.toLowerCase(Locale.ROOT);
                    for (ObjModelData.Face f : data.faces()) {
                        String objName = f.objectName();
                        if (objName == null) continue;
                        String objLower = objName.toLowerCase(Locale.ROOT);
                        if (!(objLower.equals(partLower) || objLower.startsWith(partLower + "/"))) continue;

                        ResourceLocation tex = resolveTexture(textures, data, f);
                        TextureAtlasSprite sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, tex));
                        if (sprite == null) sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, MissingTextureAtlasSprite.getLocation()));

                        List<BakedQuad> fQuads = ObjQuadBaker.bakeFaceToQuads(f, sprite, flipV, combined, true);
                        for (BakedQuad q : fQuads) ObjQuadBaker.addQuadWithCulling(q, quads, automaticCulling);
                    }
                    bakedParts.put(part, new ObjBakedModel(quads, particle, true, false, ItemTransforms.NO_TRANSFORMS, ItemOverrides.EMPTY));
                }
            } finally {
                ObjQuadBaker.ObjQuadBakerState.MODEL = null;
            }
            return ModelDebugDumper.wrapIfEnabled(modelLocation, factory.create(bakedParts, itemTransforms, modelLocation));
        }
    }

    private static ResourceLocation resolveTexture(Map<String, ResourceLocation> textures, ObjModelData data, ObjModelData.Face face) {
        String mat = face.materialName();
        if (mat != null) {
            String mtlTex = data.materialTexture(mat);
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

    private static final class MultiObjUnbakedModel implements ForgeLikeUnbakedModel {
        private final ResourceLocation id; private final Map<String, ResourceLocation> partToObj; private final boolean flipV; private final boolean automaticCulling; private final com.mojang.math.Transformation rootTransform; private final Map<String, ResourceLocation> textures; private final ItemTransforms transforms; private final ResourceManager rm; private final Function<HashMap<String, BakedModel>, BakedModel> factory;

        private MultiObjUnbakedModel(ResourceLocation id, Map<String, ResourceLocation> partToObj, boolean flipV, boolean automaticCulling, com.mojang.math.Transformation rootTransform, Map<String, ResourceLocation> textures, ItemTransforms transforms, ResourceManager rm, Function<HashMap<String, BakedModel>, BakedModel> factory) {
            this.id = id; this.partToObj = partToObj; this.flipV = flipV; this.automaticCulling = automaticCulling; this.rootTransform = rootTransform; this.textures = textures; this.transforms = transforms; this.rm = rm; this.factory = factory;
        }

        @Override public Map<String, ResourceLocation> textures() { return textures; }
        @Override public java.util.Collection<ResourceLocation> getDependencies() { return List.of(); }
        @Override public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter) {}

        @Override
        public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ResourceLocation modelLocation) {
            HashMap<String, BakedModel> bakedParts = new HashMap<>();
            TextureAtlasSprite particle = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, textures.getOrDefault("particle", MissingTextureAtlasSprite.getLocation())));

            // On Forge, AbstractObjPartModelLoader bakes parts with IDENTITY modelState.
            // Only rootTransform is applied (with blockCenterToCorner for centering).
            com.mojang.math.Transformation combined = ModelStateTransforms.isIdentity(rootTransform)
                    ? com.mojang.math.Transformation.identity()
                    : ModelStateTransforms.blockCenterToCorner(rootTransform);

            for (var e : partToObj.entrySet()) {
                ObjModelData data = ObjModelData.load(rm, e.getValue());
                ObjQuadBaker.ObjQuadBakerState.MODEL = data;
                try {
                    Map<Direction, List<BakedQuad>> quads = new HashMap<>();
                    for (Direction d : Direction.values()) quads.put(d, new java.util.ArrayList<>());
                    quads.put(null, new java.util.ArrayList<>());

                    for (ObjModelData.Face f : data.faces()) {
                        ResourceLocation tex = resolveTexture(textures, data, f);
                        TextureAtlasSprite sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, tex));
                        List<BakedQuad> fQuads = ObjQuadBaker.bakeFaceToQuads(f, sprite, flipV, combined, true);
                        for (BakedQuad q : fQuads) ObjQuadBaker.addQuadWithCulling(q, quads, automaticCulling);
                    }
                    bakedParts.put(e.getKey(), new ObjBakedModel(quads, particle, true, false, ItemTransforms.NO_TRANSFORMS, ItemOverrides.EMPTY));
                } finally {
                    ObjQuadBaker.ObjQuadBakerState.MODEL = null;
                }
            }
            return factory.apply(bakedParts);
        }
    }

    private static final class PerPartObjUnbakedModel implements ForgeLikeUnbakedModel {
        private final ResourceLocation id; private final Map<String, PartObjDef> partDefs; private final boolean flipV; private final boolean automaticCulling; private final com.mojang.math.Transformation rootTransform; private final Map<String, ResourceLocation> textures; private final ItemTransforms transforms; private final ResourceManager rm; private final Function<HashMap<String, BakedModel>, BakedModel> factory;

        private PerPartObjUnbakedModel(ResourceLocation id, Map<String, PartObjDef> partDefs, boolean flipV, boolean automaticCulling, com.mojang.math.Transformation rootTransform, Map<String, ResourceLocation> textures, ItemTransforms transforms, ResourceManager rm, Function<HashMap<String, BakedModel>, BakedModel> factory) {
            this.id = id; this.partDefs = partDefs; this.flipV = flipV; this.automaticCulling = automaticCulling; this.rootTransform = rootTransform; this.textures = textures; this.transforms = transforms; this.rm = rm; this.factory = factory;
        }

        @Override public Map<String, ResourceLocation> textures() { return textures; }
        @Override public java.util.Collection<ResourceLocation> getDependencies() { return List.of(); }
        @Override public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter) {}

        @Override
        public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ResourceLocation modelLocation) {
            HashMap<String, BakedModel> bakedParts = new HashMap<>();
            TextureAtlasSprite particle = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, textures.getOrDefault("particle", MissingTextureAtlasSprite.getLocation())));
            com.mojang.math.Transformation combined = ModelStateTransforms.isIdentity(rootTransform)
                    ? com.mojang.math.Transformation.identity()
                    : ModelStateTransforms.blockCenterToCorner(rootTransform);

            for (var e : partDefs.entrySet()) {
                PartObjDef def = e.getValue();
                ObjModelData data = ObjModelData.load(rm, def.model());
                ObjQuadBaker.ObjQuadBakerState.MODEL = data;
                try {
                    Map<Direction, List<BakedQuad>> quads = new HashMap<>();
                    for (Direction d : Direction.values()) quads.put(d, new java.util.ArrayList<>());
                    quads.put(null, new java.util.ArrayList<>());

                    ResourceLocation partTexture = def.texture() != null
                            ? def.texture()
                            : textures.getOrDefault("default", textures.getOrDefault("particle", MissingTextureAtlasSprite.getLocation()));
                    TextureAtlasSprite sprite = spriteGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, partTexture));
                    if (sprite == null) sprite = particle;

                    for (ObjModelData.Face f : data.faces()) {
                        List<BakedQuad> fQuads = ObjQuadBaker.bakeFaceToQuads(f, sprite, flipV, combined, true);
                        for (BakedQuad q : fQuads) ObjQuadBaker.addQuadWithCulling(q, quads, automaticCulling);
                    }
                    bakedParts.put(e.getKey(), new ObjBakedModel(quads, particle, true, false, ItemTransforms.NO_TRANSFORMS, ItemOverrides.EMPTY));
                } finally {
                    ObjQuadBaker.ObjQuadBakerState.MODEL = null;
                }
            }
            return ModelDebugDumper.wrapIfEnabled(modelLocation, factory.apply(bakedParts));
        }
    }

    private static Map<String, ResourceLocation> parseTextures(JsonObject json) {
        if (!json.has("textures")) return Map.of();
        JsonObject t = json.getAsJsonObject("textures");
        Map<String, ResourceLocation> out = new HashMap<>();
        for (var e : t.entrySet()) {
            if (!e.getValue().isJsonPrimitive()) continue;
            ResourceLocation rl = ResourceLocation.tryParse(e.getValue().getAsString());
            if (rl != null) out.put(e.getKey(), rl);
        }
        return out;
    }

    private static Vector3f parseVector3(JsonObject json, String key, Vector3f fallback) {
        if (json == null || !json.has(key) || !json.get(key).isJsonArray()) {
            return new Vector3f(fallback);
        }
        var arr = json.getAsJsonArray(key);
        if (arr.size() != 3) {
            return new Vector3f(fallback);
        }
        return new Vector3f(arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat());
    }
}
//?}