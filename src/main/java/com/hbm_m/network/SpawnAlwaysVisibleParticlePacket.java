package com.hbm_m.network;

import com.hbm_m.particle.ModExplosionParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class SpawnAlwaysVisibleParticlePacket {
    private final ResourceLocation particleTypeId;
    private final double x, y, z;
    private final double xSpeed, ySpeed, zSpeed;

    public SpawnAlwaysVisibleParticlePacket(ParticleType<?> particleType,
                                            double x, double y, double z,
                                            double xSpeed, double ySpeed, double zSpeed) {
        this.particleTypeId = ForgeRegistries.PARTICLE_TYPES.getKey(particleType);
        this.x = x;
        this.y = y;
        this.z = z;
        this.xSpeed = xSpeed;
        this.ySpeed = ySpeed;
        this.zSpeed = zSpeed;
    }

    public SpawnAlwaysVisibleParticlePacket(FriendlyByteBuf buf) {
        this.particleTypeId = buf.readResourceLocation();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.xSpeed = buf.readDouble();
        this.ySpeed = buf.readDouble();
        this.zSpeed = buf.readDouble();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(particleTypeId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(xSpeed);
        buf.writeDouble(ySpeed);
        buf.writeDouble(zSpeed);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ParticleType<?> particleType = ForgeRegistries.PARTICLE_TYPES.getValue(particleTypeId);
                if (particleType != null && Minecraft.getInstance().level != null) {
                    // КЛЮЧЕВОЙ МОМЕНТ: используем addAlwaysVisibleParticle
                    Minecraft.getInstance().level.addAlwaysVisibleParticle(
                            (ParticleOptions) particleType,
                            x, y, z,
                            xSpeed, ySpeed, zSpeed
                    );
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}