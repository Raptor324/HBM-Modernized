package com.hbm_m.inventory.menu;

import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * Shared {@code stillValid} for block-entity menus. Vanilla measures reach to the block the
 * menu was opened on, i.e. the controller; on a large multiblock (fracking tower, big doors)
 * a part can be clicked far from it and the menu closed on the next tick. Reach is measured
 * to the whole structure instead.
 */
public final class MenuReach {

    private MenuReach() {}

    public static boolean stillValid(Player player, @Nullable BlockEntity blockEntity) {
        return stillValid(player, blockEntity, null);
    }

    /** {@code expected} restores the vanilla "block is still there" check when non-null. */
    public static boolean stillValid(Player player, @Nullable BlockEntity blockEntity, @Nullable Block expected) {
        if (blockEntity == null || blockEntity.isRemoved()) {
            return false;
        }
        Level level = blockEntity.getLevel();
        if (level == null || level != player.level()) {
            return false;
        }
        BlockPos pos = blockEntity.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (expected != null && !state.is(expected)) {
            return false;
        }
        return canReach(player, pos, state);
    }

    public static boolean canReach(Player player, BlockPos controllerPos, BlockState state) {
        // Same test as AbstractContainerMenu.stillValid (canInteractWithBlock with 4.0 slop), applied
        // to every block of the structure. Stays on the vanilla method: Sable overrides it for
        // blocks on ships, and a plain AABB-vs-eye distance would close every ship GUI next tick.
        if (PlatformHooks.canInteractWithBlock(player, controllerPos, 4.0D)) {
            return true;
        }
        if (state.getBlock() instanceof IMultiblockController controller) {
            MultiblockStructureHelper helper = controller.getStructureHelper();
            if (helper != null) {
                Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING)
                        ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
                for (BlockPos part : helper.getAllPartPositions(controllerPos, facing)) {
                    if (PlatformHooks.canInteractWithBlock(player, part, 4.0D)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
