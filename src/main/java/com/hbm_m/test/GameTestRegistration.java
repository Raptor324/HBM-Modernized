package com.hbm_m.test;

import com.hbm_m.lib.RefStrings;

/**
 * Регистрация GameTest-классов на мод-шине.
 *
 * <p>Кросс-лоадерная (Forge 1.20.1 / NeoForge 1.21.1) регистрация через
 * {@code RegisterGameTestsEvent}. Оба тестовых класса регистрируются явно
 * через событие — это надёжнее авто-дискавери и работает идентично на обеих
 * версиях.
 *
 * <p>На Forge 1.20.1 событие — {@code net.minecraftforge.event.RegisterGameTestsEvent};
 * на NeoForge 1.21.1 — {@code net.neoforged.neoforge.event.RegisterGameTestsEvent}.
 * Оба имеют {@code event.register(Class)}, но это РАЗНЫЕ классы, поэтому gating
 * по лоадеру через stonecutter ({@code //? if forge} / {@code //? if neoforge}).
 *
 * <p>Шаблон {@code @EventBusSubscriber} повторяет эталон из
 * {@link com.hbm_m.radiation.ChunkRadiationManager} / {@link com.hbm_m.client.ClientSetup}:
 * Forge — {@code @Mod.EventBusSubscriber}; NeoForge — {@code @EventBusSubscriber}.
 */
//? if forge {
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.RegisterGameTestsEvent;

@Mod.EventBusSubscriber(modid = RefStrings.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
//?} elif neoforge {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@EventBusSubscriber(modid = RefStrings.MODID, bus = EventBusSubscriber.Bus.MOD)
*///?}
public final class GameTestRegistration {

    private GameTestRegistration() {}

    //? if forge {
    @SubscribeEvent
    public static void onRegisterGameTests(RegisterGameTestsEvent event) {
        event.register(PlatformHooksGameTest.class);
        event.register(CrossLoaderParityGameTest.class);
        event.register(RadiationGameTest.class);
        event.register(MachineCraftingGameTest.class);
    }
    //?} elif neoforge {
    /*@SubscribeEvent
    public static void onRegisterGameTests(RegisterGameTestsEvent event) {
        event.register(PlatformHooksGameTest.class);
        event.register(CrossLoaderParityGameTest.class);
        event.register(RadiationGameTest.class);
        event.register(MachineCraftingGameTest.class);
    }
    *///?}
}
