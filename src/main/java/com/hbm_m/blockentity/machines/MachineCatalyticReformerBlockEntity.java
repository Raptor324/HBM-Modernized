package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineCatalyticReformerMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.recipe.CatalyticReformerRecipes;
import com.hbm_m.recipe.CatalyticReformerRecipes.Triple;

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
 * Katalytischer Reformer: Portierung der Kernlogik aus {@code TileEntityMachineCatalyticReformer}
 * (1.7.10 Original). Wandelt jeden Tick 100mB eines Oel-Fluids (Tank 0) in drei Ausgangsfluide um
 * (Tanks 1-3, darunter immer etwas Wasserstoff als Nebenprodukt - anders als beim Hydrotreater
 * wird hier kein Wasserstoff verbraucht), ueber die feste Rezeptliste
 * {@link CatalyticReformerRecipes} (Direktport von {@code ReformingRecipes}). Erfordert wie im
 * Original einen katalytischen Konverter ({@link ModItems#CATALYTIC_CONVERTER}) im Katalysatorslot.
 */
public class MachineCatalyticReformerBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_CATALYST = 1;
    private static final int SLOT_COUNT = 2;

    private static final long MAX_POWER = 500_000L;
    private static final long POWER_PER_CYCLE = 20_000L;
    private static final int INPUT_CAPACITY_MB = 4000;
    private static final int OUTPUT_CAPACITY_MB = 2000;
    private static final int INPUT_PER_CYCLE_MB = 100;

    private final FluidTank[] tanks = new FluidTank[4];

    public MachineCatalyticReformerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CATALYTIC_REFORMER_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER, 0L);
        tanks[0] = new FluidTank(INPUT_CAPACITY_MB) {
            @Override
            public boolean isFluidValid(Fluid fluid) {
                return CatalyticReformerRecipes.has(fluid);
            }
        };
        tanks[1] = new FluidTank(OUTPUT_CAPACITY_MB);
        tanks[2] = new FluidTank(OUTPUT_CAPACITY_MB);
        tanks[3] = new FluidTank(OUTPUT_CAPACITY_MB);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCatalyticReformerBlockEntity be) {
        if (level.isClientSide) return;

        be.chargeFromBatterySlot(SLOT_BATTERY);
        be.reform();

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            be.trySubscribe(be.tanks[0].getTankType(), level, neighborPos, dir);
            be.tryProvide(be.tanks[1], level, neighborPos, dir);
            be.tryProvide(be.tanks[2], level, neighborPos, dir);
            be.tryProvide(be.tanks[3], level, neighborPos, dir);
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    /** Direktport von {@code reform()}. */
    private void reform() {
        Triple recipe = CatalyticReformerRecipes.get(tanks[0].getTankType());
        if (recipe == null) {
            tanks[1].conform(com.hbm_m.inventory.fluid.ModFluids.NONE.getSource());
            tanks[2].conform(com.hbm_m.inventory.fluid.ModFluids.NONE.getSource());
            tanks[3].conform(com.hbm_m.inventory.fluid.ModFluids.NONE.getSource());
            return;
        }
        if (tanks[1].isEmpty()) tanks[1].conform(recipe.outA());
        if (tanks[2].isEmpty()) tanks[2].conform(recipe.outB());
        if (tanks[3].isEmpty()) tanks[3].conform(recipe.outC());

        if (getEnergyStored() < POWER_PER_CYCLE) return;
        if (tanks[0].getFill() < INPUT_PER_CYCLE_MB) return;
        if (!hasCatalyst()) return;
        if (tanks[1].getFill() + recipe.amountA() > tanks[1].getMaxFill()) return;
        if (tanks[2].getFill() + recipe.amountB() > tanks[2].getMaxFill()) return;
        if (tanks[3].getFill() + recipe.amountC() > tanks[3].getMaxFill()) return;

        setEnergyStored(getEnergyStored() - POWER_PER_CYCLE);
        tanks[0].drainMb(INPUT_PER_CYCLE_MB);
        tanks[1].fillMb(recipe.outA(), recipe.amountA());
        tanks[2].fillMb(recipe.outB(), recipe.amountB());
        tanks[3].fillMb(recipe.outC(), recipe.amountC());
    }

    private boolean hasCatalyst() {
        ItemStack stack = inventory.getStackInSlot(SLOT_CATALYST);
        return !stack.isEmpty() && stack.is(ModItems.CATALYTIC_CONVERTER.get());
    }

    // ==================== GUI ====================

    public FluidTank[] getTanks() {
        return tanks;
    }

    // ==================== IFluidUserMK2 / MK2-Netz ====================

    @Override
    public FluidTank[] getAllTanks() {
        return tanks;
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { tanks[0] };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { tanks[1], tanks[2], tanks[3] };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null && (CatalyticReformerRecipes.has(fluid)
                || tanks[1].getTankType() == fluid || tanks[2].getTankType() == fluid || tanks[3].getTankType() == fluid);
    }

    // ==================== NBT ====================

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].writeToNBT(tag, "tank" + i);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].readFromNBT(tag, "tank" + i);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.catalytic_reformer");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return isEnergyProviderItem(stack);
        if (slot == SLOT_CATALYST) return stack.is(ModItems.CATALYTIC_CONVERTER.get());
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineCatalyticReformerMenu.create(id, inventory, this);
    }
}
