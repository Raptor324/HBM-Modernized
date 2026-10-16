package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.industrial.ItemBedrockOreGraded.Grade;
import com.hbm_m.worldgen.BedrockOreDensity.Type;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов печи Комбинации ({@code hbm_m:combination_oven}).
 *
 * <p>Порт 30 ручных рецептов из {@code data/hbm_m/recipes/combination_oven/}:</p>
 * <ul>
 *   <li>base → base_roasted × 6 типов (vitriol 50 mB, duration=200)</li>
 *   <li>primary → primary_roasted × 6 типов (vitriol 50 mB, duration=200)</li>
 *   <li>rad_byproduct → rad_roasted × 6 типов (vitriol 50 mB, duration=200)</li>
 *   <li>solvent_byproduct → solvent_roasted × 6 типов (vitriol 50 mB, duration=200)</li>
 *   <li>sulfuric_byproduct → sulfuric_roasted × 6 типов (vitriol 50 mB, duration=200)</li>
 * </ul>
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class CombinationOvenRecipeGenerator {

    private CombinationOvenRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        registerBedrockRoasts(writer);
    }

    // ─── Bedrock roast recipes ─────────────────────────────────────────────────

    /**
     * 5 конверсий × 6 типов = 30 рецептов combination_oven/bedrock_*.
     */
    private static void registerBedrockRoasts(Consumer<FinishedRecipe> writer) {
        for (Type type : Type.values()) {
            roast(writer, Grade.BASE,             Grade.BASE_ROASTED,       type);
            roast(writer, Grade.PRIMARY,          Grade.PRIMARY_ROASTED,    type);
            roast(writer, Grade.RAD_BYPRODUCT,    Grade.RAD_ROASTED,        type);
            roast(writer, Grade.SOLVENT_BYPRODUCT,Grade.SOLVENT_ROASTED,    type);
            roast(writer, Grade.SULFURIC_BYPRODUCT,Grade.SULFURIC_ROASTED,  type);
        }
    }

    /**
     * Один рецепт combination_oven: предмет + 50 mB vitriol → roasted-предмет, duration=200.
     */
    private static void roast(Consumer<FinishedRecipe> writer,
                               Grade inputGrade, Grade outputGrade, Type type) {
        ItemStack input  = bedrockOre(inputGrade,  type, 1);
        ItemStack output = bedrockOre(outputGrade, type, 1);
        if (input.isEmpty() || output.isEmpty()) return; // предмет не портирован — пропускаем

        FluidStack vitriol = FluidStack.create(ModFluids.VITRIOL.getSource(), 50L);
        String id = inputGrade.key + "_to_" + outputGrade.key + "_" + type.name().toLowerCase(Locale.ROOT);

        CombinationOvenRecipeBuilder
                .combinationOvenRecipe(Ingredient.of(input.getItem()), 1, vitriol, output, 200)
                .save(writer, "combination_oven/" + id);
    }

    /** Registry-lookup: {@code hbm_m:bedrock_ore_<grade>_<type>}. */
    private static ItemStack bedrockOre(Grade grade, Type type, int count) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("hbm_m",
                "bedrock_ore_" + grade.key + "_" + type.name().toLowerCase(Locale.ROOT));
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null || item == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(item, count);
    }
}
//?}
