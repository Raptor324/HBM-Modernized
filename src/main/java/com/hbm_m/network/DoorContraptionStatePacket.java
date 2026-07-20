package com.hbm_m.network;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * S2C-пакет: открыть/закрыть HBM-дверь на Create-контрапшене.
 *
 * <p>Замена blockstate-flip'у (который триггерил {@code resetClientContraption} и
 * сносил клиент-BE/звук). Серверный {@code HbmDoorMovementBehaviour.doToggle}
 * обновляет серверный кэш + шлёт этот пакет; клиентский хендлер populate кэш
 * (ContraptionWorld + VirtualRenderWorld) + invalidateColliders.
 *
 * <p>Поля: entityId контрапшен-сущности, controllerLocalPos, doorDeclId, facing, open.
 * Никаких blockstate-изменений → ноль resetClientContraption.
 */
public class DoorContraptionStatePacket implements S2CPacket {

    private static final String CLIENT_HANDLER = "com.hbm_m.client.compat.create.DoorContraptionClientApplier";

    private final int entityId;
    private final long controllerLocalPos;
    private final String doorDeclId;
    private final byte facingIndex;
    private final boolean open;

    public DoorContraptionStatePacket(int entityId, BlockPos controllerLocalPos, String doorDeclId, Direction facing, boolean open) {
        this.entityId = entityId;
        this.controllerLocalPos = controllerLocalPos.asLong();
        this.doorDeclId = doorDeclId;
        this.facingIndex = (byte) facing.get3DDataValue();
        this.open = open;
    }

    private DoorContraptionStatePacket(int entityId, long controllerLocalPos, String doorDeclId, byte facingIndex, boolean open) {
        this.entityId = entityId;
        this.controllerLocalPos = controllerLocalPos;
        this.doorDeclId = doorDeclId;
        this.facingIndex = facingIndex;
        this.open = open;
    }

    public static DoorContraptionStatePacket decode(FriendlyByteBuf buf) {
        return new DoorContraptionStatePacket(
                buf.readInt(), buf.readLong(), buf.readUtf(64), buf.readByte(), buf.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeLong(controllerLocalPos);
        buf.writeUtf(doorDeclId, 64);
        buf.writeByte(facingIndex);
        buf.writeBoolean(open);
    }

    public static void handle(DoorContraptionStatePacket msg, PacketContext context) {
        // Делегируем в клиент-only класс reflectively, чтобы этот класс не ссылался
        // на клиентские типы (Minecraft/VirtualRenderWorld) и грузился на сервере.
        context.queue(() -> {
            try {
                Class<?> cls = Class.forName(CLIENT_HANDLER);
                cls.getMethod("apply", int.class, long.class, String.class, byte.class, boolean.class)
                        .invoke(null, msg.entityId, msg.controllerLocalPos, msg.doorDeclId, msg.facingIndex, msg.open);
            } catch (Throwable t) {
                com.hbm_m.main.MainRegistry.LOGGER.debug("[HBM/Create] door contraption packet client handler unavailable: {}", t.toString());
            }
        });
    }

    public static void sendToNear(ServerLevel level, Vec3 pos, int entityId, BlockPos controllerLocalPos,
                                  String doorDeclId, Direction facing, boolean open) {
        ModPacketHandler.sendToPlayersNear(level, pos, 64.0,
                ModPacketHandler.DOOR_CONTRAPTION_STATE,
                new DoorContraptionStatePacket(entityId, controllerLocalPos, doorDeclId, facing, open));
    }
}
