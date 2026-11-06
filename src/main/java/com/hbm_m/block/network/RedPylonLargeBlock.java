package com.hbm_m.block.network;

import java.util.Map;
import java.util.function.Supplier;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.RedPylonLargeBlockEntity;
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
 * Порт PylonLarge (1.7.10): большой пилон, QUAD, радиус 100 м.
 * Башня высотой 14 блоков (y=0..13) с 4 опорами у основания (3x3) и перекладинами ("руками") наверху (y=11..13),
 * выступающими на 3 блока в стороны.
 * В оригинале модель можно ставить прямо (North/South) или диагонально (East/West).
 */
public class RedPylonLargeBlock extends RedPylonCoreBlock {

    private static final MultiblockStructureHelper STRUCTURE = defineStructure();

    private static MultiblockStructureHelper defineStructure() {
        // Сетка 7x7 (relX: -3..+3, relZ: -3..+3), центр [3, 3] = relX 0, relZ 0
        String[] layer0 = {
            ".......",
            ".......",
            "..L.L..",
            "...C...",
            "..L.L..",
            ".......",
            "......."
        };
        String[] layerLower = {
            ".......",
            ".......",
            "..OOO..",
            "..OPO..",
            "..OOO..",
            ".......",
            "......."
        };
        String[] layerShaft = {
            ".......",
            ".......",
            ".......",
            "...P...",
            ".......",
            ".......",
            "......."
        };
        // Верхние перекладины ("руки") на y=11..13 покрывают прямые и диагональные лучи
        String[] layerArms = {
            "A..A..A",
            ".A.A.A.",
            "..AAA..",
            "AAATAAA",
            "..AAA..",
            ".A.A.A.",
            "A..A..A"
        };

        Map<Character, PartRole> roleMap = Map.of(
            'C', PartRole.CONTROLLER,
            'L', PartRole.DEFAULT,
            'O', PartRole.DEFAULT,
            'P', PartRole.DEFAULT,
            'T', PartRole.DEFAULT,
            'A', PartRole.DEFAULT
        );

        Map<Character, VoxelShape> shapeMap = Map.of(
            'C', Block.box(2, 0, 2, 14, 16, 14),
            'L', Block.box(2, 0, 2, 14, 16, 14),
            'O', Block.box(1, 0, 1, 15, 16, 15),
            'P', Block.box(4, 0, 4, 12, 16, 12),
            'T', Block.box(3, 0, 3, 13, 16, 13),
            'A', Block.box(2, 4, 2, 14, 12, 14)
        );

        return MultiblockStructureHelper.createFromLayersWithRoles(
            new String[][]{
                layer0,
                layerLower, layerLower, layerLower, layerLower, // y=1..4 (3x3 нижняя решётка)
                layerShaft, layerShaft, layerShaft, layerShaft, layerShaft, layerShaft, // y=5..10 (1x1 шахта)
                layerArms, layerArms, layerArms // y=11..13 (руки/перекладины)
            },
            Map.of(),
            () -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState(),
            roleMap,
            shapeMap,
            shapeMap,
            null,
            null,
            null
        ).withPlacementOffset(0);
    }

    public RedPylonLargeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MultiblockStructureHelper getStructureHelper() {
        return STRUCTURE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected BlockEntityType<? extends RedPylonLargeBlockEntity> getBlockEntityType() {
        return ModBlockEntities.RED_PYLON_LARGE_BE.get();
    }

    @Override
    protected String getTypeKey() {
        return "tooltip.hbm_m.connection_quad";
    }

    @Override
    protected int getRange() {
        return 100;
    }
}
