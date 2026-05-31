package com.hbm_m.event;

import com.hbm_m.hazard.HazardSystem;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Применение опасностей инвентаря игрока. Порт {@link com.hbm.hazard.HazardSystem#updatePlayerInventory} (1.7.10).
 */
public class PlayerHazardHandler {

    public static void init() {
        TickEvent.PLAYER_POST.register(PlayerHazardHandler::onPlayerTick);
    }

    private static void onPlayerTick(Player player) {
        if (player.level().isClientSide || player.isCreative() || player.isSpectator()) {
            return;
        }

        for (int i = 0; i < player.getInventory().items.size(); i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (!stack.isEmpty()) {
                HazardSystem.applyHazards(stack, player);
            }
        }

        for (ItemStack stack : player.getArmorSlots()) {
            if (!stack.isEmpty()) {
                HazardSystem.applyHazards(stack, player);
            }
        }
    }
}
