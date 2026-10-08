//? if neoforge {
/*package com.hbm_m.main;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.PermaSyncMemePacket;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

// NeoForge-Gegenstuecke zu ForgeMainEvents (MissingMappingsEvent -> Registry-Alias) und PermaSyncMemeEvents.
// Registrierung in NeoForgeEntrypoint. Chunk-Strahlung haengt auf NeoForge als Attachment (ModAttachments).
public final class NeoForgeMainEvents {

    private NeoForgeMainEvents() {}

    // Mod-Bus: alte Port-IDs als Alias auf die neuen IDs (Forge: MissingMappingsEvent.remap).
    public static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.BLOCK)) {
            var reg = event.getRegistry(Registries.BLOCK);
            LegacyIds.MAP.forEach((from, to) -> reg.addAlias(
                    ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, from), ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, to)));
        } else if (event.getRegistryKey().equals(Registries.ITEM)) {
            var reg = event.getRegistry(Registries.ITEM);
            LegacyIds.MAP.forEach((from, to) -> reg.addAlias(
                    ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, from), ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, to)));
        }
    }

    // Spiel-Bus: Original ModEventHandler.onPlayerTick (START, Server), Abschnitt "SHITTY MEMES".
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEntity() instanceof ServerPlayer player) PermaSyncMemePacket.sendTo(player);
    }
}
*///?}
