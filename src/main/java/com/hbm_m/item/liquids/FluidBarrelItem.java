package com.hbm_m.item.liquids;

import com.hbm_m.item.ITooltipProvider;
import java.util.List;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.platform.PlatformHooks;

// Forge/NeoForge: сигнатуры IFluidHandlerItem идентичны, различаются только пакеты.
// Логика — общая (BarrelAccess ниже), эти импорты нужны лишь обёртке-адаптеру.
//? if forge {
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
//?}
//? if neoforge {
/*import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
*///?}
//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
//?}

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Fluid Barrel - A portable fluid container that can hold 16,000 mB (16 buckets) of any fluid.
 * Similar to a tank but as an item.
 *
 * Архитектура (по образцу ModFluidTank): вся логика — в платформенно-независимом
 * {@link BarrelAccess} (mB, без loader-API). Лоадерные обёртки — тонкие адаптеры:
 * Forge — initCapabilities, NeoForge — ModCapabilities.register (RegisterCapabilitiesEvent).
 */
@SuppressWarnings("UnstableApiUsage")
public class FluidBarrelItem extends Item implements ITooltipProvider {

    public static final int CAPACITY = 16000; // 16 buckets
    public static final String NBT_FLUID = "Fluid";

    public FluidBarrelItem(Properties properties) {
        super(properties.stacksTo(64));
    }

    /**
     * Возвращает правильную вместимость для генерации предметов:
     * 16000 для Forge и 1296000 (16000 * 81) для Fabric.
     */
    public static long getPlatformCapacity() {
        //? if fabric {
        /*return CAPACITY * 81L;
        *///?} else {
        return CAPACITY;
         //?}
    }

