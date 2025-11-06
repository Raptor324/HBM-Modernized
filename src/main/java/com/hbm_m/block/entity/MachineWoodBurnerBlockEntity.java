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
                long energy = MachineWoodBurnerBlockEntity.this.energyStorage.getEnergyStored();
                long maxEnergy = MachineWoodBurnerBlockEntity.this.energyStorage.getMaxEnergyStored();
                return switch (pIndex) {
                    // (ПРАВИЛЬНАЯ ЗАПИСЬ)
                    case 0 -> LongDataPacker.packHigh(energy);       // Энергия (старшие биты)
                    case 1 -> LongDataPacker.packLow(energy);        // Энергия (младшие биты)
                    case 2 -> LongDataPacker.packHigh(maxEnergy);    // Макс. Энергия (старшие биты)
                    case 3 -> LongDataPacker.packLow(maxEnergy);     // Макс. Энергия (младшие биты)
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
                    // Энергия (0-3) устанавливается только сервером, set() не нужен
                    case 4 -> MachineWoodBurnerBlockEntity.this.burnTime = pValue;
                    case 5 -> MachineWoodBurnerBlockEntity.this.maxBurnTime = pValue;
                    case 6 -> MachineWoodBurnerBlockEntity.this.isLit = pValue != 0;
                    case 7 -> { // (Логика кнопки)
                        if (pValue == -1) {
                            MachineWoodBurnerBlockEntity.this.enabled = !MachineWoodBurnerBlockEntity.this.enabled;
                            MachineWoodBurnerBlockEntity.this.setChanged();
                        } else {
                            MachineWoodBurnerBlockEntity.this.enabled = pValue != 0;
                        }
                    }
                }
            }

            @Override
            public int getCount() {
                return 8; // (ПРАВИЛЬНО)
            }
        };
    }

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, MachineWoodBurnerBlockEntity pBlockEntity) {
        if (pLevel.isClientSide()) return;

        boolean wasLit = pBlockEntity.isLit;

        // (Логика включения/выключения)
        if (!pBlockEntity.enabled) {
            if (pBlockEntity.isLit) {
                pBlockEntity.isLit = false;
                pBlockEntity.burnTime = 0;
            }
        } else {
            // (Остальная логика горения)
            if (pBlockEntity.burnTime <= 0 && pBlockEntity.canBurn()) {
                pBlockEntity.startBurning();
            }

            if (pBlockEntity.burnTime > 0) {
                pBlockEntity.burnTime--;
                pBlockEntity.isLit = true;

                if (pBlockEntity.energyStorage.getEnergyStored() < pBlockEntity.energyStorage.getMaxEnergyStored()) {
                    pBlockEntity.energyStorage.receiveEnergy(50, false);
                }

                if (pBlockEntity.burnTime <= 0) {
                    pBlockEntity.isLit = false;
                    pBlockEntity.maxBurnTime = 0;

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

        pBlockEntity.distributeEnergyToConnectors(pLevel, pPos, pState);

        if (wasLit != pBlockEntity.isLit) {
            pLevel.setBlock(pPos, pState.setValue(MachineWoodBurnerBlock.LIT, pBlockEntity.isLit), 3);
        }

        setChanged(pLevel, pPos, pState);
    }

    private void distributeEnergyToConnectors(Level level, BlockPos controllerPos, BlockState state) {
        Direction facing = state.getValue(MachineWoodBurnerBlock.FACING);

        long energyAvailable = this.energyStorage.extractEnergy(this.energyStorage.getMaxExtract(), true);
        if (energyAvailable <= 0L) return;

        BlockPos[] connectorOffsets = {
                new BlockPos(0, 0, 1),
                new BlockPos(1, 0, 1)
        };

        java.util.UUID pushId = java.util.UUID.randomUUID();
        final long[] totalSent = {0L};

        for (BlockPos localOffset : connectorOffsets) {
            if (totalSent[0] >= energyAvailable) break;

            BlockPos worldOffset = rotateOffset(localOffset, facing);
            BlockPos connectorPos = controllerPos.offset(worldOffset);

            for (Direction dir : Direction.values()) {
                if (totalSent[0] >= energyAvailable) break;

                BlockPos neighborPos = connectorPos.relative(dir);
                BlockEntity neighbor = level.getBlockEntity(neighborPos);
                if (neighbor == null) continue;

                // (Пропуск других частей)
                if (neighbor instanceof UniversalMachinePartBlockEntity ||
                        neighbor instanceof MachineWoodBurnerBlockEntity) {
                    continue;
                }

                // (Отправка проводам - long)
                if (neighbor instanceof WireBlockEntity wire) {
                    long remaining = energyAvailable - totalSent[0];
                    long accepted = wire.acceptEnergy(remaining, pushId, this.worldPosition);
                    if (accepted > 0) {
                        this.energyStorage.extractEnergy(accepted, false);
                        totalSent[0] += accepted;
                        // (debug log)
                    }
                    continue;
                }

                // (Отправка другим - int)
                neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite()).ifPresent(storage -> {
                    if (storage.canReceive()) {
                        long remainingLong = energyAvailable - totalSent[0];
                        int remainingInt = (int) Math.min(Integer.MAX_VALUE, remainingLong);
                        if (remainingInt <= 0) return;

                        int accepted = storage.receiveEnergy(remainingInt, false);
                        if (accepted > 0) {
                            this.energyStorage.extractEnergy(accepted, false);
                            totalSent[0] += accepted;
                            // (debug log)
                        }
                    }
                });
            }
        }
    }

    // (вспомогательные методы: rotateOffset, canBurn, startBurning, getBurnTime)
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
        if (item == Items.LAVA_BUCKET) return 0;
        int vanillaBurnTime = net.minecraftforge.common.ForgeHooks.getBurnTime(stack, null);
        if (vanillaBurnTime <= 0) return 0;
        return (vanillaBurnTime / 20);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, @Nonnull Inventory pPlayerInventory, @Nonnull Player pPlayer) {
        return new MachineWoodBurnerMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ModCapabilities.LONG_ENERGY) {
            return lazyLongEnergyHandler.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return lazyForgeEnergyHandler.cast();
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
        this.lazyLongEnergyHandler = LazyOptional.of(() -> longEnergyWrapper);
        this.lazyForgeEnergyHandler = this.lazyLongEnergyHandler.lazyMap(LongToForgeWrapper::new);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
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
        pTag.putLong("energy", energyStorage.getEnergyStored()); // (long)
        pTag.putInt("burnTime", burnTime);
        pTag.putInt("maxBurnTime", maxBurnTime);
        pTag.putBoolean("isLit", isLit);
        pTag.putBoolean("enabled", enabled);
    }

    @Override
    public void load(@Nonnull CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        energyStorage.setEnergy(pTag.getLong("energy")); // (long)
        burnTime = pTag.getInt("burnTime");
        maxBurnTime = pTag.getInt("maxBurnTime");
        isLit = pTag.getBoolean("isLit");
        enabled = pTag.getBoolean("enabled");
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

    public BlockEntityEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }
}