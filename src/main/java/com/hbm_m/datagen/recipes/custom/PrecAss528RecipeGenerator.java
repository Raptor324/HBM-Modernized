package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.BrokenItem;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.PartialNBTIngredient;

import static com.hbm_m.datagen.recipes.custom.OreDictIngredients.*;

/**
 * 1:1 {@code PrecAssRecipes.registerDefaults}, Block {@code if(GeneralConfig.enable528)}: Chips, Steuerungen,
 * Upgrades und Overdrive aus dem Praezisionsassembler, jeweils als {@code registerPair} - das Rezept liefert mit
 * {@code chance}% das Produkt, sonst den kaputten Gegenstand; das zweite Rezept ({@code .recycle}) gewinnt aus dem
 * kaputten Gegenstand je Zutat mit {@code reclaim}% zurueck (Expensive-Modus: 50%). Alle nur im 528-Modus
 * ({@link ConfigRecipes}). AUTOMATISCH ERZEUGT durch {@code rc/precass528.py}.
 */
public final class PrecAss528RecipeGenerator {

    private PrecAss528RecipeGenerator() {}

    /** Original {@code BrokenItem.make(output)}. */
    private static ItemStack broken(String id) {
        return BrokenItem.make(item(id));
    }

    /** Original {@code new NBTStack(BrokenItem.make(output))}. */
    private static Ingredient brokenIn(String id) {
        ItemStack b = broken(id);
        return PartialNBTIngredient.of(b.getItem(), b.getTag());
    }

    /**
     * 1:1 {@code precass.crystalcircuit} (auch ausserhalb des 528-Modus): Orbital-Baugruppe fuer die 0G-Fabrik,
     * 50% (Expensive 25%) gelingt sie, sonst kaputt; das Recycling gewinnt alles zurueck.
     */
    public static void crystalCircuit(Consumer<FinishedRecipe> writer) {
        String out = "hbm_m:orbital_assembly_crystal_circuit";
        ConfigRecipes.variants(writer)
                .base(w -> new PrecAssRecipeGenerator.Builder(600, 20000L)
                    .in(Ingredient.of(item("hbm_m:quantum_chip")), 4)
                    .in(Ingredient.of(item("hbm_m:cft_ingot")), 4)
                    .in(Ingredient.of(item("hbm_m:pcb")), 16)
                    .in(ore("oredict/wire_fine/gold"), 32)
                    .out(1F, stack(out, 1), 50, broken(out), 50)
                    .save(w, "precass/crystalcircuit"))
                .variant(v -> new PrecAssRecipeGenerator.Builder(600, 20000L)
                    .in(Ingredient.of(item("hbm_m:quantum_chip")), 8)
                    .in(Ingredient.of(item("hbm_m:capacitor_tantalum")), 16)
                    .in(Ingredient.of(item("hbm_m:cft_ingot")), 4)
                    .in(Ingredient.of(item("hbm_m:pcb")), 16)
                    .in(ore("oredict/wire_dense/bscco"), 4)
                    .out(1F, stack(out, 1), 25, broken(out), 75)
                    .save(v, "precass/crystalcircuit"), "expensive")
                .save();

        // precass.crystalcircuit.recycle (100%)
        ConfigRecipes.variants(writer)
                .base(w -> new PrecAssRecipeGenerator.Builder(600, 20000L)
                    .icon(broken(out))
                    .in(brokenIn(out), 1)
                    .out(1F, stack("hbm_m:quantum_chip", 4), 1)
                    .out(1F, stack("hbm_m:cft_ingot", 4), 1)
                    .out(1F, stack("hbm_m:pcb", 16), 1)
                    .out(1F, stack("hbm_m:wire_gold", 32), 1)
                    .save(w, "precass/crystalcircuit.recycle"))
                .variant(v -> new PrecAssRecipeGenerator.Builder(600, 20000L)
                    .icon(broken(out))
                    .in(brokenIn(out), 1)
                    .out(1F, stack("hbm_m:quantum_chip", 8), 1)
                    .out(1F, stack("hbm_m:capacitor_tantalum", 16), 1)
                    .out(1F, stack("hbm_m:cft_ingot", 4), 1)
                    .out(1F, stack("hbm_m:pcb", 16), 1)
                    .out(1F, stack("hbm_m:wire_dense_bscco", 4), 1)
                    .save(v, "precass/crystalcircuit.recycle"), "expensive")
                .save();
    }

