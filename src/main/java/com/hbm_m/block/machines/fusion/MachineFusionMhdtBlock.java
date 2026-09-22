package com.hbm_m.block.machines.fusion;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.fusion.FusionMhdtBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code MachineFusionMHDT} (1.7.10) - magnetohydrodynamische Turbine, wandelt Plasma direkt in Strom.
 */
public class MachineFusionMhdtBlock extends FusionMultiblockBlock implements com.hbm_m.interfaces.ILookOverlay {

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof FusionMhdtBlockEntity turbine)) return;

        boolean hasPlasma = turbine.hasMinimumPlasma();
        boolean isCool = turbine.isCool();
        long power = (long) Math.floor(turbine.plasmaEnergySync * FusionMhdtBlockEntity.PLASMA_EFFICIENCY);
        if (!hasPlasma) power /= 2;

        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();
        text.add(net.minecraft.network.chat.Component.literal("-> ")
                .withStyle(net.minecraft.ChatFormatting.GREEN)
                .append(net.minecraft.network.chat.Component
                        .literal(com.hbm_m.util.EnergyFormatter.format(turbine.plasmaEnergySync) + "TU/t / "
                                + com.hbm_m.util.EnergyFormatter.format(FusionMhdtBlockEntity.MINIMUM_PLASMA) + "TU/t")
                        .withStyle(hasPlasma ? net.minecraft.ChatFormatting.WHITE : net.minecraft.ChatFormatting.GOLD)));
        text.add(net.minecraft.network.chat.Component.literal("<- ")
                .withStyle(net.minecraft.ChatFormatting.RED)
                .append(net.minecraft.network.chat.Component
                        .literal(com.hbm_m.util.EnergyFormatter.format(!isCool ? 0 : power) + "HE/t")
                        .withStyle(net.minecraft.ChatFormatting.WHITE)));

        com.hbm_m.inventory.fluid.tank.FluidTank[] tanks = turbine.getAllTanks();
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

        // Original: blinkende Warnzeilen.
        if (turbine.plasmaEnergySync > 0 && !hasPlasma) {
            text.add(net.minecraft.network.chat.Component.literal("! LOW POWER !")
                    .withStyle(net.minecraft.ChatFormatting.YELLOW));
        }
        if (!isCool) {
            text.add(net.minecraft.network.chat.Component.literal("! ! ! INSUFFICIENT COOLING ! ! !")
                    .withStyle(net.minecraft.ChatFormatting.RED));
        }

        com.hbm_m.interfaces.ILookOverlay.printGeneric(guiGraphics,
                net.minecraft.network.chat.Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }


    public MachineFusionMhdtBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // Original: MachineFusionMHDT - fuenf Kaesten plus ein vorgelagerter Kasten, drei Anschlusszellen, Offset 7
        return DummyableStructureBuilder.create()
                .box(2, 0, 6, 7, 2, 2)
                .box(3, -2, 6, 2, 1, 1)
                .box(3, -2, -6, 7, 1, 1)
                .box(3, -2, -3, 5, 2, 2)
                .box(4, -3, -3, 5, 1, 1)
                .boxAt(3, 0, 0, 1, 0, 0, 1, 3, 3)
                .extra(4, 0, 3)
                .extra(4, 0, -3)
                .extra(7, 1, 0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FusionMhdtBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.FUSION_MHDT_BE.get(), FusionMhdtBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineFusionMhdtBlock> CODEC = simpleCodec(MachineFusionMhdtBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
