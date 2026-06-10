package com.hbm_m.inventory.fluid.tank;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.item.liquids.InfiniteFluidItem;

//? if fabric {
/*import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
*///?}
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
//? if forge {
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

@SuppressWarnings("UnstableApiUsage")
public class FluidTank implements Cloneable {

    public static final FluidTank[] EMPTY_ARRAY = new FluidTank[0];

    public interface LoadingHandler {
        boolean emptyItem(ItemStack[] slots, int in, int out, FluidTank tank);
        boolean fillItem(ItemStack[] slots, int in, int out, FluidTank tank);
    }

    /** 1.7.10 {@code public static final List<FluidLoadingHandler> loadingHandlers}: открытый список для регистрации сторонних обработчиков. */
    public static final List<LoadingHandler> loadingHandlers = new ArrayList<>();

    /** 1.7.10 {@code public static final Set<Item> noDualUnload}: предметы, которые нельзя выгружать в дуальном режиме. */
    public static final Set<Item> noDualUnload = new HashSet<>();

    static {
        // Порядок переноса из 1.7.10:
        // Standard -> FillableItem -> Infinite
        loadingHandlers.add(new FluidLoaderStandard());
        loadingHandlers.add(new FluidLoaderFillableItem());
        loadingHandlers.add(new FluidLoaderInfinite());
    }

    protected int capacity;
    protected int pressure = 0;
    protected Fluid conformedFluid = Fluids.EMPTY;

    // ══════════════════ Platform Storage Backings ══════════════════

    //? if forge {
    protected final net.minecraftforge.fluids.capability.templates.FluidTank forgeStorage;
    protected final LazyOptional<IFluidHandler> lazyFluidHandler;
    //?}

    //? if fabric {
    /*public static final long DROPLETS_PER_MB = 81L;
    protected final SingleVariantStorage<FluidVariant> fabricStorage;
    *///?}

    public FluidTank(int capacity) {
        this.capacity = capacity;

        //? if forge {
        this.forgeStorage = new net.minecraftforge.fluids.capability.templates.FluidTank(capacity) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                return FluidTank.this.isFluidValid(stack.getFluid());
            }

