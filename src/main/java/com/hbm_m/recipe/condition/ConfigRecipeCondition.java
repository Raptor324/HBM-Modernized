//? if forge {
package com.hbm_m.recipe.condition;

import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

/**
 * Forge-Rezeptbedingung {@code hbm_m:config}: das Rezept existiert nur, wenn der Konfig-Schalter an ist
 * ({@code "flag"}, mit {@code !} verneint; Namen siehe {@link ConfigRecipeFlags}). Entspricht den
 * {@code if(GeneralConfig...)}-Bloecken um die Rezeptregistrierung im Original.
 */
public record ConfigRecipeCondition(String flag) implements ICondition {

    @SuppressWarnings("removal")
    public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "config");

    public static void register() {
        CraftingHelper.register(Serializer.INSTANCE);
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return ConfigRecipeFlags.test(flag);
    }

    public static final class Serializer implements IConditionSerializer<ConfigRecipeCondition> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void write(JsonObject json, ConfigRecipeCondition value) {
            json.addProperty("flag", value.flag());
        }

        @Override
        public ConfigRecipeCondition read(JsonObject json) {
            return new ConfigRecipeCondition(GsonHelper.getAsString(json, "flag"));
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    }
}
//?}
