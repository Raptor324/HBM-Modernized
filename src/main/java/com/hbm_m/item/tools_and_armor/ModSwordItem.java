package com.hbm_m.item.tools_and_armor;

import com.hbm_m.item.ITooltipProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Кросс-версионная обёртка над {@link SwordItem}.
 *
 * <p>На 1.20.1 конструктор {@code SwordItem(tier, damage, speed, props)} доступен напрямую.
 * На 1.21.1 этот конструктор удалён — вместо него атрибуты собираются через
 * {@code SwordItem.createAttributes(tier, damage, speed)} и передаются через
 * {@code properties.attributes(...)}.</p>
 *
 * <p>Аналог {@link ModAxeItem} / {@link ModPickaxeItem} / {@link ModShovelItem}.
 * Реализует {@link ITooltipProvider} для единообразия с остальными HBM-инструментами
 * (пока без кастомных тултипов — заглушка).</p>
 *
 * <p>Критический API gap (MC 1.20.1 ↔ 1.21.1):  —
 * в {@code Hbm-s-Nuclear-Tech-GIT} (1.7.10) единый конструктор {@code ItemSword(ToolMaterial)}.
 * Здесь сохранён 4-аргументный конструктор 1.20.1 как общий API.</p>
 */
public class ModSwordItem extends SwordItem implements ITooltipProvider {

    public ModSwordItem(Tier tier, int attackDamage, float attackSpeed, Properties properties) {
        //? if < 1.21.1 {
        super(tier, attackDamage, attackSpeed, properties);
        //?} else {
        /*super(tier, properties.attributes(SwordItem.createAttributes(tier, attackDamage, attackSpeed)));
        *///?}
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        // Базовый меч — без специальных тултипов (заглушка ITooltipProvider).
    }
}
