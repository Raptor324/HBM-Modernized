//? if fabric {
package com.hbm_m.client.model.loading;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm_m.main.MainRegistry;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

@Environment(EnvType.CLIENT)
@SuppressWarnings("UnstableApiUsage")
public final class ForgeLikeModelLoadingFabric {

    private ForgeLikeModelLoadingFabric() {}

    private static final Gson GSON = new GsonBuilder().create();

    public static void init() {
        PreparableModelLoadingPlugin.register(ForgeLikeModelLoadingFabric::prepare, ForgeLikeModelLoadingFabric::initialize);
        MainRegistry.LOGGER.info("Registered Fabric forge-like model loading plugin (OBJ + composite + HBM loaders).");
    }

    private static CompletableFuture<Map<Object, ForgeLikeUnbakedModel>> prepare(ResourceManager rm, Executor executor) {
        return CompletableFuture.supplyAsync(() -> scanModels(rm), executor);
    }

    private static void initialize(Map<Object, ForgeLikeUnbakedModel> models, ModelLoadingPlugin.Context ctx) {
        // Important: DO NOT replace via resolveModel(), otherwise vanilla BlockModel parent resolution may
        // try to treat our custom UnbakedModel as a BlockModel and crash ("parent has to be a block model").
        // modifyModelBeforeBake swaps only the model used for baking, keeping the cached unbaked model intact.
        ctx.modifyModelBeforeBake().register((original, modifierCtx) -> {
            Object id = modifierCtx.id();
            ForgeLikeUnbakedModel replacement = models.get(id);
            return replacement != null ? replacement : original;
        });

        // Register variant/skin models so they are loaded into ModelManager.
        // On Forge this is done via ModelEvent.RegisterAdditional; on Fabric we
        // must call addModels() explicitly, otherwise getModel() returns the
        // missing-model placeholder and door skin switching silently fails.
        for (Object key : models.keySet()) {
            if (key instanceof ResourceLocation rl && rl.getPath().startsWith("block/doors/")) {
                ctx.addModels(rl);
            }
        }

        // Только JSON: vanilla не подгружает item-модель, пока к ней не обратятся через предмет/блок.
        // BuiltinItemRenderer Sodium не вызывает — подмена в {@code MixinItemRenderer#getModel}.
        // Обязательно ModelResourceLocation#inventory иначе getModel(...) не находит запись —
        // сравнение с missing провалится, миксин не подставит текстуру, игра оставит Fabric-обёртку.
        ctx.addModels(new ModelResourceLocation(new ResourceLocation(MainRegistry.MOD_ID, "assembly_template_base"), "inventory"));
    }

