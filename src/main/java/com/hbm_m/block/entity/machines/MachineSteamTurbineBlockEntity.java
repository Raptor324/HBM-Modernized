package com.hbm_m.block.entity.machines;

import org.jetbrains.annotations.Nullable;
import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MachineSteamTurbineBlockEntity extends BaseMachineBlockEntity {
    public MachineSteamTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_TURBINE_BE.get(), pos, state, 4, 1_000_000L, 0L, 50_000L);
    }
    public static void tick(Level level, BlockPos pos, BlockState state, MachineSteamTurbineBlockEntity be) {
        if (level.isClientSide()) return;
        be.ensureNetworkInitialized();
        be.setChanged();
    }
    @Override public Component getDisplayName() { return Component.translatable("container.hbm_m.steam_turbine"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return null; }
    @Override protected Component getDefaultName() { return Component.translatable("container.hbm_m.steam_turbine"); }
    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return false; }
}
