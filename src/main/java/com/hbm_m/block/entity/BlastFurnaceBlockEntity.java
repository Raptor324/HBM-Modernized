package com.hbm_m.block.entity;

// Блок-энтити для Плавильной Печи, которая переплавляет два входных предмета в один выходной с использованием топлива.
import com.hbm_m.block.BlastFurnaceBlock;
import com.hbm_m.recipe.BlastFurnaceRecipe;
import com.hbm_m.menu.BlastFurnaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class BlastFurnaceBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if(!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    private static final int FUEL_SLOT = 0;
    private static final int INPUT_SLOT_1 = 1;
    private static final int INPUT_SLOT_2 = 2;
    private static final int OUTPUT_SLOT = 3;

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 100;
    private int fuelLevel = 0;
    private int maxFuelLevel = 64;

    public BlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLAST_FURNACE_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> BlastFurnaceBlockEntity.this.progress;
                    case 1 -> BlastFurnaceBlockEntity.this.maxProgress;
                    case 2 -> BlastFurnaceBlockEntity.this.fuelLevel;
                    case 3 -> BlastFurnaceBlockEntity.this.maxFuelLevel;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> BlastFurnaceBlockEntity.this.progress = value;
                    case 1 -> BlastFurnaceBlockEntity.this.maxProgress = value;
                    case 2 -> BlastFurnaceBlockEntity.this.fuelLevel = value;
                    case 3 -> BlastFurnaceBlockEntity.this.maxFuelLevel = value;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for(int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm_m.blast_furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BlastFurnaceMenu(containerId, playerInventory, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("blast_furnace.progress", progress);
        tag.putInt("blast_furnace.max_progress", maxProgress);
        tag.putInt("blast_furnace.fuel_level", fuelLevel);
        tag.putInt("blast_furnace.max_fuel_level", maxFuelLevel);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        progress = tag.getInt("blast_furnace.progress");
        maxProgress = tag.getInt("blast_furnace.max_progress");
        fuelLevel = tag.getInt("blast_furnace.fuel_level");
        maxFuelLevel = tag.getInt("blast_furnace.max_fuel_level");
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        boolean wasBurning = isBurning();
        boolean dirty = false;

        if (!isFuelFull() && canAddFuel()) {
            addFuel();
            dirty = true;
        }

        boolean hasRecipeResult = hasRecipe();
        boolean isBurningResult = isBurning();

        if (hasRecipeResult && isBurningResult) {
            increaseCraftingProgress();
            dirty = true;

            if (hasCraftingFinished()) {
                craftItem();
                consumeFuel();
                resetProgress();
                dirty = true;
            }
        } else {
            if (this.progress > 0) {
                resetProgress();
                dirty = true;
            }
        }

        if (wasBurning != isBurning()) {
            level.setBlock(pos, state.setValue(BlastFurnaceBlock.LIT, isBurning()), 3);
            dirty = true;
        }

        if (dirty) {
            setChanged();
        }
    }

    private boolean isBurning() {
        return this.fuelLevel > 0;
    }

    private boolean isFuelFull() {
        return this.fuelLevel >= this.maxFuelLevel;
    }

    private boolean canAddFuel() {
        ItemStack fuelStack = this.itemHandler.getStackInSlot(FUEL_SLOT);
        return fuelStack.getItem() == Items.LAVA_BUCKET && fuelStack.getCount() > 0;
    }

    private void addFuel() {
        ItemStack fuelStack = this.itemHandler.getStackInSlot(FUEL_SLOT);
        if (fuelStack.getItem() == Items.LAVA_BUCKET) {
            this.fuelLevel = Math.min(this.fuelLevel + 64, this.maxFuelLevel);
            fuelStack.shrink(1);
            this.itemHandler.setStackInSlot(FUEL_SLOT, new ItemStack(Items.BUCKET, 1));
        }
    }

    private void consumeFuel() {
        if (this.fuelLevel > 0) {
            this.fuelLevel--;
        }
    }

    private void resetProgress() {
        this.progress = 0;
    }

    private void craftItem() {
        Optional<BlastFurnaceRecipe> recipe = getCurrentRecipe();
        if (recipe.isPresent()) {
            ItemStack output = recipe.get().getResultItem(getLevel().registryAccess());

            this.itemHandler.extractItem(INPUT_SLOT_1, 1, false);
            this.itemHandler.extractItem(INPUT_SLOT_2, 1, false);

            this.itemHandler.setStackInSlot(OUTPUT_SLOT, new ItemStack(output.getItem(),
                    this.itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() + output.getCount()));
        }
    }

    private boolean hasCraftingFinished() {
        return this.progress >= this.maxProgress;
    }

    private void increaseCraftingProgress() {
        this.progress++;
    }

    private boolean hasRecipe() {
        Optional<BlastFurnaceRecipe> recipe = getCurrentRecipe();

        if(recipe.isEmpty()) {
            return false;
        }
        ItemStack result = recipe.get().getResultItem(getLevel().registryAccess());

        return canInsertAmountIntoOutputSlot(result.getCount()) && canInsertItemIntoOutputSlot(result.getItem());
    }

    private Optional<BlastFurnaceRecipe> getCurrentRecipe() {
        SimpleContainer inventory = new SimpleContainer(this.itemHandler.getSlots());
        for(int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, this.itemHandler.getStackInSlot(i));
        }

        return this.level.getRecipeManager().getRecipeFor(BlastFurnaceRecipe.Type.INSTANCE, inventory, level);
    }

    private boolean canInsertItemIntoOutputSlot(net.minecraft.world.item.Item item) {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty() ||
                this.itemHandler.getStackInSlot(OUTPUT_SLOT).is(item);
    }

    private boolean canInsertAmountIntoOutputSlot(int count) {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() + count <=
                this.itemHandler.getStackInSlot(OUTPUT_SLOT).getMaxStackSize();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
}