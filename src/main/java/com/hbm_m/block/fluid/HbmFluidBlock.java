package com.hbm_m.block.fluid;

import java.util.function.Supplier;

import dev.architectury.core.block.ArchitecturyLiquidBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;

/**
 * Basis der Welt-Fluessigkeiten (Port von Forges {@code BlockFluidClassic} in der Form, wie HBM sie
 * benutzt). Die Fluessigkeit selbst ({@link com.hbm_m.inventory.fluid.HbmFlowingFluid}) ruft bei jedem
 * Fliess-Tick {@link #onFluidTick} - das entspricht dem {@code updateTick} des Originals, in dem die
 * Bloecke ihre Umgebung zersetzen. {@link #canDisplace} bildet das {@code canDisplace} des Originals
 * ab und entscheidet, wohin die Fluessigkeit ueberhaupt fliessen darf.
 */
public class HbmFluidBlock extends ArchitecturyLiquidBlock {

    public HbmFluidBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    /** Original {@code updateTick} nach {@code super.updateTick} - pro Fliess-Tick. */
    public void onFluidTick(Level level, BlockPos pos, BlockState state) { }

    /** Original {@code updateTick} ueber {@code setTickRandomly(true)} - Zufallstick. */
    public void onRandomFluidTick(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) { }

    public boolean ticksRandomly() {
        return false;
    }

    /**
     * Original {@code canDisplace}: {@code null} = Vanilla entscheiden lassen, sonst erzwungen.
     * {@code BlockFluidClassic} verdraengt im Original keine anderen Fluessigkeiten, wenn der Block
     * es wie Schlamm/Saeure ausdruecklich verbietet.
     */
    public Boolean canDisplace(BlockGetter level, BlockPos pos, BlockState target) {
        return null;
    }

    /** Original {@code entity.setInWeb()} - Spinnennetz-Bremse. */
    protected static void setInWeb(Entity entity, BlockState state) {
        entity.makeStuckInBlock(state, new Vec3(0.25D, 0.05D, 0.25D));
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, level, pos, block, fromPos, moving);
        if (!level.isClientSide) onNeighborChange(level, pos, state);
    }

    /** Original {@code onNeighborBlockChange} nach {@code super}. */
    protected void onNeighborChange(Level level, BlockPos pos, BlockState state) { }
}
