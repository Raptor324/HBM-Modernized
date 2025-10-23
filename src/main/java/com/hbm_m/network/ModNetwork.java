package com.hbm_m.network;

import com.hbm_m.lib.RefStrings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        CHANNEL.messageBuilder(SpawnAlwaysVisibleParticlePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SpawnAlwaysVisibleParticlePacket::encode)
                .decoder(SpawnAlwaysVisibleParticlePacket::new)
                .consumerMainThread(SpawnAlwaysVisibleParticlePacket::handle)
                .add();
    }

    // Утилитный метод для отправки пакета всем игрокам в радиусе
    public static void sendToAllNearby(SpawnAlwaysVisibleParticlePacket packet,
                                       net.minecraft.server.level.ServerLevel level,
                                       double x, double y, double z,
                                       double radius) {
        CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        x, y, z, radius, level.dimension()
                )),
                packet
        );
    }
}