package com.hbm_m.block.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.bomb.IBomb;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.LaunchPadBaseBlockEntity;
import com.hbm_m.blockentity.machines.LaunchPadLargeBlockEntity;
import com.hbm_m.interfaces.IDetonatable;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 {@code LaunchPadLarge} (1.7.10): grosse Startrampe, 9x9x1 ({@code getDimensions {0,0,4,4,4,4}}, Setzversatz 4),
 * acht Anschluesse am Rand (Kern +-4/+-2 und +-2/+-4), oeffnet die Rampen-GUI, startet ueber Detonator/Redstone.
 * Darstellung (Rampe, Aufrichter, Rakete) zeichnet {@code LaunchPadLargeRenderer}.
 */
public class LaunchPadLargeBlock extends DummyableMachineBlock implements IBomb, IDetonatable {

    /** w16b: Original {@code LaunchPadLarge.bounding} (detaillierte Hitbox, um die Kernmitte), je FACING gedreht. */
    private static final java.util.Map<net.minecraft.core.Direction, net.minecraft.world.phys.shapes.VoxelShape> BOUNDING_W16B =
            com.hbm_m.multiblock.MultiblockStructureHelper.boundingMasters(new double[][] {
            {-4.5D, 0D, -4.5D, 4.5D, 1D, -0.5D},
            {-4.5D, 0D, 0.5D, 4.5D, 1D, 4.5D},
            {-4.5D, 0.875D, -0.5D, 4.5D, 1D, 0.5D}
    });

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getCustomMasterVoxelShape(BlockState state) {
        return BOUNDING_W16B.get(state.getValue(FACING));
    }

    public LaunchPadLargeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // TODO(port): bounding des Originals (Mittelstreifen nur 1/8 hoch) - der Builder kennt keine Teilformen.
        return DummyableStructureBuilder.create()
                .box(0, 0, 4, 4, 4, 4)
                .extra(4, 0, 2)
                .extra(4, 0, -2)
                .extra(-4, 0, 2)
                .extra(-4, 0, -2)
                .extra(2, 0, 4)
                .extra(-2, 0, 4)
                .extra(2, 0, -4)
                .extra(-2, 0, -4)
                .placementOffset(4)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaunchPadLargeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.LAUNCH_PAD_LARGE_BE.get(), LaunchPadLargeBlockEntity::tick);
    }

    @Override
    public boolean onDetonate(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide) return false;
        BombReturnCode result = explode(level, pos);
        return result != null && result.wasSuccessful();
    }

    /** Original {@code explode}: launchFromDesignator am Kern. */
    @Override
    public BombReturnCode explode(Level level, BlockPos pos) {
        if (level.isClientSide) return BombReturnCode.UNDEFINED;
        if (level.getBlockEntity(pos) instanceof LaunchPadBaseBlockEntity launchPad) {
            return launchPad.triggerLaunch();
        }
        return BombReturnCode.UNDEFINED;
    }

    /** Original {@code onNeighborBlockChange}: updateRedstonePower am Kern. */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof LaunchPadBaseBlockEntity launchPad) {
            launchPad.checkRedstonePower();
        }
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.sidedSuccess(level.isClientSide()); // Original standardOpenBehavior: geschlichen true ohne GUI
        return openMenu(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.sidedSuccess(level.isClientSide()); // Original standardOpenBehavior: geschlichen true ohne GUI
        return openMenu(level, pos, player);
    }
    *///?}

    /** Original {@code standardOpenBehavior}. */
    private InteractionResult openMenu(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, provider, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<LaunchPadLargeBlock> CODEC = simpleCodec(LaunchPadLargeBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
