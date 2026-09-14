package com.hbm_m.block.network;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.network.RedCableGaugeBlockEntity;
import com.hbm_m.interfaces.ILookOverlay;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Порт BlockCableGauge (1.7.10): цельноблочный кабель с датчиком энергии сети.
 * Направление датчика задаётся при установке (как у поршня); при взгляде на блок
 * HUD показывает HE/тик и HE/сек (printHook 1:1).
 */
public class RedCableGaugeBlock extends BaseEntityBlock implements ILookOverlay {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    //? if > 1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<RedCableGaugeBlock> CODEC = simpleCodec(RedCableGaugeBlock::new);
    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}

    public RedCableGaugeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // BlockPistonBase.determineOrientation из оригинала
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedCableGaugeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof RedCableGaugeBlockEntity gauge) {
                RedCableGaugeBlockEntity.tick(lvl, pos, st, gauge);
            }
        };
    }

    @Override
    public void printHook(GuiGraphics guiGraphics, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof RedCableGaugeBlockEntity gauge) {
            List<Component> text = List.of(
                    Component.literal(shortNumber(gauge.deltaTick) + "HE/t"),
                    Component.literal(shortNumber(gauge.deltaLastSecond) + "HE/s"));
            ILookOverlay.printGeneric(guiGraphics, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
        }
    }

    /** BobMathUtil.getShortNumber из оригинала. */
    private static String shortNumber(long value) {
        if (Math.abs(value) < 1_000) return String.format(Locale.US, "%,d", value);
        double d = value;
        for (String suffix : new String[] {"k", "M", "B", "T", "Q", "Qi", "Sx", "Sp"}) {
            d /= 1000;
            if (Math.abs(d) < 1000) return String.format(Locale.US, "%.2f", d) + suffix;
        }
        return String.format(Locale.US, "%.2fE", d);
    }
}
