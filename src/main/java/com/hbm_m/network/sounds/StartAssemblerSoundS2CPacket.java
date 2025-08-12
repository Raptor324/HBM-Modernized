package com.hbm_m.network.sounds;

import com.hbm_m.block.entity.MachineAssemblerBlockEntity;
import com.hbm_m.sound.ClientSoundManager;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// S2C - Команда от Сервера Клиенту "Начать звук ассемблера"
public class StartAssemblerSoundS2CPacket {

    private final BlockPos pos;

    public StartAssemblerSoundS2CPacket(BlockPos pos) {
        this.pos = pos;
    }

    public StartAssemblerSoundS2CPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Этот код выполнится в основном потоке клиента, что безопасно
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // 1. Принудительно обновляем состояние на клиенте, чтобы избежать гонки состояний
                BlockEntity be = Minecraft.getInstance().level.getBlockEntity(this.pos);
                if (be instanceof MachineAssemblerBlockEntity machine) {
                    machine.setCrafting(true); // Новый метод, который мы добавим
                }
                // 2. Проигрываем звук
                ClientSoundManager.playAssemblerSound(this.pos);
            });
        });
        // 3. Сообщаем Forge, что мы обработали пакет. Это остановит дублирование и ошибку "Unknown identifier".
        context.setPacketHandled(true);
        return true;
    }
}