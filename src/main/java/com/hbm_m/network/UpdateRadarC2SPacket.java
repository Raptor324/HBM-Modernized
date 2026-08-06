package com.hbm_m.network;

import com.hbm_m.blockentity.machines.MachineRadarBlockEntity;
import com.hbm_m.inventory.menu.MachineRadarMenu;
import com.hbm_m.inventory.menu.MachineRadarSlotsMenu;

import dev.architectury.networking.NetworkManager.PacketContext;
import dev.architectury.registry.menu.MenuRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

/**
 * C2S-пакет управления радаром (порт GUIMachineRadarNT.receiveControl + NBTControlPacket).
 *
 * Три формы:
 *   1. Toggle кнопок: {pos, buttonId} — переключает scanMissiles/scanShells/scanPlayers/
 *      smart/red/showMap, очистка карты, либо открытие sub-GUI (ACTION_OPEN_SLOTS/OPEN_MAIN).
 *   2. Команда пуска по сущности: {pos, hasLaunch, LAUNCH_AT_ENTITY, linkSlot, targetId}.
 *   3. Команда пуска по координатам: {pos, hasLaunch, LAUNCH_AT_COORDS, linkSlot, x, z}.
 *
 * {@code linkSlot} — индекс линк-слота 0..7 (клавиши 1-8 → слоты 0-7), из которого
 * читается radar linker на пусковую установку (порт receiveControl["link"]).
 */
public class UpdateRadarC2SPacket implements C2SPacket {

    private final BlockPos pos;
    private final int buttonId;

    private final boolean hasLaunch;
    private final int launchAction;
    private final int linkSlot;
    private final int targetId;
    private final int targetX;
    private final int targetZ;

    /** Toggle кнопки / открытие GUI. */
    public UpdateRadarC2SPacket(BlockPos pos, int buttonId) {
        this.pos = pos;
        this.buttonId = buttonId;
        this.hasLaunch = false;
        this.launchAction = -1;
        this.linkSlot = 0;
        this.targetId = -1;
        this.targetX = 0;
        this.targetZ = 0;
    }

    /** Команда пуска по сущности-цели (порт launchEntity + link). */
    public static UpdateRadarC2SPacket launchAtEntity(BlockPos pos, int linkSlot, int targetEntityId) {
        return new UpdateRadarC2SPacket(pos, true,
                MachineRadarBlockEntity.ACTION_LAUNCH_AT_ENTITY, linkSlot, targetEntityId, 0, 0);
    }

    /** Команда пуска по координатам (порт launchPosX/Z + link). */
    public static UpdateRadarC2SPacket launchAtCoords(BlockPos pos, int linkSlot, int targetX, int targetZ) {
        return new UpdateRadarC2SPacket(pos, true,
                MachineRadarBlockEntity.ACTION_LAUNCH_AT_COORDS, linkSlot, -1, targetX, targetZ);
    }

    /** Открыть GUI слотов радара (порт переключения GUIMachineRadarNT → GUIMachineRadarNTSlots). */
    public static UpdateRadarC2SPacket openSlots(BlockPos pos) {
        return new UpdateRadarC2SPacket(pos, MachineRadarBlockEntity.ACTION_OPEN_SLOTS);
    }

    /** Вернуться из GUI слотов в главный экран (порт GUIMachineRadarNTSlots → GUIMachineRadarNT). */
    public static UpdateRadarC2SPacket openMain(BlockPos pos) {
        return new UpdateRadarC2SPacket(pos, MachineRadarBlockEntity.ACTION_OPEN_MAIN);
    }

    private UpdateRadarC2SPacket(BlockPos pos, boolean hasLaunch, int launchAction, int linkSlot,
                                 int targetId, int targetX, int targetZ) {
        this.pos = pos;
        this.buttonId = -1;
        this.hasLaunch = hasLaunch;
        this.launchAction = launchAction;
        this.linkSlot = linkSlot;
        this.targetId = targetId;
        this.targetX = targetX;
        this.targetZ = targetZ;
    }

    public static UpdateRadarC2SPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        boolean hasLaunch = buf.readBoolean();
        if (hasLaunch) {
            int action = buf.readInt();
            int linkSlot = buf.readInt();
            int targetId = buf.readInt();
            int targetX = buf.readInt();
            int targetZ = buf.readInt();
            return new UpdateRadarC2SPacket(pos, true, action, linkSlot, targetId, targetX, targetZ);
        }
        return new UpdateRadarC2SPacket(pos, buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(hasLaunch);
        if (hasLaunch) {
            buf.writeInt(launchAction);
            buf.writeInt(linkSlot);
            buf.writeInt(targetId);
            buf.writeInt(targetX);
            buf.writeInt(targetZ);
        } else {
            buf.writeInt(buttonId);
        }
    }

    public static void handle(UpdateRadarC2SPacket packet, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) {
                return;
            }

            var blockEntity = player.level().getBlockEntity(packet.pos);
            if (!(blockEntity instanceof MachineRadarBlockEntity radar)) {
                return;
            }

            if (packet.hasLaunch) {
                radar.handleLaunchCommand(packet.linkSlot, packet.launchAction,
                        packet.targetId, packet.targetX, packet.targetZ);
            } else if (packet.buttonId == MachineRadarBlockEntity.ACTION_OPEN_SLOTS) {
                MenuRegistry.openExtendedMenu(player,
                        new SimpleMenuProvider(
                                (containerId, playerInventory, p) ->
                                        new MachineRadarSlotsMenu(containerId, playerInventory, radar),
                                radar.getDisplayName()),
                        buf -> buf.writeBlockPos(packet.pos));
            } else if (packet.buttonId == MachineRadarBlockEntity.ACTION_OPEN_MAIN) {
                MenuRegistry.openExtendedMenu(player,
                        new SimpleMenuProvider(
                                (containerId, playerInventory, p) ->
                                        new MachineRadarMenu(containerId, playerInventory, radar),
                                radar.getDisplayName()),
                        buf -> buf.writeBlockPos(packet.pos));
            } else {
                radar.handleButtonPress(packet.buttonId);
            }
        });
    }
}
