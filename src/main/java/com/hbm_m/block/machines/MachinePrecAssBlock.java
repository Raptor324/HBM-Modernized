package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachinePrecAssBlockEntity;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code MachinePrecAss} (erbt {@code MachineAssemblyMachine}): {@code getDimensions {2,0,1,1,1,1}}, {@code getOffset 1};
 * die acht Randzellen des Sockels sind Anschluesse.
 */
public class MachinePrecAssBlock extends DummyableMachineBlock {

    public MachinePrecAssBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected com.hbm_m.multiblock.MultiblockStructureHelper defineStructure() {
        var b = com.hbm_m.multiblock.DummyableStructureBuilder.create().box(2, 0, 1, 1, 1, 1);
        for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) {
            if (i != 0 || j != 0) b.extra(i, 0, j);
        }
        return b.placementOffset(1)
                .build(() -> com.hbm_m.block.ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachinePrecAssBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PRECASS_BE.get(), MachinePrecAssBlockEntity::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    //?} else {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack hbmHeld, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return com.hbm_m.platform.BlockUseHooks.item(hbmUse(state, level, pos, player, hand, hit));
    }
    private InteractionResult hbmUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    *///?}
        if (!level.isClientSide() && !player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof MachinePrecAssBlockEntity be) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, be, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachinePrecAssBlock> CODEC = simpleCodec(MachinePrecAssBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
