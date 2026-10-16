package com.hbm_m.blockentity.bomb;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.bomb.VolcanoCoreBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.projectile.ShrapnelEntity;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm_m.explosion.vanillant.standard.BlockMutatorSetBlock;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm_m.particle.explosions.ServerExplosionParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Порт {@code BlockVolcano.TileEntityVolcanoCore} (1.7.10) — живое ядро вулкана.
 *
 * Каждые 10 тиков (кроме тлеющего): взрывами прокладывает вертикальный магмовый
 * канал от бедрока к ядру (аналог {@code ExAttrib.LAVA_V/LAVA_R}), поднимает магму,
 * плюётся лавовыми сгустками ({@link ShrapnelEntity}), дымит и поддерживает куб
 * лавы 3×3×3 вокруг себя. Тлеющее ядро вместо каналов взрывает магмовую камеру
 * и плавит поверхность. По своему таймеру растущее ядро поднимается на блок
 * вверх (до y&lt;200), гаснущее — застывает в лаву.
 *
 * Частота обновления (оригинал): гаснущее статичное — раз в час (72 000 тиков),
 * растущие — 288 тиков (250 раз в час), остальные — каждые 10 тиков.
 */
public class VolcanoCoreBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity {

    private int volcanoTimer;

    public VolcanoCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VOLCANO_CORE_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, VolcanoCoreBlockEntity be) {
        if (!level.isClientSide) {
            be.serverTick(level, pos, state);
        }
    }

    private void serverTick(Level level, BlockPos pos, BlockState state) {
        this.volcanoTimer++;
        int type = state.getValue(VolcanoCoreBlock.VOLCANO_TYPE);

        if (this.volcanoTimer % 10 == 0) {
            // Вертикальный канал есть у всех, кроме тлеющего
            if (type != VolcanoCoreBlock.TYPE_SMOLDERING) {
                this.blastMagmaChannel(level, pos);
                this.raiseMagma(level, pos);
            }
            if (type == VolcanoCoreBlock.TYPE_SMOLDERING) {
                this.blastMagmaChamber(level, pos, 15);
            }
            if (type == VolcanoCoreBlock.TYPE_SMOLDERING) {
                this.meltSurface(level, pos, 50, 50, 10);
            }
            if (type != VolcanoCoreBlock.TYPE_SMOLDERING) {
                this.spawnBlobs(level, pos);
                this.spawnSmoke(level, pos);
            }
            // Куб лавы 3×3×3 вокруг ядра
            this.surroundLava(level, pos);
        }

        if (this.volcanoTimer >= getUpdateRate(type)) {
            this.volcanoTimer = 0;
            this.setChanged();

            if (shouldGrow(type, pos)) {
                level.setBlock(pos.above(), state, 3);
                level.setBlock(pos, this.getLavaState(), 3);
            } else if (isExtinguishing(type)) {
                level.setBlock(pos, this.getLavaState(), 3);
            }
        }
    }

    private boolean isRadioactive() {
        return this.getBlockState().getBlock() == ModBlocks.VOLCANO_RAD_CORE.get();
    }

    private BlockState getLavaState() {
        return (this.isRadioactive() ? ModBlocks.RAD_LAVA_BLOCK : ModBlocks.VOLCANIC_LAVA_BLOCK).get().defaultBlockState();
    }

    private static boolean shouldGrow(int type, BlockPos pos) {
        return (type == VolcanoCoreBlock.TYPE_GROWING_ACTIVE || type == VolcanoCoreBlock.TYPE_GROWING_EXTINGUISHING)
                && pos.getY() < 200;
    }

    private static boolean isExtinguishing(int type) {
        return type == VolcanoCoreBlock.TYPE_STATIC_EXTINGUISHING || type == VolcanoCoreBlock.TYPE_GROWING_EXTINGUISHING;
    }

    private static int getUpdateRate(int type) {
        return switch (type) {
            case VolcanoCoreBlock.TYPE_STATIC_EXTINGUISHING -> 60 * 60 * 20; // раз в час
            case VolcanoCoreBlock.TYPE_GROWING_ACTIVE, VolcanoCoreBlock.TYPE_GROWING_EXTINGUISHING -> 60 * 60 * 20 / 250; // 250 раз в час
            default -> 10;
        };
    }

    /**
     * 1.7.10 {@code blastMagmaChannel}: два взрыва с лавой вместо разрушений —
     * столб к ядру (7F) и взрыв от бедрока до ядра (10F).
     */
    private void blastMagmaChannel(Level level, BlockPos pos) {
        this.volcanoExplosion(level,
                pos.getX() + 0.5D, pos.getY() + level.random.nextInt(15) + 1.5D, pos.getZ() + 0.5D, 7.0F);
        this.volcanoExplosion(level,
                pos.getX() + 0.5D + level.random.nextGaussian() * 3, level.random.nextInt(pos.getY() + 1), pos.getZ() + 0.5D + level.random.nextGaussian() * 3, 10.0F);
    }

    /** 1.7.10 {@code blastMagmaChamber}: два взрыва вокруг ядра — на полном и половинном радиусе. */
    private void blastMagmaChamber(Level level, BlockPos pos, double size) {
        for (int i = 0; i < 2; i++) {
            double dist = size / (double) (i + 1);
            this.volcanoExplosion(level,
                    pos.getX() + 0.5D + level.random.nextGaussian() * dist,
                    pos.getY() + 0.5D + level.random.nextGaussian() * dist,
                    pos.getZ() + 0.5D + level.random.nextGaussian() * dist, 7.0F);
        }
    }

    /** Тихий взрыв без дропа: разрушенные блоки заменяются лавой (аналог ExAttrib LAVA_V/LAVA_R). */
    private void volcanoExplosion(Level level, double x, double y, double z, float power) {
        ExplosionVNT explosion = new ExplosionVNT(level, x, y, z, power);
        explosion.setBlockAllocator(new BlockAllocatorStandard(16));
        explosion.setBlockProcessor(new BlockProcessorStandard().setNoDrop()
                .withBlockEffect(new BlockMutatorSetBlock(this::getLavaState)));
        explosion.explode();
    }

    /**
     * 1.7.10 {@code meltSurface}: случайные (гауссово) блоки поверхности в радиусе —
 * твёрдые превращает в лаву, нетвёрдые (ниже обсидиана по прочности) — в воздух.
     */
    private void meltSurface(Level level, BlockPos pos, int count, double radius, double depth) {
        for (int i = 0; i < count; i++) {
            int x = (int) Math.floor(pos.getX() + level.random.nextGaussian() * radius);
            int z = (int) Math.floor(pos.getZ() + level.random.nextGaussian() * radius);
            // Гауссово распределение: поверхность плавится чаще, глубина — реже
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 1
                    - (int) Math.floor(Math.abs(level.random.nextGaussian() * depth));
            BlockPos target = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(target);
            if (!state.isAir() && state.getBlock().getExplosionResistance() < Blocks.OBSIDIAN.getExplosionResistance()) {
                level.setBlock(target, state.isSolidRender(level, target) ? this.getLavaState() : Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    /** 1.7.10 {@code raiseMagma}: поднимает уровень лавы на случайной позиции в радиусе 10. */
    private void raiseMagma(Level level, BlockPos pos) {
        BlockPos target = new BlockPos(
                pos.getX() - 10 + level.random.nextInt(21),
                pos.getY() + level.random.nextInt(11),
                pos.getZ() - 10 + level.random.nextInt(21));
        BlockState lava = this.getLavaState();
        if (level.getBlockState(target).isAir() && level.getBlockState(target.below()).is(lava.getBlock())) {
            level.setBlock(target, lava, 3);
        }
    }

    /** 1.7.10 {@code surroundLava}: куб лавы 3×3×3 вокруг ядра (кроме самого ядра). */
    private void surroundLava(Level level, BlockPos pos) {
        BlockState lava = this.getLavaState();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    if (i != 0 || j != 0 || k != 0) {
                        level.setBlock(pos.offset(i, j, k), lava, 3);
                    }
                }
            }
        }
    }

    /** 1.7.10 {@code spawnBlobs}: три лавовых сгустка ({@code EntityShrapnel} setVolcano/setRadVolcano). */
    private void spawnBlobs(Level level, BlockPos pos) {
        for (int i = 0; i < 3; i++) {
            ShrapnelEntity frag = new ShrapnelEntity(ModEntities.SHRAPNEL.get(), level);
            frag.setPos(pos.getX() + 0.5D, pos.getY() + 1.5D, pos.getZ() + 0.5D);
            frag.setDeltaMovement(
                    level.random.nextGaussian() * 0.2D,
                    1.0D + level.random.nextDouble(),
                    level.random.nextGaussian() * 0.2D);
            if (this.isRadioactive()) {
                frag.setRadVolcano(true);
            } else {
                frag.setVolcano(true);
            }
            level.addFreshEntity(frag);
        }
    }

    /**
     * 1.7.10 {@code spawnSmoke}: AuxParticle vanillaExt mode=volcano — большой столб
     * ванильного дыма (scale 100, life 200-250, motionY 2.5+, noClip) в точке y+10.
     * Здесь — всегда видимый LARGE_SMOKE с восходящей скоростью.
     */
    private void spawnSmoke(Level level, BlockPos pos) {
        ServerExplosionParticles.sendAlwaysVisible((ServerLevel) level,
                ParticleTypes.LARGE_SMOKE,
                pos.getX() + 0.5D, pos.getY() + 10, pos.getZ() + 0.5D,
                level.random.nextGaussian() * 0.2D,
                2.5D + level.random.nextDouble(),
                level.random.nextGaussian() * 0.2D);
    }

    // Таймер персистится через BaseHbmBlockEntity.writeNbtData/readNbtData — без версионных гейтов.

    @Override
    protected void writeNbtData(CompoundTag tag, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        tag.putInt("timer", this.volcanoTimer);
    }

    @Override
    protected void readNbtData(CompoundTag tag, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        this.volcanoTimer = tag.getInt("timer");
    }
}

