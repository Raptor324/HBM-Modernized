package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineReactorControlMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.machine.ItemReactorSensor;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code TileEntityReactorControl} (1.7.10): ein Steuerpult, das einen ueber den
 * Reaktorfuehler gebundenen Forschungsreaktor waermeabhaengig regelt.
 *
 * <p>Zwischen der unteren und der oberen Waermeschwelle wird die Stabstellung nach der gewaehlten
 * Kennlinie berechnet ({@link RodFunction}), ausserhalb gilt die jeweilige Randstellung. Der
 * ermittelte Wert geht per {@code setTarget} an den Reaktor.</p>
 *
 * <p>Der Block hat im Original keinen Kreativreiter-Eintrag ({@code setCreativeTab(null)}) und
 * damit auch kein Bauplanrezept - er ist ausschliesslich ueber den Assembler zu bekommen. Hier ist
 * ein Assemblerrezept ergaenzt, damit er ueberhaupt erreichbar ist.</p>
 */
public class MachineReactorControlBlockEntity extends BaseMachineBlockEntity {

    /** Original: {@code super(1)} - ein Steckplatz fuer den Reaktorfuehler. */
    public static final int SLOT_SENSOR = 0;
    public static final int INVENTORY_SIZE = 1;

    /** Original: {@code enum RodFunction { LINEAR, QUAD, LOG }}. */
    public enum RodFunction {
        LINEAR,
        QUAD,
        LOG
    }

    private boolean isLinked = false;
    private int heat = 0;
    private int flux = 0;
    private double rodLevel = 0D;

    private double levelLower = 0D;
    private double levelUpper = 100D;
    private double heatLower = 0D;
    private double heatUpper = 100_000D;
    private RodFunction function = RodFunction.LINEAR;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> isLinked ? 1 : 0;
                case 1 -> heat;
                case 2 -> flux;
                case 3 -> (int) Math.round(rodLevel * 100D);
                case 4 -> (int) Math.round(levelLower);
                case 5 -> (int) Math.round(levelUpper);
                case 6 -> (int) Math.round(heatLower);
                case 7 -> (int) Math.round(heatUpper);
                case 8 -> function.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) { }

        @Override
        public int getCount() {
            return 9;
        }
    };

    public MachineReactorControlBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_REACTOR_CONTROL_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineReactorControlBlockEntity be) {
        if (level.isClientSide()) return;

        MachineReactorResearchBlockEntity reactor = be.establishLink(level);
        be.isLinked = reactor != null;

        if (reactor != null) {
            // Original: die Schwellen duerfen in beliebiger Reihenfolge stehen.
            double lowerBound = Math.min(be.heatLower, be.heatUpper);
            double upperBound = Math.max(be.heatLower, be.heatUpper);

            double fauxLevel;
            if (be.heat < lowerBound) {
                fauxLevel = be.levelLower;
            } else if (be.heat > upperBound) {
                fauxLevel = be.levelUpper;
            } else {
                fauxLevel = be.getTargetLevel(be.function, be.heat);
            }

            reactor.setTarget(Math.max(0D, Math.min(1D, fauxLevel * 0.01D)));
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    /**
     * Original: {@code establishLink()} - liest die Position aus dem Fuehler und uebernimmt
     * Fluss, Stellung und Waerme des dort stehenden Reaktors.
     */
    @Nullable
    private MachineReactorResearchBlockEntity establishLink(Level level) {
        ItemStack sensor = inventory.getStackInSlot(SLOT_SENSOR);
        if (sensor.isEmpty() || !sensor.is(ModItems.REACTOR_SENSOR.get())) return null;

        BlockPos bound = ItemReactorSensor.getBoundPos(sensor);
        if (bound == null) return null;

        BlockEntity be = level.getBlockEntity(bound);
        if (!(be instanceof MachineReactorResearchBlockEntity reactor)) return null;

        this.flux = reactor.getTotalFlux();
        this.rodLevel = reactor.getRodLevel();
        this.heat = reactor.getHeat();
        return reactor;
    }

    /** 1:1 aus {@code getTargetLevel(RodFunction, int)}. */
    public double getTargetLevel(RodFunction function, int heat) {
        double heatSpan = this.heatUpper - this.heatLower;
        if (heatSpan == 0D) return this.levelLower;

        return switch (function) {
            case LINEAR -> (heat - this.heatLower) * ((this.levelUpper - this.levelLower) / heatSpan) + this.levelLower;
            case LOG -> Math.pow((heat - this.heatUpper) / (this.heatLower - this.heatUpper), 2)
                    * (this.levelLower - this.levelUpper) + this.levelUpper;
            case QUAD -> Math.pow((heat - this.heatLower) / heatSpan, 2)
                    * (this.levelUpper - this.levelLower) + this.levelLower;
        };
    }

    /** Original: {@code receiveControl} - die vier Schwellen kommen als Block vom Steuerfeld. */
    public void applySettings(double levelLower, double levelUpper, double heatLower, double heatUpper) {
        this.levelLower = levelLower;
        this.levelUpper = levelUpper;
        this.heatLower = heatLower;
        this.heatUpper = heatUpper;
        setChanged();
    }

    /** Original: {@code receiveControl} mit dem Schluessel {@code function}. */
    public void setFunction(RodFunction function) {
        this.function = function;
        setChanged();
    }

    // ── Anzeige ─────────────────────────────────────────────────────────────

    public boolean isLinked()        { return isLinked; }
    public int getHeat()             { return heat; }
    public int getFlux()             { return flux; }
    public double getRodLevel()      { return rodLevel; }
    public double getLevelLower()    { return levelLower; }
    public double getLevelUpper()    { return levelUpper; }
    public double getHeatLower()     { return heatLower; }
    public double getHeatUpper()     { return heatUpper; }
    public RodFunction getFunction() { return function; }

    /**
     * Original: {@code getDisplayData()} - Stellung in Prozent, Fluss und die auf Grad Celsius
     * umgerechnete Waerme.
     */
    public int[] getDisplayData() {
        if (!isLinked) return new int[] { 0, 0, 0 };
        return new int[] {
                (int) (rodLevel * 100),
                flux,
                (int) Math.round(heat * 0.00002D * 980D + 20D)
        };
    }

    public ContainerData getContainerData() {
        return data;
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == SLOT_SENSOR && stack.is(ModItems.REACTOR_SENSOR.get());
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("isLinked", isLinked);
        tag.putDouble("levelLower", levelLower);
        tag.putDouble("levelUpper", levelUpper);
        tag.putDouble("heatLower", heatLower);
        tag.putDouble("heatUpper", heatUpper);
        tag.putInt("function", function.ordinal());
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        isLinked = tag.getBoolean("isLinked");
        levelLower = tag.getDouble("levelLower");
        levelUpper = tag.getDouble("levelUpper");
        heatLower = tag.getDouble("heatLower");
        heatUpper = tag.getDouble("heatUpper");

        int ordinal = tag.getInt("function");
        RodFunction[] values = RodFunction.values();
        function = ordinal >= 0 && ordinal < values.length ? values[ordinal] : RodFunction.LINEAR;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_controller");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineReactorControlMenu(id, inv, this);
    }
}
