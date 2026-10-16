package com.hbm_m.item.liquids;

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.platform.PlatformHooks;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
// Forge/NeoForge: сигнатуры IFluidHandlerItem идентичны, различаются только пакеты.
//? if forge {
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;
import net.minecraft.core.Direction;
//?}
//? if neoforge {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
*///?}

/**
 * Infinite fluid item (port of 1.7.10 ItemInfiniteFluid).
 *
 * Может быть:
 * - настроенным на конкретную жидкость (например "infinite water") и работать как источник+поглотитель
 *   со скоростью {@code transferRate}
 * - универсальным (fluid_barrel_infinite): тип по NBT (или по запросу), и может использоваться как "instant" для сети
 *
 * Архитектура (по образцу ModFluidTank): вся логика — в {@link InfiniteAccess} (mB, без loader-API).
 * Forge — initCapabilities, NeoForge — ModCapabilities.register (RegisterCapabilitiesEvent).
 */
@SuppressWarnings("UnstableApiUsage")
public class InfiniteFluidItem extends Item implements ITooltipProvider {

    private final int transferRate; // mB per transfer (e.g. 1_000_000_000 like 1.7.10)
    @Nullable
    private final Fluid fixedFluid;
    private final boolean instantNetwork;

    /** Универсальная бесконечная бочка (тип берётся из NBT или запроса), instant для сети. */
    public InfiniteFluidItem(Properties properties, int transferRate) {
        this(properties, null, transferRate, true);
    }

    /** Бесконечная бочка конкретной жидкости (например вода), не instant (работает со скоростью). */
    public InfiniteFluidItem(Properties properties, @Nullable Fluid fixedFluid, int transferRate) {
        this(properties, fixedFluid, transferRate, false);
    }

    private InfiniteFluidItem(Properties properties, @Nullable Fluid fixedFluid, int transferRate, boolean instantNetwork) {
        super(properties);
        this.transferRate = transferRate;
        this.fixedFluid = fixedFluid;
        this.instantNetwork = instantNetwork;
    }

