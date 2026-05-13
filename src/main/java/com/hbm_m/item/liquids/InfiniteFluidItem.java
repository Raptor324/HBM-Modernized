package com.hbm_m.item.liquids;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
//? if forge {
/*import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
*///?}

//? if fabric {
import java.util.Iterator;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
//?}

/**
 * Infinite fluid item (port of 1.7.10 ItemInfiniteFluid).
 *
 * Может быть:
 * - настроенным на конкретную жидкость (например "infinite water") и работать как источник+поглотитель
 *   со скоростью {@code transferRate}
 * - универсальным (fluid_barrel_infinite): тип по NBT (или по запросу), и может использоваться как "instant" для сети
 */
@SuppressWarnings("UnstableApiUsage")
public class InfiniteFluidItem extends Item {

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
        if (stack.hasTag() && stack.getTag().contains("FluidType")) {
            return BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(stack.getTag().getString("FluidType")));
        }
        return Fluids.EMPTY;
    }

    /** true только для универсальной бесконечной бочки (fluid_barrel_infinite). */
    public boolean isInstantNetwork() {
        return instantNetwork;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("§bInfinite Fluid"));
        tooltip.add(Component.literal("§7Output Rate: §e" + transferRate + " mB/t"));
    }

    //  FORGE — ICapabilityProvider + IFluidHandlerItem                   //
    // ================================================================== //

    //? if forge {
    /*@Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new InfiniteFluidCapabilityProvider(stack, transferRate, fixedFluid);
    }

    private static class InfiniteFluidCapabilityProvider implements ICapabilityProvider {
        private final InfiniteFluidHandler handler;
        private final LazyOptional<IFluidHandlerItem> optional;

        public InfiniteFluidCapabilityProvider(ItemStack stack, int rate, @Nullable Fluid fixedFluid) {
            this.handler = new InfiniteFluidHandler(stack, rate, fixedFluid);
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

    private static class InfiniteFluidHandler implements IFluidHandlerItem {
        private final ItemStack container;
        private final int rate;
        @Nullable
        private final Fluid fixedFluid;

        private Fluid getConfiguredFluid() {
            if (fixedFluid != null && fixedFluid != Fluids.EMPTY) return fixedFluid;
            if (container.hasTag() && container.getTag().contains("FluidType")) {
                return BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(container.getTag().getString("FluidType")));
            }
            return Fluids.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            Fluid type = getConfiguredFluid();
            if (type == Fluids.EMPTY) return FluidStack.EMPTY;
            // Возвращаем бесконечное количество (ограниченное rate или запросом трубы)
            return new FluidStack(type, Math.min(maxDrain, rate));
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return 0;
            Fluid type = getConfiguredFluid();
            // Если не настроена — поглощаем любой тип (универсальная бочка).
            // Если настроена — поглощаем только тот же substance (для ванильных water/lava).
            if (type == Fluids.EMPTY || com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(type, resource.getFluid())) {
                return Math.min(resource.getAmount(), rate);
            }
            return 0;
        }

        public InfiniteFluidHandler(ItemStack container, int rate, @Nullable Fluid fixedFluid) {
            this.container = container;
            this.rate = rate;
            this.fixedFluid = fixedFluid;
        }

        @NotNull
        @Override
        public ItemStack getContainer() {
            return container;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @NotNull
        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return false;
        }

        @NotNull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return FluidStack.EMPTY;
            Fluid type = getConfiguredFluid();
            if (type != Fluids.EMPTY && !com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(type, resource.getFluid())) {
                return FluidStack.EMPTY;
            }
            int amountToDrain = Math.min(resource.getAmount(), rate);
            return new FluidStack(resource.getFluid(), amountToDrain);
        }
    }
    *///?}

    //  FABRIC — Fabric Transfer API (Storage)                            //
    // ================================================================== //

    //? if fabric {
    public Storage<FluidVariant> createFabricStorage(ItemStack stack, @Nullable ContainerItemContext context) {
        return new Storage<FluidVariant>() {
            private ItemStack currentStack() {
                // Fabric Transfer API иногда зовёт provider с context == null (например, при FluidStorage.ITEM.find(stack, null)).
                // Для бесконечной бочки это ок: её поведение определяется NBT и не требует обязательного контекста.
                return context != null ? context.getItemVariant().toStack() : stack;
            }

            @Override
            public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
                Fluid type = getFluidType(currentStack());
                // Бесконечная бочка поглощает жидкость, если тип совпадает или не настроен (void)
                if (type == Fluids.EMPTY || type == resource.getFluid()) {
                    return maxAmount;
                }
                return 0;
            }

            @Override
            public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
                Fluid type = getFluidType(currentStack());
                // Как и в Forge версии, если тип пустой, мы разрешаем вытянуть всё, что запросят.
                // Иначе проверяем совпадение с настроенной жидкостью.
                if (type != Fluids.EMPTY && type != resource.getFluid()) {
                    return 0;
                }
                return Math.min(maxAmount, transferRate);
            }

            @Override
            public Iterator<StorageView<FluidVariant>> iterator() {
                return List.<StorageView<FluidVariant>>of(new StorageView<FluidVariant>() {
                    @Override
                    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
                        Fluid type = getFluidType(currentStack());
                        if (type != Fluids.EMPTY && type != resource.getFluid()) {
                            return 0;
                        }
                        return Math.min(maxAmount, transferRate);
                    }

                    @Override
                    public boolean isResourceBlank() {
                        return getFluidType(currentStack()) == Fluids.EMPTY;
                    }

                    @Override
                    public FluidVariant getResource() {
                        Fluid type = getFluidType(currentStack());
                        return type == Fluids.EMPTY ? FluidVariant.blank() : FluidVariant.of(type);
                    }

                    @Override
                    public long getAmount() {
                        // Показываем трубам, что жидкости у нас бесконечно много (но отдаем не больше Long.MAX_VALUE)
                        return isResourceBlank() ? 0 : Long.MAX_VALUE;
                    }

                    @Override
                    public long getCapacity() {
                        return Long.MAX_VALUE;
                    }
                }).iterator();
            }
        };
    }
    //?}
}