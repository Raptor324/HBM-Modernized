package com.hbm_m.block.gas;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code com.hbm.blocks.generic.BlockOutgas} (1.7.10): ein Block, der von sich aus
 * Gas in die umliegende Luft abgibt.
 *
 * <p>Statt der Blockidentitaet im {@code getGas()}-Wasserfall des Originals bekommt jede Instanz
 * ihr Gas hier direkt als {@link Supplier} - das Ergebnis ist dasselbe, nur ohne die lange
 * Fallunterscheidung.</p>
 *
 * <p>Das Original leitet von {@code BlockOre} ab. In diesem Port geben die betroffenen Erze ueber
 * {@code DropExperienceBlock} ohnehin 0 Erfahrung ab, darum reicht {@link Block} als Basis.</p>
 */
public class OutgasBlock extends Block {

    private final Supplier<Block> gas;
    /** Original: {@code onBreak} - beim Abbau bleibt Gas an der Stelle des Blocks zurueck. */
    private final boolean onBreak;
    /** Original: {@code onNeighbour} - eine Nachbaraenderung stoesst eine Gaswolke aus. */
    private final boolean onNeighbour;
    /** Original: der Sonderfall {@code ancient_scrap} in {@code breakBlock} - 5x5x5-Wolke. */
    private final boolean burstOnBreak;
    /** Original: {@code onEntityWalking} - nur die Asbestvarianten wirbeln beim Betreten Staub auf. */
    private final boolean dustOnStep;

    public OutgasBlock(Properties props, Supplier<Block> gas, boolean randomTick, boolean onBreak) {
        this(props, gas, randomTick, onBreak, false, false, false);
    }

    public OutgasBlock(Properties props, Supplier<Block> gas, boolean randomTick, boolean onBreak,
                       boolean onNeighbour, boolean burstOnBreak, boolean dustOnStep) {
        super(randomTick ? props.randomTicks() : props);
        this.gas = gas;
        this.onBreak = onBreak;
        this.onNeighbour = onNeighbour;
        this.burstOnBreak = burstOnBreak;
        this.dustOnStep = dustOnStep;
    }

    private BlockState gasState() {
        return gas.get().defaultBlockState();
    }

    /** Setzt Gas, wenn dort Luft ist. */
    private void gasIfAir(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).isAir()) {
            level.setBlock(pos, gasState(), 3);
        }
    }

    /**
     * Original: {@code updateTick} - eine der sechs Richtungen wird ausgewuerfelt und, wenn dort
     * Luft ist, mit Gas gefuellt.
     *
     * <p>Der Parameter {@code rate} des Originals steuert nur {@code tickRate()} und damit geplante
     * Ticks; diese Bloecke laufen aber ueber Zufallsticks, weshalb er dort wie hier folgenlos
     * bleibt.</p>
     */
    @Override
    @Deprecated
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        gasIfAir(level, pos.relative(Direction.from3DDataValue(random.nextInt(6))));
    }

    /** Original: {@code onEntityWalking} - Asbeststaub wird beim Darueberlaufen aufgewirbelt. */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);

        if (!dustOnStep || !(level instanceof ServerLevel serverLevel)) return;

        BlockPos above = pos.above();
        if (level.getBlockState(above).isAir() && level.random.nextInt(10) == 0) {
            serverLevel.setBlock(above, gasState(), 3);
        }
    }

    /** Original: {@code dropBlockAsItemWithChance} bzw. {@code breakBlock}. */
    @Override
    public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropXp) {
        super.spawnAfterBreak(state, level, pos, tool, dropXp);

        if (onBreak) {
            gasIfAir(level, pos);
        }

        if (burstOnBreak) {
            // Original: 5x5x5 um den Block herum, gefiltert ueber |dx+dy+dz| in (0, 5).
            for (int ix = -2; ix <= 2; ix++) {
                for (int iy = -2; iy <= 2; iy++) {
                    for (int iz = -2; iz <= 2; iz++) {
                        int sum = Math.abs(ix + iy + iz);
                        if (sum > 0 && sum < 5) {
                            gasIfAir(level, pos.offset(ix, iy, iz));
                        }
                    }
                }
            }
        }
    }

    /** Original: {@code onNeighborBlockChange} - mit 1/3 blaest der Block nach allen Seiten aus. */
    @Override
    @Deprecated
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);

        if (!onNeighbour || !(level instanceof ServerLevel serverLevel)) return;

        if (level.random.nextInt(3) == 0) {
            for (Direction dir : Direction.values()) {
                gasIfAir(serverLevel, pos.relative(dir));
            }
        }
    }
}
