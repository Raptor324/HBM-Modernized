package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code BlockStruct} + {@code TileEntityMultiblock}: Bauplankern fuer den Kompaktwerfer (3x3-Ring aus
 * {@code struct_launcher}) bzw. den Starttisch (9x9 aus {@code struct_launcher} plus elf {@code struct_scaffold}
 * drei Felder neben dem Kern). Steht der Plan, wird der Kern jeden Tick sofort zur Maschine.
 */
public class StructLauncherCoreBlock extends BaseEntityBlock {

    public final boolean large;

    public StructLauncherCoreBlock(Properties props, boolean large) {
        super(props);
        this.large = large;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StructBE(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.STRUCT_LAUNCHER_CORE_BE.get(), (l, p, s, be) -> be.tick(l, p, s));
    }

    public static class StructBE extends BlockEntity {

        public StructBE(BlockPos pos, BlockState state) {
            super(ModBlockEntities.STRUCT_LAUNCHER_CORE_BE.get(), pos, state);
        }

        public boolean isLarge() {
            return getBlockState().getBlock() instanceof StructLauncherCoreBlock b && b.large;
        }

        void tick(Level w, BlockPos pos, BlockState state) {
            if (!(state.getBlock() instanceof StructLauncherCoreBlock core)) return;

            if (!core.large && isCompact(w, pos)) {
                w.setBlock(pos, ModBlocks.COMPACT_LAUNCHER.get().defaultBlockState(), 3);
            }

            if (core.large) {
                Direction dir = isTable(w, pos);
                if (dir != null) {
                    w.setBlock(pos, ModBlocks.LAUNCH_TABLE.get().defaultBlockState().setValue(DummyableMachineBlock.FACING, dir), 3);
                }
            }
        }

        private static boolean isCompact(Level w, BlockPos pos) {
            Block launcher = ModBlocks.STRUCT_LAUNCHER.get();
            for (int i = -1; i <= 1; i++)
                for (int j = -1; j <= 1; j++)
                    if (!(i == 0 && j == 0))
                        if (!w.getBlockState(pos.offset(i, 0, j)).is(launcher)) return false;
            return true;
        }

        /** Original-Metadaten 0..3 = Turm bei +x, -x, +z, -z. */
        @Nullable
        private static Direction isTable(Level w, BlockPos pos) {
            Block launcher = ModBlocks.STRUCT_LAUNCHER.get();
            for (int i = -4; i <= 4; i++)
                for (int j = -4; j <= 4; j++)
                    if (!(i == 0 && j == 0))
                        if (!w.getBlockState(pos.offset(i, 0, j)).is(launcher)) return null;

            for (Direction d : new Direction[] { Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH }) {
                boolean flag = true;
                for (int k = 1; k < 12; k++) {
                    if (!w.getBlockState(pos.relative(d, 3).above(k)).is(ModBlocks.STRUCT_SCAFFOLD.get())) flag = false;
                }
                if (flag) return d;
            }
            return null;
        }

        @Override
        public net.minecraft.world.phys.AABB getRenderBoundingBox() {
            return INFINITE_EXTENT_AABB;
        }
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<StructLauncherCoreBlock> CODEC = simpleCodec(p -> new StructLauncherCoreBlock(p, false));
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
