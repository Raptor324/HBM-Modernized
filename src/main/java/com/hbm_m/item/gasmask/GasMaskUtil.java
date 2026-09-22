package com.hbm_m.item.gasmask;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.compat.curios.CuriosCompat;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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

    /**
     * A worn mask together with the helmet it is attached to, if any.
     *
     * <p>{@link ArmorModificationHelper#pryMods} decodes an attachment from the helmet's NBT into a
     * fresh stack, so edits to that mask are thrown away unless they are written back with
     * {@code applyMod} - which is what upstream {@code ItemFilter.onItemRightClick} does.</p>
     */
    public record WornMask(ItemStack mask, ItemStack helmet) {

        public static final WornMask NONE = new WornMask(ItemStack.EMPTY, ItemStack.EMPTY);

        /** Writes an attached mask back into its helmet. No-op when the mask is the worn item itself. */
        public void commit() {
            if (!helmet.isEmpty() && !mask.isEmpty()) {
                ArmorModificationHelper.applyMod(helmet, mask);
            }
        }
    }

    /**
     * Находит надетую маску на сущности: шлем-маска ИЛИ маска-модификация
     * в шлеме, ИЛИ маска в слоте лица Curios (опциональная интеграция).
     */
    public static ItemStack resolveWornMask(LivingEntity entity) {
        return resolveWornMaskRef(entity).mask();
    }

    /** As {@link #resolveWornMask}, but keeps the helmet so the mask can be written back after edits. */
    public static WornMask resolveWornMaskRef(LivingEntity entity) {
        if (entity == null) {
            return WornMask.NONE;
        }
        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack mask = resolveMask(helmet);
        if (!mask.isEmpty()) {
            return new WornMask(mask, mask == helmet ? ItemStack.EMPTY : helmet);
        }
        // A Curios stack handler hands out the live stack, so it needs no write-back.
        return new WornMask(CuriosCompat.getFaceMask(entity), ItemStack.EMPTY);
    }

    /**
     * Есть ли на голове (шлем-маска или маска-модификация) —
     * для взаимной блокировки со слотом лица Curios.
     */
    public static boolean isMaskOnHead(@Nullable LivingEntity entity) {
        return entity != null && !resolveMask(entity.getItemBySlot(EquipmentSlot.HEAD)).isEmpty();
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
