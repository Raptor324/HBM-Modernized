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
 * Генератор JSON-рецептов дуговой печи ({@code hbm_m:arc_furnace}).
 *
 * <p>Порт 36 ручных рецептов из {@code data/hbm_m/recipes/arc_furnace/}:</p>
 * <ul>
 *   <li>6 × 3 = 18 «byproduct → arc» рецептов (count=2): rad/solvent/sulfuric × 6 типов руды</li>
 *   <li>6 × 3 = 18 «roasted → arc» рецептов (count=4): rad/solvent/sulfuric × 6 типов руды</li>
 * </ul>
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class ArcFurnaceRecipeGenerator {

    private ArcFurnaceRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        registerBedrockArcRecipes(writer);
    }

    // ─── Bedrock arc recipes ──────────────────────────────────────────────────────

    /**
     * Регистрирует 36 bedrock-arc-рецептов:
     * <ul>
     *   <li>byproduct (rad/solvent/sulfuric) × 6 типов → arc × 6 типов, count=2, duration=100</li>
     *   <li>roasted (rad/solvent/sulfuric) × 6 типов → arc × 6 типов, count=4, duration=100</li>
     * </ul>
     */
    private static void registerBedrockArcRecipes(Consumer<FinishedRecipe> writer) {
        for (Type type : Type.values()) {
            // byproduct → arc (count = 2)
            arcOre(writer, Grade.RAD_BYPRODUCT,      Grade.RAD_ARC,      type, 2, 100);
            arcOre(writer, Grade.SOLVENT_BYPRODUCT,  Grade.SOLVENT_ARC,  type, 2, 100);
            arcOre(writer, Grade.SULFURIC_BYPRODUCT, Grade.SULFURIC_ARC, type, 2, 100);

            // roasted → arc (count = 4)
            arcOre(writer, Grade.RAD_ROASTED,      Grade.RAD_ARC,      type, 4, 100);
            arcOre(writer, Grade.SOLVENT_ROASTED,  Grade.SOLVENT_ARC,  type, 4, 100);
            arcOre(writer, Grade.SULFURIC_ROASTED, Grade.SULFURIC_ARC, type, 4, 100);
        }
    }

    /**
     * Один рецепт: bedrock_ore_&lt;inputGrade&gt;_&lt;type&gt; → bedrock_ore_&lt;outputGrade&gt;_&lt;type&gt;.
     */
    private static void arcOre(Consumer<FinishedRecipe> writer,
                                 Grade inputGrade, Grade outputGrade,
                                 Type type, int outputCount, int duration) {
        ItemStack input  = bedrockOre(inputGrade,  type, 1);
        ItemStack output = bedrockOre(outputGrade, type, outputCount);
        if (input.isEmpty() || output.isEmpty()) return; // предмет не портирован — пропускаем

        String id = inputGrade.key + "_to_" + outputGrade.key + "_" + type.name().toLowerCase(Locale.ROOT);
        ArcFurnaceRecipeBuilder
                .arcFurnaceRecipe(Ingredient.of(input.getItem()), output, duration)
                .save(writer, "arc_furnace/" + id);
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
