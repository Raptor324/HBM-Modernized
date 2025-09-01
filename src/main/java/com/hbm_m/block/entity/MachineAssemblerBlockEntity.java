package com.hbm_m.block.entity;

import com.hbm_m.block.MachineAssemblerBlock;
import com.hbm_m.block.MachineAssemblerPartBlock;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.energy.BlockEntityEnergyStorage;
import com.hbm_m.item.ItemAssemblyTemplate;
import com.hbm_m.item.ItemCreativeBattery;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.menu.MachineAssemblerMenu;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.sounds.RequestAssemblerStateC2SPacket;
import com.hbm_m.network.sounds.StartAssemblerSoundS2CPacket;
import com.hbm_m.network.sounds.StopAssemblerSoundS2CPacket;
import com.hbm_m.recipe.AssemblerRecipe;
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
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;

public class MachineAssemblerBlockEntity extends BlockEntity implements MenuProvider {

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

    // Temporary set of neighbor positions we pulled items from during the last pull operation
    private final Set<BlockPos> lastPullSources = new HashSet<>();


    public MachineAssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_ASSEMBLER_BE.get(), pos, state);

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

    /**
     * Попытка подтянуть ровно столько ингредиентов из соседних инвентарей, чтобы хватило на один крафт.
     * Извлекает только недостающее количество до одного крафта и только если в машине есть шаблон/рецепт.
     */
    private void pullIngredientsForOneCraft(AssemblerRecipe recipe) {
        if (level == null) return;
        lastPullSources.clear();

        // Если в машине уже есть все ресурсы для крафта, ничего не делаем
        if (hasResources(this, recipe)) return;

        // Сформируем потребности: агрегируем одинаковые объекты Ingredient и посчитаем требуемое количество
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        java.util.IdentityHashMap<Ingredient, Integer> required = new java.util.IdentityHashMap<>();
        for (Ingredient ing : ingredients) {
            required.put(ing, required.getOrDefault(ing, 0) + 1);
        }

        Direction facing = this.getBlockState().getValue(MachineAssemblerBlock.FACING);
        MultiblockStructureHelper helper = ((MachineAssemblerBlock) this.getBlockState().getBlock()).getStructureHelper();

        for (BlockPos localOffset : helper.getPartOffsets()) {
            BlockPos partPos = helper.getRotatedPos(this.worldPosition, localOffset, facing);
            BlockEntity partBE = level.getBlockEntity(partPos);

            if (!(partBE instanceof MachineAssemblerPartBlockEntity part) || !part.isConveyorConnector()) continue;

            // В этой версии коннекторы оказались инвертированы: используем противоположное значение
            // Разрешаем подтягивание только с входных частей (offsetX == 3)
            int partOffsetX = part.getBlockState().getValue(MachineAssemblerPartBlock.OFFSET_X);
            boolean partIsInput = (partOffsetX == 3);
            if (!partIsInput) continue;

            // Определим внешний сосед как единичный шаг от части в направлении от контроллера к части
            int dx = Integer.signum(partPos.getX() - this.worldPosition.getX());
            int dz = Integer.signum(partPos.getZ() - this.worldPosition.getZ());
            BlockPos neighborPosGlobal = partPos.offset(dx, 0, dz);
            BlockEntity neighbor = level.getBlockEntity(neighborPosGlobal);
            if (neighbor == null) continue;
            if (neighbor instanceof MachineAssemblerPartBlockEntity || neighbor == this) continue;
            if (lastPullSources.contains(neighborPosGlobal)) continue;

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

            BlockPos partPos = helper.getRotatedPos(this.worldPosition, localOffset, facing);
            BlockEntity partBE = level.getBlockEntity(partPos);

            if (!(partBE instanceof MachineAssemblerPartBlockEntity part) || !part.isConveyorConnector()) continue;

            int partOffsetX = part.getBlockState().getValue(MachineAssemblerPartBlock.OFFSET_X);
            boolean partIsOutput = (partOffsetX == 0);
            if (!partIsOutput) continue;

            // --- ИСПРАВЛЕННАЯ ЛОГИКА ---
            // 1. Определяем направление "наружу" от центра машины к коннектору.
            int dxOut = Integer.signum(partPos.getX() - this.worldPosition.getX());
            int dzOut = Integer.signum(partPos.getZ() - this.worldPosition.getZ());
            Direction outDir = Direction.getNearest(dxOut, 0, dzOut);

            // 2. Определяем направление "вперёд" для всей машины.
            Direction facingDir = this.getBlockState().getValue(MachineAssemblerBlock.FACING);

            // 3. Целевой инвентарь сдвинут по диагонали: на 1 блок наружу и на 1 блок НАЗАД.
            BlockPos neighborGlobal = partPos.relative(outDir).relative(facingDir.getOpposite());

            BlockEntity neighbor = level.getBlockEntity(neighborGlobal);
            if (neighbor == null || neighbor instanceof MachineAssemblerPartBlockEntity || neighbor == this) continue;
            if (lastPullSources.contains(neighborGlobal)) continue;

            // 4. Так как блок диагональный, нужно проверить обе возможные грани для подключения.
            Direction side1 = outDir.getOpposite(); // Грань, смотрящая на коннектор
            Direction side2 = facingDir; // Грань, смотрящая "вперёд", навстречу сдвигу "назад"

            IItemHandler cap = neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, side1)
                    .orElse(neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, side2)
                            .orElse(null));
            if (cap == null) continue;
            // --- КОНЕЦ ИСПРАВЛЕННОЙ ЛОГИКИ ---

            // Попытаться вставить всю стопку в соседний инвентарь
            ItemStack toInsert = out.copy();
            for (int slot = 0; slot < cap.getSlots() && !toInsert.isEmpty(); slot++) {
                ItemStack remaining = cap.insertItem(slot, toInsert.copy(), true);
                int inserted = toInsert.getCount() - remaining.getCount();
                if (inserted > 0) {
                    // делаем реальную вставку
                    cap.insertItem(slot, this.itemHandler.extractItem(OUTPUT_SLOT, inserted, false), false);
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
    protected void saveAdditional(@Nonnull CompoundTag nbt) {
        super.saveAdditional(nbt);
        nbt.put("inventory", itemHandler.serializeNBT());
        nbt.putInt("energy", energyStorage.getEnergyStored());
        nbt.putInt("progress", progress);
        nbt.putBoolean("isCrafting", this.isCrafting); // Сохраняем состояние крафта
    }

    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        itemHandler.deserializeNBT(nbt.getCompound("inventory"));
        energyStorage.setEnergy(nbt.getInt("energy"));
        progress = nbt.getInt("progress");
        this.isCrafting = nbt.getBoolean("isCrafting"); // Загружаем состояние крафта
    }

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, MachineAssemblerBlockEntity pBlockEntity) {
        if (!pLevel.isClientSide()) {
            // В самом начале серверного тика мы очищаем список обработанных запросов.
            // Это КЛЮЧЕВОЙ момент для работы системы.
            WireBlockEntity.startNewTick();
            serverTick(pLevel, pPos, pState, pBlockEntity);
        }
    }

    private static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, MachineAssemblerBlockEntity pBlockEntity) {
        pBlockEntity.requestEnergy();

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

        // ЛОГИКА КРАФТА

        Optional<AssemblerRecipe> recipeOpt = getRecipeFromTemplate(pLevel, pBlockEntity);

        // Попытаться подтянуть ресурсы из соседних инвентарей (сундуков) для одного крафта,
        // только если есть шаблон и рецепт найден.
        if (recipeOpt.isPresent()) {
            pBlockEntity.pullIngredientsForOneCraft(recipeOpt.get());
        }

        // Проверяем, можем ли мы ВООБЩЕ крафтить по этому рецепту
        if (recipeOpt.isPresent() && hasResources(pBlockEntity, recipeOpt.get()) && hasPower(pBlockEntity) && canInsertResult(pBlockEntity, recipeOpt.get().getResultItem(null))) {

            // НАЧАЛО НОВОГО КРАФТA
            // Если машина не крафтила, но теперь может, это начало нового цикла.
            if (!pBlockEntity.isCrafting) {
                pBlockEntity.isCrafting = true;
                pBlockEntity.maxProgress = recipeOpt.get().getDuration();
                // Отправляем пакет на запуск звука
                ModPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> pLevel.getChunkAt(pPos)),
                        new StartAssemblerSoundS2CPacket(pPos));
                setChanged(pLevel, pPos, pState); // Важно для синхронизации
            }

            // ПРОЦЕСС КРАФТА
            pBlockEntity.progress++;
            pBlockEntity.energyStorage.extractEnergy(10, false); // Потребляем энергию
            setChanged(pLevel, pPos, pState);

            // ЗАВЕРШЕНИЕ КРАФТА
            if (pBlockEntity.progress >= pBlockEntity.maxProgress) {
                craftItem(pBlockEntity, recipeOpt.get());
                // Сбрасываем все для следующего цикла
                pBlockEntity.progress = 0;
                // После крафта попробуем переместить результат в соседние сундуки
                pBlockEntity.pushOutputToNeighbors();
                // И сразу подтянуть следующую партию для нового крафта, если рецепт все еще валиден
                getRecipeFromTemplate(pLevel, pBlockEntity).ifPresent(pBlockEntity::pullIngredientsForOneCraft);
                // НЕ устанавливаем isCrafting в false здесь,
                // пусть это произойдет в блоке else на следующем тике, если ресурсы кончатся.
                // Это позволит машине сразу начать новый крафт без остановки звука.
            }

        } else {
            // УСЛОВИЯ ДЛЯ КРАФТА НЕ ВЫПОЛНЕНЫ
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

    private void requestEnergy() {
        if (this.energyStorage.getEnergyStored() >= this.energyStorage.getMaxEnergyStored()) {
            return;
        }

        int energyNeeded = this.energyStorage.getMaxReceive();
        if (energyNeeded <= 0) return;

        // --- ЛОГ #1: Начало запроса ---
        if (ModClothConfig.get().enableDebugLogging) {
            MainRegistry.LOGGER.debug("[ASSEMBLER >>>] Starting energy request for {} FE.", energyNeeded);
        }

        UUID requestId = UUID.randomUUID();
        Direction facing = this.getBlockState().getValue(MachineAssemblerBlock.FACING);
        MultiblockStructureHelper helper = ((MachineAssemblerBlock) this.getBlockState().getBlock()).getStructureHelper();

        for (BlockPos localOffset : helper.getPartOffsets()) {
            BlockPos partPos = helper.getRotatedPos(this.worldPosition, localOffset, facing);
            BlockEntity partBE = level.getBlockEntity(partPos);

            if (partBE instanceof MachineAssemblerPartBlockEntity part && part.isEnergyConnector()) {
                if (ModClothConfig.get().enableDebugLogging) {
                    MainRegistry.LOGGER.debug("[ASSEMBLER] Found an energy connector at {}.", partPos);
                }

                for (Direction dir : Direction.values()) {
                    BlockEntity neighbor = level.getBlockEntity(partPos.relative(dir));

                    if (neighbor == null || neighbor instanceof MachineAssemblerPartBlockEntity || neighbor == this) {
                        continue;
                    }

                    if (ModClothConfig.get().enableDebugLogging) {
                        MainRegistry.LOGGER.debug("[ASSEMBLER]  -> Connector at {} is checking neighbor at {} [{}]", partPos, partPos.relative(dir), neighbor.getClass().getSimpleName());
                    }

                    int extracted = 0;
                    if (neighbor instanceof WireBlockEntity wire) {
                        if (ModClothConfig.get().enableDebugLogging) {
                            MainRegistry.LOGGER.debug("[ASSEMBLER]    -> It's a wire. Forwarding request with ID {}.", requestId);
                        }
                        extracted = wire.requestEnergy(energyNeeded, false, requestId);
                    } else {
                        IEnergyStorage source = neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).orElse(null);
                        if (source != null && source.canExtract()) {
                            if (ModClothConfig.get().enableDebugLogging) {
                                MainRegistry.LOGGER.debug("[ASSEMBLER]    -> It's a direct source. canExtract() is true. Attempting to pull {} FE.", energyNeeded);
                            }
                            extracted = source.extractEnergy(energyNeeded, false);
                        } else if (source == null) {
                            if (ModClothConfig.get().enableDebugLogging)
                                MainRegistry.LOGGER.debug("[ASSEMBLER]    -> Neighbor does not have ENERGY capability.");
                        } else {
                            if (ModClothConfig.get().enableDebugLogging)
                                MainRegistry.LOGGER.debug("[ASSEMBLER]    -> Neighbor has capability, but canExtract() is false.");
                        }
                    }

                    if (extracted > 0) {
                        if (ModClothConfig.get().enableDebugLogging) {
                            MainRegistry.LOGGER.debug("[ASSEMBLER]      -> SUCCESS! Pulled {} FE.", extracted);
                        }
                        int accepted = this.energyStorage.receiveEnergy(extracted, false);
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

    // Проверяет, достаточно ли энергии для одного тика крафта
    private static boolean hasPower(MachineAssemblerBlockEntity pBlockEntity) {
        return pBlockEntity.energyStorage.getEnergyStored() >= 10;
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
        return saveWithoutMetadata();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
    }

    // Добавим обработку уничтожения блока для надежности
    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide() && isCrafting) {
            MainRegistry.LOGGER.info("SERVER ({}): Block is being removed while crafting. Sending final STOP packet.", worldPosition);
            ModPacketHandler.INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(worldPosition)),
                    new StopAssemblerSoundS2CPacket(worldPosition));
        }
        super.setRemoved();
    }
}