package com.hbm_m.blockentity.machines.fusion;

import com.hbm_m.api.fusion.PlasmaNetwork;
import com.hbm_m.api.fusion.PlasmaNetworkProvider;
import com.hbm_m.api.network.GenNode;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.block.machines.fusion.FusionMultiblockBlock;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1-Port von {@code TileEntityFusionCollector} (1.7.10).
 *
 * <p>Der Kollektor haengt nur als Empfaenger im Plasmanetz, verbraucht selbst nichts und
 * implementiert bewusst <b>nicht</b> {@code IFusionPowerReceiver}: der Torus zaehlt ihn getrennt
 * und erhoeht dafuer die Bonusgeschwindigkeit des Rezepts
 * (siehe {@code TileEntityFusionTorus.updateEntity}, {@code collectors * 0.5D}).</p>
 */
public class FusionCollectorBlockEntity extends BlockEntity {

    private GenNode<PlasmaNetwork> plasmaNode;

    public FusionCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_COLLECTOR_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FusionCollectorBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        be.serverTick(serverLevel, pos, state);
    }

    private void serverTick(ServerLevel level, BlockPos pos, BlockState state) {

        if (plasmaNode == null || plasmaNode.expired) {
            // Original: ForgeDirection.getOrientation(meta - 10).getOpposite()
            Direction dir = state.getValue(FusionMultiblockBlock.FACING).getOpposite();
            BlockPos nodePos = pos.offset(dir.getStepX() * 2, 2, dir.getStepZ() * 2);
            plasmaNode = UniNodespace.getNode(level, nodePos, PlasmaNetworkProvider.THE_PROVIDER);

            if (plasmaNode == null) {
                plasmaNode = new GenNode<>(PlasmaNetworkProvider.THE_PROVIDER, nodePos)
                        .setConnections(new NodeDirPos(pos.offset(dir.getStepX() * 3, 2, dir.getStepZ() * 3), dir));
                UniNodespace.createNode(level, plasmaNode);
            }
        }

        if (plasmaNode != null && plasmaNode.hasValidNet()) plasmaNode.net.addReceiver(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel serverLevel && plasmaNode != null) {
            UniNodespace.destroyNode(serverLevel, plasmaNode);
        }
    }

    private AABB renderBounds = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (renderBounds == null) {
            renderBounds = new AABB(
                    worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ() - 2,
                    worldPosition.getX() + 3, worldPosition.getY() + 4, worldPosition.getZ() + 3);
        }
        return renderBounds;
    }
}
