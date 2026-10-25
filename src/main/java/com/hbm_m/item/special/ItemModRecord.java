package com.hbm_m.item.special;

import java.util.function.Supplier;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
//? if < 1.21.1 {
import net.minecraft.world.item.RecordItem;
//?}

/**
 * 1:1 {@code com.hbm.items.special.ItemModRecord} ({@code record_lc/ss/vc/glass}): Schallplatte mit dem Namen der
 * Vanilla-Platte ("Musikplatte"), Beschreibung {@code item.hbm_m.record_<name>.desc}, selten.
 */
//? if < 1.21.1 {
public class ItemModRecord extends RecordItem {
//?} else {
/*public class ItemModRecord extends net.minecraft.world.item.Item {
*///?}

    public ItemModRecord(int comparatorValue, Supplier<SoundEvent> sound, Properties properties, int lengthInTicks) {
        //? if < 1.21.1 {
        super(comparatorValue, sound, properties.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE), lengthInTicks);
        //?} else {
        /*// 1.21.1: Musik, Laenge und Komparatorwert stehen in data/hbm_m/jukebox_song/<id>.json
        super(properties.stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE));
        com.hbm_m.platform.ItemComponentHooks.deferJukeboxSong(this);
        *///?}
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.minecraft.music_disc_11");
    }
}
