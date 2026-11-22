package com.hbm_m.particle.custom;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.particle.explosions.AbstractExplosionParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 *  AGENT ORANGE - ОТРАВЛЕННАЯ ЧАСТИЦА
 *
 * ☠️ ОСОБЕННОСТИ:
 * - 🟠 ЯРКО-ОРАНЖЕВЫЙ цвет
 * - ВЫСОКАЯ ГРАВИТАЦИЯ (падает как жидкость)
 * - Наносит урон мобам при касании
 * - Коррумпирует растительность
 */
public class AgentOrangeParticle extends AbstractExplosionParticle {

    // ☠️ НАСТРОЙКИ ЭФФЕКТОВ
    private static final double MOB_EFFECT_RADIUS = 4.5;  // Увеличил радиус взаимодействия
    private static final int BLOCK_CORRUPT_INTERVAL = 5;   // Коррупция блоков каждые 5 тиков

    private int effectCooldown = 0;                        // Кулдаун между применениями эффектов
    private int blockCorruptTimer = 0;                     // Таймер коррупции блоков

    public AgentOrangeParticle(ClientLevel level, double x, double y, double z,
                               SpriteSet sprites, double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, sprites);

        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;

        //  ВРЕМЯ ЖИЗНИ: 60-100 тиков
        this.lifetime = 60 + this.random.nextInt(40);

        //  ВЫСОКАЯ ГРАВИТАЦИЯ (падает вниз)
        this.gravity = 0.15F;

        this.hasPhysics = false;

        //  РАЗМЕР: 0.6 - 1.8
        this.quadSize = 0.9F + this.random.nextFloat() * 1.2F;
// 🟠 ЦВЕТ: ТЁМНО-ОРАНЖЕВЫЙ (было светлее)
        this.rCol = 0.8F;                                   // Красный = 80% (было 100%)
        this.gCol = 0.4F + this.random.nextFloat() * 0.1F;  // Зелёный = 40-50% (было 50-60%)
        this.bCol = 0.0F;                                   // Синий = 0%


        //  ПРОЗРАЧНОСТЬ
        this.alpha = 0.9F;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }

        // ════════════════════════════════════════════════════════════════
        // ☠️ ПРИМЕНЯЕМ ЭФФЕКТЫ КАЖДЫЙ ТИК (без кулдауна!)
        // ════════════════════════════════════════════════════════════════

        checkMobCollision(); // ← Вызываем КАЖДЫЙ тик!

        // ☠️ Коррупция блоков (с кулдауном, чтобы не лагало)
        blockCorruptTimer++;
        if (blockCorruptTimer >= BLOCK_CORRUPT_INTERVAL) {
            corruptNearbyBlocks();
            blockCorruptTimer = 0;
        }

        // ════════════════════════════════════════════════════════════════
        //  ФИЗИКА
        // ════════════════════════════════════════════════════════════════

        float fadeProgress = (float) this.age / (float) this.lifetime;
        this.alpha = 0.9F * (1.0F - fadeProgress);

        this.quadSize *= 1.002F;

        this.yd -= this.gravity;

        //  Применяем движение
        this.x += this.xd;
        this.y += this.yd;
        this.z += this.zd;

        this.xd *= 0.98F;
        this.zd *= 0.98F;

        // ════════════════════════════════════════════════════════════════
//  ПРОВЕРКА СТОЛКНОВЕНИЯ С БЛОКАМИ (ПОСЛЕ движения и эффектов!)
// ════════════════════════════════════════════════════════════════

//  Проверяем блок НИЖЕ (на который упали)
        BlockPos belowPos = BlockPos.containing(this.x, this.y - 0.3, this.z);
        var belowState = this.level.getBlockState(belowPos);

