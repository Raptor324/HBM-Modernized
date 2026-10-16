package com.hbm_m.mixin.client;

import com.hbm_m.client.render.NucleusDispatcherBypass;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Transactional presence for the Nucleus dispatcher bypass (CrankShaft
 * BlockEntityStorage pattern): machine placed / chunk loaded → enqueue add.
 * The drain runs at the start of collectMain; non-managed types are dropped
 * there (a type-set check happens in the hook itself - O(1)).
 */
@Mixin(LevelChunk.class)
public class MixinLevelChunkSetBlockEntity {

    @Inject(method = "setBlockEntity", at = @At("TAIL"))
    private void hbm_m$onSetBlockEntity(BlockEntity blockEntity, CallbackInfo ci) {
        NucleusDispatcherBypass.onMachineAdded(blockEntity);
    }
}
