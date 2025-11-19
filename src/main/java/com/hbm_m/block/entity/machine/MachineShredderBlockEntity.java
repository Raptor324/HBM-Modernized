package com.hbm_m.block.entity.machine;

import com.hbm_m.item.ItemBlades;
import com.hbm_m.item.ModItems;
import com.hbm_m.menu.MachineShredderMenu;
import com.hbm_m.recipe.ShredderRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.energy.BlockEntityEnergyStorage;
import com.hbm_m.energy.LongDataPacker;
import com.hbm_m.energy.LongToForgeWrapper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.client.ClientSoundManager;
import com.hbm_m.sound.ShredderSoundInstance;

import java.util.Optional;

/**
 * Шреддер машина - перерабатывает предметы в пыль/скрап
 * Основан на оригинальной версии из 1.7.10
 */
public class MachineShredderBlockEntity extends BaseMachineBlockEntity {

    // Константы слотов (как в оригинале: 30 слотов)
    private static final int INPUT_SLOTS = 9;      // 0-8: входные слоты
    private static final int OUTPUT_SLOTS = 18;    // 9-26: выходные слоты
    private static final int BLADE_SLOTS = 2;      // 27-28: лезвия
    private static final int BATTERY_SLOT = 29;    // 29: батарея
    private static final int TOTAL_SLOTS = 30;

    // Границы слотов
    private static final int INPUT_START = 0;
    private static final int INPUT_END = 8;
    private static final int OUTPUT_START = 9;
    private static final int OUTPUT_END = 26;
    private static final int BLADE_LEFT = 27;
    private static final int BLADE_RIGHT = 28;

    // Константы работы (как в оригинале)
    public static final long MAX_POWER = 10000L;
    public static final int PROCESSING_SPEED = 60;  // тиков на обработку
    private static final long ENERGY_PER_TICK = 5L;  // потребление энергии за тик

    // Состояние машины
    private int progress = 0;
    private int soundCycle = 0; // Устаревшее, используется только для совместимости
    private boolean isActive = false;
    private boolean clientIsActive = false; // Отдельное поле для клиента (как в Advanced Assembler)
    private int syncCounter = 0; // Счетчик для регулярной синхронизации
    private static final int SYNC_INTERVAL = 20; // Синхронизация каждые 20 тиков (1 раз в секунду)

