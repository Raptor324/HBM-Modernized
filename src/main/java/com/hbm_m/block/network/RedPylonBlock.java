package com.hbm_m.block.network;

import java.util.Map;
import java.util.function.Supplier;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.RedPylonBlockEntity;
import com.hbm_m.multiblock.MultiblockSideTuples;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Порт PylonRedWire (1.7.10): обычный (деревянный) и стальной пилон, SINGLE, радиус 25 м, башня 5 блоков.
 * Символьный мультиблок 1x1x5 (ядро C у земли, мачта P y=1..3, изолятор T y=4).
 */
public class RedPylonBlock extends RedPylonCoreBlock {

    private static final MultiblockStructureHelper STRUCTURE = defineStructure();

    private static MultiblockStructureHelper defineStructure() {
        String[] layer0 = {"C"};
        String[] layerP = {"P"};
        String[] layerT = {"T"};

        Map<Character, PartRole> roleMap = Map.of(
            'C', PartRole.CONTROLLER,
            'P', PartRole.DEFAULT,
            'T', PartRole.DEFAULT
        );

        Map<Character, VoxelShape> shapeMap = Map.of(
            'C', Block.box(4, 0, 4, 12, 16, 12),
            'P', Block.box(5, 0, 5, 11, 16, 11),
            'T', Block.box(4, 0, 4, 12, 16, 12)
        );

        Map<Character, boolean[]> energySideMap = Map.of(
            'C', MultiblockSideTuples.energy(true, true, true, true, true, false)
        );

        return MultiblockStructureHelper.createFromLayersWithRoles(
            new String[][]{layer0, layerP, layerP, layerP, layerT},
            Map.of(),
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            roleMap,
            shapeMap,
            shapeMap,
            null,
            energySideMap,
            null
        ).withPlacementOffset(0);
    }

    public RedPylonBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return STRUCTURE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockEntityType<? extends RedPylonBlockEntity> getBlockEntityType() {
        return ModBlockEntities.RED_PYLON_BE.get();
    }

    @Override
    protected String getTypeKey() {
        return "tooltip.hbm_m.connection_single";
    }

    @Override
    protected int getRange() {
        return 25;
    }
}
