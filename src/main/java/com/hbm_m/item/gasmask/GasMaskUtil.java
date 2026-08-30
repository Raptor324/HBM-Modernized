package com.hbm_m.item.gasmask;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Утилиты для масок и фильтров (обёртка над {@link IGasMask}).
 */
public final class GasMaskUtil {

    private GasMaskUtil() {
    }

    /**
     * Находит маску на сущности: сам шлем-маска ИЛИ маска, прицепленная к шлему
     * как модификация (слот helmet_only, {@link com.hbm_m.armormod.util.ArmorModificationHelper}).
     */
    @Nullable
    public static ItemStack resolveMask(ItemStack helmet) {
        if (helmet.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (helmet.getItem() instanceof IGasMask) {
            return helmet;
        }
        for (ItemStack mod : ArmorModificationHelper.pryMods(helmet)) {
            if (mod.getItem() instanceof IGasMask) {
                return mod;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Вынуть фильтр из маски, вернув его предметом с остатком ресурса. */
    public static ItemStack takeFilter(ItemStack mask) {
        if (!(mask.getItem() instanceof IGasMask) || !IGasMask.hasFilter(mask)) {
            return ItemStack.EMPTY;
        }
        Item item = IGasMask.getFilterItem(IGasMask.getFilterId(mask));
        int dmg = IGasMask.getFilterDamage(mask);
        IGasMask.removeFilter(mask);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack out = new ItemStack(item);
        out.setDamageValue(Math.max(0, dmg));
        return out;
    }
}
