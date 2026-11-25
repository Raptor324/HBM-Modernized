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
        // 1:1 PylonLarge (1.7.10): getDimensions() = {13, 0, 1, 1, 1, 1}, getOffset() = 0 -> 3x14x3 box.
        return com.hbm_m.multiblock.DummyableStructureBuilder.create()
                .box(13, 0, 1, 1, 1, 1)
                .placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
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
