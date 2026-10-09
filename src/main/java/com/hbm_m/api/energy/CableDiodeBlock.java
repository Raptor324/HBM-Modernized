package com.hbm_m.api.energy;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IEnergyConnector;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.util.BobMathUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
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

/**
 * 1:1 {@code CableDiode}: begrenzt den Durchsatz und laesst Strom nur in eine Richtung - weg vom Setzenden
 * ({@code FACING} zeigt wie beim Kolben zum Spieler, Ausgang ist die Gegenseite). Die Kabelarme folgen den
 * Nachbarn wie {@code Library.canConnect}.
 */
public class CableDiodeBlock extends BaseEntityBlock implements ILookOverlay {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH, SOUTH = BlockStateProperties.SOUTH, EAST = BlockStateProperties.EAST,
            WEST = BlockStateProperties.WEST, UP = BlockStateProperties.UP, DOWN = BlockStateProperties.DOWN;

    public CableDiodeBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST, false).setValue(WEST, false).setValue(UP, false).setValue(DOWN, false));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, NORTH, SOUTH, EAST, WEST, UP, DOWN); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    public static BooleanProperty prop(Direction d) {
        return switch (d) { case NORTH -> NORTH; case SOUTH -> SOUTH; case EAST -> EAST; case WEST -> WEST; case UP -> UP; default -> DOWN; };
    }

    private BlockState connections(LevelAccessor world, BlockPos pos, BlockState s) {
        for (Direction d : Direction.values()) s = s.setValue(prop(d), connects(world, pos.relative(d), d.getOpposite()));
        return s;
    }

    /** {@code Library.canConnect}: Energie-Verbinder, der von dieser Seite aus verbinden will. */
    private static boolean connects(LevelAccessor world, BlockPos p, Direction side) {
        BlockEntity be = world.getBlockEntity(p);
        return be instanceof IEnergyConnector c && c.canConnectEnergy(side);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return connections(ctx.getLevel(), ctx.getClickedPos(), defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection().getOpposite()));
    }

    @Override
    public BlockState updateShape(BlockState s, Direction d, BlockState n, LevelAccessor w, BlockPos pos, BlockPos np) {
        return connections(w, pos, s);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        BlockState ns = connections(level, pos, state);
        if (ns != state) level.setBlock(pos, ns, Block.UPDATE_CLIENTS);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return open(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return open(level, pos, player);
    }
    public static final com.mojang.serialization.MapCodec<CableDiodeBlock> CODEC = simpleCodec(CableDiodeBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}

    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide()) {
            dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> com.hbm_m.client.gui.DiodeScreenOpener.open(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
    *///?}
        list.add(Component.literal("Limits throughput and restricts flow direction").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof CableDiodeBlockEntity diode)) return;
        List<Component> text = new ArrayList<>();
        text.add(Component.literal("Max.: " + BobMathUtil.getShortNumber(diode.getMaxEnergyStored()) + "HE/t"));
        text.add(Component.literal("Priority: " + diode.priority.name()));
        ILookOverlay.printGeneric(g, getName(), 0xffff00, 0x404000, text);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CableDiodeBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CABLE_DIODE.get(), CableDiodeBlockEntity::tick);
    }
}
