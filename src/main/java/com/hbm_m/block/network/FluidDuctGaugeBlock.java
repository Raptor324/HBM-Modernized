package com.hbm_m.block.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.FluidDuctGaugeBlockEntity;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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

/**
 * 1:1 {@code FluidDuctGauge}: Vollblock-Rohr aus Stahl mit Rohrmarkierung, auf der Vorderseite (zum Spieler) ein
 * Messfenster. Die Blickanzeige nennt die Fluessigkeit und den Durchsatz des Netzes in mB/t und mB/s.
 */
public class FluidDuctGaugeBlock extends BaseEntityBlock implements ILookOverlay {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public FluidDuctGaugeBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    /** Original {@code BlockPistonBase.determineOrientation}. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getNearestLookingDirection().getOpposite());
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FluidDuctGaugeBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.FLUID_DUCT_GAUGE_BE.get(), FluidDuctGaugeBlockEntity::gaugeTick);
    }

    /** {@code FluidDuctBase.onBlockActivated}: Fluessigkeitsidentifikator stellt die Rohrsorte (geschlichen: ganzes Netz). */
    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    //?} else {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack hbmHeld, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return com.hbm_m.platform.BlockUseHooks.item(hbmUse(state, level, pos, player, hand, hit));
    }
    private InteractionResult hbmUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    *///?}
        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && held.getItem() instanceof IItemFluidIdentifier && level.getBlockEntity(pos) instanceof FluidDuctGaugeBlockEntity) {
            if (com.hbm_m.api.fluids.PipeTypeChanger.onIdentifier(level, pos, player, held)) return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.BlockGetter level, List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
    *///?}
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof FluidDuctGaugeBlockEntity duct)) return;

        Fluid fluid = duct.getFluidType();
        if (fluid == null || fluid == Fluids.EMPTY) fluid = ModFluids.NONE.getSource();

        List<Component> text = new ArrayList<>();
        text.add(Component.literal(HbmFluidRegistry.getFluidName(fluid)).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(HbmFluidRegistry.getTintColor(fluid) & 0xFFFFFF))));
        text.add(Component.literal(String.format(Locale.US, "%,d", duct.deltaTick) + " mB/t"));
        text.add(Component.literal(String.format(Locale.US, "%,d", duct.deltaLastSecond) + " mB/s"));
        ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<FluidDuctGaugeBlock> CODEC = simpleCodec(FluidDuctGaugeBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
