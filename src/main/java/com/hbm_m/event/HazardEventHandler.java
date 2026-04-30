package com.hbm_m.event;

import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.hazard.HazardType;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


/**
 * Этот класс обрабатывает события уровня, проверяя выброшенные предметы на наличие опасных свойств.
 * Если предмет обладает гидрореактивностью и находится в воде, он взрывается.
 */
public class HazardEventHandler {

    /**
     * Регистрация обработчика события.
     * Вызывается один раз при инициализации мода.
     */
    public static void init() {
        TickEvent.SERVER_LEVEL_POST.register(HazardEventHandler::onLevelTick);
    }

    private static void onLevelTick(ServerLevel level) {
        // Перебираем все сущности, загруженные в мире.
        // ВАЖНО: Мы создаем копию списка entity-итератора, чтобы избежать ConcurrentModificationException
        // при удалении сущности (item.discard()) во время итерации.
        for (Entity entity : level.getAllEntities()) {
            // Нас интересуют только выброшенные предметы (ItemEntity)
            if (entity instanceof ItemEntity itemEntity) {
                // Предотвращаем двойную обработку, если предмет уже помечен на удаление
                if (itemEntity.isRemoved()) {
                    continue;
                }

                ItemStack stack = itemEntity.getItem();
                if (stack.isEmpty()) {
                    continue;
                }

                // Флаг, который показывает, что предмет был уничтожен и дальнейшая обработка не нужна.
                boolean itemDestroyed = false;

                // 1. Проверка на гидрореактивность
                float hydroStrength = HazardSystem.getHazardLevelFromStack(stack, HazardType.HYDRO_REACTIVE);
                if (hydroStrength > 0 && itemEntity.isInWaterOrRain()) {
                    // Уничтожаем предмет и создаем взрыв
                    itemEntity.discard();
                    level.explode(
                            itemEntity,
                            itemEntity.getX(),
                            itemEntity.getY(),
                            itemEntity.getZ(),
                            hydroStrength,
                            Level.ExplosionInteraction.TNT
                    );
                    itemDestroyed = true;
                }

                // 2. Проверка на взрыв в огне
                if (!itemDestroyed) {
                    float explosiveStrength = HazardSystem.getHazardLevelFromStack(stack, HazardType.EXPLOSIVE_ON_FIRE);
                    if (explosiveStrength > 0 && itemEntity.isOnFire()) {
                        itemEntity.discard();
                        level.explode(
                                itemEntity,
                                itemEntity.getX(),
                                itemEntity.getY(),
                                itemEntity.getZ(),
                                explosiveStrength,
                                Level.ExplosionInteraction.TNT
                        );
                        itemDestroyed = true;
                    }
                }

                // 3. Проверка на излучение радиации 
                if (!itemDestroyed) {
                    float radiationLevel = HazardSystem.getHazardLevelFromStack(stack, HazardType.RADIATION);
                    if (radiationLevel > 0) {
                        // Здесь должна быть логика излучения.
                        // TODO: Позже реализую утилитарный класс типа ContaminationUtil, как в оригинальном HBM.

                        // ContaminationUtil.radiate(level, itemEntity.blockPosition(), 32, radiationLevel / 10f * stack.getCount());

                        // Временный плейсхолдер, чтобы показать, что логика работает:
                        // MainRegistry.LOGGER.debug("Item " + stack.getDisplayName().getString() + " is radiating at " + radiationLevel + " RAD/s");
                    }
                }
            }
        }
    }
}