package com.hbm_m.sound;

import com.hbm_m.lib.RefStrings;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import dev.architectury.registry.registries.RegistrySupplier;

public class ModSounds {
    // Создаем регистр для звуков
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = 
            DeferredRegister.create(RefStrings.MODID, Registries.SOUND_EVENT);
    
    // Регистрируем звуки счетчика Гейгера
    public static final RegistrySupplier<SoundEvent> GEIGER_1 = registerSoundEvents("item.geiger1");
    public static final RegistrySupplier<SoundEvent> GEIGER_2 = registerSoundEvents("item.geiger2");
    public static final RegistrySupplier<SoundEvent> GEIGER_3 = registerSoundEvents("item.geiger3");
    public static final RegistrySupplier<SoundEvent> GEIGER_4 = registerSoundEvents("item.geiger4");
    public static final RegistrySupplier<SoundEvent> GEIGER_5 = registerSoundEvents("item.geiger5");
    public static final RegistrySupplier<SoundEvent> GEIGER_6 = registerSoundEvents("item.geiger6");

    public static final RegistrySupplier<SoundEvent> BOMBDET3 = registerSoundEvents("bombdet3");
    public static final RegistrySupplier<SoundEvent> BOMBDET2 = registerSoundEvents("bombdet2");
    public static final RegistrySupplier<SoundEvent> BOMBDET1 = registerSoundEvents("bombdet1");
    public static final RegistrySupplier<SoundEvent> BOMBWHISTLE = registerSoundEvents("bombwhistle");
    public static final RegistrySupplier<SoundEvent> BOMBER2 = registerSoundEvents("bomber2");
    public static final RegistrySupplier<SoundEvent> BOMBER1 = registerSoundEvents("bomber1");
    public static final RegistrySupplier<SoundEvent> CLICK = registerSoundEvents("click");
    public static final RegistrySupplier<SoundEvent> CRATE_OPEN = registerSoundEvents("crateopen");
    public static final RegistrySupplier<SoundEvent> CRATE_CLOSE = registerSoundEvents("crateclose");
    public static final RegistrySupplier<SoundEvent> EXPLOSION_LARGE_NEAR = registerSoundEvents("explosionlargenear");
    public static final RegistrySupplier<SoundEvent> EXPLOSION_SMALL_NEAR1 = registerSoundEvents("explosionsmallnear1");
    public static final RegistrySupplier<SoundEvent> EXPLOSION_SMALL_NEAR2 = registerSoundEvents("explosionsmallnear2");
    public static final RegistrySupplier<SoundEvent> EXPLOSION_SMALL_NEAR3 = registerSoundEvents("explosionsmallnear3");
    public static final RegistrySupplier<SoundEvent> EXPLOSION_SMALL_FAR1 = registerSoundEvents("explosionsmallfar1");
    public static final RegistrySupplier<SoundEvent> EXPLOSION_SMALL_FAR2 = registerSoundEvents("explosionsmallfar2");
    public static final RegistrySupplier<SoundEvent> MUKE_EXPLOSION = registerSoundEvents("mukeexplosion");
    public static final RegistrySupplier<SoundEvent> NUCLEAR_EXPLOSION = registerSoundEvents("nuclear_explosion");
    public static final RegistrySupplier<SoundEvent> GRENADE_TRIGGER = registerSoundEvents("grenadetrigger");

    public static final RegistrySupplier<SoundEvent> RADAWAY_USE = registerSoundEvents("radaway_use");
    public static final RegistrySupplier<SoundEvent> CRATEBREAK5 = registerSoundEvents("block.cratebreak5");
    public static final RegistrySupplier<SoundEvent> CRATEBREAK4 = registerSoundEvents("block.cratebreak4");
    public static final RegistrySupplier<SoundEvent> CRATEBREAK3 = registerSoundEvents("block.cratebreak3");
    public static final RegistrySupplier<SoundEvent> CRATEBREAK2 = registerSoundEvents("block.cratebreak2");
    public static final RegistrySupplier<SoundEvent> CRATEBREAK1 = registerSoundEvents("block.cratebreak1");
    public static final RegistrySupplier<SoundEvent> TOOL_TECH_BLEEP = registerSoundEvents("techbleep");
    public static final RegistrySupplier<SoundEvent> TOOL_TECH_BOOP = registerSoundEvents("techboop");

