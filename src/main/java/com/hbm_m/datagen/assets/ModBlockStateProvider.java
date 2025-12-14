package com.hbm_m.datagen.assets;

// Провайдер генерации состояний блоков и моделей для блоков мода.
// Используется в классе DataGenerators для регистрации.
import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.lib.RefStrings;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.VariantBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockStateProvider extends BlockStateProvider {

    private final ExistingFileHelper existingFileHelper;

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, RefStrings.MODID, exFileHelper);
        this.existingFileHelper = exFileHelper;
    }

    @Override
    protected void registerStatesAndModels() {
        // ГЕНЕРАЦИЯ МОДЕЛЕЙ ДЛЯ БЛОКОВ-РЕСУРСОВ С ПРЕФИКСОМ "block_"
        simpleBlockWithItem(ModBlocks.STRAWBERRY_BUSH.get(), models().cross(blockTexture(ModBlocks.STRAWBERRY_BUSH.get()).getPath(),
                blockTexture(ModBlocks.STRAWBERRY_BUSH.get())).renderType("cutout"));
        // Блоки слитков теперь генерируются автоматически в цикле ниже
        blockWithItem(ModBlocks.GIGA_DET);
        blockWithItem(ModBlocks.POLONIUM210_BLOCK);
        blockWithItem(ModBlocks.EXPLOSIVE_CHARGE);
        blockWithItem(ModBlocks.CRATE_WEAPON);
        blockWithItem(ModBlocks.CRATE_METAL);
        blockWithItem(ModBlocks.CRATE);
        blockWithItem(ModBlocks.CRATE_LEAD);
        blockWithItem(ModBlocks.ASPHALT);
        blockWithItem(ModBlocks.BARRICADE);
        blockWithItem(ModBlocks.DEAD_DIRT);
        blockWithItem(ModBlocks.GEYSIR_DIRT);
        blockWithItem(ModBlocks.GEYSIR_STONE);
        blockWithItem(ModBlocks.BASALT_BRICK);
        blockWithItem(ModBlocks.BASALT_POLISHED);
        blockWithItem(ModBlocks.BRICK_BASE);
        blockWithItem(ModBlocks.BRICK_DUCRETE);
        blockWithItem(ModBlocks.BRICK_FIRE);
        blockWithItem(ModBlocks.BRICK_LIGHT);
        blockWithItem(ModBlocks.BRICK_OBSIDIAN);
        blockWithItem(ModBlocks.CONCRETE_ASBESTOS);
        blockWithItem(ModBlocks.CONCRETE_BLACK);
        blockWithItem(ModBlocks.CONCRETE_BLUE);
        blockWithItem(ModBlocks.CONCRETE_BROWN);
        blockWithItem(ModBlocks.CONCRETE_COLORED_BRONZE);
        blockWithItem(ModBlocks.CONCRETE_COLORED_INDIGO);
        blockWithItem(ModBlocks.CONCRETE_COLORED_MACHINE);
        blockWithItem(ModBlocks.CONCRETE_COLORED_PINK);
        blockWithItem(ModBlocks.CONCRETE_COLORED_PURPLE);
        blockWithItem(ModBlocks.CONCRETE_COLORED_SAND);
        blockWithItem(ModBlocks.CONCRETE_CYAN);
        blockWithItem(ModBlocks.CONCRETE_GRAY);
        blockWithItem(ModBlocks.CONCRETE_GREEN);
        blockWithItem(ModBlocks.CONCRETE_LIGHT_BLUE);
        blockWithItem(ModBlocks.CONCRETE_LIME);
        blockWithItem(ModBlocks.CONCRETE_MAGENTA);
        blockWithItem(ModBlocks.CONCRETE_ORANGE);
        blockWithItem(ModBlocks.CONCRETE_PINK);
        blockWithItem(ModBlocks.CONCRETE_PURPLE);
        blockWithItem(ModBlocks.CONCRETE_REBAR);
        blockWithItem(ModBlocks.CONCRETE_REBAR_ALT);
        blockWithItem(ModBlocks.CONCRETE_RED);
        blockWithItem(ModBlocks.CONCRETE_SILVER);
        blockWithItem(ModBlocks.CONCRETE_SUPER);
        blockWithItem(ModBlocks.CONCRETE_SUPER_BROKEN);
        blockWithItem(ModBlocks.CONCRETE_SUPER_M0);
        blockWithItem(ModBlocks.CONCRETE_SUPER_M1);
        blockWithItem(ModBlocks.CONCRETE_SUPER_M2);
        blockWithItem(ModBlocks.CONCRETE_SUPER_M3);
        blockWithItem(ModBlocks.CONCRETE_TILE);
        blockWithItem(ModBlocks.CONCRETE_TILE_TREFOIL);
        blockWithItem(ModBlocks.CONCRETE_WHITE);
        blockWithItem(ModBlocks.CONCRETE_YELLOW);
        blockWithItem(ModBlocks.CONCRETE_FLAT);
        blockWithItem(ModBlocks.DEPTH_BRICK);
        blockWithItem(ModBlocks.DEPTH_NETHER_BRICK);
        blockWithItem(ModBlocks.DEPTH_NETHER_TILES);
        blockWithItem(ModBlocks.DEPTH_STONE_NETHER);
        blockWithItem(ModBlocks.DEPTH_TILES);
        blockWithItem(ModBlocks.GNEISS_BRICK);
        blockWithItem(ModBlocks.GNEISS_STONE);
        blockWithItem(ModBlocks.GNEISS_TILE);
        blockWithItem(ModBlocks.METEOR);
        blockWithItem(ModBlocks.METEOR_BRICK);
        blockWithItem(ModBlocks.METEOR_BRICK_CRACKED);
        blockWithItem(ModBlocks.METEOR_BRICK_MOSSY);
        blockWithItem(ModBlocks.METEOR_COBBLE);
        blockWithItem(ModBlocks.METEOR_CRUSHED);
        blockWithItem(ModBlocks.METEOR_POLISHED);
        blockWithItem(ModBlocks.METEOR_TREASURE);
        blockWithItem(ModBlocks.VINYL_TILE);
        blockWithItem(ModBlocks.VINYL_TILE_SMALL);
        blockWithItem(ModBlocks.RESOURCE_ASBESTOS);
        blockWithItem(ModBlocks.RESOURCE_BAUXITE);
        blockWithItem(ModBlocks.SEQUESTRUM_ORE);
        blockWithItem(ModBlocks.RESOURCE_HEMATITE);
        blockWithItem(ModBlocks.RESOURCE_LIMESTONE);
        blockWithItem(ModBlocks.RESOURCE_MALACHITE);
        blockWithItem(ModBlocks.RESOURCE_SULFUR);
        blockWithItem(ModBlocks.DEPTH_IRON);
        blockWithItem(ModBlocks.DEPTH_TITANIUM);
        blockWithItem(ModBlocks.DEPTH_TUNGSTEN);
        blockWithItem(ModBlocks.DEPTH_CINNABAR);
        blockWithItem(ModBlocks.DEPTH_ZIRCONIUM);
        blockWithItem(ModBlocks.DEPTH_STONE);
        blockWithItem(ModBlocks.DEPTH_BORAX);
        oreWithItem(ModBlocks.URANIUM_ORE);
        blockWithItem(ModBlocks.WASTE_LEAVES);
        blockWithItem(ModBlocks.ORE_OIL);
        blockWithItem(ModBlocks.BEDROCK_OIL);
        blockWithItem(ModBlocks.REINFORCED_STONE);
        blockWithItem(ModBlocks.CONCRETE_HAZARD);
        blockWithItem(ModBlocks.BRICK_CONCRETE);
        blockWithItem(ModBlocks.BRICK_CONCRETE_BROKEN);
        blockWithItem(ModBlocks.BRICK_CONCRETE_CRACKED);
        blockWithItem(ModBlocks.BRICK_CONCRETE_MOSSY);
        blockWithItem(ModBlocks.CONCRETE_FAN);
        blockWithItem(ModBlocks.CONCRETE_VENT);
        blockWithItem(ModBlocks.CONCRETE_MOSSY);
        blockWithItem(ModBlocks.CONCRETE_CRACKED);
        blockWithItem(ModBlocks.CONCRETE);
        blockWithItem(ModBlocks.SELLAFIELD_SLAKED);
        blockWithItem(ModBlocks.SELLAFIELD_SLAKED1);
        blockWithItem(ModBlocks.SELLAFIELD_SLAKED2);
        blockWithItem(ModBlocks.SELLAFIELD_SLAKED3);

        // ✅ ДОБАВЛЕНО: Модель для ядерных осадков
        // Эта функция автоматически создаст все 8 состояний высоты для блока
        // и свяжет их с моделями, которые выглядят как снег, но с вашей текстурой.
        registerSnowLayerBlock(ModBlocks.NUCLEAR_FALLOUT, "nuclear_fallout");

        // === РЕГИСТРАЦИЯ ПАДАЮЩИХ БЛОКОВ СЕЛЛАФИТА ===
        // Используется simpleBlockWithItem с явным указанием текстуры
        simpleBlockWithItem(ModBlocks.FALLING_SELLAFIT1.get(),
                models().cubeAll(
                        ModBlocks.FALLING_SELLAFIT1.getId().getPath(),
                        modLoc("block/falling_sellafit1")
                )
        );

        simpleBlockWithItem(ModBlocks.FALLING_SELLAFIT2.get(),
                models().cubeAll(
                        ModBlocks.FALLING_SELLAFIT2.getId().getPath(),
                        modLoc("block/falling_sellafit2")
                )
        );

        simpleBlockWithItem(ModBlocks.FALLING_SELLAFIT3.get(),
                models().cubeAll(
                        ModBlocks.FALLING_SELLAFIT3.getId().getPath(),
                        modLoc("block/falling_sellafit3")
                )
        );

        simpleBlockWithItem(ModBlocks.FALLING_SELLAFIT4.get(),
                models().cubeAll(
                        ModBlocks.FALLING_SELLAFIT4.getId().getPath(),
                        modLoc("block/falling_sellafit4")
                )
        );
        // === КОНЕЦ РЕГИСТРАЦИИ ПАДАЮЩИХ БЛОКОВ ===

        blockWithItem(ModBlocks.WASTE_PLANKS);

        simpleBlockWithItem(ModBlocks.WASTE_LOG.get(),
                models().cubeBottomTop(
                        ModBlocks.WASTE_LOG.getId().getPath(),
                        modLoc("block/waste_log_side"),
                        modLoc("block/waste_log_top"),
                        modLoc("block/waste_log_top")
                )
        );

        simpleBlockWithItem(ModBlocks.BURNED_GRASS.get(),
                models().cubeBottomTop(
                        ModBlocks.BURNED_GRASS.getId().getPath(),
                        modLoc("block/burned_grass_side"),
                        modLoc("block/burned_grass_bottom"),
                        modLoc("block/burned_grass_top")
                )
        );
        simpleBlockWithItem(ModBlocks.METEOR_BRICK_CHISELED.get(),
                models().cubeBottomTop(
                        ModBlocks.METEOR_BRICK_CHISELED.getId().getPath(),
                        modLoc("block/meteor_brick_chiseled"),
                        modLoc("block/meteor_brick"),
                        modLoc("block/meteor_brick")
                )
        );
        simpleBlockWithItem(ModBlocks.GNEISS_CHISELED.get(),
                models().cubeBottomTop(
                        ModBlocks.GNEISS_CHISELED.getId().getPath(),
                        modLoc("block/gneiss_chiseled"),
                        modLoc("block/gneiss_stone"),
                        modLoc("block/gneiss_stone")
                )
        );
        simpleBlockWithItem(ModBlocks.BRICK_CONCRETE_MARKED.get(),
                models().cubeBottomTop(
                        ModBlocks.BRICK_CONCRETE_MARKED.getId().getPath(),
                        modLoc("block/brick_concrete_marked"),
                        modLoc("block/brick_concrete"),
                        modLoc("block/brick_concrete")
                )
        );
        simpleBlockWithItem(ModBlocks.CONCRETE_MARKED.get(),
                models().cubeBottomTop(
                        ModBlocks.CONCRETE_MARKED.getId().getPath(),
                        modLoc("block/concrete_marked"),
                        modLoc("block/concrete"),
                        modLoc("block/concrete")
                )
        );
        simpleBlockWithItem(ModBlocks.CONCRETE_PILLAR.get(),
                models().cubeBottomTop(
                        ModBlocks.CONCRETE_PILLAR.getId().getPath(),
                        modLoc("block/concrete_pillar_side"),
                        modLoc("block/concrete_pillar_top"),
                        modLoc("block/concrete_pillar_top")
                )
        );

        simpleBlockWithItem(ModBlocks.CONCRETE_COLORED_MACHINE_STRIPE.get(),
                models().cubeBottomTop(
                        ModBlocks.CONCRETE_COLORED_MACHINE_STRIPE.getId().getPath(),
                        modLoc("block/concrete_colored_machine_stripe"),
                        modLoc("block/concrete_colored_machine"),
                        modLoc("block/concrete_colored_machine")
                )
        );

        simpleBlockWithItem(ModBlocks.METEOR_PILLAR.get(),
                models().cubeBottomTop(
                        ModBlocks.METEOR_PILLAR.getId().getPath(),
                        modLoc("block/meteor_pillar"),
                        modLoc("block/meteor_pillar_top"),
                        modLoc("block/meteor_pillar_top")
                )
        );

        simpleBlock(ModBlocks.BLAST_FURNACE_EXTENSION.get(),
                models().getExistingFile(modLoc("block/difurnace_extension")));
        simpleBlockItem(ModBlocks.BLAST_FURNACE_EXTENSION.get(),
                models().getExistingFile(modLoc("block/difurnace_extension")));

        simpleBlockWithItem(ModBlocks.CRATE_IRON.get(),
                models().cubeBottomTop(
                        ModBlocks.CRATE_IRON.getId().getPath(),
                        modLoc("block/crate_iron_side"),
                        modLoc("block/crate_iron_top"),
                        modLoc("block/crate_iron_top")
                )
        );

        simpleBlockWithItem(ModBlocks.CRATE_STEEL.get(),
                models().cubeBottomTop(
                        ModBlocks.CRATE_STEEL.getId().getPath(),
                        modLoc("block/crate_steel_side"),
                        modLoc("block/crate_steel_top"),
                        modLoc("block/crate_steel_top")
                )
        );

        simpleBlockWithItem(ModBlocks.CRATE_DESH.get(),
                models().cubeBottomTop(
                        ModBlocks.CRATE_DESH.getId().getPath(),
                        modLoc("block/crate_desh_side"),
                        modLoc("block/crate_desh_top"),
                        modLoc("block/crate_desh_top")
                )
        );

        blockWithItem(ModBlocks.CINNABAR_ORE_DEEPSLATE);
        blockWithItem(ModBlocks.COBALT_ORE_DEEPSLATE);

        simpleBlockWithItem(ModBlocks.REINFORCED_GLASS.get(),
                models().cubeAll(ModBlocks.REINFORCED_GLASS.getId().getPath(),
                                blockTexture(ModBlocks.REINFORCED_GLASS.get()))
                        .renderType("cutout"));

        simpleBlockWithItem(ModBlocks.BARBED_WIRE.get(),
                models().cubeAll(ModBlocks.BARBED_WIRE.getId().getPath(),
                                blockTexture(ModBlocks.BARBED_WIRE.get()))
                        .renderType("cutout"));
        simpleBlockWithItem(ModBlocks.BARBED_WIRE_FIRE.get(),
                models().cubeAll(ModBlocks.BARBED_WIRE_FIRE.getId().getPath(),
                                blockTexture(ModBlocks.BARBED_WIRE_FIRE.get()))
                        .renderType("cutout"));
        simpleBlockWithItem(ModBlocks.BARBED_WIRE_POISON.get(),
                models().cubeAll(ModBlocks.BARBED_WIRE_POISON.getId().getPath(),
                                blockTexture(ModBlocks.BARBED_WIRE_POISON.get()))
                        .renderType("cutout"));
        simpleBlockWithItem(ModBlocks.BARBED_WIRE_RAD.get(),
                models().cubeAll(ModBlocks.BARBED_WIRE_RAD.getId().getPath(),
                                blockTexture(ModBlocks.BARBED_WIRE_RAD.get()))
                        .renderType("cutout"));
        simpleBlockWithItem(ModBlocks.BARBED_WIRE_WITHER.get(),
                models().cubeAll(ModBlocks.BARBED_WIRE_WITHER.getId().getPath(),
                                blockTexture(ModBlocks.BARBED_WIRE_WITHER.get()))
                        .renderType("cutout"));







        doorBlockWithRenderType(((DoorBlock) ModBlocks.METAL_DOOR.get()), modLoc("block/metal_door_bottom"), modLoc("block/metal_door_top"), "cutout");
        doorBlockWithRenderType(((DoorBlock) ModBlocks.DOOR_BUNKER.get()), modLoc("block/door_bunker_bottom"), modLoc("block/door_bunker_top"), "cutout");
        doorBlockWithRenderType(((DoorBlock) ModBlocks.DOOR_OFFICE.get()), modLoc("block/door_office_bottom"), modLoc("block/door_office_top"), "cutout");

        columnBlockWithItem(
                ModBlocks.WASTE_GRASS,
                modLoc("block/waste_grass_side"),
                modLoc("block/waste_grass_top"),
                mcLoc("block/dirt")
        );

        columnBlockWithItem(
                ModBlocks.ARMOR_TABLE,
                modLoc("block/armor_table_side"),
                modLoc("block/armor_table_top"),
                modLoc("block/armor_table_bottom")
        );

        // Блоки с кастомной OBJ моделью
        customObjBlock(ModBlocks.GEIGER_COUNTER_BLOCK);
        customObjBlock(ModBlocks.MACHINE_ASSEMBLER);
        customObjBlock(ModBlocks.LARGE_VEHICLE_DOOR);
        customObjBlock(ModBlocks.ROUND_AIRLOCK_DOOR);
        customObjBlock(ModBlocks.TRANSITION_SEAL);
        customObjBlock(ModBlocks.SILO_HATCH);
        customObjBlock(ModBlocks.SILO_HATCH_LARGE);
        customObjBlock(ModBlocks.QE_SLIDING);
        customObjBlock(ModBlocks.QE_CONTAINMENT);
        customObjBlock(ModBlocks.WATER_DOOR);
        customObjBlock(ModBlocks.FIRE_DOOR);
        customObjBlock(ModBlocks.SLIDE_DOOR);
        customObjBlock(ModBlocks.SLIDING_SEAL_DOOR);
        customObjBlock(ModBlocks.SECURE_ACCESS_DOOR);

        simpleBlock(ModBlocks.UNIVERSAL_MACHINE_PART.get(), models().getBuilder(ModBlocks.UNIVERSAL_MACHINE_PART.getId().getPath()));
        simpleBlockWithItem(ModBlocks.WIRE_COATED.get(), models().getExistingFile(modLoc("block/wire_coated")));


        blockWithItem(ModBlocks.CONVERTER_BLOCK);


        orientableBlockWithItem(
                ModBlocks.MACHINE_BATTERY,
                modLoc("block/battery_side_alt"),
                modLoc("block/battery_front_alt"),
                modLoc("block/battery_top")
        );

        orientableBlockWithItem(
                ModBlocks.MACHINE_BATTERY_LITHIUM,
                modLoc("block/machine_battery_lithium_side"),
                modLoc("block/machine_battery_lithium_front"),
                modLoc("block/machine_battery_lithium_top")
        );

        orientableBlockWithItem(
                ModBlocks.MACHINE_BATTERY_SCHRABIDIUM,
                modLoc("block/machine_battery_schrabidium_side"),
                modLoc("block/machine_battery_schrabidium_front"),
                modLoc("block/machine_battery_schrabidium_top")
        );

        orientableBlockWithItem(
                ModBlocks.MACHINE_BATTERY_DINEUTRONIUM,
                modLoc("block/machine_battery_dineutronium_side"),
                modLoc("block/machine_battery_dineutronium_front"),
                modLoc("block/machine_battery_dineutronium_top")
        );

        // Генерация моделей для ступенек
        stairsBlock((StairBlock) ModBlocks.REINFORCED_STONE_STAIRS.get(),
                modLoc("block/reinforced_stone"));
        simpleBlockItem(ModBlocks.REINFORCED_STONE_STAIRS.get(),
                models().getExistingFile(modLoc("block/reinforced_stone_stairs")));

        stairsBlock((StairBlock) ModBlocks.BRICK_CONCRETE_STAIRS.get(),
                modLoc("block/brick_concrete"));
        simpleBlockItem(ModBlocks.BRICK_CONCRETE_STAIRS.get(),
                models().getExistingFile(modLoc("block/brick_concrete_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_STAIRS.get(),
                modLoc("block/concrete"));
        simpleBlockItem(ModBlocks.CONCRETE_STAIRS.get(),
                models().getExistingFile(modLoc("block/concrete_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_CRACKED_STAIRS.get(),
                modLoc("block/concrete_cracked"));
        simpleBlockItem(ModBlocks.CONCRETE_CRACKED_STAIRS.get(),
                models().getExistingFile(modLoc("block/concrete_cracked_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_MOSSY_STAIRS.get(),
                modLoc("block/concrete_mossy"));
        simpleBlockItem(ModBlocks.CONCRETE_MOSSY_STAIRS.get(),
                models().getExistingFile(modLoc("block/concrete_mossy_stairs")));

        stairsBlock((StairBlock) ModBlocks.BRICK_CONCRETE_BROKEN_STAIRS.get(),
                modLoc("block/brick_concrete_broken"));
        simpleBlockItem(ModBlocks.BRICK_CONCRETE_BROKEN_STAIRS.get(),
                models().getExistingFile(modLoc("block/brick_concrete_broken_stairs")));

        stairsBlock((StairBlock) ModBlocks.BRICK_CONCRETE_CRACKED_STAIRS.get(),
                modLoc("block/brick_concrete_cracked"));
        simpleBlockItem(ModBlocks.BRICK_CONCRETE_CRACKED_STAIRS.get(),
                models().getExistingFile(modLoc("block/brick_concrete_cracked_stairs")));

        stairsBlock((StairBlock) ModBlocks.BRICK_CONCRETE_MOSSY_STAIRS.get(),
                modLoc("block/brick_concrete_mossy"));
        simpleBlockItem(ModBlocks.BRICK_CONCRETE_MOSSY_STAIRS.get(),
                models().getExistingFile(modLoc("block/brick_concrete_mossy_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_HAZARD_STAIRS.get(),
                modLoc("block/concrete_hazard"));
        simpleBlockItem(ModBlocks.CONCRETE_HAZARD_STAIRS.get(),
                models().getExistingFile(modLoc("block/concrete_hazard_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_ASBESTOS_STAIRS.get(), modLoc("block/concrete_asbestos"));
        simpleBlockItem(ModBlocks.CONCRETE_ASBESTOS_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_asbestos_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_BLACK_STAIRS.get(), modLoc("block/concrete_black"));
        simpleBlockItem(ModBlocks.CONCRETE_BLACK_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_black_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_BLUE_STAIRS.get(), modLoc("block/concrete_blue"));
        simpleBlockItem(ModBlocks.CONCRETE_BLUE_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_blue_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_BROWN_STAIRS.get(), modLoc("block/concrete_brown"));
        simpleBlockItem(ModBlocks.CONCRETE_BROWN_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_brown_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_COLORED_BRONZE_STAIRS.get(), modLoc("block/concrete_colored_bronze"));
        simpleBlockItem(ModBlocks.CONCRETE_COLORED_BRONZE_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_colored_bronze_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_COLORED_INDIGO_STAIRS.get(), modLoc("block/concrete_colored_indigo"));
        simpleBlockItem(ModBlocks.CONCRETE_COLORED_INDIGO_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_colored_indigo_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_COLORED_MACHINE_STAIRS.get(), modLoc("block/concrete_colored_machine"));
        simpleBlockItem(ModBlocks.CONCRETE_COLORED_MACHINE_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_colored_machine_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_COLORED_PINK_STAIRS.get(), modLoc("block/concrete_colored_pink"));
        simpleBlockItem(ModBlocks.CONCRETE_COLORED_PINK_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_colored_pink_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_COLORED_PURPLE_STAIRS.get(), modLoc("block/concrete_colored_purple"));
        simpleBlockItem(ModBlocks.CONCRETE_COLORED_PURPLE_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_colored_purple_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_COLORED_SAND_STAIRS.get(), modLoc("block/concrete_colored_sand"));
        simpleBlockItem(ModBlocks.CONCRETE_COLORED_SAND_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_colored_sand_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_CYAN_STAIRS.get(), modLoc("block/concrete_cyan"));
        simpleBlockItem(ModBlocks.CONCRETE_CYAN_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_cyan_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_GRAY_STAIRS.get(), modLoc("block/concrete_gray"));
        simpleBlockItem(ModBlocks.CONCRETE_GRAY_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_gray_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_GREEN_STAIRS.get(), modLoc("block/concrete_green"));
        simpleBlockItem(ModBlocks.CONCRETE_GREEN_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_green_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_LIGHT_BLUE_STAIRS.get(), modLoc("block/concrete_light_blue"));
        simpleBlockItem(ModBlocks.CONCRETE_LIGHT_BLUE_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_light_blue_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_LIME_STAIRS.get(), modLoc("block/concrete_lime"));
        simpleBlockItem(ModBlocks.CONCRETE_LIME_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_lime_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_MAGENTA_STAIRS.get(), modLoc("block/concrete_magenta"));
        simpleBlockItem(ModBlocks.CONCRETE_MAGENTA_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_magenta_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_ORANGE_STAIRS.get(), modLoc("block/concrete_orange"));
        simpleBlockItem(ModBlocks.CONCRETE_ORANGE_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_orange_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_PINK_STAIRS.get(), modLoc("block/concrete_pink"));
        simpleBlockItem(ModBlocks.CONCRETE_PINK_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_pink_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_PURPLE_STAIRS.get(), modLoc("block/concrete_purple"));
        simpleBlockItem(ModBlocks.CONCRETE_PURPLE_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_purple_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_RED_STAIRS.get(), modLoc("block/concrete_red"));
        simpleBlockItem(ModBlocks.CONCRETE_RED_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_red_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_SILVER_STAIRS.get(), modLoc("block/concrete_silver"));
        simpleBlockItem(ModBlocks.CONCRETE_SILVER_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_silver_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_WHITE_STAIRS.get(), modLoc("block/concrete_white"));
        simpleBlockItem(ModBlocks.CONCRETE_WHITE_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_white_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_YELLOW_STAIRS.get(), modLoc("block/concrete_yellow"));
        simpleBlockItem(ModBlocks.CONCRETE_YELLOW_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_yellow_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_SUPER_STAIRS.get(), modLoc("block/concrete_super"));
        simpleBlockItem(ModBlocks.CONCRETE_SUPER_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_super_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_SUPER_M0_STAIRS.get(), modLoc("block/concrete_super_m0"));
        simpleBlockItem(ModBlocks.CONCRETE_SUPER_M0_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_super_m0_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_SUPER_M1_STAIRS.get(), modLoc("block/concrete_super_m1"));
        simpleBlockItem(ModBlocks.CONCRETE_SUPER_M1_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_super_m1_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_SUPER_M2_STAIRS.get(), modLoc("block/concrete_super_m2"));
        simpleBlockItem(ModBlocks.CONCRETE_SUPER_M2_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_super_m2_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_SUPER_M3_STAIRS.get(), modLoc("block/concrete_super_m3"));
        simpleBlockItem(ModBlocks.CONCRETE_SUPER_M3_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_super_m3_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_SUPER_BROKEN_STAIRS.get(), modLoc("block/concrete_super_broken"));
        simpleBlockItem(ModBlocks.CONCRETE_SUPER_BROKEN_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_super_broken_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_REBAR_STAIRS.get(), modLoc("block/concrete_rebar"));
        simpleBlockItem(ModBlocks.CONCRETE_REBAR_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_rebar_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_FLAT_STAIRS.get(), modLoc("block/concrete_flat"));
        simpleBlockItem(ModBlocks.CONCRETE_FLAT_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_flat_stairs")));

        stairsBlock((StairBlock) ModBlocks.CONCRETE_TILE_STAIRS.get(), modLoc("block/concrete_tile"));
        simpleBlockItem(ModBlocks.CONCRETE_TILE_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_tile_stairs")));

        stairsBlock((StairBlock) ModBlocks.DEPTH_BRICK_STAIRS.get(), modLoc("block/depth_brick"));
        simpleBlockItem(ModBlocks.DEPTH_BRICK_STAIRS.get(), models().getExistingFile(modLoc("block/depth_brick_stairs")));

        stairsBlock((StairBlock) ModBlocks.DEPTH_TILES_STAIRS.get(), modLoc("block/depth_tiles"));
        simpleBlockItem(ModBlocks.DEPTH_TILES_STAIRS.get(), models().getExistingFile(modLoc("block/depth_tiles_stairs")));

        stairsBlock((StairBlock) ModBlocks.DEPTH_NETHER_BRICK_STAIRS.get(), modLoc("block/depth_nether_brick"));
        simpleBlockItem(ModBlocks.DEPTH_NETHER_BRICK_STAIRS.get(), models().getExistingFile(modLoc("block/depth_nether_brick_stairs")));

        stairsBlock((StairBlock) ModBlocks.DEPTH_NETHER_TILES_STAIRS.get(), modLoc("block/depth_nether_tiles"));
        simpleBlockItem(ModBlocks.DEPTH_NETHER_TILES_STAIRS.get(), models().getExistingFile(modLoc("block/depth_nether_tiles_stairs")));

        stairsBlock((StairBlock) ModBlocks.GNEISS_TILE_STAIRS.get(), modLoc("block/gneiss_tile"));
        simpleBlockItem(ModBlocks.GNEISS_TILE_STAIRS.get(), models().getExistingFile(modLoc("block/gneiss_tile_stairs")));

        stairsBlock((StairBlock) ModBlocks.GNEISS_BRICK_STAIRS.get(), modLoc("block/gneiss_brick"));
        simpleBlockItem(ModBlocks.GNEISS_BRICK_STAIRS.get(), models().getExistingFile(modLoc("block/gneiss_brick_stairs")));

        stairsBlock((StairBlock) ModBlocks.BRICK_BASE_STAIRS.get(), modLoc("block/brick_base"));
        simpleBlockItem(ModBlocks.BRICK_BASE_STAIRS.get(), models().getExistingFile(modLoc("block/brick_base_stairs")));

        stairsBlock((StairBlock) ModBlocks.BRICK_LIGHT_STAIRS.get(), modLoc("block/brick_light"));
        simpleBlockItem(ModBlocks.BRICK_LIGHT_STAIRS.get(), models().getExistingFile(modLoc("block/brick_light_stairs")));

        stairsBlock((StairBlock) ModBlocks.BRICK_FIRE_STAIRS.get(), modLoc("block/brick_fire"));
        simpleBlockItem(ModBlocks.BRICK_FIRE_STAIRS.get(), models().getExistingFile(modLoc("block/brick_fire_stairs")));

        stairsBlock((StairBlock) ModBlocks.BRICK_OBSIDIAN_STAIRS.get(), modLoc("block/brick_obsidian"));
        simpleBlockItem(ModBlocks.BRICK_OBSIDIAN_STAIRS.get(), models().getExistingFile(modLoc("block/brick_obsidian_stairs")));

        stairsBlock((StairBlock) ModBlocks.VINYL_TILE_STAIRS.get(), modLoc("block/vinyl_tile"));
        simpleBlockItem(ModBlocks.VINYL_TILE_STAIRS.get(), models().getExistingFile(modLoc("block/vinyl_tile_stairs")));

        stairsBlock((StairBlock) ModBlocks.VINYL_TILE_SMALL_STAIRS.get(), modLoc("block/vinyl_tile_small"));
        simpleBlockItem(ModBlocks.VINYL_TILE_SMALL_STAIRS.get(), models().getExistingFile(modLoc("block/vinyl_tile_small_stairs")));

        stairsBlock((StairBlock) ModBlocks.BRICK_DUCRETE_STAIRS.get(), modLoc("block/brick_ducrete"));
        simpleBlockItem(ModBlocks.BRICK_DUCRETE_STAIRS.get(), models().getExistingFile(modLoc("block/brick_ducrete_stairs")));

        stairsBlock((StairBlock) ModBlocks.ASPHALT_STAIRS.get(), modLoc("block/asphalt"));
        simpleBlockItem(ModBlocks.ASPHALT_STAIRS.get(), models().getExistingFile(modLoc("block/asphalt_stairs")));

        stairsBlock((StairBlock) ModBlocks.BASALT_POLISHED_STAIRS.get(), modLoc("block/basalt_polished"));
        simpleBlockItem(ModBlocks.BASALT_POLISHED_STAIRS.get(), models().getExistingFile(modLoc("block/basalt_polished_stairs")));

        stairsBlock((StairBlock) ModBlocks.BASALT_BRICK_STAIRS.get(), modLoc("block/basalt_brick"));
        simpleBlockItem(ModBlocks.BASALT_BRICK_STAIRS.get(), models().getExistingFile(modLoc("block/basalt_brick_stairs")));

        stairsBlock((StairBlock) ModBlocks.METEOR_POLISHED_STAIRS.get(), modLoc("block/meteor_polished"));
        simpleBlockItem(ModBlocks.METEOR_POLISHED_STAIRS.get(), models().getExistingFile(modLoc("block/meteor_polished_stairs")));

        stairsBlock((StairBlock) ModBlocks.METEOR_BRICK_STAIRS.get(), modLoc("block/meteor_brick"));
        simpleBlockItem(ModBlocks.METEOR_BRICK_STAIRS.get(), models().getExistingFile(modLoc("block/meteor_brick_stairs")));

        stairsBlock((StairBlock) ModBlocks.METEOR_BRICK_CRACKED_STAIRS.get(), modLoc("block/meteor_brick_cracked"));
        simpleBlockItem(ModBlocks.METEOR_BRICK_CRACKED_STAIRS.get(), models().getExistingFile(modLoc("block/meteor_brick_cracked_stairs")));

        stairsBlock((StairBlock) ModBlocks.METEOR_BRICK_MOSSY_STAIRS.get(), modLoc("block/meteor_brick_mossy"));
        simpleBlockItem(ModBlocks.METEOR_BRICK_MOSSY_STAIRS.get(), models().getExistingFile(modLoc("block/meteor_brick_mossy_stairs")));

        stairsBlock((StairBlock) ModBlocks.METEOR_CRUSHED_STAIRS.get(), modLoc("block/meteor_crushed"));
        simpleBlockItem(ModBlocks.METEOR_CRUSHED_STAIRS.get(), models().getExistingFile(modLoc("block/meteor_crushed_stairs")));

        // Генерация моделей для плит
        slabBlock((SlabBlock) ModBlocks.CONCRETE_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE.get()),
                modLoc("block/concrete"));
        simpleBlockItem(ModBlocks.CONCRETE_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_MOSSY_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_MOSSY.get()),
                modLoc("block/concrete_mossy"));
        simpleBlockItem(ModBlocks.CONCRETE_MOSSY_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_mossy_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_CRACKED_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_CRACKED.get()),
                modLoc("block/concrete_cracked"));
        simpleBlockItem(ModBlocks.CONCRETE_CRACKED_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_cracked_slab")));

        slabBlock((SlabBlock) ModBlocks.REINFORCED_STONE_SLAB.get(),
                blockTexture(ModBlocks.REINFORCED_STONE.get()),
                modLoc("block/reinforced_stone"));
        simpleBlockItem(ModBlocks.REINFORCED_STONE_SLAB.get(),
                models().getExistingFile(modLoc("block/reinforced_stone_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_HAZARD_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_HAZARD.get()),
                modLoc("block/concrete_hazard"));
        simpleBlockItem(ModBlocks.CONCRETE_HAZARD_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_hazard_slab")));

        slabBlock((SlabBlock) ModBlocks.BRICK_CONCRETE_SLAB.get(),
                blockTexture(ModBlocks.BRICK_CONCRETE.get()),
                modLoc("block/brick_concrete"));
        simpleBlockItem(ModBlocks.BRICK_CONCRETE_SLAB.get(),
                models().getExistingFile(modLoc("block/brick_concrete_slab")));

        slabBlock((SlabBlock) ModBlocks.BRICK_CONCRETE_MOSSY_SLAB.get(),
                blockTexture(ModBlocks.BRICK_CONCRETE_MOSSY.get()),
                modLoc("block/brick_concrete_mossy"));
        simpleBlockItem(ModBlocks.BRICK_CONCRETE_MOSSY_SLAB.get(),
                models().getExistingFile(modLoc("block/brick_concrete_mossy_slab")));

        slabBlock((SlabBlock) ModBlocks.BRICK_CONCRETE_CRACKED_SLAB.get(),
                blockTexture(ModBlocks.BRICK_CONCRETE_CRACKED.get()),
                modLoc("block/brick_concrete_cracked"));
        simpleBlockItem(ModBlocks.BRICK_CONCRETE_CRACKED_SLAB.get(),
                models().getExistingFile(modLoc("block/brick_concrete_cracked_slab")));

        slabBlock((SlabBlock) ModBlocks.BRICK_CONCRETE_BROKEN_SLAB.get(),
                blockTexture(ModBlocks.BRICK_CONCRETE_BROKEN.get()),
                modLoc("block/brick_concrete_broken"));
        simpleBlockItem(ModBlocks.BRICK_CONCRETE_BROKEN_SLAB.get(),
                models().getExistingFile(modLoc("block/brick_concrete_broken_slab")));

        slabBlock((SlabBlock) ModBlocks.ASPHALT_SLAB.get(),
                blockTexture(ModBlocks.ASPHALT.get()),
                modLoc("block/asphalt"));
        simpleBlockItem(ModBlocks.ASPHALT_SLAB.get(),
                models().getExistingFile(modLoc("block/asphalt_slab")));

        slabBlock((SlabBlock) ModBlocks.BASALT_BRICK_SLAB.get(),
                blockTexture(ModBlocks.BASALT_BRICK.get()),
                modLoc("block/basalt_brick"));
        simpleBlockItem(ModBlocks.BASALT_BRICK_SLAB.get(),
                models().getExistingFile(modLoc("block/basalt_brick_slab")));

        slabBlock((SlabBlock) ModBlocks.BASALT_POLISHED_SLAB.get(),
                blockTexture(ModBlocks.BASALT_POLISHED.get()),
                modLoc("block/basalt_polished"));
        simpleBlockItem(ModBlocks.BASALT_POLISHED_SLAB.get(),
                models().getExistingFile(modLoc("block/basalt_polished_slab")));

        slabBlock((SlabBlock) ModBlocks.BRICK_BASE_SLAB.get(),
                blockTexture(ModBlocks.BRICK_BASE.get()),
                modLoc("block/brick_base"));
        simpleBlockItem(ModBlocks.BRICK_BASE_SLAB.get(),
                models().getExistingFile(modLoc("block/brick_base_slab")));

        slabBlock((SlabBlock) ModBlocks.BRICK_DUCRETE_SLAB.get(),
                blockTexture(ModBlocks.BRICK_DUCRETE.get()),
                modLoc("block/brick_ducrete"));
        simpleBlockItem(ModBlocks.BRICK_DUCRETE_SLAB.get(),
                models().getExistingFile(modLoc("block/brick_ducrete_slab")));

        slabBlock((SlabBlock) ModBlocks.BRICK_FIRE_SLAB.get(),
                blockTexture(ModBlocks.BRICK_FIRE.get()),
                modLoc("block/brick_fire"));
        simpleBlockItem(ModBlocks.BRICK_FIRE_SLAB.get(),
                models().getExistingFile(modLoc("block/brick_fire_slab")));

        slabBlock((SlabBlock) ModBlocks.BRICK_LIGHT_SLAB.get(),
                blockTexture(ModBlocks.BRICK_LIGHT.get()),
                modLoc("block/brick_light"));
        simpleBlockItem(ModBlocks.BRICK_LIGHT_SLAB.get(),
                models().getExistingFile(modLoc("block/brick_light_slab")));

        slabBlock((SlabBlock) ModBlocks.BRICK_OBSIDIAN_SLAB.get(),
                blockTexture(ModBlocks.BRICK_OBSIDIAN.get()),
                modLoc("block/brick_obsidian"));
        simpleBlockItem(ModBlocks.BRICK_OBSIDIAN_SLAB.get(),
                models().getExistingFile(modLoc("block/brick_obsidian_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_ASBESTOS_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_ASBESTOS.get()),
                modLoc("block/concrete_asbestos"));
        simpleBlockItem(ModBlocks.CONCRETE_ASBESTOS_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_asbestos_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_BLACK_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_BLACK.get()),
                modLoc("block/concrete_black"));
        simpleBlockItem(ModBlocks.CONCRETE_BLACK_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_black_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_BLUE_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_BLUE.get()),
                modLoc("block/concrete_blue"));
        simpleBlockItem(ModBlocks.CONCRETE_BLUE_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_blue_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_BROWN_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_BROWN.get()),
                modLoc("block/concrete_brown"));
        simpleBlockItem(ModBlocks.CONCRETE_BROWN_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_brown_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_COLORED_BRONZE_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_COLORED_BRONZE.get()),
                modLoc("block/concrete_colored_bronze"));
        simpleBlockItem(ModBlocks.CONCRETE_COLORED_BRONZE_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_colored_bronze_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_COLORED_INDIGO_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_COLORED_INDIGO.get()),
                modLoc("block/concrete_colored_indigo"));
        simpleBlockItem(ModBlocks.CONCRETE_COLORED_INDIGO_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_colored_indigo_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_COLORED_MACHINE_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_COLORED_MACHINE.get()),
                modLoc("block/concrete_colored_machine"));
        simpleBlockItem(ModBlocks.CONCRETE_COLORED_MACHINE_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_colored_machine_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_COLORED_PINK_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_COLORED_PINK.get()),
                modLoc("block/concrete_colored_pink"));
        simpleBlockItem(ModBlocks.CONCRETE_COLORED_PINK_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_colored_pink_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_COLORED_PURPLE_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_COLORED_PURPLE.get()),
                modLoc("block/concrete_colored_purple"));
        simpleBlockItem(ModBlocks.CONCRETE_COLORED_PURPLE_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_colored_purple_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_COLORED_SAND_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_COLORED_SAND.get()),
                modLoc("block/concrete_colored_sand"));
        simpleBlockItem(ModBlocks.CONCRETE_COLORED_SAND_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_colored_sand_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_CYAN_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_CYAN.get()),
                modLoc("block/concrete_cyan"));
        simpleBlockItem(ModBlocks.CONCRETE_CYAN_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_cyan_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_GRAY_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_GRAY.get()),
                modLoc("block/concrete_gray"));
        simpleBlockItem(ModBlocks.CONCRETE_GRAY_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_gray_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_GREEN_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_GREEN.get()),
                modLoc("block/concrete_green"));
        simpleBlockItem(ModBlocks.CONCRETE_GREEN_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_green_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_LIGHT_BLUE_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_LIGHT_BLUE.get()),
                modLoc("block/concrete_light_blue"));
        simpleBlockItem(ModBlocks.CONCRETE_LIGHT_BLUE_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_light_blue_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_LIME_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_LIME.get()),
                modLoc("block/concrete_lime"));
        simpleBlockItem(ModBlocks.CONCRETE_LIME_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_lime_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_MAGENTA_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_MAGENTA.get()),
                modLoc("block/concrete_magenta"));
        simpleBlockItem(ModBlocks.CONCRETE_MAGENTA_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_magenta_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_ORANGE_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_ORANGE.get()),
                modLoc("block/concrete_orange"));
        simpleBlockItem(ModBlocks.CONCRETE_ORANGE_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_orange_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_PINK_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_PINK.get()),
                modLoc("block/concrete_pink"));
        simpleBlockItem(ModBlocks.CONCRETE_PINK_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_pink_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_PURPLE_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_PURPLE.get()),
                modLoc("block/concrete_purple"));
        simpleBlockItem(ModBlocks.CONCRETE_PURPLE_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_purple_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_REBAR_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_REBAR.get()),
                modLoc("block/concrete_rebar"));
        simpleBlockItem(ModBlocks.CONCRETE_REBAR_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_rebar_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_RED_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_RED.get()),
                modLoc("block/concrete_red"));
        simpleBlockItem(ModBlocks.CONCRETE_RED_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_red_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_SILVER_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_SILVER.get()),
                modLoc("block/concrete_silver"));
        simpleBlockItem(ModBlocks.CONCRETE_SILVER_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_silver_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_SUPER_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_SUPER.get()),
                modLoc("block/concrete_super"));
        simpleBlockItem(ModBlocks.CONCRETE_SUPER_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_super_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_SUPER_BROKEN_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_SUPER_BROKEN.get()),
                modLoc("block/concrete_super_broken"));
        simpleBlockItem(ModBlocks.CONCRETE_SUPER_BROKEN_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_super_broken_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_SUPER_M0_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_SUPER_M0.get()),
                modLoc("block/concrete_super_m0"));
        simpleBlockItem(ModBlocks.CONCRETE_SUPER_M0_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_super_m0_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_SUPER_M1_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_SUPER_M1.get()),
                modLoc("block/concrete_super_m1"));
        simpleBlockItem(ModBlocks.CONCRETE_SUPER_M1_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_super_m1_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_SUPER_M2_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_SUPER_M2.get()),
                modLoc("block/concrete_super_m2"));
        simpleBlockItem(ModBlocks.CONCRETE_SUPER_M2_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_super_m2_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_SUPER_M3_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_SUPER_M3.get()),
                modLoc("block/concrete_super_m3"));
        simpleBlockItem(ModBlocks.CONCRETE_SUPER_M3_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_super_m3_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_TILE_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_TILE.get()),
                modLoc("block/concrete_tile"));
        simpleBlockItem(ModBlocks.CONCRETE_TILE_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_tile_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_WHITE_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_WHITE.get()),
                modLoc("block/concrete_white"));
        simpleBlockItem(ModBlocks.CONCRETE_WHITE_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_white_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_YELLOW_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_YELLOW.get()),
                modLoc("block/concrete_yellow"));
        simpleBlockItem(ModBlocks.CONCRETE_YELLOW_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_yellow_slab")));

        slabBlock((SlabBlock) ModBlocks.CONCRETE_FLAT_SLAB.get(),
                blockTexture(ModBlocks.CONCRETE_FLAT.get()),
                modLoc("block/concrete_flat"));
        simpleBlockItem(ModBlocks.CONCRETE_FLAT_SLAB.get(),
                models().getExistingFile(modLoc("block/concrete_flat_slab")));

        slabBlock((SlabBlock) ModBlocks.DEPTH_BRICK_SLAB.get(),
                blockTexture(ModBlocks.DEPTH_BRICK.get()),
                modLoc("block/depth_brick"));
        simpleBlockItem(ModBlocks.DEPTH_BRICK_SLAB.get(),
                models().getExistingFile(modLoc("block/depth_brick_slab")));

        slabBlock((SlabBlock) ModBlocks.DEPTH_NETHER_BRICK_SLAB.get(),
                blockTexture(ModBlocks.DEPTH_NETHER_BRICK.get()),
                modLoc("block/depth_nether_brick"));
        simpleBlockItem(ModBlocks.DEPTH_NETHER_BRICK_SLAB.get(),
                models().getExistingFile(modLoc("block/depth_nether_brick_slab")));

        slabBlock((SlabBlock) ModBlocks.DEPTH_NETHER_TILES_SLAB.get(),
                blockTexture(ModBlocks.DEPTH_NETHER_TILES.get()),
                modLoc("block/depth_nether_tiles"));
        simpleBlockItem(ModBlocks.DEPTH_NETHER_TILES_SLAB.get(),
                models().getExistingFile(modLoc("block/depth_nether_tiles_slab")));

        slabBlock((SlabBlock) ModBlocks.DEPTH_STONE_NETHER_SLAB.get(),
                blockTexture(ModBlocks.DEPTH_STONE_NETHER.get()),
                modLoc("block/depth_stone_nether"));
        simpleBlockItem(ModBlocks.DEPTH_STONE_NETHER_SLAB.get(),
                models().getExistingFile(modLoc("block/depth_stone_nether_slab")));

        slabBlock((SlabBlock) ModBlocks.DEPTH_TILES_SLAB.get(),
                blockTexture(ModBlocks.DEPTH_TILES.get()),
                modLoc("block/depth_tiles"));
        simpleBlockItem(ModBlocks.DEPTH_TILES_SLAB.get(),
                models().getExistingFile(modLoc("block/depth_tiles_slab")));

        slabBlock((SlabBlock) ModBlocks.GNEISS_BRICK_SLAB.get(),
                blockTexture(ModBlocks.GNEISS_BRICK.get()),
                modLoc("block/gneiss_brick"));
        simpleBlockItem(ModBlocks.GNEISS_BRICK_SLAB.get(),
                models().getExistingFile(modLoc("block/gneiss_brick_slab")));

        slabBlock((SlabBlock) ModBlocks.GNEISS_TILE_SLAB.get(),
                blockTexture(ModBlocks.GNEISS_TILE.get()),
                modLoc("block/gneiss_tile"));
        simpleBlockItem(ModBlocks.GNEISS_TILE_SLAB.get(),
                models().getExistingFile(modLoc("block/gneiss_tile_slab")));

        slabBlock((SlabBlock) ModBlocks.METEOR_BRICK_SLAB.get(),
                blockTexture(ModBlocks.METEOR_BRICK.get()),
                modLoc("block/meteor_brick"));
        simpleBlockItem(ModBlocks.METEOR_BRICK_SLAB.get(),
                models().getExistingFile(modLoc("block/meteor_brick_slab")));

        slabBlock((SlabBlock) ModBlocks.METEOR_BRICK_CRACKED_SLAB.get(),
                blockTexture(ModBlocks.METEOR_BRICK_CRACKED.get()),
                modLoc("block/meteor_brick_cracked"));
        simpleBlockItem(ModBlocks.METEOR_BRICK_CRACKED_SLAB.get(),
                models().getExistingFile(modLoc("block/meteor_brick_cracked_slab")));

        slabBlock((SlabBlock) ModBlocks.METEOR_BRICK_MOSSY_SLAB.get(),
                blockTexture(ModBlocks.METEOR_BRICK_MOSSY.get()),
                modLoc("block/meteor_brick_mossy"));
        simpleBlockItem(ModBlocks.METEOR_BRICK_MOSSY_SLAB.get(),
                models().getExistingFile(modLoc("block/meteor_brick_mossy_slab")));

        slabBlock((SlabBlock) ModBlocks.METEOR_CRUSHED_SLAB.get(),
                blockTexture(ModBlocks.METEOR_CRUSHED.get()),
                modLoc("block/meteor_crushed"));
        simpleBlockItem(ModBlocks.METEOR_CRUSHED_SLAB.get(),
                models().getExistingFile(modLoc("block/meteor_crushed_slab")));

        slabBlock((SlabBlock) ModBlocks.METEOR_POLISHED_SLAB.get(),
                blockTexture(ModBlocks.METEOR_POLISHED.get()),
                modLoc("block/meteor_polished"));
        simpleBlockItem(ModBlocks.METEOR_POLISHED_SLAB.get(),
                models().getExistingFile(modLoc("block/meteor_polished_slab")));

        slabBlock((SlabBlock) ModBlocks.VINYL_TILE_SLAB.get(),
                blockTexture(ModBlocks.VINYL_TILE.get()),
                modLoc("block/vinyl_tile"));
        simpleBlockItem(ModBlocks.VINYL_TILE_SLAB.get(),
                models().getExistingFile(modLoc("block/vinyl_tile_slab")));

        slabBlock((SlabBlock) ModBlocks.VINYL_TILE_SMALL_SLAB.get(),
                blockTexture(ModBlocks.VINYL_TILE_SMALL.get()),
                modLoc("block/vinyl_tile_small"));
        simpleBlockItem(ModBlocks.VINYL_TILE_SMALL_SLAB.get(),
                models().getExistingFile(modLoc("block/vinyl_tile_small_slab")));







        simpleBlockWithItem(ModBlocks.SHREDDER.get(),
                new ModelFile.UncheckedModelFile(modLoc("block/shredder")));

        // АВТОМАТИЧЕСКАЯ ГЕНЕРАЦИЯ МОДЕЛЕЙ ДЛЯ БЛОКОВ СЛИТКОВ
        // АВТОМАТИЧЕСКАЯ ГЕНЕРАЦИЯ МОДЕЛЕЙ ДЛЯ БЛОКОВ СЛИТКОВ
        for (ModIngots ingot : ModIngots.values()) {
            // !!! ДОБАВЛЕНА ПРОВЕРКА !!!
            if (ModBlocks.hasIngotBlock(ingot)) {
                RegistryObject<Block> blockRegistryObject = ModBlocks.getIngotBlock(ingot);
                if (blockRegistryObject != null) {
                    resourceBlockWithItem(blockRegistryObject);
                }
            }
        }

        registerAnvils();
    }

    /**
     * Метод для блоков, у которых текстура имеет префикс "block_".
     * Например, для блока с именем "uranium_block" он будет искать текстуру "block_uranium".
     */
    private void resourceBlockWithItem(RegistryObject<Block> blockObject) {
        // 1. Получаем регистрационное имя (теперь оно уже "block_uranium")
        String registrationName = blockObject.getId().getPath();

        // 2. Имя текстуры теперь совпадает с именем блока!
        // (Если ваши текстуры называются block_uranium.png)
        String textureName = registrationName;

        // 4. Проверяем существование текстуры
        ResourceLocation textureLocation = modLoc("textures/block/" + textureName + ".png");
        if (!existingFileHelper.exists(textureLocation, net.minecraft.server.packs.PackType.CLIENT_RESOURCES)) {
            MainRegistry.LOGGER.warn("Texture not found for block {}: {}. Skipping model generation.",
                    registrationName, textureLocation);
            return;
        }

        // 5. Создаем модель
        simpleBlock(blockObject.get(), models().cubeAll(registrationName, modLoc("block/" + textureName)));

        // 6. Создаем модель для предмета
        simpleBlockItem(blockObject.get(), models().getExistingFile(blockTexture(blockObject.get())));
    }
    private void oreWithItem(RegistryObject<Block> blockObject) {
        // 1. Получаем регистрационное имя блока (например, "uranium_block")
        String registrationName = blockObject.getId().getPath();

        // 2. Трансформируем его в базовое имя (удаляем "_block" -> "uranium")
        String baseName = registrationName.replace("_ore", "");

        // 3. Создаем имя файла текстуры (добавляем "ore_" -> "ore_uranium")
        String textureName = "ore_" + baseName;

        // 4. Создаем модель блока, ЯВНО указывая путь к текстуре
        //    Метод models().cubeAll() создает модель типа "block/cube_all" с указанной текстурой.
        simpleBlock(blockObject.get(), models().cubeAll(registrationName, modLoc("block/" + textureName)));

        // 5. Создаем модель для предмета-блока, как и раньше
        simpleBlockItem(blockObject.get(), models().getExistingFile(blockTexture(blockObject.get())));
    }

    /**
     * Старый метод для блоков, у которых имя текстуры СОВПАДАЕТ с именем регистрации.
     */
    private void blockWithItem(RegistryObject<Block> blockObject) {
        simpleBlock(blockObject.get());
        simpleBlockItem(blockObject.get(), models().getExistingFile(blockTexture(blockObject.get())));
    }


    private void columnBlockWithItem(RegistryObject<Block> blockObject, ResourceLocation sideLocation, ResourceLocation topLocation, ResourceLocation bottomLocation) {
        // Создаем модель блока, передавая готовые ResourceLocation
        simpleBlock(blockObject.get(), models().cubeBottomTop(
            blockObject.getId().getPath(),
            sideLocation,
            bottomLocation,
            topLocation
        ));
        // Создаем модель предмета-блока
        simpleBlockItem(blockObject.get(), models().getExistingFile(blockTexture(blockObject.get())));
    }


    /**
     * Генерирует состояние для блока с кастомной OBJ моделью.
     * ВАЖНО: Сам файл модели (.json) должен быть создан вручную в /resources!
     */
    private <T extends Block> void customObjBlock(RegistryObject<T> blockObject) {
        // Создаём только blockstate, который ссылается на JSON модель
        // JSON модель должна лежать в resources/assets/hbm_m/models/block/<название>.json
        horizontalBlock(blockObject.get(),
            models().getExistingFile(modLoc("block/" + blockObject.getId().getPath())));
    }

    /**
     * Генерирует модель и состояние для горизонтально-ориентированного блока.
     * @param blockObject Блок
     * @param sideTexture Текстура для боковых и задней сторон
     * @param frontTexture Текстура для лицевой стороны (север)
     * @param topTexture Текстура для верха и низа
     */
    private void orientableBlockWithItem(RegistryObject<Block> blockObject, ResourceLocation sideTexture, ResourceLocation frontTexture, ResourceLocation topTexture) {
        // 1. Создаем модель блока с разными текстурами.
        //    Метод orientable использует стандартные имена: side, front, top, bottom.
        var model = models().orientable(
            blockObject.getId().getPath(),
            sideTexture,
            frontTexture,
            topTexture
        ).texture("particle", frontTexture); // Частицы при ломании будут из лицевой текстуры

        // 2. Создаем состояние блока (blockstate), которое будет вращать эту модель по горизонтали.
        horizontalBlock(blockObject.get(), model);

        // 3. Создаем модель для предмета-блока, которая выглядит так же, как и сам блок.
        simpleBlockItem(blockObject.get(), model);
    }

    private void registerAnvils() {
        ModBlocks.getAnvilBlocks().forEach(reg -> horizontalBlock(
                reg.get(),
                models().getExistingFile(modLoc("block/" + reg.getId().getPath()))
        ));
    }

    // ✅ ИСПРАВЛЕННЫЙ МЕТОД: Использует правильные ванильные модели
    private void registerSnowLayerBlock(RegistryObject<Block> block, String baseName) {
        // Получаем текстуру нашего блока (nuclear_fallout.png)
        ResourceLocation texture = blockTexture(block.get());

        // Создаем модели для разной высоты, наследуясь от ванильных моделей снега
        // Важно: используем mcLoc("block/...") чтобы указать на minecraft namespace
        ModelFile model2 = models().withExistingParent(baseName + "_height2", mcLoc("block/snow_height2")).texture("texture", texture).texture("particle", texture);
        ModelFile model4 = models().withExistingParent(baseName + "_height4", mcLoc("block/snow_height4")).texture("texture", texture).texture("particle", texture);
        ModelFile model6 = models().withExistingParent(baseName + "_height6", mcLoc("block/snow_height6")).texture("texture", texture).texture("particle", texture);
        // Для полного блока (8 слоев) используем модель height12 + еще 2 пикселя = height14? Нет, в ваниле 8 слоев = полный блок.
        // Но у снега есть хитрость: snow_height14 не существует.
        // Самый надежный способ - использовать snow_height12 и растянуть?
        // Нет, лучше всего использовать обычный куб для полного слоя, или snow_height10/12/14 если они есть.
        // В 1.20.1 модели снега: height2, height4, height6, height8, height10, height12, height14? Нет.

        // ВАНИЛЬ ИСПОЛЬЗУЕТ:
        // layers=1 -> snow_height2
        // layers=8 -> block/snow (который полный блок?)

        // Попробуем так:
        // Для слоев 1-7 используем соответствующие модели (они есть в ваниле)
        // Для слоя 8 используем куб

        // Чтобы не гадать с путями, давайте просто создадим модели с нужными размерами сами,
        // либо используем те, что точно есть.
        // Точно есть: snow_height2, snow_height4, snow_height6, snow_height8, snow_height10, snow_height12

        // Но проще всего ссылаться на mcLoc("block/snow_height" + (layer * 2))

        // Исправленная логика: генерируем варианты
        VariantBlockStateBuilder builder = getVariantBuilder(block.get());

        for (int i = 1; i <= 8; i++) {
            ModelFile model;
            if (i == 8) {
                // Полный блок
                model = models().withExistingParent(baseName + "_height16", mcLoc("block/cube_all")).texture("all", texture).texture("particle", texture);
            } else {
                // Слои 2, 4, 6, 8, 10, 12, 14
                String parentName = "block/snow_height" + (i * 2);
                model = models().withExistingParent(baseName + "_height" + (i * 2), mcLoc(parentName))
                        .texture("texture", texture)
                        .texture("particle", texture);
            }

            builder.partialState().with(SnowLayerBlock.LAYERS, i).modelForState().modelFile(model).addModel();
        }

        // Модель предмета - как слой высотой 2
        simpleBlockItem(block.get(), models().withExistingParent(baseName + "_inventory", mcLoc("block/snow_height2")).texture("texture", texture).texture("particle", texture));
    }



}