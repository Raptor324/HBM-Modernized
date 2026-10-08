package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineSolarBoilerBlockEntity;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.util.BobMathUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code MachineSolarBoiler}: {@code getDimensions {2,0,1,1,1,1}}, {@code getOffset 1}, eine Anschlusszelle oben
 * mittig auf Hoehe 2. Kein GUI, nur die Anzeige beim Hinsehen (Tanks, "Too cold!" ohne Spiegelhitze).
 * Gezeichnet vom {@code SolarBoilerRenderer} samt Lichtstrahlen der Spiegel.
 */
public class MachineSolarBoilerBlock extends DummyableMachineBlock implements com.hbm_m.interfaces.ILookOverlay {

    public MachineSolarBoilerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(2, 0, 1, 1, 1, 1)
                .extra(0, 2, 0)
                .placementOffset(1)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineSolarBoilerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.SOLAR_BOILER_BE.get(), MachineSolarBoilerBlockEntity::tick);
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof MachineSolarBoilerBlockEntity boiler)) return;

        List<Component> text = new ArrayList<>();

        FluidTank[] tanks = boiler.getAllTanks();

        for (int i = 0; i < tanks.length; i++)
            text.add((i < 1 ? Component.literal("-> ").withStyle(ChatFormatting.GREEN) : Component.literal("<- ").withStyle(ChatFormatting.RED))
                    .append(Component.literal("").withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */
                            .append(FluidType.forFluid(tanks[i].getTankType()).getLocalizedName())
                            .append(": " + tanks[i].getFill() + "/" + tanks[i].getMaxFill() + "mB")));

        if (boiler.display < 1) {
            // Original "&[" + Farbe + "&]Too cold!"
            text.add(Component.literal("Too cold!").withStyle(s -> s.withColor(TextColor.fromRgb(BobMathUtil.getBlink() ? 0xff0000 : 0xffff00))));
        }

        com.hbm_m.interfaces.ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineSolarBoilerBlock> CODEC = simpleCodec(MachineSolarBoilerBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
