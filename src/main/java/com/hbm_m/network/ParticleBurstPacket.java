package com.hbm_m.network;

import dev.architectury.networking.NetworkManager.PacketContext;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code ParticleBurstPacket}: der Client spielt an einer Blockposition die
 * Zerfallspartikel eines Blocks ab ({@code effectRenderer.addBlockDestroyEffects}) - ohne Bruchgeraeusch.
 * Block-ID + Metadatum des Originals sind hier eine Blockzustands-ID.
 */
public class ParticleBurstPacket implements S2CPacket {

    private final int x;
    private final int y;
    private final int z;
    private final int stateId;

    public ParticleBurstPacket(int x, int y, int z, int stateId) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.stateId = stateId;
    }

    public ParticleBurstPacket(int x, int y, int z, BlockState state) {
        this(x, y, z, Block.getId(state));
    }

    public static ParticleBurstPacket decode(FriendlyByteBuf buf) {
        return new ParticleBurstPacket(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(stateId);
    }

    public static void handle(ParticleBurstPacket m, PacketContext context) {
        context.queue(() -> {
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level == null) return;
                mc.particleEngine.destroy(new BlockPos(m.x, m.y, m.z), Block.stateById(m.stateId));
            } catch (Exception ignored) { }
        });
    }

    /** Original {@code sendToAllAround(.., new TargetPoint(dim, cx, cy, cz, range))}. */
    public static void sendAround(ServerLevel level, double cx, double cy, double cz, double range, int x, int y, int z, BlockState state) {
        ModPacketHandler.sendToPlayersNear(level, cx, cy, cz, range, ModPacketHandler.PARTICLE_BURST, new ParticleBurstPacket(x, y, z, state));
    }
}
