package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.BarrelTankBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.menu.BarrelIronMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Small single-block iron fluid barrel — same logic as {@link MachineFluidTankBlockEntity},
 * just a much smaller capacity and no multiblock structure.
 * <p>
 * Iron can't hold any corrosive fluid at all (unlike Steel, which tolerates regular corrosive
 * fluids fine): pouring one in turns the block into a {@link BarrelCorrodedBlockEntity} in place,
 * carrying over the tank contents, matching the original mod's "corroded barrel" concept.
 */
public class BarrelIronBlockEntity extends MachineFluidTankBlockEntity {

    public static final int CAPACITY = 16_000;

    public BarrelIronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BARREL_IRON_BE.get(), pos, state, CAPACITY);
    }

    @Override
    protected boolean canStoreHot() { return false; }

    @Override
    protected boolean canStoreCorrosive() { return false; }

    @Override
    protected boolean canStoreHighlyCorrosive() { return false; }

    @Override
    protected boolean handleIncompatibleFluid(FluidType ftype) {
        if (ftype.isCorrosive()) {
            replaceWithCorroded();
            return true;
        }
        return super.handleIncompatibleFluid(ftype);
    }

    /** Swaps this block for {@code barrel_corroded} in place, carrying the tank contents over. */
    private void replaceWithCorroded() {
        if (level == null) return;

        BlockState oldState = getBlockState();
        BlockState newState = ModBlocks.BARREL_CORRODED.get().defaultBlockState();
        if (newState.hasProperty(BarrelTankBlock.FACING) && oldState.hasProperty(BarrelTankBlock.FACING)) {
            newState = newState.setValue(BarrelTankBlock.FACING, oldState.getValue(BarrelTankBlock.FACING));
        }

        CompoundTag savedTag = saveWithoutMetadata();
        level.setBlock(worldPosition, newState, 3);

        BlockEntity newBe = level.getBlockEntity(worldPosition);
        if (newBe instanceof BarrelCorrodedBlockEntity corroded) {
            corroded.load(savedTag);
            corroded.hasExploded = true;
            corroded.setChanged();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(ModBlocks.BARREL_IRON.get().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BarrelIronMenu(id, inventory, this, this.data);
    }
}
