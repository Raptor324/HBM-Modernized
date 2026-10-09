package com.hbm_m.blockentity.network;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IPipelineBase;
import com.hbm_m.api.network.GenNode;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.api.pneumatic.IPneumaticConnector;
import com.hbm_m.api.pneumatic.PneumaticNet;
import com.hbm_m.api.pneumatic.PneumaticNetProvider;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code TileEntityPneumaticPipeAnchor}: Rohranker fuer das pneumatische Rohrnetz. Wie der Fluessigkeits-Anker per
 * Schraubenschluessel bis 10 m mit anderen pneumatischen Ankern verbunden; der Knoten verbindet die Anschlussseite
 * und alle Gegenanker.
 */
public class PneumaticPipeAnchorBlockEntity extends BaseHbmBlockEntity implements IPneumaticConnector, IPipelineBase {

    private final List<BlockPos> connected = new ArrayList<>();
    @Nullable private GenNode<PneumaticNet> node;

    public PneumaticPipeAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PIPE_ANCHOR_PNEUMATIC.get(), pos, state);
    }

    private Direction mountDir() {
        return getBlockState().getValue(BlockStateProperties.FACING).getOpposite();
    }

    @Override public List<BlockPos> getConnected() { return connected; }

    @Override
    public boolean canConnectPneumatic(@Nullable Direction dir) {
        return mountDir() == dir;
    }

    private GenNode<PneumaticNet> ensureNode(ServerLevel sl) {
        if (node == null || node.isExpired()) {
            GenNode<PneumaticNet> existing = UniNodespace.getNode(sl, worldPosition, PneumaticNetProvider.THE_PROVIDER);
            if (existing != null && !existing.isExpired()) {
                node = existing;
            } else {
                Direction dir = mountDir();
                GenNode<PneumaticNet> fresh = new GenNode<>(PneumaticNetProvider.THE_PROVIDER, worldPosition);
                fresh.setConnections(new NodeDirPos(worldPosition, null), new NodeDirPos(worldPosition.relative(dir), dir));
                for (BlockPos p : connected) fresh.addConnection(new NodeDirPos(p, null));
                UniNodespace.createNode(sl, fresh);
                node = fresh;
            }
        }
        return node;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PneumaticPipeAnchorBlockEntity te) {
        if (level instanceof ServerLevel sl) te.ensureNode(sl);
    }

    // ---- IPipelineBase ----
    @Override public NetworkType getNetworkType() { return NetworkType.PNEUMATIC; }
    @Override public ConnectionType getConnectionType() { return ConnectionType.SMALL; }
    @Override public Vec3 getMountPos() { return new Vec3(0.5, 0.5, 0.5); }
    @Override public double getMaxPipeLength() { return 10; }
    /** Kein Fluessigkeitstyp; beide Seiten melden EMPTY, damit der Typvergleich in {@link IPipelineBase#canConnect} passt. */
    @Override public Fluid getPipelineFluid() { return Fluids.EMPTY; }
    @Override public void setPipelineFluid(Fluid fluid) { }
    @Override public BlockPos getPipelinePos() { return worldPosition; }

    @Override
    public void addConnection(BlockPos pos) {
        connected.add(pos.immutable());
        if (level instanceof ServerLevel sl) {
            GenNode<PneumaticNet> n = ensureNode(sl);
            n.recentlyChanged = true;
            n.addConnection(new NodeDirPos(pos, null));
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        setChanged();
    }

    /** {@code disconnectAll}: Gegenanker vergessen diesen Anker, die Knoten werden neu gebaut. */
    public void disconnectAll() {
        if (!(level instanceof ServerLevel sl)) return;
        for (BlockPos p : connected) {
            BlockEntity te = level.getBlockEntity(p);
            if (te == this) continue;
            if (te instanceof PneumaticPipeAnchorBlockEntity other) {
                UniNodespace.destroyNode(sl, p, PneumaticNetProvider.THE_PROVIDER);
                other.connected.removeIf(c -> c.equals(worldPosition));
                other.node = null;
                other.setChanged();
                level.sendBlockUpdated(p, other.getBlockState(), other.getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        UniNodespace.destroyNode(sl, worldPosition, PneumaticNetProvider.THE_PROVIDER);
        node = null;
    }

    @Override
    public void setRemoved() {
        disconnectAll();
        super.setRemoved();
    }

    //? if forge {
    @Override
    public void onChunkUnloaded() {
        if (node != null) node.expired = true;
        super.onChunkUnloaded();
    }
    //?}

    @Override
    public AABB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        tag.putInt("conCount", connected.size());
        for (int i = 0; i < connected.size(); i++) {
            BlockPos p = connected.get(i);
            tag.putIntArray("con" + i, new int[] { p.getX(), p.getY(), p.getZ() });
        }
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        connected.clear();
        int count = tag.getInt("conCount");
        for (int i = 0; i < count; i++) {
            int[] a = tag.getIntArray("con" + i);
            if (a.length == 3) connected.add(new BlockPos(a[0], a[1], a[2]));
        }
    }
}
