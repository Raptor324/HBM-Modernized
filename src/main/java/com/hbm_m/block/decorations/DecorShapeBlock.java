package com.hbm_m.block.decorations;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Деко-блок с OBJ-моделью и коллизией по форме модели (порт DecoBlock-семейства
 * 1.7.10: toaster, tape_recorder, puter, steel_pole, steel_roof).
 *
 * <p>Базовая форма задаётся для FACING = NORTH; при {@code rotate} хитбокс
 * вращается вместе с моделью (поворот вокруг центра блока). Для блоков без
 * ориентации {@code hasFacing = false} — свойство не добавляется, лишние
 * свойства в NBT структур игнорируются ванилью.</p>
 */
public class DecorShapeBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final VoxelShape base;
    private final boolean rotate;
    private final Map<Direction, VoxelShape> cache = new EnumMap<>(Direction.class);

    /**
     * FACING добавляется всегда (createBlockStateDefinition вызывается из
     * конструктора Block до инициализации полей — ветвление там невозможно),
     * у "неориентированных" блоков повороты просто ни на что не влияют.
     */
    public DecorShapeBlock(Properties props, VoxelShape base, boolean rotate, boolean hasFacing) {
        super(props);
        this.base = base;
        this.rotate = rotate;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // панель/модель прижимается к стороне, смотрящей на игрока (как meta 2-5 в оригинале)
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapeFor(state);
    }

    private VoxelShape shapeFor(BlockState state) {
        if (!rotate) {
            return base;
        }
        Direction facing = state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
        return cache.computeIfAbsent(facing, f -> rotateShape(base, f));
    }

    /** Поворот боксов формы: база — NORTH, поворот по часовой при взгляде сверху. */
    public static VoxelShape rotateShape(VoxelShape base, Direction facing) {
        if (facing == Direction.NORTH) {
            return base;
        }
        List<AABB> boxes = base.toAabbs();
        VoxelShape result = Shapes.empty();
        for (AABB b : boxes) {
            double x0 = b.minX, x1 = b.maxX, z0 = b.minZ, z1 = b.maxZ;
            double nx0, nx1, nz0, nz1;
            switch (facing) {
                case SOUTH -> { nx0 = 1 - x1; nx1 = 1 - x0; nz0 = 1 - z1; nz1 = 1 - z0; }
                case EAST  -> { nx0 = 1 - z1; nx1 = 1 - z0; nz0 = x0; nz1 = x1; }
                case WEST  -> { nx0 = z0; nx1 = z1; nz0 = 1 - x1; nz1 = 1 - x0; }
                default -> { nx0 = x0; nx1 = x1; nz0 = z0; nz1 = z1; }
            }
            result = Shapes.or(result, Shapes.box(nx0, b.minY, nz0, nx1, b.maxY, nz1));
        }
        return result;
    }
}
