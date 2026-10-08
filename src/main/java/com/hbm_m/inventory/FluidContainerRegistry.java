package com.hbm_m.inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.inventory.fluid.FluidContainerDefs;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.ItemFluidTank;
import com.hbm_m.main.MainRegistry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
//? if < 1.21.1 {
import net.minecraft.world.item.alchemy.PotionUtils;
//?}
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code com.hbm.inventory.FluidContainerRegistry}: feste Paare (voller Behaelter, leerer Behaelter oder keiner,
 * Fluessigkeit, Menge). Gleichheit wie {@code isItemEqual}: gleicher Gegenstand, bei NBT-Behaeltern
 * ({@link ItemFluidTank}) gleiche Fluessigkeit, beim Wasserflaeschchen gleicher Trank.
 * Wasser/Lava werden ueber {@link VanillaFluidEquivalence} mit den Vanilla-Fluessigkeiten gleichgesetzt.
 */
public final class FluidContainerRegistry {

    private FluidContainerRegistry() {}

    public record FluidContainer(ItemStack fullContainer, @Nullable ItemStack emptyContainer, Fluid type, int content) {}

    public static final List<FluidContainer> allContainers = new ArrayList<>();
    private static final Map<Fluid, List<FluidContainer>> containerMap = new HashMap<>();
    private static boolean initialized = false;

    @Nullable
    private static Item item(String id) {
        ResourceLocation rl = id.contains(":") ? ResourceLocation.tryParse(id) : ResourceLocation.fromNamespaceAndPath("hbm_m", id);
        Item it = rl == null ? Items.AIR : BuiltInRegistries.ITEM.get(rl);
        if (it == Items.AIR) {
            MainRegistry.LOGGER.warn("[FluidContainerRegistry] Gegenstand {} fehlt im Port", id);
            return null;
        }
        return it;
    }

    @Nullable
    private static Fluid fluid(String name) {
        ModFluids.FluidEntry e = ModFluids.getEntry(name);
        if (e == null) {
            MainRegistry.LOGGER.warn("[FluidContainerRegistry] Fluessigkeit {} fehlt im Port", name);
            return null;
        }
        return e.getSource();
    }

    private static void reg(String full, @Nullable String empty, String fluid, int amount) {
        Item f = item(full);
        Item e = empty == null ? null : item(empty);
        Fluid type = fluid(fluid);
        if (f == null || type == null || (empty != null && e == null)) return;
        registerContainer(new FluidContainer(new ItemStack(f), e == null ? null : new ItemStack(e), type, amount));
    }