            @Override
            public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
                if (resource.isEmpty() || !isFluidValid(resource)) {
                    return 0;
                }
                FluidStack coerced = FluidTank.this.harmonizeForgeFillResource(getFluid(), resource);
                return super.fill(coerced, action);
            }

            @Override
            protected void onContentsChanged() {
                FluidTank.this.onContentsChanged();
            }

            /**
             * При выдаче жидкости наружу приводим HBM-аналоги vanilla water/lava
             * обратно к minecraft:water / minecraft:lava, иначе ванильные вёдра
             * (и другие Forge-контейнеры) откажутся принимать drain.
             */
            @Override
            public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
                FluidStack drained = super.drain(resource, action);
                if (drained.isEmpty()) return drained;
                Fluid normalized = VanillaFluidEquivalence.forVanillaContainerFill(drained.getFluid());
                return normalized != drained.getFluid()
                        ? new FluidStack(normalized, drained.getAmount())
                        : drained;
            }

            @Override
            public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
                FluidStack drained = super.drain(maxDrain, action);
                if (drained.isEmpty()) return drained;
                Fluid normalized = VanillaFluidEquivalence.forVanillaContainerFill(drained.getFluid());
                return normalized != drained.getFluid()
                        ? new FluidStack(normalized, drained.getAmount())
                        : drained;
            }
        };
        this.lazyFluidHandler = LazyOptional.of(() -> forgeStorage);
        //?}

        //? if fabric {
        /*this.fabricStorage = new SingleVariantStorage<FluidVariant>() {
            @Override
            protected FluidVariant getBlankVariant() { return FluidVariant.blank(); }
            @Override
            protected long getCapacity(FluidVariant variant) { return (long) FluidTank.this.capacity * DROPLETS_PER_MB; }
            @Override
            protected boolean canInsert(FluidVariant variant) { return FluidTank.this.isFluidValid(variant.getFluid()); }
            @Override
            protected void onFinalCommit() { FluidTank.this.onContentsChanged(); }
        };
        *///?}
    }

    public FluidTank(Fluid type, int capacity) {
        this(capacity);
        this.conform(type);
    }

    public void onContentsChanged() {}

    //? if forge {
    /**
     * Vanilla Forge {@link net.minecraftforge.fluids.capability.templates.FluidTank} смешивает только при совпадении
     * жидкости и NBT по {@link FluidStack#isFluidEqual(FluidStack)}; {@code minecraft:water} и {@code hbm_m:water} тогда блокируются.
     * Подменяем тип входящего стека на уже хранимый или на логический {@link #conformedFluid} при эквивалентности.
     */
    private FluidStack harmonizeForgeFillResource(FluidStack storedFluid, FluidStack resource) {
        if (resource.isEmpty()) {
            return resource;
        }
        if (!storedFluid.isEmpty()) {
            if (storedFluid.isFluidEqual(resource)) {
                return resource;
            }
            if (VanillaFluidEquivalence.sameSubstance(storedFluid.getFluid(), resource.getFluid())) {
                return new FluidStack(storedFluid.getFluid(), resource.getAmount());
            }
            return resource;
        }
        Fluid logicalType = getConfiguredFluid();
        if (logicalType != Fluids.EMPTY
                && logicalType != ModFluids.NONE.getSource()
                && VanillaFluidEquivalence.sameSubstance(logicalType, resource.getFluid())) {
            return new FluidStack(logicalType, resource.getAmount());
        }
        Fluid normalized = VanillaFluidEquivalence.forVanillaContainerFill(resource.getFluid());
        return normalized != resource.getFluid()
                ? new FluidStack(normalized, resource.getAmount())
                : resource;
    }

    //?}

    public boolean isFluidValid(Fluid fluid) {
        if (pressure != 0) return false;
        if (conformedFluid != Fluids.EMPTY && conformedFluid != ModFluids.NONE.getSource() && !VanillaFluidEquivalence.sameSubstance(conformedFluid, fluid)) return false;
        return true;
    }

    // ══════════════════ Modern Unified API ══════════════════

    @NotNull
    public Fluid getStoredFluid() {
        //? if forge {
        return forgeStorage.getFluid().isEmpty() ? Fluids.EMPTY : forgeStorage.getFluid().getFluid();
        //?}
        //? if fabric {
        /*return fabricStorage.getResource().isBlank() ? Fluids.EMPTY : fabricStorage.getResource().getFluid();
         *///?}
    }

    public int getFluidAmountMb() {
        //? if forge {
        return forgeStorage.getFluidAmount();
        //?}
        //? if fabric {
        /*return (int) (fabricStorage.getAmount() / DROPLETS_PER_MB);
         *///?}
    }

    public int getCapacityMb() { return capacity; }
    public int getSpaceMb() { return capacity - getFluidAmountMb(); }
    public boolean isEmpty() { return getFluidAmountMb() <= 0; }

    /**
     * Динамический лимит скорости для MK2-сети (анти-осцилляция).
     *
     * <p>Идея из оригинала: max transfer зависит от заполненности. Полный бак отдаёт быстрее, пустой — почти не отдаёт;
     * пустой бак принимает быстро, полный — почти не принимает. Это снижает "пинг-понг" между двумя трансиверами.</p>
     *
     * @param maxSpeedMbPerTick базовая скорость (верхний предел)
     * @param sending true = отдача (providerSpeed), false = приём (receiverSpeed)
     */
    public long getDynamicNetworkSpeedMb(long maxSpeedMbPerTick, boolean sending) {
        if (maxSpeedMbPerTick <= 0) return 0L;
        int cap = getCapacityMb();
        if (cap <= 0) return 0L;

        int fill = getFluidAmountMb();
        int space = Math.max(0, cap - fill);
        int weight = sending ? fill : space;
        if (weight <= 0) return 0L;

        long scaled = (maxSpeedMbPerTick * (long) weight) / (long) cap;
        // Не даём "залипнуть" на 0 при малых объёмах, но и не превышаем max.
        return Math.max(1L, Math.min(maxSpeedMbPerTick, scaled));
    }

    @NotNull
    public Fluid getConfiguredFluid() {
        Fluid stored = getStoredFluid();
        return stored != Fluids.EMPTY ? stored : conformedFluid;
    }

    public int fillMb(Fluid fluid, int amountMb) {
        if (amountMb <= 0 || fluid == Fluids.EMPTY || fluid == null) return 0;
        // Если бак был "без типа" (NONE) и пустой, но в него начали заливать жидкость,
        // фиксируем тип, чтобы после опустошения он не терял "коммит" для труб/визуала/сети.
        if (getFluidAmountMb() <= 0 && !isFluidTypeExplicitlySet(conformedFluid) && fluid != ModFluids.NONE.getSource()) {
            this.conformedFluid = fluid;
        }
        //? if forge {
        return forgeStorage.fill(new FluidStack(fluid, amountMb), IFluidHandler.FluidAction.EXECUTE);
        //?}
        //? if fabric {
        /*if (!isFluidValid(fluid)) return 0;
        try (Transaction tx = Transaction.openOuter()) {
            long inserted = fabricStorage.insert(FluidVariant.of(fluid), (long) amountMb * DROPLETS_PER_MB, tx);
            tx.commit();
            return (int) (inserted / DROPLETS_PER_MB);
        }
        *///?}
    }

    public int drainMb(int amountMb) {
        if (amountMb <= 0) return 0;
        //? if forge {
        return forgeStorage.drain(amountMb, IFluidHandler.FluidAction.EXECUTE).getAmount();
        //?}
        //? if fabric {
        /*try (Transaction tx = Transaction.openOuter()) {
            long extracted = fabricStorage.extract(fabricStorage.getResource(), (long) amountMb * DROPLETS_PER_MB, tx);
            tx.commit();
            return (int) (extracted / DROPLETS_PER_MB);
        }
        *///?}
    }

    public int fillInternal(Fluid fluid, int amountMb) {
        if (amountMb <= 0 || fluid == Fluids.EMPTY || fluid == null) return 0;
        //? if forge {
        return forgeStorage.fill(new FluidStack(fluid, amountMb), IFluidHandler.FluidAction.EXECUTE);
        //?}
        //? if fabric {
        /*try (Transaction tx = Transaction.openOuter()) {
            long inserted = fabricStorage.insert(FluidVariant.of(fluid), (long) amountMb * DROPLETS_PER_MB, tx);
            tx.commit();
            return (int) (inserted / DROPLETS_PER_MB);
        }
        *///?}
    }

    public int drainInternal(int amountMb) {
        return drainMb(amountMb);
    }

    public void setFluid(Fluid fluid, int amountMb) {
        //? if forge {
        forgeStorage.setFluid(fluid == Fluids.EMPTY || fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, amountMb));
        //?}
        //? if fabric {
        /*// Важно для Fabric: variant и amount живут отдельно. Если оставить variant != blank при amount == 0,
        // то "пустой" бак будет считаться имеющим тип storedFluid, и conformedFluid (тип, заданный идентификатором)
        // не сможет переопределить отображение/логику после перезахода в мир.
        if (fluid == null || fluid == Fluids.EMPTY || amountMb <= 0) {
            fabricStorage.variant = FluidVariant.blank();
            fabricStorage.amount = 0L;
        } else {
            fabricStorage.variant = FluidVariant.of(fluid);
            fabricStorage.amount = (long) amountMb * DROPLETS_PER_MB;
        }
        *///?}
    }

    // ══════════════════ Capabilities Exposure ══════════════════

    //? if forge {
    public LazyOptional<IFluidHandler> getCapability() { return lazyFluidHandler; }
    //?}
    //? if fabric {
    /*public SingleVariantStorage<FluidVariant> getStorage() { return fabricStorage; }
     *///?}

    // ══════════════════ Legacy 1.7.10 Methods & Aliases ══════════════════

    public void assignTypeAndZeroFluid(Fluid newType) {
        if (!isEmpty()) {
            drainMb(getFluidAmountMb());
        }
        this.conformedFluid = newType == null ? Fluids.EMPTY : newType;
    }

    public FluidTank withPressure(int pressure) {
        // 1.7.10: при смене давления зануляем только fill, тип НЕ трогаем.
        if (this.pressure != pressure) this.setFill(0);
        this.pressure = pressure;
        return this;
    }

    public FluidTank conform(Fluid type) {
        if (type == null) type = Fluids.EMPTY;
        if (!isEmpty() && !VanillaFluidEquivalence.sameSubstance(getStoredFluid(), type)) {
            // Важно: ванильные жидкости могут отличаться инстансом (source vs flowing),
            // но при этом быть одной и той же "субстанцией". Такие переключения не должны дренить бак.
            drainMb(getFluidAmountMb());
        }
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
        // 1.7.10: type = Fluids.NONE; fluid = 0; pressure = 0;
        // У нас "Fluids.NONE" — это {@code ModFluids.NONE.getSource()} (HBM-«пусто», не ваниль EMPTY).
        drainMb(getFluidAmountMb());
        this.conformedFluid = ModFluids.NONE.getSource();
        this.pressure = 0;
    }
    public Fluid getTankType() { return getConfiguredFluid(); }
    public int getFill() { return getFluidAmountMb(); }
    public int getMaxFill() { return getCapacityMb(); }
    public int getPressure() { return pressure; }

    public void setFill(int amount) { setFluid(getStoredFluid(), Mth.clamp(amount, 0, capacity)); }
    public void fill(int amount) { setFill(amount); }

    public int changeTankSize(int size) {
        int oldAmt = getFluidAmountMb();
        this.capacity = size;
        //? if forge {
        forgeStorage.setCapacity(size);
        //?}
        if (oldAmt > size) {
            setFluid(getStoredFluid(), size);
            return oldAmt - size;
        }
        return 0;
    }

    public boolean loadTank(int in, int out, ItemStack[] slots) {
        if (slots[in] == null || slots[in].isEmpty()) return false;

        // 1.7.10: проверка строго на универсальную бесконечную бочку (fluid_barrel_infinite),
        // в 1.20.1 этому соответствует instant-network-инстанс {@link InfiniteFluidItem}.
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

    /**
     * Прямой порт 1.7.10 {@code FluidTank.setType(in, out, slots)}.
     *
     * <p>Семантика:
     * <ul>
     *   <li>{@code slots[in]} обязателен и должен быть {@link FluidIdentifierItem};</li>
     *   <li>если новый тип совпадает с текущим — возвращается {@code false} без изменений;</li>
     *   <li>если {@code in == out}: тип меняется, fluid обнуляется;</li>
     *   <li>если {@code in != out}: дополнительно требуется, чтобы {@code slots[out]} был пустым,
     *       иначе возвращается {@code false} без изменений; стэк копируется в out, in зануляется.</li>
     * </ul>
     */
    public boolean setType(int in, int out, ItemStack[] slots) {
        if (slots[in] == null || slots[in].isEmpty() || !(slots[in].getItem() instanceof FluidIdentifierItem)) {
            return false;
        }
        Fluid newType = FluidIdentifierItem.resolvePrimaryForTank(slots[in]);
        if (newType == null) return false;

        if (in == out) {
            if (getConfiguredFluid() == newType) return false;
            // 1.7.10: type = newType; fluid = 0;
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

    public boolean setType(int in, ItemStack[] slots) {
        return setType(in, in, slots);
    }

    // ══════════════════ NBT & Serialization ══════════════════

    public void writeToNBT(CompoundTag nbt, String prefix) {
        nbt.putInt(prefix + "_amount", getFluidAmountMb());
        nbt.putInt(prefix + "_max", capacity);
        //? if forge {
        ResourceLocation loc = BuiltInRegistries.FLUID.getKey(getConfiguredFluid());
        //?}
        //? if fabric {
        /*ResourceLocation loc = BuiltInRegistries.FLUID.getKey(getConfiguredFluid());
         *///?}
        nbt.putString(prefix + "_type", loc != null ? loc.toString() : "minecraft:empty");
        nbt.putShort(prefix + "_p", (short) pressure);
    }

    public void readFromNBT(CompoundTag nbt, String prefix) {
        if (!nbt.contains(prefix + "_amount")) return;
        int amt = nbt.getInt(prefix + "_amount");
        capacity = nbt.getInt(prefix + "_max");
        //? if forge {
        forgeStorage.setCapacity(capacity);
        //?}

        String typeIdStr = nbt.getString(prefix + "_type");
        //? if forge {
        Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(typeIdStr));
        Fluid resolved = (f != null) ? f : Fluids.EMPTY;
        this.conformedFluid = resolved;
        setFluid(resolved, amt);
        //?}
        //? if fabric {
        /*Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(typeIdStr));
        Fluid resolved = (f != null) ? f : Fluids.EMPTY;
        this.conformedFluid = resolved;
        setFluid(resolved, amt);
        *///?}
        pressure = nbt.getShort(prefix + "_p");
    }

    public CompoundTag writeNBT(CompoundTag tag) {
        //? if forge {
        forgeStorage.writeToNBT(tag);
        //?}
        //? if fabric {
        /*FluidVariant variant = fabricStorage.getResource();
        if (!variant.isBlank()) {
            tag.merge(variant.toNbt());
            tag.putLong("Amount", fabricStorage.getAmount());
        }
        *///?}
        if (conformedFluid != Fluids.EMPTY) {
            //? if forge {
            ResourceLocation loc = BuiltInRegistries.FLUID.getKey(conformedFluid);
            //?}
            //? if fabric {
            /*ResourceLocation loc = BuiltInRegistries.FLUID.getKey(conformedFluid);
             *///?}
            if (loc != null) tag.putString("ConformedFluid", loc.toString());
        }
        tag.putShort("Pressure", (short) pressure);
        return tag;
    }

    public void readNBT(CompoundTag tag) {
        //? if forge {
        forgeStorage.readFromNBT(tag);
        //?}
        //? if fabric {
        /*try (Transaction tx = Transaction.openOuter()) {
            FluidVariant cur = fabricStorage.getResource();
            long curAmt = fabricStorage.getAmount();
            if (!cur.isBlank() && curAmt > 0) {
                fabricStorage.extract(cur, curAmt, tx);
            }

            if (tag.contains("ConformedFluid")) {
                FluidVariant variant = FluidVariant.fromNbt(tag);
                long amount = tag.getLong("Amount");
                if (!variant.isBlank() && amount > 0) {
                    fabricStorage.insert(variant, amount, tx);
                }
            }
            tx.commit();
        }
        *///?}
        if (tag.contains("ConformedFluid")) {
            //? if forge {
            Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(tag.getString("ConformedFluid")));
            //?}
            //? if fabric {
            /*Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(tag.getString("ConformedFluid")));
             *///?}
            conformedFluid = f != null ? f : Fluids.EMPTY;
        } else {
            conformedFluid = Fluids.EMPTY;
        }
        if (tag.contains("Pressure")) {
            pressure = tag.getShort("Pressure");
        }
    }

    public void serialize(FriendlyByteBuf buf) {
        buf.writeInt(getFluidAmountMb());
        buf.writeInt(capacity);
        //? if forge {
        ResourceLocation loc = BuiltInRegistries.FLUID.getKey(getStoredFluid());
        ResourceLocation cLoc = BuiltInRegistries.FLUID.getKey(conformedFluid);
        //?}
        //? if fabric {
        /*ResourceLocation loc = BuiltInRegistries.FLUID.getKey(getStoredFluid());
        ResourceLocation cLoc = BuiltInRegistries.FLUID.getKey(conformedFluid);
         *///?}
        buf.writeResourceLocation(loc != null ? loc : ResourceLocation.tryParse("minecraft:empty"));
        buf.writeResourceLocation(cLoc != null ? cLoc : ResourceLocation.tryParse("minecraft:empty"));
        buf.writeShort((short) pressure);
    }

    public void deserialize(FriendlyByteBuf buf) {
        int amt = buf.readInt();
        this.capacity = buf.readInt();
        //? if forge {
        forgeStorage.setCapacity(this.capacity);
        Fluid f = BuiltInRegistries.FLUID.get(buf.readResourceLocation());
        this.conformedFluid = BuiltInRegistries.FLUID.get(buf.readResourceLocation());
        //?}
        //? if fabric {
        /*Fluid f = BuiltInRegistries.FLUID.get(buf.readResourceLocation());
        this.conformedFluid = BuiltInRegistries.FLUID.get(buf.readResourceLocation());
         *///?}
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
        //? if < 1.21.1 {
        return ItemStack.isSameItemSameTags(stackInSlot, resultStack) &&
        //?} else {
        /*return ItemStack.isSameItemSameComponents(stackInSlot, resultStack) &&
        *///?}
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

    // ══════════════════ Client-side rendering ══════════════════

    public static boolean isFluidTypeExplicitlySet(Fluid type) {
        if (type == null || type == Fluids.EMPTY) return false;
        return type != ModFluids.NONE.getSource();
    }

    //? if forge {
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
            //?}
            //? if fabric {
    /*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
     *///?}
    public void renderTank(net.minecraft.client.gui.GuiGraphics guiGraphics, int x, int y, int width, int height) {
        renderTank(guiGraphics, x, y, width, height, 0);
    }

    //? if forge {
    @net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
            //?}
            //? if fabric {
    /*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
     *///?}
    public void renderTank(net.minecraft.client.gui.GuiGraphics guiGraphics, int x, int y, int width, int height, int orientation) {
        Fluid drawType = getConfiguredFluid();
        int fluidAmt = getFluidAmountMb();

        if (fluidAmt <= 0 || drawType == null || drawType == Fluids.EMPTY || drawType == ModFluids.NONE.getSource()) return;

        int fluidColor = com.hbm_m.api.fluids.HbmFluidRegistry.getTintColor(drawType) & 0xFFFFFF;
        float r = (fluidColor >> 16 & 255) / 255.0F;
        float g = (fluidColor >> 8 & 255) / 255.0F;
        float b = (fluidColor & 255) / 255.0F;

        //? if forge {
        dev.architectury.fluid.FluidStack fStack = dev.architectury.fluid.FluidStack.create(drawType, fluidAmt);
        //?}
        //? if fabric {
        /*dev.architectury.fluid.FluidStack fStack = dev.architectury.fluid.FluidStack.create(drawType, fluidAmt);
         *///?}

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
            //?}
            //? if fabric {
    /*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
     *///?}
    public void renderTankInfo(net.minecraft.client.gui.GuiGraphics guiGraphics, net.minecraft.client.gui.Font font, int mouseX, int mouseY, int x, int y, int width, int height) {
        if (!(mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height)) return;

        // Direct port of 1.7.10 FluidTank.renderTankInfo:
        //   list.add(this.type.getLocalizedName());
        //   list.add(fluid + "/" + maxFluid + "mB");
        //   if(pressure != 0) { ... }
        //   type.addInfo(list);
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
