package com.hbm_m.network.sounds;

import com.hbm_m.block.entity.MachineAssemblerBlockEntity;
import com.hbm_m.network.ModPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

// C2S - Клиент запрашивает у Сервера состояние звука
public class RequestAssemblerStateC2SPacket {

    private final BlockPos pos;

    public RequestAssemblerStateC2SPacket(BlockPos pos) {
        this.pos = pos;
    }

    public RequestAssemblerStateC2SPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Этот код выполняется на сервере
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            ServerLevel level = player.serverLevel();
            // Проверяем, что позиция валидна и чанк загружен
            if (level.isLoaded(this.pos)) {
                BlockEntity be = level.getBlockEntity(this.pos);
                if (be instanceof MachineAssemblerBlockEntity machine) {
                    // Если машина действительно крафтит, отправляем обратно пакет СТАРТ
                    if (machine.isCrafting()) {
                        ModPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new StartAssemblerSoundS2CPacket(this.pos));
                    }
                }
            }
        });
        context.setPacketHandled(true); 
        return true;
    }
}