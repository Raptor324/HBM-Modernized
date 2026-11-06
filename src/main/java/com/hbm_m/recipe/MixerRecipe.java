package com.hbm_m.recipe;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Datapack-facing миксер-рецепт ({@code hbm_m:mixer}).
 *
 * <p>Порт 1.7.10 {@code com.hbm.inventory.recipes.MixerRecipes}: миксер объединяет до двух жидкостных
 * входов и один необязательный твёрдый вход (слот слева) в один жидкостный выход, потребляя энергию.
 * Два {@link #inputA} и {@link #inputB} задаются как Architectury {@link dev.architectury.fluid.FluidStack}
 * (с количеством в mB, оба опциональны — пустой стак = «нет входа»); порядок {@link #matches}
 * симметричный — любой бак может подойти к любой роли (trueTankA используется машиной
 * для выбора, какой бак слить). Твёрдый вход {@link #solidInput} — {@link Ingredient} (предмет,
 * предметный тег или «любой из»), количество задаётся {@link #solidCount} (по умолчанию 1).
 * Рецепт может быть чисто твёрдым (оба жидкостных входа пусты), как FISHOIL/BITUMEN из FFA-таблицы.</p>
 *
 * <p><b>Замена статике:</b> прежний {@code MixerRecipes} хранил {@code List<MixerRecipe>} в Java-статике
 * через {@code static {}}, что блокировало data-driven рецепты. Теперь источник правды —
 * {@code RecipeManager} (JSON, генерируется датагеном {@code MixerRecipeGenerator}), а
 * {@code MixerRecipes.getRecipes(RecipeManager)} — рантайм-агрегатор, группирующий JSON-рецепты
 * по выходной жидкости в формат FFA-машины/GUI/JEI.</p>
 *
 * <p>JSON-формат:</p>
 * <pre>{@code
 * {
 *   "type": "hbm_m:mixer",
 *   "input_a": { "fluid": "hbm_m:vitriol", "amount": 1000 },
 *   "input_b": { "fluid": "minecraft:water", "amount": 1000 },
 *   "solid":   { "item": "hbm_m:niter", "count": 1 },
 *   "output":  { "fluid": "hbm_m:sulfuric_acid", "amount": 2000 },
 *   "duration": 100,
 *   "energy_per_tick": 50
 * }
 * }</pre>
 *
 * <p>Все входы опциональны (ключ отсутствует = пустой стак). Формы {@code "solid"} (count по умолчанию 1):</p>
 * <ul>
 *   <li>{@code { "item": "<id>", "count": n }} — один предмет;</li>
 *   <li>{@code { "tag": "<tagid>", "count": n }} — предметный тег (например {@code minecraft:fishes});</li>
 *   <li>{@code { "any_of": ["<id>", ...], "count": n }} — любой из перечисленных предметов.</li>
 * </ul>
 *
 * <p>Отдельный тип рецепта (а не использование {@code CombinationOvenRecipe}/{@code CrystallizerRecipe})
 * нужен, т.к. миксер — «до двух жидкостей + опциональный предмет» с выбором рецепта по выходной
 * жидкости и переключением вариантов в GUI (FFA-механика {@code MixerRecipes}).</p>
 */
public class MixerRecipe extends com.hbm_m.platform.recipe.PlatformRecipe {

    private final dev.architectury.fluid.FluidStack inputA;
    private final dev.architectury.fluid.FluidStack inputB;
    private final dev.architectury.fluid.FluidStack output;
    @Nullable private final Ingredient solidInput;
    private final int solidCount;
    private final int duration;
    private final long energyPerTick;

    public MixerRecipe(net.minecraft.resources.ResourceLocation id,
                       dev.architectury.fluid.FluidStack inputA, dev.architectury.fluid.FluidStack inputB,
                       dev.architectury.fluid.FluidStack output,
                       int duration, long energyPerTick) {
        this(id, inputA, inputB, output, null, 0, duration, energyPerTick);
    }

    public MixerRecipe(net.minecraft.resources.ResourceLocation id,
                       dev.architectury.fluid.FluidStack inputA, dev.architectury.fluid.FluidStack inputB,
                       dev.architectury.fluid.FluidStack output,
                       @Nullable Ingredient solidInput, int solidCount,
                       int duration, long energyPerTick) {
        super(id);
        this.inputA = inputA != null ? inputA : dev.architectury.fluid.FluidStack.empty();
        this.inputB = inputB != null ? inputB : dev.architectury.fluid.FluidStack.empty();
        this.output = output != null ? output : dev.architectury.fluid.FluidStack.empty();
        this.solidInput = solidInput;
        this.solidCount = solidInput != null ? Math.max(1, solidCount) : 0;
        this.duration = Math.max(1, duration);
        this.energyPerTick = Math.max(0, energyPerTick);
    }

    public dev.architectury.fluid.FluidStack getInputA() { return inputA; }
    public dev.architectury.fluid.FluidStack getInputB() { return inputB; }
    public dev.architectury.fluid.FluidStack getOutput() { return output; }
    public int getDuration() { return duration; }
    public long getEnergyPerTick() { return energyPerTick; }

    /** Есть ли твёрдый вход (слот слева). */
    public boolean hasSolidInput() { return solidInput != null; }

    /** Твёрдый вход ({@code null} = рецепту предмет не нужен). */
    @Nullable
    public Ingredient getSolidInput() { return solidInput; }

    /** Количество твёрдого входа (0, если входа нет; минимум 1, если есть). */
    public int getSolidInputCount() { return solidCount; }

    /** Симметричный match: порядок баков не важен. */
    public boolean matches(net.minecraft.world.level.material.Fluid tankA, net.minecraft.world.level.material.Fluid tankB) {
        if (tankA == null || tankB == null) return false;
        boolean direct = sameSubstance(tankA, inputA.getFluid()) && sameSubstance(tankB, inputB.getFluid());
        boolean swapped = sameSubstance(tankA, inputB.getFluid()) && sameSubstance(tankB, inputA.getFluid());
        return direct || swapped;
    }

    /** true, если {@code tankA} соответствует {@link #inputA} (т.е. баки НЕ переставлены). */
    public boolean isDirectOrder(net.minecraft.world.level.material.Fluid tankA) {
        return tankA != null && sameSubstance(tankA, inputA.getFluid());
    }

    private static boolean sameSubstance(net.minecraft.world.level.material.Fluid a,
                                          net.minecraft.world.level.material.Fluid b) {
        if (a == b) return true;
        return com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(a, b);
    }

    @Override
    public boolean matchesRecipe(com.hbm_m.platform.recipe.RecipeInputWrapper container, net.minecraft.world.level.Level level) {
        // Выбор рецепта идёт по выходной жидкости + индексу (MixerRecipes), а не стандартным мэтчингом.
        return false;
    }

    @Override
    public ItemStack assembleSafe() { return ItemStack.EMPTY; }

    @Override
    public ItemStack getResultItemSafe() { return ItemStack.EMPTY; }

    @Override
    public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public net.minecraft.world.item.crafting.RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements net.minecraft.world.item.crafting.RecipeType<MixerRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "mixer";
    }

    public static class Serializer extends com.hbm_m.platform.recipe.PlatformRecipeSerializer<MixerRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        public static final net.minecraft.resources.ResourceLocation ID = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID, "mixer");

        @Override
        public MixerRecipe readJson(net.minecraft.resources.ResourceLocation recipeId, com.google.gson.JsonObject json) {
            dev.architectury.fluid.FluidStack inputA = readFluid(json, "input_a");
            dev.architectury.fluid.FluidStack inputB = readFluid(json, "input_b");
            dev.architectury.fluid.FluidStack output = readFluid(json, "output");
            Ingredient solid = null;
            int solidCount = 1;
            if (json.has("solid")) {
                com.google.gson.JsonObject s = net.minecraft.util.GsonHelper.getAsJsonObject(json, "solid");
                solid = readSolid(s, recipeId);
                solidCount = net.minecraft.util.GsonHelper.getAsInt(s, "count", 1);
            }
            int duration = net.minecraft.util.GsonHelper.getAsInt(json, "duration", 100);
            long energy = net.minecraft.util.GsonHelper.getAsLong(json, "energy_per_tick", 0L);
            return new MixerRecipe(recipeId, inputA, inputB, output, solid, solidCount, duration, energy);
        }

        @Override
        public MixerRecipe readNetwork(net.minecraft.resources.ResourceLocation recipeId, net.minecraft.network.FriendlyByteBuf buf) {
            dev.architectury.fluid.FluidStack inputA = com.hbm_m.platform.recipe.RecipeHooks.readFluidStack(buf);
            dev.architectury.fluid.FluidStack inputB = com.hbm_m.platform.recipe.RecipeHooks.readFluidStack(buf);
            dev.architectury.fluid.FluidStack output = com.hbm_m.platform.recipe.RecipeHooks.readFluidStack(buf);
            Ingredient solid = buf.readBoolean() ? com.hbm_m.platform.recipe.RecipeHooks.readIngredient(buf) : null;
            int solidCount = buf.readVarInt();
            int duration = buf.readVarInt();
            long energy = buf.readVarLong();
            return new MixerRecipe(recipeId, inputA, inputB, output, solid, solidCount, duration, energy);
        }

        @Override
        public void writeNetwork(net.minecraft.network.FriendlyByteBuf buf, MixerRecipe recipe) {
            com.hbm_m.platform.recipe.RecipeHooks.writeFluidStack(buf, recipe.inputA);
            com.hbm_m.platform.recipe.RecipeHooks.writeFluidStack(buf, recipe.inputB);
            com.hbm_m.platform.recipe.RecipeHooks.writeFluidStack(buf, recipe.output);
            buf.writeBoolean(recipe.solidInput != null);
            if (recipe.solidInput != null) com.hbm_m.platform.recipe.RecipeHooks.writeIngredient(buf, recipe.solidInput);
            buf.writeVarInt(recipe.solidCount);
            buf.writeVarInt(recipe.duration);
            buf.writeVarLong(recipe.energyPerTick);
        }

        private static dev.architectury.fluid.FluidStack readFluid(com.google.gson.JsonObject json, String key) {
            if (!json.has(key)) return dev.architectury.fluid.FluidStack.empty();
            com.google.gson.JsonObject obj = net.minecraft.util.GsonHelper.getAsJsonObject(json, key);
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(
                    net.minecraft.util.GsonHelper.getAsString(obj, "fluid"));
            long amount = net.minecraft.util.GsonHelper.getAsLong(obj, "amount", 0L);
            if (id == null) return dev.architectury.fluid.FluidStack.empty();
            return com.hbm_m.platform.recipe.RecipeHooks.fluidStackOf(id, amount);
        }

        /**
         * Твёрдый вход: {@code {"item": id}} | {@code {"tag": id}} | {@code {"any_of": [id, ...]}}
         * (поле {@code count} читает вызывающая сторона). Неизвестные предметы/теги — ошибка загрузки JSON.
         */
        private static Ingredient readSolid(com.google.gson.JsonObject solid, net.minecraft.resources.ResourceLocation recipeId) {
            if (solid.has("item")) {
                return Ingredient.of(itemById(net.minecraft.util.GsonHelper.getAsString(solid, "item"), recipeId));
            }
            if (solid.has("tag")) {
                net.minecraft.resources.ResourceLocation tagId = net.minecraft.resources.ResourceLocation.tryParse(
                        net.minecraft.util.GsonHelper.getAsString(solid, "tag"));
                if (tagId == null) {
                    throw new com.google.gson.JsonSyntaxException("mixer: invalid tag in " + recipeId);
                }
                return Ingredient.of(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagId));
            }
            if (solid.has("any_of")) {
                com.google.gson.JsonArray arr = net.minecraft.util.GsonHelper.getAsJsonArray(solid, "any_of");
                net.minecraft.world.item.Item[] items = new net.minecraft.world.item.Item[arr.size()];
                for (int i = 0; i < arr.size(); i++) {
                    items[i] = itemById(net.minecraft.util.GsonHelper.convertToString(arr.get(i), "any_of"), recipeId);
                }
                return Ingredient.of(items);
            }
            throw new com.google.gson.JsonSyntaxException("mixer: solid needs \"item\", \"tag\" or \"any_of\" in " + recipeId);
        }

        private static net.minecraft.world.item.Item itemById(String id, net.minecraft.resources.ResourceLocation recipeId) {
            net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(id);
            net.minecraft.world.item.Item item = rl != null ? net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl) : null;
            if (item == null) {
                throw new com.google.gson.JsonSyntaxException("mixer: unknown item '" + id + "' in " + recipeId);
            }
            return item;
        }
    }
}
