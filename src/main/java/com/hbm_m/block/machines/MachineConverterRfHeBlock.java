package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineConverterRfHeBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code BlockConverterRfHe}: Energiewandler mit Anzeige beim Hinsehen. */
public class MachineConverterRfHeBlock extends BaseEntityBlock implements com.hbm_m.interfaces.ILookOverlay {

    public MachineConverterRfHeBlock(Properties properties) { super(properties); }

    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MachineConverterRfHeBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, ModBlockEntities.MACHINE_CONVERTER_RF_HE_BE.get(), MachineConverterRfHeBlockEntity::serverTick);
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof MachineConverterRfHeBlockEntity converter)) return;
        List<Component> text = new ArrayList<>();
        //? if forge {
        text.add(Component.literal(ChatFormatting.GREEN + "-> " + ChatFormatting.RESET + com.hbm_m.util.BobMathUtil.getShortNumber(converter.storage.getEnergyStored()) + "RF"));
        //?}
        text.add(Component.literal(ChatFormatting.RED + "<- " + ChatFormatting.RESET + com.hbm_m.util.BobMathUtil.getShortNumber(converter.power) + "HE"));
        com.hbm_m.interfaces.ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineConverterRfHeBlock> CODEC = simpleCodec(MachineConverterRfHeBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
