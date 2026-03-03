package com.hbm_m.inventory.fluid.tank;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.item.liquids.InfiniteFluidItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.registries.ForgeRegistries;

import com.hbm_m.item.liquids.FluidIdentifierItem;

public class FluidTank {

    // --- Интерфейс обработчиков (как в 1.7.10) ---
    public interface LoadingHandler {
        boolean emptyItem(ItemStack[] slots, int in, int out, FluidTank tank);
        boolean fillItem(ItemStack[] slots, int in, int out, FluidTank tank);
    }

    private static final List<LoadingHandler> loadingHandlers = new ArrayList<>();
    
    static {
        loadingHandlers.add(new FluidLoaderStandard());
        // В 1.20.1 стандартный загрузчик (через Capabilities) уже поддерживает кастомные канистры.
        // Добавляем специфичный лоадер для креативных/бесконечных предметов.
        loadingHandlers.add(new FluidLoaderInfinite());
    }

    // --- Состояние бака ---
    protected Fluid type = Fluids.EMPTY; // Аналог FluidType из 1.7.10
    protected int fluid;                 // Текущий объем (mB)
    protected int maxFluid;              // Максимальная вместимость
    protected int pressure = 0;          // Давление (PU)
    
    // Обёртка для труб и других модов (Capabilities)
    private final LazyOptional<IFluidHandler> lazyFluidHandler;

    public FluidTank(Fluid type, int maxFluid) {
        this.type = type == null ? Fluids.EMPTY : type;
        this.maxFluid = maxFluid;
        this.lazyFluidHandler = LazyOptional.of(() -> new ForgeFluidHandlerWrapper(this));
    }

    public FluidTank(int maxFluid) {
        this(Fluids.EMPTY, maxFluid);
    }

    // --- Основные методы (идентичны 1.7.10) ---

    public FluidTank withPressure(int pressure) {
        if (this.pressure != pressure) this.fill(0);
        this.pressure = pressure;
        return this;
    }

    public void fill(int amount) {
        this.fluid = Mth.clamp(amount, 0, maxFluid);
        if (this.fluid == 0) this.type = Fluids.EMPTY; // Очистка типа, если бак пуст и не зафильтрован
    }

    public void setTankType(Fluid type) {
        if (type == null) type = Fluids.EMPTY;
        if (this.type == type) return;
        
        this.type = type;
        this.fill(0);
    }

    public void resetTank() {
        this.type = Fluids.EMPTY;
        this.fluid = 0;
        this.pressure = 0;
    }

    public FluidTank conformWithStack(FluidStack stack) {
        this.setTankType(stack.getFluid());
        // В HBM 1.20.1 для передачи давления через FluidStack можно использовать NBT стэка,
        // но пока оставим базовую конформацию:
        this.withPressure(0); 
        return this;
    }

    public Fluid getTankType() { return type; }
    public int getFill() { return fluid; }
    public int getMaxFill() { return maxFluid; }
    public int getPressure() { return pressure; }

    public int changeTankSize(int size) {
        maxFluid = size;
        if (fluid > maxFluid) {
            int dif = fluid - maxFluid;
            fluid = maxFluid;
            return dif;
        }
        return 0;
    }

    public LazyOptional<IFluidHandler> getCapability() {
        return lazyFluidHandler;
    }

    // --- Загрузка/Выгрузка предметов (аналоги loadTank/unloadTank) ---

    public boolean loadTank(int in, int out, ItemStack[] slots) {
        if (slots[in] == null || slots[in].isEmpty()) return false;

        boolean isInfinite = slots[in].getItem() instanceof InfiniteFluidItem;
        if (!isInfinite && pressure != 0) return false; // Запрет заливки под давлением (как в 1.7.10)
        
        int prev = this.getFill();
        for (LoadingHandler handler : loadingHandlers) {
            if (handler.emptyItem(slots, in, out, this)) break;
        }
        return this.getFill() > prev;
    }

    public boolean unloadTank(int in, int out, ItemStack[] slots) {
        if (slots[in] == null || slots[in].isEmpty()) return false;
        
        int prev = this.getFill();
        for (LoadingHandler handler : loadingHandlers) {
            if (handler.fillItem(slots, in, out, this)) break;
        }
        return this.getFill() < prev;
    }

    // --- Установка типа (Фильтры) ---

    public boolean setType(int in, int out, ItemStack[] slots) {
        if (slots[in] != null && !slots[in].isEmpty() && slots[in].getItem() instanceof FluidIdentifierItem) {
            Fluid newType = FluidIdentifierItem.getType(slots[in], true); // Берем primary жидкость
            
            if (newType == null || newType == Fluids.EMPTY) return false;

            if (in == out) {
                if (type != newType) {
                    type = newType;
                    fluid = 0;
                    return true;
                }
            } else if (slots[out] == null || slots[out].isEmpty()) {
                if (type != newType) {
                    type = newType;
                    slots[out] = slots[in].copy();
                    slots[in] = ItemStack.EMPTY;
                    fluid = 0;
                    return true;
                }
            }
        }
        return false;
    }

