package com.hbm_m.compat.jade;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.api.fluids.FluidBlockAccess;
import com.hbm_m.api.fluids.IFluidUserMK2;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.machines.UniversalMachinePartBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.ViewGroup;

//? if < 1.21.1 {
import net.minecraft.server.level.ServerPlayer;
import snownee.jade.api.view.IServerExtensionProvider;
//?} else {
/*import snownee.jade.api.Accessor;
import snownee.jade.api.view.IServerExtensionProvider;
*///?}

/**
 * Server-side Jade fluid source for HBM machines: the tank list renders no matter which cell of
 * a multiblock is targeted.
 *
 * <p>Jade collects fluid data through {@code IServerExtensionProvider}s; the first provider (by
 * ascending priority) whose {@code getGroups} returns non-null wins, and its payload is rendered
 * by Jade's own client code - the exact same tank bars as its native capability display. This
 * provider is registered for {@link BaseHbmBlockEntity} with a priority below Jade's universal
 * capability bridge (1000):
 * <ul>
 *   <li>a multiblock part is resolved to its controller, so the whole machine's tanks show
 *       even on default casing cells that expose no capability of their own;</li>
 *   <li>MK2 machines report {@link IFluidUserMK2#getAllTanks()} - the full internal list, the
 *       same the crosshair HUD shows, not just the pipe-facing capability subset;</li>
 *   <li>legacy machines fall back to their exposed fluid capability;</li>
 *   <li>{@code null} (no tanks, foreign block entities) defers to Jade's own bridge.</li>
 * </ul>
 */
public final class HbmFluidStorageProvider {

    public static final ResourceLocation UID =
        //? if < 1.21.1 {
        new ResourceLocation("hbm_m", "fluid_storage");
        //?} else {
        /*ResourceLocation.fromNamespaceAndPath("hbm_m", "fluid_storage");
        *///?}

    /** Lower runs earlier; Jade's universal bridge sits at 1000 and only answers if we return null. */
    private static final int PRIORITY = 400;

    private HbmFluidStorageProvider() {}

    /**
     * Serializes one tank the way Jade's own bridge does. On 1.21.1 the two-arg overload is
     * deprecated in favour of the ops variant; NbtOps.INSTANCE is exactly what it delegates to.
     */
    private static CompoundTag writeFluidView(JadeFluidObject fluid, long capacity) {
        //? if < 1.21.1 {
        return FluidView.writeDefault(fluid, capacity);
        //?} else {
        /*return FluidView.writeDefault(fluid, capacity, net.minecraft.nbt.NbtOps.INSTANCE);
        *///?}
    }

    //? if < 1.21.1 {
    public static final class V11 implements IServerExtensionProvider<BlockEntity, CompoundTag> {
        public static final V11 INSTANCE = new V11();
        private V11() {}

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public int getDefaultPriority() {
            return PRIORITY;
        }

        @Override
        public List<ViewGroup<CompoundTag>> getGroups(ServerPlayer player, ServerLevel level,
                BlockEntity target, boolean showDetails) {
            return buildGroups(level, target);
        }
    }
    //?} else {
    /*public static final class V15 implements IServerExtensionProvider<CompoundTag> {
        public static final V15 INSTANCE = new V15();
        private V15() {}

        @Override
        public ResourceLocation getUid() {
            return UID;
        }

        @Override
        public int getDefaultPriority() {
            return PRIORITY;
        }

        @Override
        public List<ViewGroup<CompoundTag>> getGroups(Accessor<?> accessor) {
            return accessor.getTarget() instanceof BlockEntity be
                    ? buildGroups(accessor.getLevel(), be)
                    : null;
        }
    }
    *///?}

