//? if forge {
package com.hbm_m.client;

import com.hbm_m.multiblock.MultiblockBlockItem;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge-only подписчик: при удержании предмета мультиблока отменяет ванильный
 * контур цели и рисует рамку футпринта (порт drawPlacementHighlight из 1.7.10).
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class MultiblockPlacementHighlightForge {
    private MultiblockPlacementHighlightForge() {}

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
//?}
