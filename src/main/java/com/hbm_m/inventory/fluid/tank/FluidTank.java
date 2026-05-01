package com.hbm_m.inventory.fluid.tank;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.api.fluids.ModFluids;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;

//? if fabric {
import dev.architectury.fluid.FluidStack;
import dev.architectury.hooks.fluid.FluidStackHooks;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.SimpleContainer;
//?}

import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.item.liquids.InfiniteFluidItem;
import com.hbm_m.item.liquids.InfiniteWaterItem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
//? if forge {
/*import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
*///?}

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

    //? if forge {
    /*private final LazyOptional<IFluidHandler> lazyFluidHandler;
     *///?}

    public FluidTank(Fluid type, int maxFluid) {
        this.type = type == null ? Fluids.EMPTY : type;
        this.maxFluid = maxFluid;
        //? if forge {
        /*this.lazyFluidHandler = LazyOptional.of(() -> new ForgeFluidHandlerWrapper(this));
         *///?}
    }

    public FluidTank(int maxFluid) {
        this(Fluids.EMPTY, maxFluid);
    }

    public FluidTank withPressure(int pressure) {
        if (this.pressure != pressure) {
            this.type = Fluids.EMPTY;
            this.fluid = 0;
        }
        this.pressure = pressure;
        return this;
    }

    public void fill(int amount) {
        this.fluid = Mth.clamp(amount, 0, maxFluid);
    }

    public void setTankType(Fluid type) {
        if (type == null) type = Fluids.EMPTY;
        if (this.type == type) return;
        this.type = type;
        this.fluid = 0;
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

    //? if forge {
    /*public boolean isFluidValid(net.minecraftforge.fluids.FluidStack stack) {
        return true;
    }
    *///?}

    public Fluid getTankType() { return type; }
    public int getFill() { return fluid; }
    public int getMaxFill() { return maxFluid; }

    public static boolean isFluidTypeExplicitlySet(Fluid type) {
        if (type == null || type == Fluids.EMPTY) return false;
        return type != ModFluids.NONE.getSource();
    }

    public void assignTypeAndZeroFluid(Fluid newType) {
        this.type = newType == null ? Fluids.EMPTY : newType;
        this.fluid = 0;
    }

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

    //? if forge {
    /*public LazyOptional<IFluidHandler> getCapability() {
        return lazyFluidHandler;
    }
    *///?}

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
        if (slots[in] == null || slots[in].isEmpty() || !(slots[in].getItem() instanceof FluidIdentifierItem)) {
            return false;
        }
        Fluid newType = FluidIdentifierItem.resolvePrimaryForTank(slots[in]);
        if (newType == null) {
            return false;
        }
        if (type == newType) {
            return false;
        }
        type = newType;
        fluid = 0;
        if (in != out) {
            if (slots[out] == null || slots[out].isEmpty()) {
                slots[out] = slots[in].copy();
                slots[in] = ItemStack.EMPTY;
            }
        }
        return true;
    }

    public boolean setType(int in, ItemStack[] slots) {
        return setType(in, in, slots);
    }

    public void writeToNBT(CompoundTag nbt, String prefix) {
        nbt.putInt(prefix + "_amount", fluid);
        nbt.putInt(prefix + "_max", maxFluid);
        //? if forge {
        /*ResourceLocation loc = BuiltInRegistries.FLUID.getKey(type);
         *///?}
        //? if fabric {
        ResourceLocation loc = BuiltInRegistries.FLUID.getKey(type);
        //?}
        nbt.putString(prefix + "_type", loc != null ? loc.toString() : "minecraft:empty");
        nbt.putShort(prefix + "_p", (short) pressure);
    }

    public void readFromNBT(CompoundTag nbt, String prefix) {
        if (!nbt.contains(prefix + "_amount")) return;
        fluid = nbt.getInt(prefix + "_amount");
        maxFluid = nbt.getInt(prefix + "_max");
        fluid = Mth.clamp(fluid, 0, maxFluid);

        String typeIdStr = nbt.getString(prefix + "_type");
        //? if forge {
        /*Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(typeIdStr));
         *///?}
        //? if fabric {
        Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(typeIdStr));
        //?}
        type = (f != null) ? f : Fluids.EMPTY;

        pressure = nbt.getShort(prefix + "_p");
    }

    public void serialize(FriendlyByteBuf buf) {
        buf.writeInt(fluid);
        buf.writeInt(maxFluid);
        //? if forge {
        /*ResourceLocation loc = BuiltInRegistries.FLUID.getKey(type);
         *///?}
        //? if fabric {
        ResourceLocation loc = BuiltInRegistries.FLUID.getKey(type);
        //?}
        buf.writeResourceLocation(loc != null ? loc : ResourceLocation.tryParse("minecraft:empty"));
        buf.writeShort((short) pressure);
    }

    public void deserialize(FriendlyByteBuf buf) {
        fluid = buf.readInt();
        maxFluid = buf.readInt();
        //? if forge {
        /*Fluid f = BuiltInRegistries.FLUID.get(buf.readResourceLocation());
         *///?}
        //? if fabric {
        Fluid f = BuiltInRegistries.FLUID.get(buf.readResourceLocation());
        //?}
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
            if (!isFluidTypeExplicitlySet(tank.getTankType())) return false;

            //? if forge {
            /*ItemStack simCopy = inputStack.copy();
            simCopy.setCount(1);
            Boolean feasible = simCopy.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(simHandler -> {
                FluidStack drainedSim = simHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
                if (drainedSim.isEmpty()) return false;
                if (!VanillaFluidEquivalence.sameSubstance(tank.getTankType(), drainedSim.getFluid())) return false;
                int space = tank.getMaxFill() - tank.getFill();
                int amountToDrain = Math.min(drainedSim.getAmount(), space);
                if (amountToDrain <= 0) return false;
                simHandler.drain(amountToDrain, IFluidHandler.FluidAction.EXECUTE);
                ItemStack containerResult = simHandler.getContainer();
                return canPlaceItemInSlot(slots, out, containerResult);
            }).orElse(false);
            if (!feasible) return false;

            ItemStack execCopy = inputStack.copy();
            execCopy.setCount(1);
            return execCopy.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(handler -> {
                FluidStack drainedSim = handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
                int space = tank.getMaxFill() - tank.getFill();
                int amountToDrain = Math.min(drainedSim.getAmount(), space);
                FluidStack drainedReal = handler.drain(amountToDrain, IFluidHandler.FluidAction.EXECUTE);
                if (drainedReal.isEmpty()) return false;
                ItemStack containerResult = handler.getContainer();
                tank.fill(tank.getFill() + drainedReal.getAmount());
                placeItemInSlot(slots, out, containerResult);
                slots[in].shrink(1);
                if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;
                return true;
            }).orElse(false);
            *///?}

            //? if fabric {
            ItemStack simCopy = inputStack.copy();
            simCopy.setCount(1);
            SimpleContainer simInv = new SimpleContainer(simCopy);
            ContainerItemContext simCtx = ContainerItemContext.ofSingleSlot(InventoryStorage.of(simInv, null).getSlot(0));
            Storage<FluidVariant> simStorage = FluidStorage.ITEM.find(simCopy, simCtx);
            if (simStorage == null) return false;

            // Узнаём, ЧТО конкретно находится внутри бочки/ведра
            FluidVariant storedVariant = FluidVariant.blank();
            for (StorageView<FluidVariant> view : simStorage) {
                if (!view.isResourceBlank() && view.getAmount() > 0) {
                    storedVariant = view.getResource();
                    break;
                }
            }

            if (storedVariant.isBlank()) return false;

            // Проверяем, подходит ли жидкость из предмета к нашей цистерне (модовая вода = ванильная вода)
            if (!VanillaFluidEquivalence.sameSubstance(tank.getTankType(), storedVariant.getFluid())) {
                return false;
            }

            int space = tank.getMaxFill() - tank.getFill();
            long maxExtract = (long) space * 81L;

            // Simulate drain
            try (Transaction tx = Transaction.openOuter()) {
                long drainableSim = simStorage.extract(storedVariant, maxExtract, tx);
                if (drainableSim <= 0) return false;
                if (!canPlaceItemInSlot(slots, out, simInv.getItem(0))) return false;
                // don't commit — simulation only
            }

            // Execute drain
            ItemStack execCopy = inputStack.copy();
            execCopy.setCount(1);
            SimpleContainer execInv = new SimpleContainer(execCopy);
            ContainerItemContext execCtx = ContainerItemContext.ofSingleSlot(InventoryStorage.of(execInv, null).getSlot(0));
            Storage<FluidVariant> execStorage = FluidStorage.ITEM.find(execCopy, execCtx);
            if (execStorage == null) return false;

            long drained;
            try (Transaction tx = Transaction.openOuter()) {
                drained = execStorage.extract(storedVariant, maxExtract, tx);
                if (drained > 0) tx.commit();
            }
            if (drained <= 0) return false;

            // 1 ведро Fabric = 81000 droplets. 81000 / 81 = ровно 1000 mB для бака.
            tank.fill(tank.getFill() + (int) (drained / 81L));
            placeItemInSlot(slots, out, execInv.getItem(0));
            slots[in].shrink(1);
            if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;
            return true;
            //?}
        }

        @Override
        public boolean fillItem(ItemStack[] slots, int in, int out, FluidTank tank) {
            ItemStack inputStack = slots[in];
            if (inputStack == null || inputStack.isEmpty() || tank.getFill() <= 0) return false;
            if (!isFluidTypeExplicitlySet(tank.getTankType())) return false;

            //? if forge {
            /*ItemStack simCopy = inputStack.copy();
            simCopy.setCount(1);
            Boolean feasible = simCopy.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(simHandler -> {
                FluidStack resource = new FluidStack(
                    VanillaFluidEquivalence.forVanillaContainerFill(tank.getTankType()), tank.getFill());
                int filledAmount = simHandler.fill(resource, IFluidHandler.FluidAction.SIMULATE);
                if (filledAmount <= 0) return false;
                simHandler.fill(resource, IFluidHandler.FluidAction.EXECUTE);
                ItemStack containerResult = simHandler.getContainer();
                return canPlaceItemInSlot(slots, out, containerResult);
            }).orElse(false);
            if (!feasible) return false;

            ItemStack execCopy = inputStack.copy();
            execCopy.setCount(1);
            return execCopy.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(handler -> {
                FluidStack resource = new FluidStack(
                    VanillaFluidEquivalence.forVanillaContainerFill(tank.getTankType()), tank.getFill());
                int filledAmount = handler.fill(resource, IFluidHandler.FluidAction.EXECUTE);
                if (filledAmount <= 0) return false;
                ItemStack containerResult = handler.getContainer();
                tank.fill(tank.getFill() - filledAmount);
                placeItemInSlot(slots, out, containerResult);
                slots[in].shrink(1);
                if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;
                return true;
            }).orElse(false);
            *///?}

            //? if fabric {
            ItemStack simCopy = inputStack.copy();
            simCopy.setCount(1);
            SimpleContainer simInv = new SimpleContainer(simCopy);
            ContainerItemContext simCtx = ContainerItemContext.ofSingleSlot(InventoryStorage.of(simInv, null).getSlot(0));
            Storage<FluidVariant> simStorage = FluidStorage.ITEM.find(simCopy, simCtx);
            if (simStorage == null) return false;

            FluidVariant variant = FluidVariant.of(
                    VanillaFluidEquivalence.forVanillaContainerFill(tank.getTankType()));
            long droplets = (long) tank.getFill() * 81L;

            // Simulate fill
            try (Transaction tx = Transaction.openOuter()) {
                long fillableSim = simStorage.insert(variant, droplets, tx);
                if (fillableSim <= 0) return false;
                if (!canPlaceItemInSlot(slots, out, simInv.getItem(0))) return false;
                // simulation, don't commit
            }

            // Execute fill
            ItemStack execCopy = inputStack.copy();
            execCopy.setCount(1);
            SimpleContainer execInv = new SimpleContainer(execCopy);
            ContainerItemContext execCtx = ContainerItemContext.ofSingleSlot(InventoryStorage.of(execInv, null).getSlot(0));
            Storage<FluidVariant> execStorage = FluidStorage.ITEM.find(execCopy, execCtx);
            if (execStorage == null) return false;

            long filled;
            try (Transaction tx = Transaction.openOuter()) {
                filled = execStorage.insert(variant, droplets, tx);
                if (filled > 0) tx.commit();
            }
            if (filled <= 0) return false;

            tank.fill(tank.getFill() - (int) (filled / 81L));
            placeItemInSlot(slots, out, execInv.getItem(0));
            slots[in].shrink(1);
            if (slots[in].isEmpty()) slots[in] = ItemStack.EMPTY;
            return true;
            //?}
        }
    }

    public static class FluidLoaderInfinite implements LoadingHandler {
        @Override
        public boolean emptyItem(ItemStack[] slots, int in, int out, FluidTank tank) {
            ItemStack stack = slots[in];
            if (stack == null || stack.isEmpty()) return false;

            if (stack.getItem() instanceof InfiniteFluidItem) {
                if (!isFluidTypeExplicitlySet(tank.getTankType())) return false;
                if (tank.getFill() >= tank.getMaxFill()) return false;
                tank.fill(tank.getMaxFill());
                return true;
            }

            if (stack.getItem() instanceof InfiniteWaterItem) {
                if (tank.getFill() >= tank.getMaxFill()) return false;
                if (!isFluidTypeExplicitlySet(tank.getTankType()) || !VanillaFluidEquivalence.sameSubstance(tank.getTankType(), Fluids.WATER)) {
                    return false;
                }
                tank.fill(tank.getMaxFill());
                return true;
            }

            return false;
        }

        @Override
        public boolean fillItem(ItemStack[] slots, int in, int out, FluidTank tank) {
            ItemStack stack = slots[in];
            if (stack == null || stack.isEmpty()) return false;

            if (stack.getItem() instanceof InfiniteFluidItem) {
                if (tank.getFill() > 0) {
                    tank.fill(0);
                    return true;
                }
            }
            return false;
        }
    }

    //? if forge {
    /*public static class ForgeFluidHandlerWrapper implements IFluidHandler {
        private final FluidTank tank;

        public ForgeFluidHandlerWrapper(FluidTank tank) {
            this.tank = tank;
        }

        @Override
        public int getTanks() { return 1; }

        @Override
        public FluidStack getFluidInTank(int tankIndex) {
            if (!isFluidTypeExplicitlySet(tank.getTankType()) || tank.getFill() <= 0) return FluidStack.EMPTY;
            return new FluidStack(tank.getTankType(), tank.getFill());
        }

        @Override
        public int getTankCapacity(int tankIndex) { return tank.getMaxFill(); }

        @Override
        public boolean isFluidValid(int tankIndex, FluidStack stack) {
            if (tank.getPressure() != 0) return false;
            if (!isFluidTypeExplicitlySet(tank.getTankType())) return false;
            return VanillaFluidEquivalence.sameSubstance(tank.getTankType(), stack.getFluid());
        }

        @Override
        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;

            int space = tank.getMaxFill() - tank.getFill();
            int fillAmount = Math.min(resource.getAmount(), space);

            if (fillAmount > 0 && action == IFluidHandler.FluidAction.EXECUTE) {
                tank.fill(tank.getFill() + fillAmount);
            }
            return fillAmount;
        }

        @Override
        public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
            if (resource.isEmpty() || !VanillaFluidEquivalence.sameSubstance(resource.getFluid(), tank.getTankType())) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
            if (maxDrain <= 0 || tank.getFill() <= 0 || !isFluidTypeExplicitlySet(tank.getTankType())) return FluidStack.EMPTY;

            int drainAmount = Math.min(maxDrain, tank.getFill());
            FluidStack result = new FluidStack(tank.getTankType(), drainAmount);

            if (action == IFluidHandler.FluidAction.EXECUTE) {
                tank.fill(tank.getFill() - drainAmount);
            }
            return result;
        }
    }
    *///?}
}