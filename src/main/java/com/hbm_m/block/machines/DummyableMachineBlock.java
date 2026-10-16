package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.interfaces.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Gemeinsame Basis fuer Maschinen, die im Original ein {@code BlockDummyable} sind: sie belegen
 * mehr als ein Feld, aber die ganze Logik sitzt im Kern.
 *
 * <p>Das ist keine Kosmetik. Die Groesse einer Maschine bestimmt, wieviel Platz eine Anlage
 * braucht und wo Rohre und Kabel andocken koennen - ein Waermetauscher mit vier diagonalen
 * Anschlusszellen laesst sich anders verkabeln als ein Einzelblock.</p>
 *
 * <p>Die Struktur kommt aus dem {@link com.hbm_m.multiblock.DummyableStructureBuilder}, der die
 * {@code getDimensions()}-, {@code getOffset()}- und {@code makeExtra()}-Angaben des Originals
 * uebersetzt. Unterklassen liefern sie in {@link #defineStructure()}.</p>
 */
public abstract class DummyableMachineBlock extends BaseEntityBlock implements IMultiblockController {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final MultiblockStructureHelper structureHelper;

    protected DummyableMachineBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(withDefaults(this.stateDefinition.any().setValue(FACING, Direction.NORTH)));
        this.structureHelper = defineStructure();
    }

    /** Baut die Struktur dieser Maschine (wird genau einmal im Konstruktor aufgerufen). */
    protected abstract MultiblockStructureHelper defineStructure();

    /** Unterklassen mit weiteren Zustandsmerkmalen setzen hier ihre Vorgaben. */
    protected BlockState withDefaults(BlockState state) {
        return state;
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return this.structureHelper;
    }

    @Override
    public PartRole getPartRole(BlockPos localOffset) {
        return structureHelper != null ? structureHelper.resolvePartRole(localOffset, this) : PartRole.DEFAULT;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        MultiblockStructureHelper helper = getStructureHelper();
        if (helper != null && !helper.isFullBlock(helper.getControllerOffset(), state.getValue(FACING))) {
            return Shapes.empty();
        }
        return Shapes.block();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            placeMultiblockStructure(level, pos, state);
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return super.canSurvive(state, level, pos) && canSurviveMultiblockPlacement(state, level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof com.hbm_m.blockentity.BaseMachineBlockEntity machine) {
                machine.dropInventoryContents();
            }
            getStructureHelper().destroyStructure(level, pos, state.getValue(FACING));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        MultiblockStructureHelper helper = getStructureHelper();
        return helper != null ? helper.generateShapeFromParts(state.getValue(FACING)) : Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        MultiblockStructureHelper helper = getStructureHelper();
        return helper != null
                ? helper.getSpecificPartShape(helper.getControllerOffset(), state.getValue(FACING))
                : Shapes.block();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
