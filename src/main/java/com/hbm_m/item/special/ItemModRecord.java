package com.hbm_m.item.special;

import java.util.function.Supplier;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;

/**
 * 1:1 {@code com.hbm.items.special.ItemModRecord} ({@code record_lc/ss/vc/glass}): Schallplatte mit dem Namen der
 * Vanilla-Platte ("Musikplatte"), Beschreibung {@code item.hbm_m.record_<name>.desc}, selten.
 */
public class ItemModRecord extends RecordItem {

    public ItemModRecord(int comparatorValue, Supplier<SoundEvent> sound, Properties properties, int lengthInTicks) {
        super(comparatorValue, sound, properties.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE), lengthInTicks);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.minecraft.music_disc_11");
    }
}
