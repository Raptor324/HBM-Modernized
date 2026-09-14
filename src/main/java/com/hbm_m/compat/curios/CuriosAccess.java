package com.hbm_m.compat.curios;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

/**
 * Прямые вызовы API Curios. НЕ ссылаться из общего кода напрямую —
 * только через {@link CuriosCompat} после {@link CuriosCompat#isLoaded()},
 * иначе NoClassDefFoundError без установленного Curios.
 */
final class CuriosAccess {

    private CuriosAccess() {
    }

    static ItemStack getFaceMask(LivingEntity entity) {
        //? if < 1.21.1 {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .flatMap(handler -> handler.getStacksHandler("mask"))
                .map(CuriosAccess::firstStack)
                .orElse(ItemStack.EMPTY);
        //?} else {
        /*return CuriosApi.getCuriosInventory(entity)
                .flatMap(handler -> handler.getStacksHandler("mask"))
                .map(CuriosAccess::firstStack)
                .orElse(ItemStack.EMPTY);
         *///?}
    }

    private static ItemStack firstStack(top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler handler) {
        IDynamicStackHandler stacks = handler.getStacks();
        return stacks.getSlots() > 0 ? stacks.getStackInSlot(0) : ItemStack.EMPTY;
    }

    /** Силовая броня на голове блокирует маску в слоте лица (см. GasMaskCurio.canEquip). */
    static boolean isPowerArmorHead(LivingEntity entity) {
        return entity != null && entity.getItemBySlot(EquipmentSlot.HEAD).getItem()
                instanceof com.hbm_m.powerarmor.ModArmorFSBPowered;
    }
}
