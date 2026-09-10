package com.hbm_m.blockentity.machines.icf;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IMultiblockController;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1-Port von {@code TileEntityICFStruct} (1.7.10): der Aufbaukern des ICF-Reaktors.
 *
 * <p>Der Reaktor laesst sich nicht setzen - man <b>mauert ihn</b>. Der Kern prueft alle zwanzig
 * Ticks, ob rings um ihn die vollstaendige Huelle aus ICF-Bauteilen steht; erst dann verwandelt er
 * sich in den eigentlichen Reaktor und fuellt dessen Multiblock aus.</p>
 *
 * <p>Die Huelle laeuft {@value #HALF_LENGTH} Bloecke nach beiden Seiten und besteht aus drei
 * Sorten: dem blanken <b>Bauteil</b> fuer den Mittelgang, der <b>verschweissten Gefaesswand</b> in
 * den mittleren fuenf Scheiben und dem <b>vernieteten Bauteil</b> weiter aussen. Fehlt irgendwo
 * ein Block oder ist er noch nicht fertiggestellt, bricht die Pruefung ab und nichts passiert -
 * eine Rueckmeldung gibt das Original an dieser Stelle nicht.</p>
 *
 * <p>Die Pruefpositionen stammen unveraendert aus {@code updateEntity}; {@code cbarp} heisst dort
 * "check block at relative position" und rechnet quer, hoch und laengs zur Blickrichtung.</p>
 */
public class ICFStructBlockEntity extends BlockEntity {

    /** Original: die Schleife laeuft von -8 bis 8. */
    private static final int HALF_LENGTH = 8;
    /** Original: {@code Math.abs(i) <= 2 ? 2 : 4} - innen Gefaesswand, aussen vernietetes Bauteil. */
    private static final int VESSEL_RANGE = 2;

    public ICFStructBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ICF_STRUCT_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ICFStructBlockEntity be) {
        if (level.isClientSide()) return;
        if (level.getGameTime() % 20 != 0) return;

        Direction dir = state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;

        if (!be.checkShell(level, pos, dir)) return;

        // Die Huelle steht - der Kern wird zum Reaktor.
        Block icf = ModBlocks.ICF.get();
        BlockState reactor = icf.defaultBlockState();
        if (reactor.hasProperty(HorizontalDirectionalBlock.FACING)) {
            reactor = reactor.setValue(HorizontalDirectionalBlock.FACING, dir);
        }

        level.setBlock(pos, reactor, 3);

        // Original: {@code fillSpace} setzt anschliessend die Huellzellen des Multiblocks.
        if (icf instanceof IMultiblockController controller && level.getBlockEntity(pos) != null) {
            controller.getStructureHelper().placeStructure(level, pos, dir, controller);
        }
    }

    /** 1:1-Port der Pruefschleife aus {@code updateEntity}. */
    private boolean checkShell(Level level, BlockPos pos, Direction dir) {
        Block plain = ModBlocks.ICF_COMPONENT.get();
        Block vessel = ModBlocks.ICF_COMPONENT_VESSEL_WELDED.get();
        Block bolted = ModBlocks.ICF_COMPONENT_STRUCTURE_BOLTED.get();

        for (int i = -HALF_LENGTH; i <= HALF_LENGTH; i++) {

            // Der Mittelgang: blanke Bauteile ueber, in und unter der Achse.
            if (!check(level, pos, dir, plain, 0, 1, i)) return false;
            if (i != 0 && !check(level, pos, dir, plain, 0, 0, i)) return false;
            if (!check(level, pos, dir, plain, 0, -1, i)) return false;
            if (!check(level, pos, dir, plain, 0, 3, i)) return false;

            // Der Mantel: innen verschweisste Gefaesswand, weiter aussen vernietetes Bauteil.
            Block shell = Math.abs(i) <= VESSEL_RANGE ? vessel : bolted;

            for (int j = -1; j <= 1; j++) if (!check(level, pos, dir, shell, j, 1, i)) return false;
            for (int j = -2; j <= 2; j++) if (!check(level, pos, dir, shell, j, 2, i)) return false;
            for (int j = -2; j <= 2; j++) if (j != 0 && !check(level, pos, dir, shell, j, 3, i)) return false;
            for (int j = -2; j <= 2; j++) if (!check(level, pos, dir, shell, j, 4, i)) return false;
            for (int j = -1; j <= 1; j++) if (!check(level, pos, dir, shell, j, 5, i)) return false;
        }

        return true;
    }

    /**
     * 1:1-Port von {@code cbarp} - "check block at relative position".
     *
     * @param widthwise Vielfaches der Blickrichtung
     * @param y         Hoehe ueber dem Kern
     * @param lengthwise Vielfaches der Querachse
     */
    private boolean check(Level level, BlockPos pos, Direction dir, Block block,
                          int widthwise, int y, int lengthwise) {
        Direction rot = dir.getClockWise();

        BlockPos at = pos
                .relative(rot, lengthwise)
                .relative(dir, widthwise)
                .above(y);

        return level.getBlockState(at).is(block);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(HALF_LENGTH + 1);
    }
}
