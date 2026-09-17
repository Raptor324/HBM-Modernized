package com.hbm_m.inventory.menu;

import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Reach check for menus that belong to a multiblock.
 *
 * <p>Vanilla's {@code AbstractContainerMenu.stillValid(ContainerLevelAccess, ...)} measures the
 * player's distance against the <em>centre of the controller cell</em> and closes the screen past 8
 * blocks. That works for a one-block furnace and fails badly for a machine like the fusion reactor
 * vessel, whose footprint is 15x15: the player clicks a dummy cell on the hull, the click is
 * forwarded to the core in the middle of the structure, the menu opens - and is thrown out on the
 * very next tick because the core is more than 8 blocks away from where the player is standing.
 * That is the "GUI closes again immediately when I open it from the side" symptom.</p>
 *
 * <p>So the distance is measured against the structure's own hull instead of its centre: standing
 * within arm's reach of any part of the machine keeps the menu open, and the limit stays vanilla's
 * 8 blocks for every one-block machine, where the hull <em>is</em> the single cell.</p>
 */
public final class MultiblockMenuReach {

    /** Vanilla's {@code stillValid} radius, squared. */
    private static final double MAX_DISTANCE_SQ = 64.0D;

    private MultiblockMenuReach() {}

    /**
     * @param level      the menu's level (null on a detached menu - treated as invalid)
     * @param corePos    the controller position the menu was opened for
     * @param controller the block the controller is expected to still be
     */
    public static boolean stillValid(Level level, BlockPos corePos, Block controller, Player player) {
        if (level == null || corePos == null) return false;

        BlockState state = level.getBlockState(corePos);
        if (!state.is(controller)) return false;

        return player.distanceToSqr(nearestPoint(hull(state, corePos, controller), player.position()))
                <= MAX_DISTANCE_SQ;
    }

    /** The machine's full world-space footprint, or just the controller cell if it has no structure. */
    private static AABB hull(BlockState state, BlockPos corePos, Block controller) {
        if (controller instanceof IMultiblockController mb && state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            MultiblockStructureHelper helper = mb.getStructureHelper();
            if (helper != null) {
                return helper.getRenderBoundingBox(corePos, state.getValue(HorizontalDirectionalBlock.FACING), 0.0D);
            }
        }
        return new AABB(corePos);
    }

    /** Point of {@code box} closest to {@code from}; equals {@code from} when it is inside the box. */
    private static Vec3 nearestPoint(AABB box, Vec3 from) {
        return new Vec3(
                Math.max(box.minX, Math.min(from.x, box.maxX)),
                Math.max(box.minY, Math.min(from.y, box.maxY)),
                Math.max(box.minZ, Math.min(from.z, box.maxZ)));
    }
}
