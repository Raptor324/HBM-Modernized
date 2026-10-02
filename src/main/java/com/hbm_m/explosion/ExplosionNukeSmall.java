package com.hbm_m.explosion;

import java.util.Arrays;
import java.util.List;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.logic.EntityNukeExplosionMK5;
import com.hbm_m.explosion.ExplosionNT.ExAttrib;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.sound.ModSounds;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * 1:1-Port von {@code com.hbm.explosion.ExplosionNukeSmall} (1.7.10): die Mini-Nuke - Muke-Blitz
 * (1 % Balefire-Variante), Knall, Schrapnell, eine {@link ExplosionNT} mit Feuer ohne Drops, der
 * Toetungsradius und ein rautenfoermiger Strahlungsfleck ueber 5x5 Chunks. Die grosse Variante
 * ({@link #PARAMS_HIGH}) startet stattdessen eine echte MK5-Explosion mit Fat-Man-Radius.
 */
@Deprecated
public final class ExplosionNukeSmall {

    private ExplosionNukeSmall() {}

    public static void explode(Level world, double posX, double posY, double posZ, MukeParams params) {
        if (!(world instanceof ServerLevel sl)) return;

        if (params.particle != null) {
            CompoundTag data = new CompoundTag();
            data.putString("type", params.particle);
            if (params.particle.equals("muke") && (com.hbm_m.main.Polaroid.id() == 11 || world.random.nextInt(100) == 0)) {
                data.putBoolean("balefire", true);
            }
            IParticleCreator.sendPacket(sl, posX, posY + 0.5, posZ, 250, data);
        }

        SoundEvent sound = ModSounds.MUKE_EXPLOSION.orElse(null);
        if (sound != null) world.playSound(null, posX, posY, posZ, sound, SoundSource.BLOCKS, 15.0F, 1.0F);

        if (params.shrapnelCount > 0) ExplosionLarge.spawnShrapnels(world, posX, posY, posZ, params.shrapnelCount);
        if (params.miniNuke && !params.safe) {
            new ExplosionNT(world, null, posX, posY, posZ, params.blastRadius()).addAllAttrib(params.explosionAttribs).overrideResolution(params.resolution).explode();
        }
        if (params.killRadius > 0) ExplosionNukeGeneric.dealDamage(world, posX, posY, posZ, params.killRadius);
        if (!params.miniNuke) EntityNukeExplosionMK5.start(world, (int) params.blastRadius(), posX, posY, posZ);

        if (params.miniNuke) {
            float radMod = params.radiationLevel / 3F;
            for (int i = -2; i <= 2; i++) {
                for (int j = -2; j <= 2; j++) {
                    if (Math.abs(i) + Math.abs(j) < 4) {
                        ChunkRadiationManager.incrementRad(world, (int) Math.floor(posX + i * 16), (int) Math.floor(posY), (int) Math.floor(posZ + j * 16),
                                50 / (Math.abs(i) + Math.abs(j) + 1) * radMod);
                    }
                }
            }
        }
    }

    public static final MukeParams PARAMS_SAFE = new MukeParams() {{ safe = true; killRadius = 45F; radiationLevel = 2F; }};
    public static final MukeParams PARAMS_TOTS = new MukeParams() {{ blastRadius = 10F; killRadius = 30F; particle = "tinytot"; shrapnelCount = 0; resolution = 32; radiationLevel = 1; }};
    public static final MukeParams PARAMS_LOW = new MukeParams() {{ blastRadius = 15F; killRadius = 45F; radiationLevel = 2; }};
    public static final MukeParams PARAMS_MEDIUM = new MukeParams() {{ blastRadius = 20F; killRadius = 55F; radiationLevel = 3; }};
    /** {@code blastRadius = BombConfig.fatmanRadius} - im Port aus der Konfiguration beim Aufruf. */
    public static final MukeParams PARAMS_HIGH = new MukeParams() {{ miniNuke = false; blastRadius = 35F; shrapnelCount = 0; }
        @Override public float blastRadius() { return ModClothConfig.get().fatmanRadius; } };

    public static class MukeParams {
        public boolean miniNuke = true;
        public boolean safe = false;
        public float blastRadius;
        public float killRadius;
        public float radiationLevel = 1F;
        public String particle = "muke";
        public int shrapnelCount = 25;
        public int resolution = 64;
        public List<ExAttrib> explosionAttribs = Arrays.asList(ExAttrib.FIRE, ExAttrib.NOPARTICLE, ExAttrib.NOSOUND, ExAttrib.NODROP, ExAttrib.NOHURT);

        public float blastRadius() {
            return blastRadius;
        }
    }
}
