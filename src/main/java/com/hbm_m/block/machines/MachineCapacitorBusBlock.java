package com.hbm_m.block.machines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Port of {@code MachineCapacitorBus} (1.7.10 Original). Passiver Orientierungsblock OHNE
 * BlockEntity: er merkt sich nur die Blickrichtung des Spielers beim Platzieren
 * (Original: {@code BlockPistonBase.determineOrientation}) und ist ansonsten tot - gespeist
 * wird am ENDE einer geraden Kette dieser Bloecke, nicht an den Schienen selbst
 * (Kettengang: {@code MachineCapacitorBlockEntity#walkBusChain}).
 */
public class MachineCapacitorBusBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public MachineCapacitorBusBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Original: {@code BlockPistonBase.determineOrientation} - Richtung, in die der Spieler schaut. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    /** Port of {@code MachineCapacitorBus.addInformation} ({@code addStandardInfo}): shift-desc. */
    //? if < 1.21.1 {
    @Override
    public void appendHoverText(ItemStack pStack, @Nullable BlockGetter pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack pStack, net.minecraft.world.item.Item.TooltipContext pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
    *///?}
        super.appendHoverText(pStack, pLevel, pTooltip, pFlag);

        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            for (String line : Component.translatable("tooltip.hbm_m.capacitor_bus.desc").getString().split("\n")) {
                pTooltip.add(Component.literal(line).withStyle(net.minecraft.ChatFormatting.YELLOW));
            }
        } else {
            pTooltip.add(Component.translatable("tooltip.hbm_m.hold_shift_for_details")
                    .withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC));
        }
    }
}
