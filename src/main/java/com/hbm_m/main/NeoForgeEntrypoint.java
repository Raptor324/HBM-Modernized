//? if neoforge {
/*package com.hbm_m.main;

import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.capability.ModAttachments;
import com.hbm_m.event.BombDefuser;
import com.hbm_m.event.CrateBreaker;
import com.hbm_m.handler.MobGearHandler;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.radiation.PlayerHandler;
import com.hbm_m.worldgen.ModWorldGen;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(RefStrings.MODID)
public final class NeoForgeEntrypoint {
    public NeoForgeEntrypoint(IEventBus modBus) {
        MainRegistry.init();

        ModAttachments.ATTACHMENT_TYPES.register(modBus);
        modBus.addListener(ModCapabilities::register);

        // Worldgen + fluids registration (Architectury DeferredRegister сам резолвит шину).
        // NeoForge использует datapack biome-modifiers (JSON), поэтому BIOME_MODIFIERS
        // DeferredRegister здесь не нужен — только FEATURES/PROCESSORS.
        ModWorldGen.register();
        ModFluids.register();

        // Creative tabs: BuildCreativeModeTabContentsEvent на MOD-шине.
        modBus.addListener(CreativeModeTabEventHandler::onBuildCreativeModeTabContents);

        // Create compat: регистрация door-behaviours на FMLCommonSetup (guard по isLoaded внутри).
        // Method-ref грузит CreateCompat безопасно (без статических Create-refs); тело commonSetup
        // выполняется только когда Create загружен. Create поддерживает NeoForge с 0.5.1+.
        modBus.addListener(com.hbm_m.compat.create.CreateCompat::commonSetup);

        // Клиентский init через ClientSetup.onClientSetup(FMLClientSetupEvent) регистрируется
        // АВТОМАТИЧЕСКИ аннотацией @EventBusSubscriber(Bus.MOD, Dist.CLIENT) на самом классе
        // ClientSetup — отдельный addListener здесь не нужен (иначе дубликат).
    }
}
*///?}
