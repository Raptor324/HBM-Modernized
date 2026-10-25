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
 * Forge 1.20.1 kennt kein {@code forge:add_carvers} (gibt es erst bei NeoForge) – daher eigener
 * Biome-Modifier-Typ {@code hbm_m:add_carvers} mit demselben JSON-Format (biomes, carvers, step).
 */
public record AddCarversBiomeModifier(HolderSet<Biome> biomes, HolderSet<ConfiguredWorldCarver<?>> carvers,
                                      GenerationStep.Carving step) implements BiomeModifier {

    public static final Codec<AddCarversBiomeModifier> CODEC = RecordCodecBuilder.create(builder -> builder.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(AddCarversBiomeModifier::biomes),
            ConfiguredWorldCarver.LIST_CODEC.fieldOf("carvers").forGetter(AddCarversBiomeModifier::carvers),
            GenerationStep.Carving.CODEC.fieldOf("step").forGetter(AddCarversBiomeModifier::step)
    ).apply(builder, AddCarversBiomeModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase == Phase.ADD && biomes.contains(biome)) {
            for (Holder<ConfiguredWorldCarver<?>> carver : carvers) {
                builder.getGenerationSettings().addCarver(step, carver);
            }
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
//?}
