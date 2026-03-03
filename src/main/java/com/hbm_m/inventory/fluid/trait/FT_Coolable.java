package com.hbm_m.inventory.fluid.trait;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.registries.ForgeRegistries;

public class FT_Coolable extends FluidTrait {
    
    protected HashMap<CoolingType, Double> efficiency = new HashMap<>();
    
    public Fluid coolsTo;
    public int amountReq;
    public int amountProduced;
    public int heatEnergy;
    
    public FT_Coolable() { }
    
    public FT_Coolable(Fluid type, int req, int prod, int heat) {
        this.coolsTo = type;
        this.amountReq = req;
        this.amountProduced = prod;
        this.heatEnergy = heat;
    }
    
    public FT_Coolable setEff(CoolingType type, double eff) {
        efficiency.put(type, eff);
        return this;
    }
    
    public double getEfficiency(CoolingType type) {
        Double eff = this.efficiency.get(type);
        return eff != null ? eff : 0.0D;
    }
    
    @Override
    public void addInfoHidden(List<Component> info) {
        info.add(Component.literal("Thermal capacity: " + heatEnergy + " TU per " + amountReq + "mB").withStyle(ChatFormatting.RED));
        for(CoolingType type : CoolingType.values()) {
            double eff = getEfficiency(type);
            if(eff > 0) {
                info.add(Component.literal("[" + type.name + "] ")
                        .withStyle(ChatFormatting.YELLOW)
                        .append(Component.literal("Efficiency: " + ((int) (eff * 100D)) + "%").withStyle(ChatFormatting.AQUA)));
            }
        }
    }
    
    public enum CoolingType {
        TURBINE("Turbine Steam"),
        HEATEXCHANGER("Coolable");
        
        public final String name;
        
        CoolingType(String name) {
            this.name = name;
        }
    }

    @Override
    public void serializeJSON(JsonWriter writer) throws IOException {
        ResourceLocation loc = ForgeRegistries.FLUIDS.getKey(this.coolsTo);
        writer.name("coolsTo").value(loc != null ? loc.toString() : "minecraft:empty");
        writer.name("amountReq").value(this.amountReq);
        writer.name("amountProd").value(this.amountProduced);
        writer.name("heatEnergy").value(this.heatEnergy);
        
        for(Entry<CoolingType, Double> entry : this.efficiency.entrySet()) {
            writer.name(entry.getKey().name()).value(entry.getValue());
        }
    }
    
    @Override
    public void deserializeJSON(JsonObject obj) {
        Fluid f = ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse(obj.get("coolsTo").getAsString()));
        this.coolsTo = f != null ? f : Fluids.EMPTY;
        this.amountReq = obj.get("amountReq").getAsInt();
        this.amountProduced = obj.get("amountProd").getAsInt();
        this.heatEnergy = obj.get("heatEnergy").getAsInt();
        
        for(CoolingType type : CoolingType.values()) {
            if(obj.has(type.name())) efficiency.put(type, obj.get(type.name()).getAsDouble());
        }
    }
}