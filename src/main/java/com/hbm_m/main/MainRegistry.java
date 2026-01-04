package com.hbm_m.main;

// Главный класс мода, отвечающий за инициализацию и регистрацию всех систем мода.
// Здесь регистрируются блоки, предметы, меню, вкладки креативногоного режима, звуки, частицы, рецепты, эффекты и тд.
// Также здесь настраиваются обработчики событий и системы радиации.
import com.hbm_m.api.energy.EnergyNetworkManager;
import com.hbm_m.api.fluids.ModFluids;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.event.BombDefuser;
import com.hbm_m.event.CrateBreaker;
import com.hbm_m.handler.MobGearHandler;
import com.hbm_m.item.custom.fekal_electric.ModBatteryItem;
import com.hbm_m.particle.ModExplosionParticles;
import com.hbm_m.world.biome.ModBiomes;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.LevelEvent;
import com.mojang.logging.LogUtils;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import com.hbm_m.block.custom.machines.armormod.item.ItemArmorMod;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModPowders;
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
        ModBlocks.BLOCKS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModExplosionParticles.PARTICLE_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModParticleTypes.PARTICLES.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModWorldGen.BIOME_MODIFIERS.register(modEventBus);
        ModEffects.register(modEventBus);
        ModRecipes.register(modEventBus);
        registerCapabilities(modEventBus);


        // ✅ ЭТА СТРОКА ДОЛЖНА БЫТЬ ПОСЛЕДНЕЙ!
        ModWorldGen.PROCESSORS.register(modEventBus);  // ✅ ОСТАВИ!

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        ModFluids.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(ChunkRadiationManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new PlayerHandler());


        // Регистрация остальных систем resources
        // ModPacketHandler.register(); // Регистрация пакетов


        // Инстанцируем ClientSetup, чтобы его конструктор вызвал регистрацию на Forge Event Bus

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
            event.accept(ModItems.AIRSTRIKE_TEST);
            event.accept(ModItems.AIRSTRIKE_HEAVY);
            event.accept(ModItems.AIRSTRIKE_AGENT);
            event.accept(ModItems.AIRSTRIKE_NUKE);
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
            event.accept(ModBlocks.AIRBOMB);
            event.accept(ModItems.AIRBOMB_A);
            event.accept(ModBlocks.BALEBOMB_TEST);
            event.accept(ModItems.AIRNUKEBOMB_A);
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

        //СЛИТКИ И РЕСУРСЫ...
        if (event.getTab() == ModCreativeTabs.NTM_RESOURCES_TAB.get()) {

            // БАЗОВЫЕ ПРЕДМЕТЫ (все с ItemStack!)
            event.accept(new ItemStack(ModItems.BALL_TNT.get()));
            event.accept(new ItemStack(ModItems.ZIRCONIUM_SHARP.get()));
            event.accept(new ItemStack(ModItems.BORAX.get()));
            event.accept(new ItemStack(ModItems.DUST.get()));
            event.accept(new ItemStack(ModItems.DUST_TINY.get()));
            event.accept(new ItemStack(ModItems.POWDER_COAL.get()));
            event.accept(new ItemStack(ModItems.POWDER_COAL_SMALL.get()));
            event.accept(new ItemStack(ModItems.CINNABAR.get()));
            event.accept(new ItemStack(ModItems.FIRECLAY_BALL.get()));
            event.accept(new ItemStack(ModItems.SULFUR.get()));
            event.accept(new ItemStack(ModItems.SEQUESTRUM.get()));
            event.accept(new ItemStack(ModItems.LIGNITE.get()));
            event.accept(new ItemStack(ModItems.FLUORITE.get()));
            event.accept(new ItemStack(ModItems.RAREGROUND_ORE_CHUNK.get()));
            event.accept(new ItemStack(ModItems.FIREBRICK.get()));
            event.accept(new ItemStack(ModItems.WOOD_ASH_POWDER.get()));
            event.accept(new ItemStack(ModItems.SCRAP.get()));
            event.accept(new ItemStack(ModItems.NUGGET_SILICON.get()));
            event.accept(new ItemStack(ModItems.BILLET_SILICON.get()));
            event.accept(new ItemStack(ModItems.BILLET_PLUTONIUM.get()));
            event.accept(new ItemStack(ModItems.CRUDE_OIL_BUCKET.get()));
                  

            // ✅ СЛИТКИ
            for (ModIngots ingot : ModIngots.values()) {
                RegistryObject<Item> ingotItem = ModItems.getIngot(ingot);
                if (ingotItem != null && ingotItem.isPresent()) {
                    event.accept(new ItemStack(ingotItem.get()));
                }
              
            }

            // ✅ ModPowders
            for (ModPowders powder : ModPowders.values()) {
                RegistryObject<Item> powderItem = ModItems.getPowders(powder);
                if (powderItem != null && powderItem.isPresent()) {
                    event.accept(new ItemStack(powderItem.get()));
                }
            }

            // ✅ ОДИН ЦИКЛ ДЛЯ ВСЕХ ПОРОШКОВ ИЗ СЛИТКОВ (обычные + маленькие)
            for (ModIngots ingot : ModIngots.values()) {
                // Обычный порошок
                RegistryObject<Item> powder = ModItems.getPowder(ingot);
                if (powder != null && powder.isPresent()) {
                    event.accept(new ItemStack(powder.get()));
                }

                // Маленький порошок
                ModItems.getTinyPowder(ingot).ifPresent(tiny -> {
                    if (tiny != null && tiny.isPresent()) {
                        event.accept(new ItemStack(tiny.get()));
                    }
                });
            }
        }

        // РАСХОДНИКИ И МОДИФИКАТОРЫ
        if (event.getTab() == ModCreativeTabs.NTM_CONSUMABLES_TAB.get()) {
            // АВТОМАТИЧЕСКОЕ ДОБАВЛЕНИЕ ВСЕХ МОДИФИКАТОРОВ
            // 1. Получаем все зарегистрированные предметы
            List<RegistryObject<Item>> allModItems = ForgeRegistries.ITEMS.getEntries().stream()
                    .filter(entry -> entry.getKey().location().getNamespace().equals(RefStrings.MODID))
                    .map(entry -> RegistryObject.create(entry.getKey().location(), ForgeRegistries.ITEMS))
                    .collect(Collectors.toList());

            // 2. Проходимся по всем предметам и добавляем те, которые являются модификаторами
            for (RegistryObject<Item> itemObject : allModItems) {
                Item item = itemObject.get();
                if (item instanceof ItemArmorMod) {
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
            event.accept(ModItems.COIL_TUNGSTEN);

            event.accept(ModItems.PLATE_IRON);
            event.accept(ModItems.PLATE_ALUMINUM);
            event.accept(ModItems.PLATE_TITANIUM);
            event.accept(ModItems.PLATE_LEAD);
            event.accept(ModItems.PLATE_COPPER);
            event.accept(ModItems.PLATE_STEEL);
            event.accept(ModItems.PLATE_GOLD);
            event.accept(ModItems.PLATE_ADVANCED_ALLOY);
            event.accept(ModItems.PLATE_GUNMETAL);
            event.accept(ModItems.PLATE_GUNSTEEL);
            event.accept(ModItems.PLATE_DURA_STEEL);
            event.accept(ModItems.PLATE_KEVLAR);
            event.accept(ModItems.PLATE_PAA);
            event.accept(ModItems.PLATE_SCHRABIDIUM);
            event.accept(ModItems.PLATE_SATURNITE);
            event.accept(ModItems.PLATE_COMBINE_STEEL);

            event.accept(ModItems.WIRE_FINE);
            event.accept(ModItems.WIRE_ALUMINIUM);
            event.accept(ModItems.WIRE_CARBON);
            event.accept(ModItems.WIRE_TUNGSTEN);
            event.accept(ModItems.WIRE_GOLD);
            event.accept(ModItems.WIRE_COPPER);
            event.accept(ModItems.WIRE_RED_COPPER);
            event.accept(ModItems.WIRE_ADVANCED_ALLOY);
            event.accept(ModItems.WIRE_MAGNETIZED_TUNGSTEN);
            event.accept(ModItems.WIRE_SCHRABIDIUM);

            event.accept(ModItems.COIL_COPPER);
            event.accept(ModItems.COIL_ADVANCED_ALLOY);
            event.accept(ModItems.COIL_GOLD);
            event.accept(ModItems.COIL_MAGNETIZED_TUNGSTEN);
            event.accept(ModItems.COIL_COPPER_TORUS);
            event.accept(ModItems.COIL_ADVANCED_ALLOY_TORUS);
            event.accept(ModItems.COIL_GOLD_TORUS);
            event.accept(ModItems.COIL_MAGNETIZED_TUNGSTEN_TORUS);

            event.accept(ModItems.PLATE_ARMOR_TITANIUM);
            event.accept(ModItems.PLATE_ARMOR_AJR);
            event.accept(ModItems.PLATE_ARMOR_LUNAR);
            event.accept(ModItems.PLATE_ARMOR_HEV);
            event.accept(ModItems.PLATE_ARMOR_DNT);
            event.accept(ModItems.PLATE_ARMOR_DNT_RUSTED);
            event.accept(ModItems.PLATE_ARMOR_FAU);

            event.accept(ModItems.PLATE_MIXED);
            event.accept(ModItems.PLATE_DALEKANIUM);
            event.accept(ModItems.PLATE_DESH);
            event.accept(ModItems.PLATE_BISMUTH);
            event.accept(ModItems.PLATE_EUPHEMIUM);
            event.accept(ModItems.PLATE_DINEUTRONIUM);

            event.accept(ModItems.PLATE_CAST);
            event.accept(ModItems.PLATE_CAST_ALT);
            event.accept(ModItems.PLATE_CAST_BISMUTH);
            event.accept(ModItems.PLATE_CAST_DARK);

            event.accept(ModItems.MOTOR);
            event.accept(ModItems.MOTOR_DESH);
            event.accept(ModItems.MOTOR_BISMUTH);

            event.accept(ModItems.INSULATOR);
            event.accept(ModItems.SILICON_CIRCUIT);
            event.accept(ModItems.PCB);
            event.accept(ModItems.CRT_DISPLAY);
            event.accept(ModItems.VACUUM_TUBE);
            event.accept(ModItems.CAPACITOR);
            event.accept(ModItems.MICROCHIP);
            event.accept(ModItems.ANALOG_CIRCUIT);
            event.accept(ModItems.INTEGRATED_CIRCUIT);
            event.accept(ModItems.ADVANCED_CIRCUIT);
            event.accept(ModItems.CAPACITOR_BOARD);

            event.accept(ModItems.CONTROLLER_CHASSIS);
            event.accept(ModItems.CONTROLLER);
            event.accept(ModItems.CONTROLLER_ADVANCED);
            event.accept(ModItems.CAPACITOR_TANTALUM);
            event.accept(ModItems.BISMOID_CHIP);
            event.accept(ModItems.BISMOID_CIRCUIT);
            event.accept(ModItems.ATOMIC_CLOCK);
            event.accept(ModItems.QUANTUM_CHIP);
            event.accept(ModItems.QUANTUM_CIRCUIT);
            event.accept(ModItems.QUANTUM_COMPUTER);

            event.accept(ModItems.BATTLE_GEARS);
            event.accept(ModItems.BATTLE_SENSOR);
            event.accept(ModItems.BATTLE_CASING);
            event.accept(ModItems.BATTLE_COUNTER);
            event.accept(ModItems.BATTLE_MODULE);
            event.accept(ModItems.METAL_ROD);
            event.accept(ModItems.MAN_CORE);
        }
        // РУДЫ
        if (event.getTab() == ModCreativeTabs.NTM_ORES_TAB.get()) {

            event.accept(ModBlocks.DEPTH_STONE);
            event.accept(ModBlocks.DEPTH_STONE_NETHER);

            event.accept(ModBlocks.DEPTH_BORAX);
            event.accept(ModBlocks.DEPTH_IRON);
            event.accept(ModBlocks.DEPTH_TITANIUM);
            event.accept(ModBlocks.DEPTH_TUNGSTEN);
            event.accept(ModBlocks.DEPTH_CINNABAR);
            event.accept(ModBlocks.DEPTH_ZIRCONIUM);
            event.accept(ModBlocks.BEDROCK_OIL);

            event.accept(ModBlocks.ORE_OIL);
            event.accept(ModBlocks.GNEISS_STONE);
            event.accept(ModBlocks.FLUORITE_ORE);
            event.accept(ModBlocks.LIGNITE_ORE);
            event.accept(ModBlocks.TUNGSTEN_ORE);
            event.accept(ModBlocks.ASBESTOS_ORE);
            event.accept(ModBlocks.SULFUR_ORE);
            event.accept(ModBlocks.SEQUESTRUM_ORE);

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

            event.accept(ModBlocks.METEOR);
            event.accept(ModBlocks.METEOR_COBBLE);
            event.accept(ModBlocks.METEOR_CRUSHED);
            event.accept(ModBlocks.METEOR_TREASURE);

            event.accept(ModBlocks.GEYSIR_DIRT);
            event.accept(ModBlocks.GEYSIR_STONE);

            event.accept(ModBlocks.NUCLEAR_FALLOUT);
            event.accept(ModBlocks.SELLAFIELD_SLAKED);
            event.accept(ModBlocks.SELLAFIELD_SLAKED1);
            event.accept(ModBlocks.SELLAFIELD_SLAKED2);
            event.accept(ModBlocks.SELLAFIELD_SLAKED3);
            event.accept(ModBlocks.WASTE_LOG);
            event.accept(ModBlocks.WASTE_PLANKS);
            event.accept(ModBlocks.WASTE_GRASS);
            event.accept(ModBlocks.BURNED_GRASS);
            event.accept(ModBlocks.DEAD_DIRT);
            event.accept(ModBlocks.WASTE_LEAVES);

            event.accept(ModItems.STRAWBERRY);
            event.accept(ModBlocks.STRAWBERRY_BUSH);

            event.accept(ModBlocks.POLONIUM210_BLOCK);
// АВТОМАТИЧЕСКОЕ ДОБАВЛЕНИЕ ВСЕХ БЛОКОВ СЛИТКОВ
            for (ModIngots ingot : ModIngots.values()) {

                // !!! ВАЖНОЕ ИСПРАВЛЕНИЕ: ПРОВЕРКА НАЛИЧИЯ БЛОКА !!!
                if (ModBlocks.hasIngotBlock(ingot)) {

                    RegistryObject<Block> ingotBlock = ModBlocks.getIngotBlock(ingot);
                    if (ingotBlock != null) {
                        event.accept(ingotBlock.get());
                        if (ModClothConfig.get().enableDebugLogging) {
                            LOGGER.info("Added {} block to NTM Ores tab", ingotBlock.getId());
                        }
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
            event.accept(ModBlocks.RING_TEST);
            event.accept(ModBlocks.TEST3);
            event.accept(ModBlocks.BLAST_FURNACE2);
            event.accept(ModBlocks.CONCRETE);
            event.accept(ModBlocks.CONCRETE_ASBESTOS);
            event.accept(ModBlocks.CONCRETE_COLORED_SAND);
            event.accept(ModBlocks.CONCRETE_BLACK);
            event.accept(ModBlocks.CONCRETE_BLUE);
            event.accept(ModBlocks.CONCRETE_BROWN);
            event.accept(ModBlocks.CONCRETE_COLORED_INDIGO);
            event.accept(ModBlocks.CONCRETE_COLORED_PINK);
            event.accept(ModBlocks.CONCRETE_COLORED_PURPLE);
            event.accept(ModBlocks.CONCRETE_CYAN);
            event.accept(ModBlocks.CONCRETE_GRAY);
            event.accept(ModBlocks.CONCRETE_GREEN);
            event.accept(ModBlocks.CONCRETE_LIGHT_BLUE);
            event.accept(ModBlocks.CONCRETE_LIME);
            event.accept(ModBlocks.CONCRETE_MAGENTA);
            event.accept(ModBlocks.CONCRETE_ORANGE);
            event.accept(ModBlocks.CONCRETE_PINK);
            event.accept(ModBlocks.CONCRETE_PURPLE);
            event.accept(ModBlocks.CONCRETE_RED);
            event.accept(ModBlocks.CONCRETE_YELLOW);
            event.accept(ModBlocks.CONCRETE_HAZARD);
            event.accept(ModBlocks.CONCRETE_SILVER);
            event.accept(ModBlocks.CONCRETE_WHITE);

            event.accept(ModBlocks.CONCRETE_SUPER);
            event.accept(ModBlocks.CONCRETE_SUPER_M0);
            event.accept(ModBlocks.CONCRETE_SUPER_M1);
            event.accept(ModBlocks.CONCRETE_SUPER_M2);
            event.accept(ModBlocks.CONCRETE_SUPER_M3);
            event.accept(ModBlocks.CONCRETE_SUPER_BROKEN);

            event.accept(ModBlocks.CONCRETE_REBAR);
            event.accept(ModBlocks.CONCRETE_REBAR_ALT);
            event.accept(ModBlocks.CONCRETE_FLAT);
            event.accept(ModBlocks.CONCRETE_TILE);
            event.accept(ModBlocks.CONCRETE_VENT);
            event.accept(ModBlocks.CONCRETE_FAN);
            event.accept(ModBlocks.CONCRETE_TILE_TREFOIL);

            event.accept(ModBlocks.CONCRETE_MOSSY);
            event.accept(ModBlocks.CONCRETE_CRACKED);
            event.accept(ModBlocks.CONCRETE_MARKED);
            event.accept(ModBlocks.BRICK_CONCRETE);
            event.accept(ModBlocks.BRICK_CONCRETE_MOSSY);
            event.accept(ModBlocks.BRICK_CONCRETE_CRACKED);
            event.accept(ModBlocks.BRICK_CONCRETE_BROKEN);
            event.accept(ModBlocks.BRICK_CONCRETE_MARKED);
            event.accept(ModBlocks.CONCRETE_PILLAR);
            event.accept(ModBlocks.CONCRETE_COLORED_MACHINE);
            event.accept(ModBlocks.CONCRETE_COLORED_MACHINE_STRIPE);
            event.accept(ModBlocks.CONCRETE_COLORED_BRONZE);

            // Метеоритные блоки
            event.accept(ModBlocks.METEOR_POLISHED);
            event.accept(ModBlocks.METEOR_BRICK);
            event.accept(ModBlocks.METEOR_BRICK_CRACKED);
            event.accept(ModBlocks.METEOR_BRICK_MOSSY);
            event.accept(ModBlocks.METEOR_BRICK_CHISELED);
            event.accept(ModBlocks.METEOR_PILLAR);

            event.accept(ModBlocks.DEPTH_BRICK);
            event.accept(ModBlocks.DEPTH_TILES);
            event.accept(ModBlocks.DEPTH_NETHER_BRICK);
            event.accept(ModBlocks.DEPTH_NETHER_TILES);
            event.accept(ModBlocks.GNEISS_TILE);
            event.accept(ModBlocks.GNEISS_BRICK);
            event.accept(ModBlocks.GNEISS_CHISELED);

            event.accept(ModBlocks.BRICK_BASE);
            event.accept(ModBlocks.BRICK_LIGHT);
            event.accept(ModBlocks.BARRICADE);
            event.accept(ModBlocks.BRICK_FIRE);
            event.accept(ModBlocks.BRICK_OBSIDIAN);

            event.accept(ModBlocks.VINYL_TILE);
            event.accept(ModBlocks.VINYL_TILE_SMALL);
            event.accept(ModBlocks.REINFORCED_STONE);
            event.accept(ModBlocks.BRICK_DUCRETE);
            event.accept(ModBlocks.ASPHALT);
            event.accept(ModBlocks.BASALT_POLISHED);
            event.accept(ModBlocks.BASALT_BRICK);

            //ПОЛУБЛОКИ
            event.accept(ModBlocks.CONCRETE_ASBESTOS_SLAB);
            event.accept(ModBlocks.CONCRETE_BLACK_SLAB);
            event.accept(ModBlocks.CONCRETE_BLUE_SLAB);
            event.accept(ModBlocks.CONCRETE_BROWN_SLAB);
            event.accept(ModBlocks.CONCRETE_COLORED_BRONZE_SLAB);
            event.accept(ModBlocks.CONCRETE_COLORED_INDIGO_SLAB);
            event.accept(ModBlocks.CONCRETE_COLORED_MACHINE_SLAB);
            event.accept(ModBlocks.CONCRETE_COLORED_PINK_SLAB);
            event.accept(ModBlocks.CONCRETE_COLORED_PURPLE_SLAB);
            event.accept(ModBlocks.CONCRETE_COLORED_SAND_SLAB);
            event.accept(ModBlocks.CONCRETE_CYAN_SLAB);
            event.accept(ModBlocks.CONCRETE_GRAY_SLAB);
            event.accept(ModBlocks.CONCRETE_GREEN_SLAB);
            event.accept(ModBlocks.CONCRETE_LIGHT_BLUE_SLAB);
            event.accept(ModBlocks.CONCRETE_LIME_SLAB);
            event.accept(ModBlocks.CONCRETE_MAGENTA_SLAB);
            event.accept(ModBlocks.CONCRETE_ORANGE_SLAB);
            event.accept(ModBlocks.CONCRETE_PINK_SLAB);
            event.accept(ModBlocks.CONCRETE_PURPLE_SLAB);
            event.accept(ModBlocks.CONCRETE_RED_SLAB);
            event.accept(ModBlocks.CONCRETE_SILVER_SLAB);
            event.accept(ModBlocks.CONCRETE_WHITE_SLAB);
            event.accept(ModBlocks.CONCRETE_YELLOW_SLAB);
            event.accept(ModBlocks.CONCRETE_SUPER_SLAB);
            event.accept(ModBlocks.CONCRETE_SUPER_M0_SLAB);
            event.accept(ModBlocks.CONCRETE_SUPER_M1_SLAB);
            event.accept(ModBlocks.CONCRETE_SUPER_M2_SLAB);
            event.accept(ModBlocks.CONCRETE_SUPER_M3_SLAB);
            event.accept(ModBlocks.CONCRETE_SUPER_BROKEN_SLAB);
            event.accept(ModBlocks.CONCRETE_REBAR_SLAB);
            event.accept(ModBlocks.CONCRETE_FLAT_SLAB);
            event.accept(ModBlocks.CONCRETE_TILE_SLAB);
            event.accept(ModBlocks.DEPTH_BRICK_SLAB);
            event.accept(ModBlocks.DEPTH_TILES_SLAB);
            event.accept(ModBlocks.DEPTH_STONE_NETHER_SLAB);
            event.accept(ModBlocks.DEPTH_NETHER_BRICK_SLAB);
            event.accept(ModBlocks.DEPTH_NETHER_TILES_SLAB);
            event.accept(ModBlocks.GNEISS_TILE_SLAB);
            event.accept(ModBlocks.GNEISS_BRICK_SLAB);
            event.accept(ModBlocks.BRICK_BASE_SLAB);
            event.accept(ModBlocks.BRICK_LIGHT_SLAB);
            event.accept(ModBlocks.BRICK_FIRE_SLAB);
            event.accept(ModBlocks.BRICK_OBSIDIAN_SLAB);
            event.accept(ModBlocks.VINYL_TILE_SLAB);
            event.accept(ModBlocks.VINYL_TILE_SMALL_SLAB);
            event.accept(ModBlocks.BRICK_DUCRETE_SLAB);
            event.accept(ModBlocks.ASPHALT_SLAB);
            event.accept(ModBlocks.BASALT_POLISHED_SLAB);
            event.accept(ModBlocks.BASALT_BRICK_SLAB);
            event.accept(ModBlocks.METEOR_POLISHED_SLAB);
            event.accept(ModBlocks.METEOR_BRICK_SLAB);
            event.accept(ModBlocks.METEOR_BRICK_CRACKED_SLAB);
            event.accept(ModBlocks.METEOR_BRICK_MOSSY_SLAB);
            event.accept(ModBlocks.METEOR_CRUSHED_SLAB);
            event.accept(ModBlocks.BRICK_CONCRETE_SLAB);
            event.accept(ModBlocks.BRICK_CONCRETE_CRACKED_SLAB);
            event.accept(ModBlocks.BRICK_CONCRETE_BROKEN_SLAB);
            event.accept(ModBlocks.BRICK_CONCRETE_MOSSY_SLAB);
            event.accept(ModBlocks.CONCRETE_SLAB);
            event.accept(ModBlocks.CONCRETE_MOSSY_SLAB);
            event.accept(ModBlocks.CONCRETE_CRACKED_SLAB);

            //СТУПЕНИ
            event.accept(ModBlocks.CONCRETE_STAIRS);
            event.accept(ModBlocks.CONCRETE_MOSSY_STAIRS);
            event.accept(ModBlocks.CONCRETE_CRACKED_STAIRS);
            event.accept(ModBlocks.CONCRETE_HAZARD_STAIRS);
            event.accept(ModBlocks.BRICK_CONCRETE_STAIRS);
            event.accept(ModBlocks.BRICK_CONCRETE_MOSSY_STAIRS);
            event.accept(ModBlocks.BRICK_CONCRETE_CRACKED_STAIRS);
            event.accept(ModBlocks.BRICK_CONCRETE_BROKEN_STAIRS);
            event.accept(ModBlocks.CONCRETE_ASBESTOS_STAIRS);
            event.accept(ModBlocks.CONCRETE_BLACK_STAIRS);
            event.accept(ModBlocks.CONCRETE_BLUE_STAIRS);
            event.accept(ModBlocks.CONCRETE_BROWN_STAIRS);
            event.accept(ModBlocks.CONCRETE_COLORED_BRONZE_STAIRS);
            event.accept(ModBlocks.CONCRETE_COLORED_INDIGO_STAIRS);
            event.accept(ModBlocks.CONCRETE_COLORED_MACHINE_STAIRS);
            event.accept(ModBlocks.CONCRETE_COLORED_PINK_STAIRS);
            event.accept(ModBlocks.CONCRETE_COLORED_PURPLE_STAIRS);
            event.accept(ModBlocks.CONCRETE_COLORED_SAND_STAIRS);
            event.accept(ModBlocks.CONCRETE_CYAN_STAIRS);
            event.accept(ModBlocks.CONCRETE_GRAY_STAIRS);
            event.accept(ModBlocks.CONCRETE_GREEN_STAIRS);
            event.accept(ModBlocks.CONCRETE_LIGHT_BLUE_STAIRS);
            event.accept(ModBlocks.CONCRETE_LIME_STAIRS);
            event.accept(ModBlocks.CONCRETE_MAGENTA_STAIRS);
            event.accept(ModBlocks.CONCRETE_ORANGE_STAIRS);
            event.accept(ModBlocks.CONCRETE_PINK_STAIRS);
            event.accept(ModBlocks.CONCRETE_PURPLE_STAIRS);
            event.accept(ModBlocks.CONCRETE_RED_STAIRS);
            event.accept(ModBlocks.CONCRETE_SILVER_STAIRS);
            event.accept(ModBlocks.CONCRETE_WHITE_STAIRS);
            event.accept(ModBlocks.CONCRETE_YELLOW_STAIRS);
            event.accept(ModBlocks.CONCRETE_SUPER_STAIRS);
            event.accept(ModBlocks.CONCRETE_SUPER_M0_STAIRS);
            event.accept(ModBlocks.CONCRETE_SUPER_M1_STAIRS);
            event.accept(ModBlocks.CONCRETE_SUPER_M2_STAIRS);
            event.accept(ModBlocks.CONCRETE_SUPER_M3_STAIRS);
            event.accept(ModBlocks.CONCRETE_SUPER_BROKEN_STAIRS);
            event.accept(ModBlocks.CONCRETE_REBAR_STAIRS);
            event.accept(ModBlocks.CONCRETE_FLAT_STAIRS);
            event.accept(ModBlocks.CONCRETE_TILE_STAIRS);
            event.accept(ModBlocks.DEPTH_BRICK_STAIRS);
            event.accept(ModBlocks.DEPTH_TILES_STAIRS);
            event.accept(ModBlocks.DEPTH_NETHER_BRICK_STAIRS);
            event.accept(ModBlocks.DEPTH_NETHER_TILES_STAIRS);
            event.accept(ModBlocks.GNEISS_TILE_STAIRS);
            event.accept(ModBlocks.GNEISS_BRICK_STAIRS);
            event.accept(ModBlocks.BRICK_BASE_STAIRS);
            event.accept(ModBlocks.BRICK_LIGHT_STAIRS);
            event.accept(ModBlocks.BRICK_FIRE_STAIRS);
            event.accept(ModBlocks.BRICK_OBSIDIAN_STAIRS);
            event.accept(ModBlocks.VINYL_TILE_STAIRS);
            event.accept(ModBlocks.VINYL_TILE_SMALL_STAIRS);
            event.accept(ModBlocks.BRICK_DUCRETE_STAIRS);
            event.accept(ModBlocks.ASPHALT_STAIRS);
            event.accept(ModBlocks.BASALT_POLISHED_STAIRS);
            event.accept(ModBlocks.BASALT_BRICK_STAIRS);
            event.accept(ModBlocks.METEOR_POLISHED_STAIRS);
            event.accept(ModBlocks.METEOR_BRICK_STAIRS);
            event.accept(ModBlocks.METEOR_BRICK_CRACKED_STAIRS);
            event.accept(ModBlocks.METEOR_BRICK_MOSSY_STAIRS);
            event.accept(ModBlocks.METEOR_CRUSHED_STAIRS);


            event.accept(ModBlocks.REINFORCED_STONE_STAIRS);

            //СТЕКЛО
            event.accept(ModBlocks.REINFORCED_GLASS);

            //ЯЩИКИ
            event.accept(ModBlocks.FREAKY_ALIEN_BLOCK);
            event.accept(ModBlocks.CRATE);
            event.accept(ModBlocks.CRATE_LEAD);
            event.accept(ModBlocks.CRATE_METAL);
            event.accept(ModBlocks.CRATE_WEAPON);
            event.accept(ModBlocks.CRATE_CONSERVE);

            //ОСВЕЩЕНИЕ
            event.accept(ModBlocks.CAGE_LAMP);
            event.accept(ModBlocks.FLOOD_LAMP);

            //OBJ-ДЕКОР
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
            event.accept(ModBlocks.FIRE_DOOR);
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

           /* //СИЛОВАЯ БРОНЯ
            event.accept(ModItems.AJR_HELMET);
            event.accept(ModItems.AJR_CHESTPLATE);
            event.accept(ModItems.AJR_LEGGINGS);
            event.accept(ModItems.AJR_BOOTS);*/

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
        }

        // СТАНКИ
        if (event.getTab() == ModCreativeTabs.NTM_MACHINES_TAB.get()) {
            event.accept(ModBlocks.CRATE_IRON);
            event.accept(ModBlocks.CRATE_STEEL);
            event.accept(ModBlocks.BARREL_CORRODED);
            event.accept(ModBlocks.BARREL_IRON);
            event.accept(ModBlocks.BARREL_STEEL);
            event.accept(ModBlocks.BARREL_TCALLOY);
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

            event.accept(ModBlocks.FLUID_TANK);
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

            if (ModClothConfig.get().enableDebugLogging) {
                LOGGER.info("Added {} battery variants to NTM Fuel tab", batteriesToAdd.size() * 2);
            }
        }

        if (event.getTab() == ModCreativeTabs.NTM_TEMPLATES_TAB.get()) {

            event.accept(ModItems.BLADE_STEEL);
            event.accept(ModItems.BLADE_TITANIUM);
            event.accept(ModItems.BLADE_ALLOY);
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