    public static void generate(Consumer<FinishedRecipe> writer) {
        // precass.chip
        new PrecAssRecipeGenerator.Builder(100, 200L)
                .in(Ingredient.of(item("hbm_m:silicon_circuit")), 1)
                .in(Ingredient.of(item("hbm_m:plate_polymer")), 3)
                .in(ore("oredict/wire_fine/gold"), 4)
                .out(1F, stack("hbm_m:microchip", 1), 90, broken("hbm_m:microchip"), 10)
                .pool("528.chip")
                .save(ConfigRecipes.when(writer, "528"), "precass/chip");

        // precass.chip.recycle (90%, Expensive 50%)
        ConfigRecipes.variants(writer)
                .base(w -> new PrecAssRecipeGenerator.Builder(100, 200L)
                    .icon(broken("hbm_m:microchip"))
                    .in(brokenIn("hbm_m:microchip"), 1)
                    .out(0.9F, stack("hbm_m:silicon_circuit", 1), 1)
                    .out(0.9F, stack("hbm_m:plate_polymer", 3), 1)
                    .out(0.9F, stack("hbm_m:wire_gold", 4), 1)
                    .save(w, "precass/chip.recycle"))
                .variant(v -> new PrecAssRecipeGenerator.Builder(100, 200L)
                    .icon(broken("hbm_m:microchip"))
                    .in(brokenIn("hbm_m:microchip"), 1)
                    .out(0.5F, stack("hbm_m:silicon_circuit", 1), 1)
                    .out(0.5F, stack("hbm_m:plate_polymer", 3), 1)
                    .out(0.5F, stack("hbm_m:wire_gold", 4), 1)
                    .save(v, "precass/chip.recycle"), "expensive")
                .when("528")
                .save();

        // precass.chip_bismoid
        new PrecAssRecipeGenerator.Builder(200, 1000L)
                .in(Ingredient.of(item("hbm_m:silicon_circuit")), 4)
                .in(Ingredient.of(item("hbm_m:plate_polymer")), 8)
                .in(ore("oredict/nugget/any_bismoid"), 2)
                .in(ore("oredict/wire_fine/gold"), 4)
                .fluidIn(ModFluids.PERFLUOROMETHYL.getSource(), 500)
                .out(1F, stack("hbm_m:bismoid_chip", 1), 75, broken("hbm_m:bismoid_chip"), 25)
                .pool("528.chip_bismoid")
                .save(ConfigRecipes.when(writer, "528"), "precass/chip_bismoid");

        // precass.chip_bismoid.recycle (75%, Expensive 50%)
        ConfigRecipes.variants(writer)
                .base(w -> new PrecAssRecipeGenerator.Builder(200, 1000L)
                    .icon(broken("hbm_m:bismoid_chip"))
                    .in(brokenIn("hbm_m:bismoid_chip"), 1)
                    .out(0.75F, stack("hbm_m:silicon_circuit", 4), 1)
                    .out(0.75F, stack("hbm_m:plate_polymer", 8), 1)
                    .out(0.75F, stack("hbm_m:nugget_bismuth", 2), 1)
                    .out(0.75F, stack("hbm_m:wire_gold", 4), 1)
                    .fluidOut(ModFluids.PERFLUOROMETHYL.getSource(), 375)
                    .save(w, "precass/chip_bismoid.recycle"))
                .variant(v -> new PrecAssRecipeGenerator.Builder(200, 1000L)
                    .icon(broken("hbm_m:bismoid_chip"))
                    .in(brokenIn("hbm_m:bismoid_chip"), 1)
                    .out(0.5F, stack("hbm_m:silicon_circuit", 4), 1)
                    .out(0.5F, stack("hbm_m:plate_polymer", 8), 1)
                    .out(0.5F, stack("hbm_m:nugget_bismuth", 2), 1)
                    .out(0.5F, stack("hbm_m:wire_gold", 4), 1)
                    .fluidOut(ModFluids.PERFLUOROMETHYL.getSource(), 250)
                    .save(v, "precass/chip_bismoid.recycle"), "expensive")
                .when("528")
                .save();

        // precass.chip_quantum
        new PrecAssRecipeGenerator.Builder(300, 20000L)
                .in(Ingredient.of(item("hbm_m:silicon_circuit")), 8)
                .in(ore("oredict/wire_dense/bscco"), 2)
                .in(ore("oredict/ingot/any_hard_plastic"), 4)
                .in(Ingredient.of(item("hbm_m:pellet_charged")), 4)
                .in(ore("oredict/wire_fine/gold"), 8)
                .fluidIn(ModFluids.HELIUM4.getSource(), 250)
                .out(1F, stack("hbm_m:quantum_chip", 1), 90, broken("hbm_m:quantum_chip"), 10)
                .pool("528.chip_quantum")
                .save(ConfigRecipes.when(writer, "528"), "precass/chip_quantum");

        // precass.chip_quantum.recycle (75%, Expensive 50%)
        ConfigRecipes.variants(writer)
                .base(w -> new PrecAssRecipeGenerator.Builder(300, 20000L)
                    .icon(broken("hbm_m:quantum_chip"))
                    .in(brokenIn("hbm_m:quantum_chip"), 1)
                    .out(0.75F, stack("hbm_m:silicon_circuit", 8), 1)
                    .out(0.75F, stack("hbm_m:wire_dense_bscco", 2), 1)
                    .out(0.75F, stack("hbm_m:pc_ingot", 4), 1)
                    .out(0.75F, stack("hbm_m:pellet_charged", 4), 1)
                    .out(0.75F, stack("hbm_m:wire_gold", 8), 1)
                    .fluidOut(ModFluids.HELIUM4.getSource(), 188)
                    .save(w, "precass/chip_quantum.recycle"))
                .variant(v -> new PrecAssRecipeGenerator.Builder(300, 20000L)
                    .icon(broken("hbm_m:quantum_chip"))
                    .in(brokenIn("hbm_m:quantum_chip"), 1)
                    .out(0.5F, stack("hbm_m:silicon_circuit", 8), 1)
                    .out(0.5F, stack("hbm_m:wire_dense_bscco", 2), 1)
                    .out(0.5F, stack("hbm_m:pc_ingot", 4), 1)
                    .out(0.5F, stack("hbm_m:pellet_charged", 4), 1)
                    .out(0.5F, stack("hbm_m:wire_gold", 8), 1)
                    .fluidOut(ModFluids.HELIUM4.getSource(), 125)
                    .save(v, "precass/chip_quantum.recycle"), "expensive")
                .when("528")
                .save();

        // precass.atomic_clock
        new PrecAssRecipeGenerator.Builder(200, 2000L)
                .in(Ingredient.of(item("hbm_m:microchip")), 8)
                .in(ore("oredict/ingot/any_plastic"), 4)
                .in(ore("oredict/wire_fine/zirconium"), 8)
                .in(ore("oredict/dust/strontium"), 1)
                .out(1F, stack("hbm_m:atomic_clock", 1), 50, broken("hbm_m:atomic_clock"), 50)
                .pool("528.strontium")
                .save(ConfigRecipes.when(writer, "528"), "precass/atomic_clock");

        // precass.atomic_clock.recycle (75%, Expensive 50%)
        ConfigRecipes.variants(writer)
                .base(w -> new PrecAssRecipeGenerator.Builder(200, 2000L)
                    .icon(broken("hbm_m:atomic_clock"))
                    .in(brokenIn("hbm_m:atomic_clock"), 1)
                    .out(0.75F, stack("hbm_m:microchip", 8), 1)
                    .out(0.75F, stack("hbm_m:polymer_ingot", 4), 1)
                    .out(0.75F, stack("hbm_m:wire_zirconium", 8), 1)
                    .out(0.75F, stack("hbm_m:strontium_powder", 1), 1)
                    .save(w, "precass/atomic_clock.recycle"))
                .variant(v -> new PrecAssRecipeGenerator.Builder(200, 2000L)
                    .icon(broken("hbm_m:atomic_clock"))
                    .in(brokenIn("hbm_m:atomic_clock"), 1)
                    .out(0.5F, stack("hbm_m:microchip", 8), 1)
                    .out(0.5F, stack("hbm_m:polymer_ingot", 4), 1)
                    .out(0.5F, stack("hbm_m:wire_zirconium", 8), 1)
                    .out(0.5F, stack("hbm_m:strontium_powder", 1), 1)
                    .save(v, "precass/atomic_clock.recycle"), "expensive")
                .when("528")
                .save();

        // precass.controller
        new PrecAssRecipeGenerator.Builder(400, 15000L)
                .in(Ingredient.of(item("hbm_m:microchip")), 32)
                .in(Ingredient.of(item("hbm_m:capacitor")), 32)
                .in(Ingredient.of(item("hbm_m:capacitor_tantalum")), 16)
                .in(Ingredient.of(item("hbm_m:controller_chassis")), 1)
                .in(Ingredient.of(item("hbm_m:upgrade_speed_1")), 1)
                .in(ore("oredict/wire_fine/lead"), 16)
                .fluidIn(ModFluids.PERFLUOROMETHYL.getSource(), 1000)
                .out(1F, stack("hbm_m:controller", 1), 75, broken("hbm_m:controller"), 25)
                .save(ConfigRecipes.when(writer, "528"), "precass/controller");

        // precass.controller.recycle (90%, Expensive 50%)
        ConfigRecipes.variants(writer)
                .base(w -> new PrecAssRecipeGenerator.Builder(400, 15000L)
                    .icon(broken("hbm_m:controller"))
                    .in(brokenIn("hbm_m:controller"), 1)
                    .out(0.9F, stack("hbm_m:microchip", 32), 1)
                    .out(0.9F, stack("hbm_m:capacitor", 32), 1)
                    .out(0.9F, stack("hbm_m:capacitor_tantalum", 16), 1)
                    .out(0.9F, stack("hbm_m:controller_chassis", 1), 1)
                    .out(0.9F, stack("hbm_m:upgrade_speed_1", 1), 1)
                    .out(0.9F, stack("hbm_m:wire_lead", 16), 1)
                    .fluidOut(ModFluids.PERFLUOROMETHYL.getSource(), 900)
                    .save(w, "precass/controller.recycle"))
                .variant(v -> new PrecAssRecipeGenerator.Builder(400, 15000L)
                    .icon(broken("hbm_m:controller"))
                    .in(brokenIn("hbm_m:controller"), 1)
                    .out(0.5F, stack("hbm_m:microchip", 32), 1)
                    .out(0.5F, stack("hbm_m:capacitor", 32), 1)
                    .out(0.5F, stack("hbm_m:capacitor_tantalum", 16), 1)
                    .out(0.5F, stack("hbm_m:controller_chassis", 1), 1)
                    .out(0.5F, stack("hbm_m:upgrade_speed_1", 1), 1)
                    .out(0.5F, stack("hbm_m:wire_lead", 16), 1)
                    .fluidOut(ModFluids.PERFLUOROMETHYL.getSource(), 500)
                    .save(v, "precass/controller.recycle"), "expensive")
                .when("528")
                .save();

        // precass.controller_advanced
        new PrecAssRecipeGenerator.Builder(600, 25000L)
                .in(Ingredient.of(item("hbm_m:bismoid_chip")), 16)
                .in(Ingredient.of(item("hbm_m:capacitor_tantalum")), 48)
                .in(Ingredient.of(item("hbm_m:atomic_clock")), 1)
                .in(Ingredient.of(item("hbm_m:controller_chassis")), 1)
                .in(Ingredient.of(item("hbm_m:upgrade_speed_3")), 1)
                .in(ore("oredict/wire_fine/lead"), 24)
                .fluidIn(ModFluids.PERFLUOROMETHYL.getSource(), 4000)
                .out(1F, stack("hbm_m:controller_advanced", 1), 50, broken("hbm_m:controller_advanced"), 50)
                .save(ConfigRecipes.when(writer, "528"), "precass/controller_advanced");

        // precass.controller_advanced.recycle (75%, Expensive 50%)
        ConfigRecipes.variants(writer)
                .base(w -> new PrecAssRecipeGenerator.Builder(600, 25000L)
                    .icon(broken("hbm_m:controller_advanced"))
                    .in(brokenIn("hbm_m:controller_advanced"), 1)
                    .out(0.75F, stack("hbm_m:bismoid_chip", 16), 1)
                    .out(0.75F, stack("hbm_m:capacitor_tantalum", 48), 1)
                    .out(0.75F, stack("hbm_m:atomic_clock", 1), 1)
                    .out(0.75F, stack("hbm_m:controller_chassis", 1), 1)
                    .out(0.75F, stack("hbm_m:upgrade_speed_3", 1), 1)
                    .out(0.75F, stack("hbm_m:wire_lead", 24), 1)
                    .fluidOut(ModFluids.PERFLUOROMETHYL.getSource(), 3000)
                    .save(w, "precass/controller_advanced.recycle"))
                .variant(v -> new PrecAssRecipeGenerator.Builder(600, 25000L)
                    .icon(broken("hbm_m:controller_advanced"))
                    .in(brokenIn("hbm_m:controller_advanced"), 1)
                    .out(0.5F, stack("hbm_m:bismoid_chip", 16), 1)
                    .out(0.5F, stack("hbm_m:capacitor_tantalum", 48), 1)
                    .out(0.5F, stack("hbm_m:atomic_clock", 1), 1)
                    .out(0.5F, stack("hbm_m:controller_chassis", 1), 1)
                    .out(0.5F, stack("hbm_m:upgrade_speed_3", 1), 1)
                    .out(0.5F, stack("hbm_m:wire_lead", 24), 1)
                    .fluidOut(ModFluids.PERFLUOROMETHYL.getSource(), 2000)
                    .save(v, "precass/controller_advanced.recycle"), "expensive")
                .when("528")
                .save();

        // precass.controller_quantum
        new PrecAssRecipeGenerator.Builder(600, 250000L)
                .in(Ingredient.of(item("hbm_m:quantum_chip")), 16)
                .in(Ingredient.of(item("hbm_m:bismoid_chip")), 48)
                .in(Ingredient.of(item("hbm_m:atomic_clock")), 8)
                .in(Ingredient.of(item("hbm_m:controller_advanced")), 2)
                .in(Ingredient.of(item("hbm_m:upgrade_overdrive_1")), 1)
                .in(ore("oredict/wire_fine/lead"), 32)
                .fluidIn(ModFluids.PERFLUOROMETHYL_COLD.getSource(), 6000)
                .out(1F, stack("hbm_m:quantum_computer", 1), 75, broken("hbm_m:quantum_computer"), 25)
                .save(ConfigRecipes.when(writer, "528"), "precass/controller_quantum");

        // precass.controller_quantum.recycle (75%, Expensive 50%)
        ConfigRecipes.variants(writer)
                .base(w -> new PrecAssRecipeGenerator.Builder(600, 250000L)
                    .icon(broken("hbm_m:quantum_computer"))
                    .in(brokenIn("hbm_m:quantum_computer"), 1)
                    .out(0.75F, stack("hbm_m:quantum_chip", 16), 1)
                    .out(0.75F, stack("hbm_m:bismoid_chip", 48), 1)
                    .out(0.75F, stack("hbm_m:atomic_clock", 8), 1)
                    .out(0.75F, stack("hbm_m:controller_advanced", 2), 1)
                    .out(0.75F, stack("hbm_m:upgrade_overdrive_1", 1), 1)
                    .out(0.75F, stack("hbm_m:wire_lead", 32), 1)
                    .fluidOut(ModFluids.PERFLUOROMETHYL_COLD.getSource(), 4500)
                    .save(w, "precass/controller_quantum.recycle"))
                .variant(v -> new PrecAssRecipeGenerator.Builder(600, 250000L)
                    .icon(broken("hbm_m:quantum_computer"))
                    .in(brokenIn("hbm_m:quantum_computer"), 1)
                    .out(0.5F, stack("hbm_m:quantum_chip", 16), 1)
                    .out(0.5F, stack("hbm_m:bismoid_chip", 48), 1)
                    .out(0.5F, stack("hbm_m:atomic_clock", 8), 1)
                    .out(0.5F, stack("hbm_m:controller_advanced", 2), 1)
                    .out(0.5F, stack("hbm_m:upgrade_overdrive_1", 1), 1)
                    .out(0.5F, stack("hbm_m:wire_lead", 32), 1)
                    .fluidOut(ModFluids.PERFLUOROMETHYL_COLD.getSource(), 3000)
                    .save(v, "precass/controller_quantum.recycle"), "expensive")
                .when("528")
                .save();

        // precass.upgrade_speed_ii
        new PrecAssRecipeGenerator.Builder(300, 10000L)
                .in(Ingredient.of(item("hbm_m:microchip")), 8)
                .in(Ingredient.of(item("hbm_m:capacitor_tantalum")), 4)
                .in(Ingredient.of(item("hbm_m:upgrade_speed_1")), 1)
                .in(ore("oredict/ingot/any_plastic"), 4)
                .out(1F, stack("hbm_m:upgrade_speed_2", 1), 50, broken("hbm_m:upgrade_speed_2"), 50)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_speed_ii");

        // precass.upgrade_speed_ii.recycle (75%)
        new PrecAssRecipeGenerator.Builder(300, 10000L)
                .icon(broken("hbm_m:upgrade_speed_2"))
                .in(brokenIn("hbm_m:upgrade_speed_2"), 1)
                .out(0.75F, stack("hbm_m:microchip", 8), 1)
                .out(0.75F, stack("hbm_m:capacitor_tantalum", 4), 1)
                .out(0.75F, stack("hbm_m:upgrade_speed_1", 1), 1)
                .out(0.75F, stack("hbm_m:polymer_ingot", 4), 1)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_speed_ii.recycle");

        // precass.upgrade_speed_iii
        new PrecAssRecipeGenerator.Builder(400, 25000L)
                .in(Ingredient.of(item("hbm_m:microchip")), 16)
                .in(Ingredient.of(item("hbm_m:capacitor_tantalum")), 16)
                .in(Ingredient.of(item("hbm_m:upgrade_speed_2")), 1)
                .in(ore("oredict/ingot/rubber"), 4)
                .fluidIn(ModFluids.SOLVENT.getSource(), 500)
                .out(1F, stack("hbm_m:upgrade_speed_3", 1), 25, broken("hbm_m:upgrade_speed_3"), 75)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_speed_iii");

        // precass.upgrade_speed_iii.recycle (75%)
        new PrecAssRecipeGenerator.Builder(400, 25000L)
                .icon(broken("hbm_m:upgrade_speed_3"))
                .in(brokenIn("hbm_m:upgrade_speed_3"), 1)
                .out(0.75F, stack("hbm_m:microchip", 16), 1)
                .out(0.75F, stack("hbm_m:capacitor_tantalum", 16), 1)
                .out(0.75F, stack("hbm_m:upgrade_speed_2", 1), 1)
                .out(0.75F, stack("hbm_m:rubber_ingot", 4), 1)
                .fluidOut(ModFluids.SOLVENT.getSource(), 375)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_speed_iii.recycle");

        // precass.upgrade_effect_ii
        new PrecAssRecipeGenerator.Builder(300, 10000L)
                .in(Ingredient.of(item("hbm_m:microchip")), 8)
                .in(Ingredient.of(item("hbm_m:capacitor_tantalum")), 4)
                .in(Ingredient.of(item("hbm_m:upgrade_effect_1")), 1)
                .in(ore("oredict/ingot/any_plastic"), 4)
                .out(1F, stack("hbm_m:upgrade_effect_2", 1), 50, broken("hbm_m:upgrade_effect_2"), 50)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_effect_ii");

        // precass.upgrade_effect_ii.recycle (75%)
        new PrecAssRecipeGenerator.Builder(300, 10000L)
                .icon(broken("hbm_m:upgrade_effect_2"))
                .in(brokenIn("hbm_m:upgrade_effect_2"), 1)
                .out(0.75F, stack("hbm_m:microchip", 8), 1)
                .out(0.75F, stack("hbm_m:capacitor_tantalum", 4), 1)
                .out(0.75F, stack("hbm_m:upgrade_effect_1", 1), 1)
                .out(0.75F, stack("hbm_m:polymer_ingot", 4), 1)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_effect_ii.recycle");

        // precass.upgrade_effect_iii
        new PrecAssRecipeGenerator.Builder(400, 25000L)
                .in(Ingredient.of(item("hbm_m:microchip")), 16)
                .in(Ingredient.of(item("hbm_m:capacitor_tantalum")), 16)
                .in(Ingredient.of(item("hbm_m:upgrade_effect_2")), 1)
                .in(ore("oredict/ingot/rubber"), 4)
                .fluidIn(ModFluids.SOLVENT.getSource(), 500)
                .out(1F, stack("hbm_m:upgrade_effect_3", 1), 25, broken("hbm_m:upgrade_effect_3"), 75)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_effect_iii");

        // precass.upgrade_effect_iii.recycle (75%)
        new PrecAssRecipeGenerator.Builder(400, 25000L)
                .icon(broken("hbm_m:upgrade_effect_3"))
                .in(brokenIn("hbm_m:upgrade_effect_3"), 1)
                .out(0.75F, stack("hbm_m:microchip", 16), 1)
                .out(0.75F, stack("hbm_m:capacitor_tantalum", 16), 1)
                .out(0.75F, stack("hbm_m:upgrade_effect_2", 1), 1)
                .out(0.75F, stack("hbm_m:rubber_ingot", 4), 1)
                .fluidOut(ModFluids.SOLVENT.getSource(), 375)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_effect_iii.recycle");

        // precass.upgrade_power_ii
        new PrecAssRecipeGenerator.Builder(300, 10000L)
                .in(Ingredient.of(item("hbm_m:microchip")), 8)
                .in(Ingredient.of(item("hbm_m:capacitor_tantalum")), 4)
                .in(Ingredient.of(item("hbm_m:upgrade_power_1")), 1)
                .in(ore("oredict/ingot/any_plastic"), 4)
                .out(1F, stack("hbm_m:upgrade_power_2", 1), 50, broken("hbm_m:upgrade_power_2"), 50)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_power_ii");

        // precass.upgrade_power_ii.recycle (75%)
        new PrecAssRecipeGenerator.Builder(300, 10000L)
                .icon(broken("hbm_m:upgrade_power_2"))
                .in(brokenIn("hbm_m:upgrade_power_2"), 1)
                .out(0.75F, stack("hbm_m:microchip", 8), 1)
                .out(0.75F, stack("hbm_m:capacitor_tantalum", 4), 1)
                .out(0.75F, stack("hbm_m:upgrade_power_1", 1), 1)
                .out(0.75F, stack("hbm_m:polymer_ingot", 4), 1)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_power_ii.recycle");

        // precass.upgrade_power_iii
        new PrecAssRecipeGenerator.Builder(400, 25000L)
                .in(Ingredient.of(item("hbm_m:microchip")), 16)
                .in(Ingredient.of(item("hbm_m:capacitor_tantalum")), 16)
                .in(Ingredient.of(item("hbm_m:upgrade_power_2")), 1)
                .in(ore("oredict/ingot/rubber"), 4)
                .fluidIn(ModFluids.SOLVENT.getSource(), 500)
                .out(1F, stack("hbm_m:upgrade_power_3", 1), 25, broken("hbm_m:upgrade_power_3"), 75)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_power_iii");

        // precass.upgrade_power_iii.recycle (75%)
        new PrecAssRecipeGenerator.Builder(400, 25000L)
                .icon(broken("hbm_m:upgrade_power_3"))
                .in(brokenIn("hbm_m:upgrade_power_3"), 1)
                .out(0.75F, stack("hbm_m:microchip", 16), 1)
                .out(0.75F, stack("hbm_m:capacitor_tantalum", 16), 1)
                .out(0.75F, stack("hbm_m:upgrade_power_2", 1), 1)
                .out(0.75F, stack("hbm_m:rubber_ingot", 4), 1)
                .fluidOut(ModFluids.SOLVENT.getSource(), 375)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_power_iii.recycle");

        // precass.upgrade_fortune_ii
        new PrecAssRecipeGenerator.Builder(300, 10000L)
                .in(Ingredient.of(item("hbm_m:microchip")), 8)
                .in(Ingredient.of(item("hbm_m:capacitor_tantalum")), 4)
                .in(Ingredient.of(item("hbm_m:upgrade_fortune_1")), 1)
                .in(ore("oredict/ingot/any_plastic"), 4)
                .out(1F, stack("hbm_m:upgrade_fortune_2", 1), 50, broken("hbm_m:upgrade_fortune_2"), 50)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_fortune_ii");

        // precass.upgrade_fortune_ii.recycle (75%)
        new PrecAssRecipeGenerator.Builder(300, 10000L)
                .icon(broken("hbm_m:upgrade_fortune_2"))
                .in(brokenIn("hbm_m:upgrade_fortune_2"), 1)
                .out(0.75F, stack("hbm_m:microchip", 8), 1)
                .out(0.75F, stack("hbm_m:capacitor_tantalum", 4), 1)
                .out(0.75F, stack("hbm_m:upgrade_fortune_1", 1), 1)
                .out(0.75F, stack("hbm_m:polymer_ingot", 4), 1)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_fortune_ii.recycle");

        // precass.upgrade_fortune_iii
        new PrecAssRecipeGenerator.Builder(400, 25000L)
                .in(Ingredient.of(item("hbm_m:microchip")), 16)
                .in(Ingredient.of(item("hbm_m:capacitor_tantalum")), 16)
                .in(Ingredient.of(item("hbm_m:upgrade_fortune_2")), 1)
                .in(ore("oredict/ingot/rubber"), 4)
                .fluidIn(ModFluids.SOLVENT.getSource(), 500)
                .out(1F, stack("hbm_m:upgrade_fortune_3", 1), 25, broken("hbm_m:upgrade_fortune_3"), 75)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_fortune_iii");

        // precass.upgrade_fortune_iii.recycle (75%)
        new PrecAssRecipeGenerator.Builder(400, 25000L)
                .icon(broken("hbm_m:upgrade_fortune_3"))
                .in(brokenIn("hbm_m:upgrade_fortune_3"), 1)
                .out(0.75F, stack("hbm_m:microchip", 16), 1)
                .out(0.75F, stack("hbm_m:capacitor_tantalum", 16), 1)
                .out(0.75F, stack("hbm_m:upgrade_fortune_2", 1), 1)
                .out(0.75F, stack("hbm_m:rubber_ingot", 4), 1)
                .fluidOut(ModFluids.SOLVENT.getSource(), 375)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_fortune_iii.recycle");

        // precass.upgrade_ab_ii
        new PrecAssRecipeGenerator.Builder(300, 10000L)
                .in(Ingredient.of(item("hbm_m:microchip")), 8)
                .in(Ingredient.of(item("hbm_m:capacitor_tantalum")), 4)
                .in(Ingredient.of(item("hbm_m:upgrade_afterburn_1")), 1)
                .in(ore("oredict/ingot/any_plastic"), 4)
                .out(1F, stack("hbm_m:upgrade_afterburn_2", 1), 50, broken("hbm_m:upgrade_afterburn_2"), 50)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_ab_ii");

        // precass.upgrade_ab_ii.recycle (75%)
        new PrecAssRecipeGenerator.Builder(300, 10000L)
                .icon(broken("hbm_m:upgrade_afterburn_2"))
                .in(brokenIn("hbm_m:upgrade_afterburn_2"), 1)
                .out(0.75F, stack("hbm_m:microchip", 8), 1)
                .out(0.75F, stack("hbm_m:capacitor_tantalum", 4), 1)
                .out(0.75F, stack("hbm_m:upgrade_afterburn_1", 1), 1)
                .out(0.75F, stack("hbm_m:polymer_ingot", 4), 1)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_ab_ii.recycle");

        // precass.upgrade_ab_iii
        new PrecAssRecipeGenerator.Builder(400, 25000L)
                .in(Ingredient.of(item("hbm_m:microchip")), 16)
                .in(Ingredient.of(item("hbm_m:capacitor_tantalum")), 16)
                .in(Ingredient.of(item("hbm_m:upgrade_afterburn_2")), 1)
                .in(ore("oredict/ingot/rubber"), 4)
                .fluidIn(ModFluids.SOLVENT.getSource(), 500)
                .out(1F, stack("hbm_m:upgrade_afterburn_3", 1), 25, broken("hbm_m:upgrade_afterburn_3"), 75)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_ab_iii");

        // precass.upgrade_ab_iii.recycle (75%)
        new PrecAssRecipeGenerator.Builder(400, 25000L)
                .icon(broken("hbm_m:upgrade_afterburn_3"))
                .in(brokenIn("hbm_m:upgrade_afterburn_3"), 1)
                .out(0.75F, stack("hbm_m:microchip", 16), 1)
                .out(0.75F, stack("hbm_m:capacitor_tantalum", 16), 1)
                .out(0.75F, stack("hbm_m:upgrade_afterburn_2", 1), 1)
                .out(0.75F, stack("hbm_m:rubber_ingot", 4), 1)
                .fluidOut(ModFluids.SOLVENT.getSource(), 375)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_ab_iii.recycle");

        // precass.upgrade_overdive_i
        new PrecAssRecipeGenerator.Builder(200, 1000L)
                .in(Ingredient.of(item("hbm_m:upgrade_speed_3")), 1)
                .in(Ingredient.of(item("hbm_m:upgrade_effect_3")), 1)
                .in(ore("oredict/ingot/saturnite"), 16)
                .in(ore("oredict/ingot/any_hard_plastic"), 16)
                .in(Ingredient.of(item("hbm_m:advanced_circuit")), 16)
                .out(1F, stack("hbm_m:upgrade_overdrive_1", 1), 50, broken("hbm_m:upgrade_overdrive_1"), 50)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_overdive_i");

        // precass.upgrade_overdive_i.recycle (75%, Expensive 50%)
        ConfigRecipes.variants(writer)
                .base(w -> new PrecAssRecipeGenerator.Builder(200, 1000L)
                    .icon(broken("hbm_m:upgrade_overdrive_1"))
                    .in(brokenIn("hbm_m:upgrade_overdrive_1"), 1)
                    .out(0.75F, stack("hbm_m:upgrade_speed_3", 1), 1)
                    .out(0.75F, stack("hbm_m:upgrade_effect_3", 1), 1)
                    .out(0.75F, stack("hbm_m:saturnite_ingot", 16), 1)
                    .out(0.75F, stack("hbm_m:pc_ingot", 16), 1)
                    .out(0.75F, stack("hbm_m:advanced_circuit", 16), 1)
                    .save(w, "precass/upgrade_overdive_i.recycle"))
                .variant(v -> new PrecAssRecipeGenerator.Builder(200, 1000L)
                    .icon(broken("hbm_m:upgrade_overdrive_1"))
                    .in(brokenIn("hbm_m:upgrade_overdrive_1"), 1)
                    .out(0.5F, stack("hbm_m:upgrade_speed_3", 1), 1)
                    .out(0.5F, stack("hbm_m:upgrade_effect_3", 1), 1)
                    .out(0.5F, stack("hbm_m:saturnite_ingot", 16), 1)
                    .out(0.5F, stack("hbm_m:pc_ingot", 16), 1)
                    .out(0.5F, stack("hbm_m:advanced_circuit", 16), 1)
                    .save(v, "precass/upgrade_overdive_i.recycle"), "expensive")
                .when("528")
                .save();

        // precass.upgrade_overdive_ii
        new PrecAssRecipeGenerator.Builder(600, 5000L)
                .in(Ingredient.of(item("hbm_m:upgrade_overdrive_1")), 1)
                .in(Ingredient.of(item("hbm_m:upgrade_speed_3")), 1)
                .in(Ingredient.of(item("hbm_m:upgrade_effect_3")), 1)
                .in(ore("oredict/ingot/saturnite"), 16)
                .in(Ingredient.of(item("hbm_m:cft_ingot")), 8)
                .in(Ingredient.of(item("hbm_m:capacitor_board")), 16)
                .out(1F, stack("hbm_m:upgrade_overdrive_2", 1), 50, broken("hbm_m:upgrade_overdrive_2"), 50)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_overdive_ii");

        // precass.upgrade_overdive_ii.recycle (75%, Expensive 50%)
        ConfigRecipes.variants(writer)
                .base(w -> new PrecAssRecipeGenerator.Builder(600, 5000L)
                    .icon(broken("hbm_m:upgrade_overdrive_2"))
                    .in(brokenIn("hbm_m:upgrade_overdrive_2"), 1)
                    .out(0.75F, stack("hbm_m:upgrade_overdrive_1", 1), 1)
                    .out(0.75F, stack("hbm_m:upgrade_speed_3", 1), 1)
                    .out(0.75F, stack("hbm_m:upgrade_effect_3", 1), 1)
                    .out(0.75F, stack("hbm_m:saturnite_ingot", 16), 1)
                    .out(0.75F, stack("hbm_m:cft_ingot", 8), 1)
                    .out(0.75F, stack("hbm_m:capacitor_board", 16), 1)
                    .save(w, "precass/upgrade_overdive_ii.recycle"))
                .variant(v -> new PrecAssRecipeGenerator.Builder(600, 5000L)
                    .icon(broken("hbm_m:upgrade_overdrive_2"))
                    .in(brokenIn("hbm_m:upgrade_overdrive_2"), 1)
                    .out(0.5F, stack("hbm_m:upgrade_overdrive_1", 1), 1)
                    .out(0.5F, stack("hbm_m:upgrade_speed_3", 1), 1)
                    .out(0.5F, stack("hbm_m:upgrade_effect_3", 1), 1)
                    .out(0.5F, stack("hbm_m:saturnite_ingot", 16), 1)
                    .out(0.5F, stack("hbm_m:cft_ingot", 8), 1)
                    .out(0.5F, stack("hbm_m:capacitor_board", 16), 1)
                    .save(v, "precass/upgrade_overdive_ii.recycle"), "expensive")
                .when("528")
                .save();

        // precass.upgrade_overdive_iii
        new PrecAssRecipeGenerator.Builder(1200, 100000L)
                .in(Ingredient.of(item("hbm_m:upgrade_overdrive_2")), 1)
                .in(Ingredient.of(item("hbm_m:upgrade_speed_3")), 1)
                .in(Ingredient.of(item("hbm_m:upgrade_effect_3")), 1)
                .in(ore("oredict/ingot/any_bismoid_bronze"), 16)
                .in(Ingredient.of(item("hbm_m:cft_ingot")), 16)
                .in(Ingredient.of(item("hbm_m:bismoid_circuit")), 16)
                .out(1F, stack("hbm_m:upgrade_overdrive_3", 1), 25, broken("hbm_m:upgrade_overdrive_3"), 75)
                .save(ConfigRecipes.when(writer, "528"), "precass/upgrade_overdive_iii");

        // precass.upgrade_overdive_iii.recycle (75%, Expensive 50%)
        ConfigRecipes.variants(writer)
                .base(w -> new PrecAssRecipeGenerator.Builder(1200, 100000L)
                    .icon(broken("hbm_m:upgrade_overdrive_3"))
                    .in(brokenIn("hbm_m:upgrade_overdrive_3"), 1)
                    .out(0.75F, stack("hbm_m:upgrade_overdrive_2", 1), 1)
                    .out(0.75F, stack("hbm_m:upgrade_speed_3", 1), 1)
                    .out(0.75F, stack("hbm_m:upgrade_effect_3", 1), 1)
                    .out(0.75F, stack("hbm_m:bismuth_bronze_ingot", 16), 1)
                    .out(0.75F, stack("hbm_m:cft_ingot", 16), 1)
                    .out(0.75F, stack("hbm_m:bismoid_circuit", 16), 1)
                    .save(w, "precass/upgrade_overdive_iii.recycle"))
                .variant(v -> new PrecAssRecipeGenerator.Builder(1200, 100000L)
                    .icon(broken("hbm_m:upgrade_overdrive_3"))
                    .in(brokenIn("hbm_m:upgrade_overdrive_3"), 1)
                    .out(0.5F, stack("hbm_m:upgrade_overdrive_2", 1), 1)
                    .out(0.5F, stack("hbm_m:upgrade_speed_3", 1), 1)
                    .out(0.5F, stack("hbm_m:upgrade_effect_3", 1), 1)
                    .out(0.5F, stack("hbm_m:bismuth_bronze_ingot", 16), 1)
                    .out(0.5F, stack("hbm_m:cft_ingot", 16), 1)
                    .out(0.5F, stack("hbm_m:bismoid_circuit", 16), 1)
                    .save(v, "precass/upgrade_overdive_iii.recycle"), "expensive")
                .when("528")
                .save();
        checkMissing("PrecAss528RecipeGenerator");
    }
}
//?}
