package com.hbm_m.block.generic;

import com.hbm_m.platform.PlatformHooks;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code BlockOre} als {@code block_meteor_molten}: leuchtet (0.75), kuehlt bei einem Zufallstick zu
 * {@code block_meteor_cobble} ab (Zischen), setzt Wesen, die darueber laufen, 5 s in Brand und wird beim Abbauen
 * durch einen Spieler zu Lava. Ohne Behutsamkeit laesst er nichts fallen (Beutetabelle). Erbt das uebrige BlockOre-Verhalten.
 */
public class BlockMeteorMolten extends BlockOre {

    public BlockMeteorMolten(Properties properties) {
        super(properties.randomTicks());
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        world.setBlock(pos, ModBlocks.BLOCK_METEOR_COBBLE.get().defaultBlockState(), 3);
        world.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F,
                2.6F + (world.random.nextFloat() - world.random.nextFloat()) * 0.8F);
    }

    @Override
    public void stepOn(Level world, BlockPos pos, BlockState state, Entity entity) {
        PlatformHooks.setSecondsOnFire(entity, 5);
        super.stepOn(world, pos, state, entity);
    }

    /** Original {@code onBlockDestroyedByPlayer}: auch im Kreativmodus, vor den Drops (Block#destroy). */
    @Override
    public void destroy(net.minecraft.world.level.LevelAccessor world, BlockPos pos, BlockState state) {
        if (!world.isClientSide()) world.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
    }
}
