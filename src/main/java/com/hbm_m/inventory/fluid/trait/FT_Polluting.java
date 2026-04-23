package com.hbm_m.inventory.fluid.trait;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import com.hbm_m.util.EnergyFormatter;

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
