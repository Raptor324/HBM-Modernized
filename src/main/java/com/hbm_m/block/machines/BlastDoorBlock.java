package com.hbm_m.block.machines;

import javax.annotation.Nullable;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.BlastDoorBlockEntity;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tool.ItemLock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code BlastDoor} ({@code blast_door}): Tor mit 6 Rahmenbloecken ({@code dummy_block_blast}) darueber; nur zwei
 * Ausrichtungen (Metadaten 2/3). Oeffnen per Hand (Schloss beachten), Redstone oder Zuender ({@code IBomb}).
 */
public class BlastDoorBlock extends BaseEntityBlock implements IBomb {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public BlastDoorBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    /** Blickrichtung 0/2 -> Meta 2 (Nord), 1/3 -> Meta 3 (Sued). */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        int i = net.minecraft.util.Mth.floor(ctx.getRotation() * 4.0F / 360.0F + 0.5D) & 3;
        return defaultBlockState().setValue(FACING, (i == 0 || i == 2) ? Direction.NORTH : Direction.SOUTH);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (world.isClientSide || !(world.getBlockEntity(pos) instanceof BlastDoorBlockEntity te)) return;
        boolean ok = true;
        for (int i = 1; i <= 6; i++) ok &= te.placeDummy(pos.above(i));
        if (!ok) world.destroyBlock(pos, true);
    }

    @Override
    public BombReturnCode explode(Level world, BlockPos pos) {
        if (!world.isClientSide && world.getBlockEntity(pos) instanceof BlastDoorBlockEntity entity) {
            if (!entity.isLocked()) {
                entity.tryToggle();
                return BombReturnCode.TRIGGERED;
            }
            return BombReturnCode.ERROR_INCOMPATIBLE;
        }
        return BombReturnCode.UNDEFINED;
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return activate(world, pos, player, player.getItemInHand(hand));
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return activate(world, pos, player, player.getMainHandItem());
    }
    public static final com.mojang.serialization.MapCodec<BlastDoorBlock> CODEC = simpleCodec(BlastDoorBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}

    private InteractionResult activate(Level world, BlockPos pos, Player player, ItemStack held) {
        if (world.isClientSide) return InteractionResult.SUCCESS;
        if (!held.isEmpty() && (held.getItem() instanceof ItemLock || held.is(ModItems.KEY_KIT.get()))) return InteractionResult.PASS;
        if (!player.isShiftKeyDown()) {
            if (world.getBlockEntity(pos) instanceof BlastDoorBlockEntity entity) {
                if (entity.isLocked()) {
                    if (entity.canAccess(player)) entity.tryToggle();
                } else {
                    entity.tryToggle();
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BlastDoorBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.BLAST_DOOR.get(), BlastDoorBlockEntity::serverTick);
    }

    // ================================================================================================

    /** 1:1 {@code DummyBlockBlast}: Rahmenblock; wird er zerstoert, faellt das ganze Tor. */
    public static class Dummy extends BaseEntityBlock implements IBomb {
        public static boolean safeBreak = false;

        public Dummy(Properties p) { super(p); }

        @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

        @Nullable @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DummyBlockEntity(pos, state); }

        @Nullable
        private static BlastDoorBlockEntity target(Level world, BlockPos pos) {
            if (world.getBlockEntity(pos) instanceof DummyBlockEntity d && d.target != null && world.getBlockEntity(d.target) instanceof BlastDoorBlockEntity door) return door;
            return null;
        }

        @Override
        public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean moved) {
            if (!safeBreak && !state.is(newState.getBlock()) && !world.isClientSide && world.getBlockEntity(pos) instanceof DummyBlockEntity d && d.target != null) {
                world.destroyBlock(d.target, true);
            }
            super.onRemove(state, world, pos, newState, moved);
        }

        @Override
        public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) { return new ItemStack(ModBlocks.BLAST_DOOR.get()); }

        //? if < 1.21.1 {
        @Override
        public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (world.isClientSide) return InteractionResult.SUCCESS;
            ItemStack held = player.getItemInHand(hand);
            if (!held.isEmpty() && (held.getItem() instanceof ItemLock || held.is(ModItems.KEY_KIT.get()))) return InteractionResult.PASS;
            if (!player.isShiftKeyDown()) {
                BlastDoorBlockEntity door = target(world, pos);
                if (door != null && door.canAccess(player)) door.tryToggle();
            }
            return InteractionResult.SUCCESS;
        }
        //?}

        @Override
        public BombReturnCode explode(Level world, BlockPos pos) {
            if (!world.isClientSide) {
                BlastDoorBlockEntity door = target(world, pos);
                if (door != null && !door.isLocked()) {
                    door.tryToggle();
                    return BombReturnCode.TRIGGERED;
                }
                return BombReturnCode.ERROR_INCOMPATIBLE;
            }
            return BombReturnCode.UNDEFINED;
        }

        //? if >1.20.1 {
        /*public static final com.mojang.serialization.MapCodec<Dummy> CODEC = simpleCodec(Dummy::new);
        @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        *///?}
    }

    /** 1:1 {@code TileEntityDummy}: merkt sich den Torblock. */
    public static class DummyBlockEntity extends BlockEntity {
        @Nullable BlockPos target;

        public DummyBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.DUMMY_BLOCK_BLAST.get(), pos, state); }

        public void setTarget(BlockPos t) { this.target = t.immutable(); setChanged(); }

        @Override
        public void load(CompoundTag nbt) {
            super.load(nbt);
            target = nbt.contains("tx") ? new BlockPos(nbt.getInt("tx"), nbt.getInt("ty"), nbt.getInt("tz")) : null;
        }

        @Override
        protected void saveAdditional(CompoundTag nbt) {
            super.saveAdditional(nbt);
            if (target != null) { nbt.putInt("tx", target.getX()); nbt.putInt("ty", target.getY()); nbt.putInt("tz", target.getZ()); }
        }
    }
}
