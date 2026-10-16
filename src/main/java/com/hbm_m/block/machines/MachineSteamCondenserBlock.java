package com.hbm_m.block.machines;

import javax.annotation.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineSteamCondenserBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class MachineSteamCondenserBlock extends BaseEntityBlock implements com.hbm_m.interfaces.ILookOverlay {

    public MachineSteamCondenserBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** Порт MachineCondenser.printHook: баки входа/выхода. */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineSteamCondenserBlockEntity condenser)) return;

        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();
        com.hbm_m.inventory.fluid.tank.FluidTank[] tanks = condenser.getAllTanks();
        for (int i = 0; i < tanks.length; i++) {
            com.hbm_m.inventory.fluid.tank.FluidTank tank = tanks[i];
            text.add(net.minecraft.network.chat.Component.literal(i < 1 ? "-> " : "<- ")
                    .withStyle(i < 1 ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED)
                    .append(net.minecraft.network.chat.Component.literal(
                            com.hbm_m.inventory.fluid.FluidType.forFluid(tank.getTankType()).getLocalizedName().getString()
                            + ": " + tank.getFill() + "/" + tank.getMaxFill() + "mB")));
        }

        com.hbm_m.interfaces.ILookOverlay.printGeneric(guiGraphics,
                net.minecraft.network.chat.Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof BaseMachineBlockEntity be) {
                be.dropInventoryContents();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineSteamCondenserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.STEAM_CONDENSER_BE.get(), MachineSteamCondenserBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineSteamCondenserBlock> CODEC = simpleCodec(MachineSteamCondenserBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
