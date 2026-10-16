//? if neoforge {
/*package com.hbm_m.multiblock;

import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/^*
 * NeoForge-only гард «мгновенного GUI после установки мультиблока».
 *
 * <p>Зеркалирует Forge-вариант (MultiblockClickGuardForge): ваниль повторяет пакет
 * UseItemOn каждые 4 тика, пока зажата ПКМ, и из-за placement-offset свежая
 * структура сразу занимает точку прицела — повторный пакет того же клика
 * открывает GUI. Глушим такие клики в коротком окне после установки.
 ^/
@EventBusSubscriber
public final class MultiblockClickGuardNeoForge {
    private MultiblockClickGuardNeoForge() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (MultiblockStructureHelper.isRecentlyPlacedInteraction(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
*///?}
