package com.hbm_m.particle.helper;

import com.hbm_m.particle.ParticleFoundry;
import com.hbm_m.particle.nt.ParticleEngineNT;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * Порт спавна {@code ParticleFoundry} из ClientProxy 1.7.10 (тип "foundry").
 * NBT: color(int), dir(byte — Direction.get3DDataValue() направления розлива),
 * len(float), base(float), off(float).
 */
public class FoundryCreator implements IParticleCreator {

    @Override
    public void makeParticle(ClientLevel level, Player player, RandomSource rand,
                             double x, double y, double z, CompoundTag data) {
        int color = data.getInt("color");
        byte dir = data.getByte("dir");
        float length = data.getFloat("len");
        float base = data.getFloat("base");
        float offset = data.getFloat("off");

        ParticleFoundry sploosh = new ParticleFoundry(level, x, y, z, color, dir, length, base, offset);
        ParticleEngineNT.INSTANCE.add(sploosh);
    }
}
