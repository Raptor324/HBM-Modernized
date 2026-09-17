package com.hbm_m.block.generic;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.SlagBlockEntity;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.util.CrucibleUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Port of the 1.7.10 {@code BlockDynamicSlag} - the molten-material puddle dynamically placed by a
 * {@link com.hbm_m.blockentity.machines.MachineFoundrySlagtapBlockEntity}. Not the same block as the
 * static decorative {@code block_slag} (creeper-shell debris) - see {@link SlagBlockEntity} for the
 * flow/merge/spread behaviour (original updateTick, polled by the ticker registered below).
 */
public class DynamicSlagBlock extends BaseEntityBlock {

    public DynamicSlagBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SlagBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.SLAG_BE.get(), SlagBlockEntity::tick);
    }

    /**
     * 1:1 порт onBlockHarvested/getDrops оригинала: добытая лужа шлака выпадает
     * шлак-предметом своего материала ({@code ItemScraps.create}); в креативе дропа нет.
     * (На 1.20.1 хук возвращает void, на 1.21+ — BlockState.)
     */
    //? if < 1.21.1 {
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        dropSlagScrap(level, pos, player);
        super.playerWillDestroy(level, pos, state, player);
    }
    //?} else {
    /*@Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        dropSlagScrap(level, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }
    *///?}

    private void dropSlagScrap(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide && !player.isCreative()
                && level.getBlockEntity(pos) instanceof SlagBlockEntity slag
                && slag.type != null && slag.amount > 0) {
            ItemStack scrap = CrucibleUtil.createScrap(new MaterialStack(slag.type, slag.amount));
            if (!scrap.isEmpty()) {
                popResource(level, pos, scrap);
            }
        }
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<DynamicSlagBlock> CODEC = simpleCodec(DynamicSlagBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
