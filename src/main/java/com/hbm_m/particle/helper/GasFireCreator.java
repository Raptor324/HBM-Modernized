package com.hbm_m.particle.helper;

import com.hbm_m.particle.ParticleGasFlame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * Порт спавна {@code ParticleUtil.spawnGasFlame} (1.7.10, тип "gasfire"):
 * факельное пламя газовой факела с начальной скоростью.
 * NBT: mX/mY/mZ (double), scale (float, def 6.5).
 */
public class GasFireCreator implements IParticleCreator {

    @Override
    public void makeParticle(ClientLevel level, Player player, RandomSource rand,
                             double x, double y, double z, CompoundTag data) {
        int particleSetting = Minecraft.getInstance().options.particles().get().getId();
        if (particleSetting == 2) {
            return;
        }

        ParticleGasFlame fx = new ParticleGasFlame(level, x, y, z,
                data.getDouble("mX"), data.getDouble("mY"), data.getDouble("mZ"),
                data.getFloat("scale") > 0 ? data.getFloat("scale") : 6.5F);
        com.hbm_m.particle.nt.ParticleEngineNT.INSTANCE.add(fx);
    }
}
