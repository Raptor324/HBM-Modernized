package com.hbm_m.network;

// Основной обработчик сетевых пакетов мода HBM-Modernized.
// Регистрирует все сетевые пакеты, используемые модом, и обеспечивает их правильную сериализацию и обработку.
// Первый аргумент id - уникальный идентификатор пакета.
// Второй аргумент - класс пакета.
// Третий аргумент - метод для сериализации (записи) данных пакета в буфер.
// Четвертый аргумент - метод для десериализации (чтения) данных пакета из буфера.
// Пятый аргумент - метод для обработки пакета на принимающей стороне.

import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.sounds.GeigerSoundPacket;
import com.hbm_m.network.sounds.RequestAssemblerStateC2SPacket;
import com.hbm_m.network.sounds.StartAssemblerSoundS2CPacket;
import com.hbm_m.network.sounds.StopAssemblerSoundS2CPacket;

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
        INSTANCE.registerMessage(id++, ChunkRadiationDebugBatchPacket.class, ChunkRadiationDebugBatchPacket::encode, ChunkRadiationDebugBatchPacket::decode, ChunkRadiationDebugBatchPacket::handle);
        INSTANCE.registerMessage(id++, GiveTemplateC2SPacket.class, GiveTemplateC2SPacket::encode, GiveTemplateC2SPacket::decode, GiveTemplateC2SPacket::handle);
        
        INSTANCE.registerMessage(id++, StartAssemblerSoundS2CPacket.class, StartAssemblerSoundS2CPacket::toBytes, StartAssemblerSoundS2CPacket::new, (msg, ctx) -> msg.handle(ctx));
        INSTANCE.registerMessage(id++, StopAssemblerSoundS2CPacket.class, StopAssemblerSoundS2CPacket::toBytes, StopAssemblerSoundS2CPacket::new, (msg, ctx) -> msg.handle(ctx));
        INSTANCE.registerMessage(id++, RequestAssemblerStateC2SPacket.class,RequestAssemblerStateC2SPacket::toBytes,RequestAssemblerStateC2SPacket::new,(msg, ctx) -> msg.handle(ctx));
        INSTANCE.registerMessage(id++, UpdateBatteryC2SPacket.class, UpdateBatteryC2SPacket::toBytes, UpdateBatteryC2SPacket::new, (msg, ctx) -> msg.handle(ctx));
        INSTANCE.registerMessage(id++, HighlightBlocksPacket.class, HighlightBlocksPacket::toBytes, HighlightBlocksPacket::fromBytes, HighlightBlocksPacket::handle);
    }
}