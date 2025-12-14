package com.hbm_m.datagen.assets;

// Провайдер генерации тегов блоков для мода.
// Здесь мы определяем, какими инструментами можно добывать наши блоки и руды,
// а также создаем теги для совместимости с другими модами (например, для систем хранения).
// Используется в классе DataGenerators для регистрации.

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {

    public ModBlockTagProvider(PackOutput output, CompletableFuture lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, RefStrings.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(@Nonnull HolderLookup.Provider provider) {
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
                ModBlocks.BURNED_GRASS.get()
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

        // В. Блоки для КИРКИ СЛАБОГО УРОВНЯ (Каменная кирка и выше)
        Set<Block> stoneTierPickaxeBlocks = Set.of(
                ModBlocks.ALUMINUM_ORE.get(),
                ModBlocks.ALUMINUM_ORE_DEEPSLATE.get(),
                ModBlocks.LIGNITE_ORE.get(),
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
        for (RegistryObject<Block> regObject : ModBlocks.BLOCKS.getEntries()) {
            Block block = regObject.get();

            // Фильтр: пропускаем растения, листья, паутину
            boolean isPlantOrSoft = block instanceof FlowerBlock
                    || block instanceof BushBlock
                    || block instanceof LeavesBlock
                    || block instanceof WebBlock;

            if (!isPlantOrSoft) {

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
                    if (stoneTierPickaxeBlocks.contains(block)) {
                        stoneToolTag.add(block); // Каменная
                    } else {
                        ironToolTag.add(block);  // Железная (по умолчанию)
                    }
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

        // ============ ТЕГИ СОВМЕСТИМОСТИ С ДРУГИМИ МОДАМИ ============
        this.tag(BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/uranium")))
                .add(ModBlocks.URANIUM_BLOCK.get());

        this.tag(BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/plutonium")))
                .add(ModBlocks.PLUTONIUM_BLOCK.get());

        this.tag(BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", "ores/uranium")))
                .add(ModBlocks.URANIUM_ORE.get());
    }
}