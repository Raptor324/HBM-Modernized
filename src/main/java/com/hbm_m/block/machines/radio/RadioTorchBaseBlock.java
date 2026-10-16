package com.hbm_m.block.machines.radio;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Shared base for the "Radio Torch" (RTTY, "Redstone Over Radio") block family - port of
 * {@code RadioTorchBase} (1.7.10 Original). Thin, any-face-attachable marker block; right-click
 * opens the configuration GUI.
 * <p>
 * SCOPE-Vereinfachung: Das Original berechnet eine praezise, richtungsabhaengige duenne Hitbox samt
 * Support-Block-Abriss-Logik. Hier: einfache kleine Box (wie bei den Drone-Waypoint-Bloecken dieses
 * Ports), platziert an der geklickten Flaeche - gleiches bereits etabliertes Vereinfachungsmuster.
 */
public abstract class RadioTorchBaseBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape SHAPE = Block.box(6, 6, 6, 10, 10, 10);

    protected RadioTorchBaseBlock(Properties properties) {
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
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * Subclasses open their configuration screen here. Unlike most machines in this port, the
     * radio-torch GUIs (except Counter, which has real filter-item slots) are plain client-side
     * config screens with no container/menu - matching the original's non-container {@code GuiScreen}s
     * - so each subclass handles {@code use()} itself instead of going through a shared MenuProvider.
     */
    //? if < 1.21.1 {
    @Override
    public abstract InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit);
    //?} else {
    /*@Override
    protected abstract InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit);
    *///?}
}
