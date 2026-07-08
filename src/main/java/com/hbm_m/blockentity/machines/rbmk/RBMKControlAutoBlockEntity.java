package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.Mth;

/**
 * 1:1 port of TileEntityRBMKControlAuto.
 * Automatically adjusts targetLevel based on column heat using LINEAR / QUAD_UP / QUAD_DOWN functions.
 */
public class RBMKControlAutoBlockEntity extends RBMKControlBlockEntity {

    public boolean moderated  = false;

    public RBMKFunction function  = RBMKFunction.LINEAR;
    public double levelLower = 0.0;    // percent 0-100
    public double levelUpper = 100.0;  // percent 0-100
    public double heatLower  = 100.0;
    public double heatUpper  = 1200.0;

    public RBMKControlAutoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_CONTROL_AUTO_BE.get(), pos, state);
    }

    @Override public boolean isModerated()             { return moderated; }
    @Override public String  getRenderTexturePrefix() { return "rbmk_control_auto"; }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKControlAutoBlockEntity be) {
        baseTick(level, pos, state, be);
        if (!level.isClientSide) {
            be.lastLevel = be.level;
            be.computeTarget();
            be.moveLevelToTarget(level);
        }
    }

    private void computeTarget() {
        double fauxLevel;
        double lo = Math.min(heatLower, heatUpper);
        double hi = Math.max(heatLower, heatUpper);

        if (heat < lo) {
            fauxLevel = levelLower;
        } else if (heat > hi) {
            fauxLevel = levelUpper;
        } else {
            double range = heatUpper - heatLower;
            if (range == 0) {
                fauxLevel = levelLower;
            } else {
                fauxLevel = switch (function) {
                    case LINEAR ->
                        (heat - heatLower) * ((levelUpper - levelLower) / range) + levelLower;
                    case QUAD_UP ->
                        Math.pow((heat - heatLower) / range, 2) * (levelUpper - levelLower) + levelLower;
                    case QUAD_DOWN ->
                        Math.pow((heat - heatUpper) / (heatLower - heatUpper), 2) * (levelLower - levelUpper) + levelUpper;
                };
            }
        }

        // Original uses percentage (0-100) / 100 = 0-1
        this.targetLevel = Mth.clamp(fauxLevel * 0.01, 0.0, 1.0);
    }

    // ─── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("function",    function.ordinal());
        tag.putDouble("levelLower", levelLower);
        tag.putDouble("levelUpper", levelUpper);
        tag.putDouble("heatLower",  heatLower);
        tag.putDouble("heatUpper",  heatUpper);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("function"))
            function = RBMKFunction.values()[Mth.clamp(tag.getInt("function"), 0, RBMKFunction.values().length - 1)];
        levelLower = tag.getDouble("levelLower");
        levelUpper = tag.getDouble("levelUpper");
        heatLower  = tag.getDouble("heatLower");
        heatUpper  = tag.getDouble("heatUpper");
    }

    public enum RBMKFunction { LINEAR, QUAD_UP, QUAD_DOWN }
}
