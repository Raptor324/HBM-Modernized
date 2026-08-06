package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineCoreEmitterBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.ILaserable;
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

/**
 * Port von {@code TileEntityCoreEmitter} (1.7.10). Original: laedt sich per HE-Netzwerk auf,
 * feuert entlang seiner FACING-Richtung (Metadata-basiert) einen Laserstrahl bis zu 50 Bloecke
 * weit, der beim ersten {@code ILaserable}-Ziel 95% der aufgenommenen Energie abliefert
 * (5% Leitungsverlust), und verbraucht dabei Cryogel-Kuehlmittel aus einem internen Tank.
 * <p>
 * Vereinfachungen (Funktion vor Politur, siehe Aufgabenstellung):
 * <ul>
 *   <li>kein Fusionsreaktor-Ziel ({@code TileEntityCore.burn}) - Ziel ist ausschliesslich ein
 *       {@link ILaserable} (in diesem Port: {@link MachineCoreReceiverBlockEntity}).</li>
 *   <li>keine Entity-Schadens-/Feuer-Hitbox entlang des Strahls, kein Block-Sprengen bei Hindernissen -
 *       trifft der Strahl auf ein solides, nicht-ILaserable Hindernis, stoppt er dort einfach wirkungslos.</li>
 *   <li>kein GUI/Watt-Regler (0-100%) und kein OpenComputers/RoR-Toggle - der Emitter feuert immer mit
 *       voller Rate, solange Energie und Kuehlmittel vorhanden sind (kein spielerseitiges Interface noetig,
 *       Original-Container besitzt ohnehin nur Spieler-Inventar-Slots).</li>
 *   <li>kein Ueberhitzungs-Failstate (Original wandelt den Block bei leerem Tank in Lava um) - der
 *       Emitter pausiert stattdessen einfach, bis wieder Kuehlmittel verfuegbar ist.</li>
 * </ul>
 */
public class MachineCoreEmitterBlockEntity extends BaseMachineBlockEntity {

    private static final long ENERGY_PER_TICK = 40_000L;
    private static final int COOLANT_PER_TICK_MB = 20;
    private static final int COOLANT_CAPACITY_MB = 64_000;
    /** 1:1 aus dem Original ({@code TileEntityCoreEmitter.range = 50}). */
    private static final int BEAM_RANGE = 50;
    /** Original: {@code out = joules * 95 / 100} - 5% Leitungsverlust auf dem Weg zum Empfaenger. */
    private static final long TRANSMISSION_NUMERATOR = 95L;
    private static final long TRANSMISSION_DENOMINATOR = 100L;

    private final FluidTank coolantTank = new FluidTank(ModFluids.CRYOGEL.getSource(), COOLANT_CAPACITY_MB);

    /** Client-sync: aktuelle Strahllaenge in Bloecken (0 = kein aktiver Strahl), fuer Renderer. */
    private int beamLength = 0;

    public MachineCoreEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CORE_EMITTER_BE.get(), pos, state, 4, 2_000_000L, ENERGY_PER_TICK);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return coolantTank.getCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCoreEmitterBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        be.ensureNetworkInitialized();
        be.serverTick(level, pos, state);
    }

    private void serverTick(Level level, BlockPos pos, BlockState state) {
        int previousBeamLength = this.beamLength;
        this.beamLength = 0;

        long draw = Math.min(getEnergyStored(), ENERGY_PER_TICK);
        boolean hasCoolant = coolantTank.getFluidAmountMb() >= COOLANT_PER_TICK_MB;

        if (draw > 0 && hasCoolant) {
            Direction facing = state.getValue(MachineCoreEmitterBlock.FACING);
            long transmitted = draw * TRANSMISSION_NUMERATOR / TRANSMISSION_DENOMINATOR;

            boolean delivered = fireBeam(level, pos, facing, transmitted);

            if (delivered) {
                setEnergyStored(getEnergyStored() - draw);
                coolantTank.drainMb(COOLANT_PER_TICK_MB);
            }
        }

        if (previousBeamLength != this.beamLength) {
            setChanged();
            sendUpdateToClient();
        }
    }

    /**
     * Wandert entlang {@code facing} bis {@link #BEAM_RANGE} Bloecke weit. Trifft er auf ein
     * {@link ILaserable}, wird die Energie uebergeben. Trifft er zuvor auf ein solides Hindernis,
     * bricht der Strahl wirkungslos ab (vereinfachtes Verhalten, siehe Klassenkommentar).
     */
    private boolean fireBeam(Level level, BlockPos origin, Direction facing, long energy) {
        for (int i = 1; i <= BEAM_RANGE; i++) {
            BlockPos target = origin.relative(facing, i);
            if (!level.isLoaded(target)) {
                break;
            }

            BlockEntity te = level.getBlockEntity(target);
            if (te instanceof ILaserable laserable) {
                boolean accepted = laserable.addLaserEnergy(level, target, energy, facing);
                this.beamLength = i;
                return accepted;
            }

            BlockState blockState = level.getBlockState(target);
            if (!blockState.isAir() && !blockState.canBeReplaced()) {
                this.beamLength = i;
                return false;
            }
        }
        this.beamLength = BEAM_RANGE;
        return false;
    }

    public FluidTank getCoolantTank() {
        return coolantTank;
    }

    public int getBeamLength() {
        return beamLength;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        coolantTank.writeToNBT(tag, "coolant");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        coolantTank.readFromNBT(tag, "coolant");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.core_emitter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return null;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.core_emitter");
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }
}
