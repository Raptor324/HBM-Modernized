package com.hbm_m.block.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.ChargerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

/**
 * Port von {@code Charger} (1.7.10, Block-ID {@code charger}): ein wandmontiertes Ladegeraet.
 *
 * <p>Die Hitbox ist 1:1 aus {@code setBlockBoundsBasedOnState} uebernommen - ein flacher Kasten
 * zwischen y = 0,25 und y = 0,75, der sich an die Wand hinter der Blickrichtung schmiegt. Der
 * {@code default}-Zweig des Originals (freistehender Kasten in der Mitte) bleibt als Rueckfall.</p>
 */
public class ChargerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** Original {@code default}: freistehend, 5/16 bis 11/16 in beiden Achsen. */
    private static final VoxelShape SHAPE_FREE = Block.box(5.0D, 4.0D, 5.0D, 11.0D, 12.0D, 11.0D);
    /** Original Fall 2 (Metadaten 2): haengt an der Suedkante. */
    private static final VoxelShape SHAPE_SOUTH = Block.box(5.0D, 4.0D, 12.0D, 11.0D, 12.0D, 16.0D);
    /** Original Fall 3. */
    private static final VoxelShape SHAPE_NORTH = Block.box(5.0D, 4.0D, 0.0D, 11.0D, 12.0D, 4.0D);
    /** Original Fall 4. */
    private static final VoxelShape SHAPE_EAST = Block.box(12.0D, 4.0D, 5.0D, 16.0D, 12.0D, 11.0D);
    /** Original Fall 5. */
    private static final VoxelShape SHAPE_WEST = Block.box(0.0D, 4.0D, 5.0D, 4.0D, 12.0D, 11.0D);

    public ChargerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private static VoxelShape shapeFor(BlockState state) {
        if (!state.hasProperty(FACING)) return SHAPE_FREE;

        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST  -> SHAPE_EAST;
            case WEST  -> SHAPE_WEST;
            default    -> SHAPE_FREE;
        };
    }

    @Override
    @Deprecated
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    @Deprecated
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.CHARGER_BE.get(),
                (lvl, pos, st, be) -> ChargerBlockEntity.tick(lvl, pos, st, (ChargerBlockEntity) be));
    }
}
