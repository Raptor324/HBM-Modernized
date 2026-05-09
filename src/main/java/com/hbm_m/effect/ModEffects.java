package com.hbm_m.effect;

// Класс для регистрации пользовательских эффектов (зелья) в моде.
// Здесь мы создаем DeferredRegister для MobEffect и регистрируем наш эффект Radaway.

import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import dev.architectury.registry.registries.RegistrySupplier;

public class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(RefStrings.MODID, Registries.MOB_EFFECT);

    // 2. Регистрируем наш первый эффект - Антирадин
    // Мы передаем ему категорию (полезный) и цвет для отображения в GUI
    public static final RegistrySupplier<MobEffect> RADAWAY = EFFECTS.register("radaway",
            () -> new RadawayEffect(MobEffectCategory.BENEFICIAL, 0xBB4B00));

    public static void init() {
        EFFECTS.register();
    }
}