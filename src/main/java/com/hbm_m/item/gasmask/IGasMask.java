package com.hbm_m.item.gasmask;

import java.util.EnumSet;

import com.hbm_m.handler.HazardClass;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.nbt.CompoundTag;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Предмет-противогаз, принимающий фильтр (NBT на стаке маски).
 * Порт {@link com.hbm.api.item.IGasMask} (1.7.10).
 *
 * <p>В оригинале фильтр хранился как ItemStack в NBT "hfrFilter". Здесь для кросс-версионности
 * (ItemStack IO требует HolderLookup.Provider на 1.21.1) храним id предмета фильтра
 * и его повреждение двумя отдельными полями.</p>
 */
public interface IGasMask {

    String FILTER_KEY = "hbmFilter";
    String FILTER_DMG_KEY = "hbmFilterDmg";

    /** Классы опасностей, от которых эта маска не защищает даже с фильтром. */
    EnumSet<HazardClass> getBlacklist();

    /** id предмета фильтра ("hbm_m:gas_mask_filter") или пустая строка, если фильтра нет. */
    static String getFilterId(ItemStack mask) {
        CompoundTag tag = PlatformHooks.getItemTag(mask);
        return tag == null ? "" : tag.getString(FILTER_KEY);
    }

    static int getFilterDamage(ItemStack mask) {
        CompoundTag tag = PlatformHooks.getItemTag(mask);
        return tag == null ? 0 : tag.getInt(FILTER_DMG_KEY);
    }

    static boolean hasFilter(ItemStack mask) {
        String id = getFilterId(mask);
        if (id.isEmpty()) {
            return false;
        }
        return getFilterItem(id) != null;
    }

    /** Установить фильтр (id предмета); повреждение сбрасывается. */
    static void installFilter(ItemStack mask, Item filter) {
        PlatformHooks.editItemTag(mask, tag -> {
            tag.putString(FILTER_KEY, BuiltInRegistries.ITEM.getKey(filter).toString());
            tag.putInt(FILTER_DMG_KEY, 0);
        });
    }

    static void removeFilter(ItemStack mask) {
        PlatformHooks.editItemTag(mask, tag -> {
            tag.remove(FILTER_KEY);
            tag.remove(FILTER_DMG_KEY);
        });
    }

    static void damageFilter(ItemStack mask, int amount) {
        if (amount <= 0 || !hasFilter(mask)) {
            return;
        }
        int dmg = getFilterDamage(mask) + amount;
        Item filter = getFilterItem(getFilterId(mask));
        int max = filter instanceof ItemGasMaskFilter f ? f.maxFilterDamage : ItemGasMaskFilter.DEFAULT_MAX_DAMAGE;
        if (dmg > max) {
            // Фильтр выработался — тихо удаляем (как в оригинале).
            removeFilter(mask);
        } else {
            int fd = dmg;
            PlatformHooks.editItemTag(mask, tag -> tag.putInt(FILTER_DMG_KEY, fd));
        }
    }

    static Item getFilterItem(String id) {
        try {
            ResourceLocation rl = ResourceLocation.parse(id);
            //? if < 1.21.1 {
            Item item = BuiltInRegistries.ITEM.get(rl);
            return item == null || item == net.minecraft.world.item.Items.AIR ? null : item;
            //?} else {
            /*return BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
             *///?}
        } catch (Exception e) {
            return null;
        }
    }

    static boolean isFilterApplicable(ItemStack mask, ItemStack filter) {
        return filter.getItem() instanceof ItemGasMaskFilter;
    }
}
