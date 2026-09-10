package com.hbm_m.blockentity.machines.icf;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1-Port von {@code TileEntityICFController} (1.7.10): das Steuerpult des ICF-Lasers.
 *
 * <p>Der Laser ist kein festes Bauwerk, sondern ein <b>frei geformter Klumpen</b>: das Pult sucht
 * sich von seiner Rueckseite aus alles zusammen, was aus Laserbauteilen besteht, und haelt an der
 * Aussenhaut ({@link com.hbm_m.block.machines.icf.ICFLaserPart#CASING Huelle} und
 * {@link com.hbm_m.block.machines.icf.ICFLaserPart#PORT Anschluss}) an. Danach zaehlt es die
 * Bauteile - aber nur die, die auch <b>angebunden</b> sind:</p>
 *
 * <ul>
 *   <li><b>Zellen</b> zaehlen nur in gerader Linie hinter dem Pult, ohne Unterbrechung.</li>
 *   <li><b>Strahler</b> zaehlen nur, wenn sie an einer solchen Zelle liegen.</li>
 *   <li><b>Kondensatoren</b> nur an einem gezaehlten Strahler - sie geben die Speicherleistung.</li>
 *   <li><b>Turbolader</b> nur an einem gezaehlten Kondensator - sie heben sie weiter an.</li>
 * </ul>
 *
 * <p>Die Speicherleistung ist {@code sqrt(Kondensatoren) * 2.500.000 + sqrt(min(Turbolader,
 * Kondensatoren)) * 5.000.000} - beides mit Wurzel, damit sich Massenbau nicht lohnt, und die
 * Turbolader gedeckelt auf die Zahl der Kondensatoren.</p>
 *
 * <p>Jeden Tick verschiesst das Pult seinen ganzen Energiestand als Strahl nach vorn: bis zu
 * fuenfzig Bloecke weit, alles im Weg mit einem Sprengwiderstand unter 6000 wird zerstoert, alle
 * Kreaturen im Strahl nehmen fuenfzig Schaden und brennen. Trifft er den Reaktor, geht die
 * Leistung als {@code laser} dorthin.</p>
 *
 * <p><b>Abweichung:</b> die Energieanbindung laeuft ueber die Anschlussbloecke, die dieser Port
 * als zusaetzliche Netzanschluesse der Maschine meldet - im Original macht das jeder
 * Anschlussblock fuer sich.</p>
 */
public class ICFControllerBlockEntity extends BaseMachineBlockEntity {

    /** Original: {@code capacitorPower = 2_500_000}. */
    public static final int CAPACITOR_POWER = 2_500_000;
    /** Original: {@code turboPower = 5_000_000}. */
    public static final int TURBO_POWER = 5_000_000;
    /** Original: der Strahl reicht bis zu fuenfzig Bloecke. */
    public static final int MAX_LASER_LENGTH = 50;
    /** Original: was haerter ist als das, haelt dem Strahl stand. */
    private static final float BLAST_RESISTANCE_LIMIT = 6000F;

    private int laserLength;

    private int cellCount;
    private int emitterCount;
    private int capacitorCount;
    private int turbochargerCount;

    private final List<BlockPos> ports = new ArrayList<>();
    private boolean assembled;

    public ICFControllerBlockEntity(BlockPos pos, BlockState state) {
        // Die Speicherleistung ergibt sich aus dem Aufbau, siehe getMaxEnergyStored().
        super(ModBlockEntities.ICF_CONTROLLER_BE.get(), pos, state, 0, 0L, Long.MAX_VALUE, 0L);
    }

    // ── Aufbau ──────────────────────────────────────────────────────────────

    public boolean isAssembled() { return assembled; }

    public void setAssembled(boolean assembled) {
        this.assembled = assembled;
        setChanged();
    }

    public int getCellCount()         { return cellCount; }
    public int getEmitterCount()      { return emitterCount; }
    public int getCapacitorCount()    { return capacitorCount; }
    public int getTurbochargerCount() { return turbochargerCount; }
    public int getLaserLength()       { return laserLength; }

    /**
     * 1:1-Port von {@code setup}: aus dem gefundenen Klumpen die tatsaechlich angebundenen
     * Bauteile herausrechnen. Die Reihenfolge ist wesentlich - jede Stufe baut auf der vorigen auf.
     */
    public void setup(List<BlockPos> foundPorts, java.util.Set<BlockPos> cells,
                      java.util.Set<BlockPos> emitters, java.util.Set<BlockPos> capacitors,
                      java.util.Set<BlockPos> turbochargers) {

        cellCount = 0;
        emitterCount = 0;
        capacitorCount = 0;
        turbochargerCount = 0;

        Direction dir = getFacing().getOpposite();

        java.util.Set<BlockPos> validCells = new java.util.HashSet<>();
        java.util.Set<BlockPos> validEmitters = new java.util.HashSet<>();
        java.util.Set<BlockPos> validCapacitors = new java.util.HashSet<>();

        // Zellen: nur die ununterbrochene Reihe hinter dem Pult.
        for (int i = 1; i <= cells.size(); i++) {
            BlockPos at = worldPosition.relative(dir, i);
            if (!cells.contains(at)) break;
            cellCount++;
            validCells.add(at);
        }

        // Strahler an einer gueltigen Zelle
        for (BlockPos emitter : emitters) {
            for (Direction offset : Direction.values()) {
                if (validCells.contains(emitter.relative(offset))) {
                    emitterCount++;
                    validEmitters.add(emitter);
                    break;
                }
            }
        }

        // Kondensatoren an einem gueltigen Strahler
        for (BlockPos capacitor : capacitors) {
            for (Direction offset : Direction.values()) {
                if (validEmitters.contains(capacitor.relative(offset))) {
                    capacitorCount++;
                    validCapacitors.add(capacitor);
                    break;
                }
            }
        }

        // Turbolader an einem gueltigen Kondensator
        for (BlockPos turbo : turbochargers) {
            for (Direction offset : Direction.values()) {
                if (validCapacitors.contains(turbo.relative(offset))) {
                    turbochargerCount++;
                    break;
                }
            }
        }

        ports.clear();
        ports.addAll(foundPorts);
        setChanged();
    }

    private Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(HorizontalDirectionalBlock.FACING)
                ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
    }

    /** Original: {@code getMaxPower} - beide Anteile mit Wurzel, Turbolader auf Kondensatoren gedeckelt. */
    @Override
    public long getMaxEnergyStored() {
        return (long) (Math.sqrt(capacitorCount) * CAPACITOR_POWER
                + Math.sqrt(Math.min(turbochargerCount, capacitorCount)) * TURBO_POWER);
    }

    /** Die Anschlussbloecke sind die Netzanschluesse dieser Maschine. */
    @Override
    protected BlockPos[] getExtraEnergyPorts() {
        return ports.toArray(new BlockPos[0]);
    }

    // ── Tick ────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, ICFControllerBlockEntity be) {
        if (level.isClientSide()) return;

        be.ensureNetworkInitialized();

        if (!be.assembled || be.getEnergyStored() <= 0) {
            be.laserLength = 0;
            be.sendUpdateToClient();
            return;
        }

        Direction dir = be.getFacing();
        long power = be.getEnergyStored();

        for (int i = 1; i < MAX_LASER_LENGTH; i++) {
            be.laserLength = i;
            BlockPos at = pos.relative(dir, i);
            BlockState hit = level.getBlockState(at);

            // Der Reaktor selbst: sein Kern liegt acht Bloecke weiter und drei tiefer.
            if (hit.is(ModBlocks.ICF.get())) {
                BlockEntity target = level.getBlockEntity(pos.relative(dir, i + 8).below(3));
                if (target instanceof MachineICFBlockEntity icf) {
                    icf.addLaser(power, be.getMaxEnergyStored());
                    break;
                }
            }

            if (!hit.isAir()) {
                // Original: alles unter 6000 Sprengwiderstand wird weggebrannt.
                if (hit.getBlock().getExplosionResistance() < BLAST_RESISTANCE_LIMIT) {
                    level.destroyBlock(at, false);
                }
                break;
            }
        }

        // Alles im Strahl nimmt Schaden und brennt.
        BlockPos end = pos.relative(dir, be.laserLength);
        AABB beam = new AABB(
                Math.min(pos.getX(), end.getX()) + 0.2, Math.min(pos.getY(), end.getY()) + 0.2,
                Math.min(pos.getZ(), end.getZ()) + 0.2,
                Math.max(pos.getX(), end.getX()) + 0.8, Math.max(pos.getY(), end.getY()) + 0.8,
                Math.max(pos.getZ(), end.getZ()) + 0.8);

        for (Entity e : level.getEntitiesOfClass(Entity.class, beam)) {
            // Original: {@code DamageSource.inFire}, 50 Schaden und fuenf Sekunden Feuer.
            e.hurt(level.damageSources().inFire(), 50F);
            e.setSecondsOnFire(5);
        }

        be.setEnergyStored(0L);
        be.setChanged();
        be.sendUpdateToClient();
    }

    // ── Speichern ───────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("assembled", assembled);
        tag.putInt("cellCount", cellCount);
        tag.putInt("emitterCount", emitterCount);
        tag.putInt("capacitorCount", capacitorCount);
        tag.putInt("turbochargerCount", turbochargerCount);
        tag.putInt("laserLength", laserLength);

        tag.putInt("portCount", ports.size());
        for (int i = 0; i < ports.size(); i++) {
            BlockPos port = ports.get(i);
            tag.putIntArray("p" + i, new int[] { port.getX(), port.getY(), port.getZ() });
        }
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        assembled = tag.getBoolean("assembled");
        cellCount = tag.getInt("cellCount");
        emitterCount = tag.getInt("emitterCount");
        capacitorCount = tag.getInt("capacitorCount");
        turbochargerCount = tag.getInt("turbochargerCount");
        laserLength = tag.getInt("laserLength");

        ports.clear();
        int portCount = tag.getInt("portCount");
        for (int i = 0; i < portCount; i++) {
            int[] port = tag.getIntArray("p" + i);
            if (port.length == 3) ports.add(new BlockPos(port[0], port[1], port[2]));
        }
    }

    @Override
    protected boolean isItemValidForSlot(int slot, net.minecraft.world.item.ItemStack stack) {
        return false;
    }

    /** Das Steuerpult hat keine Oberflaeche - ein Klick startet nur den Zusammenbau. */
    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
            int id, net.minecraft.world.entity.player.Inventory inv,
            net.minecraft.world.entity.player.Player player) {
        return null;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.icf_controller");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }
}
