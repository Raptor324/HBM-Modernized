package com.hbm_m.network;

import com.hbm_m.blockentity.machines.MachineWoodBurnerBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Порт {@code NBTControlPacket} для дровяного генератора: оригинал шлёт ключ
 * {@code "toggle"} (кнопка вкл/выкл) или {@code "switch"} (смена режима
 * твёрдое/жидкое) через {@code receiveControl}.
 */
public class ToggleWoodBurnerPacket implements C2SPacket {

    private final BlockPos pos;
    /** false = "toggle" (вкл/выкл), true = "switch" (режим горения). */
    private final boolean switchMode;

    public ToggleWoodBurnerPacket(BlockPos pos, boolean switchMode) {
        this.pos = pos;
        this.switchMode = switchMode;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static ToggleWoodBurnerPacket decode(FriendlyByteBuf buf) {
        return new ToggleWoodBurnerPacket(buf.readBlockPos(), buf.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(switchMode);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(ToggleWoodBurnerPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(msg.pos);

            if (be instanceof MachineWoodBurnerBlockEntity woodBurner) {
                if (msg.switchMode) {
                    woodBurner.switchMode();
                } else {
                    woodBurner.toggleOn();
                }
            }
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendToServer(BlockPos pos, boolean switchMode) {
        ModPacketHandler.sendToServer(ModPacketHandler.TOGGLE_WOOD_BURNER,
                new ToggleWoodBurnerPacket(pos, switchMode));
    }
}
