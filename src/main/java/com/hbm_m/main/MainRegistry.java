//src\main\java\com\hbm_m\main\MainRegistry.java
package com.hbm_m.main;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.item.ModItems;
import com.hbm_m.menu.ModMenuTypes;
import com.hbm_m.particle.ModParticleTypes;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.radiation.PlayerRadiationHandler;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.client.ClientSetup;
import com.hbm_m.capability.ChunkRadiationProvider;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.worldgen.ModWorldGen;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
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

    static {
        // Регистрируем конфиг до любых обращений к нему!
        ModClothConfig.register();
    }

    public MainRegistry(FMLJavaModLoadingContext context) {
        LOGGER.info("Initializing " + RefStrings.NAME);
        
        IEventBus modEventBus = context.getModEventBus();

        // --- ПРЯМАЯ РЕГИСТРАЦИЯ DEFERRED REGISTERS ---
        ModBlocks.BLOCKS.register(modEventBus); // Регистрация наших блоков
        ModItems.ITEMS.register(modEventBus); // Регистрация наших предметов
        ModMenuTypes.MENUS.register(modEventBus); // Регистрация меню
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus); // Регистрация наших вкладок креативного режима
        ModSounds.SOUND_EVENTS.register(modEventBus); // Регистрация звуков
        ModParticleTypes.PARTICLES.register(modEventBus); // Регистрация частиц
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus); // Регистрация блочных сущностей
        ModWorldGen.BIOME_MODIFIERS.register(modEventBus); // Регистрация модификаторов биомов (руды, структуры и тд)

        // Регистрация обработчиков событий мода
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        
        // Регистрация обработчиков событий Forge (игровых)
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(ChunkRadiationManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new PlayerRadiationHandler());

        // Регистрация остальных систем
        ModPacketHandler.register(); // Регистрация пакетов
        
        // Инстанцируем ClientSetup, чтобы его конструктор вызвал регистрацию на Forge Event Bus
        new ClientSetup();

        LOGGER.info("Radiation handlers registered. Using {}.", ModClothConfig.get().usePrismSystem ? "ChunkRadiationHandlerPRISM" : "ChunkRadiationHandlerSimple");
        LOGGER.info("Registered event listeners for Radiation System.");
        LOGGER.info("!!! MainRegistry: ClientSetup instance created, its Forge listeners should now be registered !!!");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("commonSetup: Enqueued work running.");
        });
    }

    @SubscribeEvent
    public void onAttachCapabilitiesChunk(AttachCapabilitiesEvent<LevelChunk> event) {
        final ResourceLocation key = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "chunk_radiation");

        // Проверяем, что capability еще не присоединен другим источником (стандартная мера предосторожности).
        if (!event.getCapabilities().containsKey(key)) {
            ChunkRadiationProvider provider = new ChunkRadiationProvider();
            event.addCapability(key, provider);

            // Добавляем слушатель для инвалидации LazyOptional.
            // Когда чанк выгружается, capability становится недействительным. Этот слушатель
            // позаботится о том, чтобы наш LazyOptional тоже был помечен как недействительный,
            // что помогает избежать утечек памяти.
            event.addListener(provider.getCapability(ChunkRadiationProvider.CHUNK_RADIATION_CAPABILITY)::invalidate);
        }
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Логгирование для отладки
        LOGGER.info("Building creative tab contents for: " + event.getTabKey());
        
        // Добавляем меч во вкладку оружия NTM
        if (event.getTab() == ModCreativeTabs.NTM_WEAPONS_TAB.get()) {
            event.accept(ModItems.ALLOY_SWORD);
            if (ModClothConfig.get().enableDebugLogging) {
                LOGGER.info("Added alloy sword to NTM Weapons tab");
            }
        }
        
        // Для совместимости также добавляем его в стандартную вкладку Combat
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.ALLOY_SWORD);
            if (ModClothConfig.get().enableDebugLogging) {
                LOGGER.info("Added alloy sword to vanilla Combat tab");
            }
        }
        
        // Добавляем урановый слиток в соответствующие вкладки
        if (event.getTab() == ModCreativeTabs.NTM_RESOURCES_TAB.get()) {
            event.accept(ModItems.URANIUM_INGOT);
            if (ModClothConfig.get().enableDebugLogging) {
                LOGGER.info("Added uranium ingot to NTM Resources tab");
            }
        }
        
        // Добавляем счетчик Гейгера в соответствующие вкладки
        if (event.getTab() == ModCreativeTabs.NTM_CONSUMABLES_TAB.get()) {
            event.accept(ModItems.GEIGER_COUNTER);
            event.accept(ModItems.DOSIMETER);
            event.accept(ModBlocks.ARMOR_TABLE);
            if (ModClothConfig.get().enableDebugLogging) {
                LOGGER.info("Added dosimeter to NTM Consumables tab");
                LOGGER.info("Added geiger counter to NTM Consumables tab");
                LOGGER.info("Added armor table to NTM Consumables tab");
            }
        }
        
        // Добавляем урановый блок в соответствующие вкладки
        if (event.getTab() == ModCreativeTabs.NTM_ORES_TAB.get()) {
            event.accept(ModBlocks.URANIUM_BLOCK);
            event.accept(ModBlocks.POLONIUM210_BLOCK);
            event.accept(ModBlocks.PLUTONIUM_BLOCK);
            event.accept(ModBlocks.PLUTONIUM_FUEL_BLOCK);
            event.accept(ModBlocks.URANIUM_ORE);
            event.accept(ModBlocks.WASTE_GRASS);
            event.accept(ModBlocks.WASTE_LEAVES);
            if (ModClothConfig.get().enableDebugLogging) {
                LOGGER.info("Added uranium block to NTM Resources tab");
                LOGGER.info("Added polonium210 block to NTM Resources tab");
                LOGGER.info("Added plutonium block to NTM Resources tab");
                LOGGER.info("Added plutonium fuel block to NTM Resources tab");
                LOGGER.info("Added uranium ore to NTM Resources tab");
                LOGGER.info("Added waste leaves block to NTM Resources tab");
                LOGGER.info("Added waste grass block to NTM Resources tab");
            }
        }
    }
}
