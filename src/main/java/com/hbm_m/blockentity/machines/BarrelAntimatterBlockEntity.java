package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.BarrelAntimatterMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Magnetic Antimatter Container - the sole barrel exempt from the original's generic
 * "any non-antimatter barrel + antimatter fluid = explosion" check (see
 * {@code TileEntityBarrel#checkFluidInteraction}: {@code if(b != ModBlocks.barrel_antimatter && ...)}).
 * Also tolerates hot and highly corrosive fluids fine, matching its in-game tooltip.
 */
public class BarrelAntimatterBlockEntity extends MachineFluidTankBlockEntity {

    public static final int CAPACITY = 16_000;

    public BarrelAntimatterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BARREL_ANTIMATTER_BE.get(), pos, state, CAPACITY);
    }

    @Override
    protected boolean canStoreHighlyCorrosive() { return true; }

    @Override
    protected boolean canStoreAntimatter() { return true; }

    @Override
    public Component getDisplayName() {
        return Component.translatable(ModBlocks.BARREL_ANTIMATTER.get().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BarrelAntimatterMenu(id, inventory, this, this.data);
    }
}
