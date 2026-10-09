package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.MachineDroneWaypointBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Drone Waypoint - Port von {@code DroneWaypoint} (1.7.10 Original). Duenner "Sensor"-Block, an
 * jeder Seite montierbar (FACING = Montagerichtung); Rechtsklick (ohne Schraubenzieher) verstellt
 * die Projektionsdistanz ({@link MachineDroneWaypointBlockEntity#getHeight()}), Rechtsklick mit
 * {@link com.hbm_m.item.tools_and_armor.ItemDroneLinker} verlinkt stattdessen ({@code use()} gibt dann PASS).
 * <p>
 * Auswahlbox, fehlende Kollision und Abfallen ohne Halt wie im Original ({@link com.hbm_m.block.machines.radio.AttachedTorchShape}).
 */
public class MachineDroneWaypointBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public MachineDroneWaypointBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return com.hbm_m.block.machines.radio.AttachedTorchShape.shape(state.getValue(FACING));
    }

    /** Original: {@code getCollisionBoundingBoxFromPool} = null. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return com.hbm_m.block.machines.radio.AttachedTorchShape.canStayWaypoint(level, pos, state.getValue(FACING));
    }

    /** Original {@code onNeighborBlockChange}: ohne Halt faellt der Block als Gegenstand ab. */
    @Override
    public BlockState updateShape(BlockState state, net.minecraft.core.Direction dir, BlockState neighbor,
                                  net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (dir == state.getValue(FACING) && !state.canSurvive(level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineDroneWaypointBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.DRONE_WAYPOINT_BE.get(),
                (lvl, pos, st, be) -> MachineDroneWaypointBlockEntity.tick(lvl, pos, st, (MachineDroneWaypointBlockEntity) be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Original: mit dem Drohnenverknuepfer in der Hand reagiert der Block nicht, der Linker uebernimmt
        if (player.getItemInHand(hand).getItem() instanceof com.hbm_m.item.tools_and_armor.ItemDroneLinker) return InteractionResult.PASS;

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MachineDroneWaypointBlockEntity waypoint) {
            waypoint.adjustHeight(!player.isShiftKeyDown());
        }
        return InteractionResult.CONSUME;
        }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MachineDroneWaypointBlockEntity waypoint) {
            waypoint.adjustHeight(!player.isShiftKeyDown());
        }
        return InteractionResult.CONSUME;
        }
    *///?}


    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineDroneWaypointBlock> CODEC = simpleCodec(MachineDroneWaypointBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}

    /** Original {@code addInformation}: {@code addStandardInfo} (Umschalttaste zeigt {@code .desc}). */
    @Override
    //? if < 1.21.1 {
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.BlockGetter level,
                                java.util.List<net.minecraft.network.chat.Component> list, net.minecraft.world.item.TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, java.util.List<net.minecraft.network.chat.Component> list, net.minecraft.world.item.TooltipFlag flag) {
    *///?}
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }
}
