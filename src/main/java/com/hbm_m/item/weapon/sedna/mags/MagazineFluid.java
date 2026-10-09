package com.hbm_m.item.weapon.sedna.mags;

import com.hbm_m.platform.StackNbt;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.ItemFluidTank;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.particle.SpentCasing;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * 1:1 {@code MagazineFluid}. Portabweichung: der Fluidtyp steht als Registry-Schluessel (String) im NBT statt als
 * numerische {@code FluidType}-ID; HUD-Symbol ist ein gefuellter Fluidtank (das Original-{@code fluid_icon} gibt es im Port nicht).
 */
public class MagazineFluid implements IMagazine<Fluid> {

    public static final String KEY_MAG_COUNT = "magcount";
    public static final String KEY_MAG_TYPE = "magtype";
    public static final String KEY_MAG_PREV = "magprev";
    public static final String KEY_MAG_AFTER = "magafter";

    /** A number so the gun tell multiple mags apart */
    public int index;
    /** How much ammo this mag can hold */
    public int capacity;

    public MagazineFluid(int index, int capacity) {
        this.index = index;
        this.capacity = capacity;
    }

    @Override
    public Fluid getType(ItemStack stack, @Nullable Container inventory) {
        return getMagType(stack, index);
    }

    @Override
    public void setType(ItemStack stack, Fluid type) {
        setMagType(stack, index, type);
    }

    @Override
    public int getCapacity(ItemStack stack) {
        return capacity;
    }

    @Override
    public void useUpAmmo(ItemStack stack, @Nullable Container inventory, int amount) {
        this.setAmount(stack, this.getAmount(stack, inventory) - amount);
    }

    @Override public int getAmount(ItemStack stack, @Nullable Container inventory) { return getMagCount(stack, index); }
    @Override public void setAmount(ItemStack stack, int amount) { setMagCount(stack, index, amount); }

    @Override public boolean canReload(ItemStack stack, @Nullable Container inventory) { return false; }
    @Override public void initNewType(ItemStack stack, @Nullable Container inventory) { }
    @Override public void reloadAction(ItemStack stack, @Nullable Container inventory) { }
    @Override public SpentCasing getCasing(ItemStack stack, @Nullable Container inventory) { return null; }

    @Override public ItemStack getIconForHUD(ItemStack stack, Player player) { return fluidIcon(getMagType(stack, index)); }
    @Override public String reportAmmoStateForHUD(ItemStack stack, Player player) { return getAmount(stack, player.getInventory()) + "mB"; }

    @Override public void setAmountBeforeReload(ItemStack stack, int amount) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_PREV + index, amount); }
    @Override public int getAmountBeforeReload(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_PREV + index); }
    @Override public void setAmountAfterReload(ItemStack stack, int amount) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_AFTER + index, amount); }
    @Override public int getAmountAfterReload(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_AFTER + index); }

    /** Ersatz fuer {@code new ItemStack(ModItems.fluid_icon, 1, fluid.getID())}. */
    public static ItemStack fluidIcon(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return ItemStack.EMPTY;
        return ItemFluidTank.make(ModItems.FLUID_TANK_FULL.get(), fluid, 1);
    }

    public static Fluid getMagType(ItemStack stack, int index) {
        if (!StackNbt.has(stack)) return Fluids.EMPTY;
        ResourceLocation key = ResourceLocation.tryParse(StackNbt.read(stack).getString(KEY_MAG_TYPE + index));
        return key == null ? Fluids.EMPTY : BuiltInRegistries.FLUID.get(key);
    }
    public static void setMagType(ItemStack stack, int index, Fluid value) {
        StackNbt.orCreate(stack).putString(KEY_MAG_TYPE + index, BuiltInRegistries.FLUID.getKey(value == null ? Fluids.EMPTY : value).toString());
    }
    public static int getMagCount(ItemStack stack, int index) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_COUNT + index); }
    public static void setMagCount(ItemStack stack, int index, int value) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_COUNT + index, value); }
}
