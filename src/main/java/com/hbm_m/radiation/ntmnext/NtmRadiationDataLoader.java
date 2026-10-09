// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
//
// Ersatz fuer die Datapack-Registries des Originals (hbm:radiation_config, hbm:radiation_settings,
// hbm:rad_diffusivity, hbm:rad_source; RadiationData.applyDataPack,
// RadiationSystemNT.registerConstantSources, RadiationDiffusivity.resolveFor), Commit 3f9a261a.
// 1.20.1 erlaubt keine eigenen Datapack-Registries ohne Loader-spezifischen Code; der Port liest
// dieselben JSON-Formate beim Serverstart direkt aus dem ResourceManager.
//
// Dateien (alle optional, Datapacks koennen sie ueberschreiben/ergaenzen):
//   data/hbm_m/ntm_radiation/config.json                 globale Parameter
//   data/<dim-ns>/ntm_radiation/settings/<dim-path>.json  je Dimension
//   data/<ns>/ntm_radiation/shielding/*.json              {"blocks":[..], "hvl":{..}} (Stufe 2)
//   data/<ns>/ntm_radiation/source_mix/*.json             {"blocks":[..], "mix":{..}} (Stufe 2)
//   data/<ns>/ntm_radiation/resistant/*.json              {"blocks":[..], "state":{..}}
//   data/<ns>/ntm_radiation/sources/*.json                {"targets":..,"state":..,"rate":{..}}
// Block-Eintraege: "ns:id", "#ns:tag" oder Muster mit * ; unbekannte IDs werden uebersprungen.

package com.hbm_m.radiation.ntmnext;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm_m.main.MainRegistry;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Liest die JSON-Daten des NTM-Next-Strahlungssystems beim Serverstart. */
final class NtmRadiationDataLoader {

    private static final String ROOT = "ntm_radiation";

    private NtmRadiationDataLoader() {}

    static void load(MinecraftServer server) {
        ResourceManager rm = server.getResourceManager();

        NtmRadiationConfig.resetDefaults();
        RadiationSettings.BY_DIMENSION.clear();
        RadiationShieldingTable.clear();
        SourceMixTable.clear();
        RadiationShielding.clear();
        RadiationSources.clear();

        rm.getResource(ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, ROOT + "/config.json"))
                .ifPresent(r -> readJson(r, "config", NtmRadiationDataLoader::applyConfig));

