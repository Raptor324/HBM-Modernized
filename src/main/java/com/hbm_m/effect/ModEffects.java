package com.hbm_m.effect;

// Регистрация пользовательских эффектов (зелья) в моде.
// Кросс-версионные вызовы эффектов (add/has/remove) — через PlatformHooks.

import com.hbm_m.lib.RefStrings;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import dev.architectury.registry.registries.RegistrySupplier;

public class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(RefStrings.MODID, Registries.MOB_EFFECT);

    // Антирадин (1.7.10: HbmPotion.radaway): каждый тик снижает накопленную дозу.
    public static final RegistrySupplier<MobEffect> RADAWAY = EFFECTS.register("radaway",
            () -> new RadawayEffect(MobEffectCategory.BENEFICIAL, 0xBB4B00));

    // Порча (1.7.10: HbmPotion.taint)
    public static final RegistrySupplier<MobEffect> TAINT = EFFECTS.register("taint",
            () -> new TaintEffect());

    // Лучевая болезнь (1.7.10: HbmPotion.radiation): каждый тик накапливает
    // (amplifier + 1) × 0.05 RAD через ContaminationUtil.contaminate CREATIVE.
    // Накладывается газами radon_dense (15 с) и meltdown (60 с, amp 2).
    public static final RegistrySupplier<MobEffect> RADIATION = EFFECTS.register("radiation",
            () -> new RadiationEffect(MobEffectCategory.HARMFUL, 0x84C128));

    public static void init() {
        EFFECTS.register();
    }
}
