package com.hbm_m.interfaces;

import java.util.Map;

import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;

/**
 * Порт IUpgradeInfoProvider из 1.7.10.
 *
 * Реализуется BlockEntity машин, поддерживающих апгрейды.
 * {@link #getValidUpgrades()} возвращает карту допустимых типов
 * и их максимальных уровней.
 */
public interface IUpgradeInfoProvider {

    Map<UpgradeType, Integer> getValidUpgrades();

    /** If any of the automated display stuff should be applied for this upgrade. A level of 0 is used by the GUI's indicator, as opposed to the item tooltips */
    default boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return false;
    }

    default void provideInfo(UpgradeType type, int level, java.util.List<net.minecraft.network.chat.Component> info, boolean extendedInfo) { }

    static net.minecraft.network.chat.Component getStandardLabel(net.minecraft.world.level.block.Block block) {
        return net.minecraft.network.chat.Component.literal(">>> ").append(block.getName()).append(" <<<").withStyle(net.minecraft.ChatFormatting.YELLOW);
    }

    String KEY_ACID = "upgrade.acid";
    String KEY_BURN = "upgrade.burn";
    String KEY_CONSUMPTION = "upgrade.consumption";
    String KEY_COOLANT_CONSUMPTION = "upgrade.coolantConsumption";
    String KEY_DELAY = "upgrade.delay";
    String KEY_SPEED = "upgrade.speed";
    String KEY_EFFICIENCY = "upgrade.efficiency";
    String KEY_PRODUCTIVITY = "upgrade.productivity";
    String KEY_FORTUNE = "upgrade.fortune";
    String KEY_RANGE = "upgrade.range";
}
