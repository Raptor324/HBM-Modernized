package com.hbm_m.network;

import com.hbm_m.blockentity.network.MachineCraneBoxerBlockEntity;
import com.hbm_m.blockentity.network.MachineCraneExtractorBlockEntity;
import com.hbm_m.blockentity.network.MachineCraneGrabberBlockEntity;
import com.hbm_m.blockentity.network.MachineCraneInserterBlockEntity;
import com.hbm_m.blockentity.network.MachineCraneRouterBlockEntity;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Port of the crane GUI control buttons (1.7.10 Original sent these through its own packet as well).
 * The screens used to call the mutators straight on the client copy of the block entity, so nothing
 * reached the server and the setting reverted on the next sync.
 */
public class CraneControlPacket implements C2SPacket {

    public static final byte FILTER_MODE = 0;
    public static final byte TOGGLE_WHITELIST = 1;
    public static final byte TOGGLE_MAX_EJECT = 2;
    public static final byte BOXER_MODE = 3;
    public static final byte INSERTER_DESTROYER = 4;
    public static final byte ROUTER_TARGET_MODE = 5;

    private final BlockPos pos;
    private final byte action;
    private final int index;

    public CraneControlPacket(BlockPos pos, byte action, int index) {
        this.pos = pos;
        this.action = action;
        this.index = index;
    }

    public static CraneControlPacket decode(FriendlyByteBuf buf) {
        return new CraneControlPacket(buf.readBlockPos(), buf.readByte(), buf.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(action);
        buf.writeVarInt(index);
    }

    public static void handle(CraneControlPacket msg, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;

            BlockEntity be = ModPacketHandler.blockEntityAt(player, msg.pos);
            if (be == null) return;

            switch (msg.action) {
                case FILTER_MODE -> {
                    if (be instanceof MachineCraneExtractorBlockEntity extractor) {
                        if (inRange(msg.index, 9)) extractor.nextMode(msg.index);
                    } else if (be instanceof MachineCraneGrabberBlockEntity grabber) {
                        if (inRange(msg.index, 9)) grabber.nextMode(msg.index);
                    } else if (be instanceof MachineCraneRouterBlockEntity router) {
                        if (inRange(msg.index, MachineCraneRouterBlockEntity.INVENTORY_SIZE)) router.nextFilterMode(msg.index);
                    }
                }
                case TOGGLE_WHITELIST -> {
                    if (be instanceof MachineCraneExtractorBlockEntity extractor) extractor.toggleWhitelist();
                    else if (be instanceof MachineCraneGrabberBlockEntity grabber) grabber.toggleWhitelist();
                }
                case TOGGLE_MAX_EJECT -> {
                    if (be instanceof MachineCraneExtractorBlockEntity extractor) extractor.toggleMaxEject();
                }
                case BOXER_MODE -> {
                    if (be instanceof MachineCraneBoxerBlockEntity boxer) boxer.nextMode();
                }
                case INSERTER_DESTROYER -> {
                    if (be instanceof MachineCraneInserterBlockEntity inserter) inserter.toggleDestroyer();
                }
                case ROUTER_TARGET_MODE -> {
                    if (be instanceof MachineCraneRouterBlockEntity router && inRange(msg.index, 6)) router.nextTargetMode(msg.index);
                }
                default -> { }
            }
        });
    }

    /** The index comes from the client, so it can point anywhere. */
    private static boolean inRange(int index, int size) {
        return index >= 0 && index < size;
    }

    public static void sendToServer(BlockPos pos, byte action, int index) {
        ModPacketHandler.sendToServer(ModPacketHandler.CRANE_CONTROL, new CraneControlPacket(pos, action, index));
    }
}
