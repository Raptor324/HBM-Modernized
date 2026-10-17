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

    /**
     * 1:1 {@code MachineCondenser.printHook} (NTM Space): Warnung ohne Atmosphaere, dann "-> Dampf" / "<- Wasser"
     * mit Fuellstand.
     */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof MachineSteamCondenserBlockEntity condenser)) return;

        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();
        if (com.hbm_m.api.space.SpaceApi.condenserNeedsAtmosphere(world, false)) {
            text.add(com.hbm_m.api.space.SpaceApi.noVacuumWarning());
        }
        com.hbm_m.inventory.fluid.tank.FluidTank[] tanks = condenser.getAllTanks();
        for (int i = 0; i < tanks.length; i++) {
            text.add(net.minecraft.network.chat.Component.literal(i < 1 ? "-> " : "<- ")
                    .withStyle(i < 1 ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED)
                    .append(com.hbm_m.inventory.fluid.FluidType.forFluid(tanks[i].getTankType()).getLocalizedName().copy().withStyle(net.minecraft.ChatFormatting.WHITE))
                    .append(net.minecraft.network.chat.Component.literal(": " + tanks[i].getFill() + "/" + tanks[i].getMaxFill() + "mB").withStyle(net.minecraft.ChatFormatting.WHITE)));
        }

        com.hbm_m.interfaces.ILookOverlay.printGeneric(g, net.minecraft.network.chat.Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineSteamCondenserBlock> CODEC = simpleCodec(MachineSteamCondenserBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
