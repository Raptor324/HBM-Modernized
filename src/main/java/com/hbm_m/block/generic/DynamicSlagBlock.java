package com.hbm_m.block.generic;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.SlagBlockEntity;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.item.material.ItemScraps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
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
        // Hoehe und Materialtextur haengen am Block-Entity: gezeichnet von SlagRenderer
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SlagBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        if (world.getBlockEntity(pos) instanceof SlagBlockEntity tile && tile.amount > 0) {
            double h = Math.min(1D, Math.max(1D / 16D, (double) tile.amount / (double) SlagBlockEntity.maxAmount));
            return Shapes.box(0, 0, 0, 1, h, 1);
        }
        return Shapes.block();
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {

        BlockEntity s = world.getBlockEntity(pos);
        BlockEntity b = world.getBlockEntity(pos.below());

        /* Error here, delete the block */
        if (!(s instanceof SlagBlockEntity self)) {
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            return;
        }

        /* Flow down */
        if (world.getBlockState(pos.below()).canBeReplaced() && pos.getY() > world.getMinBuildHeight()) {
            world.setBlockAndUpdate(pos.below(), ModBlocks.SLAG_DYNAMIC.get().defaultBlockState());
            if (world.getBlockEntity(pos.below()) instanceof SlagBlockEntity tile) {
                tile.mat = self.mat;
                tile.amount = self.amount;
                tile.markForUpdate();
            }
            world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            return;

        } else if (b instanceof SlagBlockEntity below) {

            if (below.mat == self.mat && below.amount < SlagBlockEntity.maxAmount) {
                int transfer = Math.min(SlagBlockEntity.maxAmount - below.amount, self.amount);
                below.amount += transfer;
                self.amount -= transfer;

                if (self.amount <= 0) {
                    world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                } else {
                    self.markForUpdate();
                }

                below.markForUpdate();
                world.scheduleTick(pos.below(), this, 1);
                return;
            }
        }

        /* Flow sideways, no neighbors */
        Direction[] sides = new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST };
        int count = 0;
        for (Direction dir : sides) {
            if (world.getBlockState(pos.relative(dir)).canBeReplaced()) {
                count++;
            }
        }

        if (self.amount >= SlagBlockEntity.maxAmount / 5 && count > 0) {
            int toSpread = Math.max(self.amount / (count * 2), 1);

            for (Direction dir : sides) {
                BlockPos side = pos.relative(dir);
                if (world.getBlockState(side).canBeReplaced()) {
                    world.setBlockAndUpdate(side, ModBlocks.SLAG_DYNAMIC.get().defaultBlockState());
                    if (world.getBlockEntity(side) instanceof SlagBlockEntity tile) {
                        tile.mat = self.mat;
                        tile.amount = toSpread;
                        tile.markForUpdate();
                        world.scheduleTick(side, this, 1);
                    }
                    self.amount -= toSpread;
                    self.markForUpdate();
                }
            }
        }
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
