package com.hbm_m.api.fluids;

import java.util.Objects;

import com.hbm_m.block.entity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.interfaces.IMultiblockPart;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.multiblock.PartRole;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.item.ItemStack;
//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.IItemHandler;
//?}

//? if fabric {
/*import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
*///?}

import org.jetbrains.annotations.Nullable;

/**
 * Адаптер: оборачивает соседний BlockEntity с Forge IFluidHandler как MK2-транзивер.
 * Используется FluidDuctBlockEntity для регистрации обычных машин в жидкостной сети.
 *
 * Поддерживает только pressure=0 (Forge IFluidHandler не знает о давлении).
 */
@SuppressWarnings("UnstableApiUsage")
public class ForgeFluidHandlerAdapter implements IFluidStandardTransceiverMK2 {

    private final Level level;
    /** Позиция BE для {getCapability} (для FLUID_CONNECTOR мультиблока — контроллер). */
    private final BlockPos machinePos;
    /**
     * Сторона {@code machinePos} для capability; для жидкостного коннектора цистерны — {@code null}
     * (как у {@code UniversalMachinePartBlockEntity} при делегировании на контроллер).
     */
    @Nullable
    private final Direction sideOfMachineFacingDuct;
    private final Fluid targetFluid;

    public ForgeFluidHandlerAdapter(Level level, BlockPos neighborPos,
                                    Direction sideOfNeighborFacingDuct, Fluid targetFluid) {
        this.level = level;
        this.targetFluid = targetFluid;
        BlockEntity neighborBe = level.getBlockEntity(neighborPos);
        if (neighborBe instanceof IMultiblockPart part
                && part.getControllerPos() != null
                && part.getPartRole() == PartRole.FLUID_CONNECTOR) {
            this.machinePos = part.getControllerPos().immutable();
            this.sideOfMachineFacingDuct = null;
        } else {
            this.machinePos = neighborPos.immutable();
            this.sideOfMachineFacingDuct = sideOfNeighborFacingDuct;
        }
    }

    // --- ILoadedEntry ---

    @Override
    public boolean isLoaded() {
        BlockEntity be = level.getBlockEntity(machinePos);
        return be != null && !be.isRemoved();
    }

