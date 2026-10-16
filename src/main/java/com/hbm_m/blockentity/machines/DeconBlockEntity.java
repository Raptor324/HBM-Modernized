package com.hbm_m.blockentity.machines;

import java.util.List;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.particle.ModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Порт {@link com.hbm.tileentity.machine.TileEntityDecon} (1.7.10).
 */
public class DeconBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity {

    public DeconBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DECON_BE.get(), pos, blockState);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DeconBlockEntity blockEntity) {
        if (!level.isClientSide) {
            AABB box = new AABB(pos.getX() - 0.5, pos.getY(), pos.getZ() - 0.5,
                    pos.getX() + 1.5, pos.getY() + 2, pos.getZ() + 1.5);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box);

            for (LivingEntity entity : entities) {
                HbmLivingProps.incrementRadiation(entity, -0.5F);
            }
        } else {
            RandomSource random = level.getRandom();
            level.addParticle(
                    ModParticleTypes.TOWNAURA.get(),
                    pos.getX() + 0.125 + random.nextDouble() * 0.75,
                    pos.getY() + 1.1,
                    pos.getZ() + 0.125 + random.nextDouble() * 0.75,
                    0.0,
                    0.04,
                    0.0);
        }
    }
}
