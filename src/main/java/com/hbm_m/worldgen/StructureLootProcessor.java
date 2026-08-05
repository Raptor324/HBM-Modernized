package com.hbm_m.worldgen;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.main.MainRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

/**
 * Процессор структур, назначающий лут-таблицы контейнерам при генерации.
 *
 * <p>Это современный аналог ручного наполнения инвентарей через
 * {@code Component.generateInvContents} + {@code WeightedRandomChestContent}
 * в оригинале 1.7.10. Вместо того чтобы заполнять слоты напрямую, процессор
 * проставляет тег {@code LootTable} в NBT блочного-сущности — ванильный сундук
 * обрабатывает его сам, а ящики HBM через
 * {@link com.hbm_m.blockentity.crates.BaseCrateBlockEntity#unpackLootTable}.</p>
 *
 * <p>Для разнообразия таблица выбирается по хешу позиции контейнера: разные
 * сундуки/ящики в одной структуре получают разные наборы лута.</p>
 */
public class StructureLootProcessor extends StructureProcessor {

    public static final Codec<StructureLootProcessor> CODEC = Codec.unit(StructureLootProcessor::new);

    /** Тематические авторские таблицы для ванильных сундуков. */
    private static final ResourceLocation[] CHEST_TABLES = new ResourceLocation[] {
            rl("chests/generic"), rl("chests/radio_station"), rl("chests/military_cache"),
            rl("chests/nuclear_lab"), rl("chests/vault_rusty"), rl("chests/tech_cache"),
            rl("chests/supply_drop"), rl("chests/bunker_supplies")
    };

    /** Варианты лута для железных ящиков (порт ItemPools* из 1.7.10). */
    private static final ResourceLocation[] IRON_TABLES = new ResourceLocation[] {
            rl("crates/iron_crate"), rl("crates/iron_crate_expensive"),
            rl("crates/iron_crate_nuke_fuel"), rl("crates/iron_crate_nuke_trash")
    };

    /** Варианты лута для стальных ящиков. */
    private static final ResourceLocation[] STEEL_TABLES = new ResourceLocation[] {
            rl("crates/steel_crate"), rl("crates/steel_crate_silo"),
            rl("crates/steel_crate_vault_lab"), rl("crates/steel_crate_vault_lockers"),
            rl("crates/steel_crate_office"), rl("crates/iron_crate")
    };

    private static final ResourceLocation DESH_TABLE = rl("crates/desh_crate");
    private static final ResourceLocation TUNGSTEN_TABLE = rl("crates/tungsten_crate");

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, path);
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModWorldGen.LOOT_PROCESSOR.get();
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level, BlockPos pos1, BlockPos pos2,
            StructureTemplate.StructureBlockInfo info1,
            StructureTemplate.StructureBlockInfo info2,
            StructurePlaceSettings settings) {
        CompoundTag nbt = info2.nbt();
        // Только контейнеры с block-entity NBT и без уже назначенной таблицы.
        if (nbt == null || nbt.contains("LootTable", 8)) {
            return info2;
        }
        ResourceLocation table = pickTable(info2.state(), info2.pos());
        if (table == null) {
            return info2;
        }
        CompoundTag copy = nbt.copy();
        copy.putString("LootTable", table.toString());
        copy.putLong("LootTableSeed", seedFor(info2.pos()));
        return new StructureTemplate.StructureBlockInfo(info2.pos(), info2.state(), copy);
    }

    @Nullable
    private static ResourceLocation pickTable(BlockState state, BlockPos pos) {
        Block block = state.getBlock();
        if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) {
            return CHEST_TABLES[indexFor(pos, CHEST_TABLES.length)];
        }
        if (block == ModBlocks.CRATE_IRON.get()) {
            return IRON_TABLES[indexFor(pos, IRON_TABLES.length)];
        }
        if (block == ModBlocks.CRATE_STEEL.get()) {
            return STEEL_TABLES[indexFor(pos, STEEL_TABLES.length)];
        }
        if (block == ModBlocks.CRATE_DESH.get()) {
            return DESH_TABLE;
        }
        if (block == ModBlocks.CRATE_TUNGSTEN.get()) {
            return TUNGSTEN_TABLE;
        }
        return null;
    }

    /** Детерминированный индекс из позиции контейнера — даёт разнообразие по структуре. */
    private static int indexFor(BlockPos pos, int size) {
        long h = BlockPos.asLong(pos.getX(), pos.getY(), pos.getZ());
        h ^= (h >>> 21);
        h *= 0x9E3779B97F4A7C15L;
        h ^= (h >>> 27);
        return (int) Math.floorMod(h, (long) size);
    }

    private static long seedFor(BlockPos pos) {
        return BlockPos.asLong(pos.getX(), pos.getY(), pos.getZ()) ^ 0x6D6F77206C6F6F74L;
    }
}
