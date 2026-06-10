package com.hbm_m.event;

import com.hbm_m.hazard.HazardEntry;
import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.hazard.modifier.HazardModifier;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Опасности выброшенных предметов (гидрореактивность, взрыв в огне). Порт логики {@link com.hbm.hazard.HazardSystem#updateHeldItem} (1.7.10).
 */
public class HazardEventHandler {

    public static void init() {
        TickEvent.SERVER_LEVEL_POST.register(HazardEventHandler::onLevelTick);
    }

    private static void onLevelTick(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof ItemEntity itemEntity) || itemEntity.isRemoved()) {
                continue;
            }

            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) {
                continue;
            }

            for (HazardEntry entry : HazardSystem.getHazardsFromStack(stack)) {
                float levelValue = HazardModifier.evalAllModifiers(stack, null, entry.baseLevel, entry.mods);
                if (levelValue > 0) {
                    entry.type.updateEntity(itemEntity, levelValue);
                }
            }
        }
    }
}