//  Если под нами твёрдый блок И мы падаем вниз - исчезаем
        if (this.yd < 0 && !belowState.isAir() && belowState.isSolidRender(this.level, belowPos)) {
            // ☠️ Последняя коррупция перед исчезновением
            corruptNearbyBlocks();

            // 🔊 ЗВУК ПРИ КАСАНИИ ЗЕМЛИ
            this.level.playLocalSound(
                    this.x, this.y, this.z,
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS,
                    0.8F,   // Громкость
                    0.7F,   // Высота тона
                    false
            );

            MainRegistry.LOGGER.debug("[AgentOrange] Частица исчезла при касании блока: {}", belowState.getBlock());
            this.remove();
            return;
        }

    }


    /**
     * ☠️ ПРОВЕРКА СТОЛКНОВЕНИЯ С МОБАМИ
     *
     * Наносит урон и накладывает Wither 2 всем мобам в радиусе
     */
    private void checkMobCollision() {
        //  Проверяем, что мы на интегрированном сервере
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getSingleplayerServer() == null) {
            return; // Не работает на выделенных серверах (пока)
        }

        var server = mc.getSingleplayerServer();
        var serverLevel = server.getLevel(this.level.dimension());
        if (serverLevel == null) return;

        //  Получаем мобов из СЕРВЕРНОГО уровня (не клиентского!)
        List<LivingEntity> nearbyMobs = serverLevel.getEntitiesOfClass(
                LivingEntity.class,
                new net.minecraft.world.phys.AABB(
                        this.x - MOB_EFFECT_RADIUS, this.y - MOB_EFFECT_RADIUS, this.z - MOB_EFFECT_RADIUS,
                        this.x + MOB_EFFECT_RADIUS, this.y + MOB_EFFECT_RADIUS, this.z + MOB_EFFECT_RADIUS
                )
        );

        //  ВЫПОЛНЯЕМ НА СЕРВЕРНОМ ПОТОКЕ!
        if (!nearbyMobs.isEmpty()) {
            MainRegistry.LOGGER.debug("[AgentOrange] Найдено {} сущностей", nearbyMobs.size());

            server.execute(() -> {
                for (LivingEntity living : nearbyMobs) {
                    //  Проверяем invulnerableTime на СЕРВЕРЕ
                    if (living.invulnerableTime > 10) {
                        continue; // Пропускаем, если недавно получал урон
                    }

                    //  УРОН 4 сердца (8 HP) на СЕРВЕРНОЙ стороне
                    boolean damaged = living.hurt(serverLevel.damageSources().magic(), 8.0F);

                    if (damaged) {
                        MainRegistry.LOGGER.debug("[AgentOrange]  Нанесён урон: {}", living.getName().getString());

                        //  WITHER 2 на 10 секунд
                        MobEffectInstance wither = new MobEffectInstance(
                                MobEffects.WITHER,
                                200,  // 10 секунд
                                1,    // Уровень 2
                                false,
                                true,
                                true
                        );
                        living.addEffect(wither);

                        MainRegistry.LOGGER.debug("[AgentOrange]  Применён Wither 2");
                    }
                }
            });
        }
    }





    /**
     * ☠️ КОРРУПЦИЯ БЛОКОВ РЯДОМ С ЧАСТИЦЕЙ
     *
     * Превращает:
     * - Траву/землю/мицелий → DEAD_DIRT
     * - Листву → WASTE_LEAVES
     *
     * 🔧 ОПТИМИЗАЦИЯ: Только 1-2 слоя по вертикали для земли, +3 блока вверх для листвы
     */
    private void corruptNearbyBlocks() {
        if (!Minecraft.getInstance().hasSingleplayerServer()) return;

        BlockPos centerPos = BlockPos.containing(this.x, this.y, this.z);
        int horizontalRadius = 2;

        int blocksChecked = 0;
        int blocksCorrupted = 0;

        Minecraft mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        if (server == null) return;

        var serverLevel = server.getLevel(this.level.dimension());
        if (serverLevel == null) return;

        // ════════════════════════════════════════════════════════════════
        // 🟫 КОРРУПЦИЯ ЗЕМЛИ (текущий слой + 1 выше)
        // ════════════════════════════════════════════════════════════════
        for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                    blocksChecked++;
                    BlockPos checkPos = centerPos.offset(dx, dy, dz);

                    if (isCorruptibleGround(checkPos)) {
                        final BlockPos finalPos = checkPos.immutable();
                        server.execute(() -> {
                            boolean success = serverLevel.setBlock(
                                    finalPos,
                                    ModBlocks.DEAD_DIRT.get().defaultBlockState(),
                                    3
                            );
                            if (success) {
                                MainRegistry.LOGGER.debug("[AgentOrange] Земля заменена: {}", finalPos);
                               }
                        });
                        blocksCorrupted++;
                    }
                }
            }
        }

        // ════════════════════════════════════════════════════════════════
        // 🍂 УНИЧТОЖЕНИЕ ЛИСТВЫ (до 5 блоков вверх)
        // ════════════════════════════════════════════════════════════════
        for (int dx = -horizontalRadius; dx <= horizontalRadius; dx++) {
            for (int dy = 0; dy <= 5; dy++) { // ← Проверяем вверх до 5 блоков
                for (int dz = -horizontalRadius; dz <= horizontalRadius; dz++) {
                    blocksChecked++;
                    BlockPos checkPos = centerPos.offset(dx, dy, dz);



                    if (isLeafBlock(checkPos)) {
                        final BlockPos finalPos = checkPos.immutable();
                        server.execute(() -> {
                            boolean success = serverLevel.setBlock(
                                    finalPos,
                                    ModBlocks.WASTE_LEAVES.get().defaultBlockState(),
                                    3
                            );
                            if (success) {
                                MainRegistry.LOGGER.debug("[AgentOrange] Листва заменена: {}", finalPos);
                               }
                        });
                        blocksCorrupted++;
                    }
                }
            }
        }

        if (blocksCorrupted > 0) {
            MainRegistry.LOGGER.debug("[AgentOrange] Проверено блоков: {}, коррумпировано: {}", blocksChecked, blocksCorrupted);
        }
    }

    /**
     *  ПРОВЕРКА: можно ли коррупировать землю?
     */
    private boolean isCorruptibleGround(BlockPos pos) {
        var block = this.level.getBlockState(pos).getBlock();

        return block == Blocks.GRASS_BLOCK
                || block == Blocks.DIRT
                || block == Blocks.COARSE_DIRT
                || block == Blocks.DIRT_PATH
                || block == Blocks.ROOTED_DIRT
                || block == Blocks.FARMLAND
                || block == Blocks.MYCELIUM
                || block == Blocks.PODZOL;
    }

    /**
     * 🍂 ПРОВЕРКА: это листва?
     */
    private boolean isLeafBlock(BlockPos pos) {
        var block = this.level.getBlockState(pos).getBlock();

        return block == Blocks.OAK_LEAVES
                || block == Blocks.SPRUCE_LEAVES
                || block == Blocks.BIRCH_LEAVES
                || block == Blocks.JUNGLE_LEAVES
                || block == Blocks.ACACIA_LEAVES
                || block == Blocks.DARK_OAK_LEAVES
                || block == Blocks.MANGROVE_LEAVES
                || block == Blocks.CHERRY_LEAVES
                || block == Blocks.AZALEA_LEAVES
                || block == Blocks.FLOWERING_AZALEA_LEAVES;
    }

    public static class Provider extends AbstractExplosionParticle.Provider<AgentOrangeParticle> {
        public Provider(SpriteSet sprites) {
            super(sprites, AgentOrangeParticle::new);
        }
    }
}
