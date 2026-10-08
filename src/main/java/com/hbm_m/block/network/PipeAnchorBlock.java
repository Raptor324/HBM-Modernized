package com.hbm_m.block.network;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.api.fluids.PipeTypeChanger;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.PipeAnchorBlockEntity;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 1:1 {@code FluidPipeAnchor}: Anker an der angeklickten Flaeche, 8/16-Stumpf bis zur Anschlussseite, Reichweite 10 m. */
public class PipeAnchorBlock extends BaseEntityBlock implements ILookOverlay {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public PipeAnchorBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getClickedFace()); }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter w, BlockPos pos, CollisionContext ctx) {
        Direction dir = s.getValue(FACING).getOpposite();
        double min = 4 / 16D, max = 12 / 16D;
        return Shapes.box(dir == Direction.WEST ? 0 : min, dir == Direction.DOWN ? 0 : min, dir == Direction.NORTH ? 0 : min,
                dir == Direction.EAST ? 1 : max, dir == Direction.UP ? 1 : max, dir == Direction.SOUTH ? 1 : max);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return identify(level, pos, player, player.getItemInHand(hand));
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return identify(level, pos, player, player.getMainHandItem());
    }
    public static final com.mojang.serialization.MapCodec<PipeAnchorBlock> CODEC = simpleCodec(PipeAnchorBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}

    private InteractionResult identify(Level level, BlockPos pos, Player player, ItemStack held) {
        if (held.isEmpty() || !(held.getItem() instanceof IItemFluidIdentifier)) return InteractionResult.PASS;
        return PipeTypeChanger.onIdentifier(level, pos, player, held) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Connection Type: ").withStyle(ChatFormatting.GOLD).append(Component.literal("Single").withStyle(ChatFormatting.YELLOW)));
        list.add(Component.literal("Connection Range: ").withStyle(ChatFormatting.GOLD).append(Component.literal("10m").withStyle(ChatFormatting.YELLOW)));
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof PipeAnchorBlockEntity duct)) return;
        Fluid fluid = duct.getFluidType();
        if (fluid == null || fluid == Fluids.EMPTY) fluid = ModFluids.NONE.getSource();
        List<Component> text = new ArrayList<>();
        text.add(Component.literal(HbmFluidRegistry.getFluidName(fluid)).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(HbmFluidRegistry.getTintColor(fluid) & 0xFFFFFF))));
        ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PipeAnchorBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.PIPE_ANCHOR.get(), PipeAnchorBlockEntity::serverTick);
    }
}
