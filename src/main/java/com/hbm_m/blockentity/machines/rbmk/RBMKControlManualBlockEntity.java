package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.rbmk.RBMKDials;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class RBMKControlManualBlockEntity extends RBMKControlBlockEntity {

    public boolean moderated = false;

    /** Snapshot of {@link #level} taken when a withdrawal ({@code setTarget}) begins - drives the surge in {@link #getMult}. */
    private double startingLevel = 1.0;

    public RBMKControlManualBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_CONTROL_BE.get(), pos, state);
    }

    @Override
    public boolean isModerated() { return moderated; }

    /**
     * Moderated (graphite-tipped) control rods use their own dedicated body texture set
     * ({@code rbmk_control_mod_*}, already shipped as assets) rather than sharing the plain
     * {@code rbmk_control_*} set - confirmed against a second, independent modern port (the
     * 1.18.2 community remake dedicates a whole separate texture to its moderated-control
     * renderer). This BE class is shared by both the plain and moderated block variants
     * (distinguished only by the {@link #moderated} flag), so the plain
     * {@code getConsoleType()}-based prefix lookup could never tell them apart.
     */
    @Override
    public String getRenderTexturePrefix() {
        return moderated ? "rbmk_control_mod" : super.getRenderTexturePrefix();
    }

    @Override
    public void setTarget(double target) {
        this.startingLevel = this.level;
        super.setTarget(target);
    }

    /**
     * 1:1 port of the original's "positive scram" simulation: while a graphite-tipped control
     * rod is actively being withdrawn, the tip briefly increases reactivity as it passes through
     * the channel before the boron absorber section catches up - the historical RBMK tip effect
     * behind the Chernobyl accident's initial power spike. Without this, withdrawing rods never
     * transiently spiked flux at all.
     */
    @Override
    public double getMult() {
        double surge = 0;
        if (targetLevel < startingLevel && Math.abs(level - targetLevel) > 0.01 && getLevel() != null) {
            surge = Math.sin(Math.pow(1 - level, 15) * Math.PI) * (startingLevel - targetLevel)
                    * RBMKDials.getSurgeMod(getLevel());
        }
        return level + surge;
    }

    /**
     * Texture for the animated rod-cap mesh only (see {@code RBMKColumnRenderer}) - colored
     * control rods use a dedicated single-sprite texture per {@link #color} group, matching the
     * original's per-color {@code rbmk_control_<color>.png} rod-cap textures. Unlike
     * {@link #getRenderTexturePrefix()} (which drives the static column body's side/top faces
     * and has no per-color side/top variants), this is looked up as one flat sprite.
     */
    public String getCapTexture() {
        return switch (color) {
            case 0 -> "rbmk_control_red";
            case 1 -> "rbmk_control_yellow";
            case 2 -> "rbmk_control_green";
            case 3 -> "rbmk_control_blue";
            case 4 -> "rbmk_control_purple";
            default -> "rbmk_control";
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RBMKControlManualBlockEntity be) {
        baseTick(level, pos, state, be);
        if (!level.isClientSide) {
            be.lastLevel = be.level;
            be.moveLevelToTarget(level);
        }
    }
}

