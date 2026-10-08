//? if forge {
package com.hbm_m.main;

import com.hbm_m.network.PermaSyncMemePacket;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Original {@code ModEventHandler.onPlayerTick} (Phase START, Server): jeden Tick {@code PermaSyncPacket} an den
 * Spieler; hier nur der Abschnitt "SHITTY MEMES" ({@link PermaSyncMemePacket}), die uebrigen Abschnitte laufen im
 * Port ueber eigene Pakete (ImpactSyncPacket, ExtPropPacket).
 */
@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID)
public final class PermaSyncMemeEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (event.player.level().isClientSide) return;
        if (event.player instanceof ServerPlayer player) PermaSyncMemePacket.sendTo(player);
    }

    private PermaSyncMemeEvents() {}
}
//?}
