package com.hbm_m.block.generic;

import com.hbm_m.platform.EffectHooks;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.particle.ModParticleTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@link com.hbm.blocks.generic.BlockOre} (1.7.10). Die Drops ({@code getItemDropped}/{@code quantityDropped}/
 * Glueck, {@code noFortune}, keine Behutsamkeit fuer {@code ore_oil}) stehen in den Loot-Tabellen
 * ({@code ModBlockLootTableProvider}); hier das Verhalten: Effekte beim Betreten, Partikel der Trinitit-/Muellbloecke
 * und das Nachruecken des Oelerzes. Das veraltete {@code rad}-Feld nutzt nur {@code ore_schrabidium} -> {@link BlockOreRad}.
 */
public class BlockOre extends Block {

    public BlockOre(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(world, pos, state, entity);

        if (entity instanceof LivingEntity living) {

            if (this == ModBlocks.FROZEN_DIRT.get()) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2 * 60 * 20, 2));
            }
            if (this == ModBlocks.BLOCK_TRINITITE.get()) {
                living.addEffect(new MobEffectInstance(EffectHooks.of(ModEffects.RADIATION), 30 * 20, 2));
            }
            if (this == ModBlocks.BLOCK_WASTE.get()) {
                living.addEffect(new MobEffectInstance(EffectHooks.of(ModEffects.RADIATION), 30 * 20, 2));
            }
            if (this == ModBlocks.WASTE_TRINITITE.get() || this == ModBlocks.WASTE_TRINITITE_RED.get()) {
                living.addEffect(new MobEffectInstance(EffectHooks.of(ModEffects.RADIATION), 5 * 20, 2));
            }
            if (this == ModBlocks.BRICK_JUNGLE_OOZE.get()) {
                living.addEffect(new MobEffectInstance(EffectHooks.of(ModEffects.RADIATION), 15 * 20, 9));
            }
            if (this == ModBlocks.BRICK_JUNGLE_MYSTIC.get()) {
                living.addEffect(new MobEffectInstance(EffectHooks.of(ModEffects.TAINT), 15 * 20, 2));
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        super.animateTick(state, world, pos, rand);
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        if (this == ModBlocks.BLOCK_TRINITITE.get() || this == ModBlocks.BLOCK_WASTE.get()) {
            world.addParticle(ModParticleTypes.TOWNAURA.get(), x + rand.nextFloat(), y + 1.0625, z + rand.nextFloat(), 0, 0, 0);
        }
        if ((this == ModBlocks.WASTE_TRINITITE.get() || this == ModBlocks.WASTE_TRINITITE_RED.get()) && rand.nextInt(5) == 0) {
            world.addParticle(ModParticleTypes.TOWNAURA.get(), x + rand.nextFloat(), y + 1.0625, z + rand.nextFloat(), 0, 0, 0);
        }
    }

    /** onNeighborBlockChange: liegt unter dem Oelerz ein leeres Oelerz, tauschen beide (das Oel "rutscht nach"). */
    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, world, pos, block, fromPos, isMoving);
        if (world.isClientSide) return;
        if (world.getBlockState(pos.below()).is(ModBlocks.ORE_OIL_EMPTY.get())) {
            world.setBlock(pos, ModBlocks.ORE_OIL_EMPTY.get().defaultBlockState(), 3);
            world.setBlock(pos.below(), ModBlocks.ORE_OIL.get().defaultBlockState(), 3);
        }
    }
}
