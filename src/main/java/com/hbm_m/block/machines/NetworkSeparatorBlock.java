package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.NetworkSeparatorBlockEntity;
import com.hbm_m.interfaces.ILookOverlay;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code BlockNetworkSeparator} (1.7.10): Multiblock-Trenner fuer Energienetze ({1, 0, 0, 0, 1, 1}).
 */
public class NetworkSeparatorBlock extends DummyableMachineBlock implements ILookOverlay {

    public NetworkSeparatorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(1, 0, 0, 0, 1, 1)
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
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NetworkSeparatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.NETWORK_SEPARATOR_BE.get(), NetworkSeparatorBlockEntity::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return handleUse(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return handleUse(level, pos, player);
    }
    *///?}

    private InteractionResult handleUse(Level level, BlockPos pos, Player player) {
        if (!player.isShiftKeyDown()) {
            if (!(level.getBlockEntity(pos) instanceof NetworkSeparatorBlockEntity)) return InteractionResult.PASS;
            if (level.isClientSide) {
                dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () ->
                        com.hbm_m.inventory.gui.GUIDiode.open(pos));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void printHook(GuiGraphics guiGraphics, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof NetworkSeparatorBlockEntity separator)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(BobMathUtil.getShortNumber(separator.deltaTick[19]) + "HE/t").withStyle(ChatFormatting.RESET))
                .append(Component.literal(" ->").withStyle(ChatFormatting.RED)));
        text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(BobMathUtil.getShortNumber(separator.deltaSecond) + "HE/s").withStyle(ChatFormatting.RESET))
                .append(Component.literal(" ->").withStyle(ChatFormatting.RED)));
        text.add(Component.literal("Configured: " + BobMathUtil.getShortNumber(separator.configuredLimit) + "HE/t"));
        text.add(Component.literal("Priority: ")
                .append(Component.literal(separator.priority.name()).withStyle(ChatFormatting.YELLOW)));
        if (separator.redstone) {
            text.add(Component.literal("Disabled by redstone").withStyle(ChatFormatting.RED));
        }
        ILookOverlay.printGeneric(guiGraphics, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<NetworkSeparatorBlock> CODEC = simpleCodec(NetworkSeparatorBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
