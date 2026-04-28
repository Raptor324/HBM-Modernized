package com.hbm_m.network;

import com.hbm_m.network.S2CPacket;
import com.hbm_m.particle.helper.ParticleEffectClient;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class AuxParticlePacket implements S2CPacket {

    private final CompoundTag nbt;
    private final double x, y, z;

    public AuxParticlePacket(CompoundTag nbt, double x, double y, double z) {
        this.nbt = nbt != null ? nbt : new CompoundTag();
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static AuxParticlePacket decode(FriendlyByteBuf buf) {
        return new AuxParticlePacket(buf.readNbt(), buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeNbt(nbt);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(AuxParticlePacket msg, PacketContext context) {
        context.queue(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            CompoundTag data = msg.nbt.copy();
            data.putDouble("posX", msg.x);
            data.putDouble("posY", msg.y);
            data.putDouble("posZ", msg.z);

            ParticleEffectClient.effectNT(data);
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendTo(ServerPlayer player, CompoundTag nbt, double x, double y, double z) {
        ModPacketHandler.sendToPlayer(player, ModPacketHandler.AUX_PARTICLE,
                new AuxParticlePacket(nbt, x, y, z));
    }
}