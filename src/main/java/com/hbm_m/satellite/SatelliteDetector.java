package com.hbm_m.satellite;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.api.redstoneoverradio.IRORInteractive;
import com.hbm_m.satellite.DetectorEvents.RadiationBurst;

import net.minecraft.server.level.ServerLevel;

/** 1:1 {@code SatelliteDetector} (Breitband-Strahlungsdetektor): liest die Ausbrueche aus {@link DetectorEvents}. */
public class SatelliteDetector extends Satellite {

    public List<RadiationBurst> cachedResults = new ArrayList<>();

    public static final String CMD_SURVEY = "survey";
    public static final String CMD_COUNT = "count";
    public static final String CMD_GETTYPE = "gettype";
    public static final String CMD_GETPOSITION = "getposition";

    public SatelliteDetector() { }

    @Override public String getType() { return "UWB_EMISSION_DETECTOR"; }

    @Override
    public void onCommandImpl(ServerLevel world, String... cmd) {
        if (cmd.length <= 0) return;

        // Original: kein return nach survey
        if (cmd[0].equals(CMD_SURVEY)) {
            cachedResults.clear();
            cachedResults.addAll(DetectorEvents.bursts(world.dimension()));
        }

        if (cmd[0].equals(CMD_COUNT)) {
            this.tx = "" + cachedResults.size();
            return;
        }

        if (cmd[0].equals(CMD_GETTYPE) && cmd.length == 2) {
            RadiationBurst burst = getBurstFromIndex(cmd[1]);
            if (burst == null) { this.tx = ""; return; }
            this.tx = "" + burst.intensity().name();
            return;
        }

        if (cmd[0].equals(CMD_GETPOSITION) && cmd.length == 2) {
            RadiationBurst burst = getBurstFromIndex(cmd[1]);
            if (burst == null) { this.tx = ""; return; }
            this.tx = burst.x() + ";" + burst.z();
        }
    }

    public RadiationBurst getBurstFromIndex(String cmd) {
        if (cachedResults.size() <= 0) return null;
        int index = IRORInteractive.parseInt(cmd, 1, cachedResults.size()) - 1;
        return cachedResults.get(index);
    }
}
