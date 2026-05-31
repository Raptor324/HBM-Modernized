package com.hbm_m.util;

import com.hbm_m.item.ModItems;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Минимальный порт проверок брони для {@link ContaminationUtil}. Полный {@link com.hbm.util.ArmorUtil} — по мере переноса FSB/роб.
 */
public final class ArmorUtil {

    private ArmorUtil() {
    }

    public static boolean checkForHazmat(LivingEntity entity) {
        return checkArmor(entity,
                ModItems.HAZMAT_HELMET.orElse(null), ModItems.HAZMAT_CHESTPLATE.orElse(null),
                ModItems.HAZMAT_LEGGINGS.orElse(null), ModItems.HAZMAT_BOOTS.orElse(null));
    }

    public static boolean checkForHaz2(LivingEntity entity) {
        return false;
    }

    public static boolean checkForDigamma(Player player) {
        return false;
    }

    public static boolean checkForDigamma2(Player player) {
        return false;
    }

    public static boolean checkForFaraday(Player player) {
        return false;
    }

    private static boolean checkArmor(LivingEntity entity, Item helmet, Item chest, Item legs, Item boots) {
        if (helmet == null || chest == null || legs == null || boots == null) {
            return false;
        }
        return checkArmorPiece(entity, helmet, EquipmentSlot.HEAD)
                && checkArmorPiece(entity, chest, EquipmentSlot.CHEST)
                && checkArmorPiece(entity, legs, EquipmentSlot.LEGS)
                && checkArmorPiece(entity, boots, EquipmentSlot.FEET);
    }

    private static boolean checkArmorPiece(LivingEntity entity, Item armor, EquipmentSlot slot) {
        ItemStack stack = entity.getItemBySlot(slot);
        return !stack.isEmpty() && stack.is(armor);
    }
}
