package com.hbm_m.block.network;

import java.util.Map;
import java.util.function.Supplier;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.RedPylonMediumBlockEntity;
import com.hbm_m.multiblock.MultiblockSideTuples;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Порт PylonMedium (1.7.10): средний пилон, TRIPLE, радиус 45 м, башня 8 блоков (y=0..7).
 * Наверху перекладина ("рука") уходит на 2 блока вперёд (крепления кабелей на y=7).
 * Вариант с трансформером имеет блок сзади (y=0..1) для связи с машиной.
 */
public class RedPylonMediumBlock extends RedPylonCoreBlock {

    protected final boolean transformer;
    private final MultiblockStructureHelper structureHelper;

    private static MultiblockStructureHelper defineStructure(boolean transformer) {
        // Depth 5: row 0 (relZ=-2, рука край), row 1 (relZ=-1, рука середина),
        // row 2 (relZ=0, мачта/контроллер), row 3 (relZ=+1, трансформер сзади), row 4 (relZ=+2, пусто)
        String[] layer0 = {
            ".",
            ".",
            "C",
            transformer ? "B" : ".",
            "."
        };
        String[] layer1 = {
            ".",
            ".",
            "P",
            transformer ? "B" : ".",
            "."
        };
        String[] layerMast = {
            ".",
            ".",
            "P",
            ".",
            "."
        };
        String[] layerTop = {
            "A",
            "A",
            "T",
            ".",
            "."
        };

        Map<Character, PartRole> roleMap = Map.of(
            'C', PartRole.CONTROLLER,
            'P', PartRole.DEFAULT,
            'T', PartRole.DEFAULT,
            'A', PartRole.DEFAULT,
            'B', PartRole.ENERGY_CONNECTOR
        );

        Map<Character, VoxelShape> shapeMap = Map.of(
            'C', Block.box(4, 0, 4, 12, 16, 12),
            'P', Block.box(5, 0, 5, 11, 16, 11),
            'T', Block.box(4, 0, 4, 12, 16, 12),
            'A', Block.box(5, 4, 0, 11, 12, 16),
            'B', Block.box(2, 0, 2, 14, 16, 14)
        );

        Map<Character, boolean[]> energySideMap = Map.of(
            'B', MultiblockSideTuples.energy(true, true, true, true, true, true)
        );

        return MultiblockStructureHelper.createFromLayersWithRoles(
            new String[][]{layer0, layer1, layerMast, layerMast, layerMast, layerMast, layerMast, layerTop},
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

    public RedPylonMediumBlock(Properties properties, boolean transformer) {
        super(properties);
        this.transformer = transformer;
        this.structureHelper = defineStructure(transformer);
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return this.structureHelper;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Крепления уходят в сторону взгляда игрока; трансформер ставится сзади.
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    public boolean isTransformer() {
        return transformer;
    }

    @Override
    protected BlockEntityType<? extends RedPylonMediumBlockEntity> getBlockEntityType() {
        return ModBlockEntities.RED_PYLON_MEDIUM_BE.get();
    }

    @Override
    protected String getTypeKey() {
        return "tooltip.hbm_m.connection_triple";
    }

    @Override
    protected int getRange() {
        return 45;
    }
}
