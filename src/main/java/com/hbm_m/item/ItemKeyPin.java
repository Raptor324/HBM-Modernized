package com.hbm_m.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.platform.PlatformHooks;

/**
 * Port of {@code ItemKeyPin} (1.7.10 Original) - a key blank/cut key carrying a numeric pin-code in
 * NBT. Duplicated or randomized by {@code MachineKeyforgeBlockEntity}.
 * <p>
 * SCOPE-Vereinfachung: Das Original-Schloss-System ({@code TileEntityLockableBase} und Verwandte,
 * die den Pin-Code tatsaechlich zum Ver-/Entriegeln abgleichen) existiert in diesem Port nicht -
 * dieses Item traegt den Code getreu dem Original, aber nichts liest ihn ausserhalb der Keyforge
 * selbst aus.
 */
public class ItemKeyPin extends Item implements com.hbm_m.item.ITooltipProvider {

    public ItemKeyPin(Properties properties) {
        super(properties);
    }

    public static int getCode(ItemStack stack) {
        return PlatformHooks.contains(stack, "code") ? PlatformHooks.getInt(stack, "code") : -1;
    }

    public static void setCode(ItemStack stack, int code) {
        PlatformHooks.putInt(stack, "code", code);
    }

    public static boolean isTransferable(ItemStack stack) {
        return stack.getItem() instanceof ItemKeyPin;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int code = getCode(stack);
        tooltip.add(Component.literal(code >= 0 ? "Code: " + code : "Uncut")
                .withStyle(ChatFormatting.GRAY));
    }
}
