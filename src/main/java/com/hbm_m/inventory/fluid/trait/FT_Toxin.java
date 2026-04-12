package com.hbm_m.inventory.fluid.trait;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Tooltip-focused port of 1.7.10 FT_Toxin. Gameplay poisoning is not wired here;
 * lines mirror original descriptions for GUI parity.
 */
public class FT_Toxin extends FluidTrait {

    private final List<Component> detailLines = new ArrayList<>();

    public FT_Toxin addTooltipLine(Component line) {
        detailLines.add(line);
        return this;
    }

    @Override
    public void addInfoHidden(List<Component> info) {
        info.add(Component.translatable("fluid.hbm_m.trait.toxin_header").withStyle(ChatFormatting.LIGHT_PURPLE));
        info.addAll(detailLines);
    }

    @Override
    public void serializeJSON(JsonWriter writer) throws IOException {
        // Tooltip-only: no JSON persistence in Modernized yet
    }

    @Override
    public void deserializeJSON(JsonObject obj) {}
}
