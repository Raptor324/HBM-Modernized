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
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
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
import java.util.Map;

import static com.hbm_m.item.ModItems.ITEMS;

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
    private boolean enabled = true;

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
                    case 5 -> MachineWoodBurnerBlockEntity.this.enabled ? 1 : 0;
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
                    case 5 -> {
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
            }

            @Override
            public int getCount() {
                return 6; // Было 5, стало 6
            }
        };
    }

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, MachineWoodBurnerBlockEntity pBlockEntity) {
        if (pLevel.isClientSide()) return;

        boolean wasLit = pBlockEntity.isLit;

        // НОВОЕ: Проверяем, включен ли генератор
        if (!pBlockEntity.enabled) {
            // Если выключен, тушим огонь
            if (pBlockEntity.isLit) {
                pBlockEntity.isLit = false;
                pBlockEntity.burnTime = 0;
            }
        } else {
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
                            pBlockEntity.itemHandler.setStackInSlot(ASH_SLOT, new ItemStack(ModItems.WOOD_ASH_POWDER.get()));
                        } else if (ashSlotStack.getItem() == ModItems.WOOD_ASH_POWDER.get() && ashSlotStack.getCount() < 64) {
                            ashSlotStack.grow(1);
                        }
                    }
                }
            } else {
                pBlockEntity.isLit = false;
            }
        }

        // Отдаём энергию через ENERGY_CONNECTOR части мультиблока
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
    public int getBurnTime(Item item) {
        ItemStack stack = new ItemStack(item);

        // КАСТОМНОЕ ТОПЛИВО
        if (item == ModItems.LIGNITE.get()) return 30; // 400 тиков / 20 = 20 секунд

        // УГОЛЬ
        if (item == Items.COAL || item == Items.CHARCOAL) return 40; // 1600 тиков

        // ТЕГИ - автоматически покрывают все типы дерева
        if (stack.is(ItemTags.LOGS) || stack.is(ItemTags.LOGS_THAT_BURN)) return 30;
        if (stack.is(ItemTags.PLANKS)) return 15; 

        // Деревянные строительные блоки (используем ItemTags вместо BlockTags)
        if (stack.is(ItemTags.WOODEN_STAIRS)) return 15; // 300 тиков
        if (stack.is(ItemTags.WOODEN_SLABS)) return 8; // 150 тиков
        if (stack.is(ItemTags.WOODEN_FENCES)) return 15; // 300 тиков
        if (stack.is(ItemTags.WOODEN_TRAPDOORS)) return 15; // 300 тиков
        if (stack.is(ItemTags.WOODEN_DOORS)) return 10; // 200 тиков
        if (stack.is(ItemTags.WOODEN_BUTTONS)) return 5; // 100 тиков
        if (stack.is(ItemTags.WOODEN_PRESSURE_PLATES)) return 15; // 300 тиков

        // Таблички
        if (stack.is(ItemTags.SIGNS)) return 10; // 200 тиков
        if (stack.is(ItemTags.HANGING_SIGNS)) return 40; // 800 тиков

        // Лодки
        if (stack.is(ItemTags.BOATS)) return 60; // 1200 тиков
        if (stack.is(ItemTags.CHEST_BOATS)) return 60; // 1200 тиков

        // Мелочи
        if (item == Items.STICK) return 5; // 100 тиков
        if (item == Items.BOWL) return 5; // 100 тиков
        if (item == Items.BAMBOO) return 3; // 50 тиков

        // Деревянные инструменты
        if (item == Items.WOODEN_SWORD || item == Items.WOODEN_PICKAXE ||
                item == Items.WOODEN_AXE || item == Items.WOODEN_SHOVEL ||
                item == Items.WOODEN_HOE) return 10; // 200 тиков

        // Прочие деревянные блоки
        if (item == Items.CRAFTING_TABLE || item == Items.CARTOGRAPHY_TABLE ||
                item == Items.FLETCHING_TABLE || item == Items.SMITHING_TABLE ||
                item == Items.LOOM || item == Items.BARREL || item == Items.COMPOSTER ||
                item == Items.CHEST || item == Items.TRAPPED_CHEST ||
                item == Items.BOOKSHELF || item == Items.CHISELED_BOOKSHELF ||
                item == Items.LECTERN || item == Items.NOTE_BLOCK ||
                item == Items.JUKEBOX || item == Items.DAYLIGHT_DETECTOR ||
                item == Items.LADDER) return 15; // 300 тиков

        // Прочее топливо
        if (item == Items.BLAZE_ROD) return 120; // 2400 тиков
        if (item == Items.DRIED_KELP_BLOCK) return 200; // 4000 тиков

        return 0;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, @Nonnull Inventory pPlayerInventory, @Nonnull Player pPlayer) {
        return new MachineWoodBurnerMenu(pContainerId, pPlayerInventory, this, this.data); // ✅ Передаём this.data (с getCount() = 6)
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
        pTag.putBoolean("enabled", enabled); // НОВОЕ
    }

    @Override
    public void load(@Nonnull CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        energyStorage.setEnergy(pTag.getInt("energy"));
        burnTime = pTag.getInt("burnTime");
        maxBurnTime = pTag.getInt("maxBurnTime");
        isLit = pTag.getBoolean("isLit");
        enabled = pTag.getBoolean("enabled"); // НОВОЕ
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

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.setChanged();
    }

    // Публичный метод для доступа к энергохранилищу (для частей мультиблока)
    public BlockEntityEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }
}