package com.hbm_m.block.machines;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.Bat9000BlockEntity;
import com.hbm_m.blockentity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BAT9000 — large-capacity variant of {@link MachineFluidTankBlock}. Reuses the placement/removal
 * handling and tick logic (inherited); the multiblock shape is its own 5x5x5 structure (see
 * {@link #defineStructure()}), and the registered BlockEntity type (and therefore capacity) differ.
 */
public class MachineBat9000Block extends MachineFluidTankBlock {

    public MachineBat9000Block(Properties properties) {
        super(properties);
    }

    /**
     * 5 wide x 5 deep x 5 tall. Bottom layer: controller in the exact center, fluid connectors
     * ('X') ringed diagonally around it, everything else plain filler. The 4 layers above are a
     * solid 5x5 block of plain filler parts.
     */
    @Override
    protected MultiblockStructureHelper defineStructure() {
        String[] layer0 = {
            "OXOXO",
            "XOOOX",
            "OOCOO",
            "XOOOX",
            "OXOXO"
        };

        String[] fillerLayer = {
            "OOOOO",
            "OOOOO",
            "OOOOO",
            "OOOOO",
            "OOOOO"
        };

        Map<Character, PartRole> roleMap = Map.of(
            'O', PartRole.DEFAULT,
            'C', PartRole.CONTROLLER,
            'X', PartRole.FLUID_CONNECTOR
        );

        Map<Character, Supplier<BlockState>> symbolMap = Map.of(
            'O', () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            'X', () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState()
        );

        return MultiblockStructureHelper.createFromLayersWithRoles(
            new String[][]{ layer0, fillerLayer, fillerLayer, fillerLayer, fillerLayer },
            symbolMap,
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            roleMap
        );
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new Bat9000BlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(@NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.BAT9000_BE.get(), MachineFluidTankBlockEntity::tick);
    }
}
