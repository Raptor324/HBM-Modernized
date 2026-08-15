package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.menu.BarrelPlasticMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * "Safe Barrel" - the original mod's plastic barrel: cannot hold hot or corrosive fluids of any
 * kind (unlike Iron, which only rejects corrosive). 1:1 port of {@code TileEntityBarrel
 * #checkFluidInteraction}'s {@code barrel_plastic} branch: instead of slowly leaking like the
 * corroded barrel, it's destroyed outright (no drops) with a fizz sound the instant an
 * incompatible fluid touches it.
 */
public class BarrelPlasticBlockEntity extends MachineFluidTankBlockEntity {

    public static final int CAPACITY = 12_000;

    public BarrelPlasticBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BARREL_PLASTIC_BE.get(), pos, state, CAPACITY);
    }

    @Override
    protected boolean canStoreHot() { return false; }

    @Override
    protected boolean canStoreCorrosive() { return false; }

    @Override
    protected boolean canStoreHighlyCorrosive() { return false; }

    @Override
    protected boolean handleIncompatibleFluid(FluidType ftype) {
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.destroyBlock(worldPosition, false);
        }
        return true;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(ModBlocks.BARREL_PLASTIC.get().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BarrelPlasticMenu(id, inventory, this, this.data);
    }
}
