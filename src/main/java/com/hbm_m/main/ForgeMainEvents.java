//? if forge {
package com.hbm_m.main;

import java.util.Map;

import com.hbm_m.capability.ChunkRadiationProvider;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;

public final class ForgeMainEvents {

    /** Umbenannte Port-IDs, siehe {@link LegacyIds}. */
    private static final Map<String, String> LEGACY_IDS = LegacyIds.MAP;

    @SubscribeEvent
    public void onAttachCapabilitiesChunk(AttachCapabilitiesEvent<LevelChunk> event) {
        final ResourceLocation key = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "chunk_radiation");

        if (!event.getCapabilities().containsKey(key)) {
            ChunkRadiationProvider provider = new ChunkRadiationProvider();
            event.addCapability(key, provider);
            event.addListener(provider.getCapability(ChunkRadiationProvider.CHUNK_RADIATION_CAPABILITY)::invalidate);
        }
    }

    @SubscribeEvent
    public void onMissingMappings(MissingMappingsEvent event) {
        for (MissingMappingsEvent.Mapping<Block> m : event.getMappings(ForgeRegistries.Keys.BLOCKS, RefStrings.MODID)) {
            String target = LEGACY_IDS.get(m.getKey().getPath());
            if (target != null) m.remap(BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, target)));
        }
        for (MissingMappingsEvent.Mapping<Item> m : event.getMappings(ForgeRegistries.Keys.ITEMS, RefStrings.MODID)) {
            String target = LEGACY_IDS.get(m.getKey().getPath());
            if (target != null) m.remap(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, target)));
        }
    }
}
//?}
