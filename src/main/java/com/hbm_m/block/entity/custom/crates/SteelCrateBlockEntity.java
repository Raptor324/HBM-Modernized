package com.hbm_m.block.entity.custom.crates;

import com.hbm_m.block.custom.machines.crates.CrateType;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.menu.SteelCrateMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SteelCrateBlockEntity extends BaseCrateBlockEntity {

    public SteelCrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEEL_CRATE_BE.get(), pos, state, CrateType.STEEL.getSlotCount());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.crate_steel");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SteelCrateMenu(containerId, playerInventory, this);
    }
}
