package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.recipe.MixerRecipe;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Datagen-билдер {@link MixerRecipe} ({@code hbm_m:mixer}).
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген компилируется
 * только на 1.20.1-forge. Жидкостные стаки сериализуются через общую утилиту
 * {@link BaseRecipeBuilder#fluidStackToJson}; пустые стаки (нет входа — solid-only-рецепты
 * FFA вроде FISHOIL/BITUMEN) в JSON не пишутся.</p>
 *
 * <p>JSON-формат (читается {@link MixerRecipe.Serializer#readJson}):</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:mixer",
 *   "input_a": { "fluid": "...", "amount": 1000 },   // опционально
 *   "input_b": { "fluid": "...", "amount": 1000 },   // опционально
 *   "solid":   { "item" | "tag" | "any_of", "count": n },  // опционально, count по умолчанию 1
 *   "output":  { "fluid": "...", "amount": 2000 },
 *   "duration": 100,
 *   "energy_per_tick": 50
 * }
 * }</pre>
 *
 * <p>Твёрдый вход задаётся ровно одним из {@link #solidItem(Supplier, int)} /
 * {@link #solidTag(TagKey, int)} / {@link #solidAnyOf(int, Supplier...)} (FFA-таблица
 * использовала item, тег {@code minecraft:fishes} и anyOf четырёх дёгтей).</p>
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

    @Nullable private Item solidItem;
    @Nullable private TagKey<Item> solidTag;
    private final List<Item> solidAnyOf = new ArrayList<>();
    private int solidCount = 1;

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

    /** Твёрдый вход — один предмет (FFA {@code item(...)}). */
    public MixerRecipeBuilder solidItem(Supplier<? extends Item> item, int count) {
        this.solidItem = item.get();
        this.solidCount = count;
        return this;
    }

    /** Твёрдый вход — предметный тег (FFA {@code ItemTags.FISHES}). */
    public MixerRecipeBuilder solidTag(TagKey<Item> tag, int count) {
        this.solidTag = tag;
        this.solidCount = count;
        return this;
    }

    /** Твёрдый вход — любой из перечисленных предметов (FFA {@code anyOf(...)}, например четыре дёгтя). */
    @SafeVarargs
    public final MixerRecipeBuilder solidAnyOf(int count, Supplier<? extends Item>... items) {
        for (Supplier<? extends Item> i : items) this.solidAnyOf.add(i.get());
        this.solidCount = count;
        return this;
    }

    @Override
    public net.minecraft.world.item.Item getResult() {
        return Items.AIR;
    }

    @Override
    protected void serializeRecipeData(JsonObject json) {
        if (!inputA.isEmpty()) json.add("input_a", fluidStackToJson(this.inputA));
        if (!inputB.isEmpty()) json.add("input_b", fluidStackToJson(this.inputB));
        if (solidItem != null || solidTag != null || !solidAnyOf.isEmpty()) {
            JsonObject solid = new JsonObject();
            if (solidItem != null) {
                solid.addProperty("item", BuiltInRegistries.ITEM.getKey(solidItem).toString());
            } else if (solidTag != null) {
                solid.addProperty("tag", solidTag.location().toString());
            } else {
                JsonArray arr = new JsonArray();
                for (Item i : solidAnyOf) arr.add(BuiltInRegistries.ITEM.getKey(i).toString());
                solid.add("any_of", arr);
            }
            if (solidCount > 1) solid.addProperty("count", solidCount);
            json.add("solid", solid);
        }
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
