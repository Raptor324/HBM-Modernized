package com.hbm_m.main;

// Главный класс мода, отвечающий за инициализацию и регистрацию всех систем мода.
// Здесь регистрируются блоки, предметы, меню, вкладки креативногоного режима, звуки, частицы, рецепты, эффекты и тд.
// Также здесь настраиваются обработчики событий и системы радиации.
import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.event.BombDefuser;
import com.hbm_m.event.CrateBreaker;
import com.hbm_m.handler.MobGearHandler;
import com.hbm_m.item.ModBatteryItem;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.util.SellafitSolidificationTracker;
import com.hbm_m.world.biome.ModBiomes;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.LevelEvent;
import com.hbm_m.item.ModBatteryItem;
import com.hbm_m.particle.ModExplosionParticles;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.hbm_m.armormod.item.ItemArmorMod;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.ModIngots;
import com.hbm_m.item.ModPowders;
import com.hbm_m.menu.ModMenuTypes;
import com.hbm_m.particle.ModParticleTypes;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.radiation.PlayerHandler;
import com.hbm_m.recipe.ModRecipes;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.client.ClientSetup;
import com.hbm_m.capability.ChunkRadiationProvider;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.hazard.ModHazards;
import com.hbm_m.worldgen.ModWorldGen;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;

@Mod(RefStrings.MODID)
public class MainRegistry {

    // Добавляем логгер для отладки
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "hbm_m";



    private void registerCapabilities(IEventBus modEventBus) {
        modEventBus.addListener(ModCapabilities::register);
    }


    static {
        // Регистрируем конфиг до любых обращений к нему!
        ModClothConfig.register();
    }

