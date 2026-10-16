package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Consumer;

/**
 * Генератор JSON-рецептов дуговой сварки ({@code hbm_m:arc_welder}).
 *
 * <p>Порт раскомментированных рецептов из {@code com.hbm.inventory.recipes.ArcWelderRecipes#registerDefaults()}
 * (legacy 1.7.10 defaults). Завкомментированные в оригинале TODO-рецепты (предметы/блоки, ещё не портированные
 * в мод) намеренно пропущены — раскомментировать и поправить ссылки, когда соответствующий предмет появится.</p>
 *
 * <p>Чистый ванильный 1.20.1 код внутри {@code //? if forge} — датаген только для 1.20.1-forge.</p>
 */
public final class ArcWelderRecipeGenerator {

    private ArcWelderRecipeGenerator() {}

    public static void generate(Consumer<FinishedRecipe> writer) {
        registerMachineParts(writer);
        registerSatelliteParts(writer);
        registerMissiles(writer);
        registerDenseWires(writer);
        registerWeldedPlates(writer);
    }

    // ─── Ingredient helpers ────────────────────────────────────────────────────

    private static Ingredient item(ItemStack stack) { return Ingredient.of(stack); }
    private static Ingredient tag(String forgeTag) {
        net.minecraft.tags.TagKey<net.minecraft.world.item.Item> key = net.minecraft.tags.TagKey.create(
                net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", forgeTag));
        return Ingredient.of(key);
    }

    /**
     * Обёртка для компактной записи рецепта: пары {@code (Ingredient, count)}.
     * Buildер принимает два параллельных массива — собираем их здесь.
     */
    private static void emit(Consumer<FinishedRecipe> writer, String id, ItemStack output,
                              int duration, long consumption, Pair... inputs) {
        Ingredient[] ins = new Ingredient[inputs.length];
        int[] cnts = new int[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            ins[i] = inputs[i].ing;
            cnts[i] = inputs[i].count;
        }
        ArcWelderRecipeBuilder.arcWelderRecipe(ins, cnts, output, duration, consumption)
                .save(writer, "arc_welder/" + id);
    }

    private record Pair(Ingredient ing, int count) {}
    private static Pair p(ItemStack stack)        { return new Pair(item(stack), stack.getCount()); }
    private static Pair p(Ingredient ing, int c)   { return new Pair(ing, c); }

    // ─── Machine Parts ──────────────────────────────────────────────────────────

    private static void registerMachineParts(Consumer<FinishedRecipe> writer) {
        emit(writer, "motor_2x", new ItemStack(ModItems.MOTOR.get(), 2), 100, 400L,
                p(new ItemStack(ModItems.COIL_COPPER.get())),
                p(new ItemStack(ModItems.COIL_COPPER_TORUS.get())),
                p(new ItemStack(ModItems.PLATE_IRON.get(), 2)));

        emit(writer, "motor_4x", new ItemStack(ModItems.MOTOR.get(), 4), 200, 2_000L,
                p(new ItemStack(ModItems.PLATE_STEEL.get(), 2)),
                p(new ItemStack(ModItems.WIRE_DENSE_ADVANCED_ALLOY.get())),
                p(new ItemStack(ModItems.WIRE_DENSE_COPPER.get())));

        emit(writer, "low_density_element", new ItemStack(ModItems.LOW_DENSITY_ELEMENT.get()), 200, 5_000L,
                p(tag("ingots/plastic"), 1),
                p(new ItemStack(ModItems.getIngot(ModIngots.FIBERGLASS).get(), 4)),
                p(new ItemStack(ModItems.PLATE_ALUMINUM.get(), 4)));

        emit(writer, "neutron_reflector", new ItemStack(ModItems.NEUTRON_REFLECTOR.get()), 200, 10_000L,
                p(new ItemStack(ModItems.PLATE_DURA_STEEL.get())),
                p(new ItemStack(ModItems.INGOT_TUNGSTEN_CARBIDE.get(), 2)));
    }

    // ─── Satellite Parts ─────────────────────────────────────────────────────────

    private static void registerSatelliteParts(Consumer<FinishedRecipe> writer) {
        emit(writer, "sat_laser", new ItemStack(ModItems.SAT_LASER.get()), 200, 10_000L,
                p(new ItemStack(ModItems.SAT_HEAD_LASER.get())),
                p(new ItemStack(ModItems.SAT_BASE.get())));

        emit(writer, "sat_radar", new ItemStack(ModItems.SAT_RADAR.get()), 200, 10_000L,
                p(new ItemStack(ModItems.SAT_HEAD_RADAR.get())),
                p(new ItemStack(ModItems.SAT_BASE.get())));

        emit(writer, "sat_mapper", new ItemStack(ModItems.SAT_MAPPER.get()), 200, 10_000L,
                p(new ItemStack(ModItems.SAT_HEAD_MAPPER.get())),
                p(new ItemStack(ModItems.SAT_BASE.get())));

        emit(writer, "sat_resonator", new ItemStack(ModItems.SAT_RESONATOR.get()), 200, 10_000L,
                p(new ItemStack(ModItems.SAT_HEAD_RESONATOR.get())),
                p(new ItemStack(ModItems.SAT_BASE.get())));
    }

    // ─── Missiles / rocketry ─────────────────────────────────────────────────────

    private static void registerMissiles(Consumer<FinishedRecipe> writer) {
        // Fuel tanks
        emit(writer, "fuel_tank_small_a", new ItemStack(ModItems.FUEL_TANK_SMALL.get()), 100, 1_000L,
                p(new ItemStack(ModBlocks.DECO_STEEL_SCAFFOLD.get().asItem(), 4)),
                p(new ItemStack(ModItems.PLATE_COPPER.get(), 4)),
                p(new ItemStack(ModItems.PLATE_ALUMINUM.get(), 6)));

        emit(writer, "fuel_tank_small_b", new ItemStack(ModItems.FUEL_TANK_SMALL.get()), 100, 1_000L,
                p(new ItemStack(ModItems.PLATE_COPPER.get(), 4)),
                p(new ItemStack(ModItems.WIRE_ALUMINIUM.get(), 4)),
                p(new ItemStack(ModItems.PLATE_STEEL.get(), 4)));

        emit(writer, "fuel_tank_medium", new ItemStack(ModItems.FUEL_TANK_MEDIUM.get()), 100, 2_000L,
                p(new ItemStack(ModBlocks.DECO_STEEL_SCAFFOLD.get().asItem(), 12)),
                p(new ItemStack(ModItems.PLATE_TITANIUM.get(), 8)),
                p(new ItemStack(ModItems.PLATE_CAST_ALUMINIUM.get(), 4)));

        emit(writer, "fuel_tank_large", new ItemStack(ModItems.FUEL_TANK_LARGE.get()), 400, 50_000L,
                p(new ItemStack(ModBlocks.DECO_STEEL_SCAFFOLD.get().asItem(), 16)),
                p(new ItemStack(ModItems.PLATE_SATURNITE.get(), 12)),
                p(new ItemStack(ModItems.PLATE_WELDED_ALUMINIUM.get(), 8)));

        // Thrusters
        emit(writer, "thruster_small", new ItemStack(ModItems.THRUSTER_SMALL.get()), 100, 1_000L,
                p(new ItemStack(ModItems.PLATE_COPPER.get(), 4)),
                p(new ItemStack(ModItems.WIRE_ALUMINIUM.get(), 4)),
                p(new ItemStack(ModItems.PLATE_STEEL.get(), 4)));

        emit(writer, "thruster_medium", new ItemStack(ModItems.THRUSTER_MEDIUM.get()), 100, 2_000L,
                p(new ItemStack(ModItems.getIngot(ModIngots.GRAPHITE).get(), 8)),
                p(new ItemStack(ModItems.MOTOR.get())),
                p(new ItemStack(ModItems.PLATE_STEEL.get(), 8)));

        emit(writer, "thruster_large", new ItemStack(ModItems.THRUSTER_LARGE.get()), 400, 50_000L,
                p(new ItemStack(ModItems.INGOT_TUNGSTEN_CARBIDE.get(), 12)),
                p(new ItemStack(ModItems.MOTOR.get())),
                p(new ItemStack(ModItems.INGOT_HIGHSPEED_STEEL.get(), 10)));

        // Missiles
        emit(writer, "missile_generic", new ItemStack(ModItems.MISSILE_GENERIC.get()), 60, 1_000L,
                p(new ItemStack(ModItems.FUEL_TANK_SMALL.get())),
                p(new ItemStack(ModItems.THRUSTER_SMALL.get())),
                p(new ItemStack(ModItems.WARHEAD_GENERIC_SMALL.get())));

        emit(writer, "missile_rain", new ItemStack(ModItems.MISSILE_RAIN.get()), 200, 20_000L,
                p(new ItemStack(ModItems.THRUSTER_MEDIUM.get(), 4)),
                p(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get(), 2)),
                p(new ItemStack(ModItems.WARHEAD_CLUSTER_LARGE.get())));

        emit(writer, "missile_incendiary_strong", new ItemStack(ModItems.MISSILE_INCENDIARY_STRONG.get()), 100, 5_000L,
                p(new ItemStack(ModItems.THRUSTER_MEDIUM.get())),
                p(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get())),
                p(new ItemStack(ModItems.WARHEAD_INCENDIARY_MEDIUM.get())));

