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
import java.util.concurrent.atomic.AtomicInteger;

public class MachineWoodBurnerBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // ВАЖНО: maxExtract > 0 чтобы провода могли извлекать энергию
    // maxReceive > 0 для внутренней генерации (receiveEnergy в tick)
    private final BlockEntityEnergyStorage energyStorage = new BlockEntityEnergyStorage(100000, 1000, 1000);
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    // Обёртка для IENERGYSTORAGE, которая запрещает приём энергии ИЗВНЕ
    private final IEnergyStorage energyWrapper = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            // Генератор НЕ принимает энергию от внешних источников
            return 0;
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
            // Генератор НЕ принимает энергию от внешних источников
            return false;
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

        // ДОБАВЛЕНО: Активно отдаём энергию соседям (как у батареи)
        pBlockEntity.pushEnergyToNeighbors();

        // Обновляем состояние блока если изменилось
        if (wasLit != pBlockEntity.isLit) {
            pLevel.setBlock(pPos, pState.setValue(MachineWoodBurnerBlock.LIT, pBlockEntity.isLit), 3);
        }

        setChanged(pLevel, pPos, pState);
    }

    // НОВЫЙ МЕТОД: Активная передача энергии соседям
    private void pushEnergyToNeighbors() {
        if (ModClothConfig.get().enableDebugLogging) {
            MainRegistry.LOGGER.debug("[GENERATOR >>>] pushEnergyToNeighbors at {} currentEnergy={}", this.worldPosition, this.energyStorage.getEnergyStored());
        }

        AtomicInteger energyToSend = new AtomicInteger(this.energyStorage.extractEnergy(this.energyStorage.getMaxExtract(), true));

        if (energyToSend.get() <= 0) {
            if (ModClothConfig.get().enableDebugLogging) {
                MainRegistry.LOGGER.debug("[GENERATOR >>>] No energy to send");
                // Отправляем в чат ближайшему игроку
                sendDebugToNearbyPlayers("§e[GEN] No energy to send");
            }
            return; // Нечего отправлять
        }

        if (ModClothConfig.get().enableDebugLogging) {
            sendDebugToNearbyPlayers("§a[GEN] Trying to send " + energyToSend + " FE");
        }

        Level lvl = this.level;
        if (lvl == null) return;

        for (Direction direction : Direction.values()) {
            if (energyToSend.get() <= 0) {
                break; // Вся энергия роздана
            }

            BlockPos neighborPos = worldPosition.relative(direction);
            BlockEntity neighbor = lvl.getBlockEntity(neighborPos);
            if (neighbor == null) {
                continue;
            }

            // Пропускаем самого себя и другие генераторы
            if (neighbor instanceof MachineWoodBurnerBlockEntity) {
                if (ModClothConfig.get().enableDebugLogging) {
                    MainRegistry.LOGGER.debug("[GENERATOR >>>] Skipping generator at {}", neighborPos);
                    sendDebugToNearbyPlayers("§c[GEN] Skipping generator at " + direction);
                }
                continue;
            }

            // Получаем capability соседа
            LazyOptional<IEnergyStorage> neighborCapability = neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite());

            if (neighborCapability.isPresent()) {
                neighborCapability.ifPresent(neighborStorage -> {
                    if (neighborStorage.canReceive()) {
                        int accepted = neighborStorage.receiveEnergy(energyToSend.get(), false);
                        if (accepted > 0) {
                            this.energyStorage.extractEnergy(accepted, false);
                            energyToSend.addAndGet(-accepted);
                            if (ModClothConfig.get().enableDebugLogging) {
                                MainRegistry.LOGGER.debug("[GENERATOR >>>] Sent {} FE to {} (direction: {})", accepted, neighborPos, direction);
                                sendDebugToNearbyPlayers("§a[GEN] Sent " + accepted + " FE to " + direction + " -> " + neighbor.getClass().getSimpleName());
                            }
                        }
                    } else if (ModClothConfig.get().enableDebugLogging) {
                        sendDebugToNearbyPlayers("§c[GEN] Neighbor at " + direction + " can't receive (" + neighbor.getClass().getSimpleName() + ")");
                    }
                });
            } else if (ModClothConfig.get().enableDebugLogging) {
                sendDebugToNearbyPlayers("§7[GEN] No energy capability at " + direction + " (" + neighbor.getClass().getSimpleName() + ")");
            }
        }
    }

    // Вспомогательный метод для отправки debug сообщений в чат
    private void sendDebugToNearbyPlayers(String message) {
        if (this.level == null) return;
        this.level.players().forEach(player -> {
            if (player.blockPosition().distSqr(this.worldPosition) < 256) { // В радиусе 16 блоков
                player.sendSystemMessage(Component.literal(message));
            }
        });
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
        lazyEnergyHandler = LazyOptional.of(() -> energyWrapper); // Используем обёртку вместо прямого energyStorage
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
}