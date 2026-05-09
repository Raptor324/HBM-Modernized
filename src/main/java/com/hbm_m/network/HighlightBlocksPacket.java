package com.hbm_m.network;

import java.util.List;

import com.hbm_m.client.ClientRenderHandler;
import com.hbm_m.network.S2CPacket;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class HighlightBlocksPacket implements S2CPacket {

    private final List<BlockPos> positions;

    public HighlightBlocksPacket(List<BlockPos> positions) {
        this.positions = positions;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static HighlightBlocksPacket fromBytes(FriendlyByteBuf buf) {
        return new HighlightBlocksPacket(
                buf.readCollection(java.util.ArrayList::new, FriendlyByteBuf::readBlockPos));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeCollection(positions, FriendlyByteBuf::writeBlockPos);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(HighlightBlocksPacket msg, PacketContext context) {
        context.queue(() -> ClientRenderHandler.highlightBlocks(msg.positions));
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendTo(ServerPlayer player, List<BlockPos> positions) {
        ModPacketHandler.sendToPlayer(player, ModPacketHandler.HIGHLIGHT_BLOCKS,
                new HighlightBlocksPacket(positions));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Вложенный пакет: OrphanedPhantomsPacket
    // ══════════════════════════════════════════════════════════════════════════

    public static class OrphanedPhantomsPacket implements S2CPacket {

        private final List<BlockPos> addPositions;
        private final List<BlockPos> removePositions;

        public OrphanedPhantomsPacket(List<BlockPos> addPositions, List<BlockPos> removePositions) {
            this.addPositions    = addPositions;
            this.removePositions = removePositions;
        }

        // ── Serialization ─────────────────────────────────────────────────────

        public static OrphanedPhantomsPacket fromBytes(FriendlyByteBuf buf) {
            return new OrphanedPhantomsPacket(
                    buf.readCollection(java.util.ArrayList::new, FriendlyByteBuf::readBlockPos),
                    buf.readCollection(java.util.ArrayList::new, FriendlyByteBuf::readBlockPos)
            );
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeCollection(addPositions,    FriendlyByteBuf::writeBlockPos);
            buf.writeCollection(removePositions, FriendlyByteBuf::writeBlockPos);
        }

        // ── Handler ───────────────────────────────────────────────────────────

        public static void handle(OrphanedPhantomsPacket msg, PacketContext context) {
            context.queue(() -> {
                for (BlockPos pos : msg.addPositions)    ClientRenderHandler.addOrphanedPhantomBlock(pos);
                for (BlockPos pos : msg.removePositions) ClientRenderHandler.removeOrphanedPhantomBlock(pos);
            });
        }

        // ── Send helper ───────────────────────────────────────────────────────

        public static void sendTo(ServerPlayer player, List<BlockPos> add, List<BlockPos> remove) {
            // Нужно зарегистрировать ID в ModPacketHandler, например:
            // public static final ResourceLocation ORPHANED_PHANTOMS = id("orphaned_phantoms");
            ModPacketHandler.sendToPlayer(player, ModPacketHandler.ORPHANED_PHANTOMS,
                    new OrphanedPhantomsPacket(add, remove));
        }
    }
}