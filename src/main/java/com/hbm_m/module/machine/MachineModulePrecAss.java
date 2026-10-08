package com.hbm_m.module.machine;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.PrecAssRecipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * 1:1 {@code ModuleMachinePrecAss} + {@code ModuleMachineBase}: 9 positionsgebundene Eingaenge (Slots 4-12), 9 Ausgaenge
 * (13-21), je ein Ein- und Ausgangstank (mindestens 4.000 mB, sonst das Doppelte des Rezeptbedarfs). Der Fortschritt laeuft
 * von 0 bis 1 ({@code speed / duration} je Tick), der Strom wird jeden Tick abgezogen. Ausgaben mit mehreren Moeglichkeiten
 * brauchen einen leeren Ausgabeplatz.
 */
public class MachineModulePrecAss extends MachineModuleBase<PrecAssRecipe> {

    private final FluidTank inputTank;
    private final FluidTank outputTank;

    /** Original {@code progress} (0..1). */
    public double progressFrac;

    public MachineModulePrecAss(com.hbm_m.interfaces.IEnergyReceiver energy, com.hbm_m.platform.ModItemStackHandler inv,
                                FluidTank in, FluidTank out, Level level) {
        super(0, energy, inv, level);
        this.inputSlots = new int[9];
        this.outputSlots = new int[9];
        for (int i = 0; i < 9; i++) {
            this.inputSlots[i] = 4 + i;
            this.outputSlots[i] = 13 + i;
        }
        this.inputTank = in;
        this.outputTank = out;
    }

    public int[] getInputSlots() { return inputSlots; }

    @Nullable
    public PrecAssRecipe peekRecipe() {
        return getRecipeByIdCached(getRecipeType(), selectedRecipeId);
    }

    public void setSelectedRecipe(@Nullable ResourceLocation id) {
        setSelectedRecipeId(id);
    }

    private static Fluid fluid(ResourceLocation id) {
        Fluid f = BuiltInRegistries.FLUID.get(id);
        return f == null ? Fluids.EMPTY : f;
    }

    /** {@code setupTanks}: Sorte nach Rezept, Groesse max(Fuellung, 2x Bedarf, 4.000). */
    public void setupTanks(@Nullable PrecAssRecipe recipe) {
        if (recipe == null) return;
        if (!recipe.getFluidInputs().isEmpty()) {
            var f = recipe.getFluidInputs().get(0);
            inputTank.conform(fluid(f.fluidId()));
            inputTank.changeTankSize(Math.max(Math.max(inputTank.getFill(), f.amount() * 2), 4_000));
        } else {
            inputTank.resetTank();
        }
        if (!recipe.getFluidOutputs().isEmpty()) {
            var f = recipe.getFluidOutputs().get(0);
            outputTank.conform(fluid(f.fluidId()));
            outputTank.changeTankSize(Math.max(Math.max(outputTank.getFill(), f.amount() * 2), 4_000));
        } else {
            outputTank.resetTank();
        }
    }

    private boolean canProcessPrec(PrecAssRecipe recipe, double power) {
        long need = power == 1 ? recipe.getPower() : (long) (recipe.getPower() * power);
        if (energyStorage.getEnergyStored() < need) return false;
        if (!hasInput(recipe)) return false;
        return canFitOutput(recipe);
    }

    private boolean hasInput(PrecAssRecipe recipe) {
        List<PrecAssRecipe.CountedIngredient> in = recipe.getItemInputs();
        for (int i = 0; i < Math.min(in.size(), inputSlots.length); i++) {
            ItemStack stack = itemHandler.getStackInSlot(inputSlots[i]);
            if (stack.isEmpty() || !in.get(i).ingredient().test(stack) || stack.getCount() < in.get(i).count()) return false;
        }
        if (!recipe.getFluidInputs().isEmpty() && inputTank.getFill() < recipe.getFluidInputs().get(0).amount()) return false;
        return true;
    }

    private boolean canFitOutput(PrecAssRecipe recipe) {
        List<PrecAssRecipe.OutputGroup> out = recipe.getItemOutputs();
        for (int i = 0; i < Math.min(out.size(), outputSlots.length); i++) {
            ItemStack stack = itemHandler.getStackInSlot(outputSlots[i]);
            if (stack.isEmpty()) continue; // always continue if output slot is free
            PrecAssRecipe.OutputGroup output = out.get(i);
            if (output.possibleMultiOutput()) return false; // output slot needs to be empty to decide on multi outputs
            ItemStack single = output.getSingle();
            if (single == null) return false;
            if (!ItemStack.isSameItemSameTags(stack, single)) return false;
            if (stack.getCount() + single.getCount() > stack.getMaxStackSize()) return false;
        }
        if (!recipe.getFluidOutputs().isEmpty() && recipe.getFluidOutputs().get(0).amount() + outputTank.getFill() > outputTank.getMaxFill()) return false;
        return true;
    }

    private void process(PrecAssRecipe recipe, double speed, double power) {
        energyStorage.setEnergyStored(energyStorage.getEnergyStored() - (power == 1 ? recipe.getPower() : (long) (recipe.getPower() * power)));
        double step = Math.min(speed / recipe.getDuration(), 1D); // can't do more than one recipe per tick
        this.progressFrac += step;

        if (this.progressFrac >= 1D) {
            consumeInput(recipe);
            produceItem(recipe);
            if (canProcessPrec(recipe, power)) this.progressFrac -= 1D;
            else this.progressFrac = 0D;
        }
    }

