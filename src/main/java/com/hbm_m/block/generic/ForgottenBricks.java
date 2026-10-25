package com.hbm_m.block.generic;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Die vergessenen Ziegel des Originals: {@code BlockForgottenBrick} und {@code BlockForgottenLock}. */
public final class ForgottenBricks {

    private ForgottenBricks() {}

    /**
     * 1:1 {@code BlockForgottenBrick} ({@code brick_forgotten}): unzerstoerbar, sieben Arten; aus dem Loch
     * ({@code META_HOLE}) zieht man mit leerer Hand ewige Kohle, danach ist es leer.
     */
    public static class Brick extends Block {
        public static final int META_DEFAULT = 0, META_BW = 1, META_NULLSTONE = 2, META_HOLE = 3, META_HOLE_EMPTY = 4, META_NULLROOM_WOOD = 5, META_NULLROOM_STONE = 6;
        public static final IntegerProperty META = IntegerProperty.create("meta", 0, 6);

        public Brick(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(META, 0));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(META); }

        @Override
        //? if < 1.21.1 {
        public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        //?} else {
        /*public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        *///?}
            return JungleBricks.stack(asItem(), "meta", Integer.toString(state.getValue(META)));
        }

        //? if < 1.21.1 {
        @Override
        public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return activate(state, world, pos, player, hand);
        }
        //?} else {
        /*@Override
        protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
            return activate(state, world, pos, player, InteractionHand.MAIN_HAND);
        }
        *///?}

        private InteractionResult activate(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand) {
            if (state.getValue(META) == META_HOLE && hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty()) {
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.COAL_ETERNAL.get()));
                world.setBlock(pos, state.setValue(META, META_HOLE_EMPTY), 3);
                return InteractionResult.sidedSuccess(world.isClientSide);
            }
            return InteractionResult.PASS;
        }
    }

    /**
     * 1:1 {@code BlockForgottenLock} ({@code brick_forgotten_lock}): mit dem roten (oder dem gesprungenen, der dabei
     * zerbricht) Schluessel an einer Seite geoeffnet, graebt es einen 15 Bloecke tiefen, 3x3 hohlen Gang aus vergessenen Ziegeln.
     */
    public static class Lock extends Block {
        public static final int META_DEFAULT = 0, META_BW = 1, META_NULLSTONE = 2, META_THE_BLOCK_THAT_FUCKING_KILLS_YOU = 3;
        public static final IntegerProperty META = IntegerProperty.create("meta", 0, 3);

        public Lock(Properties p) {
            super(p);
            registerDefaultState(stateDefinition.any().setValue(META, 0));
        }

        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(META); }

        @Override
        //? if < 1.21.1 {
        public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        //?} else {
        /*public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) {
        *///?}
            return JungleBricks.stack(asItem(), "meta", Integer.toString(state.getValue(META)));
        }

        //? if < 1.21.1 {
        @Override
        public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return activate(world, pos, player, player.getItemInHand(hand), hit.getDirection());
        }
        //?} else {
        /*@Override
        protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return activate(world, pos, player, stack, hit.getDirection()) == InteractionResult.PASS
                    ? net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        *///?}

        private InteractionResult activate(Level world, BlockPos pos, Player player, ItemStack held, Direction side) {
            if (held.isEmpty()) return InteractionResult.PASS;
            boolean cracked = held.is(ModItems.KEY_RED_CRACKED.get());
            if ((held.is(ModItems.KEY_RED.get()) || cracked) && side.getAxis() != Direction.Axis.Y) {
                if (cracked) held.shrink(1);
                if (world.isClientSide) return InteractionResult.SUCCESS;
                generate(world, pos, side);
                world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("block.lockOpen"), SoundSource.PLAYERS, 1.0F, 1.0F);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        /** {@code dir.getRotation(UP)} dreht im Uhrzeigersinn um die Hochachse. */
        public static void generate(Level world, BlockPos pos, Direction dir) {
            Direction rot = dir.getClockWise();
            int len = 15;
            for (int w = -2; w <= 2; w++) for (int h = -2; h <= 2; h++) for (int d = 0; d < len; d++) {
                BlockState b = (w == -2 || w == 2 || h == -2 || h == 2 || d == len - 1) ? ModBlocks.BRICK_FORGOTTEN.get().defaultBlockState() : Blocks.AIR.defaultBlockState();
                world.setBlockAndUpdate(new BlockPos(pos.getX() - dir.getStepX() * d + rot.getStepX() * w, pos.getY() + h, pos.getZ() - dir.getStepZ() * d + rot.getStepZ() * w), b);
            }
        }
    }
}
