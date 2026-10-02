package com.hbm_m.block.fluid;

import java.util.function.Supplier;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.generic.BlockSellafieldSlaked;
import com.hbm_m.util.ContaminationUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * 1:1-Port von {@code RadBlock} (verstrahlte Lava): wie die Vulkanlava, erstarrt aber zu
 * {@code sellafield_slaked} bzw. Sellafield-Erzen (Metadaten 5-7 = Farbstufe), bestrahlt mit 5 RAD
 * pro Beruehrung und verwandelt Uranerz in verbranntes Sellafield-Uran bzw. -Schrabidium.
 */
public class RadLavaBlock extends VolcanicBlock {

    public RadLavaBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (entity instanceof LivingEntity living) {
            ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.CREATIVE, 5F);
        }
    }

    private static BlockState shade(Block block, BlockPos pos, int meta) {
        BlockState s = BlockSellafieldSlaked.getStateForPos(block, pos);
        if (s.hasProperty(BlockSellafieldSlaked.COLOR_LEVEL)) s = s.setValue(BlockSellafieldSlaked.COLOR_LEVEL, Math.min(meta, 10));
        return s;
    }

    @Override
    public void onSolidify(Level level, BlockPos pos, int lavaCount, int basaltCount, RandomSource rand) {
        int r = rand.nextInt(400);
        Block above = level.getBlockState(pos.above(10)).getBlock();
        boolean canMakeGem = lavaCount + basaltCount == 6 && lavaCount < 3 && (above == ModBlocks.SELLAFIELD_SLAKED.get() || above == ModBlocks.RAD_LAVA_BLOCK.get());
        int meta = 5 + rand.nextInt(3);

        if (r < 2) level.setBlock(pos, shade(ModBlocks.ORE_SELLAFIELD_DIAMOND.get(), pos, meta), 3);
        else if (r == 2) level.setBlock(pos, shade(ModBlocks.ORE_SELLAFIELD_EMERALD.get(), pos, meta), 3);
        else if (r < 20 && canMakeGem) level.setBlock(pos, shade(ModBlocks.ORE_SELLAFIELD_RADGEM.get(), pos, meta), 3);
        else level.setBlock(pos, shade(ModBlocks.SELLAFIELD_SLAKED.get(), pos, meta), 3);
    }

    @Override
    public Block getBasaltForCheck() {
        return ModBlocks.SELLAFIELD_SLAKED.get();
    }

    @Override
    public BlockState getReaction(Level level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.getFluidState().is(FluidTags.WATER)) return Blocks.STONE.defaultBlockState();
        if (s.is(BlockTags.LOGS)) return ModBlocks.WASTE_LOG.get().defaultBlockState();
        if (s.is(BlockTags.PLANKS)) return ModBlocks.WASTE_PLANKS.get().defaultBlockState();
        if (s.is(BlockTags.LEAVES)) return Blocks.FIRE.defaultBlockState();
        if (s.getBlock() == Blocks.DIAMOND_ORE || s.getBlock() == Blocks.DEEPSLATE_DIAMOND_ORE) return ModBlocks.ORE_SELLAFIELD_RADGEM.get().defaultBlockState();
        if (s.getBlock() == ModBlocks.URANIUM_ORE.get() || s.getBlock() == ModBlocks.GNEISS_URANIUM_ORE.get() || s.getBlock() == ModBlocks.GNEISS_URANIUM_ORE.get()) {
            return level.random.nextInt(5) == 0 ? ModBlocks.ORE_SELLAFIELD_SCHRABIDIUM.get().defaultBlockState() : ModBlocks.ORE_SELLAFIELD_URANIUM_SCORCHED.get().defaultBlockState();
        }
        return null;
    }
}
