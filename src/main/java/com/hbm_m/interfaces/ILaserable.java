package com.hbm_m.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Port des 1.7.10 {@code api.hbm.block.ILaserable}: Vertrag fuer Bloecke/BlockEntities, die von
 * einem Core-Emitter-Laserstrahl getroffen werden koennen (siehe {@code TileEntityCoreEmitter}
 * im Original, welches per Raytrace entlang seiner FACING-Richtung nach Zielen dieses Typs sucht).
 * <p>
 * Vereinfachung gegenueber dem Original: kein generisches Fusionsreaktor-Docking
 * ({@code TileEntityCore.burn(...)}) - in diesem Port existiert keine Fusionsreaktor-Infrastruktur,
 * daher ist der Core Emitter/Receiver ein eigenstaendiges Energie-Relais-Paar.
 */
public interface ILaserable {

    /**
     * Wird vom Core Emitter aufgerufen, wenn sein Strahl diese Position trifft.
     *
     * @param level         Serverseitige Welt.
     * @param pos           Position dieses Ziels.
     * @param energy        Uebertragene Energiemenge (bereits um den Leitungsverlust reduziert).
     * @param beamDirection Richtung, in die sich der Strahl bewegt (= FACING des Emitters).
     * @return true, wenn die Energie angenommen wurde (Strahl stoppt hier); false, wenn das Ziel
     *         die Annahme verweigert (z.B. falsche Ausrichtung) - der Strahl behandelt das Ziel dann
     *         wie ein gewoehnliches, undurchlaessiges Hindernis.
     */
    boolean addLaserEnergy(Level level, BlockPos pos, long energy, Direction beamDirection);
}
