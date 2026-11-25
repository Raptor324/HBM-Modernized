package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.FluidExhaustBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Выхлопная труба: поддерживает три типа дыма одновременно.
 * Всегда открыта, тип жидкости не настраивается — только smoke/smoke_leaded/smoke_poison.
 */
public class FluidExhaustBlock extends BaseEntityBlock implements com.hbm_m.interfaces.ILookOverlay {

    public FluidExhaustBlock(Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidExhaustBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.FLUID_EXHAUST_BE.get(), FluidExhaustBlockEntity::tick);
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();
        text.add(com.hbm_m.inventory.fluid.FluidType.forFluid(com.hbm_m.inventory.fluid.ModFluids.SMOKE.getSource()).getLocalizedName());
        text.add(com.hbm_m.inventory.fluid.FluidType.forFluid(com.hbm_m.inventory.fluid.ModFluids.SMOKE_LEADED.getSource()).getLocalizedName());
        text.add(com.hbm_m.inventory.fluid.FluidType.forFluid(com.hbm_m.inventory.fluid.ModFluids.SMOKE_POISON.getSource()).getLocalizedName());
        com.hbm_m.interfaces.ILookOverlay.printGeneric(guiGraphics,
                net.minecraft.network.chat.Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<FluidExhaustBlock> CODEC = simpleCodec(FluidExhaustBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
