package com.hbm_m.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Простой предмет со статичным lore-тултипом. Реализует {@link ITooltipProvider}, поэтому
 * тултип добавляется централизованно через {@code ClientTooltipEvent.ITEM} без
 * версионно-зависимого переопределения {@code appendHoverText}.
 *
 * <p>Замена анонимным классам {@code new Item(props) { @Override appendHoverText(...) }}
 * в {@code ModItems}, которые не могут реализовать интерфейс.
 */
public class LoreTooltipItem extends Item implements ITooltipProvider {

    private final List<Component> lines;

    public LoreTooltipItem(List<Component> lines, Properties properties) {
        super(properties);
        this.lines = List.copyOf(lines);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.addAll(lines);
    }
}
