package com.hbm_m.world.gen;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

/**
 * Port-Hilfe: die 1.7.10-Biomeigenschaften, nach denen die Original-Weltgenerierung entscheidet
 * ({@code rootHeight}, {@code heightVariation}, {@code temperature}, {@code rainfall}, Regen/Schnee,
 * {@code BiomeDictionary}-Typen und die Biomklasse fuer {@code instanceof}-Abfragen).
 *
 * <p>1.20 kennt Hoehe/Hoehenvarianz nicht mehr als Biomwert; jedes 1.20-Biom wird deshalb seinem
 * 1.7.10-Gegenstueck zugeordnet und uebernimmt dessen Werte (Originalzahlen aus {@code BiomeGenBase}
 * bzw. Forge {@code BiomeDictionary.registerVanillaBiomes}). Fremde Biome werden aus Temperatur,
 * Niederschlag und Tags abgeleitet.</p>
 */
public final class LegacyBiome {

    public enum Type {
        HOT, COLD, SPARSE, DENSE, WET, DRY, SAVANNA, CONIFEROUS, JUNGLE, SPOOKY, DEAD, LUSH, NETHER, END,
        MUSHROOM, MAGICAL, OCEAN, RIVER, MESA, FOREST, PLAINS, MOUNTAIN, HILLS, SWAMP, SANDY, SNOWY,
        WASTELAND, BEACH, RARE,
        /** Forge 1.7.10: WATER = OCEAN oder RIVER. */
        WATER
    }

    /** 1.7.10-Biomklasse fuer {@code instanceof}-Abfragen. */
    public enum Kind { PLAINS, DESERT, HILLS, FOREST, TAIGA, SWAMP, RIVER, OCEAN, SNOW, MUSHROOM, BEACH, STONE_BEACH, JUNGLE, SAVANNA, MESA, OTHER }

    /** Name des 1.7.10-Gegenstuecks ({@code BiomeGenBase.plains} -> "plains"). */
    public final String name;
    public final float rootHeight;
    public final float heightVariation;
    public final float temperature;
    public final float rainfall;
    public final boolean enableRain;
    public final boolean enableSnow;
    public final Set<Type> types;
    public final Kind kind;

    private LegacyBiome(String name, float root, float var, float temp, float rain, boolean enableRain, boolean enableSnow, Kind kind, Type... types) {
        this.name = name;
        this.rootHeight = root;
        this.heightVariation = var;
        this.temperature = temp;
        this.rainfall = rain;
        this.enableRain = enableRain;
        this.enableSnow = enableSnow;
        this.kind = kind;
        this.types = types.length == 0 ? EnumSet.noneOf(Type.class) : EnumSet.of(types[0], types);
    }

    /** Original {@code canSpawnLightningBolt}: Schnee schliesst Gewitter aus, sonst Regen. */
    public boolean canSpawnLightningBolt() {
        return enableSnow ? false : enableRain;
    }

    /** Forge {@code BiomeDictionary.isBiomeOfType}. */
    public boolean isOfType(Type type) {
        if (type == Type.WATER) return types.contains(Type.OCEAN) || types.contains(Type.RIVER);
        return types.contains(type);
    }

    /** 1.7.10 {@code topBlock}. */
    public net.minecraft.world.level.block.state.BlockState topBlock() {
        switch (kind) {
            case DESERT: case BEACH: return net.minecraft.world.level.block.Blocks.SAND.defaultBlockState();
            case STONE_BEACH: return net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
            case MESA: return net.minecraft.world.level.block.Blocks.RED_SAND.defaultBlockState();
            case MUSHROOM: return net.minecraft.world.level.block.Blocks.MYCELIUM.defaultBlockState();
            default: return net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState();
        }
    }

    /** 1.7.10 {@code fillerBlock}. */
    public net.minecraft.world.level.block.state.BlockState fillerBlock() {
        switch (kind) {
            case DESERT: case BEACH: return net.minecraft.world.level.block.Blocks.SAND.defaultBlockState();
            case STONE_BEACH: return net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
            case MESA: return net.minecraft.world.level.block.Blocks.ORANGE_TERRACOTTA.defaultBlockState();
            default: return net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState();
        }
    }

    public boolean is(String legacyName) {
        return name.equals(legacyName);
    }

    private static final Map<String, LegacyBiome> BY_KEY = new HashMap<>();

    private static void put(String key, LegacyBiome b) {
        BY_KEY.put("minecraft:" + key, b);
    }

