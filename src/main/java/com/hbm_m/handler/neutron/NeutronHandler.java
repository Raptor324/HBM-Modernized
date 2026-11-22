package com.hbm_m.handler.neutron;

import java.util.Map;

import com.hbm_m.handler.neutron.NeutronNodeWorld.StreamWorld;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

/**
 * Port of NEXT {@code com.hbm.handler.neutron.NeutronHandler}, RBMK parts removed: the RBMK
 * dial caching lives in {@code com.hbm_m.handler.rbmk.NeutronNodeWorld.tick} (RBMK streams run
 * through their own world there), and the NEXT {@code ticks}/{@code cacheClear} counter existed
 * only to pace the RBMK {@code cleanNodes} sweep, which is not ported (see
 * {@link NeutronNodeWorld}). Driven from {@code MainRegistry}'s SERVER_POST hook, after all BE
 * ticks: streams created by {@code castRay} this tick are consumed next tick - NEXT's 1-tick
 * latency is load-bearing for critical masses.
 */
public final class NeutronHandler {

    private NeutronHandler() {}

    public static void onServerTick(MinecraftServer server) {

        NeutronNodeWorld.removeEmptyWorlds();

        for (Map.Entry<Level, StreamWorld> entry : NeutronNodeWorld.streamWorlds.entrySet()) {
            Level world = entry.getKey();

            entry.getValue().runStreamInteractions(world);
            entry.getValue().removeAllStreams();
        }
    }
}
