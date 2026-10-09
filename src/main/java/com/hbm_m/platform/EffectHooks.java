package com.hbm_m.platform;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.effect.MobEffect;

/**
 * Versionsfassade fuer Mod-Effekte: 1.20.1 erwartet {@link MobEffect}, 1.21.1 {@code Holder<MobEffect>}
 * (MobEffectInstance-Konstruktor, hasEffect, removeEffect, getEffect ...).
 *
 * <pre>
 *   new MobEffectInstance(ModEffects.RADIATION.get(), t, a) -> new MobEffectInstance(EffectHooks.of(ModEffects.RADIATION), t, a)
 *   living.hasEffect(ModEffects.RADX.get())                 -> living.hasEffect(EffectHooks.of(ModEffects.RADX))
 * </pre>
 * Auf 1.21.1 ist ein Architectury-{@code RegistrySupplier} selbst ein {@code Holder}.
 * Vanilla-{@code MobEffects.X} sind auf 1.21.1 bereits Holder und brauchen keine Fassade.
 */
public final class EffectHooks {
    private EffectHooks() {}

    //? if < 1.21.1 {
    public static MobEffect of(RegistrySupplier<MobEffect> effect) {
        return effect.get();
    }
    //?} else {
    /*public static net.minecraft.core.Holder<MobEffect> of(RegistrySupplier<MobEffect> effect) {
        return effect;
    }
    *///?}
}
