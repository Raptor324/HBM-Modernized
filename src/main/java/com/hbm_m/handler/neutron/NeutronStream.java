package com.hbm_m.handler.neutron;

import java.util.Iterator;

import com.hbm_m.handler.neutron.NeutronNodeWorld.StreamWorld;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Port of NEXT {@code com.hbm.handler.neutron.NeutronStream}. The full
 * {@code (origin, vector, flux, ratio, type)} constructor QUEUES the stream into the origin's
 * {@link StreamWorld} - this is what makes {@code castRay} lazy: the ray is only marched when
 * {@code NeutronHandler} drains the queued streams after all BE ticks.
 */
public abstract class NeutronStream {

    public NeutronNode origin;
    public double fluxQuantity;

    public double fluxRatio;
    public NeutronType type = NeutronType.DUMMY;

    public Vec3 vector;

    public NeutronStream(NeutronNode origin, Vec3 vector) {
        this.origin = origin;
        this.vector = vector;
    }

    public NeutronStream(
            NeutronNode origin, Vec3 vector, double flux, double ratio, NeutronType type) {
        this.origin = origin;
        this.vector = vector;
        this.fluxQuantity = flux;
        this.fluxRatio = ratio;
        this.type = type;
        NeutronNodeWorld.getOrAddWorld(origin.tile.getLevel()).addStream(this);
    }

    public Iterator<BlockPos> getBlocks(int range) {
        BlockPos ori = origin.tile.getBlockPos();
        return new Iterator<>() {
            int i = 1;

            @Override
            public boolean hasNext() {
                return i <= range;
            }

            @Override
            public BlockPos next() {
                int x = (int) Math.floor(0.5 + vector.x * i);
                int z = (int) Math.floor(0.5 + vector.z * i);
                i++;
                return new BlockPos(ori.getX() + x, ori.getY(), ori.getZ() + z);
            }
        };
    }

    public abstract void runStreamInteraction(Level level, StreamWorld streamWorld);

    public enum NeutronType {
        DUMMY,
        RBMK,
        PILE
    }
}
