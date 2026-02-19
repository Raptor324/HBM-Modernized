package com.hbm_m.client.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * Tooltip component for crate contents preview rows.
 */
@OnlyIn(Dist.CLIENT)
public class CrateContentsTooltipComponent implements TooltipComponent {
    private final List<Entry> entries;

    public CrateContentsTooltipComponent(List<Entry> entries) {
        this.entries = entries;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public record Entry(ItemStack stack, int totalCount) {}
}
