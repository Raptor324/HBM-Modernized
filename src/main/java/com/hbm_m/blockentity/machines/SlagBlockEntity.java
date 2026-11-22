package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.material.NTMMaterial;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code BlockDynamicSlag.TileEntitySlag}: Schlackepfuetze aus einem Material, bis 16 Bloecke Menge; die Hoehe
 * des Blocks folgt der Menge. Fliessen/Zerlaufen macht der Block per geplantem Tick.
 */
public class SlagBlockEntity extends BaseHbmBlockEntity {

    public static final int maxAmount = MaterialShapes.BLOCK.q(16);
    public static final int MAX_AMOUNT = maxAmount;
    public static final int TICK_INTERVAL = 10;

    public NTMMaterial mat;
    public int amount;

    public SlagBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SLAG_BE.get(), pos, state);
    }

    public boolean isFull() {
        return amount >= maxAmount;
    }

    public void markForUpdate() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Nullable
    public MaterialStack tryAdd(MaterialStack stack) {
        if (mat != null && mat != stack.material && amount > 0) return stack;
        mat = stack.material;
        int space = maxAmount - amount;
        int toAdd = Math.min(space, stack.amount);
        amount += toAdd;
        stack.amount -= toAdd;
        markForUpdate();
        return stack.amount > 0 ? stack : null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SlagBlockEntity be) {
        if (level.isClientSide || be.amount <= 0) return;
        if (level.getGameTime() % TICK_INTERVAL != 0) return;

        BlockPos belowPos = pos.below();

        /* 1) Flow down */
        if (pos.getY() > 0 && level.getBlockState(belowPos).canBeReplaced()) {
            level.setBlock(belowPos, ModBlocks.SLAG_DYNAMIC.get().defaultBlockState(), 3);
            if (level.getBlockEntity(belowPos) instanceof SlagBlockEntity below) {
                below.mat = be.mat;
                below.amount = be.amount;
                below.markForUpdate();
            }
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            return;
        }

        /* 2) Merge down */
        if (level.getBlockEntity(belowPos) instanceof SlagBlockEntity below
                && below.mat == be.mat && below.amount < maxAmount) {
            int transfer = Math.min(maxAmount - below.amount, be.amount);
            below.amount += transfer;
            be.amount -= transfer;

            if (be.amount <= 0) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            } else {
                be.markForUpdate();
            }
            below.markForUpdate();
            return;
        }

        /* 3) Spread */
        Direction[] sides = { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
        int count = 0;
        for (Direction dir : sides) {
            if (level.getBlockState(pos.relative(dir)).canBeReplaced()) count++;
        }

        if (be.amount >= maxAmount / 5 && count > 0) {
            int toSpread = Math.max(be.amount / (count * 2), 1);

            for (Direction dir : sides) {
                BlockPos sidePos = pos.relative(dir);
                if (!level.getBlockState(sidePos).canBeReplaced()) continue;

                level.setBlock(sidePos, ModBlocks.SLAG_DYNAMIC.get().defaultBlockState(), 3);
                if (level.getBlockEntity(sidePos) instanceof SlagBlockEntity tile) {
                    tile.mat = be.mat;
                    tile.amount = toSpread;
                    tile.markForUpdate();
                }
                be.amount -= toSpread;
                be.markForUpdate();
            }
        }
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        this.mat = nbt.contains("mat") ? Mats.matById.get(nbt.getInt("mat")) : null;
        this.amount = nbt.getInt("amount");
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        if (this.mat != null) nbt.putInt("mat", this.mat.id);
        nbt.putInt("amount", this.amount);
    }
}
