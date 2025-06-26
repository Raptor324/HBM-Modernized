package com.hbm_m.network;

import com.hbm_m.lib.RefStrings;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModPacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "main_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(id++, GeigerSoundPacket.class, GeigerSoundPacket::encode, GeigerSoundPacket::decode, GeigerSoundPacket::handle);
        INSTANCE.registerMessage(id++, RadiationDataPacket.class, RadiationDataPacket::encode, RadiationDataPacket::decode, RadiationDataPacket::handle);
        INSTANCE.registerMessage(id++, ChunkRadiationDebugPacket.class, ChunkRadiationDebugPacket::encode, ChunkRadiationDebugPacket::decode, ChunkRadiationDebugPacket::handle);
    }
}