package com.hbm_m.inventory.fluid.trait;

import java.io.IOException;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public class FT_PWRModerator extends FluidTrait {

    private double multiplier;

    public FT_PWRModerator() {}

    public FT_PWRModerator(double multiplier) {
        this.multiplier = multiplier;
    }

    public double getMultiplier() {
        return multiplier;
    }

    @Override
    public void addInfo(List<Component> info) {
        info.add(Component.literal("[").append(Component.translatable("hbmfluid.trait.pwrFluxMultiplier")).append("]").withStyle(ChatFormatting.BLUE));
    }

    @Override
    public void addInfoHidden(List<Component> info) {
        int mult = (int) (multiplier * 100 - 100);
        info.add(Component.translatable("hbmfluid.trait.pwrFluxCore").append(" " + (mult >= 0 ? "+" : "") + mult + "%").withStyle(ChatFormatting.BLUE));
    }

    @Override
    public void serializeJSON(JsonWriter writer) throws IOException {
        writer.name("multiplier").value(multiplier);
    }

    @Override
    public void deserializeJSON(JsonObject obj) {
        this.multiplier = obj.get("multiplier").getAsDouble();
    }
}
