package com.hbm_m.interfaces;

import java.util.Map;

import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Конфигурация для рендеринга OBJ-брони.
 * Каждый тип брони должен реализовать этот интерфейс для предоставления своей конфигурации.
 */
public interface IArmorLayerConfig {

    /** Кость гуманоидной модели, к которой привязывается часть OBJ. */
    enum Bone { HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG }

    /** Часть OBJ-модели и кость, на которой она рисуется. */
    record PartBinding(String part, Bone bone) {}

    /**
     * Части модели, рисуемые в данном слоте, с привязкой к костям.
     * Дефолт повторяет историческое поведение силовой брони (t51 и т.п.).
     */
    default java.util.List<PartBinding> getPartsForSlot(net.minecraft.world.entity.EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> java.util.List.of(new PartBinding("Helmet", Bone.HEAD));
            case CHEST -> java.util.List.of(
                    new PartBinding("Chest", Bone.BODY),
                    new PartBinding("RightArm", Bone.RIGHT_ARM),
                    new PartBinding("LeftArm", Bone.LEFT_ARM));
            case LEGS -> java.util.List.of(
                    new PartBinding("RightLeg", Bone.RIGHT_LEG),
                    new PartBinding("LeftLeg", Bone.LEFT_LEG));
            case FEET -> java.util.List.of(
                    new PartBinding("RightBoot", Bone.RIGHT_LEG),
                    new PartBinding("LeftBoot", Bone.LEFT_LEG));
            default -> java.util.List.of();
        };
    }

    /**
     * Условный рендер части (например, баллон фильтра противогаза рисуется,
     * только если фильтр установлен).
     */
    default boolean shouldRenderPart(String partName, ItemStack stack, net.minecraft.world.entity.EquipmentSlot slot) {
        return true;
    }

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

