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
}
