package com.hbm_m.block.machines;

import com.hbm_m.platform.StackNbt;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.CustomLauncherBlockEntity;
import com.hbm_m.blockentity.machines.LaunchTableBlockEntity;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code LaunchTable}: 9x9-Starttisch. {@code FACING} ist die Seite des Geruestturms (Original-Metadaten 0/1/2/3
 * = Ost/West/Sued/Nord, beim Setzen links vom Blick des Spielers); drei Felder in diese Richtung wird die Saeule bis
 * elf Bloecke hoch freigeraeumt. Die Achse zum Turm und alle Felder abseits des Kreuzes sind Anschluesse, die
 * Querachse besteht aus Platten. Beim Abbau bleibt nur der struct_launcher_core_large.
 */
public class LaunchTableBlock extends DummyableMachineBlock implements IBomb {

    public LaunchTableBlock(Properties props) {
        super(props);
    }

    @Override
    protected com.hbm_m.multiblock.MultiblockStructureHelper defineStructure() {
        var b = com.hbm_m.multiblock.DummyableStructureBuilder.create().box(0, 0, 4, 4, 4, 4);
        for (int f = -4; f <= 4; f++) for (int s = -4; s <= 4; s++) {
            if (f == 0) continue; // Querachse = Platten (und der Kern)
            b.extra(f, 0, s);
        }
        return b.placementOffset(0).build(() -> ModBlocks.STRUCT_LAUNCHER.get().defaultBlockState());
    }

    /** Original onBlockPlacedBy: Blick nach Sueden (d=0) setzt den Turm nach Osten usw. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getCounterClockWise());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!state.is(oldState.getBlock()) && !level.isClientSide()) {
            clearTower(level, pos, state.getValue(FACING));
        }
    }

    /** Raeumt die Saeule fuer das Geruest frei (drei Felder Richtung Turm, Hoehe 1 bis 11). */
    public static void clearTower(Level level, BlockPos pos, Direction dir) {
        BlockPos base = pos.relative(dir, 3);
        for (int i = 1; i < 12; i++) level.setBlock(base.above(i), Blocks.AIR.defaultBlockState(), 3);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaunchTableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.LAUNCH_TABLE_BE.get(), CustomLauncherBlockEntity::serverTick);
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
        if (level.isClientSide) return BombReturnCode.UNDEFINED;
        if (level.getBlockEntity(pos) instanceof CustomLauncherBlockEntity be) return be.triggerLaunch();
        return BombReturnCode.UNDEFINED;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<LaunchTableBlock> CODEC = simpleCodec(LaunchTableBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
