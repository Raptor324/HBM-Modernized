package com.hbm_m.blockentity.machines;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.block.ICrucibleAcceptor;
import com.hbm_m.api.foundry.FoundryNetworkProvider;
import com.hbm_m.api.foundry.FoundryNode;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.util.CrucibleUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityFoundryChannel}: Giessrinne mit 2 Barren Fassung. Alle 5 Ticks gibt sie an einen benachbarten
 * Abnehmer (keine Rinne) ab, sonst gleicht sie sich mit Nachbarrinnen aus (1:4 Tausch, sonst Halbierung). Die Rinnen
 * bilden ein Netz; gegossen werden darf nur, wenn kein Knoten ein anderes Material fuehrt.
 */
public class MachineFoundryChannelBlockEntity extends MachineFoundryBaseBlockEntity {

    public int nextUpdate;
    public int lastFlow = 0;
    protected FoundryNode node;

    public MachineFoundryChannelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_CHANNEL_BE.get(), pos, state);
    }

    @Override
    public void updateEntity() {

        if (level instanceof ServerLevel world) {

            initNode();

            if (this.node.type != null && this.amount == 0) {
                this.node.type = null;
            }

            if (this.type == null && this.amount != 0) {
                this.amount = 0;
            }

            nextUpdate--;

            if (nextUpdate <= 0 && this.amount > 0 && this.type != null) {

                boolean hasOp = false;
                nextUpdate = 5;

                List<Integer> ints = new ArrayList<>(List.of(2, 3, 4, 5));
                Collections.shuffle(ints);
                if (lastFlow > 0) {
                    ints.remove((Integer) this.lastFlow);
                    ints.add(this.lastFlow);
                }

                for (Integer i : ints) {
                    Direction dir = Direction.from3DDataValue(i);
                    BlockPos np = worldPosition.relative(dir);
                    BlockEntity nb = world.getBlockEntity(np);
                    if (nb instanceof MachineFoundryChannelBlockEntity) continue;
                    ICrucibleAcceptor acc = CrucibleUtil.acceptorAt(world, np);

                    if (acc != null) {
                        if (acc.canAcceptPartialFlow(world, np, dir.getOpposite(), new MaterialStack(this.type, this.amount))) {
                            MaterialStack left = acc.flow(world, np, dir.getOpposite(), new MaterialStack(this.type, this.amount));
                            if (left == null) {
                                this.type = null;
                                this.amount = 0;
                                this.node.type = null;
                            } else {
                                this.amount = left.amount;
                            }
                            hasOp = true;
                            break;
                        }
                    }
                }

                if (!hasOp) {
                    for (Integer i : ints) {
                        Direction dir = Direction.from3DDataValue(i);
                        BlockEntity b = world.getBlockEntity(worldPosition.relative(dir));

                        if (b instanceof MachineFoundryChannelBlockEntity acc) {

                            if (acc.type == null || acc.type == this.type || acc.amount == 0) {

                                acc.type = this.type;
                                acc.initNode();
                                acc.node.type = this.type;
                                acc.lastFlow = dir.getOpposite().get3DDataValue();

                                if (world.random.nextInt(5) == 0 || this.amount == 1) { //force swap operations with single quanta to keep them moving
                                    //1:4 chance that the fill states are simply swapped
                                    int buf = this.amount;
                                    this.amount = acc.amount;
                                    acc.amount = buf;

                                } else {
                                    //otherwise, equalize the neighbors
                                    int diff = this.amount - acc.amount;

                                    if (diff > 0) {
                                        diff /= 2;
                                        this.amount -= diff;
                                        acc.amount += diff;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (this.amount == 0) {
                this.lastFlow = 0;
                this.nextUpdate = 5;
            }
        }

        super.updateEntity();
    }

    protected void initNode() {
        if (!(level instanceof ServerLevel world)) return;
        if (this.node == null || this.node.expired) {
            this.node = (FoundryNode) (Object) UniNodespace.getNode(world, worldPosition, FoundryNetworkProvider.THE_PROVIDER);

            if (this.node == null || this.node.expired) {
                this.node = this.createNode();
                this.node.type = this.type;
                UniNodespace.createNode(world, this.node);
            }
        }
    }

    @Override
    public int getCapacity() {
        return MaterialShapes.INGOT.q(2);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        this.lastFlow = nbt.getByte("flow");
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putByte("flow", (byte) this.lastFlow);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel world && this.node != null) {
            UniNodespace.destroyNode(world, worldPosition, FoundryNetworkProvider.THE_PROVIDER);
        }
    }

    public FoundryNode createNode() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new FoundryNode(FoundryNetworkProvider.THE_PROVIDER, worldPosition).setConnections(
                new NodeDirPos(new BlockPos(x + 1, y, z), Direction.EAST),
                new NodeDirPos(new BlockPos(x - 1, y, z), Direction.WEST),
                new NodeDirPos(new BlockPos(x, y, z + 1), Direction.SOUTH),
                new NodeDirPos(new BlockPos(x, y, z - 1), Direction.NORTH)
        );
    }

    @Override
    public boolean canAcceptPartialPour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, MaterialStack stack) {

        if (this.node == null || !this.node.hasValidNet()) return false;

        for (FoundryNode node : this.node.net.links) {
            if (node.type != null && node.type != stack.material) {
                return false;
            }
        }

        return super.canAcceptPartialPour(world, pos, dX, dY, dZ, side, stack);
    }

    @Override
    @Nullable
    public MaterialStack flow(Level world, BlockPos pos, Direction side, MaterialStack stack) {
        if (this.node != null) this.node.type = stack.material;
        return super.flow(world, pos, side, stack);
    }

    @Override
    @Nullable
    public MaterialStack pour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, MaterialStack stack) {
        if (this.node != null) this.node.type = stack.material;
        return super.pour(world, pos, dX, dY, dZ, side, stack);
    }
}
