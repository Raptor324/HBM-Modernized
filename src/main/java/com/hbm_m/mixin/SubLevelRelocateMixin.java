package com.hbm_m.mixin;

//? if >= 1.21.1 {
/*import java.util.function.UnaryOperator;

import com.hbm_m.interfaces.IRelocatable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/^*
 * Remaps the absolute positions a block entity keeps of other blocks when Sable moves it
 * ({@code SubLevelAssemblyHelper.moveBlocks}, both ship assembly and disassembly).
 *
 * <p>Sable writes the entity's NBT with only x/y/z rewritten, so an ICF phantom still points at
 * the controller's old coordinates, a pile brick at the old core, a machine part at the old
 * controller. The next tick they conclude their core is gone and revert. The transform Sable
 * used for the block positions is the exact mapping (translation plus the disassembly
 * rotation), so it is handed to {@link IRelocatable} right after the NBT load - before anything
 * ticks.
 *
 * <p>The redirect wraps the {@code loadWithComponents} call inside {@code moveBlocks} and
 * captures the method's own arguments to reach the transform. Sable exists only on 1.21.1; the
 * stub below keeps the Forge annotation processor happy, which rejects unknown string targets.
 ^/
@Mixin(targets = "dev.ryanhcode.sable.api.SubLevelAssemblyHelper")
public abstract class SubLevelRelocateMixin {

    static {
        com.hbm_m.main.MainRegistry.LOGGER.info("[HBM][Mixin] SubLevelRelocateMixin применён к SubLevelAssemblyHelper");
    }

    @Redirect(
        method = "moveBlocks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BlockEntity;loadWithComponents(Lnet/minecraft/nbt/CompoundTag;Lnet/minecraft/core/HolderLookup$Provider;)V",
            remap = true
        ),
        remap = false
    )
    private static void hbm_m$relocateAfterLoad(BlockEntity blockEntity, CompoundTag tag, HolderLookup.Provider registries,
                                                ServerLevel level,
                                                dev.ryanhcode.sable.api.SubLevelAssemblyHelper.AssemblyTransform transform,
                                                Iterable<BlockPos> blocks) {
        blockEntity.loadWithComponents(tag, registries);
        if (blockEntity instanceof IRelocatable relocatable) {
            UnaryOperator<BlockPos> mapping = pos -> transform.apply(pos);
            relocatable.relocate(mapping);
        }
    }
}
*///?} else {
// Sable is 1.21.1-only; the Mixin annotation processor rejects unknown string targets, so the
// stub points at a class that exists everywhere and contributes nothing.
@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.inventory.AbstractContainerMenu.class)
public abstract class SubLevelRelocateMixin {
}
//?}
