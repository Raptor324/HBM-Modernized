package com.hbm_m.block.entity;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.block.MachineAssemblerPartBlock;
import com.hbm_m.multiblock.IMultiblockPart;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import java.util.Objects;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

public class MachineAssemblerPartBlockEntity extends BlockEntity implements IMultiblockPart {
        
    private BlockPos controllerPos;

    private final LazyOptional<IEnergyStorage> energyProxy = LazyOptional.of(this::createEnergyProxy);
    // Прокси для взаимодействия с предметами через конвейер (вставка в входы, извлечение из выхода)
    private final LazyOptional<net.minecraftforge.items.IItemHandler> itemProxy = LazyOptional.of(this::createItemProxy);

    public MachineAssemblerPartBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.MACHINE_ASSEMBLER_PART_BE.get(), pPos, pBlockState);
    }

    public void setControllerPos(BlockPos controllerPos) {
        this.controllerPos = controllerPos;
    }

    public BlockPos getControllerPos() {
        return this.controllerPos;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag nbt = super.getUpdateTag();
        if (this.controllerPos != null) {
            nbt.put("controller", NbtUtils.writeBlockPos(this.controllerPos));
        }
        return nbt;
    }

    /**
     * Создает пакет для отправки данных клиенту.
     * Внутри он использует getUpdateTag() для получения данных.
     */
    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // --- СОХРАНЕНИЕ/ЗАГРУЗКА НА ДИСК И КЛИЕНТЕ ---

    @Override
    protected void saveAdditional(@Nonnull CompoundTag nbt) {
        if (this.controllerPos != null) {
            nbt.put("controller", NbtUtils.writeBlockPos(this.controllerPos));
        }
        super.saveAdditional(nbt);
    }

    /**
     * Этот метод вызывается как на сервере при загрузке мира,
     * так и на клиенте при получении пакета с данными блока.
     */
    @Override
    public void load(@Nonnull CompoundTag nbt) {
        super.load(nbt);
        if (nbt.contains("controller")) {
            this.controllerPos = NbtUtils.readBlockPos(nbt.getCompound("controller"));
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        // Мы предоставляем Capability только если это коннектор, чтобы другие части машины не принимали энергию
        if (cap == ForgeCapabilities.ENERGY && isEnergyConnector()) {
            return energyProxy.cast();
        }
        // Конвейерные части предоставляют прокси для вставки/извлечения предметов
        if (cap == ForgeCapabilities.ITEM_HANDLER && isConveyorConnector()) {
            return itemProxy.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyProxy.invalidate();
    itemProxy.invalidate();
    }
    
    /**
     * КЛЮЧЕВОЕ ИЗМЕНЕНИЕ: Создаем простой прокси, который ТОЛЬКО принимает энергию.
     */
    private IEnergyStorage createEnergyProxy() {
        return new IEnergyStorage() {
            /**
             * Когда в ЭТУ ЧАСТЬ (коннектор) пытаются "затолкнуть" энергию (PUSH).
             * Это единственный рабочий метод.
             */
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                if (level == null || controllerPos == null) return 0;
                Level lvl = Objects.requireNonNull(level);
                BlockEntity controllerBE = lvl.getBlockEntity(controllerPos);
                
                // Находим контроллер и просто перенаправляем энергию в него
                if (controllerBE instanceof MachineAssemblerBlockEntity assembler) {
                    return assembler.getCapability(ForgeCapabilities.ENERGY)
                                    .map(storage -> storage.receiveEnergy(maxReceive, simulate))
                                    .orElse(0);
                }
                return 0;
            }
            
            /**
             * Часть мультиблока не может сама отдавать энергию. Этим занимается контроллер.
             * Поэтому здесь всегда возвращаем 0.
             */
            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                return 0;
            }

            @Override
            public boolean canExtract() {
                // Нельзя извлекать из части.
                return false;
            }

            @Override
            public boolean canReceive() {
                // Можно принимать энергию (чтобы передать контроллеру).
                return true;
            }

            // Эти методы просто отражают состояние контроллера для UI (например, WAILA/Jade)
            @Override
            public int getEnergyStored() {
                if (level == null || controllerPos == null) return 0;
                return getControllerCapability().map(IEnergyStorage::getEnergyStored).orElse(0);
            }

            @Override
            public int getMaxEnergyStored() {
                if (level == null || controllerPos == null) return 0;
                return getControllerCapability().map(IEnergyStorage::getMaxEnergyStored).orElse(0);
            }

            // Хелпер для краткости
            private LazyOptional<IEnergyStorage> getControllerCapability() {
                if (level == null || controllerPos == null) return LazyOptional.empty();
                Level lvl = Objects.requireNonNull(level);
                BlockEntity be = lvl.getBlockEntity(controllerPos);
                if (be != null) {
                    return be.getCapability(ForgeCapabilities.ENERGY);
                }
                return LazyOptional.empty();
            }
        };
    }

    /**
     * Проксирующий ItemHandler для конвейерных частей.
     * Вставка: пытаемся добавить в входные слоты контроллера (6..17).
     * Извлечение: только из выходного слота контроллера (5).
     */
    private net.minecraftforge.items.IItemHandler createItemProxy() {
        return new net.minecraftforge.items.IItemHandler() {
            @Override
            public int getSlots() {
                return 1; // интерфейс для внешних взаимодействий — мы проксируем к контроллеру
            }

            @Override
            public net.minecraft.world.item.ItemStack getStackInSlot(int slot) {
                if (level == null || controllerPos == null) return net.minecraft.world.item.ItemStack.EMPTY;
                Level lvl = Objects.requireNonNull(level);
                BlockEntity be = lvl.getBlockEntity(controllerPos);
                if (be == null) return net.minecraft.world.item.ItemStack.EMPTY;
                return be.getCapability(ForgeCapabilities.ITEM_HANDLER).map(h -> h.getStackInSlot(5)).orElse(net.minecraft.world.item.ItemStack.EMPTY);
            }

            @Override
            public net.minecraft.world.item.ItemStack insertItem(int slot, net.minecraft.world.item.ItemStack stack, boolean simulate) {
                // Если это выходная часть — не разрешаем вставлять (выход только для получения продуктов)
                BlockState state = getBlockState();
                int offsetX = state.getValue(MachineAssemblerPartBlock.OFFSET_X);
                boolean isInput = (offsetX == 0);
                if (!isInput) return stack;

                if (stack.isEmpty() || level == null || controllerPos == null) return stack;
                Level lvl = Objects.requireNonNull(level);
                BlockEntity be = lvl.getBlockEntity(controllerPos);
                if (be == null) return stack;
                return be.getCapability(ForgeCapabilities.ITEM_HANDLER).map(h -> {
                    net.minecraft.world.item.ItemStack remaining = stack.copy();
                    // try to insert into input slots 6..17
                    for (int i = 6; i <= 17; i++) {
                        if (remaining.isEmpty()) break;
                        remaining = h.insertItem(i, remaining, simulate);
                    }
                    return remaining;
                }).orElse(stack);
            }

            @Override
            public net.minecraft.world.item.ItemStack extractItem(int slot, int amount, boolean simulate) {
                // Если это входная часть — не разрешаем извлекать из неё (вход только для подачи ресурсов)
                BlockState state = getBlockState();
                int offsetX = state.getValue(MachineAssemblerPartBlock.OFFSET_X);
                boolean isInput = (offsetX == 0);
                if (isInput) return net.minecraft.world.item.ItemStack.EMPTY;

                if (level == null || controllerPos == null) return net.minecraft.world.item.ItemStack.EMPTY;
                Level lvl = Objects.requireNonNull(level);
                BlockEntity be = lvl.getBlockEntity(controllerPos);
                if (be == null) return net.minecraft.world.item.ItemStack.EMPTY;
                return be.getCapability(ForgeCapabilities.ITEM_HANDLER).map(h -> {
                    // Try to extract from output slot only
                    net.minecraft.world.item.ItemStack out = h.extractItem(5, amount, simulate);
                    if (!out.isEmpty()) return out;
                    return net.minecraft.world.item.ItemStack.EMPTY;
                }).orElse(net.minecraft.world.item.ItemStack.EMPTY);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }

            @Override
            public boolean isItemValid(int slot, net.minecraft.world.item.ItemStack stack) {
                // Allow any item by default; controller will decide if it can be inserted into particular input slots
                return true;
            }
        };
    }

    /**
     * Возвращает true если эта часть — часть конвейера (левый/правый), т.е. должна принимать/отдавать предметы.
     * Логика: offsetY == 0 и offsetX == 0 || 3 (крайние X) и offsetZ == 1 || 2 (центральные по Z).
     */
    public boolean isConveyorConnector() {
        BlockState state = this.getBlockState();

        int offsetY = state.getValue(MachineAssemblerPartBlock.OFFSET_Y);
        int offsetX = state.getValue(MachineAssemblerPartBlock.OFFSET_X);
        int offsetZ = state.getValue(MachineAssemblerPartBlock.OFFSET_Z);

        if (offsetY != 0) return false;

        boolean isSideX = (offsetX == 0 || offsetX == 3);
        boolean isCenterZ = (offsetZ == 1 || offsetZ == 2);

        return isSideX && isCenterZ;
    }
    
    public boolean isEnergyConnector() {
        BlockState state = this.getBlockState();
        
        int offsetY = state.getValue(MachineAssemblerPartBlock.OFFSET_Y);
        int offsetX = state.getValue(MachineAssemblerPartBlock.OFFSET_X);
        int offsetZ = state.getValue(MachineAssemblerPartBlock.OFFSET_Z);

        if (offsetY != 0) {
            return false;
        }

        boolean isCentralX = (offsetX == 1 || offsetX == 2);
        boolean isExtremeZ = (offsetZ == 0 || offsetZ == 3);
        
        boolean result = isCentralX && isExtremeZ;
        
        // --- ЛОГ: Проверка коннектора ---
        // Закомментируйте это после отладки, иначе будет спамить в консоль
        // MainRegistry.LOGGER.info("[PART @ {}] isEnergyConnector check: Y={}, X={}, Z={}. Result: {}", this.worldPosition, offsetY, offsetX, offsetZ, result);

        return result;
    }
}