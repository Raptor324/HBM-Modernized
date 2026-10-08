package com.hbm_m.block.generic;

import javax.annotation.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.generic.DungeonSpawnerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code DungeonSpawner} (dungeon_spawner): unzerstoerbarer Dungeon-Spawner, Logik im Blockentity. */
public class DungeonSpawnerBlock extends BaseEntityBlock {

    public DungeonSpawnerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DungeonSpawnerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.DUNGEON_SPAWNER.get(), DungeonSpawnerBlockEntity::tick);
    }
    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<DungeonSpawnerBlock> CODEC = simpleCodec(DungeonSpawnerBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
