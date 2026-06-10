package com.hbm_m.event;

import com.hbm_m.hazard.HazardEntry;
import com.hbm_m.hazard.HazardSystem;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class HazardTooltipHandler {

    public static void appendHazardTooltips(ItemStack stack, Player player, List<Component> tooltip) {
        if (stack.isEmpty() || player == null) {
            return;
        }

        for (HazardEntry entry : HazardSystem.getHazardsFromStack(stack)) {
            entry.type.addHazardInformation(player, tooltip, entry.baseLevel, stack, entry.mods);
        }
    }
}
