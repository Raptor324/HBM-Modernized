package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.inventory.fluid.trait.PollutionType;
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
import net.minecraft.world.level.material.Fluid;

/**
 * Chimney (Brick/Industrial) - Port von {@code TileEntityChimneyBase}/{@code TileEntityChimneyBrick}/
 * {@code TileEntityChimneyIndustrial} (1.7.10 Original). Eine Klasse fuer beide Varianten,
 * unterschieden per Block-Identitaet (analog {@code MachineStirlingBlockEntity}).
 * <p>
 * Im Original ist der Schornstein die Senke der Rauch-Fluid-Pipeline: er zieht SMOKE/SMOKE_LEADED/
 * SMOKE_POISON aus dem Rohrnetz (gespeist von Diesel Generator/Combustion Engine/Boiler-Abgas) und
 * wandelt es ueber {@code PollutionHandler.incrementPollution} in ein Welt-Verschmutzungsraster um,
 * plus optionales Russ-/Asche-Abwerfen in einen darunterliegenden Ashpit.
 * <p>
 * Das Verschmutzungsraster gibt es inzwischen ({@link com.hbm_m.handler.pollution.PollutionHandler}),
 * der Schornstein traegt also wieder ein: SMOKE wird zu Russ, SMOKE_LEADED zu Schwermetall,
 * SMOKE_POISON zu Gift, jeweils {@code Menge / 100} mal dem Faktor der Bauart.
 * <p>
 * Weiterhin offen: der Ashpit-Block und damit {@code cpaturesAsh()}/{@code cpaturesSoot()} des
 * Originals - der Industrieschornstein faengt dort zusaetzlich Russ als Gegenstand ab.
 */
public class MachineChimneyBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2 {

    private static final int TANK_CAPACITY_MB = 16_000;

    private final FluidTank tank = new FluidTank(TANK_CAPACITY_MB);

    public MachineChimneyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHIMNEY_BE.get(), pos, state, 0, 0L, 0L, 0L);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return tank.getForgeFluidCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    public static void tick(Level level, BlockPos pos, BlockState state, MachineChimneyBlockEntity be) {
        if (level.isClientSide()) return;

        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                be.trySubscribe(be.tank.getTankType(), level, pos.relative(dir), dir);
            }
        }

        int amount = be.tank.getFluidAmountMb();
        if (amount > 0) {
            be.pollute(level, pos, be.tank.getTankType(), amount);
            be.tank.drainMb(amount);
            be.setChanged();
        }
    }

    /**
     * Original: {@code TileEntityChimneyBase.transferFluid} - der Rauch geht nicht verloren,
     * sondern landet als Verschmutzung im Raster.
     */
    private void pollute(Level level, BlockPos pos, Fluid fluid, int amountMb) {
        PollutionType type;

        if (fluid == ModFluids.SMOKE.getSource()) {
            type = PollutionType.SOOT;
        } else if (fluid == ModFluids.SMOKE_LEADED.getSource()) {
            type = PollutionType.HEAVYMETAL;
        } else if (fluid == ModFluids.SMOKE_POISON.getSource()) {
            type = PollutionType.POISON;
        } else {
            return;
        }

        PollutionHandler.incrementPollution(level, pos, type,
                (float) (amountMb * getPollutionMod() / 100F));
    }

    /**
     * Original: {@code getPollutionMod} - der gemauerte Schornstein laesst ein Viertel durch,
     * der industrielle nur ein Zehntel. (Der Rampant-Modus des Originals ist nicht portiert.)
     */
    private double getPollutionMod() {
        return getBlockState().is(ModBlocks.CHIMNEY_INDUSTRIAL.get()) ? 0.1D : 0.25D;
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
        return fromDir != null
                && (fluid == ModFluids.SMOKE.getSource()
                        || fluid == ModFluids.SMOKE_LEADED.getSource()
                        || fluid == ModFluids.SMOKE_POISON.getSource());
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tank.readFromNBT(tag, "tank");
    }

    // ==================== GETTERS / MENU ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.chimney");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false; // Kein Inventar.
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null; // Kein GUI im Original.
    }
}
