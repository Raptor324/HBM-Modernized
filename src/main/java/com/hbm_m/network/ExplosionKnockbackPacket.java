package com.hbm_m.network;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code ExplosionKnockbackPacket}: Explosions-Rueckstoss fuer den Spieler (Bewegung ist clientseitig). */
public class ExplosionKnockbackPacket implements S2CPacket {

    private final float motionX;
    private final float motionY;
    private final float motionZ;

    public ExplosionKnockbackPacket(Vec3 vec) {
        this((float) vec.x, (float) vec.y, (float) vec.z);
    }

    private ExplosionKnockbackPacket(float x, float y, float z) {
        this.motionX = x;
        this.motionY = y;
        this.motionZ = z;
    }

    public static ExplosionKnockbackPacket decode(FriendlyByteBuf buf) {
        return new ExplosionKnockbackPacket(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(motionX);
        buf.writeFloat(motionY);
        buf.writeFloat(motionZ);
    }

    public static void handle(ExplosionKnockbackPacket m, PacketContext context) {
        context.queue(() -> {
            Player thePlayer = Minecraft.getInstance().player;
            if (thePlayer == null) return;
            thePlayer.setDeltaMovement(thePlayer.getDeltaMovement().add(m.motionX, m.motionY, m.motionZ));
        });
    }

    public static void sendTo(ServerPlayer player, Vec3 vec) {
        ModPacketHandler.sendToPlayer(player, ModPacketHandler.EXPLOSION_KNOCKBACK, new ExplosionKnockbackPacket(vec));
    }
}
