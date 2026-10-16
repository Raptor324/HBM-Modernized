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
import java.util.List;

/**
 * Port of the 1.7.10 TileEntityFoundryTank - pure bulk storage/spreader, no mold/casting. Only
 * accepts material poured from directly above (crucible/outlet); every few ticks it either drains
 * into a tank directly below, flows sideways into any neighbouring {@link ICrucibleAcceptor} (except
 * a foundry channel), or equalizes/swaps amounts with an adjacent tank - 1:1 with the original's
 * three-tier fallback.
 */
public class MachineFoundryTankBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements ICrucibleAcceptor {

    public static final int CAPACITY = MaterialStack.BUCKET * 4;

    @Nullable public MaterialType type = null;
    public int amount = 0;
    private int nextUpdate = 0;

    public MachineFoundryTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_TANK_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFoundryTankBlockEntity be) {
        if (level.isClientSide) return;

        if (be.type == null && be.amount != 0) be.amount = 0;

        be.nextUpdate--;
        if (be.nextUpdate > 0 || be.amount <= 0 || be.type == null) return;
        be.nextUpdate = level.random.nextInt(6) + 5;

        boolean hasOp = false;

        // 1) drain down into a tank directly below
        if (level.getBlockEntity(pos.below()) instanceof MachineFoundryTankBlockEntity below) {
            if ((below.type == null || below.type == be.type) && below.amount < CAPACITY) {
                below.type = be.type;
                int toFill = Math.min(be.amount, CAPACITY - below.amount);
                be.amount -= toFill;
                below.amount += toFill;
                below.setChanged();
                hasOp = true;
            }
        }

        List<Direction> horizontal = new ArrayList<>(List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST));
        shuffle(horizontal, level.random);

        // 2) sideways flow into a non-tank, non-channel ICrucibleAcceptor
        if (!hasOp) {
            for (Direction dir : horizontal) {
                BlockPos neighborPos = pos.relative(dir);
                if (level.getBlockEntity(neighborPos) instanceof MachineFoundryChannelBlockEntity) continue;
                if (!(level.getBlockEntity(neighborPos) instanceof ICrucibleAcceptor acc)) continue;
                if (acc instanceof MachineFoundryTankBlockEntity) continue;

                MaterialStack toSend = new MaterialStack(be.type, be.amount);
                if (!acc.canAcceptPartialFlow(level, neighborPos, dir.getOpposite(), toSend)) continue;

                MaterialStack left = acc.flow(level, neighborPos, dir.getOpposite(), toSend);
                if (left == null) {
                    be.type = null;
                    be.amount = 0;
                } else {
                    be.amount = left.amount;
                }
                hasOp = true;
                break;
            }
        }

        // 3) equalize/swap with an adjacent tank
        if (!hasOp) {
            for (Direction dir : horizontal) {
                if (!(level.getBlockEntity(pos.relative(dir)) instanceof MachineFoundryTankBlockEntity neighbor)) continue;
                if (neighbor.type != null && neighbor.type != be.type && neighbor.amount != 0) continue;

                neighbor.type = be.type;
                if (level.random.nextInt(5) == 0) {
                    int buf = be.amount;
                    be.amount = neighbor.amount;
                    neighbor.amount = buf;
                } else {
                    int diff = (be.amount - neighbor.amount) / 2;
                    if (diff > 0) {
                        be.amount -= diff;
                        neighbor.amount += diff;
                    }
                }
                neighbor.setChanged();
            }
        }

        be.setChanged();
    }

    private static <T> void shuffle(List<T> list, net.minecraft.util.RandomSource random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            T tmp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, tmp);
        }
    }

    /* ── ICrucibleAcceptor ──────────────────────────────────────────────── */

    @Override
    public boolean canAcceptPartialPour(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        if (side != Direction.UP) return false;
        if (type != null && type != stack.type && amount > 0) return false;
        return amount < CAPACITY;
    }

    @Override
    public @Nullable MaterialStack pour(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        type = stack.type;
        int space = CAPACITY - amount;
        int toAdd = Math.min(space, stack.amount);
        amount += toAdd;
        stack.amount -= toAdd;
        setChanged();
        return stack.amount > 0 ? stack : null;
    }

    /* Tanks never accept sideways flow (matches the original's block-level canAcceptPartialFlow=false). */
    @Override public boolean canAcceptPartialFlow(Level level, BlockPos pos, Direction side, MaterialStack stack) { return false; }
    @Override public @Nullable MaterialStack flow(Level level, BlockPos pos, Direction side, MaterialStack stack) { return stack; }

    /* ── NBT / sync ─────────────────────────────────────────────────────── */

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        if (type != null) tag.putString("mat_type", type.name);
        tag.putInt("mat_amount", amount);
        tag.putInt("nextUpdate", nextUpdate);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        type = tag.contains("mat_type") ? MaterialType.byName(tag.getString("mat_type")) : null;
        amount = tag.getInt("mat_amount");
        nextUpdate = tag.getInt("nextUpdate");
    }

}
