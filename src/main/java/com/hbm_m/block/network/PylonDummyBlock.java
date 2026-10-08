package com.hbm_m.block.network;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.network.PylonDummyBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Невидимая часть корпуса пилона (аналог dummy-блоков BlockDummyable из 1.7.10):
 * полный блок коллизии, без модели. При разрушении рушит ядро пилона.
 */
public class PylonDummyBlock extends Block implements EntityBlock {

    public PylonDummyBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(0, 0, 0, 16, 16, 16);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PylonDummyBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        super.onRemove(state, level, pos, newState, isMoving);
        if (!level.isClientSide && newState.getBlock() != this) {
            breakCore(level, pos);
        }
    }

    //? if < 1.21.1 {
    /** audit13: Original {@code BlockDummyable.onBlockHarvested} - Abbau einer Mastzelle droppt den Mast einmal (nicht im Kreativmodus). */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
        if (!level.isClientSide) {
            BlockPos core = findCore(level, pos);
            if (core != null) level.destroyBlock(core, !player.getAbilities().instabuild);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    /** audit13: Original {@code PylonRedWire.onBlockActivated -> findCore}: Faerben ueber jede Zelle. */
    @Override
    public net.minecraft.world.InteractionResult use(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
                                                     net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        BlockPos core = findCore(level, pos);
        if (core == null) return net.minecraft.world.InteractionResult.PASS;
        return level.getBlockState(core).use(level, player, hand, hit.withPosition(core));
    }

    /** audit13: Pick-Block auf einer Mastzelle gibt den Mast. */
    @Override
    public net.minecraft.world.item.ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        if (level instanceof Level lvl) {
            BlockPos core = findCore(lvl, pos);
            if (core != null) {
                BlockState coreState = level.getBlockState(core);
                return coreState.getBlock().getCloneItemStack(level, core, coreState);
            }
        }
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
    //?}

    /** При разрушении части — рушим ядро (оно в свою очередь убирает остальные части). */
    private static void breakCore(Level level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        if (core != null) {
            level.destroyBlock(core, false);
        }
    }

    /** Ищет ядро по сохранённой позиции, иначе — сканирует вниз по колонне. */
    @Nullable
    public static BlockPos findCore(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof PylonDummyBlockEntity dummy && dummy.getCorePos() != null) {
            BlockState core = level.getBlockState(dummy.getCorePos());
            if (core.getBlock() instanceof RedPylonCoreBlock) {
                return dummy.getCorePos();
            }
        }
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int i = 0; i < 20; i++) {
            cursor.move(net.minecraft.core.Direction.DOWN);
            if (level.getBlockState(cursor).getBlock() instanceof RedPylonCoreBlock) {
                return cursor.immutable();
            }
        }
        return null;
    }
}
