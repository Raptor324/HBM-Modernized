package com.hbm_m.item.weapon.sedna.mags;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.particle.SpentCasing;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/** 1:1 {@code MagazineLiquidEngine}: Type of fixed ammo engine. FluidType -> {@link Fluid}. */
public class MagazineLiquidEngine implements IMagazine<Fluid> {

    public static final String KEY_MAG_COUNT = "magcount";
    public static final String KEY_MAG_PREV = "magprev";
    public static final String KEY_MAG_AFTER = "magafter";

    /** A number so the gun tell multiple mags apart */
    public int index;
    /** How much ammo this mag can hold */
    public int capacity;
    /** Whichever fluids we can pour in this bastard */
    public Fluid[] acceptedTypes;

    public MagazineLiquidEngine(int index, int capacity, Fluid... acceptedTypes) {
        this.index = index;
        this.capacity = capacity;
        this.acceptedTypes = acceptedTypes;
    }

    @Override public Fluid getType(ItemStack stack, @Nullable Container inventory) { return acceptedTypes[0]; }
    @Override public void setType(ItemStack stack, Fluid type) { }
    @Override public int getCapacity(ItemStack stack) { return capacity; }

    @Override
    public void useUpAmmo(ItemStack stack, @Nullable Container inventory, int amount) {
        this.setAmount(stack, Math.max(this.getAmount(stack, inventory) - amount, 0));
    }

    @Override public int getAmount(ItemStack stack, @Nullable Container inventory) { return getMagCount(stack, index); }
    @Override public void setAmount(ItemStack stack, int amount) { setMagCount(stack, index, amount); }

    @Override public boolean canReload(ItemStack stack, @Nullable Container inventory) { return false; }
    @Override public void initNewType(ItemStack stack, @Nullable Container inventory) { }
    @Override public void reloadAction(ItemStack stack, @Nullable Container inventory) { }
    @Override public SpentCasing getCasing(ItemStack stack, @Nullable Container inventory) { return null; }

    @Override public ItemStack getIconForHUD(ItemStack stack, Player player) { return MagazineFluid.fluidIcon(this.getType(stack, player.getInventory())); }
    @Override public String reportAmmoStateForHUD(ItemStack stack, Player player) { return getAmount(stack, player.getInventory()) + "/" + this.capacity + "mB"; }

    @Override public void setAmountBeforeReload(ItemStack stack, int amount) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_PREV + index, amount); }
    @Override public int getAmountBeforeReload(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_PREV + index); }
    @Override public void setAmountAfterReload(ItemStack stack, int amount) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_AFTER + index, amount); }
    @Override public int getAmountAfterReload(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_AFTER + index); }

    public static int getMagCount(ItemStack stack, int index) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_COUNT + index); }
    public static void setMagCount(ItemStack stack, int index, int value) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_COUNT + index, value); }
}
