package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineTowerSmallBlockEntity;
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
 * 1:1 {@code MachineTowerSmall}: {@code getDimensions {18,0,2,2,2,2}} (voller 5x5x19-Quader, Kern unten mittig),
 * {@code getOffset 2}, je Seite eine Anschlusszelle zwei Felder neben dem Kern ({@code makeExtra}), kein
 * {@code onBlockActivated} (kein GUI, Rechtsklick ohne Wirkung), Anzeige beim Hinsehen. Gezeichnet vom
 * {@code CoolingTowerRenderer} (ungedreht wie {@code RenderSmallTower}).
 * <p>Alte Port-Welten (hohler Schaft mit Leiter): {@code attemptAutoRepair} baut die Teile beim Laden um.</p>
 */
public class MachineTowerSmallBlock extends DummyableMachineBlock implements com.hbm_m.interfaces.ILookOverlay {

    public MachineTowerSmallBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // fillSpace: makeExtra(x + dr2.offsetX * 2, y, z + dr2.offsetZ * 2) fuer die vier Seiten
        // (i = 6 ist UNKNOWN und trifft den Kern selbst - dort passiert nichts).
        return DummyableStructureBuilder.create()
                .box(18, 0, 2, 2, 2, 2)
                .extra(2, 0, 0)
                .extra(-2, 0, 0)
                .extra(0, 0, 2)
                .extra(0, 0, -2)
                .placementOffset(2)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /**
     * Порт {@code MachineTowerSmall.printHook}: список баков, первый (горячий вход) зелёным
     * {@code "-> "}, остальные (выход) красным {@code "<- "}; формат {@code <имя>: <заполнено>/<объём>mB}.
     */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineTowerSmallBlockEntity tower)) return;

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
        return new MachineTowerSmallBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.TOWER_SMALL_BE.get(), MachineTowerSmallBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineTowerSmallBlock> CODEC = simpleCodec(MachineTowerSmallBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
