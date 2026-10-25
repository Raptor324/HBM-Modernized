package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.MachinePyroOvenBlockEntity;
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
 * 1:1 {@code MachinePyroOven}: {@code getDimensions {2,0,3,3,2,2}} (7 lang, 5 breit, 3 hoch), {@code getOffset 3};
 * Anschlusszellen ({@code makeExtra}) sind die fuenf Zellen der Laengsseite ({@code rot * 2}, {@code rot =
 * dir.getRotation(DOWN)}) und der Schornstein oben ({@code -rot}, zwei hoch). Gezeichnet vom {@code PyroOvenRenderer}.
 */
public class MachinePyroOvenBlock extends DummyableMachineBlock {

    public MachinePyroOvenBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // extra(forward, up, side) rechnet mit rot = dir.getRotation(UP); das Original nutzt getRotation(DOWN) = -side.
        DummyableStructureBuilder b = DummyableStructureBuilder.create().box(2, 0, 3, 3, 2, 2);
        for (int i = -2; i <= 2; i++) b.extra(i, 0, -2);
        b.extra(0, 2, 1);
        return b.placementOffset(3).build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachinePyroOvenBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PYROOVEN_BE.get(),
                (lvl, pos, st, be) -> MachinePyroOvenBlockEntity.tick(lvl, pos, st, (MachinePyroOvenBlockEntity) be));
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
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MachinePyroOvenBlockEntity pyro) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, pyro, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachinePyroOvenBlock> CODEC = simpleCodec(MachinePyroOvenBlock::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
