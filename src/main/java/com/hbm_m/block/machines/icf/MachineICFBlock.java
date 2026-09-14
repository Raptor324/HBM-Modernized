package com.hbm_m.block.machines.icf;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.fusion.FusionMultiblockBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.machines.icf.MachineICFBlockEntity;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code MachineICF} (1.7.10): der Traegheitsfusionsreaktor.
 *
 * <p>Der Aufbau ist gewaltig: ein Grundkasten von fuenf Bloecken Hoehe und siebzehn Bloecken
 * Breite, dazu zwei Fluegel drei Bloecke ueber dem Kern, die nach vorn und nach hinten laufen -
 * zusammen ergibt das die typische Scheibenform. Angeschlossen wird ueber sechs Zellen: eine oben,
 * eine unten und vier an den Ecken der Fluegel.</p>
 *
 * <p>Die Werte stammen unveraendert aus {@code getAllDimensions()} und {@code fillSpace()} des
 * Originals.</p>
 */
public class MachineICFBlock extends FusionMultiblockBlock {

    public MachineICFBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                // Original: getDimensions {5, 0, 1, 1, 8, 8}, Offset 1
                .box(5, 0, 1, 1, 8, 8)
                // Die beiden Fluegel, drei Bloecke ueber dem Kern
                .boxAt(0, 3, 0, 1, 1, -1, 2, 8, 8)
                .boxAt(0, 3, 0, 1, 1, 2, -1, 8, 8)
                // Anschlusszellen: oben und die vier Ecken der Fluegel
                .extra(0, 5, 0)
                .extra(2, 3, 6)
                .extra(2, 3, -6)
                .extra(-2, 3, 6)
                .extra(-2, 3, -6)
                .placementOffset(1)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    protected boolean hasMenu() {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineICFBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.MACHINE_ICF_BE.get(),
                (lvl, pos, st, be) -> MachineICFBlockEntity.tick(lvl, pos, st, (MachineICFBlockEntity) be));
    }
}
