package com.hbm_m.particle.helper;

import com.hbm_m.particle.ParticleGiblet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Порт спавна гиблетсов (1.7.10 AuxParticle "giblets" -> ClientProxy): количество
 * зависит от габаритов сущности ({@code ceil(w/0.25 * 1.5 * h/0.25 / 5)}), шанс 1/15 -
 * десятикратная скорость ("в стратосферу"). Тип всегда meat.
 */
public class GibletCreator implements IParticleCreator {

    @Override
    public void makeParticle(ClientLevel level, Player player, RandomSource rand,
                             double x, double y, double z, CompoundTag data) {
        int particleSetting = Minecraft.getInstance().options.particles().get().getId();
        if (particleSetting == 2) {
            return;
        }

        Entity entity = null;
        if (data.contains("ent")) {
            entity = level.getEntity(data.getInt("ent"));
        }

        double width = entity != null ? entity.getBbWidth() : 0.6D;
        double height = entity != null ? entity.getBbHeight() : 1.8D;
        int count = (int) Math.ceil(width / 0.25D * 1.5D * height / 0.25D / 5.0D);

        for (int i = 0; i < count; i++) {
            double mult = rand.nextInt(15) == 0 ? 10.0D : 1.0D;
            ParticleGiblet fx = new ParticleGiblet(level, x, y, z,
                    rand.nextGaussian() * 0.25D * mult,
                    rand.nextFloat() * mult,
                    rand.nextGaussian() * 0.25D * mult,
                    ParticleGiblet.TYPE_MEAT);
            com.hbm_m.particle.nt.ParticleEngineNT.INSTANCE.add(fx);
        }
    }
}
