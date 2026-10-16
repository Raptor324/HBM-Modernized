package com.hbm_m.block.fluid;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.util.ContaminationUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

/**
 * Порт {@code com.hbm.blocks.fluid.RadBlock} (1.7.10) — радиоактивная вулканическая
 * лава {@code rad_lava_block} (ядро {@code volcano_rad_core}, радиоактивные ракеты).
 * Застывает в слэкед-селлафилд с редкими селлафилдовыми рудами; контакт даёт
 * урон лавы (базовый класс) и 5 RAD/тик, как
 * {@code ContaminationUtil.contaminate(RADIATION, CREATIVE, 5F)} оригинала.
 */
public class RadLavaBlock extends VolcanicLavaBlock {

    public RadLavaBlock(Supplier<Fluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    protected BlockState getReaction(LevelReader level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.DIAMOND_ORE)) return ModBlocks.ORE_SELLAFIELD_RADGEM.get().defaultBlockState();
        // 1.7.10: ore_uranium/ore_gneiss_uranium → ore_sellafield_schrabidium (1/5)
        // или ore_sellafield_uranium_scorched; урановые руды в этом порте не портированы.
        return super.getReaction(level, pos);
    }

    @Override
    protected Block getRockForCheck() {
        return ModBlocks.SELLAFIELD_SLAKED.get();
    }

    /** 1.7.10 {@code RadBlock#onEntityCollidedWithBlock}: 5 RAD за тик контакта. */
    @Override
    protected void onEntityInsideRad(Level level, LivingEntity living) {
        ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.RADIATION,
                ContaminationUtil.ContaminationType.CREATIVE, 5.0F);
    }

    /**
     * 1.7.10 {@code RadBlock#onSolidify}: селлафилд; редкие руды — алмаз (r<2),
     * изумруд (r==2), радгем (r<20 при опоре из слэкеда/радиолавы).
     */
    @Override
    protected void onSolidify(ServerLevel level, BlockPos pos, int lavaCount, int rockCount, RandomSource random) {
        int r = random.nextInt(400);
        BlockState above = level.getBlockState(pos.above(10));
        boolean canMakeGem = lavaCount + rockCount == 6 && lavaCount < 3
                && (above.is(getRockForCheck()) || above.is(this));
        if (r < 2) level.setBlock(pos, ModBlocks.ORE_SELLAFIELD_DIAMOND.get().defaultBlockState(), 3);
        else if (r == 2) level.setBlock(pos, ModBlocks.ORE_SELLAFIELD_EMERALD.get().defaultBlockState(), 3);
        else if (r < 20 && canMakeGem) level.setBlock(pos, ModBlocks.ORE_SELLAFIELD_RADGEM.get().defaultBlockState(), 3);
        else level.setBlock(pos, ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState(), 3);
    }
}

