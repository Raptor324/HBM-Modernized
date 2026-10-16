package com.hbm_m.blockentity.machines.albion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * 1:1-Port von {@code IParticleUser} (1.7.10): jedes Bauteil der Strahlstrecke.
 *
 * <p>Der Ablauf je Schritt ist immer derselbe: die Quelle fragt {@link #canParticleEnter}, laesst
 * das Bauteil ueber {@link #onEnter} auf das Teilchen wirken und holt sich mit
 * {@link #getExitPos} die naechste Position.</p>
 */
public interface IParticleUser {

    /**
     * Darf das Teilchen hier hinein? Geprueft wird, ob es aus der richtigen Richtung und von der
     * richtigen Nachbarposition kommt.
     */
    boolean canParticleEnter(Particle particle, Direction dir, int x, int y, int z);

    /** Wirkt auf das Teilchen - Impuls, Streuung, Energieverbrauch, Abstuerze. */
    void onEnter(Particle particle, Direction dir);

    /** Wohin das Teilchen als Naechstes springt; {@code null} laesst es stehen. */
    BlockPos getExitPos(Particle particle);
}
