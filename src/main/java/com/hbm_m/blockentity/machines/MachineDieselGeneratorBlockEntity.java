package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.FluidContainerRegistry;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Combustible;
import com.hbm_m.inventory.fluid.trait.FluidTrait.FluidReleaseType;
import com.hbm_m.inventory.menu.MachineDieselGeneratorMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code TileEntityMachineDiesel}: verbrennt jedes {@code FT_Combustible} ausser Grad LOW mit konstant
 * 1 mB/Tick, Ertrag {@code combustionEnergy / 1000 * fuelEfficiency[grade]} (MEDIUM 0.5, HIGH 0.75, AERO 0.1).
 *
 * <p>Slots wie im Original: 0 voller Kanister (rein), 1 leerer Kanister (raus), 2 Batterie, 3 Fluid-Identifier
 * (stellt den Tank um). Laeuft nur mit Zuendung ({@code isOn}) und ohne Redstone-Signal; Strom, Rauch und
 * Treibstoff gehen an alle sechs Seiten. Faelle volle Batterie: verbrennt trotzdem weiter (Original).</p>
 */
public class MachineDieselGeneratorBlockEntity extends com.hbm_m.blockentity.MachinePollutingBlockEntity
        implements IFluidStandardTransceiverMK2, IControlReceiver {

    public static final int SLOT_FLUID_IN = 0;
    public static final int SLOT_FLUID_OUT = 1;
    public static final int SLOT_BATTERY = 2;
    public static final int SLOT_FLUID_ID = 3;
    private static final int SLOT_COUNT = 4;

    /** Original {@code fuelCap}. */
    private static final int TANK_CAPACITY_MB = 16_000;
    /** Original: {@code super(4, 100)} in {@code TileEntityMachineDiesel}. */
    private static final int SMOKE_BUFFER_MB = 100;
    /** Original {@code maxPower}. */
    public static final long MAX_POWER = 50_000L;

    private final FluidTank tank = new FluidTank(ModFluids.DIESEL.getSource(), TANK_CAPACITY_MB);

    /** Original: {@code isOn} - der Knopf in der Oberflaeche. */
    private boolean isOn = false;
    /** Original: {@code wasOn} - ob im letzten Tick wirklich verbrannt wurde (Laufanzeige und Ton). */
    private boolean wasOn = false;

    public MachineDieselGeneratorBlockEntity(BlockPos pos, BlockState state) {
        // Original: super(4, 100) - vier Slots, 100 mB Rauchpuffer je Sorte.
        super(ModBlockEntities.DIESEL_GENERATOR_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, 0L, MAX_POWER, SMOKE_BUFFER_MB);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineDieselGeneratorBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) be.serverTick(serverLevel, pos);
        else be.clientTick();
    }

    private void serverTick(ServerLevel level, BlockPos pos) {
        boolean wasOnBefore = this.wasOn;
        this.wasOn = false;

        ItemStack[] slots = slotsArray();
        boolean changed = tank.setType(SLOT_FLUID_ID, slots);
        changed |= tank.loadTank(SLOT_FLUID_IN, SLOT_FLUID_OUT, slots);
        if (changed) applySlots(slots);

        for (Direction dir : Direction.values()) {
            BlockPos p = pos.relative(dir);
            this.tryProvide(level, p.getX(), p.getY(), p.getZ(), dir);
            this.sendSmoke(level, p, dir);
            this.trySubscribe(tank.getTankType(), level, p, dir);
        }

        // Original: Library.chargeItemsFromTE(slots, 2, power, powerCap)
        chargeItemInSlot(SLOT_BATTERY);
        if (isOn) generate(level, pos);

        setChanged();
        sendUpdateToClient();
    }

    private void clientTick() {
        // Original: ENGINE_LOOP (Lautstaerke 1, Reichweite 10) solange wasOn
        com.hbm_m.sound.ClientSoundBootstrap.updateSound(this, wasOn, this::createAudioLoop);
    }

    private Object createAudioLoop() {
        try {
            return Class.forName("com.hbm_m.client.sound.DieselEngineLoopSoundFactory").getMethod("create", MachineDieselGeneratorBlockEntity.class).invoke(null, this);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) com.hbm_m.sound.ClientSoundBootstrap.updateSound(this, false, null);
    }

    /** Original {@code generate()}. */
    private void generate(Level level, BlockPos pos) {

        if (!this.isOn) return;
        if (level.hasNeighborSignal(pos)) return;
        if (!hasAcceptableFuel()) return;
        if (tank.getFill() <= 0) return;

        this.wasOn = true;
        tank.setFill(tank.getFill() - 1);

        if (tank.getFill() < 0) tank.setFill(0);

        if (level.getGameTime() % 5 == 0) {
            super.pollute(tank.getTankType(), FluidReleaseType.BURN, 5F);
        }

        long he = getHEFromFuel();
        if (energy + he <= capacity) {
            energy += he;
        } else {
            energy = capacity;
        }
    }

    public boolean hasAcceptableFuel() { return getHEFromFuel() > 0; }
    public long getHEFromFuel() { return getHEFromFuel(tank.getTankType()); }

    /** Original {@code getHEFromFuel(FluidType)}: LOW wird abgelehnt, sonst feste Effizienz je Grad. */
    public static long getHEFromFuel(Fluid type) {
        FT_Combustible fuel = FluidType.getTrait(type, FT_Combustible.class);
        if (fuel != null) {
            double efficiency = switch (fuel.getGrade()) {
                case MEDIUM -> 0.5D;
                case HIGH -> 0.75D;
                case AERO -> 0.1D;
                default -> 0.0D;
            };
            if (fuel.getGrade() != FT_Combustible.FuelGrade.LOW) {
                return (long) (fuel.getCombustionEnergy() / 1000L * efficiency);
            }
        }
        return 0;
    }

    private ItemStack[] slotsArray() {
        ItemStack[] arr = new ItemStack[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) arr[i] = inventory.getStackInSlot(i);
        return arr;
    }

    private void applySlots(ItemStack[] arr) {
        for (int i = 0; i < SLOT_COUNT; i++) inventory.setStackInSlot(i, arr[i] == null ? ItemStack.EMPTY : arr[i]);
    }

    // ==================== Steuerung (Original IControlReceiver) ====================

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) < 25 * 25;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("turnOn")) this.isOn = !this.isOn;
        setChanged();
        sendUpdateToClient();
    }

    /** Original: {@code receiveControl} mit dem Schluessel {@code turnOn}. */
    public void toggleIgnition() {
        CompoundTag data = new CompoundTag();
        data.putBoolean("turnOn", true);
        receiveControl(data);
    }

    // ==================== IFluidUserMK2 / MK2-Netz ====================

    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { tank }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tank }; }
    @Override public FluidTank[] getSendingTanks() { return this.getSmokeTanks(); }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tank.writeToNBT(tag, "fuel");
        tag.putBoolean("isOn", isOn);
        tag.putBoolean("wasOn", wasOn);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        if (tag.contains("tank")) tank.readFromNBT(tag, "tank"); // aeltere Port-Speicherstaende
        else tank.readFromNBT(tag, "fuel");
        isOn = tag.getBoolean("isOn");
        wasOn = tag.getBoolean("wasOn");
    }

    // ==================== GETTERS / MENU ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineDiesel");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    /** Original {@code isItemValidForSlot}: Slot 0 nur passende volle Behaelter, Slot 2 nur Batterien. */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_FLUID_IN) return FluidContainerRegistry.getFluidContent(stack, tank.getTankType()) > 0;
        if (slot == SLOT_BATTERY) return isEnergyReceiverItem(stack) || isEnergyProviderItem(stack);
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineDieselGeneratorMenu(containerId, playerInventory, this);
    }

    public FluidTank getTank() {
        return tank;
    }

    public boolean isOn() {
        return isOn;
    }

    /** Original: {@code wasOn} - laeuft der Motor gerade wirklich? */
    public boolean wasOn() {
        return wasOn;
    }

    public boolean isActive() {
        return hasAcceptableFuel() && tank.getFill() > 0;
    }

    //? if forge {
    /** Original {@code ISidedInventory}: unten {1, 2}, oben {0}, Seiten {2}; leere Kanister und volle Akkus heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return side == net.minecraft.core.Direction.DOWN ? new int[] { 1, 2 } : side == net.minecraft.core.Direction.UP ? new int[] { 0 } : new int[] { 2 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return (slot == 0 || slot == 2) && isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { if (slot == 1) return true; return slot == 2 && com.hbm_m.blockentity.SidedItemAccess.isFullBattery(stack); }
            });

    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@org.jetbrains.annotations.NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sidedItems.invalidate();
    }
    //?}
}
