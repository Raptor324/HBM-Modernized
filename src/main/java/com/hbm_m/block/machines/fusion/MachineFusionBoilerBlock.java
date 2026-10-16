package com.hbm_m.block.machines.fusion;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.fusion.FusionBoilerBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code MachineFusionBoiler} (1.7.10) - macht aus Plasmaleistung ueberhitzten Dampf.
 */
public class MachineFusionBoilerBlock extends FusionMultiblockBlock implements com.hbm_m.interfaces.ILookOverlay {

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof FusionBoilerBlockEntity boiler)) return;

        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();
        text.add(net.minecraft.network.chat.Component.literal("-> ")
                .withStyle(net.minecraft.ChatFormatting.GREEN)
                .append(net.minecraft.network.chat.Component
                        .literal(com.hbm_m.util.EnergyFormatter.format(boiler.plasmaEnergySync) + " TU")
                        .withStyle(net.minecraft.ChatFormatting.WHITE)));

        com.hbm_m.inventory.fluid.tank.FluidTank[] tanks = boiler.getAllTanks();
        for (int i = 0; i < tanks.length; i++) {
            com.hbm_m.inventory.fluid.tank.FluidTank tank = tanks[i];
            text.add(net.minecraft.network.chat.Component.literal(i == 0 ? "-> " : "<- ")
                    .withStyle(i == 0 ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED)
                    .append(com.hbm_m.api.fluids.FluidLocalization.nameFromFluidId(
                            net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(tank.getTankType()))
                            .copy().withStyle(net.minecraft.ChatFormatting.WHITE))
                    .append(net.minecraft.network.chat.Component
                            .literal(": " + tank.getFill() + "/" + tank.getMaxFill() + "mB")
                            .withStyle(net.minecraft.ChatFormatting.WHITE)));
        }

        com.hbm_m.interfaces.ILookOverlay.printGeneric(guiGraphics,
                net.minecraft.network.chat.Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }


    public MachineFusionBoilerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // Original: MachineFusionBoiler - getDimensions {3,0,4,4,1,1}, vier Anschlusszellen, Offset 4
        return DummyableStructureBuilder.create()
                .box(3, 0, 4, 4, 1, 1)
                .extra(-1, 0, 1)
                .extra(-1, 0, -1)
                .extra(2, 0, 1)
                .extra(2, 0, -1)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FusionBoilerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FUSION_BOILER_BE.get(), FusionBoilerBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineFusionBoilerBlock> CODEC = simpleCodec(MachineFusionBoilerBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
