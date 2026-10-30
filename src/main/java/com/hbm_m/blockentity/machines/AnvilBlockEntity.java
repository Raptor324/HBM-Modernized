package com.hbm_m.blockentity.machines;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.anvils.AnvilBlock;
import com.hbm_m.block.machines.anvils.AnvilTier;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.AnvilMenu;
import com.hbm_m.item.fekal_electric.ModBatteryItem;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.recipe.AnvilRecipe;
import com.hbm_m.recipe.AnvilRecipe.AStack;
import com.hbm_m.recipe.AnvilRecipeManager;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
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
import com.hbm_m.platform.StackNbt;

public class AnvilBlockEntity extends BaseHbmBlockEntity implements MenuProvider {

    private final ModItemStackHandler itemHandler = new ModItemStackHandler(3) {
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

    @Nullable
    private ResourceLocation selectedRecipeId;

    public AnvilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANVIL_BE.get(), pos, state);
    }


    // (устраняет вложенный stonecutter-баг в load())
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        // Slot 2 holds an unpaid preview of the recipe result: updateCrafting() writes it without
        // consuming anything, and only AnvilMenu pays for it when a player takes it. Persisting or
        // dropping it handed out free items, so it is left out of the tag and recomputed on load.
        ItemStack preview = itemHandler.getStackInSlot(2);
        itemHandler.setStackInSlot(2, ItemStack.EMPTY);
        tag.put("inventory", itemHandler.serialize( registries));
        itemHandler.setStackInSlot(2, preview);

        if (selectedRecipeId != null) {
            tag.putString("SelectedRecipe", selectedRecipeId.toString());
        }

        super.writeNbtData(tag, registries);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        itemHandler.deserialize(tag.getCompound("inventory"), registries);
        selectedRecipeId = tag.contains("SelectedRecipe") ?
            ResourceLocation.tryParse(tag.getString("SelectedRecipe")) : null;
    }

    public void drops() {
        // Slot 2 is the unpaid preview (see writeNbtData): dropping it duplicated the result.
        SimpleContainer inventory = new SimpleContainer(2);
        for (int i = 0; i < 2; i++) {
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
        player.openMenu(this);
    }

    public ModItemStackHandler getItemHandler() {
        return itemHandler;
    }

    /**
     * Наружу (воронки, трубы, авто-крафт) инвентарь наковальни не отдаётся:
     * в оригинале 1.7.10 наковальня не автоматизируется, а слот выхода — виртуальный.
     * Иначе воронка забирает результат без расхода материалов (дюп).
     */
    @Override
    public @Nullable Object getItemHandler(@Nullable net.minecraft.core.Direction side) {
        return null;
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

        // FFA-рецепт: спец-формы (cyanide/rename/hot) формируют результат из слотов,
        // поэтому выход берётся через getSmithingOutput, а не из первого outputs-элемента.
        ItemStack result = recipeOpt
                .map(recipe -> recipe.getSmithingOutput(slotA, slotB))
                .orElse(ItemStack.EMPTY);

        ItemStack currentOutput = itemHandler.getStackInSlot(2);

        // Обновляем только если результат изменился, чтобы не спамить пакетами
        if (!ItemStack.matches(result, currentOutput)) {
            itemHandler.setStackInSlot(2, result);
            setChangedAndNotify();
        }
    }

    /**
     * Проверяет, можно ли выполнить рецепт с текущими материалами.
     * FFA-рецепт: матчинг строго позиционный ({@link AnvilRecipe#matchesSmithing},
     * включая спец-формы cyanide/rename/mold и проверку жара).
     */
    private boolean canCraftRecipe(AnvilRecipe recipe, ItemStack slotA, ItemStack slotB) {
        if (!recipe.matchesSmithing(slotA, slotB)) {
            return false;
        }
        return recipe.canCraftOn(getTier());
    }

    private int computeMaxCrafts(ItemStack slotA, ItemStack slotB, AnvilRecipe recipe) {
        return Math.min(
                computeMaxCrafts(slotA, recipe.getLeft()),
                computeMaxCrafts(slotB, recipe.getRight())
        );
    }

    private void shrinkSlotInput(ItemStack slotA, ItemStack slotB, AnvilRecipe recipe, int craftCount) {
        // amountConsumed учитывает consume_left/consume_right (форма сохраняет заготовку и т.п.)
        int consumeForSlotA = recipe.amountConsumed(0);
        int consumeForSlotB = recipe.amountConsumed(1);

        if (consumeForSlotA > 0) {
            slotA.shrink(consumeForSlotA * craftCount);
        }

        if (consumeForSlotB > 0) {
            slotB.shrink(consumeForSlotB * craftCount);
        }
    }

    private boolean fillSlotFromPlayer(ServerPlayer player, int slotIndex, @Nullable AStack required) {
        if (required == null || required.count() <= 0) {
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

        if (!current.isEmpty() && !required.matchesIgnoreSize(current)) {
            ItemStack toReturn = current.copy();
            itemHandler.setStackInSlot(slotIndex, ItemStack.EMPTY);
            if (!player.getInventory().add(toReturn)) {
                player.drop(toReturn, false);
            }
            changed = true;
        }

        // Подтягиваем ровно столько, сколько требует рецепт (не забиваем слот под завязку)
        current = itemHandler.getStackInSlot(slotIndex);
        int have = current.isEmpty() ? 0 : current.getCount();
        int missing = required.count() - have;
        if (missing <= 0) {
            return changed;
        }

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

        if (!recipe.matchesSmithing(slotA, slotB)) {
            return false;
        }

        int craftCount = craftMax ? maxCraftsPerAction(recipe) : 1;
        if (craftMax) {
            craftCount = Math.min(craftCount, computeMaxCrafts(slotA, slotB, recipe));
        }
        if (craftCount <= 0) {
            return false;
        }

        for (AStack required : recipe.getInputs()) {
            if (required.count() <= 0) {
                continue;
            }
            int available = countItemInInventory(player, required);
            craftCount = Math.min(craftCount, available / required.count());
        }

        if (craftCount <= 0) {
            return false;
        }

        shrinkSlotInput(slotA, slotB, recipe, craftCount);

        for (AStack required : recipe.getInputs()) {
            if (required.count() <= 0) {
                continue;
            }
            removeItemFromInventory(player, required, required.count() * craftCount, tracker);
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
        EnergyTransferTracker tracker = new EnergyTransferTracker();
        collectEnergyFromSlots(tracker, slotACopy, slotBCopy, recipe, 1);

        boolean crafted = consumeMaterials(player, false, tracker);
        if (crafted && !outputStack.isEmpty()) {
            applyEnergyToOutputs(List.of(outputStack), tracker.getTotalEnergy());
        }
        updateCrafting();
    }

    /**
     * Выполняет выбранный construction-рецепт и помещает результат в инвентарь игрока
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

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        return craftConstruction(serverPlayer, recipeOpt.get(), craftMax ? 1 : 0);
    }

    /**
     * FFA {@code AnvilCraftPacket}: крафт construction-рецепта из инвентаря игрока.
     * mode >= 1 (Shift) — крафтить максимально возможное количество.
     */
    public boolean craftConstruction(ServerPlayer player, @Nullable AnvilRecipe recipe, int mode) {
        Level level = getLevel();
        if (level == null || level.isClientSide() || recipe == null || !recipe.isConstruction()) {
            return false;
        }
        if (!recipe.canCraftOn(getTier())) {
            return false;
        }

        int maxCrafts = computeInventoryCraftLimit(player, recipe);
        if (maxCrafts <= 0) {
            return false;
        }

        // Оригинал AnvilCraftPacket: один выход — до maxStackSize/размерВыхода крафтов, несколько — 64
        int craftCount = Math.min(mode >= 1 ? maxCraftsPerAction(recipe) : 1, maxCrafts);
        if (craftCount <= 0) {
            return false;
        }

        EnergyTransferTracker tracker = new EnergyTransferTracker();
        consumeInventory(player, recipe, craftCount, tracker);

        List<ItemStack> produced = rollOutputs(level, recipe, craftCount);
        applyEnergyToOutputs(produced, tracker.getTotalEnergy());
        if (!produced.isEmpty()) {
            // FFA: Achivements fuer construction-Kraefte (AnvilMenu bulk loop fired pro Ausgang)
            com.hbm_m.advancement.AchievementHandler.fire(player, produced.get(0));
            giveStacksToPlayer(player, produced);
        }

        setChangedAndNotify();
        updateCrafting();
        return true;
    }

    /** Лимит «крафнуть максимум» из оригинала: 64 для нескольких выходов, иначе стек/размерВыхода. */
    private static int maxCraftsPerAction(AnvilRecipe recipe) {
        List<AnvilRecipe.ResultEntry> outputs = recipe.getOutputs();
        if (outputs.size() > 1) {
            return 64;
        }
        ItemStack output = outputs.get(0).stack();
        int perCraft = Math.max(1, output.getCount());
        return Math.max(1, output.getMaxStackSize() / perCraft);
    }

    public void populateInputsFromPlayer(ServerPlayer player, AnvilRecipe recipe) {
        if (player == null || recipe == null) {
            return;
        }
        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        // FFA-матчинг строго позиционный: слот 0 <- left, слот 1 <- right.
        boolean changed = fillSlotFromPlayer(player, 0, recipe.getLeft())
                | fillSlotFromPlayer(player, 1, recipe.getRight());

        if (changed) {
            player.getInventory().setChanged();
            setChangedAndNotify();
            updateCrafting();
        }
    }

    private Optional<AnvilRecipe> resolveCombineRecipe(Level level, ItemStack slotA, ItemStack slotB) {
        Optional<AnvilRecipe> selected = resolveSelectedRecipe(level)
                .filter(AnvilRecipe::isSmithing)
                .filter(recipe -> canCraftRecipe(recipe, slotA, slotB));

        if (selected.isPresent()) {
            return selected;
        }

        return AnvilRecipeManager
                .findSmithing(level, slotA, slotB, getTier())
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
        return resolveSelectedRecipe(level).filter(AnvilRecipe::isConstruction);
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

    private int computeMaxCrafts(ItemStack available, @Nullable AStack required) {
        if (required == null || required.count() <= 0) {
            return Integer.MAX_VALUE;
        }
        if (available.isEmpty()) {
            return 0;
        }
        return available.getCount() / required.count();
    }

    private int countItemInInventory(Player player, AStack required) {
        if (required.count() <= 0) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (required.matchesIgnoreSize(invStack)) {
                count += invStack.getCount();
            }
        }
        return count;
    }

    private int removeItemFromInventory(Player player, AStack required, int amount, @Nullable EnergyTransferTracker tracker) {
        if (required.count() <= 0 || amount <= 0) {
            return 0;
        }
        int remaining = amount;
        int removed = 0;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (required.matchesIgnoreSize(invStack)) {
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
        List<AStack> requirements = recipe.getInputs();
        if (requirements.isEmpty()) {
            return 0;
        }

        int limit = Integer.MAX_VALUE;
        for (AStack required : requirements) {
            if (required.count() <= 0) {
                continue;
            }
            int available = countItemInInventory(player, required);
            limit = Math.min(limit, available / required.count());
        }
        return limit;
    }

    private void consumeInventory(Player player, AnvilRecipe recipe, int craftCount, @Nullable EnergyTransferTracker tracker) {
        for (AStack required : recipe.getInputs()) {
            if (required.count() <= 0) {
                continue;
            }
            removeItemFromInventory(player, required, required.count() * craftCount, tracker);
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
            if (StackNbt.sameItemSameTags(existing, addition)) {
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
                                        AnvilRecipe recipe, int craftCount) {
        if (tracker == null) {
            return;
        }
        AStack firstRequired = recipe.getLeft();
        AStack secondRequired = recipe.getRight();
        tracker.add(extractEnergyFromStack(slotA, (firstRequired == null ? 0 : firstRequired.count()) * craftCount));
        tracker.add(extractEnergyFromStack(slotB, (secondRequired == null ? 0 : secondRequired.count()) * craftCount));
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

    private int moveMatchingItemsFromInventory(Player player, @Nullable AStack template, int amount, Consumer<ItemStack> consumer) {
        if (player == null || template == null || template.count() <= 0 || amount <= 0) {
            return 0;
        }
        Inventory inventory = player.getInventory();
        int remaining = amount;
        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack invStack = inventory.getItem(i);
            if (!template.matchesIgnoreSize(invStack)) {
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

    // getUpdateTag / getUpdatePacket / handleUpdateTag удалены:
    // их семантика (super + saveAdditional / load) полностью покрывается BaseHbmBlockEntity.

    // getItemHandler(side) переопределён и возвращает null — см. комментарий у метода.
}
