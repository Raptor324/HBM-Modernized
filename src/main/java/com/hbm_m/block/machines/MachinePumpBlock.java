package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachinePumpElectricBlockEntity;
import com.hbm_m.blockentity.machines.MachinePumpSteamBlockEntity;
import com.hbm_m.blockentity.machines.PumpBlockEntity;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code MachinePump}: {@code getDimensions {3,0,1,1,1,1}}, {@code getOffset 1}; Anschlusszellen an den vier Seiten
 * des Kerns. Dampf- und Elektro-Grundwasserpumpe teilen sich Block und {@code PumpRenderer}. Kein GUI, dafuer das
 * Blick-Overlay mit Tanks, Hoehen- und Bodenwarnung.
 */
public class MachinePumpBlock extends DummyableMachineBlock implements com.hbm_m.interfaces.ILookOverlay {

    private final boolean electric;

    public MachinePumpBlock(Properties properties, boolean electric) {
        super(properties);
        this.electric = electric;
    }

    public boolean isElectric() { return electric; }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(3, 0, 1, 1, 1, 1)
                .extra(1, 0, 0)
                .extra(-1, 0, 0)
                .extra(0, 0, 1)
                .extra(0, 0, -1)
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
        return electric
                ? new MachinePumpElectricBlockEntity(pos, state)
                : new MachinePumpSteamBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (electric) {
            return createTickerHelper(type, ModBlockEntities.MACHINE_PUMP_ELECTRIC_BE.get(),
                    (lvl, pos, st, be) -> MachinePumpElectricBlockEntity.tick(lvl, pos, st, (MachinePumpElectricBlockEntity) be));
        }
        return createTickerHelper(type, ModBlockEntities.MACHINE_PUMP_STEAM_BE.get(),
                (lvl, pos, st, be) -> MachinePumpSteamBlockEntity.tick(lvl, pos, st, (MachinePumpSteamBlockEntity) be));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    private static MutableComponent tankLine(String arrow, ChatFormatting color, FluidTank tank) {
        return Component.literal(arrow).withStyle(color).append(Component.literal("").withStyle(ChatFormatting.RESET)
                .append(FluidType.forFluid(tank.getTankType()).getLocalizedName())
                .append(": " + String.format(Locale.US, "%,d", tank.getFill()) + " / " + String.format(Locale.US, "%,d", tank.getMaxFill()) + "mB"));
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos pos) {
        BlockEntity te = world.getBlockEntity(pos);

        List<Component> text = new ArrayList<>();
        boolean onGround;

        if (te instanceof MachinePumpSteamBlockEntity pump) {
            text.add(tankLine("-> ", ChatFormatting.GREEN, pump.getSteamTank()));
            text.add(tankLine("<- ", ChatFormatting.RED, pump.getLpsTank()));
            text.add(tankLine("<- ", ChatFormatting.RED, pump.getWaterTank()));
            onGround = pump.onGround;
        } else if (te instanceof MachinePumpElectricBlockEntity pump) {
            text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN).append(Component.literal("").withStyle(ChatFormatting.RESET)
                    .append(String.format(Locale.US, "%,d", pump.getEnergyStored()) + " / " + String.format(Locale.US, "%,d", MachinePumpElectricBlockEntity.MAX_POWER) + "HE")));
            text.add(tankLine("<- ", ChatFormatting.RED, pump.getWaterTank()));
            onGround = pump.onGround;
        } else {
            return;
        }

        int blink = System.currentTimeMillis() % 1000 < 500 ? 0xff0000 : 0xffff00;

        if (pos.getY() > PumpBlockEntity.GROUND_HEIGHT) {
            text.add(Component.literal("! ! ! ALTITUDE ! ! !").withStyle(s -> s.withColor(blink)));
        }

        if (!onGround) {
            text.add(Component.literal("! ! ! NO VALID GROUND ! ! !").withStyle(s -> s.withColor(blink)));
        }

        com.hbm_m.interfaces.ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if >1.20.1 {
    /*@Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return simpleCodec(p -> new MachinePumpBlock(p, this.electric));
    }
    *///?}
}
