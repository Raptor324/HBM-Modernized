package com.hbm_m.blockentity;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidReceiverMK2;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.api.network.NodeNet;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.api.rebar.RebarNetworkProvider;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.generic.BlockRebar.RebarNode;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.item.tool.ItemRebarPlacer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code BlockRebar.TileEntityRebar}: nimmt fluessigen Beton aus Rohren an und verteilt ihn ueber das
 * Bewehrungsnetz gleichmaessig auf die unterste noch nicht volle Lage (hoechstens 50 mB pro Block und Transfer).
 * Die gewaehlte Betonsorte steht als Registry-Name in "block" (Original: Block-ID + "meta").
 */
public class RebarBlockEntity extends BaseHbmBlockEntity implements IFluidReceiverMK2, NodeNet.ILoadedEntry {

    @Nullable public Block concrete;
    public int concreteMeta;
    public int progress;
    public int prevProgress;
    protected RebarNode node;
    public boolean hasConnection = false;

    public RebarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REBAR_BE.get(), pos, state);
    }

    public RebarBlockEntity setup(Block b, int m) {
        this.concrete = b;
        this.concreteMeta = m;
        this.setChanged();
        return this;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RebarBlockEntity be) {
        be.updateEntity();
    }

    private Fluid concreteFluid() {
        return ModFluids.CONCRETE.getSource();
    }

    public void updateEntity() {

        long time = level.getGameTime();

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {

            if (prevProgress != progress) {
                this.setChanged();
                prevProgress = progress;
            }

            if (this.progress >= 1_000) {
                if (concrete != null && ItemRebarPlacer.isValidConk(concrete.asItem(), concreteMeta)) {
                    level.setBlock(worldPosition, concrete.defaultBlockState(), 3);
                } else {
                    level.setBlock(worldPosition, ModBlocks.CONCRETE_REBAR.get().defaultBlockState(), 3);
                }
                return;
            }

            if (time % 60 == 0) {
                for (Direction dir : Direction.values()) {
                    this.trySubscribe(concreteFluid(), level, worldPosition.relative(dir), dir);
                }
            }

            if (this.node == null || this.node.expired) {

                this.node = (RebarNode) (Object) UniNodespace.getNode(serverLevel, worldPosition, RebarNetworkProvider.THE_PROVIDER);

                if (this.node == null || this.node.expired) {
                    this.node = this.createNode();
                    UniNodespace.createNode(serverLevel, this.node);
                }
            }

            this.networkPackNT(serverLevel);
        }
    }

    /** Original networkPackNT(100): nur der Fortschritt, ohne den Block neu zu zeichnen. */
    private void networkPackNT(ServerLevel serverLevel) {
        //? if < 1.21.1 {
        ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(this, be -> {
        //?} else {
        /*ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(this, (be, hbmRa) -> {
        *///?}
            CompoundTag tag = new CompoundTag();
            tag.putInt("progress", ((RebarBlockEntity) be).progress);
            return tag;
        });
        for (var player : serverLevel.getChunkSource().chunkMap.getPlayers(new ChunkPos(worldPosition), false)) {
            if (player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 100 * 100)
                player.connection.send(packet);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();

        if (level instanceof ServerLevel serverLevel) {
            if (this.node != null) {
                UniNodespace.destroyNode(serverLevel, worldPosition, RebarNetworkProvider.THE_PROVIDER);
            }
        }
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    protected void applyClientUpdate(@NotNull CompoundTag tag) {
        if (tag.contains("progress")) this.progress = tag.getInt("progress");
        if (tag.contains("block")) readNbtData(tag, null);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        this.progress = nbt.getInt("progress");
        this.hasConnection = nbt.getBoolean("hasConnection");

        if (nbt.contains("block")) {
            ResourceLocation id = ResourceLocation.tryParse(nbt.getString("block"));
            this.concrete = id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
            this.concreteMeta = nbt.getInt("meta");
        }
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
        nbt.putInt("progress", this.progress);
        nbt.putBoolean("hasConnection", this.hasConnection);

        if (this.concrete != null) {
            nbt.putString("block", BuiltInRegistries.BLOCK.getKey(this.concrete).toString());
            nbt.putInt("meta", this.concreteMeta);
        }
    }

    public RebarNode createNode() {
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        return new RebarNode(RebarNetworkProvider.THE_PROVIDER, worldPosition).setConnections(
                new NodeDirPos(x + 1, y, z, Direction.EAST),
                new NodeDirPos(x - 1, y, z, Direction.WEST),
                new NodeDirPos(x, y + 1, z, Direction.UP),
                new NodeDirPos(x, y - 1, z, Direction.DOWN),
                new NodeDirPos(x, y, z + 1, Direction.SOUTH),
                new NodeDirPos(x, y, z - 1, Direction.NORTH)
        );
    }

    @Override
    public FluidTank[] getAllTanks() {
        FluidTank tank = new FluidTank(concreteFluid(), 1_000);
        tank.setFill(progress);
        return new FluidTank[] {tank};
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (type != concreteFluid()) return amount;
        if (this.node == null || this.node.expired || !this.node.hasValidNet()) return amount;

        List<RebarBlockEntity> lowestLinks = new ArrayList<>();
        int lowestY = Integer.MAX_VALUE;
        int progress = 0;
        int capacity = 0;

        for (RebarNode node : this.node.net.links) {
            BlockPos p = node.positions.get(0); //rebar can only have one pos, there's no multiblock rebar
            int y = p.getY();

            if (y < lowestY) {
                lowestY = y;
                progress = 0;
                capacity = 0;
                lowestLinks.clear();
            }

            if (y == lowestY) {
                BlockEntity tile = level.getBlockEntity(p);
                if (!(tile instanceof RebarBlockEntity rebar)) continue;

                progress += rebar.progress;
                capacity += 1_000;
                lowestLinks.add(rebar);
            }
        }

        if (capacity > 0 && !lowestLinks.isEmpty()) {
            int maxSpeed = 50;
            int maxAccept = (int) Math.min(Math.min(capacity - progress, amount), (long) maxSpeed * lowestLinks.size());
            int target = Math.min((progress + maxAccept) / lowestLinks.size(), 1_000);

            for (RebarBlockEntity rebar : lowestLinks) {
                if (rebar.progress >= target) continue;
                int delta = target - rebar.progress;
                if (delta > amount) continue;

                rebar.progress += delta;
                amount -= delta;
            }
        }

        return amount;
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        return 10_000;
    }
}
