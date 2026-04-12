package com.hbm_m.inventory.fluid.trait;

import java.io.IOException;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class FT_Pheromone extends FluidTrait {

    public int type;

    public FT_Pheromone() {}

    public FT_Pheromone(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    @Override
    public void addInfo(List<Component> info) {
        if (type == 1) {
            info.add(Component.translatable("fluid.hbm_m.trait.pheromone_glyphid").withStyle(ChatFormatting.AQUA));
        } else {
            info.add(Component.translatable("fluid.hbm_m.trait.pheromone_modified").withStyle(ChatFormatting.BLUE));
        }
    }

    @Override
    public void serializeJSON(JsonWriter writer) throws IOException {
        writer.name("type").value(type);
    }

    @Override
    public void deserializeJSON(JsonObject obj) {
        this.type = obj.get("type").getAsInt();
    }
}
