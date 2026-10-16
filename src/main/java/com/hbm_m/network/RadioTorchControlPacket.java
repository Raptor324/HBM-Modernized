package com.hbm_m.network;

import com.hbm_m.blockentity.network.radio.IRadioTorchConfigurable;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Generic C2S "save settings" packet for the whole radio-torch family, matching the original's
 * single shared {@code NBTControlPacket} used by every RTTY GUI - one packet class instead of one
 * per block type, since {@link RadioTorchBaseBlockEntity#receiveControl(CompoundTag)} dispatches by
 * tag key just like the original's {@code receiveControl}.
 */
public class RadioTorchControlPacket implements C2SPacket {

    private final BlockPos pos;
    private final CompoundTag data;

    public RadioTorchControlPacket(BlockPos pos, CompoundTag data) {
        this.pos = pos;
        this.data = data != null ? data : new CompoundTag();
    }

    public static RadioTorchControlPacket decode(FriendlyByteBuf buf) {
        return new RadioTorchControlPacket(buf.readBlockPos(), buf.readNbt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeNbt(data);
    }

    public static void handle(RadioTorchControlPacket packet, PacketContext context) {
        context.queue(() -> {
            if (!(context.getPlayer() instanceof ServerPlayer player)) return;
            var be = player.level().getBlockEntity(packet.pos);
            if (be instanceof IRadioTorchConfigurable torch) {
                torch.receiveControl(packet.data);
            }
        });
    }

    public static void sendToServer(BlockPos pos, CompoundTag data) {
        ModPacketHandler.sendToServer(ModPacketHandler.RADIO_TORCH_CONTROL, new RadioTorchControlPacket(pos, data));
    }
}
