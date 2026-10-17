package com.hbm_m.item.machine;

import com.hbm_m.item.ITooltipProvider;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Port of NEXT {@code com.hbm.items.machine.ItemPileRod}: the legacy CP-1 rods. Renders the
 * shared usage lore ({@code desc.item.pileRod}) plus the per-rod {@code <descriptionId>.desc}
 * lore. NEXT overrides {@code appendHoverText}, whose signature differs between 1.20.1 and
 * 1.21.1, so Modernized uses the version-independent {@link ITooltipProvider} hook (same
 * pattern as {@code ItemPileRodMK2}).
 */
public class ItemPileRod extends Item implements ITooltipProvider {

    private static final String SHARED_DESC = "desc.item.pileRod";

    public ItemPileRod(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        for (String line : loreLines(SHARED_DESC)) tooltip.add(Component.literal(line));
        for (String line : loreLines(this.getDescriptionId() + ".desc")) tooltip.add(Component.literal(line));
    }

    /**
     * NEXT: {@code I18nUtil.loreLines} -> {@code Language.getInstance().has(key) ?
     * resolveKeyArray(key) : EMPTY}. Modernized resolves through a translatable component and
     * splits on newlines; an untranslated key resolves to itself and maps to no lines.
     */
    private static List<String> loreLines(String key) {
        String value = Component.translatable(key).getString();
        if (key.equals(value)) return List.of();
        return Arrays.stream(value.split("\n")).filter(line -> !line.isEmpty()).toList();
    }
}
