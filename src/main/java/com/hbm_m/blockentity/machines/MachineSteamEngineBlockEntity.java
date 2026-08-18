package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Coolable;
import com.hbm_m.inventory.fluid.trait.FT_Coolable.CoolingType;

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
import net.minecraft.world.level.material.Fluid;

/**
 * Steam Engine - Port von {@code TileEntitySteamEngine} (1.7.10 Original). Kein Inventar, kein
 * GUI (das Original hat weder Container noch Screen - Statusanzeige nur ueber Hover-Tooltip,
 * hier nicht uebernommen). Wandelt STEAM ueber die {@code FT_Coolable}-Fluid-Eigenschaft
 * (CoolingType.TURBINE) in SPENTSTEAM um: {@code ops = min(steamTank/amountReq,
 * spentSteamSpace/amountProduced)}, {@code output = ops * heatEnergy * (coolableEff * 0.85)} -
 * 1:1 aus dem Original ({@code efficiency=0.85}). Die dafuer noetigen Fluid-Traits (STEAM mit
 * CoolingType.TURBINE) sind in diesem Port bereits vollstaendig hinterlegt.
 * <p>
 * SCOPE-Entscheidung: Die rein optische Rotor-/Kolben-Drehanimation des Original-Renderers
 * entfaellt (keine mechanische Auswirkung). Anders als im Original (das seinen Energiepuffer
 * jeden Tick komplett auf 0 zurueck-dumpt, quasi ein reiner Durchlauferhitzer ohne Kapazitaet)
 * speichert dieser Port die Energie in einem normalen kapazitaetsbegrenzten Puffer - konsistent
 * mit allen anderen Generatoren dieser Session.
 */
public class MachineSteamEngineBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    private static final int STEAM_TANK_CAPACITY_MB = 2_000;
    private static final int SPENTSTEAM_TANK_CAPACITY_MB = 20;
    private static final long MAX_POWER = 200_000L;
    private static final double EFFICIENCY = 0.85D;

    private final FluidTank steamTank = new FluidTank(ModFluids.STEAM.getSource(), STEAM_TANK_CAPACITY_MB);
    private final FluidTank spentSteamTank = new FluidTank(ModFluids.SPENTSTEAM.getSource(), SPENTSTEAM_TANK_CAPACITY_MB);

    public MachineSteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_ENGINE_BE.get(), pos, state, 0, MAX_POWER, 0L, MAX_POWER);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return steamTank.getForgeFluidCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSteamEngineBlockEntity be) {
        if (level.isClientSide()) return;
        be.serverTick(level, pos);
    }

    private void serverTick(Level level, BlockPos pos) {
        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                trySubscribe(steamTank.getTankType(), level, pos.relative(dir), dir);
                tryProvide(spentSteamTank, level, pos.relative(dir), dir);
            }
        }

        boolean dirty = false;

        FT_Coolable trait = com.hbm_m.inventory.fluid.FluidType.getTrait(steamTank.getStoredFluid(), FT_Coolable.class);
        if (trait != null && trait.amountReq > 0 && trait.amountProduced > 0 && getEnergyStored() < getMaxEnergyStored()) {
            double eff = trait.getEfficiency(CoolingType.TURBINE) * EFFICIENCY;
            if (eff > 0) {
                int inputOps = steamTank.getFluidAmountMb() / trait.amountReq;
                int outputOps = (spentSteamTank.getCapacityMb() - spentSteamTank.getFluidAmountMb()) / trait.amountProduced;
                int ops = Math.min(inputOps, outputOps);

                if (ops > 0) {
                    steamTank.drainMb(ops * trait.amountReq);
                    spentSteamTank.fillMb(ModFluids.SPENTSTEAM.getSource(), ops * trait.amountProduced);

                    long output = (long) (ops * trait.heatEnergy * eff);
                    setEnergyStored(Math.min(getMaxEnergyStored(), getEnergyStored() + output));
                    dirty = true;
                }
            }
        }

        if (dirty) {
            setChanged();
            sendUpdateToClient();
        }
    }

    // ==================== IFluidUserMK2 / MK2-Netz ====================

    @Override
    public FluidTank[] getAllTanks() { return new FluidTank[] { steamTank, spentSteamTank }; }

    @Override
    public FluidTank[] getSendingTanks() { return new FluidTank[] { spentSteamTank }; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { steamTank }; }

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
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        steamTank.writeToNBT(tag, "tank_steam");
        spentSteamTank.writeToNBT(tag, "tank_spentsteam");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        steamTank.readFromNBT(tag, "tank_steam");
        spentSteamTank.readFromNBT(tag, "tank_spentsteam");
    }

    // ==================== GETTERS / MENU ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.steam_engine");
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

    public FluidTank getSteamTank() {
        return steamTank;
    }

    public FluidTank getSpentSteamTank() {
        return spentSteamTank;
    }
}
