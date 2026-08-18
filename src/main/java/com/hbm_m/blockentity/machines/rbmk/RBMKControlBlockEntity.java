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

    // 1:1 with the original's TileEntityRBMKControl (level defaults to 0, i.e. fully inserted/
    // SCRAMMED): a freshly placed reactor should start subcritical-safe, not with every rod
    // already withdrawn. Also fixes the control-rod cap renderer, which floated a full block
    // above the column by default when level started at 1.
    public double level       = 0.0;
    public double targetLevel = 0.0;
    public double lastLevel   = 0.0;
    public static final double SPEED = 0.00277;

    /** Color group for console control (-1 = ungrouped, 0-4 = groups). */
    public short color = -1;

    protected RBMKControlBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // 1:1 with the original's RBMKBase.hasOwnLid(): control rod variants never get the generic
    // cover/glass lid textures at all (no rbmk_control_cover_top/_glass_top assets exist) - the
    // animated rod cap itself is their "lid". Without this override, RBMKColumnRenderer's lid-
    // aware top-texture selection (added for the other column types) would try to load those
    // missing textures for every control rod and render a missing-texture checkerboard.
    @Override public boolean   hasLid()          { return false; }
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

    /**
     * 1:1 with the original's {@code TileEntityRBMKControl.onMelt}: 2-3 GRAPHITE debris if
     * moderated, plus 2-3 ROD debris always, before the standard melt. Was entirely missing -
     * control rods melted down without dropping any debris at all.
     */
    @Override
    public void onMelt(Level level, int reduce) {
        if (isModerated()) {
            int graphiteCount = 2 + level.random.nextInt(2);
            for (int i = 0; i < graphiteCount; i++) spawnDebris(level, "graphite");
        }
        int rodCount = 2 + level.random.nextInt(2);
        for (int i = 0; i < rodCount; i++) spawnDebris(level, "rod");
        standardMelt(level, reduce);
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
        tag.putDouble("lastLevel", lastLevel);
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
        // lastLevel was never sent to the client before (this same saveAdditional/load pair
        // backs both disk NBT and the per-tick network sync packet) - the renderer's
        // level/lastLevel partial-tick interpolation was lerping toward a client-side value
        // that never updated, then snapping back to it at every tick boundary. That snap-back,
        // repeated 20x/second, is the "twerking" bounce.
        lastLevel   = tag.contains("lastLevel") ? tag.getDouble("lastLevel") : level;
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

