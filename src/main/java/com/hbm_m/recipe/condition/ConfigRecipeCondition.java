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
//? if neoforge {
/*package com.hbm_m.recipe.condition;

import com.hbm_m.lib.RefStrings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

// NeoForge-Rezeptbedingung hbm_m:config (gleiches JSON {"type": "hbm_m:config", "flag": ".."}, Liste
// "neoforge:conditions" statt Forge "conditions" - Umschreibung in build.neoforge.gradle.kts processResources).
public record ConfigRecipeCondition(String flag) implements ICondition {

    public static final MapCodec<ConfigRecipeCondition> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("flag").forGetter(ConfigRecipeCondition::flag)
    ).apply(i, ConfigRecipeCondition::new));

    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
            DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, RefStrings.MODID);

    static {
        CONDITION_CODECS.register("config", () -> CODEC);
    }

    public static void register(IEventBus modBus) {
        CONDITION_CODECS.register(modBus);
    }

    @Override
    public boolean test(IContext context) {
        return ConfigRecipeFlags.test(flag);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
*///?}
