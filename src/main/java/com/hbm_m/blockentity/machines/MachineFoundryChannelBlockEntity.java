package com.hbm_m.blockentity.machines;

import com.hbm_m.api.block.ICrucibleAcceptor;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.MaterialType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Port of the 1.7.10 TileEntityFoundryChannel.
 * Every 5 ticks: first tries to flow the whole content into an adjacent
 * non-channel ICrucibleAcceptor (e.g. a foundry outlet); if none accepts,
 * the content is equalized/swapped with ALL neighbouring channels.
 */
public class MachineFoundryChannelBlockEntity extends BlockEntity implements ICrucibleAcceptor {

    public static final int CAPACITY = MaterialStack.MB_PER_INGOT * 2;

    @Nullable public MaterialType type = null;
    public int amount = 0;
    public int lastFlow = 0;
    private int nextUpdate = 5;

    @Nullable private MaterialType lastSyncType = null;
    private int lastSyncAmount = 0;

    private static final Direction[] H_DIRS = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    public MachineFoundryChannelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_CHANNEL_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFoundryChannelBlockEntity be) {
        if (level.isClientSide) return;

        if (be.type != null && be.amount == 0) be.type = null;
        if (be.type == null && be.amount != 0) be.amount = 0;

        be.nextUpdate--;

        if (be.nextUpdate <= 0 && be.amount > 0 && be.type != null) {

            boolean hasOp = false;
            be.nextUpdate = 5;

            List<Direction> dirs = new ArrayList<>(List.of(H_DIRS));
            Collections.shuffle(dirs);
            if (be.lastFlow > 0) {
                Direction preferred = Direction.from3DDataValue(be.lastFlow);
                dirs.remove(preferred);
                dirs.add(preferred);
            }

            /* Phase 1: flow into an adjacent non-channel acceptor (outlets etc.) */
            for (Direction dir : dirs) {
                BlockPos neighbor = pos.relative(dir);
                BlockEntity te = level.getBlockEntity(neighbor);

                if (te instanceof ICrucibleAcceptor acc && !(te instanceof MachineFoundryChannelBlockEntity)) {

                    MaterialStack offer = new MaterialStack(be.type, be.amount);
                    if (acc.canAcceptPartialFlow(level, neighbor, dir.getOpposite(), offer)) {
                        MaterialStack left = acc.flow(level, neighbor, dir.getOpposite(), offer);
                        if (left == null) {
                            be.type = null;
                            be.amount = 0;
                        } else {
                            be.amount = left.amount;
                        }
                        be.lastFlow = dir.get3DDataValue();
                        hasOp = true;
                        break;
                    }
                }
            }

            /* Phase 2: equalize with ALL neighbouring channels (original: no break) */
            if (!hasOp) {
                for (Direction dir : dirs) {
                    BlockPos neighbor = pos.relative(dir);
                    BlockEntity te = level.getBlockEntity(neighbor);

                    if (te instanceof MachineFoundryChannelBlockEntity acc) {

                        if (acc.type == null || acc.type == be.type || acc.amount == 0) {
                            acc.type = be.type;
                            acc.lastFlow = dir.getOpposite().get3DDataValue();

                            if (level.getRandom().nextInt(5) == 0 || be.amount == 1) {
                                // 1:4 chance (or single quantum): swap fill states to keep material moving
                                int buf = be.amount;
                                be.amount = acc.amount;
                                acc.amount = buf;
                            } else {
                                // otherwise, equalize the neighbours
                                int diff = be.amount - acc.amount;
                                if (diff > 0) {
                                    diff /= 2;
                                    be.amount -= diff;
                                    acc.amount += diff;
                                }
                            }

                            if (be.amount <= 0)  { be.amount = 0;  be.type = null; }
                            if (acc.amount <= 0) { acc.amount = 0; acc.type = null; }

                            acc.setChanged();
                        }
                    }
                }
            }
        }

        if (be.amount == 0) {
            be.lastFlow = 0;
            be.nextUpdate = 5;
        }

        if (be.lastSyncType != be.type || be.lastSyncAmount != be.amount) {
            be.lastSyncType = be.type;
            be.lastSyncAmount = be.amount;
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    public int getCapacity() { return CAPACITY; }

    /* ── ICrucibleAcceptor ──────────────────────────────────────────────── */

    private boolean standardCheck(MaterialStack stack) {
        if (this.type != null && this.type != stack.type && this.amount > 0) return false;
        if (this.amount >= getCapacity()) return false;
        return true;
    }

    private @Nullable MaterialStack standardAdd(MaterialStack stack) {
        this.type = stack.type;

        if (stack.amount + this.amount <= getCapacity()) {
            this.amount += stack.amount;
            setChanged();
            return null;
        }

        int required = getCapacity() - this.amount;
        this.amount = getCapacity();
        stack.amount -= required;
        setChanged();
        return stack;
    }

    @Override
    public boolean canAcceptPartialFlow(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return standardCheck(stack);
    }

    @Override
    public @Nullable MaterialStack flow(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return standardAdd(stack);
    }

    @Override
    public boolean canAcceptPartialPour(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        if (side != Direction.UP) return false;
        return standardCheck(stack);
    }

    @Override
    public @Nullable MaterialStack pour(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return standardAdd(stack);
    }

    /* ── NBT / sync ─────────────────────────────────────────────────────── */

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (type != null) tag.putString("mat_type", type.name);
        tag.putInt("mat_amount", amount);
        tag.putInt("lastFlow", lastFlow);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.saveAdditional(tag, registries);
        if (type != null) tag.putString("mat_type", type.name);
        tag.putInt("mat_amount", amount);
        tag.putInt("lastFlow", lastFlow);
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("mat_type")) type = MaterialType.byName(tag.getString("mat_type"));
        else type = null;
        amount   = tag.getInt("mat_amount");
        lastFlow = tag.getInt("lastFlow");
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);
        if (tag.contains("mat_type")) type = MaterialType.byName(tag.getString("mat_type"));
        else type = null;
        amount   = tag.getInt("mat_amount");
        lastFlow = tag.getInt("lastFlow");
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public CompoundTag getUpdateTag() { CompoundTag t = super.getUpdateTag(); saveAdditional(t); return t; }
    //?} else {
    /*@Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
 CompoundTag t = super.getUpdateTag(registries); saveAdditional(t, registries); return t; 
    }
    *///?}

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
