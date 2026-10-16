package com.hbm_m.block.bomb;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.bomb.VolcanoCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import javax.annotation.Nullable;

/**
 * Порт {@code com.hbm.blocks.bomb.BlockVolcano} (1.7.10) — ядро вулкана
 * {@code volcano_core} / {@code volcano_rad_core}. Пять состояний (меты оригинала):
 * статичное активное, статичное гаснущее, растущее активное, растущее гаснущее,
 * тлеющее. Вся логика — в {@link VolcanoCoreBlockEntity}.
 */
public class VolcanoCoreBlock extends BaseEntityBlock {

    public static final IntegerProperty VOLCANO_TYPE = IntegerProperty.create("volcano_type", 0, 4);

    // Меты оригинала (BlockVolcano.META_*)
    public static final int TYPE_STATIC_ACTIVE = 0;
    public static final int TYPE_STATIC_EXTINGUISHING = 1;
    public static final int TYPE_GROWING_ACTIVE = 2;
    public static final int TYPE_GROWING_EXTINGUISHING = 3;
    public static final int TYPE_SMOLDERING = 4;

    public VolcanoCoreBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(VOLCANO_TYPE, TYPE_STATIC_ACTIVE));
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<VolcanoCoreBlock> CODEC = simpleCodec(VolcanoCoreBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(VOLCANO_TYPE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VolcanoCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.VOLCANO_CORE_BE.get(), VolcanoCoreBlockEntity::tick);
    }
}
