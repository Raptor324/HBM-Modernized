package com.hbm_m.block.bomb;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.blockentity.bomb.NukeFstbmbBlockEntity;
import com.hbm_m.entity.logic.EntityBalefireExplosion;
import com.hbm_m.util.WorldUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Бомба бейлфайра (FSTBMB): после активации тикает таймер (по умолчанию 15 минут)
 * и поджигает спираль бейлфайра.
 */
public class NukeFstbmbBlock extends NukeBaseBlock implements IBomb {

    public static final int DEFAULT_TIMER = 18000;

    public NukeFstbmbBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NukeFstbmbBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof com.hbm_m.blockentity.bomb.NukeFstbmbBlockEntity fstbmb) {
                fstbmb.serverTick();
            }
        };
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Override
    protected void explode(Level level, double x, double y, double z) {
        if (!level.isClientSide) {
            WorldUtil.loadAndSpawnEntityInWorld(EntityBalefireExplosion.statFac(level, x, y, z, 250));
        }
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos) {
        if (level.isClientSide) return BombReturnCode.UNDEFINED;
        if (level.getBlockEntity(pos) instanceof NukeFstbmbBlockEntity nuke && nuke.isReady()) {
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            explode(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            return BombReturnCode.DETONATED;
        }
        return BombReturnCode.ERROR_MISSING_COMPONENT;
    }

    //? if > 1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<NukeFstbmbBlock> CODEC = simpleCodec(NukeFstbmbBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.Block> codec() { return CODEC; }
     *///?}
}
