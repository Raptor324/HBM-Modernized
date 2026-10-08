package com.hbm_m.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.platform.PlatformHooks;

/**
 * 1:1 {@code com.hbm.items.tool.ItemKeyPin}: Grundklasse fuer Schluessel ({@code ItemKey}) und Vorhaengeschloesser
 * ({@code ItemLock}); die Stiftkonfiguration steht im NBT unter {@code "pins"} (0 = nicht gesetzt). Abgeglichen wird sie
 * in {@link com.hbm_m.api.tile.LockState}, geschnitten/kopiert in der Schluesselschmiede.
 */
public class ItemKeyPin extends Item implements com.hbm_m.item.ITooltipProvider {

    public ItemKeyPin(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        if (getPins(stack) != 0)
            list.add(Component.literal("Pin configuration: " + getPins(stack)));
        else
            list.add(Component.literal("Pins not set!"));

        if (this == ModItems.KEY_FAKE.get()) {
            list.add(Component.literal(""));
            list.add(Component.literal("Pins can neither be changed, nor copied."));
        }
    }

    public static int getPins(ItemStack stack) {
        return PlatformHooks.contains(stack, "pins") ? PlatformHooks.getInt(stack, "pins") : 0;
    }

    public static void setPins(ItemStack stack, int i) {
        PlatformHooks.putInt(stack, "pins", i);
    }

    public boolean canTransfer() {
        return this != ModItems.KEY_FAKE.get();
    }

    /** Hilfe fuer Slots/Schmiede: ein Schluessel/Schloss, dessen Stifte uebertragen werden duerfen. */
    public static boolean isTransferable(ItemStack stack) {
        return stack.getItem() instanceof ItemKeyPin pin && pin.canTransfer();
    }

    /** @deprecated alter Port-Name, gleich {@link #getPins}. */
    @Deprecated
    public static int getCode(ItemStack stack) { return getPins(stack); }

    /** @deprecated alter Port-Name, gleich {@link #setPins}. */
    @Deprecated
    public static void setCode(ItemStack stack, int code) { setPins(stack, code); }
}
