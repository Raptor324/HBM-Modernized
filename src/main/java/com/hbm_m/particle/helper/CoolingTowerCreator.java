package com.hbm_m.particle.helper;

import com.hbm_m.particle.nt.ParticleCoolingTowerNT;
import com.hbm_m.particle.nt.ParticleEngineNT;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * 1:1-Port des {@code "tower"}-Zweigs aus {@code ClientProxy.effectNT}: die Dampfschwade.
 *
 * <p>Alle Werte kommen wie im Original aus dem Beipack des Pakets - {@code lift}, {@code base},
 * {@code max}, {@code life}, {@code strafe}, {@code alpha}, {@code color} und der Schalter
 * {@code noWind}. Die Absender setzen jeweils nur, was sie brauchen; der Rest bleibt auf den
 * Vorgaben der Partikelklasse.</p>
 */
public class CoolingTowerCreator implements IParticleCreator {

    @Override
    public void makeParticle(ClientLevel level, Player player, RandomSource rand,
                             double x, double y, double z, CompoundTag data) {


        // Гейтинг качества частиц (наша работа по contrail): ALL — всегда;
        // DECREASED — шанс 1/2 и жизнь/2; MINIMAL — не спавним вовсе.
        int particleSetting = net.minecraft.client.Minecraft.getInstance().options.particles().get().getId();
        if (particleSetting == 2 || (particleSetting == 1 && !rand.nextBoolean())) {
            return;
        }
        int lifeMod = particleSetting + 1;
        ParticleCoolingTowerNT fx = new ParticleCoolingTowerNT(level, x, y, z);

        if (data.contains("lift"))   fx.setLift(data.getFloat("lift"));
        if (data.contains("base"))   fx.setBaseScale(data.getFloat("base"));
        if (data.contains("max"))    fx.setMaxScale(data.getFloat("max"));
        if (data.contains("life"))   fx.setLife(data.getInt("life") / lifeMod);
        if (data.contains("strafe")) fx.setStrafe(data.getFloat("strafe"));
        if (data.contains("alpha"))  fx.alphaMod(data.getFloat("alpha"));
        if (data.contains("noWind")) fx.noWind();
        if (data.contains("color"))  fx.setColor(data.getInt("color"));

        ParticleEngineNT.INSTANCE.add(fx);
    }
}
