package com.hbm_m.item.custom.industrial;

import net.minecraft.world.item.Item;

/**
 * Базовый класс для апгрейдов машин.
 * Поддерживает различные типы апгрейдов: скорость, энергия, afterburner, overdrive.
 */
public class ItemMachineUpgrade extends Item {

    public enum UpgradeType {
        SPEED,      // Увеличивает скорость, увеличивает потребление энергии
        POWER,      // Уменьшает потребление энергии, немного снижает скорость
        AFTERBURN,  // Добавляет эффекты урона/огня
        OVERDRIVE   // Экстремальный режим (может сломать машину)
    }

    private final UpgradeType type;

    public ItemMachineUpgrade(Properties properties, UpgradeType type) {
        super(properties);
        this.type = type;
    }

    public UpgradeType getUpgradeType() {
        return this.type;
    }
}
