package com.hbm_m.datagen.assets;
//? if forge {
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

// Провайдер генерации тегов блоков для мода.
// Здесь мы определяем, какими инструментами можно добывать наши блоки и руды,
// а также создаем теги для совместимости с другими модами (например, для систем хранения).
// Используется в классе DataGenerators для регистрации.

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.item.tags_and_tiers.ModTags;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModBlockTagProvider extends BlockTagsProvider {

    public ModBlockTagProvider(PackOutput output, CompletableFuture lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, RefStrings.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(@NotNull HolderLookup.Provider provider) {
        // ============ МИНЕРАЛЬНЫЙ ТАГ: ДОБЫЧА КИРКАМИ ============


        // --- 1. Инициализация тегов ---
        var pickaxeTag = this.tag(BlockTags.MINEABLE_WITH_PICKAXE);
        var shovelTag = this.tag(BlockTags.MINEABLE_WITH_SHOVEL);
        var axeTag = this.tag(BlockTags.MINEABLE_WITH_AXE); // Тег для топора

        var ironToolTag = this.tag(BlockTags.NEEDS_IRON_TOOL);
        var stoneToolTag = this.tag(BlockTags.NEEDS_STONE_TOOL);
        var diamondToolTag = this.tag(BlockTags.NEEDS_DIAMOND_TOOL);

        // --- 2. Списки исключений (Кто чем копается) ---

        // А. Блоки для ЛОПАТЫ (Земля, песок, сыпучее)
        Set<Block> shovelBlocks = new java.util.HashSet<>(Set.of(
                ModBlocks.WASTE_GRASS.get(),
                ModBlocks.DEAD_DIRT.get(),
                ModBlocks.BURNED_GRASS.get()
        ));
        // R6a: Erde/Sand/Schnee (Original Material.ground/sand/snow)
        for (var s : java.util.List.of(ModBlocks.BLOCK_SCRAP, ModBlocks.ORE_OIL_SAND, ModBlocks.DIRT_DEAD, ModBlocks.DIRT_OILY, ModBlocks.GRAVEL_DIAMOND, ModBlocks.MOON_TURF, ModBlocks.SAND_DIRTY, ModBlocks.SAND_DIRTY_RED, ModBlocks.FROZEN_DIRT, ModBlocks.BLOCK_RED_PHOSPHORUS, ModBlocks.BLOCK_YELLOWCAKE, ModBlocks.WASTE_EARTH, ModBlocks.FROZEN_GRASS, ModBlocks.BURNING_EARTH, ModBlocks.IMPACT_DIRT, ModBlocks.NTM_DIRT, ModBlocks.BARRICADE, ModBlocks.ASH_DIGAMMA, ModBlocks.BLOCK_FOAM, ModBlocks.FOAM_LAYER, ModBlocks.SAND_BORON_LAYER)) shovelBlocks.add(s.get());
        // R6a: im Original ohne Werkzeugstufe (Material.rock/iron = beliebige Spitzhacke)
        Set<Block> r6aNoTier = new java.util.HashSet<>();
        for (var s : java.util.List.of(ModBlocks.VENT_CHLORINE, ModBlocks.STALAGMITE_SULFUR, ModBlocks.STALAGMITE_ASBESTOS, ModBlocks.STALAGMITE_ICE, ModBlocks.STALAGMITE_SNOW, ModBlocks.STALAGMITE_GLYPHID1, ModBlocks.STALAGMITE_GLYPHID2, ModBlocks.STALAGMITE_GLYPHID3, ModBlocks.STALACTITE_SULFUR, ModBlocks.STALACTITE_ASBESTOS, ModBlocks.STALACTITE_ICE, ModBlocks.STALACTITE_SNOW, ModBlocks.STALACTITE_GLYPHID1, ModBlocks.STALACTITE_GLYPHID2, ModBlocks.STALACTITE_GLYPHID3, ModBlocks.STONE_RESOURCE_SULFUR, ModBlocks.STONE_RESOURCE_ASBESTOS, ModBlocks.STONE_RESOURCE_HEMATITE, ModBlocks.STONE_RESOURCE_MALACHITE, ModBlocks.STONE_RESOURCE_LIMESTONE, ModBlocks.STONE_RESOURCE_BAUXITE, ModBlocks.PWR_BLOCK, ModBlocks.CRANE_SPLITTER, ModBlocks.SEAL_CONTROLLER, ModBlocks.SEAL_FRAME, ModBlocks.BLAST_DOOR, ModBlocks.DUMMY_BLOCK_BLAST, ModBlocks.FRACTION_SPACER, ModBlocks.CRANE_PARTITIONER, ModBlocks.PIPE_ANCHOR, ModBlocks.FLUID_DUCT_BOX, ModBlocks.FLUID_DUCT_EXHAUST, ModBlocks.RED_CABLE_BOX, ModBlocks.FLUID_DUCT_PAINTABLE, ModBlocks.FLUID_DUCT_PAINTABLE_BLOCK_EXHAUST, ModBlocks.FLUID_VALVE, ModBlocks.FLUID_SWITCH, ModBlocks.FLUID_COUNTER_VALVE, ModBlocks.MACHINE_UF6_TANK, ModBlocks.MACHINE_PUF6_TANK, ModBlocks.CABLE_SWITCH, ModBlocks.CABLE_DETECTOR, ModBlocks.CABLE_DIODE, ModBlocks.RAIL_NARROW, ModBlocks.RAIL_HIGHSPEED, ModBlocks.RAIL_BOOSTER, ModBlocks.FIELD_DISTURBER, ModBlocks.TELEANCHOR, ModBlocks.CM_FLUX, ModBlocks.CM_HEAT, ModBlocks.HADRON_COIL_ALLOY, ModBlocks.HADRON_COIL_GOLD, ModBlocks.HADRON_COIL_NEODYMIUM, ModBlocks.HADRON_COIL_MAGTUNG, ModBlocks.HADRON_COIL_SCHRABIDIUM, ModBlocks.HADRON_COIL_SCHRABIDATE, ModBlocks.HADRON_COIL_STARMETAL, ModBlocks.HADRON_COIL_CHLOROPHYTE, ModBlocks.HADRON_COIL_MESE, ModBlocks.BLOCK_GRAPHITE_DRILLED, ModBlocks.BLOCK_GRAPHITE_FUEL, ModBlocks.BLOCK_GRAPHITE_PLUTONIUM, ModBlocks.BLOCK_GRAPHITE_SOURCE, ModBlocks.BLOCK_GRAPHITE_LITHIUM, ModBlocks.BLOCK_GRAPHITE_TRITIUM, ModBlocks.BLOCK_GRAPHITE_DETECTOR, ModBlocks.BLOCK_GRAPHITE_ROD, ModBlocks.GEYSIR_NETHER, ModBlocks.GEYSIR_CHLORINE, ModBlocks.CRATE_LEAD, ModBlocks.CRATE_METAL, ModBlocks.CRATE_RED, ModBlocks.CRATE_AMMO, ModBlocks.CRATE_JUNGLE, ModBlocks.SAFE, ModBlocks.BRICK_JUNGLE, ModBlocks.BRICK_JUNGLE_CRACKED, ModBlocks.BRICK_JUNGLE_LAVA, ModBlocks.BRICK_JUNGLE_CIRCLE, ModBlocks.BRICK_JUNGLE_FRAGILE, ModBlocks.BRICK_JUNGLE_GLYPH, ModBlocks.BRICK_JUNGLE_TRAP, ModBlocks.VENT_CLOUD, ModBlocks.VENT_PINK_CLOUD, ModBlocks.BLOCK_SCRAP, ModBlocks.BLOCK_ELECTRICAL_SCRAP, ModBlocks.CONCRETE_SUPER_BROKEN, ModBlocks.ORE_OIL_SAND, ModBlocks.DIRT_DEAD, ModBlocks.DIRT_OILY, ModBlocks.GRAVEL_DIAMOND, ModBlocks.GRAVEL_OBSIDIAN, ModBlocks.MOON_TURF, ModBlocks.SAND_DIRTY, ModBlocks.SAND_DIRTY_RED, ModBlocks.STONE_CRACKED, ModBlocks.ORE_OIL, ModBlocks.BLOCK_METEOR, ModBlocks.BLOCK_METEOR_COBBLE, ModBlocks.BLOCK_METEOR_BROKEN, ModBlocks.DECO_TITANIUM, ModBlocks.WASTE_PLANKS, ModBlocks.FROZEN_DIRT, ModBlocks.FROZEN_PLANKS, ModBlocks.ORE_RARE, ModBlocks.BRICK_JUNGLE_OOZE, ModBlocks.BRICK_JUNGLE_MYSTIC, ModBlocks.BLOCK_COPPER, ModBlocks.BLOCK_ALUMINIUM, ModBlocks.BLOCK_BAKELITE, ModBlocks.BLOCK_COLTAN, ModBlocks.BLOCK_FLUORITE, ModBlocks.BLOCK_MAGNETIZED_TUNGSTEN, ModBlocks.BLOCK_NITER, ModBlocks.BLOCK_POLYMER, ModBlocks.BLOCK_RUBBER, ModBlocks.BLOCK_SULFUR, ModBlocks.BLOCK_TANTALIUM, ModBlocks.METEOR_PILLAR, ModBlocks.CONCRETE_PILLAR, ModBlocks.BLOCK_EUPHEMIUM_CLUSTER, ModBlocks.BLOCK_FIBERGLASS, ModBlocks.BLOCK_INSULATOR, ModBlocks.BLOCK_TRITIUM, ModBlocks.GNEISS_CHISELED, ModBlocks.BASALT, ModBlocks.WATZ_COOLER, ModBlocks.WATZ_ELEMENT, ModBlocks.BLOCK_SMORE, ModBlocks.METEOR_BATTERY, ModBlocks.CLUSTER_IRON, ModBlocks.CLUSTER_TITANIUM, ModBlocks.CLUSTER_ALUMINIUM, ModBlocks.CLUSTER_COPPER, ModBlocks.CLUSTER_DEPTH_IRON, ModBlocks.CLUSTER_DEPTH_TITANIUM, ModBlocks.CLUSTER_DEPTH_TUNGSTEN, ModBlocks.DEPTH_DNT, ModBlocks.STONE_DEPTH, ModBlocks.STONE_DEPTH_NETHER, ModBlocks.BLOCK_CORIUM, ModBlocks.BLOCK_PU_MIX, ModBlocks.BLOCK_TRINITITE, ModBlocks.BLOCK_WHITE_PHOSPHORUS, ModBlocks.BLOCK_WASTE, ModBlocks.BLOCK_WASTE_VITRIFIED, ModBlocks.BLOCK_WASTE_PAINTED, ModBlocks.BLOCK_RED_PHOSPHORUS, ModBlocks.BLOCK_YELLOWCAKE, ModBlocks.BLOCK_CORIUM_COBBLE, ModBlocks.ORE_GNEISS_URANIUM_SCORCHED, ModBlocks.ORE_NETHER_URANIUM_SCORCHED, ModBlocks.ORE_URANIUM_SCORCHED, ModBlocks.DECO_ASBESTOS, ModBlocks.ASPHALT, ModBlocks.ASPHALT_LIGHT, ModBlocks.WASTE_LOG, ModBlocks.FROZEN_LOG, ModBlocks.BARBED_WIRE, ModBlocks.BARBED_WIRE_FIRE, ModBlocks.BARBED_WIRE_POISON, ModBlocks.BARBED_WIRE_ACID, ModBlocks.BARBED_WIRE_WITHER, ModBlocks.BARBED_WIRE_ULTRADEATH, ModBlocks.WASTE_EARTH, ModBlocks.FROZEN_GRASS, ModBlocks.BURNING_EARTH, ModBlocks.IMPACT_DIRT, ModBlocks.NTM_DIRT, ModBlocks.REINFORCED_LAMP_OFF, ModBlocks.REINFORCED_LAMP_ON, ModBlocks.BLOCK_LITHIUM, ModBlocks.BARRICADE, ModBlocks.CONCRETE_SUPER, ModBlocks.BRICK_CONCRETE_MARKED, ModBlocks.ASH_DIGAMMA, ModBlocks.GLASS_BORON, ModBlocks.GLASS_LEAD, ModBlocks.GLASS_URANIUM, ModBlocks.GLASS_TRINITITE, ModBlocks.GLASS_POLONIUM, ModBlocks.GLASS_ASH, ModBlocks.GLASS_QUARTZ, ModBlocks.GLASS_POLARIZED, ModBlocks.REINFORCED_LAMINATE, ModBlocks.STONE_BIOME_DESERT, ModBlocks.STONE_BIOME_WOODLAND, ModBlocks.BLOCK_COKE_COAL, ModBlocks.BLOCK_COKE_LIGNITE, ModBlocks.BLOCK_COKE_PETROLEUM, ModBlocks.BLOCK_CAP_NUKA, ModBlocks.BLOCK_CAP_QUANTUM, ModBlocks.BLOCK_CAP_SPARKLE, ModBlocks.BLOCK_CAP_RAD, ModBlocks.BLOCK_CAP_KORL, ModBlocks.BLOCK_CAP_FRITZ, ModBlocks.PLATEMETAL_BASE, ModBlocks.PLATEMETAL_BLACK, ModBlocks.PLATEMETAL_WHITE, ModBlocks.PLATEMETAL_RED, ModBlocks.PLATEMETAL_GREEN, ModBlocks.PLATEMETAL_LIGHT_GRAY, ModBlocks.PLATEMETAL_BLUE, ModBlocks.PLATEMETAL_PURPLE, ModBlocks.PLATEMETAL_CYAN, ModBlocks.PLATEMETAL_PINK, ModBlocks.PLATEMETAL_LIME, ModBlocks.PLATEMETAL_YELLOW, ModBlocks.PLATEMETAL_LIGHT_BLUE, ModBlocks.PLATEMETAL_MAGENTA, ModBlocks.PLATEMETAL_ORANGE, ModBlocks.CONCRETE_SMOOTH, ModBlocks.DUCRETE_SMOOTH, ModBlocks.CONCRETE_SMOOTH_STAIRS, ModBlocks.DUCRETE_SMOOTH_STAIRS, ModBlocks.BRICK_ASBESTOS_STAIRS, ModBlocks.LIGHTSTONE_TILE_STAIRS, ModBlocks.STONES_SLAB_TILE, ModBlocks.STONES_SLAB_BRICKS, ModBlocks.BLOCK_FOAM, ModBlocks.FOAM_LAYER, ModBlocks.SAND_BORON_LAYER)) r6aNoTier.add(s.get());
        // R6b: Deko aus Material.iron (beliebige Spitzhacke)
        for (var s : java.util.List.of(ModBlocks.TAPE_RECORDER, ModBlocks.STEEL_POLE, ModBlocks.ANTENNA_TOP, ModBlocks.POLE_SATELLITE_RECEIVER, ModBlocks.PUTER, ModBlocks.CRT_CLEAN, ModBlocks.CRT_BROKEN, ModBlocks.CRT_BLINKING, ModBlocks.CRT_BSOD, ModBlocks.TOASTER, ModBlocks.TOASTER_STEEL, ModBlocks.TOASTER_WOOD, ModBlocks.STEEL_CORNER, ModBlocks.STEEL_WALL, ModBlocks.STEEL_ROOF, ModBlocks.STEEL_BEAM, ModBlocks.STEEL_SCAFFOLD, ModBlocks.STEEL_SCAFFOLD_RED, ModBlocks.STEEL_SCAFFOLD_WHITE, ModBlocks.STEEL_SCAFFOLD_YELLOW, ModBlocks.DUNGEON_CHAIN, ModBlocks.DECO_EMITTER, ModBlocks.PART_EMITTER, ModBlocks.DET_CORD, ModBlocks.DET_CHARGE, ModBlocks.DET_NUKE, ModBlocks.DUD_BALEFIRE, ModBlocks.DUD_CONVENTIONAL, ModBlocks.DUD_NUKE, ModBlocks.DUD_SALTED, ModBlocks.FIREWORKS, ModBlocks.VOLCANO_CORE, ModBlocks.VOLCANO_RAD_CORE, ModBlocks.SPIKES, ModBlocks.TRAPDOOR_STEEL, ModBlocks.REINFORCED_LAMINATE_PANE, ModBlocks.STONE_POROUS, ModBlocks.LAMP_DEMON, ModBlocks.THERM_ENDO, ModBlocks.THERM_EXO, ModBlocks.FLOAT_BOMB, ModBlocks.EMP_BOMB, ModBlocks.FLAME_WAR, ModBlocks.SKELETON_HOLDER, ModBlocks.FILE_CABINET, ModBlocks.FILE_CABINET_STEEL, ModBlocks.VENDING_MACHINE, ModBlocks.VENDING_MACHINE_SNACKS, ModBlocks.STEEL_GRATE, ModBlocks.STEEL_GRATE_WIDE, ModBlocks.BOXCAR, ModBlocks.BOAT, ModBlocks.DECO_PIPE, ModBlocks.DECO_PIPE_RUSTED, ModBlocks.DECO_PIPE_GREEN, ModBlocks.DECO_PIPE_GREEN_RUSTED, ModBlocks.DECO_PIPE_RED, ModBlocks.DECO_PIPE_MARKED, ModBlocks.DECO_PIPE_RIM, ModBlocks.DECO_PIPE_RIM_RUSTED, ModBlocks.DECO_PIPE_RIM_GREEN, ModBlocks.DECO_PIPE_RIM_GREEN_RUSTED, ModBlocks.DECO_PIPE_RIM_RED, ModBlocks.DECO_PIPE_RIM_MARKED, ModBlocks.DECO_PIPE_FRAMED, ModBlocks.DECO_PIPE_FRAMED_RUSTED, ModBlocks.DECO_PIPE_FRAMED_GREEN, ModBlocks.DECO_PIPE_FRAMED_GREEN_RUSTED, ModBlocks.DECO_PIPE_FRAMED_RED, ModBlocks.DECO_PIPE_FRAMED_MARKED, ModBlocks.DECO_PIPE_QUAD, ModBlocks.DECO_PIPE_QUAD_RUSTED, ModBlocks.DECO_PIPE_QUAD_GREEN, ModBlocks.DECO_PIPE_QUAD_GREEN_RUSTED, ModBlocks.DECO_PIPE_QUAD_RED, ModBlocks.DECO_PIPE_QUAD_MARKED)) r6aNoTier.add(s.get());
        Set<Block> r6aNoTool = new java.util.HashSet<>();
        // R6d: Holz/Erde/Sand ohne Spitzhacke
        for (var s : java.util.List.of(ModBlocks.VINE_PHOSPHOR, ModBlocks.MUSH, ModBlocks.MUSH_BLOCK, ModBlocks.MUSH_BLOCK_STEM, ModBlocks.SANDBAGS, ModBlocks.BARRICADE, ModBlocks.PINK_LOG, ModBlocks.PINK_PLANKS, ModBlocks.WOOD_BARRIER, ModBlocks.WOOD_STRUCTURE, ModBlocks.WOOD_STRUCTURE_SCAFFOLD, ModBlocks.WOOD_STRUCTURE_CEILING)) r6aNoTool.add(s.get());
        for (var s : java.util.List.of(ModBlocks.DYNAMITE, ModBlocks.SEMTEX, ModBlocks.TNT_NTM, ModBlocks.FISSURE_BOMB, ModBlocks.BLOCK_C4, ModBlocks.BLOCK_SEMTEX, ModBlocks.CHARGE_DYNAMITE, ModBlocks.CHARGE_MINER, ModBlocks.CHARGE_C4, ModBlocks.CHARGE_SEMTEX)) r6aNoTool.add(s.get());
        for (var s : java.util.List.of(ModBlocks.PLANT_DEAD, ModBlocks.PLANT_DEAD_GRASS, ModBlocks.PLANT_DEAD_FLOWER, ModBlocks.PLANT_DEAD_BIGFLOWER, ModBlocks.PLANT_DEAD_FERN, ModBlocks.LEAVES_LAYER)) r6aNoTool.add(s.get());

        // Б. Блоки для ТОПОРА (Дерево, ящики, деревянные двери)
        Set<Block> axeBlocks = new java.util.HashSet<>(Set.of(
                ModBlocks.WASTE_LOG.get(),
                ModBlocks.WASTE_PLANKS.get(),
                ModBlocks.CRATE.get(),        // Деревянный ящик
                ModBlocks.CRATE_WEAPON.get(), // Оружейный ящик (обычно дерево)
                // door_office: im Original Material.iron (Spitzhacke), siehe unten
                ModBlocks.WOOD_BURNER.get()   // В названии Wood, возможно логично рубить топором? (если нет - уберите)
        ));
        for (var s : java.util.List.of(ModBlocks.WASTE_PLANKS, ModBlocks.FROZEN_PLANKS, ModBlocks.WASTE_LOG, ModBlocks.FROZEN_LOG)) axeBlocks.add(s.get());
        for (var s : java.util.List.of(ModBlocks.RAIL_WOOD)) axeBlocks.add(s.get());

        // В. Блоки для КИРКИ СЛАБОГО УРОВНЯ (Каменная кирка и выше)
        Set<Block> stoneTierPickaxeBlocks = Set.of(
                ModBlocks.ALUMINUM_ORE.get(),
                ModBlocks.ALUMINUM_ORE_DEEPSLATE.get(),
                ModBlocks.LIGNITE_ORE.get(),
                ModBlocks.LIGNITE_ORE_DEEPSLATE.get(),
                ModBlocks.NITER_ORE.get(),
                ModBlocks.NITER_ORE_DEEPSLATE.get(),
                ModBlocks.LITHIUM_ORE.get(),
                ModBlocks.LITHIUM_ORE_DEEPSLATE.get(),
                ModBlocks.COLTAN_ORE.get(),
                ModBlocks.COLTAN_ORE_DEEPSLATE.get(),
                ModBlocks.RESOURCE_BAUXITE.get(),
                ModBlocks.RESOURCE_LIMESTONE.get(),
                ModBlocks.RESOURCE_MALACHITE.get(),
                ModBlocks.RESOURCE_SULFUR.get()
        );

        // Г. Блоки, требующие МИНИМУМ КАМЕННЫЙ инструмент (Не важно, топор или кирка)
        // Если вы хотите, чтобы дерево ломалось только КАМЕННЫМ топором (не деревянным), добавьте его сюда.
        Set<Block> needsStoneToolGeneral = Set.of(
                // ModBlocks.HARD_WOOD_LOG.get() // Пример
        );

        // --- 3. Автоматический цикл ---
        for (RegistrySupplier<Block> regObject : ModBlocks.BLOCKS) {
            Block block = regObject.get();

            // Фильтр: пропускаем растения, листья, паутину
            boolean isPlantOrSoft = block instanceof FlowerBlock
                    || block instanceof BushBlock
                    || block instanceof LeavesBlock
                    || block instanceof WebBlock;

            if (!isPlantOrSoft && !r6aNoTool.contains(block)) {

                // --- ЛОГИКА РАСПРЕДЕЛЕНИЯ ---

                if (shovelBlocks.contains(block)) {
                    // -> ЛОПАТА
                    shovelTag.add(block);

                } else if (axeBlocks.contains(block)) {
                    // -> ТОПОР
                    axeTag.add(block);

                    // Если нужно запретить деревянный топор для этого блока:
                    if (needsStoneToolGeneral.contains(block)) {
                        stoneToolTag.add(block);
                    }

                } else {
                    // -> КИРКА (По умолчанию для всех машин и руд)
                    pickaxeTag.add(block);

                    // Определяем уровень кирки
                    if (r6aNoTier.contains(block)) {
                        // R6a: keine Stufe
                    } else if (stoneTierPickaxeBlocks.contains(block)) {
                        stoneToolTag.add(block); // Каменная
                    } else {
                        ironToolTag.add(block);  // Железная (по умолчанию)
                    }
                }
            }
        }




        // ============ ТЕГ ДЛЯ OCCLUSION CULLING ============
        // Блоки, через которые можно видеть (не блокируют рендеринг машин)
        //? if fabric && < 1.21.1 {
        /*this.tag(BlockTags.create(new ResourceLocation(RefStrings.MODID, "non_occluding")))
                .add(ModBlocks.UNIVERSAL_MACHINE_PART.get())
                .addTag(Tags.Blocks.GLASS)
                .addTag(Tags.Blocks.GLASS_PANES)
                .addTag(BlockTags.FENCES)
                .addTag(BlockTags.FENCE_GATES)
                .addTag(BlockTags.WALLS)
                .addTag(BlockTags.DOORS)
                .addTag(BlockTags.TRAPDOORS)
                .addTag(BlockTags.BUTTONS)
                .addTag(BlockTags.PRESSURE_PLATES)
                .addTag(BlockTags.RAILS)
                .addTag(BlockTags.STAIRS)
                .addTag(BlockTags.SLABS)
                .addTag(BlockTags.CORAL_PLANTS)
                .addTag(BlockTags.LEAVES)
                .addTag(BlockTags.SAPLINGS)
                .addTag(BlockTags.FLOWERS)
                .addTag(BlockTags.SIGNS)
                .addTag(BlockTags.BANNERS)
                .addTag(BlockTags.CANDLES)
                .addTag(BlockTags.CLIMBABLE)
                .add(Blocks.IRON_BARS)
                .add(Blocks.CHAIN)
                .add(Blocks.LANTERN)
                .add(Blocks.SOUL_LANTERN)
                .add(Blocks.TORCH)
                .add(Blocks.SOUL_TORCH)
                .add(Blocks.REDSTONE_TORCH)
                .add(Blocks.BREWING_STAND)
                .add(Blocks.ENCHANTING_TABLE)
                .add(Blocks.END_ROD)
                .add(Blocks.LIGHTNING_ROD)
                .add(Blocks.HOPPER)
                .add(Blocks.COBWEB)
                .add(Blocks.SCAFFOLDING)
                .add(Blocks.LEVER)
                .add(Blocks.TRIPWIRE)
                .add(Blocks.TRIPWIRE_HOOK)
                .add(Blocks.CAMPFIRE);
        *///?} else {
                this.tag(BlockTags.create(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "non_occluding")))
                .add(ModBlocks.UNIVERSAL_MACHINE_PART.get())
                .addTag(Tags.Blocks.GLASS)
                .addTag(Tags.Blocks.GLASS_PANES)
                .addTag(BlockTags.FENCES)
                .addTag(BlockTags.FENCE_GATES)
                .addTag(BlockTags.WALLS)
                .addTag(BlockTags.DOORS)
                .addTag(BlockTags.TRAPDOORS)
                .addTag(BlockTags.BUTTONS)
                .addTag(BlockTags.PRESSURE_PLATES)
                .addTag(BlockTags.RAILS)
                .addTag(BlockTags.STAIRS)
                .addTag(BlockTags.SLABS)
                .addTag(BlockTags.CORAL_PLANTS)
                .addTag(BlockTags.LEAVES)
                .addTag(BlockTags.SAPLINGS)
                .addTag(BlockTags.FLOWERS)
                .addTag(BlockTags.SIGNS)
                .addTag(BlockTags.BANNERS)
                .addTag(BlockTags.CANDLES)
                .addTag(BlockTags.CLIMBABLE)
                .add(Blocks.IRON_BARS)
                .add(Blocks.CHAIN)
                .add(Blocks.LANTERN)
                .add(Blocks.SOUL_LANTERN)
                .add(Blocks.TORCH)
                .add(Blocks.SOUL_TORCH)
                .add(Blocks.REDSTONE_TORCH)
                .add(Blocks.BREWING_STAND)
                .add(Blocks.ENCHANTING_TABLE)
                .add(Blocks.END_ROD)
                .add(Blocks.LIGHTNING_ROD)
                .add(Blocks.HOPPER)
                .add(Blocks.COBWEB)
                .add(Blocks.SCAFFOLDING)
                .add(Blocks.LEVER)
                .add(Blocks.TRIPWIRE)
                .add(Blocks.TRIPWIRE_HOOK)
                .add(Blocks.CAMPFIRE);

        // Соединяемые при генерации структур блоки (фикс стыков решёток/паней после спавна)
        this.tag(BlockTags.create(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "structure_connectables")))
                .add(Blocks.IRON_BARS)
                .add(Blocks.GLASS_PANE)
                                .add(Blocks.WHITE_STAINED_GLASS_PANE)
                .add(Blocks.ORANGE_STAINED_GLASS_PANE)
                .add(Blocks.MAGENTA_STAINED_GLASS_PANE)
                .add(Blocks.LIGHT_BLUE_STAINED_GLASS_PANE)
                .add(Blocks.YELLOW_STAINED_GLASS_PANE)
                .add(Blocks.LIME_STAINED_GLASS_PANE)
                .add(Blocks.PINK_STAINED_GLASS_PANE)
                .add(Blocks.GRAY_STAINED_GLASS_PANE)
                .add(Blocks.LIGHT_GRAY_STAINED_GLASS_PANE)
                .add(Blocks.CYAN_STAINED_GLASS_PANE)
                .add(Blocks.PURPLE_STAINED_GLASS_PANE)
                .add(Blocks.BLUE_STAINED_GLASS_PANE)
                .add(Blocks.BROWN_STAINED_GLASS_PANE)
                .add(Blocks.GREEN_STAINED_GLASS_PANE)
                .add(Blocks.RED_STAINED_GLASS_PANE)
                .add(Blocks.BLACK_STAINED_GLASS_PANE)
                .add(ModBlocks.REINFORCED_GLASS_PANE.get());
        //?}


        // ============ ТЕГИ СОВМЕСТИМОСТИ С ДРУГИМИ МОДАМИ ============
        //? if fabric && < 1.21.1 {
        /*this.tag(BlockTags.create(new ResourceLocation("forge", "storage_blocks/uranium")))
                .add(ModBlocks.URANIUM_BLOCK.get());
        *///?} else {
                this.tag(BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/uranium")))
                .add(ModBlocks.URANIUM_BLOCK.get());
        //?}


        //? if fabric && < 1.21.1 {
        /*this.tag(BlockTags.create(new ResourceLocation("forge", "storage_blocks/plutonium")))
                .add(ModBlocks.PLUTONIUM_BLOCK.get());
        *///?} else {
                this.tag(BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/plutonium")))
                .add(ModBlocks.PLUTONIUM_BLOCK.get());
        //?}


        //? if fabric && < 1.21.1 {
        /*this.tag(BlockTags.create(new ResourceLocation("forge", "ores/uranium")))
                .add(ModBlocks.URANIUM_ORE.get());
        *///?} else {
                this.tag(BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "ores/uranium")))
                .add(ModBlocks.URANIUM_ORE.get());
        //?}

        // ============ CONNECTED TEXTURES (CT) ============
        // Только сталь ↔ ржавая сталь как «одна семья»; остальные деко-CT — только с тем же блоком (см. ConnectedDecoBlockBakedModel).
        this.tag(ModTags.Blocks.DECO_STEEL_CONNECTABLE)
                .add(ModBlocks.DECO_STEEL.get())
                .add(ModBlocks.DECO_RUSTY_STEEL.get());

        // ============ BEACON BASE BLOCKS ============
        // Порт 1.7.10 BlockHazard.makeBeaconable(): все блоки, помеченные makeBeaconable(),
        // добавляются в ванильный тег beacon_base_blocks (в 1.20.1 это замена Forge isBeaconBase).
        var beaconBaseTag = this.tag(BlockTags.BEACON_BASE_BLOCKS);
        for (RegistrySupplier<Block> regObject : ModBlocks.BLOCKS) {
            if (regObject.get() instanceof com.hbm_m.block.generic.BlockHazard hazard && hazard.isBeaconable()) {
                beaconBaseTag.add(regObject.get());
            }
        }
        // R6a: BlockBeaconable / BlockLithium / BlockHazardFalling.makeBeaconable()
        beaconBaseTag.add(ModBlocks.BLOCK_COPPER.get());
        beaconBaseTag.add(ModBlocks.BLOCK_ALUMINIUM.get());
        beaconBaseTag.add(ModBlocks.BLOCK_BAKELITE.get());
        beaconBaseTag.add(ModBlocks.BLOCK_COLTAN.get());
        beaconBaseTag.add(ModBlocks.BLOCK_FLUORITE.get());
        beaconBaseTag.add(ModBlocks.BLOCK_MAGNETIZED_TUNGSTEN.get());
        beaconBaseTag.add(ModBlocks.BLOCK_NITER.get());
        beaconBaseTag.add(ModBlocks.BLOCK_POLYMER.get());
        beaconBaseTag.add(ModBlocks.BLOCK_RUBBER.get());
        beaconBaseTag.add(ModBlocks.BLOCK_SULFUR.get());
        beaconBaseTag.add(ModBlocks.BLOCK_TANTALIUM.get());
        beaconBaseTag.add(ModBlocks.BLOCK_PU_MIX.get());
        beaconBaseTag.add(ModBlocks.BLOCK_TRINITITE.get());
        beaconBaseTag.add(ModBlocks.BLOCK_WHITE_PHOSPHORUS.get());
        beaconBaseTag.add(ModBlocks.BLOCK_WASTE.get());
        beaconBaseTag.add(ModBlocks.BLOCK_WASTE_VITRIFIED.get());
        beaconBaseTag.add(ModBlocks.BLOCK_WASTE_PAINTED.get());
        beaconBaseTag.add(ModBlocks.BLOCK_RED_PHOSPHORUS.get());
        beaconBaseTag.add(ModBlocks.BLOCK_YELLOWCAKE.get());
        beaconBaseTag.add(ModBlocks.BLOCK_LITHIUM.get());

    }
}
//?}