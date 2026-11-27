package com.hbm_m.platform.legacy.client.model;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm_m.client.model.nmesh.NMeshData;
import com.hbm_m.client.model.nmesh.NMeshLoader;
import com.hbm_m.client.model.nmesh.NMeshPartBakedModel;
import com.hbm_m.main.MainRegistry;
import com.mojang.math.Transformation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * [26.x] NMesh-Teile-Registry: liefert den Nucleus-Renderern ihre Modellteile.
 *
 * <p>Auf den alten Versionen holten die Renderer die Teile aus den gebackenen BakedModels
 * ({@code getBlockModel(state)} / {@code ModelManager.getModel(id)}). Auf 26.x gibt es diese Modelle
 * nicht mehr; Stonecutter leitet die Aufrufe hierher um. Gebaut wird lazy pro Modell-JSON:
 * Modell-JSON (inkl. {@code parent}-Kette) → {@link NMeshData} → ein {@link NMeshPartBakedModel}
 * pro Teil, Sprites aus dem Block-Atlas. Die Teile werden wie in {@code AbstractObjPartModelLoader}
 * ohne Blockstate-Rotation gebacken (die Ausrichtung übernimmt der BER).
 */
public final class NMeshPartRegistry {

    private static final String BASE = "Base";
    private static final Map<Identifier, BakedModel> MODELS = new ConcurrentHashMap<>();
    private static final Map<Block, Identifier> BLOCK_MODEL_IDS = new ConcurrentHashMap<>();

    static {
        // Sprites hängen am Block-Atlas: bei jedem Cache-Invalidate (Ressourcen-Reload) neu bauen.
        com.hbm_m.client.render.cache.RenderCacheManager.register(reason -> clear());
    }

    private NMeshPartRegistry() {}

    /** Ressourcen-Reload / Atlas-Neuaufbau. */
    public static void clear() {
        MODELS.clear();
        BLOCK_MODEL_IDS.clear();
    }

    /** Ersatz für {@code getBlockRenderer().getBlockModel(state)}. */
    public static BakedModel blockModel(BlockState state) {
        Identifier id = BLOCK_MODEL_IDS.computeIfAbsent(state.getBlock(), NMeshPartRegistry::findBlockModelId);
        return model(id);
    }

    /** Ersatz für {@code ModelManager.getModel(...)}: {@code ns:block/x}, {@code ns:item/x} usw. */
    public static BakedModel model(Identifier modelId) {
        return MODELS.computeIfAbsent(modelId, NMeshPartRegistry::build);
    }

    // ------------------------------------------------------------------ Blockstate → Modell-ID

    private static Identifier findBlockModelId(Block block) {
        Identifier key = BuiltInRegistries.BLOCK.getKey(block);
        Identifier fallback = Identifier.fromNamespaceAndPath(key.getNamespace(), "block/" + key.getPath());
        JsonObject bs = readJson(Identifier.fromNamespaceAndPath(key.getNamespace(), "blockstates/" + key.getPath() + ".json"));
        if (bs == null) return fallback;
        String model = firstModel(bs);
        return model != null ? Identifier.parse(model) : fallback;
    }

