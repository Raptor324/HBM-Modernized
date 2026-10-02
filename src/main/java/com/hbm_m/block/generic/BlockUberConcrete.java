package com.hbm_m.block.generic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * 1:1 {@link com.hbm.blocks.generic.BlockUberConcrete} ({@code concrete_super}): altert per Zufallstick (Metadaten
 * 0-15, je hoeher desto seltener), zeigt ab 10/12/14/15 die Rissbilder {@code concrete_super_m0..m3} und zerfaellt am Ende
 * zu {@code concrete_super_broken} - faellt als Truemmer seitlich herab, wenn darunter und daneben Luft ist.
 */
public class BlockUberConcrete extends Block {

    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 15);

    public BlockUberConcrete(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        int meta = state.getValue(AGE);

        if (rand.nextInt(meta + 1) > 0)
            return;

        if (meta < 15) {
            world.setBlock(pos, state.setValue(AGE, meta + 1), 3);
        } else {
            world.removeBlock(pos, false);

            if (world.getBlockState(pos.below()).isAir()) {
                world.setBlock(pos, ModBlocks.CONCRETE_SUPER_BROKEN.get().defaultBlockState(), 3);
                return;
            }

            List<Integer> sides = new ArrayList<>();
            Collections.addAll(sides, 2, 3, 4, 5);
            Collections.shuffle(sides, new java.util.Random(rand.nextLong()));

            for (Integer i : sides) {
                Direction dir = Direction.from3DDataValue(i);
                BlockPos side = pos.relative(dir);
                if (world.getBlockState(side).isAir() && world.getBlockState(side.below()).isAir()) {
                    FallingBlockEntity debris = FallingBlockEntity.fall(world, side, ModBlocks.CONCRETE_SUPER_BROKEN.get().defaultBlockState());
                    debris.time = 2;
                    debris.dropItem = false;
                    debris.setHurtsEntities(2.0F, 40);
                    return;
                }
            }

            world.setBlock(pos, ModBlocks.CONCRETE_SUPER_BROKEN.get().defaultBlockState(), 3);
        }
    }
}
