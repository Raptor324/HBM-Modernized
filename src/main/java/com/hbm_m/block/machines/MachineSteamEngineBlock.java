package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineSteamEngineBlockEntity;
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
 * 1:1 {@code MachineSteamEngine}: {@code getDimensions {1,0,5,1,1,1}}, {@code getOffset 1}; drei Anschlusszellen rechts
 * vom Kern in Hoehe +1 (Strom und Fluid). Kein GUI, dafuer das Blick-Overlay mit beiden Tanks.
 */
public class MachineSteamEngineBlock extends DummyableMachineBlock implements com.hbm_m.interfaces.ILookOverlay {

    public MachineSteamEngineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(1, 0, 5, 1, 1, 1)
                .extra(0, 1, 1)
                .extra(1, 1, 1)
                .extra(-1, 1, 1)
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
        return new MachineSteamEngineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.STEAM_ENGINE_BE.get(),
                (lvl, pos, st, be) -> MachineSteamEngineBlockEntity.tick(lvl, pos, st, (MachineSteamEngineBlockEntity) be));
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
    *///?}
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    private static MutableComponent tankLine(String arrow, ChatFormatting color, FluidTank tank) {
        return Component.literal(arrow).withStyle(color).append(Component.literal("").withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */
                .append(FluidType.forFluid(tank.getTankType()).getLocalizedName())
                .append(": " + String.format(Locale.US, "%,d", tank.getFill()) + " / " + String.format(Locale.US, "%,d", tank.getMaxFill()) + "mB"));
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof MachineSteamEngineBlockEntity engine)) return;

        List<Component> text = new ArrayList<>();
        text.add(tankLine("-> ", ChatFormatting.GREEN, engine.tanks[0]));
        text.add(tankLine("<- ", ChatFormatting.RED, engine.tanks[1]));

        com.hbm_m.interfaces.ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineSteamEngineBlock> CODEC = simpleCodec(MachineSteamEngineBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