    private void consumeInput(PrecAssRecipe recipe) {
        List<PrecAssRecipe.CountedIngredient> in = recipe.getItemInputs();
        for (int i = 0; i < Math.min(in.size(), inputSlots.length); i++) {
            ItemStack s = itemHandler.getStackInSlot(inputSlots[i]).copy();
            s.shrink(in.get(i).count());
            itemHandler.setStackInSlot(inputSlots[i], s.isEmpty() ? ItemStack.EMPTY : s);
        }
        if (!recipe.getFluidInputs().isEmpty()) inputTank.setFill(inputTank.getFill() - recipe.getFluidInputs().get(0).amount());
    }

    private void produceItem(PrecAssRecipe recipe) {
        List<PrecAssRecipe.OutputGroup> out = recipe.getItemOutputs();
        for (int i = 0; i < Math.min(out.size(), outputSlots.length); i++) {
            ItemStack collapse = out.get(i).collapse(level.random);
            ItemStack cur = itemHandler.getStackInSlot(outputSlots[i]);
            if (cur.isEmpty()) {
                itemHandler.setStackInSlot(outputSlots[i], collapse);
            } else if (!collapse.isEmpty()) {
                ItemStack grown = cur.copy();
                grown.grow(collapse.getCount());
                itemHandler.setStackInSlot(outputSlots[i], grown);
            }
        }
        if (!recipe.getFluidOutputs().isEmpty()) {
            var f = recipe.getFluidOutputs().get(0);
            outputTank.fillMb(fluid(f.fluidId()), f.amount());
        }
        this.needsSync = true;
    }

    /** {@code ModuleMachineBase.update}. */
    public void updatePrecAss(double speed, double power, boolean extraCondition, ItemStack blueprint) {
        PrecAssRecipe recipe = peekRecipe();

        if (recipe != null && recipe.isPooled() && !isBlueprintAllowedForPool(recipe.getBlueprintPool(), blueprint)) {
            this.didProcess = false;
            this.progressFrac = 0;
            this.selectedRecipeId = null;
            this.needsSync = true;
            return;
        }

        this.setupTanks(recipe);
        this.didProcess = false;

        if (extraCondition && recipe != null && canProcessPrec(recipe, power)) {
            this.process(recipe, speed, power);
            this.didProcess = true;
        } else {
            this.progressFrac = 0F;
        }
    }

    /** {@code isItemValid}: Eingabeplatz i nimmt nur Zutat i. */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        PrecAssRecipe recipe = peekRecipe();
        if (recipe == null) return false;
        List<PrecAssRecipe.CountedIngredient> in = recipe.getItemInputs();
        for (int i = 0; i < Math.min(inputSlots.length, in.size()); i++) {
            if (inputSlots[i] == slot && in.get(i).ingredient().test(stack)) return true;
        }
        return false;
    }

    // MachineModuleBase-Vertrag (das generische update() nutzt der Praezisionsassembler nicht)
    @Override protected net.minecraft.world.item.crafting.RecipeType<PrecAssRecipe> getRecipeType() { return PrecAssRecipe.Type.INSTANCE; }
    @Override protected @Nullable PrecAssRecipe findRecipeForInputs() { return peekRecipe(); }
    @Override protected boolean canProcess(@Nullable PrecAssRecipe recipe) { return recipe != null && canProcessPrec(recipe, 1); }
    @Override protected void processCraft(PrecAssRecipe recipe) { consumeInput(recipe); produceItem(recipe); }
    @Override protected boolean matchesCurrentRecipe(PrecAssRecipe recipe) {
        return selectedRecipeId != null && recipe != null && selectedRecipeId.equals(RecipeHooks.recipeId(level.getRecipeManager(), getRecipeType(), recipe));
    }
    @Override protected int getRecipeDuration(PrecAssRecipe recipe) { return recipe.getDuration(); }
    @Override protected long getRecipeEnergyCost(PrecAssRecipe recipe) { return recipe.getPower(); }
    @Override protected @Nullable PrecAssRecipe findRecipeForItem(ItemStack stack) { return peekRecipe(); }

    @Override
    protected void writeExtraToNbt(CompoundTag nbt) {
        nbt.putBoolean("HasRecipe", selectedRecipeId != null);
        if (selectedRecipeId != null) nbt.putString("SelectedRecipe", selectedRecipeId.toString());
        nbt.putDouble("progressFrac", progressFrac);
    }

    @Override
    protected void readExtraFromNbt(CompoundTag nbt) {
        selectedRecipeId = nbt.getBoolean("HasRecipe") ? ResourceLocation.tryParse(nbt.getString("SelectedRecipe")) : null;
        progressFrac = nbt.getDouble("progressFrac");
    }

    @Override
    protected void writeExtraToBuf(FriendlyByteBuf buf) {
        buf.writeBoolean(selectedRecipeId != null);
        if (selectedRecipeId != null) buf.writeResourceLocation(selectedRecipeId);
        buf.writeDouble(progressFrac);
    }

    @Override
    protected void readExtraFromBuf(FriendlyByteBuf buf) {
        selectedRecipeId = buf.readBoolean() ? buf.readResourceLocation() : null;
        progressFrac = buf.readDouble();
    }
}
