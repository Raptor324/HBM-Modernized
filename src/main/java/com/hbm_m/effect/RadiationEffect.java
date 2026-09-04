package com.hbm_m.effect;

import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import org.jetbrains.annotations.NotNull;

/**
 * Порт {@code com.hbm.potion.HbmPotion.radiation} (1.7.10): каждый тик действия эффект
 * накапливает (amplifier + 1) × 0.05 RAD через
 * {@link ContaminationUtil#contaminate} с типом CREATIVE — как в оригинале.
 * Накладывается газами radon_dense (15 с) и meltdown (60 с, amp 2).
 * Иконка — ванильный спрайт {@code textures/mob_effect/radiation.png}.
 */
public class RadiationEffect extends MobEffect {

    public RadiationEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    /** Единая логика тика — единственный источник поведения для обеих версий. */
    private void applyTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide()) {
            ContaminationUtil.contaminate(entity, HazardType.RADIATION, ContaminationType.CREATIVE, (amplifier + 1) * 0.05F);
        }
    }

    //? if < 1.21.1 {
    @Override
    public void applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        applyTick(entity, amplifier);
    }
    //?} else {
    /*@Override
    public boolean applyEffectTick(@NotNull LivingEntity entity, int amplifier) {
        applyTick(entity, amplifier);
        return true;
    }
     *///?}

    //? if < 1.21.1 {
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
    //?} else {
    /*// 1.21.1: isDurationEffectTick переименован в shouldApplyEffectTickThisTick.
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
     *///?}
}
