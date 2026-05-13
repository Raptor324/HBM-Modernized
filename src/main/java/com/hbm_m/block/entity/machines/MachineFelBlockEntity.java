package com.hbm_m.block.entity.machines;

import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineFelMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MachineFelBlockEntity extends BaseMachineBlockEntity {

    private static final int DEFAULT_MAX_PROGRESS = 200;

    private int progress = 0;
    private int maxProgress = DEFAULT_MAX_PROGRESS;
    private boolean active = false;

    public MachineFelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FEL_BE.get(), pos, state, 0, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFelBlockEntity blockEntity) {
        if (!level.isClientSide) {
            blockEntity.serverTick();
        }
    }

    private void serverTick() {
        if (active) {
            progress++;
            if (progress >= maxProgress) {
                progress = 0;
            }
            setChanged();
        }
    }

    public int getProgressScaled(int scale) {
        if (maxProgress <= 0) {
            return 0;
        }
        return progress * scale / maxProgress;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        setChanged();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("progress", progress);
        tag.putInt("max_progress", maxProgress);
        tag.putBoolean("active", active);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("max_progress");
        if (maxProgress <= 0) {
            maxProgress = DEFAULT_MAX_PROGRESS;
        }
        active = tag.getBoolean("active");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.fel");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineFelMenu.create(id, inventory, this);
    }
}