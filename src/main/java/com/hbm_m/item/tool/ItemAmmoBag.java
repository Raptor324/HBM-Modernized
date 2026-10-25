package com.hbm_m.item.tool;

import com.hbm_m.platform.StackNbt;

import com.hbm_m.inventory.HeldItemInventory;
import com.hbm_m.inventory.menu.HeldItemMenu;
import com.hbm_m.item.ModItems;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.items.tool.ItemAmmoBag} ({@code ammo_bag}, {@code ammo_bag_infinite}): Rechtsklick oeffnet
 * das Inventar mit 8 Plaetzen ({@code InventoryAmmoBag}, nur Stapel ohne NBT), Nachladen ueber
 * {@code MagazineSingleTypeBase.isAmmoBag/openAmmoBag}. Die Haltbarkeitsleiste zeigt den Fuellstand
 * (nicht bei der unendlichen Tasche).
 */
public class ItemAmmoBag extends ItemHeldInventory {

    public ItemAmmoBag(Properties properties) {
        super(HeldItemMenu.Layout.AMMO_BAG, properties);
    }

    @Override
    //? if < 1.21.1 {
    public int getUseDuration(ItemStack stack) {
    //?} else {
    /*public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity hbmUser) {
    *///?}
        return 1;
    }

    /** Original {@code showDurabilityBar}. */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        if (stack.is(ModItems.AMMO_BAG_INFINITE.get())) return false;
        return !StackNbt.has(stack) || getDurabilityForDisplay(stack) != 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F - (float) getDurabilityForDisplay(stack) * 13.0F);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        // 1.7.10 ForgeHooksClient: HSB((1 - dur) / 3, 1, 1)
        float f = Math.max(0.0F, (float) (1D - getDurabilityForDisplay(stack)));
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    /** Original {@code getDurabilityForDisplay}. */
    public double getDurabilityForDisplay(ItemStack stack) {
        if (!StackNbt.has(stack)) return 1D;

        ItemStack[] slots = HeldItemInventory.readStacksFromNBT(stack, HeldItemMenu.Layout.AMMO_BAG.size);
        int capacity = 0;
        int bullets = 0;
        for (int i = 0; i < HeldItemMenu.Layout.AMMO_BAG.size; i++) {
            ItemStack slot = slots == null ? null : slots[i];
            if (slot == null || slot.isEmpty()) {
                capacity += 64;
            } else {
                capacity += slot.getMaxStackSize();
                bullets += slot.getCount();
            }
        }
        return 1D - (double) bullets / (double) capacity;
    }
}
