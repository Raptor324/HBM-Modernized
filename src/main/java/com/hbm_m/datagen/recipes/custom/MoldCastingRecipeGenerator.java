package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.inventory.material.MaterialType;
import com.hbm_m.item.material.ItemCastMold;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Генератор {@code hbm_m:mold_casting} (data-driven JSON).
 *
 * <p>Перебирает все {@link ItemCastMold.MoldType} × все {@link MaterialType} и для каждой
 * пары разово разрешает выходной {@link ItemStack} по forge-тегам ({@code forge:ingots/X},
 * {@code forge:nuggets/X}, {@code forge:storage_blocks/X}) или по строковому id
 * ({@code hbm_m:plate_<mat>}, {@code hbm_m:wire_<mat>}, ...). Если выход НЕ пустой —
 * создаёт отдельный JSON-рецепт.</p>
 *
 * <p><b>Логика разрешения выхода — прямой порт прежнего {@code MoldCastingRecipes.getOutput}
 * метода</b>, встроенного сюда, чтобы статический {@code MoldCastingRecipes} можно было
 * удалить после переезда на data-driven. Runtime-код {@code MachineFoundryBasinBlockEntity}
 * больше не обращается к статическому реестру — он ищет {@link com.hbm_m.recipe.MoldCastingRecipe}
 * через {@code RecipeManager} (JSON).</p>
 *
 * <p>Каждое {@code MaterialType} из {@code MaterialType.values()} (27 значений) × 33
 * {@code MoldType} = 891 пара; после фильтра пустых выходов остаётся ~80 фактических рецептов.</p>
 */
public final class MoldCastingRecipeGenerator {

    public static void generate(Consumer<FinishedRecipe> writer) {
        for (ItemCastMold.MoldType mold : ItemCastMold.MoldType.values()) {
            for (MaterialType mat : MaterialType.values()) {
                Output out = resolveOutput(mold, mat);
                if (out == Output.EMPTY) continue;
                // Эмит JSON-рецепта: builder сам различит tag vs item (через ingredient.toJson()).
                new MoldCastingRecipeBuilder(mold, mat, out.ingredient, out.count)
                        .save(writer, "mold_casting/"
                                + mold.name().toLowerCase(Locale.ROOT)
                                + "_" + mat.name);
            }
        }
    }

    /** Контейнер выхода: {@link Ingredient} + {@code count}; {@link #EMPTY} — нет рецепта. */
    private static final class Output {
        final net.minecraft.world.item.crafting.Ingredient ingredient;
        final int count;
        final boolean isTag;

        static final Output EMPTY = new Output(net.minecraft.world.item.crafting.Ingredient.EMPTY, 0, false);

        Output(net.minecraft.world.item.crafting.Ingredient ingredient, int count, boolean isTag) {
            this.ingredient = ingredient;
            this.count = count;
            this.isTag = isTag;
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  Логика разрешения выхода: прямой порт прежнего MoldCastingRecipes.getOutput,
    //  расширенная для теговых выходов. Возвращает либо конкретный {@link ItemStack}
    //  (mod-self предмет), либо {@link #tagOutput(...)}/{@link #emptyIndicator()},
    //  кодирующий теговый выход (резолвится в runtime при загрузке датапака).
    // ══════════════════════════════════════════════════════════════════
    private static Output resolveOutput(ItemCastMold.MoldType mold, MaterialType mat) {
        return switch (mold) {
            case PLATE       -> itemOrEmpty(byId("plate_", mat, 1));
            case PLATES      -> itemOrEmpty(byId("plate_", mat, 9));
            case PLATE_CAST  -> itemOrEmpty(castPlate(mat, 1));
            case PLATES_CAST -> itemOrEmpty(castPlate(mat, 3));
            case INGOT       -> tagOutput("forge:ingots/"         + tagName(mat), 1);
            case INGOTS      -> tagOutput("forge:ingots/"         + tagName(mat), 9);
            case NUGGET      -> tagOutput("forge:nuggets/"        + tagName(mat), 1);
            case BLOCK       -> tagOutput("forge:storage_blocks/" + tagName(mat), 1);
            case WIRE        -> itemOrEmpty(byId("wire_", mat, 8));
            case WIRE_DENSE  -> itemOrEmpty(byId("wire_dense_", mat, 1));
            case WIRES_DENSE -> itemOrEmpty(byId("wire_dense_", mat, 9));
            case SHELL       -> itemOrEmpty(byId("shell_", mat, 1));
            case PIPE        -> itemOrEmpty(byId("pipe_", mat, 1));
            case BILLET      -> itemOrEmpty(byId("billet_", mat, 1));
            default          -> Output.EMPTY;
        };
    }

    /** Использует {@code MaterialType.name} как tag-суффикс (с тем же fallback的其他 spelling не поддерживается). */
    private static String tagName(MaterialType mat) {
        return mat.name;
    }

    /** Теговый выход: {@code forge:ingots/<mat>} — резолвится в runtime. */
    private static Output tagOutput(String tagId, int count) {
        net.minecraft.tags.TagKey<Item> tag = net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.parse(tagId));
        return new Output(net.minecraft.world.item.crafting.Ingredient.of(tag), count, true);
    }

    private static Output itemOrEmpty(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Output.EMPTY;
        return new Output(net.minecraft.world.item.crafting.Ingredient.of(stack),
                stack.getCount(), false);
    }

    private static ItemStack castPlate(MaterialType mat, int count) {
        ItemStack plate = mat.getCastPlate(count);
        return plate != null ? plate : ItemStack.EMPTY;
    }

    private static ItemStack byId(String prefix, MaterialType mat, int count) {
        for (String candidate : nameCandidates(mat)) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("hbm_m", prefix + candidate);
            if (BuiltInRegistries.ITEM.containsKey(id)) {
                return new ItemStack(BuiltInRegistries.ITEM.get(id), count);
            }
        }
        return ItemStack.EMPTY;
    }

    /** Alternate spellings/names used across this mod's item ids for the same material. */
    private static List<String> nameCandidates(MaterialType mat) {
        List<String> out = new ArrayList<>(3);
        out.add(mat.name);
        if (mat.name.equals("aluminium")) out.add("aluminum");
        if (mat.name.equals("cmb"))       out.add("combine_steel");
        if (mat.name.equals("alloy"))     out.add("advanced_alloy");
        return out;
    }
}
//?}
