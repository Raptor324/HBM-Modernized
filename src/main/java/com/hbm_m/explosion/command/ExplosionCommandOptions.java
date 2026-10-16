package com.hbm_m.explosion.command;

/**
 * Параметры взрыва из команды /hbm_m explosion.
 */
public final class ExplosionCommandOptions {

    public static final ExplosionCommandOptions DEFAULT =
            new ExplosionCommandOptions(true, true, true, true, 1.0f, true, true);

    private final boolean crater;
    private final boolean damage;
    private final boolean biomes;
    private final boolean particles;
    private final float amplifier;
    private final boolean sound;
    private final boolean fallout;

    public ExplosionCommandOptions(boolean crater, boolean damage, boolean biomes, boolean particles,
                                   float amplifier, boolean sound, boolean fallout) {
        this.crater = crater;
        this.damage = damage;
        this.biomes = biomes;
        this.particles = particles;
        this.amplifier = amplifier > 0 ? amplifier : 1.0f;
        this.sound = sound;
        this.fallout = fallout;
    }

    public boolean crater() {
        return crater;
    }

    public boolean damage() {
        return damage;
    }

    public boolean biomes() {
        return biomes;
    }

    public boolean particles() {
        return particles;
    }

    public float amplifier() {
        return amplifier;
    }

    public boolean sound() {
        return sound;
    }

    public boolean fallout() {
        return fallout;
    }

    public ExplosionCommandOptions withCrater(boolean v) {
        return new ExplosionCommandOptions(v, damage, biomes, particles, amplifier, sound, fallout);
    }

    public ExplosionCommandOptions withDamage(boolean v) {
        return new ExplosionCommandOptions(crater, v, biomes, particles, amplifier, sound, fallout);
    }

    public ExplosionCommandOptions withBiomes(boolean v) {
        return new ExplosionCommandOptions(crater, damage, v, particles, amplifier, sound, fallout);
    }

    public ExplosionCommandOptions withParticles(boolean v) {
        return new ExplosionCommandOptions(crater, damage, biomes, v, amplifier, sound, fallout);
    }

    public ExplosionCommandOptions withAmplifier(float amp) {
        return new ExplosionCommandOptions(crater, damage, biomes, particles, amp, sound, fallout);
    }

    public ExplosionCommandOptions withSound(boolean v) {
        return new ExplosionCommandOptions(crater, damage, biomes, particles, amplifier, v, fallout);
    }

    public ExplosionCommandOptions withFallout(boolean v) {
        return new ExplosionCommandOptions(crater, damage, biomes, particles, amplifier, sound, v);
    }
}
