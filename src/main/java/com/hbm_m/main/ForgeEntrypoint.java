//? if forge {
package com.hbm_m.main;

import com.hbm_m.api.fluids.ModFluids;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.event.BombDefuser;
import com.hbm_m.event.CrateBreaker;
import com.hbm_m.handler.MobGearHandler;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.radiation.PlayerHandler;
import com.hbm_m.worldgen.ModWorldGen;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(RefStrings.MODID)
public final class ForgeEntrypoint {
    public ForgeEntrypoint() {
        // FML mod bus + явная регистрация в Architectury: иначе DeferredRegister.register() без шины
        // не находит шину по mod id («Can't get event bus for mod … because it was not registered!»).
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(RefStrings.MODID, modBus);
        MainRegistry.init();
        ModWorldGen.register(modBus);
        ModFluids.register(modBus);
        modBus.addListener(ModCapabilities::register);
        modBus.addListener(CreativeModeTabEventHandler::onBuildCreativeModeTabContents);

        MinecraftForge.EVENT_BUS.register(new CrateBreaker());
        MinecraftForge.EVENT_BUS.register(new BombDefuser());
        MinecraftForge.EVENT_BUS.register(new MobGearHandler());
        MinecraftForge.EVENT_BUS.register(new ForgeMainEvents());
        MinecraftForge.EVENT_BUS.register(ChunkRadiationManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new PlayerHandler());
    }
}
//?}
