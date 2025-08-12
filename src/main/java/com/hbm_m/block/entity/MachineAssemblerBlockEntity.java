package com.hbm_m.block.entity;

import com.hbm_m.energy.BlockEntityEnergyStorage;
import com.hbm_m.item.ItemAssemblyTemplate;
import com.hbm_m.item.ItemCreativeBattery;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.menu.MachineAssemblerMenu;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.sounds.RequestAssemblerStateC2SPacket;
import com.hbm_m.network.sounds.StartAssemblerSoundS2CPacket;
import com.hbm_m.network.sounds.StopAssemblerSoundS2CPacket;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.sound.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener; // <-- ПРАВИЛЬНЫЙ ИМПОРТ
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.PacketDistributor;

import java.util.Optional;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MachineAssemblerBlockEntity extends BlockEntity implements MenuProvider {

    public static Supplier<BlockEntityType<MachineAssemblerBlockEntity>> TYPE_SUPPLIER;

    private final ItemStackHandler itemHandler = new ItemStackHandler(18) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };


    // Используем наш кастомный класс
    private final BlockEntityEnergyStorage energyStorage = new BlockEntityEnergyStorage(100000, 250);

    private boolean isCrafting = false;
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();
    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 100;

    // Номера слотов для удобства
    private static final int TEMPLATE_SLOT = 4;
    private static final int OUTPUT_SLOT = 5;
    // Диапазон слотов для входных ресурсов (включительно)
    private static final int INPUT_SLOT_START = 6;
    private static final int INPUT_SLOT_END = 17;

    private static final int DATA_IS_CRAFTING = 4;


    public MachineAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(TYPE_SUPPLIER.get(), pos, state);
        
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> MachineAssemblerBlockEntity.this.progress;
                    case 1 -> MachineAssemblerBlockEntity.this.maxProgress;
                    case 2 -> MachineAssemblerBlockEntity.this.energyStorage.getEnergyStored();
                    case 3 -> MachineAssemblerBlockEntity.this.energyStorage.getMaxEnergyStored();
                    // 1. Читаем состояние крафта
                    case DATA_IS_CRAFTING -> MachineAssemblerBlockEntity.this.isCrafting ? 1 : 0; 
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> MachineAssemblerBlockEntity.this.progress = value;
                    case 1 -> MachineAssemblerBlockEntity.this.maxProgress = value;
                    case 2 -> MachineAssemblerBlockEntity.this.energyStorage.setEnergy(value);
                    // 2. Устанавливаем состояние крафта
                    case DATA_IS_CRAFTING -> MachineAssemblerBlockEntity.this.isCrafting = value != 0;
                }
            }
            
            @Override
            public int getCount() {
                // 3. Увеличиваем размер до 5
                return 5;
            }
        };
    }

    // Добавляем этот публичный сеттер, который будет вызываться пакетами на клиенте
    @OnlyIn(Dist.CLIENT)
    public void setCrafting(boolean crafting) {
        this.isCrafting = crafting;
    }
    
    // 4. Добавим getter для меню
    public boolean isCrafting() {
        return this.isCrafting;
    }
    
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.machine_assembler");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        // Отправляем пакет с данными при открытии меню
        sendUpdateToClient();
        return new MachineAssemblerMenu(pContainerId, pPlayerInventory, this, this.data);
    }
    
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyEnergyHandler.cast();
        }
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
        
        // Этот код остается, он идеален для перезагрузки
        if (level != null && level.isClientSide) {
            ModPacketHandler.INSTANCE.sendToServer(new RequestAssemblerStateC2SPacket(this.worldPosition));
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyEnergyHandler.invalidate();
    }
    
    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("inventory", itemHandler.serializeNBT());
        nbt.putInt("energy", energyStorage.getEnergyStored());
        nbt.putInt("progress", progress);
        nbt.putBoolean("isCrafting", this.isCrafting); // Сохраняем состояние крафта
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        itemHandler.deserializeNBT(nbt.getCompound("inventory"));
        energyStorage.setEnergy(nbt.getInt("energy"));
        progress = nbt.getInt("progress");
        this.isCrafting = nbt.getBoolean("isCrafting"); // Загружаем состояние крафта
    }

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, MachineAssemblerBlockEntity pBlockEntity) {
        if (!pLevel.isClientSide()) {
            serverTick(pLevel, pPos, pState, pBlockEntity);
        }
    }
    
    // Серверная логика
    private static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, MachineAssemblerBlockEntity pBlockEntity) {

        final int ENERGY_SLOT_INDEX = 0; // Определяем индекс слота для зарядки
        ItemStack energySourceStack = pBlockEntity.itemHandler.getStackInSlot(ENERGY_SLOT_INDEX);

        if (!energySourceStack.isEmpty()) {
            // Проверяем, является ли предмет в слоте нашей креативной батарейкой
            if (energySourceStack.getItem() instanceof ItemCreativeBattery) {
                // Если да, мгновенно заполняем буфер машины
                pBlockEntity.energyStorage.receiveEnergy(Integer.MAX_VALUE, false);
                // setChanged() здесь не нужен, так как receiveEnergy уже вызывает его внутри
            } else {
                // Если это любой другой предмет с энергией (обычная батарейка)
                energySourceStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(itemEnergy -> {
                    // Рассчитываем, сколько энергии машина может принять
                    int energyNeeded = pBlockEntity.energyStorage.getMaxEnergyStored() - pBlockEntity.energyStorage.getEnergyStored();
                    int maxCanReceive = pBlockEntity.energyStorage.getMaxReceive();
                    int energyToTransfer = Math.min(energyNeeded, maxCanReceive);

                    if (energyToTransfer > 0) {
                        // Извлекаем энергию из предмета и передаем в машину
                        int extracted = itemEnergy.extractEnergy(energyToTransfer, false);
                        pBlockEntity.energyStorage.receiveEnergy(extracted, false);
                    }
                });
            }
        }

        // --- НОВАЯ ЛОГИКА КРАФТА ---

        Optional<AssemblerRecipe> recipeOpt = getRecipeFromTemplate(pLevel, pBlockEntity);

        // Проверяем, можем ли мы ВООБЩЕ крафтить по этому рецепту
        if (recipeOpt.isPresent() && hasResources(pBlockEntity, recipeOpt.get()) && hasPower(pBlockEntity) && canInsertResult(pBlockEntity, recipeOpt.get().getResultItem(null))) {
            
            // --- НАЧАЛО НОВОГО КРАФТА ---
            // Если машина не крафтила, но теперь может, это начало нового цикла.
            if (!pBlockEntity.isCrafting) {
                pBlockEntity.isCrafting = true;
                // Устанавливаем maxProgress ИЗ РЕЦЕПТА!
                pBlockEntity.maxProgress = recipeOpt.get().getDuration();
                // Отправляем пакет на запуск звука
                ModPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> pLevel.getChunkAt(pPos)),
                                                new StartAssemblerSoundS2CPacket(pPos));
                setChanged(pLevel, pPos, pState); // Важно для синхронизации
            }

            // --- ПРОЦЕСС КРАФТА ---
            pBlockEntity.progress++;
            pBlockEntity.energyStorage.extractEnergy(10, false); // Потребляем энергию
            setChanged(pLevel, pPos, pState);

            // --- ЗАВЕРШЕНИЕ КРАФТА ---
            if (pBlockEntity.progress >= pBlockEntity.maxProgress) {
                craftItem(pBlockEntity, recipeOpt.get());
                // Сбрасываем все для следующего цикла
                pBlockEntity.progress = 0;
                // НЕ устанавливаем isCrafting в false здесь,
                // пусть это произойдет в блоке else на следующем тике, если ресурсы кончатся.
                // Это позволит машине сразу начать новый крафт без остановки звука.
            }

        } else {
            // --- УСЛОВИЯ ДЛЯ КРАФТА НЕ ВЫПОЛНЕНЫ ---
            // Если машина крафтила, но теперь не может (например, кончились ресурсы), останавливаем ее.
            if (pBlockEntity.isCrafting) {
                pBlockEntity.progress = 0;
                pBlockEntity.isCrafting = false;
                // Отправляем пакет на остановку звука
                ModPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> pLevel.getChunkAt(pPos)),
                                                new StopAssemblerSoundS2CPacket(pPos));
                setChanged(pLevel, pPos, pState); // Важно для синхронизации
            }
        }
        
        // Синхронизируем состояние isCrafting с GUI в любом случае
        pBlockEntity.data.set(DATA_IS_CRAFTING, pBlockEntity.isCrafting ? 1 : 0);
    }

    


    /** Получает рецепт из шаблона в слоте */
    private static Optional<AssemblerRecipe> getRecipeFromTemplate(Level level, MachineAssemblerBlockEntity pBlockEntity) {
        ItemStack templateStack = pBlockEntity.itemHandler.getStackInSlot(TEMPLATE_SLOT);
        if (!(templateStack.getItem() instanceof ItemAssemblyTemplate)) {
            return Optional.empty();
        }
        
        ItemStack outputStack = ItemAssemblyTemplate.getRecipeOutput(templateStack);
        if (outputStack.isEmpty()) {
            return Optional.empty();
        }

        RecipeManager recipeManager = level.getRecipeManager();
        return recipeManager.getAllRecipesFor(AssemblerRecipe.Type.INSTANCE)
                .stream()
                .filter(r -> ItemStack.isSameItemSameTags(r.getResultItem(null), outputStack))
                .findFirst();
    }

    /** Проверяет, достаточно ли ресурсов во входных слотах */
    private static boolean hasResources(MachineAssemblerBlockEntity pBlockEntity, AssemblerRecipe recipe) {
        // Создаем "контейнер" из наших входных слотов
        SimpleContainer inventory = new SimpleContainer(INPUT_SLOT_END - INPUT_SLOT_START + 1);
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            inventory.setItem(i, pBlockEntity.itemHandler.getStackInSlot(INPUT_SLOT_START + i));
        }
        
        // Ванильный метод matches сам проверит, хватает ли предметов
        return recipe.matches(inventory, pBlockEntity.level);
    }

    /** Проверяет, достаточно ли энергии для одного тика крафта */
    private static boolean hasPower(MachineAssemblerBlockEntity pBlockEntity) {
        return pBlockEntity.energyStorage.getEnergyStored() >= 10;
    }

    /** Проверяет, можно ли поместить результат в выходной слот */
    private static boolean canInsertResult(MachineAssemblerBlockEntity pBlockEntity, ItemStack result) {
        ItemStack outputSlotStack = pBlockEntity.itemHandler.getStackInSlot(OUTPUT_SLOT);
        
        // Слот пуст ИЛИ в слоте тот же предмет и есть место
        return outputSlotStack.isEmpty() || 
               (ItemStack.isSameItemSameTags(outputSlotStack, result) && 
                outputSlotStack.getCount() + result.getCount() <= outputSlotStack.getMaxStackSize());
    }

    
    /** Выполняет крафт: списывает ресурсы и создает результат (ИСПРАВЛЕННАЯ ВЕРСЯ) */
    private static void craftItem(MachineAssemblerBlockEntity pBlockEntity, AssemblerRecipe recipe) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        ItemStack result = recipe.getResultItem(null);

        for (Ingredient ingredient : ingredients) {
            // Ищем подходящий предмет во входных слотах
            for (int i = INPUT_SLOT_START; i <= INPUT_SLOT_END; i++) {
                ItemStack stackInSlot = pBlockEntity.itemHandler.getStackInSlot(i);
                if (ingredient.test(stackInSlot)) {
                    // Нашли! Забираем один предмет.
                    // Используем extractItem, так как он корректно обрабатывает изменение стака.
                    // false - означает, что мы реально выполняем действие, а не симулируем.
                    pBlockEntity.itemHandler.extractItem(i, 1, false);
                    break; // <- Важно! Выходим из внутреннего цикла, чтобы не списать лишнего за один ингредиент.
                }
            }
        }
        
        // Помещаем результат в выходной слот
        pBlockEntity.itemHandler.insertItem(OUTPUT_SLOT, result, false);
    }
    
    // --- Синхронизация с клиентом (ИСПРАВЛЕНО) ---
    private void sendUpdateToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
    }
    
    // Добавим обработку уничтожения блока для надежности
    @Override
    public void setRemoved() {
        if (!level.isClientSide() && isCrafting) {
            // <<--- ЛОГ
            MainRegistry.LOGGER.info("SERVER ({}): Block is being removed while crafting. Sending final STOP packet.", worldPosition);
            ModPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)),
                                            new StopAssemblerSoundS2CPacket(worldPosition));
        }
        super.setRemoved();
    }  
}