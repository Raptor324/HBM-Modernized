package com.hbm_m.particle.helper;

import com.hbm_m.particle.ParticleCoolingTower;
import com.hbm_m.particle.nt.ParticleEngineNT;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

import java.awt.Color;

/**
 * Порт спавна {@code ParticleCoolingTower} из ClientProxy 1.7.10 (тип "tower").
 * NBT: lift(float, def 0.3), base(float), max(float), life(int), noWind(flag),
 * strafe(float), alpha(float), color(int RGB).
 * Гейтинг качества частиц: ALL — всегда; DECREASED — шанс 1/2 и жизнь/2;
 * MINIMAL — не спавним вовсе.
 */
public class CoolingTowerCreator implements IParticleCreator {

    @Override
    public void makeParticle(ClientLevel level, Player player, RandomSource rand,
                             double x, double y, double z, CompoundTag data) {
        int particleSetting = Minecraft.getInstance().options.particles().get().getId();
        if (!(particleSetting == 0 || (particleSetting == 1 && rand.nextBoolean()))) {
            return;
        }

        ParticleCoolingTower fx = new ParticleCoolingTower(level, x, y, z);
        fx.setLift(data.getFloat("lift"));
        fx.setBaseScale(data.getFloat("base"));
        fx.setMaxScale(data.getFloat("max"));
        fx.setLife(data.getInt("life") / (particleSetting + 1));
        if (data.contains("noWind")) fx.noWind();
        if (data.contains("strafe")) fx.setStrafe(data.getFloat("strafe"));
        if (data.contains("alpha")) fx.alphaMod(data.getFloat("alpha"));

        if (data.contains("color")) {
            Color color = new Color(data.getInt("color"));
            fx.setColor(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F);
        }

        ParticleEngineNT.INSTANCE.add(fx);
    }
}
