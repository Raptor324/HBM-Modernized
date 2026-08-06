package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous;
import com.hbm_m.inventory.menu.MachineFlareStackMenu;

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
 * Gasfackel: Portierung von {@code TileEntityMachineGasFlare} (1.7.10 Original) - ein reiner Fluid-Senke/
 * Sicherheitsventil-Mechanismus: brennbares Gas wird verbrannt und erzeugt Energie, alles andere wird einfach
 * abgefackelt/entlueftet.
 * <p>
 * Vereinfachung ggue. Original (siehe Aufgabenstellung): keine GUI-Ventil-/Zuend-Schalter ({@code isOn}/
 * {@code doesBurn}, im Original per Redstone-Control-Paket umschaltbar) - stattdessen automatisch: brennbare
 * Fluids (per {@link FT_Flammable}-Trait, siehe {@code FluidType}) werden immer verbrannt, alles andere wird
 * immer entlueftet. Keine Entity-Schadens-/Partikel-/Pollution-Effekte (Original: {@code FT_Polluting.pollute},
 * Flammen-AoE-Schaden) - reine Fluid->Energie-Umwandlung. Der {@link FT_Gaseous}-Trait wird nur fuer den
 * Verbrennungs-Energie-Malus (5 statt 10, siehe Original {@code penalty}) beruecksichtigt, nicht fuer die Vent-Menge.
 */
public class MachineFlareStackBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2 {

    private static final long MAX_POWER = 100_000L;
    private static final long MAX_EXTRACT = 1_000L;

    private static final int TANK_CAPACITY_MB = 64_000;
    private static final int MAX_VENT_MB = 50;
    private static final int MAX_BURN_MB = 10;
    private static final int BURN_PENALTY_LIQUID = 10;
    private static final int BURN_PENALTY_GASEOUS = 5;

    private final FluidTank tank;
    private int fluidUsed = 0;
    private long lastOutput = 0;

    public MachineFlareStackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLARE_STACK_BE.get(), pos, state, 0, MAX_POWER, 0L, MAX_EXTRACT);
        tank = new FluidTank(TANK_CAPACITY_MB);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineFlareStackBlockEntity be) {
        if (level.isClientSide) return;

        if (level.getGameTime() % 10 == 0) {
            for (Direction dir : Direction.values()) {
                be.trySubscribe(be.tank.getTankType(), level, pos.relative(dir), dir);
            }
        }

        int fluidUsed = 0;
        long produced = 0;

        if (be.tank.getFill() > 0) {
            boolean flammable = FluidType.hasTrait(be.tank.getTankType(), FT_Flammable.class);

            if (flammable) {
                int eject = Math.min(MAX_BURN_MB, be.tank.getFill());
                be.tank.drainMb(eject);
                fluidUsed = eject;

                FT_Flammable trait = FluidType.getTrait(be.tank.getTankType(), FT_Flammable.class);
                if (trait != null) {
                    boolean gaseous = FluidType.hasTrait(be.tank.getTankType(), FT_Gaseous.class);
                    int penalty = gaseous ? BURN_PENALTY_GASEOUS : BURN_PENALTY_LIQUID;
                    produced = (trait.getHeatEnergy() * eject / 1_000L) / penalty;
                    be.setEnergyStored(Math.min(be.getMaxEnergyStored(), be.getEnergyStored() + produced));
                }
            } else {
                int eject = Math.min(MAX_VENT_MB, be.tank.getFill());
                be.tank.drainMb(eject);
                fluidUsed = eject;
            }
        }

        be.fluidUsed = fluidUsed;
        be.lastOutput = produced;

        be.setChanged();
        be.sendUpdateToClient();
    }

    // ==================== GUI (generische GuiInfoScreen-Balken) ====================

    public int getProgress() {
        return tank.getFill();
    }

    public int getMaxProgress() {
        return tank.getMaxFill();
    }

    public int getProgressScaled(int scale) {
        int max = tank.getMaxFill();
        return max <= 0 ? 0 : tank.getFill() * scale / max;
    }

    public boolean isActive() {
        return fluidUsed > 0;
    }

    public long getLastOutput() {
        return lastOutput;
    }

    public FluidTank getTank() {
        return tank;
    }

    // ==================== IFluidUserMK2 / MK2-Netz ====================

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { tank };
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { tank };
    }

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
        tag.putInt("fluidUsed", fluidUsed);
        tag.putLong("lastOutput", lastOutput);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        fluidUsed = tag.getInt("fluidUsed");
        lastOutput = tag.getLong("lastOutput");
        tank.readFromNBT(tag, "tank");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.flare_stack");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineFlareStackMenu.create(id, inventory, this);
    }
}
