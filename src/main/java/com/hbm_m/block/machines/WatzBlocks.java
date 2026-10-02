package com.hbm_m.block.machines;

import javax.annotation.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

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
import net.minecraft.world.phys.AABB;

/** Watz-Zubehoer: {@code WatzPump} und {@code BlockWatzStruct}/{@code TileEntityWatzStruct}. */
public final class WatzBlocks {

    private WatzBlocks() {}

    /**
     * 1:1 {@code WatzPump}: zwei Bloecke hoch, sitzt mittig auf dem obersten Watz-Segment. Ein Redstonesignal auf den
     * Block ueber der Pumpe schaltet den Reaktor ein ({@code TileEntityWatz#updateEntity}).
     */
    public static class Pump extends DummyableMachineBlock {

        public Pump(Properties properties) { super(properties); }

        @Override
        protected MultiblockStructureHelper defineStructure() {
            // Original: getDimensions {1, 0, 0, 0, 0, 0}, Offset 0
            return DummyableStructureBuilder.create()
                    .box(1, 0, 0, 0, 0, 0)
                    .placementOffset(0)
                    .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
        }

        /** Original: {@code TileEntityWatzPump} dient nur dem Rendern; das Modell ist hier statisch. */
        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return null; }

        //? if >1.20.1 {
        /*public static final com.mojang.serialization.MapCodec<Pump> CODEC = simpleCodec(Pump::new);
        @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        *///?}
    }

    /** 1:1 {@code BlockWatzStruct}: Kernbauteil, das sich bei vollstaendigem Aufbau in den Watz verwandelt. */
    public static class StructCore extends BaseEntityBlock {

        public StructCore(Properties properties) { super(properties); }

        @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new StructCoreBlockEntity(pos, state); }

        @Nullable
        @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.STRUCT_WATZ_CORE.get(), StructCoreBlockEntity::serverTick);
        }

        //? if >1.20.1 {
        /*public static final com.mojang.serialization.MapCodec<StructCore> CODEC = simpleCodec(StructCore::new);
        @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        *///?}
    }

    /** 1:1 {@code TileEntityWatzStruct}. */
    public static class StructCoreBlockEntity extends BlockEntity {

        public StructCoreBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.STRUCT_WATZ_CORE.get(), pos, state); }

        public static void serverTick(Level world, BlockPos pos, BlockState state, StructCoreBlockEntity te) {
            if (world.getGameTime() % 20 != 0) return;

            Block cooler = ModBlocks.WATZ_COOLER.get();
            Block element = ModBlocks.WATZ_ELEMENT.get();
            Block end = ModBlocks.WATZ_END_BOLTED.get(); // Original: watz_end Meta 1

            if (!te.cbr(cooler, 0, 1, 0)) return;
            if (!te.cbr(cooler, 0, 2, 0)) return;

            for (int i = 0; i < 3; i++) {
                if (!te.cbr(element, 1, i, 0)) return;
                if (!te.cbr(element, 2, i, 0)) return;
                if (!te.cbr(element, 0, i, 1)) return;
                if (!te.cbr(element, 0, i, 2)) return;
                if (!te.cbr(element, -1, i, 0)) return;
                if (!te.cbr(element, -2, i, 0)) return;
                if (!te.cbr(element, 0, i, -1)) return;
                if (!te.cbr(element, 0, i, -2)) return;
                if (!te.cbr(element, 1, i, 1)) return;
                if (!te.cbr(element, 1, i, -1)) return;
                if (!te.cbr(element, -1, i, 1)) return;
                if (!te.cbr(element, -1, i, -1)) return;
                if (!te.cbr(cooler, 2, i, 1)) return;
                if (!te.cbr(cooler, 2, i, -1)) return;
                if (!te.cbr(cooler, 1, i, 2)) return;
                if (!te.cbr(cooler, -1, i, 2)) return;
                if (!te.cbr(cooler, -2, i, 1)) return;
                if (!te.cbr(cooler, -2, i, -1)) return;
                if (!te.cbr(cooler, 1, i, -2)) return;
                if (!te.cbr(cooler, -1, i, -2)) return;

                for (int j = -1; j < 2; j++) {
                    if (!te.cbr(end, 3, i, j)) return;
                    if (!te.cbr(end, j, i, 3)) return;
                    if (!te.cbr(end, -3, i, j)) return;
                    if (!te.cbr(end, j, i, -3)) return;
                }
                if (!te.cbr(end, 2, i, 2)) return;
                if (!te.cbr(end, 2, i, -2)) return;
                if (!te.cbr(end, -2, i, 2)) return;
                if (!te.cbr(end, -2, i, -2)) return;
            }

            // Original: setBlock(watz, 12) + fillSpace(NORTH, 0); onPlace des Watz baut hier die Dummyzellen
            world.setBlock(pos, ModBlocks.WATZ_POWERPLANT.get().defaultBlockState()
                    .setValue(DummyableMachineBlock.FACING, Direction.NORTH), 3);
        }

        /** [C]heck [B]lock at [R]elative position */
        private boolean cbr(Block b, int x, int y, int z) {
            return level.getBlockState(worldPosition.offset(x, y, z)).is(b);
        }

        private AABB bb = null;

        //? if forge {
        @Override
        //?}
        public AABB getRenderBoundingBox() {
            if (bb == null) {
                bb = new AABB(worldPosition.getX() - 3, worldPosition.getY(), worldPosition.getZ() - 3,
                        worldPosition.getX() + 4, worldPosition.getY() + 3, worldPosition.getZ() + 4);
            }
            return bb;
        }
    }
}
