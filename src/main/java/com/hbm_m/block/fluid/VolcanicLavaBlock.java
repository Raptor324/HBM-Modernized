package com.hbm_m.block.fluid;

import com.hbm_m.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;

import java.util.function.Supplier;

/**
 * Порт {@code com.hbm.blocks.fluid.VolcanicBlock} (1.7.10) — вулканическая лава
 * {@code volcanic_lava_block}, настоящая текучая жидкость (Architectury-флюид,
 * см. {@code ModFluids#VOLCANIC_LAVA}: tickDelay 20, slopeFindDistance 2 — медленный
 * лавоподобный растек).
 *
 * Поведение 1:1 с оригиналом:
 * <ul>
 * <li>{@code onNeighborBlockChange} — реакции на соседей: вода → камень, брёвна →
 *     {@code waste_log}, доски → {@code waste_planks}, листва → огонь,
 *     алмазная руда → {@code ore_basalt} (мета 3 = самородок, здесь блок
 *     {@code ore_basalt_gem});</li>
 * <li>{@code updateTick} (random tick) — незакреплённая лава (не источник или
 *     < 2 соседей-лавы, либо редко при < 5) застывает в базальт; на пересыщенных
 *     «коврах» с опорой сверху из базальта/лавы редко образуются базальтовые руды,
 *     включая редкий {@code ore_basalt_gem};</li>
 * <li>{@code Material.lava} — поджигает и жжёт сущностей в лаве.</li>
 * </ul>
 */
public class VolcanicLavaBlock extends LiquidBlock {

    public VolcanicLavaBlock(Supplier<Fluid> fluid, Properties properties) {
        // 1.20.1 LiquidBlock принимает Supplier<FlowingFluid>; 1.21.1 — сам инстанс флюида.
        //? if < 1.21.1 {
        super(() -> (FlowingFluid) fluid.get(), properties);
        //?} else {
        /*super((FlowingFluid) fluid.get(), properties);
        *///?}
    }

    /** Реакция на соседний блок: возвращает блок, которым он заменяется, или null. */
    protected BlockState getReaction(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.WATER)) return Blocks.STONE.defaultBlockState();
        if (state.is(BlockTags.LOGS)) return ModBlocks.WASTE_LOG.get().defaultBlockState();
        if (state.is(BlockTags.PLANKS)) return ModBlocks.WASTE_PLANKS.get().defaultBlockState();
        if (state.is(BlockTags.LEAVES)) return Blocks.FIRE.defaultBlockState();
        if (state.is(Blocks.DIAMOND_ORE)) return ModBlocks.ORE_BASALT_GEM.get().defaultBlockState();
        return null;
    }

    /** Блок-«опора» для образования руд (1.7.10 {@code getBasaltForCheck}). */
    protected Block getRockForCheck() {
        return ModBlocks.BASALT.get();
    }

    /**
     * 1.7.10 {@code VolcanicBlock#onNeighborBlockChange}: при обновлении соседа
     * проверяем все 6 направлений и заменяем реагирующие блоки.
     */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        for (Direction dir : Direction.values()) {
            BlockPos side = pos.relative(dir);
            BlockState reaction = getReaction(level, side);
            if (reaction != null) {
                level.setBlock(side, reaction, 3);
            }
        }
    }

    /**
     * 1.7.10 {@code VolcanicBlock#updateTick}: застывание лавы в базальт.
     * Условие оригинала: (!источник && лавы-соседей < 2 || редкий тик && лавы < 5)
     * && снизу не лава.
     */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int lavaCount = 0;
        int rockCount = 0;
        for (Direction dir : Direction.values()) {
            BlockState side = level.getBlockState(pos.relative(dir));
            if (side.is(this)) lavaCount++;
            else if (side.is(getRockForCheck())) rockCount++;
        }
        boolean isSource = state.getValue(LEVEL) == 0;
        if (((!isSource && lavaCount < 2) || (random.nextInt(5) == 0 && lavaCount < 5))
                && !level.getBlockState(pos.below()).is(this)) {
            onSolidify(level, pos, lavaCount, rockCount, random);
        }
    }

    /**
     * 1.7.10 {@code VolcanicBlock#onSolidify}: базальт; меты 0/1/2/4 оригинала —
     * сера/флюорит/асбест/молизит (r<5), мета 3 — вулканический самородок
     * (r<15 при пересыщенной лаве поверх базальта).
     */
    protected void onSolidify(ServerLevel level, BlockPos pos, int lavaCount, int rockCount, RandomSource random) {
        int r = random.nextInt(200);
        BlockState above = level.getBlockState(pos.above(10));
        boolean canMakeGem = lavaCount + rockCount == 6 && lavaCount < 3
                && (above.is(getRockForCheck()) || above.is(this));
        if (r < 2) level.setBlock(pos, ModBlocks.ORE_BASALT_SULFUR.get().defaultBlockState(), 3);
        else if (r == 2) level.setBlock(pos, ModBlocks.ORE_BASALT_FLUORITE.get().defaultBlockState(), 3);
        else if (r == 3) level.setBlock(pos, ModBlocks.ORE_BASALT_ASBESTOS.get().defaultBlockState(), 3);
        else if (r == 4) level.setBlock(pos, ModBlocks.ORE_BASALT_MOLYSITE.get().defaultBlockState(), 3);
        else if (r < 15 && canMakeGem) level.setBlock(pos, ModBlocks.ORE_BASALT_GEM.get().defaultBlockState(), 3);
        else level.setBlock(pos, ModBlocks.BASALT.get().defaultBlockState(), 3);
    }

    /** {@code Material.lava} оригинала: поджог + урон лавой у стоящих в блоке. */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            living.hurt(level.damageSources().lava(), 4.0F);
            com.hbm_m.platform.PlatformHooks.setSecondsOnFire(living, 15);
            onEntityInsideRad(level, living);
        }
    }

    /** Хук для радиоактивного подтипа ({@link RadLavaBlock}). */
    protected void onEntityInsideRad(Level level, LivingEntity living) {
    }

    /** 1.7.10 {@code randomDisplayTick}: редкие пузыри лавы и звук, капли снизу. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!level.getFluidState(pos.above()).isEmpty() || level.getBlockState(pos.above()).isSolidRender(level, pos.above())) {
            return;
        }
        if (random.nextInt(100) == 0) {
            double dx = pos.getX() + random.nextDouble();
            double dy = pos.getY() + 0.9D;
            double dz = pos.getZ() + random.nextDouble();
            level.addParticle(net.minecraft.core.particles.ParticleTypes.LAVA, dx, dy, dz, 0.0D, 0.0D, 0.0D);
            level.playLocalSound(dx, dy, dz, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false);
        }
        if (random.nextInt(200) == 0) {
            level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), SoundEvents.LAVA_AMBIENT, SoundSource.BLOCKS, 0.2F + random.nextFloat() * 0.2F, 0.9F + random.nextFloat() * 0.15F, false);
        }
    }

}

