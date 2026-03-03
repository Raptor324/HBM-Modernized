package com.hbm_m.inventory.fluid.tank;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.item.liquids.InfiniteFluidItem;
import com.hbm_m.item.liquids.InfiniteWaterItem; // Added import

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

import com.hbm_m.item.liquids.FluidIdentifierItem;

public class FluidTank {

    public interface LoadingHandler {
        boolean emptyItem(ItemStack[] slots, int in, int out, FluidTank tank);
        boolean fillItem(ItemStack[] slots, int in, int out, FluidTank tank);
    }

    private static final List<LoadingHandler> loadingHandlers = new ArrayList<>();
    
    static {
        // Infinite must be first to prevent Standard loader from consuming infinite items
        loadingHandlers.add(new FluidLoaderInfinite());
        loadingHandlers.add(new FluidLoaderStandard());
    }

    protected Fluid type = Fluids.EMPTY;
    protected int fluid;
    protected int maxFluid;
    protected int pressure = 0;
    
    private final LazyOptional<IFluidHandler> lazyFluidHandler;

    public FluidTank(Fluid type, int maxFluid) {
        this.type = type == null ? Fluids.EMPTY : type;
        this.maxFluid = maxFluid;
        this.lazyFluidHandler = LazyOptional.of(() -> new ForgeFluidHandlerWrapper(this));
    }

    public FluidTank(int maxFluid) {
        this(Fluids.EMPTY, maxFluid);
    }

    public FluidTank withPressure(int pressure) {
        if (this.pressure != pressure) this.fill(0);
        this.pressure = pressure;
        return this;
    }

