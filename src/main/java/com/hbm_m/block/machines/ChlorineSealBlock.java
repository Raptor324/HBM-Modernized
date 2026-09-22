package com.hbm_m.block.machines;

import com.hbm_m.block.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * 1:1-Port von {@code BlockClorineSeal} + {@code TileEntityChlorineSeal} (1.7.10): solange der
 * Block Redstonestrom bekommt, fuellt er den angrenzenden Hohlraum mit Chlorgas.
 *
 * <p>Das Original nutzt dafuer eine TileEntity, deren {@code updateEntity} jeden Tick laeuft. Hier
 * uebernimmt das ein geplanter Blocktick mit Verzoegerung 1 - gleiche Frequenz, aber ohne eine
 * eigene BlockEntity fuer einen Block ohne jeden Zustand.</p>
 */
public class ChlorineSealBlock extends Block {

    /** Original: {@code if(index > 50) return;} - begrenzt die Rekursionstiefe je Tick. */
    private static final int MAX_DEPTH = 50;

    public ChlorineSealBlock() {
        super(Block.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(5.0F, 10.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops());
    }

    @Override
    @Deprecated
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide() && level.hasNeighborSignal(pos)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    @Deprecated
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                BlockPos fromPos, boolean isMoving) {
        // Nur einplanen, wenn noch nichts laeuft - sonst schaukelt sich das Gas selbst hoch.
        if (!level.isClientSide() && level.hasNeighborSignal(pos)
                && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    @Deprecated
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.hasNeighborSignal(pos)) return;

        spread(level, pos, 0, random);
        level.scheduleTick(pos, this, 1);
    }

    /**
     * Original: {@code TileEntityChlorineSeal.spread} - setzt Chlorgas und laeuft dann in genau
     * eine zufaellige Richtung weiter, solange dort schon Gas oder ein weiterer Versiegler steht.
     */
    private void spread(ServerLevel level, BlockPos pos, int depth, RandomSource random) {
        if (depth > MAX_DEPTH) return;

        BlockState here = level.getBlockState(pos);

        if (here.canBeReplaced()) {
            level.setBlock(pos, ModBlocks.CHLORINE_GAS.get().defaultBlockState(), 3);
            here = level.getBlockState(pos);
        }

        if (!here.is(ModBlocks.CHLORINE_GAS.get()) && !here.is(this)) return;

        spread(level, pos.relative(net.minecraft.core.Direction.from3DDataValue(random.nextInt(6))),
                depth + 1, random);
    }
}
