package com.hbm_m.blockentity.machines.fusion;

import java.util.Map.Entry;

import com.hbm_m.api.fusion.IFusionPowerReceiver;
import com.hbm_m.api.fusion.KlystronNetwork;
import com.hbm_m.api.fusion.KlystronNetworkProvider;
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
 * 1:1-Port von {@code TileEntityFusionCoupler} (1.7.10).
 *
 * <p>Der Koppler haengt mit einer Seite als Empfaenger im Plasmanetz und mit der anderen als
 * Anbieter im Klystronnetz: die abgenommene Plasmaleistung wird unveraendert als Klystronenergie
 * in den dort haengenden Torus geschoben (Tandembetrieb zweier Reaktoren).</p>
 */
public class FusionCouplerBlockEntity extends FusionSyncedBlockEntity implements IFusionPowerReceiver {

    private GenNode<KlystronNetwork> klystronNode;
    private GenNode<PlasmaNetwork> plasmaNode;

    public FusionCouplerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_COUPLER_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FusionCouplerBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        be.serverTick(serverLevel, pos, state);
    }

    private void serverTick(ServerLevel level, BlockPos pos, BlockState state) {

        Direction dir = state.getValue(FusionMultiblockBlock.FACING).getOpposite();
        Direction rot = dir.getClockWise();

        if (klystronNode == null || klystronNode.expired) {
            BlockPos nodePos = pos.offset(rot.getStepX(), 2, rot.getStepZ());
            klystronNode = UniNodespace.getNode(level, nodePos, KlystronNetworkProvider.THE_PROVIDER);

            if (klystronNode == null) {
                klystronNode = new GenNode<>(KlystronNetworkProvider.THE_PROVIDER, nodePos)
                        .setConnections(new NodeDirPos(pos.offset(rot.getStepX() * 2, 2, rot.getStepZ() * 2), rot));
                UniNodespace.createNode(level, klystronNode);
            }
        }

        if (plasmaNode == null || plasmaNode.expired) {
            BlockPos nodePos = pos.offset(-rot.getStepX(), 2, -rot.getStepZ());
            plasmaNode = UniNodespace.getNode(level, nodePos, PlasmaNetworkProvider.THE_PROVIDER);

            if (plasmaNode == null) {
                plasmaNode = new GenNode<>(PlasmaNetworkProvider.THE_PROVIDER, nodePos)
                        .setConnections(new NodeDirPos(pos.offset(-rot.getStepX() * 2, 2, -rot.getStepZ() * 2), rot.getOpposite()));
                UniNodespace.createNode(level, plasmaNode);
            }
        }

        if (klystronNode.net != null) klystronNode.net.addProvider(this);
        if (plasmaNode.net != null) plasmaNode.net.addReceiver(this);
    }

    @Override
    public boolean receivesFusionPower() {
        return true;
    }

    @Override
    public void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b) {

        if (klystronNode != null && klystronNode.net != null) {
            KlystronNetwork net = klystronNode.net;

            for (Entry<BlockEntity, Long> e : net.receiverEntries.entrySet()) {
                if (e.getKey() instanceof FusionTorusBlockEntity torus) {
                    if (torus.isLoaded() && !torus.isRemoved()) {
                        torus.klystronEnergy += fusionPower;
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel serverLevel) {
            if (klystronNode != null) UniNodespace.destroyNode(serverLevel, klystronNode);
            if (plasmaNode != null) UniNodespace.destroyNode(serverLevel, plasmaNode);
        }
    }

    private AABB renderBounds = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (renderBounds == null) {
            renderBounds = new AABB(
                    worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                    worldPosition.getX() + 2, worldPosition.getY() + 4, worldPosition.getZ() + 2);
        }
        return renderBounds;
    }
}
