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
        return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect.get());
    }
    *///?}

    /**
     * Разрешает «любой» эффект в форму, требуемую версией: 1.20.1 — {@link MobEffect},
     * 1.21.1 — настоящий {@code Holder.Reference} через реестр.
     *
     * <p>Принимает {@code MobEffect}, {@code RegistrySupplier}, {@code Supplier} или
     * {@code Holder}. На 1.21.1 НЕ возвращает Architectury-RegistrySupplier как есть:
     * он имплементирует {@code Holder}, но не {@code Holder.Reference} — MobEffect.CODEC
     * крашит сохранение сущности с "Unregistered holder", а equals/hasEffect его не находят.
     * Аналогично голый {@code Holder} (не Reference) перезаворачивается через реестр.
     * Статические вызовы до регистрации эффектов дают {@code null} от {@code get()} —
     * возвращаем null, caller обязан деградировать.
     */
    //? if < 1.21.1 {
    public static MobEffect resolve(Object effect) {
        if (effect instanceof MobEffect me) return me;
        if (effect instanceof java.util.function.Supplier<?> sup) {
            Object v = sup.get();
            return v instanceof MobEffect me2 ? me2 : null;
        }
        if (effect instanceof net.minecraft.core.Holder<?> holder) {
            Object v = holder.value();
            return v instanceof MobEffect me3 ? me3 : null;
        }
        return null;
    }
    //?} else {
    /*public static net.minecraft.core.Holder<MobEffect> resolve(Object effect) {
        MobEffect me = null;
        if (effect instanceof MobEffect direct) {
            me = direct;
        } else if (effect instanceof java.util.function.Supplier<?> sup && sup.get() instanceof MobEffect resolved) {
            me = resolved;
        } else if (effect instanceof net.minecraft.core.Holder<?> holder && holder.value() instanceof MobEffect fromHolder) {
            me = fromHolder;
        }
        if (me == null) return null;
        if (effect instanceof net.minecraft.core.Holder.Reference<?> ref && ref.value() == me) {
            return (net.minecraft.core.Holder<MobEffect>) ref;
        }
        return net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(me);
    }
    *///?}

    /** {@code living.addEffect(new MobEffectInstance(effect, duration, amplifier))} на обеих версиях. */
    public static boolean addEffect(net.minecraft.world.entity.LivingEntity living,
            RegistrySupplier<MobEffect> effect, int duration, int amplifier) {
        return living.addEffect(new net.minecraft.world.effect.MobEffectInstance(of(effect), duration, amplifier));
    }

    /** {@code living.hasEffect(effect)} на обеих версиях. */
    public static boolean hasEffect(net.minecraft.world.entity.LivingEntity living,
            RegistrySupplier<MobEffect> effect) {
        return living.hasEffect(of(effect));
    }

    /** {@code living.removeEffect(effect)} на обеих версиях. */
    public static boolean removeEffect(net.minecraft.world.entity.LivingEntity living,
            RegistrySupplier<MobEffect> effect) {
        return living.removeEffect(of(effect));
    }

    /** {@code living.getEffect(effect)} на обеих версиях; null, если эффекта нет. */
    public static net.minecraft.world.effect.MobEffectInstance getEffect(net.minecraft.world.entity.LivingEntity living,
            RegistrySupplier<MobEffect> effect) {
        return living.getEffect(of(effect));
    }
}
