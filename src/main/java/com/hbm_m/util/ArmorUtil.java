package com.hbm_m.util;

import java.util.Locale;
import java.util.function.Supplier;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.handler.HazmatRegistry;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.gasmask.GasMaskUtil;
import com.hbm_m.item.gasmask.IGasMask;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1-Port von {@code com.hbm.util.ArmorUtil} (1.7.10): Satzpruefungen (Hazmat, Haz2, Asbest,
 * Digamma, Faraday, Fiend), Anzugschaden, Flugzeit-Reset und Gasmasken-Filter.
 *
 * <p>Wo der Port fuer ein Originalteil zwei IDs fuehrt (etwa {@code hazmat_plate} und das aeltere,
 * eigenstaendige {@code hazmat_plate}), zaehlen beide - bis die Ruestungsrunde die Dubletten
 * aufloest.</p>
 */
public final class ArmorUtil {

    private ArmorUtil() {}

    @SafeVarargs
    private static boolean checkArmorAny(LivingEntity entity, Supplier<? extends Item>... armor) {
        // armor: helmet, plate, legs, boots
        return checkArmorPiece(entity, armor[0], EquipmentSlot.HEAD) && checkArmorPiece(entity, armor[1], EquipmentSlot.CHEST)
                && checkArmorPiece(entity, armor[2], EquipmentSlot.LEGS) && checkArmorPiece(entity, armor[3], EquipmentSlot.FEET);
    }

    private static boolean checkArmorPiece(LivingEntity entity, Supplier<? extends Item> armor, EquipmentSlot slot) {
        ItemStack stack = entity.getItemBySlot(slot);
        if (stack.isEmpty()) return false;
        try {
            return stack.getItem() == armor.get();
        } catch (Exception e) {
            return false;
        }
    }

    /** Original ArmorUtil.external: Gefahrenklassen-Schutz, den ArmorFSB.setHazardClass anmeldet. */
    public static final java.util.List<java.util.Map.Entry<net.minecraft.world.item.Item, com.hbm_m.handler.HazardClass[]>> external = new java.util.ArrayList<>();

    public static final com.hbm_m.handler.HazardClass[] FULL_NO_LIGHT = {
            com.hbm_m.handler.HazardClass.PARTICLE_COARSE, com.hbm_m.handler.HazardClass.PARTICLE_FINE, com.hbm_m.handler.HazardClass.GAS_LUNG,
            com.hbm_m.handler.HazardClass.BACTERIA, com.hbm_m.handler.HazardClass.GAS_BLISTERING, com.hbm_m.handler.HazardClass.GAS_MONOXIDE,
            com.hbm_m.handler.HazardClass.SAND};
    public static final com.hbm_m.handler.HazardClass[] FULL_PACKAGE = {
            com.hbm_m.handler.HazardClass.PARTICLE_COARSE, com.hbm_m.handler.HazardClass.PARTICLE_FINE, com.hbm_m.handler.HazardClass.GAS_LUNG,
            com.hbm_m.handler.HazardClass.BACTERIA, com.hbm_m.handler.HazardClass.GAS_BLISTERING, com.hbm_m.handler.HazardClass.GAS_MONOXIDE,
            com.hbm_m.handler.HazardClass.LIGHT, com.hbm_m.handler.HazardClass.SAND};

    public static void registerExternalProtection(net.minecraft.world.item.Item item, com.hbm_m.handler.HazardClass... classes) {
        external.add(java.util.Map.entry(item, classes));
    }

    public static boolean checkArmor(LivingEntity entity, Item helmet, Item plate, Item legs, Item boots) {
        return checkArmorAny(entity, () -> helmet, () -> plate, () -> legs, () -> boots);
    }

    public static boolean checkArmorPiece(LivingEntity entity, Item armor, EquipmentSlot slot) {
        return checkArmorPiece(entity, () -> armor, slot);
    }

    public static void damageSuit(LivingEntity entity, EquipmentSlot slot, int amount) {
        ItemStack stack = entity.getItemBySlot(slot);
        if (stack.isEmpty()) return;
        stack.hurtAndBreak(amount, entity, e -> e.broadcastBreakEvent(slot));
    }

