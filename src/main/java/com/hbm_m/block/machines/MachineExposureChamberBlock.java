package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachineExposureChamberBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code MachineExposureChamber}: Kammer {@code {4,0,2,2,2,2}} ({@code getOffset 2}), daneben die Beschleuniger-
 * strecke nach {@code -rot} ({@code rot = dir.getRotation(UP)}) mit Magnetringen und dem Endstueck bei 7-8 Bloecken;
 * dort sitzen die fuenf Anschlusszellen. Gezeichnet vom {@code ExposureChamberRenderer}.
 */
public class MachineExposureChamberBlock extends DummyableMachineBlock {

    public MachineExposureChamberBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // Original: rot = dir.getRotation(UP).getOpposite() - im Builder also negative side-Werte
        return DummyableStructureBuilder.create()
                .box(4, 0, 2, 2, 2, 2)
                .box(3, 0, 0, 0, -3, 8)
                .boxAt(0, 2, 0, 0, 0, 1, -1, -3, 6)
                .boxAt(0, 2, 0, 0, 0, -1, 1, -3, 6)
                .boxAt(0, 0, -7, 3, 0, 1, -1, 0, 1)
                .boxAt(0, 0, -7, 3, 0, -1, 1, 0, 1)
                .extra(1, 0, -7)
                .extra(-1, 0, -7)
                .extra(1, 0, -8)
                .extra(-1, 0, -8)
                .extra(0, 0, -8)
                .placementOffset(2)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineExposureChamberBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.EXPOSURE_CHAMBER_BE.get(),
                (lvl, pos, st, be) -> MachineExposureChamberBlockEntity.tick(lvl, pos, st, be));
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.sidedSuccess(level.isClientSide()); // Original standardOpenBehavior: geschlichen true ohne GUI
        return open(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.sidedSuccess(level.isClientSide()); // Original standardOpenBehavior: geschlichen true ohne GUI
        return open(level, pos, player);
    }
    *///?}

    private InteractionResult open(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MachineExposureChamberBlockEntity chamber) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, chamber, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineExposureChamberBlock> CODEC = simpleCodec(MachineExposureChamberBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
