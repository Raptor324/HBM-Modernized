package com.hbm_m.platform.legacy.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.hbm_m.platform.legacy.LegacyBlockRemoval;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * [26.x] Ruft {@link LegacyBlockRemoval#onRemove} dort auf, wo 1.21.1 {@code BlockState#onRemove} aufrief:
 * direkt nachdem der neue Zustand im Section steht, nur auf dem Server, bevor Vanilla das alte
 * Block-Entity entfernt (die HBM-Implementierungen droppen dort noch BE-Inhalte).
 */
@Mixin(LevelChunk.class)
public abstract class LevelChunkLegacyRemovalMixin {

    @Shadow
    @Final
    private Level level;

    @WrapOperation(
            method = "setBlockState",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState hbm_m$legacyOnRemove(LevelChunkSection section, int x, int y, int z, BlockState newState,
                                            Operation<BlockState> original, BlockPos pos, BlockState state, int flags) {
        BlockState oldState = original.call(section, x, y, z, newState);
        if (oldState != null && oldState != newState && !this.level.isClientSide()) {
            ((LegacyBlockRemoval) oldState.getBlock()).onRemove(oldState, this.level, pos, newState,
                    (flags & Block.UPDATE_MOVE_BY_PISTON) != 0);
        }
        return oldState;
    }
}
