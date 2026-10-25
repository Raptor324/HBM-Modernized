package com.hbm_m.world.gen;

import com.hbm_m.saveddata.TomSaveData;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockColumnConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/**
 * 1.20-Nachbau von {@code ModEventHandlerImpact.modifyVillageGen} und {@code postImpactDecoration}.
 * Beide Original-Ereignisse gibt es nicht mehr:
 * <ul>
 *   <li>{@code BiomeEvent.GetVillageBlockID} -> {@link #VILLAGE} als Strukturprozessor, den
 *       {@code ImpactSinglePoolElementMixin} an alle Jigsaw-Teile aus {@code village/...} haengt.</li>
 *   <li>{@code DecorateBiomeEvent.Decorate} -> {@link #allowDecoration} ueber {@code ImpactConfiguredFeatureMixin}
 *       (nur bei der Weltgenerierung, also in einer {@link WorldGenRegion}). Die Typen TREE/BIG_SHROOM/GRASS/REED/
 *       FLOWERS/DEAD_BUSH/CACTUS/PUMPKIN/LILYPAD entsprechen in 1.20 TREE, HUGE_*_MUSHROOM, RANDOM_PATCH, FLOWER und
 *       BLOCK_COLUMN (Kaktus). Das Original wuerfelt das 1/9 je Chunk und Typ - hier ebenso, ueber Weltseed + Chunk.</li>
 * </ul>
 */
public final class ImpactWorldGen {

    private ImpactWorldGen() { }

    // ───────────────────────────── modifyVillageGen ─────────────────────────────

    public static final StructureProcessor VILLAGE = new StructureProcessor() {

        @Override
        public StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos, StructureBlockInfo original, StructureBlockInfo current, StructurePlaceSettings settings) {
            if (current == null) return null;

            TomSaveData data = TomSaveData.getLastCachedOrNull();
            if (data == null || !data.impact) return current;

            BlockState replacement = villageReplacement(current.state(), settings.getRandom(current.pos()));
            if (replacement == null) return current;
            return new StructureBlockInfo(current.pos(), replacement, null);
        }

        @Override
        protected StructureProcessorType<?> getType() {
            // wird nie serialisiert (nur zur Platzierung angehaengt)
            return StructureProcessorType.NOP;
        }
    };

    /** 1:1 {@code modifyVillageGen}; {@code null} = Block bleibt. */
    public static BlockState villageReplacement(BlockState state, RandomSource rand) {
        Block b = state.getBlock();

        if (isWood(state) || isGlass(state) || b == Blocks.LADDER || b instanceof CropBlock ||
                b == Blocks.CHEST || b instanceof DoorBlock || isCloth(state) || b == Blocks.WATER || b == Blocks.SMOOTH_STONE_SLAB) {
            return Blocks.AIR.defaultBlockState();

        } else if (b == Blocks.COBBLESTONE || b == Blocks.STONE_BRICKS || b == Blocks.MOSSY_STONE_BRICKS || b == Blocks.CRACKED_STONE_BRICKS || b == Blocks.CHISELED_STONE_BRICKS) {
            if (rand.nextInt(3) == 1) {
                return Blocks.GRAVEL.defaultBlockState();
            }
        } else if (b == Blocks.SANDSTONE || b == Blocks.CHISELED_SANDSTONE || b == Blocks.CUT_SANDSTONE) {
            if (rand.nextInt(3) == 1) {
                return Blocks.SAND.defaultBlockState();
            }
        } else if (b == Blocks.FARMLAND) {
            return Blocks.DIRT.defaultBlockState();
        }
        return null;
    }

    /** {@code Material.wood}: in 1.20 Holzbloecke = brennbar + Bass-Instrument. */
    private static boolean isWood(BlockState state) {
        return state.ignitedByLava() && state.instrument() == NoteBlockInstrument.BASS;
    }

    /** {@code Material.glass}: Glas, Scheiben, Glowstone. */
    private static boolean isGlass(BlockState state) {
        return state.is(BlockTags.IMPERMEABLE) || state.is(Blocks.GLASS_PANE) || state.getBlock() instanceof net.minecraft.world.level.block.StainedGlassPaneBlock || state.is(Blocks.GLOWSTONE);
    }

    /** {@code Material.cloth}: Wolle und Betten. */
    private static boolean isCloth(BlockState state) {
        return state.is(BlockTags.WOOL) || state.is(BlockTags.BEDS);
    }

    // ───────────────────────────── postImpactDecoration ─────────────────────────────

    /** @return false = Dekoration verweigert ({@code Result.DENY}). */
    public static boolean allowDecoration(WorldGenLevel level, Feature<?> feature, FeatureConfiguration config, BlockPos pos, RandomSource random) {
        if (!(level instanceof WorldGenRegion)) return true; // nur Weltgen, nicht Setzlinge/Knochenmehl
        if (!(feature == Feature.TREE || isBigShroom(feature) || feature == Feature.RANDOM_PATCH || feature == Feature.FLOWER
                || feature == Feature.NO_BONEMEAL_FLOWER || config instanceof BlockColumnConfiguration)) return true;

        TomSaveData data = TomSaveData.forWorld(level);
        if (data == null || !data.impact) return true;

        if (data.dust > 0 || data.fire > 0) {
            if (feature == Feature.TREE || isBigShroom(feature) || feature == Feature.RANDOM_PATCH || feature == Feature.FLOWER
                    || feature == Feature.NO_BONEMEAL_FLOWER || isCactus(config, random, pos)) {
                return false;
            }

        } else if (data.dust == 0 && data.fire == 0) {
            if (feature == Feature.TREE) return chunkRoll(level, pos, 1L);
            if (isBigShroom(feature)) return chunkRoll(level, pos, 2L);
            if (isCactus(config, random, pos)) return chunkRoll(level, pos, 3L);
            // GRASS/REED: Result.DEFAULT
        }
        return true;
    }

    private static boolean isBigShroom(Feature<?> feature) {
        return feature == Feature.HUGE_BROWN_MUSHROOM || feature == Feature.HUGE_RED_MUSHROOM;
    }

    private static boolean isCactus(FeatureConfiguration config, RandomSource random, BlockPos pos) {
        if (!(config instanceof BlockColumnConfiguration column)) return false;
        for (BlockColumnConfiguration.Layer layer : column.layers()) {
            // eigener Zufall, damit die Weltgen-Zufallsfolge unberuehrt bleibt
            if (layer.state().getState(RandomSource.create(0L), pos).is(Blocks.CACTUS)) return true;
        }
        return false;
    }

    /** Original {@code world.rand.nextInt(9) == 0} einmal je Chunk und Dekorationstyp. */
    private static boolean chunkRoll(WorldGenLevel level, BlockPos pos, long salt) {
        long cx = pos.getX() >> 4;
        long cz = pos.getZ() >> 4;
        java.util.Random rand = new java.util.Random(level.getSeed() ^ (cx * 341873128712L + cz * 132897987541L) ^ (salt * 0x5DEECE66DL));
        return rand.nextInt(9) == 0;
    }
}
