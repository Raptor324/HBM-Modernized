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

        // Nuclear blast от Fatman / MK5 / MK3 / Tom / Fleija -> cremate
        if (source.is(ModDamageTypes.NUCLEAR_BLAST)) {
            cremate(entity);
            return;
        }

        // Будущие типы урона (laser, electricity, plasma) - портируются отдельно
        // if (source.is(ModDamageTypes.LASER)) { pulverize(entity); return; }
        // if (source.is(ModDamageTypes.ELECTRIC)) { pulverize(entity); return; }
        // if (source.is(ModDamageTypes.PLASMA)) { cremate(entity); return; }
        // if (source.is(net.minecraft.world.damagesource.DamageTypes.EXPLOSION) || source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_EXPLOSION)) { gib(entity); return; }
        // if (source.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE)) { cremate(entity); return; }
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

    // TODO: портировать gib() и giblets при необходимости
}
