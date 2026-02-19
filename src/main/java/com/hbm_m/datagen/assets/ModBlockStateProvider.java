package com.hbm_m.datagen.assets;

// Провайдер генерации состояний блоков и моделей для блоков мода.
// Используется в классе DataGenerators для регистрации.
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.custom.decorations.DoorBlock;
import com.hbm_m.block.custom.machines.BlastFurnaceBlock;
import com.hbm_m.block.custom.machines.MachineAdvancedAssemblerBlock;
import com.hbm_m.block.custom.machines.MachineWoodBurnerBlock;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
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
        blockWithItem(ModBlocks.WASTE_LEAVES);
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
        blockWithItem(ModBlocks.FREAKY_ALIEN_BLOCK);

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

		simpleBlockWithItem(ModBlocks.NUCLEAR_CHARGE.get(),
                models().cubeBottomTop(
                        ModBlocks.NUCLEAR_CHARGE.getId().getPath(),
                        modLoc("block/nuclear_charge"),
                        modLoc("block/nuclear_charge_top"),
                        modLoc("block/nuclear_charge")
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

		simpleBlockWithItem(ModBlocks.C4.get(),
                models().cubeBottomTop(
                        ModBlocks.C4.getId().getPath(),
                        modLoc("block/c4block_side"),
                        modLoc("block/c4block_top"),
                        modLoc("block/c4block_bottom")
                )
        );

        simpleBlock(ModBlocks.BLAST_FURNACE_EXTENSION.get(),
                models().getExistingFile(modLoc("block/machines/difurnace_extension")));
        simpleBlockItem(ModBlocks.BLAST_FURNACE_EXTENSION.get(),
                models().getExistingFile(modLoc("block/machines/difurnace_extension")));

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







        doorBlockWithRenderType(((net.minecraft.world.level.block.DoorBlock) ModBlocks.METAL_DOOR.get()), modLoc("block/metal_door_bottom"), modLoc("block/metal_door_top"), "cutout");
        doorBlockWithRenderType(((net.minecraft.world.level.block.DoorBlock) ModBlocks.DOOR_BUNKER.get()), modLoc("block/door_bunker_bottom"), modLoc("block/door_bunker_top"), "cutout");
        doorBlockWithRenderType(((net.minecraft.world.level.block.DoorBlock) ModBlocks.DOOR_OFFICE.get()), modLoc("block/door_office_bottom"), modLoc("block/door_office_top"), "cutout");

        columnBlockWithItem(
                ModBlocks.WASTE_GRASS,
                modLoc("block/waste_grass_side"),
                modLoc("block/waste_grass_top"),
                mcLoc("block/dirt")
        );

        columnBlockWithItem(
                ModBlocks.DET_MINER,
                modLoc("block/det_miner_side"),
                modLoc("block/det_miner_top"),
                modLoc("block/det_miner_top")
        );

        columnBlockWithItem(
                ModBlocks.ARMOR_TABLE,
                modLoc("block/armor_table_side"),
                modLoc("block/armor_table_top"),
                modLoc("block/armor_table_bottom")
        );

		columnBlockWithItem(
                ModBlocks.WASTE_CHARGE,
                modLoc("block/waste_charge"),
                modLoc("block/waste_charge_top"),
                modLoc("block/waste_charge_bottom")
        );

        columnBlockWithItem(
                ModBlocks.SMOKE_BOMB,
                modLoc("block/smoke_bomb_side"),
                modLoc("block/smoke_bomb_top"),
                modLoc("block/smoke_bomb_bottom")
        );

        // Блоки с кастомной OBJ моделью
        // Doors
        
        customDoorBlock(ModBlocks.LARGE_VEHICLE_DOOR);
        customDoorBlock(ModBlocks.ROUND_AIRLOCK_DOOR);
        customDoorBlock(ModBlocks.TRANSITION_SEAL);
        customDoorBlock(ModBlocks.SILO_HATCH);
        customDoorBlock(ModBlocks.SILO_HATCH_LARGE);
        customDoorBlock(ModBlocks.QE_SLIDING);
        customDoorBlock(ModBlocks.QE_CONTAINMENT);
        customDoorBlock(ModBlocks.WATER_DOOR);
        customDoorBlock(ModBlocks.FIRE_DOOR);
        customDoorBlock(ModBlocks.SLIDE_DOOR);
        customDoorBlock(ModBlocks.SLIDING_SEAL_DOOR);
        customDoorBlock(ModBlocks.SECURE_ACCESS_DOOR);

        // Machines
        customMachineBlock(ModBlocks.ORE_ACIDIZER);
        customMachineBlock(ModBlocks.CHEMICAL_PLANT);
        customMachineBlock(ModBlocks.HYDRAULIC_FRACKINING_TOWER);
        customMachineBlock(ModBlocks.CENTRIFUGE);
        customMachineBlock(ModBlocks.MACHINE_ASSEMBLER);
        registerAdvancedAssemblyMachineBlock(ModBlocks.ADVANCED_ASSEMBLY_MACHINE);
        customMachineBlock(ModBlocks.PRESS);

        // Машины со свойством LIT (включен/выключен)
        registerLitMachineBlock(ModBlocks.BLAST_FURNACE, 
            BlastFurnaceBlock.FACING, BlastFurnaceBlock.LIT, 
            "blast_furnace", "blast_furnace_on");
        registerLitMachineBlock(ModBlocks.WOOD_BURNER, 
            MachineWoodBurnerBlock.FACING, MachineWoodBurnerBlock.LIT, 
            "wood_burner", "wood_burner");

        // FluidTank - только FACING
        horizontalBlock(ModBlocks.FLUID_TANK.get(),
            models().getExistingFile(modLoc("block/machines/fluid_tank")));

        // Decor
        customObjBlock(ModBlocks.CRT_BROKEN);
        customObjBlock(ModBlocks.CRT_BSOD);
        customObjBlock(ModBlocks.CRT_CLEAN);
        customObjBlock(ModBlocks.GEIGER_COUNTER_BLOCK);
        customObjBlock(ModBlocks.TAPE_RECORDER);
        customObjBlock(ModBlocks.TOASTER);

        // Other
        customObjBlock(ModBlocks.AIRBOMB);
        customObjBlock(ModBlocks.BALEBOMB_TEST);
        customObjBlock(ModBlocks.BARREL_CORRODED);
        customObjBlock(ModBlocks.BARREL_IRON);
        customObjBlock(ModBlocks.BARREL_LOX);
        customObjBlock(ModBlocks.BARREL_PINK);
        customObjBlock(ModBlocks.BARREL_RED);
        customObjBlock(ModBlocks.BARREL_PLASTIC);
        customObjBlock(ModBlocks.BARREL_STEEL);
        customObjBlock(ModBlocks.BARREL_TAINT);
        customObjBlock(ModBlocks.BARREL_TCALLOY);
        customObjBlock(ModBlocks.BARREL_VITRIFIED);
        customObjBlock(ModBlocks.BARREL_YELLOW);
        customObjBlock(ModBlocks.DUD_CONVENTIONAL);
        customObjBlock(ModBlocks.DUD_NUKE);
        customObjBlock(ModBlocks.DUD_SALTED);
        simpleBlockWithItem(ModBlocks.MINE_AP.get(), models().getExistingFile(modLoc("block/mine_ap")));
        simpleBlockWithItem(ModBlocks.MINE_FAT.get(), models().getExistingFile(modLoc("block/mine_fat")));
        customObjBlock(ModBlocks.CRATE_CONSERVE);
        customObjBlock(ModBlocks.FILE_CABINET);

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

        // orientableBlockWithItem(
        //         ModBlocks.SHREDDER,
        //         modLoc("block/shredder_side"),
        //         modLoc("block/shredder_front"),
        //         modLoc("block/shredder_top")
        // );

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
        for (ModIngots ingot : ModIngots.values()) {
            if (ModBlocks.hasIngotBlock(ingot)) {
                RegistryObject<Block> blockRegistryObject = ModBlocks.getIngotBlock(ingot);
                if (blockRegistryObject != null) {
                    resourceBlockWithItem(blockRegistryObject);
                }
            }
        }

        registerAnvils();

        // === ГЕНЕРАЦИЯ BLOCKSTATE ФАЙЛОВ ДЛЯ РУД ===
        // Используем oreWithItem() для всех руд
        oreWithItem(ModBlocks.URANIUM_ORE);
        oreWithItem(ModBlocks.URANIUM_ORE_DEEPSLATE);
        oreWithItem(ModBlocks.LIGNITE_ORE);
        oreWithItem(ModBlocks.ALUMINUM_ORE);
        oreWithItem(ModBlocks.ALUMINUM_ORE_DEEPSLATE);
        oreWithItem(ModBlocks.LEAD_ORE);
        oreWithItem(ModBlocks.LEAD_ORE_DEEPSLATE);
        oreWithItem(ModBlocks.RAREGROUND_ORE);
        oreWithItem(ModBlocks.RAREGROUND_ORE_DEEPSLATE);
        oreWithItem(ModBlocks.FLUORITE_ORE);
        oreWithItem(ModBlocks.BERYLLIUM_ORE);
        oreWithItem(ModBlocks.BERYLLIUM_ORE_DEEPSLATE);
        oreWithItem(ModBlocks.ASBESTOS_ORE);
        oreWithItem(ModBlocks.CINNABAR_ORE);
        oreWithItem(ModBlocks.CINNABAR_ORE_DEEPSLATE);
        oreWithItem(ModBlocks.COBALT_ORE);
        oreWithItem(ModBlocks.COBALT_ORE_DEEPSLATE);
        oreWithItem(ModBlocks.TUNGSTEN_ORE);
        oreWithItem(ModBlocks.THORIUM_ORE);
        oreWithItem(ModBlocks.THORIUM_ORE_DEEPSLATE);
        oreWithItem(ModBlocks.TITANIUM_ORE);
        oreWithItem(ModBlocks.TITANIUM_ORE_DEEPSLATE);
        oreWithItem(ModBlocks.SULFUR_ORE);
        oreWithItem(ModBlocks.ORE_OIL);
        oreWithItem(ModBlocks.SEQUESTRUM_ORE);
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
        // 1. Получаем регистрационное имя блока (например, "uranium_ore")
        String registrationName = blockObject.getId().getPath();

        // 2. Пробуем два варианта имени текстуры:
        //    - "ore_" + registrationName (например: ore_uranium)
        //    - registrationName (например: uranium_ore_deepslate)
        String textureName = "ore_" + registrationName;
        ResourceLocation textureLocation = modLoc("textures/block/" + textureName + ".png");

        if (!existingFileHelper.exists(textureLocation, net.minecraft.server.packs.PackType.CLIENT_RESOURCES)) {
            // Пробуем без префикса "ore_"
            textureName = registrationName;
            textureLocation = modLoc("textures/block/" + textureName + ".png");
            if (!existingFileHelper.exists(textureLocation, net.minecraft.server.packs.PackType.CLIENT_RESOURCES)) {
                MainRegistry.LOGGER.warn("Texture not found for block {} (tried: {} and {}). Skipping model generation.",
                        registrationName, "ore_" + registrationName, registrationName);
                return;
            }
        }

        // 3. Создаем модель блока
        simpleBlock(blockObject.get(), models().cubeAll(registrationName, modLoc("block/" + textureName)));

        // 4. Создаем модель для предмета-блока
        simpleBlockItem(blockObject.get(), models().getExistingFile(modLoc("block/" + textureName)));
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

    private <T extends Block> void customDoorBlock(RegistryObject<T> blockObject) {
        // Регистрируем все варианты blockstate для двери (FACING + PART_ROLE + DOOR_MOVING + OPEN)
        // rotationY(0): поворот обрабатывается внутри DoorBakedModel (совпадение с BER + doOffsetTransform)
        VariantBlockStateBuilder builder = getVariantBuilder(blockObject.get());
        ModelFile modelFile = models().getExistingFile(modLoc("block/doors/" + blockObject.getId().getPath()));
        
        for (Direction facing : Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new)) {
            for (PartRole partRole : PartRole.values()) {
                for (boolean doorMoving : new boolean[]{false, true}) {
                    for (boolean open : new boolean[]{false, true}) {
                        builder.partialState()
                            .with(DoorBlock.FACING, facing)
                            .with(DoorBlock.PART_ROLE, partRole)
                            .with(DoorBlock.DOOR_MOVING, doorMoving)
                            .with(DoorBlock.OPEN, open)
                            .modelForState()
                            .modelFile(modelFile)
                            .rotationY(0)
                            .addModel();
                    }
                }
            }
        }
    }

    private <T extends Block> void customMachineBlock(RegistryObject<T> blockObject) {
        // Создаём только blockstate, который ссылается на JSON модель
        // JSON модель должна лежать в resources/assets/hbm_m/models/block/<название>.json
        horizontalBlock(blockObject.get(),
            models().getExistingFile(modLoc("block/machines/" + blockObject.getId().getPath())));
    }

    /**
     * Advanced Assembly Machine: FACING + FRAME (frame в BlockState для запекания в чанк).
     * Одна модель — getQuads возвращает Base+Frame при frame=true.
     */
    private void registerAdvancedAssemblyMachineBlock(RegistryObject<? extends Block> blockObject) {
        VariantBlockStateBuilder builder = getVariantBuilder(blockObject.get());
        // Используем одну и ту же модель для всех состояний.
        // Логика отображения (Baked vs BER) скрыта внутри самого MachineAdvancedAssemblerBakedModel.
        ModelFile modelFile = models().getExistingFile(modLoc("block/machines/" + blockObject.getId().getPath()));
        
        for (Direction facing : Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new)) {
            for (boolean frame : new boolean[]{false, true}) {
                // Добавляем перебор состояния RENDER_ACTIVE
                for (boolean renderActive : new boolean[]{false, true}) {
                    builder.partialState()
                        .with(MachineAdvancedAssemblerBlock.FACING, facing)
                        .with(MachineAdvancedAssemblerBlock.FRAME, frame)
                        .with(MachineAdvancedAssemblerBlock.RENDER_ACTIVE, renderActive)
                        .modelForState()
                        .modelFile(modelFile)
                        .rotationY(getRotationY(facing))
                        .addModel();
                }
            }
        }
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
                models().getExistingFile(modLoc("block/machines/" + reg.getId().getPath()))
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

    /**
     * Регистрирует blockstate для машин со свойством LIT (включен/выключен).
     * Генерирует варианты для каждого направления FACING и состояния LIT.
     */
    private void registerLitMachineBlock(RegistryObject<? extends Block> blockObject, 
                                          DirectionProperty facingProperty,
                                          BooleanProperty litProperty,
                                          String offModel, String onModel) {
        VariantBlockStateBuilder builder = getVariantBuilder(blockObject.get());
        
        // Создаём модели для состояний lit=false и lit=true
        ModelFile offModelFile = models().getExistingFile(modLoc("block/machines/" + offModel));
        ModelFile onModelFile = models().getExistingFile(modLoc("block/machines/" + onModel));
        
        // Для каждого направления FACING создаём варианты для LIT=false и LIT=true
        for (Direction facing : Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new)) {
            // Состояние выключено (lit=false)
            builder.partialState()
                .with(facingProperty, facing)
                .with(litProperty, false)
                .modelForState()
                .modelFile(offModelFile)
                .rotationY(getRotationY(facing))
                .addModel();
            
            // Состояние включено (lit=true)
            builder.partialState()
                .with(facingProperty, facing)
                .with(litProperty, true)
                .modelForState()
                .modelFile(onModelFile)
                .rotationY(getRotationY(facing))
                .addModel();
        }
        
        // Модель для предмета (используем выключенную модель)
        // simpleBlockItem(blockObject.get(), offModelFile);
    }

    /**
     * Возвращает угол поворота Y для направления в градусах.
     */
    private int getRotationY(Direction facing) {
        return switch (facing) {
            case SOUTH -> 180;
            case WEST -> 270;
            case NORTH -> 0;
            case EAST -> 90;
            default -> 0;
        };
    }
}