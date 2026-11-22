package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineCoolingTowerBlockEntity;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code MachineTowerLarge}: {@code getDimensions {12,0,4,4,4,4}} (voller 9x9x13-Quader, Kern unten mittig),
 * {@code getOffset 4}, je Seite drei Anschlusszellen am Rand (mittig und +-3 quer), kein {@code onBlockActivated}
 * (kein GUI, Rechtsklick ohne Wirkung), Anzeige beim Hinsehen. Gezeichnet vom {@code CoolingTowerRenderer}
 * (ungedreht wie {@code RenderLargeTower}).
 * <p>Alte Port-Welten (hohler 7x7-Ring mit Leiter): {@code attemptAutoRepair} baut die Teile beim Laden um.</p>
 */
public class MachineTowerLargeBlock extends DummyableMachineBlock implements com.hbm_m.interfaces.ILookOverlay {

    public MachineTowerLargeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // fillSpace: je Seite makeExtra bei dr2 * 4 sowie dr2 * 4 +- rot * 3 (12 Anschluesse am Boden).
        return DummyableStructureBuilder.create()
                .box(12, 0, 4, 4, 4, 4)
                .extra(4, 0, 0).extra(4, 0, 3).extra(4, 0, -3)
                .extra(-4, 0, 0).extra(-4, 0, 3).extra(-4, 0, -3)
                .extra(0, 0, 4).extra(3, 0, 4).extra(-3, 0, 4)
                .extra(0, 0, -4).extra(3, 0, -4).extra(-3, 0, -4)
                .placementOffset(4)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /**
     * Порт {@code MachineTowerLarge.printHook}: список баков, первый (горячий вход) зелёным
     * {@code "-> "}, остальные (выход) красным {@code "<- "}; формат {@code <имя>: <заполнено>/<объём>mB}.
     */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineCoolingTowerBlockEntity tower)) return;

        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();
        // NTM Space (FFA): Warnung ohne Atmosphaere (Original printHook, vacuumOptimised = false)
        if (com.hbm_m.api.space.SpaceApi.condenserNeedsAtmosphere(level, false)) {
            text.add(com.hbm_m.api.space.SpaceApi.noVacuumWarning());
        }
        com.hbm_m.inventory.fluid.tank.FluidTank[] tanks = tower.getTanks();
        for (int i = 0; i < tanks.length; i++) {
            com.hbm_m.inventory.fluid.tank.FluidTank tank = tanks[i];
            text.add(net.minecraft.network.chat.Component.literal(i < 1 ? "-> " : "<- ")
                    .withStyle(i < 1 ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED)
                    .append(net.minecraft.network.chat.Component.literal("")
                            .append(com.hbm_m.inventory.fluid.FluidType.forFluid(tank.getTankType()).getLocalizedName())
                            .withStyle(net.minecraft.ChatFormatting.WHITE))
                    .append(net.minecraft.network.chat.Component.literal(
                            ": " + tank.getFill() + "/" + tank.getMaxFill() + "mB")
                            .withStyle(net.minecraft.ChatFormatting.WHITE)));
        }
        com.hbm_m.interfaces.ILookOverlay.printGeneric(guiGraphics,
                net.minecraft.network.chat.Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineCoolingTowerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.COOLING_TOWER_BE.get(), MachineCoolingTowerBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineTowerLargeBlock> CODEC = simpleCodec(MachineTowerLargeBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
