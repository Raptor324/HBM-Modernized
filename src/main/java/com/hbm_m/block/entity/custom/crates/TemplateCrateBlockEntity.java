package com.hbm_m.block.entity.custom.crates;

import com.hbm_m.block.custom.machines.crates.CrateType;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.menu.TemplateCrateMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class TemplateCrateBlockEntity extends BaseCrateBlockEntity {

    public TemplateCrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEMPLATE_CRATE_BE.get(), pos, state, CrateType.TEMPLATE.getSlotCount());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.crate_template");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new TemplateCrateMenu(containerId, playerInventory, this);
    }
}
