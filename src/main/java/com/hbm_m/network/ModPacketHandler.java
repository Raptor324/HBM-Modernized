package com.hbm_m.network;

// Основной обработчик сетевых пакетов мода HBM-Modernized.
// Регистрирует все сетевые пакеты, используемые модом, и обеспечивает их правильную сериализацию и обработку.
// Первый аргумент id - уникальный идентификатор пакета.
// Второй аргумент - класс пакета.
// Третий аргумент - метод для сериализации (записи) данных пакета в буфер.
// Четвертый аргумент - метод для десериализации (чтения) данных пакета из буфера.
// Пятый аргумент - метод для обработки пакета на принимающей стороне.

import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.packets.PowerArmorDashPacket;
import com.hbm_m.network.sounds.GeigerSoundPacket;

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

        INSTANCE.registerMessage(id++, GeigerSoundPacket.class, GeigerSoundPacket::encode, GeigerSoundPacket::decode, GeigerSoundPacket::handle);
        INSTANCE.registerMessage(id++, RadiationDataPacket.class, RadiationDataPacket::encode, RadiationDataPacket::decode, RadiationDataPacket::handle);
        INSTANCE.registerMessage(id++, ChunkRadiationDebugBatchPacket.class, ChunkRadiationDebugBatchPacket::encode, ChunkRadiationDebugBatchPacket::decode, ChunkRadiationDebugBatchPacket::handle);
        INSTANCE.registerMessage(id++, GiveTemplateC2SPacket.class, GiveTemplateC2SPacket::encode, GiveTemplateC2SPacket::decode, GiveTemplateC2SPacket::handle);

        INSTANCE.registerMessage(id++, UpdateBatteryC2SPacket.class, UpdateBatteryC2SPacket::toBytes, UpdateBatteryC2SPacket::new, (msg, ctx) -> msg.handle(ctx));
        INSTANCE.registerMessage(id++, HighlightBlocksPacket.class, HighlightBlocksPacket::toBytes, HighlightBlocksPacket::fromBytes, HighlightBlocksPacket::handle);

        INSTANCE.registerMessage(id++, SetAssemblerRecipeC2SPacket.class, SetAssemblerRecipeC2SPacket::encode, SetAssemblerRecipeC2SPacket::decode, SetAssemblerRecipeC2SPacket::handle);
        INSTANCE.registerMessage(id++, ToggleWoodBurnerPacket.class, ToggleWoodBurnerPacket::encode, ToggleWoodBurnerPacket::decode, ToggleWoodBurnerPacket::handle);

        // Multi-Detonator (C2S)
        INSTANCE.registerMessage(id++, DetonateAllPacket.class, DetonateAllPacket::encode, DetonateAllPacket::decode, DetonateAllPacket::handle);
        INSTANCE.registerMessage(id++, SetActivePointPacket.class, SetActivePointPacket::encode, SetActivePointPacket::decode, SetActivePointPacket::handle);
        INSTANCE.registerMessage(id++, ClearPointPacket.class, ClearPointPacket::encode, ClearPointPacket::decode, ClearPointPacket::handle);
        INSTANCE.registerMessage(id++, SyncPointPacket.class, SyncPointPacket::encode, SyncPointPacket::decode, SyncPointPacket::handle);

        INSTANCE.registerMessage(id++, AnvilCraftC2SPacket.class, AnvilCraftC2SPacket::encode, AnvilCraftC2SPacket::decode, AnvilCraftC2SPacket::handle);
        INSTANCE.registerMessage(id++, AnvilSelectRecipeC2SPacket.class, AnvilSelectRecipeC2SPacket::encode, AnvilSelectRecipeC2SPacket::decode, AnvilSelectRecipeC2SPacket::handle);

        INSTANCE.registerMessage(id++, PowerArmorDashPacket.class, PowerArmorDashPacket::toBytes, PowerArmorDashPacket::new, PowerArmorDashPacket::handle);
        INSTANCE.registerMessage(id++,
                com.hbm_m.network.packet.PacketSyncEnergy.class, // Если пакет лежит в другом пакете, укажи путь или сделай импорт
                com.hbm_m.network.packet.PacketSyncEnergy::encode,
                com.hbm_m.network.packet.PacketSyncEnergy::decode,
                com.hbm_m.network.packet.PacketSyncEnergy::handle
        );
        INSTANCE.registerMessage(id++,
            ServerboundDoorModelPacket.class,
            ServerboundDoorModelPacket::encode,
            ServerboundDoorModelPacket::decode,
            ServerboundDoorModelPacket::handle
        );
    }
}