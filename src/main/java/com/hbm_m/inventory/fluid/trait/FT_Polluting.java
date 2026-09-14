package com.hbm_m.inventory.fluid.trait;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.util.EnergyFormatter;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class FT_Polluting extends FluidTrait {

    public final HashMap<PollutionType, Float> releaseMap = new HashMap<>();
    public final HashMap<PollutionType, Float> burnMap = new HashMap<>();

    public FT_Polluting release(PollutionType type, float amount) {
        releaseMap.put(type, amount);
        return this;
    }

    public FT_Polluting burn(PollutionType type, float amount) {
        burnMap.put(type, amount);
        return this;
    }

    /**
     * 1:1-Port von {@code FT_Polluting.onFluidRelease}: verschuettetes Fluid traegt aus
     * {@link #releaseMap} ein, verbranntes aus {@link #burnMap}.
     *
     * <p><b>Hinweis:</b> Im Port ruft noch niemand diesen Haken - das Leck-/Bruch-Rohrwerk der
     * Fluidtanks fehlt. Die Maschinen, die es schon gibt, nutzen stattdessen
     * {@link #pollute(Level, BlockPos, Fluid, FluidReleaseType, float)} direkt.</p>
     */
    @Override
    public void onFluidRelease(Level level, BlockPos pos, FluidTank tank, int overflowAmount, FluidReleaseType type) {
        if (type == FluidReleaseType.SPILL) {
            for (Entry<PollutionType, Float> entry : releaseMap.entrySet()) {
                PollutionHandler.incrementPollution(level, pos, entry.getKey(), entry.getValue());
            }
        }
        if (type == FluidReleaseType.BURN) {
            for (Entry<PollutionType, Float> entry : burnMap.entrySet()) {
                PollutionHandler.incrementPollution(level, pos, entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 1:1-Port des statischen {@code FT_Polluting.pollute}: traegt die Werte des Fluids
     * mal der freigesetzten Menge in Millibuckets ein.
     */
    public static void pollute(Level level, BlockPos pos, Fluid fluid, FluidReleaseType release, float mB) {
        if (release == FluidReleaseType.VOID) return;

        FT_Polluting trait = FluidType.getTrait(fluid, FT_Polluting.class);
        if (trait == null) return;

        HashMap<PollutionType, Float> map = release == FluidReleaseType.BURN ? trait.burnMap : trait.releaseMap;

        for (Entry<PollutionType, Float> entry : map.entrySet()) {
            PollutionHandler.incrementPollution(level, pos, entry.getKey(), entry.getValue() * mB);
        }
    }

    @Override
    public void addInfo(List<Component> info) {
        info.add(Component.translatable("fluid.hbm_m.trait.polluting").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void addInfoHidden(List<Component> info) {
        if (!this.releaseMap.isEmpty()) {
            info.add(Component.translatable("fluid.hbm_m.trait.polluting.when_spilled").withStyle(ChatFormatting.GREEN));
            for (Entry<PollutionType, Float> entry : releaseMap.entrySet()) {
                info.add(Component.translatable("fluid.hbm_m.trait.polluting.line",
                        EnergyFormatter.formatTooltipNumber(entry.getValue()), entry.getKey().name()).withStyle(ChatFormatting.GREEN));
            }
        }
        if (!this.burnMap.isEmpty()) {
            info.add(Component.translatable("fluid.hbm_m.trait.polluting.when_burned").withStyle(ChatFormatting.RED));
            for (Entry<PollutionType, Float> entry : burnMap.entrySet()) {
                info.add(Component.translatable("fluid.hbm_m.trait.polluting.line",
                        EnergyFormatter.formatTooltipNumber(entry.getValue()), entry.getKey().name()).withStyle(ChatFormatting.RED));
            }
        }
    }

    @Override
    public void serializeJSON(JsonWriter writer) throws IOException {
        writer.name("release").beginObject();
        for (Entry<PollutionType, Float> entry : releaseMap.entrySet()) {
            writer.name(entry.getKey().name()).value(entry.getValue());
        }
        writer.endObject();
        writer.name("burn").beginObject();
        for (Entry<PollutionType, Float> entry : burnMap.entrySet()) {
            writer.name(entry.getKey().name()).value(entry.getValue());
        }
        writer.endObject();
    }

    @Override
    public void deserializeJSON(JsonObject obj) {
        if (obj.has("release")) {
            JsonObject release = obj.getAsJsonObject("release");
            for (PollutionType type : PollutionType.values()) {
                if (release.has(type.name())) {
                    releaseMap.put(type, release.get(type.name()).getAsFloat());
                }
            }
        }
        if (obj.has("burn")) {
            JsonObject burn = obj.getAsJsonObject("burn");
            for (PollutionType type : PollutionType.values()) {
                if (burn.has(type.name())) {
                    burnMap.put(type, burn.get(type.name()).getAsFloat());
                }
            }
        }
    }
}
