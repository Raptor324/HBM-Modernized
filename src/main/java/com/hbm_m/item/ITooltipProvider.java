package com.hbm_m.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Версионно-независимый провайдер тултипов для предметов.
 *
 * <p>Вместо переопределения ванильного {@code Item.appendHoverText(...)} (сигнатура
 * которого меняется между версиями MC: на 1.20.1 —
 * {@code (ItemStack, @Nullable Level, List<Component>, TooltipFlag)}, на 1.21.1+ —
 * {@code (ItemStack, Item.TooltipContext, ...)}), предмет реализует этот интерфейс.
 * Делегирование выполняется централизованно в {@code ClientModEvents} через Architectury
 * {@code ClientTooltipEvent.ITEM}, что позволяет поддерживать обе версии без stonecutter-обёрток
 * в каждом предмете.
 *
 * <p><b>Паттерн:</b>
 * <pre>{@code
 * public class MyItem extends Item implements ITooltipProvider {
 *     @Override
 *     public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
 *         tooltip.add(Component.translatable("..."));
 *     }
 * }
 * }</pre>
 */
public interface ITooltipProvider {
    void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag);
}
