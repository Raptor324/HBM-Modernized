package com.hbm_m.blockentity.crates;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.crates.CrateType;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.SafeMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code TileEntitySafe}: 15 Plaetze. */
public class SafeBlockEntity extends BaseCrateBlockEntity {

    public SafeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SAFE_BE.get(), pos, state, CrateType.SAFE.getSlotCount());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.safe");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SafeMenu(containerId, playerInventory, this);
    }
}
