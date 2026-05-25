package com.hbm_m.network.missile;

import com.hbm_m.client.missile.track.MissileTrackClient;
import com.hbm_m.missile.track.MissileTrackPose;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.S2CPacket;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class S2CMissileTrackPacket implements S2CPacket {

    public final int entityId;
    public final ResourceLocation dimensionId;
    public final double x;
    public final double y;
    public final double z;
    public final double vx;
    public final double vy;
    public final double vz;
    public final float yaw;
    public final float pitch;
    public final Direction launchFacing;
    public final ResourceLocation entityTypeId;
    public final ResourceLocation launchItemId;
    public final float contrailScale;
    public final long worldTick;

    public S2CMissileTrackPacket(int entityId, ResourceLocation dimensionId, MissileTrackPose pose) {
        this.entityId = entityId;
        this.dimensionId = dimensionId;
        this.x = pose.x();
        this.y = pose.y();
        this.z = pose.z();
        this.vx = pose.vx();
        this.vy = pose.vy();
        this.vz = pose.vz();
        this.yaw = pose.yaw();
        this.pitch = pose.pitch();
        this.launchFacing = pose.launchFacing();
        this.entityTypeId = pose.entityTypeId();
        this.launchItemId = pose.launchItemId();
        this.contrailScale = pose.contrailScale();
        this.worldTick = pose.worldTick();
    }

    public S2CMissileTrackPacket(int entityId, ResourceLocation dimensionId,
                                 double x, double y, double z,
                                 double vx, double vy, double vz,
                                 float yaw, float pitch, Direction launchFacing,
                                 ResourceLocation entityTypeId, ResourceLocation launchItemId,
                                 float contrailScale, long worldTick) {
        this.entityId = entityId;
        this.dimensionId = dimensionId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.yaw = yaw;
        this.pitch = pitch;
        this.launchFacing = launchFacing;
        this.entityTypeId = entityTypeId;
        this.launchItemId = launchItemId;
        this.contrailScale = contrailScale;
        this.worldTick = worldTick;
    }

    public static S2CMissileTrackPacket decode(FriendlyByteBuf buf) {
        return new S2CMissileTrackPacket(
                buf.readVarInt(),
                buf.readResourceLocation(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readFloat(), buf.readFloat(),
                buf.readEnum(Direction.class),
                buf.readResourceLocation(),
                buf.readResourceLocation(),
                buf.readFloat(),
                buf.readLong());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeResourceLocation(dimensionId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeDouble(vx);
        buf.writeDouble(vy);
        buf.writeDouble(vz);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeEnum(launchFacing);
        buf.writeResourceLocation(entityTypeId);
        buf.writeResourceLocation(launchItemId);
        buf.writeFloat(contrailScale);
        buf.writeLong(worldTick);
    }

    public static void handle(S2CMissileTrackPacket msg, PacketContext context) {
        context.queue(() -> {
            MissileTrackClient.onTrack(msg);
            if (Boolean.getBoolean("hbm_m.missileTrackDebug")) {
                com.mojang.logging.LogUtils.getLogger().info(
                    "[MissileTrack] Client received track for entity {} at {}/{}/{} yaw={} pitch={}",
                    msg.entityId, (int)msg.x, (int)msg.y, (int)msg.z, (int)msg.yaw, (int)msg.pitch);
            }
        });
    }

    public static void sendTo(ServerPlayer player, S2CMissileTrackPacket packet) {
        ModPacketHandler.sendToPlayer(player, ModPacketHandler.MISSILE_TRACK, packet);
    }
}