    /** Original {@code register()}; lazy beim ersten Zugriff, weil Gegenstaende und Fluessigkeiten dann feststehen. */
    public static synchronized void init() {
        if (initialized) return;
        initialized = true;

        Fluid water = ModFluids.WATER.getSource();
        registerContainer(new FluidContainer(new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.BUCKET), water, 1000));
        //? if < 1.21.1 {
        registerContainer(new FluidContainer(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER), new ItemStack(Items.GLASS_BOTTLE), water, 250));
        //?} else {
        /*registerContainer(new FluidContainer(net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, Potions.WATER), new ItemStack(Items.GLASS_BOTTLE), water, 250));
        *///?}
        registerContainer(new FluidContainer(new ItemStack(Items.LAVA_BUCKET), new ItemStack(Items.BUCKET), fluidOrLava(), 1000));
        reg("bucket_mud", "minecraft:bucket", "watz", 1000);
        reg("bucket_schrabidic_acid", "minecraft:bucket", "schrabidic", 1000);
        reg("bucket_sulfuric_acid", "minecraft:bucket", "sulfuric_acid", 1000);

        reg("barrel_red", "tank_steel", "diesel", 10000);
        reg("barrel_pink", "tank_steel", "kerosene", 10000);
        reg("barrel_lox", "tank_steel", "oxygen", 10000);

        reg("ore_oil", null, "crude_oil", 250);
        reg("gneiss_gas_ore", null, "petroleum", com.hbm_m.config.GeneralConfig.enable528 ? 50 : 250);

        reg("cell_deuterium", "cell_empty", "deuterium", 1000);
        reg("cell_tritium", "cell_empty", "tritium", 1000);
        reg("cell_uf6", "cell_empty", "uf6", 1000);
        reg("cell_puf6", "cell_empty", "puf6", 1000);
        reg("cell_antimatter", "cell_empty", "amat", 1000);
        reg("cell_anti_schrabidium", "cell_empty", "aschrab", 1000);
        reg("cell_sas3", "cell_empty", "sas3", 1000);
        reg("bottle_mercury", "minecraft:glass_bottle", "mercury", 1000);
        reg("nugget_mercury", null, "mercury", 125);

        reg("rod_zirnox_tritium", "rod_zirnox_empty", "tritium", 2000);

        reg("particle_hydrogen", "particle_empty", "hydrogen", 1000);
        reg("particle_amat", "particle_empty", "amat", 1000);
        reg("particle_aschrab", "particle_empty", "aschrab", 1000);

        reg("iv_blood", "iv_empty", "blood", 100);
        reg("iv_xp", "iv_xp_empty", "xpjuice", 100);
        reg("minecraft:experience_bottle", "minecraft:glass_bottle", "xpjuice", 100);

        reg("can_mug", "can_empty", "mug", 100);

        for (ModFluids.FluidEntry entry : HbmFluidRegistry.getOrderedFluids()) {
            Fluid type = entry.getSource();
            if (type == ModFluids.NONE.getSource()) continue;
            FluidType ft = FluidType.forFluid(type);

            if (FluidContainerDefs.getCanister(type) != null) registerContainer(new FluidContainer(ItemFluidTank.make(ModItems.CANISTER_FULL.get(), type, 1), new ItemStack(ModItems.CANISTER_EMPTY.get()), type, 1000));
            if (FluidContainerDefs.getGastank(type) != null) registerContainer(new FluidContainer(ItemFluidTank.make(ModItems.GAS_FULL.get(), type, 1), new ItemStack(ModItems.GAS_EMPTY.get()), type, 1000));

            if (ft.hasNoContainer()) continue;

            if (ft.isDispersable()) {
                registerContainer(new FluidContainer(ItemFluidTank.make(ModItems.DISPERSER_CANISTER.get(), type, 1), new ItemStack(ModItems.DISPERSER_CANISTER_EMPTY.get()), type, 2000));
                registerContainer(new FluidContainer(ItemFluidTank.make(ModItems.GLYPHID_GLAND.get(), type, 1), new ItemStack(ModItems.GLYPHID_GLAND_EMPTY.get()), type, 4000));
            }

            registerContainer(new FluidContainer(ItemFluidTank.make(ModItems.FLUID_TANK_LEAD_FULL.get(), type, 1), new ItemStack(ModItems.FLUID_TANK_LEAD_EMPTY.get()), type, 1000));

            if (ft.needsLeadContainer()) continue;

            registerContainer(new FluidContainer(ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), type, 1), new ItemStack(ModItems.FLUID_TANK_EMPTY.get()), type, 1000));
            registerContainer(new FluidContainer(ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), type, 1), new ItemStack(ModItems.FLUID_BARREL_EMPTY.get()), type, 16000));
        }
    }

    private static Fluid fluidOrLava() {
        ModFluids.FluidEntry e = ModFluids.getEntry("lava");
        return e != null ? e.getSource() : net.minecraft.world.level.material.Fluids.LAVA;
    }

    public static void registerContainer(FluidContainer con) {
        allContainers.add(con);
        containerMap.computeIfAbsent(con.type(), k -> new ArrayList<>()).add(con);
    }

    /** Behaelter einer Fluessigkeit; Wasser/Lava auch ueber die Vanilla-Gleichsetzung. */
    public static List<FluidContainer> getContainers(Fluid type) {
        init();
        List<FluidContainer> out = new ArrayList<>();
        for (Map.Entry<Fluid, List<FluidContainer>> e : containerMap.entrySet()) {
            if (e.getKey() == type || VanillaFluidEquivalence.sameSubstance(e.getKey(), type)) out.addAll(e.getValue());
        }
        return out;
    }

    /** isItemEqual: gleicher Gegenstand, bei Fluessigkeitsbehaeltern/Traenken gleiches NBT. */
    public static boolean isItemEqual(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty() || a.getItem() != b.getItem()) return false;
        if (a.getItem() instanceof ItemFluidTank) return ItemFluidTank.getFluid(a) == ItemFluidTank.getFluid(b);
        //? if < 1.21.1 {
        if (a.getItem() == Items.POTION) return PotionUtils.getPotion(a) == PotionUtils.getPotion(b);
        //?} else {
        /*if (a.getItem() == Items.POTION) return a.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, net.minecraft.world.item.alchemy.PotionContents.EMPTY).potion()
                .equals(b.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, net.minecraft.world.item.alchemy.PotionContents.EMPTY).potion());
        *///?}
        return true;
    }

    @Nullable
    public static FluidContainer getContainer(Fluid type, ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (FluidContainer container : getContainers(type)) {
            if (container.emptyContainer() != null && isItemEqual(container.emptyContainer(), stack))
                return container;
        }
        return null;
    }

    public static int getFluidContent(ItemStack stack, Fluid type) {
        if (stack.isEmpty()) return 0;
        for (FluidContainer container : getContainers(type)) {
            if (isItemEqual(container.fullContainer(), stack))
                return container.content();
        }
        return 0;
    }

    public static Fluid getFluidType(ItemStack stack) {
        init();
        if (stack.isEmpty()) return ModFluids.NONE.getSource();
        for (FluidContainer container : allContainers) {
            if (isItemEqual(container.fullContainer(), stack))
                return container.type();
        }
        return ModFluids.NONE.getSource();
    }

    @Nullable
    public static ItemStack getFullContainer(ItemStack stack, Fluid type) {
        if (stack.isEmpty()) return null;
        for (FluidContainer container : getContainers(type)) {
            if (container.emptyContainer() != null && isItemEqual(container.emptyContainer(), stack))
                return container.fullContainer().copy();
        }
        return null;
    }

    /** Leerer Behaelter; {@code null} wenn der volle Behaelter verbraucht wird (Erz, Quecksilbertropfen). */
    @Nullable
    public static ItemStack getEmptyContainer(ItemStack stack) {
        init();
        if (stack.isEmpty()) return null;
        for (FluidContainer container : allContainers) {
            if (isItemEqual(container.fullContainer(), stack))
                return container.emptyContainer() == null ? null : container.emptyContainer().copy();
        }
        return null;
    }
}
