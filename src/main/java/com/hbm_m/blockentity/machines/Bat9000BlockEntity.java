package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.Bat9000Menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BAT9000 — a large-capacity variant of the Fluid Tank multiblock. Identical logic to
 * {@link MachineFluidTankBlockEntity} (fill/drain, mode, antimatter/corrosive explosion, leaking,
 * MK2 network participation); only the capacity and registration differ.
 */
public class Bat9000BlockEntity extends MachineFluidTankBlockEntity {

    public static final int CAPACITY = 2_450_000;

    public Bat9000BlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BAT9000_BE.get(), pos, state, CAPACITY);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(ModBlocks.BAT9000.get().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new Bat9000Menu(id, inventory, this, this.data);
    }
}
