//? if neoforge {
/*package com.hbm_m.main;

import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.config.MachineConfig;
import com.hbm_m.event.BombDefuser;
import com.hbm_m.event.CrateBreaker;
import com.hbm_m.handler.MobGearHandler;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.radiation.PlayerHandler;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.neoforge.common.NeoForge;

@Mod(RefStrings.MODID)
public final class NeoForgeEntrypoint {
    public NeoForgeEntrypoint() {
        MainRegistry.init();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.register(MachineConfig.class);
        modBus.addListener(ModCapabilities::register);

        NeoForge.EVENT_BUS.register(new CrateBreaker());
        NeoForge.EVENT_BUS.register(new BombDefuser());
        NeoForge.EVENT_BUS.register(new MobGearHandler());
        NeoForge.EVENT_BUS.register(new NeoForgeMainEvents());
        NeoForge.EVENT_BUS.register(ChunkRadiationManager.INSTANCE);
        NeoForge.EVENT_BUS.register(new PlayerHandler());
    }
}

*///?}