    private static Map<Object, ForgeLikeUnbakedModel> scanModels(ResourceManager rm) {
        Map<Object, ForgeLikeUnbakedModel> out = new HashMap<>();

        Map<ResourceLocation, Resource> jsons = listModelJsons(rm);
        for (var entry : jsons.entrySet()) {
            ResourceLocation fileId = entry.getKey(); // e.g. hbm_m:models/block/foo.json
            JsonObject json;
            Resource res = entry.getValue();
            try (InputStream in = res.open();
                 BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                json = JsonParser.parseReader(br).getAsJsonObject();
            } catch (Exception e) {
                MainRegistry.LOGGER.warn("Failed to read model json {}", fileId, e);
                continue;
            }

            // Map file path under models/ -> model id (namespace:pathWithoutExt)
            String relPath = fileId.getPath();
            if (!relPath.startsWith("models/") || !relPath.endsWith(".json")) continue;
            String modelPath = relPath.substring("models/".length(), relPath.length() - ".json".length());

            // IMPORTANT: models/item/<name>.json maps to model id <name> (not item/<name>).
            // Block models keep their "block/<name>" ids.
            ResourceLocation modelId = modelPath.startsWith("item/")
                    ? new ResourceLocation(fileId.getNamespace(), modelPath.substring("item/".length()))
                    : new ResourceLocation(fileId.getNamespace(), modelPath);

            String loader = json.has("loader") ? json.get("loader").getAsString() : null;

            if (loader != null) {
                ForgeLikeUnbakedModel parsed = ForgeLikeUnbakedModel.parse(modelId, loader, json, rm, GSON);
                if (parsed != null) {
                    out.put(modelId, parsed);
                    out.put(new ModelResourceLocation(modelId, "inventory"), parsed);
                }
            }

            // If this is an item model with just "parent": "mod:block/...", bake using the parent's
            // forge-like model (OBJ/composite/custom loaders). This avoids breaking BlockModel parent resolution.
            if (modelPath.startsWith("item/")) {
                // Only override transforms if item json explicitly provides "display".
                // If it doesn't, vanilla/Forge behavior is to inherit transforms from the parent model.
                ItemTransforms itemDisplay = json.has("display")
                        ? JsonModelTransforms.parseItemTransforms(json.get("display"), GSON)
                        : null;

                ResourceLocation parent = tryReadParentId(json);
                if (parent != null) {
                    ForgeLikeUnbakedModel parentModel = out.get(parent);
                    if (parentModel != null) {
                        out.put(new ModelResourceLocation(modelId, "inventory"),
                                new TransformWrappingUnbakedModel(parentModel, itemDisplay));
                    }
                }

                // Handle forge:separate_transforms: use base.parent as parent for visibility (GUI fallback may be lost).
                if ("forge:separate_transforms".equals(loader)) {
                    ResourceLocation baseParent = tryReadSeparateTransformsBaseParent(json);
                    if (baseParent != null) {
                        ForgeLikeUnbakedModel baseModel = out.get(baseParent);
                        if (baseModel != null) {
                            out.put(new ModelResourceLocation(modelId, "inventory"),
                                    new TransformWrappingUnbakedModel(baseModel, itemDisplay));
                        }
                    }
                }
            }
        }

        MainRegistry.LOGGER.info("Forge-like model scan: {} custom models registered", out.size());
        return out;
    }

    private static ResourceLocation tryReadParentId(JsonObject json) {
        JsonElement e = json.get("parent");
        if (e == null || !e.isJsonPrimitive()) return null;
        String parentStr = e.getAsString();
        ResourceLocation parent = ResourceLocation.tryParse(parentStr);
        return parent;
    }

    private static ResourceLocation tryReadSeparateTransformsBaseParent(JsonObject json) {
        JsonObject base = json.has("base") && json.get("base").isJsonObject() ? json.getAsJsonObject("base") : null;
        if (base == null) return null;
        JsonElement p = base.get("parent");
        if (p == null || !p.isJsonPrimitive()) return null;
        return ResourceLocation.tryParse(p.getAsString());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Map<ResourceLocation, Resource> listModelJsons(ResourceManager rm) {
        // We need to support both Mojmap and Yarn variants.
        // Mojmap (1.20.1) usually has: listResources(String, Predicate<ResourceLocation>) -> Map<ResourceLocation, Resource>
        // Yarn has: findResources(String, Predicate<Identifier>) -> Map<Identifier, Resource>
        try {
            var m = rm.getClass().getMethod("listResources", String.class, java.util.function.Predicate.class);
            Object result = m.invoke(rm, "models", (java.util.function.Predicate) (Object o) -> {
                if (o instanceof ResourceLocation rl) return rl.getPath().endsWith(".json");
                return false;
            });
            if (result instanceof Map map) return (Map<ResourceLocation, Resource>) map;
        } catch (Throwable ignored) { }

        try {
            var m = rm.getClass().getMethod("findResources", String.class, java.util.function.Predicate.class);
            Object result = m.invoke(rm, "models", (java.util.function.Predicate) (Object o) -> {
                if (o instanceof ResourceLocation rl) return rl.getPath().endsWith(".json");
                return false;
            });
            if (result instanceof Map map) return (Map<ResourceLocation, Resource>) map;
        } catch (Throwable ignored) { }

        return Map.of();
    }
}
//?}

