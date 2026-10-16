package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineSawmillBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Sawmill - Direktport des 1.7.10 Originals ({@code MachineSawmill}/{@code TileEntitySawmill})
 * als einzelner Block. Kein Spieler-GUI (siehe {@link MachineSawmillBlockEntity}) - rein
 * automatisierungsgesteuert per Hopper/ItemHandler-Capability, genau wie im Original.
 */
public class MachineSawmillBlock extends BaseEntityBlock implements com.hbm_m.interfaces.ILookOverlay {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public MachineSawmillBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineSawmillBlockEntity(pos, state);
    }

    /**
     * Fadenkreuz-HUD: Port von {@code MachineSawmill.printHook} - Fortschrittsbalken und
     * Slotbelegung (Eingang "->", Ausgaenge "<-"). Die Original-Zeilen fuer Hitze ("TU/t",
     * Prozent, OVERSPEED) und die fehlende Saegeblatt-Klinge entfallen: dieses Port arbeitet
     * passiv ohne Heiznetzwerk und Klinge (siehe {@link MachineSawmillBlockEntity}).
     */
    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineSawmillBlockEntity sawmill)) return;

        java.util.List<net.minecraft.network.chat.Component> text = new java.util.ArrayList<>();

        // Original: 25 Balkensegmente, gruen bis zum Limiter (progress * 26 / processingTime),
        // danach weiss. Die Anzeige hier zeichnet jede Zeile einfarbig - darum bleibt der
        // Farbwechsel als Leerstellen-Form erhalten (gefuellt "▏", restlich " ").
        int limiter = sawmill.progress * 26 / sawmill.getProcessingTime();
        StringBuilder bar = new StringBuilder("[ ");
        for (int i = 0; i < 25; i++) {
            bar.append(i <= limiter ? "▏" : " ");
        }
        bar.append(" ]");
        text.add(net.minecraft.network.chat.Component.literal(bar.toString())
                .withStyle(net.minecraft.ChatFormatting.GREEN));

        for (int i = 0; i < 3; i++) {
            ItemStack stack = sawmill.getInventory().getStackInSlot(i);
            if (!stack.isEmpty()) {
                text.add(net.minecraft.network.chat.Component.literal(i == 0 ? "-> " : "<- ")
                        .withStyle(i == 0 ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED)
                        .append(stack.getHoverName())
                        .append(stack.getCount() > 1 ? " x" + stack.getCount() : ""));
            }
        }

        com.hbm_m.interfaces.ILookOverlay.printGeneric(guiGraphics,
                net.minecraft.network.chat.Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MachineSawmillBlockEntity sawmill) {
                sawmill.drops();
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.SAWMILL_BE.get(), MachineSawmillBlockEntity::tick);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineSawmillBlock> CODEC = simpleCodec(MachineSawmillBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
