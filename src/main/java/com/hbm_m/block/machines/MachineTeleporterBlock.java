package com.hbm_m.block.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineTeleporterBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Port of {@code MachineTeleporter} (1.7.10 Original). No player interaction on the block itself - see {@code ItemTeleLink}. */
public class MachineTeleporterBlock extends BaseEntityBlock implements com.hbm_m.interfaces.ILookOverlay {

    public MachineTeleporterBlock(Properties properties) { super(properties); }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    /**
     * Порт {@code MachineTeleporter.printHook}: без цели — красное "No destination set!", иначе
     * строка энергии (зелёная, если хватает на телепорт) и строка назначения.
     */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineTeleporterBlockEntity tele)) return;

        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();
        if (tele.targetY == -1) {
            text.add(net.minecraft.network.chat.Component.literal("No destination set!")
                    .withStyle(net.minecraft.ChatFormatting.RED));
        } else {
            text.add(net.minecraft.network.chat.Component.literal(
                            String.format(java.util.Locale.US, "%,d", tele.getEnergyStored())
                            + " / " + String.format(java.util.Locale.US, "%,d", tele.getMaxEnergyStored()))
                    .withStyle(tele.getEnergyStored() >= tele.getConsumption()
                            ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED));
            text.add(net.minecraft.network.chat.Component.literal("Destination: " + tele.targetX
                    + " / " + tele.targetY + " / " + tele.targetZ + " (D: " + tele.targetDim + ")"));
        }
        com.hbm_m.interfaces.ILookOverlay.printGeneric(guiGraphics,
                net.minecraft.network.chat.Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineTeleporterBlockEntity(pos, state);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MACHINE_TELEPORTER_BE.get(),
                (lvl, pos, st, be) -> MachineTeleporterBlockEntity.tick(lvl, pos, st, (MachineTeleporterBlockEntity) be));
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineTeleporterBlock> CODEC = simpleCodec(MachineTeleporterBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
