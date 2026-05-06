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
        if (newType == null) return false;
        if (type == newType) return false;
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

    // ══════════════════ Helper methods for loading handlers ══════════════════

    static boolean canPlaceItemInSlot(ItemStack[] slots, int slotOut, ItemStack resultStack) {
        if (resultStack.isEmpty()) return true;
        ItemStack stackInSlot = slots[slotOut];
        if (stackInSlot == null || stackInSlot.isEmpty()) return true;
        return ItemStack.isSameItemSameTags(stackInSlot, resultStack) &&
                stackInSlot.getCount() + resultStack.getCount() <= stackInSlot.getMaxStackSize();
    }

    static void placeItemInSlot(ItemStack[] slots, int slotOut, ItemStack resultStack) {
        if (resultStack.isEmpty()) return;
        if (slots[slotOut] == null || slots[slotOut].isEmpty()) {
            slots[slotOut] = resultStack;
        } else {
            slots[slotOut].grow(resultStack.getCount());
        }
    }

    // ══════════════════ Forge IFluidHandler wrapper ══════════════════

    //? if forge {
    /*public static class ForgeFluidHandlerWrapper implements IFluidHandler {
        private final FluidTank tank;

        public ForgeFluidHandlerWrapper(FluidTank tank) { this.tank = tank; }

        @Override public int getTanks() { return 1; }

        @Override
        public FluidStack getFluidInTank(int tankIndex) {
            if (!isFluidTypeExplicitlySet(tank.getTankType()) || tank.getFill() <= 0) return FluidStack.EMPTY;
            return new FluidStack(tank.getTankType(), tank.getFill());
        }

        @Override public int getTankCapacity(int tankIndex) { return tank.getMaxFill(); }

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
