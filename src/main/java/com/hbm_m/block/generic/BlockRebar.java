package com.hbm_m.block.generic;

import javax.annotation.Nullable;

import com.hbm_m.api.network.GenNode;
import com.hbm_m.api.network.INetworkProvider;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.api.rebar.RebarNetwork;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.RebarBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.blocks.generic.BlockRebar} ({@code rebar}): Bewehrungsgitter, das ueber Rohre mit
 * fluessigem Beton gefuellt wird. Die Bewehrungsbloecke bilden ein eigenes Netz ({@link RebarNetwork}); Beton fuellt
 * immer die unterste Lage zuerst auf, bei 1000 wird der Block zu der beim Setzen gewaehlten Betonsorte (sonst
 * {@code concrete_rebar}). Die Stabgeometrie liegt als Blockmodell vor ({@code rebar} bzw. {@code rebar_simple}
 * fuer {@code ClientConfig.RENDER_REBAR_SIMPLE}), der Betonstand wird vom {@code RebarRenderer} gezeichnet.
 */
public class BlockRebar extends BaseEntityBlock {

    public BlockRebar(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RebarBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.REBAR_BE.get(), RebarBlockEntity::tick);
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, world, pos, block, fromPos, moving);
        BlockEntity tile = world.getBlockEntity(pos);
        if (!(tile instanceof RebarBlockEntity rebar)) return;

        rebar.hasConnection = false;

        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = world.getBlockEntity(pos.relative(dir));
            if (neighbor instanceof com.hbm_m.blockentity.machines.FluidDuctBlockEntity) {
                rebar.hasConnection = true;
                return;
            }
        }
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockRebar> CODEC = simpleCodec(BlockRebar::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}

    public static class RebarNode extends GenNode<RebarNetwork> {

        public RebarNode(INetworkProvider<RebarNetwork> provider, BlockPos... positions) {
            super(provider, positions);
        }

        @Override
        public RebarNode setConnections(NodeDirPos... connections) {
            super.setConnections(connections);
            return this;
        }
    }
}
