package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.BarrelTcalloyMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Small single-block technetium-alloy fluid barrel — same logic as {@link MachineFluidTankBlockEntity},
 * just a much smaller capacity and no multiblock structure. Port of 1.7.10's {@code BlockFluidBarrel}
 * for {@code barrel_tcalloy}: unlike {@code barrel_corroded} (whose {@code createNewTileEntity} is
 * explicitly hard-coded to return {@code null} in the original - purely decorative), the tcalloy
 * variant gets a real functional {@code TileEntityBarrel}, identical in behavior to iron/steel.
 * <p>
 * Confirmed against the original's in-game tooltip: 24,000 mB capacity; can store hot and highly
 * corrosive fluids (unlike plastic, which explodes/fizzes on those); cannot store antimatter (no
 * barrel besides {@code barrel_antimatter} itself can, per the original's generic antimatter check).
 */
public class BarrelTcalloyBlockEntity extends MachineFluidTankBlockEntity {

    public static final int CAPACITY = 24_000;

    public BarrelTcalloyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BARREL_TCALLOY_BE.get(), pos, state, CAPACITY);
    }

    @Override
    protected boolean canStoreHighlyCorrosive() { return true; }

    @Override
    public Component getDisplayName() {
        return Component.translatable(ModBlocks.BARREL_TCALLOY.get().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new BarrelTcalloyMenu(id, inventory, this, this.data);
    }
}
