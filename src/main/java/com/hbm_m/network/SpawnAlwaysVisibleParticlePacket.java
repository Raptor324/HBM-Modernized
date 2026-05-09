package com.hbm_m.network;

import com.hbm_m.network.S2CPacket;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class SpawnAlwaysVisibleParticlePacket implements S2CPacket {

    private final ResourceLocation particleTypeId;
    private final double x, y, z;
    private final double xSpeed, ySpeed, zSpeed;

    public SpawnAlwaysVisibleParticlePacket(ParticleType<?> particleType,
                                            double x, double y, double z,
                                            double xSpeed, double ySpeed, double zSpeed) {
        this.particleTypeId = BuiltInRegistries.PARTICLE_TYPE.getKey(particleType);
        this.x = x;
        this.y = y;
        this.z = z;
        this.xSpeed = xSpeed;
        this.ySpeed = ySpeed;
        this.zSpeed = zSpeed;
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public static SpawnAlwaysVisibleParticlePacket decode(FriendlyByteBuf buf) {
        ResourceLocation typeId = buf.readResourceLocation();
        double x      = buf.readDouble();
        double y      = buf.readDouble();
        double z      = buf.readDouble();
        double xSpeed = buf.readDouble();
        double ySpeed = buf.readDouble();
        double zSpeed = buf.readDouble();

        ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(typeId);
        return new SpawnAlwaysVisibleParticlePacket(type, x, y, z, xSpeed, ySpeed, zSpeed);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(particleTypeId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(xSpeed);
        buf.writeDouble(ySpeed);
        buf.writeDouble(zSpeed);
    }

    // ── Handler ───────────────────────────────────────────────────────────────

    public static void handle(SpawnAlwaysVisibleParticlePacket msg, PacketContext context) {
        context.queue(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            ParticleType<?> particleType = BuiltInRegistries.PARTICLE_TYPE.get(msg.particleTypeId);
            if (particleType != null) {
                mc.level.addAlwaysVisibleParticle(
                        (ParticleOptions) particleType,
                        msg.x, msg.y, msg.z,
                        msg.xSpeed, msg.ySpeed, msg.zSpeed
                );
            }
        });
    }

    // ── Send helper ───────────────────────────────────────────────────────────

    public static void sendTo(ServerPlayer player, ParticleType<?> type,
                              double x, double y, double z,
                              double xSpeed, double ySpeed, double zSpeed) {
        // Нужен ID в ModPacketHandler:
        // public static final ResourceLocation SPAWN_PARTICLE = id("spawn_particle");
        ModPacketHandler.sendToPlayer(player, ModPacketHandler.SPAWN_PARTICLE,
                new SpawnAlwaysVisibleParticlePacket(type, x, y, z, xSpeed, ySpeed, zSpeed));
    }
}