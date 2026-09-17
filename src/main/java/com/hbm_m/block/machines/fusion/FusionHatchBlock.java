package com.hbm_m.block.machines.fusion;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code FusionHatch} (1.7.10).
 *
 * <p>Die Luke hat drei verschiedene Seiten: oben und unten Wolframblock, auf der Vorderseite die
 * eigentliche Luke, ringsum die Heizerwand. Welche Seite die Vorderseite ist, legt das Original in
 * {@code onBlockPlacedBy} aus der Blickrichtung des Spielers fest ({@code rotationYaw} -> Metadaten
 * 2-5). Im Port war der Block ein Platzhalter ohne jede Richtung, der auf allen sechs Seiten die
 * Lukentextur trug.</p>
 *
 * <p>Wie im Original gilt: {@code setCreativeTab(null)} und {@code @Deprecated} - die Luke ist ein
 * Altbestand und wird von keinem Rezept mehr erzeugt, existiert aber weiter, damit alte Welten sie
 * behalten.</p>
 */
public class FusionHatchBlock extends Block {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public FusionHatchBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Original: die Luke schaut den Spieler an, der sie setzt.
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
