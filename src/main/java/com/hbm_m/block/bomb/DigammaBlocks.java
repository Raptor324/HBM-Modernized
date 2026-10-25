package com.hbm_m.block.bomb;

import com.hbm_m.util.ContaminationUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** {@code DigammaFlame} und {@code DigammaMatter} des Originals. */
public final class DigammaBlocks {

    private DigammaBlocks() {}

    /** 1:1 {@code DigammaFlame} ("Lingering Digamma"): Feuerdarstellung, nur auf fester Oberseite, verstrahlt mit Digamma. */
    public static class Flame extends Block {
        public Flame(Properties p) { super(p); }

        @Override
        public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
            if (entity instanceof LivingEntity living) {
                ContaminationUtil.contaminate(living, ContaminationUtil.HazardType.DIGAMMA, ContaminationUtil.ContaminationType.DIGAMMA, 0.05F);
            }
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
            return Shapes.empty();
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
            return Shapes.empty();
        }

        @Override
        public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
            return world.getBlockState(pos.below()).isFaceSturdy(world, pos.below(), Direction.UP);
        }

        @Override
        public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor world, BlockPos pos, BlockPos npos) {
            return canSurvive(state, world, pos) ? state : Blocks.AIR.defaultBlockState();
        }
    }

    /**
     * 1:1 {@code DigammaMatter}: frisst sich 10-50 Ticks nach dem Setzen in die Nachbarschaft (Manhattan-Abstand 1-2)
     * und verschwindet dabei selbst; nach sieben Generationen ist Schluss. Keine Kollision, zappelnde Umrisse.
     */
    public static class Matter extends Block {
        public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
        private static final java.util.Random RAND = new java.util.Random();

        public Matter(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(AGE, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(AGE);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
            return Shapes.empty();
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
            double px = 0.0625;
            return Shapes.box(RAND.nextInt(9) * px, RAND.nextInt(9) * px, RAND.nextInt(9) * px,
                    1.0 - RAND.nextInt(9) * px, 1.0 - RAND.nextInt(9) * px, 1.0 - RAND.nextInt(9) * px);
        }

        @Override
        public void onPlace(BlockState state, Level world, BlockPos pos, BlockState old, boolean moving) {
            if (!world.isClientSide) world.scheduleTick(pos, this, 10 + RAND.nextInt(40));
        }

        @Override
        public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
            int meta = state.getValue(AGE);
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            if (meta >= 7) return;
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    for (int k = -1; k <= 1; k++) {
                        int dist = Math.abs(i) + Math.abs(j) + Math.abs(k);
                        if (dist > 0 && dist < 3) {
                            BlockPos p = pos.offset(i, j, k);
                            if (!world.getBlockState(p).is(this)) world.setBlock(p, defaultBlockState().setValue(AGE, meta + 1), 3);
                        }
                    }
                }
            }
        }
    }
}
