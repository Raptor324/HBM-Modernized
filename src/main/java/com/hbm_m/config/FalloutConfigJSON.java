// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: 2026 mlbv <51232730+mlbv@users.noreply.github.com>
// SPDX-License-Identifier: LGPL-3.0-only
// Ported from the NTM Next project (MK5 crater generation system).
package com.hbm_m.config;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.generic.BlockSellafieldSlaked;
import com.hbm_m.main.MainRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Правила подмены блоков при fallout-stomp (порт {@code com.hbm.config.FalloutConfigJSON}).
 */
public final class FalloutConfigJSON {

    public static final List<FalloutEntry> entries = new ArrayList<>();
    public static final Random rand = new Random();

    private FalloutConfigJSON() {}

    public static void initialize() {
        entries.clear();
        initDefault();
        MainRegistry.LOGGER.info("[Fallout] Loaded {} default fallout substitution rules", entries.size());
    }

    public static void initDefault() {
        // Правила выровнены 1:1 с датапак-таблицей NEXT fallout_table/default.json
        // (порядок важен: побеждает первое совпавшее правило; руды идут ДО общих
        // slake-правил по инструментальным тегам).
        double woodEffectRange = 65D;

        entries.add(new FalloutEntry().mT(BlockTags.LOGS).prim(state(ModBlocks.WASTE_LOG.get()), 1).max(woodEffectRange).pre("axis"));
        entries.add(new FalloutEntry().mBS(Blocks.MUSHROOM_STEM.defaultBlockState()).prim(state(ModBlocks.WASTE_LOG.get()), 1).max(woodEffectRange));
        entries.add(new FalloutEntry().mBS(Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState()).prim(state(Blocks.AIR), 1).max(woodEffectRange));
        entries.add(new FalloutEntry().mBS(Blocks.RED_MUSHROOM_BLOCK.defaultBlockState()).prim(state(Blocks.AIR), 1).max(woodEffectRange));
        entries.add(new FalloutEntry().mBS(Blocks.SNOW.defaultBlockState()).prim(state(Blocks.AIR), 1).max(woodEffectRange));
        entries.add(new FalloutEntry().mT(BlockTags.PLANKS).prim(state(ModBlocks.WASTE_PLANKS.get()), 1).max(woodEffectRange));
        // Всё колунное (заборы, верстаки, двери построек) — в пыль, как в NEXT
        entries.add(new FalloutEntry().mT(BlockTags.MINEABLE_WITH_AXE).prim(state(Blocks.AIR), 1).max(woodEffectRange));
        entries.add(new FalloutEntry().mT(BlockTags.LEAVES).prim(state(Blocks.AIR), 1).max(woodEffectRange));
        entries.add(new FalloutEntry().mBS(state(ModBlocks.WASTE_LEAVES.get())).prim(state(Blocks.AIR), 1).max(woodEffectRange));
        entries.add(new FalloutEntry().mT(BlockTags.SAPLINGS).prim(state(Blocks.AIR), 1).max(woodEffectRange));
        entries.add(new FalloutEntry().mT(BlockTags.FLOWERS).prim(state(Blocks.AIR), 1).max(woodEffectRange));
        entries.add(new FalloutEntry().mT(BlockTags.BEE_GROWABLES).prim(state(Blocks.AIR), 1).max(woodEffectRange));
        entries.add(new FalloutEntry().mT(BlockTags.REPLACEABLE).prim(state(Blocks.AIR), 1).max(woodEffectRange));
        entries.add(new FalloutEntry().mT(BlockTags.LEAVES).prim(state(ModBlocks.WASTE_LEAVES.get()), 1).max(woodEffectRange + 35D));

        entries.add(new FalloutEntry().mBS(Blocks.MOSSY_COBBLESTONE.defaultBlockState()).prim(state(Blocks.COAL_ORE), 1));
        entries.add(new FalloutEntry().mBS(Blocks.CLAY.defaultBlockState()).prim(state(Blocks.TERRACOTTA), 1));

        // Кольца дистанций 5..50%: shade 9 у эпицентра → 0 к периферии. Slake бьёт
        // по ВСЕМУ mineable/pickaxe и mineable/shovel (постройки тоже запекаются),
        // руды — своими правилами выше в этом же кольце.
        for (int i = 1; i <= 10; i++) {
            int m = 10 - i;
            BlockState sellafite = sellafiteForLevel(m);
            BlockState bedrock = bedrockForLevel(m);

            entries.add(new FalloutEntry()
                    .prim(oreState(ModBlocks.ORE_SELLAFIELD_DIAMOND.get(), m), 3, oreState(ModBlocks.ORE_SELLAFIELD_EMERALD.get(), m), 2)
                    .c(0.5)
                    .max(i * 5)
                    .sol(true)
                    .mBS(Blocks.COAL_ORE.defaultBlockState()));

            entries.add(new FalloutEntry()
                    .prim(oreState(ModBlocks.ORE_SELLAFIELD_DIAMOND.get(), m), 1)
                    .c(0.2)
                    .max(i * 5)
                    .sol(true)
                    .mBS(ModBlocks.LIGNITE_ORE.get().defaultBlockState()));

            entries.add(new FalloutEntry()
                    .prim(oreState(ModBlocks.ORE_SELLAFIELD_EMERALD.get(), m), 1)
                    .max(i * 5)
                    .sol(true)
                    .mBS(ModBlocks.BERYLLIUM_ORE.get().defaultBlockState()));

            entries.add(new FalloutEntry()
                    .prim(oreState(ModBlocks.ORE_SELLAFIELD_SCHRABIDIUM.get(), m), 1,
                            oreState(ModBlocks.ORE_SELLAFIELD_URANIUM_SCORCHED.get(), m), 9)
                    .max(i * 5)
                    .sol(true)
                    .mBS(ModBlocks.URANIUM_ORE.get().defaultBlockState()));

            entries.add(new FalloutEntry()
                    .prim(oreState(ModBlocks.ORE_SELLAFIELD_RADGEM.get(), m), 1)
                    .max(i * 5)
                    .sol(true)
                    .mBS(Blocks.DIAMOND_ORE.defaultBlockState()));

            entries.add(new FalloutEntry()
                    .prim(bedrock, 1)
                    .max(i * 5)
                    .sol(true)
                    .mBS(Blocks.BEDROCK.defaultBlockState()));

            entries.add(new FalloutEntry()
                    .prim(bedrock, 1)
                    .max(i * 5)
                    .sol(true)
                    .mBS(ModBlocks.ORE_BEDROCK.get().defaultBlockState()));

            entries.add(new FalloutEntry()
                    .prim(bedrock, 1)
                    .max(i * 5)
                    .sol(true)
                    .mBS(ModBlocks.ORE_BEDROCK_OIL.get().defaultBlockState()));

            entries.add(new FalloutEntry()
                    .prim(bedrock, 1)
                    .max(i * 5)
                    .sol(true)
                    .mBS(ModBlocks.SELLAFIELD_BEDROCK.get().defaultBlockState()));

            entries.add(new FalloutEntry()
                    .prim(sellafite, 1)
                    .max(i * 5)
                    .sol(true)
                    .mT(BlockTags.MINEABLE_WITH_PICKAXE));

            entries.add(new FalloutEntry()
                    .prim(sellafite, 1)
                    .max(i * 5)
                    .sol(true)
                    .mT(BlockTags.MINEABLE_WITH_SHOVEL));
        }

        entries.add(new FalloutEntry().mBS(Blocks.MYCELIUM.defaultBlockState()).prim(state(ModBlocks.WASTE_MYCELIUM.get()), 1));
        entries.add(new FalloutEntry().mBS(Blocks.SAND.defaultBlockState()).prim(state(ModBlocks.WASTE_TRINITITE.get()), 1).c(0.05));
        entries.add(new FalloutEntry().mBS(Blocks.RED_SAND.defaultBlockState()).prim(state(ModBlocks.WASTE_TRINITITE_RED.get()), 1).c(0.05));
    }

