package com.hbm_m.item.liquids;

import java.util.List;

import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.HbmFluidRegistry;

//? if forge {
/*import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
*///?}

//? if fabric {
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantItemStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
//?}

import dev.architectury.fluid.FluidStack;
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
 */
public class FluidBarrelItem extends Item {

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
        return CAPACITY * 81L;
        //?} else {
        /*return CAPACITY;
         *///?}
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        // Если жидкости нет (например, пустая бочка), возвращаем базовое имя
        if (fluid.isEmpty()) {
            return Component.translatable("item.hbm_m.fluid_barrel.empty");
        }
        // Если жидкость есть, подставляем её переведенное название (например, "Water Barrel")
        return Component.translatable("item.hbm_m.fluid_barrel", fluid.getName());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        FluidStack fluid = getFluid(stack);
        if (fluid.isEmpty()) {
            tooltip.add(Component.literal("Empty").withStyle(ChatFormatting.GRAY));
        } else {
            long amount = fluid.getAmount();
            //? if fabric {
            amount /= 81L;
            //?}
            tooltip.add(Component.literal("Fluid: ").withStyle(ChatFormatting.GRAY)
                    .append(fluid.getName().copy().withStyle(ChatFormatting.AQUA)));
            tooltip.add(Component.literal("Amount: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(amount + " / " + CAPACITY + " mB").withStyle(ChatFormatting.YELLOW)));
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        if (fluid.isEmpty()) return false;
        long amount = fluid.getAmount();
        //? if fabric {
        amount /= 81L;
        //?}
        return amount < CAPACITY;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        if (fluid.isEmpty()) return 0;
        long amount = fluid.getAmount();
        //? if fabric {
        amount /= 81L;
        //?}
        return Math.round(13.0F * amount / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFFF00;
    }

    //? if forge {
    /*@Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FluidBarrelCapabilityProvider(stack);
    }
    *///?}

    // Static helper methods for NBT access
    public static FluidStack getFluid(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(NBT_FLUID)) {
            CompoundTag fluidTag = tag.getCompound(NBT_FLUID);
            // Читаем напрямую по ключам, чтобы избежать проблем с разными форматами Architectury
            Fluid f = BuiltInRegistries.FLUID.get(new ResourceLocation(fluidTag.getString("id")));
            long amount = fluidTag.getLong("amount");

            if (f != Fluids.EMPTY && amount > 0) {
                f = VanillaFluidEquivalence.forVanillaContainerFill(f);
                return FluidStack.create(f, amount);
            }
        }
        return FluidStack.empty();
    }

    public static void setFluid(ItemStack stack, FluidStack fluid) {
        if (fluid.isEmpty() || fluid.getAmount() <= 0) {
            stack.removeTagKey(NBT_FLUID);
            // Если после удаления тега он пустой — удаляем весь CompoundTag, чтобы стакалось с чистыми бочками
            if (stack.hasTag() && stack.getTag().isEmpty()) {
                stack.setTag(null);
            }
        } else {
            CompoundTag tag = stack.getOrCreateTag();
            CompoundTag fluidTag = new CompoundTag();

            // Нормализация: вода всегда записывается как minecraft:water, чтобы бочки стакались
            Fluid normalized = VanillaFluidEquivalence.forVanillaContainerFill(fluid.getFluid());

            fluidTag.putString("id", BuiltInRegistries.FLUID.getKey(normalized).toString());
            // Явно используем Long, чтобы NBT-тип всегда был одинаковым (Long)
            fluidTag.putLong("amount", fluid.getAmount());

            tag.put(NBT_FLUID, fluidTag);
        }
    }

    /** Returns tint color for overlay layer (for ItemColor). */
    public static int getTintColor(ItemStack stack) {
        FluidStack fluid = getFluid(stack);
        if (fluid.isEmpty()) return 0xFFFFFF;
        return HbmFluidRegistry.getTintColor(fluid.getFluid());
    }

    // ================================================================== //
    //  FORGE — ICapabilityProvider + IFluidHandlerItem                   //
    // ================================================================== //

    //? if forge {
    /*private static class FluidBarrelCapabilityProvider implements ICapabilityProvider {
        private final FluidBarrelForgeHandler handler;
        private final LazyOptional<IFluidHandlerItem> optional;

        FluidBarrelCapabilityProvider(ItemStack stack) {
            this.handler  = new FluidBarrelForgeHandler(stack);
            this.optional = LazyOptional.of(() -> handler);
        }

        @NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == ForgeCapabilities.FLUID_HANDLER_ITEM
                    ? optional.cast()
                    : LazyOptional.empty();
        }
    }

    // Forge FluidStack ≠ Architectury FluidStack → конвертируем внутри хэндлера
    private static class FluidBarrelForgeHandler implements IFluidHandlerItem {
        private final ItemStack container;

        FluidBarrelForgeHandler(ItemStack container) { this.container = container; }

        // --- helpers ---

        private dev.architectury.fluid.FluidStack archFluid() {
            return FluidBarrelItem.getFluid(container);
        }

        private static net.minecraftforge.fluids.FluidStack toForge(dev.architectury.fluid.FluidStack arch) {
            return arch.isEmpty()
                    ? net.minecraftforge.fluids.FluidStack.EMPTY
                    : new net.minecraftforge.fluids.FluidStack(arch.getFluid(), (int) arch.getAmount());
        }

        /^* Вода/лава: HBM-реестр vs vanilla — один состав, но разные объекты {@link Fluid}. ^/
        private static boolean sameFluidPhysical(Fluid a, Fluid b) {
            if (a == b) return true;
            return VanillaFluidEquivalence.sameSubstance(a, b);
        }

        // --- IFluidHandlerItem ---

        @NotNull @Override public ItemStack getContainer() { return container; }

        @Override public int getTanks() { return 1; }

        @NotNull
        @Override
        public net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            return toForge(archFluid());
        }

        @Override
        public int getTankCapacity(int tank) { return (int) CAPACITY; }

        @Override
        public boolean isFluidValid(int tank, @NotNull net.minecraftforge.fluids.FluidStack stack) {
            dev.architectury.fluid.FluidStack cur = archFluid();
            return cur.isEmpty()
                    || sameFluidPhysical(cur.getFluid(), stack.getFluid());
        }

        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return 0;

            dev.architectury.fluid.FluidStack cur = archFluid();
            if (!cur.isEmpty() && !sameFluidPhysical(cur.getFluid(), resource.getFluid())) return 0;

            long have   = cur.isEmpty() ? 0L : cur.getAmount();
            long space  = CAPACITY - have;
            long toFill = Math.min(space, resource.getAmount());

            if (toFill > 0 && action.execute()) {
                // setFluid уже нормализует воду/лаву под vanilla id в NBT
                Fluid mergedType = VanillaFluidEquivalence.forVanillaContainerFill(
                        cur.isEmpty() ? resource.getFluid() : cur.getFluid());
                FluidBarrelItem.setFluid(container,
                        dev.architectury.fluid.FluidStack.create(mergedType, have + toFill));
            }
            return (int) toFill;
        }

        @NotNull
        @Override
        public net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return net.minecraftforge.fluids.FluidStack.EMPTY;
            dev.architectury.fluid.FluidStack cur = archFluid();
            if (cur.isEmpty() || !sameFluidPhysical(cur.getFluid(), resource.getFluid())) return net.minecraftforge.fluids.FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }

        @NotNull
        @Override
        public net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            dev.architectury.fluid.FluidStack cur = archFluid();
            if (cur.isEmpty() || maxDrain <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;

            long toDrain  = Math.min(cur.getAmount(), maxDrain);
            net.minecraftforge.fluids.FluidStack out = new net.minecraftforge.fluids.FluidStack(cur.getFluid(), (int) toDrain);

            if (action.execute()) {
                long remaining = cur.getAmount() - toDrain;
                FluidBarrelItem.setFluid(container, remaining > 0
                        ? dev.architectury.fluid.FluidStack.create(cur.getFluid(), remaining)
                        : dev.architectury.fluid.FluidStack.empty());
            }
            return out;
        }
    }
    *///?}

    // ================================================================== //
    //  FABRIC — Fabric Transfer API (SingleVariantItemStorage)            //
    //  Регистрацию делай через FluidStorage.ITEM.registerForItems(...)    //
    //  в точке входа (onInitialize).                                      //
    // ================================================================== //

    //? if fabric {
    public static Storage<FluidVariant> createFabricStorage(ContainerItemContext ctx) {
        if (ctx == null) return null;
        return new SingleVariantItemStorage<FluidVariant>(ctx) {

            // Возвращает «пустой» вариант типа — аналог null для ресурса
            @Override
            protected FluidVariant getBlankResource() {
                return FluidVariant.blank();
            }

            // Читаем текущую жидкость из NBT предмета
            @Override
            protected FluidVariant getResource(ItemVariant currentVariant) {
                FluidStack fs = FluidBarrelItem.getFluid(currentVariant.toStack());
                return fs.isEmpty() ? FluidVariant.blank() : FluidVariant.of(fs.getFluid());
            }

            // Читаем текущее количество из NBT предмета
            @Override
            protected long getAmount(ItemVariant currentVariant) {
                FluidStack fs = FluidBarrelItem.getFluid(currentVariant.toStack());
                return fs.isEmpty() ? 0L : fs.getAmount();
            }

            @Override
            protected long getCapacity(FluidVariant variant) {
                return CAPACITY * 81L;
            }

            // Создаём новый ItemVariant с обновлённым NBT (не мутируем оригинал)
            // Вызывается фреймворком; откат транзакции обеспечивается через context.exchange()
            @Override
            protected ItemVariant getUpdatedVariant(ItemVariant currentVariant,
                                                    FluidVariant newResource,
                                                    long newAmount) {
                // toStack() сохраняет весь NBT (имя, зачарования и т.д.) — только меняем fluid-тег
                ItemStack stack = currentVariant.toStack();
                if (newAmount == 0L || newResource.isBlank()) {
                    // Очищаем тег, чтобы пустые бочки могли складываться в стак
                    FluidBarrelItem.setFluid(stack, FluidStack.empty());
                } else {
                    FluidBarrelItem.setFluid(stack,
                            FluidStack.create(newResource.getFluid(), newAmount));
                }
                return ItemVariant.of(stack);
            }
        };
    }
    //?}
}