    public static List<ViewGroup<CompoundTag>> buildGroups(Level level, BlockEntity target) {
        if (level == null || target == null) {
            return null;
        }

        BlockEntity be = target;
        // A multiblock phantom speaks for the whole machine: resolve to the controller.
        if (be instanceof UniversalMachinePartBlockEntity part) {
            BlockPos controllerPos = part.getControllerPos();
            if (controllerPos == null) {
                return null;
            }
            BlockEntity controller = level.getBlockEntity(controllerPos);
            if (!(controller instanceof BaseHbmBlockEntity)) {
                return null; // foreign or broken controller - let Jade's own bridge try
            }
            be = controller;
        }
        if (!(be instanceof BaseHbmBlockEntity)) {
            return null;
        }

        List<CompoundTag> views = new ArrayList<>();
        long emptyVolume = 0;
        boolean hasTanks = false;

        if (be instanceof IFluidUserMK2 mk2) {
            FluidTank[] tanks = mk2.getAllTanks();
            if (tanks != null) {
                for (FluidTank tank : tanks) {
                    if (tank == null) {
                        continue;
                    }
                    int capacity = tank.getCapacityMb();
                    if (capacity <= 0) {
                        continue;
                    }
                    hasTanks = true;
                    Fluid type = tank.getTankType();
                    int fill = tank.getFill();
                    if (fill <= 0 || type == null || type == Fluids.EMPTY
                            || type == ModFluids.NONE.getSource()) {
                        emptyVolume += capacity;
                    } else {
                        views.add(writeFluidView(JadeFluidObject.of(type, fill), capacity));
                    }
                }
            }
        } else {
            // Legacy machines keep their tanks behind the loader fluid capability.
            for (TankView tank : readCapabilityTanks(level, be)) {
                hasTanks = true;
                if (tank.isEmpty()) {
                    emptyVolume += tank.capacity();
                } else {
                    views.add(writeFluidView(
                            JadeFluidObject.of(tank.fluid(), tank.amount()), tank.capacity()));
                }
            }
        }

        if (!hasTanks) {
            return null;
        }
        if (views.isEmpty()) {
            // Jade's native behaviour: nothing stored anywhere - one merged "empty" bar.
            if (emptyVolume <= 0) {
                return null;
            }
            views.add(writeFluidView(JadeFluidObject.empty(), emptyVolume));
        }
        return List.of(new ViewGroup<>(views));
    }

    private record TankView(Fluid fluid, int amount, int capacity) {
        boolean isEmpty() {
            return fluid == null || fluid == Fluids.EMPTY || amount <= 0;
        }
    }

    //? if forge {
    private static List<TankView> readCapabilityTanks(Level level, BlockEntity be) {
        net.minecraftforge.fluids.capability.IFluidHandler handler =
                FluidBlockAccess.getFluidHandler(level, be.getBlockPos(), null);
        List<TankView> out = new ArrayList<>();
        if (handler == null) {
            return out;
        }
        for (int i = 0; i < handler.getTanks(); i++) {
            int capacity = handler.getTankCapacity(i);
            if (capacity <= 0) {
                continue;
            }
            net.minecraftforge.fluids.FluidStack stack = handler.getFluidInTank(i);
            out.add(stack.isEmpty()
                    ? new TankView(Fluids.EMPTY, 0, capacity)
                    : new TankView(stack.getFluid(), stack.getAmount(), capacity));
        }
        return out;
    }
    //?} else {
    /*private static List<TankView> readCapabilityTanks(Level level, BlockEntity be) {
        net.neoforged.neoforge.fluids.capability.IFluidHandler handler =
                FluidBlockAccess.getFluidHandler(level, be.getBlockPos(), null);
        List<TankView> out = new ArrayList<>();
        if (handler == null) {
            return out;
        }
        for (int i = 0; i < handler.getTanks(); i++) {
            int capacity = handler.getTankCapacity(i);
            if (capacity <= 0) {
                continue;
            }
            net.neoforged.neoforge.fluids.FluidStack stack = handler.getFluidInTank(i);
            out.add(stack.isEmpty()
                    ? new TankView(Fluids.EMPTY, 0, capacity)
                    : new TankView(stack.getFluid(), stack.getAmount(), capacity));
        }
        return out;
    }
    *///?}
}
