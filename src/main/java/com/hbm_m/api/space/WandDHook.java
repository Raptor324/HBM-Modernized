package com.hbm_m.api.space;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Space-API: Debug-Zauberstab {@code wand_d}. NTM Space ersetzt {@code ItemWandD} vollstaendig (Master: Kran-Struktur
 * {@code crane_mod.nbt} bauen; Space: Dimensions-Teleporter + Atmosphaeren-Debugger + "Sonne zerstoeren"). Ohne Addon
 * ({@link #DEFAULT}) bleibt der HBM-/Master-Stand unveraendert.
 *
 * <p>Mit {@link #active()} == true uebernimmt das Addon {@code onItemRightClick} (serverseitig, der Client gibt wie im
 * Original sofort zurueck) und {@code addInformation}. Setzen ueber {@link SpaceApi#setWandDHook(WandDHook)}.
 */
public interface WandDHook {

    /** HBM-/Master-Verhalten. */
    WandDHook DEFAULT = new WandDHook() { };

    /** true = Space-{@code ItemWandD} ersetzt das HBM-Verhalten (Rechtsklick + Tooltip). */
    default boolean active() {
        return false;
    }

    /** Rechtsklick, nur Server und nur wenn {@link #active()} (Original Space {@code onItemRightClick}). */
    default InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand, ItemStack stack) {
        return InteractionResultHolder.pass(stack);
    }

    /** Tooltip, nur wenn {@link #active()} (Original Space {@code addInformation}). */
    default void appendTooltip(ItemStack stack, @Nullable Level level, List<Component> list) { }
}