    //ingot
    public MainRegistry(FMLJavaModLoadingContext context) {
        LOGGER.info("Initializing " + RefStrings.NAME);

        IEventBus modEventBus = context.getModEventBus();
        // ПРЯМАЯ РЕГИСТРАЦИЯ DEFERRED REGISTERS
        // Добавь эту:

        MinecraftForge.EVENT_BUS.register(new CrateBreaker());
        MinecraftForge.EVENT_BUS.register(new BombDefuser());
        MinecraftForge.EVENT_BUS.register(new MobGearHandler());
        ModBiomes.BIOMES.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus); // Регистрация наших блоков
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModExplosionParticles.PARTICLE_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus); // Регистрация наших предметов
        ModMenuTypes.MENUS.register(modEventBus); // Регистрация меню
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus); // Регистрация наших вкладок креативного режима
        ModSounds.SOUND_EVENTS.register(modEventBus); // Регистрация звуков
        ModParticleTypes.PARTICLES.register(modEventBus); // Регистрация частиц
        ModBlockEntities.register(modEventBus); // Регистрация блочных сущностей
        ModWorldGen.BIOME_MODIFIERS.register(modEventBus); // Регистрация модификаторов биомов (руды, структуры и тд)
        ModEffects.register(modEventBus); // Регистрация эффектов
        ModRecipes.register(modEventBus);
        registerCapabilities(modEventBus);
        // Регистрация обработчиков событий мода
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        // Регистрация обработчиков событий Forge (игровых)
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(ChunkRadiationManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new PlayerHandler());


        // Регистрация остальных систем resources
        // ModPacketHandler.register(); // Регистрация пакетов

        // Инстанцируем ClientSetup, чтобы его конструктор вызвал регистрацию на Forge Event Bus
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> new ClientSetup());

        LOGGER.info("Radiation handlers registered. Using {}.", ModClothConfig.get().usePrismSystem ? "ChunkRadiationHandlerPRISM" : "ChunkRadiationHandlerSimple");
        LOGGER.info("Registered event listeners for Radiation System.");
        LOGGER.info("!!! MainRegistry: ClientSetup instance created, its Forge listeners should now be registered !!!");
    }


    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModPacketHandler.register();
            ModHazards.registerHazards(); // Регистрация опасностей (радиация, биологическая опасность в будущем и тд)
            // MinecraftForge.EVENT_BUS.addListener(this::onRenderLevelStage);

            LOGGER.info("HazardSystem initialized successfully");
        });
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel) {
            SellafitSolidificationTracker.clearAll();
        }
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



    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ServerLevel level = event.getServer().overworld(); // или через все миры
            EnergyNetworkManager.get(level).tick();
        }
    }

    @SubscribeEvent
    public static void onServerWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            EnergyNetworkManager.get(serverLevel).rebuildAllNetworks();
        }
    }


    // private void onRenderLevelStage(RenderLevelStageEvent event) {
    //     if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
    //         GPUInstancedRenderer.beginInstances("Ring");
    //     }
    //     else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
    //         GPUInstancedRenderer.endInstances();
    //     }
    // }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Логгирование для отладки
        LOGGER.info("Building creative tab contents for: " + event.getTabKey());


        // ТАЙМЕР ЗАКАНЧИВАЕТСЯ, ВЗРЫВЕМСЯ!
        if (event.getTab() == ModCreativeTabs.NTM_WEAPONS_TAB.get()) {

            event.accept(ModBlocks.BARBED_WIRE_FIRE);
            event.accept(ModBlocks.BARBED_WIRE_POISON);
            event.accept(ModBlocks.BARBED_WIRE_RAD);
            event.accept(ModBlocks.BARBED_WIRE_WITHER);
            event.accept(ModBlocks.BARBED_WIRE);

            event.accept(ModItems.DETONATOR);
            event.accept(ModItems.MULTI_DETONATOR);
            event.accept(ModItems.RANGE_DETONATOR);

            event.accept(ModItems.GRENADE);
            event.accept(ModItems.GRENADEHE);
            event.accept(ModItems.GRENADEFIRE);
            event.accept(ModItems.GRENADESMART);
            event.accept(ModItems.GRENADESLIME);

            event.accept(ModItems.GRENADE_IF);
            event.accept(ModItems.GRENADE_IF_HE);
            event.accept(ModItems.GRENADE_IF_SLIME);
            event.accept(ModItems.GRENADE_IF_FIRE);

            event.accept(ModItems.GRENADE_NUC);

            event.accept(ModBlocks.MINE_AP);
            event.accept(ModBlocks.MINE_FAT);

            event.accept(ModBlocks.DET_MINER);
            event.accept(ModBlocks.GIGA_DET);
            event.accept(ModBlocks.WASTE_CHARGE);

            event.accept(ModBlocks.SMOKE_BOMB);
            event.accept(ModBlocks.EXPLOSIVE_CHARGE);
            event.accept(ModBlocks.NUCLEAR_CHARGE);
            event.accept(ModBlocks.C4);

            event.accept(ModBlocks.DUD_FUGAS_TONG);
            event.accept(ModBlocks.DUD_NUKE);
            event.accept(ModBlocks.DUD_SALTED);

            if (ModClothConfig.get().enableDebugLogging) {
                LOGGER.info("Added Alloy Sword to NTM Weapons tab");
            }
        }

        //СЛИТКИ И РЕСУРСЫ
        if (event.getTab() == ModCreativeTabs.NTM_RESOURCES_TAB.get()) {

            event.accept(ModItems.BALL_TNT);
            event.accept(ModItems.ZIRCONIUM_SHARP);
            event.accept(ModItems.BORAX);
            event.accept(ModItems.DUST.get());
            event.accept(ModItems.DUST_TINY.get());
            event.accept(ModItems.CINNABAR);
            event.accept(ModItems.FIRECLAY_BALL);
            event.accept(ModItems.SULFUR);
            event.accept(ModItems.LIGNITE);
            event.accept(ModItems.FLUORITE);
            event.accept(ModItems.RAREGROUND_ORE_CHUNK);
            event.accept(ModItems.FIREBRICK);
            event.accept(ModItems.WOOD_ASH_POWDER);
            event.accept(ModItems.SCRAP);
            event.accept(ModItems.NUGGET_SILICON);
            event.accept(ModItems.BILLET_SILICON);
            event.accept(ModItems.BILLET_PLUTONIUM);
            // Проходимся циклом по ВСЕМ слиткам
            for (RegistryObject<Item> ingotObject : ModItems.INGOTS.values()) {

                event.accept(ingotObject.get());
                if (ModClothConfig.get().enableDebugLogging) {
                    LOGGER.info("Added {} to NTM Resources tab", ingotObject.get());
                }
            }
            for (ModPowders powder : ModPowders.values()) {
                RegistryObject<Item> powderItem = ModItems.getPowders(powder);
                if (powderItem != null) {
                    event.accept(powderItem.get());
                }
            }
            for (ModIngots ingot : ModIngots.values()) {
                RegistryObject<Item> powder = ModItems.getPowder(ingot);
                if (powder != null) {
                    event.accept(powder.get());
                }
                ModItems.getTinyPowder(ingot).ifPresent(tiny -> event.accept(tiny.get()));
            }

        }
        // РАСХОДНИКИ И МОДИФИКАТОРЫ
        if (event.getTab() == ModCreativeTabs.NTM_CONSUMABLES_TAB.get()) {
            // АВТОМАТИЧЕСКОЕ ДОБАВЛЕНИЕ ВСЕХ МОДИФИКАТОРОВ

            // 1. Получаем все зарегистрированные предметы из вашего мода
            List<RegistryObject<Item>> allModItems = ForgeRegistries.ITEMS.getEntries().stream()
                    .filter(entry -> entry.getKey().location().getNamespace().equals(RefStrings.MODID))
                    .map(entry -> RegistryObject.create(entry.getKey().location(), ForgeRegistries.ITEMS))
                    .collect(Collectors.toList());

            // 2. Проходимся по всем предметам и добавляем те, которые являются модификаторами
            for (RegistryObject<Item> itemObject : allModItems) {
                Item item = itemObject.get();
                if (item instanceof ItemArmorMod) { // Проверяем, является ли предмет наследником ItemArmorMod
                    event.accept(item);
                    if (ModClothConfig.get().enableDebugLogging) {
                        LOGGER.info("Automatically added Armor Mod [{}] to NTM Consumables tab", itemObject.getId());
                    }
                }
            }
            event.accept(ModItems.RADAWAY);
            event.accept(ModItems.CAN_KEY);
            event.accept(ModItems.CAN_EMPTY);
            event.accept(ModItems.CANNED_ASBESTOS);
            event.accept(ModItems.CANNED_ASS);
            event.accept(ModItems.CANNED_BARK);
            event.accept(ModItems.CANNED_BEEF);
            event.accept(ModItems.CANNED_BHOLE);
            event.accept(ModItems.CANNED_CHEESE);
            event.accept(ModItems.CANNED_CHINESE);
            event.accept(ModItems.CANNED_DIESEL);
            event.accept(ModItems.CANNED_FIST);
            event.accept(ModItems.CANNED_FRIED);
            event.accept(ModItems.CANNED_HOTDOGS);
            event.accept(ModItems.CANNED_JIZZ);
            event.accept(ModItems.CANNED_KEROSENE);
            event.accept(ModItems.CANNED_LEFTOVERS);
            event.accept(ModItems.CANNED_MILK);
            event.accept(ModItems.CANNED_MYSTERY);
            event.accept(ModItems.CANNED_NAPALM);
            event.accept(ModItems.CANNED_OIL);
            event.accept(ModItems.CANNED_PASHTET);
            event.accept(ModItems.CANNED_PIZZA);
            event.accept(ModItems.CANNED_RECURSION);
            event.accept(ModItems.CANNED_SPAM);
            event.accept(ModItems.CANNED_STEW);
            event.accept(ModItems.CANNED_TOMATO);
            event.accept(ModItems.CANNED_TUNA);
            event.accept(ModItems.CANNED_TUBE);
            event.accept(ModItems.CANNED_YOGURT);
            event.accept(ModItems.CAN_BEPIS);
            event.accept(ModItems.CAN_BREEN);
            event.accept(ModItems.CAN_CREATURE);
            event.accept(ModItems.CAN_LUNA);
            event.accept(ModItems.CAN_MRSUGAR);
            event.accept(ModItems.CAN_MUG);
            event.accept(ModItems.CAN_OVERCHARGE);
            event.accept(ModItems.CAN_REDBOMB);
            event.accept(ModItems.CAN_SMART);
        }

        // ЗАПЧАСТИ
        if (event.getTab() == ModCreativeTabs.NTM_SPAREPARTS_TAB.get()) {


            event.accept(ModItems.BOLT_STEEL);
            event.accept(ModItems.PLATE_IRON);
            event.accept(ModItems.PLATE_STEEL);
            event.accept(ModItems.PLATE_GOLD);
            event.accept(ModItems.PLATE_GUNMETAL);
            event.accept(ModItems.PLATE_GUNSTEEL);
            event.accept(ModItems.PLATE_TITANIUM);
            event.accept(ModItems.PLATE_KEVLAR);
            event.accept(ModItems.PLATE_LEAD);
            event.accept(ModItems.PLATE_MIXED);
            event.accept(ModItems.PLATE_PAA);
            event.accept(ModItems.PLATE_SATURNITE);
            event.accept(ModItems.PLATE_SCHRABIDIUM);
            event.accept(ModItems.PLATE_ADVANCED_ALLOY);
            event.accept(ModItems.PLATE_ALUMINUM);
            event.accept(ModItems.PLATE_COPPER);
            event.accept(ModItems.PLATE_BISMUTH);
            event.accept(ModItems.PLATE_ARMOR_AJR);
            event.accept(ModItems.PLATE_ARMOR_DNT);
            event.accept(ModItems.PLATE_ARMOR_DNT_RUSTED);
            event.accept(ModItems.PLATE_ARMOR_FAU);
            event.accept(ModItems.PLATE_ARMOR_HEV);
            event.accept(ModItems.PLATE_ARMOR_LUNAR);
            event.accept(ModItems.PLATE_ARMOR_TITANIUM);
            event.accept(ModItems.PLATE_CAST);
            event.accept(ModItems.PLATE_CAST_ALT);
            event.accept(ModItems.PLATE_CAST_BISMUTH);
            event.accept(ModItems.PLATE_CAST_DARK);
            event.accept(ModItems.PLATE_COMBINE_STEEL);
            event.accept(ModItems.PLATE_DURA_STEEL);
            event.accept(ModItems.PLATE_DALEKANIUM);
            event.accept(ModItems.PLATE_DESH);
            event.accept(ModItems.PLATE_DINEUTRONIUM);
            event.accept(ModItems.PLATE_EUPHEMIUM);

            event.accept(ModItems.COIL_ADVANCED_ALLOY);
            event.accept(ModItems.COIL_ADVANCED_ALLOY_TORUS);
            event.accept(ModItems.COIL_COPPER);
            event.accept(ModItems.COIL_COPPER_TORUS);
            event.accept(ModItems.COIL_GOLD);
            event.accept(ModItems.COIL_GOLD_TORUS);
            event.accept(ModItems.COIL_MAGNETIZED_TUNGSTEN);
            event.accept(ModItems.COIL_MAGNETIZED_TUNGSTEN_TORUS);
            event.accept(ModItems.WIRE_RED_COPPER);
            event.accept(ModItems.WIRE_COPPER);
            event.accept(ModItems.WIRE_TUNGSTEN);
            event.accept(ModItems.WIRE_GOLD);
            event.accept(ModItems.WIRE_ALUMINIUM);
            event.accept(ModItems.WIRE_MAGNETIZED_TUNGSTEN);
            event.accept(ModItems.WIRE_SCHRABIDIUM);
            event.accept(ModItems.WIRE_FINE);
            event.accept(ModItems.WIRE_CARBON);
            event.accept(ModItems.WIRE_ADVANCED_ALLOY);
            event.accept(ModItems.INSULATOR);
            event.accept(ModItems.SILICON_CIRCUIT);

            event.accept(ModItems.MOTOR_BISMUTH);
            event.accept(ModItems.MOTOR_DESH);
            event.accept(ModItems.MOTOR);
            event.accept(ModItems.CONTROLLER_ADVANCED);
            event.accept(ModItems.CONTROLLER_CHASSIS);
            event.accept(ModItems.CONTROLLER);
            event.accept(ModItems.CAPACITOR_TANTALUM);
            event.accept(ModItems.CAPACITOR_BOARD);
            event.accept(ModItems.QUANTUM_CIRCUIT);
            event.accept(ModItems.QUANTUM_COMPUTER);
            event.accept(ModItems.QUANTUM_CHIP);
            event.accept(ModItems.BISMOID_CHIP);
            event.accept(ModItems.BISMOID_CIRCUIT);
            event.accept(ModItems.PCB);
            event.accept(ModItems.VACUUM_TUBE);
            event.accept(ModItems.CAPACITOR);
            event.accept(ModItems.MICROCHIP);
            event.accept(ModItems.ANALOG_CIRCUIT);
            event.accept(ModItems.INTEGRATED_CIRCUIT);
            event.accept(ModItems.ADVANCED_CIRCUIT);
            event.accept(ModItems.ATOMIC_CLOCK);

            event.accept(ModItems.BATTLE_GEARS);
            event.accept(ModItems.BATTLE_SENSOR);
            event.accept(ModItems.BATTLE_CASING);
            event.accept(ModItems.BATTLE_COUNTER);
            event.accept(ModItems.BATTLE_MODULE);
            event.accept(ModItems.METAL_ROD);

        }
        // РУДЫ
        if (event.getTab() == ModCreativeTabs.NTM_ORES_TAB.get()) {

            event.accept(ModBlocks.DEPTH_BORAX);
            event.accept(ModBlocks.DEPTH_IRON);
            event.accept(ModBlocks.DEPTH_TITANIUM);
            event.accept(ModBlocks.DEPTH_TUNGSTEN);
            event.accept(ModBlocks.DEPTH_STONE);
            event.accept(ModBlocks.DEPTH_CINNABAR);
            event.accept(ModBlocks.DEPTH_ZIRCONIUM);
            event.accept(ModBlocks.BEDROCK_OIL);

            event.accept(ModBlocks.ORE_OIL);
            event.accept(ModBlocks.FLUORITE_ORE);
            event.accept(ModBlocks.LIGNITE_ORE);
            event.accept(ModBlocks.TUNGSTEN_ORE);
            event.accept(ModBlocks.ASBESTOS_ORE);
            event.accept(ModBlocks.SULFUR_ORE);

            event.accept(ModBlocks.ALUMINUM_ORE);
            event.accept(ModBlocks.ALUMINUM_ORE_DEEPSLATE);
            event.accept(ModBlocks.TITANIUM_ORE);
            event.accept(ModBlocks.TITANIUM_ORE_DEEPSLATE);
            event.accept(ModBlocks.COBALT_ORE);
            event.accept(ModBlocks.COBALT_ORE_DEEPSLATE);
            event.accept(ModBlocks.THORIUM_ORE);
            event.accept(ModBlocks.THORIUM_ORE_DEEPSLATE);
            event.accept(ModBlocks.RAREGROUND_ORE);
            event.accept(ModBlocks.RAREGROUND_ORE_DEEPSLATE);
            event.accept(ModBlocks.BERYLLIUM_ORE);
            event.accept(ModBlocks.BERYLLIUM_ORE_DEEPSLATE);
            event.accept(ModBlocks.LEAD_ORE);
            event.accept(ModBlocks.LEAD_ORE_DEEPSLATE);
            event.accept(ModBlocks.CINNABAR_ORE);
            event.accept(ModBlocks.CINNABAR_ORE_DEEPSLATE);
            event.accept(ModBlocks.URANIUM_ORE_DEEPSLATE);

            event.accept(ModBlocks.RESOURCE_ASBESTOS.get());
            event.accept(ModBlocks.RESOURCE_BAUXITE.get());
            event.accept(ModBlocks.RESOURCE_HEMATITE.get());
            event.accept(ModBlocks.RESOURCE_LIMESTONE.get());
            event.accept(ModBlocks.RESOURCE_MALACHITE.get());
            event.accept(ModBlocks.RESOURCE_SULFUR.get());

            event.accept(ModItems.ALUMINUM_RAW);
            event.accept(ModItems.BERYLLIUM_RAW);
            event.accept(ModItems.COBALT_RAW);
            event.accept(ModItems.LEAD_RAW);
            event.accept(ModItems.THORIUM_RAW);
            event.accept(ModItems.TITANIUM_RAW);
            event.accept(ModItems.TUNGSTEN_RAW);
            event.accept(ModItems.URANIUM_RAW);

            event.accept(ModBlocks.SELLAFIELD_SLAKED);
            event.accept(ModBlocks.SELLAFIELD_SLAKED1);
            event.accept(ModBlocks.SELLAFIELD_SLAKED2);
            event.accept(ModBlocks.SELLAFIELD_SLAKED3);
            event.accept(ModBlocks.WASTE_LOG);
            event.accept(ModBlocks.WASTE_PLANKS);
            event.accept(ModBlocks.WASTE_GRASS);
            event.accept(ModBlocks.BURNED_GRASS);
            event.accept(ModBlocks.WASTE_LEAVES);

            event.accept(ModItems.STRAWBERRY);
            event.accept(ModBlocks.STRAWBERRY_BUSH);

            event.accept(ModBlocks.POLONIUM210_BLOCK);
// АВТОМАТИЧЕСКОЕ ДОБАВЛЕНИЕ ВСЕХ БЛОКОВ СЛИТКОВ
            for (ModIngots ingot : ModIngots.values()) {
                RegistryObject<Block> ingotBlock = ModBlocks.getIngotBlock(ingot);
                if (ingotBlock != null) {
                    event.accept(ingotBlock.get());
                    if (ModClothConfig.get().enableDebugLogging) {
                        LOGGER.info("Added {} block to NTM Ores tab", ingotBlock.getId());
                    }
                }
            }
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


        // СТРОИТЕЛЬНЫЕ БЛОКИ
        if (event.getTab() == ModCreativeTabs.NTM_BUILDING_TAB.get()) {


            event.accept(ModBlocks.DECO_STEEL);
            event.accept(ModBlocks.CONCRETE_STAIRS);
            event.accept(ModBlocks.CONCRETE_SLAB);
            event.accept(ModBlocks.CONCRETE);
            event.accept(ModBlocks.CONCRETE_HAZARD);
            event.accept(ModBlocks.CONCRETE_HAZARD_SLAB);
            event.accept(ModBlocks.CONCRETE_HAZARD_STAIRS);
            event.accept(ModBlocks.CONCRETE_MARKED);
            event.accept(ModBlocks.CONCRETE_VENT);
            event.accept(ModBlocks.CONCRETE_FAN);
            event.accept(ModBlocks.CONCRETE_CRACKED_STAIRS);
            event.accept(ModBlocks.CONCRETE_CRACKED_SLAB);
            event.accept(ModBlocks.CONCRETE_CRACKED);
            event.accept(ModBlocks.CONCRETE_MOSSY_STAIRS);
            event.accept(ModBlocks.CONCRETE_MOSSY_SLAB);
            event.accept(ModBlocks.CONCRETE_MOSSY);
            event.accept(ModBlocks.BRICK_CONCRETE);
            event.accept(ModBlocks.BRICK_CONCRETE_SLAB);
            event.accept(ModBlocks.BRICK_CONCRETE_STAIRS);
            event.accept(ModBlocks.BRICK_CONCRETE_BROKEN);
            event.accept(ModBlocks.BRICK_CONCRETE_BROKEN_SLAB);
            event.accept(ModBlocks.BRICK_CONCRETE_BROKEN_STAIRS);
            event.accept(ModBlocks.BRICK_CONCRETE_CRACKED);
            event.accept(ModBlocks.BRICK_CONCRETE_CRACKED_SLAB);
            event.accept(ModBlocks.BRICK_CONCRETE_CRACKED_STAIRS);
            event.accept(ModBlocks.BRICK_CONCRETE_MOSSY);
            event.accept(ModBlocks.BRICK_CONCRETE_MOSSY_SLAB);
            event.accept(ModBlocks.BRICK_CONCRETE_MOSSY_STAIRS);
            event.accept(ModBlocks.BRICK_CONCRETE_MARKED);
            event.accept(ModBlocks.REINFORCED_STONE);
            event.accept(ModBlocks.REINFORCED_STONE_SLAB);
            event.accept(ModBlocks.REINFORCED_STONE_STAIRS);

            event.accept(ModBlocks.REINFORCED_GLASS);

            event.accept(ModBlocks.FREAKY_ALIEN_BLOCK);
            event.accept(ModBlocks.CRATE);
            event.accept(ModBlocks.CRATE_LEAD);
            event.accept(ModBlocks.CRATE_METAL);
            event.accept(ModBlocks.CRATE_WEAPON);
            event.accept(ModBlocks.CRATE_CONSERVE);

            event.accept(ModBlocks.CAGE_LAMP);
            event.accept(ModBlocks.FLOOD_LAMP);

            event.accept(ModBlocks.B29);
            event.accept(ModBlocks.DORNIER);
            event.accept(ModBlocks.FILE_CABINET);
            event.accept(ModBlocks.TAPE_RECORDER);
            event.accept(ModBlocks.CRT_BROKEN);
            event.accept(ModBlocks.CRT_CLEAN);
            event.accept(ModBlocks.CRT_BSOD);
            event.accept(ModBlocks.TOASTER);
            event.accept(ModBlocks.BARREL_PINK);
            event.accept(ModBlocks.BARREL_LOX);
            event.accept(ModBlocks.BARREL_YELLOW);
            event.accept(ModBlocks.BARREL_VITRIFIED);
            event.accept(ModBlocks.BARREL_TAINT);

            event.accept(ModBlocks.DOOR_OFFICE);
            event.accept(ModBlocks.DOOR_BUNKER);
            event.accept(ModBlocks.METAL_DOOR);
            event.accept(ModBlocks.LARGE_VEHICLE_DOOR);
            event.accept(ModBlocks.ROUND_AIRLOCK_DOOR);
            event.accept(ModBlocks.TRANSITION_SEAL);
            event.accept(ModBlocks.FIRE_DOOR);
            event.accept(ModBlocks.SLIDE_DOOR);
            event.accept(ModBlocks.SLIDING_SEAL_DOOR);
            event.accept(ModBlocks.SECURE_ACCESS_DOOR);
            event.accept(ModBlocks.QE_CONTAINMENT);
            event.accept(ModBlocks.QE_SLIDING);
            event.accept(ModBlocks.WATER_DOOR);
            event.accept(ModBlocks.SILO_HATCH);
            event.accept(ModBlocks.SILO_HATCH_LARGE);


            if (ModClothConfig.get().enableDebugLogging) {
                // LOGGER.info("Added concrete hazard to NTM Resources tab");
            }
        }


        // ИНСТРУМЕНТЫ
        if (event.getTab() == ModCreativeTabs.NTM_INSTRUMENTS_TAB.get()) {


            // БРОНЯ
            event.accept(ModItems.TITANIUM_HELMET);
            event.accept(ModItems.TITANIUM_CHESTPLATE);
            event.accept(ModItems.TITANIUM_LEGGINGS);
            event.accept(ModItems.TITANIUM_BOOTS);

            event.accept(ModItems.COBALT_HELMET);
            event.accept(ModItems.COBALT_CHESTPLATE);
            event.accept(ModItems.COBALT_LEGGINGS);
            event.accept(ModItems.COBALT_BOOTS);

            event.accept(ModItems.STEEL_HELMET);
            event.accept(ModItems.STEEL_CHESTPLATE);
            event.accept(ModItems.STEEL_LEGGINGS);
            event.accept(ModItems.STEEL_BOOTS);

            event.accept(ModItems.ALLOY_HELMET);
            event.accept(ModItems.ALLOY_CHESTPLATE);
            event.accept(ModItems.ALLOY_LEGGINGS);
            event.accept(ModItems.ALLOY_BOOTS);

            event.accept(ModItems.STARMETAL_HELMET);
            event.accept(ModItems.STARMETAL_CHESTPLATE);
            event.accept(ModItems.STARMETAL_LEGGINGS);
            event.accept(ModItems.STARMETAL_BOOTS);

            //СПЕЦ БРОНЯ
            event.accept(ModItems.SECURITY_HELMET);
            event.accept(ModItems.SECURITY_CHESTPLATE);
            event.accept(ModItems.SECURITY_LEGGINGS);
            event.accept(ModItems.SECURITY_BOOTS);

            event.accept(ModItems.ASBESTOS_HELMET);
            event.accept(ModItems.ASBESTOS_CHESTPLATE);
            event.accept(ModItems.ASBESTOS_LEGGINGS);
            event.accept(ModItems.ASBESTOS_BOOTS);

            event.accept(ModItems.HAZMAT_HELMET);
            event.accept(ModItems.HAZMAT_CHESTPLATE);
            event.accept(ModItems.HAZMAT_LEGGINGS);
            event.accept(ModItems.HAZMAT_BOOTS);

            event.accept(ModItems.PAA_HELMET);
            event.accept(ModItems.PAA_CHESTPLATE);
            event.accept(ModItems.PAA_LEGGINGS);
            event.accept(ModItems.PAA_BOOTS);

            event.accept(ModItems.LIQUIDATOR_HELMET);
            event.accept(ModItems.LIQUIDATOR_CHESTPLATE);
            event.accept(ModItems.LIQUIDATOR_LEGGINGS);
            event.accept(ModItems.LIQUIDATOR_BOOTS);

            //СИЛОВАЯ БРОНЯ
            event.accept(ModItems.AJR_HELMET);
            event.accept(ModItems.AJR_CHESTPLATE);
            event.accept(ModItems.AJR_LEGGINGS);
            event.accept(ModItems.AJR_BOOTS);

            //МЕЧИ
            event.accept(ModItems.TITANIUM_SWORD);
            event.accept(ModItems.STEEL_SWORD);
            event.accept(ModItems.ALLOY_SWORD);
            event.accept(ModItems.STARMETAL_SWORD);

            //ТОПОРЫ
            event.accept(ModItems.TITANIUM_AXE);
            event.accept(ModItems.STEEL_AXE);
            event.accept(ModItems.ALLOY_AXE);
            event.accept(ModItems.STARMETAL_AXE);

            //КИРКИ
            event.accept(ModItems.TITANIUM_PICKAXE);
            event.accept(ModItems.STEEL_PICKAXE);
            event.accept(ModItems.ALLOY_PICKAXE);
            event.accept(ModItems.STARMETAL_PICKAXE);

            //ЛОПАТЫ
            event.accept(ModItems.TITANIUM_SHOVEL);
            event.accept(ModItems.STEEL_SHOVEL);
            event.accept(ModItems.ALLOY_SHOVEL);
            event.accept(ModItems.STARMETAL_SHOVEL);

            //МОТЫГИ
            event.accept(ModItems.TITANIUM_HOE);
            event.accept(ModItems.STEEL_HOE);
            event.accept(ModItems.ALLOY_HOE);
            event.accept(ModItems.STARMETAL_HOE);

            //СПЕЦ. ИНСТРУМЕНТЫ
            event.accept(ModItems.DEFUSER);
            event.accept(ModItems.CROWBAR);

            event.accept(ModItems.DOSIMETER);
            event.accept(ModItems.GEIGER_COUNTER);
            event.accept(ModBlocks.GEIGER_COUNTER_BLOCK);

            event.accept(ModItems.OIL_DETECTOR);
            event.accept(ModItems.DEPTH_ORES_SCANNER);

            event.accept(ModBlocks.CRATE_IRON);
            event.accept(ModBlocks.CRATE_STEEL);
            event.accept(ModBlocks.CRATE_DESH);
        }

        // СТАНКИ
        if (event.getTab() == ModCreativeTabs.NTM_MACHINES_TAB.get()) {
            event.accept(ModBlocks.BARREL_CORRODED);
            event.accept(ModBlocks.BARREL_IRON);
            event.accept(ModBlocks.BARREL_STEEL);
            event.accept(ModBlocks.BARREL_TCALLOY);
            event.accept(ModBlocks.ANVIL_BLOCK);
            event.accept(ModBlocks.ANVIL_IRON);
            event.accept(ModBlocks.ANVIL_LEAD);
            event.accept(ModBlocks.ANVIL_STEEL);
            event.accept(ModBlocks.ANVIL_DESH);
            event.accept(ModBlocks.ANVIL_FERROURANIUM);
            event.accept(ModBlocks.ANVIL_SATURNITE);
            event.accept(ModBlocks.ANVIL_BISMUTH_BRONZE);
            event.accept(ModBlocks.ANVIL_ARSENIC_BRONZE);
            event.accept(ModBlocks.ANVIL_SCHRABIDATE);
            event.accept(ModBlocks.ANVIL_DNT);
            event.accept(ModBlocks.ANVIL_OSMIRIDIUM);
            event.accept(ModBlocks.ANVIL_MURKY);
            event.accept(ModBlocks.PRESS);
            event.accept(ModBlocks.BLAST_FURNACE);
            event.accept(ModBlocks.BLAST_FURNACE_EXTENSION);
            event.accept(ModBlocks.SHREDDER);
            event.accept(ModBlocks.WOOD_BURNER);
            event.accept(ModBlocks.MACHINE_ASSEMBLER);
            event.accept(ModBlocks.ADVANCED_ASSEMBLY_MACHINE);
            event.accept(ModBlocks.ARMOR_TABLE);

            // event.accept(ModBlocks.FLUID_TANK);
            event.accept(ModBlocks.MACHINE_BATTERY);
            event.accept(ModBlocks.MACHINE_BATTERY_LITHIUM);
            event.accept(ModBlocks.MACHINE_BATTERY_SCHRABIDIUM);
            event.accept(ModBlocks.MACHINE_BATTERY_DINEUTRONIUM);
            event.accept(ModBlocks.CONVERTER_BLOCK);



            event.accept(ModBlocks.WIRE_COATED);
            event.accept(ModBlocks.SWITCH);
            if (ModClothConfig.get().enableDebugLogging) {
                LOGGER.info("Added geiger counter BLOCK to NTM Machines tab");
                LOGGER.info("Added assembly machine BLOCK to NTM Machines tab");
                LOGGER.info("Added advanced assembly machine BLOCK to NTM Machines tab");
                LOGGER.info("Added battery machine BLOCK to NTM Machines tab");
                LOGGER.info("Added wire coated BLOCK to NTM Machines tab");
            }
        }

        // ТОПЛИВО И ЭЛЕМЕНТЫ МЕХАНИЗМОВ
        if (event.getTab() == ModCreativeTabs.NTM_FUEL_TAB.get()) {
            // Сначала добавляем предметы, которые НЕ батарейки


            // Креативная батарейка - отдельный класс, добавляем ее 1 раз
            event.accept(ModItems.CREATIVE_BATTERY);

// --- Новая логика для всех ModBatteryItem ---

// 1. Создаем список всех батареек
            List<RegistryObject<Item>> batteriesToAdd = List.of(
                    ModItems.BATTERY_POTATO,
                    ModItems.BATTERY,
                    ModItems.BATTERY_RED_CELL,
                    ModItems.BATTERY_RED_CELL_6,
                    ModItems.BATTERY_RED_CELL_24,
                    ModItems.BATTERY_ADVANCED,
                    ModItems.BATTERY_ADVANCED_CELL,
                    ModItems.BATTERY_ADVANCED_CELL_4,
                    ModItems.BATTERY_ADVANCED_CELL_12,
                    ModItems.BATTERY_LITHIUM,
                    ModItems.BATTERY_LITHIUM_CELL,
                    ModItems.BATTERY_LITHIUM_CELL_3,
                    ModItems.BATTERY_LITHIUM_CELL_6,
                    ModItems.BATTERY_SCHRABIDIUM,
                    ModItems.BATTERY_SCHRABIDIUM_CELL,
                    ModItems.BATTERY_SCHRABIDIUM_CELL_2,
                    ModItems.BATTERY_SCHRABIDIUM_CELL_4,
                    ModItems.BATTERY_SPARK,
                    ModItems.BATTERY_TRIXITE,
                    ModItems.BATTERY_SPARK_CELL_6,
                    ModItems.BATTERY_SPARK_CELL_25,
                    ModItems.BATTERY_SPARK_CELL_100,
                    ModItems.BATTERY_SPARK_CELL_1000,
                    ModItems.BATTERY_SPARK_CELL_2500,
                    ModItems.BATTERY_SPARK_CELL_10000,
                    ModItems.BATTERY_SPARK_CELL_POWER
            );

// 2. Проходимся по списку и добавляем 2 версии каждой
            for (RegistryObject<Item> batteryRegObj : batteriesToAdd) {
                Item item = batteryRegObj.get();

                // Проверка, что это ModBatteryItem
                if (item instanceof ModBatteryItem batteryItem) {
                    // Добавляем пустую батарею
                    ItemStack emptyStack = new ItemStack(batteryItem);
                    event.accept(emptyStack);

                    // Создаем заряженную батарею
                    ItemStack chargedStack = new ItemStack(batteryItem);
                    ModBatteryItem.setEnergy(chargedStack, batteryItem.getCapacity());
                    event.accept(chargedStack);

                    if (ModClothConfig.get().enableDebugLogging) {
                        LOGGER.debug("Added empty and charged variants of {} to creative tab",
                                batteryRegObj.getId());
                    }
                } else {
                    // На всякий случай, если в списке что-то не ModBatteryItem
                    event.accept(item);
                    LOGGER.warn("Item {} is not a ModBatteryItem, added as regular item",
                            batteryRegObj.getId());
                }
            }
// --- [Конец новой логики] ---

            if (ModClothConfig.get().enableDebugLogging) {
                LOGGER.info("Added {} battery variants to NTM Fuel tab", batteriesToAdd.size() * 2);
            }
        }
        // --- [КОНЕЦ БЛОКА С ИЗМЕНЕНИЯМИ] ---
        if (event.getTab() == ModCreativeTabs.NTM_TEMPLATES_TAB.get()) {

            event.accept(ModItems.BLADE_TEST);
            event.accept(ModItems.STAMP_STONE_FLAT);
            event.accept(ModItems.STAMP_STONE_PLATE);
            event.accept(ModItems.STAMP_STONE_WIRE);
            event.accept(ModItems.STAMP_STONE_CIRCUIT);
            event.accept(ModItems.STAMP_IRON_FLAT);
            event.accept(ModItems.STAMP_IRON_PLATE);
            event.accept(ModItems.STAMP_IRON_WIRE);
            event.accept(ModItems.STAMP_IRON_CIRCUIT);
            event.accept(ModItems.STAMP_IRON_9);
            event.accept(ModItems.STAMP_IRON_44);
            event.accept(ModItems.STAMP_IRON_50);
            event.accept(ModItems.STAMP_IRON_357);
            event.accept(ModItems.STAMP_STEEL_FLAT);
            event.accept(ModItems.STAMP_STEEL_PLATE);
            event.accept(ModItems.STAMP_STEEL_WIRE);
            event.accept(ModItems.STAMP_STEEL_CIRCUIT);
            event.accept(ModItems.STAMP_TITANIUM_FLAT);
            event.accept(ModItems.STAMP_TITANIUM_PLATE);
            event.accept(ModItems.STAMP_TITANIUM_WIRE);
            event.accept(ModItems.STAMP_TITANIUM_FLAT);
            event.accept(ModItems.STAMP_TITANIUM_PLATE);
            event.accept(ModItems.STAMP_TITANIUM_WIRE);
            event.accept(ModItems.STAMP_TITANIUM_CIRCUIT);
            event.accept(ModItems.STAMP_OBSIDIAN_FLAT);
            event.accept(ModItems.STAMP_OBSIDIAN_PLATE);
            event.accept(ModItems.STAMP_OBSIDIAN_WIRE);
            event.accept(ModItems.STAMP_OBSIDIAN_CIRCUIT);
            event.accept(ModItems.STAMP_DESH_FLAT);
            event.accept(ModItems.STAMP_DESH_PLATE);
            event.accept(ModItems.STAMP_DESH_WIRE);
            event.accept(ModItems.STAMP_DESH_CIRCUIT);
            event.accept(ModItems.STAMP_DESH_9);
            event.accept(ModItems.STAMP_DESH_44);
            event.accept(ModItems.STAMP_DESH_50);
            event.accept(ModItems.STAMP_DESH_357);

            event.accept(ModItems.TEMPLATE_FOLDER);
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientSetup.addTemplatesClient(event);
            });
        }
    }
}

