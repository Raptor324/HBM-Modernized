package com.hbm_m.datagen.assets;
//? if forge {
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.decorations.DoorBlock;
// Провайдер генерации состояний блоков и моделей для блоков мода.
// Используется в классе DataGenerators для регистрации.
import com.hbm_m.block.generic.BlockAbsorber;
import com.hbm_m.block.generic.BlockSellafieldSlaked;
import com.hbm_m.block.machines.BlastFurnaceBlock;
import com.hbm_m.block.machines.MachineAdvancedAssemblerBlock;
import com.hbm_m.block.machines.MachineAssemblerBlock;
import com.hbm_m.block.machines.MachineChemicalPlantBlock;
import com.hbm_m.block.machines.MachineWoodBurnerBlock;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.multiblock.PartRole;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.VariantBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;

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
        registerSellafieldSlaked(ModBlocks.SELLAFIELD_BEDROCK, "sellafield_bedrock");
        registerSellafieldOre(ModBlocks.ORE_SELLAFIELD_DIAMOND, "sellafield_ore_diamond", "block/ore_overlay_diamond");
        registerSellafieldOre(ModBlocks.ORE_SELLAFIELD_EMERALD, "sellafield_ore_emerald", "block/ore_overlay_emerald");
        registerSellafieldOre(ModBlocks.ORE_SELLAFIELD_URANIUM_SCORCHED, "sellafield_ore_uranium_scorched", "block/ore_overlay_uranium_scorched");
        registerSellafieldOre(ModBlocks.ORE_SELLAFIELD_SCHRABIDIUM, "sellafield_ore_schrabidium", "block/ore_overlay_schrabidium");
        registerSellafieldOre(ModBlocks.ORE_SELLAFIELD_RADGEM, "sellafield_ore_radgem", "block/ore_overlay_radgem");
        blockWithItem(ModBlocks.WASTE_TRINITITE);
        blockWithItem(ModBlocks.WASTE_TRINITITE_RED);
        simpleBlockWithItem(ModBlocks.WASTE_MYCELIUM.get(),
                models().withExistingParent("waste_mycelium", mcLoc("block/block"))
                        .texture("particle", modLoc("block/waste_earth_bottom"))
                        .texture("bottom", modLoc("block/waste_earth_bottom"))
                        .texture("top", modLoc("block/waste_mycelium_top"))
                        .texture("side", modLoc("block/waste_mycelium_side"))
                        .element()
                        .from(0, 0, 0)
                        .to(16, 16, 16)
                        .face(Direction.DOWN).uvs(0, 0, 16, 16).texture("#bottom").cullface(Direction.DOWN).end()
                        .face(Direction.UP).uvs(0, 0, 16, 16).texture("#top").cullface(Direction.UP).end()
                        .face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#side").cullface(Direction.NORTH).end()
                        .face(Direction.SOUTH).uvs(0, 0, 16, 16).texture("#side").cullface(Direction.SOUTH).end()
                        .face(Direction.WEST).uvs(0, 0, 16, 16).texture("#side").cullface(Direction.WEST).end()
                        .face(Direction.EAST).uvs(0, 0, 16, 16).texture("#side").cullface(Direction.EAST).end()
                        .end());
        blockWithItem(ModBlocks.FREAKY_ALIEN_BLOCK);

        // Connected textures blocks (настоящий CT рендерится через BakedModel wrapper).
        registerDecoCtBlock(ModBlocks.DECO_STEEL, "deco_steel");
        registerDecoCtBlock(ModBlocks.DECO_RUSTY_STEEL, "deco_rusty_steel");
        registerDecoCtBlock(ModBlocks.DECO_TUNGSTEN, "deco_tungsten");
        registerDecoCtBlock(ModBlocks.DECO_RED_COPPER, "deco_red_copper");
        registerDecoCtBlock(ModBlocks.DECO_ALUMINUM, "deco_aluminum");
        registerDecoCtBlock(ModBlocks.DECO_BERYLLIUM, "deco_beryllium");
        registerDecoCtBlock(ModBlocks.DECO_LEAD, "deco_lead");

        // Модель для ядерных осадков
        // Эта функция автоматически создаст все 8 состояний высоты для блока
        // и свяжет их с моделями, которые выглядят как снег, но с вашей текстурой.
        registerFalloutLayerBlock(ModBlocks.NUCLEAR_FALLOUT, "nuclear_fallout");
        registerFalloutBlock(ModBlocks.BLOCK_FALLOUT, "block_fallout", "nuclear_fallout");

        // === РЕГИСТРАЦИЯ ПАДАЮЩИХ БЛОКОВ СЕЛЛАФИТА ===
        // Turrets: echte Original-Modelle (Base statisch per Blockmodell, Carriage/Pitch-Gruppe per BER animiert
        // - siehe MachineTurretRenderer). Statische Composite-Modelle liegen handgeschrieben unter
        // models/block/turret_<name>.json (Sichtbarkeits-Split aus den Original-OBJs).
        for (var turretBlock : java.util.List.of(
                ModBlocks.TURRET_SENTRY, ModBlocks.TURRET_CHEKHOV, ModBlocks.TURRET_FRIENDLY, ModBlocks.TURRET_JEREMY,
                ModBlocks.TURRET_TAUON, ModBlocks.TURRET_RICHARD, ModBlocks.TURRET_HOWARD,
                ModBlocks.TURRET_MAXWELL, ModBlocks.TURRET_FRITZ, ModBlocks.TURRET_ARTY, ModBlocks.TURRET_HIMARS)) {
            simpleBlockWithItem(turretBlock.get(),
                    models().getExistingFile(modLoc("block/" + turretBlock.getId().getPath())));
        }

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

        simpleBlockWithItem(ModBlocks.CRATE_TUNGSTEN.get(),
                models().cubeBottomTop(
                        ModBlocks.CRATE_TUNGSTEN.getId().getPath(),
                        modLoc("block/crate_tungsten_side"),
                        modLoc("block/crate_tungsten_top"),
                        modLoc("block/crate_tungsten_top")
                )
        );

        simpleBlockWithItem(ModBlocks.CRATE_TEMPLATE.get(),
                models().cubeAll(
                        ModBlocks.CRATE_TEMPLATE.getId().getPath(),
                        modLoc("block/crate_template")
                )
        );

        simpleBlockWithItem(ModBlocks.REINFORCED_GLASS.get(),
                models().cubeAll(ModBlocks.REINFORCED_GLASS.getId().getPath(),
                                blockTexture(ModBlocks.REINFORCED_GLASS.get()))
                        .renderType("cutout"));

        simpleBlockWithItem(ModBlocks.MACHINE_SIREN.get(),
                models().cubeBottomTop(
                        ModBlocks.MACHINE_SIREN.getId().getPath(),
                        blockTexture(ModBlocks.MACHINE_SIREN.get()),
                        modLoc("block/block_steel_machine"),
                        modLoc("block/block_steel_machine")
                ));

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
        customDoorBlock(ModBlocks.VAULT_DOOR);

        // Machines
        customMachineBlock(ModBlocks.CRYSTALLIZER);
        registerChemicalPlantBlock(ModBlocks.CHEMICAL_PLANT);
        customMachineBlock(ModBlocks.CHEMICAL_FACTORY);
        customMachineBlock(ModBlocks.CATALYTIC_REFORMER);
        customMachineBlock(ModBlocks.LIQUEFACTOR);
        customMachineBlock(ModBlocks.HYDRAULIC_FRACKINING_TOWER);
        customMachineBlock(ModBlocks.COOLING_TOWER);
        customMachineBlock(ModBlocks.TOWER_SMALL);
        customMachineBlock(ModBlocks.CYCLOTRON);
        customMachineBlock(ModBlocks.ZIRNOX);
        customMachineBlock(ModBlocks.ARC_WELDER);
        customMachineBlock(ModBlocks.SOLDERING_STATION);
        customMachineBlock(ModBlocks.MIXER);
        customMachineBlock(ModBlocks.DERRICK);
        customMachineBlock(ModBlocks.RBMK_CONSOLE);
        customMachineBlock(ModBlocks.FLARE_STACK);
        customMachineBlock(ModBlocks.PUMPJACK);
        customMachineBlock(ModBlocks.RADAR);
        customMachineBlock(ModBlocks.LARGE_RADAR);
        customMachineBlock(ModBlocks.RADAR_SCREEN);
        customMachineBlock(ModBlocks.CRACKING_TOWER);
        customMachineBlock(ModBlocks.FRACTION_TOWER);
        customMachineBlock(ModBlocks.MINING_DRILL);
        customMachineBlock(ModBlocks.FEL);
        customMachineBlock(ModBlocks.SILEX);
        simpleMachineBlock(ModBlocks.FOUNDRY_BASIN);

        // --- WIP Machines (3D OBJ models) ---
        customMachineBlock(ModBlocks.AMMO_PRESS);
        customMachineBlock(ModBlocks.ANNIHILATOR);
        customMachineBlock(ModBlocks.ARC_FURNACE);
        simpleMachineBlock(ModBlocks.ASSEMBLY_FACTORY);
        horizontalBlock(ModBlocks.AUTOSAW.get(),
            models().getExistingFile(modLoc("block/machines/autosaw")));
        horizontalBlock(ModBlocks.THRESHER.get(),
            models().getExistingFile(modLoc("block/machines/thresher")));
        simpleMachineBlock(ModBlocks.BEAMLINE);
        customMachineBlock(ModBlocks.BOILER);
        horizontalBlock(ModBlocks.PUMP_STEAM.get(),
            models().cubeAll(ModBlocks.PUMP_STEAM.getId().getPath(), modLoc("block/machine/pump_steam")));
        horizontalBlock(ModBlocks.PUMP_ELECTRIC.get(),
            models().cubeAll(ModBlocks.PUMP_ELECTRIC.getId().getPath(), modLoc("block/machine/pump_electric")));
        simpleMachineBlock(ModBlocks.BOILER_FUSION);
        simpleMachineBlock(ModBlocks.BREEDER_FUSION);
        customMachineBlock(ModBlocks.CHIMNEY_BRICK);
        customMachineBlock(ModBlocks.CHIMNEY_INDUSTRIAL);
        customMachineBlock(ModBlocks.COKER);
        simpleMachineBlock(ModBlocks.COLLECTOR);
        simpleMachineBlock(ModBlocks.COMBINATION_OVEN);
        customMachineBlock(ModBlocks.COMBUSTION_ENGINE);
        horizontalBlock(ModBlocks.COMPRESSOR.get(),
            models().getExistingFile(modLoc("block/machines/compressor")));
        horizontalBlock(ModBlocks.MACHINE_COMPRESSOR_COMPACT.get(),
            models().cubeAll(ModBlocks.MACHINE_COMPRESSOR_COMPACT.getId().getPath(), modLoc("block/machine/compressor_compact")));
        customMachineBlock(ModBlocks.CONDENSER_POWERED);
        customMachineBlock(ModBlocks.LPW2);
        customMachineBlock(ModBlocks.CONVEYOR_PRESS);
        simpleMachineBlock(ModBlocks.COUPLER);
        simpleMachineBlock(ModBlocks.DETECTOR);
        customMachineBlock(ModBlocks.DIESELGEN);
        simpleMachineBlock(ModBlocks.DIPOLE);
        simpleMachineBlock(ModBlocks.DRONE);
        simpleMachineBlock(ModBlocks.ELECTRIC_HEATER);
        horizontalBlock(ModBlocks.ELECTROLYSER.get(),
            models().getExistingFile(modLoc("block/machines/electrolyser")));
        customMachineBlock(ModBlocks.EPRESS);
        horizontalBlock(ModBlocks.EXPOSURE_CHAMBER.get(),
            models().getExistingFile(modLoc("block/machines/exposure_chamber")));
        simpleMachineBlock(ModBlocks.FENSU);
        // FENSU2 (machine_battery_redd) is a MachineBatteryBlock (FACING-Blockstate) - see orientableBlockWithItem below.
        simpleMachineBlock(ModBlocks.FIREBOX);
        simpleMachineBlock(ModBlocks.FRACTION_SPACER);
        simpleMachineBlock(ModBlocks.HEATEX);
        customMachineBlock(ModBlocks.HEPHAESTUS);
        simpleMachineBlock(ModBlocks.ICF);
        simpleMachineBlock(ModBlocks.INTAKE);
        simpleMachineBlock(ModBlocks.KLYSTRON);
        simpleMachineBlock(ModBlocks.MHDT);
        horizontalBlock(ModBlocks.MICROWAVE.get(),
            models().getExistingFile(modLoc("block/machines/microwave")));
        customMachineBlock(ModBlocks.MINING_LASER);
        simpleMachineBlock(ModBlocks.OILBURNER);
        simpleMachineBlock(ModBlocks.OILBURNER_HP);
        // ORBUS is now a BarrelTankBlock (FACING blockstate) instead of a static "" variant.
        horizontalBlock(ModBlocks.ORBUS.get(), models().getExistingFile(modLoc("block/machines/orbus")));
        simpleMachineBlock(ModBlocks.ORE_SLOPPER);
        simpleMachineBlock(ModBlocks.PLASMA_FORGE);
        customMachineBlock(ModBlocks.PYROOVEN);
        simpleMachineBlock(ModBlocks.QUADRUPOLE);
        simpleMachineBlock(ModBlocks.RADGEN);
        horizontalBlock(ModBlocks.RADIOLYSIS.get(),
            models().getExistingFile(modLoc("block/machines/radiolysis")));
        simpleMachineBlock(ModBlocks.REACTOR_SMALL);
        simpleMachineBlock(ModBlocks.RFC);
        horizontalBlock(ModBlocks.SAWMILL.get(),
            models().getExistingFile(modLoc("block/machines/sawmill")));
        customMachineBlock(ModBlocks.SOLIDIFIER);
        simpleMachineBlock(ModBlocks.ASHPIT);
        simpleMachineBlock(ModBlocks.REACTOR_RESEARCH);
        simpleMachineBlock(ModBlocks.SOURCE);
        customMachineBlock(ModBlocks.STEAM_ENGINE);
        customMachineBlock(ModBlocks.STIRLING);
        customMachineBlock(ModBlocks.STIRLING_CREATIVE);
        customMachineBlock(ModBlocks.STIRLING_STEEL);
        // Strand Caster: eigenes OBJ-Modell bereits vorhanden (block/machines/strand_caster.json), FACING-Rotation.
        horizontalBlock(ModBlocks.STRAND_CASTER.get(),
            models().getExistingFile(modLoc("block/machines/strand_caster")));
        simpleMachineBlock(ModBlocks.TORUS);
        simpleMachineBlock(ModBlocks.TURBINEGAS);
        simpleMachineBlock(ModBlocks.WATZ_PUMP);
        simpleMachineBlock(ModBlocks.CHUNGUS);
        customMachineBlock(ModBlocks.CENTRIFUGE);
        customMachineBlock(ModBlocks.BREEDER);
        customMachineBlock(ModBlocks.LARGE_PYLON);
        customMachineBlock(ModBlocks.LAUNCH_PAD);
        customMachineBlock(ModBlocks.LAUNCH_PAD_RUSTED);
        customBombBlock(ModBlocks.NUKE_FAT_MAN);
        customMachineBlock(ModBlocks.CORE_EMITTER);
        customMachineBlock(ModBlocks.CORE_INJECTOR);
        customMachineBlock(ModBlocks.CORE_RECEIVER);
        customMachineBlock(ModBlocks.VACUUM_DISTILL);
        customMachineBlock(ModBlocks.TURBOFAN);
        customMachineBlock(ModBlocks.INDUSTRIAL_TURBINE);
        customMachineBlock(ModBlocks.TURBINE);
        // MACHINE_CHUNGUS nutzt das bereits vorhandene chungus.obj-Modell (Pfad weicht von der
        // Registry-ID ab, daher kein customMachineBlock()).
        horizontalBlock(ModBlocks.MACHINE_CHUNGUS.get(),
                models().getExistingFile(modLoc("block/machines/chungus")));
        customMachineBlock(ModBlocks.SUBSTATION);
        registerMachineAssemblerBlock(ModBlocks.MACHINE_ASSEMBLER);
        registerAdvancedAssemblyMachineBlock(ModBlocks.ADVANCED_ASSEMBLY_MACHINE);
        customMachineBlock(ModBlocks.PRESS);

        // Машины со свойством LIT (включен/выключен)
        registerLitMachineBlock(ModBlocks.BLAST_FURNACE, 
            BlastFurnaceBlock.FACING, BlastFurnaceBlock.LIT, 
            "blast_furnace", "blast_furnace_on");
        registerLitMachineBlock(ModBlocks.WOOD_BURNER,
            MachineWoodBurnerBlock.FACING, MachineWoodBurnerBlock.LIT,
            "wood_burner", "wood_burner");
        // Furnace Iron/Steel: kein separates lit-Modell portiert (siehe Aufgabenbeschreibung:
        // "Skip any lit/unlit block-swap") - dasselbe Modell wird fuer beide LIT-Zustaende
        // registriert, damit registerLitMachineBlock trotzdem alle FACING x LIT Kombinationen
        // fuer den Blockstate abdeckt.
        registerLitMachineBlock(ModBlocks.FURNACE_IRON,
            com.hbm_m.block.machines.MachineFurnaceIronBlock.FACING, com.hbm_m.block.machines.MachineFurnaceIronBlock.LIT,
            "furnace_iron", "furnace_iron");
        registerLitMachineBlock(ModBlocks.FURNACE_STEEL,
            com.hbm_m.block.machines.MachineFurnaceSteelBlock.FACING, com.hbm_m.block.machines.MachineFurnaceSteelBlock.LIT,
            "furnace_steel", "furnace_steel");
        // Electric Furnace / Brick Furnace: kein eigenes Modell/Textur-Set portiert (nicht in den
        // vorhandenen Assets vorhanden) - als Platzhalter wird das bereits existierende
        // furnace_iron-Modell (inkl. Textur) wiederverwendet, damit die Bloecke kompilieren und
        // sichtbar sind. Sollte spaeter durch dedizierte Modelle ersetzt werden.
        registerLitMachineBlock(ModBlocks.ELECTRIC_FURNACE,
            com.hbm_m.block.machines.MachineElectricFurnaceBlock.FACING, com.hbm_m.block.machines.MachineElectricFurnaceBlock.LIT,
            "furnace_iron", "furnace_iron");
        registerLitMachineBlock(ModBlocks.FURNACE_BRICK,
            com.hbm_m.block.machines.MachineFurnaceBrickBlock.FACING, com.hbm_m.block.machines.MachineFurnaceBrickBlock.LIT,
            "furnace_iron", "furnace_iron");
        // Rotary Furnace: eigenes OBJ-Modell bereits vorhanden (block/machines/rotary_furnace.json),
        // kein separates LIT-Modell portiert - gleiches Modell fuer beide Zustaende.
        registerLitMachineBlock(ModBlocks.ROTARY_FURNACE,
            com.hbm_m.block.machines.MachineRotaryFurnaceBlock.FACING, com.hbm_m.block.machines.MachineRotaryFurnaceBlock.LIT,
            "rotary_furnace", "rotary_furnace");

        // FluidTank - только FACING
        horizontalBlock(ModBlocks.FLUID_TANK.get(),
            models().getExistingFile(modLoc("block/machines/fluid_tank")));

        // BAT9000 - uses its own pre-existing dedicated model/texture (static, no fluid-tint swap)
        horizontalBlock(ModBlocks.BAT9000.get(),
            models().getExistingFile(modLoc("block/machines/bat9000")));

        horizontalBlock(ModBlocks.MACHINE_BATTERY_SOCKET.get(),
            models().getExistingFile(modLoc("block/machines/machine_battery_socket")));

        // Жидкостный насос / клапан / выхлоп — временно ванильный iron cube (отдельные модели позже)
        ModelFile fluidPumpModel = models().withExistingParent(ModBlocks.FLUID_PUMP.getId().getPath(), mcLoc("block/cube_all"))
                .texture("all", mcLoc("block/iron_block"))
                .texture("particle", mcLoc("block/iron_block"));
        horizontalBlock(ModBlocks.FLUID_PUMP.get(), fluidPumpModel);

        ModelFile fluidValveModel = models().withExistingParent(ModBlocks.FLUID_VALVE.getId().getPath(), mcLoc("block/cube_all"))
                .texture("all", mcLoc("block/iron_block"))
                .texture("particle", mcLoc("block/iron_block"));
        simpleBlock(ModBlocks.FLUID_VALVE.get(), fluidValveModel);

        ModelFile fluidExhaustModel = models().withExistingParent(ModBlocks.FLUID_EXHAUST.getId().getPath(), mcLoc("block/cube_all"))
                .texture("all", mcLoc("block/iron_block"))
                .texture("particle", mcLoc("block/iron_block"));
        simpleBlock(ModBlocks.FLUID_EXHAUST.get(), fluidExhaustModel);

        // Decor
        customObjBlock(ModBlocks.REBAR);

        // Decor
        customObjBlock(ModBlocks.CRT_BROKEN);
        customObjBlock(ModBlocks.CRT_BSOD);
        customObjBlock(ModBlocks.CRT_CLEAN);
        customObjBlock(ModBlocks.STEEL_POLE);
        customObjBlock(ModBlocks.ANTENNA_TOP);
        customObjBlock(ModBlocks.PUTER);
        customObjBlock(ModBlocks.GEIGER_COUNTER_BLOCK);

        columnBlockWithItem(
                ModBlocks.DECON,
                modLoc("block/decon_side"),
                modLoc("block/decon_top"),
                modLoc("block/decon_side")
        );
        registerRadAbsorber();
        customObjBlock(ModBlocks.TAPE_RECORDER);
        customObjBlock(ModBlocks.TOASTER);
        customObjBlock(ModBlocks.DECO_STEEL_SCAFFOLD);
        customObjBlock(ModBlocks.STEEL_WALL);

        // Other
        customBombBlock(ModBlocks.AIRBOMB);
        customBombBlock(ModBlocks.BALEBOMB_TEST);
        customObjBlock(ModBlocks.BARREL_CORRODED);
        customObjBlock(ModBlocks.BARREL_IRON);
        customObjBlock(ModBlocks.BARREL_LOX);
        customObjBlock(ModBlocks.BARREL_ANTIMATTER);
        customObjBlock(ModBlocks.BARREL_PINK);
        customObjBlock(ModBlocks.BARREL_RED);
        customObjBlock(ModBlocks.BARREL_PLASTIC);
        customObjBlock(ModBlocks.BARREL_STEEL);
        customObjBlock(ModBlocks.BARREL_TAINT);
        customObjBlock(ModBlocks.BARREL_TCALLOY);
        customObjBlock(ModBlocks.BARREL_VITRIFIED);
        customObjBlock(ModBlocks.BARREL_YELLOW);
        customBombBlock(ModBlocks.DUD_CONVENTIONAL);
        customBombBlock(ModBlocks.DUD_NUKE);
        customBombBlock(ModBlocks.DUD_SALTED);
        simpleBlockWithItem(ModBlocks.MINE_AP.get(), models().getExistingFile(modLoc("block/bomb/mine_ap")));
        simpleBlockWithItem(ModBlocks.MINE_FAT.get(), models().getExistingFile(modLoc("block/bomb/mine_fat")));
        customBombBlock(ModBlocks.NAVAL_MINE);
        customObjBlock(ModBlocks.CRATE_CONSERVE);
        customObjBlock(ModBlocks.FILE_CABINET);

        simpleBlock(ModBlocks.UNIVERSAL_MACHINE_PART.get(), models().getBuilder(ModBlocks.UNIVERSAL_MACHINE_PART.getId().getPath()));
        // wire_coated: manual multipart blockstate + OBJ visibility (see assets/hbm_m/blockstates/wire_coated.json)

        blockWithItem(ModBlocks.CONVERTER_BLOCK);
        blockWithItem(ModBlocks.STEAM_CONDENSER);

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

        // FENSU / machine_battery_redd: nur eine flache Textur vorhanden (kein separates side/front/top-Set,
        // Original nutzte ein rotierendes OBJ-Modell) - dieselbe Textur fuer alle drei Seiten.
        orientableBlockWithItem(
                ModBlocks.MACHINE_FENSU,
                modLoc("block/machine_fensu"),
                modLoc("block/machine_fensu"),
                modLoc("block/machine_fensu")
        );
        orientableBlockWithItem(
                ModBlocks.FENSU2,
                modLoc("block/machine/fensu2"),
                modLoc("block/machine/fensu2"),
                modLoc("block/machine/fensu2")
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

        stairsBlock((StairBlock) ModBlocks.DEPTH_STONE_STAIRS.get(), modLoc("block/depth_stone"));
        simpleBlockItem(ModBlocks.DEPTH_STONE_STAIRS.get(), models().getExistingFile(modLoc("block/basalt_brick_stairs")));

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

        slabBlock((SlabBlock) ModBlocks.DEPTH_STONE_SLAB.get(),
                blockTexture(ModBlocks.DEPTH_STONE.get()),
                modLoc("block/depth_stone"));
        simpleBlockItem(ModBlocks.DEPTH_STONE_SLAB.get(),
                models().getExistingFile(modLoc("block/depth_stone_slab")));

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
                RegistrySupplier<Block> blockRegistrySupplier = ModBlocks.getIngotBlock(ingot);
                if (blockRegistrySupplier != null) {
                    resourceBlockWithItem(blockRegistrySupplier);
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
        oreWithItem(ModBlocks.SCHRABIDIUM_ORE);
        oreWithItem(ModBlocks.SCHRABIDIUM_ORE_NETHER);
        oreWithItem(ModBlocks.SCHRABIDIUM_ORE_GNEISS);

        simpleBlockWithItem(ModBlocks.BLOCK_SCHRABIDIUM_CLUSTER.get(),
                models().cubeBottomTop(
                        ModBlocks.BLOCK_SCHRABIDIUM_CLUSTER.getId().getPath(),
                        modLoc("block/block_schrabidium_cluster_side"),
                        modLoc("block/block_schrabidium_cluster_top"),
                        modLoc("block/block_schrabidium_cluster_top")
                )
        );

        // ══════════════════════════════════════════════════════════════════════
        // DEV: Modelle fuer importierte fehlende Bloecke (siehe ModBlocks DEV-Sektion)
        // ══════════════════════════════════════════════════════════════════════
        simpleBlockWithItem(ModBlocks.ANCIENT_SCRAP.get(),
                models().cubeAll(
                        ModBlocks.ANCIENT_SCRAP.getId().getPath(),
                        modLoc("block/ancient_scrap")
                )
        );
        simpleBlockWithItem(ModBlocks.ASH_DIGAMMA.get(),
                models().cubeAll(
                        ModBlocks.ASH_DIGAMMA.getId().getPath(),
                        modLoc("block/ash_digamma")
                )
        );
        simpleBlockWithItem(ModBlocks.ASPHALT_LIGHT.get(),
                models().cubeAll(
                        ModBlocks.ASPHALT_LIGHT.getId().getPath(),
                        modLoc("block/asphalt_light")
                )
        );
        simpleBlockWithItem(ModBlocks.BARBED_WIRE_ACID.get(),
                models().cubeAll(
                        ModBlocks.BARBED_WIRE_ACID.getId().getPath(),
                        modLoc("block/barbed_wire_acid")
                )
        );
        simpleBlockWithItem(ModBlocks.BARBED_WIRE_ULTRADEATH.get(),
                models().cubeAll(
                        ModBlocks.BARBED_WIRE_ULTRADEATH.getId().getPath(),
                        modLoc("block/barbed_wire_ultradeath")
                )
        );
        simpleBlockWithItem(ModBlocks.BASALT.get(),
                models().cubeBottomTop(
                        ModBlocks.BASALT.getId().getPath(),
                        modLoc("block/basalt"),
                        modLoc("block/basalt"),
                        modLoc("block/basalt_top")
                )
        );
        simpleBlockWithItem(ModBlocks.BASALT_SMOOTH.get(),
                models().cubeAll(
                        ModBlocks.BASALT_SMOOTH.getId().getPath(),
                        modLoc("block/basalt_smooth")
                )
        );
        simpleBlockWithItem(ModBlocks.BASALT_TILES.get(),
                models().cubeAll(
                        ModBlocks.BASALT_TILES.getId().getPath(),
                        modLoc("block/basalt_tiles")
                )
        );
        simpleBlockWithItem(ModBlocks.BATTERY_LITHIUM_BLOCK.get(),
                models().cubeBottomTop(
                        ModBlocks.BATTERY_LITHIUM_BLOCK.getId().getPath(),
                        modLoc("block/battery_lithium_side"),
                        modLoc("block/battery_lithium_side"),
                        modLoc("block/battery_lithium_top")
                )
        );
        simpleBlockWithItem(ModBlocks.BATTERY_POTATO_BLOCK.get(),
                models().cubeBottomTop(
                        ModBlocks.BATTERY_POTATO_BLOCK.getId().getPath(),
                        modLoc("block/battery_potato_side"),
                        modLoc("block/battery_potato_side"),
                        modLoc("block/battery_potato_top")
                )
        );
        simpleBlockWithItem(ModBlocks.BATTERY_SCHRABIDIUM_BLOCK.get(),
                models().cubeBottomTop(
                        ModBlocks.BATTERY_SCHRABIDIUM_BLOCK.getId().getPath(),
                        modLoc("block/battery_schrabidium_side"),
                        modLoc("block/battery_schrabidium_side"),
                        modLoc("block/battery_schrabidium_top")
                )
        );
        simpleBlockWithItem(ModBlocks.BLAST_DOOR.get(),
                models().cubeAll(
                        ModBlocks.BLAST_DOOR.getId().getPath(),
                        modLoc("block/blast_door")
                )
        );
        simpleBlockWithItem(ModBlocks.BLOCK_ALUMINIUM.get(),
                models().cubeAll(
                        ModBlocks.BLOCK_ALUMINIUM.getId().getPath(),
                        modLoc("block/block_aluminium")
                )
        );
        simpleBlockWithItem(ModBlocks.BOXCAR.get(),
                models().cubeAll(
                        ModBlocks.BOXCAR.getId().getPath(),
                        modLoc("block/boxcar")
                )
        );
        simpleBlockWithItem(ModBlocks.BRICK_ASBESTOS.get(),
                models().cubeAll(
                        ModBlocks.BRICK_ASBESTOS.getId().getPath(),
                        modLoc("block/brick_asbestos")
                )
        );
        simpleBlockWithItem(ModBlocks.BRICK_COMPOUND.get(),
                models().cubeAll(
                        ModBlocks.BRICK_COMPOUND.getId().getPath(),
                        modLoc("block/brick_compound")
                )
        );
        simpleBlockWithItem(ModBlocks.BRICK_JUNGLE.get(),
                models().cubeAll(
                        ModBlocks.BRICK_JUNGLE.getId().getPath(),
                        modLoc("block/brick_jungle")
                )
        );
        simpleBlockWithItem(ModBlocks.BRICK_JUNGLE_CIRCLE.get(),
                models().cubeAll(
                        ModBlocks.BRICK_JUNGLE_CIRCLE.getId().getPath(),
                        modLoc("block/brick_jungle_circle")
                )
        );
        simpleBlockWithItem(ModBlocks.BRICK_JUNGLE_CRACKED.get(),
                models().cubeAll(
                        ModBlocks.BRICK_JUNGLE_CRACKED.getId().getPath(),
                        modLoc("block/brick_jungle_cracked")
                )
        );
        simpleBlockWithItem(ModBlocks.BRICK_JUNGLE_FRAGILE.get(),
                models().cubeAll(
                        ModBlocks.BRICK_JUNGLE_FRAGILE.getId().getPath(),
                        modLoc("block/brick_jungle_fragile")
                )
        );
        simpleBlockWithItem(ModBlocks.BRICK_JUNGLE_GLYPH.get(),
                models().cubeAll(
                        ModBlocks.BRICK_JUNGLE_GLYPH.getId().getPath(),
                        modLoc("block/brick_jungle_glyph_0")
                )
        );
        simpleBlockWithItem(ModBlocks.BRICK_JUNGLE_LAVA.get(),
                models().cubeAll(
                        ModBlocks.BRICK_JUNGLE_LAVA.getId().getPath(),
                        modLoc("block/brick_jungle_lava")
                )
        );
        simpleBlockWithItem(ModBlocks.BRICK_JUNGLE_MYSTIC.get(),
                models().cubeAll(
                        ModBlocks.BRICK_JUNGLE_MYSTIC.getId().getPath(),
                        modLoc("block/brick_jungle_mystic")
                )
        );
        simpleBlockWithItem(ModBlocks.BRICK_JUNGLE_OOZE.get(),
                models().cubeAll(
                        ModBlocks.BRICK_JUNGLE_OOZE.getId().getPath(),
                        modLoc("block/brick_jungle_ooze")
                )
        );
        simpleBlockWithItem(ModBlocks.BRICK_JUNGLE_TRAP.get(),
                models().cubeAll(
                        ModBlocks.BRICK_JUNGLE_TRAP.getId().getPath(),
                        modLoc("block/brick_jungle_trap")
                )
        );
        simpleBlockWithItem(ModBlocks.BRICK_RED.get(),
                models().cubeBottomTop(
                        ModBlocks.BRICK_RED.getId().getPath(),
                        modLoc("block/brick_red"),
                        modLoc("block/brick_red"),
                        modLoc("block/brick_red_top")
                )
        );
        simpleBlock(ModBlocks.BROADCASTER_PC.get(),
                models().getExistingFile(modLoc("block/machines/broadcaster_pc")));
        simpleBlockItem(ModBlocks.BROADCASTER_PC.get(),
                models().getExistingFile(modLoc("block/machines/broadcaster_pc")));
        simpleBlockWithItem(ModBlocks.CABLE_DETECTOR.get(),
                models().cubeAll(
                        ModBlocks.CABLE_DETECTOR.getId().getPath(),
                        modLoc("block/cable_detector_off")
                )
        );
        simpleBlockWithItem(ModBlocks.CABLE_DIODE.get(),
                models().cubeAll(
                        ModBlocks.CABLE_DIODE.getId().getPath(),
                        modLoc("block/cable_diode")
                )
        );
        simpleBlockWithItem(ModBlocks.CABLE_SWITCH.get(),
                models().cubeAll(
                        ModBlocks.CABLE_SWITCH.getId().getPath(),
                        modLoc("block/cable_switch_off")
                )
        );
        simpleBlockWithItem(ModBlocks.CAPACITOR_BUS.get(),
                models().cubeAll(
                        ModBlocks.CAPACITOR_BUS.getId().getPath(),
                        modLoc("block/capacitor_bus_side")
                )
        );
        simpleBlockWithItem(ModBlocks.CAPACITOR_COPPER.get(),
                models().cubeBottomTop(
                        ModBlocks.CAPACITOR_COPPER.getId().getPath(),
                        modLoc("block/capacitor_copper_side"),
                        modLoc("block/capacitor_copper_bottom"),
                        modLoc("block/capacitor_copper_top")
                )
        );
        simpleBlockWithItem(ModBlocks.CAPACITOR_GOLD.get(),
                models().cubeBottomTop(
                        ModBlocks.CAPACITOR_GOLD.getId().getPath(),
                        modLoc("block/capacitor_gold_side"),
                        modLoc("block/capacitor_gold_bottom"),
                        modLoc("block/capacitor_gold_top")
                )
        );
        simpleBlockWithItem(ModBlocks.CAPACITOR_NIOBIUM.get(),
                models().cubeBottomTop(
                        ModBlocks.CAPACITOR_NIOBIUM.getId().getPath(),
                        modLoc("block/capacitor_niobium_side"),
                        modLoc("block/capacitor_niobium_bottom"),
                        modLoc("block/capacitor_niobium_top")
                )
        );
        simpleBlockWithItem(ModBlocks.CAPACITOR_SCHRABIDATE.get(),
                models().cubeBottomTop(
                        ModBlocks.CAPACITOR_SCHRABIDATE.getId().getPath(),
                        modLoc("block/capacitor_schrabidate_side"),
                        modLoc("block/capacitor_schrabidate_bottom"),
                        modLoc("block/capacitor_schrabidate_top")
                )
        );
        simpleBlockWithItem(ModBlocks.CAPACITOR_TANTALIUM.get(),
                models().cubeBottomTop(
                        ModBlocks.CAPACITOR_TANTALIUM.getId().getPath(),
                        modLoc("block/capacitor_tantalium_side"),
                        modLoc("block/capacitor_tantalium_bottom"),
                        modLoc("block/capacitor_tantalium_top")
                )
        );
        simpleBlockWithItem(ModBlocks.CHARGE_C4.get(),
                models().cubeAll(
                        ModBlocks.CHARGE_C4.getId().getPath(),
                        modLoc("block/charge_c4")
                )
        );
        simpleBlockWithItem(ModBlocks.CHARGE_DYNAMITE.get(),
                models().cubeAll(
                        ModBlocks.CHARGE_DYNAMITE.getId().getPath(),
                        modLoc("block/charge_dynamite")
                )
        );
        simpleBlockWithItem(ModBlocks.CHARGE_MINER.get(),
                models().cubeAll(
                        ModBlocks.CHARGE_MINER.getId().getPath(),
                        modLoc("block/charge_miner")
                )
        );
        simpleBlockWithItem(ModBlocks.CHARGE_SEMTEX.get(),
                models().cubeAll(
                        ModBlocks.CHARGE_SEMTEX.getId().getPath(),
                        modLoc("block/charge_semtex")
                )
        );
        simpleBlockWithItem(ModBlocks.CHLORINE_GAS.get(),
                models().cubeAll(
                        ModBlocks.CHLORINE_GAS.getId().getPath(),
                        modLoc("block/chlorine_gas")
                )
        );
        simpleBlockWithItem(ModBlocks.CLUSTER_ALUMINIUM.get(),
                models().cubeAll(
                        ModBlocks.CLUSTER_ALUMINIUM.getId().getPath(),
                        modLoc("block/cluster_aluminium")
                )
        );
        simpleBlockWithItem(ModBlocks.CLUSTER_COPPER.get(),
                models().cubeAll(
                        ModBlocks.CLUSTER_COPPER.getId().getPath(),
                        modLoc("block/cluster_copper")
                )
        );
        simpleBlockWithItem(ModBlocks.CLUSTER_DEPTH_IRON.get(),
                models().cubeAll(
                        ModBlocks.CLUSTER_DEPTH_IRON.getId().getPath(),
                        modLoc("block/cluster_depth_iron")
                )
        );
        simpleBlockWithItem(ModBlocks.CLUSTER_DEPTH_TITANIUM.get(),
                models().cubeAll(
                        ModBlocks.CLUSTER_DEPTH_TITANIUM.getId().getPath(),
                        modLoc("block/cluster_depth_titanium")
                )
        );
        simpleBlockWithItem(ModBlocks.CLUSTER_DEPTH_TUNGSTEN.get(),
                models().cubeAll(
                        ModBlocks.CLUSTER_DEPTH_TUNGSTEN.getId().getPath(),
                        modLoc("block/cluster_depth_tungsten")
                )
        );
        simpleBlockWithItem(ModBlocks.CLUSTER_IRON.get(),
                models().cubeAll(
                        ModBlocks.CLUSTER_IRON.getId().getPath(),
                        modLoc("block/cluster_iron")
                )
        );
        simpleBlockWithItem(ModBlocks.CLUSTER_TITANIUM.get(),
                models().cubeAll(
                        ModBlocks.CLUSTER_TITANIUM.getId().getPath(),
                        modLoc("block/cluster_titanium")
                )
        );
        simpleBlockWithItem(ModBlocks.CM_FLUX.get(),
                models().cubeBottomTop(
                        ModBlocks.CM_FLUX.getId().getPath(),
                        modLoc("block/cm_flux_side"),
                        modLoc("block/cm_flux_side"),
                        modLoc("block/cm_flux_top")
                )
        );
        simpleBlockWithItem(ModBlocks.CM_HEAT.get(),
                models().cubeBottomTop(
                        ModBlocks.CM_HEAT.getId().getPath(),
                        modLoc("block/cm_heat_side"),
                        modLoc("block/cm_heat_side"),
                        modLoc("block/cm_heat_top")
                )
        );
        simpleBlockWithItem(ModBlocks.CMB_BRICK.get(),
                models().cubeAll(
                        ModBlocks.CMB_BRICK.getId().getPath(),
                        modLoc("block/cmb_brick")
                )
        );
        simpleBlockWithItem(ModBlocks.CMB_BRICK_REINFORCED.get(),
                models().cubeAll(
                        ModBlocks.CMB_BRICK_REINFORCED.getId().getPath(),
                        modLoc("block/cmb_brick_reinforced")
                )
        );
        simpleBlockWithItem(ModBlocks.COMPACT_LAUNCHER.get(),
                models().cubeAll(
                        ModBlocks.COMPACT_LAUNCHER.getId().getPath(),
                        modLoc("block/compact_launcher")
                )
        );
        simpleBlockWithItem(ModBlocks.CONCRETE_COLORED_EXT_BRONZE.get(),
                models().cubeAll(
                        ModBlocks.CONCRETE_COLORED_EXT_BRONZE.getId().getPath(),
                        modLoc("block/concrete_colored_ext_bronze")
                )
        );
        simpleBlockWithItem(ModBlocks.CONCRETE_COLORED_EXT_HAZARD.get(),
                models().cubeAll(
                        ModBlocks.CONCRETE_COLORED_EXT_HAZARD.getId().getPath(),
                        modLoc("block/concrete_colored_ext_hazard")
                )
        );
        simpleBlockWithItem(ModBlocks.CONCRETE_COLORED_EXT_INDIGO.get(),
                models().cubeAll(
                        ModBlocks.CONCRETE_COLORED_EXT_INDIGO.getId().getPath(),
                        modLoc("block/concrete_colored_ext_indigo")
                )
        );
        simpleBlockWithItem(ModBlocks.CONCRETE_COLORED_EXT_MACHINE.get(),
                models().cubeAll(
                        ModBlocks.CONCRETE_COLORED_EXT_MACHINE.getId().getPath(),
                        modLoc("block/concrete_colored_ext_machine")
                )
        );
        simpleBlockWithItem(ModBlocks.CONCRETE_COLORED_EXT_MACHINE_STRIPE.get(),
                models().cubeAll(
                        ModBlocks.CONCRETE_COLORED_EXT_MACHINE_STRIPE.getId().getPath(),
                        modLoc("block/concrete_colored_ext_machine_stripe")
                )
        );
        simpleBlockWithItem(ModBlocks.CONCRETE_COLORED_EXT_PINK.get(),
                models().cubeAll(
                        ModBlocks.CONCRETE_COLORED_EXT_PINK.getId().getPath(),
                        modLoc("block/concrete_colored_ext_pink")
                )
        );
        simpleBlockWithItem(ModBlocks.CONCRETE_COLORED_EXT_PURPLE.get(),
                models().cubeAll(
                        ModBlocks.CONCRETE_COLORED_EXT_PURPLE.getId().getPath(),
                        modLoc("block/concrete_colored_ext_purple")
                )
        );
        simpleBlockWithItem(ModBlocks.CONCRETE_COLORED_EXT_SAND.get(),
                models().cubeAll(
                        ModBlocks.CONCRETE_COLORED_EXT_SAND.getId().getPath(),
                        modLoc("block/concrete_colored_ext_sand")
                )
        );
        simpleBlockWithItem(ModBlocks.CONVEYOR.get(),
                models().cubeAll(
                        ModBlocks.CONVEYOR.getId().getPath(),
                        modLoc("block/conveyor")
                )
        );
        simpleBlockWithItem(ModBlocks.CONVEYOR_DOUBLE.get(),
                models().cubeAll(
                        ModBlocks.CONVEYOR_DOUBLE.getId().getPath(),
                        modLoc("block/conveyor_double")
                )
        );
        simpleBlockWithItem(ModBlocks.CONVEYOR_EXPRESS.get(),
                models().cubeAll(
                        ModBlocks.CONVEYOR_EXPRESS.getId().getPath(),
                        modLoc("block/conveyor_express")
                )
        );
        simpleBlockWithItem(ModBlocks.CONVEYOR_TRIPLE.get(),
                models().cubeAll(
                        ModBlocks.CONVEYOR_TRIPLE.getId().getPath(),
                        modLoc("block/conveyor_triple")
                )
        );
        simpleBlockWithItem(ModBlocks.CRANE_PARTITIONER.get(),
                models().cubeAll(
                        ModBlocks.CRANE_PARTITIONER.getId().getPath(),
                        modLoc("block/crane_partitioner_back")
                )
        );
        simpleBlockWithItem(ModBlocks.CRANE_ROUTER.get(),
                models().cubeAll(
                        ModBlocks.CRANE_ROUTER.getId().getPath(),
                        modLoc("block/crane_router_overlay")
                )
        );
        horizontalBlock(ModBlocks.CRANE_SPLITTER.get(),
                models().cubeBottomTop(
                        ModBlocks.CRANE_SPLITTER.getId().getPath(),
                        modLoc("block/crane_splitter_inner_side"),
                        modLoc("block/crane_splitter_inner"),
                        modLoc("block/crane_splitter_belt")
                )
        );
        simpleBlockItem(ModBlocks.CRANE_SPLITTER.get(),
                models().getExistingFile(modLoc(ModBlocks.CRANE_SPLITTER.getId().getPath())));
        simpleBlockWithItem(ModBlocks.CRATE_AMMO.get(),
                models().cubeBottomTop(
                        ModBlocks.CRATE_AMMO.getId().getPath(),
                        modLoc("block/crate_ammo_side"),
                        modLoc("block/crate_ammo_bottom"),
                        modLoc("block/crate_ammo_top")
                )
        );
        simpleBlockWithItem(ModBlocks.CRATE_CAN.get(),
                models().cubeBottomTop(
                        ModBlocks.CRATE_CAN.getId().getPath(),
                        modLoc("block/crate_can_side"),
                        modLoc("block/crate_can_bottom"),
                        modLoc("block/crate_can_top")
                )
        );
        simpleBlockWithItem(ModBlocks.CRATE_JUNGLE.get(),
                models().cubeAll(
                        ModBlocks.CRATE_JUNGLE.getId().getPath(),
                        modLoc("block/crate_jungle")
                )
        );
        simpleBlockWithItem(ModBlocks.CRATE_RED.get(),
                models().cubeAll(
                        ModBlocks.CRATE_RED.getId().getPath(),
                        modLoc("block/crate_red")
                )
        );
        simpleBlockWithItem(ModBlocks.DECO_ALUMINIUM.get(),
                models().cubeAll(
                        ModBlocks.DECO_ALUMINIUM.getId().getPath(),
                        modLoc("block/deco_aluminium")
                )
        );
        simpleBlockWithItem(ModBlocks.DEPTH_DNT.get(),
                models().cubeAll(
                        ModBlocks.DEPTH_DNT.getId().getPath(),
                        modLoc("block/depth_dnt")
                )
        );
        simpleBlockWithItem(ModBlocks.DET_CHARGE.get(),
                models().cubeAll(
                        ModBlocks.DET_CHARGE.getId().getPath(),
                        modLoc("block/det_charge")
                )
        );
        simpleBlockWithItem(ModBlocks.DET_CORD.get(),
                models().cubeAll(
                        ModBlocks.DET_CORD.getId().getPath(),
                        modLoc("block/det_cord")
                )
        );
        simpleBlockWithItem(ModBlocks.DET_NUKE.get(),
                models().cubeBottomTop(
                        ModBlocks.DET_NUKE.getId().getPath(),
                        modLoc("block/det_nuke"),
                        modLoc("block/det_nuke"),
                        modLoc("block/det_nuke_top")
                )
        );
        simpleBlockWithItem(ModBlocks.DFC_CORE.get(),
                models().cubeAll(
                        ModBlocks.DFC_CORE.getId().getPath(),
                        modLoc("block/dfc_core")
                )
        );
        simpleBlockWithItem(ModBlocks.DFC_EMITTER.get(),
                models().cubeAll(
                        ModBlocks.DFC_EMITTER.getId().getPath(),
                        modLoc("block/dfc_emitter")
                )
        );
        simpleBlockWithItem(ModBlocks.DFC_INJECTOR.get(),
                models().cubeAll(
                        ModBlocks.DFC_INJECTOR.getId().getPath(),
                        modLoc("block/dfc_injector")
                )
        );
        simpleBlockWithItem(ModBlocks.DFC_RECEIVER.get(),
                models().cubeAll(
                        ModBlocks.DFC_RECEIVER.getId().getPath(),
                        modLoc("block/dfc_receiver")
                )
        );
        simpleBlockWithItem(ModBlocks.DFC_STABILIZER.get(),
                models().cubeAll(
                        ModBlocks.DFC_STABILIZER.getId().getPath(),
                        modLoc("block/dfc_stabilizer")
                )
        );
        simpleBlockWithItem(ModBlocks.DIRT_DEAD.get(),
                models().cubeAll(
                        ModBlocks.DIRT_DEAD.getId().getPath(),
                        modLoc("block/dirt_dead")
                )
        );
        simpleBlockWithItem(ModBlocks.DIRT_OILY.get(),
                models().cubeAll(
                        ModBlocks.DIRT_OILY.getId().getPath(),
                        modLoc("block/dirt_oily")
                )
        );
        simpleBlockWithItem(ModBlocks.DRONE_CRATE.get(),
                models().cubeBottomTop(
                        ModBlocks.DRONE_CRATE.getId().getPath(),
                        modLoc("block/drone_crate_side"),
                        modLoc("block/drone_crate_bottom"),
                        modLoc("block/drone_crate_top")
                )
        );
        simpleBlockWithItem(ModBlocks.DRONE_CRATE_PROVIDER.get(),
                models().cubeBottomTop(
                        ModBlocks.DRONE_CRATE_PROVIDER.getId().getPath(),
                        modLoc("block/drone_crate_provider_side"),
                        modLoc("block/drone_crate_provider_bottom"),
                        modLoc("block/drone_crate_provider_top")
                )
        );
        simpleBlockWithItem(ModBlocks.DRONE_CRATE_REQUESTER.get(),
                models().cubeBottomTop(
                        ModBlocks.DRONE_CRATE_REQUESTER.getId().getPath(),
                        modLoc("block/drone_crate_requester_side"),
                        modLoc("block/drone_crate_requester_bottom"),
                        modLoc("block/drone_crate_requester_top")
                )
        );
        simpleBlockWithItem(ModBlocks.DRONE_DOCK.get(),
                models().cubeBottomTop(
                        ModBlocks.DRONE_DOCK.getId().getPath(),
                        modLoc("block/drone_dock_side"),
                        modLoc("block/drone_dock_bottom"),
                        modLoc("block/drone_dock_top")
                )
        );
        directionlessFacingBlock(ModBlocks.DRONE_WAYPOINT.get(),
                models().cubeAll(
                        ModBlocks.DRONE_WAYPOINT.getId().getPath(),
                        modLoc("block/drone_waypoint")
                )
        );
        directionlessFacingBlock(ModBlocks.DRONE_WAYPOINT_REQUEST.get(),
                models().cubeAll(
                        ModBlocks.DRONE_WAYPOINT_REQUEST.getId().getPath(),
                        modLoc("block/drone_waypoint_request")
                )
        );
        directionlessFacingBlock(ModBlocks.RADIO_TORCH_SENDER.get(),
                models().cubeAll(
                        ModBlocks.RADIO_TORCH_SENDER.getId().getPath(),
                        modLoc("block/rtty_sender_off")
                )
        );
        directionlessFacingBlock(ModBlocks.RADIO_TORCH_RECEIVER.get(),
                models().cubeAll(
                        ModBlocks.RADIO_TORCH_RECEIVER.getId().getPath(),
                        modLoc("block/rtty_rec_off")
                )
        );
        directionlessFacingBlock(ModBlocks.RADIO_TORCH_LOGIC.get(),
                models().cubeAll(
                        ModBlocks.RADIO_TORCH_LOGIC.getId().getPath(),
                        modLoc("block/rtty_logic_off")
                )
        );
        directionlessFacingBlock(ModBlocks.RADIO_TORCH_READER.get(),
                models().cubeAll(
                        ModBlocks.RADIO_TORCH_READER.getId().getPath(),
                        modLoc("block/rtty_reader")
                )
        );
        directionlessFacingBlock(ModBlocks.RADIO_TORCH_CONTROLLER.get(),
                models().cubeAll(
                        ModBlocks.RADIO_TORCH_CONTROLLER.getId().getPath(),
                        modLoc("block/rtty_controller")
                )
        );
        directionlessFacingBlock(ModBlocks.RADIO_TORCH_COUNTER.get(),
                models().cubeAll(
                        ModBlocks.RADIO_TORCH_COUNTER.getId().getPath(),
                        modLoc("block/rtty_counter")
                )
        );
        simpleBlockWithItem(ModBlocks.DUCRETE.get(),
                models().cubeAll(
                        ModBlocks.DUCRETE.getId().getPath(),
                        modLoc("block/ducrete")
                )
        );
        simpleBlockWithItem(ModBlocks.DYNAMITE.get(),
                models().cubeBottomTop(
                        ModBlocks.DYNAMITE.getId().getPath(),
                        modLoc("block/dynamite_side"),
                        modLoc("block/dynamite_bottom"),
                        modLoc("block/dynamite_top")
                )
        );
        simpleBlockWithItem(ModBlocks.FACTORY_ADVANCED_HULL.get(),
                models().cubeAll(
                        ModBlocks.FACTORY_ADVANCED_HULL.getId().getPath(),
                        modLoc("block/factory_advanced_hull")
                )
        );
        simpleBlockWithItem(ModBlocks.FACTORY_TITANIUM_HULL.get(),
                models().cubeAll(
                        ModBlocks.FACTORY_TITANIUM_HULL.getId().getPath(),
                        modLoc("block/factory_titanium_hull")
                )
        );
        simpleBlockWithItem(ModBlocks.FENCE_METAL.get(),
                models().cubeAll(
                        ModBlocks.FENCE_METAL.getId().getPath(),
                        modLoc("block/fence_metal")
                )
        );
        simpleBlockWithItem(ModBlocks.FENCE_METAL_POST.get(),
                models().cubeAll(
                        ModBlocks.FENCE_METAL_POST.getId().getPath(),
                        modLoc("block/fence_metal_post")
                )
        );
        simpleBlockWithItem(ModBlocks.FIELD_DISTURBER.get(),
                models().cubeAll(
                        ModBlocks.FIELD_DISTURBER.getId().getPath(),
                        modLoc("block/field_disturber")
                )
        );
        simpleBlockWithItem(ModBlocks.FIRE_DIGAMMA.get(),
                models().cubeAll(
                        ModBlocks.FIRE_DIGAMMA.getId().getPath(),
                        modLoc("block/fire_digamma")
                )
        );
        simpleBlockWithItem(ModBlocks.FIREWORKS.get(),
                models().cubeBottomTop(
                        ModBlocks.FIREWORKS.getId().getPath(),
                        modLoc("block/fireworks_side"),
                        modLoc("block/fireworks_bottom"),
                        modLoc("block/fireworks_top")
                )
        );
        simpleBlockWithItem(ModBlocks.FISSURE_BOMB.get(),
                models().cubeBottomTop(
                        ModBlocks.FISSURE_BOMB.getId().getPath(),
                        modLoc("block/fissure_bomb_side"),
                        modLoc("block/fissure_bomb_bottom"),
                        modLoc("block/fissure_bomb_top")
                )
        );
        simpleBlockWithItem(ModBlocks.FLAME_WAR.get(),
                models().cubeAll(
                        ModBlocks.FLAME_WAR.getId().getPath(),
                        modLoc("block/flame_war")
                )
        );
        simpleBlockWithItem(ModBlocks.FLUID_COUNTER_VALVE.get(),
                models().cubeAll(
                        ModBlocks.FLUID_COUNTER_VALVE.getId().getPath(),
                        modLoc("block/fluid_counter_valve_off")
                )
        );
        simpleBlockWithItem(ModBlocks.FLUID_DUCT_BOX.get(),
                models().cubeAll(
                        ModBlocks.FLUID_DUCT_BOX.getId().getPath(),
                        modLoc("block/fluid_duct_box")
                )
        );
        simpleBlockWithItem(ModBlocks.FLUID_DUCT_EXHAUST.get(),
                models().cubeAll(
                        ModBlocks.FLUID_DUCT_EXHAUST.getId().getPath(),
                        modLoc("block/fluid_duct_box")
                )
        );
        simpleBlockWithItem(ModBlocks.PIPE_ANCHOR.get(),
                models().cubeAll(
                        ModBlocks.PIPE_ANCHOR.getId().getPath(),
                        modLoc("block/block_steel")
                )
        );
        simpleBlockWithItem(ModBlocks.FLUID_DUCT_PAINTABLE.get(),
                models().cubeAll(
                        ModBlocks.FLUID_DUCT_PAINTABLE.getId().getPath(),
                        modLoc("block/fluid_duct_paintable")
                )
        );
        simpleBlockWithItem(ModBlocks.FLUID_SWITCH.get(),
                models().cubeAll(
                        ModBlocks.FLUID_SWITCH.getId().getPath(),
                        modLoc("block/fluid_switch_off")
                )
        );
        simpleBlock(ModBlocks.SLAG_DYNAMIC.get(),
                models().cubeAll(
                        ModBlocks.SLAG_DYNAMIC.getId().getPath(),
                        modLoc("block/slag_dynamic")
                )
        );
        simpleBlockWithItem(ModBlocks.FOUNDRY_MOLD.get(),
                models().cubeBottomTop(
                        ModBlocks.FOUNDRY_MOLD.getId().getPath(),
                        modLoc("block/foundry_mold_side"),
                        modLoc("block/foundry_mold_bottom"),
                        modLoc("block/foundry_mold_top")
                )
        );
        // foundry_slagtap uses a hand-written static blockstate+model (directional spout shape,
        // matching foundry_outlet's precedent - see assets/hbm_m/blockstates/foundry_slagtap.json).
        simpleBlockWithItem(ModBlocks.FOUNDRY_TANK.get(),
                models().cubeBottomTop(
                        ModBlocks.FOUNDRY_TANK.getId().getPath(),
                        modLoc("block/foundry_tank_side"),
                        modLoc("block/foundry_tank_bottom"),
                        modLoc("block/foundry_tank_top")
                )
        );
        simpleBlockWithItem(ModBlocks.FROZEN_DIRT.get(),
                models().cubeAll(
                        ModBlocks.FROZEN_DIRT.getId().getPath(),
                        modLoc("block/frozen_dirt")
                )
        );
        simpleBlockWithItem(ModBlocks.FROZEN_GRASS.get(),
                models().cubeBottomTop(
                        ModBlocks.FROZEN_GRASS.getId().getPath(),
                        modLoc("block/frozen_grass_side"),
                        modLoc("block/frozen_grass_side"),
                        modLoc("block/frozen_grass_top")
                )
        );
        simpleBlockWithItem(ModBlocks.FROZEN_LOG.get(),
                models().cubeBottomTop(
                        ModBlocks.FROZEN_LOG.getId().getPath(),
                        modLoc("block/frozen_log"),
                        modLoc("block/frozen_log"),
                        modLoc("block/frozen_log_top")
                )
        );
        simpleBlockWithItem(ModBlocks.FROZEN_PLANKS.get(),
                models().cubeAll(
                        ModBlocks.FROZEN_PLANKS.getId().getPath(),
                        modLoc("block/frozen_planks")
                )
        );
        simpleBlockWithItem(ModBlocks.FUSION_COMPONENT.get(),
                models().cubeAll(
                        ModBlocks.FUSION_COMPONENT.getId().getPath(),
                        modLoc("block/fusion_component")
                )
        );
        simpleBlockWithItem(ModBlocks.FUSION_COMPONENT_BLANKET.get(),
                models().cubeAll(
                        ModBlocks.FUSION_COMPONENT_BLANKET.getId().getPath(),
                        modLoc("block/fusion_component_blanket")
                )
        );
        simpleBlockWithItem(ModBlocks.FUSION_COMPONENT_BSCCO_WELDED.get(),
                models().cubeAll(
                        ModBlocks.FUSION_COMPONENT_BSCCO_WELDED.getId().getPath(),
                        modLoc("block/fusion_component_bscco_welded")
                )
        );
        simpleBlockWithItem(ModBlocks.FUSION_COMPONENT_MOTOR.get(),
                models().cubeAll(
                        ModBlocks.FUSION_COMPONENT_MOTOR.getId().getPath(),
                        modLoc("block/fusion_component_motor")
                )
        );
        simpleBlockWithItem(ModBlocks.FUSION_HATCH.get(),
                models().cubeAll(
                        ModBlocks.FUSION_HATCH.getId().getPath(),
                        modLoc("block/fusion_hatch")
                )
        );
        simpleBlockWithItem(ModBlocks.FUSION_HEATER.get(),
                models().cubeBottomTop(
                        ModBlocks.FUSION_HEATER.getId().getPath(),
                        modLoc("block/fusion_heater_side"),
                        modLoc("block/fusion_heater_side"),
                        modLoc("block/fusion_heater_top")
                )
        );
        simpleBlockWithItem(ModBlocks.GAS_ASBESTOS.get(),
                models().cubeAll(
                        ModBlocks.GAS_ASBESTOS.getId().getPath(),
                        modLoc("block/gas_asbestos")
                )
        );
        simpleBlockWithItem(ModBlocks.GAS_COAL.get(),
                models().cubeAll(
                        ModBlocks.GAS_COAL.getId().getPath(),
                        modLoc("block/gas_coal")
                )
        );
        simpleBlockWithItem(ModBlocks.GAS_EXPLOSIVE.get(),
                models().cubeAll(
                        ModBlocks.GAS_EXPLOSIVE.getId().getPath(),
                        modLoc("block/gas_explosive")
                )
        );
        simpleBlockWithItem(ModBlocks.GAS_FLAMMABLE.get(),
                models().cubeAll(
                        ModBlocks.GAS_FLAMMABLE.getId().getPath(),
                        modLoc("block/gas_flammable")
                )
        );
        simpleBlockWithItem(ModBlocks.GAS_MELTDOWN.get(),
                models().cubeAll(
                        ModBlocks.GAS_MELTDOWN.getId().getPath(),
                        modLoc("block/gas_meltdown")
                )
        );
        simpleBlockWithItem(ModBlocks.GAS_MONOXIDE.get(),
                models().cubeAll(
                        ModBlocks.GAS_MONOXIDE.getId().getPath(),
                        modLoc("block/gas_monoxide")
                )
        );
        simpleBlockWithItem(ModBlocks.GAS_RADON.get(),
                models().cubeAll(
                        ModBlocks.GAS_RADON.getId().getPath(),
                        modLoc("block/gas_radon")
                )
        );
        simpleBlockWithItem(ModBlocks.GAS_RADON_DENSE.get(),
                models().cubeAll(
                        ModBlocks.GAS_RADON_DENSE.getId().getPath(),
                        modLoc("block/gas_radon_dense")
                )
        );
        simpleBlockWithItem(ModBlocks.GAS_RADON_TOMB.get(),
                models().cubeAll(
                        ModBlocks.GAS_RADON_TOMB.getId().getPath(),
                        modLoc("block/gas_radon_tomb")
                )
        );
        simpleBlockWithItem(ModBlocks.GLASS_ASH.get(),
                models().cubeAll(
                        ModBlocks.GLASS_ASH.getId().getPath(),
                        modLoc("block/glass_ash")
                )
        );
        simpleBlockWithItem(ModBlocks.GLASS_BORON.get(),
                models().cubeAll(
                        ModBlocks.GLASS_BORON.getId().getPath(),
                        modLoc("block/glass_boron")
                )
        );
        simpleBlockWithItem(ModBlocks.GLASS_LEAD.get(),
                models().cubeAll(
                        ModBlocks.GLASS_LEAD.getId().getPath(),
                        modLoc("block/glass_lead")
                )
        );
        simpleBlockWithItem(ModBlocks.GLASS_POLARIZED.get(),
                models().cubeAll(
                        ModBlocks.GLASS_POLARIZED.getId().getPath(),
                        modLoc("block/glass_polarized")
                )
        );
        simpleBlockWithItem(ModBlocks.GLASS_POLONIUM.get(),
                models().cubeAll(
                        ModBlocks.GLASS_POLONIUM.getId().getPath(),
                        modLoc("block/glass_polonium")
                )
        );
        simpleBlockWithItem(ModBlocks.GLASS_QUARTZ.get(),
                models().cubeAll(
                        ModBlocks.GLASS_QUARTZ.getId().getPath(),
                        modLoc("block/glass_quartz")
                )
        );
        simpleBlockWithItem(ModBlocks.GLASS_TRINITITE.get(),
                models().cubeAll(
                        ModBlocks.GLASS_TRINITITE.getId().getPath(),
                        modLoc("block/glass_trinitite")
                )
        );
        simpleBlockWithItem(ModBlocks.GLASS_URANIUM.get(),
                models().cubeAll(
                        ModBlocks.GLASS_URANIUM.getId().getPath(),
                        modLoc("block/glass_uranium")
                )
        );
        simpleBlockWithItem(ModBlocks.GLYPHID_BASE.get(),
                models().cubeAll(
                        ModBlocks.GLYPHID_BASE.getId().getPath(),
                        modLoc("block/glyphid_base")
                )
        );
        simpleBlockWithItem(ModBlocks.GRAVEL_DIAMOND.get(),
                models().cubeAll(
                        ModBlocks.GRAVEL_DIAMOND.getId().getPath(),
                        modLoc("block/gravel_diamond")
                )
        );
        simpleBlockWithItem(ModBlocks.GRAVEL_OBSIDIAN.get(),
                models().cubeAll(
                        ModBlocks.GRAVEL_OBSIDIAN.getId().getPath(),
                        modLoc("block/gravel_obsidian")
                )
        );
        simpleBlockWithItem(ModBlocks.HADRON_COIL_ALLOY.get(),
                models().cubeAll(
                        ModBlocks.HADRON_COIL_ALLOY.getId().getPath(),
                        modLoc("block/hadron_coil_alloy")
                )
        );
        simpleBlockWithItem(ModBlocks.HADRON_COIL_CHLOROPHYTE.get(),
                models().cubeAll(
                        ModBlocks.HADRON_COIL_CHLOROPHYTE.getId().getPath(),
                        modLoc("block/hadron_coil_chlorophyte")
                )
        );
        simpleBlockWithItem(ModBlocks.HADRON_COIL_GOLD.get(),
                models().cubeAll(
                        ModBlocks.HADRON_COIL_GOLD.getId().getPath(),
                        modLoc("block/hadron_coil_gold")
                )
        );
        simpleBlockWithItem(ModBlocks.HADRON_COIL_MAGTUNG.get(),
                models().cubeAll(
                        ModBlocks.HADRON_COIL_MAGTUNG.getId().getPath(),
                        modLoc("block/hadron_coil_magtung")
                )
        );
        simpleBlockWithItem(ModBlocks.HADRON_COIL_MESE.get(),
                models().cubeAll(
                        ModBlocks.HADRON_COIL_MESE.getId().getPath(),
                        modLoc("block/hadron_coil_mese")
                )
        );
        simpleBlockWithItem(ModBlocks.HADRON_COIL_NEODYMIUM.get(),
                models().cubeAll(
                        ModBlocks.HADRON_COIL_NEODYMIUM.getId().getPath(),
                        modLoc("block/hadron_coil_neodymium")
                )
        );
        simpleBlockWithItem(ModBlocks.HADRON_COIL_SCHRABIDATE.get(),
                models().cubeAll(
                        ModBlocks.HADRON_COIL_SCHRABIDATE.getId().getPath(),
                        modLoc("block/hadron_coil_schrabidate")
                )
        );
        simpleBlockWithItem(ModBlocks.HADRON_COIL_SCHRABIDIUM.get(),
                models().cubeAll(
                        ModBlocks.HADRON_COIL_SCHRABIDIUM.getId().getPath(),
                        modLoc("block/hadron_coil_schrabidium")
                )
        );
        simpleBlockWithItem(ModBlocks.HADRON_COIL_STARMETAL.get(),
                models().cubeAll(
                        ModBlocks.HADRON_COIL_STARMETAL.getId().getPath(),
                        modLoc("block/hadron_coil_starmetal")
                )
        );
        simpleBlockWithItem(ModBlocks.HEV_BATTERY.get(),
                models().cubeAll(
                        ModBlocks.HEV_BATTERY.getId().getPath(),
                        modLoc("block/hev_battery")
                )
        );
        simpleBlockWithItem(ModBlocks.ICF_COMPONENT.get(),
                models().cubeAll(
                        ModBlocks.ICF_COMPONENT.getId().getPath(),
                        modLoc("block/icf_component")
                )
        );
        simpleBlockWithItem(ModBlocks.ICF_COMPONENT_STRUCTURE.get(),
                models().cubeAll(
                        ModBlocks.ICF_COMPONENT_STRUCTURE.getId().getPath(),
                        modLoc("block/icf_component_structure")
                )
        );
        simpleBlockWithItem(ModBlocks.ICF_COMPONENT_STRUCTURE_BOLTED.get(),
                models().cubeAll(
                        ModBlocks.ICF_COMPONENT_STRUCTURE_BOLTED.getId().getPath(),
                        modLoc("block/icf_component_structure_bolted")
                )
        );
        simpleBlockWithItem(ModBlocks.ICF_COMPONENT_VESSEL.get(),
                models().cubeAll(
                        ModBlocks.ICF_COMPONENT_VESSEL.getId().getPath(),
                        modLoc("block/icf_component_vessel")
                )
        );
        simpleBlockWithItem(ModBlocks.ICF_COMPONENT_VESSEL_WELDED.get(),
                models().cubeAll(
                        ModBlocks.ICF_COMPONENT_VESSEL_WELDED.getId().getPath(),
                        modLoc("block/icf_component_vessel_welded")
                )
        );
        simpleBlockWithItem(ModBlocks.ICF_CONTROLLER.get(),
                models().cubeAll(
                        ModBlocks.ICF_CONTROLLER.getId().getPath(),
                        modLoc("block/icf_controller")
                )
        );
        simpleBlockWithItem(ModBlocks.ITER.get(),
                models().cubeAll(
                        ModBlocks.ITER.getId().getPath(),
                        modLoc("block/iter")
                )
        );
        simpleBlockWithItem(ModBlocks.LADDER_ALUMINIUM.get(),
                models().cubeAll(
                        ModBlocks.LADDER_ALUMINIUM.getId().getPath(),
                        modLoc("block/ladder_aluminium")
                )
        );
        simpleBlockWithItem(ModBlocks.LADDER_COBALT.get(),
                models().cubeAll(
                        ModBlocks.LADDER_COBALT.getId().getPath(),
                        modLoc("block/ladder_cobalt")
                )
        );
        simpleBlockWithItem(ModBlocks.LADDER_COPPER.get(),
                models().cubeAll(
                        ModBlocks.LADDER_COPPER.getId().getPath(),
                        modLoc("block/ladder_copper")
                )
        );
        simpleBlockWithItem(ModBlocks.LADDER_GOLD.get(),
                models().cubeAll(
                        ModBlocks.LADDER_GOLD.getId().getPath(),
                        modLoc("block/ladder_gold")
                )
        );
        simpleBlockWithItem(ModBlocks.LADDER_IRON.get(),
                models().cubeAll(
                        ModBlocks.LADDER_IRON.getId().getPath(),
                        modLoc("block/ladder_iron")
                )
        );
        simpleBlockWithItem(ModBlocks.LADDER_LEAD.get(),
                models().cubeAll(
                        ModBlocks.LADDER_LEAD.getId().getPath(),
                        modLoc("block/ladder_lead")
                )
        );
        simpleBlockWithItem(ModBlocks.LADDER_STEEL.get(),
                models().cubeAll(
                        ModBlocks.LADDER_STEEL.getId().getPath(),
                        modLoc("block/ladder_steel")
                )
        );
        simpleBlockWithItem(ModBlocks.LADDER_STURDY.get(),
                models().cubeAll(
                        ModBlocks.LADDER_STURDY.getId().getPath(),
                        modLoc("block/ladder_sturdy")
                )
        );
        simpleBlockWithItem(ModBlocks.LADDER_TITANIUM.get(),
                models().cubeAll(
                        ModBlocks.LADDER_TITANIUM.getId().getPath(),
                        modLoc("block/ladder_titanium")
                )
        );
        simpleBlockWithItem(ModBlocks.LADDER_TUNGSTEN.get(),
                models().cubeAll(
                        ModBlocks.LADDER_TUNGSTEN.getId().getPath(),
                        modLoc("block/ladder_tungsten")
                )
        );
        simpleBlockWithItem(ModBlocks.LAMP_DEMON.get(),
                models().cubeAll(
                        ModBlocks.LAMP_DEMON.getId().getPath(),
                        modLoc("block/lamp_demon")
                )
        );
        simpleBlockWithItem(ModBlocks.LAMP_TRITIUM_BLUE_OFF.get(),
                models().cubeAll(
                        ModBlocks.LAMP_TRITIUM_BLUE_OFF.getId().getPath(),
                        modLoc("block/lamp_tritium_blue_off")
                )
        );
        simpleBlockWithItem(ModBlocks.LAMP_TRITIUM_BLUE_ON.get(),
                models().cubeAll(
                        ModBlocks.LAMP_TRITIUM_BLUE_ON.getId().getPath(),
                        modLoc("block/lamp_tritium_blue_on")
                )
        );
        simpleBlockWithItem(ModBlocks.LAMP_TRITIUM_GREEN_OFF.get(),
                models().cubeAll(
                        ModBlocks.LAMP_TRITIUM_GREEN_OFF.getId().getPath(),
                        modLoc("block/lamp_tritium_green_off")
                )
        );
        simpleBlockWithItem(ModBlocks.LAMP_TRITIUM_GREEN_ON.get(),
                models().cubeAll(
                        ModBlocks.LAMP_TRITIUM_GREEN_ON.getId().getPath(),
                        modLoc("block/lamp_tritium_green_on")
                )
        );
        simpleBlockWithItem(ModBlocks.LIGHTSTONE_BRICKS.get(),
                models().cubeAll(
                        ModBlocks.LIGHTSTONE_BRICKS.getId().getPath(),
                        modLoc("block/lightstone_bricks")
                )
        );
        simpleBlockWithItem(ModBlocks.LIGHTSTONE_BRICKS_CHISELED.get(),
                models().cubeAll(
                        ModBlocks.LIGHTSTONE_BRICKS_CHISELED.getId().getPath(),
                        modLoc("block/lightstone_bricks_chiseled")
                )
        );
        simpleBlockWithItem(ModBlocks.LIGHTSTONE_CHISELED.get(),
                models().cubeAll(
                        ModBlocks.LIGHTSTONE_CHISELED.getId().getPath(),
                        modLoc("block/lightstone_chiseled")
                )
        );
        simpleBlockWithItem(ModBlocks.LIGHTSTONE_TILE.get(),
                models().cubeAll(
                        ModBlocks.LIGHTSTONE_TILE.getId().getPath(),
                        modLoc("block/lightstone_tile")
                )
        );
        simpleBlockWithItem(ModBlocks.LIGHTSTONE_UNREFINED.get(),
                models().cubeAll(
                        ModBlocks.LIGHTSTONE_UNREFINED.getId().getPath(),
                        modLoc("block/lightstone_unrefined")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_AUTOCRAFTER.get(),
                models().cubeBottomTop(
                        ModBlocks.MACHINE_AUTOCRAFTER.getId().getPath(),
                        modLoc("block/machine_autocrafter_side"),
                        modLoc("block/machine_autocrafter_bottom"),
                        modLoc("block/machine_autocrafter_top")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_BOILER.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_BOILER.getId().getPath(),
                        modLoc("block/machine_boiler_base")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_CENTRIFUGE.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_CENTRIFUGE.getId().getPath(),
                        modLoc("block/machine_centrifuge")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_CONTROLLER.get(),
                models().cubeBottomTop(
                        ModBlocks.MACHINE_CONTROLLER.getId().getPath(),
                        modLoc("block/machine_controller_side"),
                        modLoc("block/machine_controller_side"),
                        modLoc("block/machine_controller_top")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_CONVERTER_HE_RF.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_CONVERTER_HE_RF.getId().getPath(),
                        modLoc("block/machine_converter_he_rf")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_CONVERTER_RF_HE.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_CONVERTER_RF_HE.getId().getPath(),
                        modLoc("block/machine_converter_rf_he")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_CRYSTALLIZER.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_CRYSTALLIZER.getId().getPath(),
                        modLoc("block/machine_crystallizer")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_DETECTOR.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_DETECTOR.getId().getPath(),
                        modLoc("block/machine_detector")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_EPRESS.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_EPRESS.getId().getPath(),
                        modLoc("block/machine_epress")
                )
        );
        // MachineBatteryBlock (FACING-Blockstate) statt simpleBlockWithItem - siehe orientableBlockWithItem-Aufrufe fuer MACHINE_BATTERY etc.
        simpleBlockWithItem(ModBlocks.MACHINE_FLUIDTANK.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_FLUIDTANK.getId().getPath(),
                        modLoc("block/machine_fluidtank")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_FORCEFIELD.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_FORCEFIELD.getId().getPath(),
                        modLoc("block/machine_forcefield")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_FUNNEL.get(),
                models().cubeBottomTop(
                        ModBlocks.MACHINE_FUNNEL.getId().getPath(),
                        modLoc("block/machine_funnel_side"),
                        modLoc("block/machine_funnel_bottom"),
                        modLoc("block/machine_funnel_top")
                )
        );
        simpleBlockWithItem(ModBlocks.PUREX.get(),
                models().cubeAll(
                        ModBlocks.PUREX.getId().getPath(),
                        modLoc("block/machine/purex")
                )
        );
        simpleBlockWithItem(ModBlocks.INDUSTRIAL_GENERATOR.get(),
                models().cubeAll(
                        ModBlocks.INDUSTRIAL_GENERATOR.getId().getPath(),
                        modLoc("block/block_steel_machine")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_GASCENT.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_GASCENT.getId().getPath(),
                        modLoc("block/machine_gascent")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_ICF_PRESS.get(),
                models().cubeBottomTop(
                        ModBlocks.MACHINE_ICF_PRESS.getId().getPath(),
                        modLoc("block/machine_icf_press_side"),
                        modLoc("block/machine_icf_press_side"),
                        modLoc("block/machine_icf_press_top")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_KEYFORGE.get(),
                models().cubeBottomTop(
                        ModBlocks.MACHINE_KEYFORGE.getId().getPath(),
                        modLoc("block/machine_keyforge_side"),
                        modLoc("block/machine_keyforge_bottom"),
                        modLoc("block/machine_keyforge_top")
                )
        );
        simpleMachineBlock(ModBlocks.MACHINE_LARGE_TURBINE);
        simpleBlockWithItem(ModBlocks.MACHINE_MICROWAVE.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_MICROWAVE.getId().getPath(),
                        modLoc("block/machine_microwave")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_MINING_LASER.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_MINING_LASER.getId().getPath(),
                        modLoc("block/machine_mining_laser")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_MISSILE_ASSEMBLY.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_MISSILE_ASSEMBLY.getId().getPath(),
                        modLoc("block/machine_missile_assembly")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_PRESS.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_PRESS.getId().getPath(),
                        modLoc("block/machine_press")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_PUF6_TANK.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_PUF6_TANK.getId().getPath(),
                        modLoc("block/machine_puf6_tank")
                )
        );
        plainFacingBlock(ModBlocks.MACHINE_FAN.get(),
                models().cubeAll(ModBlocks.MACHINE_FAN.getId().getPath(), modLoc("block/machine/fan")));
        simpleBlockWithItem(ModBlocks.MACHINE_DRAIN.get(),
                models().cubeAll(ModBlocks.MACHINE_DRAIN.getId().getPath(), modLoc("block/concrete")));
        orientableBlockWithItem(
                ModBlocks.MACHINE_DIFURNACE_RTG,
                modLoc("block/difurnace_side_tall"),
                modLoc("block/difurnace_front_off_tall"),
                modLoc("block/difurnace_top_off_alt")
        );
        simpleBlockWithItem(ModBlocks.MACHINE_TELEPORTER.get(),
                models().cubeBottomTop(
                        ModBlocks.MACHINE_TELEPORTER.getId().getPath(),
                        modLoc("block/teleporter_side"),
                        modLoc("block/teleporter_bottom"),
                        modLoc("block/teleporter_top")
                )
        );
        simpleBlockWithItem(ModBlocks.TELEANCHOR.get(),
                models().cubeBottomTop(
                        ModBlocks.TELEANCHOR.getId().getPath(),
                        modLoc("block/tele_anchor_side"),
                        modLoc("block/tele_anchor_side"),
                        modLoc("block/tele_anchor_top")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_PRECASS.get(),
                models().cubeAll(ModBlocks.MACHINE_PRECASS.getId().getPath(), modLoc("block/machine/precass")));
        simpleBlockWithItem(ModBlocks.MACHINE_TRANSFORMER.get(),
                models().cubeBottomTop(
                        ModBlocks.MACHINE_TRANSFORMER.getId().getPath(),
                        modLoc("block/machine_transformer_iron"),
                        modLoc("block/machine_transformer_top_iron"),
                        modLoc("block/machine_transformer_top_iron")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_RTG.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_RTG.getId().getPath(),
                        modLoc("block/machine/rtg")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_WASTE_DRUM.get(),
                models().cubeBottomTop(
                        ModBlocks.MACHINE_WASTE_DRUM.getId().getPath(),
                        modLoc("block/machine/waste_drum_side"),
                        modLoc("block/machine/waste_drum_top"),
                        modLoc("block/machine/waste_drum_top")
                )
        );
        customMachineBlock(ModBlocks.MACHINE_RADGEN);
        craneDirectionalBlock(ModBlocks.CRANE_INSERTER, "crane");
        craneDirectionalBlock(ModBlocks.CRANE_EXTRACTOR, "crane");
        craneDirectionalBlock(ModBlocks.CRANE_GRABBER, "crane");
        craneDirectionalBlock(ModBlocks.CRANE_BOXER, "crane");
        craneDirectionalBlock(ModBlocks.CRANE_UNBOXER, "crane");
        simpleBlockWithItem(ModBlocks.MACHINE_REACTOR.get(),
                models().cubeBottomTop(
                        ModBlocks.MACHINE_REACTOR.getId().getPath(),
                        modLoc("block/machine_reactor"),
                        modLoc("block/machine_reactor"),
                        modLoc("block/machine_reactor_top")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_REACTOR_SMALL.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_REACTOR_SMALL.getId().getPath(),
                        modLoc("block/machine_reactor_small")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_REFINERY.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_REFINERY.getId().getPath(),
                        modLoc("block/machine_refinery")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_SATLINKER.get(),
                models().cubeBottomTop(
                        ModBlocks.MACHINE_SATLINKER.getId().getPath(),
                        modLoc("block/machine_satlinker_side"),
                        modLoc("block/machine_satlinker_side"),
                        modLoc("block/machine_satlinker_top")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_SOLAR_BOILER.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_SOLAR_BOILER.getId().getPath(),
                        modLoc("block/machine_solar_boiler")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_STORAGE_DRUM.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_STORAGE_DRUM.getId().getPath(),
                        modLoc("block/machine_storage_drum")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_UF6_TANK.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_UF6_TANK.getId().getPath(),
                        modLoc("block/machine_uf6_tank")
                )
        );
        simpleBlockWithItem(ModBlocks.MASS_STORAGE.get(),
                models().cubeBottomTop(
                        ModBlocks.MASS_STORAGE.getId().getPath(),
                        modLoc("block/mass_storage_side"),
                        modLoc("block/mass_storage_side"),
                        modLoc("block/mass_storage_top")
                )
        );
        simpleBlockWithItem(ModBlocks.METEOR_SPAWNER.get(),
                models().cubeBottomTop(
                        ModBlocks.METEOR_SPAWNER.getId().getPath(),
                        modLoc("block/meteor_spawner_side"),
                        modLoc("block/meteor_spawner_side"),
                        modLoc("block/meteor_spawner_top")
                )
        );
        simpleBlockWithItem(ModBlocks.MINE_HE.get(),
                models().cubeAll(
                        ModBlocks.MINE_HE.getId().getPath(),
                        modLoc("block/mine_he")
                )
        );
        simpleBlockWithItem(ModBlocks.MINE_NAVAL.get(),
                models().cubeAll(
                        ModBlocks.MINE_NAVAL.getId().getPath(),
                        modLoc("block/mine_naval")
                )
        );
        simpleBlockWithItem(ModBlocks.MINE_SHRAP.get(),
                models().cubeAll(
                        ModBlocks.MINE_SHRAP.getId().getPath(),
                        modLoc("block/mine_shrap")
                )
        );
        simpleBlockWithItem(ModBlocks.MOON_TURF.get(),
                models().cubeAll(
                        ModBlocks.MOON_TURF.getId().getPath(),
                        modLoc("block/moon_turf")
                )
        );
        simpleBlockWithItem(ModBlocks.MUSH.get(),
                models().cubeAll(
                        ModBlocks.MUSH.getId().getPath(),
                        modLoc("block/mush")
                )
        );
        simpleBlockWithItem(ModBlocks.NUKE_FSTBMB.get(),
                models().cubeAll(
                        ModBlocks.NUKE_FSTBMB.getId().getPath(),
                        modLoc("block/nuke_fstbmb")
                )
        );
        simpleBlockWithItem(ModBlocks.NUKE_N2.get(),
                models().cubeAll(
                        ModBlocks.NUKE_N2.getId().getPath(),
                        modLoc("block/nuke_n2")
                )
        );
        simpleBlockWithItem(ModBlocks.NUKE_SOLINIUM.get(),
                models().cubeAll(
                        ModBlocks.NUKE_SOLINIUM.getId().getPath(),
                        modLoc("block/nuke_solinium")
                )
        );
        simpleBlockWithItem(ModBlocks.OIL_SPILL.get(),
                models().cubeAll(
                        ModBlocks.OIL_SPILL.getId().getPath(),
                        modLoc("block/oil_spill")
                )
        );
        simpleBlockWithItem(ModBlocks.PEDESTAL.get(),
                models().cubeBottomTop(
                        ModBlocks.PEDESTAL.getId().getPath(),
                        modLoc("block/pedestal_side"),
                        modLoc("block/pedestal_side"),
                        modLoc("block/pedestal_top")
                )
        );
        simpleBlockWithItem(ModBlocks.PINK_LOG.get(),
                models().cubeAll(
                        ModBlocks.PINK_LOG.getId().getPath(),
                        modLoc("block/pink_log")
                )
        );
        simpleBlockWithItem(ModBlocks.PINK_PLANKS.get(),
                models().cubeAll(
                        ModBlocks.PINK_PLANKS.getId().getPath(),
                        modLoc("block/pink_planks")
                )
        );
        simpleBlockWithItem(ModBlocks.PLANT_FLOWER_CD0.get(),
                models().cubeAll(
                        ModBlocks.PLANT_FLOWER_CD0.getId().getPath(),
                        modLoc("block/plant_flower_cd0")
                )
        );
        simpleBlockWithItem(ModBlocks.PLANT_FLOWER_CD1.get(),
                models().cubeAll(
                        ModBlocks.PLANT_FLOWER_CD1.getId().getPath(),
                        modLoc("block/plant_flower_cd1")
                )
        );
        simpleBlockWithItem(ModBlocks.PLANT_FLOWER_FOXGLOVE.get(),
                models().cubeAll(
                        ModBlocks.PLANT_FLOWER_FOXGLOVE.getId().getPath(),
                        modLoc("block/plant_flower_foxglove")
                )
        );
        simpleBlockWithItem(ModBlocks.PLANT_FLOWER_NIGHTSHADE.get(),
                models().cubeAll(
                        ModBlocks.PLANT_FLOWER_NIGHTSHADE.getId().getPath(),
                        modLoc("block/plant_flower_nightshade")
                )
        );
        simpleBlockWithItem(ModBlocks.PLANT_FLOWER_TOBACCO.get(),
                models().cubeAll(
                        ModBlocks.PLANT_FLOWER_TOBACCO.getId().getPath(),
                        modLoc("block/plant_flower_tobacco")
                )
        );
        simpleBlockWithItem(ModBlocks.PLANT_FLOWER_WEED.get(),
                models().cubeAll(
                        ModBlocks.PLANT_FLOWER_WEED.getId().getPath(),
                        modLoc("block/plant_flower_weed")
                )
        );
        simpleBlockWithItem(ModBlocks.PLASMA_HEATER.get(),
                models().cubeAll(
                        ModBlocks.PLASMA_HEATER.getId().getPath(),
                        modLoc("block/plasma_heater")
                )
        );
        simpleBlockWithItem(ModBlocks.PNEUMATIC_TUBE.get(),
                models().cubeAll(
                        ModBlocks.PNEUMATIC_TUBE.getId().getPath(),
                        modLoc("block/pneumatic_tube")
                )
        );
        simpleBlockWithItem(ModBlocks.PNEUMATIC_TUBE_PAINTABLE.get(),
                models().cubeAll(
                        ModBlocks.PNEUMATIC_TUBE_PAINTABLE.getId().getPath(),
                        modLoc("block/pneumatic_tube_paintable")
                )
        );
        simpleBlockWithItem(ModBlocks.PRESS_PREHEATER.get(),
                models().cubeAll(
                        ModBlocks.PRESS_PREHEATER.getId().getPath(),
                        modLoc("block/press_preheater")
                )
        );
        simpleBlockWithItem(ModBlocks.PWR_BLOCK.get(),
                models().cubeAll(
                        ModBlocks.PWR_BLOCK.getId().getPath(),
                        modLoc("block/pwr_block")
                )
        );
        simpleBlockWithItem(ModBlocks.PWR_CASING.get(),
                models().cubeAll(
                        ModBlocks.PWR_CASING.getId().getPath(),
                        modLoc("block/pwr_casing")
                )
        );
        simpleBlockWithItem(ModBlocks.PWR_CHANNEL.get(),
                models().cubeBottomTop(
                        ModBlocks.PWR_CHANNEL.getId().getPath(),
                        modLoc("block/pwr_channel_side"),
                        modLoc("block/pwr_channel_side"),
                        modLoc("block/pwr_channel_top")
                )
        );
        simpleBlockWithItem(ModBlocks.PWR_CONTROL.get(),
                models().cubeBottomTop(
                        ModBlocks.PWR_CONTROL.getId().getPath(),
                        modLoc("block/pwr_control_side"),
                        modLoc("block/pwr_control_side"),
                        modLoc("block/pwr_control_top")
                )
        );
        orientableBlockWithItem(ModBlocks.PWR_CONTROLLER,
                modLoc("block/pwr_casing"),
                modLoc("block/pwr_controller"),
                modLoc("block/pwr_casing")
        );
        simpleBlockWithItem(ModBlocks.PWR_FUEL.get(),
                models().cubeBottomTop(
                        ModBlocks.PWR_FUEL.getId().getPath(),
                        modLoc("block/pwr_fuel_side"),
                        modLoc("block/pwr_fuel_side"),
                        modLoc("block/pwr_fuel_top")
                )
        );
        simpleBlockWithItem(ModBlocks.PWR_HEATEX.get(),
                models().cubeAll(
                        ModBlocks.PWR_HEATEX.getId().getPath(),
                        modLoc("block/pwr_heatex")
                )
        );
        simpleBlockWithItem(ModBlocks.PWR_HEATSINK.get(),
                models().cubeAll(
                        ModBlocks.PWR_HEATSINK.getId().getPath(),
                        modLoc("block/pwr_heatsink")
                )
        );
        simpleBlockWithItem(ModBlocks.PWR_NEUTRON_SOURCE.get(),
                models().cubeAll(
                        ModBlocks.PWR_NEUTRON_SOURCE.getId().getPath(),
                        modLoc("block/pwr_neutron_source")
                )
        );
        simpleBlockWithItem(ModBlocks.PWR_PORT.get(),
                models().cubeAll(
                        ModBlocks.PWR_PORT.getId().getPath(),
                        modLoc("block/pwr_port")
                )
        );
        simpleBlockWithItem(ModBlocks.PWR_REFLECTOR.get(),
                models().cubeAll(
                        ModBlocks.PWR_REFLECTOR.getId().getPath(),
                        modLoc("block/pwr_reflector")
                )
        );
        simpleBlockWithItem(ModBlocks.RADIO_TELEX.get(),
                models().cubeAll(
                        ModBlocks.RADIO_TELEX.getId().getPath(),
                        modLoc("block/radio_telex")
                )
        );
        simpleBlockWithItem(ModBlocks.RADIOBOX.get(),
                models().cubeAll(
                        ModBlocks.RADIOBOX.getId().getPath(),
                        modLoc("block/radiobox")
                )
        );
        simpleBlockWithItem(ModBlocks.RADIOREC.get(),
                models().cubeAll(
                        ModBlocks.RADIOREC.getId().getPath(),
                        modLoc("block/radiorec")
                )
        );
        simpleBlockWithItem(ModBlocks.RADIO_AUTOCAL.get(),
                models().cubeAll(
                        ModBlocks.RADIO_AUTOCAL.getId().getPath(),
                        modLoc("block/radio_autocal")
                )
        );
        simpleBlockWithItem(ModBlocks.RAIL_BOOSTER.get(),
                models().cubeAll(
                        ModBlocks.RAIL_BOOSTER.getId().getPath(),
                        modLoc("block/rail_booster")
                )
        );
        simpleBlockWithItem(ModBlocks.RAIL_HIGHSPEED.get(),
                models().cubeAll(
                        ModBlocks.RAIL_HIGHSPEED.getId().getPath(),
                        modLoc("block/rail_highspeed")
                )
        );
        simpleBlockWithItem(ModBlocks.RAIL_NARROW.get(),
                models().cubeAll(
                        ModBlocks.RAIL_NARROW.getId().getPath(),
                        modLoc("block/rail_narrow")
                )
        );
        simpleBlockWithItem(ModBlocks.RAIL_WOOD.get(),
                models().cubeAll(
                        ModBlocks.RAIL_WOOD.getId().getPath(),
                        modLoc("block/rail_wood")
                )
        );
        simpleBlockWithItem(ModBlocks.RED_CABLE.get(),
                models().cubeAll(
                        ModBlocks.RED_CABLE.getId().getPath(),
                        modLoc("block/red_cable")
                )
        );
        simpleBlockWithItem(ModBlocks.RED_CABLE_CLASSIC.get(),
                models().cubeAll(
                        ModBlocks.RED_CABLE_CLASSIC.getId().getPath(),
                        modLoc("block/red_cable_classic")
                )
        );
        simpleBlockWithItem(ModBlocks.RED_CONNECTOR.get(),
                models().cubeAll(
                        ModBlocks.RED_CONNECTOR.getId().getPath(),
                        modLoc("block/red_connector")
                )
        );
        simpleBlockWithItem(ModBlocks.RED_CONNECTOR_SUPER.get(),
                models().cubeAll(
                        ModBlocks.RED_CONNECTOR_SUPER.getId().getPath(),
                        modLoc("block/red_connector")
                )
        );
        simpleBlockWithItem(ModBlocks.RED_CABLE_BOX.get(),
                models().cubeAll(
                        ModBlocks.RED_CABLE_BOX.getId().getPath(),
                        modLoc("block/fluid_duct_box")
                )
        );
        simpleBlockWithItem(ModBlocks.RED_PYLON.get(),
                models().cubeAll(
                        ModBlocks.RED_PYLON.getId().getPath(),
                        modLoc("block/red_pylon")
                )
        );
        simpleBlockWithItem(ModBlocks.RED_PYLON_MEDIUM_WOOD.get(),
                models().cubeAll(
                        ModBlocks.RED_PYLON_MEDIUM_WOOD.getId().getPath(),
                        modLoc("block/red_pylon")
                )
        );
        simpleBlockWithItem(ModBlocks.RED_PYLON_MEDIUM_STEEL.get(),
                models().cubeAll(
                        ModBlocks.RED_PYLON_MEDIUM_STEEL.getId().getPath(),
                        modLoc("block/red_pylon")
                )
        );
        simpleBlockWithItem(ModBlocks.RED_PYLON_LARGE.get(),
                models().cubeAll(
                        ModBlocks.RED_PYLON_LARGE.getId().getPath(),
                        modLoc("block/red_pylon_large")
                )
        );
        simpleBlockWithItem(ModBlocks.RED_WIRE_COATED.get(),
                models().cubeAll(
                        ModBlocks.RED_WIRE_COATED.getId().getPath(),
                        modLoc("block/red_wire_coated")
                )
        );
        simpleBlockWithItem(ModBlocks.REINFORCED_BRICK.get(),
                models().cubeAll(
                        ModBlocks.REINFORCED_BRICK.getId().getPath(),
                        modLoc("block/reinforced_brick")
                )
        );
        simpleBlockWithItem(ModBlocks.REINFORCED_DUCRETE.get(),
                models().cubeAll(
                        ModBlocks.REINFORCED_DUCRETE.getId().getPath(),
                        modLoc("block/reinforced_ducrete")
                )
        );
        simpleBlockWithItem(ModBlocks.REINFORCED_GLASS_PANE.get(),
                models().cubeAll(
                        ModBlocks.REINFORCED_GLASS_PANE.getId().getPath(),
                        modLoc("block/reinforced_glass_pane")
                )
        );
        simpleBlockWithItem(ModBlocks.REINFORCED_LAMINATE.get(),
                models().cubeAll(
                        ModBlocks.REINFORCED_LAMINATE.getId().getPath(),
                        modLoc("block/reinforced_laminate")
                )
        );
        simpleBlockWithItem(ModBlocks.REINFORCED_LAMINATE_PANE.get(),
                models().cubeAll(
                        ModBlocks.REINFORCED_LAMINATE_PANE.getId().getPath(),
                        modLoc("block/reinforced_laminate_pane")
                )
        );
        simpleBlockWithItem(ModBlocks.REINFORCED_LAMP_OFF.get(),
                models().cubeAll(
                        ModBlocks.REINFORCED_LAMP_OFF.getId().getPath(),
                        modLoc("block/reinforced_lamp_off")
                )
        );
        simpleBlockWithItem(ModBlocks.REINFORCED_LAMP_ON.get(),
                models().cubeAll(
                        ModBlocks.REINFORCED_LAMP_ON.getId().getPath(),
                        modLoc("block/reinforced_lamp_on")
                )
        );
        simpleBlockWithItem(ModBlocks.REINFORCED_LIGHT.get(),
                models().cubeAll(
                        ModBlocks.REINFORCED_LIGHT.getId().getPath(),
                        modLoc("block/reinforced_light")
                )
        );
        simpleBlockWithItem(ModBlocks.REINFORCED_SAND.get(),
                models().cubeAll(
                        ModBlocks.REINFORCED_SAND.getId().getPath(),
                        modLoc("block/reinforced_sand")
                )
        );
        simpleBlockWithItem(ModBlocks.SAFE.get(),
                models().cubeAll(
                        ModBlocks.SAFE.getId().getPath(),
                        modLoc("block/safe_front")
                )
        );
        simpleBlockWithItem(ModBlocks.SAND_BORON.get(),
                models().cubeAll(
                        ModBlocks.SAND_BORON.getId().getPath(),
                        modLoc("block/sand_boron")
                )
        );
        simpleBlockWithItem(ModBlocks.SAND_DIRTY.get(),
                models().cubeAll(
                        ModBlocks.SAND_DIRTY.getId().getPath(),
                        modLoc("block/sand_dirty")
                )
        );
        simpleBlockWithItem(ModBlocks.SAND_DIRTY_RED.get(),
                models().cubeAll(
                        ModBlocks.SAND_DIRTY_RED.getId().getPath(),
                        modLoc("block/sand_dirty_red")
                )
        );
        simpleBlockWithItem(ModBlocks.SAND_LEAD.get(),
                models().cubeAll(
                        ModBlocks.SAND_LEAD.getId().getPath(),
                        modLoc("block/sand_lead")
                )
        );
        simpleBlockWithItem(ModBlocks.SAND_POLONIUM.get(),
                models().cubeAll(
                        ModBlocks.SAND_POLONIUM.getId().getPath(),
                        modLoc("block/sand_polonium")
                )
        );
        simpleBlockWithItem(ModBlocks.SAND_QUARTZ.get(),
                models().cubeAll(
                        ModBlocks.SAND_QUARTZ.getId().getPath(),
                        modLoc("block/sand_quartz")
                )
        );
        simpleBlockWithItem(ModBlocks.SAND_URANIUM.get(),
                models().cubeAll(
                        ModBlocks.SAND_URANIUM.getId().getPath(),
                        modLoc("block/sand_uranium")
                )
        );
        simpleBlockWithItem(ModBlocks.SANDBAGS.get(),
                models().cubeAll(
                        ModBlocks.SANDBAGS.getId().getPath(),
                        modLoc("block/sandbags")
                )
        );
        simpleBlockWithItem(ModBlocks.SAT_DOCK.get(),
                models().cubeAll(
                        ModBlocks.SAT_DOCK.getId().getPath(),
                        modLoc("block/sat_dock")
                )
        );
        simpleBlockWithItem(ModBlocks.SAT_FOEQ.get(),
                models().cubeAll(
                        ModBlocks.SAT_FOEQ.getId().getPath(),
                        modLoc("block/sat_foeq")
                )
        );
        simpleBlockWithItem(ModBlocks.SAT_SCANNER.get(),
                models().cubeAll(
                        ModBlocks.SAT_SCANNER.getId().getPath(),
                        modLoc("block/sat_scanner")
                )
        );
        simpleBlockWithItem(ModBlocks.SEAL_CONTROLLER.get(),
                models().cubeAll(
                        ModBlocks.SEAL_CONTROLLER.getId().getPath(),
                        modLoc("block/seal_controller")
                )
        );
        simpleBlockWithItem(ModBlocks.SEAL_FRAME.get(),
                models().cubeAll(
                        ModBlocks.SEAL_FRAME.getId().getPath(),
                        modLoc("block/seal_frame")
                )
        );
        simpleBlockWithItem(ModBlocks.SEAL_HATCH.get(),
                models().cubeAll(
                        ModBlocks.SEAL_HATCH.getId().getPath(),
                        modLoc("block/seal_hatch_3")
                )
        );
        simpleBlockWithItem(ModBlocks.SEMTEX.get(),
                models().cubeBottomTop(
                        ModBlocks.SEMTEX.getId().getPath(),
                        modLoc("block/semtex_side"),
                        modLoc("block/semtex_bottom"),
                        modLoc("block/semtex_top")
                )
        );
        simpleBlockWithItem(ModBlocks.SOYUZ_CAPSULE.get(),
                models().cubeAll(
                        ModBlocks.SOYUZ_CAPSULE.getId().getPath(),
                        modLoc("block/soyuz_capsule")
                )
        );
        customObjBlock(ModBlocks.SOYUZ_LAUNCHER);
        customObjBlock(ModBlocks.DECO_SOYUZ_ROCKET);
        simpleBlockWithItem(ModBlocks.SPIKES.get(),
                models().cubeAll(
                        ModBlocks.SPIKES.getId().getPath(),
                        modLoc("block/spikes")
                )
        );
        simpleBlockWithItem(ModBlocks.STALACTITE_ASBESTOS.get(),
                models().cubeAll(
                        ModBlocks.STALACTITE_ASBESTOS.getId().getPath(),
                        modLoc("block/stalactite_asbestos")
                )
        );
        simpleBlockWithItem(ModBlocks.STALACTITE_SULFUR.get(),
                models().cubeAll(
                        ModBlocks.STALACTITE_SULFUR.getId().getPath(),
                        modLoc("block/stalactite_sulfur")
                )
        );
        simpleBlockWithItem(ModBlocks.STALAGMITE_ASBESTOS.get(),
                models().cubeAll(
                        ModBlocks.STALAGMITE_ASBESTOS.getId().getPath(),
                        modLoc("block/stalagmite_asbestos")
                )
        );
        simpleBlockWithItem(ModBlocks.STALAGMITE_SULFUR.get(),
                models().cubeAll(
                        ModBlocks.STALAGMITE_SULFUR.getId().getPath(),
                        modLoc("block/stalagmite_sulfur")
                )
        );
        simpleBlockWithItem(ModBlocks.STEEL_ROOF.get(),
                models().cubeAll(
                        ModBlocks.STEEL_ROOF.getId().getPath(),
                        modLoc("block/steel_roof")
                )
        );
        simpleBlockWithItem(ModBlocks.STEEL_SCAFFOLD.get(),
                models().cubeAll(
                        ModBlocks.STEEL_SCAFFOLD.getId().getPath(),
                        modLoc("block/steel_scaffold")
                )
        );
        simpleBlockWithItem(ModBlocks.STONE_CRACKED.get(),
                models().cubeAll(
                        ModBlocks.STONE_CRACKED.getId().getPath(),
                        modLoc("block/stone_cracked")
                )
        );
        simpleBlockWithItem(ModBlocks.STONE_DEPTH.get(),
                models().cubeAll(
                        ModBlocks.STONE_DEPTH.getId().getPath(),
                        modLoc("block/stone_depth")
                )
        );
        simpleBlockWithItem(ModBlocks.STONE_DEPTH_NETHER.get(),
                models().cubeAll(
                        ModBlocks.STONE_DEPTH_NETHER.getId().getPath(),
                        modLoc("block/stone_depth_nether")
                )
        );
        simpleBlockWithItem(ModBlocks.STONE_GNEISS.get(),
                models().cubeAll(
                        ModBlocks.STONE_GNEISS.getId().getPath(),
                        modLoc("block/stone_gneiss")
                )
        );
        simpleBlockWithItem(ModBlocks.STONE_KEYHOLE.get(),
                models().cubeAll(
                        ModBlocks.STONE_KEYHOLE.getId().getPath(),
                        modLoc("block/stone_keyhole")
                )
        );
        simpleBlockWithItem(ModBlocks.STONE_KEYHOLE_META.get(),
                models().cubeAll(
                        ModBlocks.STONE_KEYHOLE_META.getId().getPath(),
                        modLoc("block/stone_keyhole_meta")
                )
        );
        simpleBlockWithItem(ModBlocks.STONE_POROUS.get(),
                models().cubeAll(
                        ModBlocks.STONE_POROUS.getId().getPath(),
                        modLoc("block/stone_porous")
                )
        );
        simpleBlockWithItem(ModBlocks.STONE_RESOURCE_ASBESTOS.get(),
                models().cubeAll(
                        ModBlocks.STONE_RESOURCE_ASBESTOS.getId().getPath(),
                        modLoc("block/stone_resource_asbestos")
                )
        );
        simpleBlockWithItem(ModBlocks.STONE_RESOURCE_BAUXITE.get(),
                models().cubeAll(
                        ModBlocks.STONE_RESOURCE_BAUXITE.getId().getPath(),
                        modLoc("block/stone_resource_bauxite")
                )
        );
        simpleBlockWithItem(ModBlocks.STONE_RESOURCE_HEMATITE.get(),
                models().cubeAll(
                        ModBlocks.STONE_RESOURCE_HEMATITE.getId().getPath(),
                        modLoc("block/stone_resource_hematite")
                )
        );
        simpleBlockWithItem(ModBlocks.STONE_RESOURCE_LIMESTONE.get(),
                models().cubeAll(
                        ModBlocks.STONE_RESOURCE_LIMESTONE.getId().getPath(),
                        modLoc("block/stone_resource_limestone")
                )
        );
        simpleBlockWithItem(ModBlocks.STONE_RESOURCE_MALACHITE.get(),
                models().cubeAll(
                        ModBlocks.STONE_RESOURCE_MALACHITE.getId().getPath(),
                        modLoc("block/stone_resource_malachite")
                )
        );
        simpleBlockWithItem(ModBlocks.STONE_RESOURCE_SULFUR.get(),
                models().cubeAll(
                        ModBlocks.STONE_RESOURCE_SULFUR.getId().getPath(),
                        modLoc("block/stone_resource_sulfur")
                )
        );
        simpleBlockWithItem(ModBlocks.STRUCT_ICF_CORE.get(),
                models().cubeAll(
                        ModBlocks.STRUCT_ICF_CORE.getId().getPath(),
                        modLoc("block/struct_icf_core")
                )
        );
        simpleBlockWithItem(ModBlocks.STRUCT_LAUNCHER.get(),
                models().cubeAll(
                        ModBlocks.STRUCT_LAUNCHER.getId().getPath(),
                        modLoc("block/struct_launcher")
                )
        );
        simpleBlockWithItem(ModBlocks.STRUCT_LAUNCHER_CORE.get(),
                models().cubeAll(
                        ModBlocks.STRUCT_LAUNCHER_CORE.getId().getPath(),
                        modLoc("block/struct_launcher_core")
                )
        );
        simpleBlockWithItem(ModBlocks.STRUCT_LAUNCHER_CORE_LARGE.get(),
                models().cubeAll(
                        ModBlocks.STRUCT_LAUNCHER_CORE_LARGE.getId().getPath(),
                        modLoc("block/struct_launcher_core_large")
                )
        );
        simpleBlockWithItem(ModBlocks.STRUCT_SCAFFOLD.get(),
                models().cubeAll(
                        ModBlocks.STRUCT_SCAFFOLD.getId().getPath(),
                        modLoc("block/struct_scaffold")
                )
        );
        simpleBlockWithItem(ModBlocks.STRUCT_SOYUZ_CORE.get(),
                models().cubeAll(
                        ModBlocks.STRUCT_SOYUZ_CORE.getId().getPath(),
                        modLoc("block/struct_soyuz_core")
                )
        );
        simpleBlockWithItem(ModBlocks.STRUCT_TORUS_CORE.get(),
                models().cubeAll(
                        ModBlocks.STRUCT_TORUS_CORE.getId().getPath(),
                        modLoc("block/struct_torus_core")
                )
        );
        simpleBlockWithItem(ModBlocks.STRUCT_WATZ_CORE.get(),
                models().cubeAll(
                        ModBlocks.STRUCT_WATZ_CORE.getId().getPath(),
                        modLoc("block/struct_watz_core")
                )
        );
        simpleBlockWithItem(ModBlocks.WATZ_END.get(),
                models().cubeAll(
                        ModBlocks.WATZ_END.getId().getPath(),
                        modLoc("block/watz_end")
                )
        );
        simpleBlockWithItem(ModBlocks.WATZ_END_BOLTED.get(),
                models().cubeAll(
                        ModBlocks.WATZ_END_BOLTED.getId().getPath(),
                        modLoc("block/watz_end_bolted")
                )
        );
        simpleBlockWithItem(ModBlocks.TEKTITE.get(),
                models().cubeAll(
                        ModBlocks.TEKTITE.getId().getPath(),
                        modLoc("block/tektite")
                )
        );
        simpleBlockWithItem(ModBlocks.TESLA.get(),
                models().cubeAll(
                        ModBlocks.TESLA.getId().getPath(),
                        modLoc("block/tesla")
                )
        );
        simpleBlockWithItem(ModBlocks.THERM_ENDO.get(),
                models().cubeAll(
                        ModBlocks.THERM_ENDO.getId().getPath(),
                        modLoc("block/therm_endo")
                )
        );
        simpleBlockWithItem(ModBlocks.THERM_EXO.get(),
                models().cubeAll(
                        ModBlocks.THERM_EXO.getId().getPath(),
                        modLoc("block/therm_exo")
                )
        );
        simpleBlockWithItem(ModBlocks.TILE_LAB.get(),
                models().cubeAll(
                        ModBlocks.TILE_LAB.getId().getPath(),
                        modLoc("block/tile_lab")
                )
        );
        simpleBlockWithItem(ModBlocks.TILE_LAB_BROKEN.get(),
                models().cubeAll(
                        ModBlocks.TILE_LAB_BROKEN.getId().getPath(),
                        modLoc("block/tile_lab_broken")
                )
        );
        simpleBlockWithItem(ModBlocks.TILE_LAB_CRACKED.get(),
                models().cubeAll(
                        ModBlocks.TILE_LAB_CRACKED.getId().getPath(),
                        modLoc("block/tile_lab_cracked")
                )
        );
        simpleBlockWithItem(ModBlocks.TRAPDOOR_STEEL.get(),
                models().cubeAll(
                        ModBlocks.TRAPDOOR_STEEL.getId().getPath(),
                        modLoc("block/trapdoor_steel")
                )
        );
        simpleBlockWithItem(ModBlocks.VACUUM.get(),
                models().cubeAll(
                        ModBlocks.VACUUM.getId().getPath(),
                        modLoc("block/vacuum")
                )
        );
        simpleBlockWithItem(ModBlocks.VENT_CHLORINE.get(),
                models().cubeAll(
                        ModBlocks.VENT_CHLORINE.getId().getPath(),
                        modLoc("block/vent_chlorine")
                )
        );
        simpleBlockWithItem(ModBlocks.VENT_CHLORINE_SEAL.get(),
                models().cubeBottomTop(
                        ModBlocks.VENT_CHLORINE_SEAL.getId().getPath(),
                        modLoc("block/vent_chlorine_seal_side"),
                        modLoc("block/vent_chlorine_seal_side"),
                        modLoc("block/vent_chlorine_seal_top")
                )
        );
        simpleBlockWithItem(ModBlocks.VENT_CLOUD.get(),
                models().cubeAll(
                        ModBlocks.VENT_CLOUD.getId().getPath(),
                        modLoc("block/vent_cloud")
                )
        );
        simpleBlockWithItem(ModBlocks.VENT_PINK_CLOUD.get(),
                models().cubeAll(
                        ModBlocks.VENT_PINK_CLOUD.getId().getPath(),
                        modLoc("block/vent_pink_cloud")
                )
        );
        simpleBlockWithItem(ModBlocks.VINE_PHOSPHOR.get(),
                models().cubeAll(
                        ModBlocks.VINE_PHOSPHOR.getId().getPath(),
                        modLoc("block/vine_phosphor")
                )
        );
        simpleBlockWithItem(ModBlocks.VINYL_TILE_LARGE.get(),
                models().cubeAll(
                        ModBlocks.VINYL_TILE_LARGE.getId().getPath(),
                        modLoc("block/vinyl_tile_large")
                )
        );
        simpleBlockWithItem(ModBlocks.VOLCANO_CORE.get(),
                models().cubeAll(
                        ModBlocks.VOLCANO_CORE.getId().getPath(),
                        modLoc("block/volcano_core")
                )
        );
        simpleBlockWithItem(ModBlocks.VOLCANO_RAD_CORE.get(),
                models().cubeAll(
                        ModBlocks.VOLCANO_RAD_CORE.getId().getPath(),
                        modLoc("block/volcano_rad_core")
                )
        );
        simpleBlockWithItem(ModBlocks.WAND_AIR.get(),
                models().cubeAll(
                        ModBlocks.WAND_AIR.getId().getPath(),
                        modLoc("block/wand_air")
                )
        );
        simpleBlockWithItem(ModBlocks.WAND_JIGSAW.get(),
                models().cubeBottomTop(
                        ModBlocks.WAND_JIGSAW.getId().getPath(),
                        modLoc("block/wand_jigsaw_side"),
                        modLoc("block/wand_jigsaw_side"),
                        modLoc("block/wand_jigsaw_top")
                )
        );
        simpleBlockWithItem(ModBlocks.WAND_LOGIC.get(),
                models().cubeBottomTop(
                        ModBlocks.WAND_LOGIC.getId().getPath(),
                        modLoc("block/wand_logic"),
                        modLoc("block/wand_logic"),
                        modLoc("block/wand_logic_top")
                )
        );
        simpleBlockWithItem(ModBlocks.WAND_LOOT.get(),
                models().cubeBottomTop(
                        ModBlocks.WAND_LOOT.getId().getPath(),
                        modLoc("block/wand_loot"),
                        modLoc("block/wand_loot"),
                        modLoc("block/wand_loot_top")
                )
        );
        simpleBlockWithItem(ModBlocks.WASTE_EARTH.get(),
                models().cubeAll(
                        ModBlocks.WASTE_EARTH.getId().getPath(),
                        modLoc("block/waste_earth_bottom")
                )
        );
        simpleBlockWithItem(ModBlocks.WATZ_COOLER.get(),
                models().cubeBottomTop(
                        ModBlocks.WATZ_COOLER.getId().getPath(),
                        modLoc("block/watz_cooler_side"),
                        modLoc("block/watz_cooler_side"),
                        modLoc("block/watz_cooler_top")
                )
        );
        simpleBlockWithItem(ModBlocks.WATZ_ELEMENT.get(),
                models().cubeBottomTop(
                        ModBlocks.WATZ_ELEMENT.getId().getPath(),
                        modLoc("block/watz_element_side"),
                        modLoc("block/watz_element_side"),
                        modLoc("block/watz_element_top")
                )
        );
        simpleBlockWithItem(ModBlocks.WOOD_BARRIER.get(),
                models().cubeAll(
                        ModBlocks.WOOD_BARRIER.getId().getPath(),
                        modLoc("block/wood_barrier")
                )
        );
    }

    /**
     * Метод для блоков, у которых текстура имеет префикс "block_".
     * Например, для блока с именем "uranium_block" он будет искать текстуру "block_uranium".
     */
    private void resourceBlockWithItem(RegistrySupplier<Block> blockObject) {
        // 1. Получаем регистрационное имя (теперь оно уже "block_uranium")
        String registrationName = blockObject.getId().getPath();

        // 2. Имя текстуры теперь совпадает с именем блока!
        // (Если ваши текстуры называются block_uranium.png)
        String textureName = registrationName;

        // 4. Проверяем существование текстуры
        ResourceLocation textureLocation = modLoc("textures/block/" + textureName + ".png");
        if (!existingFileHelper.exists(textureLocation, PackType.CLIENT_RESOURCES)) {
            MainRegistry.LOGGER.warn("Texture not found for block {}: {}. Skipping model generation.",
                    registrationName, textureLocation);
            return;
        }

        // 5. Создаем модель
        simpleBlock(blockObject.get(), models().cubeAll(registrationName, modLoc("block/" + textureName)));

        // 6. Создаем модель для предмета
        simpleBlockItem(blockObject.get(), models().getExistingFile(blockTexture(blockObject.get())));
    }
    private void oreWithItem(RegistrySupplier<Block> blockObject) {
        // 1. Получаем регистрационное имя блока (например, "uranium_ore")
        String registrationName = blockObject.getId().getPath();

        // 2. Пробуем два варианта имени текстуры:
        //    - "ore_" + registrationName (например: ore_uranium)
        //    - registrationName (например: uranium_ore_deepslate)
        String textureName = "ore_" + registrationName;
        ResourceLocation textureLocation = modLoc("textures/block/" + textureName + ".png");

        if (!existingFileHelper.exists(textureLocation, PackType.CLIENT_RESOURCES)) {
            // Пробуем без префикса "ore_"
            textureName = registrationName;
            textureLocation = modLoc("textures/block/" + textureName + ".png");
            if (!existingFileHelper.exists(textureLocation, PackType.CLIENT_RESOURCES)) {
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
     * Поглотитель радиации — варианты по уровню ({@link BlockAbsorber.EnumAbsorberTier}).
     */
    private void registerRadAbsorber() {
        Block block = ModBlocks.RAD_ABSORBER.get();
        VariantBlockStateBuilder builder = getVariantBuilder(block);
        for (BlockAbsorber.EnumAbsorberTier tier : BlockAbsorber.EnumAbsorberTier.values()) {
            String modelName = "rad_absorber_" + tier.getSerializedName();
            ModelFile model = models().cubeAll(modelName, modLoc("block/" + tier.textureName));
            builder.partialState()
                    .with(BlockAbsorber.TIER, tier)
                    .modelForState()
                    .modelFile(model)
                    .addModel();
        }
    }

    /**
     * Старый метод для блоков, у которых имя текстуры СОВПАДАЕТ с именем регистрации.
     */
    private void blockWithItem(RegistrySupplier<Block> blockObject) {
        simpleBlock(blockObject.get());
        simpleBlockItem(blockObject.get(), models().getExistingFile(blockTexture(blockObject.get())));
    }

    private void registerDecoCtBlock(RegistrySupplier<Block> blockObject, String name) {
        // Важно: добавляем ссылку на *_ct текстуру в JSON, чтобы она гарантированно попала в block atlas.
        ModelFile model = models().withExistingParent(name, mcLoc("block/cube_all"))
                .texture("all", modLoc("block/" + name))
                .texture("ct", modLoc("block/" + name + "_ct"))
                .texture("particle", modLoc("block/" + name));
        simpleBlock(blockObject.get(), model);
        simpleBlockItem(blockObject.get(), model);
    }


    private void columnBlockWithItem(RegistrySupplier<Block> blockObject, ResourceLocation sideLocation, ResourceLocation topLocation, ResourceLocation bottomLocation) {
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
    private <T extends Block> void customObjBlock(RegistrySupplier<T> blockObject) {
        // Создаём только blockstate, который ссылается на JSON модель
        // JSON модель должна лежать в resources/assets/hbm_m/models/block/<название>.json
        horizontalBlock(blockObject.get(),
            models().getExistingFile(modLoc("block/" + blockObject.getId().getPath())));
    }

    private <T extends Block> void customDoorBlock(RegistrySupplier<T> blockObject) {
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

    private <T extends Block> void customMachineBlock(RegistrySupplier<T> blockObject) {
        // Создаём только blockstate, который ссылается на JSON модель
        // JSON модель должна лежать в resources/assets/hbm_m/models/block/<название>.json
        horizontalBlock(blockObject.get(),
            models().getExistingFile(modLoc("block/machines/" + blockObject.getId().getPath())));
    }

    private <T extends Block> void simpleMachineBlock(RegistrySupplier<T> blockObject) {
        simpleBlock(blockObject.get(),
                models().getExistingFile(modLoc("block/machines/" + blockObject.getId().getPath())));
    }

    /**
     * Crane-Block mit voller 6-Richtungs-{@link net.minecraft.world.level.block.state.properties.BlockStateProperties#FACING}
     * (wie ein Kolben/Dropper) statt der interaktiven Screwdriver-Ausgabeseite des Originals
     * ({@code BlockCraneBase}). Ausgabeseite ist immer die der Eingabeseite (FACING) gegenueberliegende
     * Seite - deshalb werden nur die "Default"-Icons (in/out/top/side) gebraucht, nicht die ~20
     * Turn-Varianten fuer eine per Screwdriver ueberschriebene Ausgabeseite (siehe Klassenkommentar
     * an {@code MachineCraneInserterBlockEntity} fuer die volle Scope-Begruendung).
     */
    private <T extends Block> void craneDirectionalBlock(RegistrySupplier<T> blockObject, String texturePrefix) {
        ResourceLocation inTex = modLoc("block/" + texturePrefix + "_in");
        ResourceLocation outTex = modLoc("block/" + texturePrefix + "_out");
        ResourceLocation topTex = modLoc("block/" + texturePrefix + "_top");
        ResourceLocation sideTex = modLoc("block/" + texturePrefix + "_side");

        VariantBlockStateBuilder builder = getVariantBuilder(blockObject.get());
        String basePath = blockObject.getId().getPath();

        for (Direction facing : Direction.values()) {
            Direction outputSide = facing.getOpposite();

            var model = models().withExistingParent(basePath + "_" + facing.getSerializedName(), mcLoc("block/block"))
                    .texture("particle", sideTex);

            model.element()
                    .from(0, 0, 0).to(16, 16, 16)
                    .face(Direction.DOWN).texture("#" + faceKey(Direction.DOWN, facing, outputSide)).cullface(Direction.DOWN).end()
                    .face(Direction.UP).texture("#" + faceKey(Direction.UP, facing, outputSide)).cullface(Direction.UP).end()
                    .face(Direction.NORTH).texture("#" + faceKey(Direction.NORTH, facing, outputSide)).cullface(Direction.NORTH).end()
                    .face(Direction.SOUTH).texture("#" + faceKey(Direction.SOUTH, facing, outputSide)).cullface(Direction.SOUTH).end()
                    .face(Direction.EAST).texture("#" + faceKey(Direction.EAST, facing, outputSide)).cullface(Direction.EAST).end()
                    .face(Direction.WEST).texture("#" + faceKey(Direction.WEST, facing, outputSide)).cullface(Direction.WEST).end()
                    .end();

            model.texture("in", inTex).texture("out", outTex).texture("top", topTex).texture("side", sideTex);

            builder.partialState()
                    .with(com.hbm_m.block.machines.MachineCraneInserterBlock.FACING, facing)
                    .modelForState().modelFile(model).addModel();
        }
    }

    /**
     * Fuer Bloecke mit einer 6-Wege-{@code FACING}-Property, deren Modell aber visuell symmetrisch
     * ist (z.B. ein kleiner zentrierter Marker-Wuerfel wie {@code MachineDroneWaypointBlock}) - jede
     * Facing-Variante zeigt dasselbe unrotierte Modell, spart damit 6 separate Modell-Dateien.
     */
    private <T extends Block> void directionlessFacingBlock(T block, ModelFile model) {
        VariantBlockStateBuilder builder = getVariantBuilder(block);
        for (Direction facing : Direction.values()) {
            builder.partialState()
                    .with(com.hbm_m.block.machines.MachineDroneWaypointBlock.FACING, facing)
                    .modelForState().modelFile(model).addModel();
        }
        simpleBlockItem(block, model);
    }

    /** Same as {@link #directionlessFacingBlock} but for the vanilla FACING property. */
    private <T extends Block> void plainFacingBlock(T block, ModelFile model) {
        VariantBlockStateBuilder builder = getVariantBuilder(block);
        for (Direction facing : Direction.values()) {
            builder.partialState()
                    .with(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, facing)
                    .modelForState().modelFile(model).addModel();
        }
        simpleBlockItem(block, model);
    }

    private static String faceKey(Direction face, Direction in, Direction out) {
        if (face == in) return "in";
        if (face == out) return "out";
        if (face == Direction.UP) return "top";
        return "side";
    }

    private <T extends Block> void customBombBlock(RegistrySupplier<T> blockObject) {
        // Создаём только blockstate, который ссылается на JSON модель
        // JSON модель должна лежать в resources/assets/hbm_m/models/block/bomb/<название>.json
        horizontalBlock(blockObject.get(),
            models().getExistingFile(modLoc("block/bomb/" + blockObject.getId().getPath())));
    }

    /**
     * Advanced Assembly Machine: FACING + FRAME (frame в BlockState для запекания в чанк).
     * Одна модель - getQuads возвращает Base+Frame при frame=true.
     */
    /**
     * Chemical plant: без {@code rotationY} в blockstate - поворот задаётся только в
     * {@link com.hbm_m.client.model.MachineChemicalPlantBakedModel} через
     * {@link com.hbm_m.util.MultipartFacingTransforms#legacyBlockEntityBakedRotationY}, в точности как
     * {@code LegacyAnimator.setupBlockTransform} у VBO (иначе vanilla y + getQuads дают двойной поворот).
     */
    private void registerChemicalPlantBlock(RegistrySupplier<? extends Block> blockObject) {
        VariantBlockStateBuilder builder = getVariantBuilder(blockObject.get());
        ModelFile modelFile = models().getExistingFile(modLoc("block/machines/" + blockObject.getId().getPath()));
        for (Direction facing : Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new)) {
            for (boolean frame : new boolean[] { false, true }) {
                builder.partialState()
                    .with(MachineChemicalPlantBlock.FACING, facing)
                    .with(MachineChemicalPlantBlock.FRAME, frame)
                    .modelForState()
                    .modelFile(modelFile)
                    .addModel();
            }
        }
    }

    private void registerAdvancedAssemblyMachineBlock(RegistrySupplier<? extends Block> blockObject) {
        VariantBlockStateBuilder builder = getVariantBuilder(blockObject.get());
        // Используем одну модель для всех состояний; world render — только BER/VBO.
        ModelFile modelFile = models().getExistingFile(modLoc("block/machines/" + blockObject.getId().getPath()));
        
        for (Direction facing : Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new)) {
            for (boolean frame : new boolean[]{false, true}) {
                builder.partialState()
                    .with(MachineAdvancedAssemblerBlock.FACING, facing)
                    .with(MachineAdvancedAssemblerBlock.FRAME, frame)
                    .modelForState()
                    .modelFile(modelFile)
                    .rotationY(getRotationY(facing))
                    .addModel();
            }
        }
    }

    private void registerMachineAssemblerBlock(RegistrySupplier<? extends Block> blockObject) {
        VariantBlockStateBuilder builder = getVariantBuilder(blockObject.get());
        ModelFile modelFile = models().getExistingFile(modLoc("block/machines/" + blockObject.getId().getPath()));

        for (Direction facing : Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new)) {
            builder.partialState()
                    .with(MachineAssemblerBlock.FACING, facing)
                    .modelForState()
                    .modelFile(modelFile)
                    .rotationY(getRotationY(facing))
                    .addModel();
        }
    }

    /**
     * Генерирует модель и состояние для горизонтально-ориентированного блока.
     * @param blockObject Блок
     * @param sideTexture Текстура для боковых и задней сторон
     * @param frontTexture Текстура для лицевой стороны (север)
     * @param topTexture Текстура для верха и низа
     */
    private void orientableBlockWithItem(RegistrySupplier<Block> blockObject, ResourceLocation sideTexture, ResourceLocation frontTexture, ResourceLocation topTexture) {
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

    /** Тонкий слой осадков (не snow-layer с LAYERS). */
    private void registerFalloutLayerBlock(RegistrySupplier<Block> block, String baseName) {
        ResourceLocation texture = blockTexture(block.get());
        ModelFile model = models().withExistingParent(baseName, mcLoc("block/snow_height2"))
                .texture("texture", texture)
                .texture("particle", texture);
        simpleBlock(block.get(), model);
    }

    /** Полный блок fallout (1.7.10 block_fallout). */
    private void registerFalloutBlock(RegistrySupplier<Block> block, String baseName, String textureName) {
        simpleBlock(block.get(), models().cubeAll(baseName, modLoc("block/" + textureName)));
    }

    private void registerSnowLayerBlock(RegistrySupplier<Block> block, String baseName) {
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
    private void registerLitMachineBlock(RegistrySupplier<? extends Block> blockObject, 
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

    private void registerSellafieldSlaked(RegistrySupplier<Block> blockObject, String modelBaseName) {
        Block block = blockObject.get();
        getVariantBuilder(block).forAllStatesExcept(state -> {
            int variant = state.getValue(BlockSellafieldSlaked.VARIANT);
            String modelName = modelBaseName + (variant == 0 ? "" : "_" + variant);
            String texName = variant == 0 ? "sellafield_slaked" : "sellafield_slaked_" + variant;

            ModelFile tintedModel = models().withExistingParent(modelName, mcLoc("block/cube"))
                    .texture("particle", modLoc("block/" + texName))
                    .texture("down", modLoc("block/" + texName))
                    .texture("up", modLoc("block/" + texName))
                    .texture("north", modLoc("block/" + texName))
                    .texture("south", modLoc("block/" + texName))
                    .texture("west", modLoc("block/" + texName))
                    .texture("east", modLoc("block/" + texName))
                    .element()
                    .from(0, 0, 0).to(16, 16, 16)
                    .allFaces((dir, face) -> face.texture("#" + dir.getName()).tintindex(0))
                    .end();

            return ConfiguredModel.builder().modelFile(tintedModel).build();
        }, BlockSellafieldSlaked.COLOR_LEVEL);

        simpleBlockItem(block, models().cubeAll(modelBaseName, modLoc("block/sellafield_slaked")));
    }

    private void registerSellafieldOre(RegistrySupplier<Block> blockObject, String baseName, String overlayTexture) {
        Block block = blockObject.get();
        getVariantBuilder(block).forAllStatesExcept(state -> {
            int variant = state.getValue(BlockSellafieldSlaked.VARIANT);
            String modelName = baseName + (variant == 0 ? "" : "_" + variant);
            String baseTex = variant == 0 ? "sellafield_slaked" : "sellafield_slaked_" + variant;

            ModelFile oreModel = models().withExistingParent(modelName, mcLoc("block/cube"))
                    .renderType("cutout")
                    .texture("base", modLoc("block/" + baseTex))
                    .texture("overlay", modLoc(overlayTexture))
                    .texture("particle", modLoc(overlayTexture))
                    .element()
                    .from(0, 0, 0).to(16, 16, 16)
                    .allFaces((dir, face) -> face.texture("#base").tintindex(0))
                    .end()
                    .element()
                    .from(0, 0, 0).to(16, 16, 16)
                    .allFaces((dir, face) -> face.texture("#overlay"))
                    .end();

            return ConfiguredModel.builder().modelFile(oreModel).build();
        }, BlockSellafieldSlaked.COLOR_LEVEL);

        simpleBlockItem(block, models().cubeAll(blockObject.getId().getPath(), modLoc(overlayTexture)));
    }
}
//?}