package com.hbm_m.block;

import com.hbm_m.blockentity.BaseMachineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Drops a machine's inventory when its block is removed.
 *
 * <p>The original does this for every machine: {@code BlockDummyable.breakBlock} walks the tile
 * entity's {@code ISidedInventory} and {@code BlockCraneBase.breakBlock} does the same for the
 * crane family. Blocks here that never overrode {@code onRemove}, or overrode it only to tear down
 * their multiblock, destroyed everything the machine was holding.
 *
 * <p>{@link BaseMachineBlockEntity#dropInventoryContents()} already refuses to drop while a
 * contraption move window is open, so this is safe to call from {@code onRemove}.</p>
 */
public final class MachineDrops {

    private MachineDrops() {}

    public static void dropInventory(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BaseMachineBlockEntity machine) {
            machine.dropInventoryContents();
            return;
        }
        // Machines on BaseHbmBlockEntity keep their handler private under different field names;
        // go through the capability instead. Same contraption guard as dropInventoryContents.
        if (com.hbm_m.multiblock.ContraptionAssemblyGuard.isMoving()) return;
        var handler = com.hbm_m.api.item.ItemHandlerAccess.getItemHandler(level, pos, null);
        if (handler == null) return;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            net.minecraft.world.item.ItemStack stack = handler.extractItem(slot, Integer.MAX_VALUE, false);
            if (!stack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            }
        }
    }
}
