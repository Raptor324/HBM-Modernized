package com.hbm_m.block.machines;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineCapacitorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

/** Port of {@code MachineCapacitor} (1.7.10 Original). Directional HE buffer, no GUI. */
public class MachineCapacitorBlock extends BaseEntityBlock implements com.hbm_m.interfaces.ILookOverlay {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private final long capacity;

    public MachineCapacitorBlock(Properties properties, long capacity) {
        super(properties);
        this.capacity = capacity;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public long getCapacity() {
        return capacity;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineCapacitorBlockEntity(pos, state);
    }

    /** Port of {@code MachineCapacitor.printHook}: charge level, percent and per-tick rates. */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineCapacitorBlockEntity battery)) return;

        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();
        text.add(net.minecraft.network.chat.Component.literal(
                com.hbm_m.util.EnergyFormatter.format(battery.getEnergyStored()) + " / "
                        + com.hbm_m.util.EnergyFormatter.format(battery.getMaxEnergyStored()) + "HE"));

        double percent = (double) battery.getEnergyStored() / (double) battery.getMaxEnergyStored();
        int charge = (int) Math.floor(percent * 10_000D);
        int color = ((int) (0xFF - 0xFF * percent)) << 16 | ((int) (0xFF * percent) << 8);
        text.add(net.minecraft.network.chat.Component.literal((charge / 100D) + "%")
                .withStyle(style -> style.withColor(net.minecraft.network.chat.TextColor.fromRgb(color))));
        text.add(net.minecraft.network.chat.Component.literal("-> ").withStyle(net.minecraft.ChatFormatting.GREEN)
                .append(net.minecraft.network.chat.Component.literal("+" + com.hbm_m.util.EnergyFormatter.format(battery.getReceiveSpeed()) + "HE/t")));
        text.add(net.minecraft.network.chat.Component.literal("<- ").withStyle(net.minecraft.ChatFormatting.RED)
                .append(net.minecraft.network.chat.Component.literal("-" + com.hbm_m.util.EnergyFormatter.format(battery.getProvideSpeed()) + "HE/t")));

        com.hbm_m.interfaces.ILookOverlay.printGeneric(guiGraphics,
                net.minecraft.network.chat.Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    //? if >1.20.1 {
    /*@Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return simpleCodec(p -> new MachineCapacitorBlock(p, this.capacity));
    }
    *///?}

}
