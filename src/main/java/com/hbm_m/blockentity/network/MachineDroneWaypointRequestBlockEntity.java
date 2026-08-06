package com.hbm_m.blockentity.network;

import com.hbm_m.block.machines.MachineDroneWaypointRequestBlock;
import com.hbm_m.blockentity.network.request.RequestNetwork.PathNode;
import com.hbm_m.blockentity.network.request.RequestNetworkParticipant;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Drone Waypoint Request - Port von {@code TileEntityDroneWaypointRequest} (1.7.10 Original,
 * Pipeline B). Reiner Netzwerk-Knoten ohne Inventar ("torch waypoint" - Pflicht-Relaisstation, da
 * {@link RequestNetworkParticipant#areNodesConnectable} verlangt, dass mindestens einer der beiden
 * Knoten ein Torch-Waypoint ist - Provider/Requester/Dock koennen sich nur ueber diese Bloecke
 * verbinden, nicht direkt).
 */
public class MachineDroneWaypointRequestBlockEntity extends BlockEntity {

    public static final int MIN_HEIGHT = 1;
    public static final int MAX_HEIGHT = 15;
    public static final int DEFAULT_HEIGHT = 5;

    private int height = DEFAULT_HEIGHT;
    private final RequestNetworkParticipant network = new RequestNetworkParticipant(pos -> new PathNode(pos, java.util.Set.of()));

    public MachineDroneWaypointRequestBlockEntity(BlockPos pos, BlockState state) {
        super(com.hbm_m.blockentity.ModBlockEntities.DRONE_WAYPOINT_REQUEST_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineDroneWaypointRequestBlockEntity be) {
        if (level.isClientSide) return;
        if (level.getGameTime() % 20 != 0) return;
        be.network.tick(level, be.getNodePos(), level.hasNeighborSignal(pos));
    }

    public BlockPos getNodePos() {
        Direction mount = getBlockState().getValue(MachineDroneWaypointRequestBlock.FACING);
        return worldPosition.relative(mount, height);
    }

    public RequestNetworkParticipant getNetwork() { return network; }

    public int getHeight() { return height; }

    public void adjustHeight(boolean increase) {
        height = increase ? Math.min(MAX_HEIGHT, height + 1) : Math.max(MIN_HEIGHT, height - 1);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("height", height);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        height = tag.contains("height") ? tag.getInt("height") : DEFAULT_HEIGHT;
    }
}
