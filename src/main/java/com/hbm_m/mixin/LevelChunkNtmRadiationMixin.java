// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only
//
// Port aus NTM Next (com.hbm.mixin.MixinLevelChunk#hbm$radShieldEdit), Commit 3f9a261a.

package com.hbm_m.mixin;

//? if forge || neoforge {
import com.hbm_m.radiation.ntmnext.NtmRadiationSystem;
import com.hbm_m.radiation.ntmnext.RadiationSystemSelector;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Meldet Blockwechsel an das NTM-Next-Strahlungsfeld (nur wenn radiationSystem = ADVANCED):
 * Wechsel von Abschirmung/Quelle -> Sektion neu aufbauen, Wechsel der Diffusivitaet -> neu mitteln.
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkNtmRadiationMixin {

    @Inject(method = "setBlockState", at = @At("HEAD"))
    //? if >= 1.21.5 {
    /*private void hbm_m$ntmRadShieldEdit(BlockPos pos, BlockState state, int flags,
                                         CallbackInfoReturnable<BlockState> cir) {
    *///?} else {
    private void hbm_m$ntmRadShieldEdit(BlockPos pos, BlockState state, boolean isMoving,
                                         CallbackInfoReturnable<BlockState> cir) {
    //?}
        if (!RadiationSystemSelector.isAdvanced()) return;
        NtmRadiationSystem.onBlockStateReplaced((LevelChunk) (Object) this, pos, state);
    }
}
//?}
