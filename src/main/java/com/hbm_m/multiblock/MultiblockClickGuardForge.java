//? if forge {
package com.hbm_m.multiblock;

import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge-only гард «мгновенного GUI после установки мультиблока».
 *
 * <p>Ваниль повторяет пакет UseItemOn каждые 4 тика, пока зажата ПКМ. Из-за
 * placement-offset свежая структура сразу занимает точку прицела, и повторный
 * пакет того же клика открывает GUI. Здесь мы глушим такие клики в коротком
 * окне после установки (см. MultiblockStructureHelper#markRecentlyPlaced).
 */
@Mod.EventBusSubscriber
public final class MultiblockClickGuardForge {
    private MultiblockClickGuardForge() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (MultiblockStructureHelper.isRecentlyPlacedInteraction(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
//?}
