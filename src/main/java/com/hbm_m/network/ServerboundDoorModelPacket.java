package com.hbm_m.network;

import java.util.function.Supplier;

import com.hbm_m.block.entity.custom.doors.DoorBlockEntity;
import com.hbm_m.client.model.variant.*;
import com.hbm_m.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

/**
 * Пакет для синхронизации выбора модели двери.
 * Отправляется с клиента при выборе модели/скина в UI меню.
 * 
 * @author HBM-M Team
 */
public class ServerboundDoorModelPacket {
    
    private final BlockPos blockPos;
    private final DoorModelSelection selection;
    
    public ServerboundDoorModelPacket(BlockPos blockPos, DoorModelSelection selection) {
        this.blockPos = blockPos;
        this.selection = selection;
    }
    
    /**
     * Статический метод для кодирования пакета в буфер
     */
    public static void encode(ServerboundDoorModelPacket msg, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(msg.blockPos);
        buffer.writeUtf(msg.selection.getModelType().getId());
        buffer.writeUtf(msg.selection.getSkin().getId());
    }
    
    /**
     * Статический метод для декодирования пакета из буфера
     */
    public static ServerboundDoorModelPacket decode(FriendlyByteBuf buffer) {
        BlockPos blockPos = buffer.readBlockPos();
        
        String typeId = buffer.readUtf(16);
        String skinId = buffer.readUtf(64);
        
        DoorModelType type = DoorModelType.fromId(typeId);
        DoorSkin skin = DoorSkin.of(skinId, skinId);
        
        DoorModelSelection selection = new DoorModelSelection(type, skin);
        return new ServerboundDoorModelPacket(blockPos, selection);
    }
    
    /**
     * Статический метод для обработки пакета
     */
    public static void handle(ServerboundDoorModelPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            ServerLevel level = player.serverLevel();
            if (level == null) return;
            
            // Проверка расстояния
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
            if (!(be instanceof DoorBlockEntity doorEntity)) {
                return;
            }
            
            if (!doorEntity.isController()) {
                return;
            }
            
            // Устанавливаем выбор
            doorEntity.setModelSelection(msg.selection);
            
            MainRegistry.LOGGER.debug("Door model changed: {} -> {} at {}", 
                doorEntity.getDoorDeclId(), msg.selection.getDisplayName(), msg.blockPos);
            
        });
        
        context.setPacketHandled(true);
    }
    
    /**
     * Отправить пакет на сервер
     */
    public static void send(BlockPos blockPos, DoorModelSelection selection) {
        ModPacketHandler.INSTANCE.sendToServer(new ServerboundDoorModelPacket(blockPos, selection));
        MainRegistry.LOGGER.debug("Sending door model packet: {} -> {}", blockPos, selection);
    }
}
