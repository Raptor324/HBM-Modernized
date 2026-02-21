package com.hbm_m.network;

// Основной обработчик сетевых пакетов мода HBM-Modernized.
// Регистрирует все сетевые пакеты, используемые модом, и обеспечивает их правильную сериализацию и обработку.
// Первый аргумент id - уникальный идентификатор пакета.
// Второй аргумент - класс пакета.
// Третий аргумент - метод для сериализации (записи) данных пакета в буфер.
// Четвертый аргумент - метод для десериализации (чтения) данных пакета из буфера.
// Пятый аргумент - метод для обработки пакета на принимающей стороне.

import com.hbm_m.lib.RefStrings;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModPacketHandler {

    private static final String PROTOCOL_VERSION = "1";
    private static boolean REGISTERED = false;

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "main_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        if (REGISTERED) {
            return;
        }
        REGISTERED = true;

        int id = 0;

        INSTANCE.registerMessage(id++, HighlightBlocksPacket.class, HighlightBlocksPacket::toBytes, HighlightBlocksPacket::fromBytes, HighlightBlocksPacket::handle);

        INSTANCE.registerMessage(id++,
            ServerboundDoorModelPacket.class,
            ServerboundDoorModelPacket::encode,
            ServerboundDoorModelPacket::decode,
            ServerboundDoorModelPacket::handle
        );
    }
}