    @Override
    public Component getName(ItemStack stack) {
        dev.architectury.fluid.FluidStack fluid = getFluid(stack);
        // Если жидкости нет (например, пустая бочка), возвращаем базовое имя
        if (fluid.isEmpty()) {
            return Component.translatable("item.hbm_m.fluid_barrel.empty");
        }
        // Если жидкость есть, подставляем её переведенное название (например, "Water Barrel")
        return Component.translatable("item.hbm_m.fluid_barrel", fluid.getName());
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {

        dev.architectury.fluid.FluidStack fluid = getFluid(stack);
        if (fluid.isEmpty()) {
            tooltip.add(Component.literal("Empty").withStyle(ChatFormatting.GRAY));
        } else {
            long amount = fluid.getAmount();
            //? if fabric {
            /*amount /= 81L;
            *///?}
            tooltip.add(Component.literal("Fluid: ").withStyle(ChatFormatting.GRAY)
                    .append(fluid.getName().copy().withStyle(ChatFormatting.AQUA)));
            tooltip.add(Component.literal("Amount: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(amount + " / " + CAPACITY + " mB").withStyle(ChatFormatting.YELLOW)));
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        dev.architectury.fluid.FluidStack fluid = getFluid(stack);
        if (fluid.isEmpty()) return false;
        long amount = fluid.getAmount();
        //? if fabric {
        /*amount /= 81L;
        *///?}
        return amount < CAPACITY;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        dev.architectury.fluid.FluidStack fluid = getFluid(stack);
        if (fluid.isEmpty()) return 0;
        long amount = fluid.getAmount();
        //? if fabric {
        /*amount /= 81L;
        *///?}
        return Math.round(13.0F * amount / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFFF00;
    }

    //? if forge {
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FluidBarrelCapabilityProvider(stack);
    }
    //?}

    // Static helper methods for NBT access
    public static dev.architectury.fluid.FluidStack getFluid(ItemStack stack) {
        CompoundTag tag = PlatformHooks.getItemTag(stack);
        if (tag != null && tag.contains(NBT_FLUID)) {
            CompoundTag fluidTag = tag.getCompound(NBT_FLUID);
            // Читаем напрямую по ключам, чтобы избежать проблем с разными форматами Architectury
            Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluidTag.getString("id")));
            long amount = fluidTag.getLong("amount");

            if (f != Fluids.EMPTY && amount > 0) {
                f = VanillaFluidEquivalence.forVanillaContainerFill(f);
                return dev.architectury.fluid.FluidStack.create(f, amount);
            }
        }
        return dev.architectury.fluid.FluidStack.empty();
    }

    public static void setFluid(ItemStack stack, dev.architectury.fluid.FluidStack fluid) {
        if (fluid.isEmpty() || fluid.getAmount() <= 0) {
            PlatformHooks.remove(stack, NBT_FLUID);
            // Если после удаления тега он пустой — удаляем весь CompoundTag, чтобы стакалось с чистыми бочками
            CompoundTag existing = PlatformHooks.getItemTag(stack);
            if (existing != null && existing.isEmpty()) {
                PlatformHooks.setItemTag(stack, null);
            }
        } else {
            CompoundTag fluidTag = new CompoundTag();

            // Нормализация: вода всегда записывается как minecraft:water, чтобы бочки стакались
            Fluid normalized = VanillaFluidEquivalence.forVanillaContainerFill(fluid.getFluid());

            fluidTag.putString("id", BuiltInRegistries.FLUID.getKey(normalized).toString());
            // Явно используем Long, чтобы NBT-тип всегда был одинаковым (Long)
            fluidTag.putLong("amount", fluid.getAmount());

            PlatformHooks.put(stack, NBT_FLUID, fluidTag);
        }
    }

    /** Returns tint color for overlay layer (for ItemColor). */
    public static int getTintColor(ItemStack stack) {
        dev.architectury.fluid.FluidStack fluid = getFluid(stack);
        if (fluid.isEmpty()) return 0xFFFFFF;
        return HbmFluidRegistry.getTintColor(fluid.getFluid());
    }

    // ================================================================== //
    //  ОБЩАЯ ЛОГИКА (без loader-API, mB) — аналог ModFluidTank            //
    // ================================================================== //

    /**
     * Платформенно-независимая логика бочки: fill/drain в mB поверх NBT.
     * Используется лоадерными адаптерами (Forge initCapabilities / NeoForge
     * RegisterCapabilitiesEvent) и доступна для прямых вызовов из машин.
     */
    public static class BarrelAccess {
        protected final ItemStack container;

        public BarrelAccess(ItemStack container) {
            this.container = container;
        }

        public dev.architectury.fluid.FluidStack getFluid() {
            return FluidBarrelItem.getFluid(container);
        }

        public int getCapacityMb() {
            return CAPACITY;
        }

        public int getFluidAmountMb() {
            dev.architectury.fluid.FluidStack cur = getFluid();
            return cur.isEmpty() ? 0 : (int) cur.getAmount();
        }

        /** Вода/лава: HBM-реестр vs vanilla — один состав, но разные объекты Fluid. */
        public static boolean sameFluidPhysical(Fluid a, Fluid b) {
            if (a == b) return true;
            return VanillaFluidEquivalence.sameSubstance(a, b);
        }

        public boolean canFill(Fluid fluid) {
            dev.architectury.fluid.FluidStack cur = getFluid();
            return cur.isEmpty() || sameFluidPhysical(cur.getFluid(), fluid);
        }

        public boolean canDrain(Fluid fluid) {
            dev.architectury.fluid.FluidStack cur = getFluid();
            return !cur.isEmpty() && sameFluidPhysical(cur.getFluid(), fluid);
        }

        /** Возвращает фактически залитый объём (mB). */
        public int fill(Fluid fluid, int amount, boolean simulate) {
            if (fluid == null || fluid == Fluids.EMPTY || amount <= 0) return 0;

            dev.architectury.fluid.FluidStack cur = getFluid();
            if (!cur.isEmpty() && !sameFluidPhysical(cur.getFluid(), fluid)) return 0;

            long have = cur.isEmpty() ? 0L : cur.getAmount();
            long toFill = Math.min(CAPACITY - have, amount);

            if (toFill > 0 && !simulate) {
                // setFluid уже нормализует воду/лаву под vanilla id в NBT
                Fluid mergedType = VanillaFluidEquivalence.forVanillaContainerFill(
                        cur.isEmpty() ? fluid : cur.getFluid());
                FluidBarrelItem.setFluid(container,
                        dev.architectury.fluid.FluidStack.create(mergedType, have + toFill));
            }
            return (int) toFill;
        }

        /** Возвращает слитую жидкость (architectury FluidStack, mB) или empty. */
        public dev.architectury.fluid.FluidStack drain(int maxDrain, boolean simulate) {
            dev.architectury.fluid.FluidStack cur = getFluid();
            if (cur.isEmpty() || maxDrain <= 0) return dev.architectury.fluid.FluidStack.empty();

            long toDrain = Math.min(cur.getAmount(), maxDrain);
            if (!simulate) {
                long remaining = cur.getAmount() - toDrain;
                FluidBarrelItem.setFluid(container, remaining > 0
                        ? dev.architectury.fluid.FluidStack.create(cur.getFluid(), remaining)
                        : dev.architectury.fluid.FluidStack.empty());
            }
            return dev.architectury.fluid.FluidStack.create(cur.getFluid(), toDrain);
        }
    }

    // ================================================================== //
    //  FORGE / NEOFORGE — тонкий адаптер IFluidHandlerItem над BarrelAccess //
    //  NeoForge регистрируется в ModCapabilities.register (у NeoForge нет  //
    //  initCapabilities/ICapabilityProvider).                              //
    // ================================================================== //

    //? if forge || neoforge {
    public static class FluidBarrelCapabilityHandler implements IFluidHandlerItem {
        protected final BarrelAccess access;

        public FluidBarrelCapabilityHandler(ItemStack container) {
            this.access = new BarrelAccess(container);
        }

        @Override public ItemStack getContainer() { return access.container; }

        @Override public int getTanks() { return 1; }

        @Override public FluidStack getFluidInTank(int tank) {
            return toPlatform(access.getFluid());
        }

        @Override public int getTankCapacity(int tank) { return CAPACITY; }

        @Override public boolean isFluidValid(int tank, FluidStack stack) {
            return access.canFill(stack.getFluid());
        }

        @Override
        public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
            if (resource.isEmpty()) return 0;
            return access.fill(resource.getFluid(), resource.getAmount(), action.simulate());
        }

        @Override
        public FluidStack drain(FluidStack resource, IFluidHandler.FluidAction action) {
            if (resource.isEmpty() || !access.canDrain(resource.getFluid())) return FluidStack.EMPTY;
            return toPlatform(access.drain(resource.getAmount(), action.simulate()));
        }

        @Override
        public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
            return toPlatform(access.drain(maxDrain, action.simulate()));
        }

        protected static FluidStack toPlatform(dev.architectury.fluid.FluidStack arch) {
            return arch.isEmpty()
                    ? FluidStack.EMPTY
                    : new FluidStack(arch.getFluid(), (int) arch.getAmount());
        }
    }
    //?}

    //? if forge {
    private static class FluidBarrelCapabilityProvider implements ICapabilityProvider {
        private final FluidBarrelCapabilityHandler handler;
        private final LazyOptional<IFluidHandlerItem> optional;

        FluidBarrelCapabilityProvider(ItemStack stack) {
            this.handler = new FluidBarrelCapabilityHandler(stack);
            this.optional = LazyOptional.of(() -> handler);
        }

        @org.jetbrains.annotations.NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@org.jetbrains.annotations.NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == ForgeCapabilities.FLUID_HANDLER_ITEM
                    ? optional.cast()
                    : LazyOptional.empty();
        }
    }
    //?}
}