        emit(writer, "missile_buster", new ItemStack(ModItems.MISSILE_BUSTER.get()), 60, 1_000L,
                p(new ItemStack(ModItems.THRUSTER_SMALL.get())),
                p(new ItemStack(ModItems.FUEL_TANK_SMALL.get())),
                p(new ItemStack(ModItems.WARHEAD_BUSTER_SMALL.get())));

        emit(writer, "missile_incendiary", new ItemStack(ModItems.MISSILE_INCENDIARY.get()), 60, 1_000L,
                p(new ItemStack(ModItems.THRUSTER_SMALL.get())),
                p(new ItemStack(ModItems.FUEL_TANK_SMALL.get())),
                p(new ItemStack(ModItems.WARHEAD_INCENDIARY_SMALL.get())));

        emit(writer, "missile_abm", new ItemStack(ModItems.MISSILE_ABM.get()), 60, 1_000L,
                p(new ItemStack(ModItems.THRUSTER_SMALL.get(), 4)),
                p(new ItemStack(ModItems.MISSILE_ASSEMBLY.get())),
                p(new ItemStack(ModItems.BALL_TNT.get(), 3)));

        emit(writer, "missile_cluster", new ItemStack(ModItems.MISSILE_CLUSTER.get()), 60, 1_000L,
                p(new ItemStack(ModItems.THRUSTER_SMALL.get())),
                p(new ItemStack(ModItems.FUEL_TANK_SMALL.get())),
                p(new ItemStack(ModItems.WARHEAD_CLUSTER_SMALL.get())));

