package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonObject;
import com.hbm_m.recipe.MixerRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Datagen-билдер {@link MixerRecipe} ({@code hbm_m:mixer}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген компилируется
 * только на 1.20.1-forge. Жидкостные стаки сериализуются через общую утилиту
 * {@link BaseRecipeBuilder#fluidStackToJson}.</p>
 *
 * <p>JSON-формат (читается {@link MixerRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:mixer",
 *   "input_a": { "fluid": "...", "amount": 1000 },
 *   "input_b": { "fluid": "...", "amount": 1000 },
 *   "output":   { "fluid": "...", "amount": 2000 },
 *   "duration": 100,
 *   "energy_per_tick": 50
 * }
 * }</pre>
 *
 * <p>Миксер не имеет предметного выхода — {@link #getResult()} возвращает {@link Items#AIR}
 * (ванильный {@code RecipeBuilder} требует реализацию, но для чисто жидкостных рецептов
 * результат не используется).</p>
 */
public class MixerRecipeBuilder extends BaseRecipeBuilder<MixerRecipeBuilder> {

    private final FluidStack inputA;
    private final FluidStack inputB;
    private final FluidStack output;
    private final int duration;
    private final long energyPerTick;

    private MixerRecipeBuilder(FluidStack inputA, FluidStack inputB, FluidStack output,
                               int duration, long energyPerTick) {
        this.inputA = inputA;
        this.inputB = inputB;
        this.output = output;
        this.duration = duration;
        this.energyPerTick = energyPerTick;
    }

    public static MixerRecipeBuilder mixerRecipe(FluidStack inputA, FluidStack inputB,
                                                 FluidStack output, int duration, long energyPerTick) {
        return new MixerRecipeBuilder(inputA, inputB, output, duration, energyPerTick);
    }

    @Override
    public net.minecraft.world.item.Item getResult() {
        return Items.AIR;
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        json.add("input_a", fluidStackToJson(this.inputA));
        json.add("input_b", fluidStackToJson(this.inputB));
        json.add("output", fluidStackToJson(this.output));
        json.addProperty("duration", this.duration);
        json.addProperty("energy_per_tick", this.energyPerTick);
    }

    @Override
    protected RecipeSerializer<?> getType() {
        return MixerRecipe.Serializer.INSTANCE;
    }
}
//?}
