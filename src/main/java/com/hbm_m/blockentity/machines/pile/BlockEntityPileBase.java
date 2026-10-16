package com.hbm_m.blockentity.machines.pile;

import com.hbm_m.handler.neutron.NeutronNodeWorld;
import com.hbm_m.handler.neutron.NeutronNodeWorld.StreamWorld;
import com.hbm_m.handler.neutron.PileNeutronHandler;
import com.hbm_m.handler.neutron.PileNeutronHandler.PileNeutronNode;
import com.hbm_m.handler.neutron.PileNeutronHandler.PileNeutronStream;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Port of NEXT {@code com.hbm.tileentity.machine.pile.BlockEntityPileBase}: abstract base of the
 * CP-1 channel BEs that cast neutron rays. NOT the Meiler lineage - that is
 * {@code PileBaseBlockEntity} (1.7.10 {@code TileEntityPileBaseMK2}), an unrelated class that
 * happened to share the name. A plain BlockEntity, no client sync, like the detector.
 */
public abstract class BlockEntityPileBase extends BlockEntity {

    protected BlockEntityPileBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract void tickServer();

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Eager node removal replaces NEXT's periodic cleanNodes sweep (which only pruned RBMK
        // nodes anyway) - see com.hbm_m.handler.neutron.NeutronNodeWorld.
        if (level != null) NeutronNodeWorld.removeNode(level, worldPosition);
    }

    /**
     * Port of NEXT {@code castRay}: queues a randomly-rotated {@link PileNeutronStream} carrying
     * {@code flux} neutrons; the ray only marches when NeutronHandler drains the stream world
     * after all BE ticks. flux == 0 drops this BE's node instead.
     */
    protected void castRay(int flux) {

        if (flux == 0) {
            NeutronNodeWorld.removeNode(level, worldPosition);
            return;
        }

        StreamWorld streamWorld = NeutronNodeWorld.getOrAddWorld(level);
        PileNeutronNode node = (PileNeutronNode) streamWorld.getNode(worldPosition);

        if (node == null) {
            node = PileNeutronHandler.makeNode(streamWorld, this);
            streamWorld.addNode(node);
        }

        RandomSource rand = level.getRandom();
        Vec3 neutronVector =
                new Vec3(1, 0, 0)
                        .zRot((float) (Math.PI * 2D * rand.nextDouble()))
                        .yRot((float) (Math.PI * 2D * rand.nextDouble()))
                        .xRot((float) (Math.PI * 2D * rand.nextDouble()));

        new PileNeutronStream(node, neutronVector, flux);
    }
}
