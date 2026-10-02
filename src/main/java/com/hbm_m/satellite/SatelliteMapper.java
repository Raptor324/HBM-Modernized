package com.hbm_m.satellite;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.hbm_m.handler.pollution.PollutionData;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.inventory.fluid.trait.PollutionType;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;

/** 1:1 {@code SatelliteMapper} (Spionagesatellit): Chunk geladen?, Russ am Ziel, Spieler unter freiem Himmel. */
public class SatelliteMapper extends Satellite {

    public static final String CMD_TARGET_LOADED = "targetloaded";
    public static final String CMD_GETSMOG = "getsmog";
    public static final String CMD_SPOT_PLAYER = "spotplayers";

    public static final int SPOT_PLAYER_MAX_RANGE = 250;

    public SatelliteMapper() { }

    @Override public String getType() { return "NOT_A_SPY_SATELLITE_:)"; }

    @Override
    public void onCommandImpl(ServerLevel world, String... cmd) {
        if (cmd.length <= 0) return;

        if (cmd[0].equals(CMD_TARGET_LOADED)) {
            this.tx = "" + world.getChunkSource().hasChunk(targetX >> 4, targetZ >> 4);
            this.tx = this.tx.toUpperCase(Locale.US);
            return;
        }

        if (cmd[0].equals(CMD_GETSMOG)) {
            PollutionData data = PollutionHandler.getPollutionData(world, this.targetX, 255, this.targetZ);
            if (data != null) {
                float soot = data.pollution[PollutionType.SOOT.ordinal()];
                this.tx = "" + (int) Math.ceil(soot);
            }
            return;
        }

        if (cmd[0].equals(CMD_SPOT_PLAYER)) {
            List<String> names = new ArrayList<>();
            for (Player player : world.players()) {
                int x = (int) Math.floor(player.getX());
                int z = (int) Math.floor(player.getZ());
                double dX = x - targetX;
                double dZ = z - targetZ;
                if (dX * dX + dZ * dZ <= SPOT_PLAYER_MAX_RANGE * SPOT_PLAYER_MAX_RANGE) {
                    int height = world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                    if (height < player.getY() + 2) names.add(player.getGameProfile().getName());
                }
            }

            if (names.isEmpty()) {
                this.tx = "NONE";
                return;
            }

            this.tx = String.join(";", names);
        }
    }
}
