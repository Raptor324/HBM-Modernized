package com.hbm_m.datagen;

// Провайдер генерации тегов блоков для мода.
// Здесь мы определяем, какими инструментами можно добывать наши блоки и руды,
// а также создаем теги для совместимости с другими модами (например, для систем хранения).
// Используется в классе DataGenerators для регистрации.

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {

    public ModBlockTagProvider(PackOutput output, CompletableFuture lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, RefStrings.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(@Nonnull HolderLookup.Provider provider) {
        // ============ МИНЕРАЛЬНЫЙ ТАГ: ДОБЫЧА КИРКАМИ ============
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                // ===== РУДЫ И СЛИТКИ =====
                .add(ModBlocks.URANIUM_ORE.get())
                .add(ModBlocks.URANIUM_BLOCK.get())
                .add(ModBlocks.POLONIUM210_BLOCK.get())
                .add(ModBlocks.PLUTONIUM_BLOCK.get())
                .add(ModBlocks.PLUTONIUM_FUEL_BLOCK.get())
                .add(ModBlocks.MACHINE_BATTERY.get())
                .add(ModBlocks.BLAST_FURNACE.get())
                .add(ModBlocks.BLAST_FURNACE_EXTENSION.get())
                .add(ModBlocks.ALUMINUM_ORE.get())
                .add(ModBlocks.ALUMINUM_ORE_DEEPSLATE.get())
                .add(ModBlocks.URANIUM_ORE_H.get())
                .add(ModBlocks.URANIUM_ORE_DEEPSLATE.get())
                .add(ModBlocks.BERYLLIUM_ORE.get())
                .add(ModBlocks.BERYLLIUM_ORE_DEEPSLATE.get())
                .add(ModBlocks.CINNABAR_ORE.get())
                .add(ModBlocks.CINNABAR_ORE_DEEPSLATE.get())
                .add(ModBlocks.LIGNITE_ORE.get())
                .add(ModBlocks.ASBESTOS_ORE.get())
                .add(ModBlocks.LEAD_ORE.get())
                .add(ModBlocks.LEAD_ORE_DEEPSLATE.get())
                .add(ModBlocks.FLUORITE_ORE.get())
                .add(ModBlocks.RAREGROUND_ORE.get())
                .add(ModBlocks.RAREGROUND_ORE_DEEPSLATE.get())
                .add(ModBlocks.SULFUR_ORE.get())
                .add(ModBlocks.TITANIUM_ORE.get())
                .add(ModBlocks.TITANIUM_ORE_DEEPSLATE.get())
                .add(ModBlocks.THORIUM_ORE.get())
                .add(ModBlocks.THORIUM_ORE_DEEPSLATE.get())
                .add(ModBlocks.TUNGSTEN_ORE.get())
                .add(ModBlocks.COBALT_ORE.get())
                .add(ModBlocks.COBALT_ORE_DEEPSLATE.get())

                // ===== МАШИНЫ И СТАНКИ =====
                .add(ModBlocks.PRESS.get())
                .add(ModBlocks.WOOD_BURNER.get())
                .add(ModBlocks.ANVIL_BLOCK.get())
                .add(ModBlocks.ARMOR_TABLE.get())
                .add(ModBlocks.SHREDDER.get())
                .add(ModBlocks.MACHINE_ASSEMBLER.get())
                .add(ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get())
                .add(ModBlocks.GEIGER_COUNTER_BLOCK.get())
                .add(ModBlocks.WIRE_COATED.get())
                .add(ModBlocks.UNIVERSAL_MACHINE_PART.get())

                // ===== СТРУКТУРНЫЕ БЛОКИ =====
                .add(ModBlocks.REINFORCED_STONE.get())
                .add(ModBlocks.REINFORCED_STONE_STAIRS.get())
                .add(ModBlocks.REINFORCED_STONE_SLAB.get())
                .add(ModBlocks.REINFORCED_GLASS.get())

                // ===== БЕТОН =====
                .add(ModBlocks.CONCRETE.get())
                .add(ModBlocks.CONCRETE_STAIRS.get())
                .add(ModBlocks.CONCRETE_SLAB.get())
                .add(ModBlocks.CONCRETE_CRACKED.get())
                .add(ModBlocks.CONCRETE_CRACKED_STAIRS.get())
                .add(ModBlocks.CONCRETE_CRACKED_SLAB.get())
                .add(ModBlocks.CONCRETE_MOSSY.get())
                .add(ModBlocks.CONCRETE_MOSSY_STAIRS.get())
                .add(ModBlocks.CONCRETE_MOSSY_SLAB.get())
                .add(ModBlocks.CONCRETE_HAZARD.get())
                .add(ModBlocks.CONCRETE_HAZARD_STAIRS.get())
                .add(ModBlocks.CONCRETE_HAZARD_SLAB.get())
                .add(ModBlocks.CONCRETE_VENT.get())
                .add(ModBlocks.CONCRETE_FAN.get())
                .add(ModBlocks.CONCRETE_MARKED.get())

                // ===== КИРПИЧНЫЙ БЕТОН =====
                .add(ModBlocks.BRICK_CONCRETE.get())
                .add(ModBlocks.BRICK_CONCRETE_STAIRS.get())
                .add(ModBlocks.BRICK_CONCRETE_SLAB.get())
                .add(ModBlocks.BRICK_CONCRETE_BROKEN.get())
                .add(ModBlocks.BRICK_CONCRETE_BROKEN_STAIRS.get())
                .add(ModBlocks.BRICK_CONCRETE_BROKEN_SLAB.get())
                .add(ModBlocks.BRICK_CONCRETE_CRACKED.get())
                .add(ModBlocks.BRICK_CONCRETE_CRACKED_STAIRS.get())
                .add(ModBlocks.BRICK_CONCRETE_CRACKED_SLAB.get())
                .add(ModBlocks.BRICK_CONCRETE_MOSSY.get())
                .add(ModBlocks.BRICK_CONCRETE_MOSSY_STAIRS.get())
                .add(ModBlocks.BRICK_CONCRETE_MOSSY_SLAB.get())
                .add(ModBlocks.BRICK_CONCRETE_MARKED.get())

                // ===== ЯЩИКИ ХРАНИЛИЩА =====
                .add(ModBlocks.CRATE.get())
                .add(ModBlocks.CRATE_LEAD.get())
                .add(ModBlocks.CRATE_METAL.get())
                .add(ModBlocks.CRATE_WEAPON.get())
                .add(ModBlocks.CRATE_IRON.get())
                .add(ModBlocks.CRATE_STEEL.get())
                .add(ModBlocks.CRATE_DESH.get())

                // ===== БОЧКИ =====
                .add(ModBlocks.BARREL_LOX.get())
                .add(ModBlocks.BARREL_CORRODED.get())
                .add(ModBlocks.BARREL_IRON.get())
                .add(ModBlocks.BARREL_PINK.get())
                .add(ModBlocks.BARREL_PLASTIC.get())
                .add(ModBlocks.BARREL_RED.get())
                .add(ModBlocks.BARREL_STEEL.get())
                .add(ModBlocks.BARREL_TAINT.get())
                .add(ModBlocks.BARREL_TCALLOY.get())
                .add(ModBlocks.BARREL_VITRIFIED.get())
                .add(ModBlocks.BARREL_YELLOW.get())

                // ===== КОЛЮЧАЯ ПРОВОЛОКА =====
                .add(ModBlocks.BARBED_WIRE.get())
                .add(ModBlocks.BARBED_WIRE_FIRE.get())
                .add(ModBlocks.BARBED_WIRE_WITHER.get())
                .add(ModBlocks.BARBED_WIRE_POISON.get())
                .add(ModBlocks.BARBED_WIRE_RAD.get())

                // ===== ДВЕРИ =====
                .add(ModBlocks.DOOR_BUNKER.get())
                .add(ModBlocks.DOOR_OFFICE.get())
                .add(ModBlocks.METAL_DOOR.get())
                .add(ModBlocks.LARGE_VEHICLE_DOOR.get())
                .add(ModBlocks.ROUND_AIRLOCK_DOOR.get())
                .add(ModBlocks.TRANSITION_SEAL.get())
                .add(ModBlocks.FIRE_DOOR.get())
                .add(ModBlocks.SLIDE_DOOR.get())
                .add(ModBlocks.SLIDING_SEAL_DOOR.get())
                .add(ModBlocks.SECURE_ACCESS_DOOR.get())
                .add(ModBlocks.QE_SLIDING.get())
                .add(ModBlocks.QE_CONTAINMENT.get())
                .add(ModBlocks.WATER_DOOR.get())
                .add(ModBlocks.SILO_HATCH.get())
                .add(ModBlocks.SILO_HATCH_LARGE.get())

                // ===== ОТХОДЫ И ЗАГРЯЗНЕНИЕ =====
                .add(ModBlocks.WASTE_GRASS.get())
                .add(ModBlocks.WASTE_LEAVES.get())
                .add(ModBlocks.WASTE_PLANKS.get())
                .add(ModBlocks.WASTE_LOG.get())
                .add(ModBlocks.BURNED_GRASS.get())

                // ===== DEPTH БЛОКИ =====
                .add(ModBlocks.DEPTH_STONE.get())
                .add(ModBlocks.DEPTH_BORAX.get())
                .add(ModBlocks.DEPTH_CINNABAR.get())
                .add(ModBlocks.DEPTH_IRON.get())
                .add(ModBlocks.DEPTH_TUNGSTEN.get())
                .add(ModBlocks.DEPTH_TITANIUM.get())
                .add(ModBlocks.DEPTH_ZIRCONIUM.get())

                // ===== СЕЛЛАФИТ ЗАГРЯЗНЕНИЕ =====
                .add(ModBlocks.SELLAFIELD_SLAKED.get())
                .add(ModBlocks.SELLAFIELD_SLAKED1.get())
                .add(ModBlocks.SELLAFIELD_SLAKED2.get())
                .add(ModBlocks.SELLAFIELD_SLAKED3.get())

                // ===== ВЗРЫВЧАТЫЕ ВЕЩЕСТВА И ОПАСНЫЕ БЛОКИ =====
                .add(ModBlocks.DET_MINER.get())
                .add(ModBlocks.GIGA_DET.get())
                .add(ModBlocks.EXPLOSIVE_CHARGE.get())
                .add(ModBlocks.SMOKE_BOMB.get())
                .add(ModBlocks.NUCLEAR_CHARGE.get())
                .add(ModBlocks.WASTE_CHARGE.get())
                .add(ModBlocks.C4.get())

                // ===== ТЕХОБЪЕКТЫ =====
                .add(ModBlocks.DORNIER.get())
                .add(ModBlocks.ORE_OIL.get())
                .add(ModBlocks.BEDROCK_OIL.get())
                .add(ModBlocks.TOASTER.get())
                .add(ModBlocks.CRT_BSOD.get())
                .add(ModBlocks.CRT_CLEAN.get())
                .add(ModBlocks.CRT_BROKEN.get())

                // ===== ПРОЧИЕ БЛОКИ =====
                .add(ModBlocks.FREAKY_ALIEN_BLOCK.get());

        // ============ ТРЕБУЕМЫЙ УРОВЕНЬ: ЖЕЛЕЗНАЯ КИРКА ============
        this.tag(BlockTags.NEEDS_IRON_TOOL)
                // Руды и слитки требующие железа
                .add(ModBlocks.BERYLLIUM_ORE.get())
                .add(ModBlocks.BERYLLIUM_ORE_DEEPSLATE.get())
                .add(ModBlocks.URANIUM_ORE_H.get())
                .add(ModBlocks.URANIUM_ORE_DEEPSLATE.get())
                .add(ModBlocks.CINNABAR_ORE.get())
                .add(ModBlocks.CINNABAR_ORE_DEEPSLATE.get())
                .add(ModBlocks.LEAD_ORE.get())
                .add(ModBlocks.LEAD_ORE_DEEPSLATE.get())
                .add(ModBlocks.FLUORITE_ORE.get())
                .add(ModBlocks.RAREGROUND_ORE.get())
                .add(ModBlocks.RAREGROUND_ORE_DEEPSLATE.get())
                .add(ModBlocks.SULFUR_ORE.get())
                .add(ModBlocks.TITANIUM_ORE.get())
                .add(ModBlocks.TITANIUM_ORE_DEEPSLATE.get())
                .add(ModBlocks.THORIUM_ORE.get())
                .add(ModBlocks.THORIUM_ORE_DEEPSLATE.get())
                .add(ModBlocks.TUNGSTEN_ORE.get())
                .add(ModBlocks.COBALT_ORE.get())
                .add(ModBlocks.COBALT_ORE_DEEPSLATE.get())
                .add(ModBlocks.BLAST_FURNACE_EXTENSION.get())
                .add(ModBlocks.ASBESTOS_ORE.get())
                .add(ModBlocks.URANIUM_ORE.get())
                .add(ModBlocks.URANIUM_BLOCK.get())
                .add(ModBlocks.POLONIUM210_BLOCK.get())
                .add(ModBlocks.PLUTONIUM_BLOCK.get())
                .add(ModBlocks.PLUTONIUM_FUEL_BLOCK.get())
                .add(ModBlocks.MACHINE_BATTERY.get())
                .add(ModBlocks.BLAST_FURNACE.get())

                // Машины и станки
                .add(ModBlocks.PRESS.get())
                .add(ModBlocks.WOOD_BURNER.get())
                .add(ModBlocks.ANVIL_BLOCK.get())
                .add(ModBlocks.ARMOR_TABLE.get())
                .add(ModBlocks.SHREDDER.get())
                .add(ModBlocks.MACHINE_ASSEMBLER.get())
                .add(ModBlocks.ADVANCED_ASSEMBLY_MACHINE.get())
                .add(ModBlocks.GEIGER_COUNTER_BLOCK.get())
                .add(ModBlocks.WIRE_COATED.get())
                .add(ModBlocks.UNIVERSAL_MACHINE_PART.get())

                // Структурные блоки
                .add(ModBlocks.REINFORCED_STONE.get())
                .add(ModBlocks.REINFORCED_STONE_STAIRS.get())
                .add(ModBlocks.REINFORCED_STONE_SLAB.get())
                .add(ModBlocks.REINFORCED_GLASS.get())

                // Бетон
                .add(ModBlocks.CONCRETE.get())
                .add(ModBlocks.CONCRETE_STAIRS.get())
                .add(ModBlocks.CONCRETE_SLAB.get())
                .add(ModBlocks.CONCRETE_CRACKED.get())
                .add(ModBlocks.CONCRETE_CRACKED_STAIRS.get())
                .add(ModBlocks.CONCRETE_CRACKED_SLAB.get())
                .add(ModBlocks.CONCRETE_MOSSY.get())
                .add(ModBlocks.CONCRETE_MOSSY_STAIRS.get())
                .add(ModBlocks.CONCRETE_MOSSY_SLAB.get())
                .add(ModBlocks.CONCRETE_HAZARD.get())
                .add(ModBlocks.CONCRETE_HAZARD_STAIRS.get())
                .add(ModBlocks.CONCRETE_HAZARD_SLAB.get())
                .add(ModBlocks.CONCRETE_VENT.get())
                .add(ModBlocks.CONCRETE_FAN.get())
                .add(ModBlocks.CONCRETE_MARKED.get())

                // Кирпичный бетон
                .add(ModBlocks.BRICK_CONCRETE.get())
                .add(ModBlocks.BRICK_CONCRETE_STAIRS.get())
                .add(ModBlocks.BRICK_CONCRETE_SLAB.get())
                .add(ModBlocks.BRICK_CONCRETE_BROKEN.get())
                .add(ModBlocks.BRICK_CONCRETE_BROKEN_STAIRS.get())
                .add(ModBlocks.BRICK_CONCRETE_BROKEN_SLAB.get())
                .add(ModBlocks.BRICK_CONCRETE_CRACKED.get())
                .add(ModBlocks.BRICK_CONCRETE_CRACKED_STAIRS.get())
                .add(ModBlocks.BRICK_CONCRETE_CRACKED_SLAB.get())
                .add(ModBlocks.BRICK_CONCRETE_MOSSY.get())
                .add(ModBlocks.BRICK_CONCRETE_MOSSY_STAIRS.get())
                .add(ModBlocks.BRICK_CONCRETE_MOSSY_SLAB.get())
                .add(ModBlocks.BRICK_CONCRETE_MARKED.get())

                // Ящики и бочки
                .add(ModBlocks.CRATE.get())
                .add(ModBlocks.CRATE_LEAD.get())
                .add(ModBlocks.CRATE_METAL.get())
                .add(ModBlocks.CRATE_WEAPON.get())
                .add(ModBlocks.CRATE_IRON.get())
                .add(ModBlocks.CRATE_STEEL.get())
                .add(ModBlocks.CRATE_DESH.get())
                .add(ModBlocks.BARREL_LOX.get())
                .add(ModBlocks.BARREL_CORRODED.get())
                .add(ModBlocks.BARREL_IRON.get())
                .add(ModBlocks.BARREL_PINK.get())
                .add(ModBlocks.BARREL_PLASTIC.get())
                .add(ModBlocks.BARREL_RED.get())
                .add(ModBlocks.BARREL_STEEL.get())
                .add(ModBlocks.BARREL_TAINT.get())
                .add(ModBlocks.BARREL_TCALLOY.get())
                .add(ModBlocks.BARREL_VITRIFIED.get())
                .add(ModBlocks.BARREL_YELLOW.get())

                // Колючая проволока
                .add(ModBlocks.BARBED_WIRE.get())
                .add(ModBlocks.BARBED_WIRE_FIRE.get())
                .add(ModBlocks.BARBED_WIRE_WITHER.get())
                .add(ModBlocks.BARBED_WIRE_POISON.get())
                .add(ModBlocks.BARBED_WIRE_RAD.get())

                // Двери
                .add(ModBlocks.DOOR_BUNKER.get())
                .add(ModBlocks.DOOR_OFFICE.get())
                .add(ModBlocks.METAL_DOOR.get())
                .add(ModBlocks.LARGE_VEHICLE_DOOR.get())
                .add(ModBlocks.ROUND_AIRLOCK_DOOR.get())
                .add(ModBlocks.TRANSITION_SEAL.get())
                .add(ModBlocks.FIRE_DOOR.get())
                .add(ModBlocks.SLIDE_DOOR.get())
                .add(ModBlocks.SLIDING_SEAL_DOOR.get())
                .add(ModBlocks.SECURE_ACCESS_DOOR.get())
                .add(ModBlocks.QE_SLIDING.get())
                .add(ModBlocks.QE_CONTAINMENT.get())
                .add(ModBlocks.WATER_DOOR.get())
                .add(ModBlocks.SILO_HATCH.get())
                .add(ModBlocks.SILO_HATCH_LARGE.get())

                // Отходы и загрязнение
                .add(ModBlocks.WASTE_GRASS.get())
                .add(ModBlocks.WASTE_LEAVES.get())
                .add(ModBlocks.WASTE_PLANKS.get())
                .add(ModBlocks.WASTE_LOG.get())
                .add(ModBlocks.BURNED_GRASS.get())

                // Depth блоки
                .add(ModBlocks.DEPTH_STONE.get())
                .add(ModBlocks.DEPTH_BORAX.get())
                .add(ModBlocks.DEPTH_CINNABAR.get())
                .add(ModBlocks.DEPTH_IRON.get())
                .add(ModBlocks.DEPTH_TUNGSTEN.get())
                .add(ModBlocks.DEPTH_TITANIUM.get())
                .add(ModBlocks.DEPTH_ZIRCONIUM.get())

                // Селлафит загрязнение
                .add(ModBlocks.SELLAFIELD_SLAKED.get())
                .add(ModBlocks.SELLAFIELD_SLAKED1.get())
                .add(ModBlocks.SELLAFIELD_SLAKED2.get())
                .add(ModBlocks.SELLAFIELD_SLAKED3.get())

                // Взрывчатые вещества и опасные блоки
                .add(ModBlocks.DET_MINER.get())
                .add(ModBlocks.GIGA_DET.get())
                .add(ModBlocks.EXPLOSIVE_CHARGE.get())
                .add(ModBlocks.SMOKE_BOMB.get())
                .add(ModBlocks.NUCLEAR_CHARGE.get())
                .add(ModBlocks.WASTE_CHARGE.get())
                .add(ModBlocks.C4.get())

                // Техобъекты
                .add(ModBlocks.DORNIER.get())
                .add(ModBlocks.ORE_OIL.get())
                .add(ModBlocks.BEDROCK_OIL.get())
                .add(ModBlocks.TOASTER.get())
                .add(ModBlocks.CRT_BSOD.get())
                .add(ModBlocks.CRT_CLEAN.get())
                .add(ModBlocks.CRT_BROKEN.get())

                // Прочие
                .add(ModBlocks.FREAKY_ALIEN_BLOCK.get());

        // ============ ТРЕБУЕМЫЙ УРОВЕНЬ: КАМЕННАЯ КИРКА ============
        this.tag(BlockTags.NEEDS_STONE_TOOL)
                .add(ModBlocks.ALUMINUM_ORE_DEEPSLATE.get())
                .add(ModBlocks.LIGNITE_ORE.get())
                .add(ModBlocks.ALUMINUM_ORE.get());

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