    public boolean setType(int in, ItemStack[] slots) {
        return setType(in, in, slots);
    }

    // --- Сохранение и Сеть ---

    public void writeToNBT(CompoundTag nbt, String prefix) {
        nbt.putInt(prefix + "_amount", fluid);
        nbt.putInt(prefix + "_max", maxFluid);
        ResourceLocation loc = ForgeRegistries.FLUIDS.getKey(type);
        nbt.putString(prefix + "_type", loc != null ? loc.toString() : "minecraft:empty");
        nbt.putShort(prefix + "_p", (short) pressure);
    }

    public void readFromNBT(CompoundTag nbt, String prefix) {
        if (!nbt.contains(prefix + "_amount")) return;
        fluid = nbt.getInt(prefix + "_amount");
        maxFluid = nbt.getInt(prefix + "_max");
        fluid = Mth.clamp(fluid, 0, maxFluid);
        
        String typeIdStr = nbt.getString(prefix + "_type");
        Fluid f = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(typeIdStr));
        type = (f != null) ? f : Fluids.EMPTY;
        
        pressure = nbt.getShort(prefix + "_p");
    }

    public void serialize(FriendlyByteBuf buf) {
        buf.writeInt(fluid);
        buf.writeInt(maxFluid);
        ResourceLocation loc = ForgeRegistries.FLUIDS.getKey(type);
        buf.writeResourceLocation(loc != null ? loc : new ResourceLocation("minecraft:empty"));
        buf.writeShort((short) pressure);
    }

    public void deserialize(FriendlyByteBuf buf) {
        fluid = buf.readInt();
        maxFluid = buf.readInt();
        Fluid f = ForgeRegistries.FLUIDS.getValue(buf.readResourceLocation());
        type = (f != null) ? f : Fluids.EMPTY;
        pressure = buf.readShort();
    }

    // ===================================================================================== //
    // ОБРАБОТЧИКИ (LOADERS)
    // ===================================================================================== //

    // Вспомогательный метод для работы со слотами предметов
    private static boolean canPlaceItemInSlot(ItemStack[] slots, int slotOut, ItemStack resultStack) {
        if (resultStack.isEmpty()) return true;
        if (slots[slotOut] == null || slots[slotOut].isEmpty()) return true;
        return ItemStack.isSameItemSameTags(slots[slotOut], resultStack) && 
               slots[slotOut].getCount() + resultStack.getCount() <= slots[slotOut].getMaxStackSize();
    }

    private static void placeItemInSlot(ItemStack[] slots, int slotOut, ItemStack resultStack) {
        if (resultStack.isEmpty()) return;
        if (slots[slotOut] == null || slots[slotOut].isEmpty()) {
            slots[slotOut] = resultStack;
        } else {
            slots[slotOut].grow(resultStack.getCount());
        }
    }

    /** 
     * Стандартный обработчик: заменяет и FluidLoaderStandard, и FluidLoaderFillableItem из 1.7.10 
     * благодаря системе Forge Capabilities.
     */
    public static class FluidLoaderStandard implements LoadingHandler {
        @Override
        public boolean emptyItem(ItemStack[] slots, int in, int out, FluidTank tank) {
            ItemStack inputStack = slots[in];
            if (inputStack == null || inputStack.isEmpty()) return true;

            // Обрабатываем по одному предмету за раз
            ItemStack singleItem = inputStack.copy();
            singleItem.setCount(1);

            LazyOptional<IFluidHandlerItem> cap = singleItem.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
            if (cap.isPresent()) {
                IFluidHandlerItem handler = cap.orElse(null);
                
                // Пробуем слить максимум
                FluidStack simulatedDrain = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
                if (simulatedDrain.isEmpty()) return false;

                // Устанавливаем тип бака, если он пустой
                if (tank.getTankType() == Fluids.EMPTY && tank.getFill() == 0) {
                    tank.setTankType(simulatedDrain.getFluid());
                }

                // Проверка типа
                if (tank.getTankType() == simulatedDrain.getFluid()) {
                    int space = tank.getMaxFill() - tank.getFill();
                    if (space > 0) {
                        // Сливаем реально только то, что поместится
                        FluidStack realDrain = handler.drain(space, IFluidHandler.FluidAction.EXECUTE);
                        ItemStack containerResult = handler.getContainer();

                        if (realDrain.getAmount() > 0 && canPlaceItemInSlot(slots, out, containerResult)) {
                            tank.fill(tank.getFill() + realDrain.getAmount());
                            placeItemInSlot(slots, out, containerResult);
                            slots[in].shrink(1);
                            if (slots[in].getCount() <= 0) slots[in] = ItemStack.EMPTY;
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public boolean fillItem(ItemStack[] slots, int in, int out, FluidTank tank) {
            ItemStack inputStack = slots[in];
            if (inputStack == null || inputStack.isEmpty() || tank.getFill() <= 0) return false;

            ItemStack singleItem = inputStack.copy();
            singleItem.setCount(1);

            LazyOptional<IFluidHandlerItem> cap = singleItem.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM);
            if (cap.isPresent()) {
                IFluidHandlerItem handler = cap.orElse(null);
                FluidStack toFill = new FluidStack(tank.getTankType(), tank.getFill());
                
                int simulatedFill = handler.fill(toFill, IFluidHandler.FluidAction.SIMULATE);
                if (simulatedFill > 0) {
                    int realFill = handler.fill(new FluidStack(tank.getTankType(), simulatedFill), IFluidHandler.FluidAction.EXECUTE);
                    ItemStack containerResult = handler.getContainer();

                    if (realFill > 0 && canPlaceItemInSlot(slots, out, containerResult)) {
                        tank.fill(tank.getFill() - realFill);
                        placeItemInSlot(slots, out, containerResult);
                        slots[in].shrink(1);
                        if (slots[in].getCount() <= 0) slots[in] = ItemStack.EMPTY;
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /** 
     * Обработчик для креативных/бесконечных источников.
     * Полностью копирует логику из 1.7.10: ItemInfiniteFluid может как добавлять, так и удалять жидкость.
     */
    public static class FluidLoaderInfinite implements LoadingHandler {
        @Override
        public boolean emptyItem(ItemStack[] slots, int in, int out, FluidTank tank) {
            ItemStack stack = slots[in];
            if (stack == null || !(stack.getItem() instanceof InfiniteFluidItem) || tank.getTankType() == Fluids.EMPTY) return false;
            
            // Заполняем бак на 100% при контакте с бесконечным источником нужного типа
            // Примечание: в 1.20.1 предмете нет getChance(), поэтому заполняем гарантированно
            tank.fill(tank.getMaxFill());
            return true;
        }

        @Override
        public boolean fillItem(ItemStack[] slots, int in, int out, FluidTank tank) {
            ItemStack stack = slots[in];
            if (stack == null || !(stack.getItem() instanceof InfiniteFluidItem)) return false;
            
            // Если пытаемся "наполнить" бесконечную бочку, она работает как Void (уничтожитель жидкости)
            tank.fill(0);
            return true;
        }
    }

    // ===================================================================================== //
    // ОБЁРТКА ДЛЯ FORGE CAPABILITIES (Трубы, другие моды)
    // ===================================================================================== //

    public static class ForgeFluidHandlerWrapper implements IFluidHandler {
        private final FluidTank tank;

        public ForgeFluidHandlerWrapper(FluidTank tank) {
            this.tank = tank;
        }

        @Override
        public int getTanks() { return 1; }

        @Override
        public FluidStack getFluidInTank(int tankIndex) {
            if (tank.getTankType() == Fluids.EMPTY || tank.getFill() <= 0) return FluidStack.EMPTY;
            return new FluidStack(tank.getTankType(), tank.getFill());
        }

        @Override
        public int getTankCapacity(int tankIndex) { return tank.getMaxFill(); }

        @Override
        public boolean isFluidValid(int tankIndex, FluidStack stack) {
            if (tank.getPressure() != 0) return false; // Внешние трубы не могут заливать в бак под давлением
            return tank.getTankType() == Fluids.EMPTY || tank.getTankType() == stack.getFluid();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;

            int space = tank.getMaxFill() - tank.getFill();
            int fillAmount = Math.min(resource.getAmount(), space);

            if (fillAmount > 0 && action == FluidAction.EXECUTE) {
                if (tank.getTankType() == Fluids.EMPTY && tank.getFill() == 0) {
                    tank.setTankType(resource.getFluid());
                }
                tank.fill(tank.getFill() + fillAmount);
            }
            return fillAmount;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || resource.getFluid() != tank.getTankType()) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || tank.getFill() <= 0 || tank.getTankType() == Fluids.EMPTY) return FluidStack.EMPTY;

            int drainAmount = Math.min(maxDrain, tank.getFill());
            FluidStack result = new FluidStack(tank.getTankType(), drainAmount);

            if (action == FluidAction.EXECUTE) {
                tank.fill(tank.getFill() - drainAmount);
            }
            return result;
        }
    }
}