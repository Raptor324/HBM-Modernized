package com.hbm_m.inventory.fluid.trait;

import java.io.IOException;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * @deprecated in 1.7.10 in favor of FT_Toxin; kept for tooltip parity.
 */
@Deprecated
public class FT_Poison extends FluidTrait {

    protected boolean withering;
    protected int level;

    public FT_Poison() {}

    public FT_Poison(boolean withering, int level) {
        this.withering = withering;
        this.level = level;
    }

    public boolean isWithering() {
        return this.withering;
    }

    public int getLevel() {
        return this.level;
    }

    @Override
    public void addInfoHidden(List<Component> info) {
        info.add(Component.translatable("fluid.hbm_m.trait.toxic_fumes").withStyle(ChatFormatting.GREEN));
    }

    @Override
    public void serializeJSON(JsonWriter writer) throws IOException {
        writer.name("level").value(this.level);
        writer.name("withering").value(this.withering);
    }

    @Override
    public void deserializeJSON(JsonObject obj) {
        this.level = obj.get("level").getAsInt();
        this.withering = obj.get("withering").getAsBoolean();
    }
}