        forEachJson(
                rm,
                ROOT + "/settings",
                (id, json) -> {
                    ResourceLocation dim = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath());
                    RadiationSettings.BY_DIMENSION.put(dim, parseSettings(json));
                });

        // Stufe 2: Halbwertsschichten je Art ({"blocks":[..], "hvl":{"gamma":..}, "priority":n}).
        int[] shieldEntries = {0};
        forEachJson(
                rm,
                ROOT + "/shielding",
                (id, json) -> {
                    JsonObject hvlJson = json.getAsJsonObject("hvl");
                    if (hvlJson == null) return;
                    float[] hvl = {Float.NaN, Float.NaN, Float.NaN};
                    for (RadiationType type : RadiationType.FIELD_TYPES) {
                        if (!hvlJson.has(type.id)) continue;
                        float v = hvlJson.get(type.id).getAsFloat();
                        // 0 oder negativ = keine Abschirmung fuer diese Art.
                        hvl[type.ordinal()] = v > 0.0F ? v : RadiationShieldingTable.INF;
                    }
                    int priority = json.has("priority") ? json.get("priority").getAsInt() : 0;
                    for (Block block : parseBlocks(json.get("blocks"))) {
                        RadiationShieldingTable.put(block, hvl, priority);
                        shieldEntries[0]++;
                    }
                });
        RadiationShieldingTable.bake();

        // Stufe 2: Strahlungsarten-Mix je Quellblock ({"blocks":[..], "mix":{..}, "priority":n}).
        forEachJson(
                rm,
                ROOT + "/source_mix",
                (id, json) -> {
                    float[] mix = parseMix(json.getAsJsonObject("mix"));
                    if (mix == null) return;
                    int priority = json.has("priority") ? json.get("priority").getAsInt() : 0;
                    float[] contam = parseGroups(json.getAsJsonObject("contamination"));
                    for (Block block : parseBlocks(json.get("blocks"))) {
                        SourceMixTable.put(block, mix, priority);
                        if (contam != null) SourceMixTable.putContamination(block, contam);
                    }
                });

        int[] resistantBlocks = {0};
        forEachJson(
                rm,
                ROOT + "/resistant",
                (id, json) -> {
                    Optional<StatePropertiesPredicate> state = parseState(json);
                    for (Block block : parseBlocks(json.get("blocks"))) {
                        if (state.isPresent()) {
                            StatePropertiesPredicate p = state.get();
                            RadiationShielding.markRadResistant(block, p::matches);
                        } else {
                            RadiationShielding.markRadResistant(block);
                        }
                        resistantBlocks[0]++;
                    }
                });

        List<RadSourceRule> rules = new ArrayList<>();
        forEachJson(
                rm,
                ROOT + "/sources",
                (id, json) -> {
                    RadSourceRule.Rate rate = parseRate(id, json.getAsJsonObject("rate"));
                    if (rate == null) return;
                    rules.add(
                            new RadSourceRule(
                                    parseBlocks(json.get("targets")), parseState(json), rate));
                });
        RadiationSources.registerConstantSources(rules);

        MainRegistry.LOGGER.info(
                "[NtmRadiation] loaded: diffusivity={} halfLife={}s fogRad={} fogChance={} tickRate={}, "
                        + "{} dimension settings, {} shielding entries, {} source mixes, {} resistant blocks",
                NtmRadiationConfig.diffusivity,
                NtmRadiationConfig.halfLifeSeconds,
                NtmRadiationConfig.fogRad,
                NtmRadiationConfig.fogChance,
                NtmRadiationConfig.radTickRate,
                RadiationSettings.BY_DIMENSION.size(),
                shieldEntries[0],
                SourceMixTable.size(),
                resistantBlocks[0]);
    }

    // ------------------------------------------------------------------

    private static void applyConfig(JsonObject json) {
        NtmRadiationConfig.fogRad = getDouble(json, "fog_rad", NtmRadiationConfig.fogRad);
        NtmRadiationConfig.fogChance =
                Math.max(1D, getDouble(json, "fog_chance", NtmRadiationConfig.fogChance));
        NtmRadiationConfig.worldRadEffects =
                getBool(json, "world_rad_effects", NtmRadiationConfig.worldRadEffects);
        NtmRadiationConfig.enableChunkRads =
                getBool(json, "enable_chunk_rads", NtmRadiationConfig.enableChunkRads);
        NtmRadiationConfig.diffusivity =
                Math.max(0.000001D, getDouble(json, "diffusivity", NtmRadiationConfig.diffusivity));
        NtmRadiationConfig.halfLifeSeconds =
                Math.max(
                        0.000001D,
                        getDouble(json, "half_life_seconds", NtmRadiationConfig.halfLifeSeconds));
        NtmRadiationConfig.radTickRate =
                Math.max(1, (int) getDouble(json, "rad_tick_rate", NtmRadiationConfig.radTickRate));

        // Stufe 2
        if (json.has("types")) {
            JsonObject types = json.getAsJsonObject("types");
            for (RadiationType type : RadiationType.FIELD_TYPES) {
                if (!types.has(type.id)) continue;
                JsonObject t = types.getAsJsonObject(type.id);
                int i = type.ordinal();
                NtmRadiationConfig.typeWeight[i] =
                        Math.max(0.0D, getDouble(t, "weight", NtmRadiationConfig.typeWeight[i]));
                NtmRadiationConfig.halfLifeMult[i] =
                        Math.max(1.0e-3D, getDouble(t, "half_life_mult", NtmRadiationConfig.halfLifeMult[i]));
                NtmRadiationConfig.diffusivityMult[i] =
                        Math.max(1.0e-3D, getDouble(t, "diffusivity_mult", NtmRadiationConfig.diffusivityMult[i]));
                NtmRadiationConfig.hvlScale[i] =
                        Math.max(1.0e-3D, getDouble(t, "hvl_scale", NtmRadiationConfig.hvlScale[i]));
            }
        }
        if (json.has("default_mix")) {
            float[] mix = parseMix(json.getAsJsonObject("default_mix"));
            if (mix != null) {
                float[] norm = SourceMixTable.normalize(mix);
                System.arraycopy(norm, 0, NtmRadiationConfig.defaultMix, 0, norm.length);
            }
        }
        NtmRadiationConfig.effectShare =
                Math.max(0.01D, getDouble(json, "effect_share", NtmRadiationConfig.effectShare));
        NtmRadiationConfig.wallScale =
                Math.max(1.0D, getDouble(json, "wall_length_scale", NtmRadiationConfig.wallScale));
        if (json.has("contamination")) {
            JsonObject c = json.getAsJsonObject("contamination");
            NtmRadiationConfig.contaminationEnabled =
                    getBool(c, "enabled", NtmRadiationConfig.contaminationEnabled);
            NtmRadiationConfig.oneOffContaminationFraction =
                    Math.max(0.0D, Math.min(1.0D, getDouble(c, "one_off_fraction",
                            NtmRadiationConfig.oneOffContaminationFraction)));
            NtmRadiationConfig.deconFactor =
                    Math.max(0.0D, getDouble(c, "decon_factor", NtmRadiationConfig.deconFactor));
            NtmRadiationConfig.contamTickInterval =
                    Math.max(1, (int) getDouble(c, "tick_interval", NtmRadiationConfig.contamTickInterval));
            float[] split = parseGroups(c.getAsJsonObject("fallout_split"));
            if (split != null) {
                float sum = 0.0F;
                for (float v : split) sum += Math.max(0.0F, v);
                if (sum > 0.0F)
                    for (int i = 0; i < split.length; i++)
                        NtmRadiationConfig.falloutSplit[i] = Math.max(0.0F, split[i]) / sum;
            }
            if (c.has("groups")) {
                JsonObject groups = c.getAsJsonObject("groups");
                for (ContaminationGroup g : ContaminationGroup.values()) {
                    if (!groups.has(g.id)) continue;
                    JsonObject gj = groups.getAsJsonObject(g.id);
                    int i = g.ordinal();
                    NtmRadiationConfig.contamHalfLife[i] = Math.max(1.0D,
                            getDouble(gj, "half_life_seconds", NtmRadiationConfig.contamHalfLife[i]));
                    NtmRadiationConfig.contamEmission[i] = Math.max(0.0D,
                            getDouble(gj, "emission_per_unit", NtmRadiationConfig.contamEmission[i]));
                    float[] mix = parseMix(gj.getAsJsonObject("mix"));
                    if (mix != null) {
                        float[] norm = SourceMixTable.normalize(mix);
                        System.arraycopy(norm, 0, NtmRadiationConfig.contamMix[i], 0, norm.length);
                    }
                }
            }
        }
        if (json.has("weather")) {
            JsonObject w = json.getAsJsonObject("weather");
            NtmRadiationConfig.airborneFraction = clamp01(getDouble(w, "airborne_fraction", NtmRadiationConfig.airborneFraction));
            NtmRadiationConfig.settleRate = Math.max(0.0D, getDouble(w, "settle_rate", NtmRadiationConfig.settleRate));
            if (w.has("wind")) {
                JsonObject o = w.getAsJsonObject("wind");
                NtmRadiationConfig.windEnabled = getBool(o, "enabled", NtmRadiationConfig.windEnabled);
                NtmRadiationConfig.windBaseSpeed = Math.max(0.0D, getDouble(o, "base_speed", NtmRadiationConfig.windBaseSpeed));
                NtmRadiationConfig.windSpeedVariation = Math.max(0.0D, getDouble(o, "speed_variation", NtmRadiationConfig.windSpeedVariation));
                NtmRadiationConfig.windDirectionPeriod = Math.max(1.0D, getDouble(o, "direction_period_seconds", NtmRadiationConfig.windDirectionPeriod));
                NtmRadiationConfig.gustStrength = Math.max(0.0D, getDouble(o, "gust_strength", NtmRadiationConfig.gustStrength));
                NtmRadiationConfig.gustPeriod = Math.max(1.0D, getDouble(o, "gust_period_seconds", NtmRadiationConfig.gustPeriod));
                NtmRadiationConfig.windMaxFraction = clamp01(getDouble(o, "max_fraction_per_step", NtmRadiationConfig.windMaxFraction));
            }
            if (w.has("rain")) {
                JsonObject o = w.getAsJsonObject("rain");
                NtmRadiationConfig.rainEnabled = getBool(o, "enabled", NtmRadiationConfig.rainEnabled);
                NtmRadiationConfig.washoutRate = Math.max(0.0D, getDouble(o, "washout_rate", NtmRadiationConfig.washoutRate));
                NtmRadiationConfig.snowWashoutRate = Math.max(0.0D, getDouble(o, "snow_washout_rate", NtmRadiationConfig.snowWashoutRate));
                NtmRadiationConfig.thunderMult = Math.max(0.0D, getDouble(o, "thunder_mult", NtmRadiationConfig.thunderMult));
                NtmRadiationConfig.runoffEnabled = getBool(o, "runoff_enabled", NtmRadiationConfig.runoffEnabled);
                NtmRadiationConfig.runoffRate = Math.max(0.0D, getDouble(o, "runoff_rate", NtmRadiationConfig.runoffRate));
                NtmRadiationConfig.runoffMinDrop = Math.max(1, (int) getDouble(o, "runoff_min_drop", NtmRadiationConfig.runoffMinDrop));
            }
            if (w.has("water")) {
                JsonObject o = w.getAsJsonObject("water");
                NtmRadiationConfig.waterEnabled = getBool(o, "enabled", NtmRadiationConfig.waterEnabled);
                NtmRadiationConfig.waterMinDepth = Math.max(1, (int) getDouble(o, "min_depth", NtmRadiationConfig.waterMinDepth));
                NtmRadiationConfig.waterMixRate = Math.max(0.0D, getDouble(o, "mix_rate", NtmRadiationConfig.waterMixRate));
                NtmRadiationConfig.oceanDilutionHalfLife = Math.max(1.0D, getDouble(o, "ocean_dilution_half_life_seconds", NtmRadiationConfig.oceanDilutionHalfLife));
            }
        }
        if (json.has("near_field")) {
            JsonObject n = json.getAsJsonObject("near_field");
            NtmRadiationConfig.nearEnabled = getBool(n, "enabled", NtmRadiationConfig.nearEnabled);
            NtmRadiationConfig.nearRadius =
                    Math.max(1, Math.min(16, (int) getDouble(n, "radius", NtmRadiationConfig.nearRadius)));
            NtmRadiationConfig.nearGain = Math.max(0.0D, getDouble(n, "gain", NtmRadiationConfig.nearGain));
            NtmRadiationConfig.nearMaxEmitters =
                    Math.max(1, (int) getDouble(n, "max_emitters", NtmRadiationConfig.nearMaxEmitters));
            NtmRadiationConfig.nearCacheTicks =
                    Math.max(1, (int) getDouble(n, "cache_ticks", NtmRadiationConfig.nearCacheTicks));
        }
    }

    /** {"short":..,"medium":..,"long":..,"exotic":..} -> Array (nicht normiert). */
    private static float[] parseGroups(JsonObject json) {
        if (json == null) return null;
        float[] out = new float[ContaminationGroup.COUNT];
        for (ContaminationGroup g : ContaminationGroup.values()) {
            if (json.has(g.id)) out[g.ordinal()] = json.get(g.id).getAsFloat();
        }
        return out;
    }

    /** {"gamma":..,"neutron":..,"beta":..,"alpha":..} -> Array (nicht normiert). */
    private static float[] parseMix(JsonObject json) {
        if (json == null) return null;
        float[] mix = new float[RadiationType.MIX_COUNT];
        for (RadiationType type : RadiationType.values()) {
            if (json.has(type.id)) mix[type.ordinal()] = json.get(type.id).getAsFloat();
        }
        return mix;
    }

    private static RadiationSettings parseSettings(JsonObject json) {
        return new RadiationSettings(
                optDouble(json, "diffusivity", 1.0e-6D),
                optDouble(json, "half_life_seconds", 1.0e-6D),
                optDouble(json, "fog_rad", 0.0D),
                optDouble(json, "fog_chance", 1.0D),
                json.has("world_rad_effects")
                        ? Optional.of(json.get("world_rad_effects").getAsBoolean())
                        : Optional.empty(),
                optDouble(json, "ambient_rad", 0.0D),
                json.has("diffusivity_transport")
                        ? Optional.of(json.get("diffusivity_transport").getAsBoolean())
                        : Optional.empty());
    }

    private static RadSourceRule.Rate parseRate(ResourceLocation id, JsonObject rate) {
        if (rate == null) {
            MainRegistry.LOGGER.warn("[NtmRadiation] source rule {} has no rate, skipped", id);
            return null;
        }
        String type = rate.has("type") ? rate.get("type").getAsString() : "";
        switch (type) {
            case "fixed":
                return new RadSourceRule.Rate.Fixed(
                        rate.get("emission").getAsDouble(), getDouble(rate, "saturation", 0.0D));
            case "item_hazard":
                return new RadSourceRule.Rate.ItemHazard();
            default:
                MainRegistry.LOGGER.warn(
                        "[NtmRadiation] unknown rad source rate {} in {}, known: [fixed, item_hazard]",
                        type,
                        id);
                return null;
        }
    }

    private static Optional<StatePropertiesPredicate> parseState(JsonObject json) {
        if (!json.has("state")) return Optional.empty();
        //? if < 1.21.1 {
        return Optional.of(StatePropertiesPredicate.fromJson(json.get("state")));
        //?} else {
        /*return Optional.of(StatePropertiesPredicate.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json.get("state")).getOrThrow(com.google.gson.JsonParseException::new));
        *///?}
    }

    /** "ns:id", "#ns:tag" oder eine Liste davon. */
    static List<Block> parseBlocks(JsonElement element) {
        List<Block> out = new ArrayList<>();
        if (element == null) return out;
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement e : array) addBlocks(e.getAsString(), out);
        } else {
            addBlocks(element.getAsString(), out);
        }
        return out;
    }

    private static void addBlocks(String entry, List<Block> out) {
        if (entry.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(entry.substring(1));
            if (tagId == null) return;
            BuiltInRegistries.BLOCK
                    .getTag(TagKey.create(Registries.BLOCK, tagId))
                    .ifPresent(set -> set.forEach((Holder<Block> h) -> out.add(h.value())));
            return;
        }
        if (entry.indexOf('*') >= 0) {
            // Muster, z. B. "hbm_m:rbmk_*" oder "hbm_m:*fallout*".
            java.util.regex.Pattern p =
                    java.util.regex.Pattern.compile(
                            java.util.Arrays.stream(entry.split("\\*", -1))
                                    .map(java.util.regex.Pattern::quote)
                                    .collect(java.util.stream.Collectors.joining(".*")));
            for (ResourceLocation key : BuiltInRegistries.BLOCK.keySet()) {
                if (p.matcher(key.toString()).matches()) out.add(BuiltInRegistries.BLOCK.get(key));
            }
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(entry);
        if (id == null) return;
        BuiltInRegistries.BLOCK.getOptional(id).ifPresent(out::add);
    }

    private interface JsonHandler {
        void accept(ResourceLocation id, JsonObject json);
    }

    /** Alle JSON-Dateien unter {@code dir}; id = Namespace + Pfad relativ zu dir, ohne Endung. */
    private static void forEachJson(ResourceManager rm, String dir, JsonHandler handler) {
        Map<ResourceLocation, Resource> found =
                rm.listResources(dir, rl -> rl.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> e : found.entrySet()) {
            ResourceLocation file = e.getKey();
            String path = file.getPath();
            String rel = path.substring(dir.length() + 1, path.length() - ".json".length());
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(file.getNamespace(), rel);
            readJson(e.getValue(), file.toString(), json -> handler.accept(id, json));
        }
    }

    private static void readJson(Resource resource, String name, Consumer<JsonObject> consumer) {
        try (Reader reader = resource.openAsReader()) {
            consumer.accept(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (Exception ex) {
            MainRegistry.LOGGER.error("[NtmRadiation] Failed to read {}", name, ex);
        }
    }

    private static double clamp01(double v) {
        return Math.max(0.0D, Math.min(1.0D, v));
    }

    private static double getDouble(JsonObject json, String key, double fallback) {
        return json.has(key) ? json.get(key).getAsDouble() : fallback;
    }

    private static boolean getBool(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    private static Optional<Double> optDouble(JsonObject json, String key, double min) {
        if (!json.has(key)) return Optional.empty();
        double v = json.get(key).getAsDouble();
        return v >= min && Double.isFinite(v) ? Optional.of(v) : Optional.empty();
    }
}
