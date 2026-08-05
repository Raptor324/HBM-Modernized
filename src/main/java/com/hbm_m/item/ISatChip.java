package com.hbm_m.item;

import com.hbm_m.platform.PlatformHooks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Port of legacy {@code com.hbm.items.ISatChip}. Items implementing this can be launched
 * as a Soyuz satellite payload and/or used as a remote-control item; both share the same
 * NBT-tagged "freq" so a payload and a designator can be matched to each other.
 */
public interface ISatChip {

    static int getFreqS(ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof ISatChip chip) {
            return chip.getFreq(stack);
        }
        return 0;
    }

    static void setFreqS(ItemStack stack, int freq) {
        if (!stack.isEmpty() && stack.getItem() instanceof ISatChip chip) {
            chip.setFreq(stack, freq);
        }
    }

    default int getFreq(ItemStack stack) {
        CompoundTag tag = PlatformHooks.getItemTag(stack);
        return tag != null ? tag.getInt("freq") : 0;
    }

    default void setFreq(ItemStack stack, int freq) {
        PlatformHooks.editItemTag(stack, t -> t.putInt("freq", freq));
    }
}
