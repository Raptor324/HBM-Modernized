package com.hbm_m.block.entity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.inventory.menu.MachineGasCentrifugeMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.recipe.GasCentrifugeRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Gas Centrifuge BlockEntity.
 * Simplified machine with battery slot, input/output slots, and energy.
 */
public class MachineGasCentrifugeBlockEntity extends BaseMachineBlockEntity {

    private static final int SLOT_COUNT = 5;
    private static final int SLOT_BATTERY = 0;
    private static final int SLOT_INPUT_1 = 1;
    private static final int SLOT_INPUT_2 = 2;
    private static final int SLOT_OUTPUT_1 = 3;
    private static final int SLOT_OUTPUT_2 = 4;

    private static final long MAX_POWER = 100_000;

    /** Default durability of a Centristick before it is consumed (legacy 1.7.10 value). */
    private static final int CENTRISTICK_MAX_DURABILITY = 250;

    private boolean didProcess = false;

    private int progress = 0;
    private int maxProgress = 100;

    private float anim = 0.0F;
    private float prevAnim = 0.0F;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 2;
        }
    };

    public MachineGasCentrifugeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_CENTRIFUGE_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineGasCentrifugeBlockEntity entity) {
        entity.prevAnim = entity.anim;

        if (level.isClientSide) {
            if (entity.didProcess) {
                entity.anim += 0.05F;
            }
            if (entity.anim > (float) (Math.PI * 2.0)) {
                entity.anim -= (float) (Math.PI * 2.0);
            }
            return;
        }

        entity.ensureNetworkInitialized();
        entity.chargeFromBattery();

        if (level.getGameTime() % 10L == 0L) {
            entity.updateEnergyDelta(entity.getEnergyStored());
        }

        boolean dirty = false;

        GasCentrifugeRecipes.Recipe recipe = GasCentrifugeRecipes.getRecipe(entity.inventory.getStackInSlot(SLOT_INPUT_1));

        if (recipe != null && entity.hasUsableCentristick() && entity.hasRoomForOutputs(recipe)
                && entity.getEnergyStored() >= recipe.energyPerTick()) {

            entity.setEnergyStored(entity.getEnergyStored() - recipe.energyPerTick());
            entity.maxProgress = recipe.processDuration();
            entity.progress++;
            entity.didProcess = true;
            dirty = true;

            if (entity.progress >= entity.maxProgress) {
                entity.progress = 0;
                entity.finishCycle(recipe);
                dirty = true;
            }
        } else {
            if (entity.progress != 0) {
                entity.progress = 0;
                dirty = true;
            }
            if (entity.didProcess) {
                entity.didProcess = false;
                dirty = true;
            }
        }

        if (dirty) {
            entity.setChanged();
            entity.sendUpdateToClient();
        }
    }

    /** Checks whether the centristick in the catalyst slot still has durability remaining. */
    private boolean hasUsableCentristick() {
        ItemStack stick = inventory.getStackInSlot(SLOT_INPUT_2);
        if (stick.isEmpty() || !(stick.getItem() == ModItems.CENTRI_STICK.get())) {
            return false;
        }
        return getCentristickDurability(stick) > 0;
    }

    /** Reads the remaining durability of a Centristick from its NBT data. */
    private static int getCentristickDurability(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("CentriDurability")) {
            return CENTRISTICK_MAX_DURABILITY;
        }
        return tag.getInt("CentriDurability");
    }

    /** Damages the Centristick by the given amount, breaking (consuming) it once depleted. */
    private void damageCentristick(int amount) {
        ItemStack stick = inventory.getStackInSlot(SLOT_INPUT_2);
        if (stick.isEmpty()) return;

        int remaining = getCentristickDurability(stick) - amount;
        if (remaining <= 0) {
            inventory.setStackInSlot(SLOT_INPUT_2, ItemStack.EMPTY);
        } else {
            CompoundTag tag = stick.getOrCreateTag();
            tag.putInt("CentriDurability", remaining);
        }
    }

    /** Checks that both output slots have room for the recipe's results. */
    private boolean hasRoomForOutputs(GasCentrifugeRecipes.Recipe recipe) {
        return canAccept(SLOT_OUTPUT_1, recipe.enrichedOutput()) && canAccept(SLOT_OUTPUT_2, recipe.emptyCellOutput());
    }

    private boolean canAccept(int slot, ItemStack result) {
        ItemStack existing = inventory.getStackInSlot(slot);
        if (existing.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(existing, result)) return false;
        return existing.getCount() + result.getCount() <= existing.getMaxStackSize();
    }

    /** Completes a processing cycle: consumes the input cell and centristick durability, and produces outputs. */
    private void finishCycle(GasCentrifugeRecipes.Recipe recipe) {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT_1);
        if (input.isEmpty()) return;

        placeOutput(SLOT_OUTPUT_1, recipe.enrichedOutput());
        placeOutput(SLOT_OUTPUT_2, recipe.emptyCellOutput());

        input.shrink(1);
        damageCentristick(recipe.centristickDamagePerCycle());
    }

    private void placeOutput(int slot, ItemStack result) {
        if (result.isEmpty()) return;
        ItemStack existing = inventory.getStackInSlot(slot);
        if (existing.isEmpty()) {
            inventory.setStackInSlot(slot, result.copy());
        } else if (ItemStack.isSameItemSameTags(existing, result)) {
            existing.grow(result.getCount());
        }
    }

    private void chargeFromBattery() {
        ItemStack stack = inventory.getStackInSlot(SLOT_BATTERY);
        if (stack.isEmpty()) return;

        if (stack.getItem() instanceof ItemCreativeBattery) {
            setEnergyStored(getMaxEnergyStored());
            return;
        }

        stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).ifPresent(provider -> {
            long needed = getMaxEnergyStored() - getEnergyStored();
            if (needed <= 0) return;
            long extracted = provider.extractEnergy(Math.min(needed, getReceiveSpeed()), false);
            if (extracted > 0) {
                setEnergyStored(getEnergyStored() + extracted);
                setChanged();
            }
        });

        if (!stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).isPresent()) {
            stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(provider -> {
                long needed = getMaxEnergyStored() - getEnergyStored();
                if (needed <= 0) return;
                int extracted = provider.extractEnergy((int) Math.min(needed, getReceiveSpeed()), false);
                if (extracted > 0) {
                    setEnergyStored(getEnergyStored() + extracted);
                    setChanged();
                }
            });
        }
    }

    public void drops() {
        if (level == null) return;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
            }
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.gas_centrifuge");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) {
            return stack.getCapability(ForgeCapabilities.ENERGY).isPresent()
                || stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER).isPresent();
        }
        if (slot == SLOT_INPUT_1) {
            return GasCentrifugeRecipes.isValidInput(stack);
        }
        if (slot == SLOT_INPUT_2) {
            return stack.getItem() == ModItems.CENTRI_STICK.get();
        }
        if (slot == SLOT_OUTPUT_1 || slot == SLOT_OUTPUT_2) {
            return false;
        }
        return true;
    }

    @Override
    protected void setupFluidCapability() {
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineGasCentrifugeMenu(containerId, playerInventory, this, data);
    }

    public ContainerData getContainerData() {
        return data;
    }

    public boolean getDidProcess() {
        return didProcess;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public float getAnim(float partialTicks) {
        return prevAnim + (anim - prevAnim) * partialTicks;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("didProcess", didProcess);
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        tag.putFloat("anim", anim);
        tag.putFloat("prevAnim", prevAnim);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        didProcess = tag.getBoolean("didProcess");
        progress = tag.getInt("progress");
        maxProgress = tag.getInt("maxProgress");
        if (maxProgress <= 0) maxProgress = 100;
        anim = tag.getFloat("anim");
        prevAnim = tag.getFloat("prevAnim");
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return super.getCapability(cap, side);
    }
}
