package com.hbm_m.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod.EventBusSubscriber(modid = "hbm_m", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModNetwork {

    public static final String PROTOCOL_VERSION = "1";
    public static SimpleChannel CHANNEL = null;

    // Инициализировать канал нужно в setup события, а не в статическом блоке!
    public static void registerChannels() {
        if (CHANNEL != null) {
            return; // Уже инициализирован
        }

        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation("hbm_m", "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );

        int packetId = 0;

        // Регистрируем пакет DetonateAllPacket
        CHANNEL.messageBuilder(DetonateAllPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(DetonateAllPacket::decode)
                .encoder(DetonateAllPacket::encode)
                .consumerMainThread(DetonateAllPacket::handle)
                .add();

        CHANNEL.messageBuilder(SetActivePointPacket.class, packetId++)
                .decoder(SetActivePointPacket::decode)
                .encoder(SetActivePointPacket::encode)
                .consumerMainThread(SetActivePointPacket::handle)
                .add();

        ModNetwork.CHANNEL.messageBuilder(ClearPointPacket.class, packetId++)
                .decoder(ClearPointPacket::decode)
                .encoder(ClearPointPacket::encode)
                .consumerMainThread(ClearPointPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncPointPacket.class, packetId++)
                .decoder(SyncPointPacket::decode)
                .encoder(SyncPointPacket::encode)
                .consumerMainThread(SyncPointPacket::handle)
                .add();

    }
}