    static {
        LegacyBiome plains = new LegacyBiome("plains", 0.125F, 0.05F, 0.8F, 0.4F, true, false, Kind.PLAINS, Type.PLAINS);
        put("plains", plains);
        put("sunflower_plains", new LegacyBiome("sunflower_plains", 0.225F, 0.25F, 0.8F, 0.4F, true, false, Kind.PLAINS, Type.PLAINS));
        put("meadow", new LegacyBiome("meadow", 0.45F, 0.05F, 0.5F, 0.8F, true, false, Kind.PLAINS, Type.PLAINS));
        put("snowy_plains", new LegacyBiome("icePlains", 0.125F, 0.05F, 0.0F, 0.5F, true, true, Kind.SNOW, Type.COLD, Type.SNOWY, Type.WASTELAND));
        put("ice_spikes", new LegacyBiome("icePlainsSpikes", 0.425F, 0.45F, 0.0F, 0.5F, true, true, Kind.SNOW, Type.COLD, Type.SNOWY, Type.WASTELAND));
        put("desert", new LegacyBiome("desert", 0.125F, 0.05F, 2.0F, 0.0F, false, false, Kind.DESERT, Type.HOT, Type.DRY, Type.SANDY));
        put("windswept_hills", new LegacyBiome("extremeHills", 1.0F, 0.5F, 0.2F, 0.3F, true, false, Kind.HILLS, Type.MOUNTAIN, Type.HILLS));
        put("windswept_gravelly_hills", new LegacyBiome("extremeHillsM", 1.1F, 0.7F, 0.2F, 0.3F, true, false, Kind.HILLS, Type.MOUNTAIN, Type.HILLS));
        put("windswept_forest", new LegacyBiome("extremeHillsPlus", 1.0F, 0.5F, 0.2F, 0.3F, true, false, Kind.HILLS, Type.MOUNTAIN, Type.FOREST, Type.SPARSE));
        put("stony_peaks", new LegacyBiome("extremeHills", 1.0F, 0.5F, 0.2F, 0.3F, true, false, Kind.HILLS, Type.MOUNTAIN, Type.HILLS));
        put("forest", new LegacyBiome("forest", 0.1F, 0.2F, 0.7F, 0.8F, true, false, Kind.FOREST, Type.FOREST));
        put("flower_forest", new LegacyBiome("flowerForest", 0.2F, 0.4F, 0.7F, 0.8F, true, false, Kind.FOREST, Type.FOREST));
        put("cherry_grove", new LegacyBiome("forestHills", 0.45F, 0.3F, 0.7F, 0.8F, true, false, Kind.FOREST, Type.FOREST, Type.HILLS));
        put("birch_forest", new LegacyBiome("birchForest", 0.1F, 0.2F, 0.6F, 0.6F, true, false, Kind.FOREST, Type.FOREST));
        put("old_growth_birch_forest", new LegacyBiome("birchForestM", 0.2F, 0.4F, 0.6F, 0.6F, true, false, Kind.FOREST, Type.FOREST));
        put("dark_forest", new LegacyBiome("roofedForest", 0.1F, 0.2F, 0.7F, 0.8F, true, false, Kind.FOREST, Type.SPOOKY, Type.DENSE, Type.FOREST));
        put("taiga", new LegacyBiome("taiga", 0.2F, 0.2F, 0.25F, 0.8F, true, false, Kind.TAIGA, Type.COLD, Type.CONIFEROUS, Type.FOREST));
        put("old_growth_pine_taiga", new LegacyBiome("megaTaiga", 0.2F, 0.2F, 0.3F, 0.8F, true, false, Kind.TAIGA, Type.COLD, Type.CONIFEROUS, Type.FOREST));
        put("old_growth_spruce_taiga", new LegacyBiome("megaSpruceTaiga", 0.3F, 0.4F, 0.25F, 0.8F, true, false, Kind.TAIGA, Type.COLD, Type.CONIFEROUS, Type.FOREST));
        put("snowy_taiga", new LegacyBiome("coldTaiga", 0.2F, 0.2F, -0.5F, 0.4F, true, true, Kind.TAIGA, Type.COLD, Type.CONIFEROUS, Type.FOREST, Type.SNOWY));
        put("grove", new LegacyBiome("coldTaigaHills", 0.45F, 0.3F, -0.5F, 0.4F, true, true, Kind.TAIGA, Type.COLD, Type.CONIFEROUS, Type.FOREST, Type.SNOWY, Type.HILLS));
        put("snowy_slopes", new LegacyBiome("iceMountains", 0.45F, 0.3F, 0.0F, 0.5F, true, true, Kind.SNOW, Type.COLD, Type.SNOWY, Type.MOUNTAIN));
        put("frozen_peaks", new LegacyBiome("iceMountains", 0.45F, 0.3F, 0.0F, 0.5F, true, true, Kind.SNOW, Type.COLD, Type.SNOWY, Type.MOUNTAIN));
        put("jagged_peaks", new LegacyBiome("iceMountains", 0.45F, 0.3F, 0.0F, 0.5F, true, true, Kind.SNOW, Type.COLD, Type.SNOWY, Type.MOUNTAIN));
        LegacyBiome swamp = new LegacyBiome("swampland", -0.2F, 0.1F, 0.8F, 0.9F, true, false, Kind.SWAMP, Type.WET, Type.SWAMP);
        put("swamp", swamp);
        put("mangrove_swamp", swamp);
        put("river", new LegacyBiome("river", -0.5F, 0.0F, 0.5F, 0.5F, true, false, Kind.RIVER, Type.RIVER));
        put("frozen_river", new LegacyBiome("frozenRiver", -0.5F, 0.0F, 0.0F, 0.5F, true, true, Kind.RIVER, Type.COLD, Type.RIVER, Type.SNOWY));
        LegacyBiome ocean = new LegacyBiome("ocean", -1.0F, 0.1F, 0.5F, 0.5F, true, false, Kind.OCEAN, Type.OCEAN);
        LegacyBiome deep = new LegacyBiome("deepOcean", -1.8F, 0.1F, 0.5F, 0.5F, true, false, Kind.OCEAN, Type.OCEAN);
        put("ocean", ocean); put("lukewarm_ocean", ocean); put("warm_ocean", ocean); put("cold_ocean", ocean);
        put("deep_ocean", deep); put("deep_lukewarm_ocean", deep); put("deep_cold_ocean", deep);
        put("frozen_ocean", new LegacyBiome("frozenOcean", -1.0F, 0.1F, 0.0F, 0.5F, true, true, Kind.OCEAN, Type.COLD, Type.OCEAN, Type.SNOWY));
        put("deep_frozen_ocean", new LegacyBiome("frozenOcean", -1.8F, 0.1F, 0.0F, 0.5F, true, true, Kind.OCEAN, Type.COLD, Type.OCEAN, Type.SNOWY));
        put("mushroom_fields", new LegacyBiome("mushroomIsland", 0.2F, 0.3F, 0.9F, 1.0F, true, false, Kind.MUSHROOM, Type.MUSHROOM, Type.RARE));
        put("beach", new LegacyBiome("beach", 0.0F, 0.025F, 0.8F, 0.4F, true, false, Kind.BEACH, Type.BEACH));
        put("snowy_beach", new LegacyBiome("coldBeach", 0.0F, 0.025F, 0.05F, 0.3F, true, true, Kind.BEACH, Type.COLD, Type.BEACH, Type.SNOWY));
        put("stony_shore", new LegacyBiome("stoneBeach", 0.1F, 0.8F, 0.2F, 0.3F, true, false, Kind.STONE_BEACH, Type.BEACH));
        LegacyBiome jungle = new LegacyBiome("jungle", 0.1F, 0.2F, 0.95F, 0.9F, true, false, Kind.JUNGLE, Type.HOT, Type.WET, Type.DENSE, Type.JUNGLE);
        put("jungle", jungle); put("bamboo_jungle", jungle);
        put("sparse_jungle", new LegacyBiome("jungleEdge", 0.1F, 0.2F, 0.95F, 0.8F, true, false, Kind.JUNGLE, Type.HOT, Type.WET, Type.JUNGLE, Type.FOREST));
        put("savanna", new LegacyBiome("savanna", 0.125F, 0.05F, 1.2F, 0.0F, false, false, Kind.SAVANNA, Type.HOT, Type.SAVANNA, Type.PLAINS, Type.SPARSE));
        put("savanna_plateau", new LegacyBiome("savannaPlateau", 1.5F, 0.025F, 1.0F, 0.0F, false, false, Kind.SAVANNA, Type.HOT, Type.SAVANNA, Type.PLAINS, Type.SPARSE));
        put("windswept_savanna", new LegacyBiome("savannaM", 0.3625F, 1.225F, 1.1F, 0.0F, false, false, Kind.SAVANNA, Type.HOT, Type.SAVANNA, Type.PLAINS, Type.SPARSE));
        put("badlands", new LegacyBiome("mesa", 0.1F, 0.2F, 2.0F, 0.0F, false, false, Kind.MESA, Type.MESA, Type.SANDY));
        put("wooded_badlands", new LegacyBiome("mesaPlateau_F", 1.5F, 0.025F, 2.0F, 0.0F, false, false, Kind.MESA, Type.MESA, Type.SPARSE, Type.SANDY));
        put("eroded_badlands", new LegacyBiome("mesaBryce", 0.2F, 0.4F, 2.0F, 0.0F, false, false, Kind.MESA, Type.MESA, Type.SANDY));
        // Hoehlenbiome liegen nie an der Oberflaeche - wie Wald behandeln
        put("lush_caves", new LegacyBiome("forest", 0.1F, 0.2F, 0.7F, 0.8F, true, false, Kind.FOREST, Type.FOREST));
        put("dripstone_caves", new LegacyBiome("extremeHills", 1.0F, 0.5F, 0.2F, 0.3F, true, false, Kind.HILLS, Type.MOUNTAIN, Type.HILLS));
        put("deep_dark", new LegacyBiome("extremeHills", 1.0F, 0.5F, 0.2F, 0.3F, true, false, Kind.HILLS, Type.MOUNTAIN, Type.HILLS));
    }

