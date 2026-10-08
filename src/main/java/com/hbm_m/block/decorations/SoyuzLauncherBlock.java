package com.hbm_m.block.decorations;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.SoyuzLauncherBlockEntity;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code SoyuzLauncher} (BlockDummyable): die Sojus-Startrampe. Immer nach Osten ausgerichtet, Kern vier Bloecke
 * ueber dem Setzpunkt. Struktur aus den sieben Quadern von {@code getAllDimensions} (Tisch 13x13x2, vier Beine,
 * Stuetze und Turmsockel); der Rand des Tisches auf Kernhoehe sind Anschluesse. Wird normalerweise vom
 * {@code struct_soyuz_core} errichtet; beim Abbau faellt kein Block, sondern das Baumaterial heraus.
 */
public class SoyuzLauncherBlock extends DummyableMachineBlock {

    public static final int HEIGHT = 4;

    public SoyuzLauncherBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    protected com.hbm_m.multiblock.MultiblockStructureHelper defineStructure() {
        var b = com.hbm_m.multiblock.DummyableStructureBuilder.create()
                .box(0, 1, 6, 6, 6, 6)
                .box(-2, 4, -3, 6, -3, 6)
                .box(-2, 4, 6, -3, -3, 6)
                .box(-2, 4, 6, -3, 6, -3)
                .box(-2, 4, -3, 6, 6, -3)
                .box(0, 4, 1, 1, -6, 8)
                .box(0, 4, 2, 2, 9, -5);
        for (int ix = -6; ix <= 6; ix++) for (int iz = -6; iz <= 6; iz++) {
            if (ix == 6 || ix == -6 || iz == 6 || iz == -6) b.extra(iz, 0, ix);
        }
        return b.placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    /** {@code ForgeDirection dir = ForgeDirection.EAST} - die Rampe dreht sich nie. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, Direction.EAST);
    }

    @Override
    public int getHeightOffset() {
        return HEIGHT;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SoyuzLauncherBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return createTickerHelper(type, ModBlockEntities.SOYUZ_LAUNCHER_BE.get(), SoyuzLauncherBlockEntity::clientTick);
        return createTickerHelper(type, ModBlockEntities.SOYUZ_LAUNCHER_BE.get(), SoyuzLauncherBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.PASS; // Original: Client true, Server geschlichen false
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SoyuzLauncherBlockEntity be) {
            com.hbm_m.handler.BossSpawnHandler.markFBI((ServerPlayer) player);
            MenuRegistry.openExtendedMenu((ServerPlayer) player, be, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Original {@code breakBlock}: Inhalt und das gesamte Baumaterial samt Strukturkern fallen heraus. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
            for (int l = 0; l < 6; l++) Containers.dropItemStack(level, x, y, z, new ItemStack(ModBlocks.STRUCT_LAUNCHER.get(), 64));
            for (int l = 0; l < 4; l++) Containers.dropItemStack(level, x, y, z, new ItemStack(ModBlocks.CONCRETE_SMOOTH.get(), 64));
            for (int l = 0; l < 6; l++) Containers.dropItemStack(level, x, y, z, new ItemStack(ModBlocks.STRUCT_SCAFFOLD.get(), 64));
            Containers.dropItemStack(level, x, y, z, new ItemStack(ModBlocks.STRUCT_LAUNCHER.get(), 30));
            Containers.dropItemStack(level, x, y, z, new ItemStack(ModBlocks.STRUCT_SCAFFOLD.get(), 63));
            Containers.dropItemStack(level, x, y, z, new ItemStack(ModBlocks.CONCRETE_SMOOTH.get(), 38));
            Containers.dropItemStack(level, x, y, z, new ItemStack(ModBlocks.STRUCT_SOYUZ_CORE.get(), 1));
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<SoyuzLauncherBlock> CODEC = simpleCodec(SoyuzLauncherBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
