package com.hbm_m.block.generic;

import com.hbm_m.hazard.HazardRegistry;
import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.particle.ModParticleTypes;
import com.hbm_m.radiation.ChunkRadiationManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт {@link com.hbm.blocks.generic.BlockHazard} (1.7.10).
 *
 * <p>Базовый класс для радиоактивных/опасных блоков. Реализует пер-тик эмиттер радиации
 * в чанк через свой собственный scheduled {@code tick} (каждые 20 тиков):
 * {@code rad = hazard × 0.1F} накачивается в ambient чанка через {@link ChunkRadiationManager#incrementRad}.
 * Чанк не хранит источник — пока блок существует и его scheduled-тики выполняются,
 * радиация добавляется; если блок сломать, эмиттер останавливается и накопленный ambient
 * естественным образом затухает (×0.99 − 0.05 за цикл).</p>
 *
 * <p>{@link ExtDisplayEffect} управляет визуальными частицами (RADFOG/SCHRAB/FLAMES/LAVAPOP).</p>
 */
public class BlockHazard extends Block {

    /** {@link com.hbm.blocks.generic.BlockHazard.ExtDisplayEffect} (1.7.10). */
    public enum ExtDisplayEffect {
        RADFOG,
        SPARKS,
        SCHRAB,
        FLAMES,
        LAVAPOP
    }

    /** 1.7.10 {@code tickRate(world) = 20} при rad > 0. */
    private static final int RAD_TICK_INTERVAL = 20;

    protected ExtDisplayEffect extEffect = null;
    private boolean beaconable = false;

    public BlockHazard(Properties properties) {
        super(properties);
    }

    public BlockHazard setDisplayEffect(ExtDisplayEffect effect) {
        this.extEffect = effect;
        return this;
    }

    public BlockHazard makeBeaconable() {
        this.beaconable = true;
        return this;
    }

    // В 1.20.1 ванильный BeaconBlock использует тег beacon_base_blocks вместо isBeaconBase (Forge IForgeBlock).
    // BlockHazard.makeBeaconable() выставляет флаг, а ModBlockTagProvider регистрирует блок в этом теге через датаген.
    public boolean isBeaconable() {
        return beaconable;
    }

    /**
     * Порт {@code BlockHazard#onBlockAdded} (1.7.10): вычисляет {@code rad = hazard × 0.1}
     * и планирует первый scheduled-tick.
     */
    // who wrote this???
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        float rad = currentRad();
        if (rad > 0F && !level.isClientSide) {
            level.scheduleTick(pos, this, RAD_TICK_INTERVAL);
        }
    }

    /**
     * Порт {@code BlockHazard#updateTick} (1.7.10): эмиттит {@code rad} в чанк и перепланирует себя.
     */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        float rad = currentRad();
        if (rad > 0F) {
            ChunkRadiationManager.incrementRad(level, pos.getX(), pos.getY(), pos.getZ(), rad);
            level.scheduleTick(pos, this, RAD_TICK_INTERVAL);
        }
    }

    /** {@code rad = HazardSystem.getHazardLevelFromStack(item) × 0.1F} (1.7.10 onBlockAdded). */
    protected float currentRad() {
        return HazardSystem.getHazardLevelFromStack(new ItemStack(this), HazardRegistry.RADIATION) * 0.1F;
    }

    /**
     * Порт {@code BlockHazard#randomDisplayTick} (1.7.10): диспетч частиц по {@link #extEffect}.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        super.animateTick(state, level, pos, rand);
        if (extEffect == null) {
            return;
        }
        switch (extEffect) {
            case RADFOG, SCHRAB, FLAMES -> sPart(level, pos, rand);
            case LAVAPOP -> level.addParticle(ParticleTypes.LAVA,
                    pos.getX() + rand.nextFloat(), pos.getY() + 1.1F, pos.getZ() + rand.nextFloat(),
                    0.0D, 0.0D, 0.0D);
            default -> { /* SPARKS — нет своих частиц в 1.7.10 */ }
        }
    }

    /**
     * Порт {@code BlockHazard#sPart} (1.7.10): частицы в соседних воздушных клетках.
     * RADFOG → TOWNAURA, SCHRAB → SCHRABFOG, FLAMES → FLAME+SMOKE.
     */
    private void sPart(Level level, BlockPos pos, RandomSource rand) {
        for (Direction dir : Direction.values()) {
            if (dir == Direction.DOWN && extEffect == ExtDisplayEffect.FLAMES) {
                continue;
            }
            BlockPos adjacent = pos.relative(dir);
            if (!level.getBlockState(adjacent).isAir()) {
                continue;
            }

            double ix = pos.getX() + 0.5D + dir.getStepX() + rand.nextDouble() * 3.0D - 1.5D;
            double iy = pos.getY() + 0.5D + dir.getStepY() + rand.nextDouble() * 3.0D - 1.5D;
            double iz = pos.getZ() + 0.5D + dir.getStepZ() + rand.nextDouble() * 3.0D - 1.5D;

            if (dir.getStepX() != 0) {
                ix = pos.getX() + 0.5D + dir.getStepX() * 0.5D + rand.nextDouble() * dir.getStepX();
            }
            if (dir.getStepY() != 0) {
                iy = pos.getY() + 0.5D + dir.getStepY() * 0.5D + rand.nextDouble() * dir.getStepY();
            }
            if (dir.getStepZ() != 0) {
                iz = pos.getZ() + 0.5D + dir.getStepZ() * 0.5D + rand.nextDouble() * dir.getStepZ();
            }

            if (extEffect == ExtDisplayEffect.RADFOG) {
                level.addParticle(ModParticleTypes.TOWNAURA.get(), ix, iy, iz, 0.0D, 0.0D, 0.0D);
            } else if (extEffect == ExtDisplayEffect.SCHRAB) {
                level.addParticle(ModParticleTypes.SCHRABFOG.get(), ix, iy, iz, 0.0D, 0.0D, 0.0D);
            } else if (extEffect == ExtDisplayEffect.FLAMES) {
                level.addParticle(ParticleTypes.FLAME, ix, iy, iz, 0.0D, 0.0D, 0.0D);
                level.addParticle(ParticleTypes.SMOKE, ix, iy, iz, 0.0D, 0.0D, 0.0D);
                level.addParticle(ParticleTypes.SMOKE, ix, iy, iz, 0.0D, 0.1D, 0.0D);
            }
        }
    }
}
