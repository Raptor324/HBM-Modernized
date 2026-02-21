package com.hbm_m.main;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;

// Главный класс мода, отвечающий за инициализацию и регистрацию всех систем мода.
// Здесь регистрируются блоки, предметы, меню, вкладки креативногоного режима, звуки, частицы, рецепты, эффекты и тд.
// Также здесь настраиваются обработчики событий и системы радиации.

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.entity.custom.doors.DoorDeclRegistry;
import com.hbm_m.client.ClientSetup;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModPowders;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.world.biome.ModBiomes;
import com.hbm_m.worldgen.ModWorldGen;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(RefStrings.MODID)
public class MainRegistry {

    // Добавляем логгер для отладки
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "hbm_m";


    static {
        // Регистрируем конфиг до любых обращений к нему!
        ModClothConfig.register();
    }

    //ingot
    public MainRegistry(FMLJavaModLoadingContext context) {
        LOGGER.info("Initializing " + RefStrings.NAME);

        IEventBus modEventBus = context.getModEventBus();
        // ПРЯМАЯ РЕГИСТРАЦИЯ DEFERRED REGISTERS
        DoorDeclRegistry.init();

        ModBiomes.BIOMES.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModWorldGen.BIOME_MODIFIERS.register(modEventBus);


        ModWorldGen.PROCESSORS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);

        MinecraftForge.EVENT_BUS.register(this);

        // Регистрация остальных систем resources

        // Инстанцируем ClientSetup, чтобы его конструктор вызвал регистрацию на Forge Event Bus

