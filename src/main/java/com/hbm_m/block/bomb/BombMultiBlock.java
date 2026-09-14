package com.hbm_m.block.bomb;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.blockentity.bomb.BombMultiBlockEntity;
import com.hbm_m.explosion.MultiBombExplosion;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Многоцелевая бомба: 4 заряда ТНТ по углам + 2 слота модификаторов
 * (порох/ТНТ — мощность, огнеснаряд — поджог, газовый пеллет — облако).
 */
public class BombMultiBlock extends NukeBaseBlock implements IBomb {

    public BombMultiBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BombMultiBlockEntity(pos, state);
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Override
    protected void explode(Level level, double x, double y, double z) {
        if (level instanceof net.minecraft.server.level.ServerLevel server) {
            MultiBombExplosion.detonate(server, x, y, z, 0, 0);
        }
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos) {
        if (level.isClientSide) return BombReturnCode.UNDEFINED;
        if (level.getBlockEntity(pos) instanceof BombMultiBlockEntity bomb && bomb.isReady()) {
            int type2 = bomb.return2type();
            int type5 = bomb.return5type();
            bomb.clearContent();
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            if (level instanceof net.minecraft.server.level.ServerLevel server) {
                MultiBombExplosion.detonate(server,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, type2, type5);
            }
            return BombReturnCode.DETONATED;
        }
        return BombReturnCode.ERROR_MISSING_COMPONENT;
    }

    //? if > 1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<BombMultiBlock> CODEC = simpleCodec(BombMultiBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.Block> codec() { return CODEC; }
     *///?}
}