        emit(writer, "missile_strong", new ItemStack(ModItems.MISSILE_STRONG.get()), 200, 10_000L,
                p(new ItemStack(ModItems.THRUSTER_MEDIUM.get())),
                p(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get())),
                p(new ItemStack(ModItems.WARHEAD_GENERIC_MEDIUM.get())));

        emit(writer, "missile_emp_strong", new ItemStack(ModItems.MISSILE_EMP_STRONG.get()), 100, 5_000L,
                p(new ItemStack(ModItems.THRUSTER_MEDIUM.get())),
                p(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get())),
                p(new ItemStack(ModBlocks.EMP.get().asItem(), 3)));

        emit(writer, "missile_buster_strong", new ItemStack(ModItems.MISSILE_BUSTER_STRONG.get()), 100, 5_000L,
                p(new ItemStack(ModItems.THRUSTER_MEDIUM.get())),
                p(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get())),
                p(new ItemStack(ModItems.WARHEAD_BUSTER_MEDIUM.get())));

        emit(writer, "missile_cluster_strong", new ItemStack(ModItems.MISSILE_CLUSTER_STRONG.get()), 100, 5_000L,
                p(new ItemStack(ModItems.THRUSTER_MEDIUM.get())),
                p(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get())),
                p(new ItemStack(ModItems.WARHEAD_CLUSTER_MEDIUM.get())));

        emit(writer, "missile_drill", new ItemStack(ModItems.MISSILE_DRILL.get()), 200, 20_000L,
                p(new ItemStack(ModItems.THRUSTER_MEDIUM.get(), 4)),
                p(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get(), 2)),
                p(new ItemStack(ModItems.WARHEAD_BUSTER_LARGE.get())));

        emit(writer, "missile_burst", new ItemStack(ModItems.MISSILE_BURST.get()), 200, 20_000L,
                p(new ItemStack(ModItems.THRUSTER_MEDIUM.get(), 4)),
                p(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get(), 2)),
                p(new ItemStack(ModItems.WARHEAD_GENERIC_LARGE.get())));

        emit(writer, "missile_nuclear", new ItemStack(ModItems.MISSILE_NUCLEAR.get()), 200, 50_000L,
                p(new ItemStack(ModItems.THRUSTER_LARGE.get(), 3)),
                p(new ItemStack(ModItems.FUEL_TANK_LARGE.get())),
                p(new ItemStack(ModItems.WARHEAD_NUCLEAR.get())));

        emit(writer, "missile_volcano", new ItemStack(ModItems.MISSILE_VOLCANO.get()), 200, 50_000L,
                p(new ItemStack(ModItems.THRUSTER_LARGE.get(), 3)),
                p(new ItemStack(ModItems.FUEL_TANK_LARGE.get())),
                p(new ItemStack(ModItems.WARHEAD_VOLCANO.get())));

        emit(writer, "missile_nuclear_cluster", new ItemStack(ModItems.MISSILE_NUCLEAR_CLUSTER.get()), 200, 50_000L,
                p(new ItemStack(ModItems.THRUSTER_LARGE.get(), 3)),
                p(new ItemStack(ModItems.FUEL_TANK_LARGE.get())),
                p(new ItemStack(ModItems.WARHEAD_MIRV.get())));
    }

    // ─── Dense Wires ────────────────────────────────────────────────────────────

    private static void registerDenseWires(Consumer<FinishedRecipe> writer) {
        denseWire(writer, "iron",           ModItems.WIRE_DENSE_IRON.get(),          new ItemStack(ModItems.WIRE_IRON.get(),          8), 100, 1_000L);
        denseWire(writer, "aluminium",     ModItems.WIRE_DENSE_ALUMINIUM.get(),     new ItemStack(ModItems.WIRE_ALUMINIUM.get(),     8), 100, 1_000L);
        denseWire(writer, "titanium",      ModItems.WIRE_DENSE_TITANIUM.get(),      new ItemStack(ModItems.WIRE_TITANIUM.get(),      8), 200, 10_000L);
        denseWireWithTag(writer, "lead",   ModItems.WIRE_DENSE_LEAD.get(),          tag("wires_fine/lead"),      8, 100,   500L);
        denseWire(writer, "copper",        ModItems.WIRE_DENSE_COPPER.get(),        new ItemStack(ModItems.WIRE_COPPER.get(),        8), 100, 1_000L);
        denseWire(writer, "steel",         ModItems.WIRE_DENSE_STEEL.get(),         new ItemStack(ModItems.WIRE_STEEL.get(),         8), 100, 2_000L);
        denseWire(writer, "gold",          ModItems.WIRE_DENSE_GOLD.get(),          new ItemStack(ModItems.WIRE_GOLD.get(),          8), 100, 1_000L);
        denseWire(writer, "advanced_alloy",ModItems.WIRE_DENSE_ADVANCED_ALLOY.get(), new ItemStack(ModItems.WIRE_ADVANCED_ALLOY.get(),8), 200, 20_000L);
        denseWire(writer, "schrabidium",   ModItems.WIRE_DENSE_SCHRABIDIUM.get(),   new ItemStack(ModItems.WIRE_SCHRABIDIUM.get(),   8), 400, 50_000L);
        denseWire(writer, "saturnite",     ModItems.WIRE_DENSE_SATURNITE.get(),     new ItemStack(ModItems.WIRE_SATURNITE.get(),     8), 200, 20_000L);
        denseWire(writer, "combine_steel", ModItems.WIRE_DENSE_COMBINE_STEEL.get(), new ItemStack(ModItems.WIRE_COMBINE_STEEL.get(), 8), 200, 10_000L);
    }

    private static void denseWire(Consumer<FinishedRecipe> writer, String id, net.minecraft.world.item.Item out,
                                  ItemStack input, int duration, long consumption) {
        emit(writer, "wire_dense_" + id, new ItemStack(out), duration, consumption, p(input));
    }

    private static void denseWireWithTag(Consumer<FinishedRecipe> writer, String id, net.minecraft.world.item.Item out,
                                         Ingredient inputTag, int count, int duration, long consumption) {
        emit(writer, "wire_dense_" + id, new ItemStack(out), duration, consumption, p(inputTag, count));
    }

    // ─── Welded Plates ──────────────────────────────────────────────────────────

    private static void registerWeldedPlates(Consumer<FinishedRecipe> writer) {
        weldedPlate(writer, "iron",       ModItems.PLATE_WELDED_IRON.get(),     new ItemStack(ModItems.PLATE_CAST_IRON.get(),     2), 100,    100L);
        weldedPlate(writer, "steel",      ModItems.PLATE_WELDED_STEEL.get(),    new ItemStack(ModItems.PLATE_CAST_STEEL.get(),     2), 100,    500L);
        weldedPlate(writer, "copper",     ModItems.PLATE_WELDED_COPPER.get(),  new ItemStack(ModItems.PLATE_CAST_COPPER.get(),    2), 200,  1_000L);
        weldedPlate(writer, "titanium",   ModItems.PLATE_WELDED_TITANIUM.get(), new ItemStack(ModItems.PLATE_CAST_TITANIUM.get(),  2), 600, 50_000L);
        weldedPlate(writer, "aluminium",  ModItems.PLATE_WELDED_ALUMINIUM.get(),new ItemStack(ModItems.PLATE_CAST_ALUMINIUM.get(), 2), 300, 10_000L);
        weldedPlate(writer, "tungsten",   ModItems.PLATE_WELDED_TUNGSTEN.get(), new ItemStack(ModItems.PLATE_CAST_TUNGSTEN.get(),  2), 600, 50_000L);
        weldedPlate(writer, "zirconium",  ModItems.PLATE_WELDED_ZIRCONIUM.get(),new ItemStack(ModItems.PLATE_CAST_ZIRCONIUM.get(), 2), 600, 10_000L);
        weldedPlate(writer, "osmiridium", ModItems.PLATE_WELDED_OSMIRIDIUM.get(),new ItemStack(ModItems.PLATE_CAST_OSMIRIDIUM.get(),2),800,100_000L);
        weldedPlate(writer, "tcalloy",    ModItems.PLATE_WELDED_TCALLOY.get(),  new ItemStack(ModItems.PLATE_CAST_TCALLOY.get(),   2),1200,1_000_000L);
        weldedPlate(writer, "cdalloy",    ModItems.PLATE_WELDED_CDALLOY.get(),  new ItemStack(ModItems.PLATE_CAST_CDALLOY.get(),   2),1200,1_000_000L);
        weldedPlate(writer, "cmb",        ModItems.PLATE_WELDED_CMB.get(),      new ItemStack(ModItems.PLATE_CAST_CMB.get(),       2),1200,1_000_000L);
    }

    private static void weldedPlate(Consumer<FinishedRecipe> writer, String id, net.minecraft.world.item.Item out,
                                     ItemStack input, int duration, long consumption) {
        emit(writer, "plate_welded_" + id, new ItemStack(out), duration, consumption, p(input));
    }
}
//?}
