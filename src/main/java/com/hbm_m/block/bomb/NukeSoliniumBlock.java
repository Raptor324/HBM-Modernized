package com.hbm_m.block.bomb;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.blockentity.bomb.NukeSoliniumBlockEntity;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.logic.EntitySoliniumExplosion;
import com.hbm_m.util.WorldUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Солиниевая бомба («синяя стирка»): удаляет органику, всё остальное — облучает.
 */
public class NukeSoliniumBlock extends NukeBaseBlock implements IBomb {

    public NukeSoliniumBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NukeSoliniumBlockEntity(pos, state);
    }

    @Override
    public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    @Override
    protected void explode(Level level, double x, double y, double z) {
        int radius;
        try {
            radius = Math.max(1, ModClothConfig.get().soliniumRadius);
        } catch (Exception e) {
            radius = 150;
        }
        if (!level.isClientSide) {
            EntitySoliniumExplosion explosion = EntitySoliniumExplosion.statFac(level, x, y, z, radius);
            WorldUtil.loadAndSpawnEntityInWorld(explosion);
        }
    }

    //? if > 1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<NukeSoliniumBlock> CODEC = simpleCodec(NukeSoliniumBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.Block> codec() { return CODEC; }
     *///?}
}