    // Звуки силовой брони
    public static final RegistrySupplier<SoundEvent> STEP_POWERED = registerSoundEvents("step.powered");
    public static final RegistrySupplier<SoundEvent> STEP_METAL = registerSoundEvents("step.metal");
    public static final RegistrySupplier<SoundEvent> STEP_IRON_JUMP = registerSoundEvents("step.iron_jump");
    public static final RegistrySupplier<SoundEvent> STEP_IRON_LAND = registerSoundEvents("step.iron_land");
    public static final RegistrySupplier<SoundEvent> NULL_SOUND = registerSoundEvents("item.null_sound");
    public static final RegistrySupplier<SoundEvent> MACE_SMASH_AIR = registerSoundEvents("mace.smash_air");
    public static final RegistrySupplier<SoundEvent> MACE_SMASH_GROUND = registerSoundEvents("mace.smash_ground");
    public static final RegistrySupplier<SoundEvent> MACE_SMASH_GROUND_HEAVY = registerSoundEvents("mace.smash_ground_heavy");

    public static final RegistrySupplier<SoundEvent> REPAIR_1 = registerSoundEvents("tool.repair1");
    public static final RegistrySupplier<SoundEvent> REPAIR_2 = registerSoundEvents("tool.repair2");
    public static final RegistrySupplier<SoundEvent> REPAIR_3 = registerSoundEvents("tool.repair3");
    public static final RegistrySupplier<SoundEvent> REPAIR_4 = registerSoundEvents("tool.repair4");
    public static final RegistrySupplier<SoundEvent> REPAIR_5 = registerSoundEvents("tool.repair5");
    public static final RegistrySupplier<SoundEvent> REPAIR_6 = registerSoundEvents("tool.repair6");
    public static final RegistrySupplier<SoundEvent> REPAIR_7 = registerSoundEvents("tool.repair7");

    public static final RegistrySupplier<SoundEvent> BOUNCE1 = registerSoundEvents("item.bounce1");
    public static final RegistrySupplier<SoundEvent> BOUNCE2 = registerSoundEvents("item.bounce2");
    public static final RegistrySupplier<SoundEvent> BOUNCE3 = registerSoundEvents("item.bounce3");

    public static final RegistrySupplier<SoundEvent> EXTRACT_1 = registerSoundEvents("tool.extract1");
    public static final RegistrySupplier<SoundEvent> EXTRACT_2 = registerSoundEvents("tool.extract2");

    public static final RegistrySupplier<SoundEvent> ASSEMBLER_OPERATE = registerSoundEvents("block.assembler_operate");

    public static final RegistrySupplier<SoundEvent> ASSEMBLER_CUT = registerSoundEvents("block.assembler_cut");
    public static final RegistrySupplier<SoundEvent> ASSEMBLER_START = registerSoundEvents("block.assembler_start");
    public static final RegistrySupplier<SoundEvent> ASSEMBLER_STOP = registerSoundEvents("block.assembler_stop");
    public static final RegistrySupplier<SoundEvent> ASSEMBLER_STRIKE_1 = registerSoundEvents("block.assembler_strike1");
    public static final RegistrySupplier<SoundEvent> ASSEMBLER_STRIKE_2 = registerSoundEvents("block.assembler_strike2");
    public static final RegistrySupplier<SoundEvent> MOTOR = registerSoundEvents("block.motor");
    public static final RegistrySupplier<SoundEvent> METAL_BOX_OPEN = registerSoundEvents("block.metal_box_open");
    public static final RegistrySupplier<SoundEvent> METAL_BOX_CLOSE = registerSoundEvents("block.metal_box_close");
    public static final RegistrySupplier<SoundEvent> PRESS_OPERATE = registerSoundEvents("block.press_operate");
    public static final RegistrySupplier<SoundEvent> SHREDDER = registerSoundEvents("block.shredder");
    public static final RegistrySupplier<SoundEvent> CHEMICAL_PLANT = registerSoundEvents("block.chemical_plant");

