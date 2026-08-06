package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidConnectorMK2;
import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * Powered Condenser - Port von {@code TileEntityCondenserPowered} (1.7.10 Original, erbt von
 * {@code TileEntityCondenser}). Wandelt SPENTSTEAM 1:1 in WATER um, begrenzt durch Tankfuellstand
 * und freien Ausgangsplatz ({@code convert = min(inFill, outSpace)}); zusaetzlich gate-t die
 * gepowerte Variante die Umwandlung an {@code power >= convert*10} und zieht danach {@code
 * convert*powerConsumption(=10)} HE ab - 1:1 aus dem Original. Kein Inventar, kein GUI (das
 * Original hatte weder Container noch Screen, nur einen Hover-Tooltip - hier nicht uebernommen,
 * analog zu {@code MachineSteamEngineBlockEntity}).
 */
public class MachineCondenserPoweredBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    private static final int INPUT_TANK_CAPACITY  = 1_000_000;
    private static final int OUTPUT_TANK_CAPACITY  = 1_000_000;
    private static final long MAX_POWER            = 10_000_000L;
    private static final long POWER_CONSUMPTION_PER_MB = 10L;

    private final FluidTank spentSteamTank = new FluidTank(ModFluids.SPENTSTEAM.getSource(), INPUT_TANK_CAPACITY);
    private final FluidTank waterTank      = new FluidTank(ModFluids.WATER.getSource(), OUTPUT_TANK_CAPACITY);

    private int throughput = 0;

    public MachineCondenserPoweredBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONDENSER_POWERED_BE.get(), pos, state, 0, MAX_POWER, MAX_POWER, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCondenserPoweredBlockEntity be) {
        if (level.isClientSide()) return;
        be.serverTick(level, pos);
    }

    private void serverTick(Level level, BlockPos pos) {
        ensureNetworkInitialized();

        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                if (!(neighborBe instanceof IFluidConnectorMK2)) continue;

                trySubscribe(spentSteamTank.getTankType(), level, neighborPos, dir);
                if (waterTank.getFill() > 0) {
                    tryProvide(waterTank, level, neighborPos, dir);
                }
            }
        }

        int convert = Math.min(spentSteamTank.getFluidAmountMb(), waterTank.getCapacityMb() - waterTank.getFluidAmountMb());
        throughput = convert;

        boolean dirty = false;
        if (convert > 0 && getEnergyStored() >= convert * POWER_CONSUMPTION_PER_MB) {
            spentSteamTank.drainMb(convert);
            waterTank.fillMb(ModFluids.WATER.getSource(), convert);
            setEnergyStored(Math.max(0L, getEnergyStored() - convert * POWER_CONSUMPTION_PER_MB));
            dirty = true;
        }

        if (dirty) {
            setChanged();
            sendUpdateToClient();
        }
    }

    // ==================== IFluidUserMK2 / MK2-Netz ====================

    @Override
    public FluidTank[] getAllTanks() { return new FluidTank[] { spentSteamTank, waterTank }; }

    @Override
    public FluidTank[] getSendingTanks() { return new FluidTank[] { waterTank }; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { spentSteamTank }; }

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
        spentSteamTank.writeToNBT(tag, "tank_spentsteam");
        waterTank.writeToNBT(tag, "tank_water");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        spentSteamTank.readFromNBT(tag, "tank_spentsteam");
        waterTank.readFromNBT(tag, "tank_water");
    }

    // ==================== GETTERS / MENU ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.condenser_powered");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false; // Kein Inventar - siehe Klassenkommentar.
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null; // Kein GUI im Original.
    }

    public FluidTank getSpentSteamTank() { return spentSteamTank; }
    public FluidTank getWaterTank()      { return waterTank; }
    public int getThroughput()           { return throughput; }
}
