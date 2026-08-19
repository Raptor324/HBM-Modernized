package com.hbm_m.blockentity.machines;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachinePUREXMenu;
import com.hbm_m.recipe.PurexRecipe;

import dev.architectury.fluid.FluidStack;
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
 * PUREX - Port von {@code TileEntityMachinePUREX}/{@code ModuleMachinePUREX} (1.7.10 Original,
 * "Plutonium-Uranium Redox EXtraction"). Der mod-interne allgemeine Wiederaufbereitungs-Knoten
 * fuer ~40 unzusammenhaengende Chemie-/Abfall-Rezepte (Zirnox-/PWR-/Watz-Abfall -> Nuggets +
 * Kern-Abfall, Schrabidium-Extraktion, Antimaterie-Verarbeitung, etc.) - siehe {@link PurexRecipe}
 * fuer die Rezeptform (bis zu 3 Item-Eingaenge, 3 Fluid-Eingaenge, 6 Item-Ausgaenge, 1 Fluid-
 * Ausgang je Rezept), 1:1 aus dem Original-Limit uebernommen.
 * <p>
 * SCOPE-Entscheidungen:
 * <ul>
 *   <li>Kein echtes 5x2x5-Multiblock mit Eck-Dummy-Bloecken (Original: {@code BlockDummyable}) -
 *   wie bei anderen "grosses Modell, im Kern automatisiert"-Maschinen dieser Session vereinfacht
 *   auf einen Einzelblock (siehe {@code MachineAmmoPressBlockEntity}).</li>
 *   <li>Kein Blueprint-Slot/Rezeptauswahl-GUI (Original waehlt manuell aus einer Liste passender
 *   Rezepte). Stattdessen automatische Erkennung: jeden Tick wird das erste {@link PurexRecipe}
 *   gesucht, dessen Item-/Fluid-Eingaenge mit dem aktuellen Slot-/Tank-Inhalt uebereinstimmen -
 *   entspricht in etwa den "Autoswitch-Gruppen" des Originals (automatische Erkennung anhand des
 *   eingelegten Materials), nur global statt gruppenweise.</li>
 *   <li>Kein Upgrade-System (SPEED/POWER-Slots) - konsistent mit dem Rest dieses Ports.</li>
 * </ul>
 */
