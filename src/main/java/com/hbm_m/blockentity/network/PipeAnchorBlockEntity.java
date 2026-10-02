package com.hbm_m.blockentity.network;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.FluidNetProvider;
import com.hbm_m.api.fluids.FluidNode;
import com.hbm_m.api.fluids.IFluidPipeMK2;
import com.hbm_m.api.fluids.IPipelineBase;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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
 * 1:1 {@code TileEntityPipeAnchor} ({@code TileEntityPipelineBase}): Rohranker, per Schraubenschluessel mit anderen
 * Ankern bis 10 m verbunden; der Knoten verbindet die Anschlussseite und alle Gegenanker.
 */
public class PipeAnchorBlockEntity extends BaseHbmBlockEntity implements IFluidPipeMK2, IPipelineBase {

    private Fluid type = Fluids.EMPTY;
    private final List<BlockPos> connected = new ArrayList<>();
    @Nullable private FluidNode node;

    public PipeAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PIPE_ANCHOR.get(), pos, state);
    }

    /** Anschlussseite: Gegenseite der Anbringflaeche ({@code getOrientation(meta).getOpposite()}). */
    private Direction mountDir() {
        return getBlockState().getValue(BlockStateProperties.FACING).getOpposite();
    }

    public List<BlockPos> getConnected() { return connected; }

    @Override public Fluid getFluidType() { return type; }

    @Override
    public boolean canConnect(Fluid fluid, Direction dir) {
        return mountDir() == dir && fluid == this.type;
    }

    public FluidNode createNode(Fluid fluid) {
        Direction dir = mountDir();
        FluidNode n = new FluidNode(FluidNetProvider.forFluid(fluid), worldPosition).setConnections(
                new NodeDirPos(worldPosition, null),
                new NodeDirPos(worldPosition.relative(dir), dir));
        for (BlockPos p : connected) n.addConnection(new NodeDirPos(p, null));
        return n;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PipeAnchorBlockEntity te) {
        if (!(level instanceof ServerLevel sl) || te.type == Fluids.EMPTY) return;
        if (te.node == null || te.node.isExpired()) {
            var existing = UniNodespace.getNode(sl, pos, FluidNetProvider.forFluid(te.type));
            if (existing instanceof FluidNode fn && !fn.isExpired()) te.node = fn;
            else {
                te.node = te.createNode(te.type);
                UniNodespace.createNode(sl, te.node);
            }
        }
    }

    public void setType(Fluid fluid) {
        Fluid prev = this.type;
        this.type = fluid == null ? Fluids.EMPTY : fluid;
        setChanged();
        if (level instanceof ServerLevel sl) {
            if (node != null && !node.isExpired()) UniNodespace.destroyNode(sl, node);
            node = null;
            if (prev != Fluids.EMPTY && prev != type) UniNodespace.destroyNode(sl, worldPosition, FluidNetProvider.forFluid(prev));
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- IPipelineBase ----
    @Override public ConnectionType getConnectionType() { return ConnectionType.SMALL; }
    @Override public Vec3 getMountPos() { return new Vec3(0.5, 0.5, 0.5); }
    @Override public double getMaxPipeLength() { return 10; }
    @Override public Fluid getPipelineFluid() { return type; }
    @Override public void setPipelineFluid(Fluid fluid) { setType(fluid); }
    @Override public BlockPos getPipelinePos() { return worldPosition; }

    @Override
    public void addConnection(BlockPos pos) {
        connected.add(pos.immutable());
        if (level instanceof ServerLevel sl) {
            if (node == null || node.isExpired()) {
                var existing = UniNodespace.getNode(sl, worldPosition, FluidNetProvider.forFluid(type));
                if (existing instanceof FluidNode fn && !fn.isExpired()) node = fn;
                else {
                    node = createNode(type);
                    UniNodespace.createNode(sl, node);
                }
            }
            node.recentlyChanged = true;
            node.addConnection(new NodeDirPos(pos, null));
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
            if (te instanceof PipeAnchorBlockEntity other) {
                UniNodespace.destroyNode(sl, p, FluidNetProvider.forFluid(type));
                other.connected.removeIf(c -> c.equals(worldPosition));
                other.node = null;
                other.setChanged();
                level.sendBlockUpdated(p, other.getBlockState(), other.getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        UniNodespace.destroyNode(sl, worldPosition, FluidNetProvider.forFluid(type));
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
        ResourceLocation loc = BuiltInRegistries.FLUID.getKey(type);
        if (loc != null) tag.putString("FluidType", loc.toString());
        tag.putInt("conCount", connected.size());
        for (int i = 0; i < connected.size(); i++) {
            BlockPos p = connected.get(i);
            tag.putIntArray("con" + i, new int[] { p.getX(), p.getY(), p.getZ() });
        }
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        if (tag.contains("FluidType")) {
            Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(tag.getString("FluidType")));
            type = f != null ? f : Fluids.EMPTY;
        }
        connected.clear();
        int count = tag.getInt("conCount");
        for (int i = 0; i < count; i++) {
            int[] a = tag.getIntArray("con" + i);
            if (a.length == 3) connected.add(new BlockPos(a[0], a[1], a[2]));
        }
    }
}
