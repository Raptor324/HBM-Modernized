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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code TileEntityCoreReceiver} (1.7.10): das Gegenstueck zum Strahler.
 *
 * <p>Er nimmt den Laserstrahl entgegen und rechnet ihn in Netzenergie um - <b>und zwar mit dem
 * Faktor {@value #HE_PER_SPK}</b>. Das ist der eigentliche Sinn der ganzen Anlage: der Strahler
 * setzt fuenftausend Energieeinheiten in ein Spk um, der Kern vervielfacht das, und hier wird
 * wieder Netzenergie daraus. Ohne diesen Faktor waere ein Fusionsreaktor ein Verlustgeschaeft.</p>
 *
 * <p><b>Nur von vorn.</b> Trifft ihn ein Strahl von der falschen Seite, fliegt er in die Luft -
 * das Original nennt das ausdruecklich so, und es verhindert, dass sich ein Empfaenger seitlich
 * an eine bestehende Strecke haengen laesst.</p>
 *
 * <p>Wie der Strahler braucht auch er Cryogel: 20 mB je Tick, solange etwas ankommt. Geht der Tank
 * leer, wird der Block zu fliessender Lava.</p>
 */
public class MachineCoreReceiverBlockEntity extends BaseMachineBlockEntity implements ILaserable {

    /** Original: {@code power = joules * 5000}. */
    public static final long HE_PER_SPK = 5000L;
    /** Original: {@code tank.setFill(tank.getFill() - 20)}. */
    public static final int COOLANT_PER_TICK_MB = 20;
    public static final int COOLANT_CAPACITY_MB = 64_000;

    /**
     * Das Original hat hier keinen Deckel - {@code getMaxPower()} gibt schlicht die eben erzeugte
     * Menge zurueck, der Empfaenger ist ein reiner Durchlauf. Dieser Port braucht eine feste
     * Obergrenze fuer den Speicher; sie ist so hoch gewaehlt, dass sie im Spiel nicht greift.
     */
    public static final long MAX_POWER = 1_000_000_000_000L;

    private final FluidTank coolantTank = new FluidTank(ModFluids.CRYOGEL.getSource(), COOLANT_CAPACITY_MB);

    /** Original: {@code joules} - was in diesem Tick angekommen ist. */
    private long joules;

    public MachineCoreReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CORE_RECEIVER_BE.get(), pos, state, 4, MAX_POWER, 0L, MAX_POWER);
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

    public FluidTank getCoolantTank() { return coolantTank; }
    public long getJoules()           { return joules; }

    /** 1:1-Port von {@code updateEntity}. */
    public static void tick(Level level, BlockPos pos, BlockState state, MachineCoreReceiverBlockEntity be) {
        if (level.isClientSide()) return;

        be.ensureNetworkInitialized();

        // Original: power = joules * 5000, jeden Tick neu gesetzt statt aufaddiert.
        be.setEnergyStored(Math.min(MAX_POWER, be.joules * HE_PER_SPK));

        if (be.joules > 0) {
            if (be.coolantTank.getFill() >= COOLANT_PER_TICK_MB) {
                be.coolantTank.setFill(be.coolantTank.getFill() - COOLANT_PER_TICK_MB);
            } else {
                level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
                return;
            }
        }

        be.joules = 0;
        be.setChanged();
        be.sendUpdateToClient();
    }

    /**
     * 1:1-Port von {@code addEnergy}: nur Strahlen von vorn werden angenommen. Alles andere
     * sprengt den Empfaenger.
     */
    @Override
    public boolean addLaserEnergy(Level level, BlockPos pos, long energy, Direction beamDirection) {

        Direction facing = getBlockState().getValue(MachineCoreReceiverBlock.FACING);

        if (beamDirection.getOpposite() == facing) {
            joules += energy;
            setChanged();
            return true;
        }

        level.destroyBlock(pos, false);
        level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 2.5F,
                Level.ExplosionInteraction.BLOCK);
        return false;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        coolantTank.writeToNBT(tag, "coolant");
        tag.putLong("joules", joules);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        coolantTank.readFromNBT(tag, "coolant");
        joules = tag.getLong("joules");
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
