//? if forge {
package com.hbm_m.handler;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge-подписчик гарда запертых контейнеров (см. {@link LockClickGuard}).
 */
@Mod.EventBusSubscriber
public final class LockClickGuardForge {
    private LockClickGuardForge() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (LockClickGuard.shouldDenyOpen(event.getEntity(), event.getLevel(), event.getPos())) {
            // Глушим только открытие блока; useItem (key_kit/padlock) продолжает работать
            event.setUseBlock(Event.Result.DENY);
        }
    }
}
//?}
