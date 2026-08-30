package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Большой радар — мультиблок 3×3×5 + энергоконнекторы по сторонам основания
 * (порт {@code MachineRadarLarge}, dimensions {@code {4,0,1,1,1,1}}).
 */
public class MachineLargeRadarBlock extends MachineRadarBlock {

    public MachineLargeRadarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper createStructureHelper() {
        String[] baseLayer = {
                "AEA",
                "ECE",
                "AEA"
        };

        String[] upperLayer = {
                "AAA",
                "AAA",
                "AAA"
        };

        Map<Character, PartRole> roleMap = Map.of(
                'A', PartRole.DEFAULT,
                'C', PartRole.CONTROLLER,
                'E', PartRole.ENERGY_CONNECTOR
        );

        Map<Character, Supplier<BlockState>> symbolMap = Map.of();

        return MultiblockStructureHelper.createFromLayersWithRoles(
                new String[][] { baseLayer, upperLayer, upperLayer, upperLayer, upperLayer },
                symbolMap,
                () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
                roleMap,
                null,
                null
        );
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getStructureHelper().generateShapeFromParts(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
}
