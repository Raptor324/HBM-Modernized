package com.hbm_m.block.entity;

import com.hbm_m.menu.ShredderMenu;
import com.hbm_m.recipe.ShredderRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ShredderBlockEntity extends BlockEntity implements MenuProvider {

    // Индексы слотов
    private static final int INPUT_SLOTS = 9;
    private static final int BLADE_SLOTS = 2;
    private static final int OUTPUT_SLOTS = 18;
    private static final int TOTAL_SLOTS = INPUT_SLOTS + BLADE_SLOTS + OUTPUT_SLOTS;

    // Диапазоны слотов
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
            // Входные слоты (0-8) - любые предметы
            if (slot >= INPUT_START && slot <= INPUT_END) {
                return true;
            }
            // Слоты для лезвий (9-10) - только BLADE_TEST
            if (slot >= BLADE_START && slot <= BLADE_END) {
                // Проверка на BLADE_TEST
                // return stack.is(ModItems.BLADE_TEST.get());
                return true; // Временно разрешаем все
            }
            // Выходные слоты (11-28) - ничего нельзя положить
            if (slot >= OUTPUT_START && slot <= OUTPUT_END) {
                return false;
            }
            return true;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            // Из входных слотов и слотов лезвий можно извлекать
            if (slot >= INPUT_START && slot <= BLADE_END) {
                return super.extractItem(slot, amount, simulate);
            }
            // Из выходных слотов можно извлекать
            if (slot >= OUTPUT_START && slot <= OUTPUT_END) {
                return super.extractItem(slot, amount, simulate);
            }
            return ItemStack.EMPTY;
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    private int progressTime = 0;
    private static final int MAX_PROGRESS = 100; // Время обработки в тиках

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

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("progress", progressTime);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        progressTime = tag.getInt("progress");
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

        // Проверяем наличие двух лезвий
        if (!blockEntity.hasTwoBlades()) {
            blockEntity.progressTime = 0;
            return;
        }

        // Проверяем, есть ли предметы для обработки
        if (blockEntity.hasItemsToProcess()) {
            blockEntity.progressTime++;

            if (blockEntity.progressTime >= MAX_PROGRESS) {
                blockEntity.processItems();
                blockEntity.progressTime = 0;
            }

            setChanged(level, pos, state);
        } else {
            blockEntity.progressTime = 0;
        }
    }

    private boolean hasTwoBlades() {
        int bladeCount = 0;
        for (int i = BLADE_START; i <= BLADE_END; i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                // Проверка на BLADE_TEST
                // if (stack.is(ModItems.BLADE_TEST.get())) {
                //     bladeCount++;
                // }
                bladeCount++; // Временно считаем все предметы
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

    private void processItems() {
        for (int i = INPUT_START; i <= INPUT_END; i++) {
            ItemStack inputStack = itemHandler.getStackInSlot(i);
            if (!inputStack.isEmpty()) {
                // Получаем результат для этого предмета
                ItemStack result = getRecipeResult(inputStack);

                // Пытаемся поместить результат в выходные слоты
                if (canInsertItemIntoOutputSlots(result)) {
                    insertItemIntoOutputSlots(result);
                    itemHandler.extractItem(i, 1, false);
                }
            }
        }
    }

    private ItemStack getRecipeResult(ItemStack input) {
        // Проверяем рецепты
        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, input);

        Optional<ShredderRecipe> recipe = level.getRecipeManager()
                .getRecipeFor(ShredderRecipe.Type.INSTANCE, container, level);

        if (recipe.isPresent()) {
            return recipe.get().getResultItem(level.registryAccess()).copy();
        }

        // Если рецепт не найден, возвращаем металлолом
        // return new ItemStack(ModItems.SCRAP.get(), 1);
        return ItemStack.EMPTY; // Временно - замените на металлолом
    }

    private boolean canInsertItemIntoOutputSlots(ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }

        for (int i = OUTPUT_START; i <= OUTPUT_END; i++) {
            ItemStack slotStack = itemHandler.getStackInSlot(i);

            // Пустой слот
            if (slotStack.isEmpty()) {
                return true;
            }

            // Слот с таким же предметом и есть место
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

            // Пустой слот
            if (slotStack.isEmpty()) {
                itemHandler.setStackInSlot(i, result.copy());
                return;
            }

            // Слот с таким же предметом
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
}