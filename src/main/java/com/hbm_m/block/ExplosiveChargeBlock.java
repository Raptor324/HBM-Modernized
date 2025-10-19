package com.hbm_m.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ExplosiveChargeBlock extends Block implements IDetonatable {

    // Более сильный взрыв для горного дела
    private static final float EXPLOSION_POWER = 15.0F;

    public ExplosiveChargeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onDetonate(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            // Удаляем блок
            level.removeBlock(pos, false);

            // Создаем мощный взрыв
            level.explode(
                    null,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    EXPLOSION_POWER,
                    Level.ExplosionInteraction.BLOCK
            );

            return true;
        }
        return false;
    }

}