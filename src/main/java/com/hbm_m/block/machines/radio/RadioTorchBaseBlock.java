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
 * Auswahlbox, fehlende Kollision und Abfallen ohne Halt wie im Original ({@link AttachedTorchShape}).
 */
public abstract class RadioTorchBaseBlock extends BaseEntityBlock implements com.hbm_m.interfaces.ILookOverlay {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

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
        return com.hbm_m.block.machines.radio.AttachedTorchShape.shape(state.getValue(FACING));
    }

    /** Original: {@code getCollisionBoundingBoxFromPool} = null. */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return canBlockStay(level, pos, state.getValue(FACING));
    }

    /**
     * Original {@code RadioTorchRWBase.printHook} (Sender/Empfaenger) bzw. {@code RadioTorchLogic.printHook}:
     * Frequenz und letzter Zustand. Leser, Steuerung und Zaehler ueberschreiben das.
     */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level level, BlockPos pos) {
        net.minecraft.world.level.block.entity.BlockEntity te = level.getBlockEntity(pos);
        String channel;
        int lastState;
        if (te instanceof com.hbm_m.blockentity.network.radio.RadioTorchBaseBlockEntity radio) {
            channel = radio.channel; lastState = radio.lastState;
        } else if (te instanceof com.hbm_m.blockentity.network.radio.RadioTorchLogicBlockEntity radio) {
            channel = radio.channel; lastState = radio.lastState;
        } else {
            return;
        }
        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();
        if (channel != null && !channel.isEmpty()) text.add(net.minecraft.network.chat.Component.literal("Freq: " + channel).withStyle(net.minecraft.ChatFormatting.AQUA));
        text.add(net.minecraft.network.chat.Component.literal("Signal: " + lastState).withStyle(net.minecraft.ChatFormatting.RED));
        com.hbm_m.interfaces.ILookOverlay.printGeneric(g, getName(), 0xffff00, 0x404000, text);
    }

    /** Original {@code canBlockStay}; Leser und Steuerung verlangen statt dessen einen Funk-Baustein dahinter. */
    protected boolean canBlockStay(net.minecraft.world.level.LevelReader level, BlockPos pos, Direction facing) {
        return com.hbm_m.block.machines.radio.AttachedTorchShape.canStay(level, pos, facing);
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
