package com.hbm_m.network;

import com.hbm_m.block.entity.doors.DoorBlockEntity;
import com.hbm_m.client.model.variant.DoorModelSelection;
import com.hbm_m.client.model.variant.DoorModelType;
import com.hbm_m.client.model.variant.DoorSkin;
import com.hbm_m.main.MainRegistry;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ServerboundDoorModelPacket implements C2SPacket {

    private final BlockPos           blockPos;
    private final DoorModelSelection selection;

    public ServerboundDoorModelPacket(BlockPos blockPos, DoorModelSelection selection) {
        this.blockPos  = blockPos;
        this.selection = selection;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static ServerboundDoorModelPacket decode(FriendlyByteBuf buf) {
        BlockPos       blockPos = buf.readBlockPos();
        String         typeId   = buf.readUtf(16);
        String         skinId   = buf.readUtf(64);

        DoorModelType type = DoorModelType.fromId(typeId);
        DoorSkin      skin = DoorSkin.of(skinId);

        return new ServerboundDoorModelPacket(blockPos, new DoorModelSelection(type, skin));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
        // Должно совпадать с readUtf(16/64), иначе декодер на другой стороне уезжает по буферу.
        buf.writeUtf(selection.getModelType().getId(), 16);
        buf.writeUtf(selection.getSkin().getId(), 64);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(ServerboundDoorModelPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            ServerLevel level = player.serverLevel();

            double distanceSq = player.distanceToSqr(
                    msg.blockPos.getX() + 0.5,
                    msg.blockPos.getY() + 0.5,
                    msg.blockPos.getZ() + 0.5
            );

            if (distanceSq > 64.0) {
                MainRegistry.LOGGER.debug("Player too far from door: {}", msg.blockPos);
                return;
            }

            BlockEntity be = level.getBlockEntity(msg.blockPos);
            if (!(be instanceof DoorBlockEntity doorEntity) || !doorEntity.isController()) return;

            doorEntity.setModelSelection(msg.selection);

            MainRegistry.LOGGER.debug("Door model changed: {} -> {} at {}",
                    doorEntity.getDoorDeclId(),
                    msg.selection.getDisplayName(doorEntity.getDoorDeclId()).getString(),
                    msg.blockPos);
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void send(BlockPos blockPos, DoorModelSelection selection) {
        ModPacketHandler.sendToServer(ModPacketHandler.DOOR_MODEL,
                new ServerboundDoorModelPacket(blockPos, selection));
        MainRegistry.LOGGER.debug("Sending door model packet: {} -> {}", blockPos, selection);
    }
}