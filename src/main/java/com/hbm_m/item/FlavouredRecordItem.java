package com.hbm_m.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if < 1.21.1 {
import net.minecraft.world.item.RecordItem;
//?}
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A music disc that carries an extra flavour line under the usual artist/title line.
 *
 * <p>Vanilla's {@code RecordItem#appendHoverText} adds exactly one line - the {@code .desc}
 * translation, in grey - and there is no way to get a second line out of a single translation key.
 * This subclass reproduces that first line itself rather than calling {@code super}: the method's
 * signature differs between 1.20.1 and 1.21.1+, so a {@code super} call would need its own
 * Stonecutter branch inside the body.</p>
 *
 * <p>1.21.1 dropped {@code RecordItem} entirely - jukebox playback became a data component - so
 * there the class is a plain {@link Item} and the disc's sound is wired up through the item's
 * jukebox_playable component instead of the constructor.</p>
 */
//? if < 1.21.1 {
public class FlavouredRecordItem extends RecordItem {
//?} else {
/*public class FlavouredRecordItem extends Item {
*///?}

    private final String flavourKey;

    //? if < 1.21.1 {
    public FlavouredRecordItem(int comparatorValue, SoundEvent sound, Properties properties,
                               int lengthInTicks, String flavourKey) {
        super(comparatorValue, sound, properties, lengthInTicks);
        this.flavourKey = flavourKey;
    }
    //?} else {
    /*public FlavouredRecordItem(int comparatorValue, SoundEvent sound, Properties properties,
                               int lengthInTicks, String flavourKey) {
        super(properties);
        this.flavourKey = flavourKey;
    }
    *///?}

    //? if < 1.21.1 {
    // @Override omitted intentionally - Stonecutter removes this block for >= 1.21.1
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        // Line 1: what vanilla RecordItem draws - the ".desc" key, grey.
        list.add(this.getDisplayName().withStyle(ChatFormatting.GRAY));
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
        // RecordItem is gone; reproduce its ".desc" line from the item's own description id.
        list.add(Component.translatable(this.getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    *///?}
        // Line 2: the disc's own flavour text.
        list.add(Component.translatable(flavourKey).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
