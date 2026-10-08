package com.hbm_m.block.machines;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineCondenserPoweredBlockEntity;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
 * 1:1 {@code MachineCondenserPowered}: {@code getDimensions {2,0,1,1,3,3}}, {@code getOffset 1}, sechs
 * Anschlusszellen auf Hoehe 1, Anzeige beim Hinsehen. Gezeichnet vom {@code CondenserPoweredRenderer}.
 */
public class MachineCondenserPoweredBlock extends DummyableMachineBlock implements com.hbm_m.interfaces.ILookOverlay {

    public MachineCondenserPoweredBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(2, 0, 1, 1, 3, 3)
                .extra(0, 1, 3)
                .extra(0, 1, -3)
                .extra(1, 1, 1)
                .extra(1, 1, -1)
                .extra(-1, 1, 1)
                .extra(-1, 1, -1)
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
        return new MachineCondenserPoweredBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.CONDENSER_POWERED_BE.get(),
                (lvl, pos, st, be) -> MachineCondenserPoweredBlockEntity.tick(lvl, pos, st, be));
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof MachineCondenserPoweredBlockEntity tower)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal(com.hbm_m.util.BobMathUtil.getShortNumber(tower.getEnergyStored()) + "HE / "
                + com.hbm_m.util.BobMathUtil.getShortNumber(MachineCondenserPoweredBlockEntity.maxPower) + "HE"));

        for (int i = 0; i < tower.tanks.length; i++) {
            text.add(Component.literal(i < 1 ? "-> " : "<- ").withStyle(i < 1 ? ChatFormatting.GREEN : ChatFormatting.RED)
                    .append(FluidType.forFluid(tower.tanks[i].getTankType()).getLocalizedName().copy().withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */)
                    .append(Component.literal(": " + String.format(Locale.US, "%,d", tower.tanks[i].getFill()) + "/"
                            + String.format(Locale.US, "%,d", tower.tanks[i].getMaxFill()) + "mB").withStyle(ChatFormatting.WHITE) /* 1.7 §r = Grundfarbe */));
        }

        com.hbm_m.interfaces.ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> list, TooltipFlag flag) {
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineCondenserPoweredBlock> CODEC = simpleCodec(MachineCondenserPoweredBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