    public void fill(int amount) {
        this.fluid = Mth.clamp(amount, 0, maxFluid);
        if (this.fluid == 0) this.type = Fluids.EMPTY;
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

    public boolean loadTank(int in, int out, ItemStack[] slots) {
        if (slots[in] == null || slots[in].isEmpty()) return false;

        boolean isInfinite = slots[in].getItem() instanceof InfiniteFluidItem || slots[in].getItem() instanceof InfiniteWaterItem;
        if (!isInfinite && pressure != 0) return false;
        
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

    public boolean setType(int in, int out, ItemStack[] slots) {
        if (slots[in] != null && !slots[in].isEmpty() && slots[in].getItem() instanceof FluidIdentifierItem) {
            Fluid newType = FluidIdentifierItem.getType(slots[in], true);
            
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
        Fluid f = ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse(typeIdStr));
        type = (f != null) ? f : Fluids.EMPTY;
        
        pressure = nbt.getShort(prefix + "_p");
    }

    public void serialize(FriendlyByteBuf buf) {
        buf.writeInt(fluid);
        buf.writeInt(maxFluid);
        ResourceLocation loc = ForgeRegistries.FLUIDS.getKey(type);
        buf.writeResourceLocation(loc != null ? loc : ResourceLocation.parse("minecraft:empty"));
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
    // LOADERS
    // ===================================================================================== //

    private static boolean canPlaceItemInSlot(ItemStack[] slots, int slotOut, ItemStack resultStack) {
        if (resultStack.isEmpty()) return true;
        ItemStack stackInSlot = slots[slotOut];
        if (stackInSlot == null || stackInSlot.isEmpty()) return true;
        
        return ItemStack.isSameItemSameTags(stackInSlot, resultStack) && 
               stackInSlot.getCount() + resultStack.getCount() <= stackInSlot.getMaxStackSize();
    }

    private static void placeItemInSlot(ItemStack[] slots, int slotOut, ItemStack resultStack) {
        if (resultStack.isEmpty()) return;
        if (slots[slotOut] == null || slots[slotOut].isEmpty()) {
            slots[slotOut] = resultStack;
        } else {
            slots[slotOut].grow(resultStack.getCount());
        }
    }

    public static class FluidLoaderStandard implements LoadingHandler {
        @Override
        public boolean emptyItem(ItemStack[] slots, int in, int out, FluidTank tank) {
            ItemStack inputStack = slots[in];
            if (inputStack == null || inputStack.isEmpty()) return false;

            ItemStack stackToDrain = inputStack.copy();
            stackToDrain.setCount(1);

            return stackToDrain.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(handler -> {
                FluidStack drainedSim = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
                if (drainedSim.isEmpty()) return false;

                if (tank.getTankType() != Fluids.EMPTY && tank.getTankType() != drainedSim.getFluid()) {
                    return false;
                }

                int space = tank.getMaxFill() - tank.getFill();
                int amountToDrain = Math.min(drainedSim.getAmount(), space);
                if (amountToDrain <= 0) return false;

                FluidStack drainedReal = handler.drain(amountToDrain, IFluidHandler.FluidAction.EXECUTE);
                if (drainedReal.isEmpty()) return false;

                ItemStack containerResult = handler.getContainer();

                if (canPlaceItemInSlot(slots, out, containerResult)) {
                    if (tank.getTankType() == Fluids.EMPTY) {
                        tank.setTankType(drainedReal.getFluid());
                    }
                    tank.fill(tank.getFill() + drainedReal.getAmount());
                    
                    placeItemInSlot(slots, out, containerResult);
                    slots[in].shrink(1);
                    if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;
                    return true;
                }
                return false;
            }).orElse(false);
        }

        @Override
        public boolean fillItem(ItemStack[] slots, int in, int out, FluidTank tank) {
            ItemStack inputStack = slots[in];
            if (inputStack == null || inputStack.isEmpty() || tank.getFill() <= 0) return false;

            ItemStack stackToFill = inputStack.copy();
            stackToFill.setCount(1);

            return stackToFill.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(handler -> {
                FluidStack resource = new FluidStack(tank.getTankType(), tank.getFill());
                int filledAmount = handler.fill(resource, IFluidHandler.FluidAction.EXECUTE);
                
                if (filledAmount <= 0) return false;

                ItemStack containerResult = handler.getContainer();

                if (canPlaceItemInSlot(slots, out, containerResult)) {
                    tank.fill(tank.getFill() - filledAmount);
                    
                    placeItemInSlot(slots, out, containerResult);
                    slots[in].shrink(1);
                    if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;
                    return true;
                }
                return false;
            }).orElse(false);
        }
    }

    public static class FluidLoaderInfinite implements LoadingHandler {
        @Override
        public boolean emptyItem(ItemStack[] slots, int in, int out, FluidTank tank) {
            ItemStack stack = slots[in];
            if (stack == null || stack.isEmpty()) return false;
            
            Fluid fluidType = Fluids.EMPTY;
            
            // Check for Generic Infinite Fluid Item
            if (stack.getItem() instanceof InfiniteFluidItem) {
                fluidType = ((InfiniteFluidItem) stack.getItem()).getFluidType(stack);
            } 
            // Check for Specific Infinite Water Item
            else if (stack.getItem() instanceof InfiniteWaterItem) {
                fluidType = Fluids.WATER;
            }

            if (fluidType != Fluids.EMPTY) {
                // If tank is full, do nothing
                if (tank.getFill() >= tank.getMaxFill()) return false;
                
                // If tank is empty or compatible
                if (tank.getTankType() == Fluids.EMPTY || tank.getTankType() == fluidType) {
                    tank.setTankType(fluidType);
                    tank.fill(tank.getMaxFill());
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean fillItem(ItemStack[] slots, int in, int out, FluidTank tank) {
            ItemStack stack = slots[in];
            if (stack == null || stack.isEmpty()) return false;
            
            // Infinite Fluid Item acts as void
            if (stack.getItem() instanceof InfiniteFluidItem) {
                if (tank.getFill() > 0) {
                    tank.fill(0);
                    return true;
                }
            }
            return false;
        }
    }

    // ===================================================================================== //
    // WRAPPER
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
            if (tank.getPressure() != 0) return false;
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