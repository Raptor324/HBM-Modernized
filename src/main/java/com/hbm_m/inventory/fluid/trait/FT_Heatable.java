package com.hbm_m.inventory.fluid.trait;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm_m.util.EnergyFormatter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

public class FT_Heatable extends FluidTrait {

    protected final List<HeatingStep> steps = new ArrayList<>();
    protected final HashMap<HeatingType, Double> efficiency = new HashMap<>();

    public FT_Heatable addStep(int heat, int req, Fluid typeProduced, int prod) {
        steps.add(new HeatingStep(req, heat, typeProduced, prod));
        return this;
    }

    public FT_Heatable setEff(HeatingType type, double eff) {
        efficiency.put(type, eff);
        return this;
    }

    public double getEfficiency(HeatingType type) {
        Double eff = this.efficiency.get(type);
        return eff != null ? eff : 0.0D;
    }

    public HeatingStep getFirstStep() {
        return this.steps.isEmpty() ? null : this.steps.get(0);
    }

    @Override
    public void addInfoHidden(List<Component> info) {
        HeatingStep first = getFirstStep();
        if (first == null) return;
        info.add(Component.translatable("fluid.hbm_m.trait.heatable.thermal_capacity",
                        EnergyFormatter.formatTooltipNumber(first.heatReq),
                        String.valueOf(first.amountReq))
                .withStyle(ChatFormatting.RED));
        for (HeatingType type : HeatingType.values()) {
            double eff = getEfficiency(type);
            if (eff > 0) {
                info.add(Component.literal("[" + type.displayName + "] ")
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.translatable("fluid.hbm_m.trait.efficiency_pct", (int) (eff * 100D))
                                .withStyle(ChatFormatting.AQUA)));
            }
        }
    }

    public static class HeatingStep {
        public final int amountReq;
        public final int heatReq;
        public final Fluid typeProduced;
        public final int amountProduced;

        public HeatingStep(int req, int heat, Fluid type, int prod) {
            this.amountReq = req;
            this.heatReq = heat;
            this.typeProduced = type;
            this.amountProduced = prod;
        }
    }

    public enum HeatingType {
        BOILER("Boilable"),
        HEATEXCHANGER("Heatable"),
        PWR("PWR Coolant"),
        ICF("ICF Coolant"),
        PA("Particle Accelerator Coolant");

        public final String displayName;

        HeatingType(String displayName) {
            this.displayName = displayName;
        }
    }

    @Override
    public void serializeJSON(JsonWriter writer) throws IOException {
        writer.name("steps").beginArray();
        for (HeatingStep step : steps) {
            writer.beginObject();
            ResourceLocation loc = ForgeRegistries.FLUIDS.getKey(step.typeProduced);
            writer.name("typeProduced").value(loc != null ? loc.toString() : "minecraft:empty");
            writer.name("amountReq").value(step.amountReq);
            writer.name("amountProd").value(step.amountProduced);
            writer.name("heatReq").value(step.heatReq);
            writer.endObject();
        }
        writer.endArray();
        for (Entry<HeatingType, Double> entry : this.efficiency.entrySet()) {
            writer.name(entry.getKey().name()).value(entry.getValue());
        }
    }

    @Override
    public void deserializeJSON(JsonObject obj) {
        steps.clear();
        if (obj.has("steps")) {
            JsonArray arr = obj.getAsJsonArray("steps");
            for (int i = 0; i < arr.size(); i++) {
                JsonObject step = arr.get(i).getAsJsonObject();
                Fluid f = ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse(step.get("typeProduced").getAsString()));
                this.steps.add(new HeatingStep(
                        step.get("amountReq").getAsInt(),
                        step.get("heatReq").getAsInt(),
                        f != null ? f : Fluids.EMPTY,
                        step.get("amountProd").getAsInt()));
            }
        }
        for (HeatingType type : HeatingType.values()) {
            if (obj.has(type.name())) {
                efficiency.put(type, obj.get(type.name()).getAsDouble());
            }
        }
    }
}
