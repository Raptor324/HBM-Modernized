package com.hbm_m.item.machine;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code ItemCassette} ({@code siren_track}): je Original-Meta (TrackType) ein eigener Gegenstand {@code cassette_<titel>}.
 * Zweite Ebene {@code cassette_overlay} wird mit der Trackfarbe eingefaerbt (ClientSetup.onRegisterItemColors).
 */
public class ItemCassette extends Item {

    public final TrackType track;

    public ItemCassette(TrackType track, Properties props) {
        super(props.stacksTo(1));
        this.track = track;
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
    *///?}
        list.add(Component.literal("Siren sound cassette:"));
        list.add(Component.literal("   Name: " + track.title));
        list.add(Component.literal("   Type: " + track.type.name()));
        list.add(Component.literal("   Volume: " + track.volume));
    }

    /** Original {@code ItemCassette.getType(stack)}. */
    public static TrackType getType(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemCassette c ? c.track : TrackType.NULL;
    }

    /** Original {@code getColorFromItemStack}: Ebene 1 in Trackfarbe. */
    public static int getColor(ItemStack stack, int tintIndex) {
        return tintIndex == 1 ? getType(stack).color : 0xFFFFFF;
    }
}
