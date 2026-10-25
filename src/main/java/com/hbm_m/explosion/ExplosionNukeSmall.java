package com.hbm_m.explosion;

import java.util.Arrays;
import java.util.List;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.logic.EntityNukeExplosionMK5;
import com.hbm_m.explosion.ExplosionNT.ExAttrib;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.sound.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * Port of {@code com.hbm.explosion.ExplosionNukeSmall}: the shared "muke" detonation behind mini
 * nukes, the nuclear creeper and the UFO. The mini branch fires an {@link ExplosionNT} with
 * FIRE/NODROP/NOHURT-style attribs; upstream's non-mini branch (PARAMS_HIGH, the Fat Man) starts
 * an {@link EntityNukeExplosionMK5}. PARAMS_TOTS uses the "tinytot" particle.
 */
public final class ExplosionNukeSmall {

    private ExplosionNukeSmall() {}

    public static final MukeParams PARAMS_SAFE = new MukeParams().safe(true).killRadius(45F).radiationLevel(2F);
    public static final MukeParams PARAMS_TOTS = new MukeParams().blastRadius(10F).killRadius(30F)
            .particle("tinytot").shrapnelCount(0).resolution(32).radiationLevel(1F);
    public static final MukeParams PARAMS_LOW = new MukeParams().blastRadius(15F).killRadius(45F).radiationLevel(2F);
    public static final MukeParams PARAMS_MEDIUM = new MukeParams().blastRadius(20F).killRadius(55F).radiationLevel(3F);
    /** {@code blastRadius = BombConfig.fatmanRadius} — read from the config at call time. */
    public static final MukeParams PARAMS_HIGH = new MukeParams() {
        @Override public float blastRadius() { return ModClothConfig.get().fatmanRadius; }
    }.miniNuke(false).blastRadius(35F).shrapnelCount(0);

    public static void explode(Level level, double x, double y, double z, MukeParams params) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (params.particle != null) {
            CompoundTag data = new CompoundTag();
            data.putString("type", params.particle);
            data.putBoolean("balefire", params.particle.equals("muke") && level.random.nextInt(100) == 0);
            data.putDouble("posX", x);
            data.putDouble("posY", y + 0.5D);
            data.putDouble("posZ", z);
            IParticleCreator.sendPacket(serverLevel, x, y + 0.5D, z, 250, data);
        }

        SoundEvent sound = ModSounds.MUKE_EXPLOSION.orElse(null);
        if (sound != null) {
            level.playSound(null, BlockPos.containing(x, y, z), sound, SoundSource.HOSTILE, 15.0F, 1.0F);
        }

        if (params.shrapnelCount > 0) {
            MissileWarheadEffects.spawnShrapnelBurst(serverLevel, x, y, z, params.shrapnelCount);
        }
        if (params.miniNuke) {
            if (!params.safe && params.blastRadius > 0F) {
                new ExplosionNT(level, null, x, y, z, params.blastRadius())
                        .addAllAttrib(params.explosionAttribs)
                        .overrideResolution(params.resolution)
                        .explode();
            }
            if (params.killRadius > 0F) {
                ExplosionNukeGeneric.dealDamage(level, x, y, z, params.killRadius);
            }
            com.hbm_m.api.space.SpaceApi.fireNuclearDetonation(level, x, y, z, params.blastRadius(),
                    com.hbm_m.api.space.NuclearDetonation.Kind.MINI_NUKE);

            float radMod = params.radiationLevel / 3F;
            for (int i = -2; i <= 2; i++) {
                for (int j = -2; j <= 2; j++) {
                    if (Math.abs(i) + Math.abs(j) >= 4) continue;
                    ChunkRadiationManager.incrementRad(level,
                            (int) Math.floor(x + i * 16),
                            (int) Math.floor(y),
                            (int) Math.floor(z + j * 16),
                            50F / (Math.abs(i) + Math.abs(j) + 1) * radMod);
                }
            }
        } else {
            EntityNukeExplosionMK5.start(serverLevel, (int) params.blastRadius(), x, y, z);
        }
    }

    /** Builder-shaped copy of the upstream parameter block. */
    public static class MukeParams {
        boolean miniNuke = true;
        boolean safe = false;
        String particle = "muke";
        float blastRadius = 0F;
        float killRadius = 0F;
        float radiationLevel = 1F;
        int shrapnelCount = 25;
        int resolution = 64;
        List<ExAttrib> explosionAttribs = Arrays.asList(ExAttrib.FIRE, ExAttrib.NOPARTICLE, ExAttrib.NOSOUND, ExAttrib.NODROP, ExAttrib.NOHURT);

        public float blastRadius() { return blastRadius; }

        public MukeParams miniNuke(boolean v) { this.miniNuke = v; return this; }
        public MukeParams safe(boolean v) { this.safe = v; return this; }
        public MukeParams particle(String v) { this.particle = v; return this; }
        public MukeParams blastRadius(float v) { this.blastRadius = v; return this; }
        public MukeParams killRadius(float v) { this.killRadius = v; return this; }
        public MukeParams radiationLevel(float v) { this.radiationLevel = v; return this; }
        public MukeParams shrapnelCount(int v) { this.shrapnelCount = v; return this; }
        public MukeParams resolution(int v) { this.resolution = v; return this; }

        public MukeParams copy() {
            return new MukeParams().miniNuke(miniNuke).safe(safe).particle(particle).blastRadius(blastRadius)
                    .killRadius(killRadius).radiationLevel(radiationLevel).shrapnelCount(shrapnelCount)
                    .resolution(resolution);
        }
    }
}
