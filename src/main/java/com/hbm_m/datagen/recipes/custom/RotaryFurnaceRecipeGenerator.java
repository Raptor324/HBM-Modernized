package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import dev.architectury.fluid.FluidStack;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов вращающейся печи ({@code hbm_m:rotary_furnace}).
 *
 * <p>Порт 10 рецептов из удалённого статического {@code RotaryFurnaceRecipes} (static-блок;
 * оригинал 1.7.10 — {@code com.hbm.inventory.recipes.RotaryFurnaceRecipes}). Жидкостные стаки
 * создаются через {@link FluidStack#create} из {@link ModFluids} (mB). Чистый ванильный 1.20.1 код
 * внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class RotaryFurnaceRecipeGenerator {

    private RotaryFurnaceRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        // IRON + COAL -> STEEL x1 (duration 100)
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[]{ ing(Items.IRON_INGOT), ing(Items.COAL) },
                new int[]{ 1, 1 },
                new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)), 100
        ).save(writer, "rotary_furnace/steel");

        // 9x iron fragment (nugget) + COAL -> STEEL x2 (duration 200)
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[]{ ing(Items.IRON_NUGGET), ing(Items.COAL) },
                new int[]{ 9, 1 },
                new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT), 2), 200
        ).save(writer, "rotary_furnace/steel_nuggets");

        // 9x iron fragment + COAL + flux -> STEEL x4 (duration 400)
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[]{ ing(Items.IRON_NUGGET), ing(Items.COAL), ing(ModItems.POWDER_FLUX.get()) },
                new int[]{ 9, 1, 1 },
                new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT), 4), 400
        ).save(writer, "rotary_furnace/steel_nuggets_flux");

        // LIGHTOIL(100) + desh_ready_powder -> DESH x1 (duration 100)
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[]{ ing(ModItems.POWDER_DESH_READY.get()) },
                new int[]{ 1 },
                fluid(ModFluids.LIGHTOIL, 100),
                new ItemStack(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.INGOT)), 100
        ).save(writer, "rotary_furnace/desh");

        // 3x CU + 1x AL -> GUNMETAL x4 (duration 200)
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[]{ ing(Items.COPPER_INGOT), ing(ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.INGOT)) },
                new int[]{ 3, 1 },
                new ItemStack(ModMaterialItems.item(ModMaterials.GUNMETAL, MaterialShape.INGOT), 4), 200
        ).save(writer, "rotary_furnace/gunmetal");

        // GAS_COKER(100) + STEEL + 2x flux -> WEAPONSTEEL x1 (duration 200)
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[]{ ing(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)), ing(ModItems.POWDER_FLUX.get()) },
                new int[]{ 1, 2 },
                fluid(ModFluids.GAS_COKER, 100),
                new ItemStack(ModMaterialItems.item(ModMaterials.WEAPONSTEEL, MaterialShape.INGOT)), 200
        ).save(writer, "rotary_furnace/weaponsteel");

        // REFORMGAS(250) + 4x dura_steel dust + Cu dust -> SATURNITE x2 (duration 200)
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[]{ ing(ModMaterialItems.item(ModMaterials.DURA_STEEL, MaterialShape.POWDER)), ing(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.POWDER)) },
                new int[]{ 4, 1 },
                fluid(ModFluids.REFORMGAS, 250),
                new ItemStack(ModMaterialItems.item(ModMaterials.SATURNITE, MaterialShape.INGOT), 2), 200
        ).save(writer, "rotary_furnace/saturnite");

        // REFORMGAS(250) + 4x dura_steel dust + Cu dust + borax -> SATURNITE x4 (duration 300)
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[]{ ing(ModMaterialItems.item(ModMaterials.DURA_STEEL, MaterialShape.POWDER)), ing(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.POWDER)),
                                  ing(ModItems.BORAX.get()) },
                new int[]{ 4, 1, 1 },
                fluid(ModFluids.REFORMGAS, 250),
                new ItemStack(ModMaterialItems.item(ModMaterials.SATURNITE, MaterialShape.INGOT), 4), 300
        ).save(writer, "rotary_furnace/saturnite_borax");

        // SODIUM_ALUMINATE(150) -> ALUMINUM x2 (duration 100) — без предметных входов
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[0], new int[0],
                fluid(ModFluids.SODIUM_ALUMINATE, 150),
                new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.INGOT), 2), 100
        ).save(writer, "rotary_furnace/aluminum");

        // SODIUM_ALUMINATE(150) + 2x flux -> ALUMINUM x3 (duration 40)
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[]{ ing(ModItems.POWDER_FLUX.get()) },
                new int[]{ 2 },
                fluid(ModFluids.SODIUM_ALUMINATE, 150),
                new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.INGOT), 3), 40
        ).save(writer, "rotary_furnace/aluminum_flux");
    }

    private static Ingredient ing(net.minecraft.world.item.Item item) {
        return Ingredient.of(item);
    }

    private static FluidStack fluid(ModFluids.FluidEntry entry, int amountMb) {
        return FluidStack.create(entry.getSource(), (long) amountMb);
    }
}
//?}
