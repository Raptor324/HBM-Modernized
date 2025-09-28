package com.hbm_m.effect;

// Класс для регистрации пользовательских эффектов (зелья) в моде.
// Здесь мы создаем DeferredRegister для MobEffect и регистрируем наш эффект Radaway.

import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {

    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, RefStrings.MODID);

    // 2. Регистрируем наш первый эффект - Антирадин
    // Мы передаем ему категорию (полезный) и цвет для отображения в GUI
    public static final RegistryObject<MobEffect> RADAWAY = EFFECTS.register("radaway",
            () -> new RadawayEffect(MobEffectCategory.BENEFICIAL, 0xBB4B00));

    // 3. Метод для вызова регистрации в главном классе мода
    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
        MainRegistry.LOGGER.info("Registered MobEffects for " + RefStrings.MODID);
    }
}