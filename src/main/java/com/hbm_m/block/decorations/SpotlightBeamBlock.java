package com.hbm_m.block.decorations;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code SpotlightBeam} (+{@code BlockBeamBase}): unsichtbarer Luftblock mit Licht 15. Die eingehenden
 * Lichtrichtungen (im Original 6 Bits im {@code TileEntityData}) stehen hier als sechs Blockzustands-Flags, die Farbe
 * (Gelb/Gruen/Blau, im Original die Metadaten) als {@link #COLOR}. Sind alle Richtungen aus, verschwindet der Strahl.
 */
public class SpotlightBeamBlock extends Block {

    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 2);
    public static final BooleanProperty[] DIRS = {
            BooleanProperty.create("down"), BooleanProperty.create("up"), BooleanProperty.create("north"),
            BooleanProperty.create("south"), BooleanProperty.create("west"), BooleanProperty.create("east") };

    public SpotlightBeamBlock(Properties properties) {
        super(properties);
        BlockState def = this.stateDefinition.any().setValue(COLOR, 0);
        for (BooleanProperty p : DIRS) def = def.setValue(p, false);
        this.registerDefaultState(def);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
        builder.add(DIRS);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    /** getDirections */
    public static List<Direction> getDirections(BlockState state) {
        List<Direction> directions = new ArrayList<>(6);
        if (!(state.getBlock() instanceof SpotlightBeamBlock)) return directions;
        for (Direction dir : Direction.values()) {
            if (state.getValue(DIRS[dir.get3DDataValue()])) directions.add(dir);
        }
        return directions;
    }

    /** setDirection: gibt die Zahl der verbleibenden Richtungen zurueck (0 = Strahl entfernen). */
    public static int setDirection(Level world, BlockPos pos, Direction dir, boolean state) {
        BlockState s = world.getBlockState(pos);
        if (!(s.getBlock() instanceof SpotlightBeamBlock)) return 0;
        s = s.setValue(DIRS[dir.get3DDataValue()], state);
        world.setBlock(pos, s, 2);
        int n = 0;
        for (BooleanProperty p : DIRS) if (s.getValue(p)) n++;
        return n;
    }

    // If a block is placed onto the beam, handle the new cutoff
    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!world.isClientSide && !newState.is(this)) {
            for (Direction dir : getDirections(state)) {
                SpotlightBlock.unpropagateBeam(world, pos, dir);
            }
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    // If a block in the beam path is removed, repropagate beam
    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block neighborBlock, BlockPos fromPos, boolean isMoving) {
        if (world.isClientSide) return;
        if (neighborBlock instanceof SpotlightBeamBlock) return;
        for (Direction dir : getDirections(state)) {
            SpotlightBlock.backPropagate(world, pos, dir, state.getValue(COLOR));
        }
    }
}
