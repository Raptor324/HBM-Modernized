//src\main\java\com\hbm_m\main\MainRegistry.java
package com.hbm_m.main;

import com.hbm_m.armormod.item.ItemArmorMod;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.item.ItemAssemblyTemplate;
import com.hbm_m.item.ModItems;
import com.hbm_m.menu.ModMenuTypes;
import com.hbm_m.particle.ModParticleTypes;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.radiation.ChunkRadiationManager;
import com.hbm_m.radiation.PlayerRadiationHandler;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.recipe.ModRecipes;
import com.hbm_m.sound.ModSounds;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.client.ClientSetup;
import com.hbm_m.capability.ChunkRadiationProvider;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.hazard.ModHazards;
import com.hbm_m.worldgen.ModWorldGen;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.List;
import java.util.stream.Collectors;

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
        ModBlockEntities.register(modEventBus); // Регистрация блочных сущностей
        ModWorldGen.BIOME_MODIFIERS.register(modEventBus); // Регистрация модификаторов биомов (руды, структуры и тд)
        ModEffects.register(modEventBus); // Регистрация эффектов
        ModRecipes.register(modEventBus); // Регистрация рецептов

        // Регистрация обработчиков событий мода
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        
        // Регистрация обработчиков событий Forge (игровых)
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(ChunkRadiationManager.INSTANCE);
        MinecraftForge.EVENT_BUS.register(new PlayerRadiationHandler());
        

        // Регистрация остальных систем
        // ModPacketHandler.register(); // Регистрация пакетов
        
        // Инстанцируем ClientSetup, чтобы его конструктор вызвал регистрацию на Forge Event Bus
        new ClientSetup();

        LOGGER.info("Radiation handlers registered. Using {}.", ModClothConfig.get().usePrismSystem ? "ChunkRadiationHandlerPRISM" : "ChunkRadiationHandlerSimple");
        LOGGER.info("Registered event listeners for Radiation System.");
        LOGGER.info("!!! MainRegistry: ClientSetup instance created, its Forge listeners should now be registered !!!");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModPacketHandler.register();
            ModHazards.registerHazards(); // Регистрация опасностей (радиация, биологическая опасность в будущем и тд)
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

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Логгирование для отладки
        LOGGER.info("Building creative tab contents for: " + event.getTabKey());

        if (event.getTab() == ModCreativeTabs.NTM_WEAPONS_TAB.get()) {
            event.accept(ModItems.ALLOY_SWORD);
            if (ModClothConfig.get().enableDebugLogging) {
                LOGGER.info("Added Alloy Sword to NTM Weapons tab");
            }
        }
        
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.ALLOY_SWORD);
            if (ModClothConfig.get().enableDebugLogging) {
                LOGGER.info("Added Alloy Sword to vanilla Combat tab");
            }
        }
        
        if (event.getTab() == ModCreativeTabs.NTM_RESOURCES_TAB.get()) {
            // Проходимся циклом по ВСЕМ сллиткам
            for (RegistryObject<Item> ingotObject : ModItems.INGOTS.values()) {
                event.accept(ingotObject.get());
                if (ModClothConfig.get().enableDebugLogging) {
                    LOGGER.info("Added {} to NTM Resources tab", ingotObject.get());
                }
            }
            event.accept(ModItems.PLATE_IRON);
            event.accept(ModItems.PLATE_STEEL);
            event.accept(ModItems.PLATE_GOLD);
            event.accept(ModItems.PLATE_GUNMETAL);
            event.accept(ModItems.PLATE_GUNSTEEL);
            event.accept(ModItems.PLATE_KEVLAR);
            event.accept(ModItems.PLATE_LEAD);
            event.accept(ModItems.PLATE_MIXED);
            event.accept(ModItems.PLATE_PAA);
            event.accept(ModItems.PLATE_POLYMER);
            event.accept(ModItems.PLATE_SATURNITE);
            event.accept(ModItems.PLATE_SCHRABIDIUM);
        }
        
        if (event.getTab() == ModCreativeTabs.NTM_CONSUMABLES_TAB.get()) {
            // --- АВТОМАТИЧЕСКОЕ ДОБАВЛЕНИЕ ВСЕХ МОДИФИКАТОРОВ ---
            
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
            
            // --- ДОБАВЛЕНИЕ ОСТАЛЬНЫХ ПРЕДМЕТОВ ВРУЧНУЮ ---
            event.accept(ModBlocks.ARMOR_TABLE);
            event.accept(ModItems.DOSIMETER);
            event.accept(ModItems.GEIGER_COUNTER);
            event.accept(ModItems.RADAWAY);
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
        if (event.getTab() == ModCreativeTabs.NTM_MACHINES_TAB.get()) {
            event.accept(ModBlocks.GEIGER_COUNTER_BLOCK);
            event.accept(ModBlocks.MACHINE_ASSEMBLER);
            if (ModClothConfig.get().enableDebugLogging) {
                LOGGER.info("Added geiger counter BLOCK to NTM Machines tab");
                LOGGER.info("Added assembly machine BLOCK to NTM Machines tab");
            }
        }
        if (event.getTab() == ModCreativeTabs.NTM_FUEL_TAB.get()) {
            event.accept(ModItems.CREATIVE_BATTERY);
            if (ModClothConfig.get().enableDebugLogging) {
                LOGGER.info("");
            }
        }

        if (event.getTab() == ModCreativeTabs.NTM_TEMPLATES_TAB.get()) {
            
            event.accept(ModItems.TEMPLATE_FOLDER);

            // --- ИСПРАВЛЕНИЕ: Получаем RecipeManager через клиент Minecraft ---
            // Этот код выполняется на стороне клиента, поэтому такой доступ безопасен
            if (Minecraft.getInstance().level != null) {
                RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();
                List<AssemblerRecipe> recipes = recipeManager.getAllRecipesFor(AssemblerRecipe.Type.INSTANCE);

                for (AssemblerRecipe recipe : recipes) {
                    ItemStack templateStack = new ItemStack(ModItems.ASSEMBLY_TEMPLATE.get());
                    ItemAssemblyTemplate.writeRecipeOutput(templateStack, recipe.getResultItem(null));
                    event.accept(templateStack);
                }
                
                if (ModClothConfig.get().enableDebugLogging) {
                    LOGGER.info("Added {} templates to NTM Templates tab", recipes.size());
                }
            } else {
                 if (ModClothConfig.get().enableDebugLogging) {
                    LOGGER.warn("Could not populate templates tab: Minecraft level is null.");
                }
            }
        }

        if (event.getTab() == ModCreativeTabs.NTM_BOMBS_TAB.get()) {
            if (ModClothConfig.get().enableDebugLogging) {
                LOGGER.info("");
            }
        }
        if (event.getTab() == ModCreativeTabs.NTM_MISSILES_TAB.get()) {
            if (ModClothConfig.get().enableDebugLogging) {
                LOGGER.info("");
            }
        }
    }
}
