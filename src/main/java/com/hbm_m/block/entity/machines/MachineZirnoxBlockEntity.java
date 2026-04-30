package com.hbm_m.block.entity.machines;

import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineZirnoxMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class MachineZirnoxBlockEntity extends BaseMachineBlockEntity {

    public static final long TANK_MAX = 10000;

    public long steam = 0;
    public long carbonDioxide = 0;
    public long water = 0;

    public long heat = 0;
    public long pressure = 0;
    public boolean isOn = false;

    public MachineZirnoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ZIRNOX_BE.get(), pos, state, 0, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineZirnoxBlockEntity blockEntity) {
        if (!level.isClientSide) {
            blockEntity.update(level);
        }
    }

    private void update(Level level) {
        // Reactor simulation logic
    }

    public int getGaugeScaled(int scale, int type) {
        switch (type) {
            case 0:
                return (int) (steam * scale / TANK_MAX);
            case 1:
                return (int) (carbonDioxide * scale / TANK_MAX);
            case 2:
                return (int) (water * scale / TANK_MAX);
            case 3:
                return (int) Math.min((heat * scale / 780000L), scale);
            case 4:
                return (int) Math.min((pressure * scale / 300000L), scale);
            default:
                return 0;
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong("steam", steam);
        tag.putLong("co2", carbonDioxide);
        tag.putLong("water", water);
        tag.putLong("heat", heat);
        tag.putLong("pressure", pressure);
        tag.putBoolean("isOn", isOn);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        steam = tag.getLong("steam");
        carbonDioxide = tag.getLong("co2");
        water = tag.getLong("water");
        heat = tag.getLong("heat");
        pressure = tag.getLong("pressure");
        isOn = tag.getBoolean("isOn");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.zirnox");
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
        return MachineZirnoxMenu.create(id, inventory, this);
    }
}
