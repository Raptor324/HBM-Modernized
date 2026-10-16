package com.hbm_m.block.decorations;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.decorations.DemonLampBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import org.jetbrains.annotations.Nullable;

/**
 * Порт {@code DemonLamp} — лампа из заряда-демона.
 *
 * <p>Ориентируется по кликнутой грани ({@code FACING}, все 6 направлений),
 * всегда светится на полную (свет 15). Вся игровая логика (радиация,
 * поджиг сущностей) — в {@link DemonLampBlockEntity}, статичная сфера рендерится
 * чанк-мешем (OBJ-модель), лучи — {@link com.hbm_m.client.render.implementations.DemonLampRenderer}.
 */
public class DemonLampBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = DirectionalBlock.FACING;

    public DemonLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DirectionalBlock.FACING, Direction.DOWN));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DirectionalBlock.FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(DirectionalBlock.FACING, ctx.getClickedFace());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DemonLampBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.DEMON_LAMP_BE.get(), DemonLampBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<DemonLampBlock> CODEC = simpleCodec(DemonLampBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
     *///?}
}
