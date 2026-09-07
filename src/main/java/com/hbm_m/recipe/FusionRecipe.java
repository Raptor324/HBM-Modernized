package com.hbm_m.recipe;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.PlatformRecipe;
import com.hbm_m.platform.recipe.PlatformRecipeSerializer;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.platform.recipe.RecipeInputWrapper;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * 1:1-Port von {@code com.hbm.inventory.recipes.FusionRecipe} (1.7.10), umgesetzt als
 * datapack-faehiger Rezepttyp {@code hbm_m:fusion}.
 *
 * <p>Der Fusionstorus waehlt sein Rezept ausschliesslich per ID aus (wie das Original ueber
 * {@code GenericRecipes} und den Blueprint-Slot), deshalb gibt es kein Slot-Matching.</p>
 *
 * <p>Zusaetzlich zu Dauer/Energie eines gewoehnlichen Rezepts traegt ein Fusionsrezept:</p>
 * <ul>
 *   <li>{@code ignition_temp} - minimale Klystron-Energie, um das Plasma zu zuenden (KyU/t)</li>
 *   <li>{@code output_temp} - Plasma-Ausgangsenergie bei Vollast (TU/t)</li>
 *   <li>{@code output_flux} - Neutronenfluss bei Vollast (flux/t)</li>
 *   <li>{@code r}/{@code g}/{@code b} - Plasmafarbe, die an alle Abnehmer weitergereicht wird</li>
 * </ul>
 */
public class FusionRecipe extends PlatformRecipe {

    private final List<FluidStack> fluidInputs;
    private final List<ItemStack> itemOutputs;
    private final List<FluidStack> fluidOutputs;
    private final int duration;
    private final long power;

    private final long ignitionTemp;
    private final long outputTemp;
    private final double neutronFlux;
    private final float r;
    private final float g;
    private final float b;

    @Nullable
    private final ItemStack iconItem;
    @Nullable
    private final ResourceLocation iconFluid;

    public FusionRecipe(ResourceLocation id,
                        List<FluidStack> fluidInputs,
                        List<ItemStack> itemOutputs,
                        List<FluidStack> fluidOutputs,
                        int duration,
                        long power,
                        long ignitionTemp,
                        long outputTemp,
                        double neutronFlux,
                        float r, float g, float b,
                        @Nullable ItemStack iconItem,
                        @Nullable ResourceLocation iconFluid) {
        super(id);
        this.fluidInputs = fluidInputs != null ? fluidInputs : List.of();
        this.itemOutputs = itemOutputs != null ? itemOutputs : List.of();
        this.fluidOutputs = fluidOutputs != null ? fluidOutputs : List.of();
        this.duration = duration;
        this.power = power;
        this.ignitionTemp = ignitionTemp;
        this.outputTemp = outputTemp;
        this.neutronFlux = neutronFlux;
        this.r = r;
        this.g = g;
        this.b = b;
        this.iconItem = iconItem != null && !iconItem.isEmpty() ? iconItem : null;
        this.iconFluid = iconFluid;
    }

    public List<FluidStack> getFluidInputs() { return fluidInputs; }
    public List<ItemStack> getItemOutputs() { return itemOutputs; }
    public List<FluidStack> getFluidOutputs() { return fluidOutputs; }
    public int getDuration() { return duration; }
    public long getPower() { return power; }

    /** Minimale Klystron-Energie zum Zuenden (Original: {@code ignitionTemp}). */
    public long getIgnitionTemp() { return ignitionTemp; }
    /** Plasma-Ausgangsenergie bei Vollast (Original: {@code outputTemp}). */
    public long getOutputTemp() { return outputTemp; }
    /** Neutronenfluss bei Vollast (Original: {@code neutronFlux}). */
    public double getNeutronFlux() { return neutronFlux; }
    public float getR() { return r; }
    public float getG() { return g; }
    public float getB() { return b; }

    @Override
    public boolean matchesRecipe(@NotNull RecipeInputWrapper container, @NotNull Level level) {
        // Der Torus waehlt per ID aus, kein Shaped-Matching.
        return false;
    }

