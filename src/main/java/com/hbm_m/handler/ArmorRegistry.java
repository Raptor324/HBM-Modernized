package com.hbm_m.handler;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.item.gasmask.IGasMask;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Реестр защиты предметов по классам опасностей.
 * Порт {@link com.hbm.util.ArmorRegistry} (1.7.10): защита лёгких идёт не от самой маски,
 * а от установленного фильтра; прицепленные модификации (attachment_mask) проверяются рекурсивно.
 */
public final class ArmorRegistry {

    private static final Map<Item, EnumSet<HazardClass>> PROTECTION = new HashMap<>();

    private ArmorRegistry() {
    }

    public static void register(@NotNull Item item, @NotNull HazardClass... classes) {
        EnumSet<HazardClass> set = PROTECTION.computeIfAbsent(item, k -> EnumSet.noneOf(HazardClass.class));
        for (HazardClass clazz : classes) {
            set.add(clazz);
        }
    }

    public static EnumSet<HazardClass> getProtection(Item item) {
        return PROTECTION.getOrDefault(item, EnumSet.noneOf(HazardClass.class));
    }

    /**
     * Защищает ли предмет (броня, маска, фильтр или модификация) от класса опасности.
     * Рекурсивно проверяет фильтр в маске и прицепленные модификации.
     */
    public static boolean getProtectionFromItem(ItemStack stack, HazardClass clazz) {
        if (stack.isEmpty()) {
            return false;
        }

        EnumSet<HazardClass> own = getProtection(stack.getItem());
        boolean has = own.contains(clazz);

        if (stack.getItem() instanceof IGasMask mask) {
            // Чёрный список маски ветирует даже фильтр (например, mono не защищает от газов).
            if (mask.getBlacklist().contains(clazz)) {
                return false;
            }
            if (!has && IGasMask.hasFilter(stack)) {
                Item filter = IGasMask.getFilterItem(IGasMask.getFilterId(stack));
                if (filter != null && getProtection(filter).contains(clazz)) {
                    has = true;
                }
            }
        }

        if (has) {
            return true;
        }

        // Модификации брони (attachment_mask в шлеме и т.п.)
        for (ItemStack mod : ArmorModificationHelper.pryMods(stack)) {
            if (!mod.isEmpty() && getProtectionFromItem(mod, clazz)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Проверка защиты слота брони. {@code slot} — индекс инвентаря брони как в 1.7.10:
     * 0 = ботинки, 1 = штаны, 2 = нагрудник, 3 = шлем.
     */
    public static boolean hasProtection(LivingEntity entity, int slot, HazardClass clazz) {
        EquipmentSlot eq = switch (slot) {
            case 0 -> EquipmentSlot.FEET;
            case 1 -> EquipmentSlot.LEGS;
            case 2 -> EquipmentSlot.CHEST;
            case 3 -> EquipmentSlot.HEAD;
            default -> null;
        };
        if (eq == null) {
            return false;
        }
        return getProtectionFromItem(entity.getItemBySlot(eq), clazz);
    }
}
