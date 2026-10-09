//? if forge {
package com.hbm_m.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

/**
 * Forge 1.20.1 kennt {@code forge:add_carvers} noch nicht (erst NeoForge) - ohne diesen Typ bricht
 * {@code data/hbm_m/forge/biome_modifier/add_ntm_terrain.json} das Laden der Welt-Registries ab
 * (Server startet nicht). Gleiches JSON-Format wie NeoForges {@code add_carvers}:
 * {@code {"type": "hbm_m:add_carvers", "biomes": .., "carvers": .., "step": "air"|"liquid"}}.
 * Auf NeoForge schreibt processResources den Typ auf {@code neoforge:add_carvers} um.
 */
public record AddCarversBiomeModifierForge(HolderSet<Biome> biomes, HolderSet<ConfiguredWorldCarver<?>> carvers,
                                          GenerationStep.Carving step) implements BiomeModifier {

    public static final Codec<AddCarversBiomeModifierForge> CODEC = RecordCodecBuilder.create(b -> b.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(AddCarversBiomeModifierForge::biomes),
            ConfiguredWorldCarver.LIST_CODEC.fieldOf("carvers").forGetter(AddCarversBiomeModifierForge::carvers),
            GenerationStep.Carving.CODEC.fieldOf("step").forGetter(AddCarversBiomeModifierForge::step)
    ).apply(b, AddCarversBiomeModifierForge::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD && biomes.contains(biome)) {
            carvers.forEach(c -> builder.getGenerationSettings().addCarver(step, c));
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
//?}
