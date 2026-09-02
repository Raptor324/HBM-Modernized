package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineCoreReceiverBlock;
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
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port von {@code TileEntityCoreReceiver} (1.7.10). Original: nimmt per {@code ILaserable.addEnergy}
 * einen Laserstrahl (vom Core Emitter) entgegen, wandelt ihn in HE-Netzwerk-Energie um
 * ({@code power = joules * 5000}) und speist sie ins Energienetz ein; verbraucht dabei Cryogel aus
 * einem internen Tank. Nur Strahlen von "vorne" (der der FACING-Seite gegenueberliegenden Richtung)
 * werden akzeptiert - Original sprengt den Block bei Treffern von der falschen Seite.
 * <p>
 * Vereinfachungen (Funktion vor Politur):
 * <ul>
 *   <li>der 5000x-HE/SPK-Umrechnungsfaktor des Originals stammt aus dessen zweigeteilter
 *       Energie-Oekonomie (schwache "SPK"-Laserenergie vs. starke "HE"-Maschinenenergie) und hat in
 *       diesem Port mit einer einzigen einheitlichen Energiewaehrung keine Entsprechung - die
 *       empfangene Energie wird stattdessen 1:1 (abzueglich des bereits im Emitter angewandten
 *       5%-Leitungsverlusts) in den eigenen Energiespeicher uebernommen.</li>
 *   <li>keine Explosion bei Treffern von der falschen Seite (Original: Anti-Cheese-Mechanik) - die
 *       Energie wird hier einfach nicht angenommen, der Emitter-Strahl behandelt den Receiver dann
 *       wie ein gewoehnliches Hindernis.</li>
 *   <li>kein Ueberhitzungs-Failstate (Original: Block wird bei leerem Tank zu Lava) - fehlt Kuehlmittel,
 *       wird die Energie trotzdem angenommen (das Kuehlmittel dient hier nur der Bilanz/Fuellstandsanzeige,
 *       keine Weltmanipulation als Bestrafung).</li>
 * </ul>
 */
public class MachineCoreReceiverBlockEntity extends BaseMachineBlockEntity implements ILaserable {

    private static final long ENERGY_EXTRACT_PER_TICK = 40_000L;
    private static final int COOLANT_PER_HIT_MB = 20;
    private static final int COOLANT_CAPACITY_MB = 64_000;

    private final FluidTank coolantTank = new FluidTank(ModFluids.CRYOGEL.getSource(), COOLANT_CAPACITY_MB);

    public MachineCoreReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CORE_RECEIVER_BE.get(), pos, state, 4, 2_000_000L, 0L, ENERGY_EXTRACT_PER_TICK);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return coolantTank.getForgeFluidCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCoreReceiverBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        be.ensureNetworkInitialized();
        // Die eigentliche Energieuebernahme passiert in addLaserEnergy(...), aufgerufen vom
        // Core Emitter waehrend dessen Tick. Hier wird nur die Netzwerkanbindung sichergestellt -
        // extractEnergy() (geerbt von BaseMachineBlockEntity) uebernimmt die Ausspeisung ins Netz.
    }

    @Override
    public boolean addLaserEnergy(Level level, BlockPos pos, long energy, Direction beamDirection) {
        if (energy <= 0 || !pos.equals(this.getBlockPos())) {
            return false;
        }

        // Original: "only accept lasers from the front" - der Strahl muss auf die FACING-Seite
        // des Receivers treffen, d.h. er bewegt sich in die der FACING-Richtung entgegengesetzte Richtung.
        Direction facing = getBlockState().getValue(MachineCoreReceiverBlock.FACING);
        if (beamDirection != facing) {
            return false;
        }

        setEnergyStored(getEnergyStored() + energy);
        if (coolantTank.getFluidAmountMb() >= COOLANT_PER_HIT_MB) {
            coolantTank.drainMb(COOLANT_PER_HIT_MB);
        }
        setChanged();
        sendUpdateToClient();
        return true;
    }

    public FluidTank getCoolantTank() {
        return coolantTank;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        coolantTank.writeToNBT(tag, "coolant");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        coolantTank.readFromNBT(tag, "coolant");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.core_receiver");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return com.hbm_m.inventory.menu.MachineCoreReceiverMenu.create(id, inv, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.core_receiver");
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }
}
