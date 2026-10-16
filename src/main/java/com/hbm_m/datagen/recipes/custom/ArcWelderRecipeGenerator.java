package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

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
                p(new ItemStack(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE), 2)));

        emit(writer, "motor_4x", new ItemStack(ModItems.MOTOR.get(), 4), 200, 2_000L,
                p(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE), 2)),
                p(new ItemStack(ModMaterialItems.item(ModMaterials.ADVANCED_ALLOY, MaterialShape.WIRE_DENSE))),
                p(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.WIRE_DENSE))));

        emit(writer, "low_density_element", new ItemStack(ModItems.LOW_DENSITY_ELEMENT.get()), 200, 5_000L,
                p(tag("ingots/plastic"), 1),
                p(new ItemStack(ModMaterialItems.item(ModMaterials.FIBERGLASS, MaterialShape.INGOT), 4)),
                p(new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.PLATE), 4)));

        emit(writer, "neutron_reflector", new ItemStack(ModItems.NEUTRON_REFLECTOR.get()), 200, 10_000L,
                p(new ItemStack(ModMaterialItems.item(ModMaterials.DURA_STEEL, MaterialShape.PLATE))),
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
                p(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE), 4)),
                p(new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.PLATE), 6)));

        emit(writer, "fuel_tank_small_b", new ItemStack(ModItems.FUEL_TANK_SMALL.get()), 100, 1_000L,
                p(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE), 4)),
                p(new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.WIRE), 4)),
                p(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE), 4)));

        emit(writer, "fuel_tank_medium", new ItemStack(ModItems.FUEL_TANK_MEDIUM.get()), 100, 2_000L,
                p(new ItemStack(ModBlocks.DECO_STEEL_SCAFFOLD.get().asItem(), 12)),
                p(new ItemStack(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE), 8)),
                p(new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.PLATE_CAST), 4)));

        emit(writer, "fuel_tank_large", new ItemStack(ModItems.FUEL_TANK_LARGE.get()), 400, 50_000L,
                p(new ItemStack(ModBlocks.DECO_STEEL_SCAFFOLD.get().asItem(), 16)),
                p(new ItemStack(ModMaterialItems.item(ModMaterials.SATURNITE, MaterialShape.PLATE), 12)),
                p(new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.PLATE_WELDED), 8)));

        // Thrusters
        emit(writer, "thruster_small", new ItemStack(ModItems.THRUSTER_SMALL.get()), 100, 1_000L,
                p(new ItemStack(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE), 4)),
                p(new ItemStack(ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.WIRE), 4)),
                p(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE), 4)));

        emit(writer, "thruster_medium", new ItemStack(ModItems.THRUSTER_MEDIUM.get()), 100, 2_000L,
                p(new ItemStack(ModMaterialItems.item(ModMaterials.GRAPHITE, MaterialShape.INGOT), 8)),
                p(new ItemStack(ModItems.MOTOR.get())),
                p(new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE), 8)));

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
        denseWire(writer, "iron",           ModMaterialItems.item(ModMaterials.IRON, MaterialShape.WIRE_DENSE),          ModMaterialItems.stack(ModMaterials.IRON, MaterialShape.WIRE,          8), 100, 1_000L);
        denseWire(writer, "aluminium",     ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.WIRE_DENSE),     ModMaterialItems.stack(ModMaterials.ALUMINIUM, MaterialShape.WIRE,     8), 100, 1_000L);
        denseWire(writer, "titanium",      ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.WIRE_DENSE),      ModMaterialItems.stack(ModMaterials.TITANIUM, MaterialShape.WIRE,      8), 200, 10_000L);
        denseWireWithTag(writer, "lead",   ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.WIRE_DENSE),          tag("wires_fine/lead"),      8, 100,   500L);
        denseWire(writer, "copper",        ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.WIRE_DENSE),        ModMaterialItems.stack(ModMaterials.COPPER, MaterialShape.WIRE,        8), 100, 1_000L);
        denseWire(writer, "steel",         ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.WIRE_DENSE),         ModMaterialItems.stack(ModMaterials.STEEL, MaterialShape.WIRE,         8), 100, 2_000L);
        denseWire(writer, "gold",          ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.WIRE_DENSE),          ModMaterialItems.stack(ModMaterials.GOLD, MaterialShape.WIRE,          8), 100, 1_000L);
        denseWire(writer, "advanced_alloy",ModMaterialItems.item(ModMaterials.ADVANCED_ALLOY, MaterialShape.WIRE_DENSE), ModMaterialItems.stack(ModMaterials.ADVANCED_ALLOY, MaterialShape.WIRE,8), 200, 20_000L);
        denseWire(writer, "schrabidium",   ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.WIRE_DENSE),   ModMaterialItems.stack(ModMaterials.SCHRABIDIUM, MaterialShape.WIRE,   8), 400, 50_000L);
        denseWire(writer, "saturnite",     ModMaterialItems.item(ModMaterials.SATURNITE, MaterialShape.WIRE_DENSE),     ModMaterialItems.stack(ModMaterials.SATURNITE, MaterialShape.WIRE,     8), 200, 20_000L);
        denseWire(writer, "combine_steel", ModMaterialItems.item(ModMaterials.COMBINE_STEEL, MaterialShape.WIRE_DENSE), ModMaterialItems.stack(ModMaterials.COMBINE_STEEL, MaterialShape.WIRE, 8), 200, 10_000L);
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
        weldedPlate(writer, "iron",       ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE_WELDED),     ModMaterialItems.stack(ModMaterials.IRON, MaterialShape.PLATE_CAST,     2), 100,    100L);
        weldedPlate(writer, "steel",      ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_WELDED),    ModMaterialItems.stack(ModMaterials.STEEL, MaterialShape.PLATE_CAST,    2), 100,    500L);
        weldedPlate(writer, "copper",     ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_WELDED),  ModMaterialItems.stack(ModMaterials.COPPER, MaterialShape.PLATE_CAST,    2), 200,  1_000L);
        weldedPlate(writer, "titanium",   ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE_WELDED), ModMaterialItems.stack(ModMaterials.TITANIUM, MaterialShape.PLATE_CAST,  2), 600, 50_000L);
        weldedPlate(writer, "aluminium",  ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.PLATE_WELDED),ModMaterialItems.stack(ModMaterials.ALUMINIUM, MaterialShape.PLATE_CAST, 2), 300, 10_000L);
        weldedPlate(writer, "tungsten",   ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.PLATE_WELDED), ModMaterialItems.stack(ModMaterials.TUNGSTEN, MaterialShape.PLATE_CAST,  2), 600, 50_000L);
        weldedPlate(writer, "zirconium",  ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.PLATE_WELDED),ModMaterialItems.stack(ModMaterials.ZIRCONIUM, MaterialShape.PLATE_CAST, 2), 600, 10_000L);
        weldedPlate(writer, "osmiridium", ModMaterialItems.item(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_WELDED),ModMaterialItems.stack(ModMaterials.OSMIRIDIUM, MaterialShape.PLATE_CAST,2),800,100_000L);
        weldedPlate(writer, "tcalloy",    ModMaterialItems.item(ModMaterials.TCALLOY, MaterialShape.PLATE_WELDED),  ModMaterialItems.stack(ModMaterials.TCALLOY, MaterialShape.PLATE_CAST,   2),1200,1_000_000L);
        weldedPlate(writer, "cdalloy",    ModMaterialItems.item(ModMaterials.CDALLOY, MaterialShape.PLATE_WELDED),  ModMaterialItems.stack(ModMaterials.CDALLOY, MaterialShape.PLATE_CAST,   2),1200,1_000_000L);
        weldedPlate(writer, "cmb",        ModMaterialItems.item(ModMaterials.CMB, MaterialShape.PLATE_WELDED),      ModMaterialItems.stack(ModMaterials.CMB, MaterialShape.PLATE_CAST,       2),1200,1_000_000L);
    }

    private static void weldedPlate(Consumer<FinishedRecipe> writer, String id, net.minecraft.world.item.Item out,
                                     ItemStack input, int duration, long consumption) {
        emit(writer, "plate_welded_" + id, new ItemStack(out), duration, consumption, p(input));
    }
}
//?}
