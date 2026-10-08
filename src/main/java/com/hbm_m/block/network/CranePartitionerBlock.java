package com.hbm_m.block.network;

import javax.annotation.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.CranePartitionerBlockEntity;
import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 {@code CranePartitioner}: Foerderband-Endstueck, das Teile nur aus Laufrichtung annimmt, Kristallisierer-Eingaben
 * einlagert und in passenden Portionen wieder auf das Band gibt; alles andere kommt in den Ueberlauf.
 */
public class CranePartitionerBlock extends BaseEntityBlock implements IConveyorBelt, IEnterableBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 0.75, 1);

    public CranePartitionerBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState s, BlockGetter w, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    public Direction getTravelDirection(Level level, BlockPos pos) {
        return level.getBlockState(pos).getValue(FACING);
    }

    @Override public boolean canItemStay(Level level, BlockPos pos, Vec3 itemPos) { return true; }

    @Override
    public Vec3 getTravelLocation(Level level, BlockPos pos, Vec3 itemPos, double speed) {
        Direction dir = getTravelDirection(level, pos);
        Vec3 snap = getClosestSnappingPosition(level, pos, itemPos);
        Vec3 dest = new Vec3(snap.x - dir.getStepX() * speed, snap.y - dir.getStepY() * speed, snap.z - dir.getStepZ() * speed);
        Vec3 motion = dest.subtract(itemPos);
        double len = motion.length();
        return new Vec3(itemPos.x + motion.x / len * speed, itemPos.y + motion.y / len * speed, itemPos.z + motion.z / len * speed);
    }

    @Override
    public Vec3 getClosestSnappingPosition(Level level, BlockPos pos, Vec3 itemPos) {
        Direction dir = getTravelDirection(level, pos);
        double ix = Mth.clamp(itemPos.x, pos.getX(), pos.getX() + 1);
        double iz = Mth.clamp(itemPos.z, pos.getZ(), pos.getZ() + 1);
        double posX = pos.getX() + 0.5, posZ = pos.getZ() + 0.5;
        if (dir.getStepX() != 0) posX = ix;
        if (dir.getStepZ() != 0) posZ = iz;
        return new Vec3(posX, pos.getY() + 0.25, posZ);
    }

    @Override
    public boolean canItemEnter(Level level, BlockPos pos, @Nullable Direction dir, MovingConveyorItemEntity item) {
        return getTravelDirection(level, pos) == dir;
    }

    @Override
    public void onItemEnter(Level level, BlockPos pos, MovingConveyorItemEntity entity) {
        if (!(level.getBlockEntity(pos) instanceof CranePartitionerBlockEntity partitioner)) return;
        ItemStack stack = entity.getItem();
        ItemStack remainder;
        if (CranePartitionerBlockEntity.getAmount(level, stack) > 0) {
            remainder = partitioner.tryAdd(0, CranePartitionerBlockEntity.SLOT_COUNT - 1, stack);
        } else {
            remainder = partitioner.tryAdd(CranePartitionerBlockEntity.SLOT_COUNT, CranePartitionerBlockEntity.SLOT_COUNT * 2 - 1, stack);
        }
        if (!remainder.isEmpty()) level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, remainder.copy()));
        entity.discard();
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CranePartitionerBlockEntity te) {
            for (int i = 0; i < te.inventory.getSlots(); i++) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), te.inventory.getStackInSlot(i));
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.BlockGetter level,
                                java.util.List<net.minecraft.network.chat.Component> list, net.minecraft.world.item.TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, java.util.List<net.minecraft.network.chat.Component> list, net.minecraft.world.item.TooltipFlag flag) {
    *///?}
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CranePartitionerBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CRANE_PARTITIONER.get(), CranePartitionerBlockEntity::serverTick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<CranePartitionerBlock> CODEC = simpleCodec(CranePartitionerBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
