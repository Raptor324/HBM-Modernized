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
import com.hbm_m.inventory.fluid.ModFluids;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code TileEntityPipeExhaustAnchor}: Rohranker fuer Abgas. Wie der Fluessigkeits-Anker, aber fest auf die drei
 * Rauchsorten (SMOKE, SMOKE_LEADED, SMOKE_POISON) - je Sorte ein eigener Knoten an derselben Position.
 */
public class ExhaustPipeAnchorBlockEntity extends BaseHbmBlockEntity implements IFluidPipeMK2, IPipelineBase {

    private final List<BlockPos> connected = new ArrayList<>();
    private final FluidNode[] nodes = new FluidNode[3];

    public ExhaustPipeAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PIPE_ANCHOR_EXHAUST.get(), pos, state);
    }

    private static Fluid[] smokes() {
        return new Fluid[] { ModFluids.SMOKE.getSource(), ModFluids.SMOKE_LEADED.getSource(), ModFluids.SMOKE_POISON.getSource() };
    }

    private Direction mountDir() {
        return getBlockState().getValue(BlockStateProperties.FACING).getOpposite();
    }

    @Override public List<BlockPos> getConnected() { return connected; }

    /** Mehrere Sorten - wie die Abgasleitung ohne festen Typ. */
    @Override public Fluid getFluidType() { return null; }

    @Override
    public boolean canConnect(Fluid fluid, Direction dir) {
        if (mountDir() != dir) return false;
        for (Fluid smoke : smokes()) if (fluid == smoke) return true;
        return false;
    }

    private FluidNode createNode(Fluid fluid) {
        Direction dir = mountDir();
        FluidNode n = new FluidNode(FluidNetProvider.forFluid(fluid), worldPosition).setConnections(
                new NodeDirPos(worldPosition, null),
                new NodeDirPos(worldPosition.relative(dir), dir));
        for (BlockPos p : connected) n.addConnection(new NodeDirPos(p, null));
        return n;
    }

    private FluidNode ensureNode(ServerLevel sl, int i) {
        Fluid fluid = smokes()[i];
        if (nodes[i] == null || nodes[i].isExpired()) {
            var existing = UniNodespace.getNode(sl, worldPosition, FluidNetProvider.forFluid(fluid));
            if (existing instanceof FluidNode fn && !fn.isExpired()) nodes[i] = fn;
            else {
                nodes[i] = createNode(fluid);
                UniNodespace.createNode(sl, nodes[i]);
            }
        }
        return nodes[i];
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ExhaustPipeAnchorBlockEntity te) {
        if (!(level instanceof ServerLevel sl)) return;
        for (int i = 0; i < te.nodes.length; i++) te.ensureNode(sl, i);
    }

    // ---- IPipelineBase ----
    @Override public NetworkType getNetworkType() { return NetworkType.EXHAUST; }
    @Override public ConnectionType getConnectionType() { return ConnectionType.SMALL; }
    @Override public Vec3 getMountPos() { return new Vec3(0.5, 0.5, 0.5); }
    @Override public double getMaxPipeLength() { return 10; }
    /** Fester Typ, damit der Fluessigkeitsvergleich in {@link IPipelineBase#canConnect} zwischen Abgas-Ankern greift. */
    @Override public Fluid getPipelineFluid() { return ModFluids.SMOKE.getSource(); }
    @Override public void setPipelineFluid(Fluid fluid) { }
    @Override public BlockPos getPipelinePos() { return worldPosition; }

    @Override
    public void addConnection(BlockPos pos) {
        connected.add(pos.immutable());
        if (level instanceof ServerLevel sl) {
            for (int i = 0; i < nodes.length; i++) {
                FluidNode n = ensureNode(sl, i);
                n.recentlyChanged = true;
                n.addConnection(new NodeDirPos(pos, null));
            }
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
        setChanged();
    }

    private void destroyNodes(ServerLevel sl, BlockPos p) {
        for (Fluid smoke : smokes()) UniNodespace.destroyNode(sl, p, FluidNetProvider.forFluid(smoke));
    }

    /** {@code disconnectAll}: Gegenanker vergessen diesen Anker, die Knoten werden neu gebaut. */
    public void disconnectAll() {
        if (!(level instanceof ServerLevel sl)) return;
        for (BlockPos p : connected) {
            BlockEntity te = level.getBlockEntity(p);
            if (te == this) continue;
            if (te instanceof ExhaustPipeAnchorBlockEntity other) {
                destroyNodes(sl, p);
                other.connected.removeIf(c -> c.equals(worldPosition));
                java.util.Arrays.fill(other.nodes, null);
                other.setChanged();
                level.sendBlockUpdated(p, other.getBlockState(), other.getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        destroyNodes(sl, worldPosition);
        java.util.Arrays.fill(nodes, null);
    }

    @Override
    public void setRemoved() {
        disconnectAll();
        super.setRemoved();
    }

    //? if forge {
    @Override
    public void onChunkUnloaded() {
        for (FluidNode n : nodes) if (n != null) n.expired = true;
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
