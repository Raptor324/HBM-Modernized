package com.hbm_m.block.entity;

import com.hbm_m.block.MachineWoodBurnerBlock;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.energy.BlockEntityEnergyStorage;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class MachineWoodBurnerBlockEntity extends BaseMachineBlockEntity {
    
    // Слоты
    private static final int SLOT_COUNT = 2;
    private static final int FUEL_SLOT = 0;
    private static final int ASH_SLOT = 1;
    
    // Энергия
    private final BlockEntityEnergyStorage energyStorage = new BlockEntityEnergyStorage(100_000, 1000, 1000);
    
    // Обертка для IEnergyStorage, которая запрещает приём энергии извне
    private final IEnergyStorage energyWrapper = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0; // Генератор НЕ принимает энергию от внешних источников
        }
        
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return energyStorage.extractEnergy(maxExtract, simulate);
        }
        
        @Override
        public int getEnergyStored() {
            return energyStorage.getEnergyStored();
        }
        
        @Override
        public int getMaxEnergyStored() {
            return energyStorage.getMaxEnergyStored();
        }
        
        @Override
        public boolean canExtract() {
            return energyStorage.canExtract();
        }
        
        @Override
        public boolean canReceive() {
            return false; // Генератор НЕ принимает энергию от внешних источников
        }
    };
    
    // Переменные состояния
    private int burnTime = 0;
    private int maxBurnTime = 0;
    private boolean isLit = false;
    private boolean enabled = true;
    
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> energyStorage.getEnergyStored();
                case 1 -> energyStorage.getMaxEnergyStored();
                case 2 -> burnTime;
                case 3 -> maxBurnTime;
                case 4 -> isLit ? 1 : 0;
                case 5 -> enabled ? 1 : 0;
                case 6 -> energyDelta;
                default -> 0;
            };
        }
        
        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> energyStorage.setEnergy(value);
                case 2 -> burnTime = value;
                case 3 -> maxBurnTime = value;
                case 4 -> isLit = value != 0;
                case 5 -> {
                    if (value == -1) {
                        // Специальное значение -1 означает "переключить"
                        enabled = !enabled;
                        setChanged();
                    } else {
                        enabled = value != 0;
                    }
                }
                case 6 -> energyDelta = value;
            }
        }
        
        @Override
        public int getCount() {
            return 7;
        }
    };
    
    public MachineWoodBurnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WOOD_BURNER_BE.get(), pos, state, SLOT_COUNT);
    }
    
    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.wood_burner");
    }
    
    @Override
    protected void setupEnergyCapability() {
        // Используем обертку вместо прямого energyStorage
        energyHandler = LazyOptional.of(() -> energyWrapper);
    }
    
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case FUEL_SLOT -> getBurnTime(stack.getItem()) > 0;
            case ASH_SLOT -> false; // Только для вывода
            default -> false;
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
        int energyAvailable = this.energyStorage.extractEnergy(this.energyStorage.getMaxExtract(), true);
        
        if (energyAvailable <= 0) return;
        
        // Получаем позиции ENERGY_CONNECTOR частей (задние нижние блоки)
        BlockPos[] connectorOffsets = {
            new BlockPos(0, 0, 1),
            new BlockPos(1, 0, 1)
        };
        
        UUID pushId = UUID.randomUUID();
        final int[] totalSent = {0};
        
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
                
                // Пропускаем части мультиблока и другие генераторы
                if (neighbor instanceof UniversalMachinePartBlockEntity ||
                    neighbor instanceof MachineWoodBurnerBlockEntity) {
                    continue;
                }
                
                // Если сосед — провод, используем acceptEnergy как у батареи
                if (neighbor instanceof WireBlockEntity wire) {
                    int remaining = energyAvailable - totalSent[0];
                    int accepted = wire.acceptEnergy(remaining, pushId, this.worldPosition);
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
                
                // Для других устройств используем capability
                neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(storage -> {
                    if (storage.canReceive()) {
                        int remaining = energyAvailable - totalSent[0];
                        int accepted = storage.receiveEnergy(remaining, false);
                        if (accepted > 0) {
                            this.energyStorage.extractEnergy(accepted, false);
                            totalSent[0] += accepted;
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
    
    // ==================== UTILITY ====================
    
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
