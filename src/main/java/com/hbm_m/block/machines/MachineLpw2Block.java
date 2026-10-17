package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineLpw2BlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code MachineLPW2}: {@code getDimensions {6,0,3,3,9,10}}, {@code getOffset 3}, keine Anschluesse, kein GUI.
 * Gezeichnet vom animierten {@code Lpw2Renderer}.
 */
public class MachineLpw2Block extends DummyableMachineBlock {

    public MachineLpw2Block(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        if (com.hbm_m.api.space.SpaceApi.lpw2().active()) {
            // Space-API W-R6B-1, 1:1 NTM Space MachineLPW2: getOffset 9, makeExtra bei +-dir*3 - rot, y+3 (Fluid-Proxies)
            return DummyableStructureBuilder.create()
                    .box(6, 0, 3, 3, 9, 10)
                    .placementOffset(9)
                    .extra(3, 3, -1)
                    .extra(-3, 3, -1)
                    .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        }
        return DummyableStructureBuilder.create()
                .box(6, 0, 3, 3, 9, 10)
                .placementOffset(3)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    /** Space-API W-R6B-1: {@code getDirModified = dir.getRotation(DOWN)} (N -> W) nur als Stationstriebwerk. */
    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state != null && com.hbm_m.api.space.SpaceApi.lpw2().active()) {
            state = state.setValue(FACING, state.getValue(FACING).getCounterClockWise());
        }
        return state;
    }

    /** Ticker nur als Stationstriebwerk (Space-API W-R6B-1); ohne Addon Deko ohne Logik wie bisher. */
    @Nullable
    /** W-R9B4-2: mit aktivem Space-Triebwerk der Addon-Name ("LPW-2 Station Thruster"), sonst HBM. */
    @Override
    public String getDescriptionId() {
        if (com.hbm_m.api.space.SpaceApi.lpw2().active()) {
            String id = com.hbm_m.api.space.SpaceApi.lpw2().descriptionId();
            if (id != null) return id;
        }
        return super.getDescriptionId();
    }

    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level, BlockState state,
            net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (!com.hbm_m.api.space.SpaceApi.lpw2().active()) return null;
        return createTickerHelper(type, com.hbm_m.blockentity.ModBlockEntities.LPW2_BE.get(), MachineLpw2BlockEntity::tick);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineLpw2BlockEntity(pos, state);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineLpw2Block> CODEC = simpleCodec(MachineLpw2Block::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
