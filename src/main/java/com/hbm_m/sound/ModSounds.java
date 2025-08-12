package com.hbm_m.sound;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;

public class ModSounds {
    // Создаем регистр для звуков
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = 
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, RefStrings.MODID);
    
    // Регистрируем звуки счетчика Гейгера
    public static final RegistryObject<SoundEvent> GEIGER_1 = registerSoundEvents("geiger1");
    public static final RegistryObject<SoundEvent> GEIGER_2 = registerSoundEvents("geiger2");
    public static final RegistryObject<SoundEvent> GEIGER_3 = registerSoundEvents("geiger3");
    public static final RegistryObject<SoundEvent> GEIGER_4 = registerSoundEvents("geiger4");
    public static final RegistryObject<SoundEvent> GEIGER_5 = registerSoundEvents("geiger5");
    public static final RegistryObject<SoundEvent> GEIGER_6 = registerSoundEvents("geiger6");

    public static final RegistryObject<SoundEvent> RADAWAY_USE = registerSoundEvents("radaway_use");

    public static final RegistryObject<SoundEvent> TOOL_TECH_BOOP = registerSoundEvents("techboop");

    public static final RegistryObject<SoundEvent> REPAIR_1 = registerSoundEvents("tool.repair1");
    public static final RegistryObject<SoundEvent> REPAIR_2 = registerSoundEvents("tool.repair2");
    public static final RegistryObject<SoundEvent> REPAIR_3 = registerSoundEvents("tool.repair3");
    public static final RegistryObject<SoundEvent> REPAIR_4 = registerSoundEvents("tool.repair4");
    public static final RegistryObject<SoundEvent> REPAIR_5 = registerSoundEvents("tool.repair5");
    public static final RegistryObject<SoundEvent> REPAIR_6 = registerSoundEvents("tool.repair6");
    public static final RegistryObject<SoundEvent> REPAIR_7 = registerSoundEvents("tool.repair7");

    public static final RegistryObject<SoundEvent> EXTRACT_1 = registerSoundEvents("tool.extract1");
    public static final RegistryObject<SoundEvent> EXTRACT_2 = registerSoundEvents("tool.extract2");

    public static final RegistryObject<SoundEvent> ASSEMBLER_OPERATE = registerSoundEvents("block.assembler_operate");    

    // РЕГИСТРАЦИЯ АБСТРАКТНОГО СОБЫТИЯ 
    // Это тот самый звук, который мы будем вызывать в коде.
    // Minecraft сам выберет один из 7 реальных звуков случайным образом.
    public static final RegistryObject<SoundEvent> REPAIR_RANDOM = registerSoundEvents("tool.repair_random");
    public static final RegistryObject<SoundEvent> EXTRACT_RANDOM = registerSoundEvents("tool.extract_random");
    
    // Вспомогательный метод для регистрации
    private static RegistryObject<SoundEvent> registerSoundEvents(String name) {
        // Создаем ResourceLocation, избегая устаревших методов
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
    
    // Метод для регистрации в Forge EventBus
    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
        MainRegistry.LOGGER.info("Registered SoundEvents for " + RefStrings.MODID);
    }
}