package com.hbm_m.item.weapon.sedna.mags;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.HeldItemInventory;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.weapon.sedna.BulletConfig;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.particle.SpentCasing;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code MagazineBelt}: magazinlos, zieht die Munition direkt aus dem Inventar (inkl. Munitionstaschen). */
public class MagazineBelt implements IMagazine<BulletConfig> {

    public List<BulletConfig> acceptedBullets = new ArrayList<>();

    public MagazineBelt addConfigs(BulletConfig... cfgs) { for (BulletConfig cfg : cfgs) acceptedBullets.add(cfg); return this; }

    @Override
    public BulletConfig getType(ItemStack stack, @Nullable Container inventory) {
        BulletConfig config = getFirstConfig(stack, inventory);
        if (getMagType(stack) != config.id) {
            setMagType(stack, config.id);
        }
        return config;
    }

    @Override
    public void useUpAmmo(ItemStack stack, @Nullable Container inventory, int amount) {
        if (inventory == null) return;
        if (!IMagazine.shouldUseUpTrenchie(inventory)) return;

        BulletConfig first = this.getFirstConfig(stack, inventory);

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);

            if (!slot.isEmpty()) {
                if (first.ammo.matchesRecipe(slot, true)) {
                    int toRemove = Math.min(slot.getCount(), amount);
                    amount -= toRemove;
                    inventory.removeItem(i, toRemove);
                    IMagazine.handleAmmoBag(inventory, first, toRemove);
                    if (amount <= 0) return;
                }

                boolean infBag = slot.is(ModItems.AMMO_BAG_INFINITE.get());
                if (MagazineSingleTypeBase.isAmmoBag(slot)) {
                    HeldItemInventory bag = MagazineSingleTypeBase.openAmmoBag(slot);
                    for (int j = 0; j < bag.getContainerSize(); j++) {
                        ItemStack bagslot = bag.getItem(j);

                        if (!bagslot.isEmpty()) {
                            if (first.ammo.matchesRecipe(bagslot, true)) {
                                int toRemove = Math.min(bagslot.getCount(), amount);
                                amount -= toRemove;
                                if (!infBag) bag.removeItem(j, toRemove);
                                IMagazine.handleAmmoBag(inventory, first, toRemove);
                                if (amount <= 0) { bag.setChanged(); return; }
                            }
                        }
                    }
                    bag.setChanged();
                }
            }
        }
    }

    @Override public void setType(ItemStack stack, BulletConfig type) { }
    @Override public int getCapacity(ItemStack stack) { return 0; }
    @Override public void setAmount(ItemStack stack, int amount) { }
    @Override public boolean canReload(ItemStack stack, @Nullable Container inventory) { return false; }
    @Override public void initNewType(ItemStack stack, @Nullable Container inventory) { }
    @Override public void reloadAction(ItemStack stack, @Nullable Container inventory) { }
    @Override public void setAmountBeforeReload(ItemStack stack, int amount) { }
    @Override public int getAmountBeforeReload(ItemStack stack) { return 0; }
    @Override public void setAmountAfterReload(ItemStack stack, int amount) { }
    @Override public int getAmountAfterReload(ItemStack stack) { return 0; }

    @Override
    public int getAmount(ItemStack stack, @Nullable Container inventory) {
        if (inventory == null) return 1; // for EntityAIFireGun
        BulletConfig first = this.getFirstConfig(stack, inventory);
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);

            if (!slot.isEmpty()) {
                if (first.ammo.matchesRecipe(slot, true)) count += slot.getCount();

                boolean infBag = slot.is(ModItems.AMMO_BAG_INFINITE.get());
                if (MagazineSingleTypeBase.isAmmoBag(slot)) {
                    HeldItemInventory bag = MagazineSingleTypeBase.openAmmoBag(slot);
                    for (int j = 0; j < bag.getContainerSize(); j++) {
                        ItemStack bagslot = bag.getItem(j);

                        if (!bagslot.isEmpty()) {
                            if (first.ammo.matchesRecipe(bagslot, true)) {
                                if (infBag) return 9_999;
                                count += bagslot.getCount();
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    @Override
    public ItemStack getIconForHUD(ItemStack stack, Player player) {
        BulletConfig first = this.getFirstConfig(stack, player.getInventory());
        return first.ammo.toStack();
    }

    @Override
    public String reportAmmoStateForHUD(ItemStack stack, Player player) {
        return "x" + getAmount(stack, player.getInventory());
    }

    @Override
    public SpentCasing getCasing(ItemStack stack, @Nullable Container invnetory) {
        return getFirstConfig(stack, invnetory).casing;
    }

    public BulletConfig getFirstConfig(ItemStack stack, @Nullable Container inventory) {

        if (inventory == null) return acceptedBullets.get(0);

        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack slot = inventory.getItem(i);

            if (!slot.isEmpty()) {
                for (BulletConfig config : this.acceptedBullets) {
                    if (config.ammo.matchesRecipe(slot, true)) return config;
                }

                if (MagazineSingleTypeBase.isAmmoBag(slot)) {
                    HeldItemInventory bag = MagazineSingleTypeBase.openAmmoBag(slot);
                    for (int j = 0; j < bag.getContainerSize(); j++) {
                        ItemStack bagslot = bag.getItem(j);

                        if (!bagslot.isEmpty()) {
                            for (BulletConfig config : this.acceptedBullets) {
                                if (config.ammo.matchesRecipe(bagslot, true)) return config;
                            }
                        }
                    }
                }
            }
        }

        int type = getMagType(stack);
        BulletConfig cached = type >= 0 && type < BulletConfig.configs.size() ? BulletConfig.configs.get(type) : null;
        return cached != null && acceptedBullets.contains(cached) ? cached : acceptedBullets.get(0);
    }

    public static final String KEY_MAG_TYPE = "magtype";
    public static int getMagType(ItemStack stack) { return ItemGunBaseNT.getValueInt(stack, KEY_MAG_TYPE); }
    public static void setMagType(ItemStack stack, int value) { ItemGunBaseNT.setValueInt(stack, KEY_MAG_TYPE, value); }
}
