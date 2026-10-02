package com.hbm_m.datagen.assets;

import java.util.List;

import java.util.Map;
//? if forge {
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.RedBrickBlock;
import com.hbm_m.block.RedBrickBlock.RedFace;
import com.hbm_m.block.decorations.DoorBlock;
// Провайдер генерации состояний блоков и моделей для блоков мода.
// Используется в классе DataGenerators для регистрации.
import com.hbm_m.block.generic.BlockAbsorber;
import com.hbm_m.block.generic.BlockSellafieldSlaked;
import com.hbm_m.block.machines.BlastFurnaceBlock;
import com.hbm_m.block.machines.MachineBlastFurnaceBlock;
import com.hbm_m.block.machines.MachineAdvancedAssemblerBlock;
import com.hbm_m.block.machines.MachineAssemblerBlock;
import com.hbm_m.block.machines.MachineChemicalPlantBlock;
import com.hbm_m.block.machines.MachineWoodBurnerBlock;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.multiblock.PartRole;

import java.util.Map;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelBuilder;
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
        registerR6aBlocks();
        registerMigratedManualBlocks();

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
            // Blockstate only. The item model is hand-written under models/item/turret_<name>.json
            // because it needs per-turret display transforms: these composite OBJ models carry no
            // "display" section, so the items rendered at full world scale and spilled far out of
            // their inventory slots (the artillery mesh alone spans over four blocks).
            simpleBlock(turretBlock.get(),
                    models().getExistingFile(modLoc("block/" + turretBlock.getId().getPath())));
        }

        // ─── AUTO-PORT: fehlende Original-Bloecke (nur DEV-Tab, ungeprueft) ───
        // Texturen liegen unter block/ported/, getrennt vom handgepflegten Bestand.
        simpleBlockWithItem(ModBlocks.BLOCK_ASBESTOS.get(), models().cubeAll("block_asbestos", modLoc("block/ported/block_asbestos")));
        simpleBlockWithItem(ModBlocks.BLOCK_BAKELITE.get(), models().cubeAll("block_bakelite", modLoc("block/ported/block_bakelite")));
        simpleBlockWithItem(ModBlocks.BLOCK_COLTAN.get(), models().cubeAll("block_coltan", modLoc("block/ported/block_coltan")));
        simpleBlockWithItem(ModBlocks.BLOCK_CORIUM.get(), models().cubeAll("block_corium", modLoc("block/ported/block_corium")));
        simpleBlockWithItem(ModBlocks.BLOCK_CORIUM_COBBLE.get(), models().cubeAll("block_corium_cobble", modLoc("block/ported/block_corium_cobble")));
        simpleBlockWithItem(ModBlocks.BLOCK_FLUORITE.get(), models().cubeAll("block_fluorite", modLoc("block/ported/block_fluorite")));
        simpleBlockWithItem(ModBlocks.BLOCK_LITHIUM.get(), models().cubeAll("block_lithium", modLoc("block/ported/block_lithium")));
        simpleBlockWithItem(ModBlocks.BLOCK_MAGNETIZED_TUNGSTEN.get(), models().cubeAll("block_magnetized_tungsten", modLoc("block/ported/block_magnetized_tungsten")));
        simpleBlockWithItem(ModBlocks.BLOCK_METEOR_MOLTEN.get(), models().cubeAll("block_meteor_molten", modLoc("block/ported/block_meteor_molten")));
        simpleBlockWithItem(ModBlocks.BLOCK_METEOR_TREASURE.get(), models().cubeAll("block_meteor_treasure", modLoc("block/ported/block_meteor_treasure")));
        simpleBlockWithItem(ModBlocks.ORE_METEOR_IRON.get(), models().cubeAll("ore_meteor_iron", modLoc("block/ported/ore_meteor_iron")));
        simpleBlockWithItem(ModBlocks.ORE_METEOR_COPPER.get(), models().cubeAll("ore_meteor_copper", modLoc("block/ported/ore_meteor_copper")));
        simpleBlockWithItem(ModBlocks.ORE_METEOR_ALUMINIUM.get(), models().cubeAll("ore_meteor_aluminium", modLoc("block/ported/ore_meteor_aluminium")));
        simpleBlockWithItem(ModBlocks.ORE_METEOR_RAREEARTH.get(), models().cubeAll("ore_meteor_rareearth", modLoc("block/ported/ore_meteor_rareearth")));
        simpleBlockWithItem(ModBlocks.ORE_METEOR_COBALT.get(), models().cubeAll("ore_meteor_cobalt", modLoc("block/ported/ore_meteor_cobalt")));
        simpleBlockWithItem(ModBlocks.BLOCK_NITER.get(), models().cubeAll("block_niter", modLoc("block/ported/block_niter")));
        simpleBlockWithItem(ModBlocks.BLOCK_POLYMER.get(), models().cubeAll("block_polymer", modLoc("block/ported/block_polymer")));
        simpleBlockWithItem(ModBlocks.BLOCK_PU_MIX.get(), models().cubeAll("block_pu_mix", modLoc("block/ported/block_pu_mix")));
        simpleBlockWithItem(ModBlocks.BLOCK_RED_PHOSPHORUS.get(), models().cubeAll("block_red_phosphorus", modLoc("block/ported/block_red_phosphorus")));
        simpleBlockWithItem(ModBlocks.BLOCK_RUBBER.get(), models().cubeAll("block_rubber", modLoc("block/ported/block_rubber")));
        simpleBlockWithItem(ModBlocks.BLOCK_SULFUR.get(), models().cubeAll("block_sulfur", modLoc("block/ported/block_sulfur")));
        simpleBlockWithItem(ModBlocks.BLOCK_TANTALIUM.get(), models().cubeAll("block_tantalium", modLoc("block/ported/block_tantalium")));
        simpleBlockWithItem(ModBlocks.BLOCK_TRINITITE.get(), models().cubeAll("block_trinitite", modLoc("block/ported/block_trinitite")));
        simpleBlockWithItem(ModBlocks.BLOCK_WASTE.get(), models().cubeAll("block_waste", modLoc("block/ported/block_waste")));
        simpleBlockWithItem(ModBlocks.BLOCK_WASTE_VITRIFIED.get(), models().cubeAll("block_waste_vitrified", modLoc("block/ported/block_waste_vitrified")));
        simpleBlockWithItem(ModBlocks.BLOCK_WHITE_PHOSPHORUS.get(), models().cubeAll("block_white_phosphorus", modLoc("block/ported/block_white_phosphorus")));
        simpleBlockWithItem(ModBlocks.BLOCK_YELLOWCAKE.get(), models().cubeAll("block_yellowcake", modLoc("block/ported/block_yellowcake")));
        worldFluids();
        simpleBlockWithItem(ModBlocks.DUNGEON_SPAWNER.get(), models().cubeAll("dungeon_spawner", modLoc("block/ported/dungeon_spawner")));
        simpleBlockWithItem(ModBlocks.EVENT_TESTER.get(), models().cubeAll("event_tester", modLoc("block/ported/event_tester")));
        icfPhantomState();
        simpleBlockWithItem(ModBlocks.LAUNCH_TABLE.get(), models().cubeAll("launch_table", modLoc("block/ported/launch_table")));
        simpleBlockWithItem(ModBlocks.LOGIC_BLOCK.get(), models().cubeAll("logic_block", modLoc("block/ported/logic_block")));
        simpleBlockWithItem(ModBlocks.ORE_ALUMINIUM.get(), models().cubeAll("ore_aluminium", modLoc("block/ported/ore_aluminium")));
        simpleBlockWithItem(ModBlocks.ORE_AUSTRALIUM.get(), models().cubeAll("ore_australium", modLoc("block/ported/ore_australium")));
        simpleBlockWithItem(ModBlocks.ORE_COPPER.get(), models().cubeAll("ore_copper", modLoc("block/ported/ore_copper")));
        simpleBlockWithItem(ModBlocks.ORE_GNEISS_URANIUM_SCORCHED.get(), models().cubeAll("ore_gneiss_uranium_scorched", modLoc("block/ported/ore_gneiss_uranium_scorched")));
        simpleBlockWithItem(ModBlocks.ORE_NETHER_PLUTONIUM.get(), models().cubeAll("ore_nether_plutonium", modLoc("block/ported/ore_nether_plutonium")));
        simpleBlockWithItem(ModBlocks.ORE_NETHER_TUNGSTEN.get(), models().cubeAll("ore_nether_tungsten", modLoc("block/ported/ore_nether_tungsten")));
        simpleBlockWithItem(ModBlocks.ORE_NETHER_URANIUM_SCORCHED.get(), models().cubeAll("ore_nether_uranium_scorched", modLoc("block/ported/ore_nether_uranium_scorched")));
        simpleBlockWithItem(ModBlocks.ORE_TEKTITE_OSMIRIDIUM.get(), models().cubeAll("ore_tektite_osmiridium", modLoc("block/ported/ore_tektite_osmiridium")));
        simpleBlockWithItem(ModBlocks.ORE_URANIUM_SCORCHED.get(), models().cubeAll("ore_uranium_scorched", modLoc("block/ported/ore_uranium_scorched")));
        pileBlockStates();
        simpleBlockWithItem(ModBlocks.PILE_BRICK.get(), models().cubeBottomTop("pile_brick",
                modLoc("block/ported/pile_brick_side"), modLoc("block/ported/pile_brick_bottom"), modLoc("block/ported/pile_brick_top")));
        pileDeviceStates();
        simpleBlockWithItem(ModBlocks.PNEUMATIC_STORAGE_ACCESS.get(), models().cubeAll("pneumatic_storage_access", modLoc("block/ported/pneumatic_storage_access")));
        simpleBlockWithItem(ModBlocks.PNEUMATIC_STORAGE_CLUTTER.get(), models().cubeAll("pneumatic_storage_clutter", modLoc("block/ported/pneumatic_storage_clutter")));
        simpleBlockWithItem(ModBlocks.PNEUMATIC_STORAGE_EXPORTER.get(), models().cubeAll("pneumatic_storage_exporter", modLoc("block/ported/pneumatic_storage_exporter")));
        simpleBlockWithItem(ModBlocks.PNEUMATIC_STORAGE_IMPORTER.get(), models().cubeAll("pneumatic_storage_importer", modLoc("block/ported/pneumatic_storage_importer")));
        simpleBlockWithItem(ModBlocks.PNEUMATIC_STORAGE_MONO.get(), models().cubeAll("pneumatic_storage_mono", modLoc("block/ported/pneumatic_storage_mono")));
        simpleBlockWithItem(ModBlocks.STRUCTURE_ANCHOR.get(), models().cubeAll("structure_anchor", modLoc("block/ported/structure_anchor")));
        simpleBlockWithItem(ModBlocks.WAND_TANDEM.get(), models().cubeBottomTop("wand_tandem",
                modLoc("block/ported/wand_tandem_side"), modLoc("block/ported/wand_tandem_bottom"), modLoc("block/ported/wand_tandem_top")));
        // ─── ENDE AUTO-PORT Bloecke ───

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
                        modLoc("block/nuclear_charge"),
                        modLoc("block/nuclear_charge_top")
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
        simpleBlockWithItem(ModBlocks.CONCRETE_MARKED.get(),
                models().cubeBottomTop(
                        ModBlocks.CONCRETE_MARKED.getId().getPath(),
                        modLoc("block/concrete_marked"),
                        modLoc("block/concrete"),
                        modLoc("block/concrete")
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


		simpleBlockWithItem(ModBlocks.C4.get(),
                models().withExistingParent(ModBlocks.C4.getId().getPath(), mcLoc("block/orientable"))
                        .texture("front", modLoc("block/c4block_front"))
                        .texture("side", modLoc("block/c4"))
                        .texture("top", modLoc("block/c4"))
                        .texture("bottom", modLoc("block/c4"))
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

        // Колючая проволока: составные OBJ-модели остаются ручными (forge:composite + forge:obj)







        doorBlockWithRenderType(((net.minecraft.world.level.block.DoorBlock) ModBlocks.METAL_DOOR.get()), modLoc("block/door_metal_bottom"), modLoc("block/door_metal_top"), "cutout");
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
                modLoc("block/smoke_bomb_top")
        );

        // Блоки с кастомной OBJ моделью
        // Doors
        
        customDoorBlock(ModBlocks.LARGE_VEHICLE_DOOR);
        customDoorBlock(ModBlocks.ROUND_AIRLOCK_DOOR);
        horizontalBlock(ModBlocks.TRANSITION_SEAL.get(),
            models().getExistingFile(modLoc("block/doors/transition_seal")));
        horizontalBlock(ModBlocks.SLIDE_DOOR.get(),
            models().getExistingFile(modLoc("block/doors/sliding_blast_door")));
        customDoorBlock(ModBlocks.SILO_HATCH);
        customDoorBlock(ModBlocks.SILO_HATCH_LARGE);
        customDoorBlock(ModBlocks.QE_SLIDING);
        customDoorBlock(ModBlocks.QE_CONTAINMENT);
        customDoorBlock(ModBlocks.WATER_DOOR);
        customDoorBlock(ModBlocks.FIRE_DOOR);
        customDoorBlock(ModBlocks.SLIDING_SEAL_DOOR);
        customDoorBlock(ModBlocks.SECURE_ACCESS_DOOR);
        customDoorBlock(ModBlocks.VAULT_DOOR);
        customDoorBlock(ModBlocks.CARGO_DOOR);

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
        simpleBlock(ModBlocks.AUTOSAW.get(),
            models().getExistingFile(modLoc("block/machines/autosaw")));
        horizontalBlock(ModBlocks.THRESHER.get(),
            models().getExistingFile(modLoc("block/machines/thresher")));
        simpleMachineBlock(ModBlocks.BEAMLINE);
        explodableMachineBlock(ModBlocks.BOILER,
                "block/machines/boiler", "block/machines/boiler_burst");
        horizontalBlock(ModBlocks.PUMP_STEAM.get(),
            models().cubeAll(ModBlocks.PUMP_STEAM.getId().getPath(), modLoc("block/machine/pump_steam")));
        horizontalBlock(ModBlocks.PUMP_ELECTRIC.get(),
            models().cubeAll(ModBlocks.PUMP_ELECTRIC.getId().getPath(), modLoc("block/machine/pump_electric")));
        customMachineBlock(ModBlocks.BOILER_FUSION);
        customMachineBlock(ModBlocks.BREEDER_FUSION);
        customMachineBlock(ModBlocks.CHIMNEY_BRICK);
        customMachineBlock(ModBlocks.CHIMNEY_INDUSTRIAL);
        customMachineBlock(ModBlocks.COKER);
        customMachineBlock(ModBlocks.COLLECTOR);
        simpleMachineBlock(ModBlocks.COMBINATION_OVEN);
        customMachineBlock(ModBlocks.COMBUSTION_ENGINE);
        horizontalBlock(ModBlocks.COMPRESSOR.get(),
            models().getExistingFile(modLoc("block/machines/compressor")));
        customMachineBlock(ModBlocks.COMPRESSOR_COMPACT);
        customMachineBlock(ModBlocks.CONDENSER_POWERED);
        customMachineBlock(ModBlocks.LPW2);
        customMachineBlock(ModBlocks.CONVEYOR_PRESS);
        customMachineBlock(ModBlocks.COUPLER);
        simpleMachineBlock(ModBlocks.DETECTOR);
        customMachineBlock(ModBlocks.DIESELGEN);
        simpleMachineBlock(ModBlocks.DIPOLE);
        simpleMachineBlock(ModBlocks.DRONE);
        // Multiblock mit Ausrichtung: das Modell dreht sich jetzt mit der Struktur.
        horizontalBlock(ModBlocks.ELECTRIC_HEATER.get(),
                models().getExistingFile(modLoc("block/machines/electric_heater")));
        horizontalBlock(ModBlocks.ELECTROLYSER.get(),
            models().getExistingFile(modLoc("block/machines/electrolyser")));
        customMachineBlock(ModBlocks.EPRESS);
        horizontalBlock(ModBlocks.EXPOSURE_CHAMBER.get(),
            models().getExistingFile(modLoc("block/machines/exposure_chamber")));
        simpleMachineBlock(ModBlocks.FENSU);
        // FENSU2 (machine_battery_redd) is a MachineBatteryBlock (FACING-Blockstate) - see orientableBlockWithItem below.
        simpleMachineBlock(ModBlocks.FIREBOX);
        horizontalBlock(ModBlocks.HEATEX.get(),
                models().getExistingFile(modLoc("block/machines/heatex")));
        customMachineBlock(ModBlocks.HEPHAESTUS);
        // Der Reaktor hat seit dem Port eine Blickrichtung - das Modell dreht sich mit.
        horizontalBlock(ModBlocks.ICF.get(), models().getExistingFile(modLoc("block/machines/icf")));
        icfLaserStates();
        // Der Aufbaukern zeigt in die Richtung, in die der fertige Reaktor blickt.
        horizontalBlock(ModBlocks.STRUCT_ICF_CORE.get(),
                models().cubeAll("struct_icf_core", modLoc("block/struct_icf_core")));
        simpleBlockItem(ModBlocks.STRUCT_ICF_CORE.get(), models().getExistingFile(modLoc("block/struct_icf_core")));
        simpleMachineBlock(ModBlocks.INTAKE);
        customMachineBlock(ModBlocks.KLYSTRON);
        customMachineBlock(ModBlocks.KLYSTRON_CREATIVE);
        customMachineBlock(ModBlocks.MHDT);
        horizontalBlock(ModBlocks.MICROWAVE.get(),
            models().getExistingFile(modLoc("block/machines/microwave")));
        customMachineBlock(ModBlocks.MINING_LASER);
        horizontalBlock(ModBlocks.OILBURNER.get(),
                models().getExistingFile(modLoc("block/machines/oilburner")));
        horizontalBlock(ModBlocks.OILBURNER_HP.get(),
                models().getExistingFile(modLoc("block/machines/oilburner_hp")));
        // ORBUS is now a BarrelTankBlock (FACING blockstate) instead of a static "" variant.
        horizontalBlock(ModBlocks.ORBUS.get(), models().getExistingFile(modLoc("block/machines/orbus")));
        simpleMachineBlock(ModBlocks.ORE_SLOPPER);
        customMachineBlock(ModBlocks.PLASMA_FORGE);
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
        customMachineBlock(ModBlocks.MACHINE_SATLINK);
        customMachineBlock(ModBlocks.STIRLING_CREATIVE);
        customMachineBlock(ModBlocks.STIRLING_STEEL);
        // Strand Caster: eigenes OBJ-Modell bereits vorhanden (block/machines/strand_caster.json), FACING-Rotation.
        horizontalBlock(ModBlocks.STRAND_CASTER.get(),
            models().getExistingFile(modLoc("block/machines/strand_caster")));
        customMachineBlock(ModBlocks.TORUS);
        simpleMachineBlock(ModBlocks.TURBINEGAS);
        simpleMachineBlock(ModBlocks.WATZ_PUMP);
        simpleMachineBlock(ModBlocks.CHUNGUS);
        customMachineBlock(ModBlocks.CENTRIFUGE);
        customMachineBlock(ModBlocks.BREEDER);
        customMachineBlock(ModBlocks.LARGE_PYLON);
        customMachineBlock(ModBlocks.LAUNCH_PAD);
        customMachineBlock(ModBlocks.MOBILE_LAUNCH_PAD);
        customMachineBlock(ModBlocks.TOPOL_LAUNCH_PAD);
        customMachineBlock(ModBlocks.LAUNCH_PAD_RUSTED);
        customBombBlock(ModBlocks.NUKE_FAT_MAN);
        customBombBlock(ModBlocks.NUKE_GADGET);
        customBombBlock(ModBlocks.NUKE_BOY);
        customBombBlock(ModBlocks.NUKE_MIKE);
        customBombBlock(ModBlocks.NUKE_TSAR);
        customBombBlock(ModBlocks.NUKE_FLEIJA);
        customMachineBlock(ModBlocks.CORE_EMITTER);
        customMachineBlock(ModBlocks.CORE_INJECTOR);
        customMachineBlock(ModBlocks.CORE_RECEIVER);
        customMachineBlock(ModBlocks.VACUUM_DISTILL);
        customMachineBlock(ModBlocks.TURBOFAN);
        customMachineBlock(ModBlocks.INDUSTRIAL_TURBINE);
        // TURBINE: ориентация модели отличается от стандартной horizontalBlock-развёртки
        // (ручной эталон: east=0, north=90, south=270, west=180)
        VariantBlockStateBuilder turbineBuilder = getVariantBuilder(ModBlocks.TURBINE.get());
        ModelFile turbineModel = models().getExistingFile(modLoc("block/machines/turbine"));
        turbineBuilder.partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.EAST)
                .modelForState().modelFile(turbineModel).addModel();
        turbineBuilder.partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                .modelForState().modelFile(turbineModel).rotationY(90).addModel();
        turbineBuilder.partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH)
                .modelForState().modelFile(turbineModel).rotationY(270).addModel();
        turbineBuilder.partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.WEST)
                .modelForState().modelFile(turbineModel).rotationY(180).addModel();
        // MACHINE_CHUNGUS nutzt das bereits vorhandene chungus.obj-Modell (Pfad weicht von der
        // Registry-ID ab, daher kein customMachineBlock()).
        horizontalBlock(ModBlocks.MACHINE_CHUNGUS.get(),
                models().getExistingFile(modLoc("block/machines/chungus")));
        customMachineBlock(ModBlocks.SUBSTATION);
        registerMachineAssemblerBlock(ModBlocks.MACHINE_ASSEMBLER);
        registerAdvancedAssemblyMachineBlock(ModBlocks.ADVANCED_ASSEMBLY_MACHINE);
        customMachineBlock(ModBlocks.PRESS);

        // Машины со свойством LIT (включен/выключен)
        // Legacy одноблочная доменная печь: ванильный orientable c текстурами оригинала (difurnace_*).
        registerLitMachineBlock(ModBlocks.BLAST_FURNACE,
            BlastFurnaceBlock.FACING, BlastFurnaceBlock.LIT,
            "blast_furnace", "blast_furnace_on");
        // Мультиблочная доменная печь 3x7x3: цельная OBJ-модель ядра.
        registerLitMachineBlock(ModBlocks.MACHINE_BLAST_FURNACE,
            MachineBlastFurnaceBlock.FACING, MachineBlastFurnaceBlock.LIT,
            "machine_blast_furnace", "machine_blast_furnace");
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

        // FluidTank - FACING plus the wrecked variant
        explodableMachineBlock(ModBlocks.FLUID_TANK,
                "block/machines/fluid_tank", "block/machines/fluid_tank_exploded");

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


        ModelFile fluidExhaustModel = models().withExistingParent(ModBlocks.FLUID_EXHAUST.getId().getPath(), mcLoc("block/cube_all"))
                .texture("all", mcLoc("block/iron_block"))
                .texture("particle", mcLoc("block/iron_block"));
        simpleBlock(ModBlocks.FLUID_EXHAUST.get(), fluidExhaustModel);

        // Decor
        customObjBlockNoFacing(ModBlocks.REBAR);

        // Decor
        customObjBlock(ModBlocks.GEIGER_COUNTER_BLOCK);

        columnBlockWithItem(
                ModBlocks.DECON,
                modLoc("block/decon_side"),
                modLoc("block/decon_top"),
                modLoc("block/decon_side")
        );
        registerRadAbsorber();

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
        simpleBlockWithItem(ModBlocks.MINE_AP.get(), models().getExistingFile(modLoc("block/bomb/mine_ap")));
        simpleBlockWithItem(ModBlocks.MINE_FAT.get(), models().getExistingFile(modLoc("block/bomb/mine_fat")));
        customBombBlock(ModBlocks.NAVAL_MINE);
        customBombBlock(ModBlocks.MINE_NAVAL);
        customObjBlock(ModBlocks.CRATE_CONSERVE);

        // Технический блок без визуала; particle нужен, чтобы партиклы разрушения не были missing tex.
        simpleBlock(ModBlocks.UNIVERSAL_MACHINE_PART.get(), models().getBuilder(ModBlocks.UNIVERSAL_MACHINE_PART.getId().getPath())
                .texture("particle", modLoc("block/block_steel_machine")));
        // wire_coated: manual multipart blockstate + OBJ visibility (see assets/hbm_m/blockstates/wire_coated.json)
        // red_* ЛЭП (коннекторы/пилоны): ручные blockstates с OBJ-моделями и поворотами (assets/hbm_m/blockstates).
        // PYLON_DUMMY не датагенится вовсе (invisible).

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

        // ================== ДЕКОР СТРУКТУР (порт 1.7.10) ==================
        blockWithItem(ModBlocks.BLOCK_COPPER);
        // BLOCK_RED_COPPER / BLOCK_STARMETAL — алиасы слитковых блоков (auto-loop ниже), тут не конфигурируем.
        grateBlockWithItem(ModBlocks.STEEL_GRATE, "block/steel_grate", "block/steel_grate_end");
        grateBlockWithItem(ModBlocks.STEEL_GRATE_WIDE, "block/steel_grate_wide", "block/steel_grate_wide_end");

        // груда лута
        simpleBlock(ModBlocks.DECO_LOOT.get(),
                models().cubeAll("block/deco_loot", modLoc("block/deco_rusty_steel")));
        simpleBlockItem(ModBlocks.DECO_LOOT.get(), models().getExistingFile(modLoc("block/deco_loot")));

        // мёртвое растение (cross)
        ModelFile plantDead = models().cross("block/plant_dead", modLoc("block/plant_dead")).renderType("cutout");

        // стальные балки и трубы (axis-колонны)
        // Порт DecoBlock: вертикальная колонна 2x2px, AXIS-ориентация
        // Deko-Rohre: statische, gebackene OBJ-Modelle (models/block/pipes, R6b)

        // ================== КОНЕЦ ДЕКОРА СТРУКТУР ==================

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
        for (ModMaterials mat : ModMaterials.values()) {
            if (!mat.has(MaterialShape.BLOCK)) continue;
            if (ModBlocks.hasIngotBlock(mat)) {
                RegistrySupplier<Block> blockRegistrySupplier = ModBlocks.getIngotBlock(mat);
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
        oreWithItem(ModBlocks.SEQUESTRUM_ORE);
        oreWithItem(ModBlocks.SCHRABIDIUM_ORE);
        oreWithItem(ModBlocks.SCHRABIDIUM_ORE_NETHER);
        oreWithItem(ModBlocks.SCHRABIDIUM_ORE_GNEISS);
        // Руды паритета генерации с 1.7.10 (медная руда не нужна — есть ванильная)
        oreWithItem(ModBlocks.NITER_ORE);
        oreWithItem(ModBlocks.NITER_ORE_DEEPSLATE);
        oreWithItem(ModBlocks.LITHIUM_ORE);
        oreWithItem(ModBlocks.LITHIUM_ORE_DEEPSLATE);
        oreWithItem(ModBlocks.ALEXANDRITE_ORE);
        oreWithItem(ModBlocks.COLTAN_ORE);
        oreWithItem(ModBlocks.COLTAN_ORE_DEEPSLATE);
        oreWithItem(ModBlocks.SULFUR_ORE_DEEPSLATE);
        oreWithItem(ModBlocks.TUNGSTEN_ORE_DEEPSLATE);
        oreWithItem(ModBlocks.ASBESTOS_ORE_DEEPSLATE);
        oreWithItem(ModBlocks.FLUORITE_ORE_DEEPSLATE);
        oreWithItem(ModBlocks.LIGNITE_ORE_DEEPSLATE);
        oreWithItem(ModBlocks.NETHER_URANIUM_ORE);
        oreWithItem(ModBlocks.NETHER_TUNGSTEN_ORE);
        oreWithItem(ModBlocks.NETHER_SULFUR_ORE);
        oreWithItem(ModBlocks.NETHER_FIRE_ORE);
        oreWithItem(ModBlocks.NETHER_COAL_ORE);
        oreWithItem(ModBlocks.NETHER_COBALT_ORE);
        oreWithItem(ModBlocks.NETHER_PLUTONIUM_ORE);
        oreWithItem(ModBlocks.NETHER_SMOLDERING_ORE);
        oreWithItem(ModBlocks.DEPTH_NETHER_NEODYMIUM);
        oreWithItem(ModBlocks.AUSTRALIUM_ORE);
        oreWithItem(ModBlocks.GNEISS_IRON_ORE);
        oreWithItem(ModBlocks.GNEISS_GOLD_ORE);
        oreWithItem(ModBlocks.GNEISS_URANIUM_ORE);
        oreWithItem(ModBlocks.GNEISS_COPPER_ORE);
        oreWithItem(ModBlocks.GNEISS_ASBESTOS_ORE);
        oreWithItem(ModBlocks.GNEISS_LITHIUM_ORE);
        oreWithItem(ModBlocks.GNEISS_RARE_ORE);
        oreWithItem(ModBlocks.GNEISS_GAS_ORE);
        oreWithItem(ModBlocks.TIKITE_ORE);

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
        simpleBlockWithItem(ModBlocks.BLOCK_ALUMINIUM.get(),
                models().cubeAll(
                        ModBlocks.BLOCK_ALUMINIUM.getId().getPath(),
                        modLoc("block/block_aluminium")
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
        simpleBlockWithItem(ModBlocks.BRICK_JUNGLE_LAVA.get(),
                models().cubeAll(
                        ModBlocks.BRICK_JUNGLE_LAVA.getId().getPath(),
                        modLoc("block/brick_jungle_lava")
                )
        );
        // brick_red — порт BlockRedBrick (1.7.10): одна грань (RED_FACE) красная
        // (вертикальная — brick_red, горизонтальная — brick_red_top), остальные
        // серые brick_base. NONE — все грани серые (оболочка/meta-комната).
        ModelFile brickRedNone = models().cubeAll("block/brick_red_none", modLoc("block/brick_base"));
        ResourceLocation bb = modLoc("block/brick_base");
        ResourceLocation red = modLoc("block/brick_red"), redTop = modLoc("block/brick_red_top");
        // Ванильный шаблон block/cube НЕ определяет "particle" (в отличие от cube_all) —
        // без явной текстуры партиклы разрушения будут missing tex.
        ModelFile brickRedDown = models().cube("block/brick_red_down", redTop, bb, bb, bb, bb, bb).texture("particle", redTop);
        ModelFile brickRedUp = models().cube("block/brick_red_up", bb, redTop, bb, bb, bb, bb).texture("particle", redTop);
        ModelFile brickRedNorth = models().cube("block/brick_red_north", bb, bb, red, bb, bb, bb).texture("particle", red);
        ModelFile brickRedSouth = models().cube("block/brick_red_south", bb, bb, bb, red, bb, bb).texture("particle", red);
        ModelFile brickRedEast = models().cube("block/brick_red_east", bb, bb, bb, bb, red, bb).texture("particle", red);
        ModelFile brickRedWest = models().cube("block/brick_red_west", bb, bb, bb, bb, bb, red).texture("particle", red);
        VariantBlockStateBuilder brickRedBuilder = getVariantBuilder(ModBlocks.BRICK_RED.get());
        brickRedBuilder.partialState().with(RedBrickBlock.RED_FACE, RedFace.NONE).setModels(ConfiguredModel.builder().modelFile(brickRedNone).build());
        brickRedBuilder.partialState().with(RedBrickBlock.RED_FACE, RedFace.DOWN).setModels(ConfiguredModel.builder().modelFile(brickRedDown).build());
        brickRedBuilder.partialState().with(RedBrickBlock.RED_FACE, RedFace.UP).setModels(ConfiguredModel.builder().modelFile(brickRedUp).build());
        brickRedBuilder.partialState().with(RedBrickBlock.RED_FACE, RedFace.NORTH).setModels(ConfiguredModel.builder().modelFile(brickRedNorth).build());
        brickRedBuilder.partialState().with(RedBrickBlock.RED_FACE, RedFace.SOUTH).setModels(ConfiguredModel.builder().modelFile(brickRedSouth).build());
        brickRedBuilder.partialState().with(RedBrickBlock.RED_FACE, RedFace.WEST).setModels(ConfiguredModel.builder().modelFile(brickRedWest).build());
        brickRedBuilder.partialState().with(RedBrickBlock.RED_FACE, RedFace.EAST).setModels(ConfiguredModel.builder().modelFile(brickRedEast).build());
        simpleBlockItem(ModBlocks.BRICK_RED.get(), brickRedNone);
        simpleBlock(ModBlocks.BROADCASTER_PC.get(),
                models().getExistingFile(modLoc("block/machines/broadcaster_pc")));
        simpleBlockItem(ModBlocks.BROADCASTER_PC.get(),
                models().getExistingFile(modLoc("block/machines/broadcaster_pc")));
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
        // Конвейеры: blockstate/модели остаются ручными (свойства facing+bend, element-модели network/)
        craneRouter1to1();
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
        simpleBlockWithItem(ModBlocks.DEPTH_DNT.get(),
                models().cubeAll(
                        ModBlocks.DEPTH_DNT.getId().getPath(),
                        modLoc("block/depth_dnt")
                )
        );
        simpleBlockWithItem(ModBlocks.DFC_CORE.get(),
                models().cubeAll(
                        ModBlocks.DFC_CORE.getId().getPath(),
                        modLoc("block/dfc_core")
                )
        );
        // Der Stabilisator schiesst in seine Blickrichtung, das Modell dreht sich mit.
        directionalBlock(ModBlocks.DFC_STABILIZER.get(),
                models().cubeAll("dfc_stabilizer", modLoc("block/dfc_stabilizer")));
        simpleBlockItem(ModBlocks.DFC_STABILIZER.get(),
                models().getExistingFile(modLoc("block/dfc_stabilizer")));
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
        fenceBlockWithItem(ModBlocks.FENCE_METAL, "block/fence_metal");
        // одиночный столб chainlink-забора
        ModelFile postModel = models().withExistingParent(ModBlocks.FENCE_METAL_POST.getId().getPath(), mcLoc("block/fence_post"))
                .texture("texture", modLoc("block/fence_metal_post"));
        VariantBlockStateBuilder postBuilder = getVariantBuilder(ModBlocks.FENCE_METAL_POST.get());
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            postBuilder.partialState().with(com.hbm_m.block.decorations.DecorShapeBlock.FACING, facing)
                    .modelForState().modelFile(postModel).addModel();
        }
        simpleBlockItem(ModBlocks.FENCE_METAL_POST.get(), postModel);
        simpleBlockWithItem(ModBlocks.FIELD_DISTURBER.get(),
                models().cubeAll(
                        ModBlocks.FIELD_DISTURBER.getId().getPath(),
                        modLoc("block/field_disturber")
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
        simpleBlockWithItem(ModBlocks.FROZEN_LOG.get(),
                models().cubeBottomTop(
                        ModBlocks.FROZEN_LOG.getId().getPath(),
                        modLoc("block/frozen_log"),
                        modLoc("block/frozen_log"),
                        modLoc("block/frozen_log_top")
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
        // 1:1 aus FusionHatch.getIcon: oben/unten Wolframblock, die Vorderseite (= FACING) traegt
        // die Lukentextur, alle uebrigen Seiten die Heizerwand. Vorher lag die Lukentextur auf allen
        // sechs Flaechen, weil der Block noch ein richtungsloser Platzhalter war.
        ModelFile fusionHatchModel = models().orientable(
                ModBlocks.FUSION_HATCH.getId().getPath(),
                modLoc("block/fusion_heater_side"),
                modLoc("block/fusion_hatch"),
                modLoc("block/block_tungsten"));
        horizontalBlock(ModBlocks.FUSION_HATCH.get(), fusionHatchModel);
        simpleBlockItem(ModBlocks.FUSION_HATCH.get(), fusionHatchModel);
        // Original ist ein BlockPillar: Deckeltextur an beiden Enden der Achse, Seitentextur rundum,
        // Achse nach der angeklickten Flaeche. Vorher ein fester Wuerfel ohne Achse.
        axisBlock((net.minecraft.world.level.block.RotatedPillarBlock) ModBlocks.FUSION_HEATER.get(),
                modLoc("block/fusion_heater_side"),
                modLoc("block/fusion_heater_top"));
        simpleBlockItem(ModBlocks.FUSION_HEATER.get(),
                models().getExistingFile(modLoc("block/" + ModBlocks.FUSION_HEATER.getId().getPath())));
        // Газовые блоки невидимы (см. com.hbm_m.block.gas.BlockGasBase): пустая модель + дымовые частицы.
        // Die Gasbloecke sind im Original unsichtbar (BlockGasBase.getRenderType() == -1):
        // leeres Modell, das Item-Modell traegt die Textur. Einzige Ausnahme ist Chlorgas.
        for (RegistrySupplier<Block> gas : List.of(
                ModBlocks.GAS_ASBESTOS, ModBlocks.GAS_COAL, ModBlocks.GAS_EXPLOSIVE,
                ModBlocks.GAS_FLAMMABLE, ModBlocks.GAS_MELTDOWN, ModBlocks.GAS_MONOXIDE,
                ModBlocks.GAS_RADON, ModBlocks.GAS_RADON_DENSE, ModBlocks.GAS_RADON_TOMB)) {
            simpleBlock(gas.get(), models().getExistingFile(modLoc("block/invisible_gas")));
        }
        // Teilchenbeschleuniger: Quelle und Detektor. Die uebrigen vier Bauteile hatten als
        // Platzhalter schon Eintraege weiter unten.
        simpleBlockWithItem(ModBlocks.PA_SOURCE.get(),
                models().cubeAll(ModBlocks.PA_SOURCE.getId().getPath(), modLoc("block/block_steel")));
        simpleBlockWithItem(ModBlocks.PA_DETECTOR.get(),
                models().cubeAll(ModBlocks.PA_DETECTOR.getId().getPath(), modLoc("block/machine_detector")));
        simpleBlockWithItem(ModBlocks.YELLOW_BARREL.get(),
                models().cubeAll(ModBlocks.YELLOW_BARREL.getId().getPath(), modLoc("block/barrel_yellow")));
        // Die Ladeplatte nutzt im Original die Stahlblock-Textur (die Teslaspule hat weiter unten
        // schon einen eigenen Eintrag mit passenderen Texturen).
        // Das Ladegeraet haengt an der Wand und hat eine Blickrichtung - das Modell muss mitdrehen,
        // sonst passt die Hitbox nicht zum Bild.
        {
            var chargerModel = models().cubeAll(
                    ModBlocks.CHARGER.getId().getPath(), modLoc("block/block_steel"));
            horizontalBlock(ModBlocks.CHARGER.get(), chargerModel);
            simpleBlockItem(ModBlocks.CHARGER.get(), chargerModel);
        }
        // Original: der Grosstank nutzt ebenfalls die Stahlblock-Textur.
        simpleBlockWithItem(ModBlocks.MACHINE_BIGASSTANK.get(),
                models().cubeAll(ModBlocks.MACHINE_BIGASSTANK.getId().getPath(), modLoc("block/block_steel")));
        // Original: der RTG ist ein schlichter Wuerfel mit der Textur "rtg" auf allen Seiten.
        simpleBlockWithItem(ModBlocks.MACHINE_RTG.get(),
                models().cubeAll(ModBlocks.MACHINE_RTG.getId().getPath(), modLoc("block/rtg")));
        simpleBlockWithItem(ModBlocks.GLYPHID_BASE.get(),
                models().cubeAll(
                        ModBlocks.GLYPHID_BASE.getId().getPath(),
                        modLoc("block/glyphid_base")
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
        // Das Steuerpult zeigt mit der bedruckten Seite nach vorn.
        horizontalBlock(ModBlocks.ICF_CONTROLLER.get(), models().orientableWithBottom("icf_controller",
                modLoc("block/icf_casing"), modLoc("block/icf_controller"),
                modLoc("block/icf_casing"), modLoc("block/icf_casing")));
        simpleBlockItem(ModBlocks.ICF_CONTROLLER.get(), models().getExistingFile(modLoc("block/icf_controller")));
        simpleBlockWithItem(ModBlocks.ITER.get(),
                models().cubeAll(
                        ModBlocks.ITER.getId().getPath(),
                        modLoc("block/iter")
                )
        );
        ladderBlockWithItem(ModBlocks.LADDER_ALUMINIUM.get(), "block/ladder_aluminium");
        ladderBlockWithItem(ModBlocks.LADDER_COBALT.get(), "block/ladder_cobalt");
        ladderBlockWithItem(ModBlocks.LADDER_COPPER.get(), "block/ladder_copper");
        ladderBlockWithItem(ModBlocks.LADDER_GOLD.get(), "block/ladder_gold");
        ladderBlockWithItem(ModBlocks.LADDER_IRON.get(), "block/ladder_iron");
        ladderBlockWithItem(ModBlocks.LADDER_LEAD.get(), "block/ladder_lead");
        ladderBlockWithItem(ModBlocks.LADDER_STEEL.get(), "block/ladder_steel");
        ladderBlockWithItem(ModBlocks.LADDER_STURDY.get(), "block/ladder_sturdy");
        ladderBlockWithItem(ModBlocks.LADDER_TITANIUM.get(), "block/ladder_titanium");
        ladderBlockWithItem(ModBlocks.LADDER_TUNGSTEN.get(), "block/ladder_tungsten");
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
        // Das Steuerpult ist im Original nur halb hoch ({@code setBlockBounds(0,0,0, 1,0.5F,1)}),
        // darum ein eigenes Modell statt eines vollen Wuerfels - sonst passte die Hitbox nicht zum Bild.
        simpleBlockWithItem(ModBlocks.MACHINE_CONTROLLER.get(),
                models().withExistingParent(ModBlocks.MACHINE_CONTROLLER.getId().getPath(), "block/block")
                        .texture("particle", modLoc("block/machine_controller_side"))
                        .texture("side", modLoc("block/machine_controller_side"))
                        .texture("top", modLoc("block/machine_controller_top"))
                        .element()
                                .from(0, 0, 0).to(16, 8, 16)
                                .face(net.minecraft.core.Direction.DOWN).texture("#top").end()
                                .face(net.minecraft.core.Direction.UP).texture("#top").end()
                                .face(net.minecraft.core.Direction.NORTH).texture("#side").end()
                                .face(net.minecraft.core.Direction.SOUTH).texture("#side").end()
                                .face(net.minecraft.core.Direction.WEST).texture("#side").end()
                                .face(net.minecraft.core.Direction.EAST).texture("#side").end()
                        .end()
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
        // MachineBatteryBlock (FACING-Blockstate) statt simpleBlockWithItem - siehe orientableBlockWithItem-Aufrufe fuer MACHINE_BATTERY etc.
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
        {
            // Multiblock mit Ausrichtung: das Modell dreht sich mit der Struktur.
            var purex = models().cubeAll(ModBlocks.PUREX.getId().getPath(), modLoc("block/machine/purex"));
            horizontalBlock(ModBlocks.PUREX.get(), purex);
            simpleBlockItem(ModBlocks.PUREX.get(), purex);
        }
        simpleBlockWithItem(ModBlocks.INDUSTRIAL_GENERATOR.get(),
                models().cubeAll(
                        ModBlocks.INDUSTRIAL_GENERATOR.getId().getPath(),
                        modLoc("block/block_steel_machine")
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
        simpleBlockWithItem(ModBlocks.MACHINE_MISSILE_ASSEMBLY.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_MISSILE_ASSEMBLY.getId().getPath(),
                        modLoc("block/machine_missile_assembly")
                )
        );
        {
            // Multiblock mit Ausrichtung: das Modell dreht sich jetzt mit der Struktur.
            var drain = models().cubeAll(ModBlocks.MACHINE_DRAIN.getId().getPath(), modLoc("block/concrete"));
            horizontalBlock(ModBlocks.MACHINE_DRAIN.get(), drain);
            simpleBlockItem(ModBlocks.MACHINE_DRAIN.get(), drain);
        }
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
        simpleBlockWithItem(ModBlocks.MACHINE_TRANSFORMER.get(),
                models().cubeBottomTop(
                        ModBlocks.MACHINE_TRANSFORMER.getId().getPath(),
                        modLoc("block/machine_transformer_iron"),
                        modLoc("block/machine_transformer_top_iron"),
                        modLoc("block/machine_transformer_top_iron")
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
        // 1:1 BlockCraneBase.getIcon/getRotationFromSide je Eingang/Ausgang
        craneBlock1to1(ModBlocks.CRANE_INSERTER, com.hbm_m.block.network.CraneTextures.standard("crane_in", false));
        craneBlock1to1(ModBlocks.CRANE_EXTRACTOR, com.hbm_m.block.network.CraneTextures.standard("crane_out", true));
        craneBlock1to1(ModBlocks.CRANE_GRABBER, new com.hbm_m.block.network.CraneTextures.Set("crane_top", "crane_side", "crane_pull", "crane_side_pull", "crane_out", "crane_side_out", "crane_grabber", false));
        craneBlock1to1(ModBlocks.CRANE_BOXER, new com.hbm_m.block.network.CraneTextures.Set("crane_top", "crane_side", "crane_in", "crane_side_in", "crane_box", "crane_side_box", "crane_boxer", false));
        craneBlock1to1(ModBlocks.CRANE_UNBOXER, new com.hbm_m.block.network.CraneTextures.Set("crane_top", "crane_side", "crane_in", "crane_side_in", "crane_box", "crane_side_box", "crane_unboxer", true));
        simpleBlockWithItem(ModBlocks.MACHINE_SATLINKER.get(),
                models().cubeBottomTop(
                        ModBlocks.MACHINE_SATLINKER.getId().getPath(),
                        modLoc("block/machine_satlinker_side"),
                        modLoc("block/machine_satlinker_side"),
                        modLoc("block/machine_satlinker_top")
                )
        );
        simpleBlockWithItem(ModBlocks.MACHINE_STORAGE_DRUM.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_STORAGE_DRUM.getId().getPath(),
                        modLoc("block/machine_storage_drum")
                )
        );
        // Die vier Groessen des Originals - jede mit ihrem eigenen Texturzusatz.
        for (var entry : java.util.List.of(
                java.util.Map.entry(ModBlocks.MASS_STORAGE, ""),
                java.util.Map.entry(ModBlocks.MASS_STORAGE_WOOD, "_wood"),
                java.util.Map.entry(ModBlocks.MASS_STORAGE_IRON, "_iron"),
                java.util.Map.entry(ModBlocks.MASS_STORAGE_DESH, "_desh"))) {

            var storage = entry.getKey();
            String suffix = entry.getValue();

            simpleBlockWithItem(storage.get(),
                    models().cubeBottomTop(
                            storage.getId().getPath(),
                            modLoc("block/mass_storage_side" + suffix),
                            modLoc("block/mass_storage_side" + suffix),
                            modLoc("block/mass_storage_top" + suffix)
                    )
            );
        }
        simpleBlockWithItem(ModBlocks.METEOR_SPAWNER.get(),
                models().cubeBottomTop(
                        ModBlocks.METEOR_SPAWNER.getId().getPath(),
                        modLoc("block/meteor_spawner_side"),
                        modLoc("block/meteor_spawner_side"),
                        modLoc("block/meteor_spawner_top")
                )
        );
        simpleBlockWithItem(ModBlocks.MINE_HE.get(), models().getExistingFile(modLoc("block/bomb/mine_he")));
        simpleBlockWithItem(ModBlocks.MINE_SHRAP.get(), models().getExistingFile(modLoc("block/bomb/mine_shrap")));
        customBombBlock(ModBlocks.NUKE_N2);
        customBombBlock(ModBlocks.NUKE_SOLINIUM);
        customBombBlock(ModBlocks.NUKE_FSTBMB);
        customBombBlock(ModBlocks.NUKE_CUSTOM);
        customBombBlock(ModBlocks.BOMB_MULTI);
        layerBlockWithItem(ModBlocks.OIL_SPILL, "block/oil_spill");
        // Пьедестал — порт RenderPedestal (1.7.10 ISBRH): нижняя плита (0-4px),
        // колонна 12x12 (4-12px), верхняя плита (12-16px); между плитами
        // сквозной вырез, в котором парит предмет.
        ModelFile pedestalModel = models().getBuilder(ModBlocks.PEDESTAL.getId().getPath())
                .texture("top", modLoc("block/pedestal_top"))
                .texture("side", modLoc("block/pedestal_side"))
                .element().from(0.0F, 0.0F, 0.0F).to(16.0F, 4.0F, 16.0F)
                        .allFaces((dir, face) -> face.texture(dir.getAxis() == Direction.Axis.Y ? "#top" : "#side"))
                .end()
                .element().from(2.0F, 4.0F, 2.0F).to(14.0F, 12.0F, 14.0F)
                        .allFaces((dir, face) -> face.texture(dir.getAxis() == Direction.Axis.Y ? "#top" : "#side"))
                .end()
                .element().from(0.0F, 12.0F, 0.0F).to(16.0F, 16.0F, 16.0F)
                        .allFaces((dir, face) -> face.texture(dir.getAxis() == Direction.Axis.Y ? "#top" : "#side"))
                .end()
                .texture("particle", modLoc("block/pedestal_side"));
        simpleBlock(ModBlocks.PEDESTAL.get(), pedestalModel);
        simpleBlockItem(ModBlocks.PEDESTAL.get(), pedestalModel);
        simpleBlockWithItem(ModBlocks.PLASMA_HEATER.get(),
                models().cubeAll(
                        ModBlocks.PLASMA_HEATER.getId().getPath(),
                        modLoc("block/plasma_heater")
                )
        );
        pneumoTubeState();
        // Die bemalbare Fassung ist ein Vollblock und traegt dieselben sechs Anschlussmerkmale
        // wie das Rohr - gezeichnet wird sie im Renderer, das Modell dient nur dem Gegenstand.
        {
            var paintable = ModBlocks.PNEUMATIC_TUBE_PAINTABLE.get();
            var model = models().cubeAll("pneumatic_tube_paintable", modLoc("block/pneumatic_tube_paintable"));

            getVariantBuilder(paintable).forAllStates(state ->
                    net.minecraftforge.client.model.generators.ConfiguredModel.builder()
                            .modelFile(model).build());

            simpleBlockItem(paintable, model);
        }
        simpleBlockWithItem(ModBlocks.PRESS_PREHEATER.get(),
                models().cubeAll(
                        ModBlocks.PRESS_PREHEATER.getId().getPath(),
                        modLoc("block/press_preheater")
                )
        );
        // R7r: 1:1 PWR - Saeulen (BlockPillarPWR), einfache Bauteile, Controller mit pwr_casing_blank, Traeger mit CT
        for (var p : java.util.List.of(ModBlocks.PWR_FUEL, ModBlocks.PWR_CONTROL, ModBlocks.PWR_CHANNEL)) {
            String n = p.getId().getPath();
            axisBlock((net.minecraft.world.level.block.RotatedPillarBlock) p.get(), modLoc("block/" + n + "_side"), modLoc("block/" + n + "_top"));
            simpleBlockItem(p.get(), models().getExistingFile(modLoc("block/" + n)));
        }
        for (var p : java.util.List.of(ModBlocks.PWR_HEATEX, ModBlocks.PWR_HEATSINK, ModBlocks.PWR_NEUTRON_SOURCE, ModBlocks.PWR_REFLECTOR, ModBlocks.PWR_CASING, ModBlocks.PWR_PORT)) {
            simpleBlockWithItem(p.get(), models().cubeAll(p.getId().getPath(), modLoc("block/" + p.getId().getPath())));
        }
        orientableBlockWithItem(ModBlocks.PWR_CONTROLLER,
                modLoc("block/pwr_casing_blank"),
                modLoc("block/pwr_controller"),
                modLoc("block/pwr_casing_blank")
        );
        getVariantBuilder(ModBlocks.PWR_BLOCK.get()).forAllStates(state -> {
            boolean port = state.getValue(com.hbm_m.block.machines.PWRBlock.PORT);
            String name = port ? "pwr_block_port" : "pwr_block";
            return net.minecraftforge.client.model.generators.ConfiguredModel.builder()
                    .modelFile(models().cubeAll(name, modLoc("block/" + (port ? "pwr_casing_port" : "pwr_block"))))
                    .build();
        });
        {
            var telex = models().cubeAll(
                    ModBlocks.RADIO_TELEX.getId().getPath(), modLoc("block/radio_telex"));
            horizontalBlock(ModBlocks.RADIO_TELEX.get(), telex);
            simpleBlockItem(ModBlocks.RADIO_TELEX.get(), telex);
        }
        simpleBlockWithItem(ModBlocks.RADIOBOX.get(),
                models().cubeAll(
                        ModBlocks.RADIOBOX.getId().getPath(),
                        modLoc("block/radiobox")
                )
        );
        customObjBlockRotated(ModBlocks.RADIOREC, Map.of(
                Direction.NORTH, 180, Direction.SOUTH, 0, Direction.WEST, 90, Direction.EAST, 270));
        simpleBlockWithItem(ModBlocks.RADIO_AUTOCAL.get(),
                models().cubeAll(
                        ModBlocks.RADIO_AUTOCAL.getId().getPath(),
                        modLoc("block/radio_autocal")
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

        // Декор-лестницы/плиты (родительские модели созданы в общей секции выше)
        stairsBlock((StairBlock) ModBlocks.BRICK_COMPOUND_STAIRS.get(), modLoc("block/brick_compound"));
        simpleBlockItem(ModBlocks.BRICK_COMPOUND_STAIRS.get(), models().getExistingFile(modLoc("block/brick_compound_stairs")));
        stairsBlock((StairBlock) ModBlocks.REINFORCED_BRICK_STAIRS.get(), modLoc("block/reinforced_brick"));
        simpleBlockItem(ModBlocks.REINFORCED_BRICK_STAIRS.get(), models().getExistingFile(modLoc("block/reinforced_brick_stairs")));
        stairsBlock((StairBlock) ModBlocks.LIGHTSTONE_BRICKS_STAIRS.get(), modLoc("block/lightstone_bricks"));
        simpleBlockItem(ModBlocks.LIGHTSTONE_BRICKS_STAIRS.get(), models().getExistingFile(modLoc("block/lightstone_bricks_stairs")));
        slabBlock((SlabBlock) ModBlocks.REINFORCED_BRICK_SLAB.get(),
                blockTexture(ModBlocks.REINFORCED_BRICK.get()),
                modLoc("block/reinforced_brick"));
        simpleBlockItem(ModBlocks.REINFORCED_BRICK_SLAB.get(), models().getExistingFile(modLoc("block/reinforced_brick_slab")));
        slabBlock((SlabBlock) ModBlocks.BRICK_COMPOUND_SLAB.get(),
                blockTexture(ModBlocks.BRICK_COMPOUND.get()),
                modLoc("block/brick_compound"));
        simpleBlockItem(ModBlocks.BRICK_COMPOUND_SLAB.get(), models().getExistingFile(modLoc("block/brick_compound_slab")));

        simpleBlockWithItem(ModBlocks.REINFORCED_DUCRETE.get(),
                models().cubeAll(
                        ModBlocks.REINFORCED_DUCRETE.getId().getPath(),
                        modLoc("block/reinforced_ducrete")
                )
        );
        // Порт BlockNTMGlassPane: ванильная панель (Post/Side/NoSide)
        paneBlock((net.minecraft.world.level.block.IronBarsBlock) ModBlocks.REINFORCED_GLASS_PANE.get(),
                modLoc("block/reinforced_glass_pane"), modLoc("block/reinforced_glass_pane_edge"));
        simpleBlockItem(ModBlocks.REINFORCED_GLASS_PANE.get(), models().getExistingFile(modLoc("block/reinforced_glass_pane_post")));
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
        simpleBlockWithItem(ModBlocks.SAND_BORON.get(),
                models().cubeAll(
                        ModBlocks.SAND_BORON.getId().getPath(),
                        modLoc("block/sand_boron")
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
        customMachineBlock(ModBlocks.SAT_DOCK);
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
        simpleBlockWithItem(ModBlocks.SOYUZ_CAPSULE.get(),
                models().cubeAll(
                        ModBlocks.SOYUZ_CAPSULE.getId().getPath(),
                        modLoc("block/soyuz_capsule")
                )
        );
        customObjBlock(ModBlocks.SOYUZ_LAUNCHER);
        customObjBlock(ModBlocks.DECO_SOYUZ_ROCKET);
        // Порт BlockScaffold: OBJ-панель 2/16..14/16, AXIS-ориентация (модель-обёртка в resources)
        // RBMK support/deco blocks (1:1 with com.hbm.blocks.ModBlocks - deco_rbmk reuses
        // rbmk/rbmk_top, deco_rbmk_smooth reuses rbmk/rbmk_blank_top).
        // These four have textures of their own in CE; the first two were borrowing the RBMK
        // column's top faces as stand-ins and the panelled pair did not exist at all.
        simpleBlockWithItem(ModBlocks.DECO_RBMK.get(),
                models().cubeAll(ModBlocks.DECO_RBMK.getId().getPath(), modLoc("block/deco_rbmk")));
        simpleBlockWithItem(ModBlocks.DECO_RBMK_SMOOTH.get(),
                models().cubeAll(ModBlocks.DECO_RBMK_SMOOTH.getId().getPath(), modLoc("block/deco_rbmk_smooth")));
        simpleBlockWithItem(ModBlocks.DECO_RBMK_PANEL.get(),
                models().cubeAll(ModBlocks.DECO_RBMK_PANEL.getId().getPath(), modLoc("block/deco_rbmk_panel")));
        simpleBlockWithItem(ModBlocks.DECO_RBMK_SMOOTH_PANEL.get(),
                models().cubeAll(ModBlocks.DECO_RBMK_SMOOTH_PANEL.getId().getPath(), modLoc("block/deco_rbmk_panel_smooth")));

        // CE's thin panel slabs: a 2px single and the 4px double it stacks into. The side faces use
        // a dedicated narrow strip texture, sampled from the bottom of the sheet so the panel's
        // edge lines up whichever height it is.
        rbmkPanelSlab(ModBlocks.DECO_RBMK_PANEL_SLAB2.get(), "deco_rbmk_panel_slab2",
                "deco_rbmk_panel", "deco_rbmk_panel_side", 2);
        rbmkPanelSlab(ModBlocks.DECO_RBMK_PANEL_SLAB4.get(), "deco_rbmk_panel_slab4",
                "deco_rbmk_panel", "deco_rbmk_panel_side", 4);
        rbmkPanelSlab(ModBlocks.DECO_RBMK_SMOOTH_PANEL_SLAB2.get(), "deco_rbmk_smooth_panel_slab2",
                "deco_rbmk_panel_smooth", "deco_rbmk_panel_smooth_side", 2);
        rbmkPanelSlab(ModBlocks.DECO_RBMK_SMOOTH_PANEL_SLAB4.get(), "deco_rbmk_smooth_panel_slab4",
                "deco_rbmk_panel_smooth", "deco_rbmk_panel_smooth_side", 4);
        simpleBlockWithItem(ModBlocks.BLOCK_GRAPHITE.get(),
                models().cubeAll(ModBlocks.BLOCK_GRAPHITE.getId().getPath(), modLoc("block/block_graphite")));
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
        // Ручные blockstates: у keyhole-блоков есть FACING — модель одна, повороты через варианты.
        // В 1.7.10 stone_keyhole: боковые стороны — текстура скважины, верх/низ — ванильный камень.
        ModelFile stoneKeyholeModel = models().cubeBottomTop(
                ModBlocks.STONE_KEYHOLE.getId().getPath(),
                modLoc("block/stone_keyhole"),
                mcLoc("block/stone"),
                mcLoc("block/stone"));
        horizontalBlock(ModBlocks.STONE_KEYHOLE.get(), stoneKeyholeModel);
        simpleBlockItem(ModBlocks.STONE_KEYHOLE.get(), stoneKeyholeModel);
        ModelFile stoneKeyholeMetaModel = models().withExistingParent(ModBlocks.STONE_KEYHOLE_META.getId().getPath(), mcLoc("block/orientable"))
                .texture("front", modLoc("block/stone_keyhole_meta"))
                .texture("side", modLoc("block/brick_base"))
                .texture("top", modLoc("block/brick_red_top"));
        horizontalBlock(ModBlocks.STONE_KEYHOLE_META.get(), stoneKeyholeMetaModel);
        simpleBlockItem(ModBlocks.STONE_KEYHOLE_META.get(), stoneKeyholeMetaModel);
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
        simpleBlockWithItem(ModBlocks.VACUUM.get(),
                models().cubeAll(
                        ModBlocks.VACUUM.getId().getPath(),
                        modLoc("block/vacuum")
                )
        );
        simpleBlockWithItem(ModBlocks.VENT_CHLORINE.get(), models().cubeBottomTop(ModBlocks.VENT_CHLORINE.getId().getPath(),
                modLoc("block/vent_chlorine"), modLoc("block/vent_blank"), modLoc("block/vent_blank")));
        simpleBlockWithItem(ModBlocks.VENT_CHLORINE_SEAL.get(),
                models().cubeBottomTop(
                        ModBlocks.VENT_CHLORINE_SEAL.getId().getPath(),
                        modLoc("block/vent_chlorine_seal_side"),
                        modLoc("block/vent_chlorine_seal_side"),
                        modLoc("block/vent_chlorine_seal_top")
                )
        );
        simpleBlockWithItem(ModBlocks.VENT_CLOUD.get(), models().cubeBottomTop(ModBlocks.VENT_CLOUD.getId().getPath(),
                modLoc("block/vent_cloud"), modLoc("block/vent_blank"), modLoc("block/vent_blank")));
        simpleBlockWithItem(ModBlocks.VENT_PINK_CLOUD.get(), models().cubeBottomTop(ModBlocks.VENT_PINK_CLOUD.getId().getPath(),
                modLoc("block/vent_pink_cloud"), modLoc("block/vent_blank"), modLoc("block/vent_blank")));
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
    /** R6a: Bloecke mit 1:1-Modellen (Saeulen, Schichten, Glas, Biomstein, Ueberbeton ...). */
    private void registerR6aBlocks() {
        simpleBlockWithItem(ModBlocks.BLOCK_SCRAP.get(), models().cubeAll("block_scrap", mcOrMod("block/block_scrap")));
        simpleBlockWithItem(ModBlocks.BLOCK_ELECTRICAL_SCRAP.get(), models().cubeAll("block_electrical_scrap", mcOrMod("block/electrical_scrap")));
        simpleBlockWithItem(ModBlocks.CONCRETE_SUPER_BROKEN.get(), models().cubeAll("concrete_super_broken", mcOrMod("block/concrete_super_broken")));
        simpleBlockWithItem(ModBlocks.ORE_OIL_SAND.get(), models().cubeAll("ore_oil_sand", mcOrMod("block/ore_oil_sand_alt")));
        simpleBlockWithItem(ModBlocks.DIRT_DEAD.get(), models().cubeAll("dirt_dead", mcOrMod("block/dirt_dead")));
        simpleBlockWithItem(ModBlocks.DIRT_OILY.get(), models().cubeAll("dirt_oily", mcOrMod("block/dirt_oily")));
        simpleBlockWithItem(ModBlocks.GRAVEL_DIAMOND.get(), models().cubeAll("gravel_diamond", mcOrMod("block/gravel_diamond")));
        simpleBlockWithItem(ModBlocks.GRAVEL_OBSIDIAN.get(), models().cubeAll("gravel_obsidian", mcOrMod("block/gravel_obsidian")));
        simpleBlockWithItem(ModBlocks.MOON_TURF.get(), models().cubeAll("moon_turf", mcOrMod("block/moon_turf")));
        simpleBlockWithItem(ModBlocks.SAND_DIRTY.get(), models().cubeAll("sand_dirty", mcOrMod("block/sand_dirty")));
        simpleBlockWithItem(ModBlocks.SAND_DIRTY_RED.get(), models().cubeAll("sand_dirty_red", mcOrMod("block/sand_dirty_red")));
        simpleBlockWithItem(ModBlocks.STONE_CRACKED.get(), models().cubeAll("stone_cracked", mcOrMod("block/stone_cracked")));
        simpleBlockWithItem(ModBlocks.ORE_OIL.get(), models().cubeAll("ore_oil", mcOrMod("block/ore_oil")));
        simpleBlockWithItem(ModBlocks.BLOCK_METEOR.get(), models().cubeAll("block_meteor", mcOrMod("block/meteor")));
        simpleBlockWithItem(ModBlocks.BLOCK_METEOR_COBBLE.get(), models().cubeAll("block_meteor_cobble", mcOrMod("block/meteor_cobble")));
        simpleBlockWithItem(ModBlocks.BLOCK_METEOR_BROKEN.get(), models().cubeAll("block_meteor_broken", mcOrMod("block/meteor_crushed")));
        simpleBlockWithItem(ModBlocks.DECO_TITANIUM.get(), models().cubeAll("deco_titanium", mcOrMod("block/deco_titanium")));
        simpleBlockWithItem(ModBlocks.WASTE_PLANKS.get(), models().cubeAll("waste_planks", mcOrMod("block/waste_planks")));
        simpleBlockWithItem(ModBlocks.FROZEN_DIRT.get(), models().cubeAll("frozen_dirt", mcOrMod("block/frozen_dirt")));
        simpleBlockWithItem(ModBlocks.FROZEN_PLANKS.get(), models().cubeAll("frozen_planks", mcOrMod("block/frozen_planks")));
        simpleBlockWithItem(ModBlocks.ORE_RARE.get(), models().cubeAll("ore_rare", mcOrMod("block/ported/ore_rare")));
        simpleBlockWithItem(ModBlocks.BRICK_JUNGLE_OOZE.get(), models().cubeAll("brick_jungle_ooze", mcOrMod("block/brick_jungle_ooze")));
        simpleBlockWithItem(ModBlocks.BRICK_JUNGLE_MYSTIC.get(), models().cubeAll("brick_jungle_mystic", mcOrMod("block/brick_jungle_mystic")));
        axisBlock((net.minecraft.world.level.block.RotatedPillarBlock) ModBlocks.METEOR_PILLAR.get(), mcOrMod("block/meteor_pillar"), mcOrMod("block/meteor_pillar_top"));
        simpleBlockItem(ModBlocks.METEOR_PILLAR.get(), models().getExistingFile(modLoc("block/meteor_pillar")));
        axisBlock((net.minecraft.world.level.block.RotatedPillarBlock) ModBlocks.CONCRETE_PILLAR.get(), mcOrMod("block/concrete_pillar_side"), mcOrMod("block/concrete_pillar_top"));
        simpleBlockItem(ModBlocks.CONCRETE_PILLAR.get(), models().getExistingFile(modLoc("block/concrete_pillar")));
        axisBlock((net.minecraft.world.level.block.RotatedPillarBlock) ModBlocks.BLOCK_EUPHEMIUM_CLUSTER.get(), mcOrMod("block/block_euphemium_cluster_side"), mcOrMod("block/block_euphemium_cluster_top"));
        simpleBlockItem(ModBlocks.BLOCK_EUPHEMIUM_CLUSTER.get(), models().getExistingFile(modLoc("block/block_euphemium_cluster")));
        axisBlock((net.minecraft.world.level.block.RotatedPillarBlock) ModBlocks.BLOCK_FIBERGLASS.get(), mcOrMod("block/block_fiberglass_side"), mcOrMod("block/block_fiberglass_top"));
        simpleBlockItem(ModBlocks.BLOCK_FIBERGLASS.get(), models().getExistingFile(modLoc("block/block_fiberglass")));
        axisBlock((net.minecraft.world.level.block.RotatedPillarBlock) ModBlocks.BLOCK_INSULATOR.get(), mcOrMod("block/block_insulator_side"), mcOrMod("block/block_insulator_top"));
        simpleBlockItem(ModBlocks.BLOCK_INSULATOR.get(), models().getExistingFile(modLoc("block/block_insulator")));
        axisBlock((net.minecraft.world.level.block.RotatedPillarBlock) ModBlocks.BLOCK_TRITIUM.get(), mcOrMod("block/block_tritium_side"), mcOrMod("block/block_tritium_top"));
        simpleBlockItem(ModBlocks.BLOCK_TRITIUM.get(), models().getExistingFile(modLoc("block/block_tritium")));
        simpleBlockWithItem(ModBlocks.GNEISS_CHISELED.get(), models().cubeColumn("gneiss_chiseled", mcOrMod("block/gneiss_chiseled"), mcOrMod("block/gneiss_tile")));
        simpleBlockWithItem(ModBlocks.BASALT.get(), models().cubeColumn("basalt", mcOrMod("block/basalt"), mcOrMod("block/basalt_top")));
        simpleBlockWithItem(ModBlocks.WATZ_COOLER.get(), models().cubeColumn("watz_cooler", mcOrMod("block/watz_cooler_side"), mcOrMod("block/watz_cooler_top")));
        simpleBlockWithItem(ModBlocks.WATZ_ELEMENT.get(), models().cubeColumn("watz_element", mcOrMod("block/watz_element_side"), mcOrMod("block/watz_element_top")));
        simpleBlockWithItem(ModBlocks.BLOCK_SMORE.get(), models().cubeColumn("block_smore", mcOrMod("block/block_smore_side"), mcOrMod("block/block_smore_top")));
        simpleBlockWithItem(ModBlocks.METEOR_BATTERY.get(), models().cubeColumn("meteor_battery", mcOrMod("block/meteor_spawner_side"), mcOrMod("block/meteor_power")));
        simpleBlockWithItem(ModBlocks.BLOCK_WASTE_PAINTED.get(), models().cubeAll("block_waste_painted", mcOrMod("block/block_waste_painted")));
        simpleBlockWithItem(ModBlocks.DECO_ASBESTOS.get(), models().cubeAll("deco_asbestos", mcOrMod("block/deco_asbestos")));
        simpleBlockWithItem(ModBlocks.WASTE_EARTH.get(), models().cubeBottomTop("waste_earth", mcOrMod("block/waste_grass_side"), mcOrMod("block/waste_earth_bottom"), mcOrMod("block/waste_grass_top")));
        simpleBlockWithItem(ModBlocks.FROZEN_GRASS.get(), models().cubeBottomTop("frozen_grass", mcOrMod("block/frozen_grass_side"), mcOrMod("block/frozen_dirt"), mcOrMod("block/frozen_grass_top")));
        simpleBlockWithItem(ModBlocks.BURNING_EARTH.get(), models().cubeBottomTop("burning_earth", mcOrMod("block/burning_grass_side"), mcOrMod("block/waste_earth_bottom"), mcOrMod("block/burning_grass_top")));
        simpleBlockWithItem(ModBlocks.IMPACT_DIRT.get(), models().cubeAll("impact_dirt", mcOrMod("block/waste_earth_bottom")));
        simpleBlockWithItem(ModBlocks.NTM_DIRT.get(), models().cubeAll("ntm_dirt", mcOrMod("minecraft:block/dirt")));
        { var b = ModBlocks.CONCRETE_SUPER.get(); var m0 = models().cubeAll("concrete_super", modLoc("block/concrete_super")); var ms = new net.minecraftforge.client.model.generators.ModelFile[4]; for (int q = 0; q < 4; q++) ms[q] = models().cubeAll("concrete_super_m" + q, modLoc("block/concrete_super_m" + q)); getVariantBuilder(b).forAllStates(st -> { int a = st.getValue(com.hbm_m.block.generic.BlockUberConcrete.AGE); var mf = a == 15 ? ms[3] : a == 14 ? ms[2] : a > 11 ? ms[1] : a > 9 ? ms[0] : m0; return net.minecraftforge.client.model.generators.ConfiguredModel.builder().modelFile(mf).build(); }); simpleBlockItem(b, m0); }
        simpleBlockWithItem(ModBlocks.BRICK_CONCRETE_MARKED.get(), models().cubeColumn("brick_concrete_marked", mcOrMod("block/brick_concrete_marked"), mcOrMod("block/brick_concrete")));
        { var m = models().cross("plant_dead", mcOrMod("block/plant_dead_generic")).renderType("cutout"); simpleBlock(ModBlocks.PLANT_DEAD.get(), m); itemModels().withExistingParent("plant_dead", "item/generated").texture("layer0", mcOrMod("block/plant_dead_generic")); }
        { var m = models().cross("plant_dead_grass", mcOrMod("block/plant_dead_grass")).renderType("cutout"); simpleBlock(ModBlocks.PLANT_DEAD_GRASS.get(), m); itemModels().withExistingParent("plant_dead_grass", "item/generated").texture("layer0", mcOrMod("block/plant_dead_grass")); }
        { var m = models().cross("plant_dead_flower", mcOrMod("block/plant_dead_flower")).renderType("cutout"); simpleBlock(ModBlocks.PLANT_DEAD_FLOWER.get(), m); itemModels().withExistingParent("plant_dead_flower", "item/generated").texture("layer0", mcOrMod("block/plant_dead_flower")); }
        { var m = models().cross("plant_dead_bigflower", mcOrMod("block/plant_dead_bigflower")).renderType("cutout"); simpleBlock(ModBlocks.PLANT_DEAD_BIGFLOWER.get(), m); itemModels().withExistingParent("plant_dead_bigflower", "item/generated").texture("layer0", mcOrMod("block/plant_dead_bigflower")); }
        { var m = models().cross("plant_dead_fern", mcOrMod("block/plant_dead_fern")).renderType("cutout"); simpleBlock(ModBlocks.PLANT_DEAD_FERN.get(), m); itemModels().withExistingParent("plant_dead_fern", "item/generated").texture("layer0", mcOrMod("block/plant_dead_fern")); }
        simpleBlockWithItem(ModBlocks.GLASS_BORON.get(), models().cubeAll("glass_boron", mcOrMod("block/glass_boron")).renderType("cutout"));
        simpleBlockWithItem(ModBlocks.GLASS_LEAD.get(), models().cubeAll("glass_lead", mcOrMod("block/glass_lead")).renderType("cutout"));
        simpleBlockWithItem(ModBlocks.GLASS_URANIUM.get(), models().cubeAll("glass_uranium", mcOrMod("block/glass_uranium")).renderType("translucent"));
        simpleBlockWithItem(ModBlocks.GLASS_TRINITITE.get(), models().cubeAll("glass_trinitite", mcOrMod("block/glass_trinitite")).renderType("translucent"));
        simpleBlockWithItem(ModBlocks.GLASS_POLONIUM.get(), models().cubeAll("glass_polonium", mcOrMod("block/glass_polonium")).renderType("translucent"));
        simpleBlockWithItem(ModBlocks.GLASS_ASH.get(), models().cubeAll("glass_ash", mcOrMod("block/glass_ash")).renderType("translucent"));
        simpleBlockWithItem(ModBlocks.GLASS_QUARTZ.get(), models().cubeAll("glass_quartz", mcOrMod("block/glass_quartz")).renderType("cutout"));
        simpleBlockWithItem(ModBlocks.GLASS_POLARIZED.get(), models().cubeAll("glass_polarized", mcOrMod("block/glass_polarized")).renderType("cutout"));
        simpleBlockWithItem(ModBlocks.REINFORCED_LAMINATE.get(), models().cubeAll("reinforced_laminate", mcOrMod("block/reinforced_laminate")).renderType("translucent"));
        { var b = ModBlocks.STONE_BIOME_DESERT.get(); var open = models().cubeBottomTop("stone_biome_desert", mcOrMod("block/stone_biome_layer_desert"), mcOrMod("block/stone_biome_top_desert"), mcOrMod("block/stone_biome_top_desert")); var cov = models().cubeBottomTop("stone_biome_desert_covered", mcOrMod("block/stone_biome_desert"), mcOrMod("block/stone_biome_top_desert"), mcOrMod("block/stone_biome_top_desert")); getVariantBuilder(b).forAllStates(st -> net.minecraftforge.client.model.generators.ConfiguredModel.builder().modelFile(st.getValue(com.hbm_m.block.generic.BlockBiomeStone.COVERED) ? cov : open).build()); simpleBlockItem(b, open); }
        { var b = ModBlocks.STONE_BIOME_WOODLAND.get(); var open = models().cubeBottomTop("stone_biome_woodland", mcOrMod("block/stone_biome_layer_woodland"), mcOrMod("block/stone_biome_top_woodland"), mcOrMod("block/stone_biome_top_woodland")); var cov = models().cubeBottomTop("stone_biome_woodland_covered", mcOrMod("block/stone_biome_woodland"), mcOrMod("block/stone_biome_top_woodland"), mcOrMod("block/stone_biome_top_woodland")); getVariantBuilder(b).forAllStates(st -> net.minecraftforge.client.model.generators.ConfiguredModel.builder().modelFile(st.getValue(com.hbm_m.block.generic.BlockBiomeStone.COVERED) ? cov : open).build()); simpleBlockItem(b, open); }
        simpleBlockWithItem(ModBlocks.BLOCK_COKE_COAL.get(), models().cubeAll("block_coke_coal", mcOrMod("block/block_coke_coal")));
        simpleBlockWithItem(ModBlocks.BLOCK_COKE_LIGNITE.get(), models().cubeAll("block_coke_lignite", mcOrMod("block/block_coke_lignite")));
        simpleBlockWithItem(ModBlocks.BLOCK_COKE_PETROLEUM.get(), models().cubeAll("block_coke_petroleum", mcOrMod("block/block_coke_petroleum")));
        simpleBlockWithItem(ModBlocks.BLOCK_CAP_NUKA.get(), models().cubeBottomTop("block_cap_nuka", mcOrMod("block/block_cap_nuka"), mcOrMod("block/block_cap_nuka_top"), mcOrMod("block/block_cap_nuka_top")));
        simpleBlockWithItem(ModBlocks.BLOCK_CAP_QUANTUM.get(), models().cubeBottomTop("block_cap_quantum", mcOrMod("block/block_cap_quantum"), mcOrMod("block/block_cap_quantum_top"), mcOrMod("block/block_cap_quantum_top")));
        simpleBlockWithItem(ModBlocks.BLOCK_CAP_SPARKLE.get(), models().cubeBottomTop("block_cap_sparkle", mcOrMod("block/block_cap_sparkle"), mcOrMod("block/block_cap_sparkle_top"), mcOrMod("block/block_cap_sparkle_top")));
        simpleBlockWithItem(ModBlocks.BLOCK_CAP_RAD.get(), models().cubeBottomTop("block_cap_rad", mcOrMod("block/block_cap_rad"), mcOrMod("block/block_cap_rad_top"), mcOrMod("block/block_cap_rad_top")));
        simpleBlockWithItem(ModBlocks.BLOCK_CAP_KORL.get(), models().cubeBottomTop("block_cap_korl", mcOrMod("block/block_cap_korl"), mcOrMod("block/block_cap_korl_top"), mcOrMod("block/block_cap_korl_top")));
        simpleBlockWithItem(ModBlocks.BLOCK_CAP_FRITZ.get(), models().cubeBottomTop("block_cap_fritz", mcOrMod("block/block_cap_fritz"), mcOrMod("block/block_cap_fritz_top"), mcOrMod("block/block_cap_fritz_top")));
        simpleBlockWithItem(ModBlocks.PLATEMETAL_BASE.get(), models().cubeAll("platemetal_base", mcOrMod("block/platemetal_base")));
        simpleBlockWithItem(ModBlocks.PLATEMETAL_BLACK.get(), models().cubeAll("platemetal_black", mcOrMod("block/platemetal_black")));
        simpleBlockWithItem(ModBlocks.PLATEMETAL_WHITE.get(), models().cubeAll("platemetal_white", mcOrMod("block/platemetal_white")));
        simpleBlockWithItem(ModBlocks.PLATEMETAL_RED.get(), models().cubeAll("platemetal_red", mcOrMod("block/platemetal_red")));
        simpleBlockWithItem(ModBlocks.PLATEMETAL_GREEN.get(), models().cubeAll("platemetal_green", mcOrMod("block/platemetal_green")));
        simpleBlockWithItem(ModBlocks.PLATEMETAL_LIGHT_GRAY.get(), models().cubeAll("platemetal_light_gray", mcOrMod("block/platemetal_light_gray")));
        simpleBlockWithItem(ModBlocks.PLATEMETAL_BLUE.get(), models().cubeAll("platemetal_blue", mcOrMod("block/platemetal_blue")));
        simpleBlockWithItem(ModBlocks.PLATEMETAL_PURPLE.get(), models().cubeAll("platemetal_purple", mcOrMod("block/platemetal_purple")));
        simpleBlockWithItem(ModBlocks.PLATEMETAL_CYAN.get(), models().cubeAll("platemetal_cyan", mcOrMod("block/platemetal_cyan")));
        simpleBlockWithItem(ModBlocks.PLATEMETAL_PINK.get(), models().cubeAll("platemetal_pink", mcOrMod("block/platemetal_pink")));
        simpleBlockWithItem(ModBlocks.PLATEMETAL_LIME.get(), models().cubeAll("platemetal_lime", mcOrMod("block/platemetal_lime")));
        simpleBlockWithItem(ModBlocks.PLATEMETAL_YELLOW.get(), models().cubeAll("platemetal_yellow", mcOrMod("block/platemetal_yellow")));
        simpleBlockWithItem(ModBlocks.PLATEMETAL_LIGHT_BLUE.get(), models().cubeAll("platemetal_light_blue", mcOrMod("block/platemetal_light_blue")));
        simpleBlockWithItem(ModBlocks.PLATEMETAL_MAGENTA.get(), models().cubeAll("platemetal_magenta", mcOrMod("block/platemetal_magenta")));
        simpleBlockWithItem(ModBlocks.PLATEMETAL_ORANGE.get(), models().cubeAll("platemetal_orange", mcOrMod("block/platemetal_orange")));
        simpleBlockWithItem(ModBlocks.CONCRETE_SMOOTH.get(), models().cubeAll("concrete_smooth", mcOrMod("block/concrete")));
        simpleBlockWithItem(ModBlocks.DUCRETE_SMOOTH.get(), models().cubeAll("ducrete_smooth", mcOrMod("block/ducrete")));
        stairsBlock((net.minecraft.world.level.block.StairBlock) ModBlocks.CONCRETE_SMOOTH_STAIRS.get(), mcOrMod("block/concrete")); simpleBlockItem(ModBlocks.CONCRETE_SMOOTH_STAIRS.get(), models().getExistingFile(modLoc("block/concrete_smooth_stairs")));
        stairsBlock((net.minecraft.world.level.block.StairBlock) ModBlocks.DUCRETE_SMOOTH_STAIRS.get(), mcOrMod("block/ducrete")); simpleBlockItem(ModBlocks.DUCRETE_SMOOTH_STAIRS.get(), models().getExistingFile(modLoc("block/ducrete_smooth_stairs")));
        stairsBlock((net.minecraft.world.level.block.StairBlock) ModBlocks.BRICK_ASBESTOS_STAIRS.get(), mcOrMod("block/brick_asbestos")); simpleBlockItem(ModBlocks.BRICK_ASBESTOS_STAIRS.get(), models().getExistingFile(modLoc("block/brick_asbestos_stairs")));
        stairsBlock((net.minecraft.world.level.block.StairBlock) ModBlocks.LIGHTSTONE_TILE_STAIRS.get(), mcOrMod("block/lightstone_tile")); simpleBlockItem(ModBlocks.LIGHTSTONE_TILE_STAIRS.get(), models().getExistingFile(modLoc("block/lightstone_tile_stairs")));
        slabBlock((net.minecraft.world.level.block.SlabBlock) ModBlocks.STONES_SLAB_TILE.get(), models().slab("stones_slab_tile", mcOrMod("block/lightstone_tile"), mcOrMod("block/lightstone_tile"), mcOrMod("block/lightstone_tile")), models().slabTop("stones_slab_tile_top", mcOrMod("block/lightstone_tile"), mcOrMod("block/lightstone_tile"), mcOrMod("block/lightstone_tile")), models().cubeAll("stones_slab_tile_double", mcOrMod("block/lightstone_tile"))); simpleBlockItem(ModBlocks.STONES_SLAB_TILE.get(), models().getExistingFile(modLoc("block/stones_slab_tile")));
        slabBlock((net.minecraft.world.level.block.SlabBlock) ModBlocks.STONES_SLAB_BRICKS.get(), models().slab("stones_slab_bricks", mcOrMod("block/lightstone_bricks"), mcOrMod("block/lightstone_bricks"), mcOrMod("block/lightstone_bricks")), models().slabTop("stones_slab_bricks_top", mcOrMod("block/lightstone_bricks"), mcOrMod("block/lightstone_bricks"), mcOrMod("block/lightstone_bricks")), models().cubeAll("stones_slab_bricks_double", mcOrMod("block/lightstone_bricks"))); simpleBlockItem(ModBlocks.STONES_SLAB_BRICKS.get(), models().getExistingFile(modLoc("block/stones_slab_bricks")));
        simpleBlockWithItem(ModBlocks.BLOCK_FOAM.get(), models().cubeAll("block_foam", mcOrMod("block/foam")));
        { var b = ModBlocks.FOAM_LAYER.get(); getVariantBuilder(b).forAllStates(st -> { int l = st.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS); var m = l == 8 ? models().cubeAll("foam_layer_height16", mcOrMod("block/foam")) : models().withExistingParent("foam_layer_height" + (l * 2), mcLoc("block/snow_height" + (l * 2))).texture("texture", mcOrMod("block/foam")).texture("particle", mcOrMod("block/foam")); return net.minecraftforge.client.model.generators.ConfiguredModel.builder().modelFile(m).build(); }); simpleBlockItem(b, models().getExistingFile(modLoc("block/foam_layer_height2"))); }
        { var b = ModBlocks.SAND_BORON_LAYER.get(); getVariantBuilder(b).forAllStates(st -> { int l = st.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS); var m = l == 8 ? models().cubeAll("sand_boron_layer_height16", mcOrMod("block/sand_boron")) : models().withExistingParent("sand_boron_layer_height" + (l * 2), mcLoc("block/snow_height" + (l * 2))).texture("texture", mcOrMod("block/sand_boron")).texture("particle", mcOrMod("block/sand_boron")); return net.minecraftforge.client.model.generators.ConfiguredModel.builder().modelFile(m).build(); }); simpleBlockItem(b, models().getExistingFile(modLoc("block/sand_boron_layer_height2"))); }
        { var b = ModBlocks.LEAVES_LAYER.get(); getVariantBuilder(b).forAllStates(st -> { int l = st.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS); var m = l == 8 ? models().cubeAll("leaves_layer_height16", mcOrMod("block/waste_leaves")) : models().withExistingParent("leaves_layer_height" + (l * 2), mcLoc("block/snow_height" + (l * 2))).texture("texture", mcOrMod("block/waste_leaves")).texture("particle", mcOrMod("block/waste_leaves")); return net.minecraftforge.client.model.generators.ConfiguredModel.builder().modelFile(m).build(); }); simpleBlockItem(b, models().getExistingFile(modLoc("block/leaves_layer_height2"))); }
    }

    private net.minecraft.resources.ResourceLocation mcOrMod(String path) {
        return path.startsWith("minecraft:") ? net.minecraft.resources.ResourceLocation.parse(path) : modLoc(path);
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
    // =====================================================================================
    // МИГРАЦИЯ ручных blockstates/моделей на датаген.
    // Ранее это были рукописные JSON в src/main/resources/assets/hbm_m/blockstates и models/block.
    // Сложные вещи (OBJ/composite-модели, двери, multipart-конфиги duct/wire/pipe) оставлены ручными.
    // =====================================================================================
    private void registerMigratedManualBlocks() {
        ModelFile mblock_slag = models().withExistingParent("block/block_slag", mcLoc("block/cube_all"))
                .texture("all", modLoc("block/block_slag"));
        ModelFile mblock_slag_broken = models().withExistingParent("block/block_slag_broken", mcLoc("block/cube_all"))
                .texture("all", modLoc("block/block_slag_broken"));
        ModelFile memp = models().withExistingParent("block/emp", mcLoc("block/cube_column"))
                .texture("end", modLoc("block/bomb_emp_top"))
                .texture("side", modLoc("block/bomb_emp_side"));
        ModelFile mjas39_trophy = models().withExistingParent("block/jas39_trophy", mcLoc("block/air"));
        ModelFile mmachines_steam_turbine = models().withExistingParent("block/machines/steam_turbine", mcLoc("block/cube"))
                .texture("up", modLoc("block/machine/machine_turbine_top"))
                .texture("down", modLoc("block/machine/machine_turbine_base"))
                .texture("north", modLoc("block/machine/machine_turbine_base"))
                .texture("south", modLoc("block/machine/machine_turbine_base"))
                .texture("east", modLoc("block/machine/machine_turbine_base"))
                .texture("west", modLoc("block/machine/machine_turbine_base"))
                .texture("particle", modLoc("block/machine/machine_turbine_base"))
                .transforms()
                .transform(net.minecraft.world.item.ItemDisplayContext.GUI)
                .rotation(30, 225, 0).scale(0.625f).end().end();
        ModelFile more_bedrock_mineral = models().withExistingParent("block/ore_bedrock_mineral", mcLoc("block/cube_all"))
                .texture("all", modLoc("block/ore_bedrock_block"));
        ModelFile more_bedrock_oil = models().withExistingParent("block/ore_bedrock_oil", mcLoc("block/cube_all"))
                .texture("all", modLoc("block/ore_bedrock_oil"));
        ModelFile mparticle_test_block = models().withExistingParent("block/particle_test_block", mcLoc("block/cube_all"))
                .texture("all", modLoc("block/block_slag"));
        ModelFile mrbmk_rbmk_absorber = models().withExistingParent("block/rbmk/rbmk_absorber", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_absorber_top"))
                .texture("up", modLoc("block/rbmk/rbmk_absorber_top"))
                .texture("north", modLoc("block/rbmk/rbmk_absorber_side"))
                .texture("south", modLoc("block/rbmk/rbmk_absorber_side"))
                .texture("east", modLoc("block/rbmk/rbmk_absorber_side"))
                .texture("west", modLoc("block/rbmk/rbmk_absorber_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_absorber_side"));
        ModelFile mrbmk_rbmk_autoloader = models().withExistingParent("block/rbmk/rbmk_autoloader", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_blank_top"))
                .texture("up", modLoc("block/rbmk/rbmk_blank_top"))
                .texture("north", modLoc("block/rbmk/rbmk_blank_side"))
                .texture("south", modLoc("block/rbmk/rbmk_blank_side"))
                .texture("east", modLoc("block/rbmk/rbmk_blank_side"))
                .texture("west", modLoc("block/rbmk/rbmk_blank_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_blank_side"));
        ModelFile mrbmk_rbmk_blank = models().withExistingParent("block/rbmk/rbmk_blank", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_blank_top"))
                .texture("up", modLoc("block/rbmk/rbmk_blank_top"))
                .texture("north", modLoc("block/rbmk/rbmk_blank_side"))
                .texture("south", modLoc("block/rbmk/rbmk_blank_side"))
                .texture("east", modLoc("block/rbmk/rbmk_blank_side"))
                .texture("west", modLoc("block/rbmk/rbmk_blank_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_blank_side"));
        ModelFile mrbmk_rbmk_boiler = models().withExistingParent("block/rbmk/rbmk_boiler", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_boiler_top"))
                .texture("up", modLoc("block/rbmk/rbmk_boiler_top"))
                .texture("north", modLoc("block/rbmk/rbmk_boiler_side"))
                .texture("south", modLoc("block/rbmk/rbmk_boiler_side"))
                .texture("east", modLoc("block/rbmk/rbmk_boiler_side"))
                .texture("west", modLoc("block/rbmk/rbmk_boiler_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_boiler_side"));
        ModelFile mrbmk_rbmk_control = models().withExistingParent("block/rbmk/rbmk_control", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_control_top"))
                .texture("up", modLoc("block/rbmk/rbmk_control_top"))
                .texture("north", modLoc("block/rbmk/rbmk_control_side"))
                .texture("south", modLoc("block/rbmk/rbmk_control_side"))
                .texture("east", modLoc("block/rbmk/rbmk_control_side"))
                .texture("west", modLoc("block/rbmk/rbmk_control_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_control_side"));
        ModelFile mrbmk_rbmk_control_auto = models().withExistingParent("block/rbmk/rbmk_control_auto", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_control_auto_top"))
                .texture("up", modLoc("block/rbmk/rbmk_control_auto_top"))
                .texture("north", modLoc("block/rbmk/rbmk_control_auto_side"))
                .texture("south", modLoc("block/rbmk/rbmk_control_auto_side"))
                .texture("east", modLoc("block/rbmk/rbmk_control_auto_side"))
                .texture("west", modLoc("block/rbmk/rbmk_control_auto_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_control_auto_side"));
        ModelFile mrbmk_rbmk_control_blue = models().withExistingParent("block/rbmk/rbmk_control_blue", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_control_top"))
                .texture("up", modLoc("block/rbmk/rbmk_control_top"))
                .texture("north", modLoc("block/rbmk/rbmk_control_blue"))
                .texture("south", modLoc("block/rbmk/rbmk_control_blue"))
                .texture("east", modLoc("block/rbmk/rbmk_control_blue"))
                .texture("west", modLoc("block/rbmk/rbmk_control_blue"))
                .texture("particle", modLoc("block/rbmk/rbmk_control_blue"));
        ModelFile mrbmk_rbmk_control_green = models().withExistingParent("block/rbmk/rbmk_control_green", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_control_top"))
                .texture("up", modLoc("block/rbmk/rbmk_control_top"))
                .texture("north", modLoc("block/rbmk/rbmk_control_green"))
                .texture("south", modLoc("block/rbmk/rbmk_control_green"))
                .texture("east", modLoc("block/rbmk/rbmk_control_green"))
                .texture("west", modLoc("block/rbmk/rbmk_control_green"))
                .texture("particle", modLoc("block/rbmk/rbmk_control_green"));
        ModelFile mrbmk_rbmk_control_mod = models().withExistingParent("block/rbmk/rbmk_control_mod", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_control_mod_top"))
                .texture("up", modLoc("block/rbmk/rbmk_control_mod_top"))
                .texture("north", modLoc("block/rbmk/rbmk_control_mod_side"))
                .texture("south", modLoc("block/rbmk/rbmk_control_mod_side"))
                .texture("east", modLoc("block/rbmk/rbmk_control_mod_side"))
                .texture("west", modLoc("block/rbmk/rbmk_control_mod_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_control_mod_side"));
        ModelFile mrbmk_rbmk_control_mod_auto = models().withExistingParent("block/rbmk/rbmk_control_mod_auto", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_control_auto_top"))
                .texture("up", modLoc("block/rbmk/rbmk_control_auto_top"))
                .texture("north", modLoc("block/rbmk/rbmk_control_auto_side"))
                .texture("south", modLoc("block/rbmk/rbmk_control_auto_side"))
                .texture("east", modLoc("block/rbmk/rbmk_control_auto_side"))
                .texture("west", modLoc("block/rbmk/rbmk_control_auto_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_control_auto_side"));
        ModelFile mrbmk_rbmk_control_purple = models().withExistingParent("block/rbmk/rbmk_control_purple", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_control_top"))
                .texture("up", modLoc("block/rbmk/rbmk_control_top"))
                .texture("north", modLoc("block/rbmk/rbmk_control_purple"))
                .texture("south", modLoc("block/rbmk/rbmk_control_purple"))
                .texture("east", modLoc("block/rbmk/rbmk_control_purple"))
                .texture("west", modLoc("block/rbmk/rbmk_control_purple"))
                .texture("particle", modLoc("block/rbmk/rbmk_control_purple"));
        ModelFile mrbmk_rbmk_control_reasim = models().withExistingParent("block/rbmk/rbmk_control_reasim", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_control_reasim_top"))
                .texture("up", modLoc("block/rbmk/rbmk_control_reasim_top"))
                .texture("north", modLoc("block/rbmk/rbmk_control_reasim_side"))
                .texture("south", modLoc("block/rbmk/rbmk_control_reasim_side"))
                .texture("east", modLoc("block/rbmk/rbmk_control_reasim_side"))
                .texture("west", modLoc("block/rbmk/rbmk_control_reasim_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_control_reasim_side"));
        ModelFile mrbmk_rbmk_control_reasim_auto = models().withExistingParent("block/rbmk/rbmk_control_reasim_auto", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_control_reasim_auto_top"))
                .texture("up", modLoc("block/rbmk/rbmk_control_reasim_auto_top"))
                .texture("north", modLoc("block/rbmk/rbmk_control_reasim_auto_side"))
                .texture("south", modLoc("block/rbmk/rbmk_control_reasim_auto_side"))
                .texture("east", modLoc("block/rbmk/rbmk_control_reasim_auto_side"))
                .texture("west", modLoc("block/rbmk/rbmk_control_reasim_auto_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_control_reasim_auto_side"));
        ModelFile mrbmk_rbmk_control_yellow = models().withExistingParent("block/rbmk/rbmk_control_yellow", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_control_top"))
                .texture("up", modLoc("block/rbmk/rbmk_control_top"))
                .texture("north", modLoc("block/rbmk/rbmk_control_yellow"))
                .texture("south", modLoc("block/rbmk/rbmk_control_yellow"))
                .texture("east", modLoc("block/rbmk/rbmk_control_yellow"))
                .texture("west", modLoc("block/rbmk/rbmk_control_yellow"))
                .texture("particle", modLoc("block/rbmk/rbmk_control_yellow"));
        ModelFile mrbmk_rbmk_cooler = models().withExistingParent("block/rbmk/rbmk_cooler", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_cooler_top"))
                .texture("up", modLoc("block/rbmk/rbmk_cooler_top"))
                .texture("north", modLoc("block/rbmk/rbmk_cooler_side"))
                .texture("south", modLoc("block/rbmk/rbmk_cooler_side"))
                .texture("east", modLoc("block/rbmk/rbmk_cooler_side"))
                .texture("west", modLoc("block/rbmk/rbmk_cooler_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_cooler_side"));
        ModelFile mrbmk_rbmk_crane_console = models().withExistingParent("block/rbmk/rbmk_crane_console", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_console"))
                .texture("up", modLoc("block/rbmk/rbmk_console"))
                .texture("north", modLoc("block/rbmk/rbmk_console"))
                .texture("south", modLoc("block/rbmk/rbmk_console"))
                .texture("east", modLoc("block/rbmk/rbmk_console"))
                .texture("west", modLoc("block/rbmk/rbmk_console"))
                .texture("particle", modLoc("block/rbmk/rbmk_console"));
        ModelFile mrbmk_rbmk_debris = models().withExistingParent("block/rbmk/rbmk_debris", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_debris"))
                .texture("up", modLoc("block/rbmk/rbmk_debris"))
                .texture("north", modLoc("block/rbmk/rbmk_debris"))
                .texture("south", modLoc("block/rbmk/rbmk_debris"))
                .texture("east", modLoc("block/rbmk/rbmk_debris"))
                .texture("west", modLoc("block/rbmk/rbmk_debris"))
                .texture("particle", modLoc("block/rbmk/rbmk_debris"));
        ModelFile mrbmk_rbmk_debris_burning = models().withExistingParent("block/rbmk/rbmk_debris_burning", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_debris_burning"))
                .texture("up", modLoc("block/rbmk/rbmk_debris_burning"))
                .texture("north", modLoc("block/rbmk/rbmk_debris_burning"))
                .texture("south", modLoc("block/rbmk/rbmk_debris_burning"))
                .texture("east", modLoc("block/rbmk/rbmk_debris_burning"))
                .texture("west", modLoc("block/rbmk/rbmk_debris_burning"))
                .texture("particle", modLoc("block/rbmk/rbmk_debris_burning"));
        ModelFile mrbmk_rbmk_debris_digamma = models().withExistingParent("block/rbmk/rbmk_debris_digamma", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_debris_digamma"))
                .texture("up", modLoc("block/rbmk/rbmk_debris_digamma"))
                .texture("north", modLoc("block/rbmk/rbmk_debris_digamma"))
                .texture("south", modLoc("block/rbmk/rbmk_debris_digamma"))
                .texture("east", modLoc("block/rbmk/rbmk_debris_digamma"))
                .texture("west", modLoc("block/rbmk/rbmk_debris_digamma"))
                .texture("particle", modLoc("block/rbmk/rbmk_debris_digamma"));
        ModelFile mrbmk_rbmk_debris_radiating = models().withExistingParent("block/rbmk/rbmk_debris_radiating", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_debris_radiating"))
                .texture("up", modLoc("block/rbmk/rbmk_debris_radiating"))
                .texture("north", modLoc("block/rbmk/rbmk_debris_radiating"))
                .texture("south", modLoc("block/rbmk/rbmk_debris_radiating"))
                .texture("east", modLoc("block/rbmk/rbmk_debris_radiating"))
                .texture("west", modLoc("block/rbmk/rbmk_debris_radiating"))
                .texture("particle", modLoc("block/rbmk/rbmk_debris_radiating"));
        ModelFile mrbmk_rbmk_display = models().withExistingParent("block/rbmk/rbmk_display", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_display"))
                .texture("up", modLoc("block/rbmk/rbmk_display"))
                .texture("north", modLoc("block/rbmk/rbmk_display"))
                .texture("south", modLoc("block/rbmk/rbmk_display"))
                .texture("east", modLoc("block/rbmk/rbmk_display"))
                .texture("west", modLoc("block/rbmk/rbmk_display"))
                .texture("particle", modLoc("block/rbmk/rbmk_display"));
        ModelFile mrbmk_rbmk_display_blank = models().withExistingParent("block/rbmk/rbmk_display_blank", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_display"))
                .texture("up", modLoc("block/rbmk/rbmk_display"))
                .texture("north", modLoc("block/rbmk/rbmk_display"))
                .texture("south", modLoc("block/rbmk/rbmk_display"))
                .texture("east", modLoc("block/rbmk/rbmk_display"))
                .texture("west", modLoc("block/rbmk/rbmk_display"))
                .texture("particle", modLoc("block/rbmk/rbmk_display"));
        ModelFile mrbmk_rbmk_element = models().withExistingParent("block/rbmk/rbmk_element", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_element_top"))
                .texture("up", modLoc("block/rbmk/rbmk_element_top"))
                .texture("north", modLoc("block/rbmk/rbmk_element_side"))
                .texture("south", modLoc("block/rbmk/rbmk_element_side"))
                .texture("east", modLoc("block/rbmk/rbmk_element_side"))
                .texture("west", modLoc("block/rbmk/rbmk_element_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_element_side"));
        ModelFile mrbmk_rbmk_element_mod = models().withExistingParent("block/rbmk/rbmk_element_mod", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_element_mod_top"))
                .texture("up", modLoc("block/rbmk/rbmk_element_mod_top"))
                .texture("north", modLoc("block/rbmk/rbmk_element_mod_side"))
                .texture("south", modLoc("block/rbmk/rbmk_element_mod_side"))
                .texture("east", modLoc("block/rbmk/rbmk_element_mod_side"))
                .texture("west", modLoc("block/rbmk/rbmk_element_mod_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_element_mod_side"));
        ModelFile mrbmk_rbmk_element_reasim = models().withExistingParent("block/rbmk/rbmk_element_reasim", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_element_reasim_top"))
                .texture("up", modLoc("block/rbmk/rbmk_element_reasim_top"))
                .texture("north", modLoc("block/rbmk/rbmk_element_reasim_side"))
                .texture("south", modLoc("block/rbmk/rbmk_element_reasim_side"))
                .texture("east", modLoc("block/rbmk/rbmk_element_reasim_side"))
                .texture("west", modLoc("block/rbmk/rbmk_element_reasim_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_element_reasim_side"));
        ModelFile mrbmk_rbmk_element_reasim_mod = models().withExistingParent("block/rbmk/rbmk_element_reasim_mod", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_element_reasim_mod_top"))
                .texture("up", modLoc("block/rbmk/rbmk_element_reasim_mod_top"))
                .texture("north", modLoc("block/rbmk/rbmk_element_reasim_mod_side"))
                .texture("south", modLoc("block/rbmk/rbmk_element_reasim_mod_side"))
                .texture("east", modLoc("block/rbmk/rbmk_element_reasim_mod_side"))
                .texture("west", modLoc("block/rbmk/rbmk_element_reasim_mod_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_element_reasim_mod_side"));
        ModelFile mrbmk_rbmk_gauge = models().withExistingParent("block/rbmk/rbmk_gauge", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_element_top"))
                .texture("up", modLoc("block/rbmk/rbmk_element_top"))
                .texture("north", modLoc("block/rbmk/rbmk_element_side"))
                .texture("south", modLoc("block/rbmk/rbmk_element_side"))
                .texture("east", modLoc("block/rbmk/rbmk_element_side"))
                .texture("west", modLoc("block/rbmk/rbmk_element_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_element_side"));
        ModelFile mrbmk_rbmk_graph = models().withExistingParent("block/rbmk/rbmk_graph", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_element_top"))
                .texture("up", modLoc("block/rbmk/rbmk_element_top"))
                .texture("north", modLoc("block/rbmk/rbmk_element_side"))
                .texture("south", modLoc("block/rbmk/rbmk_element_side"))
                .texture("east", modLoc("block/rbmk/rbmk_element_side"))
                .texture("west", modLoc("block/rbmk/rbmk_element_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_element_side"));
        ModelFile mrbmk_rbmk_heater = models().withExistingParent("block/rbmk/rbmk_heater", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_heater_top"))
                .texture("up", modLoc("block/rbmk/rbmk_heater_top"))
                .texture("north", modLoc("block/rbmk/rbmk_heater_side"))
                .texture("south", modLoc("block/rbmk/rbmk_heater_side"))
                .texture("east", modLoc("block/rbmk/rbmk_heater_side"))
                .texture("west", modLoc("block/rbmk/rbmk_heater_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_heater_side"));
        ModelFile mrbmk_rbmk_indicator = models().withExistingParent("block/rbmk/rbmk_indicator", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_element_top"))
                .texture("up", modLoc("block/rbmk/rbmk_element_top"))
                .texture("north", modLoc("block/rbmk/rbmk_element_side"))
                .texture("south", modLoc("block/rbmk/rbmk_element_side"))
                .texture("east", modLoc("block/rbmk/rbmk_element_side"))
                .texture("west", modLoc("block/rbmk/rbmk_element_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_element_side"));
        ModelFile mrbmk_rbmk_keypad = models().withExistingParent("block/rbmk/rbmk_keypad", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_control_top"))
                .texture("up", modLoc("block/rbmk/rbmk_control_top"))
                .texture("north", modLoc("block/rbmk/rbmk_control_side"))
                .texture("south", modLoc("block/rbmk/rbmk_control_side"))
                .texture("east", modLoc("block/rbmk/rbmk_control_side"))
                .texture("west", modLoc("block/rbmk/rbmk_control_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_control_side"));
        ModelFile mrbmk_rbmk_lever = models().withExistingParent("block/rbmk/rbmk_lever", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_control_top"))
                .texture("up", modLoc("block/rbmk/rbmk_control_top"))
                .texture("north", modLoc("block/rbmk/rbmk_control_side"))
                .texture("south", modLoc("block/rbmk/rbmk_control_side"))
                .texture("east", modLoc("block/rbmk/rbmk_control_side"))
                .texture("west", modLoc("block/rbmk/rbmk_control_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_control_side"));
        // The connector ships its own texture (standalone_rbmk_loader, byte-identical to the
        // original's blocks/rbmk_loader.png) on all six faces, as setBlockTextureName does in the
        // original - it was wearing the blank column's side/top instead.
        ModelFile mrbmk_rbmk_loader = models().withExistingParent("block/rbmk/rbmk_loader", mcLoc("block/cube_all"))
                .texture("all", modLoc("block/rbmk/standalone_rbmk_loader"))
                .texture("particle", modLoc("block/rbmk/standalone_rbmk_loader"));
        ModelFile mrbmk_rbmk_moderator = models().withExistingParent("block/rbmk/rbmk_moderator", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_moderator_top"))
                .texture("up", modLoc("block/rbmk/rbmk_moderator_top"))
                .texture("north", modLoc("block/rbmk/rbmk_moderator_side"))
                .texture("south", modLoc("block/rbmk/rbmk_moderator_side"))
                .texture("east", modLoc("block/rbmk/rbmk_moderator_side"))
                .texture("west", modLoc("block/rbmk/rbmk_moderator_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_moderator_side"));
        ModelFile mrbmk_rbmk_numitron = models().withExistingParent("block/rbmk/rbmk_numitron", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_element_top"))
                .texture("up", modLoc("block/rbmk/rbmk_element_top"))
                .texture("north", modLoc("block/rbmk/rbmk_element_side"))
                .texture("south", modLoc("block/rbmk/rbmk_element_side"))
                .texture("east", modLoc("block/rbmk/rbmk_element_side"))
                .texture("west", modLoc("block/rbmk/rbmk_element_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_element_side"));
        ModelFile mrbmk_rbmk_outgasser = models().withExistingParent("block/rbmk/rbmk_outgasser", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_outgasser_top"))
                .texture("up", modLoc("block/rbmk/rbmk_outgasser_top"))
                .texture("north", modLoc("block/rbmk/rbmk_outgasser_side"))
                .texture("south", modLoc("block/rbmk/rbmk_outgasser_side"))
                .texture("east", modLoc("block/rbmk/rbmk_outgasser_side"))
                .texture("west", modLoc("block/rbmk/rbmk_outgasser_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_outgasser_side"));
        ModelFile mrbmk_rbmk_reflector = models().withExistingParent("block/rbmk/rbmk_reflector", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_reflector_top"))
                .texture("up", modLoc("block/rbmk/rbmk_reflector_top"))
                .texture("north", modLoc("block/rbmk/rbmk_reflector_side"))
                .texture("south", modLoc("block/rbmk/rbmk_reflector_side"))
                .texture("east", modLoc("block/rbmk/rbmk_reflector_side"))
                .texture("west", modLoc("block/rbmk/rbmk_reflector_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_reflector_side"));
        ModelFile mrbmk_rbmk_steam_inlet = models().withExistingParent("block/rbmk/rbmk_steam_inlet", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_boiler_pipe_top"))
                .texture("up", modLoc("block/rbmk/rbmk_boiler_pipe_top"))
                .texture("north", modLoc("block/rbmk/rbmk_boiler_pipe_side"))
                .texture("south", modLoc("block/rbmk/rbmk_boiler_pipe_side"))
                .texture("east", modLoc("block/rbmk/rbmk_boiler_pipe_side"))
                .texture("west", modLoc("block/rbmk/rbmk_boiler_pipe_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_boiler_pipe_side"));
        ModelFile mrbmk_rbmk_steam_outlet = models().withExistingParent("block/rbmk/rbmk_steam_outlet", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_boiler_pipe_top"))
                .texture("up", modLoc("block/rbmk/rbmk_boiler_pipe_top"))
                .texture("north", modLoc("block/rbmk/rbmk_boiler_pipe_side"))
                .texture("south", modLoc("block/rbmk/rbmk_boiler_pipe_side"))
                .texture("east", modLoc("block/rbmk/rbmk_boiler_pipe_side"))
                .texture("west", modLoc("block/rbmk/rbmk_boiler_pipe_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_boiler_pipe_side"));
        ModelFile mrbmk_rbmk_storage = models().withExistingParent("block/rbmk/rbmk_storage", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_storage_top"))
                .texture("up", modLoc("block/rbmk/rbmk_storage_top"))
                .texture("north", modLoc("block/rbmk/rbmk_storage_side"))
                .texture("south", modLoc("block/rbmk/rbmk_storage_side"))
                .texture("east", modLoc("block/rbmk/rbmk_storage_side"))
                .texture("west", modLoc("block/rbmk/rbmk_storage_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_storage_side"));
        ModelFile mrbmk_rbmk_terminal = models().withExistingParent("block/rbmk/rbmk_terminal", mcLoc("block/cube"))
                .texture("down", modLoc("block/rbmk/rbmk_element_top"))
                .texture("up", modLoc("block/rbmk/rbmk_element_top"))
                .texture("north", modLoc("block/rbmk/rbmk_element_side"))
                .texture("south", modLoc("block/rbmk/rbmk_element_side"))
                .texture("east", modLoc("block/rbmk/rbmk_element_side"))
                .texture("west", modLoc("block/rbmk/rbmk_element_side"))
                .texture("particle", modLoc("block/rbmk/rbmk_element_side"));
        ModelFile mrbmk_corium = models().withExistingParent("block/rbmk_corium", mcLoc("block/cube_all"))
                .texture("all", modLoc("block/block_corium"));
        ModelFile msu47_trophy = models().withExistingParent("block/su47_trophy", mcLoc("block/air"));
        ModelFile mswitch_off = models().withExistingParent("block/switch_off", mcLoc("block/cube_all"))
                .texture("all", modLoc("block/switch_off"));
        ModelFile mswitch_on = models().withExistingParent("block/switch_on", mcLoc("block/cube_all"))
                .texture("all", modLoc("block/switch_on"));
        ModelFile mtaint = models().withExistingParent("block/taint", mcLoc("block/cube_all"))
                .texture("all", modLoc("block/taint"));

        mig("rbmk_absorber", mrbmk_rbmk_absorber, "@0");
        mig("rbmk_autoloader", mrbmk_rbmk_autoloader, "@0");
        mig("rbmk_blank", mrbmk_rbmk_blank, "@0");
        mig("rbmk_boiler", mrbmk_rbmk_boiler, "@0");
        mig("rbmk_control", mrbmk_rbmk_control, "@0");
        mig("rbmk_control_auto", mrbmk_rbmk_control_auto, "@0");
        mig("rbmk_control_mod", mrbmk_rbmk_control_mod, "@0");
        mig("rbmk_control_reasim", mrbmk_rbmk_control_reasim, "@0");
        mig("rbmk_control_reasim_auto", mrbmk_rbmk_control_reasim_auto, "@0");
        mig("rbmk_cooler", mrbmk_rbmk_cooler, "@0");
        mig("rbmk_crane_console", mrbmk_rbmk_crane_console, "@0");
        mig("rbmk_debris", mrbmk_rbmk_debris, "@0");
        mig("rbmk_debris_burning", mrbmk_rbmk_debris_burning, "@0");
        mig("rbmk_debris_digamma", mrbmk_rbmk_debris_digamma, "@0");
        mig("rbmk_debris_radiating", mrbmk_rbmk_debris_radiating, "@0");
        mig("rbmk_display", mrbmk_rbmk_display, "@0");
        mig("rbmk_display_blank", mrbmk_rbmk_display_blank, "@0");
        mig("rbmk_element", mrbmk_rbmk_element, "@0");
        mig("rbmk_element_mod", mrbmk_rbmk_element_mod, "@0");
        mig("rbmk_element_reasim", mrbmk_rbmk_element_reasim, "@0");
        mig("rbmk_element_reasim_mod", mrbmk_rbmk_element_reasim_mod, "@0");
        mig("rbmk_gauge", mrbmk_rbmk_gauge, "@0");
        mig("rbmk_graph", mrbmk_rbmk_graph, "@0");
        mig("rbmk_heater", mrbmk_rbmk_heater, "@0");
        mig("rbmk_indicator", mrbmk_rbmk_indicator, "@0");
        mig("rbmk_key_pad", mrbmk_rbmk_keypad, "@0");
        mig("rbmk_lever", mrbmk_rbmk_lever, "@0");
        mig("rbmk_loader", mrbmk_rbmk_loader, "@0");
        mig("rbmk_moderator", mrbmk_rbmk_moderator, "@0");
        mig("rbmk_numitron", mrbmk_rbmk_numitron, "@0");
        mig("rbmk_outgasser", mrbmk_rbmk_outgasser, "@0");
        mig("rbmk_reflector", mrbmk_rbmk_reflector, "@0");
        mig("rbmk_steam_inlet", mrbmk_rbmk_steam_inlet, "@0");
        mig("rbmk_steam_outlet", mrbmk_rbmk_steam_outlet, "@0");
        mig("rbmk_storage", mrbmk_rbmk_storage, "@0");
        mig("rbmk_terminal", mrbmk_rbmk_terminal, "@0");
        mig("rbmk_corium", mrbmk_corium, "@0");
        mig("taint", mtaint, "age=0@0", "age=1@0", "age=10@0", "age=11@0", "age=12@0", "age=13@0", "age=14@0", "age=15@0", "age=2@0", "age=3@0", "age=4@0", "age=5@0", "age=6@0", "age=7@0", "age=8@0", "age=9@0");
        mig("switch", mswitch_off, "facing=east,powered=false@90", "facing=north,powered=false@0", "facing=south,powered=false@180", "facing=west,powered=false@270");
        mig("switch", mswitch_on, "facing=east,powered=true@90", "facing=north,powered=true@0", "facing=south,powered=true@180", "facing=west,powered=true@270");
        mig("block_slag", mblock_slag, "broken=false@0");
        mig("block_slag", mblock_slag_broken, "broken=true@0");
        mig("emp", memp, "@0");
        mig("ore_bedrock_mineral", more_bedrock_mineral, "@0");
        mig("ore_bedrock_oil", more_bedrock_oil, "@0");
        mig("particle_test_block", mparticle_test_block, "@0");
        mig("steam_turbine", mmachines_steam_turbine, "facing=east@90", "facing=north@0", "facing=south@180", "facing=west@270");
    }

    /** Блок по registry-имени (поиск по статическим полям ModBlocks). */
    private Block migBlock(String name) {
        for (java.lang.reflect.Field f : ModBlocks.class.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) && RegistrySupplier.class.isAssignableFrom(f.getType())) {
                try {
                    @SuppressWarnings("unchecked")
                    RegistrySupplier<Block> sup = (RegistrySupplier<Block>) f.get(null);
                    if (sup != null && sup.getId().getPath().equals(name)) return sup.get();
                } catch (IllegalAccessException ignored) {}
            }
        }
        throw new IllegalStateException("MIGRATE: block '" + name + "' not found in ModBlocks");
    }

    /**
     * Воспроизводит ручной blockstate: каждый элемент variants — "ключ@поворотY".
     * Пустой ключ ("" ) — единственный вариант без свойств. Свойства разрешаются по блоку.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void mig(String blockName, ModelFile model, String... variants) {
        Block block = migBlock(blockName);
        VariantBlockStateBuilder builder = getVariantBuilder(block);
        for (String spec : variants) {
            int at = spec.lastIndexOf('@');
            String key = spec.substring(0, at);
            int rotY = Integer.parseInt(spec.substring(at + 1));
            VariantBlockStateBuilder.PartialBlockstate ps = builder.partialState();
            if (!key.isEmpty()) {
                for (String kv : key.split(",")) {
                    String[] p = kv.split("=", 2);
                    net.minecraft.world.level.block.state.properties.Property prop = null;
                    for (net.minecraft.world.level.block.state.properties.Property q : block.getStateDefinition().getProperties()) {
                        if (q.getName().equals(p[0])) { prop = q; break; }
                    }
                    if (prop == null)
                        throw new IllegalStateException("MIGRATE: property '" + p[0] + "' not found on block '"+blockName+"'");
                    Comparable val;
                    if (prop instanceof net.minecraft.world.level.block.state.properties.BooleanProperty)
                        val = Boolean.parseBoolean(p[1]);
                    else if (prop instanceof net.minecraft.world.level.block.state.properties.IntegerProperty)
                        val = Integer.parseInt(p[1]);
                    else {
                        val = null;
                        for (Object v : prop.getPossibleValues()) {
                            if (v instanceof net.minecraft.util.StringRepresentable sr && sr.getSerializedName().equalsIgnoreCase(p[1])) { val = (Comparable) v; break; }
                        }
                        if (val == null)
                            throw new IllegalStateException("MIGRATE: value '" + p[1] + "' invalid for '" + p[0] + "' on '" + blockName + "'");
                    }
                    ps = ps.with(prop, val);
                }
            }
            ps.modelForState().modelFile(model).rotationY(rotY).addModel();
        }
    }

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

    /**
     * A horizontally-placed machine that has a wrecked model for when it has been blown up.
     *
     * <p>Emits the usual four facings twice over, keyed on the block's {@code exploded} property,
     * so the model swaps the moment the block entity flips that flag. The original does the same
     * swap inside its tile-entity renderer; the port has these on plain baked models, so the
     * blockstate is the place for it.</p>
     */
    private <T extends Block> void explodableMachineBlock(RegistrySupplier<T> blockObject,
                                                          String intactModel, String explodedModel) {
        var intact = models().getExistingFile(modLoc(intactModel));
        var wrecked = models().getExistingFile(modLoc(explodedModel));

        getVariantBuilder(blockObject.get()).forAllStates(state -> {
            var facing = state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
            // Each block declares its own BooleanProperty instance, so the property has to be
            // taken from the state itself - passing another block's instance to getValue throws.
            var property = state.getBlock().getStateDefinition().getProperty("exploded");
            boolean exploded = property instanceof net.minecraft.world.level.block.state.properties.BooleanProperty flag
                    && state.getValue(flag);
            return net.minecraftforge.client.model.generators.ConfiguredModel.builder()
                    .modelFile(exploded ? wrecked : intact)
                    .rotationY(((int) facing.toYRot() + 180) % 360)
                    .build();
        });
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
    /** 1:1 {@code BlockCraneBase}: ein Modell je (Eingang, wirksamer Ausgang) plus Inventarmodell ({@code getIcon(side, 0)}). */
    private <T extends Block> void craneBlock1to1(RegistrySupplier<T> blockObject, com.hbm_m.block.network.CraneTextures.Set tex) {
        VariantBlockStateBuilder builder = getVariantBuilder(blockObject.get());
        String base = blockObject.getId().getPath();
        java.util.Map<String, ModelFile> made = new java.util.HashMap<>();

        for (Direction in : Direction.values()) {
            for (Direction out : Direction.values()) {
                Direction eff = out == in ? in.getOpposite() : out;
                String name = base + "_" + in.getSerializedName() + "_" + eff.getSerializedName();
                ModelFile file = made.computeIfAbsent(name, n -> {
                    var model = models().withExistingParent(n, mcLoc("block/block")).texture("particle", modLoc("block/" + tex.side()));
                    var el = model.element().from(0, 0, 0).to(16, 16, 16);
                    for (Direction face : Direction.values()) {
                        String t = com.hbm_m.block.network.CraneTextures.icon(tex, in, eff, face);
                        var f = el.face(face).texture("#" + t).cullface(face);
                        if (face == Direction.UP) {
                            int deg = com.hbm_m.block.network.CraneTextures.topRotationDegrees(com.hbm_m.block.network.CraneTextures.topRotation(tex, in, eff));
                            if (deg == 90) f.rotation(net.minecraftforge.client.model.generators.ModelBuilder.FaceRotation.CLOCKWISE_90);
                            if (deg == 180) f.rotation(net.minecraftforge.client.model.generators.ModelBuilder.FaceRotation.UPSIDE_DOWN);
                            if (deg == 270) f.rotation(net.minecraftforge.client.model.generators.ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90);
                        }
                        f.end();
                        model.texture(t, modLoc("block/" + t));
                    }
                    el.end();
                    return model;
                });
                builder.partialState()
                        .with(com.hbm_m.block.network.CraneBaseBlock.FACING, in)
                        .with(com.hbm_m.block.network.CraneBaseBlock.OUTPUT, out)
                        .modelForState().modelFile(file).addModel();
            }
        }

        var inv = models().withExistingParent(base + "_inventory", mcLoc("block/block")).texture("particle", modLoc("block/" + tex.side()));
        var el = inv.element().from(0, 0, 0).to(16, 16, 16);
        for (Direction face : Direction.values()) {
            String t = com.hbm_m.block.network.CraneTextures.itemIcon(tex, face);
            el.face(face).texture("#" + t).end();
            inv.texture(t, modLoc("block/" + t));
        }
        el.end();
    }

    /** 1:1 {@code CraneRouter}: {@code crane_in} plus je Seite eine getoente Markierung (Durchgang 1-6). */
    private void craneRouter1to1() {
        String base = ModBlocks.CRANE_ROUTER.getId().getPath();
        var model = models().withExistingParent(base, mcLoc("block/block"))
                .texture("particle", modLoc("block/crane_in")).texture("base", modLoc("block/crane_in")).texture("overlay", modLoc("block/crane_router_overlay"))
                .renderType("cutout");
        var el = model.element().from(0, 0, 0).to(16, 16, 16);
        for (Direction face : Direction.values()) el.face(face).texture("#base").cullface(face).end();
        el.end();
        var ov = model.element().from(0, 0, 0).to(16, 16, 16);
        for (Direction face : Direction.values()) ov.face(face).texture("#overlay").cullface(face).tintindex(face.get3DDataValue()).end();
        ov.end();
        simpleBlockWithItem(ModBlocks.CRANE_ROUTER.get(), model);
    }

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

    /**
     * One RBMK panel slab: a flat plate {@code height} pixels tall, textured with the panel sheet on
     * the flat faces and the narrow edge strip on the sides. 1:1 with CE's hand-written
     * {@code deco_rbmk_*_slab2/4} models.
     */
    private void rbmkPanelSlab(net.minecraft.world.level.block.Block block, String name,
                                String topTex, String sideTex, int height) {
        var model = models().getBuilder(name)
                .texture("particle", modLoc("block/" + topTex))
                .texture("texture", modLoc("block/" + topTex))
                .texture("side", modLoc("block/" + sideTex));

        model.element()
                .from(0, 0, 0).to(16, height, 16)
                .face(Direction.DOWN).uvs(0, 0, 16, 16).texture("#texture").cullface(Direction.DOWN).end()
                .face(Direction.UP).uvs(0, 0, 16, 16).texture("#texture").end()
                .face(Direction.NORTH).uvs(0, 16 - height, 16, 16).texture("#side").cullface(Direction.NORTH).end()
                .face(Direction.SOUTH).uvs(0, 16 - height, 16, 16).texture("#side").cullface(Direction.SOUTH).end()
                .face(Direction.WEST).uvs(0, 16 - height, 16, 16).texture("#side").cullface(Direction.WEST).end()
                .face(Direction.EAST).uvs(0, 16 - height, 16, 16).texture("#side").cullface(Direction.EAST).end()
                .end();

        simpleBlock(block, model);

        // Only the single slab has an item; the double is never carried.
        if (block.asItem() != net.minecraft.world.item.Items.AIR) {
            simpleBlockItem(block, model);
        }
    }

    /** Axis-колонна (балки/трубы структурного декора) + предметная модель. */
    /** Ванильная лестница (порт BlockNTMLadder): blockstate как у ladder.json, модель-родитель block/ladder. */
    private void ladderBlockWithItem(Block block, String texture) {
        String name = texture.substring("block/".length());
        ModelFile model = models().withExistingParent(name, mcLoc("block/ladder"))
                .texture("texture", modLoc(texture))
                .texture("particle", modLoc(texture));
        horizontalBlock(block, model);
        simpleBlockItem(block, model);
    }

    /**
     * GrateBlock (порт BlockGrate): панель высотой 2px на высоте pos*2px.
     * pos=8 (потолок) визуально h7, pos=9 (под блоком) — h0. Стороны обрезают
     * текстуру по высоте, как ISBRH оригинала.
     */
    private void grateBlockWithItem(RegistrySupplier<Block> blockObject, String top, String side) {
        Block block = blockObject.get();
        String name = blockObject.getId().getPath();
        ModelFile[] levels = new ModelFile[8];
        for (int i = 0; i < 8; i++) {
            int lo = i * 2, hi = lo + 2;
            var b = models().getBuilder(name + "_h" + i)
                    .texture("top", modLoc(top))
                    .texture("side", modLoc(side))
                    .texture("particle", modLoc(top))
                    .renderType("cutout");
            var el = b.element().from(0, lo, 0).to(16, hi, 16);
            el.face(Direction.DOWN).uvs(0, 0, 16, 16).texture("#top").end();
            el.face(Direction.UP).uvs(0, 0, 16, 16).texture("#top").end();
            el.face(Direction.NORTH).uvs(0, 16 - hi, 16, 16 - lo).texture("#side").end();
            el.face(Direction.SOUTH).uvs(0, 16 - hi, 16, 16 - lo).texture("#side").end();
            el.face(Direction.EAST).uvs(0, 16 - hi, 16, 16 - lo).texture("#side").end();
            el.face(Direction.WEST).uvs(0, 16 - hi, 16, 16 - lo).texture("#side").end();
            el.end();
            levels[i] = b;
        }
        VariantBlockStateBuilder builder = getVariantBuilder(block);
        for (int pos = 0; pos <= 9; pos++) {
            builder.partialState().with(com.hbm_m.block.decorations.GrateBlock.POS, pos)
                    .modelForState().modelFile(levels[Math.min(pos, 7)]).addModel();
        }
        simpleBlockItem(block, levels[0]);
    }

    /**
     * AXIS-блок с готовой JSON/OBJ моделью-обёрткой из resources.
     * baseVertical=true — база модели вертикальна (steel_beam: колонна вдоль Y),
     * false — база ориентирована по X (steel_scaffold: панель, нормаль X).
     */
    private void objAxisBlockWithItem(RegistrySupplier<Block> blockObject, String modelName, boolean baseVertical) {
        Block block = blockObject.get();
        ModelFile model = models().getExistingFile(modLoc(modelName));
        // Плоская (axis=y) ориентация панели: blockstate-повороты не могут развернуть нормаль X в Y,
        // поэтому flat-модель — та же OBJ с transform.rotation [0,0,90] (только для baseVertical=false).
        ModelFile flatModel = baseVertical ? null : models().getExistingFile(modLoc(modelName + "_flat"));
        VariantBlockStateBuilder builder = getVariantBuilder(block);
        for (Direction.Axis axis : Direction.Axis.values()) {
            var ps = builder.partialState()
                    .with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, axis);
            if (baseVertical) {
                switch (axis) {
                    case Y -> ps.modelForState().modelFile(model).addModel();
                    case X -> ps.modelForState().modelFile(model).rotationX(90).addModel();
                    case Z -> ps.modelForState().modelFile(model).rotationX(90).rotationY(90).addModel();
                }
            } else {
                switch (axis) {
                    case X -> ps.modelForState().modelFile(model).addModel();
                    case Z -> ps.modelForState().modelFile(model).rotationY(90).addModel();
                    case Y -> ps.modelForState().modelFile(flatModel).addModel();
                }
            }
        }
        simpleBlockItem(block, model);
    }

    /** OilSpillBlock (порт BlockLayering): слои как у снега, модели поверх vanilla snow_heightN. */
    private void layerBlockWithItem(RegistrySupplier<Block> blockObject, String texture) {
        Block block = blockObject.get();
        String name = blockObject.getId().getPath();
        for (int layers = 1; layers <= 7; layers++) {
            models().withExistingParent(name + "_height" + (layers * 2), mcLoc("block/snow_height" + (layers * 2)))
                    .texture("texture", modLoc(texture))
                    .texture("particle", modLoc(texture));
        }
        ModelFile full = models().cubeAll(name, modLoc(texture));
        VariantBlockStateBuilder builder = getVariantBuilder(block);
        for (int layers = 1; layers <= 8; layers++) {
            ModelFile model = layers == 8 ? full
                    : models().getExistingFile(modLoc(name + "_height" + (layers * 2)));
            builder.partialState()
                    .with(net.minecraft.world.level.block.state.properties.BlockStateProperties.LAYERS, layers)
                    .modelForState().modelFile(model).addModel();
        }
        // плоский предмет (как у снежного слоя)
        itemModels().withExistingParent(name, mcLoc("item/generated")).texture("layer0", modLoc(texture));
    }

    /** OBJ-блок без ориентации (модель-обёртка в resources, identity transform). */
    private void customObjBlockSimple(RegistrySupplier<Block> blockObject) {
        ModelFile model = models().getExistingFile(modLoc("block/" + blockObject.getId().getPath()));
        VariantBlockStateBuilder builder = getVariantBuilder(blockObject.get());
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            builder.partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, facing)
                    .modelForState().modelFile(model).addModel();
        }
        simpleBlockItem(blockObject.get(), model);
    }

    /** OBJ-блок без свойства facing (обычный Block): один вариант, без поворотов. */
    private void customObjBlockNoFacing(RegistrySupplier<Block> blockObject) {
        ModelFile model = models().getExistingFile(modLoc("block/" + blockObject.getId().getPath()));
        getVariantBuilder(blockObject.get()).partialState().modelForState().modelFile(model).addModel();
        simpleBlockItem(blockObject.get(), model);
    }

    /** OBJ-блок с нестандартной картой поворотов facing→y (из рендера оригинала). */
    private void customObjBlockRotated(RegistrySupplier<Block> blockObject, Map<Direction, Integer> rotY) {
        ModelFile model = models().getExistingFile(modLoc("block/" + blockObject.getId().getPath()));
        VariantBlockStateBuilder builder = getVariantBuilder(blockObject.get());
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            builder.partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, facing)
                    .modelForState().modelFile(model).rotationY(rotY.getOrDefault(facing, 0)).addModel();
        }
        simpleBlockItem(blockObject.get(), model);
    }

    /** Ванильный забор (порт BlockMetalFence): post/side шаблоны с текстурой. */
    private void fenceBlockWithItem(RegistrySupplier<Block> blockObject, String texture) {
        ModelFile post = models().withExistingParent(blockObject.getId().getPath() + "_post", mcLoc("block/fence_post"))
                .texture("texture", modLoc(texture));
        ModelFile side = models().withExistingParent(blockObject.getId().getPath() + "_side", mcLoc("block/fence_side"))
                .texture("texture", modLoc(texture));
        net.minecraft.world.level.block.FenceBlock fence = (net.minecraft.world.level.block.FenceBlock) blockObject.get();
        var n = net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH;
        var s2 = net.minecraft.world.level.block.state.properties.BlockStateProperties.SOUTH;
        var w = net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST;
        var e = net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST;
        getMultipartBuilder(fence)
                // столб виден, если хотя бы с одной стороны нет соединения (OR)
                .part().modelFile(post).addModel().useOr()
                .condition(n, false).condition(s2, false).condition(w, false).condition(e, false).end()
                .part().modelFile(side).addModel().condition(n, true).end()
                .part().modelFile(side).addModel().condition(s2, true).end()
                .part().modelFile(side).addModel().condition(w, true).end()
                .part().modelFile(side).addModel().condition(e, true).end();
        simpleBlockItem(fence, post);
    }

    private void axisBlockWithItem(Block block, String side, String end) {
        ModelFile model = models().cubeColumn(side, modLoc(side), modLoc(end));
        VariantBlockStateBuilder builder = getVariantBuilder(block);
        for (Direction.Axis axis : Direction.Axis.values()) {
            var ps = builder.partialState()
                    .with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, axis);
            if (axis == Direction.Axis.Y) {
                ps.modelForState().modelFile(model).addModel();
            } else {
                ps.modelForState().modelFile(model).rotationX(90).rotationY(axis == Direction.Axis.X ? 90 : 0).addModel();
            }
        }
        simpleBlockItem(block, model);
    }

    /**
     * Der Meilerblock hat neun Rollen (siehe {@code PileBlockType}). Das Original setzt sie mit
     * einem Verbundtexturen-Renderer zusammen; hier bekommt jede Rolle ihr eigenes Modell aus den
     * Originaltexturen. Kanaleingaenge und -ausgaenge tragen dieselbe Oeffnungstextur auf allen
     * vier Seiten, weil ohne Metadatenrichtung nicht feststeht, wohin der Kanal laeuft.
     */
    private void pileBlockStates() {
        var block = (com.hbm_m.block.machines.pile.PileBlock) ModBlocks.PILE_BLOCK.get();

        getVariantBuilder(block).forAllStates(state -> {
            var type = state.getValue(com.hbm_m.block.machines.pile.PileBlock.TYPE);
            String name = "pile_block_" + type.getSerializedName();

            net.minecraft.resources.ResourceLocation side = modLoc("block/ported/pile_block");
            net.minecraft.resources.ResourceLocation top = modLoc("block/ported/pile_block_top");

            switch (type) {
                case CORE -> top = modLoc("block/ported/pile_block_core_ct");
                case CONTROL -> top = modLoc("block/ported/pile_block_control_top_ct");
                case FUEL_IN, AIR_IN -> side = modLoc("block/ported/pile_block_input_ct");
                case FUEL_OUT, AIR_OUT -> side = modLoc("block/ported/pile_block_output_ct");
                default -> { }
            }

            return net.minecraftforge.client.model.generators.ConfiguredModel.builder()
                    .modelFile(models().cubeBottomTop(name, side, top, top))
                    .build();
        });
    }

    /** Die drei Meilergeraete: ein Wuerfel mit ihrer Modelltextur, nach der Blickrichtung gedreht. */
    private void pileDeviceStates() {
        horizontalBlock(ModBlocks.PILE_LOADER.get(),
                models().cubeAll("pile_loader", modLoc("block/ported/pile_loader")));
        simpleBlockItem(ModBlocks.PILE_LOADER.get(), models().getExistingFile(modLoc("block/pile_loader")));

        horizontalBlock(ModBlocks.PILE_VENT.get(),
                models().cubeAll("pile_vent", modLoc("block/ported/pile_vent")));
        simpleBlockItem(ModBlocks.PILE_VENT.get(), models().getExistingFile(modLoc("block/pile_vent")));

        horizontalBlock(ModBlocks.PILE_CONTROL.get(),
                models().cubeAll("pile_control", modLoc("block/ported/pile_control")));
        simpleBlockItem(ModBlocks.PILE_CONTROL.get(), models().getExistingFile(modLoc("block/pile_control")));
    }
    /**
     * Das Druckluftrohr: ein Kern plus je ein Arm zu den Seiten, an denen etwas angeschlossen ist.
     * Das Original setzt das mit einem eigenen Renderer zusammen ({@code RenderPneumoTube}), hier
     * uebernimmt das die Mehrteil-Blockstate.
     */
    private void pneumoTubeState() {
        var block = ModBlocks.PNEUMATIC_TUBE.get();

        var core = models().withExistingParent("pneumatic_tube_core", mcLoc("block/block"))
                .texture("all", modLoc("block/pneumatic_tube"))
                .texture("particle", modLoc("block/pneumatic_tube"))
                .element().from(5, 5, 5).to(11, 11, 11)
                .allFaces((dir, face) -> face.texture("#all")).end();

        var arm = models().withExistingParent("pneumatic_tube_arm", mcLoc("block/block"))
                .texture("all", modLoc("block/pneumatic_tube_straight"))
                .texture("particle", modLoc("block/pneumatic_tube_straight"))
                .element().from(5, 5, 0).to(11, 11, 5)
                .allFaces((dir, face) -> face.texture("#all")).end();

        var builder = getMultipartBuilder(block);
        builder.part().modelFile(core).addModel().end();

        // Norden ist die Grundstellung des Arms, die uebrigen Seiten entstehen durch Drehung.
        builder.part().modelFile(arm).addModel()
                .condition(com.hbm_m.block.network.PneumoTubeBlock.NORTH, true).end();
        builder.part().modelFile(arm).rotationY(90).addModel()
                .condition(com.hbm_m.block.network.PneumoTubeBlock.EAST, true).end();
        builder.part().modelFile(arm).rotationY(180).addModel()
                .condition(com.hbm_m.block.network.PneumoTubeBlock.SOUTH, true).end();
        builder.part().modelFile(arm).rotationY(270).addModel()
                .condition(com.hbm_m.block.network.PneumoTubeBlock.WEST, true).end();
        builder.part().modelFile(arm).rotationX(270).addModel()
                .condition(com.hbm_m.block.network.PneumoTubeBlock.UP, true).end();
        builder.part().modelFile(arm).rotationX(90).addModel()
                .condition(com.hbm_m.block.network.PneumoTubeBlock.DOWN, true).end();

        simpleBlockItem(block, models().cubeAll("pneumatic_tube_inventory", modLoc("block/pneumatic_tube")));
    }

    /**
     * Der Platzhalter des ICF-Lasers. Die Anschlussstellen tragen eine eigene Textur, sonst sieht
     * er wie die uebrige Aussenhaut aus. Ein Gegenstand entfaellt - er faellt nie.
     */
    private void icfPhantomState() {
        var block = ModBlocks.ICF_BLOCK.get();

        getVariantBuilder(block).forAllStates(state -> {
            boolean port = state.getValue(com.hbm_m.block.machines.icf.ICFPhantomBlock.PORT);
            String name = port ? "icf_block_port" : "icf_block";
            var tex = modLoc("block/" + (port ? "icf_block_port" : "ported/icf_block"));

            return net.minecraftforge.client.model.generators.ConfiguredModel.builder()
                    .modelFile(models().cubeAll(name, tex))
                    .build();
        });
    }

    /**
     * Die sechs Laserbauteile. Kondensator und Turbolader tragen oben und unten eine andere
     * Textur als an den Seiten, genau wie im Original.
     */
    private void icfLaserStates() {
        simpleBlockWithItem(ModBlocks.ICF_LASER_CASING.get(),
                models().cubeAll("icf_laser_casing", modLoc("block/icf_casing")));
        simpleBlockWithItem(ModBlocks.ICF_LASER_PORT.get(),
                models().cubeAll("icf_laser_port", modLoc("block/icf_port")));
        simpleBlockWithItem(ModBlocks.ICF_LASER_CELL.get(),
                models().cubeAll("icf_laser_cell", modLoc("block/icf_cell")));
        simpleBlockWithItem(ModBlocks.ICF_LASER_EMITTER.get(),
                models().cubeAll("icf_laser_emitter", modLoc("block/icf_emitter")));
        simpleBlockWithItem(ModBlocks.ICF_LASER_CAPACITOR.get(),
                models().cubeBottomTop("icf_laser_capacitor", modLoc("block/icf_capacitor_side"),
                        modLoc("block/icf_capacitor_top"), modLoc("block/icf_capacitor_top")));
        simpleBlockWithItem(ModBlocks.ICF_LASER_TURBOCHARGER.get(),
                models().cubeBottomTop("icf_laser_turbocharger", modLoc("block/icf_turbocharger"),
                        modLoc("block/icf_capacitor_top"), modLoc("block/icf_capacitor_top")));
    }

    /**
     * Welt-Fluessigkeiten: die fliessenden zeichnet der Fluid-Renderer, das Blockmodell traegt nur
     * die Partikeltextur. Die endlichen Fluessigkeiten (Corium, Fluessigbeton) bekommen je Quantum
     * einen Quader der passenden Hoehe; Basalterze sind Saeulen mit eigener Oberseite.
     */
    private void worldFluids() {
        String[][] liquids = {
                { "mud_block", "mud" }, { "acid_block", "acid" }, { "toxic_block", "toxic" },
                { "schrabidic_block", "schrabidic_acid" }, { "volcanic_lava_block", "volcanic_lava" },
                { "rad_lava_block", "rad_lava" }, { "sulfuric_acid_block", "sulfuric_acid" } };
        for (String[] l : liquids) {
            net.minecraft.world.level.block.Block b = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(modLoc(l[0]));
            simpleBlock(b, models().getBuilder(l[0]).texture("particle", modLoc("block/fluid/" + l[1] + "_still")));
        }
        finiteFluid(ModBlocks.CORIUM_BLOCK.get(), "corium_block", "block/fluid/corium_still", 5);
        finiteFluid(ModBlocks.CONCRETE_LIQUID.get(), "concrete_liquid", "block/fluid/concrete_liquid", 4);

        for (String t : new String[] { "sulfur", "fluorite", "asbestos", "gem", "molysite" }) {
            net.minecraft.world.level.block.Block b = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(modLoc("ore_basalt_" + t));
            var model = models().cubeColumn("ore_basalt_" + t, modLoc("block/ore_basalt_" + t), modLoc("block/ore_basalt_" + t + "_top"));
            simpleBlockWithItem(b, model);
        }
    }

    private void finiteFluid(net.minecraft.world.level.block.Block block, String name, String texture, int quanta) {
        var builder = getVariantBuilder(block);
        for (int q = 0; q < 16; q++) {
            int level = Math.min(q, quanta - 1) + 1;
            float h = Math.max(1F, 14F * level / quanta);
            var model = models().withExistingParent(name + "_" + level, mcLoc("block/block"))
                    .texture("particle", modLoc(texture)).texture("tex", modLoc(texture))
                    .renderType("translucent")
                    .element().from(0, 0, 0).to(16, h, 16).allFaces((dir, face) -> face.texture("#tex")).end();
            builder.partialState().with(com.hbm_m.block.fluid.FiniteFluidBlock.LEVEL, q).addModels(new net.minecraftforge.client.model.generators.ConfiguredModel(model));
        }
    }
}
//?}