    /**
     * Original: {@code NetHandlerPlayServer.floatingTickCount = 0} - verhindert den "Flying is not
     * enabled"-Kick bei Jetpacks, Leitern an Multiblocks usw. 1.20: {@code aboveGroundTickCount}.
     */
    public static void resetFlightTime(Player player) {
        if (player instanceof ServerPlayer mp) {
            FlightTimeAccess.reset(mp);
        }
    }

    private static final class FlightTimeAccess {
        private static java.lang.reflect.Field field;
        private static boolean failed;

        static void reset(ServerPlayer mp) {
            if (failed || mp.connection == null) return;
            try {
                if (field == null) {
                    for (String n : new String[] { "aboveGroundTickCount", "f_9737_" }) {
                        field = findField(mp.connection.getClass(), n);
                        if (field != null) break;
                    }
                    if (field == null) { failed = true; return; }
                    field.setAccessible(true);
                }
                field.setInt(mp.connection, 0);
            } catch (Throwable t) {
                failed = true;
            }
        }

        private static java.lang.reflect.Field findField(Class<?> c, String name) {
            while (c != null) {
                try {
                    return c.getDeclaredField(name);
                } catch (NoSuchFieldException e) {
                    c = c.getSuperclass();
                }
            }
            return null;
        }
    }

    @Deprecated
    public static boolean checkForHazmat(LivingEntity player) {
        if (checkArmorAny(player, ModItems.HAZMAT_HELMET, ModItems.HAZMAT_PLATE, ModItems.HAZMAT_LEGS, ModItems.HAZMAT_BOOTS)
                || checkArmorAny(player, ModItems.HAZMAT_HELMET, ModItems.HAZMAT_PLATE, ModItems.HAZMAT_LEGS, ModItems.HAZMAT_BOOTS)
                || checkArmorAny(player, ModItems.HAZMAT_HELMET_RED, ModItems.HAZMAT_PLATE_RED, ModItems.HAZMAT_LEGS_RED, ModItems.HAZMAT_BOOTS_RED)
                || checkArmorAny(player, ModItems.HAZMAT_HELMET_GREY, ModItems.HAZMAT_PLATE_GREY, ModItems.HAZMAT_LEGS_GREY, ModItems.HAZMAT_BOOTS_GREY)
                || checkArmorAny(player, ModItems.SCHRABIDIUM_HELMET, ModItems.SCHRABIDIUM_PLATE, ModItems.SCHRABIDIUM_LEGS, ModItems.SCHRABIDIUM_BOOTS)
                || checkForHaz2(player)) {
            return true;
        }
        return player.hasEffect(com.hbm_m.effect.ModEffects.MUTATION.get());
    }

    @Deprecated
    public static boolean checkForHaz2(LivingEntity player) {
        return checkArmorAny(player, ModItems.HAZMAT_PAA_HELMET, ModItems.HAZMAT_PAA_PLATE, ModItems.HAZMAT_PAA_LEGS, ModItems.HAZMAT_PAA_BOOTS)
                || checkArmorAny(player, ModItems.LIQUIDATOR_HELMET, ModItems.LIQUIDATOR_PLATE, ModItems.LIQUIDATOR_LEGS, ModItems.LIQUIDATOR_BOOTS)
                || checkArmorAny(player, ModItems.LIQUIDATOR_HELMET, ModItems.LIQUIDATOR_PLATE, ModItems.LIQUIDATOR_LEGS, ModItems.LIQUIDATOR_BOOTS)
                || checkArmorAny(player, ModItems.EUPHEMIUM_HELMET, ModItems.EUPHEMIUM_PLATE, ModItems.EUPHEMIUM_LEGS, ModItems.EUPHEMIUM_BOOTS)
                || checkArmorAny(player, ModItems.RPA_HELMET, ModItems.RPA_PLATE, ModItems.RPA_LEGS, ModItems.RPA_BOOTS)
                || checkArmorAny(player, ModItems.FAU_HELMET, ModItems.FAU_PLATE, ModItems.FAU_LEGS, ModItems.FAU_BOOTS)
                || checkArmorAny(player, ModItems.DNS_HELMET, ModItems.DNS_PLATE, ModItems.DNS_LEGS, ModItems.DNS_BOOTS);
    }

