package com.hbm_m.item.special;

import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.item.ITooltipProvider;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.special.ItemHotDusted} ({@code ingot_steel_dusted}, Meta 0-9 = Anzahl Faltungen). Die
 * Metas sind im Port eigene Gegenstaende ({@code steel_dusted_ingot}, {@code steel_dusted_ingot_1} ... {@code _9}).
 * Das statische {@code getMaxHeat} des Originals verdeckt nur und wird nie aufgerufen - die Hitze ist die von ItemHot.
 */
public class ItemHotDusted extends ItemHot implements ITooltipProvider {

    public final int forged;

    public ItemHotDusted(int heat, int forged, ResourceLocation hotTexture, Properties properties) {
        super(heat, hotTexture, properties);
        this.forged = forged;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.translatable("item.hot_dusted.forged", forged));
    }
}
