package com.hbm_m.explosion.vanillant.standard;

import java.util.HashMap;
import java.util.Map.Entry;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.interfaces.IPlayerProcessor;
import com.hbm_m.network.ExplosionKnockbackPacket;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code PlayerProcessorStandard}: schickt jedem getroffenen Spieler seinen Rueckstoss. */
public class PlayerProcessorStandard implements IPlayerProcessor {

    @Override
    public void process(ExplosionVNT explosion, Level world, double x, double y, double z, HashMap<Player, Vec3> affectedPlayers) {
        for (Entry<Player, Vec3> entry : affectedPlayers.entrySet()) {
            if (entry.getKey() instanceof ServerPlayer mp) {
                ExplosionKnockbackPacket.sendTo(mp, entry.getValue());
            }
        }
    }
}
