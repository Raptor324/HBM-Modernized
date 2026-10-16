package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.BarrelSteelMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Small single-block steel fluid barrel — same logic as {@link MachineFluidTankBlockEntity},
 * just a much smaller capacity and no multiblock structure. Capacity/capability confirmed against
 * the original's in-game tooltip: 16,000 mB; can store hot and (regular) corrosive fluids, but not
 * highly corrosive ones "properly"; cannot store antimatter (matches this class's inherited
 * defaults from {@link MachineFluidTankBlockEntity} - no overrides needed here).
 */
public class BarrelSteelBlockEntity extends MachineFluidTankBlockEntity {

    public static final int CAPACITY = 16_000;

    public BarrelSteelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BARREL_STEEL_BE.get(), pos, state, CAPACITY);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(ModBlocks.BARREL_STEEL.get().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BarrelSteelMenu(id, inventory, this, this.data);
    }
}
