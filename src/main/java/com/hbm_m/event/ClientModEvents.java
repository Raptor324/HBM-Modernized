package com.hbm_m.event;

import com.hbm_m.lib.RefStrings;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        
        if (stack.isEmpty() || stack.getItem() instanceof ArmorItem) {
            return;
        }
        
        // --- Логика для опасностей ---
        HazardTooltipHandler.appendHazardTooltips(event.getItemStack(), event.getToolTip());

        // --- Логика для OreDict тегов ---
        boolean hasTags = event.getItemStack().getTags().findAny().isPresent();
        if (hasTags) {
            if (Screen.hasShiftDown()) {
                event.getToolTip().add(Component.empty());
                event.getToolTip().add(Component.translatable("tooltip.hbm_m.tags").withStyle(ChatFormatting.GRAY));
                event.getItemStack().getTags()
                    .map(TagKey::location)
                    .sorted(ResourceLocation::compareTo)
                    .forEach(location -> {
                        event.getToolTip().add(
                            Component.literal("  - " + location.toString())
                                     .withStyle(ChatFormatting.DARK_GRAY)
                        );
                    });
            } else {
                event.getToolTip().add(
                    Component.translatable("tooltip.hbm_m.hold_shift_for_details")
                             .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)
                );
            }
        }
    }
}