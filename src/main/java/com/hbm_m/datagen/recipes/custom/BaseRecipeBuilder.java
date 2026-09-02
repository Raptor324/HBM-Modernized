package com.hbm_m.datagen.recipes.custom;

//? if forge {
import com.google.gson.JsonObject;
import com.hbm_m.lib.RefStrings;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Базовый класс всех кастомных билдеров рецептов HBM.
 *
 * <p><b>Датаген компилируется и запускается только на 1.20.1-forge</b> — весь файл обёрнут в
 * {@code //? if forge}. Поэтому внутри — <em>чистый ванильный код 1.20.1</em> без Stonecutter-ветвлений.
 * Внутри билдеров/генераторов больше нет условий {@code //? if fabric && < 1.21.1} —
 * они здесь не нужны (другие версии MC/лоадеры этот код даже не компилируют).</p>
 *
 * <p>Предоставляет:</p>
 * <ul>
 *   <li>{@code unlockedBy} / {@code group} — ванильный {@link RecipeBuilder} API (критерии, группы);</li>
 *   <li>{@code save(writer, id)} / {@code save(writer, "path")} — публикация готового рецепта;</li>
 *   <li>{@link #resLoc(String)} — статический помощник для {@link ResourceLocation} под модом;</li>
 *   <li>{@link #stackToJson(ItemStack)} / {@link #fluidStackToJson(dev.architectury.fluid.FluidStack)} —
 *       общие утилиты сериализации стак/жидкость → JSON, используемые всеми наследниками
 *       (устраняет дублирование логики «item + count» в каждом билдере).</li>
 * </ul>
 */
public abstract class BaseRecipeBuilder<T extends BaseRecipeBuilder<T>> implements RecipeBuilder {
    protected final Advancement.Builder advancement = Advancement.Builder.advancement();
    protected String group;

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull T unlockedBy(@NotNull String criterionName, @NotNull CriterionTriggerInstance criterionTrigger) {
        this.advancement.addCriterion(criterionName, criterionTrigger);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull T group(@Nullable String groupName) {
        this.group = groupName;
        return (T) this;
    }

    @Override
    public void save(@NotNull Consumer<FinishedRecipe> consumer, @NotNull ResourceLocation recipeId) {
        consumer.accept(new Result(recipeId, this));
    }

    /**
     * Универсальный метод сохранения рецепта по строковому пути.
     * No-op Stonecutter: датаген — только 1.20.1-forge, поэтому чистый ванильный код.
     */
    @SuppressWarnings("removal") // ResourceLocation(String,String) deprecated в Forge 1.20.1 backport
    public void save(@NotNull Consumer<FinishedRecipe> consumer, @NotNull String path) {
        save(consumer, new ResourceLocation(RefStrings.MODID, path));
    }

    /**
     * Строит {@link ResourceLocation} с namespace {@link RefStrings#MODID} и указанным путём.
     * Помощник для передачи id в ванильные билдеры
     * ({@link net.minecraft.data.recipes.ShapedRecipeBuilder#save},
     * {@link net.minecraft.data.recipes.SimpleCookingRecipeBuilder#save}, etc.),
     * которые принимают {@link ResourceLocation}, а не {@code String}.
     *
     * <pre>{@code
     * ShapedRecipeBuilder.shaped(...)
     *     ...
     *     .save(writer, BaseRecipeBuilder.resLoc("smelting/" + name));
     * }</pre>
     */
    @SuppressWarnings("removal") // ResourceLocation(String,String) deprecated в Forge 1.20.1 backport
    public static ResourceLocation resLoc(@NotNull String path) {
        return new ResourceLocation(RefStrings.MODID, path);
    }

    /**
     * Сериализует {@link ItemStack} в компактный JSON-объект рецепта:
     * <pre>{@code
     * { "item": "<registry_id>", "count": <n> }   // count — только если > 1
     * }</pre>
     *
     * <p>Используется всеми кастомными билдерами (Shredder, Centrifuge, Crystallizer, ...)
     * для единообразной записи item-выходов и устранения дублирования логики
     * «item + count» в каждом наследнике. NBT в датагене намеренно не обрабатывается —
     * генерируемые стеки никогда не содержат NBT (только предмет + количество),
     * а NBT-рецепты JSON-форматом мода пока не поддерживаются.</p>
     */
    protected JsonObject stackToJson(@NotNull ItemStack stack) {
        JsonObject json = new JsonObject();
        json.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (stack.getCount() > 1) {
            json.addProperty("count", stack.getCount());
        }
        return json;
    }

    /**
     * Сериализует {@link dev.architectury.fluid.FluidStack} в JSON-объект рецепта:
     * <pre>{@code
     * { "fluid": "<registry_id>", "amount": <mB> }
     * }</pre>
     *
     * <p>Используется билдерами рецептов с жидкостными входами/выходами
     * (ChemicalPlant, Crystallizer, Mixer, ...) — единый формат, совпадающий с
     * форматом чтения {@link com.hbm_m.platform.recipe.RecipeHooks#readFluidStack}.
     * Пустые стаки пропускаются вызывающей стороной (в билдере), здесь проверки нет —
     * метод исключительно кодирует переданный стак.</p>
     */
    protected JsonObject fluidStackToJson(@NotNull dev.architectury.fluid.FluidStack stack) {
        JsonObject json = new JsonObject();
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(stack.getFluid());
        json.addProperty("fluid", id != null ? id.toString() : "minecraft:empty");
        json.addProperty("amount", stack.getAmount());
        return json;
    }

    protected abstract void serializeRecipeData(JsonObject json);
    protected abstract RecipeSerializer<?> getType();

    protected class Result implements FinishedRecipe {
        private final ResourceLocation id;
        private final BaseRecipeBuilder<T> builder;

        public Result(ResourceLocation id, BaseRecipeBuilder<T> builder) {
            this.id = id;
            this.builder = builder;
        }

        @Override
        public void serializeRecipeData(@NotNull JsonObject json) {
            builder.serializeRecipeData(json);
        }

        @Override
        public @NotNull ResourceLocation getId() {
            return id;
        }

        @Override
        public @NotNull RecipeSerializer<?> getType() {
            return builder.getType();
        }

        @Nullable
        @Override
        public JsonObject serializeAdvancement() { return null; }

        @Nullable
        @Override
        public ResourceLocation getAdvancementId() { return null; }
    }
}
//?}
