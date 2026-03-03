package com.hbm_m.inventory.fluid.trait;

import java.io.IOException;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class FT_Combustible extends FluidTrait {
    
    protected FuelGrade fuelGrade;
    protected long combustionEnergy;
    
    public FT_Combustible() { }
    
    public FT_Combustible(FuelGrade grade, long energy) {
        this.fuelGrade = grade;
        this.combustionEnergy = energy;
    }
    
    @Override
    public void addInfo(List<Component> info) {
        super.addInfo(info);

        info.add(Component.literal("[Combustible]").withStyle(ChatFormatting.GOLD));
        
        if(combustionEnergy > 0) {
            String formattedEnergy = String.format("%,d", combustionEnergy);
            
            info.add(Component.literal("Provides ")
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(formattedEnergy + "HE ").withStyle(ChatFormatting.RED))
                    .append(Component.literal("per bucket").withStyle(ChatFormatting.GOLD)));
                    
            info.add(Component.literal("Fuel grade: ")
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(this.fuelGrade.getGrade()).withStyle(ChatFormatting.RED)));
        }
    }
    
    public long getCombustionEnergy() {
        return this.combustionEnergy;
    }
    
    public FuelGrade getGrade() {
        return this.fuelGrade;
    }
    
    public enum FuelGrade {
        LOW("Low"),         
        MEDIUM("Medium"),   
        HIGH("High"),       
        AERO("Aviation"),   
        GAS("Gaseous");     
        
        private final String grade;
        
        FuelGrade(String grade) {
            this.grade = grade;
        }
        
        public String getGrade() {
            return this.grade;
        }
    }

    @Override
    public void serializeJSON(JsonWriter writer) throws IOException {
        writer.name("energy").value(combustionEnergy);
        writer.name("grade").value(fuelGrade.name());
    }
    
    @Override
    public void deserializeJSON(JsonObject obj) {
        this.combustionEnergy = obj.get("energy").getAsLong();
        this.fuelGrade = FuelGrade.valueOf(obj.get("grade").getAsString());
    }
}