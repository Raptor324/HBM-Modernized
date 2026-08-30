package com.hbm_m.compat.curios;

import dev.architectury.platform.Platform;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Точка входа опциональной интеграции с Curios (слот лица для противогазов).
 * Прямых импортов классов Curios здесь нет — они только в {@link CuriosAccess},
 * который грузится строго после проверки {@link #isLoaded()}.
 */
public final class CuriosCompat {

    private static Boolean cachedLoaded;

    private CuriosCompat() {
    }

    public static boolean isLoaded() {
        if (cachedLoaded == null) {
            boolean present = false;
            try {
                present = Platform.isModLoaded("curios");
            } catch (Throwable ignored) {
            }
            cachedLoaded = present;
        }
        return cachedLoaded;
    }

    /**
     * Маска в слоте лица Curios ("mask", первый слот) или EMPTY.
     * Безопасно вызывать всегда — без Curios вернёт EMPTY.
     */
    public static ItemStack getFaceMask(LivingEntity entity) {
        if (entity == null || !isLoaded()) {
            return ItemStack.EMPTY;
        }
        return CuriosAccess.getFaceMask(entity);
    }
}
