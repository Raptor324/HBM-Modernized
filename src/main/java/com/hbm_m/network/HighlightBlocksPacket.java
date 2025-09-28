package com.hbm_m.network;

// Пакет для подсветки блоков на клиенте.
// Отправляется с сервера на клиент для выделения определённых блоков (при проверке структуры).

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

import com.hbm_m.client.ClientRenderHandler;

public class HighlightBlocksPacket {

    private final List<BlockPos> positions;

    public HighlightBlocksPacket(List<BlockPos> positions) {
        this.positions = positions;
    }

    // Метод для записи данных (энкодер)
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeCollection(positions, FriendlyByteBuf::writeBlockPos);
    }

    // Метод для чтения данных (декодер)
    public static HighlightBlocksPacket fromBytes(FriendlyByteBuf buf) {

        return new HighlightBlocksPacket(buf.readCollection(java.util.ArrayList::new, FriendlyByteBuf::readBlockPos));
    }

    // Статический метод для обработки пакета (обработчик)
    public static void handle(HighlightBlocksPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Просто вызываем метод без длительности
            ClientRenderHandler.highlightBlocks(msg.positions);
        });
        ctx.get().setPacketHandled(true);
    }
}