    // --- IFluidConnectorMK2 ---

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null && VanillaFluidEquivalence.sameSubstance(fluid, targetFluid);
    }

    @Override
    public boolean isInfiniteNetworkSource(Fluid fluid) {
        if (!VanillaFluidEquivalence.sameSubstance(fluid, targetFluid)) return false;
        // Источник "бесконечный" только если в инвентаре машины лежит бесконечная бочка-источник.
        return hasInfiniteBarrel(true);
    }

    @Override
    public boolean isInfiniteNetworkSink(Fluid fluid) {
        if (!VanillaFluidEquivalence.sameSubstance(fluid, targetFluid)) return false;
        // Сток "бесконечный" только если в инвентаре машины лежит бесконечная бочка-утилизатор (InfiniteFluidItem).
        return hasInfiniteBarrel(false);
    }

    // --- IFluidUserMK2 ---

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[0]; // адаптер не имеет собственных HBM-баков
    }

    // --- IFluidStandardSenderMK2 ---

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[0]; // используем прямой override ниже
    }

    @Override
    public long getFluidAvailable(Fluid fluid, int pressure) {
        if (!VanillaFluidEquivalence.sameSubstance(fluid, targetFluid) || pressure != 0) return 0;
        //? if forge {
        IFluidHandler handler = getForgeHandler();
        if (handler == null) return 0;
        // Важно для ванильных жидкостей: Forge IFluidHandler часто требует точного совпадения
        // fluid instance (source vs flowing). drain(int) не требует указания типа и корректно
        // работает для любых хранилищ, после чего мы фильтруем по substance.
        FluidStack simulated = handler.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
        if (simulated.isEmpty() || !VanillaFluidEquivalence.sameSubstance(simulated.getFluid(), fluid)) {
            return 0;
        }
        return simulated.isEmpty() ? 0 : simulated.getAmount();
        //?}

        //? if fabric {
        /*Storage<FluidVariant> storage = getFabricStorage();
        if (storage == null || !storage.supportsExtraction()) return 0;
        try (Transaction tx = Transaction.openOuter()) {
            return storage.extract(FluidVariant.of(fluid), Long.MAX_VALUE, tx);
            // не коммитим — это симуляция
        }
        *///?}
    }

    @Override
    public void useUpFluid(Fluid fluid, int pressure, long amount) {
        if (!VanillaFluidEquivalence.sameSubstance(fluid, targetFluid) || pressure != 0 || amount <= 0) return;
        //? if forge {
        IFluidHandler handler = getForgeHandler();
        if (handler == null) return;
        FluidStack simulated = handler.drain(clampInt(amount), FluidAction.SIMULATE);
        if (simulated.isEmpty() || !VanillaFluidEquivalence.sameSubstance(simulated.getFluid(), fluid)) {
            return;
        }
        handler.drain(Math.min(simulated.getAmount(), clampInt(amount)), FluidAction.EXECUTE);
        //?}

        //? if fabric {
        /*Storage<FluidVariant> storage = getFabricStorage();
        if (storage == null) return;
        try (Transaction tx = Transaction.openOuter()) {
            storage.extract(FluidVariant.of(fluid), amount, tx);
            tx.commit();
        }
        *///?}
    }

    @Override
    public int[] getProvidingPressureRange(Fluid fluid) {
        return VanillaFluidEquivalence.sameSubstance(fluid, targetFluid)
                ? IFluidUserMK2.DEFAULT_PRESSURE_RANGE
                : new int[]{ 1, 0 };
    }

    // --- IFluidStandardReceiverMK2 ---

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[0];
    }

    @Override
    public long getDemand(Fluid fluid, int pressure) {
        if (!VanillaFluidEquivalence.sameSubstance(fluid, targetFluid) || pressure != 0) return 0;
        //? if forge {
        IFluidHandler handler = getForgeHandler();
        if (handler == null) return 0;
        return handler.fill(new FluidStack(resolveForgeFillFluid(handler, fluid), Integer.MAX_VALUE), FluidAction.SIMULATE);
        //?}

        //? if fabric {
        /*Storage<FluidVariant> storage = getFabricStorage();
        if (storage == null || !storage.supportsInsertion()) return 0;
        try (Transaction tx = Transaction.openOuter()) {
            return storage.insert(FluidVariant.of(fluid), Long.MAX_VALUE, tx);
            // не коммитим — это симуляция
        }
        *///?}
    }

    /**
     * Как в 1.7.10 {@code TileEntityMachineFluidTank#getFluidPriority}: цистерна в режиме «Буфер»
     * получает низкий приоритет приёма, чтобы обычные приёмники (ввод/вывод) забирали жидкость первыми.
     */
    @Override
    public ConnectionPriority getFluidPriority() {
        BlockEntity be = level.getBlockEntity(machinePos);
        if (be instanceof MachineFluidTankBlockEntity tank
                && !tank.hasExploded
                && tank.getMode() == 1) {
            return ConnectionPriority.LOW;
        }
        return ConnectionPriority.NORMAL;
    }

    @Override
    public long transferFluid(Fluid fluid, int pressure, long amount) {
        if (!VanillaFluidEquivalence.sameSubstance(fluid, targetFluid) || pressure != 0 || amount <= 0) return amount;
        //? if forge {
        IFluidHandler handler = getForgeHandler();
        if (handler == null) return amount;
        int filled = handler.fill(new FluidStack(resolveForgeFillFluid(handler, fluid), clampInt(amount)), FluidAction.EXECUTE);
        return amount - filled;
        //?}

        //? if fabric {
        /*Storage<FluidVariant> storage = getFabricStorage();
        if (storage == null) return amount;
        try (Transaction tx = Transaction.openOuter()) {
            long filled = storage.insert(FluidVariant.of(fluid), amount, tx);
            if (filled > 0) tx.commit();
            return amount - filled;
        }
        *///?}
    }

    @Override
    public int[] getReceivingPressureRange(Fluid fluid) {
        return VanillaFluidEquivalence.sameSubstance(fluid, targetFluid)
                ? IFluidUserMK2.DEFAULT_PRESSURE_RANGE
                : new int[]{ 1, 0 };
    }

    // --- Helpers ---

    //? if forge {
    /**
     * Для fill() на Forge важно использовать тот же fluid instance, который уже хранится в IFluidHandler,
     * иначе ванильные баки/forge tanks могут отвергнуть заполнение (source vs flowing).
     * Если хранилище пусто, используем канонический представитель для water/lava семейства.
     */
    private static Fluid resolveForgeFillFluid(IFluidHandler handler, Fluid target) {
        for (int i = 0; i < handler.getTanks(); i++) {
            FluidStack inTank = handler.getFluidInTank(i);
            if (!inTank.isEmpty()) {
                return inTank.getFluid();
            }
        }
        return VanillaFluidEquivalence.forVanillaContainerFill(target);
    }

    @Nullable
    private IFluidHandler getForgeHandler() {
        BlockEntity be = level.getBlockEntity(machinePos);
        if (be == null || be.isRemoved()) return null;
        return be.getCapability(ForgeCapabilities.FLUID_HANDLER, sideOfMachineFacingDuct)
                .orElse(null);
    }

    @SuppressWarnings("removal")
    private boolean hasInfiniteBarrel(boolean wantSource) {
        BlockEntity be = level.getBlockEntity(machinePos);
        if (be == null || be.isRemoved()) return false;
        IItemHandler items = be.getCapability(ForgeCapabilities.ITEM_HANDLER, null).orElse(null);
        if (items == null) return false;
        for (int i = 0; i < items.getSlots(); i++) {
            var stack = items.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof com.hbm_m.item.liquids.InfiniteFluidItem inf) {
                if (!inf.isInstantNetwork()) continue;
                return true;
            }
        }
        return false;
    }
    //?}

    //? if fabric {
    /*@SuppressWarnings("UnstableApiUsage")
    @Nullable
    private Storage<FluidVariant> getFabricStorage() {
        BlockEntity be = level.getBlockEntity(machinePos);
        if (be == null || be.isRemoved()) return null;
        BlockState st = level.getBlockState(machinePos);
        return FluidStorage.SIDED.find(level, machinePos, st, be, sideOfMachineFacingDuct);
    }

    /^* Паритет с Forge {@code hasInfiniteBarrel}: скан инвентаря контроллера на бочку {@link com.hbm_m.item.liquids.InfiniteFluidItem#isInstantNetwork()}. ^/
    @SuppressWarnings("UnstableApiUsage")
    private boolean hasInfiniteBarrel(boolean wantSource) {
        BlockEntity be = level.getBlockEntity(machinePos);
        if (be == null || be.isRemoved()) return false;
        BlockState st = level.getBlockState(machinePos);
        Storage<ItemVariant> inv = ItemStorage.SIDED.find(level, machinePos, st, be, null);
        if (inv == null) return false;
        for (StorageView<ItemVariant> view : inv) {
            if (view.isResourceBlank()) continue;
            int n = (int) Math.min(view.getAmount(), view.getResource().toStack(1).getMaxStackSize());
            if (n <= 0) continue;
            ItemStack stack = view.getResource().toStack(n);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof com.hbm_m.item.liquids.InfiniteFluidItem inf) {
                if (!inf.isInstantNetwork()) continue;
                return true;
            }
        }
        return false;
    }
    *///?}

    //? if forge {
    private static int clampInt(long v) {
        return (int) Math.min(v, Integer.MAX_VALUE);
    }
    //?}

    /** Объект создаётся каждый раз при необходимости, идентичность определяется по (pos, dir, fluid). */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ForgeFluidHandlerAdapter other)) return false;
        return machinePos.equals(other.machinePos)
                && Objects.equals(sideOfMachineFacingDuct, other.sideOfMachineFacingDuct)
                && targetFluid == other.targetFluid;
    }

    @Override
    public int hashCode() {
        int result = machinePos.hashCode();
        result = 31 * result + (sideOfMachineFacingDuct != null ? sideOfMachineFacingDuct.ordinal() : 0);
        result = 31 * result + System.identityHashCode(targetFluid);
        return result;
    }
}