public class MachinePUREXBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final int SLOT_BATTERY = 0;
    public static final int ITEM_INPUT_START = 1;
    private static final int ITEM_INPUT_COUNT = 3;
    public static final int ITEM_OUTPUT_START = 4;
    private static final int ITEM_OUTPUT_COUNT = 6;
    private static final int SLOT_COUNT = 10;

    private static final int TANK_CAPACITY_MB = 24_000;
    private static final long MAX_POWER = 1_000_000L;

    private final FluidTank[] inputTanks = new FluidTank[3];
    private final FluidTank outputTank = new FluidTank(TANK_CAPACITY_MB);

    private int progressTicks = 0;
    private int currentDuration = 1;

    public MachinePUREXBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUREX_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER);
        for (int i = 0; i < inputTanks.length; i++) {
            inputTanks[i] = new FluidTank(TANK_CAPACITY_MB);
        }
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return inputTanks[0].getForgeFluidCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    public static void tick(Level level, BlockPos pos, BlockState state, MachinePUREXBlockEntity be) {
        if (level.isClientSide()) return;
        be.serverTick(level, pos);
    }

    private void serverTick(Level level, BlockPos pos) {
        chargeFromBatterySlot(SLOT_BATTERY);

        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                for (FluidTank tank : inputTanks) {
                    trySubscribe(tank.getTankType(), level, pos.relative(dir), dir);
                }
                tryProvide(outputTank, level, pos.relative(dir), dir);
            }
        }

        PurexRecipe recipe = findRecipe(level);
        boolean dirty = false;

        if (recipe != null && canProcess(recipe) && getEnergyStored() >= recipe.getPowerConsumption()) {
            currentDuration = Math.max(1, recipe.getDuration());
            progressTicks++;
            setEnergyStored(getEnergyStored() - recipe.getPowerConsumption());
            dirty = true;
            if (progressTicks >= currentDuration) {
                progressTicks = 0;
                completeCycle(recipe);
            }
        } else if (progressTicks > 0) {
            progressTicks = 0;
            dirty = true;
        }

        if (dirty) {
            setChanged();
            sendUpdateToClient();
        }
    }

    @Nullable
    private PurexRecipe findRecipe(Level level) {
        // Кросс-версионный доступ: RecipeHooks.getAllRecipes разворачивает RecipeHolder на 1.21.1.
        List<PurexRecipe> recipes = com.hbm_m.platform.recipe.RecipeHooks.getAllRecipes(level, PurexRecipe.Type.INSTANCE);
        for (PurexRecipe recipe : recipes) {
            if (canProcess(recipe)) return recipe;
        }
        return null;
    }

    private boolean canProcess(PurexRecipe recipe) {
        List<PurexRecipe.CountedIngredient> itemInputs = recipe.getItemInputs();
        if (itemInputs.size() > ITEM_INPUT_COUNT) return false;
        for (int i = 0; i < itemInputs.size(); i++) {
            ItemStack slotStack = inventory.getStackInSlot(ITEM_INPUT_START + i);
            PurexRecipe.CountedIngredient req = itemInputs.get(i);
            if (!req.ingredient().test(slotStack) || slotStack.getCount() < req.count()) return false;
        }

        List<PurexRecipe.FluidIngredient> fluidInputs = recipe.getFluidInputs();
        if (fluidInputs.size() > inputTanks.length) return false;
        for (int i = 0; i < fluidInputs.size(); i++) {
            PurexRecipe.FluidIngredient req = fluidInputs.get(i);
            Fluid fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(req.fluidId());
            FluidTank tank = inputTanks[i];
            if (tank.isEmpty() || tank.getStoredFluid() != fluid || tank.getFluidAmountMb() < req.amount()) {
                return false;
            }
        }

        List<ItemStack> itemOutputs = recipe.getItemOutputs();
        if (itemOutputs.size() > ITEM_OUTPUT_COUNT) return false;
        for (int i = 0; i < itemOutputs.size(); i++) {
            ItemStack output = itemOutputs.get(i);
            if (output.isEmpty()) continue;
            ItemStack outSlot = inventory.getStackInSlot(ITEM_OUTPUT_START + i);
            if (!outSlot.isEmpty()) {
                if (!com.hbm_m.platform.PlatformHooks.isSameItemSameTags(outSlot, output)) return false;
                if (outSlot.getCount() + output.getCount() > outSlot.getMaxStackSize()) return false;
            }
        }

        List<FluidStack> fluidOutputs = recipe.getFluidOutputs();
        if (!fluidOutputs.isEmpty()) {
            FluidStack fluidOut = fluidOutputs.get(0);
            if (!fluidOut.isEmpty()) {
                if (!outputTank.isEmpty() && outputTank.getStoredFluid() != fluidOut.getFluid()) return false;
                if (outputTank.getFluidAmountMb() + fluidOut.getAmount() > outputTank.getCapacityMb()) return false;
            }
        }

        return true;
    }

    private void completeCycle(PurexRecipe recipe) {
        List<PurexRecipe.CountedIngredient> itemInputs = recipe.getItemInputs();
        for (int i = 0; i < itemInputs.size(); i++) {
            inventory.getStackInSlot(ITEM_INPUT_START + i).shrink(itemInputs.get(i).count());
        }

        List<PurexRecipe.FluidIngredient> fluidInputs = recipe.getFluidInputs();
        for (int i = 0; i < fluidInputs.size(); i++) {
            inputTanks[i].drainMb(fluidInputs.get(i).amount());
        }

        List<ItemStack> itemOutputs = recipe.getItemOutputs();
        for (int i = 0; i < itemOutputs.size(); i++) {
            ItemStack output = itemOutputs.get(i);
            if (output.isEmpty()) continue;
            ItemStack outSlot = inventory.getStackInSlot(ITEM_OUTPUT_START + i);
            if (outSlot.isEmpty()) {
                inventory.setStackInSlot(ITEM_OUTPUT_START + i, output.copy());
            } else {
                outSlot.grow(output.getCount());
            }
        }

        List<FluidStack> fluidOutputs = recipe.getFluidOutputs();
        if (!fluidOutputs.isEmpty()) {
            FluidStack fluidOut = fluidOutputs.get(0);
            if (!fluidOut.isEmpty()) {
                outputTank.fillMb(fluidOut.getFluid(), (int) fluidOut.getAmount());
            }
        }
    }

    // ==================== IFluidUserMK2 / MK2-Netz ====================

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { inputTanks[0], inputTanks[1], inputTanks[2], outputTank };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { outputTank };
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return inputTanks;
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
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("progress", progressTicks);
        tag.putInt("duration", currentDuration);
        for (int i = 0; i < inputTanks.length; i++) {
            inputTanks[i].writeToNBT(tag, "tank_in_" + i);
        }
        outputTank.writeToNBT(tag, "tank_out");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        progressTicks = tag.getInt("progress");
        currentDuration = tag.contains("duration") ? Math.max(1, tag.getInt("duration")) : 1;
        for (int i = 0; i < inputTanks.length; i++) {
            inputTanks[i].readFromNBT(tag, "tank_in_" + i);
        }
        outputTank.readFromNBT(tag, "tank_out");
    }

    // ==================== GETTERS / MENU ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.purex");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return isEnergyProviderItem(stack);
        if (slot >= ITEM_INPUT_START && slot < ITEM_INPUT_START + ITEM_INPUT_COUNT) return true;
        return false; // Ausgabe-Slots: kein manuelles Einlegen.
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachinePUREXMenu(containerId, playerInventory, this);
    }

    public FluidTank getInputTank(int index) {
        return inputTanks[index];
    }

    public FluidTank getOutputTank() {
        return outputTank;
    }

    public int getProgressScaled(int scale) {
        return currentDuration <= 0 ? 0 : (progressTicks * scale) / currentDuration;
    }

    public boolean isActive() {
        return progressTicks > 0;
    }
}
