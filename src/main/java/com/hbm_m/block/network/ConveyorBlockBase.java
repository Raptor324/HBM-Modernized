package com.hbm_m.block.network;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Port of {@code com.hbm.blocks.network.BlockConveyorBase} (1.7.10 Original) - straight-line
 * conveyor movement, 1/4-block-tall collision shape, and the item-pickup-on-touch mechanic that
 * turns a dropped {@link ItemEntity} into a smoothly-moving {@link MovingConveyorItemEntity}.
 * <p>
 * FACING here matches the original's metadata semantics 1:1: it's the direction items TRAVEL
 * (not the input side), set on placement from the player's look direction exactly like the
 * original's {@code onBlockPlacedBy}.
 */
public class ConveyorBlockBase extends Block implements IConveyorBelt {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);

    public ConveyorBlockBase(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Original: floor((yaw*4/360)+0.5)&3 → 0=south,1=west,2=north,3=east (Notchian yaw wrap),
        // matched here 1:1 via the player's horizontal-facing (which uses the same convention).
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    // ==================== IConveyorBelt ====================

    @Override
    public boolean canItemStay(Level level, BlockPos pos, Vec3 itemPos) {
        return true;
    }

    @Override
    public Vec3 getTravelLocation(Level level, BlockPos pos, Vec3 itemPos, double speed) {
        Direction dir = getTravelDirection(level, pos, itemPos);
        Vec3 snap = getClosestSnappingPosition(level, pos, itemPos);
        Vec3 dest = new Vec3(snap.x - dir.getStepX() * speed, snap.y - dir.getStepY() * speed, snap.z - dir.getStepZ() * speed);
        Vec3 motion = dest.subtract(itemPos);
        double len = motion.length();
        if (len < 1.0E-6) return itemPos;
        return itemPos.add(motion.scale(speed / len));
    }

    public Direction getTravelDirection(Level level, BlockPos pos, Vec3 itemPos) {
        return level.getBlockState(pos).getValue(FACING);
    }

    @Override
    public Vec3 getClosestSnappingPosition(Level level, BlockPos pos, Vec3 itemPos) {
        Direction dir = getTravelDirection(level, pos, itemPos);

        double clampedX = Math.max(pos.getX(), Math.min(pos.getX() + 1, itemPos.x));
        double clampedZ = Math.max(pos.getZ(), Math.min(pos.getZ() + 1, itemPos.z));

        double posX = pos.getX() + 0.5;
        double posZ = pos.getZ() + 0.5;

        if (dir.getStepX() != 0) posX = clampedX;
        if (dir.getStepZ() != 0) posZ = clampedZ;

        return new Vec3(posX, pos.getY() + 0.25, posZ);
    }

    // ==================== item pickup ====================

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof ItemEntity itemEntity && entity.tickCount > 10 && entity.isAlive()) {
            Vec3 snap = getClosestSnappingPosition(level, pos, entity.position());
            MovingConveyorItemEntity moving = MovingConveyorItemEntity.create(level, snap.x, snap.y, snap.z, itemEntity.getItem().copy());
            level.addFreshEntity(moving);
            itemEntity.discard();
        }
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // ==================== screwdriver reconfiguration ====================

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.phys.BlockHitResult hit) {
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
    *///?}
        if (held.getItem() != ModItems.SCREWDRIVER.get()) return InteractionResult.PASS;

        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockState newState = onScrew(level, pos, state, player);
        if (newState != null && newState != state) {
            level.setBlock(pos, newState, 3);
        }
        return InteractionResult.CONSUME;
    }

    /** Port of {@code IToolable.onScrew}: non-sneak = rotate 90° clockwise, sneak = subclass-defined. */
    protected BlockState onScrew(Level level, BlockPos pos, BlockState state, Player player) {
        if (!player.isShiftKeyDown()) {
            return state.setValue(FACING, state.getValue(FACING).getClockWise());
        }
        return onScrewSneak(level, pos, state, player);
    }

    /** Overridden by variants that support sneak-click behavior (bend cycling, lift/chute swap). */
    protected BlockState onScrewSneak(Level level, BlockPos pos, BlockState state, Player player) {
        return state;
    }

    @Override
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
