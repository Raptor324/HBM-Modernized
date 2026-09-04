package com.hbm_m.test;

import com.hbm_m.lib.RefStrings;

/**
 * Registers GameTest classes on the mod bus.
 *
 * <p>Cross-loader (Forge 1.20.1 / NeoForge 1.21.1) registration via
 * {@code RegisterGameTestsEvent}. Both test classes are registered explicitly
 * through the event — more reliable than auto-discovery and works identically
 * on both versions.
 *
 * <p>On Forge 1.20.1 the event is {@code net.minecraftforge.event.RegisterGameTestsEvent};
 * on NeoForge 1.21.1 it is {@code net.neoforged.neoforge.event.RegisterGameTestsEvent}.
 * Both have {@code event.register(Class)}, but they are DIFFERENT classes, hence the
 * loader gating via stonecutter ({@code //? if forge} / {@code //? if neoforge}).
 *
 * <p>The {@code @EventBusSubscriber} pattern mirrors the reference in
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

@EventBusSubscriber(modid = RefStrings.MODID)
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
        event.register(RadiationObservabilityGameTest.class);
        event.register(SteelCrateGameTest.class);
        event.register(StorageCrateVariantGameTest.class);
        event.register(SandbagGameTest.class);
        event.register(LegacyWoodBarrierGameTest.class);
        event.register(SteelTrapdoorGameTest.class);
        event.register(EnergyNetworkGameTest.class);
        event.register(GasGameTest.class);
        event.register(CableGameTest.class);
    }
    //?} elif neoforge {
    /*@SubscribeEvent
    public static void onRegisterGameTests(RegisterGameTestsEvent event) {
        event.register(PlatformHooksGameTest.class);
        event.register(CrossLoaderParityGameTest.class);
        event.register(RadiationGameTest.class);
        event.register(MachineCraftingGameTest.class);
        event.register(RadiationObservabilityGameTest.class);
        event.register(SteelCrateGameTest.class);
        event.register(StorageCrateVariantGameTest.class);
        event.register(SandbagGameTest.class);
        event.register(LegacyWoodBarrierGameTest.class);
        event.register(SteelTrapdoorGameTest.class);
        event.register(EnergyNetworkGameTest.class);
        event.register(GasGameTest.class);
        event.register(CableGameTest.class);
    }
     *///?}
}



