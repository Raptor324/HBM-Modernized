package com.hbm_m.block.generic;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.SlagBlockEntity;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.item.material.ItemScraps;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code BlockDynamicSlag}: Schlackepfuetze aus dem Schlackenabstich. Faellt in Luecken nach unten, fuellt eine
 * Pfuetze gleichen Materials darunter auf und zerlaeuft ab einem Fuenftel Fuellung zu den Seiten. Hoehe = Menge;
 * abgebaut gibt sie ihren Inhalt als Schrott.
 */
public class DynamicSlagBlock extends BaseEntityBlock {

    public DynamicSlagBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
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
        if (!level.isClientSide() && (player == null || !player.isCreative())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SlagBlockEntity tile && tile.mat != null && tile.amount > 0) {
                ItemStack scrap = ItemScraps.create(new MaterialStack(tile.mat, tile.amount));
                if (!scrap.isEmpty()) {
                    popResource(level, pos, scrap);
                }
            }
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        if (world.getBlockEntity(pos) instanceof SlagBlockEntity tile) {
            double height = Math.max(1, tile.amount) / (double) SlagBlockEntity.MAX_AMOUNT;
            return Shapes.box(0, 0, 0, 1, Math.min(1.0, height), 1);
        }
        return Shapes.box(0, 0, 0, 1, 0.0625, 1);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> ret = new ArrayList<>();
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof SlagBlockEntity tile && tile.mat != null && tile.amount > 0) {
            ItemStack scrap = ItemScraps.create(new MaterialStack(tile.mat, tile.amount));
            if (!scrap.isEmpty()) ret.add(scrap);
        }
        return ret;
    }

    @Override
    //? if < 1.21.1 {
    public ItemStack getCloneItemStack(BlockGetter world, BlockPos pos, BlockState state) {
    //?} else {
    /*public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader world, BlockPos pos, BlockState state) {
    *///?}
        if (world.getBlockEntity(pos) instanceof SlagBlockEntity tile && tile.mat != null) {
            return ItemScraps.create(new MaterialStack(tile.mat, tile.amount));
        }
        return ItemStack.EMPTY;
    }

    /** Original {@code colorMultiplier}: Farbe der Schmelze des Materials. */
    public static int getColor(BlockGetter world, BlockPos pos) {
        if (world != null && pos != null && world.getBlockEntity(pos) instanceof SlagBlockEntity tile && tile.mat != null) {
            return tile.mat.moltenColor;
        }
        return 0xFFFFFF;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<DynamicSlagBlock> CODEC = simpleCodec(DynamicSlagBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
