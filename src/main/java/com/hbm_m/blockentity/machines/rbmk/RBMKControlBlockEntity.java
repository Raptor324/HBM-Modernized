package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.handler.rbmk.RBMKDials;
import com.hbm_m.handler.rbmk.RBMKNeutronHandler.RBMKType;
import com.hbm_m.inventory.menu.RBMKControlMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class RBMKControlBlockEntity extends RBMKColumnBlockEntity implements MenuProvider {

    public double level       = 1.0;
    public double targetLevel = 1.0;
    public double lastLevel   = 1.0;
    public static final double SPEED = 0.00277;

    /** Color group for console control (-1 = ungrouped, 0-4 = groups). */
    public short color = -1;

    protected RBMKControlBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override public boolean   isLidRemovable()  { return false; }
    @Override public RBMKType  getRBMKType()      { return RBMKType.CONTROL_ROD; }
    @Override public ColumnType getConsoleType()  { return ColumnType.CONTROL; }

    public double getMult() { return level; }

    public void setTarget(double target) {
        this.targetLevel = Math.max(0, Math.min(1, target));
    }

    protected void moveLevelToTarget(Level level) {
        double speed = SPEED * RBMKDials.getControlSpeed(level);
        if (this.level < targetLevel)      this.level = Math.min(this.level + speed, targetLevel);
        else if (this.level > targetLevel) this.level = Math.max(this.level - speed, targetLevel);
    }

    @Override public Component getDisplayName() { return Component.translatable("block.hbm_m.rbmk_control"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return new RBMKControlMenu(id, inv, this); }

    @Override
    public CompoundTag getNBTForConsole() {
        CompoundTag d = new CompoundTag();
        d.putDouble("level", level);
        d.putShort("color", color);
        return d;
    }

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putDouble("level", level);
        tag.putDouble("targetLevel", targetLevel);
        tag.putShort("color", color);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.saveAdditional(tag, registries);
        tag.putDouble("level", level);
        tag.putDouble("targetLevel", targetLevel);
        tag.putShort("color", color);
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        level       = tag.getDouble("level");
        targetLevel = tag.getDouble("targetLevel");
        color       = tag.getShort("color");
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);
        level       = tag.getDouble("level");
        targetLevel = tag.getDouble("targetLevel");
        color       = tag.getShort("color");
    
    }
    *///?}
}

