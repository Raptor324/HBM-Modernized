package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Combustible;
import com.hbm_m.inventory.menu.MachineCombustionEngineMenu;
import com.hbm_m.item.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * Combustion Engine - Port von {@code TileEntityMachineCombustionEngine} (1.7.10 Original). Wie
 * der Diesel Generator ein {@code FT_Combustible}-Treibstoffverbrenner, aber fest auf Diesel
 * (Grade HIGH) verriegelt und mit einer Kolben-Effizienz-Stufe: der Ausstoss haengt vom
 * eingesetzten Kolben-Item ab (1:1 aus der Original-Effizienz-Matrix, nur die Diesel/HIGH-Spalte
 * uebernommen, da der Tank fest auf Diesel steht): Stahl=0.75, Dura=1.00, Desh=0.50,
 * Starmetal=0.75. {@code maxPower=2_500_000} (Original), Tank 24.000mB.
 * <p>
 * SCOPE-Entscheidungen:
 * <ul>
 *   <li>Kein Zuend-Knopf + Drossel-Schieberegler (Original: separater GUI-Ignition-Toggle UND ein
 *   0-30-Drag-Regler fuer die Brenngeschwindigkeit) - stattdessen wie beim Diesel Generator nur
 *   Redstone-Sperre, und immer volle Drosselstellung wenn aktiv (konsistent mit anderen Maschinen
 *   dieser Session, die manuelle GUI-Regler zugunsten von Automatikbetrieb gestrichen haben, z.B.
 *   Compressor).</li>
 *   <li>Multiblock ueber dieses Repo-eigene {@link com.hbm_m.multiblock.MultiblockStructureHelper}-
 *   Framework statt des veralteten 1.7.10 {@code BlockDummyable}/Proxy-Block-Systems - vereinfacht-
 *   aber-proportionales Footprint (siehe {@code MachineCombustionEngineBlock}).</li>
 *   <li>Kein Bucket-/Kanister-Item-Slot-Paar - Tank wird direkt ueber das MK2-Fluid-Netz befuellt.</li>
 *   <li>{@code TileEntityMachinePolluting} entfaellt (durchgaengig etablierte Luecke).</li>
 *   <li>Tuer-Animation/Motorengeraeusch-Loop des Original-Renderers entfallen (rein optisch/
 *   akustisch, keine mechanische Auswirkung).</li>
 * </ul>
 */
public class MachineCombustionEngineBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2 {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_PISTON = 1;
    private static final int SLOT_COUNT = 2;

    private static final int TANK_CAPACITY_MB = 24_000;
    private static final long MAX_POWER = 2_500_000L;
    private static final int BURN_MB_PER_TICK = 6;

    private final FluidTank tank = new FluidTank(ModFluids.DIESEL.getSource(), TANK_CAPACITY_MB);

    public MachineCombustionEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMBUSTION_ENGINE_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, 0L, MAX_POWER);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return tank.getCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCombustionEngineBlockEntity be) {
        if (level.isClientSide()) return;
        be.serverTick(level, pos);
    }

    private void serverTick(Level level, BlockPos pos) {
        chargeItemInSlot(SLOT_BATTERY);

        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                trySubscribe(tank.getTankType(), level, pos.relative(dir), dir);
            }
        }

        boolean dirty = false;

        if (!level.hasNeighborSignal(pos)) {
            double eff = pistonEfficiency(inventory.getStackInSlot(SLOT_PISTON).getItem());
            Fluid fuel = tank.getStoredFluid();
            FT_Combustible combustible = FluidType.getTrait(fuel, FT_Combustible.class);

            if (eff > 0 && combustible != null && tank.getFluidAmountMb() >= 1 && getEnergyStored() < getMaxEnergyStored()) {
                int toBurn = Math.min(BURN_MB_PER_TICK, tank.getFluidAmountMb());
                long output = (long) (toBurn * (combustible.getCombustionEnergy() / 10_000D) * eff);

                tank.drainMb(toBurn);
                setEnergyStored(Math.min(getMaxEnergyStored(), getEnergyStored() + output));
                dirty = true;
            }
        }

        if (dirty) {
            setChanged();
            sendUpdateToClient();
        }
    }

    /** 1:1 aus der Original-Effizienz-Matrix, nur die Diesel/HIGH-Spalte (Tank ist fest auf Diesel verriegelt). */
    private static double pistonEfficiency(Item piston) {
        if (piston == ModItems.PISTON_SET_STEEL.get()) return 0.75D;
        if (piston == ModItems.PISTON_SET_DURA.get()) return 1.00D;
        if (piston == ModItems.PISTON_SET_DESH.get()) return 0.50D;
        if (piston == ModItems.PISTON_SET_STARMETAL.get()) return 0.75D;
        return 0.0D;
    }

    // ==================== IFluidUserMK2 / MK2-Netz ====================

    @Override
    public FluidTank[] getAllTanks() { return new FluidTank[] { tank }; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { tank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null;
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        tank.readFromNBT(tag, "tank");
    }

    // ==================== GETTERS / MENU ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.combustion_engine");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return isEnergyProviderItem(stack);
        if (slot == SLOT_PISTON) return pistonEfficiency(stack.getItem()) > 0;
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineCombustionEngineMenu(containerId, playerInventory, this);
    }

    public FluidTank getTank() {
        return tank;
    }

    public boolean isActive() {
        return pistonEfficiency(inventory.getStackInSlot(SLOT_PISTON).getItem()) > 0 && tank.getFluidAmountMb() >= 1;
    }
}
