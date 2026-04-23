package com.hbm_m.network;

import com.hbm_m.particle.helper.ParticleEffectClient;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** S2C packet: spawn custom NT particle at (x,y,z) with NBT data. */
public class AuxParticlePacket {

    private final CompoundTag nbt;
    private final double x, y, z;

    public AuxParticlePacket(CompoundTag nbt, double x, double y, double z) {
        this.nbt = nbt != null ? nbt : new CompoundTag();
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static void encode(AuxParticlePacket msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.nbt);
        buf.writeDouble(msg.x);
        buf.writeDouble(msg.y);
        buf.writeDouble(msg.z);
    }

    public static AuxParticlePacket decode(FriendlyByteBuf buf) {
        return new AuxParticlePacket(buf.readNbt(), buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(AuxParticlePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    Minecraft mc = Minecraft.getInstance();
                    CompoundTag data = msg.nbt.copy();
                    data.putDouble("posX", msg.x);
                    data.putDouble("posY", msg.y);
                    data.putDouble("posZ", msg.z);
                    if (mc.level != null) {
                        ParticleEffectClient.effectNT(data);
                    }
                }));
    }
}
