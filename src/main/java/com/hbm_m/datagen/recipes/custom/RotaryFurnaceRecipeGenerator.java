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
 * <p>1:1 порт всех 12 рецептов оригинала 1.7.10 ({@code com.hbm.inventory.recipes.RotaryFurnaceRecipes#registerDefaults}): те же материалы/предметы/жидкости/
 * количества/длительности/расход пара. Входы-формы материала выражаются каноническим предметом формы (железный
 * слиток — ванильный {@code minecraft:iron_ingot}, фрагмент бедрок-руды — {@code hbm_m:bedrock_ore_fragment_iron},
 * порошки — {@code <материал>_powder}), {@code anyCoke()} — ингредиентом из трёх видов кокса. Выход — слиток
 * материала (шим восстанавливает из него {@code MaterialStack} через {@code Mats.getShape}). Пушечная бронза E8:
 * канонический Master-вариант — 1 алюминий (цинк есть только в Space-форке и остаётся его датапак-оверрайдом).
 * Жидкостные стаки создаются через {@link FluidStack#create} из {@link ModFluids} (mB). Чистый ванильный 1.20.1 код
 * внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class RotaryFurnaceRecipeGenerator {

    private RotaryFurnaceRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        // STEEL x1: IRON INGOT + COAL -> STEEL x1 (duration 100, steam 100)
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[]{ ing(Items.IRON_INGOT), ing(Items.COAL) },
                new int[]{ 1, 1 },
                out(ModMaterials.STEEL, 1), 100, 100
        ).save(writer, "rotary_furnace/steel_ingot_coal");

        // STEEL x1: IRON INGOT + ANY COKE -> STEEL x1 (duration 100, steam 100)
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[]{ ing(Items.IRON_INGOT), anyCoke() },
                new int[]{ 1, 1 },
                out(ModMaterials.STEEL, 1), 100, 100
        ).save(writer, "rotary_furnace/steel_ingot_coke");

        // STEEL x2: 9x IRON FRAGMENT + COAL -> STEEL x2 (duration 200, steam 25)
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[]{ ing(ModItems.BEDROCK_ORE_FRAGMENT_IRON.get()), ing(Items.COAL) },
                new int[]{ 9, 1 },
                out(ModMaterials.STEEL, 2), 200, 25
        ).save(writer, "rotary_furnace/steel_fragment_coal");

        // STEEL x3: 9x IRON FRAGMENT + ANY COKE -> STEEL x3 (duration 200, steam 25)
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[]{ ing(ModItems.BEDROCK_ORE_FRAGMENT_IRON.get()), anyCoke() },
                new int[]{ 9, 1 },
                out(ModMaterials.STEEL, 3), 200, 25
        ).save(writer, "rotary_furnace/steel_fragment_coke");

        // STEEL x4: 9x IRON FRAGMENT + ANY COKE + FLUX -> STEEL x4 (duration 400, steam 25)
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[]{ ing(ModItems.BEDROCK_ORE_FRAGMENT_IRON.get()), anyCoke(), ing(ModItems.POWDER_FLUX.get()) },
                new int[]{ 9, 1, 1 },
                out(ModMaterials.STEEL, 4), 400, 25
        ).save(writer, "rotary_furnace/steel_fragment_coke_flux");

        // DESH x1: LIGHTOIL(100) + DESH_READY_POWDER -> DESH x1 (duration 100, steam 200)
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[]{ ing(ModItems.POWDER_DESH_READY.get()) },
                new int[]{ 1 },
                fluid(ModFluids.LIGHTOIL, 100),
                out(ModMaterials.DESH, 1), 100, 200
        ).save(writer, "rotary_furnace/desh_light_oil");

        // GUNMETAL x4: 3x COPPER INGOT + 1x ALUMINIUM INGOT -> GUNMETAL x4 (duration 200, steam 100)
        // E8 (форк — только с Space-аддоном): цинк вместо алюминия; канон FFA (Master) = алюминий.
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[]{ ing(Items.COPPER_INGOT), ing(ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.INGOT)) },
                new int[]{ 3, 1 },
                out(ModMaterials.GUNMETAL, 4), 200, 100
        ).save(writer, "rotary_furnace/gunmetal");

        // WEAPONSTEEL x1: GAS_COKER(100) + STEEL INGOT + 2x FLUX -> WEAPONSTEEL x1 (duration 200, steam 400)
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[]{ ing(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)), ing(ModItems.POWDER_FLUX.get()) },
                new int[]{ 1, 2 },
                fluid(ModFluids.GAS_COKER, 100),
                out(ModMaterials.WEAPONSTEEL, 1), 200, 400
        ).save(writer, "rotary_furnace/weaponsteel_coker_gas");

        // SATURNITE x2: REFORMGAS(250) + 4x DURA_STEEL POWDER + COPPER POWDER -> SATURNITE x2 (duration 200, steam 400)
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[]{ ing(ModMaterialItems.item(ModMaterials.DURA_STEEL, MaterialShape.POWDER)),
                                  ing(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.POWDER)) },
                new int[]{ 4, 1 },
                fluid(ModFluids.REFORMGAS, 250),
                out(ModMaterials.SATURNITE, 2), 200, 400
        ).save(writer, "rotary_furnace/saturnite_reformgas");

        // SATURNITE x4: REFORMGAS(250) + 4x DURA_STEEL POWDER + COPPER POWDER + BORAX -> SATURNITE x4 (duration 200, steam 300)
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[]{ ing(ModMaterialItems.item(ModMaterials.DURA_STEEL, MaterialShape.POWDER)),
                                  ing(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.POWDER)),
                                  ing(ModItems.BORAX.get()) },
                new int[]{ 4, 1, 1 },
                fluid(ModFluids.REFORMGAS, 250),
                out(ModMaterials.SATURNITE, 4), 200, 300
        ).save(writer, "rotary_furnace/saturnite_reformgas_borax");

        // ALUMINIUM x2: SODIUM_ALUMINATE(150) -> ALUMINIUM x2 (duration 100, steam 400) — без предметных входов
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[0], new int[0],
                fluid(ModFluids.SODIUM_ALUMINATE, 150),
                out(ModMaterials.ALUMINUM, 2), 100, 400
        ).save(writer, "rotary_furnace/aluminium_sodium_aluminate");

        // ALUMINIUM x3: SODIUM_ALUMINATE(150) + 2x FLUX -> ALUMINIUM x3 (duration 40, steam 200)
        RotaryFurnaceRecipeBuilder.rotaryFurnaceRecipe(
                new Ingredient[]{ ing(ModItems.POWDER_FLUX.get()) },
                new int[]{ 2 },
                fluid(ModFluids.SODIUM_ALUMINATE, 150),
                out(ModMaterials.ALUMINUM, 3), 40, 200
        ).save(writer, "rotary_furnace/aluminium_sodium_aluminate_flux");
    }

    /** Слиток материала как выход ({@code new MaterialStack(MAT, INGOT.q(count))} статической таблицы). */
    private static ItemStack out(ModMaterials mat, int count) {
        return new ItemStack(ModMaterialItems.item(mat, MaterialShape.INGOT), count);
    }

    /** {@code anyCoke()}: угольный, нефтяной и буроугольный кокс. */
    private static Ingredient anyCoke() {
        return Ingredient.of(ModItems.COAL_COKE.get(), ModItems.COKE_PETROLEUM.get(), ModItems.LIGNITE_COKE.get());
    }

    private static Ingredient ing(net.minecraft.world.item.Item... items) {
        return Ingredient.of(items);
    }

    private static FluidStack fluid(ModFluids.FluidEntry entry, int amountMb) {
        return FluidStack.create(entry.getSource(), (long) amountMb);
    }
}
//?}
