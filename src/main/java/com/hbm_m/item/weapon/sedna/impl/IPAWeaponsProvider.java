package com.hbm_m.item.weapon.sedna.impl;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code com.hbm.items.armor.IPAWeaponsProvider}: von Power-Armor-Brustteilen (Original ArmorNCRPA, ArmorRPA)
 * implementiert, liefert Nah-/Fernkampfkomponente fuer {@code gun_pa_melee}/{@code gun_pa_ranged}.
 * Original {@code armorInventory[2]} = Brustplatte.
 */
public interface IPAWeaponsProvider {

    public IPAMelee getMeleeComponent(Player entity);

    /** Nur clientseitig aufrufen. */
    @Nullable
    public static IPAMelee getMeleeComponentClient() {
        return getMeleeComponentCommon(com.hbm_m.client.weapon.GunClientHooks.clientPlayer());
    }

    @Nullable
    public static IPAMelee getMeleeComponentCommon(@Nullable Player player) {
        if (player == null) return null;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty() && chest.getItem() instanceof IPAWeaponsProvider prov) {
            return prov.getMeleeComponent(player);
        }
        return null;
    }

    public IPARanged getRangedComponent(Player entity);

    /** Nur clientseitig aufrufen. */
    @Nullable
    public static IPARanged getRangedComponentClient() {
        return getRangedComponentCommon(com.hbm_m.client.weapon.GunClientHooks.clientPlayer());
    }

    @Nullable
    public static IPARanged getRangedComponentCommon(@Nullable Player player) {
        if (player == null) return null;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty() && chest.getItem() instanceof IPAWeaponsProvider prov) {
            return prov.getRangedComponent(player);
        }
        return null;
    }
}
