package com.hbm_m.block.entity.custom.machines;

import com.hbm_m.block.custom.machines.anvils.AnvilBlock;
import com.hbm_m.block.custom.machines.anvils.AnvilTier;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.item.custom.fekal_electric.ModBatteryItem;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.util.RandomSource;

public class AnvilBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            // Если изменились входные слоты, нужно пересчитать выход
            if (slot == 0 || slot == 1) {
                updateCrafting();
            }
            setChanged();
        }
    
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot != 2;
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
        // Просто сохраняем весь инвентарь как есть, включая слот 2
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
        if (level == null) return;
    
        ItemStack slotA = itemHandler.getStackInSlot(0);
        ItemStack slotB = itemHandler.getStackInSlot(1);
        Optional<AnvilRecipe> recipeOpt = resolveCombineRecipe(level, slotA, slotB);
    
        ItemStack result = recipeOpt
                .map(recipe -> recipe.getResultItem(level.registryAccess()).copy())
                .orElse(ItemStack.EMPTY);
    
        ItemStack currentOutput = itemHandler.getStackInSlot(2);
        
        // Обновляем только если результат изменился, чтобы не спамить пакетами
        if (!ItemStack.matches(result, currentOutput)) {
            itemHandler.setStackInSlot(2, result);
            setChangedAndNotify(); 
        }
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

        return matchesIngredient(actual, required) &&
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
        if (required.isEmpty()) {
            return actual.isEmpty() ? 0 : 1;
        }
        if (actual.isEmpty()) {
            return 1;
        }
        return matchesIngredient(actual, required) ? 0 : 1;
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
        // Определяем, какой слот соответствует какому требованию рецепта
        // NORMAL: slotA -> inputA, slotB -> inputB
        // SWAPPED: slotA -> inputB, slotB -> inputA
        
        boolean isNormal = orientation == InputOrientation.NORMAL;
        
        // Требуемые предметы (для расчета количества, если нужно)
        ItemStack reqForSlotA = isNormal ? recipe.getInputA() : recipe.getInputB();
        ItemStack reqForSlotB = isNormal ? recipe.getInputB() : recipe.getInputA();
    
        // Проверяем, нужно ли потреблять предметы в слоте A
        // Если ориентация NORMAL, смотрим consumesA. Если SWAPPED, смотрим consumesB.
        boolean shouldConsumeSlotA = isNormal ? recipe.consumesA() : recipe.consumesB();
        
        // Аналогично для слота B
        boolean shouldConsumeSlotB = isNormal ? recipe.consumesB() : recipe.consumesA();
    
        if (shouldConsumeSlotA) {
            slotA.shrink(reqForSlotA.getCount() * craftCount);
        }
        
        if (shouldConsumeSlotB) {
            slotB.shrink(reqForSlotB.getCount() * craftCount);
        }
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

        if (!current.isEmpty() && !matchesIngredient(current, required)) {
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
        int pulled = moveMatchingItemsFromInventory(player, required, missing, extracted -> {
            ItemStack remainder = itemHandler.insertItem(slotIndex, extracted, false);
            if (!remainder.isEmpty()) {
                if (!player.getInventory().add(remainder)) {
                    player.drop(remainder, false);
                }
            }
        });
        if (pulled > 0) {
            changed = true;
        }
        return changed;
    }

    /**
     * Расходует материалы после крафта
     * 
     * @param player игрок, выполняющий крафт
     * @param craftMax если true, крафтит максимальное количество
     * @return true, если материалы были успешно расходованы
     */
    public boolean consumeMaterials(Player player, boolean craftMax, @Nullable EnergyTransferTracker tracker) {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return false;
        }

        ItemStack slotA = itemHandler.getStackInSlot(0);
        ItemStack slotB = itemHandler.getStackInSlot(1);

        Optional<AnvilRecipe> recipeOpt = resolveCombineRecipe(level, slotA, slotB);
        if (recipeOpt.isEmpty()) {
            return false;
        }

        AnvilRecipe recipe = recipeOpt.get();

        InputOrientation orientation = resolveOrientation(slotA, slotB, recipe);
        if (orientation == InputOrientation.NONE) {
            return false;
        }

        int craftCount = craftMax ? 64 : 1;
        if (craftMax) {
            craftCount = Math.min(craftCount, computeMaxCrafts(slotA, slotB, recipe, orientation));
        }
        if (craftCount <= 0) {
            return false;
        }

        for (ItemStack required : recipe.getInventoryInputs()) {
            if (required.isEmpty()) {
                continue;
            }
            int available = countItemInInventory(player, required);
            craftCount = Math.min(craftCount, available / required.getCount());
        }

        if (craftCount <= 0) {
            return false;
        }

        shrinkSlotInput(slotA, slotB, recipe, orientation, craftCount);

        for (ItemStack required : recipe.getInventoryInputs()) {
            if (required.isEmpty()) {
                continue;
            }
            removeItemFromInventory(player, required, required.getCount() * craftCount, tracker);
        }

        player.getInventory().setChanged();
        setChangedAndNotify();
        return true;
    }

    public void handleCombineOutputTaken(Player player, ItemStack outputStack) {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        ItemStack slotACopy = itemHandler.getStackInSlot(0).copy();
        ItemStack slotBCopy = itemHandler.getStackInSlot(1).copy();
        Optional<AnvilRecipe> recipeOpt = resolveCombineRecipe(level, slotACopy, slotBCopy);
        if (recipeOpt.isEmpty()) {
            consumeMaterials(player, false, null);
            updateCrafting();
            return;
        }

        AnvilRecipe recipe = recipeOpt.get();
        InputOrientation orientation = resolveOrientation(slotACopy, slotBCopy, recipe);
        if (orientation == InputOrientation.NONE) {
            consumeMaterials(player, false, null);
            updateCrafting();
            return;
        }

        EnergyTransferTracker tracker = new EnergyTransferTracker();
        collectEnergyFromSlots(tracker, slotACopy, slotBCopy, recipe, orientation, 1);

        boolean crafted = consumeMaterials(player, false, tracker);
        if (crafted && !outputStack.isEmpty()) {
            applyEnergyToOutputs(List.of(outputStack), tracker.getTotalEnergy());
        }
        updateCrafting();
    }

    /**
     * Выполняет крафт и помещает результат в инвентарь игрока
     */
    public boolean craft(Player player, boolean craftMax) {
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return false;
        }

        Optional<AnvilRecipe> recipeOpt = resolveSelectedInventoryRecipe(level);
        if (recipeOpt.isEmpty()) {
            return false;
        }

        AnvilRecipe recipe = recipeOpt.get();

        int maxCrafts = computeInventoryCraftLimit(player, recipe);
        if (maxCrafts <= 0) {
            return false;
        }

        int craftCount = Math.min(craftMax ? 64 : 1, maxCrafts);
        if (craftCount <= 0) {
            return false;
        }

        EnergyTransferTracker tracker = new EnergyTransferTracker();
        consumeInventory(player, recipe, craftCount, tracker);

        List<ItemStack> produced = rollOutputs(level, recipe, craftCount);
        applyEnergyToOutputs(produced, tracker.getTotalEnergy());
        if (!produced.isEmpty()) {
            giveStacksToPlayer(player, produced);
        }

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

    private Optional<AnvilRecipe> resolveCombineRecipe(Level level, ItemStack slotA, ItemStack slotB) {
        Optional<AnvilRecipe> selected = resolveSelectedRecipe(level)
                .filter(AnvilRecipe::usesMachineInputs)
                .filter(recipe -> canCraftRecipe(recipe, slotA, slotB));

        if (selected.isPresent()) {
            return selected;
        }

        return AnvilRecipeManager
                .findRecipe(level, slotA, slotB, getTier())
                .filter(AnvilRecipe::usesMachineInputs)
                .filter(recipe -> canCraftRecipe(recipe, slotA, slotB));
    }

    private Optional<AnvilRecipe> resolveSelectedRecipe(Level level) {
        if (selectedRecipeId == null) {
            return Optional.empty();
        }

        Optional<AnvilRecipe> selected = AnvilRecipeManager.getRecipe(level, selectedRecipeId);
        if (selected.isEmpty() || !selected.get().canCraftOn(getTier())) {
            clearSelectedRecipeId();
            return Optional.empty();
        }

        return selected;
    }

    private Optional<AnvilRecipe> resolveSelectedInventoryRecipe(Level level) {
        return resolveSelectedRecipe(level).filter(recipe -> !recipe.usesMachineInputs());
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
            if (matchesIngredient(invStack, stack)) {
                count += invStack.getCount();
            }
        }
        return count;
    }

    private int removeItemFromInventory(Player player, ItemStack stack, int amount, @Nullable EnergyTransferTracker tracker) {
        if (stack.isEmpty() || amount <= 0) {
            return 0;
        }
        int remaining = amount;
        int removed = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (matchesIngredient(invStack, stack)) {
                int toRemove = Math.min(remaining, invStack.getCount());
                if (tracker != null && isBattery(invStack)) {
                    long energyPerItem = ModBatteryItem.getEnergy(invStack);
                    tracker.add(energyPerItem * toRemove);
                }
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

    private int computeInventoryCraftLimit(Player player, AnvilRecipe recipe) {
        List<ItemStack> requirements = recipe.getInventoryInputs();
        if (requirements.isEmpty()) {
            return 0;
        }

        int limit = Integer.MAX_VALUE;
        for (ItemStack required : requirements) {
            if (required.isEmpty()) {
                continue;
            }
            int available = countItemInInventory(player, required);
            limit = Math.min(limit, available / required.getCount());
        }
        return limit;
    }

    private void consumeInventory(Player player, AnvilRecipe recipe, int craftCount, @Nullable EnergyTransferTracker tracker) {
        for (ItemStack required : recipe.getInventoryInputs()) {
            if (required.isEmpty()) {
                continue;
            }
            removeItemFromInventory(player, required, required.getCount() * craftCount, tracker);
        }
    }

    private List<ItemStack> rollOutputs(Level level, AnvilRecipe recipe, int craftCount) {
        List<ItemStack> result = new ArrayList<>();
        RandomSource random = level.random;

        for (int i = 0; i < craftCount; i++) {
            for (AnvilRecipe.ResultEntry entry : recipe.getOutputs()) {
                if (entry.chance() >= 1.0F || random.nextFloat() <= entry.chance()) {
                    ItemStack stack = entry.stack().copy();
                    mergeStack(result, stack);
                }
            }
        }

        return result;
    }

    private void mergeStack(List<ItemStack> stacks, ItemStack addition) {
        if (addition.isEmpty()) {
            return;
        }

        for (ItemStack existing : stacks) {
            if (ItemStack.isSameItemSameTags(existing, addition)) {
                existing.grow(addition.getCount());
                return;
            }
        }

        stacks.add(addition.copy());
    }

    private void giveStacksToPlayer(Player player, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        player.getInventory().setChanged();
    }

    private void collectEnergyFromSlots(@Nullable EnergyTransferTracker tracker, ItemStack slotA, ItemStack slotB,
                                        AnvilRecipe recipe, InputOrientation orientation, int craftCount) {
        if (tracker == null) {
            return;
        }
        ItemStack firstRequired = orientation == InputOrientation.NORMAL ? recipe.getInputA() : recipe.getInputB();
        ItemStack secondRequired = orientation == InputOrientation.NORMAL ? recipe.getInputB() : recipe.getInputA();
        tracker.add(extractEnergyFromStack(slotA, firstRequired.getCount() * craftCount));
        tracker.add(extractEnergyFromStack(slotB, secondRequired.getCount() * craftCount));
    }

    private long extractEnergyFromStack(ItemStack stack, int itemCount) {
        if (!isBattery(stack) || itemCount <= 0) {
            return 0;
        }
        int copies = Math.min(itemCount, Math.max(1, stack.getCount()));
        long energyPerItem = ModBatteryItem.getEnergy(stack);
        return energyPerItem * copies;
    }

    private void applyEnergyToOutputs(List<ItemStack> outputs, long totalEnergy) {
        if (totalEnergy <= 0 || outputs.isEmpty()) {
            return;
        }

        List<ItemStack> batteryOutputs = new ArrayList<>();
        for (ItemStack stack : outputs) {
            if (isBattery(stack)) {
                batteryOutputs.add(stack);
            }
        }

        if (batteryOutputs.isEmpty()) {
            return;
        }

        long remaining = totalEnergy;
        for (ItemStack stack : batteryOutputs) {
            if (remaining <= 0) {
                break;
            }
            ModBatteryItem battery = (ModBatteryItem) stack.getItem();
            long capacity = battery.getCapacity();
            long current = ModBatteryItem.getEnergy(stack);
            long space = Math.max(0, capacity - current);
            if (space <= 0) {
                continue;
            }
            long toInsert = Math.min(space, remaining);
            addEnergyToBattery(stack, toInsert);
            remaining -= toInsert;
        }
    }

    private void addEnergyToBattery(ItemStack stack, long delta) {
        if (delta <= 0 || !isBattery(stack)) {
            return;
        }
        ModBatteryItem battery = (ModBatteryItem) stack.getItem();
        long current = ModBatteryItem.getEnergy(stack);
        long newEnergy = Math.min(battery.getCapacity(), current + delta);
        ModBatteryItem.setEnergy(stack, newEnergy);
    }

    private boolean isBattery(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ModBatteryItem;
    }

    private static final class EnergyTransferTracker {
        private long totalEnergy = 0;

        void add(long energy) {
            if (energy > 0) {
                totalEnergy += energy;
            }
        }

        long getTotalEnergy() {
            return totalEnergy;
        }
    }

    private boolean matchesIngredient(ItemStack actual, ItemStack required) {
        if (required.isEmpty()) {
            return actual.isEmpty();
        }
        if (actual.isEmpty()) {
            return false;
        }
        if (required.hasTag()) {
            return ItemStack.isSameItemSameTags(actual, required);
        }
        return actual.is(required.getItem());
    }

    private int moveMatchingItemsFromInventory(Player player, ItemStack template, int amount, Consumer<ItemStack> consumer) {
        if (player == null || template.isEmpty() || amount <= 0) {
            return 0;
        }
        Inventory inventory = player.getInventory();
        int remaining = amount;
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack invStack = inventory.getItem(i);
            if (!matchesIngredient(invStack, template)) {
                continue;
            }
            int toExtract = Math.min(remaining, invStack.getCount());
            ItemStack extracted = invStack.split(toExtract);
            if (extracted.isEmpty()) {
                continue;
            }
            remaining -= extracted.getCount();
            consumer.accept(extracted);
        }
        if (remaining < amount) {
            inventory.setChanged();
        }
        return amount - remaining;
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

