package com.hbm_m.recipe;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import dev.architectury.fluid.FluidStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Datapack-facing газо-центрифужный рецепт ({@code hbm_m:gas_centrifuge}).
 *
 * <p><b>Только для JEI-отображения.</b> Runtime-логика Gas Centrifuge — каскадное обогащение
 * через {@code PseudoFluidType} (см. {@code MachineGasCentrifugeBlockEntity}), а не поиск рецептов.
 * Этот JSON-рецепт описывает «полный каскад-результат» для статичного показа в JEI (как в
 * оригинале 1.7.10 NEI {@code GasCentrifugeHandler}, который показывал полностью обогащённый
 * результат, а не per-tick логику).</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:gas_centrifuge",
 *   "input": { "fluid": "hbm_m:uf6", "amount": 1200 },   // FluidStack вход каскада
 *   "outputs": [ { "item": "...", "count": 11 }, ... ],  // ItemStack[] — все выходы каскада
 *   "high_speed": true,
 *   "centrifuge_count": 4
 * }
 * }</pre>
 *
 * <p>{@code matches} всегда false (JEI-only рецепт — runtime не использует его для крафта).</p>
 */
public class GasCentrifugeRecipe extends PlatformRecipe {

    private final FluidStack input;
    private final ItemStack[] outputs;
    private final boolean highSpeed;
    private final int centrifugeCount;

    public GasCentrifugeRecipe(ResourceLocation id, FluidStack input, ItemStack[] outputs,
                                boolean highSpeed, int centrifugeCount) {
        super(id);
        this.input = input != null ? input : FluidStack.empty();
        this.outputs = outputs != null ? outputs : new ItemStack[0];
        this.highSpeed = highSpeed;
        this.centrifugeCount = Math.max(1, centrifugeCount);
    }

    public FluidStack getInput() { return input; }
    public ItemStack[] getOutputs() {
        ItemStack[] copy = new ItemStack[outputs.length];
        for (int i = 0; i < outputs.length; i++) copy[i] = outputs[i] != null ? outputs[i].copy() : ItemStack.EMPTY;
        return copy;
    }
    public boolean isHighSpeed() { return highSpeed; }
    public int getCentrifugeCount() { return centrifugeCount; }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        // JEI-only рецепт — runtime никогда не вызывает матчинг.
        return false;
    }

    @Override
    public ItemStack assembleSafe() { return ItemStack.EMPTY; }

    @Override
    public ItemStack getResultItemSafe() { return ItemStack.EMPTY; }

    @Override
    public RecipeSerializer<?> getSerializer() { return Serializer.INSTANCE; }

    @Override
    public RecipeType<?> getType() { return Type.INSTANCE; }

    public static class Type implements RecipeType<GasCentrifugeRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "gas_centrifuge";
    }

    public static class Serializer extends PlatformRecipeSerializer<GasCentrifugeRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "gas_centrifuge");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "gas_centrifuge");
        //?}

        @Override
        public GasCentrifugeRecipe readJson(ResourceLocation recipeId, JsonObject json) {
            FluidStack input = FluidStack.empty();
            if (json.has("input")) {
                JsonObject inObj = GsonHelper.getAsJsonObject(json, "input");
                ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(inObj, "fluid"));
                int amount = GsonHelper.getAsInt(inObj, "amount", 0);
                if (id != null && amount > 0) {
                    input = RecipeHooks.fluidStackOf(id, amount);
                }
            }

            JsonArray arr = GsonHelper.getAsJsonArray(json, "outputs");
            ItemStack[] outputs = new ItemStack[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                outputs[i] = RecipeHooks.itemStackFromJson(arr.get(i).getAsJsonObject());
            }

            boolean highSpeed = GsonHelper.getAsBoolean(json, "high_speed", false);
            int count = GsonHelper.getAsInt(json, "centrifuge_count", 1);
            return new GasCentrifugeRecipe(recipeId, input, outputs, highSpeed, count);
        }

        @Override
        public GasCentrifugeRecipe readNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            FluidStack input = RecipeHooks.readFluidStack(buf);
            int n = buf.readVarInt();
            ItemStack[] outputs = new ItemStack[n];
            for (int i = 0; i < n; i++) outputs[i] = RecipeHooks.readItem(buf);
            boolean highSpeed = buf.readBoolean();
            int count = buf.readVarInt();
            return new GasCentrifugeRecipe(recipeId, input, outputs, highSpeed, count);
        }

        @Override
        public void writeNetwork(FriendlyByteBuf buf, GasCentrifugeRecipe recipe) {
            RecipeHooks.writeFluidStack(buf, recipe.input);
            buf.writeVarInt(recipe.outputs.length);
            for (ItemStack out : recipe.outputs) RecipeHooks.writeItem(buf, out);
            buf.writeBoolean(recipe.highSpeed);
            buf.writeVarInt(recipe.centrifugeCount);
        }
    }
}
