package com.hbm_m.block.machines.dfc;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.dfc.DFCCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import dev.architectury.registry.menu.MenuRegistry;

public class DFCCoreBlock extends BaseEntityBlock {

    public DFCCoreBlock(Properties properties) {
        super(properties);
    }

    /** Original {@code isOpaqueCube() = false}: der unsichtbare Block darf Nachbarflaechen nicht verdecken. */
    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getOcclusionShape(BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED; // Kern-Kugel zeichnet DFCRenderers.Core (RenderCore)
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DFCCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, ModBlockEntities.DFC_CORE_BE.get(), DFCCoreBlockEntity::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && player.isShiftKeyDown()) return InteractionResult.PASS; // Original: geschlichen auf dem Server false
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider menuProvider) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, menuProvider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player.isShiftKeyDown()) return InteractionResult.PASS; // Original: geschlichen auf dem Server false
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider menuProvider) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, menuProvider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
    *///?}

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<DFCCoreBlock> CODEC = simpleCodec(DFCCoreBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
