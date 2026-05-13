package com.hbm_m.block.entity.machines;

import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BlockEntity for the Cooling Tower multiblock (WIP).
 */
public class MachineCoolingTowerBlockEntity extends BaseMachineBlockEntity {

    public MachineCoolingTowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COOLING_TOWER_BE.get(), pos, state, 0, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCoolingTowerBlockEntity be) {
        // WIP - keine Logik vorhanden
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.cooling_tower");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        // No inventory slots are exposed for this WIP machine.
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        // WIP - kein Menü
        return null;
    }
}
