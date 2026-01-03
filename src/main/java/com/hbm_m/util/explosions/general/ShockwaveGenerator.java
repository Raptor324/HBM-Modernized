package com.hbm_m.util.explosions.general;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ShockwaveGenerator {

    private static final int ZONE_3_RADIUS = 25;        // Зона уничтожения деревьев
    private static final int ZONE_4_RADIUS = 35;        // Зона частичного урона
    private static final int DAMAGE_ZONE_HEIGHT = 20;   // Высота обработки по Y

    /**
     * Генерация воронки кратера с применением защиты блоков
     */
    public static void generateCrater(ServerLevel level, BlockPos centerPos, int craterRadius, int craterDepth,
                                      Block wasteLogBlock, Block wastePlanksBlock, Block burnedGrass) {

        int centerX = centerPos.getX();
        int centerY = centerPos.getY();
        int centerZ = centerPos.getZ();

        RandomSource random = level.random;

        // Сфера разрушения с естественным профилем
        for (int x = centerX - craterRadius; x <= centerX + craterRadius; x++) {
            for (int y = centerY - craterRadius; y <= centerY + craterRadius; y++) {
                for (int z = centerZ - craterRadius; z <= centerZ + craterRadius; z++) {
                    double dx = x - centerX;
                    double dy = y - centerY;
                    double dz = z - centerZ;
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

                    if (dist > craterRadius) continue;

                    double norm = dist / craterRadius;
                    double smoothDepth = craterDepth * Math.cos(norm * Math.PI / 2);
                    double noise = (random.nextDouble() - 0.5) * 2.0;

                    if (dist <= smoothDepth + noise) {
                        BlockPos checkPos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(checkPos);


                        if (!state.isAir()) {
                            level.setBlock(checkPos, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        // Зона повреждения деревьев и окружающих блоков
        applyDamageZones(level, centerPos, wasteLogBlock, wastePlanksBlock, burnedGrass);
    }

    private static void applyDamageZones(ServerLevel level, BlockPos centerPos,
                                         Block wasteLogBlock, Block wastePlanksBlock, Block burnedGrass) {

        int centerX = centerPos.getX();
        int centerY = centerPos.getY();
        int centerZ = centerPos.getZ();

        int searchRadius = ZONE_4_RADIUS;
        int topSearchHeight = DAMAGE_ZONE_HEIGHT + 40;
        int bottomSearchHeight = 20;

        RandomSource random = level.random;

        for (int x = centerX - searchRadius; x <= centerX + searchRadius; x++) {
            for (int z = centerZ - searchRadius; z <= centerZ + searchRadius; z++) {
                for (int y = centerY - bottomSearchHeight; y <= centerY + topSearchHeight; y++) {
                    BlockPos checkPos = new BlockPos(x, y, z);

                    double dx = x - centerX;
                    double dz = z - centerZ;
                    double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

                    if (horizontalDistance > ZONE_4_RADIUS) continue;

                    BlockState state = level.getBlockState(checkPos);


                    if (horizontalDistance <= ZONE_3_RADIUS) {
                        if (state.is(BlockTags.LEAVES)) {
                            level.setBlock(checkPos, Blocks.FIRE.defaultBlockState(), 3);
                            continue;
                        }
                        if (state.is(BlockTags.LOGS)) {
                            level.setBlock(checkPos, wasteLogBlock.defaultBlockState(), 3);
                            continue;
                        }
                        if (state.is(BlockTags.PLANKS)) {
                            level.setBlock(checkPos, wastePlanksBlock.defaultBlockState(), 3);
                            continue;
                        }
                        if (state.is(Blocks.GRASS_BLOCK)) {
                            level.setBlock(checkPos, burnedGrass.defaultBlockState(), 3);
                            continue;
                        }

                    }
                    else if (state.is(BlockTags.WOODEN_STAIRS) || state.is(BlockTags.WOODEN_SLABS) || state.is(BlockTags.LEAVES)  || state.is(BlockTags.WOODEN_TRAPDOORS) ||
                            state.is(Blocks.TORCH) || state.is(BlockTags.WOOL_CARPETS) || state.is(BlockTags.WOOL) ||
                            state.is(BlockTags.WOODEN_FENCES) || state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON) || state.is(BlockTags.WOODEN_DOORS)) {
                        level.removeBlock(checkPos, false);
                    }

                    else if (horizontalDistance <= ZONE_4_RADIUS) {
                        if (state.is(BlockTags.LEAVES)) {
                            if (random.nextFloat() < 0.4F) {
                                level.removeBlock(checkPos, false);
                            } else if (random.nextFloat() < 0.2F) {
                                level.setBlock(checkPos, Blocks.FIRE.defaultBlockState(), 3);
                            }
                            continue;
                        }
                    }
                }
            }
        }
    }
}
