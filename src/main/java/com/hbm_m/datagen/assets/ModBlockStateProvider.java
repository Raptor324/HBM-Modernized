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
        registerFfaBlockstatePorts();

        // ГЕНЕРАЦИЯ МОДЕЛЕЙ ДЛЯ БЛОКОВ-РЕСУРСОВ С ПРЕФИКСОМ "block_"
        simpleBlockWithItem(ModBlocks.STRAWBERRY_BUSH.get(), models().cross(blockTexture(ModBlocks.STRAWBERRY_BUSH.get()).getPath(),
                blockTexture(ModBlocks.STRAWBERRY_BUSH.get())).renderType("cutout"));
        // Блоки слитков теперь генерируются автоматически в цикле ниже

        blockWithItem(ModBlocks.GIGA_DET);
        // Textures ported from the 1.7.10 assets - these three had no blockstate at all.
        // geiger (geiger_counter_block) — ниже через customObjBlock (канон FFA); machines-дубль удалён.
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
        // concrete_colored_ext (frueher auch concrete_colored_<typ>/concrete_hazard) vor Treppen/Stufen erzeugen
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
        // Original BlockConcreteColoredExt.getIcon: MACHINE_STRIPE oben/unten mit MACHINE-Textur
        simpleBlockWithItem(ModBlocks.CONCRETE_COLORED_EXT_MACHINE_STRIPE.get(),
                models().cubeBottomTop(
                        ModBlocks.CONCRETE_COLORED_EXT_MACHINE_STRIPE.getId().getPath(),
                        modLoc("block/concrete_colored_ext_machine_stripe"),
                        modLoc("block/concrete_colored_ext_machine"),
                        modLoc("block/concrete_colored_ext_machine")
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
        blockWithItem(ModBlocks.DEPTH_CINNABAR);
        blockWithItem(ModBlocks.DEPTH_ZIRCONIUM);
        blockWithItem(ModBlocks.DEPTH_STONE);
        blockWithItem(ModBlocks.DEPTH_BORAX);
        blockWithItem(ModBlocks.WASTE_LEAVES);
        blockWithItem(ModBlocks.REINFORCED_STONE);
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
        // Restport: heisses Sellafit (Original sellafield Meta 0-5), vier Positionsvarianten je Stufe
        registerSellafieldHot(ModBlocks.SELLAFIELD_0, 0);
        registerSellafieldHot(ModBlocks.SELLAFIELD_1, 1);
        registerSellafieldHot(ModBlocks.SELLAFIELD_2, 2);
        registerSellafieldHot(ModBlocks.SELLAFIELD_3, 3);
        registerSellafieldHot(ModBlocks.SELLAFIELD_4, 4);
        registerSellafieldHot(ModBlocks.SELLAFIELD_5, 5);
        // Restport: obj_tester (nur Blockentity-Renderer, Item = Original-Blockicon test_render)
        rendererBlockWithCubeItem(ModBlocks.OBJ_TESTER, "block/test_render");
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
        registerFalloutBlock(ModBlocks.BLOCK_FALLOUT, "block_fallout", "ash"); // Original: hbm:ash

        // === РЕГИСТРАЦИЯ ПАДАЮЩИХ БЛОКОВ СЕЛЛАФИТА ===
        // Turrets: echte Original-Modelle (Base statisch per Blockmodell, Carriage/Pitch-Gruppe per BER animiert
        // - siehe MachineTurretRenderer). Statische Composite-Modelle liegen handgeschrieben unter
        // models/block/turret_<name>.json (Sichtbarkeits-Split aus den Original-OBJs).
        for (var turretBlock : java.util.List.of(
                ModBlocks.TURRET_SENTRY, ModBlocks.TURRET_CHEKHOV, ModBlocks.TURRET_FRIENDLY, ModBlocks.TURRET_JEREMY,
                ModBlocks.TURRET_TAUON, ModBlocks.TURRET_RICHARD, ModBlocks.TURRET_HOWARD,
                ModBlocks.TURRET_MAXWELL, ModBlocks.TURRET_FRITZ, ModBlocks.TURRET_ARTY, ModBlocks.TURRET_HIMARS,
                ModBlocks.TURRET_SENTRY_DAMAGED, ModBlocks.TURRET_HOWARD_DAMAGED)) {
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
        // ffa-port: item model -> manual assets/hbm_m/models/item/block_c4.json
        simpleBlockItem(ModBlocks.BLOCK_C4.get(), models().getExistingFile(modLoc("item/block_c4")));
        simpleBlockWithItem(ModBlocks.BLOCK_COLTAN.get(), models().cubeAll("block_coltan", modLoc("block/ported/block_coltan")));
        simpleBlockWithItem(ModBlocks.BLOCK_CORIUM.get(), models().cubeAll("block_corium", modLoc("block/ported/block_corium")));
        simpleBlockWithItem(ModBlocks.BLOCK_CORIUM_COBBLE.get(), models().cubeAll("block_corium_cobble", modLoc("block/ported/block_corium_cobble")));
        // block_euphemium_cluster — FFA-канон: axisBlock ниже по файлу; авто-порт-дубль удалён.
        // block_fiberglass — FFA-канон: axisBlock ниже по файлу; авто-порт-дубль удалён.
        simpleBlockWithItem(ModBlocks.BLOCK_FLUORITE.get(), models().cubeAll("block_fluorite", modLoc("block/ported/block_fluorite")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/block_graphite_tritium.json
        simpleBlockItem(ModBlocks.BLOCK_GRAPHITE_TRITIUM.get(), models().getExistingFile(modLoc("item/block_graphite_tritium")));
        // block_insulator — FFA-канон: axisBlock ниже по файлу; авто-порт-дубль удалён.
        simpleBlockWithItem(ModBlocks.BLOCK_LITHIUM.get(), models().cubeAll("block_lithium", modLoc("block/ported/block_lithium")));
        simpleBlockWithItem(ModBlocks.BLOCK_MAGNETIZED_TUNGSTEN.get(), models().cubeAll("block_magnetized_tungsten", modLoc("block/ported/block_magnetized_tungsten")));
        simpleBlockWithItem(ModBlocks.BLOCK_METEOR_MOLTEN.get(), models().cubeAll("block_meteor_molten", modLoc("block/ported/block_meteor_molten")));
        simpleBlockWithItem(ModBlocks.BLOCK_METEOR_TREASURE.get(), models().cubeAll("block_meteor_treasure", modLoc("block/ported/block_meteor_treasure")));
        simpleBlockWithItem(ModBlocks.BLOCK_NITER.get(), models().cubeAll("block_niter", modLoc("block/ported/block_niter")));
        simpleBlockWithItem(ModBlocks.BLOCK_POLYMER.get(), models().cubeAll("block_polymer", modLoc("block/ported/block_polymer")));
        simpleBlockWithItem(ModBlocks.BLOCK_PU_MIX.get(), models().cubeAll("block_pu_mix", modLoc("block/ported/block_pu_mix")));
        simpleBlockWithItem(ModBlocks.BLOCK_RED_PHOSPHORUS.get(), models().cubeAll("block_red_phosphorus", modLoc("block/ported/block_red_phosphorus")));
        simpleBlockWithItem(ModBlocks.BLOCK_RUBBER.get(), models().cubeAll("block_rubber", modLoc("block/ported/block_rubber")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/block_semtex.json
        simpleBlockItem(ModBlocks.BLOCK_SEMTEX.get(), models().getExistingFile(modLoc("item/block_semtex")));
        simpleBlockWithItem(ModBlocks.BLOCK_SULFUR.get(), models().cubeAll("block_sulfur", modLoc("block/ported/block_sulfur")));
        simpleBlockWithItem(ModBlocks.BLOCK_TANTALIUM.get(), models().cubeAll("block_tantalium", modLoc("block/ported/block_tantalium")));
        simpleBlockWithItem(ModBlocks.BLOCK_TRINITITE.get(), models().cubeAll("block_trinitite", modLoc("block/ported/block_trinitite")));
        // block_tritium — FFA-канон: axisBlock ниже по файлу; авто-порт-дубль удалён.
        simpleBlockWithItem(ModBlocks.BLOCK_WASTE.get(), models().cubeAll("block_waste", modLoc("block/ported/block_waste")));
        simpleBlockWithItem(ModBlocks.BLOCK_WASTE_VITRIFIED.get(), models().cubeAll("block_waste_vitrified", modLoc("block/ported/block_waste_vitrified")));
        simpleBlockWithItem(ModBlocks.BLOCK_WHITE_PHOSPHORUS.get(), models().cubeAll("block_white_phosphorus", modLoc("block/ported/block_white_phosphorus")));
        simpleBlockWithItem(ModBlocks.BLOCK_YELLOWCAKE.get(), models().cubeAll("block_yellowcake", modLoc("block/ported/block_yellowcake")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/brick_forgotten.json
        simpleBlockItem(ModBlocks.BRICK_FORGOTTEN.get(), models().getExistingFile(modLoc("item/brick_forgotten")));
        simpleBlockWithItem(ModBlocks.CONCRETE_LIQUID.get(), models().cubeAll("concrete_liquid", modLoc("block/ported/concrete_liquid")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/digamma_matter.json
        simpleBlockItem(ModBlocks.DIGAMMA_MATTER.get(), models().getExistingFile(modLoc("item/digamma_matter")));
        simpleBlockWithItem(ModBlocks.DUNGEON_SPAWNER.get(), models().cubeAll("dungeon_spawner", modLoc("block/ported/dungeon_spawner")));
        simpleBlockWithItem(ModBlocks.EVENT_TESTER.get(), models().cubeAll("event_tester", modLoc("block/ported/event_tester")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/fluid_duct_paintable_block_exhaust.json
        simpleBlockItem(ModBlocks.FLUID_DUCT_PAINTABLE_BLOCK_EXHAUST.get(), models().getExistingFile(modLoc("item/fluid_duct_paintable_block_exhaust")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/geysir_nether.json
        simpleBlockItem(ModBlocks.GEYSIR_NETHER.get(), models().getExistingFile(modLoc("item/geysir_nether")));
        icfPhantomState();
        simpleBlockWithItem(ModBlocks.LAUNCH_TABLE.get(), models().cubeAll("launch_table", modLoc("block/ported/launch_table")));
        simpleBlockWithItem(ModBlocks.LOGIC_BLOCK.get(), models().cubeAll("logic_block", modLoc("block/ported/logic_block")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/mush_block_stem.json
        simpleBlockItem(ModBlocks.MUSH_BLOCK_STEM.get(), models().getExistingFile(modLoc("item/mush_block_stem")));
        simpleBlockWithItem(ModBlocks.ORE_GNEISS_URANIUM_SCORCHED.get(), models().cubeAll("ore_gneiss_uranium_scorched", modLoc("block/ported/ore_gneiss_uranium_scorched")));
        simpleBlockWithItem(ModBlocks.ORE_ALUMINIUM.get(), models().cubeAll("ore_aluminium", modLoc("block/ported/ore_aluminium")));
        simpleBlockWithItem(ModBlocks.ORE_AUSTRALIUM.get(), models().cubeAll("ore_australium", modLoc("block/ported/ore_australium")));
        simpleBlockWithItem(ModBlocks.ORE_COPPER.get(), models().cubeAll("ore_copper", modLoc("block/ported/ore_copper")));
        simpleBlockWithItem(ModBlocks.ORE_NETHER_PLUTONIUM.get(), models().cubeAll("ore_nether_plutonium", modLoc("block/ported/ore_nether_plutonium")));
        simpleBlockWithItem(ModBlocks.ORE_NETHER_TUNGSTEN.get(), models().cubeAll("ore_nether_tungsten", modLoc("block/ported/ore_nether_tungsten")));
        simpleBlockWithItem(ModBlocks.ORE_NETHER_URANIUM_SCORCHED.get(), models().cubeAll("ore_nether_uranium_scorched", modLoc("block/ported/ore_nether_uranium_scorched")));
        simpleBlockWithItem(ModBlocks.ORE_TEKTITE_OSMIRIDIUM.get(), models().cubeAll("ore_tektite_osmiridium", modLoc("block/ported/ore_tektite_osmiridium")));
        simpleBlockWithItem(ModBlocks.ORE_URANIUM_SCORCHED.get(), models().cubeAll("ore_uranium_scorched", modLoc("block/ported/ore_uranium_scorched")));
        pileBlockStates();
        simpleBlockWithItem(ModBlocks.PILE_BRICK.get(), models().cubeBottomTop("pile_brick",
                modLoc("block/ported/pile_brick_side"), modLoc("block/ported/pile_brick_bottom"), modLoc("block/ported/pile_brick_top")));
        pileDeviceStates();
        // Original: the terminal shows its screen on the side facing the player (piston orientation).
        {
            ModelFile access = models().cube("pneumatic_storage_access",
                    modLoc("block/ported/pneumatic_storage_access"), modLoc("block/ported/pneumatic_storage_access"),
                    modLoc("block/ported/pneumatic_storage_access_front"), modLoc("block/ported/pneumatic_storage_access"),
                    modLoc("block/ported/pneumatic_storage_access"), modLoc("block/ported/pneumatic_storage_access"))
                    .texture("particle", modLoc("block/ported/pneumatic_storage_access"));
            getVariantBuilder(ModBlocks.PNEUMATIC_STORAGE_ACCESS.get()).forAllStates(state -> {
                Direction facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
                int x = facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
                int y = facing.getAxis().isHorizontal() ? ((int) facing.toYRot() + 180) % 360 : 0;
                return ConfiguredModel.builder().modelFile(access).rotationX(x).rotationY(y).build();
            });
            simpleBlockItem(ModBlocks.PNEUMATIC_STORAGE_ACCESS.get(), access);
        }
        simpleBlockWithItem(ModBlocks.PNEUMATIC_STORAGE_CLUTTER.get(), models().cubeAll("pneumatic_storage_clutter", modLoc("block/ported/pneumatic_storage_clutter")));
        simpleBlockWithItem(ModBlocks.PNEUMATIC_STORAGE_EXPORTER.get(), models().cubeAll("pneumatic_storage_exporter", modLoc("block/ported/pneumatic_storage_exporter")));
        simpleBlockWithItem(ModBlocks.PNEUMATIC_STORAGE_IMPORTER.get(), models().cubeAll("pneumatic_storage_importer", modLoc("block/ported/pneumatic_storage_importer")));
        simpleBlockWithItem(ModBlocks.PNEUMATIC_STORAGE_MONO.get(), models().cubeAll("pneumatic_storage_mono", modLoc("block/ported/pneumatic_storage_mono")));
        simpleBlockItem(ModBlocks.SOLAR_MIRROR.get(), models().cubeAll("solar_mirror", modLoc("block/ported/solar_mirror")));
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
                // Original BlockGeneric: alle Seiten meteor_brick_chiseled
                models().cubeBottomTop(
                        ModBlocks.METEOR_BRICK_CHISELED.getId().getPath(),
                        modLoc("block/meteor_brick_chiseled"),
                        modLoc("block/meteor_brick_chiseled"),
                        modLoc("block/meteor_brick_chiseled")
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

        // SEDNA: Original BlockWeaponTable (gun_table_side/top/bottom)
        columnBlockWithItem(
                ModBlocks.MACHINE_WEAPON_TABLE,
                modLoc("block/gun_table_side"),
                modLoc("block/gun_table_top"),
                modLoc("block/gun_table_bottom")
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
        // blast_door — FFA-канон: рукописный blockstate + BER BlastDoorRenderer; customDoorBlock (новая система) несовместим со старым блоком.

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
        // Audit 7: RenderGasFlare dreht nicht nach Metadaten (fest 180 Grad im Modell-Transform)
        simpleMachineBlock(ModBlocks.FLARE_STACK);
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
        simpleMachineBlock(ModBlocks.FAN);
        {
            var gauge = models().getBuilder("fluid_duct_gauge").parent(models().getExistingFile(mcLoc("block/block"))).renderType("cutout")
                    .texture("particle", modLoc("block/deco_steel")).texture("base", modLoc("block/deco_steel"))
                    .texture("overlay", modLoc("block/fluid_duct_paintable_overlay")).texture("gauge", modLoc("block/pipe_gauge"))
                    .element().from(0, 0, 0).to(16, 16, 16).allFaces((d, f) -> f.texture("#base").cullface(d)).end()
                    .element().from(0, 0, 0).to(16, 16, 16).allFaces((d, f) -> f.texture(d == net.minecraft.core.Direction.UP ? "#gauge" : "#overlay").cullface(d)).end();
            directionalBlock(ModBlocks.FLUID_DUCT_GAUGE.get(), gauge);
            simpleBlockItem(ModBlocks.FLUID_DUCT_GAUGE.get(), gauge);
        }
        simpleBlockWithItem(ModBlocks.DEUTERIUM_EXTRACTOR.get(), models().cubeColumn("machine_deuterium_extractor", modLoc("block/deuterium_extractor_side"), modLoc("block/deuterium_extractor_top_water")));
        directionalBlock(ModBlocks.PISTON_INSERTER.get(), models().getBuilder("piston_inserter").texture("particle", modLoc("block/block_steel")));
        horizontalBlock(ModBlocks.REFUELER.get(), models().getBuilder("refueler").texture("particle", modLoc("block/block_steel")));
        horizontalBlock(ModBlocks.LANTERN_BEHEMOTH.get(), models().getBuilder("lantern_behemoth").texture("particle", modLoc("block/block_rust")));
        simpleBlockWithItem(ModBlocks.CRYSTAL_VIRUS.get(), models().cubeAll("crystal_virus", modLoc("block/crystal_virus")));
        simpleBlockWithItem(ModBlocks.CRYSTAL_HARDENED.get(), models().cubeAll("crystal_hardened", modLoc("block/crystal_hardened")));
        simpleBlockWithItem(ModBlocks.CRYSTAL_PULSAR.get(), models().cubeAll("crystal_pulsar", modLoc("block/crystal_pulsar")));

        // --- WIP Machines (3D OBJ models) ---
        customMachineBlock(ModBlocks.AMMO_PRESS);
        customMachineBlock(ModBlocks.ANNIHILATOR);
        // Arc furnace: Original-TESR-Drehtabelle (RenderArcFurnace: N->90, S->270, W->180, E->0).
        customMachineBlockRotated(ModBlocks.ARC_FURNACE, Map.of(
                Direction.NORTH, 90, Direction.SOUTH, 270, Direction.WEST, 180, Direction.EAST, 0));
        simpleMachineBlock(ModBlocks.ASSEMBLY_FACTORY);
        simpleBlock(ModBlocks.AUTOSAW.get(),
            models().getExistingFile(modLoc("block/machines/autosaw")));
        horizontalBlock(ModBlocks.THRESHER.get(),
            models().getExistingFile(modLoc("block/machines/thresher")));
        customMachineBlock(ModBlocks.BEAMLINE);
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
        // Audit 7: PowerDetector ist ein Wuerfel (machine_detector_off / an: machine_detector), nicht das PA-Detektormodell
        {
            var detOff = models().cubeAll("detector", modLoc("block/machine_detector_off"));
            var detOn = models().cubeAll("detector_on", modLoc("block/machine_detector"));
            getVariantBuilder(ModBlocks.DETECTOR.get()).forAllStates(st -> ConfiguredModel.builder()
                    .modelFile(st.getValue(com.hbm_m.block.machines.PowerDetectorBlock.POWERED) ? detOn : detOff).build());
        }
        customMachineBlock(ModBlocks.DIESELGEN);
        customMachineBlock(ModBlocks.DIPOLE);
        simpleMachineBlock(ModBlocks.DRONE);
        // Multiblock mit Ausrichtung: das Modell dreht sich jetzt mit der Struktur.
        horizontalBlock(ModBlocks.ELECTRIC_HEATER.get(),
                models().getExistingFile(modLoc("block/machines/electric_heater")));
        horizontalBlock(ModBlocks.ELECTROLYSER.get(),
            models().getExistingFile(modLoc("block/machines/electrolyser")));
        customMachineBlock(ModBlocks.EPRESS);
        horizontalBlock(ModBlocks.EXPOSURE_CHAMBER.get(),
            models().getExistingFile(modLoc("block/machines/exposure_chamber")));
        // fensu (machine_fensu) — зарегистрирован ниже через horizontalBlock+getBuilder (FFA-канон).
        // FENSU2 (machine_battery_redd) is a MachineBatteryBlock (FACING-Blockstate) - see orientableBlockWithItem below.
        horizontalBlock(ModBlocks.FIREBOX.get(),
            models().getExistingFile(modLoc("block/machines/firebox")));
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
        customMachineBlock(ModBlocks.QUADRUPOLE);
        simpleMachineBlock(ModBlocks.RADGEN);
        horizontalBlock(ModBlocks.RADIOLYSIS.get(),
            models().getExistingFile(modLoc("block/machines/radiolysis")));
        simpleMachineBlock(ModBlocks.REACTOR_SMALL);
        customMachineBlock(ModBlocks.RFC);
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
        customMachineBlock(ModBlocks.MACHINE_CENTRIFUGE);
        customMachineBlock(ModBlocks.MACHINE_GASCENT);
        customMachineBlock(ModBlocks.BREEDER);
        customMachineBlock(ModBlocks.LARGE_PYLON);
        customMachineBlock(ModBlocks.LAUNCH_PAD);
        customMachineBlock(ModBlocks.LAUNCH_PAD_LARGE);
        customMachineBlock(ModBlocks.MOBILE_LAUNCH_PAD);
        customMachineBlock(ModBlocks.TOPOL_LAUNCH_PAD);
        customMachineBlock(ModBlocks.LAUNCH_PAD_RUSTED);
        customBombBlock(ModBlocks.NUKE_FAT_MAN);
        customBombBlock(ModBlocks.NUKE_GADGET);
        customBombBlock(ModBlocks.NUKE_BOY);
        customBombBlock(ModBlocks.NUKE_MIKE);
        customBombBlock(ModBlocks.NUKE_TSAR);
        customBombBlock(ModBlocks.NUKE_FLEIJA);
        // The model and its OBJ were already ported, only the blockstate was never emitted.
        customBombBlock(ModBlocks.NUKE_PROTOTYPE);
        simpleMachineBlock(ModBlocks.CORE_EMITTER);
        simpleMachineBlock(ModBlocks.CORE_INJECTOR);
        simpleMachineBlock(ModBlocks.CORE_RECEIVER);
        customMachineBlock(ModBlocks.VACUUM_DISTILL);
        customMachineBlock(ModBlocks.TURBOFAN);
        customMachineBlock(ModBlocks.INDUSTRIAL_TURBINE);
        // machine_large_turbine — FFA-канон: simpleMachineBlock (выше); ручная развёртка machines удалена.
        // MACHINE_CHUNGUS nutzt das bereits vorhandene chungus.obj-Modell (Pfad weicht von der
        // Registry-ID ab, daher kein customMachineBlock()).
        horizontalBlock(ModBlocks.MACHINE_CHUNGUS.get(),
                models().getExistingFile(modLoc("block/machines/chungus")));
        // Audit 7: RenderSubstation kennt nur zwei Lagen (2/3: 90 Grad, 4/5: 0 Grad)
        customMachineBlockRotated(ModBlocks.SUBSTATION, Map.of(Direction.NORTH, 0, Direction.SOUTH, 0, Direction.EAST, 90, Direction.WEST, 90));
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
        // Electric Furnace / Brick Furnace: own texture sets ported from the 1.7.10 assets
        // (machine_electric_furnace_*, machine_furnace_brick_*), so both stop borrowing the iron
        // furnace model. Separate front texture for the lit state, like vanilla's furnace.
        registerLitMachineBlock(ModBlocks.ELECTRIC_FURNACE,
            com.hbm_m.block.machines.MachineElectricFurnaceBlock.FACING, com.hbm_m.block.machines.MachineElectricFurnaceBlock.LIT,
            "electric_furnace", "electric_furnace_on");
        registerLitMachineBlock(ModBlocks.FURNACE_BRICK,
            com.hbm_m.block.machines.MachineFurnaceBrickBlock.FACING, com.hbm_m.block.machines.MachineFurnaceBrickBlock.LIT,
            "furnace_brick", "furnace_brick_on");
        // Rotary Furnace: eigenes OBJ-Modell (block/machines/rotary_furnace), kein LIT-Property
        // im FFA-Block — FFA-Kanon: customMachineBlock.
        customMachineBlock(ModBlocks.ROTARY_FURNACE);

        // FluidTank - FACING plus the wrecked variant
        explodableMachineBlock(ModBlocks.FLUID_TANK,
                "block/machines/fluid_tank", "block/machines/fluid_tank_exploded");

        // BAT9000 - uses its own pre-existing dedicated model/texture (static, no fluid-tint swap)
        horizontalBlock(ModBlocks.BAT9000.get(),
            models().getExistingFile(modLoc("block/machines/bat9000")));

        horizontalBlock(ModBlocks.MACHINE_BATTERY_SOCKET.get(),
            models().getExistingFile(modLoc("block/machines/machine_battery_socket")));

        // Жидкостный насос / клапан / выхлоп — временно ванильный iron cube (отдельные модели позже)
        // 1.7.10 "fluid_diode" OBJ (RenderFluidPump).
        horizontalBlock(ModBlocks.FLUID_PUMP.get(), models().getExistingFile(modLoc("block/machines/fluid_pump")));

        ModelFile fluidValveModel = models().withExistingParent(ModBlocks.FLUID_VALVE.getId().getPath(), mcLoc("block/cube_all"))
                .texture("all", modLoc("block/fluid_valve_off"))
                .texture("particle", modLoc("block/fluid_valve_off"));

        ModelFile fluidExhaustModel = models().withExistingParent(ModBlocks.FLUID_EXHAUST.getId().getPath(), mcLoc("block/cube_all"))
                .texture("all", mcLoc("block/iron_block"))
                .texture("particle", mcLoc("block/iron_block"));
        simpleBlock(ModBlocks.FLUID_EXHAUST.get(), fluidExhaustModel);

        // Decor
        customObjBlockNoFacing(ModBlocks.REBAR);
        // Hand-written OBJ models existed, the blockstates never did: every barbed wire
        // block logged "missing model for variant" and rendered as the missing model.
        customObjBlockNoFacing(ModBlocks.BARBED_WIRE);
        customObjBlockNoFacing(ModBlocks.BARBED_WIRE_FIRE);
        customObjBlockNoFacing(ModBlocks.BARBED_WIRE_POISON);
        customObjBlock(ModBlocks.BARBED_WIRE_WITHER);
        customObjBlock(ModBlocks.BARBED_WIRE_RAD);

        // Decor
        customObjBlock(ModBlocks.GEIGER_COUNTER_BLOCK);

        columnBlockWithItem(
                ModBlocks.DECON,
                modLoc("block/decon_side"),
                modLoc("block/decon_top"),
                modLoc("block/decon_side")
        );
        registerRadAbsorber();
        // steel_scaffold — objAxisBlockWithItem ниже (FFA-канон); алиас-дубль удалён.

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
        // mine_ap: Tarnung je Biom (RenderLandmine), Stein = mine_ap_placed.
        getVariantBuilder(ModBlocks.MINE_AP.get()).forAllStates(state -> {
            com.hbm_m.block.bomb.LandmineAPBlock.Camo camo = state.getValue(com.hbm_m.block.bomb.LandmineAPBlock.CAMO);
            String suffix = camo == com.hbm_m.block.bomb.LandmineAPBlock.Camo.STONE ? "" : "_" + camo.getSerializedName();
            return ConfiguredModel.builder().modelFile(models().getExistingFile(modLoc("block/bomb/mine_ap_placed" + suffix))).build();
        });
        placedMineBlock(ModBlocks.MINE_FAT, "mine_fat", false);
        placedMineBlock(ModBlocks.MINE_NAVAL, "naval_mine", false);
        customObjBlock(ModBlocks.CRATE_CONSERVE);

        // Технический блок без визуала; particle нужен, чтобы партиклы разрушения не были missing tex.
        simpleBlock(ModBlocks.UNIVERSAL_MACHINE_PART.get(), models().getBuilder(ModBlocks.UNIVERSAL_MACHINE_PART.getId().getPath())
                .texture("particle", modLoc("block/block_steel_machine")));
        // wire_coated: manual multipart blockstate + OBJ visibility (see assets/hbm_m/blockstates/wire_coated.json)
        // red_* ЛЭП (коннекторы/пилоны): ручные blockstates с OBJ-моделями и поворотами (assets/hbm_m/blockstates).
        // PYLON_DUMMY не датагенится вовсе (invisible).

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

        // FENSU (machine_fensu): Multiblock, gerendert per FENSURenderer; Blockmodell nur fuer Partikel,
        // Item-Modell = statisches OBJ block/machines/fensu (ModItemModelProvider)
        horizontalBlock(ModBlocks.MACHINE_FENSU.get(), models().getBuilder("machine_fensu").texture("particle", modLoc("block/machine/fensu")));
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
        // POLE_TOP — рукописный blockstate в assets (FFA-канон); генерация удалена.
        blockWithItem(ModBlocks.TOXIC_BLOCK);
        grateBlockWithItem(ModBlocks.STEEL_GRATE, "block/steel_grate", "block/steel_grate_end");
        grateBlockWithItem(ModBlocks.STEEL_GRATE_WIDE, "block/steel_grate_wide", "block/steel_grate_wide_end");

        // груда лута
        simpleBlock(ModBlocks.DECO_LOOT.get(),
                models().cubeAll("block/deco_loot", modLoc("block/deco_rusty_steel")));
        simpleBlockItem(ModBlocks.DECO_LOOT.get(), models().getExistingFile(modLoc("block/deco_loot")));

        // мёртвое растение (cross)
        ModelFile plantDead = models().cross("block/plant_dead", modLoc("block/plant_dead")).renderType("cutout");
        // plant_dead регистрируется в секции cross-моделей ниже (канон FFA); machines-дубль удалён.

        // стальная балка — DecoObjBlock (Placement.NONE, только FACING): одна OBJ-модель без поворотов
        customObjBlockSimple(ModBlocks.STEEL_BEAM);
        java.util.Map<Block, String[]> pipes = new java.util.LinkedHashMap<>();
        // Текстур deco_pipe_rim* не существует (ручные модели ссылались на них «в дырку»),
        // поэтому rim-трубы рендерятся framed-моделями, как и до миграции
        pipes.put(ModBlocks.DECO_PIPE_RIM.get(), new String[]{"block/deco_pipe_framed", "block/deco_pipe_framed"});
        pipes.forEach((block, tex) -> axisBlockWithItem(block, tex[0], tex[1]));

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
        // ore_oil и ore_oil_sand генерируются в registerR6aBlocks() со своими 1:1 моделями — здесь не дублировать
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

        // Метеоритные руды (порт BlockMeteorOre 1.7.10, ore_meteor.*)
        oreWithItem(ModBlocks.ORE_METEOR_IRON);
        oreWithItem(ModBlocks.ORE_METEOR_COPPER);
        oreWithItem(ModBlocks.ORE_METEOR_ALUMINIUM);
        oreWithItem(ModBlocks.ORE_METEOR_RAREEARTH);
        oreWithItem(ModBlocks.ORE_METEOR_COBALT);

        // Руды базальта (порт BlockOreBasalt 1.7.10): боковая текстура + "_top" для верхней
        // и нижней граней (ориг. getIcon: side <= 1 -> topIcons).
        basaltOreWithItem(ModBlocks.ORE_BASALT_SULFUR);
        basaltOreWithItem(ModBlocks.ORE_BASALT_FLUORITE);
        basaltOreWithItem(ModBlocks.ORE_BASALT_ASBESTOS);
        basaltOreWithItem(ModBlocks.ORE_BASALT_GEM);
        basaltOreWithItem(ModBlocks.ORE_BASALT_MOLYSITE);

        // ore_volcano (порт BlockFissure 1.7.10, Geothermal Vent): рендерится как bedrock
        // (первый мультипасс оригинала; оверлей molten_overlay не портирован).
        simpleBlockWithItem(ModBlocks.ORE_VOLCANO.get(),
                models().withExistingParent("ore_volcano", mcLoc("block/cube_all"))
                        .texture("all", mcLoc("block/bedrock")));

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
        // block_aluminium уже генерируется в цикле INGOT_BLOCKS (resourceBlockWithItem) — здесь не дублировать
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
        simpleBlockWithItem(ModBlocks.BRICK_JUNGLE_LAVA.get(),
                models().cubeAll(
                        ModBlocks.BRICK_JUNGLE_LAVA.getId().getPath(),
                        modLoc("block/brick_jungle_lava")
                )
        );
        // ffa-port: item model -> manual assets/hbm_m/models/item/brick_jungle_trap.json
        simpleBlockItem(ModBlocks.BRICK_JUNGLE_TRAP.get(), models().getExistingFile(modLoc("item/brick_jungle_trap")));
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
        // broadcaster_pc: 4 facing variants, y-rotations verbatim from NEXT
        // assets/hbm/blockstates/broadcaster_pc.json (model front = south).
        ModelFile broadcasterPcModel = models().getExistingFile(modLoc("block/machines/broadcaster_pc"));
        VariantBlockStateBuilder broadcasterPcBuilder = getVariantBuilder(ModBlocks.BROADCASTER_PC.get());
        broadcasterPcBuilder.partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.SOUTH)
                .setModels(ConfiguredModel.builder().modelFile(broadcasterPcModel).rotationY(0).build());
        broadcasterPcBuilder.partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.WEST)
                .setModels(ConfiguredModel.builder().modelFile(broadcasterPcModel).rotationY(90).build());
        broadcasterPcBuilder.partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.NORTH)
                .setModels(ConfiguredModel.builder().modelFile(broadcasterPcModel).rotationY(180).build());
        broadcasterPcBuilder.partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.EAST)
                .setModels(ConfiguredModel.builder().modelFile(broadcasterPcModel).rotationY(270).build());
        simpleBlockItem(ModBlocks.BROADCASTER_PC.get(),
                models().getExistingFile(modLoc("block/machines/broadcaster_pc")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/cable_detector.json
        simpleBlockItem(ModBlocks.CABLE_DETECTOR.get(), models().getExistingFile(modLoc("item/cable_detector")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/cable_diode.json
        simpleBlockItem(ModBlocks.CABLE_DIODE.get(), models().getExistingFile(modLoc("item/cable_diode")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/cable_switch.json
        simpleBlockItem(ModBlocks.CABLE_SWITCH.get(), models().getExistingFile(modLoc("item/cable_switch")));
        // Capacitor/Bus: FACING sichtbar machen. Die Eingangsface (FACING = geklickte Face beim
        // Platzieren) bekommt die inner_top-Textur ("Steckdose"), der Bus seine capacitor_bus_out-
        // Face (Original: getIcon(side, meta)). Modell ist mit dem Eingang auf NORTH genaeht; die
        // sechs Rotationen folgen der Observer-Konvention (wie beim DFC-Stabilisator oben).
        capacitorBlockWithFacing(ModBlocks.CAPACITOR_BUS,
                "capacitor_bus_out", "capacitor_bus_side", "capacitor_bus_side", "capacitor_bus_side");
        capacitorBlockWithFacing(ModBlocks.CAPACITOR_COPPER,
                "capacitor_copper_inner_top", "capacitor_copper_side", "capacitor_copper_top", "capacitor_copper_bottom");
        capacitorBlockWithFacing(ModBlocks.CAPACITOR_GOLD,
                "capacitor_gold_inner_top", "capacitor_gold_side", "capacitor_gold_top", "capacitor_gold_bottom");
        capacitorBlockWithFacing(ModBlocks.CAPACITOR_NIOBIUM,
                "capacitor_niobium_inner_top", "capacitor_niobium_side", "capacitor_niobium_top", "capacitor_niobium_bottom");
        capacitorBlockWithFacing(ModBlocks.CAPACITOR_SCHRABIDATE,
                "capacitor_schrabidate_inner_top", "capacitor_schrabidate_side", "capacitor_schrabidate_top", "capacitor_schrabidate_bottom");
        capacitorBlockWithFacing(ModBlocks.CAPACITOR_TANTALIUM,
                "capacitor_tantalium_inner_top", "capacitor_tantalium_side", "capacitor_tantalium_top", "capacitor_tantalium_bottom");
        // ffa-port: item model -> manual assets/hbm_m/models/item/charge_c4.json
        simpleBlockItem(ModBlocks.CHARGE_C4.get(), models().getExistingFile(modLoc("item/charge_c4")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/charge_dynamite.json
        simpleBlockItem(ModBlocks.CHARGE_DYNAMITE.get(), models().getExistingFile(modLoc("item/charge_dynamite")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/charge_miner.json
        simpleBlockItem(ModBlocks.CHARGE_MINER.get(), models().getExistingFile(modLoc("item/charge_miner")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/charge_semtex.json
        simpleBlockItem(ModBlocks.CHARGE_SEMTEX.get(), models().getExistingFile(modLoc("item/charge_semtex")));
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
        axisBlock((net.minecraft.world.level.block.RotatedPillarBlock) ModBlocks.CM_FLUX.get(), modLoc("block/cm_flux_side"), modLoc("block/cm_flux_top"));
        simpleBlockItem(ModBlocks.CM_FLUX.get(), models().getExistingFile(modLoc("block/cm_flux")));
        axisBlock((net.minecraft.world.level.block.RotatedPillarBlock) ModBlocks.CM_HEAT.get(), modLoc("block/cm_heat_side"), modLoc("block/cm_heat_top"));
        simpleBlockItem(ModBlocks.CM_HEAT.get(), models().getExistingFile(modLoc("block/cm_heat")));
        // Custom-Machine-Bauteile: Textur cm_<art>_<enum> wie BlockCM.registerBlockIcons
        for (var maps : java.util.List.of(ModBlocks.CM_BLOCK, ModBlocks.CM_SHEET, ModBlocks.CM_PORT, ModBlocks.CM_ENGINE, ModBlocks.CM_CIRCUIT)) {
            for (var s : maps.values()) simpleBlockWithItem(s.get(), models().cubeAll(s.getId().getPath(), modLoc("block/" + s.getId().getPath())));
        }
        for (var s : ModBlocks.CM_TANK.values()) {
            simpleBlockWithItem(s.get(), models().cubeAll(s.getId().getPath(), modLoc("block/" + s.getId().getPath())).renderType("cutout"));
        }
        {
            // BlockCustomMachine: Front cm_terminal_front auf der Facing-Seite; BlockCMAnchor analog mit cmt_*
            var cm = models().orientable("custom_machine", modLoc("block/cm_terminal_side"), modLoc("block/cm_terminal_front"), modLoc("block/cm_terminal_side"));
            horizontalBlock(ModBlocks.CUSTOM_MACHINE.get(), cm);
            var an = models().orientable("custom_machine_anchor", modLoc("block/cmt_terminal_side"), modLoc("block/cmt_terminal_front"), modLoc("block/cmt_terminal_side"));
            horizontalBlock(ModBlocks.CM_ANCHOR.get(), an);
            simpleBlockItem(ModBlocks.CM_ANCHOR.get(), an);
        }
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
        // Конвейеры: blockstate/модели остаются ручными (свойства facing+bend, element-модели network/)
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
        // FFA merge loss restore: генерация block-модели сплиттера потеряна при мердже.
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
        simpleBlockWithItem(ModBlocks.CRATE_SUPPLY.get(), models().cubeBottomTop("crate_supply", modLoc("block/crate_can_side"), modLoc("block/crate_can_bottom"), modLoc("block/crate_can_top")));
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
        // 1.7.10 draws the injector OBJ with the stabilizer texture. The mesh faces NORTH; the
        // vanilla directionalBlock helper expects an UP-facing model, so the six rotations are spelled out.
        {
            ModelFile stabilizer = models().getExistingFile(modLoc("block/machines/dfc_stabilizer"));
            getVariantBuilder(ModBlocks.DFC_STABILIZER.get()).forAllStates(state -> {
                Direction facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
                int x = facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
                int y = facing.getAxis().isHorizontal() ? ((int) facing.toYRot() + 180) % 360 : 0;
                return ConfiguredModel.builder().modelFile(stabilizer).rotationX(x).rotationY(y).build();
            });
            simpleBlockItem(ModBlocks.DFC_STABILIZER.get(), stabilizer);
        }
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
        // slag_dynamic регистрируется ниже (канон FFA/machines); здесь был битый дубль с моделью fire_digamma.
        // ffa-port: item model -> manual assets/hbm_m/models/item/fireworks.json
        simpleBlockItem(ModBlocks.FIREWORKS.get(), models().getExistingFile(modLoc("item/fireworks")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/fissure_bomb.json
        simpleBlockItem(ModBlocks.FISSURE_BOMB.get(), models().getExistingFile(modLoc("item/fissure_bomb")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/flame_war.json
        simpleBlockItem(ModBlocks.FLAME_WAR.get(), models().getExistingFile(modLoc("item/flame_war")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/fluid_counter_valve.json
        simpleBlockItem(ModBlocks.FLUID_COUNTER_VALVE.get(), models().getExistingFile(modLoc("item/fluid_counter_valve")));
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
        // ffa-port: item model -> manual assets/hbm_m/models/item/pipe_anchor.json
        simpleBlockItem(ModBlocks.PIPE_ANCHOR.get(), models().getExistingFile(modLoc("item/pipe_anchor")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/fluid_duct_paintable.json
        simpleBlockItem(ModBlocks.FLUID_DUCT_PAINTABLE.get(), models().getExistingFile(modLoc("item/fluid_duct_paintable")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/fluid_switch.json
        simpleBlockItem(ModBlocks.FLUID_SWITCH.get(), models().getExistingFile(modLoc("item/fluid_switch")));
        simpleBlock(ModBlocks.SLAG_DYNAMIC.get(),
                models().cubeAll(
                        ModBlocks.SLAG_DYNAMIC.getId().getPath(),
                        modLoc("block/slag_dynamic")
                )
        );
        // foundry_mold / foundry_tank: полые "чашка"-модели 1:1 с оригиналом
        // (hand-written в assets/hbm_m/models/block/machines/, cutout) — как foundry_basin.
        simpleMachineBlock(ModBlocks.FOUNDRY_MOLD);
        // foundry_slagtap uses a hand-written static blockstate+model (directional spout shape,
        // matching foundry_outlet's precedent - see assets/hbm_m/blockstates/foundry_slagtap.json).
        simpleMachineBlock(ModBlocks.FOUNDRY_TANK);
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
        // The OBJ models of the source and detector already exist (used by the old decorative blocks).
        horizontalBlock(ModBlocks.PA_SOURCE.get(), models().getExistingFile(modLoc("block/machines/source")));
        simpleBlockItem(ModBlocks.PA_SOURCE.get(), models().getExistingFile(modLoc("block/machines/source")));
        horizontalBlock(ModBlocks.PA_DETECTOR.get(), models().getExistingFile(modLoc("block/machines/detector")));
        simpleBlockItem(ModBlocks.PA_DETECTOR.get(), models().getExistingFile(modLoc("block/machines/detector")));
        // barrel_yellow — через customObjBlock (канон FFA); simpleBlockWithItem-дубль удалён.
        // Die Ladeplatte nutzt im Original die Stahlblock-Textur (die Teslaspule hat weiter unten
        // schon einen eigenen Eintrag mit passenderen Texturen).
        // Das Ladegeraet haengt an der Wand und hat eine Blickrichtung - das Modell muss mitdrehen,
        // sonst passt die Hitbox nicht zum Bild.
        // 1.7.10 OBJ: the base is the block model, the arms are drawn by ChargerRenderer.
        horizontalBlock(ModBlocks.CHARGER.get(), models().getExistingFile(modLoc("block/machines/charger")));
        simpleBlockItem(ModBlocks.CHARGER.get(), models().getExistingFile(modLoc("block/machines/charger_item")));
        // Original: der Grosstank nutzt ebenfalls die Stahlblock-Textur.
        simpleBlockWithItem(ModBlocks.MACHINE_BIGASSTANK.get(),
                models().getExistingFile(modLoc("block/machines/machine_bigasstank")));
        // Original: der RTG ist ein schlichter Wuerfel mit der Textur "rtg" auf allen Seiten.
        // 1.7.10 OBJ: the generator is the block model, the cable connectors are drawn by RtgRenderer.
        simpleBlock(ModBlocks.MACHINE_RTG.get(), models().getExistingFile(modLoc("block/machines/rtg")));
        simpleBlockItem(ModBlocks.MACHINE_RTG.get(), models().getExistingFile(modLoc("block/machines/rtg_item")));
        simpleBlockWithItem(ModBlocks.GLYPHID_BASE.get(),
                models().cubeAll(
                        ModBlocks.GLYPHID_BASE.getId().getPath(),
                        modLoc("block/glyphid_base")
                )
        );
        simpleBlockWithItem(ModBlocks.HADRON_COIL_ALLOY.get(), models().getExistingFile(modLoc("block/hadron_coil_alloy")));
        simpleBlockWithItem(ModBlocks.HADRON_COIL_CHLOROPHYTE.get(), models().getExistingFile(modLoc("block/hadron_coil_chlorophyte")));
        simpleBlockWithItem(ModBlocks.HADRON_COIL_GOLD.get(), models().getExistingFile(modLoc("block/hadron_coil_gold")));
        simpleBlockWithItem(ModBlocks.HADRON_COIL_MAGTUNG.get(), models().getExistingFile(modLoc("block/hadron_coil_magtung")));
        simpleBlockWithItem(ModBlocks.HADRON_COIL_MESE.get(), models().getExistingFile(modLoc("block/hadron_coil_mese")));
        simpleBlockWithItem(ModBlocks.HADRON_COIL_NEODYMIUM.get(), models().getExistingFile(modLoc("block/hadron_coil_neodymium")));
        simpleBlockWithItem(ModBlocks.HADRON_COIL_SCHRABIDATE.get(), models().getExistingFile(modLoc("block/hadron_coil_schrabidate")));
        simpleBlockWithItem(ModBlocks.HADRON_COIL_SCHRABIDIUM.get(), models().getExistingFile(modLoc("block/hadron_coil_schrabidium")));
        simpleBlockWithItem(ModBlocks.HADRON_COIL_STARMETAL.get(), models().getExistingFile(modLoc("block/hadron_coil_starmetal")));
        // FFA port: glyphid spawner nest (three TYPE variants share the eggs texture here)
        simpleBlockWithItem(ModBlocks.GLYPHID_SPAWNER.get(), models().cubeAll("glyphid_eggs_alt_block", modLoc("block/glyphid_eggs_alt")));
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
        // lamp_demon: blockstate + block model hand-written (OBJ via forge:composite,
        // 6-way facing), only the item model is generated.
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
        // MachineBatteryBlock (FACING-Blockstate) statt simpleBlockWithItem - siehe orientableBlockWithItem-Aufrufe fuer MACHINE_BATTERY etc.
        // Конвертеры HE<->RF: текстуры machine_converter_* восстановлены из FFA (FFA merge loss restore).
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
        simpleBlockWithItem(ModBlocks.MACHINE_FORCEFIELD.get(),
                models().getExistingFile(modLoc("block/machine_forcefield"))
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
            // Текстура machine/precass восстановлена из FFA (FFA merge loss restore); геометрию рисует
            // MachinePrecAssRenderer, blockstate задаёт только particle-текстуру.
            horizontalBlock(ModBlocks.MACHINE_PRECASS.get(), models().getBuilder("machine_precass").texture("particle", modLoc("block/machine/precass")));
            // PUREX — multipart-модель machine_parts (части Frame/Fan/Pump/Base из models/purex.obj).
            var purex = models().getExistingFile(modLoc("block/purex"));
            horizontalBlock(ModBlocks.PUREX.get(), purex);
            simpleBlockItem(ModBlocks.PUREX.get(), purex);
        }
        // 1.7.10 "igen" OBJ (RenderIGenerator); the block has no facing, so it stands the one way.
        simpleBlockWithItem(ModBlocks.INDUSTRIAL_GENERATOR.get(), models().getExistingFile(modLoc("block/machines/igen")));
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
        // 1.7.10 OBJ (RenderMissileAssembly); the block has no facing, so it stands the one way.
        simpleBlockWithItem(ModBlocks.MACHINE_MISSILE_ASSEMBLY.get(), models().getExistingFile(modLoc("block/machines/missile_assembly")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/machine_puf6_tank.json
        simpleBlockItem(ModBlocks.MACHINE_PUF6_TANK.get(), models().getExistingFile(modLoc("item/machine_puf6_tank")));
        {
            // Multiblock mit Ausrichtung: das Modell dreht sich jetzt mit der Struktur.
            // 1.7.10 OBJ (RenderDrain), three blocks long like the structure.
            var drain = models().getExistingFile(modLoc("block/machines/drain"));
            horizontalBlock(ModBlocks.MACHINE_DRAIN.get(), drain);
            simpleBlockItem(ModBlocks.MACHINE_DRAIN.get(), drain);
        }
        // 1:1 MachineDiFurnaceRTG (FFA merge loss restore): an/aus wechselt Front- und Deckeltextur,
        // Seiten/Boden machine_rtg_furnace_side_alt; das LIT-Property des Blocks steuert Modell und Licht.
        {
            var rtgOff = models().orientableWithBottom("machine_difurnace_rtg_off", modLoc("block/machine_rtg_furnace_side_alt"),
                    modLoc("block/rtg_difurnace_front_off"), modLoc("block/machine_rtg_furnace_side_alt"), modLoc("block/rtg_difurnace_top_off"))
                    .texture("particle", modLoc("block/rtg_difurnace_front_off"));
            var rtgOn = models().orientableWithBottom("machine_difurnace_rtg_on", modLoc("block/machine_rtg_furnace_side_alt"),
                    modLoc("block/rtg_difurnace_front_on"), modLoc("block/machine_rtg_furnace_side_alt"), modLoc("block/rtg_difurnace_top_on"))
                    .texture("particle", modLoc("block/rtg_difurnace_front_on"));
            getVariantBuilder(ModBlocks.MACHINE_DIFURNACE_RTG.get()).forAllStates(st -> ConfiguredModel.builder()
                    .modelFile(st.getValue(com.hbm_m.block.machines.MachineDifurnaceRtgBlock.LIT) ? rtgOn : rtgOff)
                    .rotationY(((int) st.getValue(com.hbm_m.block.machines.MachineDifurnaceRtgBlock.FACING).toYRot() + 180) % 360)
                    .build());
            simpleBlockItem(ModBlocks.MACHINE_DIFURNACE_RTG.get(), rtgOff);
        }
        {
            // 1:1 MachineRtgFurnace: Front an/aus, Deckel/Boden base_alt, Seiten side_alt
            var off = models().orientableWithBottom("machine_rtg_furnace_off", modLoc("block/machine_rtg_furnace_side_alt"),
                    modLoc("block/machine_rtg_furnace_off_alt"), modLoc("block/machine_rtg_furnace_base_alt"), modLoc("block/machine_rtg_furnace_base_alt"))
                    .texture("particle", modLoc("block/machine_rtg_furnace_side_alt"));
            var on = models().orientableWithBottom("machine_rtg_furnace_on", modLoc("block/machine_rtg_furnace_side_alt"),
                    modLoc("block/machine_rtg_furnace_on_alt"), modLoc("block/machine_rtg_furnace_base_alt"), modLoc("block/machine_rtg_furnace_base_alt"))
                    .texture("particle", modLoc("block/machine_rtg_furnace_side_alt"));
            getVariantBuilder(ModBlocks.MACHINE_RTG_FURNACE.get()).forAllStates(st -> ConfiguredModel.builder()
                    .modelFile(st.getValue(com.hbm_m.block.machines.MachineRtgFurnaceBlock.LIT) ? on : off)
                    .rotationY(((int) st.getValue(com.hbm_m.block.machines.MachineRtgFurnaceBlock.FACING).toYRot() + 180) % 360)
                    .build());
            simpleBlockItem(ModBlocks.MACHINE_RTG_FURNACE.get(), off);
        }
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
        // machine_radgen — зарегистрирован выше; дубль удалён.
        // 1:1 BlockCraneBase.getIcon/getRotationFromSide je Eingang/Ausgang
        // (краны конфигурируются только craneBlock1to1 ниже — craneDirectionalBlock дублировал те же состояния)
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
        // Original RenderStorageDrum: statisches drum.obj mit drum_gray - als OBJ-Blockmodell
        simpleBlockWithItem(ModBlocks.MACHINE_STORAGE_DRUM.get(),
                models().cubeAll(
                        ModBlocks.MACHINE_STORAGE_DRUM.getId().getPath(),
                        modLoc("block/machine_storage_drum")
                )
        );
        // ffa-port: item model -> manual assets/hbm_m/models/item/machine_uf6_tank.json
        simpleBlockItem(ModBlocks.MACHINE_UF6_TANK.get(), models().getExistingFile(modLoc("item/machine_uf6_tank")));
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
                        modLoc("block/meteor_polished"),
                        modLoc("block/meteor_spawner_top")
                )
        );
        simpleBlockWithItem(ModBlocks.MINE_HE.get(),
                models().cubeAll(
                        ModBlocks.MINE_HE.getId().getPath(),
                        modLoc("block/mine_he")
                )
        );
        // mine_naval уже размещён через placedMineBlock (модель bomb/naval_mine_placed) — здесь не дублировать
        simpleBlockWithItem(ModBlocks.MINE_SHRAP.get(),
                models().cubeAll(
                        ModBlocks.MINE_SHRAP.getId().getPath(),
                        modLoc("block/mine_shrap")
                )
        );
        // ffa-port: item model -> manual assets/hbm_m/models/item/mush.json
        simpleBlockItem(ModBlocks.MUSH.get(), models().getExistingFile(modLoc("item/mush")));
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
        // ffa-port: item model -> manual assets/hbm_m/models/item/pink_log.json
        simpleBlockItem(ModBlocks.PINK_LOG.get(), models().getExistingFile(modLoc("item/pink_log")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/pink_planks.json
        simpleBlockItem(ModBlocks.PINK_PLANKS.get(), models().getExistingFile(modLoc("item/pink_planks")));
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
        railBlockstate(ModBlocks.RAIL_BOOSTER, "rail_booster");
        railBlockstate(ModBlocks.RAIL_HIGHSPEED, "rail_highspeed");
        railBlockstate(ModBlocks.RAIL_NARROW, "rail_narrow");
        // ffa-port: item model -> manual assets/hbm_m/models/item/rail_wood.json
        simpleBlockItem(ModBlocks.RAIL_WOOD.get(), models().getExistingFile(modLoc("item/rail_wood")));
                                        // red_cable_box: blockstate/model/item — вручную (hbm_m:box_cable loader), 5 размеров
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
        // Glass needs the cutout layer: paneBlock leaves render_type unset, so the pane baked
        // into the solid layer and its edges came out opaque.
        paneBlockWithRenderType((net.minecraft.world.level.block.IronBarsBlock) ModBlocks.REINFORCED_GLASS_PANE.get(),
                modLoc("block/reinforced_glass_pane"), modLoc("block/reinforced_glass_pane_edge"),
                mcLoc("cutout"));
        simpleBlockItem(ModBlocks.REINFORCED_GLASS_PANE.get(), models().getExistingFile(modLoc("block/reinforced_glass_pane_post")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/reinforced_laminate_pane.json
        simpleBlockItem(ModBlocks.REINFORCED_LAMINATE_PANE.get(), models().getExistingFile(modLoc("item/reinforced_laminate_pane")));
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
        // Kapsel rendert per BlockEntityRenderer (RenderCapsule); Blockmodell nur fuer Partikel
        getVariantBuilder(ModBlocks.SOYUZ_CAPSULE.get()).forAllStates(st -> ConfiguredModel.builder()
                .modelFile(models().getBuilder("soyuz_capsule").texture("particle", modLoc("block/soyuz_capsule"))).build());
        customObjBlock(ModBlocks.SOYUZ_LAUNCHER);
        customObjBlock(ModBlocks.DECO_SOYUZ_ROCKET);
        // ffa-port: item model -> manual assets/hbm_m/models/item/spikes.json
        simpleBlockItem(ModBlocks.SPIKES.get(), models().getExistingFile(modLoc("item/spikes")));
        simpleBlockWithItem(ModBlocks.STALACTITE_ASBESTOS.get(), models().getExistingFile(modLoc("block/stalactite_asbestos")));
        simpleBlockWithItem(ModBlocks.STALACTITE_SULFUR.get(), models().getExistingFile(modLoc("block/stalactite_sulfur")));
        simpleBlockWithItem(ModBlocks.STALAGMITE_ASBESTOS.get(), models().getExistingFile(modLoc("block/stalagmite_asbestos")));
        simpleBlockWithItem(ModBlocks.STALAGMITE_SULFUR.get(), models().getExistingFile(modLoc("block/stalagmite_sulfur")));
        // Порт BlockScaffold: SteelScaffoldBlock.ORIENT (4 ориентации), модели как у цветных вариантов
        var scO = com.hbm_m.block.decorations.SteelScaffoldBlock.ORIENT;
        getVariantBuilder(ModBlocks.STEEL_SCAFFOLD.get()).partialState().with(scO, com.hbm_m.block.decorations.SteelScaffoldBlock.Orient.UPRIGHT_X).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_scaffold_upright_x"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_SCAFFOLD.get()).partialState().with(scO, com.hbm_m.block.decorations.SteelScaffoldBlock.Orient.UPRIGHT_Z).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_scaffold_upright_z"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_SCAFFOLD.get()).partialState().with(scO, com.hbm_m.block.decorations.SteelScaffoldBlock.Orient.FLAT_NS).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_scaffold_flat_ns"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_SCAFFOLD.get()).partialState().with(scO, com.hbm_m.block.decorations.SteelScaffoldBlock.Orient.FLAT_WE).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_scaffold_flat_we"))).addModel();
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
        // ffa-port: item model -> manual assets/hbm_m/models/item/stone_porous.json
        simpleBlockItem(ModBlocks.STONE_POROUS.get(), models().getExistingFile(modLoc("item/stone_porous")));
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
        simpleBlockWithItem(ModBlocks.TESLA.get(), models().getExistingFile(modLoc("block/machines/tesla")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/therm_endo.json
        simpleBlockItem(ModBlocks.THERM_ENDO.get(), models().getExistingFile(modLoc("item/therm_endo")));
        // ffa-port: item model -> manual assets/hbm_m/models/item/therm_exo.json
        simpleBlockItem(ModBlocks.THERM_EXO.get(), models().getExistingFile(modLoc("item/therm_exo")));
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
        // ffa-port: item model -> manual assets/hbm_m/models/item/vine_phosphor.json
        simpleBlockItem(ModBlocks.VINE_PHOSPHOR.get(), models().getExistingFile(modLoc("item/vine_phosphor")));
        // Ядро вулкана: 5 состояний (меты BlockVolcano 1.7.10), одна текстура на все
        getVariantBuilder(ModBlocks.VOLCANO_CORE.get()).partialState().setModels(
                ConfiguredModel.builder().modelFile(
                        models().cubeAll(
                                ModBlocks.VOLCANO_CORE.getId().getPath(),
                                modLoc("block/volcano_core")
                        )
                ).build()
        );
        itemModels().withExistingParent(ModBlocks.VOLCANO_CORE.getId().getPath(), modLoc("block/volcano_core"));
        // FFA merge loss restore: block-модель volcano_rad_core потеряна при мердже (осталась item).
        getVariantBuilder(ModBlocks.VOLCANO_RAD_CORE.get()).partialState().setModels(
                ConfiguredModel.builder().modelFile(
                        models().cubeAll(
                                ModBlocks.VOLCANO_RAD_CORE.getId().getPath(),
                                modLoc("block/volcano_rad_core")
                        )
                ).build()
        );
        itemModels().withExistingParent(ModBlocks.VOLCANO_RAD_CORE.getId().getPath(), modLoc("block/volcano_rad_core"));
        // Вулканические лавы — флюид-блоки (LEVEL×FALLING): модель нужна только для
        // частиц разрушения, реальный рендер — флюидный по текстурам ModFluids
        getVariantBuilder(ModBlocks.VOLCANIC_LAVA_BLOCK.get()).partialState().setModels(
                ConfiguredModel.builder().modelFile(
                        models().cubeAll(
                                ModBlocks.VOLCANIC_LAVA_BLOCK.getId().getPath(),
                                modLoc("block/volcanic_lava_still")
                        )
                ).build()
        );
        getVariantBuilder(ModBlocks.RAD_LAVA_BLOCK.get()).partialState().setModels(
                ConfiguredModel.builder().modelFile(
                        models().cubeAll(
                                ModBlocks.RAD_LAVA_BLOCK.getId().getPath(),
                                modLoc("block/rad_lava_still")
                        )
                ).build()
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
        // ffa-port: item model -> manual assets/hbm_m/models/item/wood_barrier.json
        simpleBlockItem(ModBlocks.WOOD_BARRIER.get(), models().getExistingFile(modLoc("item/wood_barrier")));
        simpleBlockWithItem(ModBlocks.WAND_STRUCTURE_SAVE.get(), models().cubeAll("wand_structure_save", modLoc("block/wand_structure_save")));
        simpleBlockWithItem(ModBlocks.WAND_STRUCTURE_LOAD.get(), models().cubeAll("wand_structure_load", modLoc("block/wand_structure_load")));
        simpleBlockWithItem(ModBlocks.LOGIC_BLOCK_INVIS.get(), models().cubeAll("logic_block_invis", modLoc("block/ported/logic_block")));
        // FFA port: corium finite fluid levels (finiteFluid helper kept from FFA)
        finiteFluid(ModBlocks.CORIUM_BLOCK.get(), "corium_block", "block/fluid/corium_still", 5);
        registerPortedBlockstates();
    }

    /**
     * Порт рукописных blockstates из assets (FFA-канон), собранный из фрагментов
     * параллельных портеров (batch_1..batch_12). Один блок — одна регистрация:
     * дубликаты отбрасываются (первое вхождение выигрывает); блоки, уже
     * зарегистрированные выше по файлу, портерами исключены как redundant.
     */
    private void registerPortedBlockstates() {
        getVariantBuilder(ModBlocks.ANTENNA_TOP.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/antenna_top"))).addModel();
        getVariantBuilder(ModBlocks.ANTENNA_TOP.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/antenna_top"))).addModel();
        getVariantBuilder(ModBlocks.ANTENNA_TOP.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/antenna_top"))).addModel();
        getVariantBuilder(ModBlocks.ANTENNA_TOP.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/antenna_top"))).addModel();
        getVariantBuilder(ModBlocks.B29.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/b29"))).addModel();
        var balefireBuilder = getMultipartBuilder(ModBlocks.BALEFIRE.get());
        net.minecraft.world.level.block.state.properties.BooleanProperty balefireNorth = net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH;
        net.minecraft.world.level.block.state.properties.BooleanProperty balefireEast = net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST;
        net.minecraft.world.level.block.state.properties.BooleanProperty balefireSouth = net.minecraft.world.level.block.state.properties.BlockStateProperties.SOUTH;
        net.minecraft.world.level.block.state.properties.BooleanProperty balefireWest = net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST;
        net.minecraft.world.level.block.state.properties.BooleanProperty balefireUp = net.minecraft.world.level.block.state.properties.BlockStateProperties.UP;
        balefireBuilder.part().modelFile(models().getExistingFile(modLoc("block/bomb/balefire_floor"))).addModel().condition(balefireNorth, false).condition(balefireEast, false).condition(balefireSouth, false).condition(balefireWest, false).condition(balefireUp, false).end();
        balefireBuilder.part().modelFile(models().getExistingFile(modLoc("block/bomb/balefire_side"))).nextModel().modelFile(models().getExistingFile(modLoc("block/bomb/balefire_side_alt"))).addModel().useOr().nestedGroup().condition(balefireNorth, true).end().nestedGroup().condition(balefireEast, false).end().nestedGroup().condition(balefireNorth, false).end().nestedGroup().condition(balefireSouth, false).end().nestedGroup().condition(balefireUp, false).end().nestedGroup().condition(balefireWest, false).end().end();
        balefireBuilder.part().modelFile(models().getExistingFile(modLoc("block/bomb/balefire_side"))).rotationY(90).nextModel().modelFile(models().getExistingFile(modLoc("block/bomb/balefire_side_alt"))).rotationY(90).addModel().useOr().nestedGroup().condition(balefireEast, true).end().nestedGroup().condition(balefireEast, false).end().nestedGroup().condition(balefireNorth, false).end().nestedGroup().condition(balefireSouth, false).end().nestedGroup().condition(balefireUp, false).end().nestedGroup().condition(balefireWest, false).end().end();
        balefireBuilder.part().modelFile(models().getExistingFile(modLoc("block/bomb/balefire_side"))).rotationY(180).nextModel().modelFile(models().getExistingFile(modLoc("block/bomb/balefire_side_alt"))).rotationY(180).addModel().useOr().nestedGroup().condition(balefireSouth, true).end().nestedGroup().condition(balefireEast, false).end().nestedGroup().condition(balefireNorth, false).end().nestedGroup().condition(balefireSouth, false).end().nestedGroup().condition(balefireUp, false).end().nestedGroup().condition(balefireWest, false).end().end();
        balefireBuilder.part().modelFile(models().getExistingFile(modLoc("block/bomb/balefire_side"))).rotationY(270).nextModel().modelFile(models().getExistingFile(modLoc("block/bomb/balefire_side_alt"))).rotationY(270).addModel().useOr().nestedGroup().condition(balefireWest, true).end().nestedGroup().condition(balefireEast, false).end().nestedGroup().condition(balefireNorth, false).end().nestedGroup().condition(balefireSouth, false).end().nestedGroup().condition(balefireUp, false).end().nestedGroup().condition(balefireWest, false).end().end();
        balefireBuilder.part().modelFile(models().getExistingFile(modLoc("block/bomb/balefire_up"))).nextModel().modelFile(models().getExistingFile(modLoc("block/bomb/balefire_up_alt"))).addModel().condition(balefireUp, true).end();
        getVariantBuilder(ModBlocks.BARRICADE.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/deco/barricade"))).addModel();
        getVariantBuilder(ModBlocks.BLAST_DOOR.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/blast_door_particle"))).addModel();
        getVariantBuilder(ModBlocks.BLAST_DOOR.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/blast_door_particle"))).addModel();
        getVariantBuilder(ModBlocks.BLAST_DOOR.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/blast_door_particle"))).addModel();
        getVariantBuilder(ModBlocks.BLAST_DOOR.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/blast_door_particle"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DETECTOR.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_detector"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DETECTOR.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_detector"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DETECTOR.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_detector"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DETECTOR.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DETECTOR.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_detector_aluminum"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DETECTOR.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_detector_aluminum"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DETECTOR.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_detector_aluminum"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DETECTOR.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DETECTOR.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_detector_out"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DETECTOR.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_detector_out"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DETECTOR.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_detector_out"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DETECTOR.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DETECTOR.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_detector_out_aluminum"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DETECTOR.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_detector_out_aluminum"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DETECTOR.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_detector_out_aluminum"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DETECTOR.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DRILLED.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_drilled"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DRILLED.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_drilled"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DRILLED.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_drilled"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DRILLED.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DRILLED.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_drilled_aluminum"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DRILLED.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_drilled_aluminum"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DRILLED.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_drilled_aluminum"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DRILLED.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DRILLED.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_drilled"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DRILLED.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_drilled"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DRILLED.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_drilled"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DRILLED.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DRILLED.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_drilled_aluminum"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DRILLED.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_drilled_aluminum"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DRILLED.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_drilled_aluminum"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_DRILLED.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_FUEL.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_fuel"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_FUEL.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_fuel"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_FUEL.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_fuel"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_FUEL.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_FUEL.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_fuel_aluminum"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_FUEL.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_fuel_aluminum"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_FUEL.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_fuel_aluminum"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_FUEL.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_FUEL.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_fuel"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_FUEL.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_fuel"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_FUEL.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_fuel"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_FUEL.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_FUEL.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_fuel_aluminum"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_FUEL.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_fuel_aluminum"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_FUEL.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_fuel_aluminum"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_FUEL.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_LITHIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_lithium"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_LITHIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_lithium"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_LITHIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_lithium"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_LITHIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_LITHIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_lithium_aluminum"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_LITHIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_lithium_aluminum"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_LITHIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_lithium_aluminum"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_LITHIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_LITHIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_lithium"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_LITHIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_lithium"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_LITHIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_lithium"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_LITHIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_LITHIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_lithium_aluminum"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_LITHIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_lithium_aluminum"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_LITHIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_lithium_aluminum"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_LITHIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_plutonium"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_plutonium"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_plutonium"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_plutonium_aluminum"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_plutonium_aluminum"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_plutonium_aluminum"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_plutonium"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_plutonium"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_plutonium"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_plutonium_aluminum"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_plutonium_aluminum"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_plutonium_aluminum"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_PLUTONIUM.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_ROD.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_rod_in"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_ROD.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_rod_in"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_ROD.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_rod_in"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_ROD.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_ROD.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_rod_in_aluminum"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_ROD.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_rod_in_aluminum"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_ROD.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_rod_in_aluminum"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_ROD.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_ROD.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_rod_out"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_ROD.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_rod_out"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_ROD.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_rod_out"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_ROD.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_ROD.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_rod_out_aluminum"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_ROD.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_rod_out_aluminum"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_ROD.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_rod_out_aluminum"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_ROD.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_SOURCE.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_source"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_SOURCE.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_source"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_SOURCE.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_source"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_SOURCE.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_SOURCE.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_source_aluminum"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_SOURCE.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_source_aluminum"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_SOURCE.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_source_aluminum"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_SOURCE.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_SOURCE.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_source"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_SOURCE.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_source"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_SOURCE.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_source"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_SOURCE.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_SOURCE.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_source_aluminum"))).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_SOURCE.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_source_aluminum"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_SOURCE.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/block_graphite_source_aluminum"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.BLOCK_GRAPHITE_SOURCE.get()).partialState().with(com.hbm_m.block.machines.pile.GraphitePileBlocks.META, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/pile_mk1/plain"))).addModel();
        getVariantBuilder(ModBlocks.BOAT.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/boat_particle"))).addModel();
        getVariantBuilder(ModBlocks.BOAT.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/boat_particle"))).addModel();
        getVariantBuilder(ModBlocks.BOAT.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/boat_particle"))).addModel();
        getVariantBuilder(ModBlocks.BOAT.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/boat_particle"))).addModel();
        getVariantBuilder(ModBlocks.BOAT.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/boat_particle"))).addModel();
        getVariantBuilder(ModBlocks.BOAT.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/boat_particle"))).addModel();
        getVariantBuilder(ModBlocks.BOBBLEHEAD.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.ROTATION_16, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/bobblehead_particle"))).addModel();
        getVariantBuilder(ModBlocks.BOBBLEHEAD.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.ROTATION_16, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/bobblehead_particle"))).addModel();
        getVariantBuilder(ModBlocks.BOBBLEHEAD.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.ROTATION_16, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/bobblehead_particle"))).addModel();
        getVariantBuilder(ModBlocks.BOBBLEHEAD.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.ROTATION_16, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/bobblehead_particle"))).addModel();
        getVariantBuilder(ModBlocks.BOBBLEHEAD.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.ROTATION_16, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/bobblehead_particle"))).addModel();
        getVariantBuilder(ModBlocks.BOBBLEHEAD.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.ROTATION_16, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/bobblehead_particle"))).addModel();
        getVariantBuilder(ModBlocks.BOBBLEHEAD.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.ROTATION_16, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/bobblehead_particle"))).addModel();
        getVariantBuilder(ModBlocks.BOBBLEHEAD.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.ROTATION_16, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/bobblehead_particle"))).addModel();
        getVariantBuilder(ModBlocks.BOBBLEHEAD.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.ROTATION_16, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/bobblehead_particle"))).addModel();
        getVariantBuilder(ModBlocks.BOBBLEHEAD.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.ROTATION_16, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/bobblehead_particle"))).addModel();
        getVariantBuilder(ModBlocks.BOBBLEHEAD.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.ROTATION_16, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/bobblehead_particle"))).addModel();
        getVariantBuilder(ModBlocks.BOBBLEHEAD.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.ROTATION_16, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/bobblehead_particle"))).addModel();
        getVariantBuilder(ModBlocks.BOBBLEHEAD.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.ROTATION_16, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/bobblehead_particle"))).addModel();
        getVariantBuilder(ModBlocks.BOBBLEHEAD.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.ROTATION_16, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/bobblehead_particle"))).addModel();
        getVariantBuilder(ModBlocks.BOBBLEHEAD.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.ROTATION_16, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/bobblehead_particle"))).addModel();
        getVariantBuilder(ModBlocks.BOBBLEHEAD.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.ROTATION_16, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/bobblehead_particle"))).addModel();
        getVariantBuilder(ModBlocks.BRICK_FORGOTTEN_LOCK.get()).partialState().with(com.hbm_m.block.generic.ForgottenBricks.Lock.META, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/brick_forgotten_lock_0"))).addModel();
        getVariantBuilder(ModBlocks.BRICK_FORGOTTEN_LOCK.get()).partialState().with(com.hbm_m.block.generic.ForgottenBricks.Lock.META, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/brick_forgotten_lock_1"))).addModel();
        getVariantBuilder(ModBlocks.BRICK_FORGOTTEN_LOCK.get()).partialState().with(com.hbm_m.block.generic.ForgottenBricks.Lock.META, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/brick_forgotten_lock_2"))).addModel();
        getVariantBuilder(ModBlocks.BRICK_FORGOTTEN_LOCK.get()).partialState().with(com.hbm_m.block.generic.ForgottenBricks.Lock.META, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/brick_forgotten_lock_3"))).addModel();
        getVariantBuilder(ModBlocks.BRICK_JUNGLE_GLYPH.get()).partialState().with(com.hbm_m.block.generic.JungleBricks.Glyph.GLYPH, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/brick_jungle_glyph_0"))).addModel();
        getVariantBuilder(ModBlocks.BRICK_JUNGLE_GLYPH.get()).partialState().with(com.hbm_m.block.generic.JungleBricks.Glyph.GLYPH, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/brick_jungle_glyph_1"))).addModel();
        getVariantBuilder(ModBlocks.BRICK_JUNGLE_GLYPH.get()).partialState().with(com.hbm_m.block.generic.JungleBricks.Glyph.GLYPH, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/brick_jungle_glyph_2"))).addModel();
        getVariantBuilder(ModBlocks.BRICK_JUNGLE_GLYPH.get()).partialState().with(com.hbm_m.block.generic.JungleBricks.Glyph.GLYPH, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/brick_jungle_glyph_3"))).addModel();
        getVariantBuilder(ModBlocks.BRICK_JUNGLE_GLYPH.get()).partialState().with(com.hbm_m.block.generic.JungleBricks.Glyph.GLYPH, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/brick_jungle_glyph_4"))).addModel();
        getVariantBuilder(ModBlocks.BRICK_JUNGLE_GLYPH.get()).partialState().with(com.hbm_m.block.generic.JungleBricks.Glyph.GLYPH, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/brick_jungle_glyph_5"))).addModel();
        getVariantBuilder(ModBlocks.BRICK_JUNGLE_GLYPH.get()).partialState().with(com.hbm_m.block.generic.JungleBricks.Glyph.GLYPH, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/brick_jungle_glyph_6"))).addModel();
        getVariantBuilder(ModBlocks.BRICK_JUNGLE_GLYPH.get()).partialState().with(com.hbm_m.block.generic.JungleBricks.Glyph.GLYPH, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/brick_jungle_glyph_7"))).addModel();
        getVariantBuilder(ModBlocks.BRICK_JUNGLE_GLYPH.get()).partialState().with(com.hbm_m.block.generic.JungleBricks.Glyph.GLYPH, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/brick_jungle_glyph_8"))).addModel();
        getVariantBuilder(ModBlocks.BRICK_JUNGLE_GLYPH.get()).partialState().with(com.hbm_m.block.generic.JungleBricks.Glyph.GLYPH, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/brick_jungle_glyph_9"))).addModel();
        getVariantBuilder(ModBlocks.BRICK_JUNGLE_GLYPH.get()).partialState().with(com.hbm_m.block.generic.JungleBricks.Glyph.GLYPH, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/brick_jungle_glyph_10"))).addModel();
        getVariantBuilder(ModBlocks.BRICK_JUNGLE_GLYPH.get()).partialState().with(com.hbm_m.block.generic.JungleBricks.Glyph.GLYPH, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/brick_jungle_glyph_11"))).addModel();
        getVariantBuilder(ModBlocks.BRICK_JUNGLE_GLYPH.get()).partialState().with(com.hbm_m.block.generic.JungleBricks.Glyph.GLYPH, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/brick_jungle_glyph_12"))).addModel();
        getVariantBuilder(ModBlocks.BRICK_JUNGLE_GLYPH.get()).partialState().with(com.hbm_m.block.generic.JungleBricks.Glyph.GLYPH, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/brick_jungle_glyph_13"))).addModel();
        getVariantBuilder(ModBlocks.BRICK_JUNGLE_GLYPH.get()).partialState().with(com.hbm_m.block.generic.JungleBricks.Glyph.GLYPH, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/brick_jungle_glyph_14"))).addModel();
        getVariantBuilder(ModBlocks.BRICK_JUNGLE_GLYPH.get()).partialState().with(com.hbm_m.block.generic.JungleBricks.Glyph.GLYPH, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/brick_jungle_glyph_15"))).addModel();
        customObjBlockNoFacing(ModBlocks.BROADCASTER);
        // cargo_elevator.json — CargoElevatorBlock has no blockstate properties; single model block/cargo_elevator
        customObjBlockNoFacing(ModBlocks.CARGO_ELEVATOR);
        // conveyor.json — ConveyorBlock (ConveyorBlockBase.FACING + ConveyorBendableBlock.BEND), 12 variants
        getVariantBuilder(ModBlocks.CONVEYOR.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.NORTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.STRAIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor"))).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.NORTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.LEFT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_curve_left"))).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.NORTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.RIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_curve_right"))).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.EAST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.STRAIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.EAST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.LEFT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_curve_left"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.EAST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.RIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_curve_right"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.SOUTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.STRAIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.SOUTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.LEFT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_curve_left"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.SOUTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.RIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_curve_right"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.WEST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.STRAIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.WEST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.LEFT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_curve_left"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.WEST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.RIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_curve_right"))).rotationY(270).addModel();
        // conveyor_chute.json — ConveyorChuteBlock (ConveyorBlockBase.FACING only), single model
        getVariantBuilder(ModBlocks.CONVEYOR_CHUTE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_chute"))).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_CHUTE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_chute"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_CHUTE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_chute"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_CHUTE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_chute"))).rotationY(270).addModel();
        // conveyor_double.json — ConveyorDoubleBlock, 12 variants
        getVariantBuilder(ModBlocks.CONVEYOR_DOUBLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.NORTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.STRAIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_double"))).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_DOUBLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.NORTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.LEFT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_double_curve_left"))).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_DOUBLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.NORTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.RIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_double_curve_right"))).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_DOUBLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.EAST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.STRAIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_double"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_DOUBLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.EAST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.LEFT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_double_curve_left"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_DOUBLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.EAST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.RIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_double_curve_right"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_DOUBLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.SOUTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.STRAIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_double"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_DOUBLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.SOUTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.LEFT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_double_curve_left"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_DOUBLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.SOUTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.RIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_double_curve_right"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_DOUBLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.WEST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.STRAIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_double"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_DOUBLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.WEST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.LEFT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_double_curve_left"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_DOUBLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.WEST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.RIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_double_curve_right"))).rotationY(270).addModel();
        // conveyor_express.json — ConveyorExpressBlock, 12 variants
        getVariantBuilder(ModBlocks.CONVEYOR_EXPRESS.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.NORTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.STRAIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_express"))).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_EXPRESS.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.NORTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.LEFT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_express_curve_left"))).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_EXPRESS.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.NORTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.RIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_express_curve_right"))).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_EXPRESS.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.EAST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.STRAIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_express"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_EXPRESS.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.EAST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.LEFT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_express_curve_left"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_EXPRESS.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.EAST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.RIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_express_curve_right"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_EXPRESS.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.SOUTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.STRAIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_express"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_EXPRESS.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.SOUTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.LEFT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_express_curve_left"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_EXPRESS.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.SOUTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.RIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_express_curve_right"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_EXPRESS.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.WEST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.STRAIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_express"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_EXPRESS.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.WEST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.LEFT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_express_curve_left"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_EXPRESS.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.WEST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.RIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_express_curve_right"))).rotationY(270).addModel();
        // conveyor_lift.json — ConveyorLiftBlock (ConveyorBlockBase.FACING only), single model
        getVariantBuilder(ModBlocks.CONVEYOR_LIFT.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_lift"))).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_LIFT.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_lift"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_LIFT.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_lift"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_LIFT.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_lift"))).rotationY(270).addModel();
        // conveyor_triple.json — ConveyorTripleBlock, 12 variants
        getVariantBuilder(ModBlocks.CONVEYOR_TRIPLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.NORTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.STRAIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_triple"))).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_TRIPLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.NORTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.LEFT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_triple_curve_left"))).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_TRIPLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.NORTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.RIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_triple_curve_right"))).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_TRIPLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.EAST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.STRAIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_triple"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_TRIPLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.EAST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.LEFT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_triple_curve_left"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_TRIPLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.EAST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.RIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_triple_curve_right"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_TRIPLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.SOUTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.STRAIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_triple"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_TRIPLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.SOUTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.LEFT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_triple_curve_left"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_TRIPLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.SOUTH).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.RIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_triple_curve_right"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_TRIPLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.WEST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.STRAIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_triple"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_TRIPLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.WEST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.LEFT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_triple_curve_left"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.CONVEYOR_TRIPLE.get()).partialState().with(com.hbm_m.block.network.ConveyorBlockBase.FACING, Direction.WEST).with(com.hbm_m.block.network.ConveyorBendableBlock.BEND, com.hbm_m.block.network.ConveyorBend.RIGHT).modelForState().modelFile(models().getExistingFile(modLoc("block/network/conveyor_triple_curve_right"))).rotationY(270).addModel();
        // crt_blinking.json — DecoObjBlock (HORIZONTAL_FACING); json rotation map: south=270, west=0, north=90, east=180
        getVariantBuilder(ModBlocks.CRT_BLINKING.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/crt_blinking"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.CRT_BLINKING.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/crt_blinking"))).addModel();
        getVariantBuilder(ModBlocks.CRT_BLINKING.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/crt_blinking"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.CRT_BLINKING.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/crt_blinking"))).rotationY(180).addModel();
        // crt_broken.json
        getVariantBuilder(ModBlocks.CRT_BROKEN.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/crt_broken"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.CRT_BROKEN.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/crt_broken"))).addModel();
        getVariantBuilder(ModBlocks.CRT_BROKEN.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/crt_broken"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.CRT_BROKEN.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/crt_broken"))).rotationY(180).addModel();
        // crt_bsod.json
        getVariantBuilder(ModBlocks.CRT_BSOD.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/crt_bsod"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.CRT_BSOD.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/crt_bsod"))).addModel();
        getVariantBuilder(ModBlocks.CRT_BSOD.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/crt_bsod"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.CRT_BSOD.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/crt_bsod"))).rotationY(180).addModel();
        // crt_clean.json
        getVariantBuilder(ModBlocks.CRT_CLEAN.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/crt_clean"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.CRT_CLEAN.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/crt_clean"))).addModel();
        getVariantBuilder(ModBlocks.CRT_CLEAN.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/crt_clean"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.CRT_CLEAN.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/crt_clean"))).rotationY(180).addModel();
        // crucible.json — MachineCrucibleBlock inherits DummyableMachineBlock.FACING (HorizontalDirectionalBlock.FACING)
        getVariantBuilder(ModBlocks.CRUCIBLE.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/crucible"))).addModel();
        getVariantBuilder(ModBlocks.CRUCIBLE.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/crucible"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.CRUCIBLE.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/crucible"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.CRUCIBLE.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/crucible"))).rotationY(90).addModel();
        // deco_emitter.json — EmitterBlock.FACING = BlockStateProperties.FACING (all 6), all variants same model, no rotation
        getVariantBuilder(ModBlocks.DECO_EMITTER.get()).partialState().with(com.hbm_m.block.decorations.EmitterBlock.FACING, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/deco_emitter"))).addModel();
        getVariantBuilder(ModBlocks.DECO_EMITTER.get()).partialState().with(com.hbm_m.block.decorations.EmitterBlock.FACING, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/deco_emitter"))).addModel();
        getVariantBuilder(ModBlocks.DECO_EMITTER.get()).partialState().with(com.hbm_m.block.decorations.EmitterBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/deco_emitter"))).addModel();
        getVariantBuilder(ModBlocks.DECO_EMITTER.get()).partialState().with(com.hbm_m.block.decorations.EmitterBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/deco_emitter"))).addModel();
        getVariantBuilder(ModBlocks.DECO_EMITTER.get()).partialState().with(com.hbm_m.block.decorations.EmitterBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/deco_emitter"))).addModel();
        getVariantBuilder(ModBlocks.DECO_EMITTER.get()).partialState().with(com.hbm_m.block.decorations.EmitterBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/deco_emitter"))).addModel();
        // cage_lamp.json — SpotlightBlock.FACING (all 6) + SpotlightBlock.BROKEN; model per facing, no rotation; broken=true/false share the model
        getVariantBuilder(ModBlocks.CAGE_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_down"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_down"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_up"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_up"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_north"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_north"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_south"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_south"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_west"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_west"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_east"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_east"))).addModel();
        // cage_lamp_off.json — same SpotlightBlock properties, off model set
        getVariantBuilder(ModBlocks.CAGE_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_off_down"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_off_down"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_off_up"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_off_up"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_off_north"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_off_north"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_off_south"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_off_south"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_off_west"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_off_west"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_off_east"))).addModel();
        getVariantBuilder(ModBlocks.CAGE_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/cage_lamp_off_east"))).addModel();
        // deco_pipe_green.json (DecoPipeBlock extends RotatedPillarBlock -> AXIS only)
        getVariantBuilder(ModBlocks.DECO_PIPE_GREEN.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.X).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_green_x"))).addModel();
        getVariantBuilder(ModBlocks.DECO_PIPE_GREEN.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Y).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_green_y"))).addModel();
        getVariantBuilder(ModBlocks.DECO_PIPE_GREEN.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Z).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_green_z"))).addModel();
        // deco_pipe_green_rusted.json
        getVariantBuilder(ModBlocks.DECO_PIPE_GREEN_RUSTED.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.X).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_green_rusted_x"))).addModel();
        getVariantBuilder(ModBlocks.DECO_PIPE_GREEN_RUSTED.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Y).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_green_rusted_y"))).addModel();
        getVariantBuilder(ModBlocks.DECO_PIPE_GREEN_RUSTED.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Z).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_green_rusted_z"))).addModel();
        // deco_pipe_quad_green.json
        getVariantBuilder(ModBlocks.DECO_PIPE_QUAD_GREEN.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.X).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_quad_green_x"))).addModel();
        getVariantBuilder(ModBlocks.DECO_PIPE_QUAD_GREEN.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Y).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_quad_green_y"))).addModel();
        getVariantBuilder(ModBlocks.DECO_PIPE_QUAD_GREEN.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Z).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_quad_green_z"))).addModel();
        // deco_pipe_quad_green_rusted.json
        getVariantBuilder(ModBlocks.DECO_PIPE_QUAD_GREEN_RUSTED.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.X).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_quad_green_rusted_x"))).addModel();
        getVariantBuilder(ModBlocks.DECO_PIPE_QUAD_GREEN_RUSTED.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Y).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_quad_green_rusted_y"))).addModel();
        getVariantBuilder(ModBlocks.DECO_PIPE_QUAD_GREEN_RUSTED.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Z).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_quad_green_rusted_z"))).addModel();
        // deco_pipe_rim_green.json
        getVariantBuilder(ModBlocks.DECO_PIPE_RIM_GREEN.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.X).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_rim_green_x"))).addModel();
        getVariantBuilder(ModBlocks.DECO_PIPE_RIM_GREEN.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Y).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_rim_green_y"))).addModel();
        getVariantBuilder(ModBlocks.DECO_PIPE_RIM_GREEN.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Z).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_rim_green_z"))).addModel();
        // deco_pipe_rim_green_rusted.json
        getVariantBuilder(ModBlocks.DECO_PIPE_RIM_GREEN_RUSTED.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.X).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_rim_green_rusted_x"))).addModel();
        getVariantBuilder(ModBlocks.DECO_PIPE_RIM_GREEN_RUSTED.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Y).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_rim_green_rusted_y"))).addModel();
        getVariantBuilder(ModBlocks.DECO_PIPE_RIM_GREEN_RUSTED.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Z).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_rim_green_rusted_z"))).addModel();
        // deco_pipe_rim_red.json
        getVariantBuilder(ModBlocks.DECO_PIPE_RIM_RED.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.X).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_rim_red_x"))).addModel();
        getVariantBuilder(ModBlocks.DECO_PIPE_RIM_RED.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Y).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_rim_red_y"))).addModel();
        getVariantBuilder(ModBlocks.DECO_PIPE_RIM_RED.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Z).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_rim_red_z"))).addModel();
        // deco_pipe_framed_green.json
        getVariantBuilder(ModBlocks.DECO_PIPE_FRAMED_GREEN.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.X).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_framed_green_x"))).addModel();
        getVariantBuilder(ModBlocks.DECO_PIPE_FRAMED_GREEN.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Y).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_framed_green_y"))).addModel();
        getVariantBuilder(ModBlocks.DECO_PIPE_FRAMED_GREEN.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Z).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_framed_green_z"))).addModel();
        // deco_pipe_framed_marked.json
        getVariantBuilder(ModBlocks.DECO_PIPE_FRAMED_MARKED.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.X).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_framed_marked_x"))).addModel();
        getVariantBuilder(ModBlocks.DECO_PIPE_FRAMED_MARKED.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Y).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_framed_marked_y"))).addModel();
        getVariantBuilder(ModBlocks.DECO_PIPE_FRAMED_MARKED.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS, Direction.Axis.Z).modelForState().modelFile(models().getExistingFile(modLoc("block/pipes/deco_pipe_framed_marked_z"))).addModel();
        // det_charge.json (ExplosiveCharge -> BlockDetonatable -> BlockFlammable -> Block, no properties; single variant;
        // customObjBlockNoFacing not usable because it derives model path "block/det_charge" but the model lives at "block/bomb/det_charge")
        getVariantBuilder(ModBlocks.DET_CHARGE.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/bomb/det_charge"))).addModel();
        // det_cord — DetCordBlock uses PipeBlock.PROPERTY_BY_DIRECTION (BlockStateProperties NORTH/SOUTH/EAST/WEST/UP/DOWN);
        // hand-written json is multipart with OR-groups; the AND/OR forms below were verified equivalent on all 64 states.
        var dcB = ModBlocks.DET_CORD.get();
        var dcN = net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH;
        var dcS = net.minecraft.world.level.block.state.properties.BlockStateProperties.SOUTH;
        var dcE = net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST;
        var dcW = net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST;
        var dcU = net.minecraft.world.level.block.state.properties.BlockStateProperties.UP;
        var dcD = net.minecraft.world.level.block.state.properties.BlockStateProperties.DOWN;
        var dcb = getMultipartBuilder(dcB);
        // cx: (east|west) and not(north|south|up|down)
        dcb.part().modelFile(models().getExistingFile(modLoc("block/bomb/det_cord_cx"))).addModel()
            .nestedGroup().useOr().condition(dcE, true).condition(dcW, true).end()
            .nestedGroup().useOr().condition(dcN, false).condition(dcS, false).condition(dcU, false).condition(dcD, false).end()
            .end();
        // cy: (up|down) and not(east|west|north|south)
        dcb.part().modelFile(models().getExistingFile(modLoc("block/bomb/det_cord_cy"))).addModel()
            .nestedGroup().useOr().condition(dcU, true).condition(dcD, true).end()
            .nestedGroup().useOr().condition(dcE, false).condition(dcW, false).condition(dcN, false).condition(dcS, false).end()
            .end();
        // cz: (north|south) and not(east|west|up|down)
        dcb.part().modelFile(models().getExistingFile(modLoc("block/bomb/det_cord_cz"))).addModel()
            .nestedGroup().useOr().condition(dcN, true).condition(dcS, true).end()
            .nestedGroup().useOr().condition(dcE, false).condition(dcW, false).condition(dcU, false).condition(dcD, false).end()
            .end();
        // core: not(cx|cy|cz) == AND of the three negated axis-corner groups (verified: 55 of 64 states, exactly the json's OR-list)
        dcb.part().modelFile(models().getExistingFile(modLoc("block/bomb/det_cord_core"))).addModel()
            .nestedGroup().useOr()
                .nestedGroup().condition(dcN, false).condition(dcS, false).endNestedGroup()
                .nestedGroup().condition(dcE, true).endNestedGroup()
                .nestedGroup().condition(dcW, true).endNestedGroup()
                .nestedGroup().condition(dcU, true).endNestedGroup()
                .nestedGroup().condition(dcD, true).endNestedGroup()
            .end()
            .nestedGroup().useOr()
                .nestedGroup().condition(dcU, false).condition(dcD, false).endNestedGroup()
                .nestedGroup().condition(dcE, true).endNestedGroup()
                .nestedGroup().condition(dcW, true).endNestedGroup()
                .nestedGroup().condition(dcN, true).endNestedGroup()
                .nestedGroup().condition(dcS, true).endNestedGroup()
            .end()
            .nestedGroup().useOr()
                .nestedGroup().condition(dcE, false).condition(dcW, false).endNestedGroup()
                .nestedGroup().condition(dcN, true).endNestedGroup()
                .nestedGroup().condition(dcS, true).endNestedGroup()
                .nestedGroup().condition(dcU, true).endNestedGroup()
                .nestedGroup().condition(dcD, true).endNestedGroup()
            .end()
            .end();
        // posx: east and (north|south|up|down)
        dcb.part().modelFile(models().getExistingFile(modLoc("block/bomb/det_cord_posx"))).addModel()
            .nestedGroup().condition(dcE, true).end()
            .nestedGroup().useOr().condition(dcN, true).condition(dcS, true).condition(dcU, true).condition(dcD, true).end()
            .end();
        // negx: west and (north|south|up|down)
        dcb.part().modelFile(models().getExistingFile(modLoc("block/bomb/det_cord_negx"))).addModel()
            .nestedGroup().condition(dcW, true).end()
            .nestedGroup().useOr().condition(dcN, true).condition(dcS, true).condition(dcU, true).condition(dcD, true).end()
            .end();
        // posy: up and (east|west|north|south)
        dcb.part().modelFile(models().getExistingFile(modLoc("block/bomb/det_cord_posy"))).addModel()
            .nestedGroup().condition(dcU, true).end()
            .nestedGroup().useOr().condition(dcE, true).condition(dcW, true).condition(dcN, true).condition(dcS, true).end()
            .end();
        // negy: down and (east|west|north|south)
        dcb.part().modelFile(models().getExistingFile(modLoc("block/bomb/det_cord_negy"))).addModel()
            .nestedGroup().condition(dcD, true).end()
            .nestedGroup().useOr().condition(dcE, true).condition(dcW, true).condition(dcN, true).condition(dcS, true).end()
            .end();
        // posz (json semantics): north and (east|west|up|down)
        dcb.part().modelFile(models().getExistingFile(modLoc("block/bomb/det_cord_posz"))).addModel()
            .nestedGroup().condition(dcN, true).end()
            .nestedGroup().useOr().condition(dcE, true).condition(dcW, true).condition(dcU, true).condition(dcD, true).end()
            .end();
        // negz (json semantics): south and (east|west|up|down)
        dcb.part().modelFile(models().getExistingFile(modLoc("block/bomb/det_cord_negz"))).addModel()
            .nestedGroup().condition(dcS, true).end()
            .nestedGroup().useOr().condition(dcE, true).condition(dcW, true).condition(dcU, true).condition(dcD, true).end()
            .end();
        // det_nuke — ExplosiveCharge (no blockstate properties), single variant
        getVariantBuilder(ModBlocks.DET_NUKE.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/bomb/det_nuke"))).addModel();
        // deuterium_tower — MachineDeuteriumTowerBlock.FACING (horizontal); model lives under machines/, so customObjBlockRotated's
        // id-derived path would not resolve; explicit form with the json's y rotations.
        getVariantBuilder(ModBlocks.DEUTERIUM_TOWER.get()).partialState().with(com.hbm_m.block.machines.MachineDeuteriumTowerBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/deuterium_tower"))).rotationY(0).addModel();
        getVariantBuilder(ModBlocks.DEUTERIUM_TOWER.get()).partialState().with(com.hbm_m.block.machines.MachineDeuteriumTowerBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/deuterium_tower"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.DEUTERIUM_TOWER.get()).partialState().with(com.hbm_m.block.machines.MachineDeuteriumTowerBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/deuterium_tower"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.DEUTERIUM_TOWER.get()).partialState().with(com.hbm_m.block.machines.MachineDeuteriumTowerBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/deuterium_tower"))).rotationY(90).addModel();
        // door_red — BlockModDoor extends vanilla DoorBlock (FACING/HALF/HINGE/OPEN; POWERED not keyed in json)
        var drF = net.minecraft.world.level.block.DoorBlock.FACING;
        var drH = net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF;
        var drG = net.minecraft.world.level.block.state.properties.BlockStateProperties.DOOR_HINGE;
        var drO = net.minecraft.world.level.block.DoorBlock.OPEN;
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.EAST).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT).with(drO, false).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_bottom_left"))).rotationY(0).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.EAST).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT).with(drO, true).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_bottom_left_open"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.EAST).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT).with(drO, false).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_bottom_right"))).rotationY(0).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.EAST).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT).with(drO, true).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_bottom_right_open"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.EAST).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT).with(drO, false).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_top_left"))).rotationY(0).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.EAST).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT).with(drO, true).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_top_left_open"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.EAST).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT).with(drO, false).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_top_right"))).rotationY(0).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.EAST).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT).with(drO, true).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_top_right_open"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.NORTH).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT).with(drO, false).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_bottom_left"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.NORTH).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT).with(drO, true).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_bottom_left_open"))).rotationY(0).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.NORTH).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT).with(drO, false).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_bottom_right"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.NORTH).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT).with(drO, true).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_bottom_right_open"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.NORTH).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT).with(drO, false).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_top_left"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.NORTH).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT).with(drO, true).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_top_left_open"))).rotationY(0).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.NORTH).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT).with(drO, false).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_top_right"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.NORTH).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT).with(drO, true).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_top_right_open"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.SOUTH).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT).with(drO, false).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_bottom_left"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.SOUTH).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT).with(drO, true).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_bottom_left_open"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.SOUTH).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT).with(drO, false).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_bottom_right"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.SOUTH).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT).with(drO, true).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_bottom_right_open"))).rotationY(0).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.SOUTH).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT).with(drO, false).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_top_left"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.SOUTH).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT).with(drO, true).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_top_left_open"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.SOUTH).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT).with(drO, false).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_top_right"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.SOUTH).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT).with(drO, true).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_top_right_open"))).rotationY(0).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.WEST).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT).with(drO, false).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_bottom_left"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.WEST).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT).with(drO, true).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_bottom_left_open"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.WEST).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT).with(drO, false).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_bottom_right"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.WEST).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT).with(drO, true).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_bottom_right_open"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.WEST).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT).with(drO, false).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_top_left"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.WEST).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT).with(drO, true).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_top_left_open"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.WEST).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT).with(drO, false).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_top_right"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.DOOR_RED_BLOCK.get()).partialState().with(drF, Direction.WEST).with(drH, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER).with(drG, net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT).with(drO, true).modelForState().modelFile(models().getExistingFile(modLoc("block/door_red_top_right_open"))).rotationY(90).addModel();
        // dornier — vanilla net.minecraft.world.level.block.BarrelBlock (FACING/OPEN); json is a single "" variant, one unconditional partial state covers all states
        getVariantBuilder(ModBlocks.DORNIER.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/dornier"))).addModel();
        // dud_balefire — CrashedBombBlock (no blockstate properties)
        getVariantBuilder(ModBlocks.DUD_BALEFIRE.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/bomb/dud_balefire_particle"))).addModel();
        // dud_conventional — CrashedBombBlock
        getVariantBuilder(ModBlocks.DUD_CONVENTIONAL.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/bomb/dud_conventional_particle"))).addModel();
        // dud_nuke — CrashedBombBlock
        getVariantBuilder(ModBlocks.DUD_NUKE.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/bomb/dud_nuke_particle"))).addModel();
        // dud_salted — CrashedBombBlock
        getVariantBuilder(ModBlocks.DUD_SALTED.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/bomb/dud_salted_particle"))).addModel();
        // dungeon_chain — DungeonChainBlock.FACING (all but down) + END
        getVariantBuilder(ModBlocks.DUNGEON_CHAIN.get()).partialState().with(com.hbm_m.block.decorations.DungeonChainBlock.END, false).with(com.hbm_m.block.decorations.DungeonChainBlock.FACING, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/dungeon_chain_hanging"))).addModel();
        getVariantBuilder(ModBlocks.DUNGEON_CHAIN.get()).partialState().with(com.hbm_m.block.decorations.DungeonChainBlock.END, false).with(com.hbm_m.block.decorations.DungeonChainBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/dungeon_chain_wall"))).rotationY(0).addModel();
        getVariantBuilder(ModBlocks.DUNGEON_CHAIN.get()).partialState().with(com.hbm_m.block.decorations.DungeonChainBlock.END, false).with(com.hbm_m.block.decorations.DungeonChainBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/dungeon_chain_wall"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.DUNGEON_CHAIN.get()).partialState().with(com.hbm_m.block.decorations.DungeonChainBlock.END, false).with(com.hbm_m.block.decorations.DungeonChainBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/dungeon_chain_wall"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.DUNGEON_CHAIN.get()).partialState().with(com.hbm_m.block.decorations.DungeonChainBlock.END, false).with(com.hbm_m.block.decorations.DungeonChainBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/dungeon_chain_wall"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.DUNGEON_CHAIN.get()).partialState().with(com.hbm_m.block.decorations.DungeonChainBlock.END, true).with(com.hbm_m.block.decorations.DungeonChainBlock.FACING, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/dungeon_chain_hanging_end"))).addModel();
        getVariantBuilder(ModBlocks.DUNGEON_CHAIN.get()).partialState().with(com.hbm_m.block.decorations.DungeonChainBlock.END, true).with(com.hbm_m.block.decorations.DungeonChainBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/dungeon_chain_wall_end"))).rotationY(0).addModel();
        getVariantBuilder(ModBlocks.DUNGEON_CHAIN.get()).partialState().with(com.hbm_m.block.decorations.DungeonChainBlock.END, true).with(com.hbm_m.block.decorations.DungeonChainBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/dungeon_chain_wall_end"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.DUNGEON_CHAIN.get()).partialState().with(com.hbm_m.block.decorations.DungeonChainBlock.END, true).with(com.hbm_m.block.decorations.DungeonChainBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/dungeon_chain_wall_end"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.DUNGEON_CHAIN.get()).partialState().with(com.hbm_m.block.decorations.DungeonChainBlock.END, true).with(com.hbm_m.block.decorations.DungeonChainBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/dungeon_chain_wall_end"))).rotationY(270).addModel();
        // dynamite — TNTBlocks.Dynamite extends BlockTNTBase (IGNITE_ON_BREAK); both json variants use the same model
        getVariantBuilder(ModBlocks.DYNAMITE.get()).partialState().with(com.hbm_m.block.bomb.BlockTNTBase.IGNITE_ON_BREAK, false).modelForState().modelFile(models().getExistingFile(modLoc("block/bomb/dynamite"))).addModel();
        getVariantBuilder(ModBlocks.DYNAMITE.get()).partialState().with(com.hbm_m.block.bomb.BlockTNTBase.IGNITE_ON_BREAK, true).modelForState().modelFile(models().getExistingFile(modLoc("block/bomb/dynamite"))).addModel();
        // emp_bomb — SimpleBombs.Float (no blockstate properties)
        getVariantBuilder(ModBlocks.EMP_BOMB.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/bomb/emp_bomb"))).addModel();
        // file_cabinet — FileCabinetBlock.FACING (horizontal); json has all 4 facings on one model without rotation
        getVariantBuilder(ModBlocks.FILE_CABINET.get()).partialState().with(com.hbm_m.block.decorations.FileCabinetBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/file_cabinet_particle"))).addModel();
        getVariantBuilder(ModBlocks.FILE_CABINET.get()).partialState().with(com.hbm_m.block.decorations.FileCabinetBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/file_cabinet_particle"))).addModel();
        getVariantBuilder(ModBlocks.FILE_CABINET.get()).partialState().with(com.hbm_m.block.decorations.FileCabinetBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/file_cabinet_particle"))).addModel();
        getVariantBuilder(ModBlocks.FILE_CABINET.get()).partialState().with(com.hbm_m.block.decorations.FileCabinetBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/file_cabinet_particle"))).addModel();
        // file_cabinet_steel — FileCabinetBlock.FACING, same particle model
        getVariantBuilder(ModBlocks.FILE_CABINET_STEEL.get()).partialState().with(com.hbm_m.block.decorations.FileCabinetBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/file_cabinet_particle"))).addModel();
        getVariantBuilder(ModBlocks.FILE_CABINET_STEEL.get()).partialState().with(com.hbm_m.block.decorations.FileCabinetBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/file_cabinet_particle"))).addModel();
        getVariantBuilder(ModBlocks.FILE_CABINET_STEEL.get()).partialState().with(com.hbm_m.block.decorations.FileCabinetBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/file_cabinet_particle"))).addModel();
        getVariantBuilder(ModBlocks.FILE_CABINET_STEEL.get()).partialState().with(com.hbm_m.block.decorations.FileCabinetBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/file_cabinet_particle"))).addModel();
        // fire_digamma — DigammaBlocks.Flame (no blockstate properties)
        getVariantBuilder(ModBlocks.FIRE_DIGAMMA.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/bomb/fire_digamma"))).addModel();
        // fissure_bomb json — not ported: ModBlocks.FISSURE_BOMB is already registered in ModBlockStateProvider (cubeBottomTop)
        // fireworks json — not ported: ModBlocks.FIREWORKS is already registered in ModBlockStateProvider (cubeBottomTop)
        // flame_war json — not ported: ModBlocks.FLAME_WAR is already registered in ModBlockStateProvider (cubeAll)
        // digamma_matter json — not ported: ModBlocks.DIGAMMA_MATTER is already registered in ModBlockStateProvider (cubeAll)
        // float_bomb — SimpleBombs.Float (no blockstate properties)
        getVariantBuilder(ModBlocks.FLOAT_BOMB.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/bomb/float_bomb"))).addModel();
        // flood_lamp — SpotlightBlock.FACING (all 6) + BROKEN; json maps both broken values to the same per-facing model, no rotations
        getVariantBuilder(ModBlocks.FLOOD_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_down"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_down"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_up"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_up"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_north"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_north"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_south"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_south"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_west"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_west"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_east"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_east"))).addModel();
        // flood_lamp_off — SpotlightBlock, off variants
        getVariantBuilder(ModBlocks.FLOOD_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_off_down"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_off_down"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_off_up"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_off_up"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_off_north"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_off_north"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_off_south"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_off_south"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_off_west"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_off_west"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, false).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_off_east"))).addModel();
        getVariantBuilder(ModBlocks.FLOOD_LAMP_OFF.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.BROKEN, true).with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/flood_lamp_off_east"))).addModel();
        // floodlight — FloodlightBlock.META (0..11), all variants use the same particle model
        getVariantBuilder(ModBlocks.FLOODLIGHT.get()).partialState().with(com.hbm_m.block.decorations.FloodlightBlock.META, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/floodlight_particle"))).addModel();
        getVariantBuilder(ModBlocks.FLOODLIGHT.get()).partialState().with(com.hbm_m.block.decorations.FloodlightBlock.META, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/floodlight_particle"))).addModel();
        getVariantBuilder(ModBlocks.FLOODLIGHT.get()).partialState().with(com.hbm_m.block.decorations.FloodlightBlock.META, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/floodlight_particle"))).addModel();
        getVariantBuilder(ModBlocks.FLOODLIGHT.get()).partialState().with(com.hbm_m.block.decorations.FloodlightBlock.META, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/floodlight_particle"))).addModel();
        getVariantBuilder(ModBlocks.FLOODLIGHT.get()).partialState().with(com.hbm_m.block.decorations.FloodlightBlock.META, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/floodlight_particle"))).addModel();
        getVariantBuilder(ModBlocks.FLOODLIGHT.get()).partialState().with(com.hbm_m.block.decorations.FloodlightBlock.META, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/floodlight_particle"))).addModel();
        getVariantBuilder(ModBlocks.FLOODLIGHT.get()).partialState().with(com.hbm_m.block.decorations.FloodlightBlock.META, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/floodlight_particle"))).addModel();
        getVariantBuilder(ModBlocks.FLOODLIGHT.get()).partialState().with(com.hbm_m.block.decorations.FloodlightBlock.META, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/floodlight_particle"))).addModel();
        getVariantBuilder(ModBlocks.FLOODLIGHT.get()).partialState().with(com.hbm_m.block.decorations.FloodlightBlock.META, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/floodlight_particle"))).addModel();
        getVariantBuilder(ModBlocks.FLOODLIGHT.get()).partialState().with(com.hbm_m.block.decorations.FloodlightBlock.META, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/floodlight_particle"))).addModel();
        getVariantBuilder(ModBlocks.FLOODLIGHT.get()).partialState().with(com.hbm_m.block.decorations.FloodlightBlock.META, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/floodlight_particle"))).addModel();
        getVariantBuilder(ModBlocks.FLOODLIGHT.get()).partialState().with(com.hbm_m.block.decorations.FloodlightBlock.META, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/floodlight_particle"))).addModel();
        // floodlight_beam — FloodlightBeamBlock (no blockstate properties)
        getVariantBuilder(ModBlocks.FLOODLIGHT_BEAM.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/floodlight_particle"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_down_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_down_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_down_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_down_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_down_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_down_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_down_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_down_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_down_cap_north"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_down_cap_south"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_down_cap_west"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_down_cap_east"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_down_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_down_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_down_mid_north"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_down_mid_south"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_down_mid_west"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.DOWN).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_down_mid_east"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_up_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_up_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_up_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_up_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_up_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_up_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_up_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_up_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_up_cap_north"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_up_cap_south"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_up_cap_west"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_up_cap_east"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_up_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_up_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_up_mid_north"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_up_mid_south"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_up_mid_west"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.UP).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_up_mid_east"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_north_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_north_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_north_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_north_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_north_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_north_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_north_cap_down"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_north_cap_up"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_north_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_north_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_north_cap_west"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_north_cap_east"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_north_mid_down"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_north_mid_up"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_north_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_north_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_north_mid_west"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.NORTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_north_mid_east"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_south_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_south_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_south_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_south_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_south_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_south_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_south_cap_down"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_south_cap_up"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_south_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_south_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_south_cap_west"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_south_cap_east"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_south_mid_down"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_south_mid_up"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_south_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_south_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_south_mid_west"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_south_mid_east"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_west_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_west_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_west_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_west_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_west_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_west_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_west_cap_down"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_west_cap_up"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_west_cap_north"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_west_cap_south"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_west_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_west_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_west_mid_down"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_west_mid_up"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_west_mid_north"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_west_mid_south"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_west_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.WEST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_west_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_east_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_east_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_east_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_east_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_east_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.SINGLE).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_east_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_east_cap_down"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_east_cap_up"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_east_cap_north"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_east_cap_south"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_east_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.CAP).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_east_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_east_mid_down"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_east_mid_up"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_east_mid_north"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_east_mid_south"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_east_single"))).addModel();
        getVariantBuilder(ModBlocks.FLUORESCENT_LAMP.get()).partialState().with(com.hbm_m.block.decorations.SpotlightBlock.FACING, Direction.EAST).with(com.hbm_m.block.decorations.SpotlightBlock.PART, com.hbm_m.block.decorations.SpotlightBlock.Part.MID).with(com.hbm_m.block.decorations.SpotlightBlock.CONN, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lights/fluorescent_lamp_east_single"))).addModel();

        registerPortedBlockstates2();
    }

    private void registerPortedBlockstates2() {
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/iso_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.ISOLATED);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/iso_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.ISOLATED);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/through_x_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_X);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/through_x_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_X);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/through_y_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_Y);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/through_y_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_Y);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/through_z_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_Z);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/through_z_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_Z);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/arm_east_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/arm_east_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/arm_west_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/arm_west_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/arm_up_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/arm_up_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/arm_down_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/arm_down_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/arm_south_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/arm_south_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/arm_north_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/arm_north_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/oct_ppn_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/oct_ppn_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/oct_ppp_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/oct_ppp_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/oct_npn_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/oct_npn_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/oct_npp_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/oct_npp_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/oct_pnn_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/oct_pnn_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/oct_pnp_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/oct_pnp_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/oct_nnn_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/oct_nnn_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/oct_nnp_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_neo/oct_nnp_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/iso_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.ISOLATED);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/iso_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.ISOLATED);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/through_x_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_X);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/through_x_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_X);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/through_y_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_Y);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/through_y_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_Y);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/through_z_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_Z);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/through_z_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_Z);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/arm_east_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/arm_east_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/arm_west_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/arm_west_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/arm_up_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/arm_up_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/arm_down_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/arm_down_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/arm_south_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/arm_south_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/arm_north_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/arm_north_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/oct_ppn_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/oct_ppn_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/oct_ppp_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/oct_ppp_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/oct_npn_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/oct_npn_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/oct_npp_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/oct_npp_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/oct_pnn_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/oct_pnn_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/oct_pnp_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/oct_pnp_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/oct_nnn_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/oct_nnn_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/oct_nnp_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_COLORED.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_colored/oct_nnp_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/iso_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.ISOLATED);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/iso_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.ISOLATED);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/through_x_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_X);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/through_x_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_X);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/through_y_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_Y);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/through_y_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_Y);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/through_z_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_Z);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/through_z_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_Z);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/arm_east_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/arm_east_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/arm_west_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/arm_west_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/arm_up_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/arm_up_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/arm_down_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/arm_down_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/arm_south_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/arm_south_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/arm_north_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/arm_north_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, true);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/oct_ppn_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/oct_ppn_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/oct_ppp_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/oct_ppp_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/oct_npn_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/oct_npn_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/oct_npp_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/oct_npp_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/oct_pnn_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/oct_pnn_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/oct_pnp_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/oct_pnp_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/oct_nnn_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/oct_nnn_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/oct_nnp_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FLUID_DUCT_SILVER.get()).part().modelFile(models().getExistingFile(modLoc("block/pipe_silver/oct_nnp_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false);
        getMultipartBuilder(ModBlocks.FOUNDRY_CHANNEL.get()).part().modelFile(models().getExistingFile(modLoc("block/machines/foundry_channel_center"))).addModel();
        getMultipartBuilder(ModBlocks.FOUNDRY_CHANNEL.get()).part().modelFile(models().getExistingFile(modLoc("block/machines/foundry_channel_connected"))).addModel().condition(com.hbm_m.block.machines.MachineFoundryChannelBlock.EAST, true);
        getMultipartBuilder(ModBlocks.FOUNDRY_CHANNEL.get()).part().modelFile(models().getExistingFile(modLoc("block/machines/foundry_channel_closed"))).addModel().condition(com.hbm_m.block.machines.MachineFoundryChannelBlock.EAST, false);
        getMultipartBuilder(ModBlocks.FOUNDRY_CHANNEL.get()).part().modelFile(models().getExistingFile(modLoc("block/machines/foundry_channel_connected"))).rotationY(90).addModel().condition(com.hbm_m.block.machines.MachineFoundryChannelBlock.SOUTH, true);
        getMultipartBuilder(ModBlocks.FOUNDRY_CHANNEL.get()).part().modelFile(models().getExistingFile(modLoc("block/machines/foundry_channel_closed"))).rotationY(90).addModel().condition(com.hbm_m.block.machines.MachineFoundryChannelBlock.SOUTH, false);
        getMultipartBuilder(ModBlocks.FOUNDRY_CHANNEL.get()).part().modelFile(models().getExistingFile(modLoc("block/machines/foundry_channel_connected"))).rotationY(180).addModel().condition(com.hbm_m.block.machines.MachineFoundryChannelBlock.WEST, true);
        getMultipartBuilder(ModBlocks.FOUNDRY_CHANNEL.get()).part().modelFile(models().getExistingFile(modLoc("block/machines/foundry_channel_closed"))).rotationY(180).addModel().condition(com.hbm_m.block.machines.MachineFoundryChannelBlock.WEST, false);
        getMultipartBuilder(ModBlocks.FOUNDRY_CHANNEL.get()).part().modelFile(models().getExistingFile(modLoc("block/machines/foundry_channel_connected"))).rotationY(270).addModel().condition(com.hbm_m.block.machines.MachineFoundryChannelBlock.NORTH, true);
        getMultipartBuilder(ModBlocks.FOUNDRY_CHANNEL.get()).part().modelFile(models().getExistingFile(modLoc("block/machines/foundry_channel_closed"))).rotationY(270).addModel().condition(com.hbm_m.block.machines.MachineFoundryChannelBlock.NORTH, false);
        getVariantBuilder(ModBlocks.FOUNDRY_OUTLET.get()).partialState().with(com.hbm_m.block.machines.MachineFoundryOutletBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/foundry_outlet"))).addModel();
        getVariantBuilder(ModBlocks.FOUNDRY_OUTLET.get()).partialState().with(com.hbm_m.block.machines.MachineFoundryOutletBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/foundry_outlet"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.FOUNDRY_OUTLET.get()).partialState().with(com.hbm_m.block.machines.MachineFoundryOutletBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/foundry_outlet"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.FOUNDRY_OUTLET.get()).partialState().with(com.hbm_m.block.machines.MachineFoundryOutletBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/foundry_outlet"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.FOUNDRY_SLAGTAP.get()).partialState().with(com.hbm_m.block.machines.MachineFoundrySlagtapBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/foundry_slagtap"))).addModel();
        getVariantBuilder(ModBlocks.FOUNDRY_SLAGTAP.get()).partialState().with(com.hbm_m.block.machines.MachineFoundrySlagtapBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/foundry_slagtap"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.FOUNDRY_SLAGTAP.get()).partialState().with(com.hbm_m.block.machines.MachineFoundrySlagtapBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/foundry_slagtap"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.FOUNDRY_SLAGTAP.get()).partialState().with(com.hbm_m.block.machines.MachineFoundrySlagtapBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/foundry_slagtap"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.FRACTION_SPACER.get()).partialState().with(com.hbm_m.block.machines.FractionSpacerBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/fraction_spacer"))).addModel();
        getVariantBuilder(ModBlocks.FRACTION_SPACER.get()).partialState().with(com.hbm_m.block.machines.FractionSpacerBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/fraction_spacer"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.FRACTION_SPACER.get()).partialState().with(com.hbm_m.block.machines.FractionSpacerBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/fraction_spacer"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.FRACTION_SPACER.get()).partialState().with(com.hbm_m.block.machines.FractionSpacerBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/fraction_spacer"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.GEYSIR_CHLORINE.get()).partialState().with(com.hbm_m.block.generic.BlockGeysir.ERUPTING, false).modelForState().modelFile(models().getExistingFile(modLoc("block/geysir_chlorine"))).addModel();
        getVariantBuilder(ModBlocks.GEYSIR_CHLORINE.get()).partialState().with(com.hbm_m.block.generic.BlockGeysir.ERUPTING, true).modelForState().modelFile(models().getExistingFile(modLoc("block/geysir_chlorine"))).addModel();
        // heating_oven (HeatingOvenBlock: FACING horizontal)
        getVariantBuilder(ModBlocks.HEATING_OVEN.get()).partialState().with(com.hbm_m.block.machines.HeatingOvenBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/heating_oven"))).rotationY(0).addModel();
        getVariantBuilder(ModBlocks.HEATING_OVEN.get()).partialState().with(com.hbm_m.block.machines.HeatingOvenBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/heating_oven"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.HEATING_OVEN.get()).partialState().with(com.hbm_m.block.machines.HeatingOvenBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/heating_oven"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.HEATING_OVEN.get()).partialState().with(com.hbm_m.block.machines.HeatingOvenBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/heating_oven"))).rotationY(270).addModel();
        // hydrotreater (DummyableMachineBlock: FACING horizontal)
        getVariantBuilder(ModBlocks.HYDROTREATER.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/hydrotreater"))).rotationY(0).addModel();
        getVariantBuilder(ModBlocks.HYDROTREATER.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/hydrotreater"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.HYDROTREATER.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/hydrotreater"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.HYDROTREATER.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/hydrotreater"))).rotationY(90).addModel();
        // industrial_boiler (MachineIndustrialBoilerBlock: FACING horizontal + LIT; json uses the same model for both lit states)
        getVariantBuilder(ModBlocks.INDUSTRIAL_BOILER.get()).partialState().with(com.hbm_m.block.machines.MachineIndustrialBoilerBlock.FACING, Direction.NORTH).with(com.hbm_m.block.machines.MachineIndustrialBoilerBlock.LIT, false).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/industrial_boiler"))).rotationY(0).addModel();
        getVariantBuilder(ModBlocks.INDUSTRIAL_BOILER.get()).partialState().with(com.hbm_m.block.machines.MachineIndustrialBoilerBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.machines.MachineIndustrialBoilerBlock.LIT, false).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/industrial_boiler"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.INDUSTRIAL_BOILER.get()).partialState().with(com.hbm_m.block.machines.MachineIndustrialBoilerBlock.FACING, Direction.WEST).with(com.hbm_m.block.machines.MachineIndustrialBoilerBlock.LIT, false).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/industrial_boiler"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.INDUSTRIAL_BOILER.get()).partialState().with(com.hbm_m.block.machines.MachineIndustrialBoilerBlock.FACING, Direction.EAST).with(com.hbm_m.block.machines.MachineIndustrialBoilerBlock.LIT, false).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/industrial_boiler"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.INDUSTRIAL_BOILER.get()).partialState().with(com.hbm_m.block.machines.MachineIndustrialBoilerBlock.FACING, Direction.NORTH).with(com.hbm_m.block.machines.MachineIndustrialBoilerBlock.LIT, true).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/industrial_boiler"))).rotationY(0).addModel();
        getVariantBuilder(ModBlocks.INDUSTRIAL_BOILER.get()).partialState().with(com.hbm_m.block.machines.MachineIndustrialBoilerBlock.FACING, Direction.SOUTH).with(com.hbm_m.block.machines.MachineIndustrialBoilerBlock.LIT, true).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/industrial_boiler"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.INDUSTRIAL_BOILER.get()).partialState().with(com.hbm_m.block.machines.MachineIndustrialBoilerBlock.FACING, Direction.WEST).with(com.hbm_m.block.machines.MachineIndustrialBoilerBlock.LIT, true).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/industrial_boiler"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.INDUSTRIAL_BOILER.get()).partialState().with(com.hbm_m.block.machines.MachineIndustrialBoilerBlock.FACING, Direction.EAST).with(com.hbm_m.block.machines.MachineIndustrialBoilerBlock.LIT, true).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/industrial_boiler"))).rotationY(90).addModel();
        // lamp_demon (DemonLampBlock: FACING = DirectionalBlock.FACING, alle 6 Richtungen)
        getVariantBuilder(ModBlocks.LAMP_DEMON.get()).partialState().with(com.hbm_m.block.decorations.DemonLampBlock.FACING, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/lamp_demon"))).addModel();
        getVariantBuilder(ModBlocks.LAMP_DEMON.get()).partialState().with(com.hbm_m.block.decorations.DemonLampBlock.FACING, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/lamp_demon"))).rotationX(180).addModel();
        getVariantBuilder(ModBlocks.LAMP_DEMON.get()).partialState().with(com.hbm_m.block.decorations.DemonLampBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lamp_demon"))).rotationX(90).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.LAMP_DEMON.get()).partialState().with(com.hbm_m.block.decorations.DemonLampBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lamp_demon"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.LAMP_DEMON.get()).partialState().with(com.hbm_m.block.decorations.DemonLampBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lamp_demon"))).rotationX(90).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.LAMP_DEMON.get()).partialState().with(com.hbm_m.block.decorations.DemonLampBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lamp_demon"))).rotationX(90).rotationY(90).addModel();
        // lantern (LanternBlock extends DummyableMachineBlock: FACING horizontal; json: alle 4 Facings ohne Rotation)
        getVariantBuilder(ModBlocks.LANTERN.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lantern_particle"))).addModel();
        getVariantBuilder(ModBlocks.LANTERN.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/lantern_particle"))).addModel();
        getVariantBuilder(ModBlocks.LANTERN.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/lantern_particle"))).addModel();
        getVariantBuilder(ModBlocks.LANTERN.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/lantern_particle"))).addModel();
        // launchpad_lambda (DummyableMachineBlock: FACING horizontal)
        getVariantBuilder(ModBlocks.LAUNCHPAD_LAMBDA.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/launchpad_lambda_particle"))).addModel();
        getVariantBuilder(ModBlocks.LAUNCHPAD_LAMBDA.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/launchpad_lambda_particle"))).addModel();
        getVariantBuilder(ModBlocks.LAUNCHPAD_LAMBDA.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/launchpad_lambda_particle"))).addModel();
        getVariantBuilder(ModBlocks.LAUNCHPAD_LAMBDA.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/launchpad_lambda_particle"))).addModel();
        // machine_rockmill (DummyableMachineBlock: FACING horizontal)
        getVariantBuilder(ModBlocks.MACHINE_ROCKMILL.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/machine_rockmill_particle"))).addModel();
        getVariantBuilder(ModBlocks.MACHINE_ROCKMILL.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/machine_rockmill_particle"))).addModel();
        getVariantBuilder(ModBlocks.MACHINE_ROCKMILL.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/machine_rockmill_particle"))).addModel();
        getVariantBuilder(ModBlocks.MACHINE_ROCKMILL.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/machine_rockmill_particle"))).addModel();
        // machine_supercomputer (DummyableMachineBlock: FACING horizontal)
        getVariantBuilder(ModBlocks.MACHINE_SUPERCOMPUTER.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/machine_supercomputer_particle"))).addModel();
        getVariantBuilder(ModBlocks.MACHINE_SUPERCOMPUTER.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/machine_supercomputer_particle"))).addModel();
        getVariantBuilder(ModBlocks.MACHINE_SUPERCOMPUTER.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/machine_supercomputer_particle"))).addModel();
        getVariantBuilder(ModBlocks.MACHINE_SUPERCOMPUTER.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/machine_supercomputer_particle"))).addModel();
        // machine_tape_drive (MachineTapeDriveBlock: eigenes FACING horizontal)
        getVariantBuilder(ModBlocks.MACHINE_TAPE_DRIVE.get()).partialState().with(com.hbm_m.block.machines.MachineTapeDriveBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/machine_tape_drive_particle"))).addModel();
        getVariantBuilder(ModBlocks.MACHINE_TAPE_DRIVE.get()).partialState().with(com.hbm_m.block.machines.MachineTapeDriveBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/machine_tape_drive_particle"))).addModel();
        getVariantBuilder(ModBlocks.MACHINE_TAPE_DRIVE.get()).partialState().with(com.hbm_m.block.machines.MachineTapeDriveBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/machine_tape_drive_particle"))).addModel();
        getVariantBuilder(ModBlocks.MACHINE_TAPE_DRIVE.get()).partialState().with(com.hbm_m.block.machines.MachineTapeDriveBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/machine_tape_drive_particle"))).addModel();
        // mush_block (R6dPlants.MushHuge extends Block: keine Properties)
        getVariantBuilder(ModBlocks.MUSH_BLOCK.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/deco/mush_block"))).addModel();
        // part_emitter (PartEmitterBlock extends BaseEntityBlock: keine Properties)
        getVariantBuilder(ModBlocks.PART_EMITTER.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/deco/part_emitter"))).addModel();
        // pipe_anchor_exhaust (ExhaustPipeAnchorBlock extends PipeAnchorBlock: FACING = BlockStateProperties.FACING, alle 6 Richtungen; json: alle Facings ohne Rotation)
        getVariantBuilder(ModBlocks.PIPE_ANCHOR_EXHAUST.get()).partialState().with(com.hbm_m.block.network.PipeAnchorBlock.FACING, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/pipe_anchor_particle"))).addModel();
        getVariantBuilder(ModBlocks.PIPE_ANCHOR_EXHAUST.get()).partialState().with(com.hbm_m.block.network.PipeAnchorBlock.FACING, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/pipe_anchor_particle"))).addModel();
        getVariantBuilder(ModBlocks.PIPE_ANCHOR_EXHAUST.get()).partialState().with(com.hbm_m.block.network.PipeAnchorBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/pipe_anchor_particle"))).addModel();
        getVariantBuilder(ModBlocks.PIPE_ANCHOR_EXHAUST.get()).partialState().with(com.hbm_m.block.network.PipeAnchorBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/pipe_anchor_particle"))).addModel();
        getVariantBuilder(ModBlocks.PIPE_ANCHOR_EXHAUST.get()).partialState().with(com.hbm_m.block.network.PipeAnchorBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/pipe_anchor_particle"))).addModel();
        getVariantBuilder(ModBlocks.PIPE_ANCHOR_EXHAUST.get()).partialState().with(com.hbm_m.block.network.PipeAnchorBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/pipe_anchor_particle"))).addModel();
        // oil_pipe (FluidDuctBlock: NORTH/EAST/SOUTH/WEST/UP/DOWN + SHAPE; Multipart 1:1 aus dem handgeschriebenen JSON, jeweils base + overlay)
        var oilPipeBuilder = getMultipartBuilder(ModBlocks.OIL_PIPE.get());
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/iso_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.ISOLATED).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/iso_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.ISOLATED).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/through_x_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_X).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/through_x_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_X).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/through_y_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_Y).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/through_y_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_Y).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/through_z_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_Z).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/through_z_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.THROUGH_Z).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/arm_east_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, true).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/arm_east_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, true).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/arm_west_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, true).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/arm_west_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, true).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/arm_up_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, true).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/arm_up_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, true).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/arm_down_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, true).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/arm_down_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, true).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/arm_south_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, true).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/arm_south_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, true).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/arm_north_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, true).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/arm_north_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, true).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/oct_ppn_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/oct_ppn_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/oct_ppp_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/oct_ppp_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/oct_npn_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/oct_npn_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/oct_npp_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/oct_npp_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.UP, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/oct_pnn_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/oct_pnn_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/oct_pnp_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/oct_pnp_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.EAST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/oct_nnn_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/oct_nnn_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.SOUTH, false).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/oct_nnp_base"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false).end();
        oilPipeBuilder.part().modelFile(models().getExistingFile(modLoc("block/pipe_oil/oct_nnp_overlay"))).addModel().condition(com.hbm_m.block.machines.FluidDuctBlock.SHAPE, com.hbm_m.block.machines.PipeRenderShape.COMPLEX).condition(com.hbm_m.block.machines.FluidDuctBlock.WEST, false).condition(com.hbm_m.block.machines.FluidDuctBlock.DOWN, false).condition(com.hbm_m.block.machines.FluidDuctBlock.NORTH, false).end();
        // pipe_anchor_pneumatic — PipeAnchorBlock: FACING (all 6), model block/pipe_anchor_particle, no rotations
        getVariantBuilder(ModBlocks.PIPE_ANCHOR_PNEUMATIC.get()).partialState().with(com.hbm_m.block.network.PipeAnchorBlock.FACING, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/pipe_anchor_particle"))).addModel();
        getVariantBuilder(ModBlocks.PIPE_ANCHOR_PNEUMATIC.get()).partialState().with(com.hbm_m.block.network.PipeAnchorBlock.FACING, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/pipe_anchor_particle"))).addModel();
        getVariantBuilder(ModBlocks.PIPE_ANCHOR_PNEUMATIC.get()).partialState().with(com.hbm_m.block.network.PipeAnchorBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/pipe_anchor_particle"))).addModel();
        getVariantBuilder(ModBlocks.PIPE_ANCHOR_PNEUMATIC.get()).partialState().with(com.hbm_m.block.network.PipeAnchorBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/pipe_anchor_particle"))).addModel();
        getVariantBuilder(ModBlocks.PIPE_ANCHOR_PNEUMATIC.get()).partialState().with(com.hbm_m.block.network.PipeAnchorBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/pipe_anchor_particle"))).addModel();
        getVariantBuilder(ModBlocks.PIPE_ANCHOR_PNEUMATIC.get()).partialState().with(com.hbm_m.block.network.PipeAnchorBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/pipe_anchor_particle"))).addModel();
        // plant_reeds — NTMPlants.Reeds: no state properties, single variant, model path matches block id
        customObjBlockNoFacing(ModBlocks.PLANT_REEDS);
        // plant_tall_weed — NTMPlants.TallPlant: HALF (DoubleBlockHalf)
        getVariantBuilder(ModBlocks.PLANT_TALL_WEED.get()).partialState().with(com.hbm_m.block.generic.NTMPlants.TallPlant.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER).modelForState().modelFile(models().getExistingFile(modLoc("block/plant_tall_weed_lower"))).addModel();
        getVariantBuilder(ModBlocks.PLANT_TALL_WEED.get()).partialState().with(com.hbm_m.block.generic.NTMPlants.TallPlant.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER).modelForState().modelFile(models().getExistingFile(modLoc("block/plant_tall_weed_upper"))).addModel();
        // plant_tall_cd2
        getVariantBuilder(ModBlocks.PLANT_TALL_CD2.get()).partialState().with(com.hbm_m.block.generic.NTMPlants.TallPlant.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER).modelForState().modelFile(models().getExistingFile(modLoc("block/plant_tall_cd2_lower"))).addModel();
        getVariantBuilder(ModBlocks.PLANT_TALL_CD2.get()).partialState().with(com.hbm_m.block.generic.NTMPlants.TallPlant.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER).modelForState().modelFile(models().getExistingFile(modLoc("block/plant_tall_cd2_upper"))).addModel();
        // plant_tall_cd3
        getVariantBuilder(ModBlocks.PLANT_TALL_CD3.get()).partialState().with(com.hbm_m.block.generic.NTMPlants.TallPlant.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER).modelForState().modelFile(models().getExistingFile(modLoc("block/plant_tall_cd3_lower"))).addModel();
        getVariantBuilder(ModBlocks.PLANT_TALL_CD3.get()).partialState().with(com.hbm_m.block.generic.NTMPlants.TallPlant.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER).modelForState().modelFile(models().getExistingFile(modLoc("block/plant_tall_cd3_upper"))).addModel();
        // plant_tall_cd4
        getVariantBuilder(ModBlocks.PLANT_TALL_CD4.get()).partialState().with(com.hbm_m.block.generic.NTMPlants.TallPlant.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER).modelForState().modelFile(models().getExistingFile(modLoc("block/plant_tall_cd4_lower"))).addModel();
        getVariantBuilder(ModBlocks.PLANT_TALL_CD4.get()).partialState().with(com.hbm_m.block.generic.NTMPlants.TallPlant.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER).modelForState().modelFile(models().getExistingFile(modLoc("block/plant_tall_cd4_upper"))).addModel();
        // plushie — TrinketBlock: ROTATION_16, 16 variants, same model
        getVariantBuilder(ModBlocks.PLUSHIE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/plushie_particle"))).addModel();
        getVariantBuilder(ModBlocks.PLUSHIE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/plushie_particle"))).addModel();
        getVariantBuilder(ModBlocks.PLUSHIE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/plushie_particle"))).addModel();
        getVariantBuilder(ModBlocks.PLUSHIE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/plushie_particle"))).addModel();
        getVariantBuilder(ModBlocks.PLUSHIE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/plushie_particle"))).addModel();
        getVariantBuilder(ModBlocks.PLUSHIE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/plushie_particle"))).addModel();
        getVariantBuilder(ModBlocks.PLUSHIE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/plushie_particle"))).addModel();
        getVariantBuilder(ModBlocks.PLUSHIE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/plushie_particle"))).addModel();
        getVariantBuilder(ModBlocks.PLUSHIE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/plushie_particle"))).addModel();
        getVariantBuilder(ModBlocks.PLUSHIE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/plushie_particle"))).addModel();
        getVariantBuilder(ModBlocks.PLUSHIE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/plushie_particle"))).addModel();
        getVariantBuilder(ModBlocks.PLUSHIE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/plushie_particle"))).addModel();
        getVariantBuilder(ModBlocks.PLUSHIE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/plushie_particle"))).addModel();
        getVariantBuilder(ModBlocks.PLUSHIE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/plushie_particle"))).addModel();
        getVariantBuilder(ModBlocks.PLUSHIE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/plushie_particle"))).addModel();
        getVariantBuilder(ModBlocks.PLUSHIE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/plushie_particle"))).addModel();
        // pole_satellite_receiver — DecoObjBlock: HORIZONTAL_FACING; replaces the "генерация удалена" removal at ModBlockStateProvider.java:1159
        getVariantBuilder(ModBlocks.POLE_SATELLITE_RECEIVER.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/pole_satellite_receiver"))).addModel();
        getVariantBuilder(ModBlocks.POLE_SATELLITE_RECEIVER.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/pole_satellite_receiver"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.POLE_SATELLITE_RECEIVER.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/pole_satellite_receiver"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.POLE_SATELLITE_RECEIVER.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/pole_satellite_receiver"))).rotationY(270).addModel();
        // puter — DecoObjBlock: HORIZONTAL_FACING, non-standard rotation map (north=180, south=0, west=90, east=270)
        getVariantBuilder(ModBlocks.PUTER.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/puter"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.PUTER.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/puter"))).rotationY(0).addModel();
        getVariantBuilder(ModBlocks.PUTER.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/puter"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.PUTER.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/puter"))).rotationY(270).addModel();
        // pylon_dummy — PylonDummyBlock: no state properties, single variant; registered without item, so no simpleBlockItem
        getVariantBuilder(ModBlocks.PYLON_DUMMY.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/pylon_dummy"))).addModel();
        // rail_large_straight — RailDummyableBlock: META 0..15
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_r0_m12"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_r0_m13"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_r90_m14"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_r90_m15"))).addModel();
        // rail_large_curve
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_r180"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_r0"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_r270"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_r90"))).addModel();
        // rail_large_curve_7
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_7.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_7_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_7.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_7_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_7.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_7_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_7.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_7_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_7.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_7_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_7.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_7_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_7.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_7_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_7.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_7_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_7.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_7_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_7.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_7_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_7.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_7_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_7.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_7_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_7.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_7_r180"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_7.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_7_r0"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_7.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_7_r270"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_7.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_7_r90"))).addModel();
        // rail_large_curve_9
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_9.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_9_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_9.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_9_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_9.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_9_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_9.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_9_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_9.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_9_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_9.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_9_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_9.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_9_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_9.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_9_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_9.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_9_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_9.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_9_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_9.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_9_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_9.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_9_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_9.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_9_r180"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_9.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_9_r0"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_9.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_9_r270"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_CURVE_9.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_curve_9_r90"))).addModel();
        // rail_large_ramp
        getVariantBuilder(ModBlocks.RAIL_LARGE_RAMP.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_ramp_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_RAMP.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_ramp_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_RAMP.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_ramp_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_RAMP.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_ramp_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_RAMP.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_ramp_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_RAMP.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_ramp_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_RAMP.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_ramp_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_RAMP.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_ramp_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_RAMP.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_ramp_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_RAMP.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_ramp_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_RAMP.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_ramp_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_RAMP.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_ramp_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_RAMP.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_ramp_r180_m12"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_RAMP.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_ramp_r0_m13"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_RAMP.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_ramp_r270_m14"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_RAMP.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_ramp_r90_m15"))).addModel();
        // rail_large_buffer
        getVariantBuilder(ModBlocks.RAIL_LARGE_BUFFER.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_buffer_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_BUFFER.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_buffer_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_BUFFER.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_buffer_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_BUFFER.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_buffer_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_BUFFER.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_buffer_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_BUFFER.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_buffer_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_BUFFER.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_buffer_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_BUFFER.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_buffer_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_BUFFER.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_buffer_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_BUFFER.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_buffer_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_BUFFER.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_buffer_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_BUFFER.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_buffer_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_BUFFER.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_buffer_r180_m12"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_BUFFER.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_buffer_r0_m13"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_BUFFER.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_buffer_r270_m14"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_BUFFER.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_buffer_r90_m15"))).addModel();
        // rail_large_straight_short — RailStandardStraightShort (RailDummyableBlock): META 0-15, rotations baked into model names
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT_SHORT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_short_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT_SHORT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_short_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT_SHORT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_short_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT_SHORT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_short_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT_SHORT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_short_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT_SHORT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_short_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT_SHORT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_short_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT_SHORT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_short_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT_SHORT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_short_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT_SHORT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_short_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT_SHORT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_short_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT_SHORT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_short_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT_SHORT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_short_r0_m12"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT_SHORT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_short_r0_m13"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT_SHORT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_short_r90_m14"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_STRAIGHT_SHORT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_straight_short_r90_m15"))).addModel();
        // rail_large_switch — RailStandardSwitch (BlockRailWaypointSystem extends RailDummyableBlock): META 0-15
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_r180_m12"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_r0_m13"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_r270_m14"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_r90_m15"))).addModel();
        // rail_large_switch_flipped — RailStandardSwitchFlipped (BlockRailWaypointSystem extends RailDummyableBlock): META 0-15
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH_FLIPPED.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_flipped_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH_FLIPPED.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_flipped_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH_FLIPPED.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_flipped_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH_FLIPPED.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_flipped_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH_FLIPPED.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_flipped_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH_FLIPPED.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_flipped_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH_FLIPPED.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_flipped_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH_FLIPPED.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_flipped_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH_FLIPPED.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_flipped_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH_FLIPPED.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_flipped_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH_FLIPPED.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_flipped_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH_FLIPPED.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_flipped_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH_FLIPPED.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_flipped_r180_m12"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH_FLIPPED.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_flipped_r0_m13"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH_FLIPPED.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_flipped_r270_m14"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_LARGE_SWITCH_FLIPPED.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_large_switch_flipped_r90_m15"))).addModel();
        // rail_narrow_curve — RailNarrowCurve (RailDummyableBlock): META 0-15
        getVariantBuilder(ModBlocks.RAIL_NARROW_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_curve_empty"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_curve_r90"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_curve_r270"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_curve_r180"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_CURVE.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_curve_r0"))).addModel();
        // rail_narrow_straight — RailNarrowStraight (RailDummyableBlock): META 0-15, r90 for 2,3,12,13; r0 otherwise
        getVariantBuilder(ModBlocks.RAIL_NARROW_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_straight_r0"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_straight_r0"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_straight_r90"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_straight_r90"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_straight_r0"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_straight_r0"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_straight_r0"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_straight_r0"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_straight_r0"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_straight_r0"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_straight_r0"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_straight_r0"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_straight_r90"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_straight_r90"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_straight_r0"))).addModel();
        getVariantBuilder(ModBlocks.RAIL_NARROW_STRAIGHT.get()).partialState().with(com.hbm_m.block.rail.RailDummyableBlock.META, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/rail/rail_narrow_straight_r0"))).addModel();
        // rbmk_column_filler — RBMKColumnFillerBlock: plain Block, no state properties, single model, no item (registerBlockWithoutItem)
        getVariantBuilder(ModBlocks.RBMK_COLUMN_FILLER.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/rbmk_column_filler"))).addModel();
        // red_cable — WireBlock: multipart; negated center conditions (e.g. "!straight_z") expressed as positive multi-value conditions
        // ("junction|straight_x|straight_y"), which Forge serializes as vanilla's OR-of-values syntax.
        getMultipartBuilder(ModBlocks.RED_CABLE.get()).part().modelFile(models().getExistingFile(modLoc("block/red_cable_core"))).addModel().condition(com.hbm_m.api.energy.WireBlock.CENTER, com.hbm_m.api.energy.WireCenterVisual.JUNCTION).end();
        getMultipartBuilder(ModBlocks.RED_CABLE.get()).part().modelFile(models().getExistingFile(modLoc("block/red_cable_straight_x"))).addModel().condition(com.hbm_m.api.energy.WireBlock.CENTER, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_X).end();
        getMultipartBuilder(ModBlocks.RED_CABLE.get()).part().modelFile(models().getExistingFile(modLoc("block/red_cable_straight_y"))).addModel().condition(com.hbm_m.api.energy.WireBlock.CENTER, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_Y).end();
        getMultipartBuilder(ModBlocks.RED_CABLE.get()).part().modelFile(models().getExistingFile(modLoc("block/red_cable_straight_z"))).addModel().condition(com.hbm_m.api.energy.WireBlock.CENTER, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_Z).end();
        getMultipartBuilder(ModBlocks.RED_CABLE.get()).part().modelFile(models().getExistingFile(modLoc("block/red_cable_arm_north"))).addModel().condition(com.hbm_m.api.energy.WireBlock.NORTH, true).condition(com.hbm_m.api.energy.WireBlock.CENTER, com.hbm_m.api.energy.WireCenterVisual.JUNCTION, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_X, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_Y).end();
        getMultipartBuilder(ModBlocks.RED_CABLE.get()).part().modelFile(models().getExistingFile(modLoc("block/red_cable_arm_south"))).addModel().condition(com.hbm_m.api.energy.WireBlock.SOUTH, true).condition(com.hbm_m.api.energy.WireBlock.CENTER, com.hbm_m.api.energy.WireCenterVisual.JUNCTION, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_X, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_Y).end();
        getMultipartBuilder(ModBlocks.RED_CABLE.get()).part().modelFile(models().getExistingFile(modLoc("block/red_cable_arm_east"))).addModel().condition(com.hbm_m.api.energy.WireBlock.EAST, true).condition(com.hbm_m.api.energy.WireBlock.CENTER, com.hbm_m.api.energy.WireCenterVisual.JUNCTION, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_Y, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_Z).end();
        getMultipartBuilder(ModBlocks.RED_CABLE.get()).part().modelFile(models().getExistingFile(modLoc("block/red_cable_arm_west"))).addModel().condition(com.hbm_m.api.energy.WireBlock.WEST, true).condition(com.hbm_m.api.energy.WireBlock.CENTER, com.hbm_m.api.energy.WireCenterVisual.JUNCTION, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_Y, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_Z).end();
        getMultipartBuilder(ModBlocks.RED_CABLE.get()).part().modelFile(models().getExistingFile(modLoc("block/red_cable_arm_up"))).addModel().condition(com.hbm_m.api.energy.WireBlock.UP, true).condition(com.hbm_m.api.energy.WireBlock.CENTER, com.hbm_m.api.energy.WireCenterVisual.JUNCTION, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_X, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_Z).end();
        getMultipartBuilder(ModBlocks.RED_CABLE.get()).part().modelFile(models().getExistingFile(modLoc("block/red_cable_arm_down"))).addModel().condition(com.hbm_m.api.energy.WireBlock.DOWN, true).condition(com.hbm_m.api.energy.WireBlock.CENTER, com.hbm_m.api.energy.WireCenterVisual.JUNCTION, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_X, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_Z).end();
        // red_cable_box — BoxDuctBlock (META + N/S/E/W/UP/DOWN): hand-written json is a single "" variant covering all states
        getVariantBuilder(ModBlocks.RED_CABLE_BOX.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/red_cable_box"))).addModel();
        // red_cable_classic.json — WireBlock (north/east/south/west/up/down BooleanProperty), multipart
        getMultipartBuilder(ModBlocks.RED_CABLE_CLASSIC.get()).part().modelFile(models().getExistingFile(modLoc("block/cable_classic_core"))).addModel().end()
                .part().modelFile(models().getExistingFile(modLoc("block/cable_classic_arm_north"))).addModel().condition(com.hbm_m.api.energy.WireBlock.NORTH, true).end()
                .part().modelFile(models().getExistingFile(modLoc("block/cable_classic_arm_south"))).addModel().condition(com.hbm_m.api.energy.WireBlock.SOUTH, true).end()
                .part().modelFile(models().getExistingFile(modLoc("block/cable_classic_arm_east"))).addModel().condition(com.hbm_m.api.energy.WireBlock.EAST, true).end()
                .part().modelFile(models().getExistingFile(modLoc("block/cable_classic_arm_west"))).addModel().condition(com.hbm_m.api.energy.WireBlock.WEST, true).end()
                .part().modelFile(models().getExistingFile(modLoc("block/cable_classic_arm_up"))).addModel().condition(com.hbm_m.api.energy.WireBlock.UP, true).end()
                .part().modelFile(models().getExistingFile(modLoc("block/cable_classic_arm_down"))).addModel().condition(com.hbm_m.api.energy.WireBlock.DOWN, true).end();
        // red_cable_gauge.json — RedCableGaugeBlock.FACING (six-way BlockStateProperties.FACING), x/y rotations
        getVariantBuilder(ModBlocks.RED_CABLE_GAUGE.get()).partialState().with(com.hbm_m.block.network.RedCableGaugeBlock.FACING, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/cable_gauge_block"))).addModel();
        getVariantBuilder(ModBlocks.RED_CABLE_GAUGE.get()).partialState().with(com.hbm_m.block.network.RedCableGaugeBlock.FACING, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/cable_gauge_block"))).rotationX(180).addModel();
        getVariantBuilder(ModBlocks.RED_CABLE_GAUGE.get()).partialState().with(com.hbm_m.block.network.RedCableGaugeBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/cable_gauge_block"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.RED_CABLE_GAUGE.get()).partialState().with(com.hbm_m.block.network.RedCableGaugeBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/cable_gauge_block"))).rotationX(270).addModel();
        getVariantBuilder(ModBlocks.RED_CABLE_GAUGE.get()).partialState().with(com.hbm_m.block.network.RedCableGaugeBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/cable_gauge_block"))).rotationX(90).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.RED_CABLE_GAUGE.get()).partialState().with(com.hbm_m.block.network.RedCableGaugeBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/cable_gauge_block"))).rotationX(90).rotationY(270).addModel();
        // red_cable_paintable.json — RedCablePaintableBlock.OVERLAY ("overlay" BooleanProperty), both values use the base cube
        getVariantBuilder(ModBlocks.RED_CABLE_PAINTABLE.get()).partialState().with(com.hbm_m.block.network.RedCablePaintableBlock.OVERLAY, true).modelForState().modelFile(models().getExistingFile(modLoc("block/red_cable_base_cube"))).addModel();
        getVariantBuilder(ModBlocks.RED_CABLE_PAINTABLE.get()).partialState().with(com.hbm_m.block.network.RedCablePaintableBlock.OVERLAY, false).modelForState().modelFile(models().getExistingFile(modLoc("block/red_cable_base_cube"))).addModel();
        // red_cable_paintable_base.json — plain Block, one variant, model name differs from block path
        getVariantBuilder(ModBlocks.RED_CABLE_PAINTABLE_BASE.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/red_cable_base_cube"))).addModel();
        // red_cable_paintable_veil.json — plain Block, one variant, model name differs from block path
        getVariantBuilder(ModBlocks.RED_CABLE_PAINTABLE_VEIL.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/red_cable_overlay_cube"))).addModel();
        // red_connector.json — RedConnectorBlock.FACING (six-way)
        getVariantBuilder(ModBlocks.RED_CONNECTOR.get()).partialState().with(com.hbm_m.block.network.RedConnectorBlock.FACING, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/red_connector"))).addModel();
        getVariantBuilder(ModBlocks.RED_CONNECTOR.get()).partialState().with(com.hbm_m.block.network.RedConnectorBlock.FACING, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/red_connector"))).rotationX(180).addModel();
        getVariantBuilder(ModBlocks.RED_CONNECTOR.get()).partialState().with(com.hbm_m.block.network.RedConnectorBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/red_connector"))).rotationX(270).addModel();
        getVariantBuilder(ModBlocks.RED_CONNECTOR.get()).partialState().with(com.hbm_m.block.network.RedConnectorBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/red_connector"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.RED_CONNECTOR.get()).partialState().with(com.hbm_m.block.network.RedConnectorBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/red_connector"))).rotationX(270).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.RED_CONNECTOR.get()).partialState().with(com.hbm_m.block.network.RedConnectorBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/red_connector"))).rotationX(270).rotationY(270).addModel();
        // red_connector_super.json — same shape as red_connector
        getVariantBuilder(ModBlocks.RED_CONNECTOR_SUPER.get()).partialState().with(com.hbm_m.block.network.RedConnectorBlock.FACING, Direction.DOWN).modelForState().modelFile(models().getExistingFile(modLoc("block/red_connector_super"))).addModel();
        getVariantBuilder(ModBlocks.RED_CONNECTOR_SUPER.get()).partialState().with(com.hbm_m.block.network.RedConnectorBlock.FACING, Direction.UP).modelForState().modelFile(models().getExistingFile(modLoc("block/red_connector_super"))).rotationX(180).addModel();
        getVariantBuilder(ModBlocks.RED_CONNECTOR_SUPER.get()).partialState().with(com.hbm_m.block.network.RedConnectorBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/red_connector_super"))).rotationX(270).addModel();
        getVariantBuilder(ModBlocks.RED_CONNECTOR_SUPER.get()).partialState().with(com.hbm_m.block.network.RedConnectorBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/red_connector_super"))).rotationX(90).addModel();
        getVariantBuilder(ModBlocks.RED_CONNECTOR_SUPER.get()).partialState().with(com.hbm_m.block.network.RedConnectorBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/red_connector_super"))).rotationX(270).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.RED_CONNECTOR_SUPER.get()).partialState().with(com.hbm_m.block.network.RedConnectorBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/red_connector_super"))).rotationX(270).rotationY(270).addModel();
        // red_pylon.json — RedPylonBlock (FACING defined in RedPylonCoreBlock but json has only the "" variant, replicated as-is)
        getVariantBuilder(ModBlocks.RED_PYLON.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/red_pylon"))).addModel();
        // red_pylon_steel.json — same, "" variant only
        getVariantBuilder(ModBlocks.RED_PYLON_STEEL.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/red_pylon_steel"))).addModel();
        // red_pylon_large.json — RedPylonCoreBlock.FACING (horizontal), per-facing y
        customObjBlockRotated(ModBlocks.RED_PYLON_LARGE, Map.of(Direction.NORTH, 180, Direction.SOUTH, 0, Direction.EAST, 270, Direction.WEST, 90));
        // red_pylon_medium_steel.json
        customObjBlockRotated(ModBlocks.RED_PYLON_MEDIUM_STEEL, Map.of(Direction.NORTH, 180, Direction.SOUTH, 0, Direction.EAST, 270, Direction.WEST, 90));
        // red_pylon_medium_steel_transformer.json
        customObjBlockRotated(ModBlocks.RED_PYLON_MEDIUM_STEEL_TRANSFORMER, Map.of(Direction.NORTH, 180, Direction.SOUTH, 0, Direction.EAST, 270, Direction.WEST, 90));
        // red_pylon_medium_wood.json
        customObjBlockRotated(ModBlocks.RED_PYLON_MEDIUM_WOOD, Map.of(Direction.NORTH, 180, Direction.SOUTH, 0, Direction.EAST, 270, Direction.WEST, 90));
        // red_pylon_medium_wood_transformer.json
        customObjBlockRotated(ModBlocks.RED_PYLON_MEDIUM_WOOD_TRANSFORMER, Map.of(Direction.NORTH, 180, Direction.SOUTH, 0, Direction.EAST, 270, Direction.WEST, 90));
        // refinery.json — MachineRefineryBlock.FACING + EXPLODED, 8 variants
        getVariantBuilder(ModBlocks.REFINERY.get()).partialState().with(com.hbm_m.block.machines.MachineRefineryBlock.EXPLODED, false).with(com.hbm_m.block.machines.MachineRefineryBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/refinery"))).addModel();
        getVariantBuilder(ModBlocks.REFINERY.get()).partialState().with(com.hbm_m.block.machines.MachineRefineryBlock.EXPLODED, false).with(com.hbm_m.block.machines.MachineRefineryBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/refinery"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.REFINERY.get()).partialState().with(com.hbm_m.block.machines.MachineRefineryBlock.EXPLODED, false).with(com.hbm_m.block.machines.MachineRefineryBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/refinery"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.REFINERY.get()).partialState().with(com.hbm_m.block.machines.MachineRefineryBlock.EXPLODED, false).with(com.hbm_m.block.machines.MachineRefineryBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/refinery"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.REFINERY.get()).partialState().with(com.hbm_m.block.machines.MachineRefineryBlock.EXPLODED, true).with(com.hbm_m.block.machines.MachineRefineryBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/refinery_exploded"))).addModel();
        getVariantBuilder(ModBlocks.REFINERY.get()).partialState().with(com.hbm_m.block.machines.MachineRefineryBlock.EXPLODED, true).with(com.hbm_m.block.machines.MachineRefineryBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/refinery_exploded"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.REFINERY.get()).partialState().with(com.hbm_m.block.machines.MachineRefineryBlock.EXPLODED, true).with(com.hbm_m.block.machines.MachineRefineryBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/refinery_exploded"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.REFINERY.get()).partialState().with(com.hbm_m.block.machines.MachineRefineryBlock.EXPLODED, true).with(com.hbm_m.block.machines.MachineRefineryBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/refinery_exploded"))).rotationY(90).addModel();
        // safe.json — SafeBlock.FACING (BlockStateProperties.HORIZONTAL_FACING), standard y map; block has no item (registerBlockWithoutItem) so no helper item model
        getVariantBuilder(ModBlocks.SAFE.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/safe"))).addModel();
        getVariantBuilder(ModBlocks.SAFE.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/safe"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.SAFE.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/safe"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.SAFE.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/safe"))).rotationY(270).addModel();
        // sandbags.json — R6dBlocks.Sandbags N/S/E/W booleans, 16 variants (bits N,S,E,W)
        // seal_controller.json — SealBlocks.Controller.FACING + POWERED, powered does not change the model
        getVariantBuilder(ModBlocks.SEAL_CONTROLLER.get()).partialState().with(com.hbm_m.block.machines.SealBlocks.Controller.FACING, Direction.NORTH).with(com.hbm_m.block.machines.SealBlocks.Controller.POWERED, false).modelForState().modelFile(models().getExistingFile(modLoc("block/seal_controller"))).addModel();
        getVariantBuilder(ModBlocks.SEAL_CONTROLLER.get()).partialState().with(com.hbm_m.block.machines.SealBlocks.Controller.FACING, Direction.NORTH).with(com.hbm_m.block.machines.SealBlocks.Controller.POWERED, true).modelForState().modelFile(models().getExistingFile(modLoc("block/seal_controller"))).addModel();
        getVariantBuilder(ModBlocks.SEAL_CONTROLLER.get()).partialState().with(com.hbm_m.block.machines.SealBlocks.Controller.FACING, Direction.EAST).with(com.hbm_m.block.machines.SealBlocks.Controller.POWERED, false).modelForState().modelFile(models().getExistingFile(modLoc("block/seal_controller"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.SEAL_CONTROLLER.get()).partialState().with(com.hbm_m.block.machines.SealBlocks.Controller.FACING, Direction.EAST).with(com.hbm_m.block.machines.SealBlocks.Controller.POWERED, true).modelForState().modelFile(models().getExistingFile(modLoc("block/seal_controller"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.SEAL_CONTROLLER.get()).partialState().with(com.hbm_m.block.machines.SealBlocks.Controller.FACING, Direction.SOUTH).with(com.hbm_m.block.machines.SealBlocks.Controller.POWERED, false).modelForState().modelFile(models().getExistingFile(modLoc("block/seal_controller"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.SEAL_CONTROLLER.get()).partialState().with(com.hbm_m.block.machines.SealBlocks.Controller.FACING, Direction.SOUTH).with(com.hbm_m.block.machines.SealBlocks.Controller.POWERED, true).modelForState().modelFile(models().getExistingFile(modLoc("block/seal_controller"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.SEAL_CONTROLLER.get()).partialState().with(com.hbm_m.block.machines.SealBlocks.Controller.FACING, Direction.WEST).with(com.hbm_m.block.machines.SealBlocks.Controller.POWERED, false).modelForState().modelFile(models().getExistingFile(modLoc("block/seal_controller"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.SEAL_CONTROLLER.get()).partialState().with(com.hbm_m.block.machines.SealBlocks.Controller.FACING, Direction.WEST).with(com.hbm_m.block.machines.SealBlocks.Controller.POWERED, true).modelForState().modelFile(models().getExistingFile(modLoc("block/seal_controller"))).rotationY(270).addModel();
        // seal_frame.json — plain Block, one variant
        customObjBlockNoFacing(ModBlocks.SEAL_FRAME);
        // seal_hatch.json — SealBlocks.Hatch has no properties; registerBlockWithoutItem, so explicit form (no item model side effect)
        getVariantBuilder(ModBlocks.SEAL_HATCH.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/seal_hatch"))).addModel();
        // semtex.json — BlockTNTBase.IGNITE_ON_BREAK, both values use bomb/semtex
        getVariantBuilder(ModBlocks.SEMTEX.get()).partialState().with(com.hbm_m.block.bomb.BlockTNTBase.IGNITE_ON_BREAK, false).modelForState().modelFile(models().getExistingFile(modLoc("block/bomb/semtex"))).addModel();
        getVariantBuilder(ModBlocks.SEMTEX.get()).partialState().with(com.hbm_m.block.bomb.BlockTNTBase.IGNITE_ON_BREAK, true).modelForState().modelFile(models().getExistingFile(modLoc("block/bomb/semtex"))).addModel();
        // skeleton_holder.json — SkeletonHolderBlock.FACING (horizontal), nonstandard y map;
        // explicit form because the model lives at block/deco/skeleton_holder, not block/skeleton_holder
        getVariantBuilder(ModBlocks.SKELETON_HOLDER.get()).partialState().with(com.hbm_m.block.decorations.SkeletonHolderBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/skeleton_holder"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.SKELETON_HOLDER.get()).partialState().with(com.hbm_m.block.decorations.SkeletonHolderBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/skeleton_holder"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.SKELETON_HOLDER.get()).partialState().with(com.hbm_m.block.decorations.SkeletonHolderBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/skeleton_holder"))).addModel();
        getVariantBuilder(ModBlocks.SKELETON_HOLDER.get()).partialState().with(com.hbm_m.block.decorations.SkeletonHolderBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/skeleton_holder"))).rotationY(270).addModel();
        // snowglobe.json — TrinketBlock.ROTATION (BlockStateProperties.ROTATION_16), same model for all 16 rotations
        getVariantBuilder(ModBlocks.SNOWGLOBE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 0).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/snowglobe_particle"))).addModel();
        getVariantBuilder(ModBlocks.SNOWGLOBE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 1).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/snowglobe_particle"))).addModel();
        getVariantBuilder(ModBlocks.SNOWGLOBE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 2).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/snowglobe_particle"))).addModel();
        getVariantBuilder(ModBlocks.SNOWGLOBE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 3).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/snowglobe_particle"))).addModel();
        getVariantBuilder(ModBlocks.SNOWGLOBE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 4).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/snowglobe_particle"))).addModel();
        getVariantBuilder(ModBlocks.SNOWGLOBE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 5).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/snowglobe_particle"))).addModel();
        getVariantBuilder(ModBlocks.SNOWGLOBE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 6).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/snowglobe_particle"))).addModel();
        getVariantBuilder(ModBlocks.SNOWGLOBE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 7).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/snowglobe_particle"))).addModel();
        getVariantBuilder(ModBlocks.SNOWGLOBE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 8).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/snowglobe_particle"))).addModel();
        getVariantBuilder(ModBlocks.SNOWGLOBE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 9).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/snowglobe_particle"))).addModel();
        getVariantBuilder(ModBlocks.SNOWGLOBE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 10).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/snowglobe_particle"))).addModel();
        getVariantBuilder(ModBlocks.SNOWGLOBE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 11).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/snowglobe_particle"))).addModel();
        getVariantBuilder(ModBlocks.SNOWGLOBE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 12).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/snowglobe_particle"))).addModel();
        getVariantBuilder(ModBlocks.SNOWGLOBE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 13).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/snowglobe_particle"))).addModel();
        getVariantBuilder(ModBlocks.SNOWGLOBE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 14).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/snowglobe_particle"))).addModel();
        getVariantBuilder(ModBlocks.SNOWGLOBE.get()).partialState().with(com.hbm_m.block.decorations.TrinketBlock.ROTATION, 15).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/snowglobe_particle"))).addModel();
        // solar_boiler — MachineSolarBoilerBlock (via DummyableMachineBlock): HORIZONTAL_FACING, all 4 facings same model, no rotations
        getVariantBuilder(ModBlocks.SOLAR_BOILER.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/solar_boiler"))).addModel();
        getVariantBuilder(ModBlocks.SOLAR_BOILER.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/solar_boiler"))).addModel();
        getVariantBuilder(ModBlocks.SOLAR_BOILER.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/solar_boiler"))).addModel();
        getVariantBuilder(ModBlocks.SOLAR_BOILER.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/solar_boiler"))).addModel();
        // spotlight_beam — SpotlightBeamBlock: hand-written json has a single "" variant; block has COLOR+6 dir properties, replicated as the same "" partial state
        getVariantBuilder(ModBlocks.SPOTLIGHT_BEAM.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/floodlight_particle"))).addModel();
        // stalactite_ice — NTMPlants.Stalagmite: no state properties, single variant, model path matches block id
        customObjBlockNoFacing(ModBlocks.STALACTITE_ICE);
        // stalactite_snow — NTMPlants.Stalagmite: no state properties, single variant, model path matches block id
        customObjBlockNoFacing(ModBlocks.STALACTITE_SNOW);
        // stalactite_glyphid1 — NTMPlants.Stalagmite: no state properties, single variant, model path matches block id
        customObjBlockNoFacing(ModBlocks.STALACTITE_GLYPHID1);
        // stalactite_glyphid2 — NTMPlants.Stalagmite: no state properties, single variant, model path matches block id
        customObjBlockNoFacing(ModBlocks.STALACTITE_GLYPHID2);
        // stalactite_glyphid3 — NTMPlants.Stalagmite: no state properties, single variant, model path matches block id
        customObjBlockNoFacing(ModBlocks.STALACTITE_GLYPHID3);
        // stalagmite_ice — NTMPlants.Stalagmite: no state properties, single variant, model path matches block id
        customObjBlockNoFacing(ModBlocks.STALAGMITE_ICE);
        // stalagmite_snow — NTMPlants.Stalagmite: no state properties, single variant, model path matches block id
        customObjBlockNoFacing(ModBlocks.STALAGMITE_SNOW);
        // stalagmite_glyphid1 — NTMPlants.Stalagmite: no state properties, single variant, model path matches block id
        customObjBlockNoFacing(ModBlocks.STALAGMITE_GLYPHID1);
        // stalagmite_glyphid2 — NTMPlants.Stalagmite: no state properties, single variant, model path matches block id
        customObjBlockNoFacing(ModBlocks.STALAGMITE_GLYPHID2);
        // stalagmite_glyphid3 — NTMPlants.Stalagmite: no state properties, single variant, model path matches block id
        customObjBlockNoFacing(ModBlocks.STALAGMITE_GLYPHID3);
        // statue_elb_f — StatueBlock: HORIZONTAL_FACING, all 4 facings same model, no rotations
        getVariantBuilder(ModBlocks.STATUE_ELB_F.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/statue_elb_f"))).addModel();
        getVariantBuilder(ModBlocks.STATUE_ELB_F.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/statue_elb_f"))).addModel();
        getVariantBuilder(ModBlocks.STATUE_ELB_F.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/statue_elb_f"))).addModel();
        getVariantBuilder(ModBlocks.STATUE_ELB_F.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/statue_elb_f"))).addModel();
        // steel_corner — SteelCornerBlock (via SteelWallBlock): has FACING, but hand-written json has only a single "" variant; replicated as the same "" partial state
        getVariantBuilder(ModBlocks.STEEL_CORNER.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/steel_corner"))).addModel();
        // steel_pole — DecoObjBlock (Placement.OPPOSITE): HORIZONTAL_FACING with y rotations north=0, south=180, west=270, east=90
        getVariantBuilder(ModBlocks.STEEL_POLE.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_pole"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_POLE.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_pole"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.STEEL_POLE.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_pole"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.STEEL_POLE.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_pole"))).rotationY(90).addModel();
        // steel_roof — DecorShapeBlock: HORIZONTAL_FACING, all 4 facings same model, no rotations; replaces the "генерация удалена" removal at ModBlockStateProvider.java:3089
        getVariantBuilder(ModBlocks.STEEL_ROOF.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_roof"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_ROOF.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_roof"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_ROOF.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_roof"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_ROOF.get()).partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_roof"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_SCAFFOLD_RED.get()).partialState().with(com.hbm_m.block.decorations.SteelScaffoldBlock.ORIENT, com.hbm_m.block.decorations.SteelScaffoldBlock.Orient.UPRIGHT_X).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_scaffold_red_upright_x"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_SCAFFOLD_RED.get()).partialState().with(com.hbm_m.block.decorations.SteelScaffoldBlock.ORIENT, com.hbm_m.block.decorations.SteelScaffoldBlock.Orient.FLAT_NS).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_scaffold_red_flat_ns"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_SCAFFOLD_RED.get()).partialState().with(com.hbm_m.block.decorations.SteelScaffoldBlock.ORIENT, com.hbm_m.block.decorations.SteelScaffoldBlock.Orient.UPRIGHT_Z).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_scaffold_red_upright_z"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_SCAFFOLD_RED.get()).partialState().with(com.hbm_m.block.decorations.SteelScaffoldBlock.ORIENT, com.hbm_m.block.decorations.SteelScaffoldBlock.Orient.FLAT_WE).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_scaffold_red_flat_we"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_SCAFFOLD_WHITE.get()).partialState().with(com.hbm_m.block.decorations.SteelScaffoldBlock.ORIENT, com.hbm_m.block.decorations.SteelScaffoldBlock.Orient.UPRIGHT_X).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_scaffold_white_upright_x"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_SCAFFOLD_WHITE.get()).partialState().with(com.hbm_m.block.decorations.SteelScaffoldBlock.ORIENT, com.hbm_m.block.decorations.SteelScaffoldBlock.Orient.FLAT_NS).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_scaffold_white_flat_ns"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_SCAFFOLD_WHITE.get()).partialState().with(com.hbm_m.block.decorations.SteelScaffoldBlock.ORIENT, com.hbm_m.block.decorations.SteelScaffoldBlock.Orient.UPRIGHT_Z).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_scaffold_white_upright_z"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_SCAFFOLD_WHITE.get()).partialState().with(com.hbm_m.block.decorations.SteelScaffoldBlock.ORIENT, com.hbm_m.block.decorations.SteelScaffoldBlock.Orient.FLAT_WE).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_scaffold_white_flat_we"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_SCAFFOLD_YELLOW.get()).partialState().with(com.hbm_m.block.decorations.SteelScaffoldBlock.ORIENT, com.hbm_m.block.decorations.SteelScaffoldBlock.Orient.UPRIGHT_X).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_scaffold_yellow_upright_x"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_SCAFFOLD_YELLOW.get()).partialState().with(com.hbm_m.block.decorations.SteelScaffoldBlock.ORIENT, com.hbm_m.block.decorations.SteelScaffoldBlock.Orient.FLAT_NS).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_scaffold_yellow_flat_ns"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_SCAFFOLD_YELLOW.get()).partialState().with(com.hbm_m.block.decorations.SteelScaffoldBlock.ORIENT, com.hbm_m.block.decorations.SteelScaffoldBlock.Orient.UPRIGHT_Z).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_scaffold_yellow_upright_z"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_SCAFFOLD_YELLOW.get()).partialState().with(com.hbm_m.block.decorations.SteelScaffoldBlock.ORIENT, com.hbm_m.block.decorations.SteelScaffoldBlock.Orient.FLAT_WE).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_scaffold_yellow_flat_we"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_WALL.get()).partialState().with(com.hbm_m.block.decorations.SteelWallBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_wall_north"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_WALL.get()).partialState().with(com.hbm_m.block.decorations.SteelWallBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_wall_south"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_WALL.get()).partialState().with(com.hbm_m.block.decorations.SteelWallBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_wall_east"))).addModel();
        getVariantBuilder(ModBlocks.STEEL_WALL.get()).partialState().with(com.hbm_m.block.decorations.SteelWallBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/steel_wall_west"))).addModel();
        getVariantBuilder(ModBlocks.TAPE_RECORDER.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/tape_recorder"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.TAPE_RECORDER.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/tape_recorder"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.TAPE_RECORDER.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/tape_recorder"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.TAPE_RECORDER.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/tape_recorder"))).addModel();
        getVariantBuilder(ModBlocks.TEST_BLOCK.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/test/geo_dead"))).addModel();
        getVariantBuilder(ModBlocks.TNT_NTM.get()).partialState().with(com.hbm_m.block.bomb.BlockTNTBase.IGNITE_ON_BREAK, false).modelForState().modelFile(models().getExistingFile(modLoc("block/bomb/tnt_ntm"))).addModel();
        getVariantBuilder(ModBlocks.TNT_NTM.get()).partialState().with(com.hbm_m.block.bomb.BlockTNTBase.IGNITE_ON_BREAK, true).modelForState().modelFile(models().getExistingFile(modLoc("block/bomb/tnt_ntm"))).addModel();
        getVariantBuilder(ModBlocks.TOASTER.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/toaster"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.TOASTER.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/toaster"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.TOASTER.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/toaster"))).addModel();
        getVariantBuilder(ModBlocks.TOASTER.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/toaster"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.TOASTER_STEEL.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/toaster_steel"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.TOASTER_STEEL.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/toaster_steel"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.TOASTER_STEEL.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/toaster_steel"))).addModel();
        getVariantBuilder(ModBlocks.TOASTER_STEEL.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/toaster_steel"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.TOASTER_WOOD.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/toaster_wood"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.TOASTER_WOOD.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/toaster_wood"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.TOASTER_WOOD.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/toaster_wood"))).addModel();
        getVariantBuilder(ModBlocks.TOASTER_WOOD.get()).partialState().with(com.hbm_m.block.decorations.DecoObjBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/toaster_wood"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.TRAPDOOR_STEEL.get()).partialState().with(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.EAST).with(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.BOTTOM).with(net.minecraft.world.level.block.TrapDoorBlock.OPEN, false).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/trapdoor_steel_bottom"))).addModel();
        getVariantBuilder(ModBlocks.TRAPDOOR_STEEL.get()).partialState().with(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.EAST).with(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.BOTTOM).with(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/trapdoor_steel_open"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.TRAPDOOR_STEEL.get()).partialState().with(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.EAST).with(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP).with(net.minecraft.world.level.block.TrapDoorBlock.OPEN, false).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/trapdoor_steel_top"))).addModel();
        getVariantBuilder(ModBlocks.TRAPDOOR_STEEL.get()).partialState().with(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.EAST).with(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP).with(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/trapdoor_steel_open"))).rotationY(90).addModel();
        getVariantBuilder(ModBlocks.TRAPDOOR_STEEL.get()).partialState().with(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.NORTH).with(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.BOTTOM).with(net.minecraft.world.level.block.TrapDoorBlock.OPEN, false).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/trapdoor_steel_bottom"))).addModel();
        getVariantBuilder(ModBlocks.TRAPDOOR_STEEL.get()).partialState().with(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.NORTH).with(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.BOTTOM).with(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/trapdoor_steel_open"))).addModel();
        getVariantBuilder(ModBlocks.TRAPDOOR_STEEL.get()).partialState().with(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.NORTH).with(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP).with(net.minecraft.world.level.block.TrapDoorBlock.OPEN, false).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/trapdoor_steel_top"))).addModel();
        getVariantBuilder(ModBlocks.TRAPDOOR_STEEL.get()).partialState().with(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.NORTH).with(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP).with(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/trapdoor_steel_open"))).addModel();
        getVariantBuilder(ModBlocks.TRAPDOOR_STEEL.get()).partialState().with(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.SOUTH).with(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.BOTTOM).with(net.minecraft.world.level.block.TrapDoorBlock.OPEN, false).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/trapdoor_steel_bottom"))).addModel();
        getVariantBuilder(ModBlocks.TRAPDOOR_STEEL.get()).partialState().with(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.SOUTH).with(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.BOTTOM).with(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/trapdoor_steel_open"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.TRAPDOOR_STEEL.get()).partialState().with(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.SOUTH).with(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP).with(net.minecraft.world.level.block.TrapDoorBlock.OPEN, false).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/trapdoor_steel_top"))).addModel();
        getVariantBuilder(ModBlocks.TRAPDOOR_STEEL.get()).partialState().with(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.SOUTH).with(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP).with(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/trapdoor_steel_open"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.TRAPDOOR_STEEL.get()).partialState().with(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.WEST).with(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.BOTTOM).with(net.minecraft.world.level.block.TrapDoorBlock.OPEN, false).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/trapdoor_steel_bottom"))).addModel();
        getVariantBuilder(ModBlocks.TRAPDOOR_STEEL.get()).partialState().with(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.WEST).with(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.BOTTOM).with(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/trapdoor_steel_open"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.TRAPDOOR_STEEL.get()).partialState().with(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.WEST).with(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP).with(net.minecraft.world.level.block.TrapDoorBlock.OPEN, false).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/trapdoor_steel_top"))).addModel();
        getVariantBuilder(ModBlocks.TRAPDOOR_STEEL.get()).partialState().with(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.WEST).with(net.minecraft.world.level.block.TrapDoorBlock.HALF, net.minecraft.world.level.block.state.properties.Half.TOP).with(net.minecraft.world.level.block.TrapDoorBlock.OPEN, true).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/trapdoor_steel_open"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.VENDING_MACHINE.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/vending_machine_particle"))).addModel();
        getVariantBuilder(ModBlocks.VENDING_MACHINE.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/vending_machine_particle"))).addModel();
        getVariantBuilder(ModBlocks.VENDING_MACHINE.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/vending_machine_particle"))).addModel();
        getVariantBuilder(ModBlocks.VENDING_MACHINE.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/vending_machine_particle"))).addModel();
        getVariantBuilder(ModBlocks.VENDING_MACHINE_SNACKS.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/vending_machine_particle"))).addModel();
        getVariantBuilder(ModBlocks.VENDING_MACHINE_SNACKS.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/vending_machine_particle"))).addModel();
        getVariantBuilder(ModBlocks.VENDING_MACHINE_SNACKS.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/vending_machine_particle"))).addModel();
        getVariantBuilder(ModBlocks.VENDING_MACHINE_SNACKS.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/vending_machine_particle"))).addModel();
        // --- watz_powerplant ---
        getVariantBuilder(ModBlocks.WATZ_POWERPLANT.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, net.minecraft.core.Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/watz_powerplant"))).rotationY(0).addModel();
        getVariantBuilder(ModBlocks.WATZ_POWERPLANT.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, net.minecraft.core.Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/watz_powerplant"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.WATZ_POWERPLANT.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, net.minecraft.core.Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/watz_powerplant"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.WATZ_POWERPLANT.get()).partialState().with(com.hbm_m.block.machines.DummyableMachineBlock.FACING, net.minecraft.core.Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/watz_powerplant"))).rotationY(90).addModel();
        // --- zirnox_destroyed ---
        getVariantBuilder(ModBlocks.ZIRNOX_DESTROYED.get()).partialState().with(com.hbm_m.block.machines.MachineZirnoxDestroyedBlock.FACING, net.minecraft.core.Direction.NORTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/zirnox_destroyed"))).rotationY(0).addModel();
        getVariantBuilder(ModBlocks.ZIRNOX_DESTROYED.get()).partialState().with(com.hbm_m.block.machines.MachineZirnoxDestroyedBlock.FACING, net.minecraft.core.Direction.SOUTH).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/zirnox_destroyed"))).rotationY(180).addModel();
        getVariantBuilder(ModBlocks.ZIRNOX_DESTROYED.get()).partialState().with(com.hbm_m.block.machines.MachineZirnoxDestroyedBlock.FACING, net.minecraft.core.Direction.WEST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/zirnox_destroyed"))).rotationY(270).addModel();
        getVariantBuilder(ModBlocks.ZIRNOX_DESTROYED.get()).partialState().with(com.hbm_m.block.machines.MachineZirnoxDestroyedBlock.FACING, net.minecraft.core.Direction.EAST).modelForState().modelFile(models().getExistingFile(modLoc("block/machines/zirnox_destroyed"))).rotationY(90).addModel();
        // --- zirnox_deb_* ---
        getVariantBuilder(ModBlocks.ZIRNOX_DEB_BLANK.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/machines/zirnox_deb_blank"))).addModel();
        getVariantBuilder(ModBlocks.ZIRNOX_DEB_ELEMENT.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/machines/zirnox_deb_element"))).addModel();
        getVariantBuilder(ModBlocks.ZIRNOX_DEB_SHRAPNEL.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/machines/zirnox_deb_shrapnel"))).addModel();
        getVariantBuilder(ModBlocks.ZIRNOX_DEB_CONCRETE.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/machines/zirnox_deb_concrete"))).addModel();
        getVariantBuilder(ModBlocks.ZIRNOX_DEB_EXCHANGER.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/machines/zirnox_deb_exchanger"))).addModel();
        // --- wood_structure (roof; Ziffernfolge = nx px) ---
        getVariantBuilder(ModBlocks.WOOD_STRUCTURE.get()).partialState().with(com.hbm_m.block.generic.R6dBlocks.WoodStructure.NX, false).with(com.hbm_m.block.generic.R6dBlocks.WoodStructure.PX, false).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/wood_structure_roof_00"))).addModel();
        getVariantBuilder(ModBlocks.WOOD_STRUCTURE.get()).partialState().with(com.hbm_m.block.generic.R6dBlocks.WoodStructure.NX, false).with(com.hbm_m.block.generic.R6dBlocks.WoodStructure.PX, true).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/wood_structure_roof_01"))).addModel();
        getVariantBuilder(ModBlocks.WOOD_STRUCTURE.get()).partialState().with(com.hbm_m.block.generic.R6dBlocks.WoodStructure.NX, true).with(com.hbm_m.block.generic.R6dBlocks.WoodStructure.PX, false).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/wood_structure_roof_10"))).addModel();
        getVariantBuilder(ModBlocks.WOOD_STRUCTURE.get()).partialState().with(com.hbm_m.block.generic.R6dBlocks.WoodStructure.NX, true).with(com.hbm_m.block.generic.R6dBlocks.WoodStructure.PX, true).modelForState().modelFile(models().getExistingFile(modLoc("block/deco/wood_structure_roof_11"))).addModel();
        // --- wood_structure_ceiling ---
        getVariantBuilder(ModBlocks.WOOD_STRUCTURE_CEILING.get()).partialState().modelForState().modelFile(models().getExistingFile(modLoc("block/deco/wood_structure_ceiling"))).addModel();
        // --- wood_structure_scaffold (Bits py nx nz px pz, py=MSB) ---
        migVar("wood_structure_scaffold", "nx=false,nz=false,px=false,py=false,pz=false", "block/deco/wood_structure_scaffold_00000", 0, 0);
        migVar("wood_structure_scaffold", "nx=false,nz=false,px=false,py=false,pz=true", "block/deco/wood_structure_scaffold_00001", 0, 0);
        migVar("wood_structure_scaffold", "nx=false,nz=false,px=true,py=false,pz=false", "block/deco/wood_structure_scaffold_00010", 0, 0);
        migVar("wood_structure_scaffold", "nx=false,nz=false,px=true,py=false,pz=true", "block/deco/wood_structure_scaffold_00011", 0, 0);
        migVar("wood_structure_scaffold", "nx=false,nz=true,px=false,py=false,pz=false", "block/deco/wood_structure_scaffold_00100", 0, 0);
        migVar("wood_structure_scaffold", "nx=false,nz=true,px=false,py=false,pz=true", "block/deco/wood_structure_scaffold_00101", 0, 0);
        migVar("wood_structure_scaffold", "nx=false,nz=true,px=true,py=false,pz=false", "block/deco/wood_structure_scaffold_00110", 0, 0);
        migVar("wood_structure_scaffold", "nx=false,nz=true,px=true,py=false,pz=true", "block/deco/wood_structure_scaffold_00111", 0, 0);
        migVar("wood_structure_scaffold", "nx=true,nz=false,px=false,py=false,pz=false", "block/deco/wood_structure_scaffold_01000", 0, 0);
        migVar("wood_structure_scaffold", "nx=true,nz=false,px=false,py=false,pz=true", "block/deco/wood_structure_scaffold_01001", 0, 0);
        migVar("wood_structure_scaffold", "nx=true,nz=false,px=true,py=false,pz=false", "block/deco/wood_structure_scaffold_01010", 0, 0);
        migVar("wood_structure_scaffold", "nx=true,nz=false,px=true,py=false,pz=true", "block/deco/wood_structure_scaffold_01011", 0, 0);
        migVar("wood_structure_scaffold", "nx=true,nz=true,px=false,py=false,pz=false", "block/deco/wood_structure_scaffold_01100", 0, 0);
        migVar("wood_structure_scaffold", "nx=true,nz=true,px=false,py=false,pz=true", "block/deco/wood_structure_scaffold_01101", 0, 0);
        migVar("wood_structure_scaffold", "nx=true,nz=true,px=true,py=false,pz=false", "block/deco/wood_structure_scaffold_01110", 0, 0);
        migVar("wood_structure_scaffold", "nx=true,nz=true,px=true,py=false,pz=true", "block/deco/wood_structure_scaffold_01111", 0, 0);
        migVar("wood_structure_scaffold", "nx=false,nz=false,px=false,py=true,pz=false", "block/deco/wood_structure_scaffold_10000", 0, 0);
        migVar("wood_structure_scaffold", "nx=false,nz=false,px=false,py=true,pz=true", "block/deco/wood_structure_scaffold_10001", 0, 0);
        migVar("wood_structure_scaffold", "nx=false,nz=false,px=true,py=true,pz=false", "block/deco/wood_structure_scaffold_10010", 0, 0);
        migVar("wood_structure_scaffold", "nx=false,nz=false,px=true,py=true,pz=true", "block/deco/wood_structure_scaffold_10011", 0, 0);
        migVar("wood_structure_scaffold", "nx=false,nz=true,px=false,py=true,pz=false", "block/deco/wood_structure_scaffold_10100", 0, 0);
        migVar("wood_structure_scaffold", "nx=false,nz=true,px=false,py=true,pz=true", "block/deco/wood_structure_scaffold_10101", 0, 0);
        migVar("wood_structure_scaffold", "nx=false,nz=true,px=true,py=true,pz=false", "block/deco/wood_structure_scaffold_10110", 0, 0);
        migVar("wood_structure_scaffold", "nx=false,nz=true,px=true,py=true,pz=true", "block/deco/wood_structure_scaffold_10111", 0, 0);
        migVar("wood_structure_scaffold", "nx=true,nz=false,px=false,py=true,pz=false", "block/deco/wood_structure_scaffold_11000", 0, 0);
        migVar("wood_structure_scaffold", "nx=true,nz=false,px=false,py=true,pz=true", "block/deco/wood_structure_scaffold_11001", 0, 0);
        migVar("wood_structure_scaffold", "nx=true,nz=false,px=true,py=true,pz=false", "block/deco/wood_structure_scaffold_11010", 0, 0);
        migVar("wood_structure_scaffold", "nx=true,nz=false,px=true,py=true,pz=true", "block/deco/wood_structure_scaffold_11011", 0, 0);
        migVar("wood_structure_scaffold", "nx=true,nz=true,px=false,py=true,pz=false", "block/deco/wood_structure_scaffold_11100", 0, 0);
        migVar("wood_structure_scaffold", "nx=true,nz=true,px=false,py=true,pz=true", "block/deco/wood_structure_scaffold_11101", 0, 0);
        migVar("wood_structure_scaffold", "nx=true,nz=true,px=true,py=true,pz=false", "block/deco/wood_structure_scaffold_11110", 0, 0);
        migVar("wood_structure_scaffold", "nx=true,nz=true,px=true,py=true,pz=true", "block/deco/wood_structure_scaffold_11111", 0, 0);
        // --- wire_coated (multipart; !straight_x/y/z als positive OR-Liste) ---
        getMultipartBuilder(ModBlocks.WIRE_COATED.get()).part().modelFile(models().getExistingFile(modLoc("block/wire_coated_core"))).addModel().condition(com.hbm_m.api.energy.WireBlock.CENTER, com.hbm_m.api.energy.WireCenterVisual.JUNCTION).end();
        getMultipartBuilder(ModBlocks.WIRE_COATED.get()).part().modelFile(models().getExistingFile(modLoc("block/wire_coated_straight_x"))).addModel().condition(com.hbm_m.api.energy.WireBlock.CENTER, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_X).end();
        getMultipartBuilder(ModBlocks.WIRE_COATED.get()).part().modelFile(models().getExistingFile(modLoc("block/wire_coated_straight_y"))).addModel().condition(com.hbm_m.api.energy.WireBlock.CENTER, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_Y).end();
        getMultipartBuilder(ModBlocks.WIRE_COATED.get()).part().modelFile(models().getExistingFile(modLoc("block/wire_coated_straight_z"))).addModel().condition(com.hbm_m.api.energy.WireBlock.CENTER, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_Z).end();
        getMultipartBuilder(ModBlocks.WIRE_COATED.get()).part().modelFile(models().getExistingFile(modLoc("block/wire_coated_arm_north"))).addModel().condition(com.hbm_m.api.energy.WireBlock.NORTH, true).condition(com.hbm_m.api.energy.WireBlock.CENTER, com.hbm_m.api.energy.WireCenterVisual.JUNCTION, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_X, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_Y).end();
        getMultipartBuilder(ModBlocks.WIRE_COATED.get()).part().modelFile(models().getExistingFile(modLoc("block/wire_coated_arm_south"))).addModel().condition(com.hbm_m.api.energy.WireBlock.SOUTH, true).condition(com.hbm_m.api.energy.WireBlock.CENTER, com.hbm_m.api.energy.WireCenterVisual.JUNCTION, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_X, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_Y).end();
        getMultipartBuilder(ModBlocks.WIRE_COATED.get()).part().modelFile(models().getExistingFile(modLoc("block/wire_coated_arm_east"))).addModel().condition(com.hbm_m.api.energy.WireBlock.EAST, true).condition(com.hbm_m.api.energy.WireBlock.CENTER, com.hbm_m.api.energy.WireCenterVisual.JUNCTION, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_Z, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_Y).end();
        getMultipartBuilder(ModBlocks.WIRE_COATED.get()).part().modelFile(models().getExistingFile(modLoc("block/wire_coated_arm_west"))).addModel().condition(com.hbm_m.api.energy.WireBlock.WEST, true).condition(com.hbm_m.api.energy.WireBlock.CENTER, com.hbm_m.api.energy.WireCenterVisual.JUNCTION, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_Z, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_Y).end();
        getMultipartBuilder(ModBlocks.WIRE_COATED.get()).part().modelFile(models().getExistingFile(modLoc("block/wire_coated_arm_up"))).addModel().condition(com.hbm_m.api.energy.WireBlock.UP, true).condition(com.hbm_m.api.energy.WireBlock.CENTER, com.hbm_m.api.energy.WireCenterVisual.JUNCTION, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_X, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_Z).end();
        getMultipartBuilder(ModBlocks.WIRE_COATED.get()).part().modelFile(models().getExistingFile(modLoc("block/wire_coated_arm_down"))).addModel().condition(com.hbm_m.api.energy.WireBlock.DOWN, true).condition(com.hbm_m.api.energy.WireBlock.CENTER, com.hbm_m.api.energy.WireCenterVisual.JUNCTION, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_X, com.hbm_m.api.energy.WireCenterVisual.STRAIGHT_Z).end();
    }

    /**
     * CP-1 pile channel blocks (1:1 aus NEXT): cube_column-Modelle mit gemeinsamer
     * Seitentextur {@code block/block_graphite}; Modellname/-wahl nach SHROUDED (Suffix
     * {@code _aluminum}) dann WITHDRAWN (Suffix {@code _out}); Varianten-Rotationen wie in
     * NEXTs Blockstate-JSONs: axis=x → x:90,y:90, axis=y → keine, axis=z → x:90.
     *
     * @param baseName  Modell-Basisname (entspricht der Block-Registry-ID)
     * @param plainEnd  End-Textur der Normal-Variante (NEXT: rod zeigt auf block_graphite_rod_in)
     * @param outEnd    End-Textur der withdrawn-Variante (null, wenn hasWithdrawn == false)
     * @param hasWithdrawn ob der Block ein withdrawn-Property besitzt (rod/detector: ja, drilled: nein)
     */
    private void pilePillarBlock(Block block, String baseName, String plainEnd, String outEnd, boolean hasWithdrawn) {
        ModelFile plain = models().cubeColumn(baseName, modLoc("block/block_graphite"), modLoc("block/" + plainEnd));
        ModelFile aluminum = models().cubeColumn(baseName + "_aluminum", modLoc("block/block_graphite"), modLoc("block/" + plainEnd + "_aluminum"));
        ModelFile out = hasWithdrawn ? models().cubeColumn(baseName + "_out", modLoc("block/block_graphite"), modLoc("block/" + outEnd)) : null;
        ModelFile outAluminum = hasWithdrawn ? models().cubeColumn(baseName + "_out_aluminum", modLoc("block/block_graphite"), modLoc("block/" + outEnd + "_aluminum")) : null;
        getVariantBuilder(block).forAllStates(state -> {
            boolean shrouded = state.getValue(com.hbm_m.block.machines.pile.BlockGraphiteDrilledBase.SHROUDED);
            boolean withdrawn = hasWithdrawn && state.getValue(com.hbm_m.block.machines.pile.BlockGraphiteDrilledBase.WITHDRAWN);
            ModelFile model = plain;
            if (shrouded && withdrawn) model = outAluminum;
            else if (withdrawn) model = out;
            else if (shrouded) model = aluminum;
            ConfiguredModel.Builder<?> mb = ConfiguredModel.builder().modelFile(model);
            Direction.Axis axis = state.getValue(com.hbm_m.block.machines.pile.BlockGraphiteDrilledBase.AXIS);
            if (axis == Direction.Axis.X) mb = mb.rotationX(90).rotationY(90);
            else if (axis == Direction.Axis.Z) mb = mb.rotationX(90);
            // Forge: build() liefert bereits ConfiguredModel[] (forAllStates erwartet Function<.., ConfiguredModel[]>)
            return mb.build();
        });
        simpleBlockItem(block, plain);
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
        // vI: erschoepftes Oelvorkommen hatte weder Blockzustand noch Modell (Missing-Model); Original BlockGeneric "ore_oil_empty"
        simpleBlockWithItem(ModBlocks.ORE_OIL_EMPTY.get(), models().cubeAll("ore_oil_empty", modLoc("block/ore_oil_empty")));
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
        // Restport: ducrete_stairs, brick_asbestos_slab, pink_slab, pink_stairs
        stairsBlock((net.minecraft.world.level.block.StairBlock) ModBlocks.DUCRETE_STAIRS.get(), mcOrMod("block/ducrete")); simpleBlockItem(ModBlocks.DUCRETE_STAIRS.get(), models().getExistingFile(modLoc("block/ducrete_stairs")));
        slabBlock((net.minecraft.world.level.block.SlabBlock) ModBlocks.BRICK_ASBESTOS_SLAB.get(), models().slab("brick_asbestos_slab", mcOrMod("block/brick_asbestos"), mcOrMod("block/brick_asbestos"), mcOrMod("block/brick_asbestos")), models().slabTop("brick_asbestos_slab_top", mcOrMod("block/brick_asbestos"), mcOrMod("block/brick_asbestos"), mcOrMod("block/brick_asbestos")), models().cubeAll("brick_asbestos_slab_double", mcOrMod("block/brick_asbestos"))); simpleBlockItem(ModBlocks.BRICK_ASBESTOS_SLAB.get(), models().getExistingFile(modLoc("block/brick_asbestos_slab")));
        slabBlock((net.minecraft.world.level.block.SlabBlock) ModBlocks.PINK_SLAB.get(), models().slab("pink_slab", mcOrMod("block/pink_planks"), mcOrMod("block/pink_planks"), mcOrMod("block/pink_planks")), models().slabTop("pink_slab_top", mcOrMod("block/pink_planks"), mcOrMod("block/pink_planks"), mcOrMod("block/pink_planks")), models().cubeAll("pink_slab_double", mcOrMod("block/pink_planks"))); simpleBlockItem(ModBlocks.PINK_SLAB.get(), models().getExistingFile(modLoc("block/pink_slab")));
        stairsBlock((net.minecraft.world.level.block.StairBlock) ModBlocks.PINK_STAIRS.get(), mcOrMod("block/pink_planks")); simpleBlockItem(ModBlocks.PINK_STAIRS.get(), models().getExistingFile(modLoc("block/pink_stairs")));
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
     * Руда базальта (порт BlockOreBasalt 1.7.10): боковая текстура {@code ore_basalt_<subtype>}
     * и верхняя {@code ore_basalt_<subtype>_top} — она же идёт в низ (в оригинале side <= 1
     * возвращает topIcons, т.е. и верх, и низ показывают "_top").
     */
    private void basaltOreWithItem(RegistrySupplier<Block> blockObject) {
        String name = blockObject.getId().getPath(); // например "ore_basalt_sulfur"
        ModelFile model = models().cubeBottomTop(
                name,
                modLoc("block/" + name),
                modLoc("block/" + name + "_top"),
                modLoc("block/" + name + "_top"));
        simpleBlockWithItem(blockObject.get(), model);
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
        ModelFile mrbmk_rbmk_autoloader = models().withExistingParent("block/rbmk/rbmk_autoloader", mcLoc("block/cube_all"))
                .texture("all", modLoc("block/rbmk/standalone_rbmk_autoloader"))
                .texture("particle", modLoc("block/rbmk/standalone_rbmk_autoloader"));
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

    /** FFA-порт: воспроизводит ручной blockstate из variants-JSON. Ключ "prop=val,..." или "". */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void migVar(String blockName, String key, String model, int rotX, int rotY) {
        try {
        Block block = migBlock(blockName);
        VariantBlockStateBuilder.PartialBlockstate ps = getVariantBuilder(block).partialState();
        if (!key.isEmpty()) {
            for (String kv : key.split(",")) {
                String[] p = kv.split("=", 2);
                net.minecraft.world.level.block.state.properties.Property prop = migFindProp(block, p[0]);
                ps = ps.with(prop, (Comparable) migValOn(prop, p[1], blockName));
            }
        }
        var mb = ps.modelForState().modelFile(models().getExistingFile(modLoc(model)));
        if (rotX != 0) mb.rotationX(rotX);
        if (rotY != 0) mb.rotationY(rotY);
        mb.addModel();
        } catch (RuntimeException e) {
            throw new IllegalStateException("FFA-PORT: migVar failed for block '" + blockName + "' key '" + key + "'", e);
        }
    }

    private net.minecraft.world.level.block.state.properties.Property migFindProp(Block block, String name) {
        for (net.minecraft.world.level.block.state.properties.Property q : block.getStateDefinition().getProperties()) {
            if (q.getName().equals(name)) return q;
        }
        throw new IllegalStateException("FFA-PORT: property '" + name + "' not found on " + block);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Comparable migValOn(net.minecraft.world.level.block.state.properties.Property prop, String val, String blockName) {
        if (prop instanceof net.minecraft.world.level.block.state.properties.BooleanProperty) return Boolean.parseBoolean(val);
        if (prop instanceof net.minecraft.world.level.block.state.properties.IntegerProperty) return Integer.parseInt(val);
        for (Object v : prop.getPossibleValues()) {
            if (v instanceof net.minecraft.util.StringRepresentable sr && sr.getSerializedName().equals(val)) return (Comparable) v;
            if (v instanceof Direction d && d.getName().equals(val)) return (Comparable) v;
        }
        throw new IllegalStateException("FFA-PORT: value '" + val + "' invalid for '" + prop.getName() + "' on " + blockName);
    }

    private Block mpOwner;

    private ModelFile exm(String path) { return models().getExistingFile(modLoc(path)); }

    private net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder mpPart(net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder b, String model, int x, int y) {
        var pb = b.part().modelFile(exm(model));
        if (x != 0) pb = pb.rotationX(x);
        if (y != 0) pb = pb.rotationY(y);
        return pb.addModel();
    }

    private net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder mpOr(net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder p) { p.useOr(); return p; }

    private net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder.ConditionGroup mpOr(net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder.ConditionGroup g) { g.useOr = true; return g; }

    private net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder.ConditionGroup mpNg(net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder p) { return p.nestedGroup(); }

    private net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder.ConditionGroup mpNg(net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder.ConditionGroup g) { return g.nestedGroup(); }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder mpCond(net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder p, String prop, String... vals) {
        Comparable[] cv = new Comparable[vals.length];
        for (int i = 0; i < vals.length; i++) cv[i] = migValOn(migFindProp(mpOwner, prop), vals[i], "mp:" + prop);
        return p.condition(migFindProp(mpOwner, prop), cv);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder.ConditionGroup mpCond(net.minecraftforge.client.model.generators.MultiPartBlockStateBuilder.PartBuilder.ConditionGroup g, String prop, String... vals) {
        Comparable[] cv = new Comparable[vals.length];
        for (int i = 0; i < vals.length; i++) cv[i] = migValOn(migFindProp(mpOwner, prop), vals[i], "mp:" + prop);
        return g.condition(migFindProp(mpOwner, prop), cv);
    }

    /** FFA-порт: все блоки, чьи ручные blockstates отличались от сгенерированных. */
    private void registerFfaBlockstatePorts() {
        // --- block_c4
        // --- block_graphite_tritium
        // --- block_semtex
        // --- boxcar
        // --- brick_forgotten
        // --- brick_jungle_trap
        // --- cable_detector
        // --- cable_diode (multipart)
        {
            mpOwner = migBlock("cable_diode");
            var mp = getMultipartBuilder(mpOwner);
            var p0 = mpPart(mp, "block/cable_diode_pad", 0, 0);
            var p1 = mpPart(mp, "block/cable_diode_plate", 0, 180);
            mpCond(p1, "facing", "north");
            var p2 = mpPart(mp, "block/cable_diode_plate", 0, 270);
            mpCond(p2, "facing", "east");
            var p3 = mpPart(mp, "block/cable_diode_plate", 0, 0);
            mpCond(p3, "facing", "south");
            var p4 = mpPart(mp, "block/cable_diode_plate", 0, 90);
            mpCond(p4, "facing", "west");
            var p5 = mpPart(mp, "block/cable_diode_plate", 90, 0);
            mpCond(p5, "facing", "up");
            var p6 = mpPart(mp, "block/cable_diode_plate", 270, 0);
            mpCond(p6, "facing", "down");
            var p7 = mpPart(mp, "block/red_cable_arm_north", 0, 0);
            mpCond(p7, "north", "true");
            var p8 = mpPart(mp, "block/red_cable_arm_east", 0, 0);
            mpCond(p8, "east", "true");
            var p9 = mpPart(mp, "block/red_cable_arm_south", 0, 0);
            mpCond(p9, "south", "true");
            var p10 = mpPart(mp, "block/red_cable_arm_west", 0, 0);
            mpCond(p10, "west", "true");
            var p11 = mpPart(mp, "block/red_cable_arm_up", 0, 0);
            mpCond(p11, "up", "true");
            var p12 = mpPart(mp, "block/red_cable_arm_down", 0, 0);
            mpCond(p12, "down", "true");
        }
        // --- cable_switch
        // --- charge_c4
        // --- charge_dynamite
        // --- charge_miner
        // --- charge_semtex
        // --- crane_partitioner
        // --- crane_splitter
        // --- deco_pipe
        migVar("deco_pipe", "axis=x", "block/pipes/deco_pipe_x", 0, 0);
        migVar("deco_pipe", "axis=y", "block/pipes/deco_pipe_y", 0, 0);
        migVar("deco_pipe", "axis=z", "block/pipes/deco_pipe_z", 0, 0);
        // --- deco_pipe_framed
        migVar("deco_pipe_framed", "axis=x", "block/pipes/deco_pipe_framed_x", 0, 0);
        migVar("deco_pipe_framed", "axis=y", "block/pipes/deco_pipe_framed_y", 0, 0);
        migVar("deco_pipe_framed", "axis=z", "block/pipes/deco_pipe_framed_z", 0, 0);
        // --- deco_pipe_framed_green_rusted
        migVar("deco_pipe_framed_green_rusted", "axis=x", "block/pipes/deco_pipe_framed_green_rusted_x", 0, 0);
        migVar("deco_pipe_framed_green_rusted", "axis=y", "block/pipes/deco_pipe_framed_green_rusted_y", 0, 0);
        migVar("deco_pipe_framed_green_rusted", "axis=z", "block/pipes/deco_pipe_framed_green_rusted_z", 0, 0);
        // --- deco_pipe_framed_red
        migVar("deco_pipe_framed_red", "axis=x", "block/pipes/deco_pipe_framed_red_x", 0, 0);
        migVar("deco_pipe_framed_red", "axis=y", "block/pipes/deco_pipe_framed_red_y", 0, 0);
        migVar("deco_pipe_framed_red", "axis=z", "block/pipes/deco_pipe_framed_red_z", 0, 0);
        // --- deco_pipe_framed_rusted
        migVar("deco_pipe_framed_rusted", "axis=x", "block/pipes/deco_pipe_framed_rusted_x", 0, 0);
        migVar("deco_pipe_framed_rusted", "axis=y", "block/pipes/deco_pipe_framed_rusted_y", 0, 0);
        migVar("deco_pipe_framed_rusted", "axis=z", "block/pipes/deco_pipe_framed_rusted_z", 0, 0);
        // --- deco_pipe_marked
        migVar("deco_pipe_marked", "axis=x", "block/pipes/deco_pipe_marked_x", 0, 0);
        migVar("deco_pipe_marked", "axis=y", "block/pipes/deco_pipe_marked_y", 0, 0);
        migVar("deco_pipe_marked", "axis=z", "block/pipes/deco_pipe_marked_z", 0, 0);
        // --- deco_pipe_quad
        migVar("deco_pipe_quad", "axis=x", "block/pipes/deco_pipe_quad_x", 0, 0);
        migVar("deco_pipe_quad", "axis=y", "block/pipes/deco_pipe_quad_y", 0, 0);
        migVar("deco_pipe_quad", "axis=z", "block/pipes/deco_pipe_quad_z", 0, 0);
        // --- deco_pipe_quad_marked
        migVar("deco_pipe_quad_marked", "axis=x", "block/pipes/deco_pipe_quad_marked_x", 0, 0);
        migVar("deco_pipe_quad_marked", "axis=y", "block/pipes/deco_pipe_quad_marked_y", 0, 0);
        migVar("deco_pipe_quad_marked", "axis=z", "block/pipes/deco_pipe_quad_marked_z", 0, 0);
        // --- deco_pipe_quad_red
        migVar("deco_pipe_quad_red", "axis=x", "block/pipes/deco_pipe_quad_red_x", 0, 0);
        migVar("deco_pipe_quad_red", "axis=y", "block/pipes/deco_pipe_quad_red_y", 0, 0);
        migVar("deco_pipe_quad_red", "axis=z", "block/pipes/deco_pipe_quad_red_z", 0, 0);
        // --- deco_pipe_quad_rusted
        migVar("deco_pipe_quad_rusted", "axis=x", "block/pipes/deco_pipe_quad_rusted_x", 0, 0);
        migVar("deco_pipe_quad_rusted", "axis=y", "block/pipes/deco_pipe_quad_rusted_y", 0, 0);
        migVar("deco_pipe_quad_rusted", "axis=z", "block/pipes/deco_pipe_quad_rusted_z", 0, 0);
        // --- deco_pipe_red
        migVar("deco_pipe_red", "axis=x", "block/pipes/deco_pipe_red_x", 0, 0);
        migVar("deco_pipe_red", "axis=y", "block/pipes/deco_pipe_red_y", 0, 0);
        migVar("deco_pipe_red", "axis=z", "block/pipes/deco_pipe_red_z", 0, 0);
        // --- deco_pipe_rim
        // --- deco_pipe_rim_marked
        migVar("deco_pipe_rim_marked", "axis=x", "block/pipes/deco_pipe_rim_marked_x", 0, 0);
        migVar("deco_pipe_rim_marked", "axis=y", "block/pipes/deco_pipe_rim_marked_y", 0, 0);
        migVar("deco_pipe_rim_marked", "axis=z", "block/pipes/deco_pipe_rim_marked_z", 0, 0);
        // --- deco_pipe_rim_rusted
        migVar("deco_pipe_rim_rusted", "axis=x", "block/pipes/deco_pipe_rim_rusted_x", 0, 0);
        migVar("deco_pipe_rim_rusted", "axis=y", "block/pipes/deco_pipe_rim_rusted_y", 0, 0);
        migVar("deco_pipe_rim_rusted", "axis=z", "block/pipes/deco_pipe_rim_rusted_z", 0, 0);
        // --- deco_pipe_rusted
        migVar("deco_pipe_rusted", "axis=x", "block/pipes/deco_pipe_rusted_x", 0, 0);
        migVar("deco_pipe_rusted", "axis=y", "block/pipes/deco_pipe_rusted_y", 0, 0);
        migVar("deco_pipe_rusted", "axis=z", "block/pipes/deco_pipe_rusted_z", 0, 0);
        // --- digamma_matter
        // --- fireworks
        // --- fissure_bomb
        // --- flame_war
        // --- fluid_counter_valve
        // --- fluid_duct_paintable
        // --- fluid_duct_paintable_block_exhaust
        // --- fluid_switch
        // --- fluid_valve
        // --- fluorescent_lamp_off
        migVar("fluorescent_lamp_off", "broken=false,conn=down,facing=down,part=single", "block/lights/fluorescent_lamp_off_down_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=down,facing=down,part=single", "block/lights/fluorescent_lamp_off_down_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=up,facing=down,part=single", "block/lights/fluorescent_lamp_off_down_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=up,facing=down,part=single", "block/lights/fluorescent_lamp_off_down_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=north,facing=down,part=single", "block/lights/fluorescent_lamp_off_down_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=north,facing=down,part=single", "block/lights/fluorescent_lamp_off_down_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=south,facing=down,part=single", "block/lights/fluorescent_lamp_off_down_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=south,facing=down,part=single", "block/lights/fluorescent_lamp_off_down_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=west,facing=down,part=single", "block/lights/fluorescent_lamp_off_down_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=west,facing=down,part=single", "block/lights/fluorescent_lamp_off_down_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=east,facing=down,part=single", "block/lights/fluorescent_lamp_off_down_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=east,facing=down,part=single", "block/lights/fluorescent_lamp_off_down_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=down,facing=down,part=cap", "block/lights/fluorescent_lamp_off_down_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=down,facing=down,part=cap", "block/lights/fluorescent_lamp_off_down_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=down,facing=down,part=mid", "block/lights/fluorescent_lamp_off_down_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=down,facing=down,part=mid", "block/lights/fluorescent_lamp_off_down_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=up,facing=down,part=cap", "block/lights/fluorescent_lamp_off_down_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=up,facing=down,part=cap", "block/lights/fluorescent_lamp_off_down_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=up,facing=down,part=mid", "block/lights/fluorescent_lamp_off_down_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=up,facing=down,part=mid", "block/lights/fluorescent_lamp_off_down_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=north,facing=down,part=cap", "block/lights/fluorescent_lamp_off_down_cap_north", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=north,facing=down,part=cap", "block/lights/fluorescent_lamp_off_down_cap_north", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=north,facing=down,part=mid", "block/lights/fluorescent_lamp_off_down_mid_north", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=north,facing=down,part=mid", "block/lights/fluorescent_lamp_off_down_mid_north", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=south,facing=down,part=cap", "block/lights/fluorescent_lamp_off_down_cap_south", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=south,facing=down,part=cap", "block/lights/fluorescent_lamp_off_down_cap_south", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=south,facing=down,part=mid", "block/lights/fluorescent_lamp_off_down_mid_south", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=south,facing=down,part=mid", "block/lights/fluorescent_lamp_off_down_mid_south", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=west,facing=down,part=cap", "block/lights/fluorescent_lamp_off_down_cap_west", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=west,facing=down,part=cap", "block/lights/fluorescent_lamp_off_down_cap_west", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=west,facing=down,part=mid", "block/lights/fluorescent_lamp_off_down_mid_west", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=west,facing=down,part=mid", "block/lights/fluorescent_lamp_off_down_mid_west", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=east,facing=down,part=cap", "block/lights/fluorescent_lamp_off_down_cap_east", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=east,facing=down,part=cap", "block/lights/fluorescent_lamp_off_down_cap_east", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=east,facing=down,part=mid", "block/lights/fluorescent_lamp_off_down_mid_east", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=east,facing=down,part=mid", "block/lights/fluorescent_lamp_off_down_mid_east", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=down,facing=up,part=single", "block/lights/fluorescent_lamp_off_up_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=down,facing=up,part=single", "block/lights/fluorescent_lamp_off_up_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=up,facing=up,part=single", "block/lights/fluorescent_lamp_off_up_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=up,facing=up,part=single", "block/lights/fluorescent_lamp_off_up_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=north,facing=up,part=single", "block/lights/fluorescent_lamp_off_up_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=north,facing=up,part=single", "block/lights/fluorescent_lamp_off_up_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=south,facing=up,part=single", "block/lights/fluorescent_lamp_off_up_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=south,facing=up,part=single", "block/lights/fluorescent_lamp_off_up_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=west,facing=up,part=single", "block/lights/fluorescent_lamp_off_up_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=west,facing=up,part=single", "block/lights/fluorescent_lamp_off_up_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=east,facing=up,part=single", "block/lights/fluorescent_lamp_off_up_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=east,facing=up,part=single", "block/lights/fluorescent_lamp_off_up_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=down,facing=up,part=cap", "block/lights/fluorescent_lamp_off_up_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=down,facing=up,part=cap", "block/lights/fluorescent_lamp_off_up_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=down,facing=up,part=mid", "block/lights/fluorescent_lamp_off_up_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=down,facing=up,part=mid", "block/lights/fluorescent_lamp_off_up_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=up,facing=up,part=cap", "block/lights/fluorescent_lamp_off_up_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=up,facing=up,part=cap", "block/lights/fluorescent_lamp_off_up_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=up,facing=up,part=mid", "block/lights/fluorescent_lamp_off_up_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=up,facing=up,part=mid", "block/lights/fluorescent_lamp_off_up_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=north,facing=up,part=cap", "block/lights/fluorescent_lamp_off_up_cap_north", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=north,facing=up,part=cap", "block/lights/fluorescent_lamp_off_up_cap_north", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=north,facing=up,part=mid", "block/lights/fluorescent_lamp_off_up_mid_north", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=north,facing=up,part=mid", "block/lights/fluorescent_lamp_off_up_mid_north", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=south,facing=up,part=cap", "block/lights/fluorescent_lamp_off_up_cap_south", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=south,facing=up,part=cap", "block/lights/fluorescent_lamp_off_up_cap_south", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=south,facing=up,part=mid", "block/lights/fluorescent_lamp_off_up_mid_south", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=south,facing=up,part=mid", "block/lights/fluorescent_lamp_off_up_mid_south", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=west,facing=up,part=cap", "block/lights/fluorescent_lamp_off_up_cap_west", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=west,facing=up,part=cap", "block/lights/fluorescent_lamp_off_up_cap_west", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=west,facing=up,part=mid", "block/lights/fluorescent_lamp_off_up_mid_west", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=west,facing=up,part=mid", "block/lights/fluorescent_lamp_off_up_mid_west", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=east,facing=up,part=cap", "block/lights/fluorescent_lamp_off_up_cap_east", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=east,facing=up,part=cap", "block/lights/fluorescent_lamp_off_up_cap_east", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=east,facing=up,part=mid", "block/lights/fluorescent_lamp_off_up_mid_east", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=east,facing=up,part=mid", "block/lights/fluorescent_lamp_off_up_mid_east", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=down,facing=north,part=single", "block/lights/fluorescent_lamp_off_north_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=down,facing=north,part=single", "block/lights/fluorescent_lamp_off_north_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=up,facing=north,part=single", "block/lights/fluorescent_lamp_off_north_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=up,facing=north,part=single", "block/lights/fluorescent_lamp_off_north_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=north,facing=north,part=single", "block/lights/fluorescent_lamp_off_north_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=north,facing=north,part=single", "block/lights/fluorescent_lamp_off_north_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=south,facing=north,part=single", "block/lights/fluorescent_lamp_off_north_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=south,facing=north,part=single", "block/lights/fluorescent_lamp_off_north_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=west,facing=north,part=single", "block/lights/fluorescent_lamp_off_north_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=west,facing=north,part=single", "block/lights/fluorescent_lamp_off_north_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=east,facing=north,part=single", "block/lights/fluorescent_lamp_off_north_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=east,facing=north,part=single", "block/lights/fluorescent_lamp_off_north_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=down,facing=north,part=cap", "block/lights/fluorescent_lamp_off_north_cap_down", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=down,facing=north,part=cap", "block/lights/fluorescent_lamp_off_north_cap_down", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=down,facing=north,part=mid", "block/lights/fluorescent_lamp_off_north_mid_down", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=down,facing=north,part=mid", "block/lights/fluorescent_lamp_off_north_mid_down", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=up,facing=north,part=cap", "block/lights/fluorescent_lamp_off_north_cap_up", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=up,facing=north,part=cap", "block/lights/fluorescent_lamp_off_north_cap_up", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=up,facing=north,part=mid", "block/lights/fluorescent_lamp_off_north_mid_up", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=up,facing=north,part=mid", "block/lights/fluorescent_lamp_off_north_mid_up", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=north,facing=north,part=cap", "block/lights/fluorescent_lamp_off_north_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=north,facing=north,part=cap", "block/lights/fluorescent_lamp_off_north_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=north,facing=north,part=mid", "block/lights/fluorescent_lamp_off_north_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=north,facing=north,part=mid", "block/lights/fluorescent_lamp_off_north_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=south,facing=north,part=cap", "block/lights/fluorescent_lamp_off_north_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=south,facing=north,part=cap", "block/lights/fluorescent_lamp_off_north_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=south,facing=north,part=mid", "block/lights/fluorescent_lamp_off_north_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=south,facing=north,part=mid", "block/lights/fluorescent_lamp_off_north_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=west,facing=north,part=cap", "block/lights/fluorescent_lamp_off_north_cap_west", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=west,facing=north,part=cap", "block/lights/fluorescent_lamp_off_north_cap_west", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=west,facing=north,part=mid", "block/lights/fluorescent_lamp_off_north_mid_west", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=west,facing=north,part=mid", "block/lights/fluorescent_lamp_off_north_mid_west", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=east,facing=north,part=cap", "block/lights/fluorescent_lamp_off_north_cap_east", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=east,facing=north,part=cap", "block/lights/fluorescent_lamp_off_north_cap_east", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=east,facing=north,part=mid", "block/lights/fluorescent_lamp_off_north_mid_east", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=east,facing=north,part=mid", "block/lights/fluorescent_lamp_off_north_mid_east", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=down,facing=south,part=single", "block/lights/fluorescent_lamp_off_south_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=down,facing=south,part=single", "block/lights/fluorescent_lamp_off_south_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=up,facing=south,part=single", "block/lights/fluorescent_lamp_off_south_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=up,facing=south,part=single", "block/lights/fluorescent_lamp_off_south_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=north,facing=south,part=single", "block/lights/fluorescent_lamp_off_south_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=north,facing=south,part=single", "block/lights/fluorescent_lamp_off_south_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=south,facing=south,part=single", "block/lights/fluorescent_lamp_off_south_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=south,facing=south,part=single", "block/lights/fluorescent_lamp_off_south_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=west,facing=south,part=single", "block/lights/fluorescent_lamp_off_south_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=west,facing=south,part=single", "block/lights/fluorescent_lamp_off_south_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=east,facing=south,part=single", "block/lights/fluorescent_lamp_off_south_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=east,facing=south,part=single", "block/lights/fluorescent_lamp_off_south_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=down,facing=south,part=cap", "block/lights/fluorescent_lamp_off_south_cap_down", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=down,facing=south,part=cap", "block/lights/fluorescent_lamp_off_south_cap_down", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=down,facing=south,part=mid", "block/lights/fluorescent_lamp_off_south_mid_down", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=down,facing=south,part=mid", "block/lights/fluorescent_lamp_off_south_mid_down", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=up,facing=south,part=cap", "block/lights/fluorescent_lamp_off_south_cap_up", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=up,facing=south,part=cap", "block/lights/fluorescent_lamp_off_south_cap_up", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=up,facing=south,part=mid", "block/lights/fluorescent_lamp_off_south_mid_up", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=up,facing=south,part=mid", "block/lights/fluorescent_lamp_off_south_mid_up", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=north,facing=south,part=cap", "block/lights/fluorescent_lamp_off_south_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=north,facing=south,part=cap", "block/lights/fluorescent_lamp_off_south_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=north,facing=south,part=mid", "block/lights/fluorescent_lamp_off_south_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=north,facing=south,part=mid", "block/lights/fluorescent_lamp_off_south_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=south,facing=south,part=cap", "block/lights/fluorescent_lamp_off_south_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=south,facing=south,part=cap", "block/lights/fluorescent_lamp_off_south_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=south,facing=south,part=mid", "block/lights/fluorescent_lamp_off_south_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=south,facing=south,part=mid", "block/lights/fluorescent_lamp_off_south_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=west,facing=south,part=cap", "block/lights/fluorescent_lamp_off_south_cap_west", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=west,facing=south,part=cap", "block/lights/fluorescent_lamp_off_south_cap_west", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=west,facing=south,part=mid", "block/lights/fluorescent_lamp_off_south_mid_west", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=west,facing=south,part=mid", "block/lights/fluorescent_lamp_off_south_mid_west", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=east,facing=south,part=cap", "block/lights/fluorescent_lamp_off_south_cap_east", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=east,facing=south,part=cap", "block/lights/fluorescent_lamp_off_south_cap_east", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=east,facing=south,part=mid", "block/lights/fluorescent_lamp_off_south_mid_east", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=east,facing=south,part=mid", "block/lights/fluorescent_lamp_off_south_mid_east", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=down,facing=west,part=single", "block/lights/fluorescent_lamp_off_west_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=down,facing=west,part=single", "block/lights/fluorescent_lamp_off_west_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=up,facing=west,part=single", "block/lights/fluorescent_lamp_off_west_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=up,facing=west,part=single", "block/lights/fluorescent_lamp_off_west_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=north,facing=west,part=single", "block/lights/fluorescent_lamp_off_west_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=north,facing=west,part=single", "block/lights/fluorescent_lamp_off_west_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=south,facing=west,part=single", "block/lights/fluorescent_lamp_off_west_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=south,facing=west,part=single", "block/lights/fluorescent_lamp_off_west_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=west,facing=west,part=single", "block/lights/fluorescent_lamp_off_west_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=west,facing=west,part=single", "block/lights/fluorescent_lamp_off_west_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=east,facing=west,part=single", "block/lights/fluorescent_lamp_off_west_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=east,facing=west,part=single", "block/lights/fluorescent_lamp_off_west_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=down,facing=west,part=cap", "block/lights/fluorescent_lamp_off_west_cap_down", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=down,facing=west,part=cap", "block/lights/fluorescent_lamp_off_west_cap_down", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=down,facing=west,part=mid", "block/lights/fluorescent_lamp_off_west_mid_down", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=down,facing=west,part=mid", "block/lights/fluorescent_lamp_off_west_mid_down", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=up,facing=west,part=cap", "block/lights/fluorescent_lamp_off_west_cap_up", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=up,facing=west,part=cap", "block/lights/fluorescent_lamp_off_west_cap_up", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=up,facing=west,part=mid", "block/lights/fluorescent_lamp_off_west_mid_up", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=up,facing=west,part=mid", "block/lights/fluorescent_lamp_off_west_mid_up", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=north,facing=west,part=cap", "block/lights/fluorescent_lamp_off_west_cap_north", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=north,facing=west,part=cap", "block/lights/fluorescent_lamp_off_west_cap_north", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=north,facing=west,part=mid", "block/lights/fluorescent_lamp_off_west_mid_north", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=north,facing=west,part=mid", "block/lights/fluorescent_lamp_off_west_mid_north", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=south,facing=west,part=cap", "block/lights/fluorescent_lamp_off_west_cap_south", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=south,facing=west,part=cap", "block/lights/fluorescent_lamp_off_west_cap_south", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=south,facing=west,part=mid", "block/lights/fluorescent_lamp_off_west_mid_south", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=south,facing=west,part=mid", "block/lights/fluorescent_lamp_off_west_mid_south", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=west,facing=west,part=cap", "block/lights/fluorescent_lamp_off_west_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=west,facing=west,part=cap", "block/lights/fluorescent_lamp_off_west_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=west,facing=west,part=mid", "block/lights/fluorescent_lamp_off_west_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=west,facing=west,part=mid", "block/lights/fluorescent_lamp_off_west_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=east,facing=west,part=cap", "block/lights/fluorescent_lamp_off_west_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=east,facing=west,part=cap", "block/lights/fluorescent_lamp_off_west_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=east,facing=west,part=mid", "block/lights/fluorescent_lamp_off_west_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=east,facing=west,part=mid", "block/lights/fluorescent_lamp_off_west_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=down,facing=east,part=single", "block/lights/fluorescent_lamp_off_east_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=down,facing=east,part=single", "block/lights/fluorescent_lamp_off_east_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=up,facing=east,part=single", "block/lights/fluorescent_lamp_off_east_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=up,facing=east,part=single", "block/lights/fluorescent_lamp_off_east_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=north,facing=east,part=single", "block/lights/fluorescent_lamp_off_east_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=north,facing=east,part=single", "block/lights/fluorescent_lamp_off_east_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=south,facing=east,part=single", "block/lights/fluorescent_lamp_off_east_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=south,facing=east,part=single", "block/lights/fluorescent_lamp_off_east_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=west,facing=east,part=single", "block/lights/fluorescent_lamp_off_east_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=west,facing=east,part=single", "block/lights/fluorescent_lamp_off_east_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=east,facing=east,part=single", "block/lights/fluorescent_lamp_off_east_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=east,facing=east,part=single", "block/lights/fluorescent_lamp_off_east_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=down,facing=east,part=cap", "block/lights/fluorescent_lamp_off_east_cap_down", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=down,facing=east,part=cap", "block/lights/fluorescent_lamp_off_east_cap_down", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=down,facing=east,part=mid", "block/lights/fluorescent_lamp_off_east_mid_down", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=down,facing=east,part=mid", "block/lights/fluorescent_lamp_off_east_mid_down", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=up,facing=east,part=cap", "block/lights/fluorescent_lamp_off_east_cap_up", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=up,facing=east,part=cap", "block/lights/fluorescent_lamp_off_east_cap_up", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=up,facing=east,part=mid", "block/lights/fluorescent_lamp_off_east_mid_up", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=up,facing=east,part=mid", "block/lights/fluorescent_lamp_off_east_mid_up", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=north,facing=east,part=cap", "block/lights/fluorescent_lamp_off_east_cap_north", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=north,facing=east,part=cap", "block/lights/fluorescent_lamp_off_east_cap_north", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=north,facing=east,part=mid", "block/lights/fluorescent_lamp_off_east_mid_north", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=north,facing=east,part=mid", "block/lights/fluorescent_lamp_off_east_mid_north", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=south,facing=east,part=cap", "block/lights/fluorescent_lamp_off_east_cap_south", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=south,facing=east,part=cap", "block/lights/fluorescent_lamp_off_east_cap_south", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=south,facing=east,part=mid", "block/lights/fluorescent_lamp_off_east_mid_south", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=south,facing=east,part=mid", "block/lights/fluorescent_lamp_off_east_mid_south", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=west,facing=east,part=cap", "block/lights/fluorescent_lamp_off_east_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=west,facing=east,part=cap", "block/lights/fluorescent_lamp_off_east_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=west,facing=east,part=mid", "block/lights/fluorescent_lamp_off_east_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=west,facing=east,part=mid", "block/lights/fluorescent_lamp_off_east_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=east,facing=east,part=cap", "block/lights/fluorescent_lamp_off_east_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=east,facing=east,part=cap", "block/lights/fluorescent_lamp_off_east_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=false,conn=east,facing=east,part=mid", "block/lights/fluorescent_lamp_off_east_single", 0, 0);
        migVar("fluorescent_lamp_off", "broken=true,conn=east,facing=east,part=mid", "block/lights/fluorescent_lamp_off_east_single", 0, 0);
        // --- geysir_nether
        // --- machine_puf6_tank
        // --- machine_uf6_tank
        // --- mush
        // --- mush_block_stem
        // --- ore_volcano
        // --- pink_log
        // --- pink_planks
        // --- pipe_anchor
        // --- rail_wood
        // --- reinforced_laminate_pane (multipart)
        {
            mpOwner = migBlock("reinforced_laminate_pane");
            var mp = getMultipartBuilder(mpOwner);
            var p0 = mpPart(mp, "block/deco/laminate_pane_post", 0, 0);
            var p1 = mpPart(mp, "block/deco/laminate_pane_side", 0, 0);
            mpCond(p1, "north", "true");
            var p2 = mpPart(mp, "block/deco/laminate_pane_side", 0, 90);
            mpCond(p2, "east", "true");
            var p3 = mpPart(mp, "block/deco/laminate_pane_side_alt", 0, 0);
            mpCond(p3, "south", "true");
            var p4 = mpPart(mp, "block/deco/laminate_pane_side_alt", 0, 90);
            mpCond(p4, "west", "true");
            var p5 = mpPart(mp, "block/deco/laminate_pane_noside", 0, 0);
            mpCond(p5, "north", "false");
            var p6 = mpPart(mp, "block/deco/laminate_pane_noside_alt", 0, 0);
            mpCond(p6, "east", "false");
            var p7 = mpPart(mp, "block/deco/laminate_pane_noside_alt", 0, 90);
            mpCond(p7, "south", "false");
            var p8 = mpPart(mp, "block/deco/laminate_pane_noside", 0, 270);
            mpCond(p8, "west", "false");
        }
        // --- sandbags
        // --- solar_mirror
        // --- spikes
        // --- steel_beam
        migVar("sandbags", "north=false,south=false,east=false,west=false", "block/deco/sandbags_0000", 0, 0);
        migVar("sandbags", "north=false,south=false,east=false,west=true", "block/deco/sandbags_0001", 0, 0);
        migVar("sandbags", "north=false,south=false,east=true,west=false", "block/deco/sandbags_0010", 0, 0);
        migVar("sandbags", "north=false,south=false,east=true,west=true", "block/deco/sandbags_0011", 0, 0);
        migVar("sandbags", "north=false,south=true,east=false,west=false", "block/deco/sandbags_0100", 0, 0);
        migVar("sandbags", "north=false,south=true,east=false,west=true", "block/deco/sandbags_0101", 0, 0);
        migVar("sandbags", "north=false,south=true,east=true,west=false", "block/deco/sandbags_0110", 0, 0);
        migVar("sandbags", "north=false,south=true,east=true,west=true", "block/deco/sandbags_0111", 0, 0);
        migVar("sandbags", "north=true,south=false,east=false,west=false", "block/deco/sandbags_1000", 0, 0);
        migVar("sandbags", "north=true,south=false,east=false,west=true", "block/deco/sandbags_1001", 0, 0);
        migVar("sandbags", "north=true,south=false,east=true,west=false", "block/deco/sandbags_1010", 0, 0);
        migVar("sandbags", "north=true,south=false,east=true,west=true", "block/deco/sandbags_1011", 0, 0);
        migVar("sandbags", "north=true,south=true,east=false,west=false", "block/deco/sandbags_1100", 0, 0);
        migVar("sandbags", "north=true,south=true,east=false,west=true", "block/deco/sandbags_1101", 0, 0);
        migVar("sandbags", "north=true,south=true,east=true,west=false", "block/deco/sandbags_1110", 0, 0);
        migVar("sandbags", "north=true,south=true,east=true,west=true", "block/deco/sandbags_1111", 0, 0);
        // --- stone_porous
        // --- therm_endo
        // --- therm_exo
        // --- vine_phosphor
        // --- volcano_core
        // --- volcano_rad_core
        // --- wood_barrier
        // --- wood_structure_scaffold
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

    /** Рельсы (RailGenericBlock extends RailBlock): 10 SHAPE-вариантов, 1:1 с исходными blockstate-JSON. */
    private void railBlockstate(RegistrySupplier<? extends Block> block, String name) {
        ModelFile flat = models().getExistingFile(modLoc("block/" + name + "_flat"));
        ModelFile raisedNe = models().getExistingFile(modLoc("block/" + name + "_raised_ne"));
        ModelFile raisedSw = models().getExistingFile(modLoc("block/" + name + "_raised_sw"));
        ModelFile curved = models().getExistingFile(modLoc("block/" + name + "_curved"));
        var p = net.minecraft.world.level.block.state.properties.BlockStateProperties.RAIL_SHAPE;
        VariantBlockStateBuilder b = getVariantBuilder(block.get());
        b.partialState().with(p, net.minecraft.world.level.block.state.properties.RailShape.NORTH_SOUTH).modelForState().modelFile(flat).addModel();
        b.partialState().with(p, net.minecraft.world.level.block.state.properties.RailShape.EAST_WEST).modelForState().modelFile(flat).rotationY(90).addModel();
        b.partialState().with(p, net.minecraft.world.level.block.state.properties.RailShape.ASCENDING_EAST).modelForState().modelFile(raisedNe).rotationY(90).addModel();
        b.partialState().with(p, net.minecraft.world.level.block.state.properties.RailShape.ASCENDING_WEST).modelForState().modelFile(raisedSw).rotationY(90).addModel();
        b.partialState().with(p, net.minecraft.world.level.block.state.properties.RailShape.ASCENDING_NORTH).modelForState().modelFile(raisedNe).addModel();
        b.partialState().with(p, net.minecraft.world.level.block.state.properties.RailShape.ASCENDING_SOUTH).modelForState().modelFile(raisedSw).addModel();
        b.partialState().with(p, net.minecraft.world.level.block.state.properties.RailShape.SOUTH_EAST).modelForState().modelFile(curved).addModel();
        b.partialState().with(p, net.minecraft.world.level.block.state.properties.RailShape.SOUTH_WEST).modelForState().modelFile(curved).rotationY(90).addModel();
        b.partialState().with(p, net.minecraft.world.level.block.state.properties.RailShape.NORTH_WEST).modelForState().modelFile(curved).rotationY(180).addModel();
        b.partialState().with(p, net.minecraft.world.level.block.state.properties.RailShape.NORTH_EAST).modelForState().modelFile(curved).rotationY(270).addModel();
        simpleBlockItem(block.get(), flat);
    }

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

    /** Wie {@link #customMachineBlock}, aber mit eigener facing->y-Tabelle (Original-TESR mit abweichendem Drehschalter). */
    private <T extends Block> void customMachineBlockRotated(RegistrySupplier<T> blockObject, Map<Direction, Integer> rotY) {
        ModelFile model = models().getExistingFile(modLoc("block/machines/" + blockObject.getId().getPath()));
        VariantBlockStateBuilder builder = getVariantBuilder(blockObject.get());
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            builder.partialState().with(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, facing)
                    .modelForState().modelFile(model).rotationY(rotY.getOrDefault(facing, 0)).addModel();
        }
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

    /** 1:1 RenderLandmine: keine FACING-Drehung im Original; Weltmodell <model>_placed, Itemmodell <model>. */
    private <T extends Block> void placedMineBlock(RegistrySupplier<T> blockObject, String model, boolean withItem) {
        simpleBlock(blockObject.get(), models().getExistingFile(modLoc("block/bomb/" + model + "_placed")));
        if (withItem) simpleBlockItem(blockObject.get(), models().getExistingFile(modLoc("block/bomb/" + model)));
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
     * {@link com.hbm_m.client.model.ConfiguredMultipartBakedModel} через
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
        // Original fallout: setBlockTextureName("hbm:ash")
        ResourceLocation texture = modLoc("block/ash");
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

    /** Original getIcon(world,...): Variante aus Positionshash - hier die zufaellige (positionsfeste) Modellwahl. */
    private void registerSellafieldHot(RegistrySupplier<Block> blockObject, int level) {
        ConfiguredModel[] models = new ConfiguredModel[4];
        for (int v = 0; v < 4; v++) {
            String n = "sellafield_" + level + "_" + v;
            models[v] = new ConfiguredModel(models().cubeAll(n, modLoc("block/" + n)));
        }
        getVariantBuilder(blockObject.get()).partialState().setModels(models);
        simpleBlockItem(blockObject.get(), models().getExistingFile(modLoc("block/sellafield_" + level + "_0")));
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
     * Конденсаторы и шина (6-way FACING = BlockStateProperties.FACING): модель сшита с
     * "входной" текстурой на северной грани, шесть поворотов — по конвенции observer'а
     * (down: x=90, up: x=270, горизонтальные: y-повороты) — как у DFC-стабилизатора выше.
     * Для конденсаторов входная грань — inner_top ("розетка"), для шины — capacitor_bus_out
     * (оригинальный getIcon(side, meta)).
     */
    private void capacitorBlockWithFacing(RegistrySupplier<Block> blockObject, String inputTexture,
                                          String side, String top, String bottom) {
        Block block = blockObject.get();
        String name = blockObject.getId().getPath();
        ModelFile model = models().cube(name,
                modLoc("block/" + bottom), modLoc("block/" + top),
                modLoc("block/" + inputTexture),
                modLoc("block/" + side), modLoc("block/" + side), modLoc("block/" + side))
                .texture("particle", modLoc("block/" + side));
        getVariantBuilder(block).forAllStates(state -> {
            Direction facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
            int x = facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0;
            int y = facing.getAxis().isHorizontal() ? ((int) facing.toYRot() + 180) % 360 : 0;
            return ConfiguredModel.builder().modelFile(model).rotationX(x).rotationY(y).build();
        });
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
        // horizontalBlock вместо ручного partialState-цикла: ванильный хелпер сам раскладывает
        // все 4 ориентации и не конфликтует с уже сконфигурированными состояниями.
        horizontalBlock(blockObject.get(), model);
        simpleBlockItem(blockObject.get(), model);
    }

    /**
     * Maschine, die ganz vom Blockentity-Renderer gezeichnet wird (Original getRenderType -1): jede Ausrichtung zeigt
     * nur ein Partikelmodell, das Item den Original-Blockicon-Wuerfel.
     */
    private void rendererBlockWithCubeItem(RegistrySupplier<Block> blockObject, String texture) {
        String name = blockObject.getId().getPath();
        ModelFile particle = models().getBuilder(name + "_particle").texture("particle", modLoc(texture));
        VariantBlockStateBuilder builder = getVariantBuilder(blockObject.get());
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            builder.partialState().with(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, facing)
                    .modelForState().modelFile(particle).addModel();
        }
        simpleBlockItem(blockObject.get(), models().cubeAll(name, modLoc(texture)));
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
        // 1.7.10 OBJ models; the moving parts (fan, lever, rod) come from the renderers.
        horizontalBlock(ModBlocks.PILE_LOADER.get(), models().getExistingFile(modLoc("block/machines/pile_loader")));
        simpleBlockItem(ModBlocks.PILE_LOADER.get(), models().getExistingFile(modLoc("block/machines/pile_loader_item")));

        horizontalBlock(ModBlocks.PILE_VENT.get(), models().getExistingFile(modLoc("block/machines/pile_vent")));
        simpleBlockItem(ModBlocks.PILE_VENT.get(), models().getExistingFile(modLoc("block/machines/pile_vent_item")));

        horizontalBlock(ModBlocks.PILE_CONTROL.get(), models().getExistingFile(modLoc("block/machines/pile_control")));
        simpleBlockItem(ModBlocks.PILE_CONTROL.get(), models().getExistingFile(modLoc("block/machines/pile_control_item")));
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
