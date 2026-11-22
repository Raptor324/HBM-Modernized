package com.hbm_m.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rotates the FACING of this mod's blocks in {@code BlockBehaviour.rotate}/{@code mirror}.
 *
 * <p>Nearly every machine here extends {@code BaseEntityBlock} directly and declares its own
 * FACING, so it never inherited {@code HorizontalDirectionalBlock.rotate} - and
 * {@code BlockBehaviour.rotate} returns the state unchanged. Create ({@code StructureTransform}) and
 * Sable ({@code AssemblyTransform}) both turn moved block states through exactly that method,
 * so a ship disassembled at a right angle put every block on its rotated position while its
 * facing stayed put. For a multiblock that breaks the deterministic relink
 * ({@code controllerPos = partPos - rotate(offset, facing)}), and the controller then rebuilds
 * its phantoms across the rotated ones.
 *
 * <p>Only blocks from {@code com.hbm_m} that reach the base method are touched: a block with
 * its own override never gets here, other mods' blocks are left alone.
 */
@Mixin(BlockBehaviour.class)
public abstract class BlockFacingRotationMixin {

    private static final ClassValue<Boolean> HBM_M$OURS = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            return type.getName().startsWith("com.hbm_m.");
        }
    };

    private static DirectionProperty hbm_m$facingOf(BlockState state) {
        if (state.hasProperty(HorizontalDirectionalBlock.FACING)) return HorizontalDirectionalBlock.FACING;
        if (state.hasProperty(DirectionalBlock.FACING)) return DirectionalBlock.FACING;
        return null;
    }

    @Inject(method = "rotate(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/Rotation;)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"), cancellable = true)
    private void hbm_m$rotateFacing(BlockState state, Rotation rotation, CallbackInfoReturnable<BlockState> cir) {
        if (rotation == Rotation.NONE || !HBM_M$OURS.get(getClass())) return;
        DirectionProperty facing = hbm_m$facingOf(state);
        if (facing == null) return;
        cir.setReturnValue(state.setValue(facing, rotation.rotate(state.getValue(facing))));
    }

    @Inject(method = "mirror(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/Mirror;)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"), cancellable = true)
    private void hbm_m$mirrorFacing(BlockState state, Mirror mirror, CallbackInfoReturnable<BlockState> cir) {
        if (mirror == Mirror.NONE || !HBM_M$OURS.get(getClass())) return;
        DirectionProperty facing = hbm_m$facingOf(state);
        if (facing == null) return;
        Direction dir = state.getValue(facing);
        cir.setReturnValue(state.setValue(facing, mirror.getRotation(dir).rotate(dir)));
    }
}
