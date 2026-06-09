package com.hbm_m.inventory.recipes;

import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.item.ModItems;
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

        // ── Satellite Parts ────────────────────────────────────────────────────
        recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.SAT_LASER.get()), 200, 10_000L,
                item(new ItemStack(ModItems.SAT_HEAD_LASER.get())),
                item(new ItemStack(ModItems.SAT_BASE.get()))));

        // TODO: neutron_reflector, motor parts, dense wires once items are ported
        // recipes.add(new ArcWelderRecipe(new ItemStack(ModItems.NEUTRON_REFLECTOR.get(), 2), 400, 50_000L,
        //         tag("ingots/tungsten_carbide", 2), tag("plates/dura_steel")));

        // ── Dense Wires ────────────────────────────────────────────────────────
        // TODO: wire_dense with per-material sub-items once ported
        // recipes.add(new ArcWelderRecipe(new ItemStack(wire_dense_copper), 100, 10_000L,
        //         tag("wires_fine/copper", 8)));
        // ... (mingrade, alloy, gold variants)

        // ── Welded Plates ──────────────────────────────────────────────────────
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
