package com.hbm_m.block.entity;

import com.hbm_m.block.MachineWoodBurnerBlock;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.energy.BlockEntityEnergyStorage;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.menu.MachineWoodBurnerMenu;
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
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class MachineWoodBurnerBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final BlockEntityEnergyStorage energyStorage = new BlockEntityEnergyStorage(100000, 1000, 1000);
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    // Обёртка для IENERGYSTORAGE, которая запрещает приём энергии ИЗВНЕ
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

    protected final ContainerData data;
    private int burnTime = 0;
    private int maxBurnTime = 0;
    private boolean isLit = false;

    private static final int FUEL_SLOT = 0;
    private static final int ASH_SLOT = 1;

    public MachineWoodBurnerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.WOOD_BURNER_BE.get(), pPos, pBlockState);

        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> MachineWoodBurnerBlockEntity.this.energyStorage.getEnergyStored();
                    case 1 -> MachineWoodBurnerBlockEntity.this.energyStorage.getMaxEnergyStored();
                    case 2 -> MachineWoodBurnerBlockEntity.this.burnTime;
                    case 3 -> MachineWoodBurnerBlockEntity.this.maxBurnTime;
                    case 4 -> MachineWoodBurnerBlockEntity.this.isLit ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> MachineWoodBurnerBlockEntity.this.energyStorage.setEnergy(pValue);
                    case 2 -> MachineWoodBurnerBlockEntity.this.burnTime = pValue;
                    case 3 -> MachineWoodBurnerBlockEntity.this.maxBurnTime = pValue;
                    case 4 -> MachineWoodBurnerBlockEntity.this.isLit = pValue != 0;
                }
            }

            @Override
            public int getCount() {
                return 5;
            }
        };
    }

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, MachineWoodBurnerBlockEntity pBlockEntity) {
        if (pLevel.isClientSide()) return;

        boolean wasLit = pBlockEntity.isLit;

        // Проверяем, можем ли начать новое горение
        if (pBlockEntity.burnTime <= 0 && pBlockEntity.canBurn()) {
            pBlockEntity.startBurning();
        }

        // Процесс горения
        if (pBlockEntity.burnTime > 0) {
            pBlockEntity.burnTime--;
            pBlockEntity.isLit = true;

            // Генерируем энергию только если есть место в хранилище
            if (pBlockEntity.energyStorage.getEnergyStored() < pBlockEntity.energyStorage.getMaxEnergyStored()) {
                pBlockEntity.energyStorage.receiveEnergy(50, false);
            }

            // Когда топливо полностью сгорает
            if (pBlockEntity.burnTime <= 0) {
                pBlockEntity.isLit = false;
                pBlockEntity.maxBurnTime = 0;

                // Шанс выпадения пепла 50%
                if (pLevel.random.nextFloat() < 0.5f) {
                    ItemStack ashSlotStack = pBlockEntity.itemHandler.getStackInSlot(ASH_SLOT);
                    if (ashSlotStack.isEmpty()) {
                        pBlockEntity.itemHandler.setStackInSlot(ASH_SLOT, new ItemStack(Items.GUNPOWDER));
                    } else if (ashSlotStack.getItem() == Items.GUNPOWDER && ashSlotStack.getCount() < 64) {
                        ashSlotStack.grow(1);
                    }
                }
            }
        } else {
            pBlockEntity.isLit = false;
        }

        // НОВОЕ: Отдаём энергию через ENERGY_CONNECTOR части мультиблока
        pBlockEntity.distributeEnergyToConnectors(pLevel, pPos, pState);

        // Обновляем состояние блока если изменилось
        if (wasLit != pBlockEntity.isLit) {
            pLevel.setBlock(pPos, pState.setValue(MachineWoodBurnerBlock.LIT, pBlockEntity.isLit), 3);
        }

        setChanged(pLevel, pPos, pState);
    }

    // НОВЫЙ МЕТОД: Распределение энергии через части мультиблока (унифицировано с батареей)
    private void distributeEnergyToConnectors(Level level, BlockPos controllerPos, BlockState state) {
        Direction facing = state.getValue(MachineWoodBurnerBlock.FACING);

        int energyAvailable = this.energyStorage.extractEnergy(this.energyStorage.getMaxExtract(), true);
        if (energyAvailable <= 0) return;

        // Получаем позиции ENERGY_CONNECTOR частей (задние нижние блоки)
        BlockPos[] connectorOffsets = {
                new BlockPos(0, 0, 1),
                new BlockPos(1, 0, 1)
        };

        java.util.UUID pushId = java.util.UUID.randomUUID();
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

    // Вспомогательный метод для поворота локальных координат
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
        ItemStack fuelStack = itemHandler.getStackInSlot(FUEL_SLOT);
        return !fuelStack.isEmpty() &&
                getBurnTime(fuelStack.getItem()) > 0 &&
                energyStorage.getEnergyStored() < energyStorage.getMaxEnergyStored();
    }

    private void startBurning() {
        ItemStack fuelStack = itemHandler.getStackInSlot(FUEL_SLOT);
        if (!fuelStack.isEmpty()) {
            maxBurnTime = getBurnTime(fuelStack.getItem()) * 20;
            burnTime = maxBurnTime;
            fuelStack.shrink(1);
        }
    }

    private int getBurnTime(Item item) {
        if (item == Items.OAK_PLANKS || item == Items.BIRCH_PLANKS || item == Items.SPRUCE_PLANKS ||
                item == Items.JUNGLE_PLANKS || item == Items.ACACIA_PLANKS || item == Items.DARK_OAK_PLANKS ||
                item == Items.MANGROVE_PLANKS || item == Items.CHERRY_PLANKS || item == Items.BAMBOO_PLANKS ||
                item == Items.CRIMSON_PLANKS || item == Items.WARPED_PLANKS) {
            return 30;
        }
        if (item == Items.OAK_LOG || item == Items.BIRCH_LOG || item == Items.SPRUCE_LOG ||
                item == Items.JUNGLE_LOG || item == Items.ACACIA_LOG || item == Items.DARK_OAK_LOG ||
                item == Items.MANGROVE_LOG || item == Items.CHERRY_LOG || item == Items.BAMBOO_BLOCK ||
                item == Items.CRIMSON_STEM || item == Items.WARPED_STEM ||
                item == Items.STRIPPED_OAK_LOG || item == Items.STRIPPED_BIRCH_LOG || item == Items.STRIPPED_SPRUCE_LOG ||
                item == Items.STRIPPED_JUNGLE_LOG || item == Items.STRIPPED_ACACIA_LOG || item == Items.STRIPPED_DARK_OAK_LOG ||
                item == Items.STRIPPED_MANGROVE_LOG || item == Items.STRIPPED_CHERRY_LOG ||
                item == Items.STRIPPED_CRIMSON_STEM || item == Items.STRIPPED_WARPED_STEM) {
            return 60;
        }
        if (item == Items.BROWN_MUSHROOM || item == Items.RED_MUSHROOM) {
            return 80;
        }
        if (item == Items.COAL) {
            return 120;
        }
        return 0;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, @Nonnull Inventory pPlayerInventory, @Nonnull Player pPlayer) {
        return new MachineWoodBurnerMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return lazyEnergyHandler.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        lazyEnergyHandler = LazyOptional.of(() -> energyWrapper);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyEnergyHandler.invalidate();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.wood_burner");
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.putInt("energy", energyStorage.getEnergyStored());
        pTag.putInt("burnTime", burnTime);
        pTag.putInt("maxBurnTime", maxBurnTime);
        pTag.putBoolean("isLit", isLit);
    }

    @Override
    public void load(@Nonnull CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        energyStorage.setEnergy(pTag.getInt("energy"));
        burnTime = pTag.getInt("burnTime");
        maxBurnTime = pTag.getInt("maxBurnTime");
        isLit = pTag.getBoolean("isLit");
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        if (this.level != null) {
            Containers.dropContents(this.level, this.worldPosition, inventory);
        }
    }

    public int getComparatorPower() {
        return (int) Math.floor(((double) this.energyStorage.getEnergyStored() / this.energyStorage.getMaxEnergyStored()) * 15.0);
    }

    // Публичный метод для доступа к энергохранилищу (для частей мультиблока)
    public BlockEntityEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }
}