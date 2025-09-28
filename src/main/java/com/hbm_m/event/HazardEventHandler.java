package com.hbm_m.event;

// Этот класс обрабатывает события уровня, проверяя выброшенные предметы на наличие опасных свойств.
// Если предмет обладает гидрореактивностью и находится в воде, он взрывается
import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.hazard.HazardType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


@Mod.EventBusSubscriber
public class HazardEventHandler {

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        // Нас интересует только логика на сервере и только в конце тика, чтобы избежать рассинхрона.
        if (event.side.isClient() || event.phase != TickEvent.Phase.END) {
            return;
        }

        ServerLevel level = (ServerLevel) event.level;

        // Перебираем все сущности, загруженные в мире.
        // ВАЖНО: Мы создаем копию списка entity-итератора, чтобы избежать ConcurrentModificationException
        // при удалении сущности (item.kill()) во время итерации.
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
                    itemEntity.discard(); // Используем discard вместо kill/remove для правильной обработки
                    level.explode(itemEntity, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), hydroStrength, Level.ExplosionInteraction.TNT);
                    itemDestroyed = true;
                }

                // 2. Проверка на взрыв в огне 
                if (!itemDestroyed) {
                    float explosiveStrength = HazardSystem.getHazardLevelFromStack(stack, HazardType.EXPLOSIVE_ON_FIRE);
                    if (explosiveStrength > 0 && itemEntity.isOnFire()) {
                        itemEntity.discard();
                        level.explode(itemEntity, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), explosiveStrength, Level.ExplosionInteraction.TNT);
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
                        // System.out.println("Item " + stack.getDisplayName().getString() + " is radiating at " + radiationLevel + " RAD/s");
                    }
                }
            }
        }
    }
}