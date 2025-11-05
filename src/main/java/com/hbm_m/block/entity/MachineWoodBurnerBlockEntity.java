package com.hbm_m.block.entity;

import com.hbm_m.block.MachineWoodBurnerBlock;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.energy.BlockEntityEnergyStorage;
import com.hbm_m.energy.ILongEnergyStorage;
import com.hbm_m.energy.LongToForgeWrapper;

import com.hbm_m.energy.LongDataPacker;
import com.hbm_m.item.ModItems;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.menu.MachineWoodBurnerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class MachineWoodBurnerBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final BlockEntityEnergyStorage energyStorage = new BlockEntityEnergyStorage(100000L, 1000L, 1000L);
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<ILongEnergyStorage> lazyLongEnergyHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyForgeEnergyHandler = LazyOptional.empty();

    // Обёртка для IENERGYSTORAGE, которая запрещает приём энергии ИЗВНЕ
    private final ILongEnergyStorage longEnergyWrapper = new ILongEnergyStorage() {
        @Override
        public long receiveEnergy(long maxReceive, boolean simulate) {
            return 0L; // Генератор НЕ принимает энергию
        }
        
        @Override
        public long extractEnergy(long maxExtract, boolean simulate) {
            return energyStorage.extractEnergy(maxExtract, simulate);
        }
        
        @Override
        public long getEnergyStored() {
            return energyStorage.getEnergyStored();
        }
        
        @Override
        public long getMaxEnergyStored() {
            return energyStorage.getMaxEnergyStored();
        }
        
        @Override
        public boolean canExtract() {
            return energyStorage.canExtract();
        }
        
        @Override
        public boolean canReceive() {
            return false; // Генератор НЕ принимает энергию
        }
    };
    
    // Переменные состояния
    private int burnTime = 0;
    private int maxBurnTime = 0;
    private boolean isLit = false;
    private boolean enabled = true;

    public MachineWoodBurnerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.WOOD_BURNER_BE.get(), pPos, pBlockState);

        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                long energy = MachineWoodBurnerBlockEntity.this.energyStorage.getEnergyStored();
                long maxEnergy = MachineWoodBurnerBlockEntity.this.energyStorage.getMaxEnergyStored();
                return switch (pIndex) {
                    case 0 -> LongDataPacker.packHigh(energy);
                    case 1 -> LongDataPacker.packLow(energy);
                    case 2 -> LongDataPacker.packHigh(maxEnergy);
                    case 3 -> LongDataPacker.packLow(maxEnergy);
                    case 4 -> MachineWoodBurnerBlockEntity.this.burnTime;
                    case 5 -> MachineWoodBurnerBlockEntity.this.maxBurnTime;
                    case 6 -> MachineWoodBurnerBlockEntity.this.isLit ? 1 : 0;
                    case 7 -> MachineWoodBurnerBlockEntity.this.enabled ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 4 -> MachineWoodBurnerBlockEntity.this.burnTime = pValue;
                    case 5 -> MachineWoodBurnerBlockEntity.this.maxBurnTime = pValue;
                    case 6 -> MachineWoodBurnerBlockEntity.this.isLit = pValue != 0;
                    case 7 -> {
                        // Обработка переключения
                        if (pValue == -1) {
                            // Специальное значение -1 означает "переключить"
                            MachineWoodBurnerBlockEntity.this.enabled = !MachineWoodBurnerBlockEntity.this.enabled;
                            MachineWoodBurnerBlockEntity.this.setChanged();
                        } else {
                            // Обычная установка значения
                            MachineWoodBurnerBlockEntity.this.enabled = pValue != 0;
                        }
                    }
                }
                case 6 -> energyDelta = value;
            }

            @Override
            public int getCount() {
                return 8; // Было 5, стало 6
            }
        };
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineWoodBurnerMenu(containerId, playerInventory, this, this.data);
    }
    
    // ==================== TICK LOGIC ====================
    
    public static void tick(Level level, BlockPos pos, BlockState state, MachineWoodBurnerBlockEntity entity) {
        if (level.isClientSide()) return;
        
        boolean wasLit = entity.isLit;
        
        // Проверяем, включен ли генератор
        if (!entity.enabled) {
            // Если выключен, тушим огонь
            if (entity.isLit) {
                entity.isLit = false;
                entity.burnTime = 0;
            }
        } else {
            // Проверяем, можем ли начать новое горение
            if (entity.burnTime <= 0 && entity.canBurn()) {
                entity.startBurning();
            }
            
            // Процесс горения
            if (entity.burnTime > 0) {
                entity.burnTime--;
                entity.isLit = true;
                
                // Генерируем энергию только если есть место в хранилище
                if (entity.energyStorage.getEnergyStored() < entity.energyStorage.getMaxEnergyStored()) {
                    entity.energyStorage.receiveEnergy(50, false);
                }
                
                // Когда топливо полностью сгорает
                if (entity.burnTime <= 0) {
                    entity.isLit = false;
                    entity.maxBurnTime = 0;
                    
                    // Шанс выпадения пепла 50%
                    if (level.random.nextFloat() < 0.5f) {
                        ItemStack ashSlotStack = entity.inventory.getStackInSlot(ASH_SLOT);
                        if (ashSlotStack.isEmpty()) {
                            entity.inventory.setStackInSlot(ASH_SLOT, new ItemStack(ModItems.WOOD_ASH_POWDER.get()));
                        } else if (ashSlotStack.getItem() == ModItems.WOOD_ASH_POWDER.get() && ashSlotStack.getCount() < 64) {
                            ashSlotStack.grow(1);
                        }
                    }
                }
            } else {
                entity.isLit = false;
            }
        }
        
        // Отдаём энергию через ENERGY_CONNECTOR части мультиблока
        entity.distributeEnergyToConnectors(level, pos, state);
        
        // Обновление энергетической дельты каждый тик
        entity.updateEnergyDelta(entity.energyStorage.getEnergyStored());
        
        // Обновляем состояние блока если изменилось
        if (wasLit != entity.isLit) {
            level.setBlock(pos, state.setValue(MachineWoodBurnerBlock.LIT, entity.isLit), 3);
        }
        
        entity.setChanged();
    }
    
    private void distributeEnergyToConnectors(Level level, BlockPos controllerPos, BlockState state) {
        Direction facing = state.getValue(MachineWoodBurnerBlock.FACING);

        // --- ИСПРАВЛЕНИЕ 1 (image_cc8d9a.png) ---
        long energyAvailable = this.energyStorage.extractEnergy(this.energyStorage.getMaxExtract(), true); // был int
        if (energyAvailable <= 0L) return; // 0L

        BlockPos[] connectorOffsets = {
            new BlockPos(0, 0, 1),
            new BlockPos(1, 0, 1)
        };

        java.util.UUID pushId = java.util.UUID.randomUUID();
        // --- ИСПРАВЛЕНИЕ 2 ---
        final long[] totalSent = {0L}; // был int[]

        for (BlockPos localOffset : connectorOffsets) {
            if (totalSent[0] >= energyAvailable) break;
            
            BlockPos worldOffset = rotateOffset(localOffset, facing);
            BlockPos connectorPos = controllerPos.offset(worldOffset);
            
            // Проверяем всех соседей этого коннектора
            for (Direction dir : Direction.values()) {
                if (totalSent[0] >= energyAvailable) break;
                
                BlockPos neighborPos = connectorPos.relative(dir);
                BlockEntity neighbor = level.getBlockEntity(neighborPos);
                
                if (neighbor == null) continue;

                // ... (проверки neighbor в порядке) ...

                // Если сосед — провод, используем acceptEnergy (твой long-метод)
                if (neighbor instanceof WireBlockEntity wire) {

                    // --- ИСПРАВЛЕНИЕ 3 (image_cc8dd4.png) ---
                    long remaining = energyAvailable - totalSent[0]; // был int
                    long accepted = wire.acceptEnergy(remaining, pushId, this.worldPosition); // был int
                    // ---

                    if (accepted > 0) {
                        this.energyStorage.extractEnergy(accepted, false);
                        totalSent[0] += accepted;
                        if (ModClothConfig.get().enableDebugLogging) {
                            MainRegistry.LOGGER.debug("[GENERATOR] Sent {} FE to wire at {} via connector {}",
                                accepted, neighborPos, connectorPos);
                        }
                    }
                    continue;
                }

                // Для других (старых int) устройств используем capability
                neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(storage -> {
                    if (storage.canReceive()) {

                        // --- ИСПРАВЛЕНИЕ 4 (Совместимость с int) ---
                        long remainingLong = energyAvailable - totalSent[0]; // Это long

                        // Безопасно усекаем long до int ПЕРЕД отправкой в старую систему
                        int remainingInt = (int) Math.min(Integer.MAX_VALUE, remainingLong);
                        if (remainingInt <= 0) return; // Нечего отправлять в int-систему

                        int accepted = storage.receiveEnergy(remainingInt, false); // Отправляем int
                        // ---

                        if (accepted > 0) {
                            this.energyStorage.extractEnergy(accepted, false); // Извлекаем long
                            totalSent[0] += accepted; // int безопасно расширяется до long
                            if (ModClothConfig.get().enableDebugLogging) {
                                MainRegistry.LOGGER.debug("[GENERATOR] Sent {} FE to {} at {} via connector {}",
                                    accepted, neighbor.getClass().getSimpleName(), neighborPos, connectorPos);
                            }
                        }
                    }
                });
            }
        }
    }
    
    private BlockPos rotateOffset(BlockPos local, Direction facing) {
        int x = local.getX();
        int y = local.getY();
        int z = local.getZ();
        
        return switch (facing) {
            case NORTH -> new BlockPos(x, y, z);
            case SOUTH -> new BlockPos(-x, y, -z);
            case WEST -> new BlockPos(z, y, -x);
            case EAST -> new BlockPos(-z, y, x);
            default -> local;
        };
    }
    
    private boolean canBurn() {
        ItemStack fuelStack = inventory.getStackInSlot(FUEL_SLOT);
        return !fuelStack.isEmpty() &&
               getBurnTime(fuelStack.getItem()) > 0 &&
               energyStorage.getEnergyStored() < energyStorage.getMaxEnergyStored();
    }
    
    private void startBurning() {
        ItemStack fuelStack = inventory.getStackInSlot(FUEL_SLOT);
        if (!fuelStack.isEmpty()) {
            maxBurnTime = getBurnTime(fuelStack.getItem()) * 20;
            burnTime = maxBurnTime;
            fuelStack.shrink(1);
        }
    }
    
    public int getBurnTime(Item item) {
        ItemStack stack = new ItemStack(item);
        
        // Запрещаем ведро лавы
        if (item == Items.LAVA_BUCKET) return 0;
        
        // Получаем ванильное время горения в тиках
        int vanillaBurnTime = net.minecraftforge.common.ForgeHooks.getBurnTime(stack, null);
        if (vanillaBurnTime <= 0) return 0;
        
        // Конвертируем тики в секунды (делим на 20)
        return (vanillaBurnTime / 20);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, @Nonnull Inventory pPlayerInventory, @Nonnull Player pPlayer) {
        return new MachineWoodBurnerMenu(pContainerId, pPlayerInventory, this, this.data); // ✅ Передаём this.data (с getCount() = 6)
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {

        // --- ИЗМЕНЕНИЕ ---

        // 1. Отвечаем на запросы о нашей long-системе
        if (cap == ModCapabilities.LONG_ENERGY) {
            return lazyLongEnergyHandler.cast();
        }

        // 2. Отвечаем на запросы о старой int-системе
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyForgeEnergyHandler.cast();
        }

        // 3. Обработка предметов (как и было)
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);

        // --- ИЗМЕНЕНИЕ ---
        // 1. Инициализируем наш long-capability
        this.lazyLongEnergyHandler = LazyOptional.of(() -> longEnergyWrapper);
        // 2. Создаем int-capability (Forge) путем оборачивания нашего long-capability
        this.lazyForgeEnergyHandler = this.lazyLongEnergyHandler.lazyMap(LongToForgeWrapper::new);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();

        // --- ИЗМЕНЕНИЕ ---
        this.lazyLongEnergyHandler.invalidate();
        this.lazyForgeEnergyHandler.invalidate();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.wood_burner");
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.put("inventory", itemHandler.serializeNBT());

        // --- ИСПРАВЛЕНИЕ ---
        pTag.putLong("energy", energyStorage.getEnergyStored()); // Было putInt
        // ---

        pTag.putInt("burnTime", burnTime);
        pTag.putInt("maxBurnTime", maxBurnTime);
        pTag.putBoolean("isLit", isLit);
        pTag.putBoolean("enabled", enabled);
    }

    @Override
    public void load(@Nonnull CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));

        // --- ИСПРАВЛЕНИЕ ---
        energyStorage.setEnergy(pTag.getLong("energy")); // Было getInt
        // ---

        burnTime = pTag.getInt("burnTime");
        maxBurnTime = pTag.getInt("maxBurnTime");
        isLit = pTag.getBoolean("isLit");
        enabled = pTag.getBoolean("enabled");
    }

    public void drops() {
        SimpleContainer container = new SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            container.setItem(i, inventory.getStackInSlot(i));
        }
        
        if (this.level != null) {
            Containers.dropContents(this.level, this.worldPosition, container);
        }
    }
    
    public int getComparatorPower() {
        return (int) Math.floor(((double)this.energyStorage.getEnergyStored() / 
            this.energyStorage.getMaxEnergyStored()) * 15.0);
    }
    
    public boolean isEnabled() {
        return this.enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.setChanged();
    }
    
    public BlockEntityEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }
    
    // ==================== NBT ====================
    
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); // ОБЯЗАТЕЛЬНО ПЕРВЫМ
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.putInt("burnTime", burnTime);
        tag.putInt("maxBurnTime", maxBurnTime);
        tag.putBoolean("isLit", isLit);
        tag.putBoolean("enabled", enabled);
    }
    
    @Override
    public void load(CompoundTag tag) {
        super.load(tag); // ОБЯЗАТЕЛЬНО ПЕРВЫМ
        energyStorage.setEnergy(tag.getInt("energy"));
        burnTime = tag.getInt("burnTime");
        maxBurnTime = tag.getInt("maxBurnTime");
        isLit = tag.getBoolean("isLit");
        enabled = tag.getBoolean("enabled");
    }
}
