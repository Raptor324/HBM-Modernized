package com.hbm_m.util.explosions.general;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class BlastExplosionGenerator {
    // Параметры взрыва
    private static final float BASE_HARDNESS_THRESHOLD = 3.0F;
    private static final float MAX_HARDNESS_THRESHOLD = 50.0F;
    private static final int DEBRIS_COUNT_MULTIPLIER = 6;
    private static final int FLYING_DEBRIS_COUNT = 200; // Количество летящих осколков

    // Параметры урона
    private static final float BASE_EXPLOSION_DAMAGE = 25.0F;
    private static final float DAMAGE_FALLOFF = 0.6F;

    /**
     * Создаёт естественный кратер с эффектами осколков и непрерывным уроном
     */
    public static void generateNaturalCrater(ServerLevel level, BlockPos centerPos,
                                             int radius, int depth) {
        RandomSource random = level.random;
        List<CraterData> craterBlocks = new ArrayList<>();
        Set<BlockPos> debrisPositions = new HashSet<>();
        Set<BlockPos> aboveGroundBlocks = new HashSet<>();

        // Фаза 1: Расчёт блоков для естественной формы кратера
        calculateNaturalCraterShape(level, centerPos, radius, depth, random, craterBlocks);

        // Фаза 2: Расчёт блоков над поверхностью для уничтожения
        calculateAboveGroundDestruction(level, centerPos, radius, random, aboveGroundBlocks);

        // Фаза 3: Генерация позиций для осколков
        generateDebrisField(level, centerPos, radius, depth, random, debrisPositions);

        // Фаза 4: Начинаем поэтапное разрушение с уроном
        startCraterGeneration(level, centerPos, craterBlocks, debrisPositions,
                aboveGroundBlocks, radius, depth);
    }

    /**
     * Вычисляет естественную форму кратера с шумом и неровностями
     */
    private static void calculateNaturalCraterShape(ServerLevel level, BlockPos centerPos,
                                                    int radius, int depth, RandomSource random,
                                                    List<CraterData> craterBlocks) {
        // Генерируем шумовую карту для естественности
        int mapSize = radius * 2 + 10;
        double[][] noiseMap = generatePerlinNoise(mapSize, mapSize, random);

        for (int x = -radius - 5; x <= radius + 5; x++) {
            for (int z = -radius - 5; z <= radius + 5; z++) {
                double horizontalDistance = Math.sqrt(x * x + z * z);

                // Получаем значение шума для этой позиции
                int noiseX = Math.min(mapSize - 1, Math.max(0, x + radius + 5));
                int noiseZ = Math.min(mapSize - 1, Math.max(0, z + radius + 5));
                double noiseValue = noiseMap[noiseX][noiseZ];

                // Модифицируем радиус с помощью шума для неровных краёв
                double modifiedRadius = radius + (noiseValue - 0.5) * radius * 0.25;

                if (horizontalDistance <= modifiedRadius) {
                    // Вычисляем глубину используя параболическую функцию
                    int localDepth = calculateParabolicDepth(horizontalDistance, modifiedRadius, depth, noiseValue);

                    for (int y = 0; y >= -localDepth; y--) {
                        BlockPos checkPos = centerPos.offset(x, y, z);

                        // Добавляем вероятностное разрушение для естественности
                        if (shouldDestroyBlock(level, checkPos, horizontalDistance, modifiedRadius, y, localDepth, random)) {
                            CraterData craterData = new CraterData(
                                    checkPos,
                                    calculateDestructionDelay(horizontalDistance, modifiedRadius),
                                    horizontalDistance < modifiedRadius * 0.65 // Осколки только в центральной зоне
                            );
                            craterBlocks.add(craterData);
                        }
                    }
                }

                // Добавляем эффект "поднятого края" кратера
                if (horizontalDistance > modifiedRadius * 0.7 && horizontalDistance <= modifiedRadius * 1.3) {
                    double rimHeight = calculateRimHeight(horizontalDistance, modifiedRadius, radius, noiseValue);

                    for (int y = 1; y <= (int)rimHeight; y++) {
                        BlockPos checkPos = centerPos.offset(x, y, z);
                        if (random.nextFloat() < 0.65f) { // 65% шанс разрушения
                            CraterData craterData = new CraterData(
                                    checkPos,
                                    calculateDestructionDelay(horizontalDistance, modifiedRadius),
                                    false
                            );
                            craterBlocks.add(craterData);
                        }
                    }
                }
            }
        }

        // Сортируем по времени разрушения (от края к центру - волна разрушения)
        craterBlocks.sort(Comparator.comparingInt(CraterData::getDelay));
    }

    /**
     * Вычисляет блоки над землёй для уничтожения
     */
    private static void calculateAboveGroundDestruction(ServerLevel level, BlockPos centerPos,
                                                        int radius, RandomSource random,
                                                        Set<BlockPos> aboveGroundBlocks) {
        // Уничтожаем блоки над землёй в сферическом радиусе
        int verticalRadius = radius; // Сфера, а не цилиндр

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = 1; y <= verticalRadius; y++) {
                    BlockPos checkPos = centerPos.offset(x, y, z);
                    double distance = Math.sqrt(x * x + y * y + z * z);

                    // Сферическое разрушение с затуханием
                    if (distance <= radius) {
                        float destructionChance = Math.max(0.3f, 1.0f - (float)(distance / radius));

                        if (random.nextFloat() < destructionChance) {
                            BlockState state = level.getBlockState(checkPos);
                            if (!state.isAir() && !state.is(Blocks.BEDROCK)) {
                                aboveGroundBlocks.add(checkPos);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Генерирует поля осколков вокруг кратера
     */
    private static void generateDebrisField(ServerLevel level, BlockPos centerPos, int radius, int depth,
                                            RandomSource random, Set<BlockPos> debrisPositions) {
        int debrisCount = radius * DEBRIS_COUNT_MULTIPLIER;
        int debrisRadius = (int)(radius * 1.8); // Осколки разлетаются на 1.8x расстояния

        for (int i = 0; i < debrisCount; i++) {
            // Случайная позиция в расширенной области
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = radius * 0.8 + random.nextDouble() * debrisRadius;
            int x = (int)(Math.cos(angle) * distance);
            int z = (int)(Math.sin(angle) * distance);

            // Находим поверхность для размещения осколка
            BlockPos surfacePos = findSurfacePosition(level, centerPos.offset(x, 0, z));
            if (surfacePos != null && random.nextFloat() < 0.5f) { // 50% шанс появления осколка
                debrisPositions.add(surfacePos);
            }
        }
    }

    /**
     * Начинает поэтапное создание кратера с эффектами
     */
    private static void startCraterGeneration(ServerLevel level, BlockPos centerPos,
                                              List<CraterData> craterBlocks, Set<BlockPos> debrisPositions,
                                              Set<BlockPos> aboveGroundBlocks, int radius, int depth) {
        // Группируем блоки по задержкам для пакетного обновления
        Map<Integer, List<CraterData>> delayGroups = new HashMap<>();
        for (CraterData data : craterBlocks) {
            delayGroups.computeIfAbsent(data.getDelay(), k -> new ArrayList<>()).add(data);
        }

        // Немедленно создаём летящие осколки для визуального эффекта
        spawnFlyingDebris(level, centerPos, radius);

        // Запускаем поэтапное разрушение
        processDelayedDestruction(level, centerPos, delayGroups, debrisPositions,
                aboveGroundBlocks, radius, depth, 0);
    }

    /**
     * Создаёт летящие осколки-частицы
     */
    private static void spawnFlyingDebris(ServerLevel level, BlockPos centerPos, int radius) {
        RandomSource random = level.random;

        for (int i = 0; i < FLYING_DEBRIS_COUNT; i++) {
            // Случайная позиция на поверхности кратера
            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = random.nextDouble() * radius * 0.7;
            double x = centerPos.getX() + Math.cos(angle) * dist + 0.5;
            double y = centerPos.getY() + 0.5;
            double z = centerPos.getZ() + Math.sin(angle) * dist + 0.5;

            // Скорость вылета осколка (от центра)
            double velocityX = (Math.cos(angle) + (random.nextDouble() - 0.5) * 0.5) * 2.5;
            double velocityY = 1.5 + random.nextDouble() * 2.0;
            double velocityZ = (Math.sin(angle) + (random.nextDouble() - 0.5) * 0.5) * 2.5;




            // Добавляем дым для эффекта
            if (random.nextFloat() < 0.3f) {
                level.sendParticles(
                        ParticleTypes.LARGE_SMOKE,
                        x, y, z,
                        1,
                        velocityX * 0.5, velocityY * 0.5, velocityZ * 0.5,
                        0.1
                );
            }
        }
    }

    /**
     * Обрабатывает разрушение с задержкой и эффектами
     */
    private static void processDelayedDestruction(ServerLevel level, BlockPos centerPos,
                                                  Map<Integer, List<CraterData>> delayGroups,
                                                  Set<BlockPos> debrisPositions,
                                                  Set<BlockPos> aboveGroundBlocks,
                                                  int radius, int depth, int currentDelay) {
        // ВАЖНО: Наносим урон всем сущностям в радиусе на каждой итерации
        int maxDelay = delayGroups.keySet().stream().max(Integer::compare).orElse(0);
        damageEntitiesInRadius(level, centerPos, radius + 10, currentDelay, maxDelay);

        // Обрабатываем текущую группу блоков под землёй
        List<CraterData> currentGroup = delayGroups.get(currentDelay);
        if (currentGroup != null) {
            for (CraterData data : currentGroup) {
                BlockPos pos = data.getPosition();

                // Создаем эффекты частиц перед разрушением
                createExplosionParticles(level, pos);

                // Проверяем прочность блока
                BlockState state = level.getBlockState(pos);
                float hardness = state.getDestroySpeed(level, pos);
                float breakChance = calculateBreakChance(hardness);

                if (level.random.nextFloat() < breakChance) {
                    // Создаем осколки только для блоков на поверхности
                    if (data.isCreateDebris()) {
                        createDebrisParticle(level, pos);
                    }

                    level.removeBlock(pos, false);
                }
            }
        }

        // Уничтожаем блоки над землёй на первой итерации
        if (currentDelay == 0) {
            for (BlockPos pos : aboveGroundBlocks) {
                BlockState state = level.getBlockState(pos);

                // Создаём эффекты разрушения
                createExplosionParticles(level, pos);
                createDebrisParticle(level, pos);

                level.removeBlock(pos, false);
            }

            // Создаем осколки на поверхности
            createScatteredDebris(level, debrisPositions);
        }

        // Планируем следующую итерацию
        if (currentDelay < maxDelay) {
            int nextDelay = currentDelay + 1;
            level.getServer().tell(new net.minecraft.server.TickTask(2, () -> {
                processDelayedDestruction(level, centerPos, delayGroups, debrisPositions,
                        aboveGroundBlocks, radius, depth, nextDelay);
            }));
        } else {
            // Завершаем создание поверхности кратера
            generateCraterSurface(level, centerPos, extractPositions(delayGroups));
        }
    }

    /**
     * Наносит урон всем сущностям в радиусе взрыва
     */
    private static void damageEntitiesInRadius(ServerLevel level, BlockPos centerPos,
                                               int radius, int currentDelay, int totalDelays) {
        AABB damageArea = new AABB(centerPos).inflate(radius);
        List<Entity> entities = level.getEntities((Entity) null, damageArea);

        Vec3 center = centerPos.getCenter();

        for (Entity entity : entities) {
            if (entity instanceof LivingEntity livingEntity) {
                double distance = entity.position().distanceTo(center);

                if (distance <= radius) {
                    // Рассчитываем урон с учетом расстояния и времени
                    float damage = calculateExplosionDamage(distance, radius, currentDelay, totalDelays);

                    if (damage > 0.5f) { // Минимальный порог урона
                        DamageSource explosionDamage = level.damageSources().explosion(null, null);
                        livingEntity.hurt(explosionDamage, damage);

                        // Добавляем эффекты отбрасывания
                        applyKnockback(livingEntity, centerPos, distance, radius);
                    }
                }
            }
        }
    }

    /**
     * Вычисляет урон от взрыва с учетом расстояния и времени
     */
    private static float calculateExplosionDamage(double distance, int radius, int currentDelay, int totalDelays) {
        // Базовый урон уменьшается с расстоянием
        float distanceFactor = Math.max(0, 1.0f - (float)(distance / radius));

        // Урон сильнее в начале, но продолжается на протяжении всей генерации
        float timeFactor = Math.max(0.2f, 1.0f - (float)currentDelay / totalDelays * DAMAGE_FALLOFF);

        return BASE_EXPLOSION_DAMAGE * distanceFactor * distanceFactor * timeFactor;
    }

    /**
     * Применяет эффект отбрасывания к сущности
     */
    private static void applyKnockback(LivingEntity entity, BlockPos centerPos, double distance, int radius) {
        if (distance > 0.1) {
            double knockbackStrength = Math.max(0.1, 1.0 - distance / radius) * 3.0;

            Vec3 entityPos = entity.position();
            Vec3 center = centerPos.getCenter();

            double deltaX = entityPos.x - center.x;
            double deltaZ = entityPos.z - center.z;
            double deltaY = 0.7; // Подбрасываем вверх

            // Нормализуем горизонтальный вектор
            double length = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            if (length > 0) {
                deltaX /= length;
                deltaZ /= length;
            }

            entity.setDeltaMovement(entity.getDeltaMovement().add(
                    deltaX * knockbackStrength,
                    deltaY * knockbackStrength * 0.6,
                    deltaZ * knockbackStrength
            ));
        }
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private static class CraterData {
        private final BlockPos position;
        private final int delay;
        private final boolean createDebris;

        public CraterData(BlockPos position, int delay, boolean createDebris) {
            this.position = position;
            this.delay = delay;
            this.createDebris = createDebris;
        }

        public BlockPos getPosition() { return position; }
        public int getDelay() { return delay; }
        public boolean isCreateDebris() { return createDebris; }
    }

    /**
     * Генерирует карту шума Перлина для естественности формы
     */
    private static double[][] generatePerlinNoise(int width, int height, RandomSource random) {
        double[][] noise = new double[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Многооктавный шум для большей детализации
                double value = 0;
                double amplitude = 1;
                double frequency = 0.08;

                for (int octave = 0; octave < 5; octave++) {
                    value += noise(x * frequency, y * frequency, random) * amplitude;
                    amplitude *= 0.5;
                    frequency *= 2;
                }

                noise[x][y] = Math.max(0, Math.min(1, value * 0.5 + 0.5));
            }
        }

        return noise;
    }

    private static double noise(double x, double y, RandomSource random) {
        int ix = (int)Math.floor(x);
        int iy = (int)Math.floor(y);

        // Используем детерминированный seed для стабильности
        random.setSeed((long)(ix * 374761393L + iy * 668265263L));
        return random.nextDouble() * 2 - 1;
    }

    /**
     * Вычисляет глубину используя параболическую функцию (более естественно)
     */
    private static int calculateParabolicDepth(double distance, double radius, int maxDepth, double noise) {
        // Параболическая функция для формы кратера
        double normalizedDist = distance / radius;
        double depthFactor = 1.0 - normalizedDist * normalizedDist; // Парабола
        depthFactor = Math.pow(depthFactor, 0.8); // Делаем более крутым

        // Добавляем шум для неровности дна
        double noiseModifier = 1.0 + (noise - 0.5) * 0.3;

        return Math.max(1, (int)(maxDepth * depthFactor * noiseModifier));
    }

    /**
     * Вычисляет высоту поднятого края кратера
     */
    private static double calculateRimHeight(double distance, double modifiedRadius, int radius, double noise) {
        // Гауссовская функция для формы края
        double rimCenter = modifiedRadius;
        double rimWidth = radius * 0.3;
        double distFromRim = Math.abs(distance - rimCenter);

        double heightFactor = Math.exp(-(distFromRim * distFromRim) / (2 * rimWidth * rimWidth));
        double baseHeight = radius * 0.08; // 8% от радиуса

        return (baseHeight * heightFactor + noise * 2) * (1.0 + noise * 0.5);
    }

    private static boolean shouldDestroyBlock(ServerLevel level, BlockPos pos, double distance,
                                              double radius, int y, int depth, RandomSource random) {
        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);

        // Базовый шанс разрушения
        float baseChance = calculateBreakChance(hardness);

        // Модификаторы шанса на основе позиции
        float distanceModifier = Math.max(0.4f, 1.0f - (float)(distance / radius));
        float depthModifier = Math.max(0.6f, 1.0f - Math.abs(y) / (float)depth);

        return random.nextFloat() < baseChance * distanceModifier * depthModifier;
    }

    private static int calculateDestructionDelay(double distance, double radius) {
        // Блоки ближе к краю разрушаются раньше (волна от края к центру)
        return (int)((distance / radius) * 25); // Максимум 25 тиков задержки
    }

    private static void createExplosionParticles(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.EXPLOSION,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                2, 0.3, 0.3, 0.3, 0.05);

        level.sendParticles(ParticleTypes.SMOKE,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                4, 0.8, 0.8, 0.8, 0.03);
    }

    private static void createDebrisParticle(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        // Создаем частицы разрушения блока
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                6, 0.4, 0.4, 0.4, 0.15);
    }

    private static void createScatteredDebris(ServerLevel level, Set<BlockPos> debrisPositions) {
        RandomSource random = level.random;

        for (BlockPos pos : debrisPositions) {
            if (level.getBlockState(pos).isAir() || level.getBlockState(pos).canBeReplaced()) {



                // Добавляем частицы дыма для эффект
                level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                        pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5,
                        2, 0.1, 0.1, 0.1, 0.02);
            }
        }
    }

    /**
     * Возвращает случайный блок из ModBlocks для осколков
     */
    /**
     * Возвращает случайный блок из SELLAFIELD_SLAKED для осколков
     */


    private static BlockPos findSurfacePosition(ServerLevel level, BlockPos startPos) {
        // Находим поверхность для размещения осколка (сканируем вверх/вниз)
        for (int y = startPos.getY() + 20; y >= startPos.getY() - 10; y--) {
            BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());
            BlockPos below = checkPos.below();

            if (!level.getBlockState(checkPos).isSolid() && level.getBlockState(below).isSolid()) {
                return checkPos;
            }
        }

        return null;
    }

    private static List<BlockPos> extractPositions(Map<Integer, List<CraterData>> delayGroups) {
        List<BlockPos> positions = new ArrayList<>();
        for (List<CraterData> group : delayGroups.values()) {
            for (CraterData data : group) {
                positions.add(data.getPosition());
            }
        }
        return positions;
    }

    private static float calculateBreakChance(float hardness) {
        if (hardness < 0) return 0.0F; // Неразрушимые блоки
        if (hardness <= BASE_HARDNESS_THRESHOLD) return 1.0F;
        if (hardness >= MAX_HARDNESS_THRESHOLD) return 0.1F; // Минимальный шанс даже для очень твёрдых

        float range = MAX_HARDNESS_THRESHOLD - BASE_HARDNESS_THRESHOLD;
        return 1.0F - ((hardness - BASE_HARDNESS_THRESHOLD) / range);
    }

    private static void generateCraterSurface(ServerLevel level, BlockPos centerPos,
                                              List<BlockPos> craterBlocks) {
        RandomSource random = level.random;
        Set<BlockPos> craterBlocksSet = new HashSet<>(craterBlocks);
        Set<BlockPos> bottomBlocks = new HashSet<>();

        // Находим "дно" кратера (блоки без блоков под ними)
        for (BlockPos pos : craterBlocks) {
            BlockPos below = pos.below();
            if (!craterBlocksSet.contains(below) && !level.getBlockState(pos).isAir()) {
                bottomBlocks.add(pos);
            }
        }


                }
            }


