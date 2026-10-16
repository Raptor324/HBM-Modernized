package com.hbm_m.blockentity.machines;

import com.hbm_m.api.block.ICrucibleAcceptor;
import com.hbm_m.block.ModBlocks;
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

/**
 * Port of the 1.7.10 BlockDynamicSlag.TileEntitySlag - a world-placed "puddle" of molten material
 * dumped by a {@link MachineFoundrySlagtapBlockEntity} when its downstream target is full/missing.
 * Holds a single {@link MaterialType} + amount, up to 16 blocks' worth (original maxAmount =
 * MaterialShapes.BLOCK.q(16) = 144 000 mB).
 * <p>
 * Behaviour is 1:1 with the original updateTick (see {@link #tick}): flow down into a replaceable
 * block, merge into a same-material puddle below, or spread sideways once at least a fifth full.
 * The original drives this via scheduled block updates; here it's polled every
 * {@link #TICK_INTERVAL} ticks by the block's ticker, which replicates the same progression.
 */
public class SlagBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements ICrucibleAcceptor {

    /** Оригинал: BLOCK.q(16) = 648 × 16 = 10368 квантов = 144 000 mB (16 блоков). */
    public static final int MAX_AMOUNT = 144_000;

    /** Период опроса тикера (в оригинале — запланированные обновления блока). */
    public static final int TICK_INTERVAL = 10;

    @Nullable public MaterialType type = null;
    public int amount = 0;

    public SlagBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SLAG_BE.get(), pos, state);
    }

    public boolean isFull() {
        return amount >= MAX_AMOUNT;
    }

    @Nullable
    public MaterialStack tryAdd(MaterialStack stack) {
        if (type != null && type != stack.type && amount > 0) return stack;

        type = stack.type;
        int space = MAX_AMOUNT - amount;
        int toAdd = Math.min(space, stack.amount);
        amount += toAdd;
        stack.amount -= toAdd;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        return stack.amount > 0 ? stack : null;
    }

    /* ── tick: оригинальный BlockDynamicSlag.updateTick ─────────────────── */

    public static void tick(Level level, BlockPos pos, BlockState state, SlagBlockEntity be) {
        if (level.isClientSide || be.amount <= 0) return;
        if (level.getGameTime() % TICK_INTERVAL != 0) return;

        BlockPos belowPos = pos.below();

        /* 1) Flow down: блок снизу заменим — переносим всю лужу вниз (ориг. "flow down") */
        if (pos.getY() > 0 && level.getBlockState(belowPos).canBeReplaced()) {
            level.setBlock(belowPos, ModBlocks.SLAG_DYNAMIC.get().defaultBlockState(), 3);
            if (level.getBlockEntity(belowPos) instanceof SlagBlockEntity below) {
                below.type = be.type;
                below.amount = be.amount;
                below.setChanged();
                level.sendBlockUpdated(belowPos, below.getBlockState(), below.getBlockState(), 3);
            }
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            return;
        }

        /* 2) Merge down: такая же лужа снизу — перелить, сколько влезет (ориг. "merge") */
        if (level.getBlockEntity(belowPos) instanceof SlagBlockEntity below
                && below.type == be.type && below.amount < MAX_AMOUNT) {
            int transfer = Math.min(MAX_AMOUNT - below.amount, be.amount);
            below.amount += transfer;
            be.amount -= transfer;

            if (be.amount <= 0) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            } else {
                be.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
            }
            below.setChanged();
            level.sendBlockUpdated(belowPos, below.getBlockState(), below.getBlockState(), 3);
            return;
        }

        /* 3) Spread: заполнение >= maxAmount/5 и есть заменимые соседи (ориг. "flow sideways") */
        Direction[] sides = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
        int count = 0;
        for (Direction dir : sides) {
            if (level.getBlockState(pos.relative(dir)).canBeReplaced()) count++;
        }

        if (be.amount >= MAX_AMOUNT / 5 && count > 0) {
            int toSpread = Math.max(be.amount / (count * 2), 1);

            for (Direction dir : sides) {
                BlockPos sidePos = pos.relative(dir);
                if (!level.getBlockState(sidePos).canBeReplaced()) continue;

                level.setBlock(sidePos, ModBlocks.SLAG_DYNAMIC.get().defaultBlockState(), 3);
                if (level.getBlockEntity(sidePos) instanceof SlagBlockEntity tile) {
                    tile.type = be.type;
                    tile.amount = toSpread;
                    tile.setChanged();
                    level.sendBlockUpdated(sidePos, tile.getBlockState(), tile.getBlockState(), 3);
                }
                be.amount -= toSpread;
                level.sendBlockUpdated(pos, state, state, 3);
            }
            be.setChanged();
        }
    }

    /* ── ICrucibleAcceptor ──────────────────────────────────────────────── */

    @Override
    public boolean canAcceptPartialPour(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return side == Direction.UP && !isFull() && (type == null || type == stack.type);
    }

    @Override
    public @Nullable MaterialStack pour(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return tryAdd(stack);
    }

    @Override
    public boolean canAcceptPartialFlow(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return !isFull() && (type == null || type == stack.type);
    }

    @Override
    public @Nullable MaterialStack flow(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        return tryAdd(stack);
    }

    /* ── NBT / sync ─────────────────────────────────────────────────────── */

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        if (type != null) tag.putString("mat_type", type.name);
        tag.putInt("mat_amount", amount);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        type = tag.contains("mat_type") ? MaterialType.byName(tag.getString("mat_type")) : null;
        amount = tag.getInt("mat_amount");
    }

}
