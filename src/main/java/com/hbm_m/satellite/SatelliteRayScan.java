package com.hbm_m.satellite;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.api.redstoneoverradio.IRORInteractive;
import com.hbm_m.satellite.RayScanEvents.RayEvent;

import net.minecraft.server.level.ServerLevel;

/** 1:1 {@code SatelliteRayScan} (Schmalband-Emissionsscanner): liest {@link RayScanEvents} im Umkreis von 250. */
public class SatelliteRayScan extends Satellite {

    public List<RayEvent> cachedResults = new ArrayList<>();

    public static final int MAX_SCAN_RANGE = 250;

    public static final String CMD_SURVEY = "survey";
    public static final String CMD_COUNT = "count";
    public static final String CMD_GETINFO = "getinfo";
    public static final String CMD_GETPOSITION = "getposition";

    public SatelliteRayScan() { }

    @Override public String getType() { return "NB_RAY_SCANNER"; }

    @Override
    public void onCommandImpl(ServerLevel world, String... cmd) {
        if (cmd.length <= 0) return;

        if (cmd[0].equals(CMD_SURVEY)) {
            this.cachedResults.clear();
            this.cachedResults.addAll(RayScanEvents.survey(world.dimension(), this.targetX, this.targetZ));
            return;
        }

        if (cmd[0].equals(CMD_COUNT)) {
            this.tx = "" + cachedResults.size();
            return;
        }

        if (cmd[0].equals(CMD_GETINFO) && cmd.length == 2) {
            RayEvent event = getEventFromIndex(cmd[1]);
            if (event == null) { this.tx = ""; return; }
            this.tx = "" + event.info();
            return;
        }

        if (cmd[0].equals(CMD_GETPOSITION) && cmd.length == 2) {
            RayEvent event = getEventFromIndex(cmd[1]);
            if (event == null) { this.tx = ""; return; }
            this.tx = event.x() + ";" + event.z();
        }
    }

    public RayEvent getEventFromIndex(String cmd) {
        if (cachedResults.size() <= 0) return null;
        int index = IRORInteractive.parseInt(cmd, 1, cachedResults.size()) - 1;
        return cachedResults.get(index);
    }
}
