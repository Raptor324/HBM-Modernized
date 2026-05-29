package com.hbm_m.explosion.vanillant.interfaces;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;

public interface IPlayerProcessor {
    void process(ExplosionVNT explosion, Level level, double x, double y, double z, HashMap<Player, Vec3> affectedPlayers);
}
