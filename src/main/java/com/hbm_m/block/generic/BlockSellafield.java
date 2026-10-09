package com.hbm_m.block.generic;

import com.hbm_m.platform.EffectHooks;

import java.util.function.Supplier;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.radiation.ChunkRadiationManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code BlockSellafield} (1.7.10, "sellafield" Meta 0-5): heisses Sellafit. Die sechs Metas sind hier
 * eigene Bloecke sellafield_0..5 ({@link #level}); das Abkuehlen ersetzt den Block durch die naechstniedrigere
 * Stufe, Stufe 0 wird zu sellafield_slaked. Texturen: per RGB-Remap des Originals vorgerechnet
 * (sellafield_&lt;stufe&gt;_&lt;variante&gt;), die Positionsvariante waehlt der Blockstate.
 */
public class BlockSellafield extends Block {

    public static final int SELLAFITE_LEVELS = 6;
    /** Original: {@code this.rad = 0.5F}. */
    public static final float RAD = 0.5F;

    public final int level;
    private final Supplier<Block> cooler;

    /** @param cooler die naechstniedrigere Stufe (null bei Stufe 0 = sellafield_slaked) */
    public BlockSellafield(Properties properties, int level, Supplier<Block> cooler) {
        super(properties.randomTicks());
        this.level = level;
        this.cooler = cooler;
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        if (entity instanceof LivingEntity living)
            living.addEffect(new MobEffectInstance(EffectHooks.of(ModEffects.RADIATION), 30 * 20, level < 5 ? level : level * 2));
        super.stepOn(world, pos, state, entity);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        ChunkRadiationManager.incrementRad(world, pos.getX(), pos.getY(), pos.getZ(), RAD * (level + 1));

        if (rand.nextInt(level == 0 ? 25 : 15) == 0) {
            if (level > 0)
                world.setBlock(pos, cooler.get().defaultBlockState(), 2);
            else
                world.setBlock(pos, BlockSellafieldSlaked.getStateForPos(ModBlocks.SELLAFIELD_SLAKED.get(), pos), 3);
        }
    }
}
