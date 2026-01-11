package com.hbm_m.client.model;

import com.hbm_m.powerarmor.ModPowerArmorItem;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/**
 * Конфигурация для рендеринга иконок брони в GUI.
 * Каждый сет брони должен реализовать этот интерфейс для предоставления своей конфигурации.
 * Аналогично IArmorLayerConfig для entity рендеринга.
 */
public interface IArmorModelConfig {
    /**
     * Уникальный идентификатор сета брони (например, "t51", "hev", "rpa").
     * Используется для идентификации и логирования.
     */
    String getArmorSetId();

    /**
     * Порядок частей модели.
     * Используется для определения порядка рендеринга и для getPartNames().
     */
    String[] getPartOrder();

    /**
     * Определяет, какие части модели нужно рендерить для данного типа брони.
     * @param armorType Тип брони (HELMET, CHESTPLATE, LEGGINGS, BOOTS)
     * @return Массив имен частей модели, которые нужно рендерить
     */
    String[] getPartsForType(ArmorItem.Type armorType);

    /**
     * Класс предмета брони для проверки в ItemOverrides.
     * @return Класс, который должен быть экземпляром ModPowerArmorItem
     */
    Class<? extends ModPowerArmorItem> getArmorItemClass();

    /**
     * ModelResourceLocation для базовой модели данного сета брони.
     * Используется для загрузки модели из JSON.
     */
    ModelResourceLocation getBaseModelLocation();

    /**
     * Проверяет, подходит ли данный ItemStack для этого сета брони.
     * @param stack ItemStack для проверки
     * @return true, если предмет является броней данного сета
     */
    default boolean isItemValid(ItemStack stack) {
        return stack.getItem() instanceof ModPowerArmorItem;
    }
}



