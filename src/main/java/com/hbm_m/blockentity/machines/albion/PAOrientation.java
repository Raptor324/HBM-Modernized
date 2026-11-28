package com.hbm_m.blockentity.machines.albion;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gemeinsame Ableitung der Strahlachse aus der Blickrichtung eines Bauteils.
 *
 * <p>Das Original rechnet {@code ForgeDirection.getOrientation(meta - 10).getRotation(DOWN)} - die
 * Achse steht also quer zur Blickrichtung ({@code getCounterClockWise()}, da {@code UP} in
 * {@code MultiblockStructureHelper.rotatePorts} auf {@code getClockWise()} abgebildet wird).</p>
 */
public final class PAOrientation {

    private PAOrientation() {}

    public static Direction facingOf(BlockState state) {
        return state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING)
                : Direction.NORTH;
    }

    /** Die Achse, entlang der der Strahl durch dieses Bauteil laeuft. */
    public static Direction beamAxis(BlockState state) {
        return facingOf(state).getCounterClockWise();
    }
}
