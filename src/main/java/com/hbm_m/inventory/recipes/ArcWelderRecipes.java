package com.hbm_m.inventory.recipes;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Recipe for Adding all the ArcWelder Machine
 *  Made by Fuchs
 */
public class ArcWelderRecipes {

    public static final List<ArcWelderRecipe> recipes = new ArrayList<>();

    // ─── Ingredient helpers ─────────────────────────────────────────────────── Fuchs

    /** Tagged ingredient (replaces OreDictStack) with count. */
    public static ArcIngredient tag(String forgeTag, int count) {
        TagKey<Item> key = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("forge", forgeTag));
        return new ArcIngredient(Ingredient.of(key), count);
    }

    public static ArcIngredient tag(String forgeTag) { return tag(forgeTag, 1); }

    /** Exact item ingredient (replaces ComparableStack). */
    public static ArcIngredient item(ItemStack stack) {
        return new ArcIngredient(Ingredient.of(stack), stack.getCount());
    }

    // ─── Recipe registration ────────────────────────────────────────────────── Fuchs

    public static void registerDefaults() {

        // ── Machine Parts ──────────────────────────────────────────────────────
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.MOTOR.get(), 2), 100, 400L,
                item(new ItemStack(ModItems.COIL_COPPER.get())),
                item(new ItemStack(ModItems.COIL_COPPER_TORUS.get())),
                item(new ItemStack(ModItems.PLATE_IRON.get(), 2))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.MOTOR.get(), 4), 200, 2_000L,
                item(new ItemStack(ModItems.PLATE_STEEL.get(), 2)),
                item(new ItemStack(ModItems.WIRE_DENSE_ADVANCED_ALLOY.get())),
                item(new ItemStack(ModItems.WIRE_DENSE_COPPER.get()))));

        // ── Satellite Parts ────────────────────────────────────────────────────
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.SAT_LASER.get()), 200, 10_000L,
                item(new ItemStack(ModItems.SAT_HEAD_LASER.get())),
                item(new ItemStack(ModItems.SAT_BASE.get()))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.SAT_RADAR.get()), 200, 10_000L,
                item(new ItemStack(ModItems.SAT_HEAD_RADAR.get())),
                item(new ItemStack(ModItems.SAT_BASE.get()))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.SAT_MAPPER.get()), 200, 10_000L,
                item(new ItemStack(ModItems.SAT_HEAD_MAPPER.get())),
                item(new ItemStack(ModItems.SAT_BASE.get()))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.SAT_RESONATOR.get()), 200, 10_000L,
                item(new ItemStack(ModItems.SAT_HEAD_RESONATOR.get())),
                item(new ItemStack(ModItems.SAT_BASE.get()))));

        // ── Machine Parts ─────────────────────────────────────────────────────
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.LOW_DENSITY_ELEMENT.get()), 200, 5_000L,
                tag("ingots/plastic"),
                item(new ItemStack(ModItems.getIngot(ModIngots.FIBERGLASS).get(), 4)),
                item(new ItemStack(ModItems.PLATE_ALUMINUM.get(), 4))));

        // ── Missile Parts ─────────────────────────────────────────────────────
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.FUEL_TANK_SMALL.get()), 100, 1_000L,
                item(new ItemStack(ModBlocks.DECO_STEEL_SCAFFOLD.get().asItem(), 4)),
                item(new ItemStack(ModItems.PLATE_COPPER.get(), 4)),
                item(new ItemStack(ModItems.PLATE_ALUMINUM.get(), 6))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.FUEL_TANK_SMALL.get()), 100, 1_000L,
                item(new ItemStack(ModItems.PLATE_COPPER.get(), 4)),
                item(new ItemStack(ModItems.WIRE_ALUMINIUM.get(), 4)),
                item(new ItemStack(ModItems.PLATE_STEEL.get(), 4))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get()), 100, 2_000L,
                item(new ItemStack(ModBlocks.DECO_STEEL_SCAFFOLD.get().asItem(), 12)),
                item(new ItemStack(ModItems.PLATE_TITANIUM.get(), 8)),
                item(new ItemStack(ModItems.PLATE_CAST_ALUMINIUM.get(), 4))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.THRUSTER_MEDIUM.get()), 100, 2_000L,
                item(new ItemStack(ModItems.getIngot(ModIngots.GRAPHITE).get(), 8)),
                item(new ItemStack(ModItems.MOTOR.get())),
                item(new ItemStack(ModItems.PLATE_STEEL.get(), 8))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.THRUSTER_LARGE.get()), 400, 50_000L,
                item(new ItemStack(ModItems.INGOT_TUNGSTEN_CARBIDE.get(), 12)),
                item(new ItemStack(ModItems.MOTOR.get())),
                item(new ItemStack(ModItems.INGOT_HIGHSPEED_STEEL.get(), 10))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.NEUTRON_REFLECTOR.get()), 200, 10_000L,
                item(new ItemStack(ModItems.PLATE_DURA_STEEL.get())),
                item(new ItemStack(ModItems.INGOT_TUNGSTEN_CARBIDE.get(), 2))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.MISSILE_GENERIC.get()), 60, 1_000L,
                item(new ItemStack(ModItems.FUEL_TANK_SMALL.get())),
                item(new ItemStack(ModItems.THRUSTER_SMALL.get())),
                item(new ItemStack(ModItems.WARHEAD_GENERIC_SMALL.get()))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.MISSILE_RAIN.get()), 200, 20_000L,
                item(new ItemStack(ModItems.THRUSTER_MEDIUM.get(), 4)),
                item(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get(), 2)),
                item(new ItemStack(ModItems.WARHEAD_CLUSTER_LARGE.get()))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.MISSILE_INCENDIARY_STRONG.get()), 100, 5_000L,
                item(new ItemStack(ModItems.THRUSTER_MEDIUM.get())),
                item(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get())),
                item(new ItemStack(ModItems.WARHEAD_INCENDIARY_MEDIUM.get()))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.MISSILE_BUSTER.get()), 60, 1_000L,
                item(new ItemStack(ModItems.THRUSTER_SMALL.get())),
                item(new ItemStack(ModItems.FUEL_TANK_SMALL.get())),
                item(new ItemStack(ModItems.WARHEAD_BUSTER_SMALL.get()))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.FUEL_TANK_LARGE.get()), 400, 50_000L,
                item(new ItemStack(ModBlocks.DECO_STEEL_SCAFFOLD.get().asItem(), 16)),
                item(new ItemStack(ModItems.PLATE_SATURNITE.get(), 12)),
                item(new ItemStack(ModItems.PLATE_WELDED_ALUMINIUM.get(), 8))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.THRUSTER_SMALL.get()), 100, 1_000L,
                item(new ItemStack(ModItems.PLATE_COPPER.get(), 4)),
                item(new ItemStack(ModItems.WIRE_ALUMINIUM.get(), 4)),
                item(new ItemStack(ModItems.PLATE_STEEL.get(), 4))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.MISSILE_NUCLEAR_CLUSTER.get()), 200, 50_000L,
                item(new ItemStack(ModItems.THRUSTER_LARGE.get(), 3)),
                item(new ItemStack(ModItems.FUEL_TANK_LARGE.get())),
                item(new ItemStack(ModItems.WARHEAD_MIRV.get()))));

        // ── Missiles ──────────────────────────────────────────────────────────
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.MISSILE_INCENDIARY.get()), 60, 1_000L,
                item(new ItemStack(ModItems.THRUSTER_SMALL.get())),
                item(new ItemStack(ModItems.FUEL_TANK_SMALL.get())),
                item(new ItemStack(ModItems.WARHEAD_INCENDIARY_SMALL.get()))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.MISSILE_ABM.get()), 60, 1_000L,
                item(new ItemStack(ModItems.THRUSTER_SMALL.get(), 4)),
                item(new ItemStack(ModItems.MISSILE_ASSEMBLY.get())),
                item(new ItemStack(ModItems.BALL_TNT.get(), 3))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.MISSILE_CLUSTER.get()), 60, 1_000L,
                item(new ItemStack(ModItems.THRUSTER_SMALL.get())),
                item(new ItemStack(ModItems.FUEL_TANK_SMALL.get())),
                item(new ItemStack(ModItems.WARHEAD_CLUSTER_SMALL.get()))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.MISSILE_STRONG.get()), 200, 10_000L,
                item(new ItemStack(ModItems.THRUSTER_MEDIUM.get())),
                item(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get())),
                item(new ItemStack(ModItems.WARHEAD_GENERIC_MEDIUM.get()))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.MISSILE_EMP_STRONG.get()), 100, 5_000L,
                item(new ItemStack(ModItems.THRUSTER_MEDIUM.get())),
                item(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get())),
                item(new ItemStack(ModBlocks.EMP.get().asItem(), 3))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.MISSILE_BUSTER_STRONG.get()), 100, 5_000L,
                item(new ItemStack(ModItems.THRUSTER_MEDIUM.get())),
                item(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get())),
                item(new ItemStack(ModItems.WARHEAD_BUSTER_MEDIUM.get()))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.MISSILE_CLUSTER_STRONG.get()), 100, 5_000L,
                item(new ItemStack(ModItems.THRUSTER_MEDIUM.get())),
                item(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get())),
                item(new ItemStack(ModItems.WARHEAD_CLUSTER_MEDIUM.get()))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.MISSILE_DRILL.get()), 200, 20_000L,
                item(new ItemStack(ModItems.THRUSTER_MEDIUM.get(), 4)),
                item(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get(), 2)),
                item(new ItemStack(ModItems.WARHEAD_BUSTER_LARGE.get()))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.MISSILE_BURST.get()), 200, 20_000L,
                item(new ItemStack(ModItems.THRUSTER_MEDIUM.get(), 4)),
                item(new ItemStack(ModItems.FUEL_TANK_MEDIUM.get(), 2)),
                item(new ItemStack(ModItems.WARHEAD_GENERIC_LARGE.get()))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.MISSILE_NUCLEAR.get()), 200, 50_000L,
                item(new ItemStack(ModItems.THRUSTER_LARGE.get(), 3)),
                item(new ItemStack(ModItems.FUEL_TANK_LARGE.get())),
                item(new ItemStack(ModItems.WARHEAD_NUCLEAR.get()))));

        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.MISSILE_VOLCANO.get()), 200, 50_000L,
                item(new ItemStack(ModItems.THRUSTER_LARGE.get(), 3)),
                item(new ItemStack(ModItems.FUEL_TANK_LARGE.get())),
                item(new ItemStack(ModItems.WARHEAD_VOLCANO.get()))));

        // TODO: neutron_reflector, motor parts, dense wires once items are ported
        // recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.NEUTRON_REFLECTOR.get(), 2), 400, 50_000L,
        //         tag("ingots/tungsten_carbide", 2), tag("plates/dura_steel")));

        // ── Dense Wires ────────────────────────────────────────────────────────
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.WIRE_DENSE_IRON.get()),           100,  1_000L, item(new ItemStack(ModItems.WIRE_IRON.get(),           8))));
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.WIRE_DENSE_ALUMINIUM.get()),      100,  1_000L, item(new ItemStack(ModItems.WIRE_ALUMINIUM.get(),      8))));
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.WIRE_DENSE_TITANIUM.get()),       200, 10_000L, item(new ItemStack(ModItems.WIRE_TITANIUM.get(),       8))));
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.WIRE_DENSE_LEAD.get()),           100,    500L, tag("wires_fine/lead",           8)));
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.WIRE_DENSE_COPPER.get()),         100,  1_000L, item(new ItemStack(ModItems.WIRE_COPPER.get(),         8))));
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.WIRE_DENSE_STEEL.get()),          100,  2_000L, item(new ItemStack(ModItems.WIRE_STEEL.get(),          8))));
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.WIRE_DENSE_GOLD.get()),           100,  1_000L, item(new ItemStack(ModItems.WIRE_GOLD.get(),           8))));
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.WIRE_DENSE_ADVANCED_ALLOY.get()), 200, 20_000L, item(new ItemStack(ModItems.WIRE_ADVANCED_ALLOY.get(), 8))));
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.WIRE_DENSE_SCHRABIDIUM.get()),    400, 50_000L, item(new ItemStack(ModItems.WIRE_SCHRABIDIUM.get(),    8))));
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.WIRE_DENSE_SATURNITE.get()),      200, 20_000L, item(new ItemStack(ModItems.WIRE_SATURNITE.get(),      8))));
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.WIRE_DENSE_COMBINE_STEEL.get()),  200, 10_000L, item(new ItemStack(ModItems.WIRE_COMBINE_STEEL.get(),  8))));

        // ── Welded Plates ──────────────────────────────────────────────────────
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.PLATE_WELDED_IRON.get()),       100,    100L, item(new ItemStack(ModItems.PLATE_CAST_IRON.get(),       2))));
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.PLATE_WELDED_STEEL.get()),      100,    500L, item(new ItemStack(ModItems.PLATE_CAST_STEEL.get(),      2))));
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.PLATE_WELDED_COPPER.get()),     200,  1_000L, item(new ItemStack(ModItems.PLATE_CAST_COPPER.get(),     2))));
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.PLATE_WELDED_TITANIUM.get()),   600, 50_000L, item(new ItemStack(ModItems.PLATE_CAST_TITANIUM.get(),   2))));
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.PLATE_WELDED_ALUMINIUM.get()),  300, 10_000L, item(new ItemStack(ModItems.PLATE_CAST_ALUMINIUM.get(),  2))));
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.PLATE_WELDED_TUNGSTEN.get()),   600, 50_000L, item(new ItemStack(ModItems.PLATE_CAST_TUNGSTEN.get(),   2))));
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.PLATE_WELDED_ZIRCONIUM.get()),  600, 10_000L, item(new ItemStack(ModItems.PLATE_CAST_ZIRCONIUM.get(),  2))));
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.PLATE_WELDED_OSMIRIDIUM.get()), 800, 100_000L, item(new ItemStack(ModItems.PLATE_CAST_OSMIRIDIUM.get(), 2))));
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.PLATE_WELDED_TCALLOY.get()),   1200, 1_000_000L, item(new ItemStack(ModItems.PLATE_CAST_TCALLOY.get(),  2))));
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.PLATE_WELDED_CDALLOY.get()),   1200, 1_000_000L, item(new ItemStack(ModItems.PLATE_CAST_CDALLOY.get(),  2))));
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.PLATE_WELDED_CMB.get()),       1200, 1_000_000L, item(new ItemStack(ModItems.PLATE_CAST_CMB.get(),      2))));
        // TODO: plate_welded with per-material sub-items once ported
        // recipes.add(new ArcWelderRecipe(new ItemStack(plate_welded_iron),   100,       100L, tag("plates_cast/iron",      2)));
        // recipes.add(new ArcWelderRecipe(new ItemStack(plate_welded_steel),  100,       500L, tag("plates_cast/steel",     2)));
        // recipes.add(new ArcWelderRecipe(new ItemStack(plate_welded_copper), 200,     1_000L, tag("plates_cast/copper",    2)));
        // recipes.add(new ArcWelderRecipe(new ItemStack(plate_welded_ti),     600,    50_000L, tag("plates_cast/titanium",  2)));
        // recipes.add(new ArcWelderRecipe(new ItemStack(plate_welded_zr),     600,    10_000L, tag("plates_cast/zirconium", 2)));
        // recipes.add(new ArcWelderRecipe(new ItemStack(plate_welded_al),     300,    10_000L, tag("plates_cast/aluminum",  2)));
        // recipes.add(new ArcWelderRecipe(plate_welded_tcalloy, 1200, 1_000_000L, new FluidRequirement(Fluids.OXYGEN, 1_000), tag("plates_cast/tcalloy",   2)));
        // recipes.add(new ArcWelderRecipe(plate_welded_cdalloy, 1200, 1_000_000L, new FluidRequirement(Fluids.OXYGEN, 1_000), tag("plates_cast/cdalloy",   2)));
        // recipes.add(new ArcWelderRecipe(plate_welded_tungsten,1200,  250_000L, new FluidRequirement(Fluids.OXYGEN, 1_000), tag("plates_cast/tungsten",   2)));

        // ── Missile Parts ──────────────────────────────────────────────────────
        // TODO: thrusters, fuel tanks once ported
        // recipes.add(new ArcWelderRecipe(THRUSTER_SMALL,  60,  1_000L, tag("plates/steel",4), tag("wires_fine/aluminum",4), tag("plates/copper",4)));
        // recipes.add(new ArcWelderRecipe(THRUSTER_MEDIUM,100,  2_000L, tag("plates/steel",8), item(MOTOR,1), tag("ingots/graphite",8)));
        // recipes.add(new ArcWelderRecipe(THRUSTER_LARGE, 200,  5_000L, tag("ingots/dura_steel",10), item(MOTOR,1), tag("ingots/tungsten_carbide",12)));
        // recipes.add(new ArcWelderRecipe(FUEL_TANK_SMALL, 60,  1_000L, tag("plates/aluminum",6), tag("plates/copper",4), item(STEEL_SCAFFOLD,4)));
        // recipes.add(new ArcWelderRecipe(FUEL_TANK_MEDIUM,100, 2_000L, tag("plates_cast/aluminum",4), tag("plates/titanium",8), item(STEEL_SCAFFOLD,12)));
        // recipes.add(new ArcWelderRecipe(FUEL_TANK_LARGE, 200, 5_000L, tag("plates_welded/aluminum",8), tag("plates/bigmt",12), item(STEEL_SCAFFOLD,16)));

        // ── Missiles ──────────────────────────────────────────────────────────
        // TODO: warheads, assembly parts once ported
        // recipes.add(new ArcWelderRecipe(MISSILE_GENERIC, 100, 5_000L,
        //         item(WARHEAD_GENERIC_SMALL), item(FUEL_TANK_SMALL), item(THRUSTER_SMALL)));
        // ... (incendiary, cluster, buster, decoy variants)
        // ... (nuclear, volcano once ported)

        // ── Satellites ────────────────────────────────────────────────────────
        // TODO: satellite parts once ported
        // recipes.add(new ArcWelderRecipe(SAT_MAPPER,  600, 10_000L, item(SAT_BASE), item(SAT_HEAD_MAPPER)));
        // ...
    }

    // ─── Recipe lookup ──────────────────────────────────────────────────────── Fuchs


    @Nullable
    public static ArcWelderRecipe getRecipe(ItemStack slot0, ItemStack slot1, ItemStack slot2) {
        ItemStack[] inputs = { slot0, slot1, slot2 };
        outer:
        for (ArcWelderRecipe recipe : recipes) {
            List<ArcIngredient> remaining = new ArrayList<>(List.of(recipe.ingredients));
            for (ItemStack input : inputs) {
                if (input.isEmpty()) continue;
                boolean matched = false;
                Iterator<ArcIngredient> it = remaining.iterator();
                while (it.hasNext()) {
                    ArcIngredient ing = it.next();
                    if (ing.matches(input)) { it.remove(); matched = true; break; }
                }
                if (!matched) continue outer;
            }
            if (remaining.isEmpty()) return recipe;
        }
        return null;
    }

    // ─── Data classes -────────────────────────────────────────────────────────────── Fuchs

    /** Single ingredient with required count. */
    public record ArcIngredient(Ingredient ingredient, int count) {
        public boolean matches(ItemStack stack) {
            return !stack.isEmpty() && ingredient.test(stack) && stack.getCount() >= count;
        }
    }

    /** Optional fluid requirement. */
    public record FluidRequirement(FluidType type, int amount) {
        /** Returns true if the tank satisfies this requirement. */
        public boolean satisfiedBy(FluidTank tank) {
            // Convert the stored vanilla Fluid to HBM FluidType for comparison
            FluidType stored = FluidType.forFluid(tank.getStoredFluid());
            return stored == type && tank.getFill() >= amount;
        }
        /** Consume from the tank (call only after {@link #satisfiedBy} returns true). */
        public void consume(FluidTank tank) {
            tank.setFill(tank.getFill() - amount);
        }
    }

    public static class ArcWelderRecipe {
        public final ArcIngredient[] ingredients;
        public final @Nullable FluidRequirement fluid;
        public final ItemStack output;
        public final int duration;
        public final long consumption;

        public ArcWelderRecipe(ItemStack output, int duration, long consumption,
                                @Nullable FluidRequirement fluid, ArcIngredient... ingredients) {
            this.output      = output;
            this.duration    = duration;
            this.consumption = consumption;
            this.fluid       = fluid;
            this.ingredients = ingredients;
        }

        public ArcWelderRecipe(ItemStack output, int duration, long consumption,
                                ArcIngredient... ingredients) {
            this(output, duration, consumption, null, ingredients);
        }
    }
}
