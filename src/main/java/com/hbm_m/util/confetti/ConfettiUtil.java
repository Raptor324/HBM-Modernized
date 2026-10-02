package com.hbm_m.util.confetti;

import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.particle.helper.AshesCreator;
import com.hbm_m.particle.helper.SkeletonCreator;
import com.hbm_m.sound.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.Mth;

/**
 * Портированный {@code ConfettiUtil} из HBM 1.7.10.
 * Вызывается после смерти живой сущности и решает, какой эффект воспроизводить,
 * в зависимости от типа урона.
 */
public final class ConfettiUtil {

    private ConfettiUtil() {}

    /**
     * Диспетчер эффектов после смерти сущности.
     *
     * @param entity умершая сущность
     * @param source источник урона
     */
    public static void decideConfetti(LivingEntity entity, DamageSource source) {
        if (entity.isAlive()) return;

        String type = source.getMsgId().toLowerCase(java.util.Locale.US);
        if (type.equals("laser")) pulverize(entity);
        if (type.equals("electric")) pulverize(entity);
        if (type.equals("plasma")) cremate(entity);
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) gib(entity);
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) cremate(entity);
        // Port: Kernwaffen haben einen eigenen Schadenstyp (im Original eine Explosion mit Feuer)
        if (source.is(ModDamageTypes.NUCLEAR_BLAST)) cremate(entity);
    }


    /**
     * Полное распыление: пепел + скелет с полной яркостью.
     * Используется для лазерного/электрического урона и для Fatman.
     */
    public static void pulverize(LivingEntity entity) {
        int amount = Mth.clamp((int) (entity.getBbWidth() * entity.getBbHeight() * entity.getBbWidth() * 25), 5, 50);
        AshesCreator.composeEffect(entity.level(), entity, amount, 0.125F);
        SkeletonCreator.composeEffect(entity.level(), entity, 1.0F);
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                ModSounds.DISINTEGRATION.get(), SoundSource.HOSTILE,
                2.0F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
    }

    /**
     * Сжигание: скелет с пониженной яркостью (обгоревший).
     * Используется для ядерного взрыва, плазмы и огня.
     */
    public static void cremate(LivingEntity entity) {
        int amount = Mth.clamp((int) (entity.getBbWidth() * entity.getBbHeight() * entity.getBbWidth() * 25), 5, 50);
        AshesCreator.composeEffect(entity.level(), entity, amount, 0.125F);
        SkeletonCreator.composeEffect(entity.level(), entity, 0.25F);
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                ModSounds.DISINTEGRATION.get(), SoundSource.HOSTILE,
                2.0F, 0.9F + entity.getRandom().nextFloat() * 0.2F);
    }

    /** 1:1 {@code ConfettiUtil.gib}: blutige Knochen, Fleischfetzen ("giblets") und Knack-Geraeusch. */
    public static void gib(LivingEntity entity) {
        if (entity instanceof net.minecraft.world.entity.animal.Ocelot) return;

        int type = 0;
        if (entity instanceof net.minecraft.world.entity.monster.Slime) type = 1; // MagmaCube ist ein Slime
        if (entity instanceof net.minecraft.world.entity.monster.Creeper) type = 1;
        if (entity instanceof net.minecraft.world.entity.animal.AbstractGolem) type = 2;
        if (entity instanceof net.minecraft.world.entity.monster.Blaze) type = 2;
        if (entity instanceof com.hbm_m.entity.mob.EntityRADBeast) type = 2;
        if (entity instanceof com.hbm_m.entity.mob.EntityUFO) type = 2;
        if (entity instanceof com.hbm_m.entity.mob.botprime.EntityBOTPrimeBase) type = 2;
        // EntityCyberCrab/TeslaCrab/TaintCrab/FBIDrone (type 2) kommen mit den Mobs

        SkeletonCreator.composeEffectGib(entity.level(), entity, 0.25F);

        if (entity instanceof net.minecraft.world.entity.monster.AbstractSkeleton) return;

        if (entity.level() instanceof net.minecraft.server.level.ServerLevel server) {
            net.minecraft.nbt.CompoundTag vdat = new net.minecraft.nbt.CompoundTag();
            vdat.putString("type", "giblets");
            vdat.putInt("ent", entity.getId());
            vdat.putInt("gibType", type);
            com.hbm_m.particle.helper.IParticleCreator.sendPacket(server, entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(), 150, vdat);
        }
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), net.minecraft.sounds.SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, 0.95F + entity.getRandom().nextFloat() * 0.2F);
    }
}
