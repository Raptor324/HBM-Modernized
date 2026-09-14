package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

import dev.architectury.fluid.FluidStack;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов паяльной станции ({@code hbm_m:soldering_station}).
 *
 * <p>Порт рецептов из {@code com.hbm.inventory.recipes.SolderingRecipes#registerDefaults()}
 * (legacy 1.7.10 defaults). TODO-рецепты (предметы, ещё не портированные) намеренно пропущены.</p>
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class SolderingRecipeGenerator {

    private SolderingRecipeGenerator() {}

    // Общие предметы (alias из оригинала SolderingRecipes.registerDefaults).
    private static final Item VAC_TUBE  = ModItems.VACUUM_TUBE.get();
    private static final Item CAP       = ModItems.CAPACITOR.get();
    private static final Item CAP_TAL   = ModItems.CAPACITOR_TANTALUM.get();
    private static final Item PCB       = ModItems.PCB.get();
    private static final Item CHIP      = ModItems.SILICON_CIRCUIT.get();
    private static final Item CHIP_BIS = ModItems.BISMOID_CHIP.get();
    private static final Item CHIP_Q    = ModItems.QUANTUM_CHIP.get();
    private static final Item CLOCK     = ModItems.ATOMIC_CLOCK.get();
    private static final Item CHASSIS   = ModItems.CONTROLLER_CHASSIS.get();

    public static void generate(Consumer<FinishedRecipe> writer) {
        // ANALOG_CIRCUIT — без жидкости
        solder(writer, "analog_circuit", new ItemStack(ModItems.ANALOG_CIRCUIT.get()),
                100, 100, null,
                new Pair[]{p(VAC_TUBE, 3), p(CAP, 2)},
                new Pair[]{p(PCB, 4)},
                new Pair[]{pTag("wires_fine/lead", 4)});

        // INTEGRATED_CIRCUIT — без жидкости
        solder(writer, "integrated_circuit", new ItemStack(ModItems.INTEGRATED_CIRCUIT.get()),
                200, 250, null,
                new Pair[]{p(ModItems.MICROCHIP.get(), 4)},
                new Pair[]{p(PCB, 4)},
                new Pair[]{pTag("wires_fine/lead", 4)});

        // ADVANCED_CIRCUIT — sulfuric acid 1000 mB
        solder(writer, "advanced_circuit", new ItemStack(ModItems.ADVANCED_CIRCUIT.get()),
                300, 1000, fluid(ModFluids.SULFURIC_ACID, 1000),
                new Pair[]{p(ModItems.MICROCHIP.get(), 16), p(CAP, 4)},
                new Pair[]{p(PCB, 8), p(ModMaterialItems.item(ModMaterials.RUBBER, MaterialShape.INGOT), 2)},
                new Pair[]{pTag("wires_fine/lead", 8)});

        // CAPACITOR_BOARD — peroxide 250 mB
        solder(writer, "capacitor_board", new ItemStack(ModItems.CAPACITOR_BOARD.get()),
                200, 300, fluid(ModFluids.PEROXIDE, 250),
                new Pair[]{p(CAP_TAL, 3)},
                new Pair[]{p(PCB, 1)},
                new Pair[]{pTag("wires_fine/lead", 3)});

        // BISMOID_CIRCUIT — solvent 1000 mB
        solder(writer, "bismoid_circuit", new ItemStack(ModItems.BISMOID_CIRCUIT.get()),
                400, 10_000, fluid(ModFluids.SOLVENT, 1000),
                new Pair[]{p(CHIP_BIS, 4), p(CHIP, 16), p(CAP, 24)},
                new Pair[]{p(PCB, 12), pTag("ingots/plastic", 2)},
                new Pair[]{pTag("wires_fine/lead", 12)});

        // QUANTUM_CIRCUIT — helium4 1000 mB
        solder(writer, "quantum_circuit", new ItemStack(ModItems.QUANTUM_CIRCUIT.get()),
                400, 100_000, fluid(ModFluids.HELIUM4, 1000),
                new Pair[]{p(CHIP_Q, 4), p(CHIP_BIS, 16), p(CLOCK, 4)},
                new Pair[]{p(PCB, 16), pTag("ingots/plastic", 4)},
                new Pair[]{pTag("wires_fine/lead", 16)});

        // CONTROLLER_ADVANCED — perfluoromethyl 4000 mB
        solder(writer, "controller_advanced", new ItemStack(ModItems.CONTROLLER_ADVANCED.get()),
                600, 25_000, fluid(ModFluids.PERFLUOROMETHYL, 4000),
                new Pair[]{p(CHIP_BIS, 16), p(CAP_TAL, 48), p(CLOCK, 1)},
                new Pair[]{p(CHASSIS, 1), p(ModItems.UPGRADE_SPEED_3.get(), 1)},
                new Pair[]{pTag("wires_fine/lead", 24)});

        // Upgrades tier1
        upgradeTier1(writer, ModItems.UPGRADE_SPEED_1.get(), ModItems.UPGRADE_SPEED_2.get());
        upgradeTier1(writer, ModItems.UPGRADE_EFFECT_1.get(), ModItems.UPGRADE_EFFECT_2.get());
        upgradeTier1(writer, ModItems.UPGRADE_POWER_1.get(), ModItems.UPGRADE_POWER_2.get());
        upgradeTier1(writer, ModItems.UPGRADE_FORTUNE_1.get(), ModItems.UPGRADE_FORTUNE_2.get());
        upgradeTier1(writer, ModItems.UPGRADE_AFTERBURN_1.get(), ModItems.UPGRADE_AFTERBURN_2.get());

        // Upgrades tier2
        upgradeTier2(writer, ModItems.UPGRADE_SPEED_2.get(), ModItems.UPGRADE_SPEED_3.get());
        upgradeTier2(writer, ModItems.UPGRADE_EFFECT_2.get(), ModItems.UPGRADE_EFFECT_3.get());
        upgradeTier2(writer, ModItems.UPGRADE_POWER_2.get(), ModItems.UPGRADE_POWER_3.get());
        upgradeTier2(writer, ModItems.UPGRADE_FORTUNE_2.get(), ModItems.UPGRADE_FORTUNE_3.get());
        upgradeTier2(writer, ModItems.UPGRADE_AFTERBURN_2.get(), ModItems.UPGRADE_AFTERBURN_3.get());
    }

    // ─── helpers ──────────────────────────────────────────────────────────────────

    private record Pair(Ingredient ing, int count) {}
    private static Pair p(Item item, int c) { return new Pair(Ingredient.of(item), c); }
    private static Pair p(Item item)        { return p(item, 1); }
    private static Pair pTag(String forgeTag, int c) {
        net.minecraft.tags.TagKey<Item> key = net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", forgeTag));
        return new Pair(Ingredient.of(key), c);
    }

    private static FluidStack fluid(ModFluids.FluidEntry entry, int mb) {
        return FluidStack.create(entry.getSource(), mb);
    }

    private static void solder(Consumer<FinishedRecipe> writer, String id, ItemStack output,
                                int duration, long consumption, FluidStack fluid,
                                Pair[] toppings, Pair[] pcb, Pair[] solder) {
        Ingredient[] t = toIns(toppings), p = toIns(pcb), s = toIns(solder);
        int[] tc = toCounts(toppings), pc = toCounts(pcb), sc = toCounts(solder);
        SolderingRecipeBuilder.solderingRecipe(t, tc, p, pc, s, sc, fluid, output, duration, consumption)
                .save(writer, "soldering_station/" + id);
    }

    private static Ingredient[] toIns(Pair[] pairs) {
        Ingredient[] ins = new Ingredient[pairs.length];
        for (int i = 0; i < pairs.length; i++) ins[i] = pairs[i].ing;
        return ins;
    }

    private static int[] toCounts(Pair[] pairs) {
        int[] cnts = new int[pairs.length];
        for (int i = 0; i < pairs.length; i++) cnts[i] = pairs[i].count;
        return cnts;
    }

    private static void upgradeTier1(Consumer<FinishedRecipe> writer, Item lower, Item higher) {
        solder(writer, lower.toString() + "_to_" + higher.toString() + "_t1", new ItemStack(higher),
                300, 10_000, null,
                new Pair[]{p(CHIP, 8), p(CAP, 4)},
                new Pair[]{p(lower), pTag("ingots/plastic", 4)},
                new Pair[]{});
    }

    private static void upgradeTier2(Consumer<FinishedRecipe> writer, Item lower, Item higher) {
        solder(writer, lower.toString() + "_to_" + higher.toString() + "_t2", new ItemStack(higher),
                400, 25_000, fluid(ModFluids.SOLVENT, 500),
                new Pair[]{p(CHIP, 16), p(CAP, 16)},
                new Pair[]{p(lower), pTag("ingots/rubber", 4)},
                new Pair[]{});
    }
}
//?}