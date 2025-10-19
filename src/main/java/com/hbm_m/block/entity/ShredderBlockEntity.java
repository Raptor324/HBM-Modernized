// ShredderBlockEntity.java
package com.hbm_m.block.entity;

import com.hbm_m.item.ModItems;
import com.hbm_m.menu.ShredderMenu;
import com.hbm_m.recipe.ShredderRecipe;
import com.hbm_m.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ShredderBlockEntity extends BlockEntity implements MenuProvider {

    private static final int INPUT_SLOTS = 9;
    private static final int BLADE_SLOTS = 2;
    private static final int OUTPUT_SLOTS = 18;
    private static final int TOTAL_SLOTS = INPUT_SLOTS + BLADE_SLOTS + OUTPUT_SLOTS;

    private static final int INPUT_START = 0;
    private static final int INPUT_END = 8;
    private static final int BLADE_START = 9;
    private static final int BLADE_END = 10;
    private static final int OUTPUT_START = 11;
    private static final int OUTPUT_END = 28;

    private final ItemStackHandler itemHandler = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot >= INPUT_START && slot <= INPUT_END) {
                return true;
            }
            if (slot >= BLADE_START && slot <= BLADE_END) {
                return stack.is(ModItems.BLADE_TEST.get()); // replace with your blade item
            }
            if (slot >= OUTPUT_START && slot <= OUTPUT_END) {
                return false;
            }
            return true;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if ((slot >= INPUT_START && slot <= BLADE_END) || (slot >= OUTPUT_START && slot <= OUTPUT_END)) {
                return super.extractItem(slot, amount, simulate);
            }
            return ItemStack.EMPTY;
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    // Energy
    private final EnergyStorage energyStorage = new EnergyStorage(100000, 1000, 0) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }
    };

    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();
    private static final int ENERGY_PER_TICK = 200;

    private int progressTime = 0;
    private static final int MAX_PROGRESS = 100; // processing time in ticks

    public ShredderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHREDDER.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Shredder");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ShredderMenu(containerId, playerInventory, this);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyEnergyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyEnergyHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("progress", progressTime);
        tag.putInt("energy", energyStorage.getEnergyStored());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        progressTime = tag.getInt("progress");
        if (tag.contains("energy")) {
            energyStorage.receiveEnergy(tag.getInt("energy"), false);
        }
    }

    // Add these methods for client-side synchronization
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag); // Saves inventory, progress, and energy
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        load(tag); // Loads inventory, progress, and energy
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ShredderBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        boolean changed = false;

        if (!blockEntity.hasTwoBlades()) {
            if (blockEntity.progressTime != 0) {
                blockEntity.progressTime = 0;
                changed = true;
            }
        }

        if (blockEntity.hasItemsToProcess() && blockEntity.hasEnoughEnergy()) {
            int energyBefore = blockEntity.energyStorage.getEnergyStored();
            blockEntity.energyStorage.extractEnergy(ENERGY_PER_TICK, false);
            if (blockEntity.energyStorage.getEnergyStored() != energyBefore) {
                changed = true;
            }

            blockEntity.progressTime++;
            changed = true;

            if (blockEntity.progressTime >= MAX_PROGRESS) {
                blockEntity.processItems();
                blockEntity.progressTime = 0;
                changed = true;
            }
        } else {
            if (blockEntity.progressTime != 0) {
                blockEntity.progressTime = 0;
                changed = true;
            }
        }

        if (changed) {
            setChanged(level, pos, state);
            // setChanged() internally calls level.sendBlockUpdated() for client updates
        }
    }

    private boolean hasTwoBlades() {
        int bladeCount = 0;
        for (int i = BLADE_START; i <= BLADE_END; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (stack.is(ModItems.BLADE_TEST.get())) {
                    bladeCount++;
                }
            }
        }
        return bladeCount >= 2;
    }

    private boolean hasItemsToProcess() {
        for (int i = INPUT_START; i <= INPUT_END; i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasEnoughEnergy() {
        return energyStorage.getEnergyStored() >= ENERGY_PER_TICK;
    }

    private void processItems() {
        for (int i = INPUT_START; i <= INPUT_END; i++) {
            ItemStack inputStack = itemHandler.getStackInSlot(i);
            if (!inputStack.isEmpty()) {
                ItemStack result = getRecipeResult(inputStack);
                if (canInsertItemIntoOutputSlots(result)) {
                    insertItemIntoOutputSlots(result);
                    itemHandler.extractItem(i, 1, false);
                }
            }
        }
        // You can add sounds or effects here if needed
    }

    private ItemStack getRecipeResult(ItemStack input) {
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, input);
        Optional<ShredderRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(ShredderRecipe.Type.INSTANCE, container, level);
        if (recipe.isPresent()) {
            return recipe.get().getResultItem(level.registryAccess()).copy();
        }
        return new ItemStack(ModItems.SCRAP.get(), 1);
    }

    private boolean canInsertItemIntoOutputSlots(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        for (int i = OUTPUT_START; i <= OUTPUT_END; i++) {
            ItemStack slotStack = itemHandler.getStackInSlot(i);
            if (slotStack.isEmpty()) {
                return true;
            }
            if (ItemStack.isSameItemSameTags(slotStack, result) &&
                    slotStack.getCount() + result.getCount() <= slotStack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private void insertItemIntoOutputSlots(ItemStack result) {
        for (int i = OUTPUT_START; i <= OUTPUT_END; i++) {
            ItemStack slotStack = itemHandler.getStackInSlot(i);
            if (slotStack.isEmpty()) {
                itemHandler.setStackInSlot(i, result.copy());
                return;
            }
            if (ItemStack.isSameItemSameTags(slotStack, result) &&
                    slotStack.getCount() + result.getCount() <= slotStack.getMaxStackSize()) {
                slotStack.grow(result.getCount());
                return;
            }
        }
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public int getProgress() {
        return progressTime;
    }

    public int getMaxProgress() {
        return MAX_PROGRESS;
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public int getEnergy() {
        return energyStorage.getEnergyStored();
    }

    public int getMaxEnergy() {
        return energyStorage.getMaxEnergyStored();
    }
}