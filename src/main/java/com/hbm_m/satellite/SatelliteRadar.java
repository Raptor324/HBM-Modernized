package com.hbm_m.satellite;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.hbm_m.api.redstoneoverradio.IRORInteractive;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * 1:1 {@code SatelliteRadar}: erfasst alle radarsichtbaren Objekte ({@code IRadarDetectable} und Spieler, die
 * Klassenliste von {@code TileEntityMachineRadarNT.matchingEntities}) im Umkreis von 1000 Bloecken um das Ziel.
 */
public class SatelliteRadar extends Satellite {

    public static final int MAX_SCAN_RANGE = 1_000;

    public static final String CMD_SURVEY = "survey";
    public static final String CMD_FILTER = "filter";
    public static final String CMD_COUNT = "count";
    public static final String CMD_GETTARGETID = "gettargetid";
    public static final String CMD_GETPOSITION = "getposition";
    public static final String CMD_GETNAME = "getname";

    public List<Entity> cachedRadarResults = new ArrayList<>();
    public List<Entity> filteredRadarResults = new ArrayList<>();

    public SatelliteRadar() { }

    @Override public String getType() { return "LEO_RADAR"; }

    private static boolean isRadarVisible(Entity e) {
        return e instanceof api.hbm_m.entity.IRadarDetectable || e instanceof Player;
    }

    @Override
    public void onCommandImpl(ServerLevel world, String... cmd) {
        if (cmd.length <= 0) return;

        if (cmd[0].equals(CMD_SURVEY)) {
            cachedRadarResults.clear();

            for (Entity entity : world.getAllEntities()) {
                if (!isRadarVisible(entity)) continue;
                int x = (int) Math.floor(entity.getX());
                int z = (int) Math.floor(entity.getZ());
                double dX = x - targetX;
                double dZ = z - targetZ;
                if (dX * dX + dZ * dZ <= MAX_SCAN_RANGE * MAX_SCAN_RANGE) {
                    cachedRadarResults.add(entity);
                }
            }

            filteredRadarResults = new ArrayList<>(cachedRadarResults);
            return;
        }

        if (cmd[0].equals(CMD_FILTER) && cmd.length == 2) {
            filteredRadarResults.clear();
            String filter = cmd[1].toLowerCase(Locale.US);
            for (Entity entity : cachedRadarResults) {
                if (entity.isRemoved()) continue;
                String classname = entity.getClass().getSimpleName().toLowerCase(Locale.US);
                if (classname.contains(filter)) {
                    filteredRadarResults.add(entity);
                }
            }
            return;
        }

        if (cmd[0].equals(CMD_COUNT)) {
            this.tx = "" + filteredRadarResults.size();
            return;
        }

        if (cmd[0].equals(CMD_GETTARGETID) && cmd.length == 2) {
            Entity target = getTargetFromIndex(cmd[1]);
            if (target == null) { this.tx = ""; return; }
            this.tx = "" + target.getId();
            return;
        }

        if (cmd[0].equals(CMD_GETPOSITION) && cmd.length == 2) {
            Entity target = getTargetFromIndex(cmd[1]);
            if (target == null) { this.tx = ""; return; }
            this.tx = (int) Math.floor(target.getX()) + ";" + (int) Math.floor(target.getY()) + ";" + (int) Math.floor(target.getZ());
            return;
        }

        if (cmd[0].equals(CMD_GETNAME) && cmd.length == 2) {
            Entity target = getTargetFromIndex(cmd[1]);
            if (target == null) { this.tx = ""; return; }
            this.tx = target.getClass().getSimpleName().toLowerCase(Locale.US);
        }
    }

    public Entity getTargetFromIndex(String cmd) {
        if (filteredRadarResults.size() <= 0) return null;
        int index = IRORInteractive.parseInt(cmd, 1, filteredRadarResults.size()) - 1;
        Entity target = filteredRadarResults.get(index);
        if (target.isRemoved()) return null;
        return target;
    }
}