    private static final Map<Holder<Biome>, LegacyBiome> DERIVED = new java.util.concurrent.ConcurrentHashMap<>();

    public static LegacyBiome of(Holder<Biome> biome) {
        String key = biome.unwrapKey().map(ResourceKey::location).map(Object::toString).orElse("");
        LegacyBiome b = BY_KEY.get(key);
        if (b != null) return b;
        return DERIVED.computeIfAbsent(biome, LegacyBiome::derive);
    }

    /** Fremde Biome: aus 1.20-Werten und Tags naeherungsweise abgeleitet. */
    private static LegacyBiome derive(Holder<Biome> holder) {
        Biome biome = holder.value();
        float temp = biome.getBaseTemperature();
        boolean rain = biome.hasPrecipitation();
        boolean snow = rain && temp < 0.15F;
        Set<Type> t = EnumSet.noneOf(Type.class);
        Kind kind = Kind.OTHER;
        float root = 0.1F, var = 0.2F;
        if (holder.is(BiomeTags.IS_OCEAN)) { t.add(Type.OCEAN); kind = Kind.OCEAN; root = holder.is(BiomeTags.IS_DEEP_OCEAN) ? -1.8F : -1.0F; var = 0.1F; }
        if (holder.is(BiomeTags.IS_RIVER)) { t.add(Type.RIVER); kind = Kind.RIVER; root = -0.5F; var = 0.0F; }
        if (holder.is(BiomeTags.IS_BEACH)) { t.add(Type.BEACH); kind = Kind.BEACH; root = 0.0F; var = 0.025F; }
        if (holder.is(BiomeTags.IS_FOREST)) { t.add(Type.FOREST); kind = Kind.FOREST; }
        if (holder.is(BiomeTags.IS_JUNGLE)) { t.add(Type.JUNGLE); kind = Kind.JUNGLE; }
        if (holder.is(BiomeTags.IS_SAVANNA)) { t.add(Type.SAVANNA); t.add(Type.SPARSE); kind = Kind.SAVANNA; root = 0.125F; var = 0.05F; }
        if (holder.is(BiomeTags.IS_BADLANDS)) { t.add(Type.MESA); t.add(Type.SANDY); kind = Kind.MESA; }
        if (holder.is(BiomeTags.IS_MOUNTAIN) || holder.is(BiomeTags.IS_HILL)) { t.add(Type.MOUNTAIN); t.add(Type.HILLS); kind = Kind.HILLS; root = 1.0F; var = 0.5F; }
        if (holder.is(BiomeTags.IS_TAIGA)) { t.add(Type.CONIFEROUS); t.add(Type.FOREST); kind = Kind.TAIGA; }
        if (temp >= 1.5F) t.add(Type.HOT);
        if (temp < 0.3F) t.add(Type.COLD);
        if (snow) t.add(Type.SNOWY);
        if (!rain) t.add(Type.DRY);
        return new LegacyBiome(key(holder), root, var, temp, rain ? 0.5F : 0.0F, rain, snow, kind, t.toArray(new Type[0]));
    }

    private static String key(Holder<Biome> holder) {
        return holder.unwrapKey().map(ResourceKey::location).map(Object::toString).orElse("unknown");
    }
}
