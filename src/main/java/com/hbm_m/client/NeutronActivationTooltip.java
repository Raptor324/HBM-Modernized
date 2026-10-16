package com.hbm_m.client;

import java.util.List;

import com.hbm_m.util.ContaminationUtil;

import dev.architectury.event.events.client.ClientTooltipEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1:1 port of CE's {@code ContaminationUtil.addNeutronRadInfo}: an item that has picked up induced
 * radioactivity from sitting in a neutron flux says so on its tooltip.
 *
 * <p>Items that are radioactive in their own right are skipped - their own hazard tooltip already
 * covers them, and {@link ContaminationUtil#getNeutronRads} refuses to stack activation on top of
 * them anyway.</p>
 */
public final class NeutronActivationTooltip {

    private NeutronActivationTooltip() {}

    public static void init() {
        ClientTooltipEvent.ITEM.register(NeutronActivationTooltip::onItemTooltip);
    }

    private static void onItemTooltip(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        float activationRads = ContaminationUtil.getNeutronRads(stack);
        if (activationRads <= 0) return;

        tooltip.add(Component.literal("[")
                .append(Component.translatable("trait.radioactive"))
                .append("]")
                .withStyle(ChatFormatting.GREEN));

        float perItem = activationRads / stack.getCount();
        tooltip.add(Component.literal(" " + round(perItem) + " RAD/s").withStyle(ChatFormatting.YELLOW));

        if (stack.getCount() > 1) {
            tooltip.add(Component.literal(" Stack: " + round(activationRads) + " RAD/s")
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    private static String round(float value) {
        return String.valueOf(Math.round(value * 1000F) / 1000F);
    }
}
