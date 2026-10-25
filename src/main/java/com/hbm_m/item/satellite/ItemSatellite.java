package com.hbm_m.item.satellite;

import java.util.List;

import com.hbm_m.item.ISatChip;
import com.hbm_m.item.ITooltipProvider;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.items.special.ItemSatellite}: die Satelliten fuer die Soyuz ({@code satellite}, Meta = EnumSatType;
 * im Port je ein Gegenstand {@code satellite_<typ>}). Tooltip nur die Frequenz.
 */
public class ItemSatellite extends Item implements ISatChip, ITooltipProvider {

    /** Original EnumSatType in Meta-Reihenfolge. */
    public static final String[] TYPES = {
            "spy", "scanner", "radar", "miner_astro", "miner_lunar", "precision_laser",
            "death_ray", "xenium_resonator", "relay", "detector", "ray_scan"
    };

    public ItemSatellite(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.translatable("satchip.frequency").append(": " + getFreq(stack)));
    }
}
