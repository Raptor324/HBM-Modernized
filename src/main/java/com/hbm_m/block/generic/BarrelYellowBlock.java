package com.hbm_m.block.generic;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.bomb.BlockDetonatable;
import com.hbm_m.entity.item.EntityTNTPrimedBase;
import com.hbm_m.explosion.ExplosionNukeGeneric;
import com.hbm_m.radiation.ChunkRadiationManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code YellowBarrel} (yellow_barrel = barrel_yellow, vitrified_barrel, frueher Port-ID barrel_vitrified):
 * strahlt alle 20 Ticks (gelb 5 RAD, verglast 0,5 RAD). Nur das gelbe Fass wird von Explosionen gezuendet
 * ({@code onBlockDestroyedByExplosion}); gezuendet: Giftblock oder Explosion 12, Verseuchung, Radongas, 35 RAD.
 * Port: FACING nur fuer die vorhandenen OBJ-Blockstates (barrel_*).
 */
public class BarrelYellowBlock extends BlockDetonatable {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 16.0D, 14.0D);

    public BarrelYellowBlock(Properties properties) {
        super(properties, 0, 0, 100, true, false);
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

    private boolean isYellow() {
        return this == ModBlocks.BARREL_YELLOW.get();
    }

    @Override
    public void wasExploded(Level world, BlockPos pos, Explosion explosion) {
        if (!isYellow()) return;
        super.wasExploded(world, pos, explosion);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        super.animateTick(state, world, pos, rand);
        // "townaura" -> Myzelpartikel
        world.addParticle(ParticleTypes.MYCELIUM, pos.getX() + rand.nextFloat() * 0.5F + 0.25F, pos.getY() + 1.1F,
                pos.getZ() + rand.nextFloat() * 0.5F + 0.25F, 0.0D, 0.0D, 0.0D);
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        super.tick(state, world, pos, rand);
        ChunkRadiationManager.incrementRad(world, pos.getX(), pos.getY(), pos.getZ(), isYellow() ? 5.0F : 0.5F);
        world.scheduleTick(pos, this, 20);
    }

    @Override
    public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, world, pos, oldState, moving);
        if (!world.isClientSide) world.scheduleTick(pos, this, 20);
    }

    @Override
    public void explodeEntity(Level world, double x, double y, double z, @Nullable EntityTNTPrimedBase entity) {
        int ix = Mth.floor(x), iy = Mth.floor(y), iz = Mth.floor(z);
        RandomSource rand = world.random;

        if (rand.nextInt(3) == 0) {
            world.setBlock(new BlockPos(ix, iy, iz), ModBlocks.TOXIC_BLOCK.get().defaultBlockState(), 3);
        } else {
            // createExplosion(entity, x, y, z, 12, true)
            world.explode(entity, x, y, z, 12.0F, Level.ExplosionInteraction.TNT);
        }
        ExplosionNukeGeneric.waste(world, ix, iy, iz, 35);

        for (int i = -5; i <= 5; i++) {
            for (int j = -5; j <= 5; j++) {
                for (int k = -5; k <= 5; k++) {
                    BlockPos p = new BlockPos(ix + i, iy + j, iz + k);
                    if (world.random.nextInt(5) == 0 && world.getBlockState(p).isAir())
                        world.setBlock(p, ModBlocks.GAS_RADON_DENSE.get().defaultBlockState(), 3);
                }
            }
        }
        ChunkRadiationManager.incrementRad(world, ix, iy, iz, 35);
    }
}
