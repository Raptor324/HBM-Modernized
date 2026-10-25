package com.hbm_m.item.weapon.sedna.mags;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.weapon.sedna.BulletConfig;
import com.hbm_m.particle.SpentCasing;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code MagazineInfinite}: unendliches Magazin mit festem Geschoss (Geschuetze, Debug). */
public class MagazineInfinite implements IMagazine<Object> {

    public BulletConfig type;

    public MagazineInfinite(BulletConfig type) {
        this.type = type;
    }

    @Override public Object getType(ItemStack stack, @Nullable Container inventory) { return this.type; }
    @Override public void setType(ItemStack stack, Object type) { }
    @Override public int getCapacity(ItemStack stack) { return 9999; }
    @Override public int getAmount(ItemStack stack, @Nullable Container inventory) { return 9999; }
    @Override public void setAmount(ItemStack stack, int amount) { }
    @Override public void useUpAmmo(ItemStack stack, @Nullable Container inventory, int amount) { }
    @Override public boolean canReload(ItemStack stack, @Nullable Container inventory) { return false; }
    @Override public void initNewType(ItemStack stack, @Nullable Container inventory) { }
    @Override public void reloadAction(ItemStack stack, @Nullable Container inventory) { }
    /** Original {@code new ItemStack(ModItems.nothing)}. */
    @Override public ItemStack getIconForHUD(ItemStack stack, Player player) { return new ItemStack(com.hbm_m.item.ModItems.NOTHING.get()); }
    @Override public String reportAmmoStateForHUD(ItemStack stack, Player player) { return "\u221E"; }
    @Override public SpentCasing getCasing(ItemStack stack, @Nullable Container inventory) { return this.type.casing; }
    @Override public void setAmountBeforeReload(ItemStack stack, int amount) { }
    @Override public int getAmountBeforeReload(ItemStack stack) { return 9999; }
    @Override public void setAmountAfterReload(ItemStack stack, int amount) { }
    @Override public int getAmountAfterReload(ItemStack stack) { return 9999; }
}
