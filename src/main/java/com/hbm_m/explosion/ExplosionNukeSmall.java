package com.hbm_m.explosion;

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
 * nukes, the nuclear creeper and the UFO. The blast is a vanilla explosion, as in the other ported
 * blasts — upstream fires an ExplosionNT with FIRE/NODROP/NOHURT, which this port has no equivalent
 * for. Upstream's non-mini branch (PARAMS_HIGH, the Fat Man) lives in {@link NuclearExplosionAPI},
 * and PARAMS_TOTS is left out because its "tinytot" particle is not ported.
 */
public final class ExplosionNukeSmall {

    private ExplosionNukeSmall() {}

    public static final MukeParams PARAMS_SAFE = new MukeParams().safe(true).killRadius(45F).radiationLevel(2F);
    public static final MukeParams PARAMS_LOW = new MukeParams().blastRadius(15F).killRadius(45F).radiationLevel(2F);
    public static final MukeParams PARAMS_MEDIUM = new MukeParams().blastRadius(20F).killRadius(55F).radiationLevel(3F);

    public static void explode(Level level, double x, double y, double z, MukeParams params) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (params.particle) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "muke");
            data.putBoolean("balefire", level.random.nextInt(100) == 0);
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
        if (!params.safe && params.blastRadius > 0F) {
            serverLevel.explode(null, x, y, z, params.blastRadius, Level.ExplosionInteraction.MOB);
        }
        if (params.killRadius > 0F) {
            ExplosionNukeGeneric.dealDamage(level, x, y, z, params.killRadius);
        }

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
    }

    /** Builder-shaped copy of the upstream parameter block. */
    public static final class MukeParams {
        boolean safe = false;
        boolean particle = true;
        float blastRadius = 0F;
        float killRadius = 0F;
        float radiationLevel = 1F;
        int shrapnelCount = 25;

        public MukeParams safe(boolean v) { this.safe = v; return this; }
        public MukeParams particle(boolean v) { this.particle = v; return this; }
        public MukeParams blastRadius(float v) { this.blastRadius = v; return this; }
        public MukeParams killRadius(float v) { this.killRadius = v; return this; }
        public MukeParams radiationLevel(float v) { this.radiationLevel = v; return this; }
        public MukeParams shrapnelCount(int v) { this.shrapnelCount = v; return this; }

        public MukeParams copy() {
            return new MukeParams().safe(safe).particle(particle).blastRadius(blastRadius)
                    .killRadius(killRadius).radiationLevel(radiationLevel).shrapnelCount(shrapnelCount);
        }
    }
}
