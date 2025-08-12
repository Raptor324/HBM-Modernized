package com.hbm_m.network.sounds;

import com.hbm_m.sound.ClientSoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// S2C - Команда от Сервера Клиенту "Остановить звук ассемблера"
public class StopAssemblerSoundS2CPacket {

    private final BlockPos pos;

    public StopAssemblerSoundS2CPacket(BlockPos pos) {
        this.pos = pos;
    }

    public StopAssemblerSoundS2CPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() ->
            // Выполняем код только на клиенте, вызывая наш новый менеджер
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSoundManager.stopAssemblerSound(this.pos))
        );
        context.setPacketHandled(true); 
        return true;
    }
}