    // Doors
    public static final RegistrySupplier<SoundEvent> GARAGE_MOVE = registerSoundEvents("block.garage_move");
    public static final RegistrySupplier<SoundEvent> GARAGE_STOP = registerSoundEvents("block.garage_stop");
    public static final RegistrySupplier<SoundEvent> ALARM_6 = registerSoundEvents("block.alarm_6");
    public static final RegistrySupplier<SoundEvent> DOOR_WGH_BIG_START = registerSoundEvents("block.door_wgh_big_start");
    public static final RegistrySupplier<SoundEvent> DOOR_WGH_BIG_STOP = registerSoundEvents("block.door_wgh_big_stop");
    public static final RegistrySupplier<SoundEvent> DOOR_MOVE_2 = registerSoundEvents("block.door_move_2");
    public static final RegistrySupplier<SoundEvent> DOOR_SHUT_1 = registerSoundEvents("block.door_shut_1");
    public static final RegistrySupplier<SoundEvent> DOOR_SLIDE_OPENED_1 = registerSoundEvents("block.door_slide_opened_1");
    public static final RegistrySupplier<SoundEvent> DOOR_SLIDE_OPENING_1 = registerSoundEvents("block.door_slide_opening_1");
    public static final RegistrySupplier<SoundEvent> LEVER_1 = registerSoundEvents("block.lever_1");
    public static final RegistrySupplier<SoundEvent> METAL_STOP_1 = registerSoundEvents("block.metal_stop_1");
    public static final RegistrySupplier<SoundEvent> SLIDING_DOOR_OPENED = registerSoundEvents("block.sliding_door_opened");
    public static final RegistrySupplier<SoundEvent> SLIDING_DOOR_OPENING = registerSoundEvents("block.sliding_door_opening");
    public static final RegistrySupplier<SoundEvent> SLIDING_DOOR_SHUT = registerSoundEvents("block.sliding_door_shut");
    public static final RegistrySupplier<SoundEvent> TRANSITION_SEAL_OPEN = registerSoundEvents("block.transition_seal_open");
    public static final RegistrySupplier<SoundEvent> TRANSITION_SEAL_CLOSE = registerSoundEvents("block.transition_seal_close");
    public static final RegistrySupplier<SoundEvent> WGH_START = registerSoundEvents("block.wgh_start");
    public static final RegistrySupplier<SoundEvent> WGH_STOP = registerSoundEvents("block.wgh_stop");
    public static final RegistrySupplier<SoundEvent> VAULT_SCRAPE = registerSoundEvents("block.vault_scrape");
    public static final RegistrySupplier<SoundEvent> VAULT_THUD = registerSoundEvents("block.vault_thud");

    public static final RegistrySupplier<SoundEvent> SWITCH_ON = registerSoundEvents("block.switch.on");

    // РЕГИСТРАЦИЯ АБСТРАКТНОГО СОБЫТИЯ 
    // Это тот самый звук, который мы будем вызывать в коде.
    // Minecraft сам выберет один из 7 реальных звуков случайным образом.
    public static final RegistrySupplier<SoundEvent> REPAIR_RANDOM = registerSoundEvents("tool.repair_random");
    public static final RegistrySupplier<SoundEvent> EXTRACT_RANDOM = registerSoundEvents("tool.extract_random");
    public static final RegistrySupplier<SoundEvent> ASSEMBLER_STRIKE_RANDOM = registerSoundEvents("block.assembler_strike_random");
    public static final RegistrySupplier<SoundEvent> BOUNCE_RANDOM = registerSoundEvents("item.bounce_random");
    
    // Thermal vision sounds
    public static final RegistrySupplier<SoundEvent> NVG_ON = registerSoundEvents("tool.nvg_on");
    public static final RegistrySupplier<SoundEvent> NVG_OFF = registerSoundEvents("tool.nvg_off");

    // Music discs
    public static final RegistrySupplier<SoundEvent> MUSIC_DISC_BUNKER = registerSoundEvents("music_disc.bunker");
    
    // Вспомогательный метод для регистрации
    private static RegistrySupplier<SoundEvent> registerSoundEvents(String name) {
        // Создаем ResourceLocation, избегая устаревших методов
        //? if fabric && < 1.21.1 {
        /*ResourceLocation id = new ResourceLocation(RefStrings.MODID, name);
        *///?} else {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, name);
        //?}

        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
    
    // Метод для регистрации в Forge EventBus
    public static void init() {
        SOUND_EVENTS.register();
    }
}