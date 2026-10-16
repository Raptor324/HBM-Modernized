//? if neoforge {
/*package com.hbm_m.client;

import com.hbm_m.multiblock.MultiblockBlockItem;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

/^*
 * NeoForge-only подписчик: при удержании предмета мультиблока отменяет ванильный
 * контур цели и рисует рамку футпринта (порт drawPlacementHighlight из 1.7.10).
 * Зеркалирует MultiblockPlacementHighlightForge.
 ^/
@EventBusSubscriber(value = Dist.CLIENT)
public final class MultiblockPlacementHighlightNeoForge {
    private MultiblockPlacementHighlightNeoForge() {}

    @SubscribeEvent
    public static void onHighlight(RenderHighlightEvent.Block event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getMainHandItem().getItem() instanceof MultiblockBlockItem item)) return;

        BlockHitResult target = event.getTarget();
        if (target.getType() == BlockHitResult.Type.MISS) return;

        event.setCanceled(true);
        MultiblockPlacementHighlight.render(mc.level, mc.player, item, target, event.getPoseStack());
    }
}
*///?}
