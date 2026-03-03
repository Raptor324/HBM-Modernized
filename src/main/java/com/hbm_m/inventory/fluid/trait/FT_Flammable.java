package com.hbm_m.inventory.fluid.trait;

import java.io.IOException;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class FT_Flammable extends FluidTrait {
    
    private long energy;
    
    public FT_Flammable() { }
    
    public FT_Flammable(long energy) {
        this.energy = energy;
    }
    
    public long getHeatEnergy() {
        return this.energy;
    }
    
    @Override
    public void addInfo(List<Component> info) {
        super.addInfo(info);
        
        info.add(Component.literal("[Flammable]").withStyle(ChatFormatting.YELLOW));
        
        if(energy > 0) {
            // Форматируем число с разделителями (как в BobMathUtil.getShortNumber)
            String formattedEnergy = String.format("%,d", energy);
            info.add(Component.literal("Provides ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(formattedEnergy + "TU ").withStyle(ChatFormatting.RED))
                    .append(Component.literal("per bucket").withStyle(ChatFormatting.YELLOW)));
        }
    }

    @Override
    public void serializeJSON(JsonWriter writer) throws IOException {
        writer.name("energy").value(energy);
    }
    
    @Override
    public void deserializeJSON(JsonObject obj) {
        this.energy = obj.get("energy").getAsLong();
    }
}