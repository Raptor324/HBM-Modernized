package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonObject;
import com.hbm_m.recipe.VacuumDistillRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link VacuumDistillRecipe} ({@code hbm_m:vacuum_distill}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.
 * Жидкостные стаки сериализуются через общую утилиту {@link BaseRecipeBuilder#fluidStackToJson}.</p>
 *
 * <p>JSON-формат (читается {@link VacuumDistillRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:vacuum_distill",
 *   "input":     { "fluid": "...", "amount": 100 },
 *   "heavy":     { "fluid": "...", "amount": 40 },
 *   "reformate": { "fluid": "...", "amount": 25 },
 *   "light":     { "fluid": "...", "amount": 20 },
 *   "sour":      { "fluid": "...", "amount": 15 }
 * }
 * }</pre>
 *
 * <p>Предметного выхода нет — {@link #getResult()} возвращает {@link Items#AIR}
 * (ванильный {@code RecipeBuilder} требует реализацию).</p>
 */
public class VacuumDistillRecipeBuilder extends BaseRecipeBuilder<VacuumDistillRecipeBuilder> {

    private final FluidStack input;
    private final FluidStack heavy;
    private final FluidStack reformate;
    private final FluidStack light;
    private final FluidStack sour;

    private VacuumDistillRecipeBuilder(FluidStack input, FluidStack heavy, FluidStack reformate,
                                       FluidStack light, FluidStack sour) {
        this.input = input;
        this.heavy = heavy;
        this.reformate = reformate;
        this.light = light;
        this.sour = sour;
    }

    public static VacuumDistillRecipeBuilder vacuumDistillRecipe(FluidStack input,
                                                                 FluidStack heavy, FluidStack reformate,
                                                                 FluidStack light, FluidStack sour) {
        return new VacuumDistillRecipeBuilder(input, heavy, reformate, light, sour);
    }

    @Override
    public net.minecraft.world.item.Item getResult() {
        return Items.AIR;
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("input", fluidStackToJson(this.input));
        json.add("heavy", fluidStackToJson(this.heavy));
        json.add("reformate", fluidStackToJson(this.reformate));
        json.add("light", fluidStackToJson(this.light));
        json.add("sour", fluidStackToJson(this.sour));
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return VacuumDistillRecipe.Serializer.INSTANCE;
    }
}
//?}
