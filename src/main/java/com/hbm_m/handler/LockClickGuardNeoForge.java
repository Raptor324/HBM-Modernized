//? if neoforge {
/*package com.hbm_m.handler;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

// NeoForge-подписчик гарда запертых контейнеров (см. LockClickGuard).
@EventBusSubscriber
public final class LockClickGuardNeoForge {
    private LockClickGuardNeoForge() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (LockClickGuard.shouldDenyOpen(event.getEntity(), event.getLevel(), event.getPos())) {
            // Глушим только открытие блока; useItem (key_kit/padlock) продолжает работать
            event.setUseBlock(net.neoforged.neoforge.common.util.TriState.FALSE);
        }
    }
}
*///?}
