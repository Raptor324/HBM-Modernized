package com.hbm_m.network;

import java.util.List;
import java.util.function.Supplier;

import com.hbm_m.client.ClientRenderHandler;

// Пакет для подсветки блоков на клиенте.
// Отправляется с сервера на клиент для выделения определённых блоков (при проверке структуры).

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

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

    /**
     * Пакет для добавления/удаления осиротевших фантомных блоков из постоянной подсветки.
     */
    public static class OrphanedPhantomsPacket {
        private final List<BlockPos> addPositions;
        private final List<BlockPos> removePositions;

        public OrphanedPhantomsPacket(List<BlockPos> addPositions, List<BlockPos> removePositions) {
            this.addPositions = addPositions;
            this.removePositions = removePositions;
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeCollection(addPositions, FriendlyByteBuf::writeBlockPos);
            buf.writeCollection(removePositions, FriendlyByteBuf::writeBlockPos);
        }

        public static OrphanedPhantomsPacket fromBytes(FriendlyByteBuf buf) {
            return new OrphanedPhantomsPacket(
                buf.readCollection(java.util.ArrayList::new, FriendlyByteBuf::readBlockPos),
                buf.readCollection(java.util.ArrayList::new, FriendlyByteBuf::readBlockPos)
            );
        }

        public static void handle(OrphanedPhantomsPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                for (BlockPos pos : msg.addPositions) {
                    ClientRenderHandler.addOrphanedPhantomBlock(pos);
                }
                for (BlockPos pos : msg.removePositions) {
                    ClientRenderHandler.removeOrphanedPhantomBlock(pos);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}