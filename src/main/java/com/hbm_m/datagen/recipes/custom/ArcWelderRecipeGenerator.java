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

        // Original: part_generic LDE (Port-ID part_generic_lde, von den Montagerezepten verlangt) - zwei Varianten
        emit(writer, "low_density_element", new ItemStack(OreDictIngredients.item("hbm_m:part_generic_lde")), 200, 5_000L,
                p(OreDictIngredients.ore("oredict/plate/aluminum"), 4),
                p(OreDictIngredients.ore("oredict/ingot/fiberglass"), 4),
                p(OreDictIngredients.ore("oredict/ingot/any_hard_plastic"), 1));
        emit(writer, "low_density_element_titanium", new ItemStack(OreDictIngredients.item("hbm_m:part_generic_lde")), 200, 10_000L,
                p(OreDictIngredients.ore("oredict/plate/titanium"), 2),
                p(OreDictIngredients.ore("oredict/ingot/fiberglass"), 4),
                p(OreDictIngredients.ore("oredict/ingot/any_hard_plastic"), 1));

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
        // 1:1 ArcWelderRecipes "Missile Parts" / "Missiles" (Zeiten, Energie, Mengen wie im Original)
        Ingredient scaffold = Ingredient.of(ModBlocks.STEEL_SCAFFOLD.get());
        emit(writer, "thruster_small", new ItemStack(ModItems.THRUSTER_SMALL.get()), 60, 1_000L,
                p(ore("oredict/plate/steel"), 4), p(ore("oredict/wire_fine/aluminum"), 4), p(ore("oredict/plate/copper"), 4));
        emit(writer, "thruster_medium", new ItemStack(ModItems.THRUSTER_MEDIUM.get()), 100, 2_000L,
                p(ore("oredict/plate/steel"), 8), p(new ItemStack(ModItems.MOTOR.get())), p(ore("oredict/ingot/graphite"), 8));
        emit(writer, "thruster_large", new ItemStack(ModItems.THRUSTER_LARGE.get()), 200, 5_000L,
                p(ore("oredict/ingot/dura_steel"), 10), p(new ItemStack(ModItems.MOTOR.get())), p(ore("oredict/ingot/tungsten_carbide"), 12));

        emit(writer, "fuel_tank_small_a", new ItemStack(ModItems.FUEL_TANK_SMALL.get()), 60, 1_000L,
                p(ore("oredict/plate/aluminum"), 6), p(ore("oredict/plate/copper"), 4), p(scaffold, 4));
        emit(writer, "fuel_tank_medium", new ItemStack(ModItems.FUEL_TANK_MEDIUM.get()), 100, 2_000L,
                p(ore("oredict/plate_triple/aluminum"), 4), p(ore("oredict/plate/titanium"), 8), p(scaffold, 12));
        emit(writer, "fuel_tank_large", new ItemStack(ModItems.FUEL_TANK_LARGE.get()), 200, 5_000L,
                p(ore("oredict/plate_sextuple/aluminum"), 8), p(ore("oredict/plate/saturnite"), 12), p(scaffold, 16));

        // Original: missile_anti_ballistic
        emit(writer, "missile_anti_ballistic", new ItemStack(ModItems.MISSILE_ABM.get()), 100, 5_000L,
                p(ore("oredict/ingot/any_highexplosive"), 3), p(new ItemStack(ModItems.MISSILE_ASSEMBLY.get())),
                p(new ItemStack(ModItems.THRUSTER_SMALL.get(), 4)));
        small(writer, "missile_generic", ModItems.MISSILE_GENERIC.get(), ModItems.WARHEAD_GENERIC_SMALL.get());
        small(writer, "missile_incendiary", ModItems.MISSILE_INCENDIARY.get(), ModItems.WARHEAD_INCENDIARY_SMALL.get());
        small(writer, "missile_cluster", ModItems.MISSILE_CLUSTER.get(), ModItems.WARHEAD_CLUSTER_SMALL.get());
        small(writer, "missile_buster", ModItems.MISSILE_BUSTER.get(), ModItems.WARHEAD_BUSTER_SMALL.get());
        emit(writer, "missile_decoy", new ItemStack(ModItems.MISSILE_DECOY.get()), 60, 2_500L,
                p(ore("oredict/ingot/steel"), 1), p(new ItemStack(ModItems.FUEL_TANK_SMALL.get())),
                p(new ItemStack(ModItems.THRUSTER_SMALL.get())));

        medium(writer, "missile_strong", ModItems.MISSILE_STRONG.get(), new ItemStack(ModItems.WARHEAD_GENERIC_MEDIUM.get()));
        medium(writer, "missile_incendiary_strong", ModItems.MISSILE_INCENDIARY_STRONG.get(), new ItemStack(ModItems.WARHEAD_INCENDIARY_MEDIUM.get()));
        medium(writer, "missile_cluster_strong", ModItems.MISSILE_CLUSTER_STRONG.get(), new ItemStack(ModItems.WARHEAD_CLUSTER_MEDIUM.get()));
        medium(writer, "missile_buster_strong", ModItems.MISSILE_BUSTER_STRONG.get(), new ItemStack(ModItems.WARHEAD_BUSTER_MEDIUM.get()));
        medium(writer, "missile_emp_strong", ModItems.MISSILE_EMP_STRONG.get(), new ItemStack(ModBlocks.EMP.get().asItem(), 3));

        large(writer, "missile_burst", ModItems.MISSILE_BURST.get(), ModItems.WARHEAD_GENERIC_LARGE.get());
        large(writer, "missile_inferno", ModItems.MISSILE_INFERNO.get(), ModItems.WARHEAD_INCENDIARY_LARGE.get());
        large(writer, "missile_rain", ModItems.MISSILE_RAIN.get(), ModItems.WARHEAD_CLUSTER_LARGE.get());
        large(writer, "missile_drill", ModItems.MISSILE_DRILL.get(), ModItems.WARHEAD_BUSTER_LARGE.get());

        huge(writer, "missile_nuclear", ModItems.MISSILE_NUCLEAR.get(), ModItems.WARHEAD_NUCLEAR.get());
        huge(writer, "missile_nuclear_cluster", ModItems.MISSILE_NUCLEAR_CLUSTER.get(), ModItems.WARHEAD_MIRV.get());
        huge(writer, "missile_volcano", ModItems.MISSILE_VOLCANO.get(), ModItems.WARHEAD_VOLCANO.get());
    }

    private static Ingredient ore(String path) { return OreDictIngredients.ore(path); }

    private static void small(Consumer<FinishedRecipe> w, String id, net.minecraft.world.item.Item out, net.minecraft.world.item.Item warhead) {
        emit(w, id, new ItemStack(out), 100, 5_000L, p(new ItemStack(warhead)),
                p(new ItemStack(ModItems.FUEL_TANK_SMALL.get())), p(new ItemStack(ModItems.THRUSTER_SMALL.get())));
    }

    private static void medium(Consumer<FinishedRecipe> w, String id, net.minecraft.world.item.Item out, ItemStack warhead) {
        emit(w, id, new ItemStack(out), 200, 10_000L, p(warhead),
                p(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get())), p(new ItemStack(ModItems.THRUSTER_MEDIUM.get())));
    }

    private static void large(Consumer<FinishedRecipe> w, String id, net.minecraft.world.item.Item out, net.minecraft.world.item.Item warhead) {
        emit(w, id, new ItemStack(out), 300, 25_000L, p(new ItemStack(warhead)),
                p(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get(), 2)), p(new ItemStack(ModItems.THRUSTER_MEDIUM.get(), 4)));
    }

    private static void huge(Consumer<FinishedRecipe> w, String id, net.minecraft.world.item.Item out, net.minecraft.world.item.Item warhead) {
        emit(w, id, new ItemStack(out), 600, 50_000L, p(new ItemStack(warhead)),
                p(new ItemStack(ModItems.FUEL_TANK_LARGE.get())), p(new ItemStack(ModItems.THRUSTER_LARGE.get(), 3)));
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
