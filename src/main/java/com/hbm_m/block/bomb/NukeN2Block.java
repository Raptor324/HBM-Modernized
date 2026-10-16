package com.hbm_m.block.bomb;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.blockentity.bomb.NukeN2BlockEntity;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.explosion.NuclearExplosionAPI;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Мина N²: огромный конвенциональный гриб без радиации и осадков.
 */
public class NukeN2Block extends NukeBaseBlock implements IBomb {

    public NukeN2Block(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NukeN2BlockEntity(pos, state);
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Override
    protected void explode(Level level, double x, double y, double z) {
        int radius;
        try {
            radius = Math.max(1, ModClothConfig.get().n2Radius);
        } catch (Exception e) {
            radius = 200;
        }
        NuclearExplosionAPI.startLargeNukeNoRad(level, x, y, z, radius);
    }

    //? if > 1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<NukeN2Block> CODEC = simpleCodec(NukeN2Block::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.Block> codec() { return CODEC; }
     *///?}
}
