package com.hbm_m.util;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.gasmask.GasMaskUtil;
import com.hbm_m.item.gasmask.IGasMask;

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
        // Оригинал: жёлтый ИЛИ красный ИЛИ серый костюм (плюс шрабидиум/мутация - те в порте пока не носибельны).
        return checkArmor(entity,
                ModItems.HAZMAT_HELMET.orElse(null), ModItems.HAZMAT_CHESTPLATE.orElse(null),
                ModItems.HAZMAT_LEGGINGS.orElse(null), ModItems.HAZMAT_BOOTS.orElse(null))
                || checkArmor(entity,
                ModItems.HAZMAT_HELMET_RED.orElse(null), ModItems.HAZMAT_PLATE_RED.orElse(null),
                ModItems.HAZMAT_LEGS_RED.orElse(null), ModItems.HAZMAT_BOOTS_RED.orElse(null))
                || checkArmor(entity,
                ModItems.HAZMAT_HELMET_GREY.orElse(null), ModItems.HAZMAT_PLATE_GREY.orElse(null),
                ModItems.HAZMAT_LEGS_GREY.orElse(null), ModItems.HAZMAT_BOOTS_GREY.orElse(null));
    }

    public static boolean checkForHaz2(LivingEntity entity) {
        // Оригинал: PaA || liquidator || euphemium || rpa || fau || dns.
        // В порте носибелен только liquidator (PaA - плейсхолдеры).
        return checkArmor(entity,
                ModItems.LIQUIDATOR_HELMET.orElse(null), ModItems.LIQUIDATOR_CHESTPLATE.orElse(null),
                ModItems.LIQUIDATOR_LEGGINGS.orElse(null), ModItems.LIQUIDATOR_BOOTS.orElse(null));
    }

    public static boolean checkForDigamma(Player player) {
        // Original prueft zusaetzlich FaU- und DNS-Ruestung; die gibt es im Port noch nicht.
        return com.hbm_m.platform.PlatformHooks.hasEffect(player, com.hbm_m.effect.ModEffects.STABILITY);
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

    /**
     * Износ фильтра на надетой маске (или на маске, прицепленной к шлему).
     * Порт {@link com.hbm.util.ArmorUtil#damageGasMaskFilter} (1.7.10).
     * The original loses the wear of an attached mask - it writes it into the copy decoded from
     * the helmet's NBT - so the result is written back here.
     */
    public static void damageGasMaskFilter(LivingEntity entity, int damage) {
        if (damage <= 0) {
            return;
        }
        GasMaskUtil.WornMask worn = GasMaskUtil.resolveWornMaskRef(entity);
        if (worn.mask().getItem() instanceof IGasMask && IGasMask.damageFilter(worn.mask(), damage)) {
            worn.commit();
        }
    }

    /** Есть ли на сущности маска (надетая, прицепленная к шлему или в слоте лица Curios). */
    public static boolean isWearingMask(LivingEntity entity) {
        return !GasMaskUtil.resolveWornMask(entity).isEmpty();
    }

    /**
     * Маска надета, но фильтра нет. Порт {@code ArmorUtil.isWearingEmptyMask} (1.7.10).
     */
    public static boolean isWearingEmptyMask(LivingEntity entity) {
        ItemStack mask = GasMaskUtil.resolveWornMask(entity);
        return !mask.isEmpty() && !IGasMask.hasFilter(mask);
    }
}
