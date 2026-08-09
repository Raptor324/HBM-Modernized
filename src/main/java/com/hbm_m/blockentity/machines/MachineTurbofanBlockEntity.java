package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Combustible;
import com.hbm_m.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm_m.inventory.menu.MachineTurbofanMenu;

import net.minecraft.core.BlockPos;
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
 * Turbofan - 1:1-Port der Kernfunktion aus {@code TileEntityMachineTurbofan} (1.7.10-Original):
 * verbrennt AERO-Grade-Treibstoff (Kerosin) zu Energie. Original-Formel: pro Tick wird
 * {@code amountToBurn = min(1 + afterburner, tankFill)} mB Treibstoff verbrannt, Energie-Output
 * {@code burnValue * amountToBurn * (1 + min(afterburner/3, 4))} mit
 * {@code burnValue = combustionEnergy / 1000}.
 * <p>
 * Bewusst NICHT uebernommen (Aufgabenstellung: Funktion vor Politur): Afterburner-Upgrade-Stufe
 * (kein Upgrade-System an diesem Block, {@code afterburner} bleibt 0), Redstone-Lock-Check,
 * Ansaug-/Abgasstrahl-Entity-Interaktion (Feuer/Instant-Kill-Hazard-Mechanik), Blut-Partikel-Fluid,
 * Rauch-/Feuerpartikel, Sound, Verschmutzung (Pollution). Reine Energieerzeugung aus Treibstoff
 * bleibt 1:1 erhalten.
 * <p>
 * GUI/Menu-Slots (manuelle Entscheidung, siehe {@code MachineTurbofanMenu}): das Original hatte 5
 * Slots (Treibstoff-Behaelter, Leer-Behaelter-Ausgabe, Upgrade, Batterie, Fluid-Identifier). Der
 * Upgrade-Slot entfaellt (kein Upgrade-System an diesem Block, siehe oben), daher bleiben genau die
 * 4 hier vorhandenen Slots uebrig: {@link #SLOT_FUEL_CONTAINER}, {@link #SLOT_EMPTY_CONTAINER},
 * {@link #SLOT_BATTERY}, {@link #SLOT_FLUID_IDENTIFIER}. Nur der Batterie-Slot ist aktiv verdrahtet
 * (wird pro Tick ueber {@code chargeItemInSlot} geladen, analog zum Combustion Engine). Behaelter-
 * Befuellung/-Leerung (slot 0/1) und das erzwungene Setzen des Tank-Typs per Identifier-Item (slot 3)
 * sind NICHT implementiert - der Tank wird ausschliesslich ueber das Fluid-Netz (Capability) befuellt.
 */
public class MachineTurbofanBlockEntity extends BaseMachineBlockEntity {

    public static final int SLOT_FUEL_CONTAINER = 0;
    public static final int SLOT_EMPTY_CONTAINER = 1;
    public static final int SLOT_BATTERY = 2;
    public static final int SLOT_FLUID_IDENTIFIER = 3;

    private static final int TANK_CAPACITY_MB = 24_000;
    private static final int BASE_BURN_MB_PER_TICK = 1;

    private final FluidTank tank = new FluidTank(ModFluids.KEROSENE.getSource(), TANK_CAPACITY_MB);

    public MachineTurbofanBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBOFAN_BE.get(), pos, state, 4, 2_000_000L, 0L, 80_000L);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return tank.getCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    public static void tick(Level level, BlockPos pos, BlockState state, MachineTurbofanBlockEntity be) {
        if (level.isClientSide()) return;

        be.chargeItemInSlot(SLOT_BATTERY);

        Fluid fuel = be.tank.getStoredFluid();
        FT_Combustible combustible = FluidType.getTrait(fuel, FT_Combustible.class);

        boolean dirty = false;
        if (combustible != null && combustible.getGrade() == FuelGrade.AERO) {
            int amountToBurn = Math.min(BASE_BURN_MB_PER_TICK, be.tank.getFluidAmountMb());
            if (amountToBurn > 0 && be.getEnergyStored() < be.getMaxEnergyStored()) {
                long burnValue = combustible.getCombustionEnergy() / 1_000L;
                long output = burnValue * amountToBurn;

                be.tank.drainMb(amountToBurn);
                be.setEnergyStored(Math.min(be.getMaxEnergyStored(), be.getEnergyStored() + output));
                dirty = true;
            }
        }

        if (dirty) {
            be.setChanged();
            be.sendUpdateToClient();
        }
    }

    public FluidTank getTank() {
        return tank;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tank.writeToNBT(tag, "fuel");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        tank.readFromNBT(tag, "fuel");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.turbofan");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineTurbofanMenu(id, inv, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.turbofan");
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return isEnergyReceiverItem(stack);
        if (slot == SLOT_FLUID_IDENTIFIER) return stack.getItem() instanceof IItemFluidIdentifier;
        if (slot == SLOT_FUEL_CONTAINER) return true;
        return false;
    }
}
