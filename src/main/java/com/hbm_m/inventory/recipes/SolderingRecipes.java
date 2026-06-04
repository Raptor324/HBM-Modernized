package com.hbm_m.inventory.recipes;

import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SolderingRecipes {

    public static final List<SolderingRecipe> recipes = new ArrayList<>();

    public static final Set<Ingredient> toppings = new HashSet<>();
    public static final Set<Ingredient> pcb       = new HashSet<>();
    public static final Set<Ingredient> solder    = new HashSet<>();

    private static SolderingIngredient tag(String forgeTag, int count) {
        TagKey<Item> key = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("forge", forgeTag));
        return new SolderingIngredient(Ingredient.of(key), count);
    }

    private static SolderingIngredient tag(String forgeTag) { return tag(forgeTag, 1); }

    private static SolderingIngredient item(Item i, int count) {
        return new SolderingIngredient(Ingredient.of(i), count);
    }

    private static SolderingIngredient item(Item i) { return item(i, 1); }

    private static FluidRequirement fluid(FluidType type, int mb) {
        return new FluidRequirement(type, mb);
    }

    private static FluidType ft(ModFluids.FluidEntry e) {
        return FluidType.forFluid(e.getSource());
    }

    public static void registerDefaults() {
        recipes.clear();
        toppings.clear();
        pcb.clear();
        solder.clear();

        Item vacTube = ModItems.VACUUM_TUBE.get();
        Item cap     = ModItems.CAPACITOR.get();
        Item capTal  = ModItems.CAPACITOR_TANTALUM.get();
        Item pcbItem = ModItems.PCB.get();
        Item chip    = ModItems.SILICON_CIRCUIT.get();
        Item chipBis = ModItems.BISMOID_CHIP.get();
        Item chipQ   = ModItems.QUANTUM_CHIP.get();
        Item clock   = ModItems.ATOMIC_CLOCK.get();
        Item chassis = ModItems.CONTROLLER_CHASSIS.get();

        addRecipe(new ItemStack(ModItems.ANALOG_CIRCUIT.get()), 100, 100, null,
                new SolderingIngredient[]{item(vacTube, 3), item(cap, 2)},
                new SolderingIngredient[]{item(pcbItem, 4)},
                new SolderingIngredient[]{tag("wires_fine/lead", 4)});

        addRecipe(new ItemStack(ModItems.INTEGRATED_CIRCUIT.get()), 200, 250, null,
                new SolderingIngredient[]{item(chip, 4)},
                new SolderingIngredient[]{item(pcbItem, 4)},
                new SolderingIngredient[]{tag("wires_fine/lead", 4)});

        addRecipe(new ItemStack(ModItems.ADVANCED_CIRCUIT.get()), 300, 1_000,
                fluid(ft(ModFluids.SULFURIC_ACID), 1_000),
                new SolderingIngredient[]{item(chip, 16), item(cap, 4)},
                new SolderingIngredient[]{item(pcbItem, 8), tag("ingots/rubber", 2)},
                new SolderingIngredient[]{tag("wires_fine/lead", 8)});

        addRecipe(new ItemStack(ModItems.CAPACITOR_BOARD.get()), 200, 300,
                fluid(ft(ModFluids.PEROXIDE), 250),
                new SolderingIngredient[]{item(capTal, 3)},
                new SolderingIngredient[]{item(pcbItem, 1)},
                new SolderingIngredient[]{tag("wires_fine/lead", 3)});

        addRecipe(new ItemStack(ModItems.BISMOID_CIRCUIT.get()), 400, 10_000,
                fluid(ft(ModFluids.SOLVENT), 1_000),
                new SolderingIngredient[]{item(chipBis, 4), item(chip, 16), item(cap, 24)},
                new SolderingIngredient[]{item(pcbItem, 12), tag("ingots/plastic", 2)},
                new SolderingIngredient[]{tag("wires_fine/lead", 12)});

        addRecipe(new ItemStack(ModItems.QUANTUM_CIRCUIT.get()), 400, 100_000,
                fluid(ft(ModFluids.HELIUM4), 1_000),
                new SolderingIngredient[]{item(chipQ, 4), item(chipBis, 16), item(clock, 4)},
                new SolderingIngredient[]{item(pcbItem, 16), tag("ingots/plastic", 4)},
                new SolderingIngredient[]{tag("wires_fine/lead", 16)});

        addRecipe(new ItemStack(ModItems.CONTROLLER_ADVANCED.get()), 600, 25_000,
                fluid(ft(ModFluids.PERFLUOROMETHYL), 4_000),
                new SolderingIngredient[]{item(chipBis, 16), item(capTal, 48), item(clock, 1)},
                new SolderingIngredient[]{item(chassis, 1), item(ModItems.UPGRADE_SPEED_3.get())},
                new SolderingIngredient[]{tag("wires_fine/lead", 24)});

        addUpgradeTier1(ModItems.UPGRADE_SPEED_1.get(),    ModItems.UPGRADE_SPEED_2.get());
        addUpgradeTier1(ModItems.UPGRADE_EFFECT_1.get(),   ModItems.UPGRADE_EFFECT_2.get());
        addUpgradeTier1(ModItems.UPGRADE_POWER_1.get(),    ModItems.UPGRADE_POWER_2.get());
        addUpgradeTier1(ModItems.UPGRADE_FORTUNE_1.get(),  ModItems.UPGRADE_FORTUNE_2.get());
        addUpgradeTier1(ModItems.UPGRADE_AFTERBURN_1.get(),ModItems.UPGRADE_AFTERBURN_2.get());

        addUpgradeTier2(ModItems.UPGRADE_SPEED_2.get(),    ModItems.UPGRADE_SPEED_3.get());
        addUpgradeTier2(ModItems.UPGRADE_EFFECT_2.get(),   ModItems.UPGRADE_EFFECT_3.get());
        addUpgradeTier2(ModItems.UPGRADE_POWER_2.get(),    ModItems.UPGRADE_POWER_3.get());
        addUpgradeTier2(ModItems.UPGRADE_FORTUNE_2.get(),  ModItems.UPGRADE_FORTUNE_3.get());
        addUpgradeTier2(ModItems.UPGRADE_AFTERBURN_2.get(),ModItems.UPGRADE_AFTERBURN_3.get());
    }

    private static void addUpgradeTier1(Item lower, Item higher) {
        addRecipe(new ItemStack(higher), 300, 10_000, null,
                new SolderingIngredient[]{item(ModItems.SILICON_CIRCUIT.get(), 8), item(ModItems.CAPACITOR.get(), 4)},
                new SolderingIngredient[]{item(lower), tag("ingots/plastic", 4)},
                new SolderingIngredient[]{});
    }

    private static void addUpgradeTier2(Item lower, Item higher) {
        addRecipe(new ItemStack(higher), 400, 25_000,
                fluid(ft(ModFluids.SOLVENT), 500),
                new SolderingIngredient[]{item(ModItems.SILICON_CIRCUIT.get(), 16), item(ModItems.CAPACITOR.get(), 16)},
                new SolderingIngredient[]{item(lower), tag("ingots/rubber", 4)},
                new SolderingIngredient[]{});
    }

    private static void addRecipe(ItemStack output, int duration, long consumption,
                                   @Nullable FluidRequirement fluidReq,
                                   SolderingIngredient[] tops, SolderingIngredient[] board, SolderingIngredient[] sol) {
        recipes.add(new SolderingRecipe(output, duration, consumption, fluidReq, tops, board, sol));
    }

    public static @Nullable SolderingRecipe getRecipe(ItemStack slot0, ItemStack slot1, ItemStack slot2,
                                                       ItemStack slot3, ItemStack slot4, ItemStack slot5) {
        ItemStack[] tops    = {slot0, slot1, slot2};
        ItemStack[] boards  = {slot3, slot4};
        ItemStack[] solders = {slot5};
        for (SolderingRecipe r : recipes) {
            if (matchesGroup(tops, r.toppings) && matchesGroup(boards, r.pcb) && matchesGroup(solders, r.solder))
                return r;
        }
        return null;
    }

    private static boolean matchesGroup(ItemStack[] inputs, SolderingIngredient[] required) {
        List<SolderingIngredient> rem = new ArrayList<>(List.of(required));
        for (ItemStack input : inputs) {
            if (input.isEmpty()) continue;
            boolean matched = false;
            Iterator<SolderingIngredient> it = rem.iterator();
            while (it.hasNext()) {
                SolderingIngredient ing = it.next();
                if (ing.matches(input)) { it.remove(); matched = true; break; }
            }
            if (!matched) return false;
        }
        return rem.isEmpty();
    }

    public record SolderingIngredient(Ingredient ingredient, int count) {
        public boolean matches(ItemStack stack) {
            return !stack.isEmpty() && ingredient.test(stack) && stack.getCount() >= count;
        }
    }

    public record FluidRequirement(FluidType type, int amount) {
        public boolean satisfiedBy(FluidTank tank) {
            return FluidType.forFluid(tank.getStoredFluid()) == type && tank.getFill() >= amount;
        }
        public void consume(FluidTank tank) { tank.setFill(tank.getFill() - amount); }
    }

    public static class SolderingRecipe {
        public final SolderingIngredient[] toppings;
        public final SolderingIngredient[] pcb;
        public final SolderingIngredient[] solder;
        public final @Nullable FluidRequirement fluid;
        public final ItemStack output;
        public final int duration;
        public final long consumption;

        public SolderingRecipe(ItemStack output, int duration, long consumption,
                                @Nullable FluidRequirement fluid,
                                SolderingIngredient[] toppings,
                                SolderingIngredient[] pcb,
                                SolderingIngredient[] solder) {
            this.output = output; this.duration = duration; this.consumption = consumption;
            this.fluid = fluid; this.toppings = toppings; this.pcb = pcb; this.solder = solder;
            for (SolderingIngredient i : toppings) SolderingRecipes.toppings.add(i.ingredient());
            for (SolderingIngredient i : pcb)      SolderingRecipes.pcb.add(i.ingredient());
            for (SolderingIngredient i : solder)   SolderingRecipes.solder.add(i.ingredient());
        }
    }
}
