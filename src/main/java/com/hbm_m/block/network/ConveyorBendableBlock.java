package com.hbm_m.block.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;

/**
 * Port of {@code com.hbm.blocks.network.BlockConveyorBendable} (1.7.10 Original) - adds the
 * curve-following travel math (pivot-point switchover from the primary direction to the
 * perpendicular one) used by the base Conveyor, Double, Triple and Express variants.
 */
public class ConveyorBendableBlock extends ConveyorBlockBase {

    public static final EnumProperty<ConveyorBend> BEND = EnumProperty.create("bend", ConveyorBend.class);

    public ConveyorBendableBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(BEND, ConveyorBend.STRAIGHT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BEND);
    }

    @Override
    public Direction getTravelDirection(Level level, BlockPos pos, Vec3 itemPos) {
        BlockState state = level.getBlockState(pos);
        Direction primary = state.getValue(FACING);
        ConveyorBend bend = state.getValue(BEND);

        if (bend == ConveyorBend.STRAIGHT) return primary;

        // dir: 0 = LEFT, 1 = RIGHT (matches the original's post-decrement pathDirection values).
        int dir = bend == ConveyorBend.LEFT ? 0 : 1;
        double ix = pos.getX() + 0.5;
        double iz = pos.getZ() + 0.5;
        Direction secondary = primary.getClockWise();

        ix -= -primary.getStepX() * 0.5 + secondary.getStepX() * (0.5 - dir);
        iz -= -primary.getStepZ() * 0.5 + secondary.getStepZ() * (0.5 - dir);

        double dX = Math.abs(itemPos.x - ix);
        double dZ = Math.abs(itemPos.z - iz);

        if (dX + dZ >= 1) {
            return dir == 0 ? secondary.getOpposite() : secondary;
        }

        return primary;
    }

    @Override
    protected BlockState onScrewSneak(Level level, BlockPos pos, BlockState state, Player player) {
        ConveyorBend bend = state.getValue(BEND);
        ConveyorBend next = switch (bend) {
            case STRAIGHT -> ConveyorBend.LEFT;
            case LEFT -> ConveyorBend.RIGHT;
            case RIGHT -> ConveyorBend.STRAIGHT;
        };
        return state.setValue(BEND, next);
    }
}
