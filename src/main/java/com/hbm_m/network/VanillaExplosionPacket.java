package com.hbm_m.network;

import com.hbm_m.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm_m.network.S2CPacket;

import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * S2C: частицы взрыва VNT (порт {@code ExplosionVanillaNewTechnologyCompressed...Packet} 1.7.10).
 */
public class VanillaExplosionPacket implements S2CPacket {

    private final double x;
    private final double y;
    private final double z;
    private final float size;
    private final List<BlockPos> affectedBlocks;

    public VanillaExplosionPacket(double x, double y, double z, float size, List<BlockPos> affectedBlocks) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.size = size;
        this.affectedBlocks = affectedBlocks;
    }

    public static VanillaExplosionPacket decode(FriendlyByteBuf buf) {
        double posX = buf.readFloat();
        double posY = buf.readFloat();
        double posZ = buf.readFloat();
        float explosionSize = buf.readFloat();
        int count = buf.readInt();
        List<BlockPos> blocks = new ArrayList<>(count);
        int baseX = (int) posX;
        int baseY = (int) posY;
        int baseZ = (int) posZ;
        for (int i = 0; i < count; i++) {
            blocks.add(new BlockPos(baseX + buf.readByte(), baseY + buf.readByte(), baseZ + buf.readByte()));
        }
        return new VanillaExplosionPacket(posX, posY, posZ, explosionSize, blocks);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeFloat((float) x);
        buf.writeFloat((float) y);
        buf.writeFloat((float) z);
        buf.writeFloat(size);
        buf.writeInt(affectedBlocks.size());
        int baseX = (int) x;
        int baseY = (int) y;
        int baseZ = (int) z;
        for (BlockPos pos : affectedBlocks) {
            buf.writeByte(pos.getX() - baseX);
            buf.writeByte(pos.getY() - baseY);
            buf.writeByte(pos.getZ() - baseZ);
        }
    }

    public static void handle(VanillaExplosionPacket msg, PacketContext context) {
        context.queue(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            ExplosionEffectStandard.performClient(level, msg.x, msg.y, msg.z, msg.size, msg.affectedBlocks);
        });
    }

    public static void sendNear(ServerLevel level, double x, double y, double z, float size, List<BlockPos> affectedBlocks) {
        ModPacketHandler.sendToPlayersNear(level, x, y, z, 250.0D,
                ModPacketHandler.VANILLA_EXPLOSION, new VanillaExplosionPacket(x, y, z, size, affectedBlocks));
    }
}
