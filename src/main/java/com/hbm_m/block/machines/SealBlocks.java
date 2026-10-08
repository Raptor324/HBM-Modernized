package com.hbm_m.block.machines;

import javax.annotation.Nullable;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Silo-Luke des Originals: {@code BlockSeal} ({@code seal_controller}) und {@code BlockHatch} ({@code seal_hatch}). */
public final class SealBlocks {

    private SealBlocks() {}

    /**
     * 1:1 {@code BlockSeal}: sucht hinter sich einen quadratischen Rahmen ({@code seal_frame}/Steuerbloecke, Groesse
     * 1-6) und fuellt dessen Innenflaeche mit Lukenbloecken bzw. raeumt sie; Klick, Redstone-Flanke oder Zuender.
     */
    public static class Controller extends Block implements IBomb {
        public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
        public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

        public Controller(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, POWERED); }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        }

        /** Rahmenmitte liegt {@code size} Bloecke hinter der Vorderseite. */
        private static int[] off(Level world, BlockPos pos, int size) {
            BlockState s = world.getBlockState(pos);
            Direction back = s.hasProperty(FACING) ? s.getValue(FACING).getOpposite() : Direction.SOUTH;
            return new int[] { back.getStepX() * size, back.getStepZ() * size };
        }

        private static boolean frame(Level world, int x, int y, int z) {
            BlockState s = world.getBlockState(new BlockPos(x, y, z));
            return s.is(ModBlocks.SEAL_FRAME.get()) || s.is(ModBlocks.SEAL_CONTROLLER.get());
        }

        public static int getFrameSize(Level world, BlockPos pos) {
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();
            for (int size = 1; size < 7; size++) {
                boolean valid = true;
                int[] o = off(world, pos, size);
                int xOff = o[0], zOff = o[1];
                for (int X = x - size; X <= x + size; X++) if (!frame(world, X + xOff, y, z + size + zOff)) valid = false;
                for (int X = x - size; X <= x + size; X++) if (!frame(world, X + xOff, y, z - size + zOff)) valid = false;
                for (int Z = z - size; Z <= z + size; Z++) if (!frame(world, x - size + xOff, y, Z + zOff)) valid = false;
                for (int Z = z - size; Z <= z + size; Z++) if (!frame(world, x + size + xOff, y, Z + zOff)) valid = false;
                if (valid) return size;
            }
            return 0;
        }

        public static void closeSeal(Level world, BlockPos pos, int size) {
            int[] o = off(world, pos, size);
            for (int X = pos.getX() - size + 1; X <= pos.getX() + size - 1; X++) for (int Z = pos.getZ() - size + 1; Z <= pos.getZ() + size - 1; Z++) {
                BlockPos p = new BlockPos(X + o[0], pos.getY(), Z + o[1]);
                if (world.getBlockState(p).isAir() && !world.isClientSide) {
                    world.setBlockAndUpdate(p, ModBlocks.SEAL_HATCH.get().defaultBlockState());
                    if (world.getBlockEntity(p) instanceof HatchBlockEntity te) te.setControllerPos(pos);
                }
            }
        }

        public static void openSeal(Level world, BlockPos pos, int size) {
            int[] o = off(world, pos, size);
            for (int X = pos.getX() - size + 1; X <= pos.getX() + size - 1; X++) for (int Z = pos.getZ() - size + 1; Z <= pos.getZ() + size - 1; Z++) {
                BlockPos p = new BlockPos(X + o[0], pos.getY(), Z + o[1]);
                if (world.getBlockState(p).is(ModBlocks.SEAL_HATCH.get()) && !world.isClientSide) world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            }
        }

        public static boolean isSealClosed(Level world, BlockPos pos, int size) {
            int[] o = off(world, pos, size);
            for (int X = pos.getX() - size + 1; X <= pos.getX() + size - 1; X++) for (int Z = pos.getZ() - size + 1; Z <= pos.getZ() + size - 1; Z++)
                if (world.getBlockState(new BlockPos(X + o[0], pos.getY(), Z + o[1])).is(ModBlocks.SEAL_HATCH.get())) return true;
            return false;
        }

        private static void toggle(Level world, BlockPos pos) {
            int i = getFrameSize(world, pos);
            if (i != 0) {
                if (isSealClosed(world, pos, i)) openSeal(world, pos, i);
                else closeSeal(world, pos, i);
            }
        }

        //? if < 1.21.1 {
        @Override
        public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        //?} else {
        /*@Override
        protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack hbmHeld, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return com.hbm_m.platform.BlockUseHooks.item(hbmUse(state, world, pos, player, hand, hit));
        }
        private InteractionResult hbmUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        *///?}
            if (world.isClientSide) return InteractionResult.SUCCESS;
            if (player.isShiftKeyDown()) return world.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS; // Original: Client true, Server geschlichen false
            toggle(world, pos);
            return InteractionResult.SUCCESS;
        }

        @Override
        public BombReturnCode explode(Level world, BlockPos pos) {
            if (!world.isClientSide) {
                int i = getFrameSize(world, pos);
                if (i != 0) {
                    if (isSealClosed(world, pos, i)) openSeal(world, pos, i);
                    else closeSeal(world, pos, i);
                    return BombReturnCode.TRIGGERED;
                }
                return BombReturnCode.ERROR_INCOMPATIBLE;
            }
            return BombReturnCode.UNDEFINED;
        }

        @Override
        public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos from, boolean moving) {
            if (world.isClientSide) return;
            if (world.hasNeighborSignal(pos)) {
                if (!state.getValue(POWERED)) {
                    world.setBlock(pos, state.setValue(POWERED, true), 2);
                    toggle(world, pos);
                }
            } else if (state.getValue(POWERED)) {
                world.setBlock(pos, state.setValue(POWERED, false), 2);
            }
        }
    }

    /** 1:1 {@code BlockHatch}: unzerstoerbarer Lukenblock, verschwindet ohne gueltigen Steuerblock samt Rahmen. */
    public static class Hatch extends BaseEntityBlock {
        public Hatch(Properties p) { super(p); }

        @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

        @Nullable @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new HatchBlockEntity(pos, state); }

        @Nullable @Override
        public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.SEAL_HATCH.get(), HatchBlockEntity::serverTick);
        }

        //? if >1.20.1 {
        /*public static final com.mojang.serialization.MapCodec<Hatch> CODEC = simpleCodec(Hatch::new);
        @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        *///?}
    }

    /** 1:1 {@code TileEntityHatch}. */
    public static class HatchBlockEntity extends BlockEntity {
        private BlockPos controller = BlockPos.ZERO;

        public HatchBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.SEAL_HATCH.get(), pos, state); }

        public void setControllerPos(BlockPos p) { controller = p.immutable(); setChanged(); }

        public static void serverTick(Level world, BlockPos pos, BlockState state, HatchBlockEntity te) {
            if (!world.getBlockState(te.controller).is(ModBlocks.SEAL_CONTROLLER.get())) {
                world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            } else if (Controller.getFrameSize(world, te.controller) == 0) {
                world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }

        @Override
        //? if < 1.21.1 {
        public void load(CompoundTag nbt) {
        //?} else {
        /*public void loadAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        *///?}
            //? if < 1.21.1 {
            super.load(nbt);
            //?} else {
            /*super.loadAdditional(nbt, registries);
            *///?}
            controller = new BlockPos(nbt.getInt("x1"), nbt.getInt("y1"), nbt.getInt("z1"));
        }

        @Override
        //? if < 1.21.1 {
        protected void saveAdditional(CompoundTag nbt) {
        //?} else {
        /*protected void saveAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        *///?}
            //? if < 1.21.1 {
            super.saveAdditional(nbt);
            //?} else {
            /*super.saveAdditional(nbt, registries);
            *///?}
            nbt.putInt("x1", controller.getX());
            nbt.putInt("y1", controller.getY());
            nbt.putInt("z1", controller.getZ());
        }
    }
}
