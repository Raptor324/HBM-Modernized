package com.hbm_m.block.generic;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.bomb.BlockDetonatable;
import com.hbm_m.block.bomb.BlockTaint;
import com.hbm_m.entity.item.EntityTNTPrimedBase;
import com.hbm_m.explosion.ExplosionThermo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code RedBarrel} (red_barrel, pink_barrel, lox_barrel, taint_barrel): von Explosion/Feuer/Beschuss
 * gezuendet wird das Fass zum TNT-Koerper ({@link BlockDetonatable}, Lunte 100, zuendet bei Beruehrung).
 * Brennbare Faesser: Feuerwerte 2/15 und Zuendung durch Beschuss.
 * Port: FACING nur fuer die vorhandenen OBJ-Blockstates (barrel_*), ohne Einfluss auf die Logik.
 */
public class RedBarrelBlock extends BlockDetonatable {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Original: setBlockBounds(2f, 0, 2f, 14f, 1, 14f). */
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    public RedBarrelBlock(Properties properties, boolean flammable) {
        super(properties, flammable ? 2 : 0, flammable ? 15 : 0, 100, true, flammable);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public void explodeEntity(Level world, double x, double y, double z, @Nullable EntityTNTPrimedBase entity) {
        int ix = Mth.floor(x), iy = Mth.floor(y), iz = Mth.floor(z);

        if (this == ModBlocks.BARREL_RED.get() || this == ModBlocks.BARREL_PINK.get()) {
            // newExplosion(entity, x, y, z, 2.5F, true, true)
            world.explode(entity, x, y, z, 2.5F, true, Level.ExplosionInteraction.TNT);
        } else if (this == ModBlocks.BARREL_LOX.get()) {
            world.explode(entity, x, y, z, 1F, false, Level.ExplosionInteraction.NONE);
            ExplosionThermo.freezer(world, ix, iy, iz, 7);
        } else if (this == ModBlocks.BARREL_TAINT.get()) {
            world.explode(entity, x, y, z, 1F, false, Level.ExplosionInteraction.NONE);

            RandomSource rand = world.random;
            for (int i = 0; i < 100; i++) {
                int a = rand.nextInt(9) - 4 + ix;
                int b = rand.nextInt(9) - 4 + iy;
                int c = rand.nextInt(9) - 4 + iz;
                BlockPos p = new BlockPos(a, b, c);
                BlockState state = world.getBlockState(p);
                // isNormalCube() && !isAir
                if (state.isRedstoneConductor(world, p) && !state.isAir()) {
                    world.setBlock(p, ModBlocks.TAINT.get().defaultBlockState().setValue(BlockTaint.AGE, rand.nextInt(3) + 4), 2);
                }
            }
        }
    }
}
