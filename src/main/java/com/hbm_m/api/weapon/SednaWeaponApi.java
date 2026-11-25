package com.hbm_m.api.weapon;

import com.hbm_m.item.weapon.sedna.BulletConfig;
import com.hbm_m.item.weapon.sedna.GunConfig;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.Receiver;
import com.hbm_m.item.weapon.sedna.mags.IMagazine;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * Stable common-side API facade for inspecting and mutating SEDNA {@link ItemGunBaseNT} state
 * without coupling external mods to internal SEDNA classes ({@code GunConfig}, {@code Receiver},
 * {@code IMagazine}, {@code BulletConfig}, etc.).
 *
 * <p>Uses only vanilla Minecraft and HBM types so it compiles cleanly across all Stonecutter
 * loader/version targets.
 */
public final class SednaWeaponApi {

    private SednaWeaponApi() {
    }

    public static boolean isSednaGun(@Nullable ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemGunBaseNT;
    }

    @Nullable
    private static Receiver receiver(ItemStack stack, int configIndex, int receiverIndex) {
        if (!(stack.getItem() instanceof ItemGunBaseNT gun)) {
            return null;
        }
        try {
            if (configIndex < 0 || configIndex >= gun.getConfigsCount()) {
                return null;
            }
            GunConfig cfg = gun.getConfig(stack, configIndex);
            Receiver[] receivers = cfg.getReceivers(stack);
            return receiverIndex >= 0 && receiverIndex < receivers.length ? receivers[receiverIndex] : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    private static Receiver primaryReceiver(ItemStack stack) {
        return receiver(stack, 0, 0);
    }

    @Nullable
    private static IMagazine<?> magazine(ItemStack stack, int configIndex, int receiverIndex) {
        Receiver rec = receiver(stack, configIndex, receiverIndex);
        return rec == null ? null : rec.getMagazine(stack);
    }

    @Nullable
    private static IMagazine<?> primaryMagazine(ItemStack stack) {
        return magazine(stack, 0, 0);
    }

    public static int getConfigCount(@Nullable ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemGunBaseNT gun
                ? gun.getConfigsCount() : 0;
    }

    public static int getReceiverCount(@Nullable ItemStack stack, int configIndex) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemGunBaseNT gun)) {
            return 0;
        }
        if (configIndex < 0 || configIndex >= gun.getConfigsCount()) {
            return 0;
        }
        return gun.getConfig(stack, configIndex).getReceivers(stack).length;
    }

    public static boolean isBeltFed(ItemStack stack) {
        return isBeltFed(stack, 0, 0);
    }

    public static boolean isBeltFed(ItemStack stack, int configIndex, int receiverIndex) {
        return magazine(stack, configIndex, receiverIndex) instanceof com.hbm_m.item.weapon.sedna.mags.MagazineBelt;
    }

    /** Returns the raw ammo count stored in receiver 0's magazine (0 if unavailable or belt-fed without inventory). */
    public static int getRawAmmoCount(ItemStack stack) {
        return getRawAmmoCount(stack, 0, 0, null);
    }

    /** Returns the raw ammo count stored in receiver 0's magazine, using {@code inventory} for belt-fed weapons. */
    public static int getRawAmmoCount(ItemStack stack, @Nullable Container inventory) {
        return getRawAmmoCount(stack, 0, 0, inventory);
    }

    public static int getRawAmmoCount(ItemStack stack, int configIndex, int receiverIndex, @Nullable Container inventory) {
        IMagazine<?> mag = magazine(stack, configIndex, receiverIndex);
        if (mag == null) {
            return 0;
        }
        if (inventory == null && mag instanceof com.hbm_m.item.weapon.sedna.mags.MagazineBelt) {
            return 0;
        }
        return Math.max(mag.getAmount(stack, inventory), 0);
    }

    /** Sets the raw ammo count stored in receiver 0's magazine (clamped to {@code >= 0}). */
    public static void setRawAmmoCount(ItemStack stack, int amount) {
        setRawAmmoCount(stack, 0, 0, amount);
    }

    public static void setRawAmmoCount(ItemStack stack, int configIndex, int receiverIndex, int amount) {
        IMagazine<?> mag = magazine(stack, configIndex, receiverIndex);
        if (mag != null) {
            mag.setAmount(stack, Math.max(amount, 0));
        }
    }

    /** Returns the capacity of receiver 0's magazine (0 if unavailable). */
    public static int getAmmoCapacity(ItemStack stack) {
        return getAmmoCapacity(stack, 0, 0);
    }

    public static int getAmmoCapacity(ItemStack stack, int configIndex, int receiverIndex) {
        IMagazine<?> mag = magazine(stack, configIndex, receiverIndex);
        return mag == null ? 0 : Math.max(mag.getCapacity(stack), 0);
    }

    /** Returns {@code true} when receiver 0's magazine can reload from {@code inventory}. */
    public static boolean canReload(ItemStack stack, @Nullable Container inventory) {
        return canReload(stack, 0, 0, inventory);
    }

    public static boolean canReload(ItemStack stack, int configIndex, int receiverIndex, @Nullable Container inventory) {
        IMagazine<?> mag = magazine(stack, configIndex, receiverIndex);
        if (mag == null) {
            return false;
        }
        try {
            return mag.canReload(stack, inventory);
        } catch (Exception ignored) {
            return true;
        }
    }

    public static int getReloadDurationTicks(ItemStack stack) {
        return getReloadDurationTicks(stack, 0, 0);
    }

    public static int getReloadDurationTicks(ItemStack stack, int configIndex, int receiverIndex) {
        Receiver rec = receiver(stack, configIndex, receiverIndex);
        return rec == null ? 0 : Math.max(rec.getReloadBeginDuration(stack) + rec.getReloadCycleDuration(stack) + rec.getReloadEndDuration(stack), 0);
    }

    public static int getAmountBeforeReload(ItemStack stack) {
        return getAmountBeforeReload(stack, 0, 0);
    }

    public static int getAmountBeforeReload(ItemStack stack, int configIndex, int receiverIndex) {
        IMagazine<?> mag = magazine(stack, configIndex, receiverIndex);
        return mag == null ? 0 : Math.max(mag.getAmountBeforeReload(stack), 0);
    }

    /** Returns {@code true} when receiver 0 supports automatic refire on hold. */
    public static boolean supportsAutoFire(ItemStack stack) {
        Receiver rec = primaryReceiver(stack);
        return rec != null && rec.getRefireOnHold(stack);
    }

    /** Returns receiver 0's fire mode index (0 = auto when supported, 1 = semi). */
    public static int getFireMode(ItemStack stack) {
        return ItemGunBaseNT.getMode(stack, 0);
    }

    /** Sets receiver 0's fire mode index (0 = auto, 1 = semi). */
    public static void setFireMode(ItemStack stack, int mode) {
        ItemGunBaseNT.setMode(stack, 0, mode);
    }

    /** Returns effective rounds-per-minute derived from receiver 0's fire delay. */
    public static int getRpm(ItemStack stack) {
        Receiver rec = primaryReceiver(stack);
        int delay = rec != null ? rec.getDelayAfterFire(stack) : 0;
        return delay > 0 ? 1200 / delay : 300;
    }

    public static boolean isAiming(ItemStack stack) {
        return ItemGunBaseNT.getIsAiming(stack);
    }

    public static void setAiming(ItemStack stack, boolean aiming) {
        ItemGunBaseNT.setIsAiming(stack, aiming);
    }

    public static ItemGunBaseNT.GunState getState(ItemStack stack) {
        return ItemGunBaseNT.getState(stack, 0);
    }

    public static int getTimer(ItemStack stack) {
        return ItemGunBaseNT.getTimer(stack, 0);
    }

    /**
     * Returns the Minecraft {@link Item} corresponding to the ammo currently loaded in receiver
     * 0's magazine, or {@code null} if unknown / non-item / air.
     */
    @Nullable
    public static Item getLoadedAmmoItem(ItemStack stack) {
        return getLoadedAmmoItem(stack, 0, 0, null);
    }

    @Nullable
    public static Item getLoadedAmmoItem(ItemStack stack, @Nullable Container inventory) {
        return getLoadedAmmoItem(stack, 0, 0, inventory);
    }

    @Nullable
    public static Item getLoadedAmmoItem(ItemStack stack, int configIndex, int receiverIndex, @Nullable Container inventory) {
        IMagazine<?> mag = magazine(stack, configIndex, receiverIndex);
        if (mag == null) {
            return null;
        }
        if (inventory == null && mag instanceof com.hbm_m.item.weapon.sedna.mags.MagazineBelt belt) {
            int cachedId = com.hbm_m.item.weapon.sedna.mags.MagazineBelt.getMagType(stack);
            for (BulletConfig cfg : belt.acceptedBullets) {
                if (cfg.id == cachedId && cfg.ammo != null) {
                    Item item = cfg.ammo.getItem();
                    return item != Items.AIR ? item : null;
                }
            }
            return null;
        }
        if (mag.getType(stack, inventory) instanceof BulletConfig bullet && bullet.ammo != null) {
            Item item = bullet.ammo.getItem();
            return item != Items.AIR ? item : null;
        }
        return null;
    }

    /**
     * Triggers receiver 0's primary-press lambda on the given stack (used e.g. to flush a queued
     * rapid semi-auto click once the gun returns to {@link ItemGunBaseNT.GunState#IDLE}).
     */
    public static void triggerPrimaryPress(ItemStack stack, LivingEntity entity, @Nullable Container inventory) {
        if (!(stack.getItem() instanceof ItemGunBaseNT gun)) {
            return;
        }
        GunConfig cfg = gun.getConfig(stack, 0);
        if (cfg != null && cfg.getPressPrimary(stack) != null) {
            cfg.getPressPrimary(stack).accept(stack, new ItemGunBaseNT.LambdaContext(cfg, entity, inventory, 0));
        }
    }
}
