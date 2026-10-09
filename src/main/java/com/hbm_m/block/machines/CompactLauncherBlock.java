package com.hbm_m.block.machines;

import com.hbm_m.platform.StackNbt;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.CompactLauncherBlockEntity;
import com.hbm_m.blockentity.machines.CustomLauncherBlockEntity;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code CompactLauncher}: 3x3-Werfer fuer Baukastenraketen. Die vier Ecken sind Anschluesse ({@code dummy_port}),
 * die Kanten Platten. Wird meist vom {@code struct_launcher_core} errichtet und gibt beim Abbau nur diesen Kern zurueck.
 */
public class CompactLauncherBlock extends DummyableMachineBlock implements IBomb {

    public CompactLauncherBlock(Properties props) {
        super(props);
    }

    @Override
    protected com.hbm_m.multiblock.MultiblockStructureHelper defineStructure() {
        var b = com.hbm_m.multiblock.DummyableStructureBuilder.create().box(0, 0, 1, 1, 1, 1);
        b.extra(1, 0, 1).extra(1, 0, -1).extra(-1, 0, 1).extra(-1, 0, -1);
        return b.placementOffset(0).build(() -> ModBlocks.STRUCT_LAUNCHER.get().defaultBlockState());
    }

    /** Das Original kennt keine Ausrichtung. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, Direction.NORTH);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CompactLauncherBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.COMPACT_LAUNCHER_BE.get(), CustomLauncherBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (StackNbt.hasCustomName(stack) && level.getBlockEntity(pos) instanceof CustomLauncherBlockEntity be) {
            be.setCustomName(stack.getHoverName().getString());
        }
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
        if (player.isShiftKeyDown()) return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS; // Original: Client true, Server geschlichen false
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CustomLauncherBlockEntity be) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, be, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof CustomLauncherBlockEntity be) return be.triggerLaunch();
        return BombReturnCode.UNDEFINED;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<CompactLauncherBlock> CODEC = simpleCodec(CompactLauncherBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
