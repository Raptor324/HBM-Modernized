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
 * the original's in-game tooltip: 16,000 mB; can store hot, (regular) and highly corrosive fluids
 * (the tooltip only warns it cannot store them "properly" — no code reaction, NEXT parity),
 * cannot store antimatter.
 */
public class BarrelSteelBlockEntity extends MachineFluidTankBlockEntity {

    public static final int CAPACITY = 16_000;

    public BarrelSteelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BARREL_STEEL_BE.get(), pos, state, CAPACITY);
    }

    /** NEXT BlockEntityBarrel: steel/tcalloy/antimatter хранят любые едкие жидкости без последствий. */
    @Override
    protected boolean canStoreHighlyCorrosive() { return true; }

    @Override
    public Component getDisplayName() {
        // Паритет NEXT BlockEntityBarrel.getDefaultName: у всех бочек общий заголовок GUI
        // "Barrel" (container.hbm.barrel); имя материала остаётся у блока/предмета.
        return Component.translatable("container.hbm_m.barrel");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BarrelSteelMenu(id, inventory, this, this.data);
    }
}
