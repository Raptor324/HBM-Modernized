package com.hbm_m.blockentity.machines;

import java.util.HashSet;

import org.jetbrains.annotations.Nullable;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntitySolarBoiler}: die Spiegel schreiben pro Tick ihre Sonnenhitze in {@link #heat}; daraus werden
 * {@code heat / 50} mB Wasser zu je 100 mB Dampf (Wassertank 100 mB, Dampftank 10.000 mB). Anschluesse oben
 * ({@code y + 3}) und unten ({@code y - 1}). Auf dem Client sammeln {@link #primary}/{@link #secondary} die
 * Spiegelpositionen fuer die Lichtstrahlen (verzoegert, weil der Kessel nicht zwingend zuerst tickt).
 */
public class MachineSolarBoilerBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    private final FluidTank water;
    private final FluidTank steam;
    public int display;
    public int heat;

    public HashSet<BlockPos> primary = new HashSet<>();
    public HashSet<BlockPos> secondary = new HashSet<>();

    public MachineSolarBoilerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_BOILER_BE.get(), pos, state, 0, 0L, 0L, 0L);
        water = new FluidTank(ModFluids.WATER.getSource(), 100);
        steam = new FluidTank(ModFluids.STEAM.getSource(), 10_000);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSolarBoilerBlockEntity be) {
        be.updateEntity(level);
    }

    private void updateEntity(Level worldObj) {

        if (!worldObj.isClientSide) {

            BlockPos top = worldPosition.above(3);
            BlockPos bottom = worldPosition.below();

            this.trySubscribe(water.getTankType(), worldObj, top, Direction.UP);
            this.trySubscribe(water.getTankType(), worldObj, bottom, Direction.DOWN);

            int process = heat / 50;
            this.display = process;
            process = Math.min(process, water.getFill());
            process = Math.min(process, (steam.getMaxFill() - steam.getFill()) / 100);

            if (process < 0) process = 0;

            water.setFill(water.getFill() - process);
            steam.setFill(steam.getFill() + process * 100);

            this.tryProvide(steam, worldObj, top, Direction.UP);
            this.tryProvide(steam, worldObj, bottom, Direction.DOWN);

            heat = 0;

            // Original networkPackNT(15)
            setChanged();
            sendUpdateToClient();
        } else {

            // a delayed queue of mirror positions because we can't expect the boiler to always tick first
            secondary.clear();
            secondary.addAll(primary);
            primary.clear();
        }
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        this.water.writeToNBT(nbt, "water");
        this.steam.writeToNBT(nbt, "steam");
        // Original nur im Paket (serialize); hier laeuft das Paket ueber das NBT
        nbt.putInt("display", display);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        this.water.readFromNBT(nbt, "water");
        this.steam.readFromNBT(nbt, "steam");
        this.display = nbt.getInt("display");
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) {
            int xCoord = worldPosition.getX(), yCoord = worldPosition.getY(), zCoord = worldPosition.getZ();
            bb = new AABB(xCoord - 1, yCoord, zCoord - 1, xCoord + 2, yCoord + 3, zCoord + 2);
        }
        return bb;
    }

    // ==================== Fluid ====================

    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { steam }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { water }; }
    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { water, steam }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ==================== Sonstiges ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.solar_boiler");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null; // Kein GUI im Original.
    }
}
