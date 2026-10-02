package com.hbm_m.block.fluid;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1-Port von {@code GenericFluidBlock} (hier: Schwefelsaeure): Gegenstaende bleiben stehen,
 * zischen und nehmen alle 20 Ticks 10 % des Schadens, Lebewesen werden im Fall gebremst und
 * bekommen den vollen Schaden. Die Errungenschaft fuer den aufgeloesten Schleimball
 * ({@code achSulfuric}) gibt es im Port noch nicht.
 */
public class GenericFluidBlock extends HbmFluidBlock {

    private BiFunction<Level, Entity, DamageSource> damageSource;
    private float damage;

    public GenericFluidBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    public GenericFluidBlock setDamage(BiFunction<Level, Entity, DamageSource> source, float amount) {
        this.damageSource = source;
        this.damage = amount;
        return this;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (damageSource == null) return;

        if (entity instanceof ItemEntity) {
            entity.setDeltaMovement(Vec3.ZERO);
            if (entity.tickCount % 20 == 0 && !level.isClientSide) {
                entity.hurt(damageSource.apply(level, entity), damage * 0.1F);
            }
            if (entity.tickCount % 5 == 0) {
                level.addParticle(ParticleTypes.CLOUD, entity.getX(), entity.getY(), entity.getZ(), 0.0, 0.0, 0.0);
            }
        } else {
            Vec3 m = entity.getDeltaMovement();
            if (m.y < -0.2) entity.setDeltaMovement(m.x, m.y * 0.5, m.z);
            if (!level.isClientSide) entity.hurt(damageSource.apply(level, entity), damage);
        }

        if (entity.tickCount % 5 == 0) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2F, 1F);
        }
    }
}
