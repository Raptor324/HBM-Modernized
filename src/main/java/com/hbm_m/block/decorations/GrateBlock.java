package com.hbm_m.block.decorations;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Порт {@code BlockGrate} (1.7.10): стальная решётка-панель высотой 1/8 блока.
 *
 * <p>Свойство {@code pos} повторяет метадату оригинала: 0-7 — высота панели
 * ({@code meta * 2px}), 8 — прижата к потолку, 9 — приклеена под блок снизу
 * (обе визуально совпадают с 7 и 0 соответственно).</p>
 */
public class GrateBlock extends Block {

    public static final IntegerProperty POS = IntegerProperty.create("pos", 0, 9);

    private static final VoxelShape[] SHAPES = new VoxelShape[10];
    static {
        for (int i = 0; i <= 7; i++) {
            double minY = i * 2.0 / 16.0;
            SHAPES[i] = Shapes.box(0, minY, 0, 1, minY + 2.0 / 16.0, 1);
        }
        // meta 8 (потолок) и 9 (под блоком) визуально идентичны 7 и 0
        SHAPES[8] = SHAPES[7];
        SHAPES[9] = SHAPES[0];
    }

    private final boolean wide;

    public GrateBlock(Properties props, boolean wide) {
        super(props);
        this.wide = wide;
        registerDefaultState(stateDefinition.any().setValue(POS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        int pos;
        if (face == Direction.DOWN) {
            pos = 7;
        } else if (face == Direction.UP) {
            pos = 0;
        } else {
            pos = (int) Math.floor(context.getClickLocation().y - context.getClickedPos().getY());
            pos = Math.max(0, Math.min(7, pos));
        }
        return defaultBlockState().setValue(POS, pos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES[state.getValue(POS)];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        // steel_grate_wide в оригинале просеивает предметы и орбы опыта вниз
        if (wide && ctx instanceof EntityCollisionContext ecc) {
            net.minecraft.world.entity.Entity e = ecc.getEntity();
            if ((e instanceof ItemEntity || e instanceof ExperienceOrb)
                    && e.getY() < pos.getY() + state.getValue(POS) * 0.125 + 0.375) {
                return Shapes.empty();
            }
        }
        return getShape(state, level, pos, ctx);
    }

    // meta 8 требует блока с просветом сверху, meta 9 — с просветом снизу (onNeighborBlockChange из оригинала)
    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        int p = state.getValue(POS);
        if (p == 8 && dir == Direction.UP && !hasHeadroom(level, neighborPos, true)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        if (p == 9 && dir == Direction.DOWN && !hasHeadroom(level, neighborPos, false)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, dir, neighborState, level, pos, neighborPos);
    }

    /** В оригинале мета 8/9 допустима только когда соседний блок не является полным кубом. */
    private static boolean hasHeadroom(LevelReader level, BlockPos neighborPos, boolean above) {
        BlockState s = level.getBlockState(neighborPos);
        return !s.isSolidRender(level, neighborPos);
    }
}
