package com.hbm_m.inventory.fluid.trait;

import java.io.IOException;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import com.hbm_m.util.BobMathUtil;

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
        info.add(Component.literal("[").append(Component.translatable("hbmfluid.trait.combustible")).append("]").withStyle(ChatFormatting.GOLD));
        if (combustionEnergy > 0) {
            info.add(Component.translatable("hbmfluid.trait.provides").append(" ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(BobMathUtil.getShortNumber(combustionEnergy) + "HE ").withStyle(ChatFormatting.RED))
                    .append(Component.translatable("hbmfluid.trait.perBucket").withStyle(ChatFormatting.GOLD)));
            info.add(Component.translatable("hbmfluid.trait.fuelGrade").append(": ").withStyle(ChatFormatting.GOLD)
                    .append(this.fuelGrade.getLocalizedName().withStyle(ChatFormatting.RED)));
        }
    }
    
    public long getCombustionEnergy() {
        return this.combustionEnergy;
    }
    
    public FuelGrade getGrade() {
        return this.fuelGrade;
    }
    
    public enum FuelGrade {
        LOW("low"),         
        MEDIUM("medium"),   
        HIGH("high"),       
        AERO("aviation"),   
        GAS("gaseous");     
        
        private final String grade;
        
        FuelGrade(String grade) {
            this.grade = grade;
        }
        
        public String getGrade() {
            return this.grade;
        }

        /** 1:1 getLocalizedName(): hbmfluid.trait.fuel.<grade> */
        public net.minecraft.network.chat.MutableComponent getLocalizedName() {
            return Component.translatable("hbmfluid.trait.fuel." + this.grade);
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