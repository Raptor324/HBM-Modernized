package com.hbm_m.block.generic;

import javax.annotation.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.generic.GeysirBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * 1:1 {@code BlockGeysir} ({@code geysir_chlorine}, {@code geysir_nether}): wechselt zwischen Ruhe und Ausbruch
 * (Metadaten 0/1 = {@code ERUPTING}), laesst nichts fallen; der Nether-Geysir flackert oben.
 */
public class BlockGeysir extends BaseEntityBlock {

    public static final BooleanProperty ERUPTING = BooleanProperty.create("erupting");

    private final boolean nether;

    public BlockGeysir(Properties p, boolean nether) {
        super(p);
        this.nether = nether;
        registerDefaultState(stateDefinition.any().setValue(ERUPTING, false));
    }

    public boolean isNether() { return nether; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(ERUPTING); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GeysirBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.GEYSIR.get(), GeysirBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        if (nether) world.addParticle(ParticleTypes.FLAME, pos.getX() + 0.5F, pos.getY() + 1.0625F, pos.getZ() + 0.5F, 0.0D, 0.0D, 0.0D);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockGeysir> CODEC = simpleCodec(p -> new BlockGeysir(p, false));
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
