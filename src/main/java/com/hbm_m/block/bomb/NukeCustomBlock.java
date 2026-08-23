package com.hbm_m.block.bomb;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.blockentity.bomb.NukeCustomBlockEntity;
import com.hbm_m.explosion.CustomNukeExplosion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Кастомная бомба: 27 произвольных слотов, тип и мощность взрыва
 * определяются содержимым (см. CustomNukeExplosion).
 */
public class NukeCustomBlock extends NukeBaseBlock implements IBomb {

    public NukeCustomBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NukeCustomBlockEntity(pos, state);
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Override
    protected void explode(Level level, double x, double y, double z) {
        // Радиус уже посчитан в explode(BlockPos); сюда попадаем только через базовый путь.
        if (level instanceof ServerLevel server) {
            CustomNukeExplosion.explodeCustom(server, x, y, z,
                    new CustomNukeExplosion.Yields(4F * 10, 0, 0, 0, 0));
        }
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos) {
        if (level.isClientSide) return BombReturnCode.UNDEFINED;
        if (level.getBlockEntity(pos) instanceof NukeCustomBlockEntity nuke && nuke.isReady()) {
            CustomNukeExplosion.Yields yields = CustomNukeExplosion.computeYields(nuke.slots);
            Containers.dropContents(level, pos, nuke);
            nuke.clearContent();
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            if (level instanceof ServerLevel server) {
                CustomNukeExplosion.explodeCustom(server,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, yields);
            }
            return BombReturnCode.DETONATED;
        }
        return BombReturnCode.ERROR_MISSING_COMPONENT;
    }

    //? if > 1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<NukeCustomBlock> CODEC = simpleCodec(NukeCustomBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.Block> codec() { return CODEC; }
     *///?}
}
