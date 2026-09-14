package com.hbm_m.blockentity.machines.icf;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineICFPressMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.machine.ItemICFPellet;
import com.hbm_m.item.machine.ItemICFPellet.EnumICFFuel;

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
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code TileEntityICFPress} (1.7.10): die Presse, die aus zwei Brennstoffen eine
 * Kapsel macht.
 *
 * <p>Sie braucht eine leere Kapsel und <b>zwei verschiedene</b> Brennstoffe - gleiche gehen nicht.
 * Jede Seite nimmt entweder ein Gas aus ihrem Tank (1000 mB) oder einen festen Barren aus ihrem
 * Platz; liegt beides an, hat das Gas Vorrang.</p>
 *
 * <p>Ein Myonenbehaelter in der Presse laedt sechzehn Ladungen auf; jede davon macht die naechste
 * Kapsel <b>myonenkatalysiert</b> und viertelt damit ihren Zuendaufwand. Der leere Behaelter kommt
 * in den Ausgabeplatz darunter.</p>
 *
 * <p>Die Presse braucht keine Energie und keine Zeit - sie arbeitet, sobald alles anliegt.</p>
 */
public class MachineICFPressBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2 {

    public static final int SLOT_EMPTY_CAPSULE = 0;
    public static final int SLOT_FILLED_CAPSULE = 1;
    public static final int SLOT_MUON_FULL = 2;
    public static final int SLOT_MUON_EMPTY = 3;
    public static final int SLOT_SOLID_A = 4;
    public static final int SLOT_SOLID_B = 5;
    public static final int SLOT_FLUID_ID_A = 6;
    public static final int SLOT_FLUID_ID_B = 7;
    public static final int INVENTORY_SIZE = 8;

    /** Original: {@code maxMuon = 16}. */
    public static final int MAX_MUON = 16;
    /** Original: ein Gas zaehlt ab tausend Millibucket. */
    private static final int FLUID_PER_PELLET = 1_000;
    private static final int TANK_CAPACITY = 16_000;

    private final FluidTank[] tanks = new FluidTank[2];
    private int muon;

    /** Merkt sich je Seite, ob beim letzten Pressen das Gas oder der Barren genommen wurde. */
    private final boolean[] usedFluid = new boolean[2];

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> muon;
                case 1 -> tanks[0].getFill();
                case 2 -> tanks[1].getFill();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) { }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public MachineICFPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_ICF_PRESS_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
        tanks[0] = new FluidTank(ModFluids.DEUTERIUM.getSource(), TANK_CAPACITY);
        tanks[1] = new FluidTank(ModFluids.TRITIUM.getSource(), TANK_CAPACITY);
    }

    public int getMuon() { return muon; }
    public FluidTank[] getTanks() { return tanks; }
    public ContainerData getContainerData() { return data; }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineICFPressBlockEntity be) {
        if (level.isClientSide()) return;

        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                be.trySubscribe(be.tanks[0].getTankType(), level, pos.relative(dir), dir);
                be.trySubscribe(be.tanks[1].getTankType(), level, pos.relative(dir), dir);
            }
        }

        be.loadMuon();
        be.press();

        be.setChanged();
        be.sendUpdateToClient();
    }

    /** 1:1-Port: ein voller Myonenbehaelter laedt sechzehn Ladungen, der leere kommt in den Ausgang. */
    private void loadMuon() {
        if (muon > 0) return;

        ItemStack stack = getInventory().getStackInSlot(SLOT_MUON_FULL);
        if (stack.isEmpty() || !stack.is(ModItems.PARTICLE_MUON.get())) return;

        // Original: der Behaelter geht als Restgegenstand in den Platz darunter, sofern er passt.
        ItemStack container = stack.getCraftingRemainingItem();
        boolean canStore;

        if (container.isEmpty()) {
            canStore = true;
        } else {
            ItemStack outSlot = getInventory().getStackInSlot(SLOT_MUON_EMPTY);
            if (outSlot.isEmpty()) {
                getInventory().setStackInSlot(SLOT_MUON_EMPTY, container.copy());
                canStore = true;
            } else if (ItemStack.isSameItemSameTags(outSlot, container)
                    && outSlot.getCount() < outSlot.getMaxStackSize()) {
                outSlot.grow(1);
                canStore = true;
            } else {
                canStore = false;
            }
        }

        if (canStore) {
            muon = MAX_MUON;
            stack.shrink(1);
            setChanged();
        }
    }

    /** 1:1-Port von {@code press}. */
    private void press() {
        ItemStack empty = getInventory().getStackInSlot(SLOT_EMPTY_CAPSULE);
        if (empty.isEmpty() || !empty.is(ModItems.ICF_PELLET_EMPTY.get())) return;
        if (!getInventory().getStackInSlot(SLOT_FILLED_CAPSULE).isEmpty()) return;

        ItemICFPellet.init();

        EnumICFFuel fuel1 = getFuel(tanks[0], getInventory().getStackInSlot(SLOT_SOLID_A), 0);
        EnumICFFuel fuel2 = getFuel(tanks[1], getInventory().getStackInSlot(SLOT_SOLID_B), 1);

        // Original: zwei gleiche Brennstoffe ergeben keine Kapsel.
        if (fuel1 == null || fuel2 == null || fuel1 == fuel2) return;

        getInventory().setStackInSlot(SLOT_FILLED_CAPSULE,
                ItemICFPellet.setup(fuel1, fuel2, muon > 0));

        if (muon > 0) muon--;

        empty.shrink(1);
        consume(tanks[0], SLOT_SOLID_A, 0);
        consume(tanks[1], SLOT_SOLID_B, 1);
        setChanged();
    }

    private void consume(FluidTank tank, int solidSlot, int index) {
        if (usedFluid[index]) {
            tank.setFill(tank.getFill() - FLUID_PER_PELLET);
        } else {
            getInventory().getStackInSlot(solidSlot).shrink(1);
        }
    }

    /** Original: {@code getFuel} - erst der Tank, dann der feste Platz. */
    @Nullable
    private EnumICFFuel getFuel(FluidTank tank, ItemStack solid, int index) {
        usedFluid[index] = false;

        if (tank.getFill() >= FLUID_PER_PELLET) {
            EnumICFFuel fromFluid = ItemICFPellet.fuelFromFluid(tank.getTankType());
            if (fromFluid != null) {
                usedFluid[index] = true;
                return fromFluid;
            }
        }

        return ItemICFPellet.fuelFromItem(solid);
    }

    // ── IFluidStandardReceiverMK2 ───────────────────────────────────────────

    @Override public FluidTank[] getAllTanks()       { return tanks; }
    @Override public FluidTank[] getReceivingTanks() { return tanks; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack.is(ModItems.ICF_PELLET_EMPTY.get())) return slot == SLOT_EMPTY_CAPSULE;
        if (stack.is(ModItems.PARTICLE_MUON.get())) return slot == SLOT_MUON_FULL;
        return slot == SLOT_SOLID_A || slot == SLOT_SOLID_B;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tanks[0].writeToNBT(tag, "t0");
        tanks[1].writeToNBT(tag, "t1");
        tag.putByte("muon", (byte) muon);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tanks[0].readFromNBT(tag, "t0");
        tanks[1].readFromNBT(tag, "t1");
        muon = tag.getByte("muon");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.machine_icf_press");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineICFPressMenu(id, inv, this);
    }
}
