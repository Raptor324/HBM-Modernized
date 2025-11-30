package com.hbm_m.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class StructureFoundationProcessor extends StructureProcessor {

    public static final Codec<StructureFoundationProcessor> CODEC = Codec.unit(StructureFoundationProcessor::new);

    @Override
    protected StructureProcessorType<?> getType() {
        return ModWorldGen.FOUNDATION_PROCESSOR.get();
    }

    public StructureTemplate.StructureBlockInfo processBlock(
            LevelAccessor level,
            BlockPos pos1,
            BlockPos pos2,
            StructureTemplate.StructureBlockInfo info1,
            StructureTemplate.StructureBlockInfo info2,
            StructurePlaceSettings settings
    ) {
        ServerLevel serverLevel = (ServerLevel) level;

        if (!info2.state().isAir()) {
            fillFoundationBelow(serverLevel, info2.pos());
        }
        return info2;
    }

    private void fillFoundationBelow(ServerLevel level, BlockPos structurePos) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(
                structurePos.getX(), structurePos.getY() - 1, structurePos.getZ()
        );

        // ✅ МАКСИМАЛЬНАЯ ГЛУБИНА = 64 блока (было бесконечно!)
        final int MAX_FOUNDATION_DEPTH = 64;
        int blocksFilled = 0;

        while (pos.getY() > level.getMinBuildHeight() + 10 && blocksFilled < MAX_FOUNDATION_DEPTH) {
            BlockState currentState = level.getBlockState(pos);

            // ✅ ОСТАНОВИТЬСЯ на твердых блоках
            if (currentState.is(Blocks.STONE) ||
                    currentState.is(Blocks.DIRT) ||
                    currentState.is(Blocks.GRAVEL) ||
                    currentState.is(Blocks.ANDESITE) ||
                    currentState.is(Blocks.DIORITE) ||
                    currentState.is(Blocks.SAND) ||
                    currentState.is(Blocks.SANDSTONE) ||
                    currentState.is(Blocks.DEEPSLATE) ||
                    currentState.is(Blocks.GRANITE)) {
                break;
            }

            // ✅ ЗАПОЛНИТЬ ТОЛЬКО ВОЗДУХ
            if (currentState.isAir()) {
                level.setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
                blocksFilled++;
            }

            pos.move(0, -1, 0);
        }
    }
}
