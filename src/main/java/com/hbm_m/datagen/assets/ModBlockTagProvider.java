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
        Set<Block> shovelBlocks = Set.of(
                ModBlocks.WASTE_GRASS.get(),
                ModBlocks.DEAD_DIRT.get(),
                ModBlocks.BURNED_GRASS.get(),
                ModBlocks.ORE_OIL_SAND.get() // CE: BlockFallingBase.setHarvestLevel("shovel", 0)
        );

        // Б. Блоки для ТОПОРА (Дерево, ящики, деревянные двери)
        Set<Block> axeBlocks = Set.of(
                ModBlocks.WASTE_LOG.get(),
                ModBlocks.WASTE_PLANKS.get(),
                ModBlocks.CRATE.get(),        // Деревянный ящик
                ModBlocks.CRATE_WEAPON.get(), // Оружейный ящик (обычно дерево)
                ModBlocks.DOOR_OFFICE.get(),  // Офисная дверь (обычно дерево)
                ModBlocks.WOOD_BURNER.get()   // В названии Wood, возможно логично рубить топором? (если нет - уберите)
        );

        // В. РУДЫ И РЕСУРСНЫЕ БЛОКИ — тиры взяты из HBM-CE (setHarvestLevel("pickaxe", N)):
        // тир 0 = без тега needs_* (любая кирка дает дроп, рука ломает без дропа);
        // тир 1 = needs_stone_tool; тир 2 = needs_iron_tool; тиры 3-4 = needs_diamond_tool
        // (порт не различает обсидиановый тир — CE 3 и 4 объединены в needs_diamond_tool).
        // Все рудные блоки перечислены явно; руда, не попавшая ни в один тир-набор, = тир 0.
        Set<Block> oreBlocks = Set.of(
                // --- Тир 0 (CE harvest 0 / без harvest level) ---
                ModBlocks.LIGNITE_ORE.get(), ModBlocks.LIGNITE_ORE_DEEPSLATE.get(),
                ModBlocks.GNEISS_LITHIUM_ORE.get(),
                ModBlocks.GNEISS_GAS_ORE.get(), ModBlocks.ORE_GNEISS_GAS.get(),
                ModBlocks.NETHER_SMOLDERING_ORE.get(), ModBlocks.ORE_NETHER_SMOLDERING.get(), // CE BlockSmolder — без harvest level
                ModBlocks.ORE_SELLAFIELD_DIAMOND.get(), ModBlocks.ORE_SELLAFIELD_EMERALD.get(),
                ModBlocks.ORE_SELLAFIELD_URANIUM_SCORCHED.get(), ModBlocks.ORE_SELLAFIELD_SCHRABIDIUM.get(),
                ModBlocks.ORE_SELLAFIELD_RADGEM.get(),
                ModBlocks.ORE_BASALT_SULFUR.get(), ModBlocks.ORE_BASALT_FLUORITE.get(),
                ModBlocks.ORE_BASALT_ASBESTOS.get(), ModBlocks.ORE_BASALT_GEM.get(),
                ModBlocks.ORE_BASALT_MOLYSITE.get(), // CE BlockOreBasalt (BlockEnumMeta) — без harvest level
                ModBlocks.RESOURCE_ASBESTOS.get(), ModBlocks.RESOURCE_BAUXITE.get(),
                ModBlocks.RESOURCE_HEMATITE.get(), ModBlocks.RESOURCE_LIMESTONE.get(),
                ModBlocks.RESOURCE_MALACHITE.get(), ModBlocks.RESOURCE_SULFUR.get(),
                ModBlocks.STONE_RESOURCE_ASBESTOS.get(), ModBlocks.STONE_RESOURCE_BAUXITE.get(),
                ModBlocks.STONE_RESOURCE_HEMATITE.get(), ModBlocks.STONE_RESOURCE_LIMESTONE.get(),
                ModBlocks.STONE_RESOURCE_MALACHITE.get(), ModBlocks.STONE_RESOURCE_SULFUR.get(), // CE BlockResourceStone — без harvest level
                // --- Тир 1 (CE harvest 1) ---
                ModBlocks.SULFUR_ORE.get(), ModBlocks.SULFUR_ORE_DEEPSLATE.get(),
                ModBlocks.NETHER_SULFUR_ORE.get(), ModBlocks.ORE_NETHER_SULFUR.get(),
                ModBlocks.NITER_ORE.get(), ModBlocks.NITER_ORE_DEEPSLATE.get(),
                ModBlocks.ALUMINUM_ORE.get(), ModBlocks.ALUMINUM_ORE_DEEPSLATE.get(),
                ModBlocks.ORE_ALUMINIUM.get(),
                ModBlocks.FLUORITE_ORE.get(), ModBlocks.FLUORITE_ORE_DEEPSLATE.get(),
                ModBlocks.ASBESTOS_ORE.get(), ModBlocks.ASBESTOS_ORE_DEEPSLATE.get(),
                ModBlocks.CINNABAR_ORE.get(), ModBlocks.CINNABAR_ORE_DEEPSLATE.get(),
                ModBlocks.ORE_CINNEBAR.get(),
                ModBlocks.ORE_OIL.get(),
                ModBlocks.URANIUM_ORE.get(), ModBlocks.URANIUM_ORE_DEEPSLATE.get(),
                ModBlocks.ORE_URANIUM_SCORCHED.get(),
                ModBlocks.GNEISS_URANIUM_ORE.get(), ModBlocks.ORE_GNEISS_URANIUM.get(),
                ModBlocks.ORE_GNEISS_URANIUM_SCORCHED.get(),
                ModBlocks.NETHER_URANIUM_ORE.get(), ModBlocks.ORE_NETHER_URANIUM.get(),
                ModBlocks.ORE_NETHER_URANIUM_SCORCHED.get(), // урановые руды CE — BlockOutgas (tier 1)
                ModBlocks.GNEISS_IRON_ORE.get(), ModBlocks.ORE_GNEISS_IRON.get(),
                ModBlocks.GNEISS_COPPER_ORE.get(), ModBlocks.ORE_GNEISS_COPPER.get(),
                ModBlocks.ORE_COPPER.get(),
                ModBlocks.NETHER_FIRE_ORE.get(), ModBlocks.ORE_NETHER_FIRE.get(),
                ModBlocks.NETHER_COAL_ORE.get(), ModBlocks.ORE_NETHER_COAL.get(), // CE BlockNetherCoal -> BlockOutgas (tier 1)
                ModBlocks.CLUSTER_IRON.get(), ModBlocks.CLUSTER_TITANIUM.get(),
                ModBlocks.CLUSTER_ALUMINIUM.get(), ModBlocks.CLUSTER_COPPER.get(), // CE BlockCluster extends BlockNTMOre(1)
                ModBlocks.BLOCK_METEOR_BROKEN.get(), // CE block_meteor_broken = tier 1
                // --- Тир 2 (CE harvest 2) ---
                ModBlocks.THORIUM_ORE.get(), ModBlocks.THORIUM_ORE_DEEPSLATE.get(),
                ModBlocks.TITANIUM_ORE.get(), ModBlocks.TITANIUM_ORE_DEEPSLATE.get(),
                ModBlocks.TUNGSTEN_ORE.get(), ModBlocks.TUNGSTEN_ORE_DEEPSLATE.get(),
                ModBlocks.NETHER_TUNGSTEN_ORE.get(), ModBlocks.ORE_NETHER_TUNGSTEN.get(),
                ModBlocks.LEAD_ORE.get(), ModBlocks.LEAD_ORE_DEEPSLATE.get(),
                ModBlocks.BERYLLIUM_ORE.get(), ModBlocks.BERYLLIUM_ORE_DEEPSLATE.get(),
                ModBlocks.RAREGROUND_ORE.get(), ModBlocks.RAREGROUND_ORE_DEEPSLATE.get(),
                ModBlocks.ORE_RARE.get(),
                ModBlocks.GNEISS_GOLD_ORE.get(), ModBlocks.ORE_GNEISS_GOLD.get(),
                ModBlocks.GNEISS_ASBESTOS_ORE.get(), ModBlocks.ORE_GNEISS_ASBESTOS.get(),
                ModBlocks.BLOCK_METEOR_COBBLE.get(), // CE block_meteor_cobble = tier 2
                // --- Тиры 3-4 (CE harvest 3/4 -> needs_diamond_tool) ---
                ModBlocks.SCHRABIDIUM_ORE.get(), ModBlocks.SCHRABIDIUM_ORE_NETHER.get(),
                ModBlocks.SCHRABIDIUM_ORE_GNEISS.get(),
                ModBlocks.ALEXANDRITE_ORE.get(), ModBlocks.ORE_ALEXANDRITE.get(), // CE: BlockDepthOre = tier 3
                ModBlocks.COBALT_ORE.get(), ModBlocks.COBALT_ORE_DEEPSLATE.get(),
                ModBlocks.COLTAN_ORE.get(), ModBlocks.COLTAN_ORE_DEEPSLATE.get(),
                ModBlocks.ORE_COLTAN.get(),
                ModBlocks.NETHER_COBALT_ORE.get(), ModBlocks.ORE_NETHER_COBALT.get(),
                ModBlocks.NETHER_PLUTONIUM_ORE.get(), ModBlocks.ORE_NETHER_PLUTONIUM.get(),
                ModBlocks.ORE_METEOR_IRON.get(), ModBlocks.ORE_METEOR_COPPER.get(),
                ModBlocks.ORE_METEOR_ALUMINIUM.get(), ModBlocks.ORE_METEOR_RAREEARTH.get(),
                ModBlocks.ORE_METEOR_COBALT.get(),
                ModBlocks.BLOCK_METEOR.get(), ModBlocks.BLOCK_METEOR_TREASURE.get(), // CE block_meteor/_treasure = tier 3
                ModBlocks.GNEISS_RARE_ORE.get(), ModBlocks.ORE_GNEISS_RARE.get(), // CE ore_gneiss_rare = tier 3
                ModBlocks.AUSTRALIUM_ORE.get(), ModBlocks.ORE_AUSTRALIUM.get(), // CE tier 4
                ModBlocks.TIKITE_ORE.get(), // CE tier 4
                ModBlocks.DEPTH_STONE.get(), ModBlocks.DEPTH_BORAX.get(), ModBlocks.ORE_DEPTH_BORAX.get(),
                ModBlocks.DEPTH_CINNABAR.get(), ModBlocks.ORE_DEPTH_CINNEBAR.get(),
                ModBlocks.DEPTH_ZIRCONIUM.get(), ModBlocks.ORE_DEPTH_ZIRCONIUM.get(),
                ModBlocks.CLUSTER_DEPTH_IRON.get(), ModBlocks.CLUSTER_DEPTH_TITANIUM.get(),
                ModBlocks.CLUSTER_DEPTH_TUNGSTEN.get(),
                ModBlocks.DEPTH_NETHER_NEODYMIUM.get(), ModBlocks.ORE_DEPTH_NETHER_NEODYMIUM.get() // CE BlockDepth = tier 3
        );

        Set<Block> stoneTierOres = Set.of(
                ModBlocks.SULFUR_ORE.get(), ModBlocks.SULFUR_ORE_DEEPSLATE.get(),
                ModBlocks.NETHER_SULFUR_ORE.get(), ModBlocks.ORE_NETHER_SULFUR.get(),
                ModBlocks.NITER_ORE.get(), ModBlocks.NITER_ORE_DEEPSLATE.get(),
                ModBlocks.ALUMINUM_ORE.get(), ModBlocks.ALUMINUM_ORE_DEEPSLATE.get(),
                ModBlocks.ORE_ALUMINIUM.get(),
                ModBlocks.FLUORITE_ORE.get(), ModBlocks.FLUORITE_ORE_DEEPSLATE.get(),
                ModBlocks.ASBESTOS_ORE.get(), ModBlocks.ASBESTOS_ORE_DEEPSLATE.get(),
                ModBlocks.CINNABAR_ORE.get(), ModBlocks.CINNABAR_ORE_DEEPSLATE.get(),
                ModBlocks.ORE_CINNEBAR.get(),
                ModBlocks.ORE_OIL.get(),
                ModBlocks.URANIUM_ORE.get(), ModBlocks.URANIUM_ORE_DEEPSLATE.get(),
                ModBlocks.ORE_URANIUM_SCORCHED.get(),
                ModBlocks.GNEISS_URANIUM_ORE.get(), ModBlocks.ORE_GNEISS_URANIUM.get(),
                ModBlocks.ORE_GNEISS_URANIUM_SCORCHED.get(),
                ModBlocks.NETHER_URANIUM_ORE.get(), ModBlocks.ORE_NETHER_URANIUM.get(),
                ModBlocks.ORE_NETHER_URANIUM_SCORCHED.get(),
                ModBlocks.GNEISS_IRON_ORE.get(), ModBlocks.ORE_GNEISS_IRON.get(),
                ModBlocks.GNEISS_COPPER_ORE.get(), ModBlocks.ORE_GNEISS_COPPER.get(),
                ModBlocks.ORE_COPPER.get(),
                ModBlocks.NETHER_FIRE_ORE.get(), ModBlocks.ORE_NETHER_FIRE.get(),
                ModBlocks.NETHER_COAL_ORE.get(), ModBlocks.ORE_NETHER_COAL.get(),
                ModBlocks.CLUSTER_IRON.get(), ModBlocks.CLUSTER_TITANIUM.get(),
                ModBlocks.CLUSTER_ALUMINIUM.get(), ModBlocks.CLUSTER_COPPER.get(),
                ModBlocks.BLOCK_METEOR_BROKEN.get()
        );

        Set<Block> ironTierOres = Set.of(
                ModBlocks.THORIUM_ORE.get(), ModBlocks.THORIUM_ORE_DEEPSLATE.get(),
                ModBlocks.TITANIUM_ORE.get(), ModBlocks.TITANIUM_ORE_DEEPSLATE.get(),
                ModBlocks.TUNGSTEN_ORE.get(), ModBlocks.TUNGSTEN_ORE_DEEPSLATE.get(),
                ModBlocks.NETHER_TUNGSTEN_ORE.get(), ModBlocks.ORE_NETHER_TUNGSTEN.get(),
                ModBlocks.LEAD_ORE.get(), ModBlocks.LEAD_ORE_DEEPSLATE.get(),
                ModBlocks.BERYLLIUM_ORE.get(), ModBlocks.BERYLLIUM_ORE_DEEPSLATE.get(),
                ModBlocks.RAREGROUND_ORE.get(), ModBlocks.RAREGROUND_ORE_DEEPSLATE.get(),
                ModBlocks.ORE_RARE.get(),
                ModBlocks.GNEISS_GOLD_ORE.get(), ModBlocks.ORE_GNEISS_GOLD.get(),
                ModBlocks.GNEISS_ASBESTOS_ORE.get(), ModBlocks.ORE_GNEISS_ASBESTOS.get(),
                ModBlocks.BLOCK_METEOR_COBBLE.get()
        );

        Set<Block> diamondTierOres = Set.of(
                ModBlocks.SCHRABIDIUM_ORE.get(), ModBlocks.SCHRABIDIUM_ORE_NETHER.get(),
                ModBlocks.SCHRABIDIUM_ORE_GNEISS.get(),
                ModBlocks.COBALT_ORE.get(), ModBlocks.COBALT_ORE_DEEPSLATE.get(),
                ModBlocks.COLTAN_ORE.get(), ModBlocks.COLTAN_ORE_DEEPSLATE.get(),
                ModBlocks.ORE_COLTAN.get(),
                ModBlocks.NETHER_COBALT_ORE.get(), ModBlocks.ORE_NETHER_COBALT.get(),
                ModBlocks.NETHER_PLUTONIUM_ORE.get(), ModBlocks.ORE_NETHER_PLUTONIUM.get(),
                ModBlocks.ORE_METEOR_IRON.get(), ModBlocks.ORE_METEOR_COPPER.get(),
                ModBlocks.ORE_METEOR_ALUMINIUM.get(), ModBlocks.ORE_METEOR_RAREEARTH.get(),
                ModBlocks.ORE_METEOR_COBALT.get(),
                ModBlocks.BLOCK_METEOR.get(), ModBlocks.BLOCK_METEOR_TREASURE.get(),
                ModBlocks.GNEISS_RARE_ORE.get(), ModBlocks.ORE_GNEISS_RARE.get(),
                ModBlocks.AUSTRALIUM_ORE.get(), ModBlocks.ORE_AUSTRALIUM.get(),
                ModBlocks.TIKITE_ORE.get(),
                ModBlocks.DEPTH_STONE.get(), ModBlocks.DEPTH_BORAX.get(), ModBlocks.ORE_DEPTH_BORAX.get(),
                ModBlocks.DEPTH_CINNABAR.get(), ModBlocks.ORE_DEPTH_CINNEBAR.get(),
                ModBlocks.DEPTH_ZIRCONIUM.get(), ModBlocks.ORE_DEPTH_ZIRCONIUM.get(),
                ModBlocks.CLUSTER_DEPTH_IRON.get(), ModBlocks.CLUSTER_DEPTH_TITANIUM.get(),
                ModBlocks.CLUSTER_DEPTH_TUNGSTEN.get(),
                ModBlocks.DEPTH_NETHER_NEODYMIUM.get(), ModBlocks.ORE_DEPTH_NETHER_NEODYMIUM.get()
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

            if (!isPlantOrSoft) {

                // --- ЛОГИКА РАСПРЕДЕЛЕНИЯ ---

                if (oreBlocks.contains(block)) {
                    // -> РУДА/РЕСУРС: кирка + тир из CE-таблицы (по умолчанию тир 0 — без needs-тега)
                    pickaxeTag.add(block);
                    if (stoneTierOres.contains(block)) {
                        stoneToolTag.add(block);
                    } else if (ironTierOres.contains(block)) {
                        ironToolTag.add(block);
                    } else if (diamondTierOres.contains(block)) {
                        diamondToolTag.add(block);
                    }

                } else if (shovelBlocks.contains(block)) {
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
                    // -> КИРКА (По умолчанию для всех машин и прочих блоков)
                    pickaxeTag.add(block);
                    ironToolTag.add(block); // Железная (по умолчанию)
                }
            }
        }




        // ============ ТЕГ ДЛЯ OCCLUSION CULLING ============
        // Блоки, через которые можно видеть (не блокируют рендеринг машин)
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


        // ============ ТЕГИ СОВМЕСТИМОСТИ С ДРУГИМИ МОДАМИ ============
        //  Storage blocks for every material with a BLOCK form. Only uranium and plutonium were
        //  listed by hand, so recipes for the other storage_blocks/* matched an empty tag.
                var storageBlocksTagBuilder = this.tag(BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks")));

        //  Iterate the registry itself instead of filtering ModMaterials by hand: INGOT_BLOCKS is
        //  filled exactly on mat.has(BLOCK), so it cannot drift from ModItemTagProvider, which
        //  copies the same tags.
        for (var entry : ModBlocks.INGOT_BLOCKS.entrySet()) {
            Block storageBlock = entry.getValue().get();
                        this.tag(BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/" + entry.getKey().getId())))
                    .add(storageBlock);

            storageBlocksTagBuilder.add(storageBlock);
        }


                this.tag(BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "ores/uranium")))
                .add(ModBlocks.URANIUM_ORE.get());

        // Все блоки хранения материалов → forge:storage_blocks/<имя MaterialType>
        // (или ModMaterials.getId(), если материал не плавится). Тег потребляется
        // mold_casting/block_*.json (выход блочной формы) — раньше был только для
        // uranium/plutonium, из-за чего ни один модовый блок не отливался.
        for (com.hbm_m.item.material.ModMaterials mat : com.hbm_m.item.material.ModMaterials.values()) {
            if (!mat.has(com.hbm_m.item.material.MaterialShape.BLOCK)) continue;
            if (!ModBlocks.hasIngotBlock(mat)) continue;
            com.hbm_m.inventory.material.MaterialType mt = com.hbm_m.inventory.material.MaterialType.of(mat);
            String tagName = mt != null ? mt.name : mat.getId();
            this.tag(BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/" + tagName)))
                    .add(ModBlocks.getIngotBlock(mat).get());
        }

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

    }
}
//?}