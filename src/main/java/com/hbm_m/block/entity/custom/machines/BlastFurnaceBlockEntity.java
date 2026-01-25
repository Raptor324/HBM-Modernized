package com.hbm_m.block.entity.custom.machines;

// Блок-энтити для Плавильной Печи, которая переплавляет два входных предмета в один выходной с использованием топлива.
import com.hbm_m.block.custom.machines.BlastFurnaceBlock;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.item.ModItems;
import com.hbm_m.menu.BlastFurnaceMenu;
import com.hbm_m.recipe.BlastFurnaceRecipe;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class BlastFurnaceBlockEntity extends BlockEntity implements MenuProvider {

    private static final int FUEL_SLOT = 0;
    private static final int INPUT_SLOT_TOP = 1;
    private static final int INPUT_SLOT_BOTTOM = 2;
    private static final int OUTPUT_SLOT = 3;
    private static final int PROCESS_TIME = 400;
    private static final int MAX_FUEL = 12_800;

    private final ItemStackHandler itemHandler = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == OUTPUT_SLOT) {
                return false;
            }
            if (slot == FUEL_SLOT) {
                return isFuel(stack);
            }
            return true;
        }
    };

    private final Map<Direction, LazyOptional<IItemHandler>> sidedItemHandlers = new EnumMap<>(Direction.class);
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    private final ContainerData data;
    private int progress;
    private int fuel;
    private int sideUpper = Direction.UP.get3DDataValue();
    private int sideLower = Direction.UP.get3DDataValue();
    private int sideFuel = Direction.UP.get3DDataValue();

    private static final int DATA_COUNT = 7;
    private static final int DATA_INDEX_PROGRESS = 0;
    private static final int DATA_INDEX_MAX_PROGRESS = 1;
    private static final int DATA_INDEX_FUEL = 2;
    private static final int DATA_INDEX_MAX_FUEL = 3;
    private static final int DATA_INDEX_SIDE_UPPER = 4;
    private static final int DATA_INDEX_SIDE_LOWER = 5;
    private static final int DATA_INDEX_SIDE_FUEL = 6;

    public BlastFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLAST_FURNACE_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_INDEX_PROGRESS -> BlastFurnaceBlockEntity.this.progress;
                    case DATA_INDEX_MAX_PROGRESS -> PROCESS_TIME;
                    case DATA_INDEX_FUEL -> BlastFurnaceBlockEntity.this.fuel;
                    case DATA_INDEX_MAX_FUEL -> MAX_FUEL;
                    case DATA_INDEX_SIDE_UPPER -> BlastFurnaceBlockEntity.this.sideUpper;
                    case DATA_INDEX_SIDE_LOWER -> BlastFurnaceBlockEntity.this.sideLower;
                    case DATA_INDEX_SIDE_FUEL -> BlastFurnaceBlockEntity.this.sideFuel;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case DATA_INDEX_PROGRESS -> BlastFurnaceBlockEntity.this.progress = value;
                    case DATA_INDEX_FUEL -> BlastFurnaceBlockEntity.this.fuel = value;
                    case DATA_INDEX_SIDE_UPPER -> BlastFurnaceBlockEntity.this.sideUpper = value;
                    case DATA_INDEX_SIDE_LOWER -> BlastFurnaceBlockEntity.this.sideLower = value;
                    case DATA_INDEX_SIDE_FUEL -> BlastFurnaceBlockEntity.this.sideFuel = value;
                    default -> { }
                }
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (side == null) {
                return lazyItemHandler.cast();
            }
            return sidedItemHandlers.getOrDefault(side, lazyItemHandler).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        sidedItemHandlers.clear();
        for (Direction direction : Direction.values()) {
            sidedItemHandlers.put(direction, LazyOptional.of(() -> new DirectionalItemHandler(direction)));
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        sidedItemHandlers.values().forEach(LazyOptional::invalidate);
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
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
        tag.putInt("blast_furnace.fuel", fuel);
        tag.putInt("blast_furnace.side_upper", sideUpper);
        tag.putInt("blast_furnace.side_lower", sideLower);
        tag.putInt("blast_furnace.side_fuel", sideFuel);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        progress = tag.getInt("blast_furnace.progress");
        fuel = tag.getInt("blast_furnace.fuel");
        sideUpper = tag.getInt("blast_furnace.side_upper");
        sideLower = tag.getInt("blast_furnace.side_lower");
        sideFuel = tag.getInt("blast_furnace.side_fuel");
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        boolean dirty = false;
        boolean wasBurning = isBurning();

        if (fuel < MAX_FUEL && tryConsumeFuelItem()) {
            dirty = true;
        }

        boolean canProcess = hasRecipe() && fuel > 0;
        if (canProcess) {
            fuel = Math.max(0, fuel - 1);
            progress += getProgressPerTick();
            if (progress >= PROCESS_TIME) {
                craftItem();
                progress -= PROCESS_TIME;
            }
            dirty = true;
        } else if (progress != 0) {
            progress = 0;
            dirty = true;
        }

        if (wasBurning != isBurning()) {
            level.setBlock(pos, state.setValue(BlastFurnaceBlock.LIT, isBurning()), 3);
            dirty = true;
        }

        if (dirty) {
            setChanged();
        }
    }

    private int getProgressPerTick() {
        return hasExtension() ? 3 : 1;
    }

    private boolean hasExtension() {
        if (level == null) {
            return false;
        }
        BlockState above = level.getBlockState(worldPosition.above());
        return above.is(ModBlocks.BLAST_FURNACE_EXTENSION.get());
    }

    private boolean tryConsumeFuelItem() {
        ItemStack stack = itemHandler.getStackInSlot(FUEL_SLOT);
        if (stack.isEmpty()) {
            return false;
        }
        int value = getFuelValue(stack);
        if (value <= 0) {
            return false;
        }

        int newFuelLevel = Math.min(MAX_FUEL, fuel + value);
        if (newFuelLevel == fuel) {
            return false;
        }
        fuel = newFuelLevel;

        ItemStack remainder = stack.getCraftingRemainingItem();
        stack.shrink(1);
        if (stack.isEmpty()) {
            itemHandler.setStackInSlot(FUEL_SLOT, remainder);
        }
        return true;
    }

    private boolean hasRecipe() {
        Optional<BlastFurnaceRecipe> recipe = getCurrentRecipe();
        if (recipe.isEmpty() || level == null) {
            return false;
        }
        ItemStack result = recipe.get().getResultItem(level.registryAccess());
        return !result.isEmpty() && canAcceptResult(result);
    }

    private boolean canAcceptResult(ItemStack result) {
        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (currentOutput.isEmpty()) {
            return true;
        }
        if (!ItemHandlerHelper.canItemStacksStack(currentOutput, result)) {
            return false;
        }
        return currentOutput.getCount() + result.getCount() <= currentOutput.getMaxStackSize();
    }

    private Optional<BlastFurnaceRecipe> getCurrentRecipe() {
        if (level == null) {
            return Optional.empty();
        }
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        return level.getRecipeManager().getRecipeFor(BlastFurnaceRecipe.Type.INSTANCE, inventory, level);
    }

    private void craftItem() {
        Optional<BlastFurnaceRecipe> recipe = getCurrentRecipe();
        if (recipe.isEmpty() || level == null) {
            return;
        }
        ItemStack result = recipe.get().getResultItem(level.registryAccess()).copy();

        itemHandler.extractItem(INPUT_SLOT_TOP, 1, false);
        itemHandler.extractItem(INPUT_SLOT_BOTTOM, 1, false);

        ItemStack output = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, result);
        } else {
            output.grow(result.getCount());
            itemHandler.setStackInSlot(OUTPUT_SLOT, output);
        }
    }

    private boolean isBurning() {
        return fuel > 0;
    }

    public static boolean isFuel(ItemStack stack) {
        return getFuelValue(stack) > 0;
    }

    private static int getFuelValue(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.LAVA_BUCKET) {
            return 12_800;
        }
        if (item == Items.COAL || item == Items.CHARCOAL) {
            return 200;
        }
        if (item == Items.COAL_BLOCK) {
            return 2_000;
        }
        if (item == Items.BLAZE_ROD) {
            return 1_000;
        }
        if (item == Items.BLAZE_POWDER) {
            return 300;
        }
        if (item == ModItems.LIGNITE.get()) {
            return 150;
        }
        return 0;
    }

    public void cycleSide(int slot) {
        switch (slot) {
            case 0 -> sideUpper = nextDirection(sideUpper);
            case 1 -> sideLower = nextDirection(sideLower);
            case 2 -> sideFuel = nextDirection(sideFuel);
            default -> {
                return;
            }
        }
        markSidesChanged();
    }

    public Direction getConfiguredDirection(int slot) {
        return Direction.from3DDataValue(switch (slot) {
            case 0 -> sideUpper;
            case 1 -> sideLower;
            case 2 -> sideFuel;
            default -> Direction.UP.get3DDataValue();
        });
    }

    private static int nextDirection(int current) {
        int next = (current + 1) % Direction.values().length;
        return next < 0 ? 0 : next;
    }

    private void markSidesChanged() {
        setChanged();
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    private boolean canInsertFromDirection(int slot, Direction direction) {
        if (direction == null) {
            return true;
        }
        return switch (slot) {
            case INPUT_SLOT_TOP -> matches(direction, sideUpper);
            case INPUT_SLOT_BOTTOM -> matches(direction, sideLower);
            case FUEL_SLOT -> matches(direction, sideFuel);
            default -> false;
        };
    }

    private static boolean matches(Direction direction, int stored) {
        return direction.get3DDataValue() == stored;
    }

    private boolean canExtractFromDirection(int slot) {
        return slot == OUTPUT_SLOT;
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

    private class DirectionalItemHandler implements IItemHandler {
        private final Direction direction;

        private DirectionalItemHandler(Direction direction) {
            this.direction = direction;
        }

        @Override
        public int getSlots() {
            return itemHandler.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!canInsertFromDirection(slot, direction) || !itemHandler.isItemValid(slot, stack)) {
                return stack;
            }
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!canExtractFromDirection(slot)) {
                return ItemStack.EMPTY;
            }
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return itemHandler.isItemValid(slot, stack) && canInsertFromDirection(slot, direction);
        }
    }
}
