package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.DeuteriumExtractorBlockEntity;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.util.BobMathUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code MachineDeuteriumExtractor}: Seiten deuterium_extractor_side, oben/unten ..._top_water; Blickanzeige mit Strom und Tanks. */
public class DeuteriumExtractorBlock extends BaseEntityBlock implements ILookOverlay {

    public DeuteriumExtractorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DeuteriumExtractorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.DEUTERIUM_EXTRACTOR_BE.get(), DeuteriumExtractorBlockEntity::tick);
    }

    @Override
    public void printHook(GuiGraphics guiGraphics, Level world, BlockPos pos) {

        if (!(world.getBlockEntity(pos) instanceof DeuteriumExtractorBlockEntity extractor)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal("Power: " + BobMathUtil.getShortNumber(extractor.power) + "HE").withStyle(extractor.power < extractor.getMaxEnergyStored() / 20 ? ChatFormatting.RED : ChatFormatting.GREEN));

        for (int i = 0; i < extractor.tanks.length; i++)
            text.add(Component.literal(i < 1 ? "-> " : "<- ").withStyle(i < 1 ? ChatFormatting.GREEN : ChatFormatting.RED)
                    .append(Component.empty().withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */.append(com.hbm_m.inventory.fluid.FluidType.forFluid(extractor.tanks[i].getTankType()).getLocalizedName())
                            .append(": " + extractor.tanks[i].getFill() + "/" + extractor.tanks[i].getMaxFill() + "mB")));

        ILookOverlay.printGeneric(guiGraphics, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<DeuteriumExtractorBlock> CODEC = simpleCodec(DeuteriumExtractorBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