    /**
     * Тип из NBT (для возможностей предмета / внешних потребителей). Цистерна с {@link InfiniteFluidItem}
     * наполняется типом, заданным идентификатором на баке, без опоры на этот тег.
     */
    public Fluid getFluidType(ItemStack stack) {
        if (fixedFluid != null && fixedFluid != Fluids.EMPTY) {
            return fixedFluid;
        }
        if (PlatformHooks.hasItemTag(stack) && PlatformHooks.contains(stack, "FluidType")) {
            return BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(PlatformHooks.getString(stack, "FluidType")));
        }
        return Fluids.EMPTY;
    }

    /** true только для универсальной бесконечной бочки (fluid_barrel_infinite). */
    public boolean isInstantNetwork() {
        return instantNetwork;
    }

    // Геттеры для регистрации NeoForge item-капабилити (ModCapabilities.register) —
    // платформенный glue: на NeoForge нет initCapabilities, скорость/тип нужны регистратору.
    public int getTransferRate() {
        return transferRate;
    }

    @Nullable
    public Fluid getFixedFluid() {
        return fixedFluid;
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§bInfinite Fluid"));
        tooltip.add(Component.literal("§7Output Rate: §e" + transferRate + " mB/t"));
    }

    // ================================================================== //
    //  ОБЩАЯ ЛОГИКА (без loader-API, mB) — аналог ModFluidTank            //
    // ================================================================== //

    /**
     * Платформенно-независимая логика бесконечной бочки: бесконечный drain/fill
     * с ограничением по {@code rate} (mB).
     */
    public static class InfiniteAccess {
        protected final ItemStack container;
        protected final int rate;
        @Nullable
        protected final Fluid fixedFluid;

        public InfiniteAccess(ItemStack container, int rate, @Nullable Fluid fixedFluid) {
            this.container = container;
            this.rate = rate;
            this.fixedFluid = fixedFluid;
        }

        public Fluid getConfiguredFluid() {
            if (fixedFluid != null && fixedFluid != Fluids.EMPTY) return fixedFluid;
            if (PlatformHooks.hasItemTag(container) && PlatformHooks.contains(container, "FluidType")) {
                return BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(PlatformHooks.getString(container, "FluidType")));
            }
            return Fluids.EMPTY;
        }

        /** Возвращает фактически поглощённый объём (mB). */
        public int fill(Fluid fluid, int amount, boolean simulate) {
            if (fluid == null || amount <= 0) return 0;
            Fluid type = getConfiguredFluid();
            // Если не настроена — поглощаем любой тип (универсальная бочка).
            // Если настроена — поглощаем только тот же substance (для ванильных water/lava).
            if (type == Fluids.EMPTY || com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(type, fluid)) {
                return Math.min(amount, rate);
            }
            return 0;
        }

        /** Возвращает слитую жидкость (mB) — бесконечный источник с ограничением rate. */
        public dev.architectury.fluid.FluidStack drain(int maxDrain, boolean simulate) {
            Fluid type = getConfiguredFluid();
            if (type == Fluids.EMPTY) return dev.architectury.fluid.FluidStack.empty();
            return dev.architectury.fluid.FluidStack.create(type, Math.min(maxDrain, rate));
        }

        public dev.architectury.fluid.FluidStack drain(Fluid fluid, int maxDrain, boolean simulate) {
            if (fluid == null) return dev.architectury.fluid.FluidStack.empty();
            Fluid type = getConfiguredFluid();
            if (type != Fluids.EMPTY && !com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(type, fluid)) {
                return dev.architectury.fluid.FluidStack.empty();
            }
            return dev.architectury.fluid.FluidStack.create(fluid, Math.min(maxDrain, rate));
        }
    }

    // ================================================================== //
    //  FORGE / NEOFORGE — тонкий адаптер IFluidHandlerItem над InfiniteAccess //
    // ================================================================== //

    //? if forge || neoforge {
    public static class InfiniteFluidCapabilityHandler implements IFluidHandlerItem {
        protected final InfiniteAccess access;

        public InfiniteFluidCapabilityHandler(ItemStack container, int rate, @Nullable Fluid fixedFluid) {
            this.access = new InfiniteAccess(container, rate, fixedFluid);
        }

        @Override public ItemStack getContainer() { return access.container; }

        @Override public int getTanks() { return 1; }

        @Override public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            if (resource.isEmpty()) return 0;
            return access.fill(resource.getFluid(), resource.getAmount(), action.simulate());
        }

        @Override
        public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
            return toPlatform(access.drain(maxDrain, action.simulate()));
        }

        @Override
        public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
            if (resource.isEmpty()) return FluidStack.EMPTY;
            return toPlatform(access.drain(resource.getFluid(), resource.getAmount(), action.simulate()));
        }

        protected static FluidStack toPlatform(dev.architectury.fluid.FluidStack arch) {
            return arch.isEmpty()
                    ? FluidStack.EMPTY
                    : new FluidStack(arch.getFluid(), (int) arch.getAmount());
        }
    }
    //?}

    //? if forge {
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new InfiniteFluidCapabilityProvider(stack, transferRate, fixedFluid);
    }

    private static class InfiniteFluidCapabilityProvider implements ICapabilityProvider {
        private final InfiniteFluidCapabilityHandler handler;
        private final LazyOptional<IFluidHandlerItem> optional;

        public InfiniteFluidCapabilityProvider(ItemStack stack, int rate, @Nullable Fluid fixedFluid) {
            this.handler = new InfiniteFluidCapabilityHandler(stack, rate, fixedFluid);
            this.optional = LazyOptional.of(() -> handler);
        }

        @NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            if (cap == ForgeCapabilities.FLUID_HANDLER_ITEM) {
                return optional.cast();
            }
            return LazyOptional.empty();
        }
    }
    //?}
}
