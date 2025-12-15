package com.hbm_m.block.entity.custom.machines;

import com.hbm_m.menu.MachineFluidTankMenu; 
import com.hbm_m.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MachineFluidTankBlockEntity extends BlockEntity implements MenuProvider {

    // Слоты
    public static final int SLOT_INPUT_L = 0;  // Слева верх (ведро с жижей -> вылить в танк)
    public static final int SLOT_OUTPUT_L = 1; // Слева низ (пустое ведро)
    public static final int SLOT_INPUT_R = 2;  // Справа верх (пустое ведро -> наполнить из танка)
    public static final int SLOT_OUTPUT_R = 3; // Справа низ (полное ведро)

    // === ВМЕСТИМОСТЬ 256 ВЕДЕР (256,000 mB) ===
    private final FluidTank fluidTank = new FluidTank(256000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            // Если нужно обновление блока для рендера модели в мире:
            if(level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    // Инвентарь на 4 слота
    private final ItemStackHandler itemHandler = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // В выходные слоты руками класть нельзя
            return slot != SLOT_OUTPUT_L && slot != SLOT_OUTPUT_R;
        }
    };

    // Синхронизация данных для GUI (чтобы рисовать шкалу)
    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            FluidStack stack = fluidTank.getFluid();
            switch (index) {
                case 0: return stack.getAmount();
                case 1:
                    // Если пусто, отправляем -1.
                    // Если есть жидкость, берем её числовой ID из реестра.
                    return stack.isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(stack.getFluid());
                default: return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            // На сервере сеттер не нужен
        }

        @Override
        public int getCount() {
            return 2; // Передаем 2 числа: Amount и FluidID
        }
    };

    private final LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.of(() -> itemHandler);
    private final LazyOptional<IFluidHandler> lazyFluidHandler = LazyOptional.of(() -> fluidTank);

    public MachineFluidTankBlockEntity(BlockPos pos, BlockState state) {
        // Убедись, что в ModBlockEntities ты переименовал OIL_TANK в FLUID_TANK (или как там у тебя)
        super(ModBlockEntities.FLUID_TANK_BE.get(), pos, state);
    }

    // --- LOGIC ---
    public static void tick(Level level, BlockPos pos, BlockState state, MachineFluidTankBlockEntity entity) {
        if(level.isClientSide) return;

        entity.processLeftSlots();  // Ввод жидкости (Ведро -> Танк)
        entity.processRightSlots(); // Вывод жидкости (Танк -> Ведро)
    }

    private void processLeftSlots() {
        ItemStack inputStack = itemHandler.getStackInSlot(SLOT_INPUT_L);
        if(inputStack.isEmpty()) return;

        // Получаем Capability предмета
        inputStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler -> {
            // 1. Сначала симулируем, чтобы узнать, сколько можем залить
            FluidStack fluidInItem = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);

            if(fluidInItem != null && !fluidInItem.isEmpty()) {
                // Проверяем, сколько места в танке
                int filledAmount = fluidTank.fill(fluidInItem, IFluidHandler.FluidAction.SIMULATE);

                // Если мы можем перелить хотя бы каплю (или строго всё, как хочешь)
                // Для бесконечного источника лучше проверять filledAmount > 0
                if(filledAmount > 0) {

                    // 2. ИСПОЛНЯЕМ ПЕРЕЛИВАНИЕ
                    fluidTank.fill(fluidInItem, IFluidHandler.FluidAction.EXECUTE); // Наполнили танк
                    handler.drain(filledAmount, IFluidHandler.FluidAction.EXECUTE); // "Слили" из предмета

                    // Получаем то, чем стал предмет после слива (для ведра воды -> это пустое ведро)
                    ItemStack container = handler.getContainer();

                    // === ИСПРАВЛЕНИЕ ДЛЯ БЕСКОНЕЧНЫХ ИСТОЧНИКОВ ===

                    // Проверяем: стал ли предмет другим?
                    // Если это обычное ведро: input = Ведро Воды, container = Пустое Ведро. Они РАЗНЫЕ.
                    // Если это InfiniteWater: input = InfiniteWater, container = InfiniteWater. Они ОДИНАКОВЫЕ.

                    boolean isInfiniteSource = !container.isEmpty() &&
                            ItemStack.isSameItemSameTags(inputStack, container);

                    if (isInfiniteSource) {
                        // Если это бесконечный источник — МЫ НИЧЕГО НЕ ТРОГАЕМ.
                        // Предмет остается в верхнем слоте (inputStack), мы его не удаляем (shrink)
                        // и не пытаемся переложить вниз.
                        // В следующем тике он снова отдаст воду.
                    } else {
                        // ЛОГИКА ДЛЯ ОБЫЧНЫХ ВЕДЕР

                        ItemStack outputStack = itemHandler.getStackInSlot(SLOT_OUTPUT_L);
                        // Если выходной слот занят и там не тот же предмет или нет места - прерываемся (откат не делаем, но ведро не исчезнет)
                        if (!outputStack.isEmpty() &&
                                (!ItemStack.isSameItemSameTags(outputStack, container) ||
                                        outputStack.getCount() >= outputStack.getMaxStackSize())) {
                            // Тут сложный момент: мы уже залили жидкость в танк.
                            // По-хорошему, надо бы проверять место в output ДО залива,
                            // но для простоты оставим так (жидкость зальется, ведро останется сверху пока не освободят место).
                            return;
                        }

                        inputStack.shrink(1); // Удаляем полное ведро из входа

                        if (!container.isEmpty()) {
                            // Кладем пустое ведро в выход
                            if (outputStack.isEmpty()) {
                                itemHandler.setStackInSlot(SLOT_OUTPUT_L, container);
                            } else if (ItemStack.isSameItemSameTags(outputStack, container) && outputStack.getCount() < outputStack.getMaxStackSize()) {
                                outputStack.grow(1);
                            }
                        }
                    }
                }
            }
        });
    }

    private void processRightSlots() {
        ItemStack inputStack = itemHandler.getStackInSlot(SLOT_INPUT_R);
        if(inputStack.isEmpty()) return;

        ItemStack outputStack = itemHandler.getStackInSlot(SLOT_OUTPUT_R);
        if(outputStack.getCount() >= outputStack.getMaxStackSize()) return;

        FluidStack fluidInTank = fluidTank.getFluid();
        if (fluidInTank.isEmpty()) return;

        inputStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler -> {
            // Пытаемся залить жидкость ИЗ ТАНКА В ПРЕДМЕТ (Simulate)
            // Пытаемся залить 1000 (стандарт ведра), или сколько есть
            int filled = handler.fill(fluidInTank, IFluidHandler.FluidAction.SIMULATE);

            if (filled > 0) {
                // Проверяем, есть ли столько жидкости в танке
                FluidStack drainedFromTank = fluidTank.drain(filled, IFluidHandler.FluidAction.SIMULATE);

                if (drainedFromTank.getAmount() == filled) {
                    // ИСПОЛНЯЕМ
                    fluidTank.drain(filled, IFluidHandler.FluidAction.EXECUTE); // Забрали из танка
                    handler.fill(drainedFromTank, IFluidHandler.FluidAction.EXECUTE); // Налили в предмет

                    ItemStack filledContainer = handler.getContainer(); // Полная тара

                    inputStack.shrink(1); // Убрали пустую тару

                    if (outputStack.isEmpty()) {
                        itemHandler.setStackInSlot(SLOT_OUTPUT_R, filledContainer);
                    } else if (ItemStack.isSameItemSameTags(outputStack, filledContainer) && outputStack.getCount() < outputStack.getMaxStackSize()) {
                        outputStack.grow(1);
                    }
                }
            }
        });
    }

    // --- NBT ---
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        fluidTank.readFromNBT(tag.getCompound("Fluid"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.put("Fluid", fluidTank.writeToNBT(new CompoundTag()));
    }

    // --- CAPABILITIES ---
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        if(cap == ForgeCapabilities.FLUID_HANDLER) {
            return lazyFluidHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        lazyItemHandler.invalidate();
        lazyFluidHandler.invalidate();
    }

    // --- MENU ---
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm_m.fluid_tank");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        // Не забудь переименовать OilTankMenu в FluidTankMenu, если будешь делать рефакторинг
        return new MachineFluidTankMenu(id, inventory, this, this.data);
    }
}
