package com.hbm_m.inventory.fluid.tank;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.item.liquids.InfiniteFluidItem;
import com.hbm_m.platform.IPlatformFluidHandler;
import com.hbm_m.platform.FluidHooks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FluidTank implements Cloneable {
    public static final FluidTank[] EMPTY_ARRAY = new FluidTank[0];

    public interface LoadingHandler {
        boolean emptyItem(ItemStack[] slots, int in, int out, FluidTank tank);
        boolean fillItem(ItemStack[] slots, int in, int out, FluidTank tank);
    }
    
    public static final List<LoadingHandler> loadingHandlers = new ArrayList<>();
    public static final Set<Item> noDualUnload = new HashSet<>();

    static {
        loadingHandlers.add(new FluidLoaderStandard());
        loadingHandlers.add(new FluidLoaderFillableItem());
        loadingHandlers.add(new FluidLoaderInfinite());
    }

    protected int capacity;
    protected int pressure = 0;
    protected Fluid conformedFluid = Fluids.EMPTY;
    protected final IPlatformFluidHandler backend;

    public FluidTank(int capacity) {
        this.capacity = capacity;
        this.backend = FluidHooks.createFluidHandler(capacity, this::isFluidValid, this::onContentsChanged,
            fluid -> {
                Fluid stored = getStoredFluid();
                if (stored != null && stored != Fluids.EMPTY) {
                    return VanillaFluidEquivalence.sameSubstance(stored, fluid) ? stored : fluid;
                }
                Fluid logicalType = getConfiguredFluid();
                if (logicalType != Fluids.EMPTY && logicalType != ModFluids.NONE.getSource() && VanillaFluidEquivalence.sameSubstance(logicalType, fluid)) {
                    return logicalType;
                }
                Fluid norm = VanillaFluidEquivalence.forVanillaContainerFill(fluid);
                return norm != fluid ? norm : fluid;
            },
            fluid -> {
                Fluid norm = VanillaFluidEquivalence.forVanillaContainerFill(fluid);
                return norm != fluid ? norm : fluid;
            }
        );
    }

    public FluidTank(Fluid type, int capacity) {
        this(capacity);
        this.conform(type);
    }

    public void onContentsChanged() {}

    public boolean isFluidValid(Fluid fluid) {
        if (pressure != 0) return false;
        if (conformedFluid != Fluids.EMPTY && conformedFluid != ModFluids.NONE.getSource() && !VanillaFluidEquivalence.sameSubstance(conformedFluid, fluid)) return false;
        return true;
    }

    @NotNull
    public Fluid getStoredFluid() { return backend.getStoredFluid(); }

    public int getFluidAmountMb() { return backend.getFluidAmountMb(); }
    public int getCapacityMb() { return capacity; }
    public int getSpaceMb() { return capacity - getFluidAmountMb(); }
    public boolean isEmpty() { return getFluidAmountMb() <= 0; }

    public long getDynamicNetworkSpeedMb(long maxSpeedMbPerTick, boolean sending) {
        if (maxSpeedMbPerTick <= 0) return 0L;
        int cap = getCapacityMb();
        if (cap <= 0) return 0L;
        int fill = getFluidAmountMb();
        int space = Math.max(0, cap - fill);
        int weight = sending ? fill : space;
        if (weight <= 0) return 0L;
        long scaled = (maxSpeedMbPerTick * (long) weight) / (long) cap;
        return Math.max(1L, Math.min(maxSpeedMbPerTick, scaled));
    }

    @NotNull
    public Fluid getConfiguredFluid() {
        Fluid stored = getStoredFluid();
        return stored != Fluids.EMPTY ? stored : conformedFluid;
    }

    public int fillMb(Fluid fluid, int amountMb, boolean simulate) {
        if (amountMb <= 0 || fluid == Fluids.EMPTY || fluid == null) return 0;
        if (getFluidAmountMb() <= 0 && !isFluidTypeExplicitlySet(conformedFluid) && fluid != ModFluids.NONE.getSource()) {
            if (!simulate) this.conformedFluid = fluid;
        }
        return backend.fillMb(fluid, amountMb, simulate);
    }

    public int fillMb(Fluid fluid, int amountMb) { return fillMb(fluid, amountMb, false); }
    public int drainMb(int amountMb, boolean simulate) { return backend.drainMb(amountMb, simulate); }
    public int drainMb(int amountMb) { return drainMb(amountMb, false); }
    
    public int fillInternal(Fluid fluid, int amountMb, boolean simulate) { return fillMb(fluid, amountMb, simulate); }
    public int fillInternal(Fluid fluid, int amountMb) { return fillMb(fluid, amountMb, false); }
    public int drainInternal(int amountMb, boolean simulate) { return drainMb(amountMb, simulate); }
    public int drainInternal(int amountMb) { return drainMb(amountMb, false); }

    public void setFluid(Fluid fluid, int amountMb) { backend.setFluid(fluid, amountMb); }

    public Object getCapability() { return backend.getCapability(); }

    //? if forge {
    /** Типизированный forge-доступ к капабилити бака (LazyOptional&lt;IFluidHandler&gt;) для BE.getCapability. */
    @SuppressWarnings("unchecked")
    public net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler> getForgeFluidCapability() {
        return (net.minecraftforge.common.util.LazyOptional<net.minecraftforge.fluids.capability.IFluidHandler>) getCapability();
    }
    //?}

    public void assignTypeAndZeroFluid(Fluid newType) {
        if (!isEmpty()) drainMb(getFluidAmountMb());
        this.conformedFluid = newType == null ? Fluids.EMPTY : newType;
    }

    public FluidTank withPressure(int pressure) {
        if (this.pressure != pressure) this.setFill(0);
        this.pressure = pressure;
        return this;
    }

    public FluidTank conform(Fluid type) {
        if (type == null) type = Fluids.EMPTY;
        if (!isEmpty() && !VanillaFluidEquivalence.sameSubstance(getStoredFluid(), type)) drainMb(getFluidAmountMb());
        this.conformedFluid = type;
        return this;
    }

    public FluidTank conform(Fluid type, int pressure) {
        this.conform(type);
        this.withPressure(pressure);
        return this;
    }

    public void setTankType(Fluid type) { conform(type); }

    public void resetTank() {
        drainMb(getFluidAmountMb());
        this.conformedFluid = ModFluids.NONE.getSource();
        this.pressure = 0;
    }
    
    public Fluid getTankType() { return getConfiguredFluid(); }
    public int getFill() { return getFluidAmountMb(); }
    public int getMaxFill() { return getCapacityMb(); }
    public int getPressure() { return pressure; }

    /**
     * 1.7.10 tanks always carry a type, so {@code setFill} on an empty one simply filled it.
     * Here {@link #getStoredFluid()} returns {@link Fluids#EMPTY} while the tank is empty, which
     * made {@code setFill} on an empty-but-typed tank a silent no-op - a boiler starting from
     * zero could never accumulate its first millibucket of steam. Fall back to the configured
     * type so a typed tank behaves the way the original's callers expect. An untyped tank has no
     * configured fluid either, so it is unaffected.
     */
    public void setFill(int amount) {
        Fluid target = getStoredFluid();
        if (target == Fluids.EMPTY) {
            Fluid configured = getConfiguredFluid();
            if (configured != Fluids.EMPTY && configured != ModFluids.NONE.getSource()) target = configured;
        }
        setFluid(target, Mth.clamp(amount, 0, capacity));
    }
    public void fill(int amount) { setFill(amount); }

    public int changeTankSize(int size) {
        int oldAmt = getFluidAmountMb();
        this.capacity = size;
        backend.setCapacityMb(size);
        if (oldAmt > size) {
            setFluid(getStoredFluid(), size);
            return oldAmt - size;
        }
        return 0;
    }

    public boolean loadTank(int in, int out, ItemStack[] slots) {
        if (slots[in] == null || slots[in].isEmpty()) return false;
        boolean isInfiniteBarrel = slots[in].getItem() instanceof InfiniteFluidItem inf && inf.isInstantNetwork();
        if (!isInfiniteBarrel && pressure != 0) return false;
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
        if (slots[in] == null || slots[in].isEmpty() || !(slots[in].getItem() instanceof FluidIdentifierItem)) return false;
        Fluid newType = FluidIdentifierItem.resolvePrimaryForTank(slots[in]);
        if (newType == null) return false;
        if (in == out) {
            if (getConfiguredFluid() == newType) return false;
            this.conformedFluid = newType;
            drainMb(getFluidAmountMb());
            return true;
        } else {
            if (slots[out] != null && !slots[out].isEmpty()) return false;
            if (getConfiguredFluid() == newType) return false;
            this.conformedFluid = newType;
            drainMb(getFluidAmountMb());
            slots[out] = slots[in].copy();
            slots[in] = ItemStack.EMPTY;
            return true;
        }
    }

    public boolean setType(int in, ItemStack[] slots) { return setType(in, in, slots); }

    public CompoundTag writeNBT(CompoundTag tag) {
        backend.writeNBT(tag);
        if (conformedFluid != Fluids.EMPTY) {
            ResourceLocation loc = BuiltInRegistries.FLUID.getKey(conformedFluid);
            if (loc != null) tag.putString("ConformedFluid", loc.toString());
        }
        tag.putShort("Pressure", (short) pressure);
        return tag;
    }

    public void readNBT(CompoundTag tag) {
        backend.readNBT(tag);
        if (tag.contains("ConformedFluid")) {
            Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(tag.getString("ConformedFluid")));
            conformedFluid = f != null ? f : Fluids.EMPTY;
        } else {
            conformedFluid = Fluids.EMPTY;
        }
        if (tag.contains("Pressure")) pressure = tag.getShort("Pressure");
    }

    // ──────────────── МЕТОДЫ ОБРАТНОЙ СОВМЕСТИМОСТИ ────────────────

    public CompoundTag writeToNBT(CompoundTag tag, String key) {
        CompoundTag subTag = new CompoundTag();
        this.writeNBT(subTag);
        tag.put(key, subTag);
        return tag;
    }

    public void readFromNBT(CompoundTag tag, String key) {
        if (tag.contains(key)) {
            this.readNBT(tag.getCompound(key));
        }
    }

    public dev.architectury.fluid.FluidStack getFluid() {
        Fluid f = getStoredFluid();
        if (f == null || f == Fluids.EMPTY) return dev.architectury.fluid.FluidStack.empty();
        return dev.architectury.fluid.FluidStack.create(f, getFluidAmountMb());
    }

    public IPlatformFluidHandler getBackend() {
        return this.backend;
    }

    public void serialize(FriendlyByteBuf buf) {
        buf.writeInt(getFluidAmountMb());
        buf.writeInt(capacity);
        ResourceLocation loc = BuiltInRegistries.FLUID.getKey(getStoredFluid());
        ResourceLocation cLoc = BuiltInRegistries.FLUID.getKey(conformedFluid);
        buf.writeResourceLocation(loc != null ? loc : ResourceLocation.tryParse("minecraft:empty"));
        buf.writeResourceLocation(cLoc != null ? cLoc : ResourceLocation.tryParse("minecraft:empty"));
        buf.writeShort((short) pressure);
    }

    public void deserialize(FriendlyByteBuf buf) {
        int amt = buf.readInt();
        this.capacity = buf.readInt();
        backend.setCapacityMb(this.capacity);
        
        Fluid f = BuiltInRegistries.FLUID.get(buf.readResourceLocation());
        this.conformedFluid = BuiltInRegistries.FLUID.get(buf.readResourceLocation());
        Fluid resolved = (f != null) ? f : Fluids.EMPTY;
        setFluid(resolved, amt);
        pressure = buf.readShort();
    }

    @Override
    public FluidTank clone() {
        try {
            FluidTank newTank = new FluidTank(this.capacity);
            newTank.conformedFluid = this.conformedFluid;
            newTank.pressure = this.pressure;
            newTank.setFluid(this.getStoredFluid(), this.getFluidAmountMb());
            return newTank;
        } catch (Exception e) {
            throw new AssertionError();
        }
    }
    
    static boolean canPlaceItemInSlot(ItemStack[] slots, int slotOut, ItemStack resultStack) {
        if (resultStack.isEmpty()) return true;
        ItemStack stackInSlot = slots[slotOut];
        if (stackInSlot == null || stackInSlot.isEmpty()) return true;
        return FluidHooks.areItemsStackable(stackInSlot, resultStack) && stackInSlot.getCount() + resultStack.getCount() <= stackInSlot.getMaxStackSize();
    }

    static void placeItemInSlot(ItemStack[] slots, int slotOut, ItemStack resultStack) {
        if (resultStack.isEmpty()) return;
        if (slots[slotOut] == null || slots[slotOut].isEmpty()) slots[slotOut] = resultStack;
        else slots[slotOut].grow(resultStack.getCount());
    }

    public static boolean isFluidTypeExplicitlySet(Fluid type) {
        return type != null && type != Fluids.EMPTY && type != ModFluids.NONE.getSource();
    }

    //? if forge {
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    //?} elif fabric {
    /*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
    *///?} elif neoforge {
    /*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    *///?}
    public void renderTank(net.minecraft.client.gui.GuiGraphics guiGraphics, int x, int y, int width, int height) {
        renderTank(guiGraphics, x, y, width, height, 0);
    }

    //? if forge {
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    //?} elif fabric {
    /*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
    *///?} elif neoforge {
    /*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    *///?}
    public void renderTank(net.minecraft.client.gui.GuiGraphics guiGraphics, int x, int y, int width, int height, int orientation) {
        Fluid drawType = getConfiguredFluid();
        int fluidAmt = getFluidAmountMb();

        if (fluidAmt <= 0 || drawType == null || drawType == Fluids.EMPTY || drawType == ModFluids.NONE.getSource()) return;

        int fluidColor = com.hbm_m.api.fluids.HbmFluidRegistry.getTintColor(drawType) & 0xFFFFFF;
        float r = (fluidColor >> 16 & 255) / 255.0F;
        float g = (fluidColor >> 8 & 255) / 255.0F;
        float b = (fluidColor & 255) / 255.0F;

        dev.architectury.fluid.FluidStack fStack = dev.architectury.fluid.FluidStack.create(drawType, fluidAmt);
        ResourceLocation fluidPng = com.hbm_m.client.gui.FluidGuiRendering.guiTexturePngForFluid(drawType, fStack);
        if (fluidPng == null) return;

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, g, b, 1.0F);

        if (orientation == 0) {
            int pixelHeight = (int) ((long) fluidAmt * height / capacity);
            if (pixelHeight == 0 && fluidAmt > 0) pixelHeight = 1;
            if (pixelHeight > height) pixelHeight = height;

            com.hbm_m.client.gui.FluidGuiRendering.renderTiledFluid(guiGraphics, fluidPng, x, y + height - pixelHeight, width, pixelHeight);
        } else if (orientation == 1) {
            int pixelWidth = (int) ((long) fluidAmt * width / capacity);
            if (pixelWidth == 0 && fluidAmt > 0) pixelWidth = 1;
            if (pixelWidth > width) pixelWidth = width;

            com.hbm_m.client.gui.FluidGuiRendering.renderTiledFluid(guiGraphics, fluidPng, x, y, pixelWidth, height);
        }

        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    //? if forge {
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
    //?} elif fabric {
    /*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
    *///?} elif neoforge {
    /*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
    *///?}
    public void renderTankInfo(net.minecraft.client.gui.GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, int mouseX, int mouseY, int x, int y, int width, int height) {
        if (!(mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height)) return;

        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        boolean shift = net.minecraft.client.gui.screens.Screen.hasShiftDown();
        Fluid drawType = getConfiguredFluid();
        int fluidAmt = getFluidAmountMb();
        com.hbm_m.inventory.fluid.FluidType type = com.hbm_m.inventory.fluid.FluidType.forFluid(drawType);

        lines.add(type.getLocalizedName());
        lines.add(net.minecraft.network.chat.Component.literal(fluidAmt + " / " + capacity + " mB"));

        if (pressure != 0) {
            lines.add(net.minecraft.network.chat.Component.translatable("gui.hbm_m.fluid_tank.pressure", pressure)
                    .withStyle(net.minecraft.ChatFormatting.RED));
            boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
            lines.add(net.minecraft.network.chat.Component.translatable("gui.hbm_m.fluid_tank.pressurized")
                    .withStyle(blink ? net.minecraft.ChatFormatting.RED : net.minecraft.ChatFormatting.DARK_RED));
        }

        type.addInfo(shift, lines);

        guiGraphics.renderTooltip(font, lines, java.util.Optional.empty(), mouseX, mouseY);
    }
}