    @Override
    public ItemStack assembleSafe() {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getResultItemSafe() {
        if (iconItem != null && !iconItem.isEmpty()) return iconItem.copy();
        for (ItemStack out : itemOutputs) {
            if (!out.isEmpty()) return out.copy();
        }
        for (FluidStack fs : fluidOutputs) {
            if (fs == null || fs.isEmpty()) continue;
            ItemStack stack = new ItemStack(ModItems.FLUID_IDENTIFIER.get());
            FluidIdentifierItem.setType(stack, fs.getFluid(), true);
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static final class Type implements RecipeType<FusionRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "fusion";
    }

    public static final class Serializer extends PlatformRecipeSerializer<FusionRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        //? if fabric && < 1.21.1 {
        /*public static final ResourceLocation ID = new ResourceLocation(RefStrings.MODID, "fusion");
        *///?} else {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "fusion");
        //?}

        @Override
        public FusionRecipe readJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            int duration = GsonHelper.getAsInt(json, "duration", 100);
            long power = GsonHelper.getAsLong(json, "power", 0L);
            long ignition = GsonHelper.getAsLong(json, "ignition_temp", 0L);
            long output = GsonHelper.getAsLong(json, "output_temp", 0L);
            double flux = GsonHelper.getAsDouble(json, "output_flux", 0D);
            float r = GsonHelper.getAsFloat(json, "r", 1F);
            float g = GsonHelper.getAsFloat(json, "g", 0.2F);
            float b = GsonHelper.getAsFloat(json, "b", 0.6F);

            List<FluidStack> fluidInputs = readFluids(json, "fluid_inputs");
            List<FluidStack> fluidOutputs = readFluids(json, "fluid_outputs");
            List<ItemStack> itemOutputs = readItemOutputs(json);

            ResourceLocation iconFluid = json.has("icon_fluid")
                    ? ResourceLocation.tryParse(GsonHelper.getAsString(json, "icon_fluid")) : null;
            ItemStack iconItem = json.has("icon_item")
                    ? RecipeHooks.itemStackFromJson(GsonHelper.getAsJsonObject(json, "icon_item")) : ItemStack.EMPTY;
            iconItem = finalizeIconStack(iconItem, iconFluid);

            return new FusionRecipe(recipeId, fluidInputs, itemOutputs, fluidOutputs, duration, power,
                    ignition, output, flux, r, g, b, iconItem, iconFluid);
        }

        @Override
        public FusionRecipe readNetwork(@NotNull ResourceLocation recipeId, @NotNull FriendlyByteBuf buf) {
            int duration = buf.readVarInt();
            long power = buf.readLong();
            long ignition = buf.readLong();
            long output = buf.readLong();
            double flux = buf.readDouble();
            float r = buf.readFloat();
            float g = buf.readFloat();
            float b = buf.readFloat();

            ItemStack iconItem = buf.readBoolean() ? RecipeHooks.readItem(buf) : ItemStack.EMPTY;
            ResourceLocation iconFluid = buf.readBoolean() ? buf.readResourceLocation() : null;
            iconItem = finalizeIconStack(iconItem, iconFluid);

            int fluidInCount = buf.readVarInt();
            List<FluidStack> fluidInputs = new ArrayList<>(fluidInCount);
            for (int i = 0; i < fluidInCount; i++) fluidInputs.add(RecipeHooks.readFluidStack(buf));

            int itemOutCount = buf.readVarInt();
            List<ItemStack> itemOutputs = new ArrayList<>(itemOutCount);
            for (int i = 0; i < itemOutCount; i++) itemOutputs.add(RecipeHooks.readItem(buf));

            int fluidOutCount = buf.readVarInt();
            List<FluidStack> fluidOutputs = new ArrayList<>(fluidOutCount);
            for (int i = 0; i < fluidOutCount; i++) fluidOutputs.add(RecipeHooks.readFluidStack(buf));

            return new FusionRecipe(recipeId, fluidInputs, itemOutputs, fluidOutputs, duration, power,
                    ignition, output, flux, r, g, b, iconItem, iconFluid);
        }

        @Override
        public void writeNetwork(@NotNull FriendlyByteBuf buf, @NotNull FusionRecipe recipe) {
            buf.writeVarInt(recipe.duration);
            buf.writeLong(recipe.power);
            buf.writeLong(recipe.ignitionTemp);
            buf.writeLong(recipe.outputTemp);
            buf.writeDouble(recipe.neutronFlux);
            buf.writeFloat(recipe.r);
            buf.writeFloat(recipe.g);
            buf.writeFloat(recipe.b);

            if (recipe.iconItem != null && !recipe.iconItem.isEmpty()) {
                buf.writeBoolean(true);
                RecipeHooks.writeItem(buf, recipe.iconItem);
            } else {
                buf.writeBoolean(false);
            }

            if (recipe.iconFluid != null) {
                buf.writeBoolean(true);
                buf.writeResourceLocation(recipe.iconFluid);
            } else {
                buf.writeBoolean(false);
            }

            buf.writeVarInt(recipe.fluidInputs.size());
            for (FluidStack fs : recipe.fluidInputs) RecipeHooks.writeFluidStack(buf, fs);

            buf.writeVarInt(recipe.itemOutputs.size());
            for (ItemStack out : recipe.itemOutputs) RecipeHooks.writeItem(buf, out);

            buf.writeVarInt(recipe.fluidOutputs.size());
            for (FluidStack fs : recipe.fluidOutputs) RecipeHooks.writeFluidStack(buf, fs);
        }

        private static List<FluidStack> readFluids(JsonObject json, String key) {
            if (!json.has(key)) return List.of();
            JsonArray arr = GsonHelper.getAsJsonArray(json, key);
            List<FluidStack> result = new ArrayList<>(arr.size());
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                ResourceLocation id = ResourceLocation.tryParse(GsonHelper.getAsString(obj, "fluid"));
                if (id == null) continue;
                result.add(RecipeHooks.fluidStackOf(id, GsonHelper.getAsLong(obj, "amount", 0L)));
            }
            return result;
        }

        private static List<ItemStack> readItemOutputs(JsonObject json) {
            if (!json.has("item_outputs")) return List.of();
            JsonArray arr = GsonHelper.getAsJsonArray(json, "item_outputs");
            List<ItemStack> result = new ArrayList<>(arr.size());
            for (JsonElement el : arr) result.add(RecipeHooks.itemStackFromJson(el.getAsJsonObject()));
            return result;
        }

        private static ItemStack finalizeIconStack(ItemStack iconItem, @Nullable ResourceLocation iconFluid) {
            if (iconItem != null && !iconItem.isEmpty()) {
                if (iconFluid != null && iconItem.getItem() instanceof FluidIdentifierItem) {
                    Fluid fluid = BuiltInRegistries.FLUID.get(iconFluid);
                    if (fluid != null && fluid != Fluids.EMPTY) FluidIdentifierItem.setType(iconItem, fluid, true);
                }
                return iconItem;
            }
            if (iconFluid == null) return ItemStack.EMPTY;
            Fluid fluid = BuiltInRegistries.FLUID.get(iconFluid);
            if (fluid == null || fluid == Fluids.EMPTY) return ItemStack.EMPTY;
            ItemStack stack = new ItemStack(ModItems.FLUID_IDENTIFIER.get());
            FluidIdentifierItem.setType(stack, fluid, true);
            return stack;
        }
    }
}
