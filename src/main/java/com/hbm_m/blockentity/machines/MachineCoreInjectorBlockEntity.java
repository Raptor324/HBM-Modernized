package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineCoreInjectorMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port von {@code TileEntityCoreInjector} (1.7.10). Original: haelt Deuterium (Tank 0, 128000mB)
 * und Tritium (Tank 1, 128000mB) bereit und feuert sie per Laser-Beam als Fusionsreaktor-Zuendung
 * in einen {@code TileEntityCore} (Fusionsreaktor-Kern).
 * <p>
 * Vereinfachung (Funktion vor Politur, siehe Aufgabenstellung): dieser Port hat noch KEINEN
 * Fusionsreaktor-Kern implementiert (kein {@code TileEntityCore}-Aequivalent existiert) - der
 * Injector fungiert daher als reiner Deuterium-/Tritium-Pufferspeicher (befuellbar per Pumpe/Rohr
 * ueber {@link CoreInjectorFluidHandler}), ohne den Laser-Zuend-Trigger des Originals. Sobald ein
 * Fusionsreaktor-Kern portiert wird, kann hier die {@code fireBeam}-Logik analog zu
 * {@link MachineCoreEmitterBlockEntity} ergaenzt werden.
 */
public class MachineCoreInjectorBlockEntity extends BaseMachineBlockEntity {

    public static final int TANK_DEUTERIUM = 0;
    public static final int TANK_TRITIUM = 1;

    private static final int TANK_CAPACITY_MB = 128_000;

    private final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(ModFluids.DEUTERIUM.getSource(), TANK_CAPACITY_MB),
            new FluidTank(ModFluids.TRITIUM.getSource(), TANK_CAPACITY_MB)
    };

    public MachineCoreInjectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CORE_INJECTOR_BE.get(), pos, state, 4, 2_000_000L, 40_000L);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return net.minecraftforge.common.util.LazyOptional.of(() -> new CoreInjectorFluidHandler(this)).cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCoreInjectorBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        be.ensureNetworkInitialized();
    }

    @Nullable
    public FluidTank getTank(int index) {
        if (index < 0 || index >= tanks.length) return null;
        return tanks[index];
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tanks[TANK_DEUTERIUM].writeToNBT(tag, "deuterium");
        tanks[TANK_TRITIUM].writeToNBT(tag, "tritium");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tanks[TANK_DEUTERIUM].readFromNBT(tag, "deuterium");
        tanks[TANK_TRITIUM].readFromNBT(tag, "tritium");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.core_injector");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return MachineCoreInjectorMenu.create(id, inv, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.core_injector");
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }
}