    private static BlockState state(Block block) {
        return block.defaultBlockState();
    }

    private static BlockState oreState(Block block, int level) {
        return block.defaultBlockState().setValue(BlockSellafieldSlaked.COLOR_LEVEL, level);
    }

    private static BlockState bedrockForLevel(int m) {
        return ModBlocks.SELLAFIELD_BEDROCK.get().defaultBlockState().setValue(BlockSellafieldSlaked.COLOR_LEVEL, m);
    }

    /** m: 9 = эпицентр (горячий), 0 = периферия — как meta в 1.7.10. */
    private static BlockState sellafiteForLevel(int m) {
        Block block;
        if (m >= 8) block = ModBlocks.SELLAFIELD_SLAKED3.get();
        else if (m >= 5) block = ModBlocks.SELLAFIELD_SLAKED2.get();
        else if (m >= 2) block = ModBlocks.SELLAFIELD_SLAKED1.get();
        else block = ModBlocks.SELLAFIELD_SLAKED.get();
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(BlockSellafieldSlaked.COLOR_LEVEL)) {
            state = state.setValue(BlockSellafieldSlaked.COLOR_LEVEL, m);
        }
        return state;
    }

    private static int sellafiteTier(Block block, BlockState state) {
        if (block == ModBlocks.SELLAFIELD_BEDROCK.get()) {
            return state.getValue(BlockSellafieldSlaked.COLOR_LEVEL) + 10;
        }
        if (block instanceof BlockSellafieldSlaked) {
            return state.getValue(BlockSellafieldSlaked.COLOR_LEVEL);
        }
        if (block == ModBlocks.SELLAFIELD_SLAKED3.get()) return 3;
        if (block == ModBlocks.SELLAFIELD_SLAKED2.get()) return 2;
        if (block == ModBlocks.SELLAFIELD_SLAKED1.get()) return 1;
        if (block == ModBlocks.SELLAFIELD_SLAKED.get()) return 0;
        return -1;
    }

    private static int newSellafiteTier(BlockState conversion) {
        Block block = conversion.getBlock();
        if (block == ModBlocks.SELLAFIELD_BEDROCK.get()) {
            return conversion.getValue(BlockSellafieldSlaked.COLOR_LEVEL) + 10;
        }
        if (block instanceof BlockSellafieldSlaked) {
            return conversion.getValue(BlockSellafieldSlaked.COLOR_LEVEL);
        }
        return sellafiteTier(block, conversion);
    }

    public static final class FalloutEntry {
        /**
         * Быстрый писатель блоков (прямая запись в секцию чанка в обход Level.setBlock).
         * null = обычный level.setBlock с флагом 50.
         */
        @FunctionalInterface
        public interface BlockWriter {
            void write(Level level, BlockPos pos, BlockState state);
        }

        private BlockState matchesBlockState = null;
        private net.minecraft.tags.TagKey<Block> matchesTag = null;
        private boolean matchesOpaque = false;

        private WeightedState[] primaryBlocks = null;
        private WeightedState[] secondaryBlocks = null;
        private double primaryChance = 1.0D;
        private double minDist = 0.0D;
        private double maxDist = 100.0D;
        private double falloffStart = 0.9D;
        private boolean isSolid = false;
        /** Свойства результата, копируемые из исходного блока (напр. axis у брёвен). */
        private String[] preserveProps = new String[0];

        public FalloutEntry mBS(BlockState state) { this.matchesBlockState = state; return this; }
        public FalloutEntry mT(net.minecraft.tags.TagKey<Block> tag) { this.matchesTag = tag; return this; }
        public FalloutEntry mO(boolean opaque) { this.matchesOpaque = opaque; return this; }

        public FalloutEntry prim(BlockState state, int weight) {
            this.primaryBlocks = new WeightedState[] { new WeightedState(state, weight) };
            return this;
        }

        public FalloutEntry prim(BlockState a, int wa, BlockState b, int wb) {
            this.primaryBlocks = new WeightedState[] { new WeightedState(a, wa), new WeightedState(b, wb) };
            return this;
        }

        public FalloutEntry sec(BlockState state, int weight) {
            this.secondaryBlocks = new WeightedState[] { new WeightedState(state, weight) };
            return this;
        }

        public FalloutEntry c(double chance) { this.primaryChance = chance; return this; }
        public FalloutEntry min(double min) { this.minDist = min; return this; }
        public FalloutEntry max(double max) { this.maxDist = max; return this; }
        public FalloutEntry fo(double falloffStart) { this.falloffStart = falloffStart; return this; }
        public FalloutEntry sol(boolean solid) { this.isSolid = solid; return this; }
        public FalloutEntry pre(String... properties) { this.preserveProps = properties; return this; }

        public boolean eval(Level level, BlockPos pos, BlockState state, double dist) {
            return eval(level, pos, state, dist, null);
        }

        public boolean eval(Level level, BlockPos pos, BlockState state, double dist, BlockWriter writer) {
            if (dist > maxDist || dist < minDist) return false;
            // Upstream matched by block with meta -1 ("any state"), and every rule here is authored
            // with defaultBlockState(). Reference equality silently skipped snowy grass, snow layers
            // above one and every naturally generated mushroom block (their face properties differ).
            if (matchesBlockState != null && !state.is(matchesBlockState.getBlock())) return false;
            if (matchesTag != null && !state.is(matchesTag)) return false;
            if (matchesOpaque && !state.isSolidRender(level, pos)) return false;
            if (dist > maxDist * falloffStart
                    && Math.abs(level.random.nextGaussian()) < Math.pow((dist - maxDist * falloffStart) / (maxDist - maxDist * falloffStart), 2D) * 3D) {
                return false;
            }

            BlockState conversion = chooseRandomOutcome(
                    (primaryChance == 1D || rand.nextDouble() < primaryChance) ? primaryBlocks : secondaryBlocks);

            if (conversion != null) {
                Block block = state.getBlock();
                if (block == ModBlocks.SELLAFIELD_BEDROCK.get() && conversion.getBlock() != ModBlocks.SELLAFIELD_BEDROCK.get()) {
                    return false;
                }

                int currentTier = sellafiteTier(block, state);
                int newTier = newSellafiteTier(conversion);
                if (currentTier >= 0 && newTier >= 0 && newTier <= currentTier) return false;
                if (pos.getY() == level.getMinBuildHeight() && conversion.getBlock() != ModBlocks.SELLAFIELD_BEDROCK.get()) return false;
                if (conversion == state) return false;

                for (String name : preserveProps) {
                    net.minecraft.world.level.block.state.properties.Property<?> property =
                            block.getStateDefinition().getProperty(name);
                    if (property != null && conversion.hasProperty(property)) {
                        conversion = copyProperty(state, conversion, property);
                    }
                }

                if (writer != null) {
                    writer.write(level, pos, conversion);
                } else {
                    level.setBlock(pos, conversion, 50);
                }
                return true;
            }

            return false;
        }

        private static <T extends Comparable<T>> BlockState copyProperty(
                BlockState from, BlockState to, net.minecraft.world.level.block.state.properties.Property<T> property) {
            return to.setValue(property, from.getValue(property));
        }

        private static BlockState chooseRandomOutcome(WeightedState[] blocks) {
            if (blocks == null || blocks.length == 0) return null;

            int weight = 0;
            for (WeightedState choice : blocks) weight += choice.weight;

            int r = rand.nextInt(weight);
            for (WeightedState choice : blocks) {
                r -= choice.weight;
                if (r <= 0) return choice.state;
            }
            return blocks[0].state;
        }

        public boolean isSolid() {
            return isSolid;
        }
    }

    private record WeightedState(BlockState state, int weight) {}
}
