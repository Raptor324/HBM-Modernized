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
    public static final RegistryObject<SoundEvent> GEIGER_1 = registerSoundEvents("item.geiger1");
    public static final RegistryObject<SoundEvent> GEIGER_2 = registerSoundEvents("item.geiger2");
    public static final RegistryObject<SoundEvent> GEIGER_3 = registerSoundEvents("item.geiger3");
    public static final RegistryObject<SoundEvent> GEIGER_4 = registerSoundEvents("item.geiger4");
    public static final RegistryObject<SoundEvent> GEIGER_5 = registerSoundEvents("item.geiger5");
    public static final RegistryObject<SoundEvent> GEIGER_6 = registerSoundEvents("item.geiger6");

    public static final RegistryObject<SoundEvent> BOMBDET3 = registerSoundEvents("bombdet3");
    public static final RegistryObject<SoundEvent> BOMBDET2 = registerSoundEvents("bombdet2");
    public static final RegistryObject<SoundEvent> BOMBDET1 = registerSoundEvents("bombdet1");
    public static final RegistryObject<SoundEvent> BOMBWHISTLE = registerSoundEvents("bombwhistle");
    public static final RegistryObject<SoundEvent> BOMBER2 = registerSoundEvents("bomber2");
    public static final RegistryObject<SoundEvent> BOMBER1 = registerSoundEvents("bomber1");
    public static final RegistryObject<SoundEvent> CLICK = registerSoundEvents("click");
    public static final RegistryObject<SoundEvent> CRATE_OPEN = registerSoundEvents("crateopen");
    public static final RegistryObject<SoundEvent> CRATE_CLOSE = registerSoundEvents("crateclose");
    public static final RegistryObject<SoundEvent> EXPLOSION_LARGE_NEAR = registerSoundEvents("explosionlargenear");
    public static final RegistryObject<SoundEvent> EXPLOSION_SMALL_NEAR1 = registerSoundEvents("explosionsmallnear1");
    public static final RegistryObject<SoundEvent> EXPLOSION_SMALL_NEAR2 = registerSoundEvents("explosionsmallnear2");
    public static final RegistryObject<SoundEvent> EXPLOSION_SMALL_NEAR3 = registerSoundEvents("explosionsmallnear3");
    public static final RegistryObject<SoundEvent> EXPLOSION_SMALL_FAR1 = registerSoundEvents("explosionsmallfar1");
    public static final RegistryObject<SoundEvent> EXPLOSION_SMALL_FAR2 = registerSoundEvents("explosionsmallfar2");
    public static final RegistryObject<SoundEvent> MUKE_EXPLOSION = registerSoundEvents("mukeexplosion");
    public static final RegistryObject<SoundEvent> GRENADE_TRIGGER = registerSoundEvents("grenadetrigger");

    public static final RegistryObject<SoundEvent> RADAWAY_USE = registerSoundEvents("radaway_use");
    public static final RegistryObject<SoundEvent> CRATEBREAK5 = registerSoundEvents("block.cratebreak5");
    public static final RegistryObject<SoundEvent> CRATEBREAK4 = registerSoundEvents("block.cratebreak4");
    public static final RegistryObject<SoundEvent> CRATEBREAK3 = registerSoundEvents("block.cratebreak3");
    public static final RegistryObject<SoundEvent> CRATEBREAK2 = registerSoundEvents("block.cratebreak2");
    public static final RegistryObject<SoundEvent> CRATEBREAK1 = registerSoundEvents("block.cratebreak1");
    public static final RegistryObject<SoundEvent> TOOL_TECH_BLEEP = registerSoundEvents("techbleep");
    public static final RegistryObject<SoundEvent> TOOL_TECH_BOOP = registerSoundEvents("techboop");

    public static final RegistryObject<SoundEvent> REPAIR_1 = registerSoundEvents("tool.repair1");
    public static final RegistryObject<SoundEvent> REPAIR_2 = registerSoundEvents("tool.repair2");
    public static final RegistryObject<SoundEvent> REPAIR_3 = registerSoundEvents("tool.repair3");
    public static final RegistryObject<SoundEvent> REPAIR_4 = registerSoundEvents("tool.repair4");
    public static final RegistryObject<SoundEvent> REPAIR_5 = registerSoundEvents("tool.repair5");
    public static final RegistryObject<SoundEvent> REPAIR_6 = registerSoundEvents("tool.repair6");
    public static final RegistryObject<SoundEvent> REPAIR_7 = registerSoundEvents("tool.repair7");

    public static final RegistryObject<SoundEvent> BOUNCE1 = registerSoundEvents("item.bounce1");
    public static final RegistryObject<SoundEvent> BOUNCE2 = registerSoundEvents("item.bounce2");
    public static final RegistryObject<SoundEvent> BOUNCE3 = registerSoundEvents("item.bounce3");

    public static final RegistryObject<SoundEvent> EXTRACT_1 = registerSoundEvents("tool.extract1");
    public static final RegistryObject<SoundEvent> EXTRACT_2 = registerSoundEvents("tool.extract2");

    public static final RegistryObject<SoundEvent> ASSEMBLER_OPERATE = registerSoundEvents("block.assembler_operate");

    public static final RegistryObject<SoundEvent> ASSEMBLER_CUT = registerSoundEvents("block.assembler_cut");
    public static final RegistryObject<SoundEvent> ASSEMBLER_START = registerSoundEvents("block.assembler_start");
    public static final RegistryObject<SoundEvent> ASSEMBLER_STOP = registerSoundEvents("block.assembler_stop");
    public static final RegistryObject<SoundEvent> ASSEMBLER_STRIKE_1 = registerSoundEvents("block.assembler_strike1");
    public static final RegistryObject<SoundEvent> ASSEMBLER_STRIKE_2 = registerSoundEvents("block.assembler_strike2");
    public static final RegistryObject<SoundEvent> MOTOR = registerSoundEvents("block.motor");
    public static final RegistryObject<SoundEvent> PRESS_OPERATE = registerSoundEvents("block.press_operate");
    public static final RegistryObject<SoundEvent> SHREDDER = registerSoundEvents("block.shredder");

    // Doors
    public static final RegistryObject<SoundEvent> GARAGE_MOVE = registerSoundEvents("block.garage_move");
    public static final RegistryObject<SoundEvent> GARAGE_STOP = registerSoundEvents("block.garage_stop");
    public static final RegistryObject<SoundEvent> ALARM_6 = registerSoundEvents("block.alarm_6");
    public static final RegistryObject<SoundEvent> DOOR_WGH_BIG_START = registerSoundEvents("block.door_wgh_big_start");
    public static final RegistryObject<SoundEvent> DOOR_WGH_BIG_STOP = registerSoundEvents("block.door_wgh_big_stop");
    public static final RegistryObject<SoundEvent> DOOR_MOVE_2 = registerSoundEvents("block.door_move_2");
    public static final RegistryObject<SoundEvent> DOOR_SHUT_1 = registerSoundEvents("block.door_shut_1");
    public static final RegistryObject<SoundEvent> DOOR_SLIDE_OPENED_1 = registerSoundEvents("block.door_slide_opened_1");
    public static final RegistryObject<SoundEvent> DOOR_SLIDE_OPENING_1 = registerSoundEvents("block.door_slide_opening_1");
    public static final RegistryObject<SoundEvent> LEVER_1 = registerSoundEvents("block.lever_1");
    public static final RegistryObject<SoundEvent> METAL_STOP_1 = registerSoundEvents("block.metal_stop_1");
    public static final RegistryObject<SoundEvent> SLIDING_DOOR_OPENED = registerSoundEvents("block.sliding_door_opened");
    public static final RegistryObject<SoundEvent> SLIDING_DOOR_OPENING = registerSoundEvents("block.sliding_door_opening");
    public static final RegistryObject<SoundEvent> SLIDING_DOOR_SHUT = registerSoundEvents("block.sliding_door_shut");
    public static final RegistryObject<SoundEvent> TRANSITION_SEAL_OPEN = registerSoundEvents("block.transition_seal_open");
    public static final RegistryObject<SoundEvent> TRANSITION_SEAL_CLOSE = registerSoundEvents("block.transition_seal_close");
    public static final RegistryObject<SoundEvent> WGH_START = registerSoundEvents("block.wgh_start");
    public static final RegistryObject<SoundEvent> WGH_STOP = registerSoundEvents("block.wgh_stop");

    public static final RegistryObject<SoundEvent> SWITCH_ON = registerSoundEvents("block.switch.on");

    // РЕГИСТРАЦИЯ АБСТРАКТНОГО СОБЫТИЯ 
    // Это тот самый звук, который мы будем вызывать в коде.
    // Minecraft сам выберет один из 7 реальных звуков случайным образом.
    public static final RegistryObject<SoundEvent> REPAIR_RANDOM = registerSoundEvents("tool.repair_random");
    public static final RegistryObject<SoundEvent> EXTRACT_RANDOM = registerSoundEvents("tool.extract_random");
    public static final RegistryObject<SoundEvent> ASSEMBLER_STRIKE_RANDOM = registerSoundEvents("block.assembler_strike_random");
    public static final RegistryObject<SoundEvent> BOUNCE_RANDOM = registerSoundEvents("item.bounce_random");
    
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