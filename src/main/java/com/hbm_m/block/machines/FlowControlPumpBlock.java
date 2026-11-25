package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.FlowControlPumpBlockEntity;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.util.BobMathUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code FlowControlPump} (1.7.10): Multiblock-Pumpe zur Durchflussregelung ({0, 0, 0, 0, 1, 1}).
 */
public class FlowControlPumpBlock extends DummyableMachineBlock implements ILookOverlay {

    public FlowControlPumpBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(0, 0, 0, 0, 1, 1)
                .extra(0, 0, 1)
                .extra(0, 0, -1)
                .placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public int[][] getAllPorts(Direction facing) {
        return rotatePorts(new int[][] {
            { 1, 0, 0, Direction.EAST.get3DDataValue() },
            { -1, 0, 0, Direction.WEST.get3DDataValue() }
        }, facing);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FlowControlPumpBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FLOW_CONTROL_PUMP_BE.get(), FlowControlPumpBlockEntity::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return handleUse(level, pos, player, hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return handleUse(level, pos, player, InteractionHand.MAIN_HAND);
    }
    *///?}

    private InteractionResult handleUse(Level level, BlockPos pos, Player player, InteractionHand hand) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FlowControlPumpBlockEntity pump)) return InteractionResult.PASS;

        ItemStack held = player.getItemInHand(hand);
        if (!held.isEmpty() && held.getItem() instanceof IItemFluidIdentifier id) {
            Fluid type = id.getType(level, pos, held);
            if (!level.isClientSide && type != null) {
                if (type == ModFluids.NONE.getSource()) type = Fluids.EMPTY;
                pump.tank.setTankType(type);
                pump.setChanged();
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                player.displayClientMessage(Component.literal("Changed type to ")
                        .withStyle(ChatFormatting.YELLOW)
                        .append(FluidType.forFluid(type).getLocalizedName())
                        .append(Component.literal("!")), false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!player.isShiftKeyDown()) {
            if (level.isClientSide) {
                dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () ->
                        com.hbm_m.inventory.gui.GUIPump.open(pos));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return InteractionResult.PASS;
    }

    @Override
    public void printHook(GuiGraphics guiGraphics, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FlowControlPumpBlockEntity pump)) return;

        List<Component> text = new ArrayList<>();
        String body = FluidType.forFluid(pump.tank.getTankType()).getLocalizedName().getString()
                + " (" + pump.tank.getPressure() + " PU): "
                + String.format(Locale.US, "%,d", pump.bufferSize) + "mB/t";
        text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(body).withStyle(ChatFormatting.RESET))
                .append(Component.literal(" ->").withStyle(ChatFormatting.RED)));
        text.add(Component.literal("Priority: ")
                .append(Component.literal(pump.priority.name()).withStyle(ChatFormatting.YELLOW)));
        if (pump.redstone) {
            text.add(Component.literal("Disabled by redstone").withStyle(ChatFormatting.RED));
        }
        if (pump.tank.getFill() > 0) {
            text.add(Component.literal(BobMathUtil.format(pump.tank.getFill()) + "mB buffered"));
        }
        ILookOverlay.printGeneric(guiGraphics, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<FlowControlPumpBlock> CODEC = simpleCodec(FlowControlPumpBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
