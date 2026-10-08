package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.FluidValveBlockEntity;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code FluidValve} (von Hand), {@code FluidSwitch} (Redstone) und {@code FluidCounterValve} (Zaehler, per
 * Redstone-ueber-Funk): Rohrstueck, das nur im Zustand AN (Metadaten 1) einen Netzknoten bildet. Die Fluessigkeit
 * setzt wie bei jedem Rohr ({@code FluidDuctBase}) der Fluessigkeits-Identifikator.
 */
public class FluidValveBlock extends BaseEntityBlock implements ILookOverlay {

    public enum Mode { VALVE, SWITCH, COUNTER }

    public static final BooleanProperty ON = BooleanProperty.create("on");

    private final Mode mode;

    public FluidValveBlock(Properties props, Mode mode) {
        super(props);
        this.mode = mode;
        registerDefaultState(stateDefinition.any().setValue(ON, false));
    }

    public FluidValveBlock(Properties props) { this(props, Mode.VALVE); }

    public Mode getMode() { return mode; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(ON); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FluidValveBlockEntity(pos, state); }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.FLUID_VALVE_BE.get(), FluidValveBlockEntity::serverTick);
    }

    /** Schaltet um und meldet es dem Blockentity ({@code setBlockMetadataWithNotify} + {@code updateState}). */
    public static void setState(Level level, BlockPos pos, boolean on, float pitch) {
        BlockState s = level.getBlockState(pos);
        if (!s.hasProperty(ON)) return;
        level.setBlock(pos, s.setValue(ON, on), 2);
        level.playSound(null, pos.getX(), pos.getY(), pos.getZ(), HbmSoundsNT.get("block.reactorStart"), SoundSource.BLOCKS, 1.0F, pitch);
        if (level.getBlockEntity(pos) instanceof FluidValveBlockEntity valve) valve.updateState();
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (mode != Mode.SWITCH || level.isClientSide) return;
        boolean on = level.hasNeighborSignal(pos);
        if (on != state.getValue(ON)) setState(level, pos, on, on ? 1.0F : 0.85F);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return handleUse(state, level, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return handleUse(state, level, pos, player, InteractionHand.MAIN_HAND);
    }
    *///?}

    private InteractionResult handleUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        // Original FluidValve: Client immer true, sonst erst super (Identifikator), dann Ventil umlegen;
        // FluidSwitch/FluidCounterValve haben nur FluidDuctBase.onBlockActivated
        if (mode == Mode.VALVE && level.isClientSide) return InteractionResult.SUCCESS;
        if (!stack.isEmpty() && stack.getItem() instanceof IItemFluidIdentifier
                && com.hbm_m.api.fluids.PipeTypeChanger.onIdentifier(level, pos, player, stack)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (mode == Mode.VALVE && !player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                boolean on = !state.getValue(ON);
                setState(level, pos, on, on ? 1.0F : 0.85F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.BlockGetter level,
                                java.util.List<net.minecraft.network.chat.Component> list, net.minecraft.world.item.TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, java.util.List<net.minecraft.network.chat.Component> list, net.minecraft.world.item.TooltipFlag flag) {
    *///?}
        if (mode == Mode.COUNTER) com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof FluidValveBlockEntity valve)) return;
        List<Component> text = new ArrayList<>();
        Fluid fluid = valve.getFluidType();
        if (fluid == null || fluid == Fluids.EMPTY) fluid = ModFluids.NONE.getSource();
        text.add(Component.literal(HbmFluidRegistry.getFluidName(fluid)).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(HbmFluidRegistry.getTintColor(fluid) & 0xFFFFFF))));
        if (mode == Mode.COUNTER) text.add(Component.literal("Counter: " + valve.getCounter()));
        ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<FluidValveBlock> CODEC = simpleCodec(FluidValveBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
