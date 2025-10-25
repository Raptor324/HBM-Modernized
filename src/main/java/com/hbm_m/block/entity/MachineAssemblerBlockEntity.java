package com.hbm_m.block.entity;

// Это блок-энтити для сборочной машины, которая может автоматически собирать сложные предметы по шаблонам.
import com.hbm_m.block.MachineAssemblerBlock;
import com.hbm_m.client.ClientSoundManager;
import com.hbm_m.energy.BlockEntityEnergyStorage;
import com.hbm_m.item.ItemAssemblyTemplate;
import com.hbm_m.item.ItemCreativeBattery;
import com.hbm_m.menu.MachineAssemblerMenu;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.multiblock.PartRole;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.sound.AssemblerSoundInstance;

import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.energy.ILongEnergyStorage;
import com.hbm_m.energy.LongToForgeWrapper;
import com.hbm_m.energy.LongDataPacker;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;

public class MachineAssemblerBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(18) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                sendUpdateToClient();
            }
        }
    };

    // Используем наш кастомный класс
    private final BlockEntityEnergyStorage energyStorage = new BlockEntityEnergyStorage(9000000000000000000L, 250L, 250L); // Добавил 'L' для ясности

    private boolean isCrafting = false;
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<ILongEnergyStorage> lazyLongEnergyHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyForgeEnergyHandler = LazyOptional.empty(); // Для совместимости

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 100;
    private long previousEnergy = 0L;
    private long energyDelta = 0L;
    private int energyDeltaUpdateCounter = 0;

    // Номера слотов для удобства
    private static final int TEMPLATE_SLOT = 4;
    private static final int OUTPUT_SLOT = 5;
    // Диапазон слотов для входных ресурсов (включительно)
    private static final int INPUT_SLOT_START = 6;
    private static final int INPUT_SLOT_END = 17;

    private LazyOptional<IItemHandler> lazyInputProxy = LazyOptional.empty();
    private LazyOptional<IItemHandler> lazyOutputProxy = LazyOptional.empty();

    private static final int DATA_IS_CRAFTING = 4;

    // Temporary set of neighbor positions we pulled items from during the last pull operation
    private final Set<BlockPos> lastPullSources = new HashSet<>();


    public MachineAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_ASSEMBLER_BE.get(), pos, state);

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                // --- ИЗМЕНЕНИЕ: Используем LongDataPacker ---
                long energy = MachineAssemblerBlockEntity.this.energyStorage.getEnergyStored();
                long maxEnergy = MachineAssemblerBlockEntity.this.energyStorage.getMaxEnergyStored();
                long delta = MachineAssemblerBlockEntity.this.energyDelta;

                return switch (index) {
                    case 0 -> MachineAssemblerBlockEntity.this.progress;
                    case 1 -> MachineAssemblerBlockEntity.this.maxProgress;

                    // Упаковываем 3 long-значения
                    case 2 -> LongDataPacker.packHigh(energy);
                    case 3 -> LongDataPacker.packLow(energy);
                    case 4 -> LongDataPacker.packHigh(maxEnergy);
                    case 5 -> LongDataPacker.packLow(maxEnergy);
                    case 6 -> LongDataPacker.packHigh(delta);
                    case 7 -> LongDataPacker.packLow(delta);

                    // Сдвигаем isCrafting
                    case 8 -> MachineAssemblerBlockEntity.this.isCrafting ? 1 : 0;
                    default -> 0;
                };
                // --- КОНЕЦ ИЗМЕНЕНИЙ ---
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> MachineAssemblerBlockEntity.this.progress = value;
                    case 1 -> MachineAssemblerBlockEntity.this.maxProgress = value;
                    // (Игнорируем 2-7, т.к. клиент не устанавливает энергию)
                    case 8 -> MachineAssemblerBlockEntity.this.isCrafting = value != 0; // Индекс сдвинут
                }
            }

            @Override
            public int getCount() {
                return 9; // ИЗМЕНЕНО: было 5, теперь 6
            }
        };
    }

    /**
     * Called by universal part entities to get a specific item handler for their role.
     * @param role The role of the part asking for the handler.
     * @return A LazyOptional containing an IItemHandler configured for that role.
     */
    public LazyOptional<IItemHandler> getItemHandlerForPart(PartRole role) {
        if (role == PartRole.ITEM_INPUT) {
            if (!lazyInputProxy.isPresent()) {
                lazyInputProxy = LazyOptional.of(this::createInputProxy);
            }
            return lazyInputProxy;
        }
        if (role == PartRole.ITEM_OUTPUT) {
            if (!lazyOutputProxy.isPresent()) {
                lazyOutputProxy = LazyOptional.of(this::createOutputProxy);
            }
            return lazyOutputProxy;
        }
        return LazyOptional.empty();
    }
    
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyLongEnergyHandler.invalidate();
        lazyForgeEnergyHandler.invalidate();
        lazyInputProxy.invalidate();
        lazyOutputProxy.invalidate();
    }

    /**
     * Creates an item handler proxy that ONLY allows inserting into the input slots.
     */
    @NotNull
    private IItemHandler createInputProxy() {

        return new IItemHandler() {
            @Override
            public int getSlots() {
                // Expose only the input slots
                return INPUT_SLOT_END - INPUT_SLOT_START + 1;
            }

            @NotNull
            @Override
            public ItemStack getStackInSlot(int slot) {
                // Map the proxy slot to the main handler's slot
                return itemHandler.getStackInSlot(slot + INPUT_SLOT_START);
            }

            @NotNull
            @Override
            public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                // Allow insertion into any of our exposed slots, mapped to the correct internal slot
                return itemHandler.insertItem(slot + INPUT_SLOT_START, stack, simulate);
            }

            @NotNull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                // Prevent extraction from the input proxy
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return itemHandler.getSlotLimit(slot + INPUT_SLOT_START);
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return itemHandler.isItemValid(slot + INPUT_SLOT_START, stack);
            }
        };
    }
    
    /**
     * Creates an item handler proxy that ONLY allows extracting from the output slot.
     */
    @NotNull
    private IItemHandler createOutputProxy() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                // Expose only the single output slot
                return 1;
            }

            @NotNull
            @Override
            public ItemStack getStackInSlot(int slot) {
                // If slot 0 is requested, return the contents of the actual output slot
                return slot == 0 ? itemHandler.getStackInSlot(OUTPUT_SLOT) : ItemStack.EMPTY;
            }

            @NotNull
            @Override
            public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                // Prevent insertion into the output proxy
                return stack;
            }

            @NotNull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                // If extraction from slot 0 is requested, extract from the actual output slot
                return slot == 0 ? itemHandler.extractItem(OUTPUT_SLOT, amount, simulate) : ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return slot == 0 ? itemHandler.getSlotLimit(OUTPUT_SLOT) : 0;
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                // No items are valid for insertion
                return false;
            }
        };
    }

    /**
     * Попытка подтянуть ровно столько ингредиентов из соседних инвентарей, чтобы хватило на один крафт.
     * Извлекает только недостающее количество до одного крафта и только если в машине есть шаблон/рецепт.
     */
    private void pullIngredientsForOneCraft(AssemblerRecipe recipe) {
        if (level == null || hasResources(this, recipe)) return;
        lastPullSources.clear();

        // (Ingredient requirement logic is unchanged)
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        Map<Ingredient, Integer> required = new IdentityHashMap<>();
        for (Ingredient ing : ingredients) {
            required.put(ing, required.getOrDefault(ing, 0) + 1);
        }

        Direction facing = this.getBlockState().getValue(MachineAssemblerBlock.FACING);
        MultiblockStructureHelper helper = ((MachineAssemblerBlock) this.getBlockState().getBlock()).getStructureHelper();

        for (BlockPos localOffset : helper.getPartOffsets()) {

            // Determine if the part at this offset is an input conveyor
            int x = localOffset.getX();
            int y = localOffset.getY();
            int z = localOffset.getZ();
            boolean isInputConveyor = (y == 0) && (x == 2) && (z == 0 || z == 1);
            
            if (!isInputConveyor) continue;


            BlockPos partPos = helper.getRotatedPos(this.worldPosition, localOffset, facing);
            BlockEntity partBE = level.getBlockEntity(partPos);

            // Check if it's a universal part
            if (!(partBE instanceof UniversalMachinePartBlockEntity)) continue;
            
            // (The rest of the logic for finding the neighbor and pulling items remains the same)
            int dx = Integer.signum(partPos.getX() - this.worldPosition.getX());
            int dz = Integer.signum(partPos.getZ() - this.worldPosition.getZ());
            BlockPos neighborPosGlobal = partPos.offset(dx, 0, dz);
            BlockEntity neighbor = level.getBlockEntity(neighborPosGlobal);
            if (neighbor == null || neighbor instanceof UniversalMachinePartBlockEntity || neighbor == this || lastPullSources.contains(neighborPosGlobal)) continue;

            // Получаем направление от части к соседу и запросим capability с этой стороны у соседа
            int dxN = partPos.getX() - neighborPosGlobal.getX();
            int dzN = partPos.getZ() - neighborPosGlobal.getZ();
            Direction dirToNeighbor;
            if (dxN == 1 && dzN == 0) dirToNeighbor = Direction.EAST;
            else if (dxN == -1 && dzN == 0) dirToNeighbor = Direction.WEST;
            else if (dxN == 0 && dzN == 1) dirToNeighbor = Direction.SOUTH;
            else if (dxN == 0 && dzN == -1) dirToNeighbor = Direction.NORTH;
            else continue;
            // Request capability on the face of the neighbor that faces the part (dirToNeighbor).
            // Previously getOpposite() was used which asked the wrong face and caused reversed/missing interaction.
            net.minecraftforge.items.IItemHandler cap = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, dirToNeighbor).orElse(null);
            if (cap == null) continue;

            // Для каждого уникального Ingredient проверяем, сколько нужно и сколько уже есть
            for (java.util.Map.Entry<Ingredient, Integer> entry : required.entrySet()) {
                Ingredient ingredient = entry.getKey();
                int need = entry.getValue();

                // Count how many of this ingredient exist in input slots
                int present = 0;
                for (int i = INPUT_SLOT_START; i <= INPUT_SLOT_END; i++) {
                    ItemStack s = this.itemHandler.getStackInSlot(i);
                    if (!s.isEmpty() && ingredient.test(s)) {
                        present += s.getCount();
                    }
                }

                int missing = need - present;
                if (missing <= 0) continue; // уже достаточно

                // Пытаемся извлечь недостающее количество из соседнего инвентаря
                for (int slot = 0; slot < cap.getSlots() && missing > 0; slot++) {
                    ItemStack possible = cap.getStackInSlot(slot);
                    if (possible.isEmpty()) continue;
                    if (!ingredient.test(possible)) continue;

                    // Симулируем извлечение up to missing
                    ItemStack simulated = cap.extractItem(slot, missing, true);
                    if (simulated.isEmpty()) continue;

                    // Попытаемся вставить simulated в наши входные слоты (симуляция)
                    ItemStack toInsert = simulated.copy();
                    for (int dest = INPUT_SLOT_START; dest <= INPUT_SLOT_END && !toInsert.isEmpty(); dest++) {
                        ItemStack remain = this.itemHandler.insertItem(dest, toInsert.copy(), true);
                        int inserted = toInsert.getCount() - remain.getCount();
                        if (inserted > 0) {
                            // Выполняем реальное извлечение и вставку
                            ItemStack actuallyExtracted = cap.extractItem(slot, inserted, false);
                            this.itemHandler.insertItem(dest, actuallyExtracted.copy(), false);
                            // Запомним источник, чтобы предотвратить немедленный возврат результата в тот же сундук
                            lastPullSources.add(neighborPosGlobal);
                            this.setChanged();
                            missing -= inserted;
                            toInsert = remain;
                        }
                    }
                }
            }
        }
    }

    /**
     * Пытается отправить содержимое выходного слота (OUTPUT_SLOT) в соседние инвентари, прилегающие к конвейерным частям.
     */
    private void pushOutputToNeighbors() {
        if (level == null) return;
        ItemStack out = this.itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (out.isEmpty()) return;

        Direction facing = this.getBlockState().getValue(MachineAssemblerBlock.FACING);
        MultiblockStructureHelper helper = ((MachineAssemblerBlock) this.getBlockState().getBlock()).getStructureHelper();

        for (BlockPos localOffset : helper.getPartOffsets()) {
            if (out.isEmpty()) break;

            // Определяем, является ли эта часть выходным коннектором по ее относительной позиции
            int x = localOffset.getX();
            int y = localOffset.getY();
            int z = localOffset.getZ();
            boolean isOutputConveyor = (y == 0) && (x == -1) && (z == 0 || z == 1);

            if (!isOutputConveyor) continue;

            BlockPos partPos = helper.getRotatedPos(this.worldPosition, localOffset, facing);
            
            // 1. Определяем направление "наружу" от центра машины к коннектору.
            int dxOut = Integer.signum(partPos.getX() - this.worldPosition.getX());
            int dzOut = Integer.signum(partPos.getZ() - this.worldPosition.getZ());
            Direction outDir = Direction.getNearest(dxOut, 0, dzOut);

            // 2. Определяем направление "вперёд" для всей машины.
            Direction facingDir = this.getBlockState().getValue(MachineAssemblerBlock.FACING);

            // 3. Целевой инвентарь сдвинут по диагонали: на 1 блок наружу и на 1 блок НАЗАД.
            BlockPos neighborPos = partPos.relative(outDir).relative(facingDir.getOpposite());

            BlockEntity neighbor = level.getBlockEntity(neighborPos);
            if (neighbor == null || neighbor instanceof UniversalMachinePartBlockEntity || neighbor == this || lastPullSources.contains(neighborPos)) continue;

            // 4. Так как блок диагональный, нужно проверить обе возможные грани для подключения.
            Direction side1 = outDir.getOpposite(); // Грань, смотрящая на коннектор
            Direction side2 = facingDir;         // Грань, смотрящая "вперёд", навстречу сдвигу "назад"

            IItemHandler cap = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, side1)
                    .orElse(neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, side2)
                            .orElse(null));
            if (cap == null) continue;

            // Попытаться вставить всю стопку в соседний инвентарь
            ItemStack toInsert = out.copy();
            for (int slot = 0; slot < cap.getSlots() && !toInsert.isEmpty(); slot++) {
                ItemStack remaining = cap.insertItem(slot, toInsert.copy(), false);
                // Если что-то было вставлено, обновляем наш инвентарь
                if (remaining.getCount() < toInsert.getCount()) {
                    // Уменьшаем стак в нашем выходном слоте на количество вставленных предметов
                    this.itemHandler.getStackInSlot(OUTPUT_SLOT).shrink(toInsert.getCount() - remaining.getCount());
                    toInsert = remaining;
                }
            }
            // Обновляем ссылку на стак в выходном слоте, так как он мог измениться
            out = this.itemHandler.getStackInSlot(OUTPUT_SLOT);
        }
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
    public AbstractContainerMenu createMenu(int pContainerId, @Nonnull Inventory pPlayerInventory, Player pPlayer) {
        // Отправляем пакет с данными при открытии меню
        sendUpdateToClient();
        return new MachineAssemblerMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {

        // --- ИЗМЕНЕНИЕ: Добавляем поддержку LONG_ENERGY ---
        if (cap == ModCapabilities.LONG_ENERGY) {
            return lazyLongEnergyHandler.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyForgeEnergyHandler.cast();
        }
        // ---

        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }
    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);

        // --- ИЗМЕНЕНИЕ: Инициализируем ОБА capability ---
        // lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
        lazyLongEnergyHandler = LazyOptional.of(() -> energyStorage);
        lazyForgeEnergyHandler = lazyLongEnergyHandler.lazyMap(LongToForgeWrapper::new); // Оборачиваем
        // ---
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("inventory", itemHandler.serializeNBT());

        // --- ИЗМЕНЕНИЕ: int -> long ---
        nbt.putLong("energy", energyStorage.getEnergyStored()); // Было putInt
        // ---

        nbt.putInt("progress", progress);
        nbt.putBoolean("isCrafting", this.isCrafting);

        // --- ИЗМЕНЕНИЕ: int -> long ---
        nbt.putLong("previousEnergy", this.previousEnergy); // Было putInt
        nbt.putLong("energyDelta", this.energyDelta); // Было putInt
        // ---
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        itemHandler.deserializeNBT(nbt.getCompound("inventory"));

        // --- ИЗМЕНЕНИЕ: int -> long ---
        energyStorage.setEnergy(nbt.getLong("energy")); // Было getInt
        // ---

        progress = nbt.getInt("progress");
        // ... (isCrafting) ...

        // --- ИЗМЕНЕНИЕ: int -> long ---
        this.previousEnergy = nbt.getLong("previousEnergy"); // Было getInt
        this.energyDelta = nbt.getLong("energyDelta"); // Было getInt
        // ---

        // ... (остальной код load) ...
    }

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, MachineAssemblerBlockEntity pBlockEntity) {
        if (pLevel.isClientSide) {
            pBlockEntity.clientTick(pLevel, pPos, pState);
        } else {
            serverTick(pLevel, pPos, pState, pBlockEntity);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void clientTick(Level level, BlockPos pos, BlockState state) {
        // Эта одна строка теперь полностью управляет запуском и остановкой зацикленного звука.
        ClientSoundManager.updateSound(this, this.isCrafting, () -> new AssemblerSoundInstance(this.getBlockPos()));
    }

    private static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, MachineAssemblerBlockEntity pBlockEntity) {
        pBlockEntity.requestEnergy();
        
        final int ENERGY_SLOT_INDEX = 0;
        ItemStack energySourceStack = pBlockEntity.itemHandler.getStackInSlot(ENERGY_SLOT_INDEX);
        
        if (!energySourceStack.isEmpty()) {
            if (energySourceStack.getItem() instanceof ItemCreativeBattery) {
                pBlockEntity.energyStorage.receiveEnergy(Integer.MAX_VALUE, false);
            } else {
                energySourceStack.getCapability(ForgeCapabilities.ENERGY).ifPresent(itemEnergy -> {
                    long energyNeeded = pBlockEntity.energyStorage.getMaxEnergyStored() - pBlockEntity.energyStorage.getEnergyStored();
                    long maxCanReceive = pBlockEntity.energyStorage.getMaxReceive();
                    long energyToTransfer = Math.min(energyNeeded, maxCanReceive);
                    
                    if (energyToTransfer > 0) {
                        int energyToTransferInt = (int) Math.min(energyToTransfer, Integer.MAX_VALUE);

                        int extracted = itemEnergy.extractEnergy(energyToTransferInt, false); // Используем int

                        pBlockEntity.energyStorage.receiveEnergy(extracted, false); // Принимаем как long
                    }

                });
            }
        }
        
        pBlockEntity.energyDeltaUpdateCounter++;
        if (pBlockEntity.energyDeltaUpdateCounter >= 20) {
            // Дельта = текущая энергия - предыдущая энергия
            // Положительное значение = приход энергии
            // Отрицательное значение = расход энергии
            long currentEnergy = pBlockEntity.energyStorage.getEnergyStored();
            pBlockEntity.energyDelta = (currentEnergy - pBlockEntity.previousEnergy) / 20L; // Делим на long
            pBlockEntity.previousEnergy = currentEnergy;
            setChanged(pLevel, pPos, pState);
        }
        
        // ЛОГИКА КРАФТА
        Optional<AssemblerRecipe> recipeOpt = getRecipeFromTemplate(pLevel, pBlockEntity);
        
        if (recipeOpt.isPresent()) {
            pBlockEntity.pullIngredientsForOneCraft(recipeOpt.get());
        }
        
        // Проверяем условия для крафта
        if (recipeOpt.isPresent() && hasResources(pBlockEntity, recipeOpt.get()) && canInsertResult(pBlockEntity, recipeOpt.get().getResultItem(null))) {
            AssemblerRecipe recipe = recipeOpt.get();
            
            // Вычисляем потребление энергии за тик (как в старой версии)
            int energyPerTick = recipe.getPowerConsumption();
            
            // Проверяем, достаточно ли энергии для этого тика
            if (pBlockEntity.energyStorage.getEnergyStored() >= energyPerTick) {
                
                // НАЧАЛО КРАФТА
                if (!pBlockEntity.isCrafting) {
                    pBlockEntity.isCrafting = true;
                    pBlockEntity.maxProgress = recipe.getDuration();
                    setChanged(pLevel, pPos, pState);
                    pBlockEntity.sendUpdateToClient();
                }
                
                // ПРОЦЕСС КРАФТА - потребляем энергию КАЖДЫЙ ТИК (как в 1.7.10)
                pBlockEntity.energyStorage.extractEnergy(energyPerTick, false);
                pBlockEntity.progress++;
                setChanged(pLevel, pPos, pState);
                
                // ЗАВЕРШЕНИЕ КРАФТА
                if (pBlockEntity.progress >= pBlockEntity.maxProgress) {
                    craftItem(pBlockEntity, recipe);
                    pBlockEntity.progress = 0;
                    
                    pBlockEntity.pushOutputToNeighbors();
                    getRecipeFromTemplate(pLevel, pBlockEntity).ifPresent(pBlockEntity::pullIngredientsForOneCraft);
                }
                
            } else {
                // Недостаточно энергии - останавливаем
                if (pBlockEntity.isCrafting) {
                    pBlockEntity.progress = 0;
                    pBlockEntity.isCrafting = false;
                    setChanged(pLevel, pPos, pState);
                    pBlockEntity.sendUpdateToClient();
                }
            }
            
        } else {
            // УСЛОВИЯ НЕ ВЫПОЛНЕНЫ
            if (pBlockEntity.isCrafting) {
                pBlockEntity.progress = 0;
                pBlockEntity.isCrafting = false;
                setChanged(pLevel, pPos, pState);
                pBlockEntity.sendUpdateToClient();
            }
        }
        
        pBlockEntity.data.set(DATA_IS_CRAFTING, pBlockEntity.isCrafting ? 1 : 0);
    }

    private void requestEnergy() {
        if (this.energyStorage.getEnergyStored() >= this.energyStorage.getMaxEnergyStored()) return;
        long energyNeeded = this.energyStorage.getMaxReceive();
        if (energyNeeded <= 0) return;

        // UUID requestId = UUID.randomUUID();
        Direction facing = this.getBlockState().getValue(MachineAssemblerBlock.FACING);
        MultiblockStructureHelper helper = ((MachineAssemblerBlock) this.getBlockState().getBlock()).getStructureHelper();

        for (BlockPos localOffset : helper.getPartOffsets()) {

            // Determine if the part at this offset is an energy connector
            int x = localOffset.getX();
            int y = localOffset.getY();
            int z = localOffset.getZ();
            boolean isEnergyConnector = (y == 0) && (x == 0 || x == 1) && (z == -1 || z == 2);
            
            if (isEnergyConnector) {
                BlockPos partPos = helper.getRotatedPos(this.worldPosition, localOffset, facing);
                BlockEntity partBE = level.getBlockEntity(partPos);

                // Check if it's a universal part
                if (!(partBE instanceof UniversalMachinePartBlockEntity)) continue;

                for (Direction dir : Direction.values()) {
                    BlockEntity neighbor = level.getBlockEntity(partPos.relative(dir));

                    if (neighbor == null || neighbor instanceof UniversalMachinePartBlockEntity || neighbor == this) {
                        continue;
                    }

                    long extracted = 0L;
                    LazyOptional<ILongEnergyStorage> longCap = neighbor.getCapability(ModCapabilities.LONG_ENERGY, dir.getOpposite());

                    if (longCap.isPresent()) {
                        ILongEnergyStorage longStorage = longCap.resolve().orElse(null);
                        if (longStorage != null && longStorage.canExtract()) {
                            extracted = longStorage.extractEnergy(energyNeeded, false);
                        }
                    }
                    // 2. Если не нашли, проверяем Wire (у тебя он int)
                    else if (neighbor instanceof WireBlockEntity wire) {
                        // (Если wire.requestEnergy всё ещё int, оставляем)
                        extracted = wire.requestEnergy(energyNeeded, false /*, requestId*/);
                    }
                    // 3. Если не нашли, проверяем Forge (int)
                    else {
                        IEnergyStorage source = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).orElse(null);
                        if (source != null && source.canExtract()) {

                            int energyNeededInt = (int) Math.min(energyNeeded, Integer.MAX_VALUE);
                            extracted = source.extractEnergy(energyNeededInt, false); // int -> int (OK)
                        }
                    }

                    if (extracted > 0) {
                        long accepted = this.energyStorage.receiveEnergy(extracted, false);
                        energyNeeded -= accepted;
                        if (energyNeeded <= 0) break;
                    }
                }
            }
            if (energyNeeded <= 0) break;
        }
    }

    // Получает рецепт из шаблона в слоте
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

    // Проверяет, достаточно ли ресурсов во входных слотах
    private static boolean hasResources(MachineAssemblerBlockEntity pBlockEntity, AssemblerRecipe recipe) {
        // Создаем "контейнер" из наших входных слотов
        SimpleContainer inventory = new SimpleContainer(INPUT_SLOT_END - INPUT_SLOT_START + 1);
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            inventory.setItem(i, pBlockEntity.itemHandler.getStackInSlot(INPUT_SLOT_START + i));
        }

        // Ванильный метод matches сам проверит, хватает ли предметов
        return recipe.matches(inventory, pBlockEntity.level);
    }

    // Проверяет, можно ли поместить результат в выходной слот
    private static boolean canInsertResult(MachineAssemblerBlockEntity pBlockEntity, ItemStack result) {
        ItemStack outputSlotStack = pBlockEntity.itemHandler.getStackInSlot(OUTPUT_SLOT);

        // Слот пуст ИЛИ в слоте тот же предмет и есть место
        return outputSlotStack.isEmpty() ||
                (ItemStack.isSameItemSameTags(outputSlotStack, result) &&
                        outputSlotStack.getCount() + result.getCount() <= outputSlotStack.getMaxStackSize());
    }


    // Выполняет крафт: списывает ресурсы и создает результат
    private static void craftItem(MachineAssemblerBlockEntity pBlockEntity, AssemblerRecipe recipe) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        // Создаем копию, чтобы не изменять объект рецепта
        ItemStack result = recipe.getResultItem(null).copy();

        for (Ingredient ingredient : ingredients) {
            // Ищем подходящий предмет во входных слотах
            for (int i = INPUT_SLOT_START; i <= INPUT_SLOT_END; i++) {
                ItemStack stackInSlot = pBlockEntity.itemHandler.getStackInSlot(i);
                if (ingredient.test(stackInSlot)) {
                    // Нашли! Забираем один предмет.
                    pBlockEntity.itemHandler.extractItem(i, 1, false);
                    break; // Выходим из внутреннего цикла, чтобы не списать лишнего за один ингредиент.
                }
            }
        }

        // Помещаем результат в выходной слот
        pBlockEntity.itemHandler.insertItem(OUTPUT_SLOT, result, false);
        pBlockEntity.sendUpdateToClient();
    }

    // Синхронизация с клиентом
    private void sendUpdateToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putBoolean("isCrafting", this.isCrafting);
        tag.put("inventory", itemHandler.serializeNBT());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        if (this.level != null && this.level.isClientSide) {
            this.isCrafting = tag.getBoolean("isCrafting");
            if (tag.contains("inventory")) {
                itemHandler.deserializeNBT(tag.getCompound("inventory"));
            }
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
    }

    // Добавим обработку уничтожения блока для надежности
    @Override
    public void setRemoved() {
        // Этот метод вызывается при удалении BlockEntity.
        // Важно выполнять операции со звуком только на стороне клиента.
        if (level.isClientSide) {
            // Используем существующий менеджер звуков, чтобы остановить звук.
            // Передаем 'false', чтобы указать, что звук больше не должен играть.
            // Supplier звука может быть null, так как он не будет использоваться при остановке.
            ClientSoundManager.updateSound(this, false, null);
        }
        super.setRemoved();
    }
}