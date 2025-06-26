package com.hbm_m.sound;

//import net.minecraftforge.common.util.ForgeSoundType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import com.hbm_m.lib.RefStrings;

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

    // Новый звук для techBoop
    public static final RegistryObject<SoundEvent> TOOL_TECH_BOOP = registerSoundEvents("techboop");

    public static final RegistryObject<SoundEvent> TEST_BOOP = registerSoundEvents("testboop");

    //public static final ForgeSoundType SOUND_BLOCK_SOUNDS = new ForgeSoundType(1f, 1f,
    //    ModSounds.GEIGER_1, ModSounds.GEIGER_1, ModSounds.GEIGER_1, ModSounds.GEIGER_1, ModSounds.GEIGER_1);
    
    // Вспомогательный метод для регистрации
    private static RegistryObject<SoundEvent> registerSoundEvents(String name) {
        // Создаем ResourceLocation, избегая устаревших методов
        //ResourceLocation id = name.contains(".") ? new ResourceLocation(RefStrings.MODID, name.replace('.', '/')) : new ResourceLocation(RefStrings.MODID, "tool/" + name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, name)));
    }
    
    // Метод для регистрации в Forge EventBus
    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}