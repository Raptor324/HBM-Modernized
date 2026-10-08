// Port-eigene Klasse (HBM-Modernized): bindet das portierte NTM-Next-Strahlungssystem an Raptors
// Schnittstelle com.hbm_m.radiation.ChunkRadiationHandler an. Raptors Code bleibt unveraendert;
// ChunkRadiationManager.getProxy() liefert diesen Handler, wenn radiationSystem = ADVANCED.

package com.hbm_m.radiation.ntmnext;

import com.hbm_m.radiation.ChunkRadiationHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

//? if forge {
import net.minecraftforge.event.level.ChunkEvent;
//?}

/**
 * Adapter Raptor-API -> NTM-Next-Feld.
 *
 * <p>Abbildung:
 * <ul>
 *   <li>getRadiation -> gewichtete Summe der Arten-Felder + Nahfeld an der Position (>= 0,
 *       ohne Hintergrund; den Nether-Hintergrund liefert weiterhin EntityEffectHandler).</li>
 *   <li>incrementRad / decrementRad -> additiver Eintrag (+/-), Arten-Mix aus dem Block an der
 *       Position (SourceMixTable), wirkt im naechsten Schritt.</li>
 *   <li>setRadiation -> Dichte der Pocket setzen.</li>
 *   <li>updateSystem / handleWorldDestruction -> leer; der Solver laeuft jeden Tick ueber
 *       {@link RadiationSystemSelector} (eigener SERVER_POST-Hook), Weltzerstoerung inklusive.</li>
 *   <li>clearSystem -> im laufenden Spiel alle Daten der Dimension verwerfen (/ntmrad clear),
 *       beim Herunterfahren nur speichern und freigeben.</li>
 * </ul>
 */
public class ChunkRadiationHandlerNTMNext extends ChunkRadiationHandler {

    @Override
    public void updateSystem() {
        // Bewusst leer: Schritt laeuft in NtmRadiationSystem.tickSim (jeden Server-Tick).
    }

    @Override
    public float getRadiation(Level level, int x, int y, int z) {
        if (!(level instanceof ServerLevel server)) return 0F;
        // Stufe 2: gewichtete Summe Gamma + Neutron + Beta inkl. Nahfeld (RAD/s, HBM-Skala).
        double field = NtmRadiationSystem.totalAt(server, new BlockPos(x, y, z), true);
        return field > 0.0D ? (float) Math.min(field, Float.MAX_VALUE) : 0F;
    }

    @Override
    public void setRadiation(Level level, int x, int y, int z, float rad) {
        if (!(level instanceof ServerLevel server)) return;
        NtmRadiationSystem.setRadForCoord(server, new BlockPos(x, y, z), rad);
    }

    @Override
    public void incrementRad(Level level, int x, int y, int z, float rad) {
        if (!(level instanceof ServerLevel server)) return;
        // Stufe 3: Luft-/Einmal-Emissionen (Fallout, Explosion, Lecks) teils als Kontamination.
        NtmRadiationSystem.incrementRadClassified(server, new BlockPos(x, y, z), rad);
    }

    @Override
    public void decrementRad(Level level, int x, int y, int z, float rad) {
        if (!(level instanceof ServerLevel server)) return;
        BlockPos pos = new BlockPos(x, y, z);
        if (rad < 0.0F) {
            // Manche Aufrufer erhoehen ueber negatives decrementRad.
            NtmRadiationSystem.incrementRadClassified(server, pos, -rad);
            return;
        }
        // Stufe 3: Absorber/Dekon bauen zusaetzlich Kontamination ab.
        NtmRadiationSystem.decontaminateAmount(server, pos, rad * NtmRadiationConfig.deconFactor);
        // Untergrenze 0 (bzw. -Hintergrund) erzwingt der Solver ueber minBound.
        NtmRadiationSystem.incrementRad(server, pos, -rad);
    }

    @Override
    public void clearSystem(Level level) {
        if (!(level instanceof ServerLevel server)) return;
        if (NtmRadiationSystem.serverStopping || !server.getServer().isRunning()) {
            NtmRadiationSystem.onLevelUnload(server);
        } else {
            NtmRadiationSystem.jettisonData(server);
        }
    }

    @Override
    public void onBlockUpdated(Level level, BlockPos pos) {
        // Blockwechsel werden bereits per Mixin (LevelChunk#setBlockState) erfasst.
        NtmRadiationSystem.markSectionForRebuild(level, pos);
    }

    @Override
    public void recalculateChunkRadiation(LevelChunk chunk) {
        if (chunk.getLevel() instanceof ServerLevel server) {
            NtmRadiationSystem.markChunkForRebuild(server, chunk);
        }
    }

    @Override
    public void receiveChunkLoad(LevelChunk chunk) {
        if (chunk.getLevel() instanceof ServerLevel server) {
            NtmRadiationSystem.onChunkLoad(server, chunk);
        }
    }

    //? if forge {
    @Override
    public void receiveChunkUnload(ChunkEvent.Unload event) {
        if (event.getChunk() instanceof LevelChunk chunk
                && chunk.getLevel() instanceof ServerLevel server) {
            NtmRadiationSystem.onChunkUnload(server, chunk);
        }
    }
    //?} else {
    /*// Fabric/NeoForge: ChunkRadiationManager ruft das nur fuer ChunkRadiationHandlerSimple auf;
    // dort muss vor Nutzung noch ein eigener Unload-Hook verdrahtet werden.
    public void receiveChunkUnload(LevelChunk chunk) {
        if (chunk.getLevel() instanceof ServerLevel server) {
            NtmRadiationSystem.onChunkUnload(server, chunk);
        }
    }
    *///?}

    @Override
    public void handleWorldDestruction() {
        // Bewusst leer: Zerstoerung und Nebel laufen in NtmRadiationSystem.runWorldEffects.
    }
}
