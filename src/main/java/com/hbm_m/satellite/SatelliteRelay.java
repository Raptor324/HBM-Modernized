package com.hbm_m.satellite;

import com.hbm_m.blockentity.network.radio.RTTYNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class SatelliteRelay extends Satellite {

    public static final String CMD_RELAY = "relay";

    public SatelliteRelay() { }

    @Override public String getType() { return "DIMENSIONAL_RELAY"; }

    @Override
    public java.util.List<net.minecraft.network.chat.Component> getInfo(ServerLevel world) {
        return java.util.List.of(
                net.minecraft.network.chat.Component.translatable(com.hbm_m.item.ModItems.SATELLITE_RELAY.get().getDescriptionId())
        );
    }

    @Override
    public void onCommandImpl(ServerLevel world, String... cmd) {
        if (cmd.length <= 0) return;
        if (cmd[0].equals(CMD_RELAY) && cmd.length > 3) {
            String dimArg = cmd[1];
            String chan = cmd[2];
            ServerLevel targetWorld = resolveLevel(world, dimArg);
            if (targetWorld != null) {
                StringBuilder signal = new StringBuilder();
                for (int i = 3; i < cmd.length; i++) {
                    if (i > 3) signal.append(" ");
                    signal.append(cmd[i]);
                }
                RTTYNetwork.broadcast(targetWorld, chan, signal.toString());
            }
        }
    }

    private static ServerLevel resolveLevel(ServerLevel current, String dimArg) {
        if ("0".equals(dimArg)) return current.getServer().getLevel(Level.OVERWORLD);
        if ("-1".equals(dimArg)) return current.getServer().getLevel(Level.NETHER);
        if ("1".equals(dimArg)) return current.getServer().getLevel(Level.END);
        for (ServerLevel lvl : current.getServer().getAllLevels()) {
            if (lvl.dimension().location().toString().equals(dimArg)
                    || lvl.dimension().location().getPath().equals(dimArg)) {
                return lvl;
            }
        }
        return null;
    }
}
