package com.hbm_m.block.entity;

import com.hbm_m.block.AnvilBlock;
import com.hbm_m.block.AnvilTier;
import com.hbm_m.menu.AnvilMenu;
import com.hbm_m.recipe.AnvilRecipe;
import com.hbm_m.recipe.AnvilRecipeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class AnvilBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot != 2; // Слот 2 только для вывода
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    
    @Nullable
    private ResourceLocation selectedRecipeId;

    public AnvilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANVIL_BE.get(), pos, state);
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
        if (selectedRecipeId != null) {
            tag.putString("SelectedRecipe", selectedRecipeId.toString());
        }
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        selectedRecipeId = tag.contains("SelectedRecipe") ? 
            ResourceLocation.parse(tag.getString("SelectedRecipe")) : null;
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
        return Component.translatable("container.hbm_m.anvil_block");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AnvilMenu(containerId, playerInventory, this);
    }

    public void openGui(ServerPlayer player) {
        NetworkHooks.openScreen(player, this, this.worldPosition);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public AnvilTier getTier() {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof AnvilBlock block) {
            return block.getTier();
        }
        return AnvilTier.IRON;
    }

    public Optional<ResourceLocation> getSelectedRecipeId() {
        return Optional.ofNullable(selectedRecipeId);
    }

    public void setSelectedRecipeId(@Nullable ResourceLocation recipeId) {
        if ((recipeId == null && this.selectedRecipeId != null) ||
            (recipeId != null && !recipeId.equals(this.selectedRecipeId))) {
            this.selectedRecipeId = recipeId;
            setChangedAndNotify();
            updateCrafting(); // Обновляем выходной слот
        }
    }

    /**
     * Обновляет выходной слот на основе текущих входов и выбранного рецепта
     */
    public void updateCrafting() {
        Level level = getLevel();
        if (level == null) {
            return;
        }

        ItemStack slotA = itemHandler.getStackInSlot(0);
        ItemStack slotB = itemHandler.getStackInSlot(1);

        Optional<AnvilRecipe> recipeOpt = resolveCraftableRecipe(level, slotA, slotB);

        ItemStack result = recipeOpt
                .map(recipe -> recipe.getResultItem(level.registryAccess()).copy())
                .orElse(ItemStack.EMPTY);

        itemHandler.setStackInSlot(2, result);
        setChangedAndNotify();
    }

    /**
     * Проверяет, можно ли выполнить рецепт с текущими материалами
     */
    private boolean canCraftRecipe(AnvilRecipe recipe, ItemStack slotA, ItemStack slotB) {
        if (!hasMatchingInputs(slotA, slotB, recipe)) {
            return false;
        }
        return recipe.canCraftOn(getTier());
    }

    /**
     * Проверяет соответствие стека рецепту
     */
    private boolean matchesInput(ItemStack actual, ItemStack required) {
        if (required.isEmpty()) {
            return actual.isEmpty();
        }
        
        return ItemStack.isSameItemSameTags(actual, required) && 
               actual.getCount() >= required.getCount();
    }

    private boolean hasMatchingInputs(ItemStack slotA, ItemStack slotB, AnvilRecipe recipe) {
        return resolveOrientation(slotA, slotB, recipe) != InputOrientation.NONE;
    }

    private InputOrientation resolveOrientation(ItemStack slotA, ItemStack slotB, AnvilRecipe recipe) {
        if (matchesPair(slotA, slotB, recipe.getInputA(), recipe.getInputB())) {
            return InputOrientation.NORMAL;
        }
        if (matchesPair(slotA, slotB, recipe.getInputB(), recipe.getInputA())) {
            return InputOrientation.SWAPPED;
        }
        return InputOrientation.NONE;
    }

    private InputOrientation chooseBestOrientation(ItemStack slotA, ItemStack slotB, AnvilRecipe recipe) {
        InputOrientation resolved = resolveOrientation(slotA, slotB, recipe);
        if (resolved != InputOrientation.NONE) {
            return resolved;
        }

        int normalConflicts = conflictValue(slotA, recipe.getInputA()) + conflictValue(slotB, recipe.getInputB());
        int swappedConflicts = conflictValue(slotA, recipe.getInputB()) + conflictValue(slotB, recipe.getInputA());

        return swappedConflicts < normalConflicts ? InputOrientation.SWAPPED : InputOrientation.NORMAL;
    }

    private int conflictValue(ItemStack actual, ItemStack required) {
        if (required.isEmpty() || actual.isEmpty()) {
            return 0;
        }
        return ItemStack.isSameItemSameTags(actual, required) ? 0 : 1;
    }

    private boolean matchesPair(ItemStack first, ItemStack second, ItemStack requiredFirst, ItemStack requiredSecond) {
        return matchesInput(first, requiredFirst) && matchesInput(second, requiredSecond);
    }

    private int computeMaxCrafts(ItemStack slotA, ItemStack slotB, AnvilRecipe recipe, InputOrientation orientation) {
        ItemStack firstRequired = orientation == InputOrientation.NORMAL ? recipe.getInputA() : recipe.getInputB();
        ItemStack secondRequired = orientation == InputOrientation.NORMAL ? recipe.getInputB() : recipe.getInputA();
        return Math.min(
                computeMaxCrafts(slotA, firstRequired),
                computeMaxCrafts(slotB, secondRequired)
        );
    }

    private void shrinkSlotInput(ItemStack slotA, ItemStack slotB, AnvilRecipe recipe, InputOrientation orientation, int craftCount) {
        ItemStack firstRequired = orientation == InputOrientation.NORMAL ? recipe.getInputA() : recipe.getInputB();
        ItemStack secondRequired = orientation == InputOrientation.NORMAL ? recipe.getInputB() : recipe.getInputA();
        slotA.shrink(firstRequired.getCount() * craftCount);
        slotB.shrink(secondRequired.getCount() * craftCount);
    }

    private boolean populateInputsForOrientation(ServerPlayer player, AnvilRecipe recipe, InputOrientation orientation) {
        boolean changed = false;
        changed |= fillSlotFromPlayer(player, 0, requiredForSlot(recipe, orientation, 0));
        changed |= fillSlotFromPlayer(player, 1, requiredForSlot(recipe, orientation, 1));
        return changed;
    }

    private ItemStack requiredForSlot(AnvilRecipe recipe, InputOrientation orientation, int slotIndex) {
        boolean normal = orientation == InputOrientation.NORMAL;
        if (slotIndex == 0) {
            return normal ? recipe.getInputA() : recipe.getInputB();
        }
        return normal ? recipe.getInputB() : recipe.getInputA();
    }

    private boolean fillSlotFromPlayer(ServerPlayer player, int slotIndex, ItemStack required) {
        if (required.isEmpty()) {
            ItemStack currentStack = itemHandler.getStackInSlot(slotIndex);
            if (currentStack.isEmpty()) {
                return false;
            }
            ItemStack toReturn = currentStack.copy();
            itemHandler.setStackInSlot(slotIndex, ItemStack.EMPTY);
            if (!player.getInventory().add(toReturn)) {
                player.drop(toReturn, false);
            }
            return true;
        }

        ItemStack current = itemHandler.getStackInSlot(slotIndex);
        boolean changed = false;

        if (!current.isEmpty() && !ItemStack.isSameItemSameTags(current, required)) {
            ItemStack toReturn = current.copy();
            itemHandler.setStackInSlot(slotIndex, ItemStack.EMPTY);
            if (!player.getInventory().add(toReturn)) {
                player.drop(toReturn, false);
            }
            changed = true;
        }

        current = itemHandler.getStackInSlot(slotIndex);
        int slotLimit = Math.min(itemHandler.getSlotLimit(slotIndex), required.getMaxStackSize());
        if (slotLimit <= 0) {
            return changed;
        }

        int have = current.isEmpty() ? 0 : current.getCount();
        if (have >= slotLimit) {
            return changed;
        }

        int missing = slotLimit - have;
        int pulled = removeItemFromInventory(player, required, missing);
        if (pulled <= 0) {
            return changed;
        }

        if (current.isEmpty()) {
            ItemStack insert = required.copy();
            insert.setCount(pulled);
            itemHandler.setStackInSlot(slotIndex, insert);
        } else {
            current.grow(pulled);
            itemHandler.setStackInSlot(slotIndex, current);
        }
        return true;
    }

    /**
     * Расходует материалы после крафта
     * 
     * @param player игрок, выполняющий крафт
     * @param craftMax если true, крафтит максимальное количество
     * @return true, если материалы были успешно расходованы
     */
    public boolean consumeMaterials(Player player, boolean craftMax) {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return false;
        }

        ItemStack slotA = itemHandler.getStackInSlot(0);
        ItemStack slotB = itemHandler.getStackInSlot(1);

        Optional<AnvilRecipe> recipeOpt = resolveCraftableRecipe(level, slotA, slotB);
        if (recipeOpt.isEmpty()) {
            return false;
        }

        AnvilRecipe recipe = recipeOpt.get();

        InputOrientation orientation = resolveOrientation(slotA, slotB, recipe);
        if (orientation == InputOrientation.NONE) {
            return false;
        }

        // Вычисляем количество крафтов
        int craftCount = craftMax ? 64 : 1;
        int maxCrafts = computeMaxCrafts(slotA, slotB, recipe, orientation);

        for (ItemStack required : recipe.getRequiredItems()) {
            if (required.isEmpty()) {
                continue;
            }
            int available = countItemInInventory(player, required);
            maxCrafts = Math.min(maxCrafts, available / required.getCount());
        }

        craftCount = Math.min(craftCount, maxCrafts);
        if (craftCount <= 0) {
            return false;
        }

        // Расходуем дополнительные материалы из инвентаря игрока
        for (ItemStack required : recipe.getRequiredItems()) {
            if (required.isEmpty()) {
                continue;
            }
            removeItemFromInventory(player, required, required.getCount() * craftCount);
        }

        // Расходуем материалы из слотов наковальни
        shrinkSlotInput(slotA, slotB, recipe, orientation, craftCount);

        player.getInventory().setChanged();
        setChangedAndNotify();
        return true;
    }

    /**
     * Выполняет крафт и помещает результат в инвентарь игрока
     */
    public boolean craft(Player player, boolean craftMax) {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return false;
        }

        ItemStack slotA = itemHandler.getStackInSlot(0);
        ItemStack slotB = itemHandler.getStackInSlot(1);

        Optional<AnvilRecipe> recipeOpt = resolveCraftableRecipe(level, slotA, slotB);
        if (recipeOpt.isEmpty()) {
            return false;
        }

        AnvilRecipe recipe = recipeOpt.get();

        InputOrientation orientation = resolveOrientation(slotA, slotB, recipe);
        if (orientation == InputOrientation.NONE) {
            return false;
        }

        // Вычисляем количество крафтов
        int craftCount = craftMax ? 64 : 1;
        int maxCrafts = computeMaxCrafts(slotA, slotB, recipe, orientation);

        for (ItemStack required : recipe.getRequiredItems()) {
            if (required.isEmpty()) {
                continue;
            }
            int available = countItemInInventory(player, required);
            maxCrafts = Math.min(maxCrafts, available / required.getCount());
        }

        craftCount = Math.min(craftCount, maxCrafts);
        if (craftCount <= 0) {
            return false;
        }

        // Расходуем материалы
        for (ItemStack required : recipe.getRequiredItems()) {
            if (required.isEmpty()) {
                continue;
            }
            removeItemFromInventory(player, required, required.getCount() * craftCount);
        }

        shrinkSlotInput(slotA, slotB, recipe, orientation, craftCount);

        // Создаём результат с учетом шанса
        ItemStack result = recipe.getResultItem(level.registryAccess()).copy();
        int successfulCrafts = 0;
        
        for (int i = 0; i < craftCount; i++) {
            if (level.random.nextFloat() < recipe.getOutputChance()) {
                successfulCrafts++;
            }
        }

        if (successfulCrafts > 0) {
            result.setCount(result.getCount() * successfulCrafts);
            
            // Добавляем результат в инвентарь или выбрасываем
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
        }

        player.getInventory().setChanged();
        setChangedAndNotify();
        updateCrafting();
        return true;
    }

    public void populateInputsFromPlayer(ServerPlayer player, AnvilRecipe recipe) {
        if (player == null || recipe == null) {
            return;
        }
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        ItemStack slotA = itemHandler.getStackInSlot(0);
        ItemStack slotB = itemHandler.getStackInSlot(1);
        InputOrientation orientation = chooseBestOrientation(slotA, slotB, recipe);

        boolean changed = populateInputsForOrientation(player, recipe, orientation);
        if (!changed) {
            InputOrientation fallback = orientation == InputOrientation.NORMAL ? InputOrientation.SWAPPED : InputOrientation.NORMAL;
            changed = populateInputsForOrientation(player, recipe, fallback);
        }

        if (changed) {
            player.getInventory().setChanged();
            setChangedAndNotify();
            updateCrafting();
        }
    }

    private Optional<AnvilRecipe> resolveCraftableRecipe(Level level, ItemStack slotA, ItemStack slotB) {
        Optional<AnvilRecipe> fallback = AnvilRecipeManager
                .findRecipe(level, slotA, slotB, getTier())
                .filter(recipe -> canCraftRecipe(recipe, slotA, slotB));

        if (fallback.isEmpty()) {
            return Optional.empty();
        }

        if (selectedRecipeId == null || selectedRecipeId.equals(fallback.get().getId())) {
            return fallback;
        }

        Optional<AnvilRecipe> selected = AnvilRecipeManager.getRecipe(level, selectedRecipeId);
        if (selected.isEmpty()) {
            clearSelectedRecipeId();
            return fallback;
        }

        if (canCraftRecipe(selected.get(), slotA, slotB)) {
            return selected;
        }

        return Optional.empty();
    }

    private void setChangedAndNotify() {
        setChanged();
        Level level = getLevel();
        if (level != null && !level.isClientSide()) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void clearSelectedRecipeId() {
        if (this.selectedRecipeId != null) {
            this.selectedRecipeId = null;
            setChangedAndNotify();
        }
    }

    private int computeMaxCrafts(ItemStack available, ItemStack required) {
        if (required.isEmpty() || required.getCount() <= 0) {
            return Integer.MAX_VALUE;
        }
        if (available.isEmpty()) {
            return 0;
        }
        return available.getCount() / required.getCount();
    }

    private int countItemInInventory(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(invStack, stack)) {
                count += invStack.getCount();
            }
        }
        return count;
    }

    private int removeItemFromInventory(Player player, ItemStack stack, int amount) {
        if (stack.isEmpty() || amount <= 0) {
            return 0;
        }
        int remaining = amount;
        int removed = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameTags(invStack, stack)) {
                int toRemove = Math.min(remaining, invStack.getCount());
                invStack.shrink(toRemove);
                remaining -= toRemove;
                removed += toRemove;
            }
        }
        if (removed > 0) {
            player.getInventory().setChanged();
        }
        return removed;
    }

    private enum InputOrientation {
        NORMAL,
        SWAPPED,
        NONE
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }
}

