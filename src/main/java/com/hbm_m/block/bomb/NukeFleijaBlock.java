package com.hbm_m.block.bomb;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.blockentity.bomb.NukeFleijaBlockEntity;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.explosion.FleijaExplosionAPI;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Бомба F.L.E.I.J.A. (MK3): спиральное стирание террейна + циановое облако.
 */
public class NukeFleijaBlock extends NukeBaseBlock implements IBomb {

    public NukeFleijaBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NukeFleijaBlockEntity(pos, state);
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Override
    protected void explode(Level level, double x, double y, double z) {
        int radius;
        try {
            radius = Math.max(1, ModClothConfig.get().fleijaRadius);
        } catch (Exception e) {
            radius = 50;
        }
        com.hbm_m.platform.PlatformHooks.playSound(level, x, y, z, SoundEvents.GENERIC_EXPLODE,
                SoundSource.BLOCKS, 6.0F, (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);
        FleijaExplosionAPI.start(level, x, y, z, radius);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos) {
        if (level.isClientSide) return BombReturnCode.UNDEFINED;
        if (level.getBlockEntity(pos) instanceof NukeFleijaBlockEntity nuke && nuke.isReady()) {
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            explode(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            return BombReturnCode.DETONATED;
        }
        return BombReturnCode.ERROR_MISSING_COMPONENT;
    }

    //? if > 1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<NukeFleijaBlock> CODEC = simpleCodec(NukeFleijaBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.Block> codec() { return CODEC; }
     *///?}
}
