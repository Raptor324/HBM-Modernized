package com.hbm_m.blockentity.machines.albion;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gemeinsame Ableitung der Strahlachse aus der Blickrichtung eines Bauteils.
 *
 * <p>Das Original rechnet {@code ForgeDirection.getOrientation(meta - 10).getRotation(DOWN)} - die
 * Achse steht also quer zur Blickrichtung. Hier steht das an einer Stelle, damit alle Bauteile
 * dieselbe Drehung nutzen: nur so passt der Ring zusammen.</p>
 *
 * <p><b>Vorbehalt:</b> Ob die Vierteldrehung im Uhrzeigersinn oder dagegen geht, laesst sich aus
 * dem Original nicht ohne die Forge-Quellen ablesen. Beide Varianten ergeben einen stimmigen Ring;
 * unterscheidet sich nur, wie herum man die Bauteile setzen muss. Zeigt sich das im Spiel
 * spiegelverkehrt, ist es hier ein einziger Wechsel auf {@code getCounterClockWise()}.</p>
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
        return facingOf(state).getClockWise();
    }
}
