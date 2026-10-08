package com.hbm_m.block.machines.custom;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.machines.custom.CMPortBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * 1:1 die Bauteile des Custom-Machine-Systems ({@code BlockCM}, {@code BlockCMGlass}, {@code BlockCMPort},
 * {@code BlockCMAnchor}) samt ihrer Metadaten-Enums ({@code EnumCMMaterials}, {@code EnumCMEngines},
 * {@code EnumCMCircuit}). Jede Meta ist im Port ein eigener Block {@code cm_<art>_<enum>}; die Reihenfolge der
 * Enum-Konstanten entspricht den Original-Metadaten, damit Konfigurationen mit {@code "metas": [..]} weiter passen.
 */
public final class CMBlocks {

    private CMBlocks() { }

    public enum CMMaterial { STEEL, ALLOY, DESH, TCALLOY; public String id() { return name().toLowerCase(Locale.US); } }
    public enum CMEngine { STANDARD, DESH, BISMUTH; public String id() { return name().toLowerCase(Locale.US); } }
    public enum CMCircuit { ALUMINIUM, COPPER, RED_COPPER, GOLD, SCHRABIDIUM; public String id() { return name().toLowerCase(Locale.US); } }

    /** {@code BlockCMGlass}: durchsichtig, gleiche Nachbarn verbergen ihre Kontaktflaechen. */
    public static class Glass extends Block {
        public Glass(Properties props) { super(props.noOcclusion()); }

        @Override
        public boolean skipRendering(BlockState state, BlockState adjacent, Direction side) {
            return adjacent.is(this) || super.skipRendering(state, adjacent, side);
        }

        @Override
        public float getShadeBrightness(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
            return 1.0F;
        }

        @Override
        public boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
            return true;
        }
    }

    /** {@code BlockCMPort}: {@code TileEntityProxyCombo().inventory().power().fluid()} - reicht alles an die Steuerung. */
    public static class Port extends BaseEntityBlock {
        public Port(Properties props) { super(props); }

        @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CMPortBlockEntity(pos, state);
        }

        //? if >1.20.1 {
        /*public static final com.mojang.serialization.MapCodec<Port> CODEC = simpleCodec(Port::new);
        @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        *///?}
    }

    /** {@code BlockCMAnchor}: reiner Lageanker, Vorderseite zum Setzenden (Meta 2/5/3/4). */
    public static class Anchor extends Block {
        public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

        public Anchor(Properties props) {
            super(props);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        }
    }
}
