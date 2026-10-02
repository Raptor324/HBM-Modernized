package com.hbm_m.block.generic;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.generic.FissureBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * 1:1 {@code BlockFissure} ({@code ore_volcano}): Grundgestein mit gluehender Ueberlagerung, unzerstoerbar, leuchtet.
 * Zufallsticks setzen darueber Vulkanlava ({@code CRATER} = Meta 1: radioaktive Lava); das Blockentity bietet nach
 * oben unbegrenzt Lava an.
 */
public class FissureBlock extends BaseEntityBlock {

    public static final BooleanProperty CRATER = BooleanProperty.create("crater");

    public FissureBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CRATER, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CRATER);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        BlockPos up = pos.above();
        if (world.getBlockState(up).canBeReplaced()) {
            world.setBlock(up, (state.getValue(CRATER) ? ModBlocks.RAD_LAVA_BLOCK.get() : ModBlocks.VOLCANIC_LAVA_BLOCK.get()).defaultBlockState(), 3);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FissureBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.FISSURE.get(), FissureBlockEntity::serverTick);
    }
}
