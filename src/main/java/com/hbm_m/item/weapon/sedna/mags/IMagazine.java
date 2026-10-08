package com.hbm_m.item.weapon.sedna.mags;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.tool.ItemHeldInventory;
import com.hbm_m.item.weapon.sedna.BulletConfig;
import com.hbm_m.particle.SpentCasing;
import com.hbm_m.powerarmor.ArmorTrenchmaster;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code IMagazine}: The magazine simply provides the receiver it's attached to with ammo, the receiver does not
 * care where it comes from. Therefore it is the mag's responsibility to handle reloading, any type restrictions as
 * well as belt-like action from "magless" guns. Inventar = {@link Container} (Spielerinventar oder Geschuetz).
 *
 * @author hbm
 */
public interface IMagazine<T> {

    /** What ammo is loaded currently */
    T getType(ItemStack stack, @Nullable Container inventory);
    /** Sets the mag's ammo type */
    void setType(ItemStack stack, T type);
    /** How much ammo this mag can carry */
    int getCapacity(ItemStack stack);
    /** How much ammo is currently loaded */
    int getAmount(ItemStack stack, @Nullable Container inventory);
    /** Sets the mag's ammo level */
    void setAmount(ItemStack stack, int amount);
    /** removes the specified amount from the magazine */
    void useUpAmmo(ItemStack stack, @Nullable Container inventory, int amount);
    /** If a reload can even be initiated, i.e. the player even has bullets to load, inventory can be null */
    boolean canReload(ItemStack stack, @Nullable Container inventory);
    /** On the begin of a reload, potentially change the mag type before the reload happens for animation purposes */
    void initNewType(ItemStack stack, @Nullable Container inventory);
    /** The action done at the end of one reload cycle, either loading one shell or replacing the whole mag, inventory can be null */
    void reloadAction(ItemStack stack, @Nullable Container inventory);
    /** The stack that should be displayed for the ammo HUD */
    ItemStack getIconForHUD(ItemStack stack, Player player);
    /** It explains itself */
    String reportAmmoStateForHUD(ItemStack stack, Player player);
    /** Casing config to use then ejecting */
    SpentCasing getCasing(ItemStack stack, @Nullable Container inventory);
    /** When reloading, remember the amount before reload is initiated */
    void setAmountBeforeReload(ItemStack stack, int amount);
    /** Amount of rounds before reload has started. Do note that the NBT stack sync likely arrives
     * after the animation packets, so for RELOAD type anims, use the live ammo count instead! */
    int getAmountBeforeReload(ItemStack stack);
    /** Sets amount of ammo after each reload operation */
    void setAmountAfterReload(ItemStack stack, int amount);
    /** Cached amount of ammo after the most recent reload */
    int getAmountAfterReload(ItemStack stack);

    static void handleAmmoBag(@Nullable Container inventory, @Nullable BulletConfig config, int shotsFired) {
        if (config != null && config.casingItem != null && !config.casingItem.isEmpty() && config.casingAmount > 0 && inventory instanceof Inventory inv) {
            for (ItemStack stack : inv.items) {
                if (!stack.isEmpty() && stack.is(ModItems.CASING_BAG.get()) && ItemHeldInventory.pushCasing(stack, config.casingItem, 1F / config.casingAmount * 0.5F * shotsFired)) return;
            }
        }
    }

    static boolean shouldUseUpTrenchie(@Nullable Container inv) {
        if (inv instanceof Inventory invPlayer) {
            boolean trenchie = ArmorTrenchmaster.isTrenchMaster(invPlayer.player);
            boolean aos = ArmorTrenchmaster.hasAoS(invPlayer.player);
            if (trenchie || aos) return invPlayer.player.getRandom().nextInt(3) < 2;
        }
        return true;
    }
}
