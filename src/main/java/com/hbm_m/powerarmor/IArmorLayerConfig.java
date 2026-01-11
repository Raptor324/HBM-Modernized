package com.hbm_m.powerarmor;

import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Конфигурация для рендеринга OBJ-брони.
 * Каждый тип брони должен реализовать этот интерфейс для предоставления своей конфигурации.
 */
public interface IArmorLayerConfig {
    /**
     * Уникальный идентификатор типа брони (например, "t51", "hev", "rpa").
     * Используется для изоляции кэша BASE_PIVOTS между разными типами брони.
     */
    String getArmorTypeId();

    /**
     * ModelResourceLocation для загрузки BakedModel данного типа брони.
     */
    ModelResourceLocation getBakedModelLocation();

    /**
     * Материалы (текстуры) для каждой части брони.
     * Ключ - имя части (например, "Helmet", "Chest", "RightArm").
     * Значение - Material с атласом и текстурой.
     */
    Map<String, Material> getPartMaterials();

    /**
     * Оффсеты для каждой части брони (в блоках Minecraft).
     * Используется для тонкой настройки позиционирования частей.
     * Ключ - имя части, значение - Vec3 оффсет.
     */
    Map<String, Vec3> getPartOffsets();

    /**
     * Масштаб для устранения z-fighting со скином игрока.
     * По умолчанию 1.015F (1.5% увеличение).
     */
    default float getZFightingScale() {
        return 1.015F;
    }

    /**
     * Проверяет, подходит ли данный ItemStack для этого типа брони.
     * @param stack ItemStack для проверки
     * @return true, если предмет является броней данного типа
     */
    boolean isItemValid(ItemStack stack);
}

