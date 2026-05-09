//? if neoforge {
/*package com.hbm_m.main;

import com.hbm_m.capability.ChunkRadiationProvider;
import com.hbm_m.lib.RefStrings;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AttachCapabilitiesEvent;

public final class NeoForgeMainEvents {
    @SubscribeEvent
    public void onAttachCapabilitiesChunk(AttachCapabilitiesEvent<LevelChunk> event) {
        final ResourceLocation key = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "chunk_radiation");

        if (!event.getCapabilities().containsKey(key)) {
            ChunkRadiationProvider provider = new ChunkRadiationProvider();
            event.addCapability(key, provider);
            event.addListener(provider.getCapability(ChunkRadiationProvider.CHUNK_RADIATION_CAPABILITY)::invalidate);
        }
    }
}
*///?}
