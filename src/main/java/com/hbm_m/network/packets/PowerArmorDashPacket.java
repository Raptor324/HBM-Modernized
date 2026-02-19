package com.hbm_m.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;


// Пакет для синхронизации dash движения силовой брони

public class PowerArmorDashPacket {

    private final int playerId;
    private final Vec3 velocity;

    public PowerArmorDashPacket(int playerId, Vec3 velocity) {
        this.playerId = playerId;
        this.velocity = velocity;
    }

    public PowerArmorDashPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readInt();
        this.velocity = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(playerId);
        buf.writeDouble(velocity.x);
        buf.writeDouble(velocity.y);
        buf.writeDouble(velocity.z);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Обработка на клиенте
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                handleClient();
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Player player = (Player) mc.level.getEntity(playerId);
        if (player != null && player != mc.player) {
            // Применяем движение к игроку (кроме локального игрока)
            player.setDeltaMovement(velocity);
        }
    }
}

