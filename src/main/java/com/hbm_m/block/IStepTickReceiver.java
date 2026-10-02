package com.hbm_m.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** 1:1 {@code com.hbm.blocks.IStepTickReceiver}: jeder Spielertick, solange der Spieler (nicht fliegend) darauf steht. */
public interface IStepTickReceiver {

    void onPlayerStep(Level level, BlockPos pos, Player player);
}
