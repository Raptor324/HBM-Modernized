package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineElectrolyserMenu;
import com.hbm_m.recipe.ElectrolyserRecipes;
import com.hbm_m.recipe.ElectrolyserRecipes.FluidRecipe;
import com.hbm_m.recipe.ElectrolyserRecipes.MetalRecipe;

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
 * Electrolyser: Direktport der Kernlogik aus {@code TileEntityElectrolyser} (1.7.10 Original) - ein
 * Zwei-Modus-Geraet (Fluid-Elektrolyse und Kristall/Erz-Elektrolyse). Siehe {@link ElectrolyserRecipes}
 * fuer die dokumentierten Vereinfachungen der Rezepttabellen.
 * <p>
 * Vereinfachungen ggue. Original: kein Item-Upgrade-System (SPEED/POWER/OVERDRIVE-Slots entfallen,
 * feste Basiswerte fuer Verbrauch/Dauer), Metall-Modus-Output als direkte Items statt Crucible-
 * Molten-Pour, kein separates GUI-Umschalt-Paket (beide Modi in einem Menu vereint statt zwei
 * getrennten Containern wie im Original {@code ContainerElectrolyserFluid}/{@code ...Metal}).
 */
public class MachineElectrolyserBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_FLUID_ID = 1;
    public static final int SLOT_BYPRODUCT_1 = 2;
    public static final int SLOT_BYPRODUCT_2 = 3;
    public static final int SLOT_BYPRODUCT_3 = 4;
    public static final int SLOT_CRYSTAL = 5;
    public static final int SLOT_METAL_OUT_1 = 6;
    public static final int SLOT_METAL_OUT_2 = 7;
    public static final int SLOT_METAL_BYPRODUCT_1 = 8;
    public static final int SLOT_METAL_BYPRODUCT_2 = 9;
    private static final int SLOT_COUNT = 10;

    private static final long MAX_POWER = 20_000_000L;
    private static final long USAGE_FLUID = 10_000L;
    private static final long USAGE_METAL = 10_000L;
    private static final int PROCESS_FLUID_TIME = 100;
    private static final int PROCESS_METAL_TIME = 600;

    private final FluidTank[] tanks = new FluidTank[4];

    public int progressFluid = 0;
    public int progressMetal = 0;

    public MachineElectrolyserBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTROLYSER_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER);
        tanks[0] = new FluidTank(16_000); // water/generic input
        tanks[1] = new FluidTank(16_000); // hydrogen-style output A
        tanks[2] = new FluidTank(16_000); // oxygen-style output B
        tanks[3] = new FluidTank(16_000); // nitric acid (metal-mode etch fluid) - kept for tank-count parity, unused by metal recipes here
    }

    public FluidTank[] getTanks() { return tanks; }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineElectrolyserBlockEntity be) {
        if (level.isClientSide()) return;

        be.chargeFromBatterySlot(SLOT_BATTERY);

        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                be.trySubscribe(be.tanks[0].getTankType(), level, pos.relative(dir), dir);
                if (be.tanks[1].getFill() > 0) be.tryProvide(be.tanks[1], level, pos.relative(dir), dir);
                if (be.tanks[2].getFill() > 0) be.tryProvide(be.tanks[2], level, pos.relative(dir), dir);
            }
        }

        if (be.canProcessFluid()) {
            be.progressFluid++;
            be.energy = Math.max(0, be.energy - USAGE_FLUID);
            if (be.progressFluid >= PROCESS_FLUID_TIME) {
                be.progressFluid = 0;
                be.processFluid();
            }
        } else {
            be.progressFluid = 0;
        }

        if (be.canProcessMetal()) {
            be.progressMetal++;
            be.energy = Math.max(0, be.energy - USAGE_METAL);
            if (be.progressMetal >= PROCESS_METAL_TIME) {
                be.progressMetal = 0;
                be.processMetal();
            }
        } else {
            be.progressMetal = 0;
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    // ==================== Fluid mode ====================

    private boolean canProcessFluid() {
        if (energy < USAGE_FLUID) return false;
        FluidRecipe recipe = ElectrolyserRecipes.getFluidRecipe(tanks[0].getTankType());
        if (recipe == null) return false;
        if (recipe.amount() > tanks[0].getFill()) return false;
        if (recipe.fillA() + tanks[1].getFill() > tanks[1].getMaxFill()) return false;
        if (recipe.fillB() + tanks[2].getFill() > tanks[2].getMaxFill()) return false;
        return canAcceptByproducts(recipe.byproducts(), SLOT_BYPRODUCT_1);
    }

    private void processFluid() {
        FluidRecipe recipe = ElectrolyserRecipes.getFluidRecipe(tanks[0].getTankType());
        if (recipe == null) return;

        tanks[0].drainMb(recipe.amount());
        if (recipe.fillA() > 0) tanks[1].fillMb(recipe.outA(), recipe.fillA());
        if (recipe.fillB() > 0) tanks[2].fillMb(recipe.outB(), recipe.fillB());
        depositByproducts(recipe.byproducts(), SLOT_BYPRODUCT_1);
    }

    // ==================== Metal mode ====================

    private boolean canProcessMetal() {
        ItemStack crystal = inventory.getStackInSlot(SLOT_CRYSTAL);
        if (crystal.isEmpty()) return false;
        if (energy < USAGE_METAL) return false;

        MetalRecipe recipe = ElectrolyserRecipes.getMetalRecipe(crystal);
        if (recipe == null) return false;

        if (!canAcceptOutput(SLOT_METAL_OUT_1, recipe.outA())) return false;
        if (!recipe.outB().isEmpty() && !canAcceptOutput(SLOT_METAL_OUT_2, recipe.outB())) return false;
        return canAcceptByproducts(recipe.byproducts(), SLOT_METAL_BYPRODUCT_1);
    }

    private void processMetal() {
        ItemStack crystal = inventory.getStackInSlot(SLOT_CRYSTAL);
        MetalRecipe recipe = ElectrolyserRecipes.getMetalRecipe(crystal);
        if (recipe == null) return;

        inventory.extractItem(SLOT_CRYSTAL, 1, false);
        depositOutput(SLOT_METAL_OUT_1, recipe.outA());
        if (!recipe.outB().isEmpty()) depositOutput(SLOT_METAL_OUT_2, recipe.outB());
        depositByproducts(recipe.byproducts(), SLOT_METAL_BYPRODUCT_1);
    }

    // ==================== shared slot helpers ====================

    private boolean canAcceptOutput(int slot, ItemStack result) {
        ItemStack current = inventory.getStackInSlot(slot);
        if (current.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(current, result)) return false;
        return current.getCount() + result.getCount() <= current.getMaxStackSize();
    }

    private void depositOutput(int slot, ItemStack result) {
        ItemStack current = inventory.getStackInSlot(slot);
        if (current.isEmpty()) {
            inventory.setStackInSlot(slot, result.copy());
        } else {
            current.grow(result.getCount());
        }
    }

    private boolean canAcceptByproducts(ItemStack[] byproducts, int firstSlot) {
        for (int i = 0; i < byproducts.length && i < 3; i++) {
            if (!canAcceptOutput(firstSlot + i, byproducts[i])) return false;
        }
        return true;
    }

    private void depositByproducts(ItemStack[] byproducts, int firstSlot) {
        for (int i = 0; i < byproducts.length && i < 3; i++) {
            depositOutput(firstSlot + i, byproducts[i]);
        }
    }

    public int getProgressFluidScaled(int scale) { return progressFluid * scale / PROCESS_FLUID_TIME; }
    public int getProgressMetalScaled(int scale) { return progressMetal * scale / PROCESS_METAL_TIME; }

    // ==================== IFluidUserMK2 / MK2-Netz ====================

    @Override
    public FluidTank[] getAllTanks() { return tanks; }

    @Override
    public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1], tanks[2] }; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0], tanks[3] }; }

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
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("progressFluid", progressFluid);
        tag.putInt("progressMetal", progressMetal);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(tag, "tank" + i);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progressFluid = tag.getInt("progressFluid");
        progressMetal = tag.getInt("progressMetal");
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(tag, "tank" + i);
    }

    // ==================== GUI ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.electrolyser");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> isEnergyProviderItem(stack);
            case SLOT_FLUID_ID -> true;
            case SLOT_CRYSTAL -> ElectrolyserRecipes.hasMetalRecipe(stack);
            default -> false;
        };
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return MachineElectrolyserMenu.create(id, inv, this);
    }
}