        LOGGER.info("Radiation handlers registered. Using {}.", ModClothConfig.get().usePrismSystem ? "ChunkRadiationHandlerPRISM" : "ChunkRadiationHandlerSimple");
        LOGGER.info("Registered event listeners for Radiation System.");
        LOGGER.info("!!! MainRegistry: ClientSetup instance created, its Forge listeners should now be registered !!!");
    }


    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModPacketHandler.register();

            LOGGER.info("HazardSystem initialized successfully");
        });
    }



    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Логгирование для отладки
        LOGGER.info("Building creative tab contents for: " + event.getTabKey());

        // СЛИТКИ И РЕСУРСЫ
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
            event.accept(new ItemStack(ModItems.FLUORITE.get()));
            event.accept(new ItemStack(ModItems.RAREGROUND_ORE_CHUNK.get()));
            event.accept(new ItemStack(ModItems.FIREBRICK.get()));
            event.accept(new ItemStack(ModItems.WOOD_ASH_POWDER.get()));
            event.accept(new ItemStack(ModItems.SCRAP.get()));
            event.accept(new ItemStack(ModItems.NUGGET_SILICON.get()));
            event.accept(new ItemStack(ModItems.BILLET_SILICON.get()));
            event.accept(new ItemStack(ModItems.BILLET_PLUTONIUM.get()));



            // Crystals (textures/crystall/*.png)
            event.accept(new ItemStack(ModItems.CRYSTAL_ALUMINIUM.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_BERYLLIUM.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_CHARRED.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_CINNEBAR.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_COAL.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_COBALT.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_COPPER.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_DIAMOND.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_FLUORITE.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_GOLD.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_HARDENED.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_HORN.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_IRON.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_LAPIS.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_LEAD.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_LITHIUM.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_NITER.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_OSMIRIDIUM.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_PHOSPHORUS.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_PLUTONIUM.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_PULSAR.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_RARE.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_REDSTONE.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_SCHRABIDIUM.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_SCHRARANIUM.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_STARMETAL.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_SULFUR.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_THORIUM.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_TITANIUM.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_TRIXITE.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_TUNGSTEN.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_URANIUM.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_VIRUS.get()));
            event.accept(new ItemStack(ModItems.CRYSTAL_XEN.get()));
                  

            // СЛИТКИ
            for (ModIngots ingot : ModIngots.values()) {
                RegistryObject<Item> ingotItem = ModItems.getIngot(ingot);
                if (ingotItem != null && ingotItem.isPresent()) {
                    event.accept(new ItemStack(ingotItem.get()));
                }
              
            }

            // ModPowders
            for (ModPowders powder : ModPowders.values()) {
                RegistryObject<Item> powderItem = ModItems.getPowders(powder);
                if (powderItem != null && powderItem.isPresent()) {
                    event.accept(new ItemStack(powderItem.get()));
                }
            }

            // ОДИН ЦИКЛ ДЛЯ ВСЕХ ПОРОШКОВ ИЗ СЛИТКОВ (обычные + маленькие)
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

            event.accept(ModItems.DEFUSER);
            event.accept(ModItems.CROWBAR);
            event.accept(ModItems.SCREWDRIVER);
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
            event.accept(ModItems.PLATE_FUEL_MOX);
            event.accept(ModItems.PLATE_FUEL_PU238BE);
            event.accept(ModItems.PLATE_FUEL_PU239);
            event.accept(ModItems.PLATE_FUEL_RA226BE);
            event.accept(ModItems.PLATE_FUEL_SA326);
            event.accept(ModItems.PLATE_FUEL_U233);
            event.accept(ModItems.PLATE_FUEL_U235);

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
            event.accept(ModBlocks.URANIUM_ORE);
            event.accept(ModBlocks.URANIUM_ORE_DEEPSLATE);

            event.accept(ModBlocks.RESOURCE_ASBESTOS);
            event.accept(ModBlocks.RESOURCE_BAUXITE);
            event.accept(ModBlocks.RESOURCE_HEMATITE);
            event.accept(ModBlocks.RESOURCE_LIMESTONE);
            event.accept(ModBlocks.RESOURCE_MALACHITE);
            event.accept(ModBlocks.RESOURCE_SULFUR);

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

            // АВТОМАТИЧЕСКОЕ ДОБАВЛЕНИЕ ВСЕХ БЛОКОВ СЛИТКОВ
            for (ModIngots ingot : ModIngots.values()) {

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
            event.accept(ModBlocks.URANIUM_BLOCK);
            event.accept(ModBlocks.PLUTONIUM_BLOCK);
            event.accept(ModBlocks.PLUTONIUM_FUEL_BLOCK);
        }


        // СТРОИТЕЛЬНЫЕ БЛОКИ
        if (event.getTab() == ModCreativeTabs.NTM_BUILDING_TAB.get()) {

            event.accept(ModBlocks.DECO_STEEL);
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
            event.accept(ModBlocks.CONCRETE_HAZARD_SLAB);
            event.accept(ModBlocks.CONCRETE_ASBESTOS_SLAB);
            event.accept(ModBlocks.CONCRETE_BLACK_SLAB);
            event.accept(ModBlocks.CONCRETE_BLUE_SLAB);
            event.accept(ModBlocks.CONCRETE_BROWN_SLAB);
            event.accept(ModBlocks.DEPTH_STONE_SLAB);
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
            event.accept(ModBlocks.REINFORCED_STONE_SLAB);
            event.accept(ModBlocks.BRICK_CONCRETE_SLAB);
            event.accept(ModBlocks.BRICK_CONCRETE_CRACKED_SLAB);
            event.accept(ModBlocks.BRICK_CONCRETE_BROKEN_SLAB);
            event.accept(ModBlocks.BRICK_CONCRETE_MOSSY_SLAB);
            event.accept(ModBlocks.CONCRETE_SLAB);
            event.accept(ModBlocks.CONCRETE_MOSSY_SLAB);
            event.accept(ModBlocks.CONCRETE_CRACKED_SLAB);

            //СТУПЕНИ
            event.accept(ModBlocks.CONCRETE_STAIRS);
            event.accept(ModBlocks.CONCRETE_ASBESTOS_STAIRS);
            event.accept(ModBlocks.CONCRETE_MOSSY_STAIRS);
            event.accept(ModBlocks.CONCRETE_CRACKED_STAIRS);
            event.accept(ModBlocks.CONCRETE_HAZARD_STAIRS);
            event.accept(ModBlocks.BRICK_CONCRETE_STAIRS);
            event.accept(ModBlocks.BRICK_CONCRETE_MOSSY_STAIRS);
            event.accept(ModBlocks.BRICK_CONCRETE_CRACKED_STAIRS);
            event.accept(ModBlocks.BRICK_CONCRETE_BROKEN_STAIRS);
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
            event.accept(ModBlocks.VAULT_DOOR);
            event.accept(ModBlocks.TRANSITION_SEAL);
            event.accept(ModBlocks.SLIDE_DOOR);

            event.accept(ModBlocks.BARREL_RED);
            event.accept(ModBlocks.BARREL_PINK);
            event.accept(ModBlocks.BARREL_TAINT);
            event.accept(ModBlocks.BARREL_LOX);
            event.accept(ModBlocks.BARREL_YELLOW);
            event.accept(ModBlocks.BARREL_VITRIFIED);
            event.accept(ModBlocks.BARBED_WIRE_FIRE);
            event.accept(ModBlocks.BARBED_WIRE_POISON);
            event.accept(ModBlocks.BARBED_WIRE_RAD);
            event.accept(ModBlocks.BARBED_WIRE_WITHER);
            event.accept(ModBlocks.BARBED_WIRE);
            event.accept(ModBlocks.BARREL_CORRODED);
            event.accept(ModBlocks.BARREL_IRON);
            event.accept(ModBlocks.BARREL_STEEL);
            event.accept(ModBlocks.BARREL_TCALLOY);
            event.accept(ModBlocks.BARREL_PLASTIC);
        }
    }
}

