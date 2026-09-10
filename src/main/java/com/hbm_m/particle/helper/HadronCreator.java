package com.hbm_m.particle.helper;

import com.hbm_m.particle.ModParticleTypes;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

/**
 * 1:1-Port des {@code "hadron"}-Zweigs aus {@code ClientProxy.effectNT}.
 *
 * <p>Der kleine Modus ({@code makeSmall}) reist im Original ueber ein eigenes Merkmal mit; hier
 * geht er als x-Geschwindigkeit an die Partikelfabrik, so wie es der Port bei den RBMK-Flammen
 * schon macht.</p>
 */
public class HadronCreator implements IParticleCreator {

    @Override
    public void makeParticle(ClientLevel level, Player player, RandomSource rand,
                             double x, double y, double z, CompoundTag data) {

        boolean small = data.getBoolean("small");
        level.addParticle(ModParticleTypes.HADRON.get(), x, y, z, small ? 1D : 0D, 0D, 0D);
    }
}
