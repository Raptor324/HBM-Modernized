// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 mlbv <51232730+mlbv@users.noreply.github.com>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from the NTM Next project (MK5 crater generation system).
package com.hbm_m.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.hbm_m.explosion.SectionGenerationAccess;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Счётчик поколений секции: каждый setBlockState инкрементирует его. Движок
 * взрыва MK5 сравнивает поколение при публикации off-thread снапшота секций —
 * если живая секция менялась в момент расчёта, публикация откладывается и
 * пересчитывается, вместо того чтобы затирать свежие изменения.
 */
@Mixin(LevelChunkSection.class)
public class LevelChunkSectionMixin implements SectionGenerationAccess {

    @Unique
    private long hbm$generation;

    @Inject(
            method = "setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;Z)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"))
    private void hbm$bumpGeneration(int x, int y, int z, BlockState state, boolean useCounts,
            CallbackInfoReturnable<BlockState> cir) {
        hbm$generation++;
    }

    @Override
    public long hbm$getGeneration() {
        return hbm$generation;
    }
}
