package com.hbm_m.block;

import com.hbm_m.particle.ModParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DetMinerBlock extends Block implements IDetonatable {

    // Свойство блока, показывающее, активирован ли он редстоуном
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");

    private static final int EXPLOSION_RADIUS = 4; // Радиус взрыва

    public DetMinerBlock(Properties properties) {
        super(properties);
        // Устанавливаем состояние по умолчанию: не активирован
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // Регистрируем наше свойство POWERED
        builder.add(POWERED);
    }

    /**
     * Вызывается, когда блок размещается в мире.
     * Проверяет, активирован ли он редстоуном сразу после размещения.
     */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        // Проверяем, что это не обновление блока, а именно новое размещение,
        // и что мы находимся на серверной стороне (логика взрыва только на сервере)
        if (!oldState.is(state.getBlock()) && !level.isClientSide()) {
            // Если есть редстоун-сигнал, активируем заряд немедленно
            if (level.hasNeighborSignal(pos)) {
                triggerMiningExplosion(level, pos);
            }
        }
    }

    /**
     * Вызывается, когда соседний блок изменяется (включая редстоун-сигнал).
     * Это основной метод для реагирования на редстоун.
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        // Логика только на серверной стороне
        if (!level.isClientSide()) {            boolean hasSignal = level.hasNeighborSignal(pos);
            boolean wasPowered = state.getValue(POWERED);

            if (hasSignal && !wasPowered) {
                // Если появился редстоун-сигнал (восходящий фронт) и блок не был активирован,
                // обновляем состояние на "активирован" и запускаем взрыв.
                // Поскольку блок будет уничтожен после взрыва, сброс состояния для этой же
                // сущности блока не потребуется, но установка POWERED=true важна
                // для корректной работы логики "восходящего фронта" в этом методе.
                level.setBlock(pos, state.setValue(POWERED, true), 3); // 3 - флаги: обновление соседей и ререндер
                triggerMiningExplosion(level, pos); // Запускаем взрыв
            } else if (!hasSignal && wasPowered) {
                // Если сигнал пропал (нисходящий фронт) и блок был активирован,
                // сбрасываем состояние. Это важно, если бы блок не уничтожался сразу
                // или если бы мы хотели, чтобы он мог быть активирован снова после потери сигнала.
                level.setBlock(pos, state.setValue(POWERED, false), 3);
            }
        }    }

    /**
     * Запускает "шахтёрский взрыв".
     * Этот метод собирает лут, уничтожает блоки в радиусе и воспроизводит эффекты,
     * не нанося урон сущностям и сохраняя весь лут.
     * @param level Мир, в котором происходит взрыв.
     * @param pos Позиция блока взрывчатки.
     */

    @Override
    public boolean onDetonate(Level level, BlockPos pos, BlockState state, Player player) {

        ServerLevel serverLevel = (ServerLevel) level;
            List<ItemStack> collectedDrops = new ArrayList<>();
            Set<BlockPos> blocksToDestroy = getBlocksInSphere(pos, EXPLOSION_RADIUS);

            // 1. Собираем лут со всех блоков, которые будут уничтожены
            for (BlockPos blockPos : blocksToDestroy) {
                BlockState blockState = serverLevel.getBlockState(blockPos);
                // Исключаем воздух, бедро и сам шахтёрский заряд из сбора лута и разрушения
                if (!blockState.isAir() && !blockState.is(Blocks.BEDROCK) && !blockState.is(this)) {
                    // Создаем LootParams для получения лута.
                    // Использование ItemStack.EMPTY в качестве инструмента означает, что лут будет
                    // собран так, как если бы блок был разрушен без конкретного инструмента
                    // (например, "разбито рукой", что обычно сохраняет весь лут, если нет специальных условий).
                    LootParams.Builder lootParamsBuilder = new LootParams.Builder(serverLevel)
                            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockPos))
                            .withParameter(LootContextParams.TOOL, ItemStack.EMPTY);

                    // Получаем лут и добавляем его в наш список
                    collectedDrops.addAll(blockState.getDrops(lootParamsBuilder));
                }

                // Запускаем эффект дыма
                spawnExplosionWave((ServerLevel) level, pos);

            }

            // 2. Уничтожаем блоки в радиусе и заменяем их воздухом
            for (BlockPos blockPos : blocksToDestroy) {
                // Убеждаемся, что не удаляем сам блок шахтёрского заряда в этом цикле.
                // Он будет удалён в конце метода, чтобы избежать двойного удаления и проблем с состоянием.
                if (!blockPos.equals(pos)) { // Пропускаем позицию самого заряда
                    serverLevel.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
                    // Генерируем игровое событие GameEvent.BLOCK_DESTROY для наблюдателей и других механизмов
                    serverLevel.gameEvent(null, GameEvent.BLOCK_DESTROY, blockPos);


                }
            }        // 3. Спавним весь собранный лут в центре взрыва
            for (ItemStack itemStack : collectedDrops) {
                ItemEntity itemEntity = new ItemEntity(serverLevel, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, itemStack);
                // Добавляем небольшой случайный разброс для более естественного вида выпадения предметов
                itemEntity.setDeltaMovement(
                        (level.random.nextDouble() - 0.5D) * 0.2D,
                        (level.random.nextDouble() * 0.2D) + 0.1D,
                        (level.random.nextDouble() - 0.5D) * 0.2D
                );
                serverLevel.addFreshEntity(itemEntity); // Добавляем предмет в мир
            }

            // 4. Воспроизводим звуки и частицы взрыва
            // Звук взрыва (без урона)
            level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);
            // Частицы взрыва (EXPLOSION_EMITTER для более заметного эффекта)
            ((ServerLevel) level).sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 1, 0.0D, 0.0D, 0.0D, 0.0D);

            // 5. Удаляем сам блок шахтёрского заряда (если он еще не был удален)
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());


        /**
         * Вспомогательный метод для получения всех позиций блоков в сферическом радиусе.
         * Возвращает Set<BlockPos> для обеспечения уникальности позиций.
         *
         * @param center Центр сферы.
         * @param radius Радиус сферы.
         * @return Множество позиций блоков в сфере.
         */

        return false;
    }
    private void triggerMiningExplosion(Level level, BlockPos pos) {
        // Взрыв обрабатывается только на сервере
        if (level.isClientSide) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        List<ItemStack> collectedDrops = new ArrayList<>();
        Set<BlockPos> blocksToDestroy = getBlocksInSphere(pos, EXPLOSION_RADIUS);

        // 1. Собираем лут со всех блоков, которые будут уничтожены
        for (BlockPos blockPos : blocksToDestroy) {
            BlockState blockState = serverLevel.getBlockState(blockPos);
            // Исключаем воздух, бедро и сам шахтёрский заряд из сбора лута и разрушения
            if (!blockState.isAir() && !blockState.is(Blocks.BEDROCK) && !blockState.is(this)) {
                // Создаем LootParams для получения лута.
                // Использование ItemStack.EMPTY в качестве инструмента означает, что лут будет
                // собран так, как если бы блок был разрушен без конкретного инструмента
                // (например, "разбито рукой", что обычно сохраняет весь лут, если нет специальных условий).
                LootParams.Builder lootParamsBuilder = new LootParams.Builder(serverLevel)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockPos))
                        .withParameter(LootContextParams.TOOL, ItemStack.EMPTY);

                // Получаем лут и добавляем его в наш список
                collectedDrops.addAll(blockState.getDrops(lootParamsBuilder));
            }

            // Запускаем эффект дыма
            spawnExplosionWave((ServerLevel) level, pos);



        }

        // 2. Уничтожаем блоки в радиусе и заменяем их воздухом
        for (BlockPos blockPos : blocksToDestroy) {
            // Убеждаемся, что не удаляем сам блок шахтёрского заряда в этом цикле.
            // Он будет удалён в конце метода, чтобы избежать двойного удаления и проблем с состоянием.
            if (!blockPos.equals(pos)) { // Пропускаем позицию самого заряда
                serverLevel.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
                // Генерируем игровое событие GameEvent.BLOCK_DESTROY для наблюдателей и других механизмов
                serverLevel.gameEvent(null, GameEvent.BLOCK_DESTROY, blockPos);

            }
        }        // 3. Спавним весь собранный лут в центре взрыва
        for (ItemStack itemStack : collectedDrops) {
            ItemEntity itemEntity = new ItemEntity(serverLevel, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, itemStack);
            // Добавляем небольшой случайный разброс для более естественного вида выпадения предметов
            itemEntity.setDeltaMovement(
                    (level.random.nextDouble() - 0.5D) * 0.2D,
                    (level.random.nextDouble() * 0.2D) + 0.1D,
                    (level.random.nextDouble() - 0.5D) * 0.2D
            );
            serverLevel.addFreshEntity(itemEntity); // Добавляем предмет в мир
        }

        // 4. Воспроизводим звуки и частицы взрыва
        // Звук взрыва (без урона)
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);
        // Частицы взрыва (EXPLOSION_EMITTER для более заметного эффекта)
        ((ServerLevel) level).sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 1, 0.0D, 0.0D, 0.0D, 0.0D);

        // 5. Удаляем сам блок шахтёрского заряда (если он еще не был удален)
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());


    }

    /**
     * Вспомогательный метод для получения всех позиций блоков в сферическом радиусе.
     * Возвращает Set<BlockPos> для обеспечения уникальности позиций.
     *
     * @param center Центр сферы.
     * @param radius Радиус сферы.
     * @return Множество позиций блоков в сфере.
     */
    private Set<BlockPos> getBlocksInSphere(BlockPos center, int radius) {
        return BlockPos.betweenClosedStream(
                        center.offset(-radius, -radius, -radius),
                        center.offset(radius, radius, radius))
                .filter(p -> center.distSqr(p) <= radius * radius) // Фильтруем, чтобы получить только блоки внутри сферы
                .map(BlockPos::immutable) // Преобразуем mutable BlockPos в immutable для безопасности
                .collect(Collectors.toSet()); // Собираем в Set
    }

    //ЭФФЕКТЫ ВЗРЫВНОЙ ВОЛНЫ---------------------------------------------------------------------------------------------------------
    private void spawnExplosionWave(ServerLevel level, BlockPos explosionPos) {
        int duration = 120; //ВРЕМЯ ЖИЗНИ ЭФФЕКТА
        scheduleExplosionWave(level, explosionPos, duration, 0);}
    private void scheduleExplosionWave(ServerLevel level, BlockPos pos, int remainingTicks, int currentTick) {
        if (remainingTicks <= 0) {return;}
        if (currentTick % 5 == 0) {
            spawnExplosionWaveParticles(level, pos);}
        level.getServer().tell(new net.minecraft.server.TickTask(
                level.getServer().getTickCount() + 1,
                () -> scheduleExplosionWave(level, pos, remainingTicks - 1, currentTick + 1)));}
    private void spawnExplosionWaveParticles(ServerLevel level, BlockPos pos) {
        double centerX = pos.getX() + 0.5;
        double centerY = pos.getY() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        int particleCount = 1 + level.random.nextInt(3);  //КОЛИЧЕСТВО ЧАСТИЦ (ОТ 5 ДО 8)
        for (int i = 0; i < particleCount; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 2.0;
            double offsetZ = (level.random.nextDouble() - 0.5) * 2.0;
            double offsetY = level.random.nextDouble() * 0.5;
            double velocityX = (level.random.nextDouble() - 0.5) * 0.1;
            double velocityY = 0.1 + level.random.nextDouble() * 0.1;
            double velocityZ = (level.random.nextDouble() - 0.5) * 0.1;
            level.sendParticles(
                    ModParticleTypes.EXPLOSION_WAVE.get(), //ТИП ЧАСТИЦЫ
                    centerX + offsetX,
                    centerY + offsetY,
                    centerZ + offsetZ,
                    1, //КОЛИЧЕСТВО ГРУПП ЧАСТИЦ
                    velocityX,
                    velocityY,
                    velocityZ,
                    0.0);}} //ОБЩАЯ СКОРОСТЬ ЧАСТИЦ



}
