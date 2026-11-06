package com.hbm_m.network;

import com.hbm_m.blockentity.machines.MachineWoodBurnerBlockEntity;
import com.hbm_m.network.C2SPacket;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ToggleWoodBurnerPacket implements C2SPacket {

    private final BlockPos pos;
    /** Целевое состояние горелки: true = включена (GUI-кнопка оригинального toggle). */
    private final boolean state;

    public ToggleWoodBurnerPacket(BlockPos pos, boolean state) {
        this.pos = pos;
        this.state = state;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static ToggleWoodBurnerPacket decode(FriendlyByteBuf buf) {
        return new ToggleWoodBurnerPacket(buf.readBlockPos(), buf.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(state);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(ToggleWoodBurnerPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            ServerLevel level = player.serverLevel();
            BlockEntity be    = ModPacketHandler.blockEntityAt(player, msg.pos);

            if (be instanceof MachineWoodBurnerBlockEntity woodBurner) {
                woodBurner.setEnabled(msg.state);
                woodBurner.setChanged();
            }
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendToServer(BlockPos pos, boolean state) {
        ModPacketHandler.sendToServer(ModPacketHandler.TOGGLE_WOOD_BURNER,
                new ToggleWoodBurnerPacket(pos, state));
    }
}
