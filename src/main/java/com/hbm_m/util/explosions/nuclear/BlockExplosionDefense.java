package com.hbm_m.util.explosions.nuclear;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.hbm_m.block.ModBlocks;

/**
 *  СИСТЕМА ЗАЩИТЫ БЛОКОВ С КОЭФФИЦИЕНТОМ ПРОБИТИЯ v3.0
 *
 *  Логичные коэффициенты по материалам:
 *  Бетон: 250
 *  Бетонные кирпичи: 350
 *  Метеорит: 500
 *  Кафель, мозаика: 180-220
 *  Специальный бетон (усиленный): 400-600
 *  Тултип с золотым цветом взрывоустойчивости
 */

@Mod.EventBusSubscriber(modid = "hbm_m", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BlockExplosionDefense {

    /**
     * Small helper result used by crater generation code.
     */
    public static final class ExplosionDefenseResult {
        public final boolean shouldBreak;
        public final float blastPower;
        public final float defenseValue;

        public ExplosionDefenseResult(boolean shouldBreak, float blastPower, float defenseValue) {
            this.shouldBreak = shouldBreak;
            this.blastPower = blastPower;
            this.defenseValue = defenseValue;
        }
    }

    /**
     * Compatibility method for crater-generation logic.
     *
     * <p>Computes a simple "blast power vs defense" heuristic and returns whether the block
     * should break.</p>
     */
    public static ExplosionDefenseResult calculateExplosionDamage(
            ServerLevel level,
            BlockPos pos,
            BlockPos center,
            float radius,
            RandomSource random
    ) {
        if (level == null) {
            return new ExplosionDefenseResult(false, 0F, 0F);
        }

        BlockState state = level.getBlockState(pos);
        if (state == null || state.isAir()) {
            return new ExplosionDefenseResult(false, 0F, 0F);
        }

        float dist = (float) Math.sqrt(pos.distSqr(center));
        float falloff = radius <= 0F ? 0F : Math.max(0F, 1F - (dist / radius));
        float blastPower = (20F + (random != null ? random.nextFloat() * 10F : 0F)) * falloff;

        float defenseValue = getBlockDefenseValue(level, pos, state);
        boolean shouldBreak = blastPower > defenseValue;
        return new ExplosionDefenseResult(shouldBreak, blastPower, defenseValue);
    }

    /**
     * ГЛАВНЫЙ МЕТОД: Получить коэффициент защиты блока
     * Основано на взрывоустойчивости и типе материала
     */
    public static float getBlockDefenseValue(ServerLevel level, BlockPos pos, BlockState state) {
        if (state == null) return 0.0F;

        // Бедрок - абсолютная защита
        if (state.is(Blocks.BEDROCK)) {
            return 10_000.0F;
        }

        if (level != null && pos != null && state.getDestroySpeed(level, pos) < 0) {
            return 10_000.0F;
        }

        Block block = state.getBlock();

        // === ЯВНО СУПЕР-ПРОЧНЫЕ БЛОКИ ===
        if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN) {
            return 250.0F;
        }

        if (block == Blocks.ANCIENT_DEBRIS) {
            return 400.0F;
        }

        if (block == Blocks.NETHERITE_BLOCK) {
            return 300.0F;
        }

        // ========== ПОЛЬЗОВАТЕЛЬСКИЕ БЛОКИ ==========

        // === БЕТОН (базовый) - 250 ===
        if (isConcreteBlock(block)) {
            return 250.0F;
        }

        // === МЕТЕОРИТ - 500 ===
        if (isMeteorBlock(block)) {
            return 500.0F;
        }

        // === БЕТОННЫЕ КИРПИЧИ - 350 ===
        if (isBrickBlock(block)) {
            return 350.0F;
        }

        // === КАФЕЛЬ И МОЗАИКА - 200 ===
        if (isTileBlock(block)) {
            return 200.0F;
        }

        // === СПЕЦИАЛЬНЫЕ МАТЕРИАЛЫ ===
        if (isDepthBlock(block)) {
            return 280.0F;
        }

        if (isGneissBlock(block)) {
            return 260.0F;
        }

        if (isBasaltBlock(block)) {
            return 240.0F;
        }

        // === ЛЕСТНИЦЫ (половина защиты от базового блока) ===
        if (isStairsBlock(block)) {
            return 150.0F;
        }

        // === СТАНДАРТНАЯ КОНВЕРСИЯ ВЗРЫВОУСТОЙЧИВОСТИ ===
        float blastRes = getBlastResistance(state);

        // Диапазон 0-50: защита 5-10 (линейно)
        if (blastRes <= 50.0F) {
            float t = blastRes / 50.0F;
            return 5.0F + t * 5.0F;
        }

        // Диапазон 50-250: защита 25
        if (blastRes <= 250.0F) {
            return 25.0F;
        }

        // Диапазон 250-1000: защита 50
        if (blastRes <= 1000.0F) {
            return 50.0F;
        }

        // 1000+: защита 100
        return 100.0F;
    }

    // ========== ОБРАБОТЧИК ТУЛТИПОВ (EventHandler встроен в класс) ==========

    /**
     * ОБРАБОТЧИК СОБЫТИЙ ТУЛТИПОВ
     * Автоматически добавляет информацию о взрывоустойчивости к блокам
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        // Проверяем, это ли BlockItem (блок в виде предмета)
        if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)) {
            return;
        }

        // Получаем блок из предмета
        var block = blockItem.getBlock();

        // Проверяем, это ли один из наших модульных блоков
        if (isModularBlock(block)) {
            // Определяем коэффициент защиты по типу блока
            float defenseValue = getDefenseValueForBlock(block);

            // Добавляем локализованную строку в тултип
            if (defenseValue >= 10_000.0F) {
                event.getToolTip().add(Component.translatable("tooltip.hbm_m.explosion_defense.unbreakable"));
            } else if (defenseValue > 0) {
                event.getToolTip().add(Component.translatable("tooltip.hbm_m.explosion_defense.value", String.format("%.0f", defenseValue)));
            }
        }
    }

    /**
     *  Проверка: это ли один из наших модульных блоков
     */
    private static boolean isModularBlock(Block block) {
        return isConcreteBlock(block) ||
                isMeteorBlock(block) ||
                isBrickBlock(block) ||
                isTileBlock(block) ||
                isDepthBlock(block) ||
                isGneissBlock(block) ||
                isBasaltBlock(block) ||
                isStairsBlock(block);
    }

    /**
     *  Получить защиту по типу блока
     */
    private static float getDefenseValueForBlock(Block block) {
        if (isConcreteBlock(block)) return 250.0F;
        if (isMeteorBlock(block)) return 500.0F;
        if (isBrickBlock(block)) return 350.0F;
        if (isTileBlock(block)) return 200.0F;
        if (isDepthBlock(block)) return 280.0F;
        if (isGneissBlock(block)) return 260.0F;
        if (isBasaltBlock(block)) return 240.0F;
        if (isStairsBlock(block)) return 150.0F;

        return 0.0F;
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ КЛАССИФИКАЦИИ ==========

    /**
     *  Базовые БЕТОННЫЕ блоки - 250
     */
    private static boolean isConcreteBlock(Block block) {
        return block == ModBlocks.CONCRETE_BLACK.get() ||
                block == ModBlocks.CONCRETE_WHITE.get() ||
                block == ModBlocks.CONCRETE_RED.get() ||
                block == ModBlocks.CONCRETE_GREEN.get() ||
                block == ModBlocks.CONCRETE_BLUE.get() ||
                block == ModBlocks.CONCRETE_YELLOW.get() ||
                block == ModBlocks.CONCRETE_CYAN.get() ||
                block == ModBlocks.CONCRETE_GRAY.get() ||
                block == ModBlocks.CONCRETE_LIGHT_BLUE.get() ||
                block == ModBlocks.CONCRETE_LIME.get() ||
                block == ModBlocks.CONCRETE_MAGENTA.get() ||
                block == ModBlocks.CONCRETE_ORANGE.get() ||
                block == ModBlocks.CONCRETE_PINK.get() ||
                block == ModBlocks.CONCRETE_PURPLE.get() ||
                block == ModBlocks.CONCRETE_BROWN.get() ||
                block == ModBlocks.CONCRETE_SILVER.get() ||
                block == ModBlocks.CONCRETE_ASBESTOS.get() ||
                block == ModBlocks.CONCRETE_FLAT.get() ||
                block == ModBlocks.CONCRETE_PILLAR.get() ||
                block == ModBlocks.CONCRETE_MARKED.get() ||
                block == ModBlocks.CONCRETE_COLORED_BRONZE.get() ||
                block == ModBlocks.CONCRETE_COLORED_INDIGO.get() ||
                block == ModBlocks.CONCRETE_COLORED_MACHINE.get() ||
                block == ModBlocks.CONCRETE_COLORED_MACHINE_STRIPE.get() ||
                block == ModBlocks.CONCRETE_COLORED_PINK.get() ||
                block == ModBlocks.CONCRETE_COLORED_PURPLE.get() ||
                block == ModBlocks.CONCRETE_COLORED_SAND.get();
    }

    /**
     *  УСИЛЕННЫЙ БЕТОН - 400
     */
    private static boolean isSpecialConcreteBlock(Block block) {
        return block == ModBlocks.CONCRETE_SUPER.get() ||
                block == ModBlocks.CONCRETE_SUPER_M0.get() ||
                block == ModBlocks.CONCRETE_SUPER_M1.get() ||
                block == ModBlocks.CONCRETE_SUPER_M2.get() ||
                block == ModBlocks.CONCRETE_SUPER_M3.get() ||
                block == ModBlocks.CONCRETE_SUPER_BROKEN.get() ||
                block == ModBlocks.CONCRETE_REBAR.get() ||
                block == ModBlocks.CONCRETE_REBAR_ALT.get();
    }

    /**
     *  МЕТЕОРИТ - 500
     */
    private static boolean isMeteorBlock(Block block) {
        return block == ModBlocks.METEOR.get() ||
                block == ModBlocks.METEOR_BRICK.get() ||
                block == ModBlocks.METEOR_BRICK_CHISELED.get() ||
                block == ModBlocks.METEOR_BRICK_CRACKED.get() ||
                block == ModBlocks.METEOR_BRICK_MOSSY.get() ||
                block == ModBlocks.METEOR_COBBLE.get() ||
                block == ModBlocks.METEOR_CRUSHED.get() ||
                block == ModBlocks.METEOR_PILLAR.get() ||
                block == ModBlocks.METEOR_POLISHED.get() ||
                block == ModBlocks.METEOR_TREASURE.get();
    }

    /**
     *  БЕТОННЫЕ КИРПИЧИ - 350
     */
    private static boolean isBrickBlock(Block block) {
        return block == ModBlocks.BRICK_BASE.get() ||
                block == ModBlocks.BRICK_DUCRETE.get() ||
                block == ModBlocks.BRICK_FIRE.get() ||
                block == ModBlocks.BRICK_LIGHT.get() ||
                block == ModBlocks.BRICK_OBSIDIAN.get();
    }

    /**
     *  КАФЕЛЬ И МОЗАИКА - 200
     */
    private static boolean isTileBlock(Block block) {
        return block == ModBlocks.CONCRETE_TILE.get() ||
                block == ModBlocks.CONCRETE_TILE_TREFOIL.get() ||
                block == ModBlocks.VINYL_TILE.get() ||
                block == ModBlocks.VINYL_TILE_SMALL.get() ||
                block == ModBlocks.DEPTH_TILES.get() ||
                block == ModBlocks.DEPTH_NETHER_TILES.get() ||
                block == ModBlocks.GNEISS_TILE.get();
    }

    /**
     *  DEPTH МАТЕРИАЛЫ - 280
     */
    private static boolean isDepthBlock(Block block) {
        return block == ModBlocks.DEPTH_BRICK.get() ||
                block == ModBlocks.DEPTH_NETHER_BRICK.get() ||
                block == ModBlocks.DEPTH_STONE_NETHER.get();
    }

    /**
     *  ГНЕЙСС - 260
     */
    private static boolean isGneissBlock(Block block) {
        return block == ModBlocks.GNEISS_BRICK.get() ||
                block == ModBlocks.GNEISS_CHISELED.get() ||
                block == ModBlocks.GNEISS_STONE.get();
    }

    /**
     *  БАЗАЛЬТ - 240
     */
    private static boolean isBasaltBlock(Block block) {
        return block == ModBlocks.BASALT_BRICK.get() ||
                block == ModBlocks.BASALT_POLISHED.get() ||
                block == ModBlocks.ASPHALT.get() ||
                block == ModBlocks.BARRICADE.get();
    }

    /**
     *  ЛЕСТНИЦЫ (STAIRS) - 150 (половина от базового)
     */
    private static boolean isStairsBlock(Block block) {
        return block == ModBlocks.CONCRETE_ASBESTOS_STAIRS.get() ||
                block == ModBlocks.CONCRETE_BLACK_STAIRS.get() ||
                block == ModBlocks.CONCRETE_BLUE_STAIRS.get() ||
                block == ModBlocks.CONCRETE_BROWN_STAIRS.get() ||
                block == ModBlocks.CONCRETE_COLORED_BRONZE_STAIRS.get() ||
                block == ModBlocks.CONCRETE_COLORED_INDIGO_STAIRS.get() ||
                block == ModBlocks.CONCRETE_COLORED_MACHINE_STAIRS.get() ||
                block == ModBlocks.CONCRETE_COLORED_PINK_STAIRS.get() ||
                block == ModBlocks.CONCRETE_COLORED_PURPLE_STAIRS.get() ||
                block == ModBlocks.CONCRETE_COLORED_SAND_STAIRS.get() ||
                block == ModBlocks.CONCRETE_CYAN_STAIRS.get() ||
                block == ModBlocks.CONCRETE_GRAY_STAIRS.get() ||
                block == ModBlocks.CONCRETE_GREEN_STAIRS.get() ||
                block == ModBlocks.CONCRETE_LIGHT_BLUE_STAIRS.get() ||
                block == ModBlocks.CONCRETE_LIME_STAIRS.get() ||
                block == ModBlocks.CONCRETE_MAGENTA_STAIRS.get() ||
                block == ModBlocks.CONCRETE_ORANGE_STAIRS.get() ||
                block == ModBlocks.CONCRETE_PINK_STAIRS.get() ||
                block == ModBlocks.CONCRETE_PURPLE_STAIRS.get() ||
                block == ModBlocks.CONCRETE_RED_STAIRS.get() ||
                block == ModBlocks.CONCRETE_SILVER_STAIRS.get() ||
                block == ModBlocks.CONCRETE_WHITE_STAIRS.get() ||
                block == ModBlocks.CONCRETE_YELLOW_STAIRS.get() ||
                block == ModBlocks.CONCRETE_SUPER_STAIRS.get() ||
                block == ModBlocks.CONCRETE_SUPER_M0_STAIRS.get() ||
                block == ModBlocks.CONCRETE_SUPER_M1_STAIRS.get() ||
                block == ModBlocks.CONCRETE_SUPER_M2_STAIRS.get() ||
                block == ModBlocks.CONCRETE_SUPER_M3_STAIRS.get() ||
                block == ModBlocks.CONCRETE_SUPER_BROKEN_STAIRS.get() ||
                block == ModBlocks.CONCRETE_REBAR_STAIRS.get() ||
                block == ModBlocks.CONCRETE_FLAT_STAIRS.get() ||
                block == ModBlocks.CONCRETE_TILE_STAIRS.get() ||
                block == ModBlocks.DEPTH_BRICK_STAIRS.get() ||
                block == ModBlocks.DEPTH_TILES_STAIRS.get() ||
                block == ModBlocks.DEPTH_NETHER_BRICK_STAIRS.get() ||
                block == ModBlocks.DEPTH_NETHER_TILES_STAIRS.get() ||
                block == ModBlocks.GNEISS_TILE_STAIRS.get() ||
                block == ModBlocks.GNEISS_BRICK_STAIRS.get() ||
                block == ModBlocks.BRICK_BASE_STAIRS.get() ||
                block == ModBlocks.BRICK_LIGHT_STAIRS.get() ||
                block == ModBlocks.BRICK_FIRE_STAIRS.get() ||
                block == ModBlocks.BRICK_OBSIDIAN_STAIRS.get() ||
                block == ModBlocks.VINYL_TILE_STAIRS.get() ||
                block == ModBlocks.VINYL_TILE_SMALL_STAIRS.get() ||
                block == ModBlocks.BRICK_DUCRETE_STAIRS.get() ||
                block == ModBlocks.ASPHALT_STAIRS.get() ||
                block == ModBlocks.BASALT_POLISHED_STAIRS.get() ||
                block == ModBlocks.BASALT_BRICK_STAIRS.get() ||
                block == ModBlocks.METEOR_POLISHED_STAIRS.get() ||
                block == ModBlocks.METEOR_BRICK_STAIRS.get() ||
                block == ModBlocks.METEOR_BRICK_CRACKED_STAIRS.get() ||
                block == ModBlocks.METEOR_BRICK_MOSSY_STAIRS.get() ||
                block == ModBlocks.METEOR_CRUSHED_STAIRS.get();
    }

    /**
     *  Получить взрывоустойчивость блока
     */
    public static float getBlastResistance(BlockState state) {
        if (state == null) return 0.0F;
        return state.getBlock().getExplosionResistance();
    }

    /**
     *  Получить уровень защиты по диапазонам (для обратной совместимости)
     */
    public static int getDefenseLevelFromResistance(float blastRes) {
        if (blastRes < 0) return 15;
        if (blastRes < 1.0F) return 0;
        if (blastRes < 2.0F) return 1;
        if (blastRes < 5.0F) return 2;
        if (blastRes < 10.0F) return 3;
        if (blastRes < 20.0F) return 4;
        if (blastRes < 30.0F) return 5;
        if (blastRes < 50.0F) return 6;
        if (blastRes < 75.0F) return 7;
        if (blastRes < 100.0F) return 8;
        if (blastRes < 150.0F) return 9;
        if (blastRes < 250.0F) return 10;
        if (blastRes < 500.0F) return 11;
        if (blastRes < 1000.0F) return 12;
        if (blastRes < 5000.0F) return 13;
        return 14;
    }
}
