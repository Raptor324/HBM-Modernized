//src\main\java\com\hbm_m\main\MainRegistry.java
package com.hbm_m.main;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.radiation.PlayerRadiationHandler;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.client.ClientSetup;
import com.hbm_m.capability.ChunkRadiationProvider;
import com.hbm_m.capability.IChunkRadiation;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent; // Добавлен импорт
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(RefStrings.MODID)
public class MainRegistry {
    // Добавляем логгер для отладки
    public static final Logger LOGGER = LogManager.getLogger(RefStrings.MODID);

    public MainRegistry(FMLJavaModLoadingContext context) {
        LOGGER.info("Initializing " + RefStrings.NAME);
        
        IEventBus modEventBus = context.getModEventBus();

        ModBlocks.register(modEventBus); // Регистрация наших блоков
        ModItems.register(modEventBus); // Регистрация наших предметов
        ModCreativeTabs.register(modEventBus); // Регистрация наших вкладок креативного режима
        ModSounds.register(modEventBus); // Регистрация звуков
        ModPacketHandler.register(); // Регистрация пакетов
        // УДАЛЕНО: ModDamageTypes.DAMAGE_TYPES.register(modEventBus); // Регистрация DeferredRegister

        modEventBus.addListener(this::commonSetup); // Добавлен слушатель для FMLCommonSetupEvent
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(ClientSetup::onClientSetupEvent);
        modEventBus.addListener(this::onRegisterCapabilities); // Регистрация Capability
        
        // Инстанцируем ClientSetup, чтобы его конструктор вызвал регистрацию на Forge Event Bus
        new ClientSetup();
        LOGGER.info("!!! MainRegistry: ClientSetup instance created, its Forge listeners should now be registered !!!");

        // Регистрируем обработчики радиации
        registerRadiationHandlers();
        
        // Регистрируем MainRegistry на глобальном Forge Event Bus для игровых событий (AttachCapabilities и др.)
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("Registered event listeners");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // УДАЛЕНО: ModDamageTypes.RADIATION_DAMAGE = ModDamageTypes.DAMAGE_TYPES.register("radiation_damage", () -> new net.minecraft.world.damagesource.DamageType("radiation", 0.1F));
            LOGGER.info("Registered custom damage type: radiation_damage during common setup (data-driven only).");
        });
    }

    private void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IChunkRadiation.class);
        ChunkRadiationProvider.CHUNK_RADIATION_CAPABILITY = CapabilityManager.get(new CapabilityToken<IChunkRadiation>() {});
        LOGGER.info("Registered IChunkRadiation capability and initialized CHUNK_RADIATION_CAPABILITY");
    }

    /**
     * Регистрирует обработчики радиации
     */
    private void registerRadiationHandlers() {
        // Регистрируем обработчики событий для радиации
        MinecraftForge.EVENT_BUS.register(new ChunkRadiationManager());
        MinecraftForge.EVENT_BUS.register(new PlayerRadiationHandler());
        
        LOGGER.info("Radiation handlers registered. Using {}.", com.hbm_m.config.RadiationConfig.usePrismSystem ? "ChunkRadiationHandlerPRISM" : "ChunkRadiationHandlerSimple");
    }

    @SubscribeEvent
    public void onAttachCapabilitiesChunk(AttachCapabilitiesEvent<LevelChunk> event) {
        ResourceLocation chunkRadKey = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "chunk_radiation");
        // MainRegistry.LOGGER.debug("attach capability: chunkPos={}, chunkHash={}", event.getObject().getPos(), System.identityHashCode(event.getObject()));
        if (!event.getCapabilities().containsKey(chunkRadKey)) {
            ChunkRadiationProvider provider = new ChunkRadiationProvider();
            event.addCapability(chunkRadKey, provider);
            MainRegistry.LOGGER.debug("Attached new ChunkRadiationProvider to chunk at {} (hash={}).", event.getObject().getPos(), System.identityHashCode(event.getObject()));
        } else {
            MainRegistry.LOGGER.debug("Chunk at {} (hash={}) already has ChunkRadiationProvider attached.", event.getObject().getPos(), System.identityHashCode(event.getObject()));
        }
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (!event.getLevel().isClientSide() && event.getChunk() instanceof LevelChunk chunk) {
            chunk.getCapability(ChunkRadiationProvider.CHUNK_RADIATION_CAPABILITY).ifPresent(rad -> {
                if (rad.getRadiation() > 0.0F) {
                    LOGGER.debug("Chunk {}:{} loaded with radiation: {}", chunk.getPos().x, chunk.getPos().z, rad.getRadiation());
                }
            });
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (!event.getLevel().isClientSide() && event.getChunk() instanceof LevelChunk chunk) {
            chunk.getCapability(ChunkRadiationProvider.CHUNK_RADIATION_CAPABILITY).ifPresent(rad -> {
                if (rad.getRadiation() > 0.0F) {
                    LOGGER.debug("Chunk {}:{} unloaded with radiation: {}", chunk.getPos().x, chunk.getPos().z, rad.getRadiation());
                }
            });
        }
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Логгирование для отладки
        LOGGER.debug("Building creative tab contents for: " + event.getTabKey());
        
        // Добавляем меч во вкладку оружия NTM
        if (event.getTab() == ModCreativeTabs.NTM_WEAPONS_TAB.get()) {
            event.accept(ModItems.ALLOY_SWORD);
            LOGGER.debug("Added alloy sword to NTM Weapons tab");
        }
        
        // Для совместимости также добавляем его в стандартную вкладку Combat
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.ALLOY_SWORD);
            LOGGER.debug("Added alloy sword to vanilla Combat tab");
        }
        
        // Добавляем урановый слиток в соответствующие вкладки
        if (event.getTab() == ModCreativeTabs.NTM_RESOURCES_TAB.get()) {
            event.accept(ModItems.URANIUM_INGOT);
            LOGGER.debug("Added uranium ingot to NTM Resources tab");
        }
        
        // Добавляем счетчик Гейгера в соответствующие вкладки
        if (event.getTab() == ModCreativeTabs.NTM_CONSUMABLES_TAB.get()) {
            event.accept(ModItems.GEIGER_COUNTER);
            LOGGER.debug("Added geiger counter to NTM Consumables tab");
        }
        
        // Добавляем урановый блок в соответствующие вкладки
        if (event.getTab() == ModCreativeTabs.NTM_RESOURCES_TAB.get()) {
            event.accept(ModBlocks.URANIUM_BLOCK);
            LOGGER.debug("Added uranium block to NTM Resources tab");
        }
        // Здесь будут добавляться остальные предметы и блоки в соответствующие вкладки
        // по мере их создания
    }
}
