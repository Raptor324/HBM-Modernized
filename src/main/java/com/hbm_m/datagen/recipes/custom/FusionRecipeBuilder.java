package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.FusionRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluid;

/**
 * Datagen-Builder fuer {@link FusionRecipe} ({@code hbm_m:fusion}).
 *
 * <p>1:1-Abbild des 1.7.10-Builders {@code new FusionRecipe(name).setInputEnergy(..).setOutputEnergy(..)
 * .setOutputFlux(..).setRGB(..).setPower(..).setDuration(..).inputFluids(..).outputFluids(..)/outputItems(..)}.</p>
 */
public class FusionRecipeBuilder extends BaseRecipeBuilder<FusionRecipeBuilder> {

    private record FluidAmount(Fluid fluid, int amount) {}

    private final int duration;
    private final long power;

    private long ignitionTemp;
    private long outputTemp;
    private double outputFlux;

    private float r = 1F;
    private float g = 0.2F;
    private float b = 0.6F;

    private final List<FluidAmount> fluidInputs = new ArrayList<>();
    private final List<ItemStack> itemOutputs = new ArrayList<>();
    private final List<FluidAmount> fluidOutputs = new ArrayList<>();

    @Nullable
    private ItemStack iconItem;
    @Nullable
    private Fluid iconFluid;

    private FusionRecipeBuilder(int duration, long power) {
        this.duration = duration;
        this.power = power;
    }

    /** Original: {@code .setPower(solenoid).setDuration(100)}. */
    public static FusionRecipeBuilder fusionRecipe(int duration, long power) {
        return new FusionRecipeBuilder(duration, power);
    }

    /** Original: {@code setInputEnergy} - minimale Klystron-Energie zum Zuenden. */
    public FusionRecipeBuilder inputEnergy(long ignitionTemp) {
        this.ignitionTemp = ignitionTemp;
        return this;
    }

    /** Original: {@code setOutputEnergy} - Plasmaleistung bei Vollast. */
    public FusionRecipeBuilder outputEnergy(long outputTemp) {
        this.outputTemp = outputTemp;
        return this;
    }

    /** Original: {@code setOutputFlux} - Neutronenfluss bei Vollast. */
    public FusionRecipeBuilder outputFlux(double outputFlux) {
        this.outputFlux = outputFlux;
        return this;
    }

    /** Original: {@code setRGB} - Plasmafarbe. */
    public FusionRecipeBuilder rgb(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
        return this;
    }

    public FusionRecipeBuilder addFluidInput(Fluid fluid, int amount) {
        this.fluidInputs.add(new FluidAmount(fluid, amount));
        return this;
    }

    public FusionRecipeBuilder addFluidOutput(Fluid fluid, int amount) {
        this.fluidOutputs.add(new FluidAmount(fluid, amount));
        return this;
    }

    public FusionRecipeBuilder addItemOutput(ItemStack stack) {
        this.itemOutputs.add(stack);
        return this;
    }

    public FusionRecipeBuilder addItemOutput(Item item, int count) {
        return addItemOutput(new ItemStack(item, count));
    }

    public FusionRecipeBuilder icon(ItemStack stack) {
        this.iconItem = stack;
        return this;
    }

    public FusionRecipeBuilder icon(Fluid fluid) {
        this.iconFluid = fluid;
        return this;
    }

    @Override
    public Item getResult() {
        for (ItemStack stack : itemOutputs) {
            if (!stack.isEmpty()) return stack.getItem();
        }
        return Items.AIR;
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.addProperty("duration", duration);
        json.addProperty("power", power);
        json.addProperty("ignition_temp", ignitionTemp);
        json.addProperty("output_temp", outputTemp);
        json.addProperty("output_flux", outputFlux);
        json.addProperty("r", r);
        json.addProperty("g", g);
        json.addProperty("b", b);

        if (iconItem != null && !iconItem.isEmpty()) {
            json.add("icon_item", stackToJson(iconItem));
        }

        if (iconFluid != null) {
            ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(iconFluid);
            if (fluidId != null) json.addProperty("icon_fluid", fluidId.toString());
        }

        JsonArray fluidInputsJson = new JsonArray();
        for (FluidAmount fa : fluidInputs) {
            if (fa.fluid() == null) continue;
            fluidInputsJson.add(fluidStackToJson(FluidStack.create(fa.fluid(), fa.amount())));
        }
        json.add("fluid_inputs", fluidInputsJson);

        JsonArray itemOutputsJson = new JsonArray();
        for (ItemStack out : itemOutputs) {
            if (out == null || out.isEmpty()) continue;
            itemOutputsJson.add(stackToJson(out));
        }
        json.add("item_outputs", itemOutputsJson);

        JsonArray fluidOutputsJson = new JsonArray();
        for (FluidAmount fa : fluidOutputs) {
            if (fa.fluid() == null) continue;
            fluidOutputsJson.add(fluidStackToJson(FluidStack.create(fa.fluid(), fa.amount())));
        }
        json.add("fluid_outputs", fluidOutputsJson);
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return FusionRecipe.Serializer.INSTANCE;
    }
}
//?}
