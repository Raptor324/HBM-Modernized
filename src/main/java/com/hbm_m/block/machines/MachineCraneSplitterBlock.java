package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.network.IConveyorBelt;
import com.hbm_m.block.network.IEnterableBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.MachineCraneSplitterBlockEntity;
import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Crane Splitter - Port von {@code CraneSplitter} (1.7.10 Original). Foerderband-Abzweig: leitet
 * ankommende Items abwechselnd im konfigurierbaren Verhaeltnis nach links/rechts (relativ zu
 * {@link #FACING}) um. Siehe {@link MachineCraneSplitterBlockEntity} fuer die Ratio-Logik und
 * dokumentierte Scope-Vereinfachung.
 * <p>
 * SCOPE-Vereinfachung: Das Original ist eine 2-Block-{@code BlockDummyable}-Struktur (linke +
 * rechte Haelfte) mit eigenem Renderer. Hier: ein einzelner Block (wie bei allen anderen
 * vereinfacht-aber-funktional-aequivalenten Multiblocks in diesem Port), Textur aus den
 * vorhandenen crane_splitter_*-Assets zusammengesetzt.
 */
public class MachineCraneSplitterBlock extends BaseEntityBlock implements IConveyorBelt, IEnterableBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);

    public MachineCraneSplitterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineCraneSplitterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    // ==================== IConveyorBelt ====================

    @Override
    public boolean canItemStay(Level level, BlockPos pos, Vec3 itemPos) {
        return true;
    }

    @Override
    public Vec3 getTravelLocation(Level level, BlockPos pos, Vec3 itemPos, double speed) {
        Direction dir = getTravelDirection(level, pos);
        Vec3 snap = getClosestSnappingPosition(level, pos, itemPos);
        Vec3 dest = new Vec3(snap.x - dir.getStepX() * speed, snap.y - dir.getStepY() * speed, snap.z - dir.getStepZ() * speed);
        Vec3 motion = dest.subtract(itemPos);
        double len = motion.length();
        if (len < 1.0E-6) return itemPos;
        return itemPos.add(motion.scale(speed / len));
    }

    private Direction getTravelDirection(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MachineCraneSplitterBlockEntity splitter) {
            return splitter.getActiveDirection(level.getBlockState(pos).getValue(FACING));
        }
        return level.getBlockState(pos).getValue(FACING);
    }

    @Override
    public Vec3 getClosestSnappingPosition(Level level, BlockPos pos, Vec3 itemPos) {
        return new Vec3(pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5);
    }

    @Override
    public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.Entity entity) {
        if (!level.isClientSide && entity instanceof net.minecraft.world.entity.item.ItemEntity itemEntity
                && entity.tickCount > 10 && entity.isAlive()) {
            Vec3 snap = getClosestSnappingPosition(level, pos, entity.position());
            MovingConveyorItemEntity moving = MovingConveyorItemEntity.create(level, snap.x, snap.y, snap.z, itemEntity.getItem().copy());
            level.addFreshEntity(moving);
            itemEntity.discard();
        }
    }

    // ==================== IEnterableBlock ====================

    @Override
    public void onItemEnter(Level level, BlockPos pos, MovingConveyorItemEntity item) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MachineCraneSplitterBlockEntity splitter) {
            splitter.onItemEnter(level, pos, item);
        }
    }

    // ==================== screwdriver reconfiguration ====================

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() != ModItems.SCREWDRIVER.get()) return InteractionResult.PASS;

        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MachineCraneSplitterBlockEntity splitter) {
                splitter.cycleRatio();
            }
        } else {
            level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), 3);
        }
        return InteractionResult.CONSUME;
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (held.getItem() != ModItems.SCREWDRIVER.get()) return InteractionResult.PASS;

        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MachineCraneSplitterBlockEntity splitter) {
                splitter.cycleRatio();
            }
        } else {
            level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), 3);
        }
        return InteractionResult.CONSUME;
        }
    *///?}


    @Override
    public BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineCraneSplitterBlock> CODEC = simpleCodec(MachineCraneSplitterBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
