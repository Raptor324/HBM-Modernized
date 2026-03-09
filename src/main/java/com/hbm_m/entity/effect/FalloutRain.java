package com.hbm_m.entity.effect;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.entity.logic.ChunkloadingEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Сущность радиоактивного "дождя" после ядерного взрыва.
 * Масштаб (радиус) синхронизируется с клиентом для рендера.
 * Периодически размещает блоки nuclear_fallout в радиусе.
 */
public class FalloutRain extends ChunkloadingEntity {

    private static final EntityDataAccessor<Integer> SCALE = SynchedEntityData.defineId(FalloutRain.class, EntityDataSerializers.INT);

    /** После этого времени сущность удаляется (примерно 20 минут). */
    private static final int MAX_AGE_TICKS = 20 * 60 * 20;
    /** Каждые N тиков размещаем порцию fallout. */
    private static final int FALLOUT_TICK_INTERVAL = 4;
    /** За один проход — сколько случайных позиций пробуем. */
    private static final int FALLOUT_PER_TICK = 12;

    public FalloutRain(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            updateChunkTicket();
            if (tickCount >= MAX_AGE_TICKS) {
                discard();
                return;
            }
            if (tickCount % FALLOUT_TICK_INTERVAL == 0) {
                tryPlaceFallout();
            }
        }
    }

    /** Размещает блоки nuclear_fallout в случайных точках в радиусе scale. */
    private void tryPlaceFallout() {
        Level level = level();
        int scale = getScale();
        if (scale < 1) return;

        BlockPos center = BlockPos.containing(getX(), getY(), getZ());
        for (int i = 0; i < FALLOUT_PER_TICK; i++) {
            double angle = level.random.nextDouble() * 2 * Math.PI;
            double r = scale * Math.sqrt(level.random.nextDouble());
            int dx = Mth.floor(r * Math.cos(angle));
            int dz = Mth.floor(r * Math.sin(angle));
            BlockPos at = center.offset(dx, 0, dz);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, at.getX(), at.getZ());
            BlockPos surface = new BlockPos(at.getX(), y, at.getZ());
            BlockState state = level.getBlockState(surface);
            BlockState above = level.getBlockState(surface.above());

            if (above.isAir() && (state.isSolidRender(level, surface) || state.is(ModBlocks.NUCLEAR_FALLOUT.get()))) {
                if (state.is(ModBlocks.NUCLEAR_FALLOUT.get())) {
                    int layers = state.getValue(SnowLayerBlock.LAYERS);
                    if (layers < 8) {
                        level.setBlock(surface, state.setValue(SnowLayerBlock.LAYERS, Math.min(8, layers + 1)), 3);
                    }
                } else if (state.getBlock() != Blocks.AIR && !state.getBlock().defaultBlockState().isAir()) {
                    level.setBlock(surface.above(), ModBlocks.NUCLEAR_FALLOUT.get().defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1), 3);
                }
            }
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SCALE, 1);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setScale(tag.getInt("Scale"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Scale", getScale());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    public void setScale(int scale) {
        this.entityData.set(SCALE, scale);
    }

    public int getScale() {
        int scale = this.entityData.get(SCALE);
        return scale == 0 ? 1 : scale;
    }
}
