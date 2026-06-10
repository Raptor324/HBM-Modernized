package com.hbm_m.client.tooltip;


import java.util.List;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;*///?}
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

/**
 * Tooltip component for crate contents preview rows.
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)*///?}
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