    private static @Nullable String firstModel(JsonElement e) {
        if (e.isJsonObject()) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("model") && o.get("model").isJsonPrimitive()) return o.get("model").getAsString();
            for (var entry : o.entrySet()) {
                String m = firstModel(entry.getValue());
                if (m != null) return m;
            }
        } else if (e.isJsonArray()) {
            for (JsonElement c : e.getAsJsonArray()) {
                String m = firstModel(c);
                if (m != null) return m;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ Bau

    private record ModelJson(String loader, @Nullable Identifier mesh, Map<String, String> textures, boolean flipV,
                             Transformation root, @Nullable JsonElement parts) {}

    private static BakedModel build(Identifier modelId) {
        try {
            ModelJson json = resolve(modelId);
            if (json == null || json.mesh() == null) return BakedModel.EMPTY;
            NMeshData data = NMeshLoader.load(json.mesh());
            if (data == null) {
                MainRegistry.LOGGER.warn("[26.x] NMeshPartRegistry: kein Mesh {} für {}", json.mesh(), modelId);
                return BakedModel.EMPTY;
            }
            Map<String, BakedModel> parts = bakeParts(data, json);
            AbstractMultipartBakedModel model = create(json.loader(), modelId, parts);
            TextureAtlasSprite particle = sprite(json.textures(), "particle");
            if (particle != null) model.setParticleIcon(particle);
            return model;
        } catch (Exception e) {
            MainRegistry.LOGGER.error("[26.x] NMeshPartRegistry: {} konnte nicht gebaut werden", modelId, e);
            return BakedModel.EMPTY;
        }
    }

    private static AbstractMultipartBakedModel create(String loader, Identifier id, Map<String, BakedModel> parts) {
        ItemTransforms t = ItemTransforms.NO_TRANSFORMS;
        if (loader.endsWith(":door")) return new DoorBakedModel(id, parts, t);
        if (loader.endsWith(":missile_loader")) return new MissileBakedModel(id, parts, t);
        if (loader.endsWith(":soyuz_rocket_loader")) return new SoyuzRocketBakedModel(id, parts, t);
        if (loader.contains(":weapon_")) return new WeaponMultipartBakedModel(id, parts, t);
        return new ConfiguredMultipartBakedModel(id, parts, t);
    }

    private static Map<String, BakedModel> bakeParts(NMeshData data, ModelJson json) {
        // Teilname → (OBJ-Gruppe, optionale Textur)
        Map<String, String[]> specs = new LinkedHashMap<>();
        JsonElement p = json.parts();
        if (p != null && p.isJsonObject()) {
            for (var e : p.getAsJsonObject().entrySet()) {
                JsonObject s = e.getValue().isJsonObject() ? e.getValue().getAsJsonObject() : new JsonObject();
                specs.put(e.getKey(), new String[] {
                        s.has("group") ? s.get("group").getAsString() : e.getKey(),
                        s.has("texture") ? s.get("texture").getAsString() : null });
            }
        } else {
            Set<String> names = new LinkedHashSet<>();
            if (p != null && p.isJsonArray()) for (JsonElement e : p.getAsJsonArray()) names.add(e.getAsString());
            if (names.isEmpty()) names.addAll(data.getRootComponentNames());
            for (String n : names) specs.put(n, new String[] { n, null });
        }

        Map<String, BakedModel> out = new HashMap<>();
        for (var e : specs.entrySet()) {
            String name = e.getKey();
            if (out.containsKey(name)) continue;
            NMeshPartBakedModel baked = bakePart(data, json, name, e.getValue()[0], e.getValue()[1]);
            out.put(name, baked);
            // Objekt/Gruppe als Alias (wie im alten Loader)
            for (NMeshData.SubMesh sm : data.subMeshes()) {
                if (sm.objectName.isEmpty() || sm.groupName.isEmpty() || sm.objectName.equals(sm.groupName)) continue;
                if (name.equals(sm.objectName)) out.putIfAbsent(sm.groupName, baked);
                else if (name.equals(sm.groupName)) out.putIfAbsent(sm.objectName, baked);
            }
        }
        if (!out.containsKey(BASE)) {
            for (var e : out.entrySet()) {
                if (e.getKey().equalsIgnoreCase(BASE)) {
                    out.put(BASE, e.getValue());
                    break;
                }
            }
        }
        return out;
    }

    private static NMeshPartBakedModel bakePart(NMeshData data, ModelJson json, String part, String group,
                                                @Nullable String textureOverride) {
        List<NMeshData.SubMesh> subs = data.getSubMeshesForPart(group);
        List<NMeshPartBakedModel.BoundSubMesh> bound = new ArrayList<>(subs.size());
        TextureAtlasSprite override = textureOverride != null ? atlasSprite(Identifier.parse(textureOverride)) : null;
        for (NMeshData.SubMesh sm : subs) {
            TextureAtlasSprite sprite = override != null ? override : partSprite(json.textures(), part, sm.materialName);
            bound.add(new NMeshPartBakedModel.BoundSubMesh(sm, sprite));
        }
        return new NMeshPartBakedModel(bound, json.flipV(), json.root(), ItemTransforms.NO_TRANSFORMS);
    }

    /** Gleiche Reihenfolge wie {@code SinglePartBakingContext#getMaterial} der alten Loader. */
    private static @Nullable TextureAtlasSprite partSprite(Map<String, String> tex, String part, String material) {
        String lower = part.toLowerCase(Locale.ROOT);
        List<String> keys = new ArrayList<>();
        if (part.equalsIgnoreCase("Label")) keys.add("label");
        else if (part.equalsIgnoreCase("Frame") || part.equalsIgnoreCase("Door")) { keys.add("default"); keys.add(lower); }
        else { keys.add(lower); keys.add("default"); }
        keys.add(material);
        keys.add(part);
        keys.add("particle");
        for (String k : keys) {
            TextureAtlasSprite s = sprite(tex, k);
            if (s != null) return s;
        }
        return null;
    }

    private static @Nullable TextureAtlasSprite sprite(Map<String, String> tex, String key) {
        String v = tex.get(key);
        for (int guard = 0; v != null && v.startsWith("#") && guard < 8; guard++) v = tex.get(v.substring(1));
        if (v == null || v.startsWith("#")) return null;
        Identifier id = Identifier.tryParse(v);
        return id != null ? atlasSprite(id) : null;
    }

    private static @Nullable TextureAtlasSprite atlasSprite(Identifier id) {
        try {
            TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
            return atlas.getSprite(id);
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ JSON

    private static @Nullable ModelJson resolve(Identifier modelId) {
        String loader = "";
        Identifier mesh = null;
        Map<String, String> textures = new HashMap<>();
        Boolean flipV = null;
        Transformation root = null;
        JsonElement parts = null;
        Identifier cur = modelId;
        for (int depth = 0; cur != null && depth < 16; depth++) {
            JsonObject o = readJson(Identifier.fromNamespaceAndPath(cur.getNamespace(), "models/" + cur.getPath() + ".json"));
            if (o == null) break;
            if (loader.isEmpty() && o.has("loader")) loader = o.get("loader").getAsString();
            if (mesh == null && o.has("model")) mesh = Identifier.tryParse(o.get("model").getAsString());
            if (flipV == null && o.has("flip_v")) flipV = o.get("flip_v").getAsBoolean();
            if (root == null && o.has("transform") && o.get("transform").isJsonObject()) root = parseTransform(o.getAsJsonObject("transform"));
            if (parts == null && o.has("parts")) parts = o.get("parts");
            if (o.has("textures") && o.get("textures").isJsonObject()) {
                for (var e : o.getAsJsonObject("textures").entrySet()) textures.putIfAbsent(e.getKey(), e.getValue().getAsString());
            }
            cur = o.has("parent") ? Identifier.tryParse(o.get("parent").getAsString()) : null;
        }
        if (mesh == null && loader.isEmpty()) return null;
        return new ModelJson(loader, mesh, textures, flipV == null || flipV,
                root != null ? root : Transformation.IDENTITY, parts);
    }

    private static Transformation parseTransform(JsonObject t) {
        Vector3f translation = vec(t.get("translation"), 0f);
        Vector3f scale = vec(t.get("scale"), 1f);
        Quaternionf rot = new Quaternionf();
        JsonElement r = t.get("rotation");
        if (r != null && r.isJsonArray() && r.getAsJsonArray().size() == 3) {
            Vector3f d = vec(r, 0f);
            rot.rotationXYZ((float) Math.toRadians(d.x), (float) Math.toRadians(d.y), (float) Math.toRadians(d.z));
        } else if (r != null && r.isJsonArray() && r.getAsJsonArray().size() == 4) {
            JsonArray a = r.getAsJsonArray();
            rot.set(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat(), a.get(3).getAsFloat());
        }
        return new Transformation(translation, rot, scale, null);
    }

    private static Vector3f vec(@Nullable JsonElement e, float def) {
        if (e == null) return new Vector3f(def);
        if (e.isJsonPrimitive()) return new Vector3f(e.getAsFloat());
        JsonArray a = e.getAsJsonArray();
        return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
    }

    private static @Nullable JsonObject readJson(Identifier location) {
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        Resource res = rm.getResource(location).orElse(null);
        if (res == null) return null;
        try (Reader r = res.openAsReader()) {
            return JsonParser.parseReader(r).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }
}
