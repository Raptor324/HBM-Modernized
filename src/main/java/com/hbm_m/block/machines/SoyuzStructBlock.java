package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.decorations.SoyuzLauncherBlock;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code BlockSoyuzStruct} + {@code TileEntitySoyuzStruct}: prueft jede Sekunde, ob die Startrampe aus
 * {@code struct_launcher} (Tisch, Plattformen), Beton (Beine) und {@code struct_scaffold} (Geruest) fertig steht.
 * Dann verschwinden Geruest und Hilfsplattformen und die {@code soyuz_launcher} entsteht vier Bloecke hoeher.
 */
public class SoyuzStructBlock extends BaseEntityBlock {

    public SoyuzStructBlock(Properties props) {
        super(props);
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
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.SOYUZ_STRUCT_BE.get(), (l, p, s, be) -> be.tick(l, p));
    }

    public static class StructBE extends BlockEntity {

        int age;

        public StructBE(BlockPos pos, BlockState state) {
            super(ModBlockEntities.SOYUZ_STRUCT_BE.get(), pos, state);
        }

        private static boolean is(Level w, int x, int y, int z, Block b) {
            return w.getBlockState(new BlockPos(x, y, z)).is(b);
        }

        private static boolean concrete(Level w, int x, int y, int z) {
            BlockState s = w.getBlockState(new BlockPos(x, y, z));
            return s.is(ModBlocks.CONCRETE.get()) || s.is(ModBlocks.CONCRETE_SMOOTH.get());
        }

        private static void air(Level w, int x, int y, int z) {
            w.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }

        void tick(Level w, BlockPos pos) {
            age++;
            if (age < 20) return;
            age = 0;

            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            Block launcher = ModBlocks.STRUCT_LAUNCHER.get();
            Block scaffold = ModBlocks.STRUCT_SCAFFOLD.get();

            /// CHECK PAD ///
            for (int i = -6; i <= 6; i++) for (int j = 3; j <= 4; j++) for (int k = -6; k <= 6; k++)
                if (!is(w, x + i, y + j, z + k, launcher)) return;
            for (int i = -1; i <= 1; i++) for (int j = 3; j <= 4; j++) for (int k = -8; k <= -7; k++)
                if (!is(w, x + i, y + j, z + k, launcher)) return;
            for (int i = -2; i <= 2; i++) for (int j = 3; j <= 4; j++) for (int k = 7; k <= 9; k++)
                if (!is(w, x + i, y + j, z + k, launcher)) return;
            for (int i = -2; i <= 2; i++) for (int k = 5; k <= 9; k++)
                if (!is(w, x + i, y + 51, z + k, launcher)) return;
            for (int i = -1; i <= 1; i++) for (int k = -8; k <= -6; k++)
                if (!is(w, x + i, y + 38, z + k, launcher)) return;

            /// CHECK LEGS ///
            for (int i = 3; i <= 6; i++) for (int j = 0; j <= 2; j++) for (int k = 3; k <= 6; k++)
                if (!concrete(w, x + i, y + j, z + k)) return;
            for (int i = -6; i <= -3; i++) for (int j = 0; j <= 2; j++) for (int k = 3; k <= 6; k++)
                if (!concrete(w, x + i, y + j, z + k)) return;
            for (int i = -6; i <= -3; i++) for (int j = 0; j <= 2; j++) for (int k = -6; k <= -3; k++)
                if (!concrete(w, x + i, y + j, z + k)) return;
            for (int i = 3; i <= 6; i++) for (int j = 0; j <= 2; j++) for (int k = -6; k <= -3; k++)
                if (!concrete(w, x + i, y + j, z + k)) return;
            for (int i = -1; i <= 1; i++) for (int j = 0; j <= 2; j++) for (int k = -8; k <= -6; k++)
                if (!concrete(w, x + i, y + j, z + k)) return;
            for (int i = -2; i <= 2; i++) for (int j = 0; j <= 2; j++) for (int k = 5; k <= 9; k++)
                if (!concrete(w, x + i, y + j, z + k)) return;

            /// CHECK SCAFFOLDING ///
            for (int i = -1; i <= 1; i++) for (int j = 5; j <= 50; j++) for (int k = 6; k <= 8; k++)
                if (!is(w, x + i, y + j, z + k, scaffold)) return;
            for (int j = 5; j <= 37; j++)
                if (!is(w, x, y + j, z - 7, scaffold)) return;
            /// CHECKS COMPLETE ///

            /// DELETE SCAFFOLDING ///
            for (int i = -2; i <= 2; i++) for (int k = 5; k <= 9; k++) air(w, x + i, y + 51, z + k);
            for (int i = -1; i <= 1; i++) for (int k = -8; k <= -6; k++) air(w, x + i, y + 38, z + k);
            for (int i = -2; i <= 2; i++) for (int j = 0; j <= 2; j++) for (int k = 5; k <= 9; k++) air(w, x + i, y + j, z + k);
            for (int i = -1; i <= 1; i++) for (int j = 5; j <= 50; j++) for (int k = 6; k <= 8; k++) air(w, x + i, y + j, z + k);
            for (int j = 5; j <= 37; j++) air(w, x, y + j, z - 7);

            /// GENERATE LAUNCHER ///
            air(w, x, y, z);
            w.setBlock(pos.above(SoyuzLauncherBlock.HEIGHT),
                    ModBlocks.SOYUZ_LAUNCHER.get().defaultBlockState().setValue(DummyableMachineBlock.FACING, Direction.EAST), 3);
        }

        @Override
        public net.minecraft.world.phys.AABB getRenderBoundingBox() {
            return INFINITE_EXTENT_AABB;
        }
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<SoyuzStructBlock> CODEC = simpleCodec(SoyuzStructBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
