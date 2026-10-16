package com.hbm_m.blockentity.machines.icf;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Heatable;
import com.hbm_m.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm_m.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.menu.MachineICFMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.machine.ItemICFPellet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code TileEntityICF} (1.7.10): der Traegheitsfusionsreaktor.
 *
 * <p>Er hat keine eigene Energiequelle. Was ihn antreibt, ist der Strahl der ICF-Laser, die auf
 * ihn zielen; jeder meldet seine Leistung als {@link #addLaser(long, long)} an, und die Summe ist
 * das, was in diesem Tick zur Verfuegung steht.</p>
 *
 * <p><b>Der Ablauf.</b> Aus den fuenf Vorratsplaetzen wird eine Kapsel in die Kammer gezogen. Ist
 * die anliegende Laserleistung mindestens so gross wie ihr Zuendaufwand, zuendet sie: die
 * eingestrahlte Leistung wird ihr als Abbrand angeschrieben und kommt mit dem Reaktionsfaktor
 * multipliziert als Hitze zurueck. Nebenbei faellt Sternenfluss an, ein Zehntausendstel der
 * anliegenden Hitze je Tick. Ist die Kapsel erschoepft, wandert sie als verbrauchte Kapsel in die
 * fuenf Ausgabeplaetze.</p>
 *
 * <p>Reicht der Laser <b>nicht</b> zum Zuenden, verpufft er trotzdem nicht ganz: ein Viertel geht
 * als blosse Aufheizung in den Reaktor.</p>
 *
 * <p>Abgefuehrt wird die Hitze wie in einem Waermetauscher: der Kuehlmitteltank hat den
 * {@link FT_Heatable}-Beschlag, dessen erste Stufe sagt, wieviel Hitze wieviel Kuehlmittel in
 * wieviel heisses Kuehlmittel verwandelt. Je Tick darf hoechstens ein Viertel der gespeicherten
 * Hitze weg, und die Hitze selbst verfaellt zusaetzlich um ein Promille je Tick.</p>
 *
 * <p>Waehrend das Pellet brennt, blitzt alle paar Ticks ein Hadronenring ueber dem Reaktor auf,
 * und alle zwanzig Ticks traegt sich die Anlage in die
 * {@link com.hbm_m.satellite.RayScanEvents Ereignisliste} des Scan-Satelliten ein - sie ist aus
 * dem Orbit als Teilchenquelle zu sehen.</p>
 */
public class MachineICFBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    /** 0-4 Vorrat, 5 Kammer, 6-10 Ausgabe, 11 Fluessigkeitskennung. */
    public static final int SLOT_INPUT_START = 0;
    public static final int SLOT_INPUT_END = 4;
    public static final int SLOT_CHAMBER = 5;
    public static final int SLOT_OUTPUT_START = 6;
    public static final int SLOT_OUTPUT_END = 10;
    public static final int SLOT_FLUID_ID = 11;
    public static final int INVENTORY_SIZE = 12;

    /** Original: {@code maxHeat = 1_000_000_000_000L}. */
    public static final long MAX_HEAT = 1_000_000_000_000L;

    private static final int COOLANT_CAPACITY = 512_000;
    private static final int FLUX_CAPACITY = 24_000;

    /** 0 = kaltes Kuehlmittel, 1 = heisses, 2 = Sternenfluss. */
    private final FluidTank[] tanks = new FluidTank[3];

    /** Was die Laser in diesem Tick eingestrahlt haben - wird am Tickende zurueckgesetzt. */
    private long laser;
    private long maxLaser;
    private long heat;
    private long heatup;
    private int consumption;
    private int output;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                // Die Werte reichen weit ueber int hinaus, darum als Promille des Hoechstwerts.
                case 0 -> (int) (maxLaser > 0 ? laser * 1000L / maxLaser : 0L);
                case 1 -> (int) (heat * 1000L / MAX_HEAT);
                case 2 -> consumption;
                case 3 -> output;
                case 4 -> tanks[2].getFill();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) { }

        @Override
        public int getCount() {
            return 5;
        }
    };

    public MachineICFBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_ICF_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
        tanks[0] = new FluidTank(ModFluids.SODIUM.getSource(), COOLANT_CAPACITY);
        tanks[1] = new FluidTank(ModFluids.SODIUM_HOT.getSource(), COOLANT_CAPACITY);
        tanks[2] = new FluidTank(ModFluids.STELLAR_FLUX.getSource(), FLUX_CAPACITY);
    }

    /** Original: die Steuerpulte addieren ihre Leistung direkt auf {@code laser}. */
    public void addLaser(long power, long maxPower) {
        this.laser += power;
        this.maxLaser += maxPower;
    }

    public long getLaser()    { return laser; }
    public long getMaxLaser() { return maxLaser; }
    public long getHeat()     { return heat; }
    public FluidTank[] getTanks() { return tanks; }
    public ContainerData getContainerData() { return data; }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineICFBlockEntity be) {
        if (level.isClientSide()) return;

        for (BlockPos port : be.connectionPositions()) {
            for (Direction dir : Direction.values()) {
                be.trySubscribe(be.tanks[0].getTankType(), level, port.relative(dir), dir);
            }
        }

        be.cyclePellets();
        be.burn(level, pos);
        be.cool();

        for (BlockPos port : be.connectionPositions()) {
            for (Direction dir : Direction.values()) {
                be.tryProvide(be.tanks[1], level, port.relative(dir), dir);
                be.tryProvide(be.tanks[2], level, port.relative(dir), dir);
            }
        }

        // Original: die Hitze verfaellt um ein Promille je Tick und ist gedeckelt.
        be.heat *= 0.999D;
        if (be.heat > MAX_HEAT) be.heat = MAX_HEAT;

        be.setChanged();
        be.sendUpdateToClient();

        // Der Strahl gilt nur fuer diesen Tick.
        be.laser = 0;
        be.maxLaser = 0;
    }

    /** Original: verbrauchte Kapsel raus, frische rein. */
    private void cyclePellets() {
        ItemStack chamber = getInventory().getStackInSlot(SLOT_CHAMBER);

        if (!chamber.isEmpty() && chamber.is(ModItems.ICF_PELLET_DEPLETED.get())) {
            for (int i = SLOT_OUTPUT_START; i <= SLOT_OUTPUT_END; i++) {
                if (getInventory().getStackInSlot(i).isEmpty()) {
                    getInventory().setStackInSlot(i, chamber.copy());
                    getInventory().setStackInSlot(SLOT_CHAMBER, ItemStack.EMPTY);
                    break;
                }
            }
        }

        if (getInventory().getStackInSlot(SLOT_CHAMBER).isEmpty()) {
            for (int i = SLOT_INPUT_START; i <= SLOT_INPUT_END; i++) {
                ItemStack stack = getInventory().getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof ItemICFPellet) {
                    getInventory().setStackInSlot(SLOT_CHAMBER, stack.copy());
                    getInventory().setStackInSlot(i, ItemStack.EMPTY);
                    break;
                }
            }
        }
    }

    /** 1:1-Port des Zuendteils aus {@code updateEntity}. */
    private void burn(Level level, BlockPos pos) {
        heatup = 0;

        ItemStack chamber = getInventory().getStackInSlot(SLOT_CHAMBER);

        if (!chamber.isEmpty() && chamber.getItem() instanceof ItemICFPellet
                && ItemICFPellet.getFusingDifficulty(chamber) <= laser) {

            heatup = ItemICFPellet.react(chamber, laser);
            heat += heatup;

            if (ItemICFPellet.getDepletion(chamber) >= ItemICFPellet.getMaxDepletion(chamber)) {
                getInventory().setStackInSlot(SLOT_CHAMBER, new ItemStack(ModItems.ICF_PELLET_DEPLETED.get()));
            }

            if (level.getGameTime() % 20 == 15) {
                com.hbm_m.satellite.RayScanEvents.reportEvent(level, worldPosition, com.hbm_m.satellite.RayScanEvents.INFO_PARTICLE, 200);
            }

            // Original: ein Hadronenring drei Bloecke ueber dem Reaktor, sichtbar auf 25 Bloecke.
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                net.minecraft.nbt.CompoundTag dPart = new net.minecraft.nbt.CompoundTag();
                dPart.putString("type", "hadron");
                com.hbm_m.particle.helper.IParticleCreator.sendPacket(serverLevel,
                        worldPosition.getX() + 0.5, worldPosition.getY() + 3.5, worldPosition.getZ() + 0.5,
                        25, dPart);
            }

            // Sternenfluss faellt nebenbei an: ein Zehntausendstel der Hitze je Tick, aufgerundet.
            int flux = (int) Math.ceil(heat * 10D / MAX_HEAT);
            tanks[2].setFill(Math.min(tanks[2].getMaxFill(), tanks[2].getFill() + flux));
        }

        // Original: ohne Zuendung geht immerhin ein Viertel des Strahls als Waerme hinein.
        if (heatup == 0) {
            heat += (long) (laser * 0.25D);
        }
    }

    /** 1:1-Port des Waermetauscherteils: die erste Stufe des Kuehlmittels, gedeckelt auf ein Viertel. */
    private void cool() {
        consumption = 0;
        output = 0;

        FT_Heatable trait = FluidType.getTrait(tanks[0].getStoredFluid(), FT_Heatable.class);
        if (trait == null) return;

        HeatingStep step = trait.getFirstStep();
        if (step == null || step.amountReq <= 0 || step.heatReq <= 0) return;

        tanks[1].setTankType(step.typeProduced);

        int coolingCycles = tanks[0].getFill() / step.amountReq;
        int heatingCycles = (tanks[1].getMaxFill() - tanks[1].getFill()) / step.amountProduced;
        // Original: hoechstens ein Viertel der Hitze je Tick, und das mal der Guete des Mittels.
        int heatCycles = (int) Math.min(
                heat / 4D / step.heatReq * trait.getEfficiency(HeatingType.ICF),
                (double) heat / step.heatReq);

        int cycles = Math.min(coolingCycles, Math.min(heatingCycles, heatCycles));
        if (cycles <= 0) return;

        tanks[0].setFill(tanks[0].getFill() - step.amountReq * cycles);
        tanks[1].setFill(tanks[1].getFill() + step.amountProduced * cycles);
        heat -= (long) step.heatReq * cycles;

        consumption = step.amountReq * cycles;
        output = step.amountProduced * cycles;
    }

    /**
     * 1:1-Port von {@code getConPos}: die sechs Anschlussstellen des Aufbaus - oben, unten und die
     * vier Ecken auf halber Hoehe.
     */
    private BlockPos[] connectionPositions() {
        Direction dir = getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        Direction rot = dir.getClockWise();

        return new BlockPos[] {
                worldPosition.above(6),
                worldPosition.below(),
                worldPosition.relative(dir, 3).relative(rot, 6).above(3),
                worldPosition.relative(dir, 3).relative(rot, -6).above(3),
                worldPosition.relative(dir, -3).relative(rot, 6).above(3),
                worldPosition.relative(dir, -3).relative(rot, -6).above(3)
        };
    }

    // ── IFluidStandardTransceiverMK2 ────────────────────────────────────────

    @Override public FluidTank[] getAllTanks()      { return tanks; }
    @Override public FluidTank[] getReceivingTanks(){ return new FluidTank[] { tanks[0] }; }
    @Override public FluidTank[] getSendingTanks()  { return new FluidTank[] { tanks[1], tanks[2] }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        // Original: nur die fuenf Vorratsplaetze nehmen Kapseln an.
        return slot <= SLOT_INPUT_END && stack.getItem() instanceof ItemICFPellet;
    }

    @Override
    public AABB getRenderBoundingBox() {
        // Der Aufbau reicht acht Bloecke in jede Richtung und fuenf nach oben.
        return new AABB(worldPosition).inflate(9D, 0D, 9D).expandTowards(0D, 6D, 0D);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        for (int i = 0; i < 3; i++) tanks[i].writeToNBT(tag, "t" + i);
        tag.putLong("heat", heat);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        for (int i = 0; i < 3; i++) tanks[i].readFromNBT(tag, "t" + i);
        heat = tag.getLong("heat");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.icf");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineICFMenu(id, inv, this);
    }
}
