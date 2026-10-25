package com.hbm_m.particle.helper;

import com.hbm_m.particle.nt.ParticleEngineNT;
import com.hbm_m.particle.nt.ParticleRotatingNT;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/** Client-Haelfte von {@link FlameCreator}. */
public class FlameClientCreator implements IParticleCreator {

	@Override
	public void makeParticle(ClientLevel world, Player player, RandomSource rand, double x, double y, double z, CompoundTag data) {
		ParticleEngineNT.INSTANCE.add(ParticleRotatingNT.flamethrower(world, x, y, z, data.getInt("meta")));
	}
}