    // Long-энергия (как в оригинале)
    private final BlockEntityEnergyStorage energyStorage = new BlockEntityEnergyStorage(MAX_POWER, 1000L, 1000L);
    private LazyOptional<IEnergyStorage> forgeEnergyHandler = LazyOptional.empty();
    private final ContainerData containerData = new ContainerData() {
        @Override
        public int get(int index) {
            long energy = energyStorage.getEnergyStored();
            long maxEnergy = energyStorage.getMaxEnergyStored();
            long delta = getEnergyDelta();
            return switch (index) {
                case 0 -> progress;
                case 1 -> PROCESSING_SPEED;
                case 2 -> LongDataPacker.packHigh(energy);
                case 3 -> LongDataPacker.packLow(energy);
                case 4 -> LongDataPacker.packHigh(maxEnergy);
                case 5 -> LongDataPacker.packLow(maxEnergy);
                case 6 -> LongDataPacker.packHigh(delta);
                case 7 -> LongDataPacker.packLow(delta);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // read-only
        }

        @Override
        public int getCount() {
            return 8;
        }
    };

    public MachineShredderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHREDDER.get(), pos, state, TOTAL_SLOTS);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.shredder");
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot >= INPUT_START && slot <= INPUT_END) {
            // Как в оригинале: разрешаем все предметы, кроме лезвий
            // Рецепт всегда вернет результат (хотя бы скрап), поэтому проверка рецепта не нужна
            return !(stack.getItem() instanceof ItemBlades);
        }
        if (slot >= OUTPUT_START && slot <= OUTPUT_END) {
            return false; // Выходные слоты только для результатов
        }
        if (slot == BLADE_LEFT || slot == BLADE_RIGHT) {
            return stack.getItem() instanceof ItemBlades;
        }
        if (slot == BATTERY_SLOT) {
            // Проверяем, что это батарея (предмет с энергетической capability)
            return stack.getCapability(ForgeCapabilities.ENERGY).isPresent();
        }
        return false;
    }

    @Override
    protected void setupEnergyCapability() {
        longEnergyStorage = energyStorage;
        longEnergyHandler = LazyOptional.of(() -> energyStorage);
        forgeEnergyHandler = longEnergyHandler.lazyMap(LongToForgeWrapper::new);
        energyHandler = forgeEnergyHandler;
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        forgeEnergyHandler.invalidate();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineShredderMenu(containerId, playerInventory, this, containerData);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("progress", progress);
        tag.putLong("power", energyStorage.getEnergyStored());
        tag.putBoolean("active", isActive);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("progress", progress);
        tag.putLong("power", energyStorage.getEnergyStored());
        tag.putBoolean("active", isActive);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this, be -> tag);
    }
    
    @Override
    public void onDataPacket(Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt) {
        load(pkt.getTag());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("progress");
        if (tag.contains("soundCycle")) {
            soundCycle = tag.getInt("soundCycle"); // Для обратной совместимости
        }
        if (tag.contains("power")) {
            energyStorage.setEnergy(tag.getLong("power"));
        }
        if (tag.contains("active")) {
            isActive = tag.getBoolean("active");
            // Обновляем clientIsActive (как в Advanced Assembler)
            // onDataPacket() вызывается только на клиенте, но load() также вызывается на сервере
            // Это не критично, так как clientIsActive используется только на клиенте
            clientIsActive = isActive;
        } else {
            isActive = false;
            clientIsActive = false;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Останавливаем звук при удалении блока
        if (level != null && level.isClientSide) {
            ClientSoundManager.stopSound(worldPosition);
        }
    }

    public void drops() {
        SimpleContainer container = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            container.setItem(i, inventory.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, container);
    }

    // ==================== TICK LOGIC ====================

    public static void tick(Level level, BlockPos pos, BlockState state, MachineShredderBlockEntity blockEntity) {
        if (level.isClientSide()) {
            blockEntity.clientTick();
        } else {
            blockEntity.serverTick(level, pos);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void clientTick() {
        ClientSoundManager.updateSound(this, getIsActive(),
                () -> new ShredderSoundInstance(this.getBlockPos()));
    }
    
    /**
     * Возвращает состояние активности машины.
     * На клиенте возвращает clientIsActive, на сервере - isActive.
     * Аналогично isCrafting() в Advanced Assembler.
     */
    public boolean getIsActive() {
        if (level != null && level.isClientSide) {
            return clientIsActive;
        }
        return isActive;
    }    

    private void serverTick(Level level, BlockPos pos) {
        boolean dirty = false;

        chargeFromBattery();

        boolean canProcess = canProcess();
        boolean wasActive = isActive;

        // Сначала пытаемся извлечь энергию, только если можем обработать
        boolean canWork = false;
        if (canProcess && hasPower()) {
            // Потребляем энергию каждый тик, пока машина работает
            long extracted = energyStorage.extractEnergy(ENERGY_PER_TICK, false);
            if (extracted >= ENERGY_PER_TICK) {
                // Успешно извлекли достаточно энергии
                canWork = true;
                dirty = true; // Энергия изменилась
            }
        }

        if (canWork) {
            progress++;
            if (progress >= PROCESSING_SPEED) {
                for (int i = BLADE_LEFT; i <= BLADE_RIGHT; i++) {
                    ItemStack blade = inventory.getStackInSlot(i);
                    if (!blade.isEmpty() && blade.getItem() instanceof ItemBlades bladeItem) {
                        int maxDamage = bladeItem.getMaxDamage(blade);
                        if (maxDamage > 0) {
                            int oldDamage = blade.getDamageValue();
                            int newDamage = Math.min(maxDamage, oldDamage + 1);
                            if (newDamage != oldDamage) {
                                blade.setDamageValue(newDamage);
                                dirty = true;
                                // Звук ломания лезвия: играем когда лезвие только что сломалось
                                // (было < maxDamage, стало >= maxDamage)
                                if (oldDamage < maxDamage && newDamage >= maxDamage) {
                                    level.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
                                }
                            }
                        }
                    }
                }
                progress = 0;
                processItem();
                dirty = true;
            } else {
                // Прогресс изменился, нужно синхронизировать
                dirty = true;
            }
        } else {
            if (progress > 0) {
                progress = 0;
                dirty = true;
            }
        }

        // Обновляем статус активности
        isActive = canWork;
        if (wasActive != isActive) {
            dirty = true;
        }
        
        // Всегда синхронизируем isActive, если машина работает (для звука)
        // Это важно для правильной работы звука на клиенте
        if (isActive || wasActive) {
            dirty = true;
        }

        // Регулярная синхронизация для обновления GUI (каждые SYNC_INTERVAL тиков)
        syncCounter++;
        if (syncCounter >= SYNC_INTERVAL) {
            syncCounter = 0;
            dirty = true;
        }

        updateEnergyDelta(energyStorage.getEnergyStored());

        if (dirty) {
            setChanged();
            sendUpdateToClient();
        }
    }

    // ==================== ENERGY ====================

    private void chargeFromBattery() {
        ItemStack batteryStack = inventory.getStackInSlot(BATTERY_SLOT);
        if (batteryStack.isEmpty()) return;

        batteryStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(itemEnergy -> {
            if (!itemEnergy.canExtract()) return;

            long energyNeeded = energyStorage.getMaxEnergyStored() - energyStorage.getEnergyStored();
            if (energyNeeded <= 0) return;

            long maxCanReceive = energyStorage.getMaxReceive();
            long energyToTransfer = Math.min(energyNeeded, maxCanReceive);
            
            if (energyToTransfer > 0) {
                int energyToTransferInt = (int) Math.min(energyToTransfer, Integer.MAX_VALUE);
                int extracted = itemEnergy.extractEnergy(energyToTransferInt, false);
                if (extracted > 0) {
                    energyStorage.receiveEnergy(extracted, false);
                    setChanged();
                }
            }
        });
    }

    public boolean hasPower() {
        return energyStorage.getEnergyStored() > 0;
    }

    public long getPower() {
        return energyStorage.getEnergyStored();
    }

    public long getMaxPower() {
        return MAX_POWER;
    }

    public long getPowerScaled(long i) {
        return (energyStorage.getEnergyStored() * i) / MAX_POWER;
    }

    // ==================== PROCESSING ====================

    public boolean canProcess() {
        // Проверяем лезвия (как в оригинале: оба должны быть > 0 и < 3)
        int gearLeft = getGearLeft();
        int gearRight = getGearRight();
        
        if (gearLeft == 0 || gearLeft == 3 || gearRight == 0 || gearRight == 3) {
            return false;
        }

        // Проверяем наличие предметов для обработки
        for (int i = INPUT_START; i <= INPUT_END; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getCount() > 0 && hasSpace(stack)) {
                return true;
            }
        }

        return false;
    }

    private void processItem() {
        // Обрабатываем все входные слоты одновременно (как в оригинале)
        for (int inpSlot = INPUT_START; inpSlot <= INPUT_END; inpSlot++) {
            ItemStack inputStack = inventory.getStackInSlot(inpSlot);
            if (!inputStack.isEmpty() && hasSpace(inputStack)) {
                ItemStack result = getRecipeResult(inputStack);
                if (result == null || result.isEmpty()) {
                    continue;
                }

                boolean flag = false;

                // Пытаемся добавить к существующему стеку
                for (int outSlot = OUTPUT_START; outSlot <= OUTPUT_END; outSlot++) {
                    ItemStack outputStack = inventory.getStackInSlot(outSlot);
                    if (!outputStack.isEmpty() && 
                        ItemStack.isSameItemSameTags(outputStack, result) &&
                        outputStack.getCount() + result.getCount() <= outputStack.getMaxStackSize()) {
                        
                        outputStack.grow(result.getCount());
                        inputStack.shrink(1);
                        flag = true;
                        break;
                    }
                }

                // Если не нашли существующий стек, ищем пустой слот
                if (!flag) {
                    for (int outSlot = OUTPUT_START; outSlot <= OUTPUT_END; outSlot++) {
                        ItemStack outputStack = inventory.getStackInSlot(outSlot);
                        if (outputStack.isEmpty()) {
                            inventory.setStackInSlot(outSlot, result.copy());
                            inputStack.shrink(1);
                            break;
                        }
                    }
                }
            }
        }
    }

    private boolean hasSpace(ItemStack stack) {
        ItemStack result = getRecipeResult(stack);
        if (result == null || result.isEmpty()) {
            return false;
        }

        for (int i = OUTPUT_START; i <= OUTPUT_END; i++) {
            ItemStack slotStack = inventory.getStackInSlot(i);
            if (slotStack.isEmpty()) {
                return true;
            }
            if (ItemStack.isSameItemSameTags(slotStack, result) &&
                slotStack.getCount() + result.getCount() <= result.getMaxStackSize()) {
                return true;
            }
        }

        return false;
    }

    private ItemStack getRecipeResult(ItemStack input) {
        if (input.isEmpty() || level == null) {
            return new ItemStack(ModItems.SCRAP.get(), 1);
        }

        SimpleContainer container = new SimpleContainer(1);
        container.setItem(0, input);
        Optional<ShredderRecipe> recipe = level.getRecipeManager()
            .getRecipeFor(ShredderRecipe.Type.INSTANCE, container, level);
        
        if (recipe.isPresent()) {
            return recipe.get().getResultItem(level.registryAccess()).copy();
        }
        
        if (isDustLike(input)) {
            ItemStack copy = input.copy();
            copy.setCount(1);
            return copy;
        }
        
        // Если рецепта нет, возвращаем скрап (как в оригинале)
        return new ItemStack(ModItems.SCRAP.get(), 1);
    }


    // ==================== BLADES ====================

    /**
     * Получить состояние левого лезвия (как в оригинале)
     * @return 0 = нет лезвия, 1 = хорошее, 2 = изношенное, 3 = сломанное
     */
    public int getGearLeft() {
        ItemStack blade = inventory.getStackInSlot(BLADE_LEFT);
        if (blade.isEmpty() || !(blade.getItem() instanceof ItemBlades)) {
            return 0;
        }

        ItemBlades bladeItem = (ItemBlades) blade.getItem();
        int maxDamage = bladeItem.getMaxDamage(blade);
        
        if (maxDamage == 0) {
            return 1; // Бесконечное лезвие
        }

        int currentDamage = blade.getDamageValue();
        if (currentDamage < maxDamage / 2) {
            return 1; // Хорошее состояние
        } else if (currentDamage < maxDamage) {
            return 2; // Изношенное
        } else {
            return 3; // Сломанное
        }
    }

    /**
     * Получить состояние правого лезвия (как в оригинале)
     * @return 0 = нет лезвия, 1 = хорошее, 2 = изношенное, 3 = сломанное
     */
    public int getGearRight() {
        ItemStack blade = inventory.getStackInSlot(BLADE_RIGHT);
        if (blade.isEmpty() || !(blade.getItem() instanceof ItemBlades)) {
            return 0;
        }

        ItemBlades bladeItem = (ItemBlades) blade.getItem();
        int maxDamage = bladeItem.getMaxDamage(blade);
        
        if (maxDamage == 0) {
            return 1; // Бесконечное лезвие
        }

        int currentDamage = blade.getDamageValue();
        if (currentDamage < maxDamage / 2) {
            return 1; // Хорошее состояние
        } else if (currentDamage < maxDamage) {
            return 2; // Изношенное
        } else {
            return 3; // Сломанное
        }
    }

    // ==================== UTILITY ====================

    public int getDiFurnaceProgressScaled(int i) {
        return (progress * i) / PROCESSING_SPEED;
    }

    public boolean isProcessing() {
        return this.progress > 0;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return PROCESSING_SPEED;
    }

    // Для совместимости с GUI
    public IEnergyStorage getEnergyStorage() {
        return forgeEnergyHandler.orElse(null);
    }

    public int getEnergy() {
        return (int) Math.min(energyStorage.getEnergyStored(), Integer.MAX_VALUE);
    }

    public int getMaxEnergy() {
        return (int) Math.min(MAX_POWER, Integer.MAX_VALUE);
    }

    private boolean isDustLike(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        if (item == ModItems.DUST.get() || item == ModItems.DUST_TINY.get()) {
            return true;
        }
        for (RegistryObject<Item> powder : ModItems.POWDERS.values()) {
            if (powder != null && powder.isPresent() && powder.get() == item) {
                return true;
            }
        }
        for (RegistryObject<Item> powder : ModItems.INGOT_POWDERS.values()) {
            if (powder != null && powder.isPresent() && powder.get() == item) {
                return true;
            }
        }
        for (RegistryObject<Item> powder : ModItems.INGOT_POWDERS_TINY.values()) {
            if (powder != null && powder.isPresent() && powder.get() == item) {
                return true;
            }
        }
        return false;
    }

    public ContainerData getContainerData() {
        return containerData;
    }
}