    public static boolean checkForAsbestos(LivingEntity player) {
        return checkArmorAny(player, ModItems.ASBESTOS_HELMET, ModItems.ASBESTOS_PLATE, ModItems.ASBESTOS_LEGS, ModItems.ASBESTOS_BOOTS)
                || checkArmorAny(player, ModItems.ASBESTOS_HELMET, ModItems.ASBESTOS_PLATE, ModItems.ASBESTOS_LEGS, ModItems.ASBESTOS_BOOTS);
    }

    public static boolean checkForDigamma(Player player) {
        if (checkArmorAny(player, ModItems.FAU_HELMET, ModItems.FAU_PLATE, ModItems.FAU_LEGS, ModItems.FAU_BOOTS)) return true;
        if (checkArmorAny(player, ModItems.DNS_HELMET, ModItems.DNS_PLATE, ModItems.DNS_LEGS, ModItems.DNS_BOOTS)) return true;
        return player.hasEffect(com.hbm_m.effect.ModEffects.STABILITY.get());
    }

    public static boolean checkForDigamma2(Player player) {
        if (!checkArmorAny(player, ModItems.ROBES_HELMET, ModItems.ROBES_PLATE, ModItems.ROBES_LEGS, ModItems.ROBES_BOOTS)) return false;
        if (!player.hasEffect(com.hbm_m.effect.ModEffects.STABILITY.get())) return false;
        for (ItemStack armor : player.getInventory().armor) {
            if (!armor.isEmpty() && ArmorModificationHelper.hasMods(armor)) {
                ItemStack[] mods = ArmorModificationHelper.pryMods(armor);
                ItemStack cl = mods[ArmorModificationHelper.cladding];
                boolean iron = cl != null && !cl.isEmpty() && BuiltInRegistries.ITEM.getKey(cl.getItem()).getPath().equals("cladding_iron");
                if (!iron) return false;
            }
        }
        return player.getMaxHealth() < 3;
    }

    public static boolean checkForFaraday(Player player) {
        for (ItemStack armor : player.getInventory().armor) {
            if (armor.isEmpty() || !isFaradayArmor(armor)) return false;
        }
        return true;
    }

    public static final String[] metals = new String[] {
            "chainmail", "iron", "silver", "gold", "platinum", "tin", "lead", "liquidator", "schrabidium",
            "euphemium", "steel", "cmb", "titanium", "alloy", "copper", "bronze", "electrum", "t45", "t51",
            "bj", "starmetal",
            "hazmat", // also count because rubber is insulating
            "rubber", "hev", "ajr", "rpa", "spacesuit"
    };

    public static boolean isFaradayArmor(ItemStack item) {
        String name = BuiltInRegistries.ITEM.getKey(item.getItem()).getPath().toLowerCase(Locale.US);
        for (String metal : metals) if (name.contains(metal)) return true;
        return HazmatRegistry.getCladding(item) > 0;
    }

    public static boolean checkForFiend(Player player) {
        return checkArmorPiece(player, ModItems.JACKT, EquipmentSlot.CHEST) && player.getMainHandItem().is(ModItems.SHIMMER_SLEDGE.get());
    }

    public static boolean checkForFiend2(Player player) {
        return checkArmorPiece(player, ModItems.JACKT2, EquipmentSlot.CHEST) && player.getMainHandItem().is(ModItems.SHIMMER_AXE.get());
    }

    /**
     * Износ фильтра на надетой маске (или на маске, прицепленной к шлему).
     * Порт {@link com.hbm.util.ArmorUtil#damageGasMaskFilter} (1.7.10).
     */
    public static void damageGasMaskFilter(LivingEntity entity, int damage) {
        if (damage <= 0) {
            return;
        }
        ItemStack mask = GasMaskUtil.resolveWornMask(entity);
        if (!mask.isEmpty() && mask.getItem() instanceof IGasMask) {
            IGasMask.damageFilter(mask, damage);
        }
    }

    /** Есть ли на сущности маска (надетая, прицепленная к шлему или в слоте лица Curios). */
    public static boolean isWearingMask(LivingEntity entity) {
        return !GasMaskUtil.resolveWornMask(entity).isEmpty();
    }
}
