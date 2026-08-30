package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Deuterium Tower - 1:1-Port von {@code TileEntityDeuteriumTower}/{@code TileEntityDeuteriumExtractor}
 * (1.7.10-Original): destilliert Wasser zu Schwerwasser. Pro Tick, wenn genug Energie und Wasser
 * vorhanden sind, wird Wasser im Verhaeltnis 50:1 in Schwerwasser umgewandelt (Original:
 * {@code convert = min(tanks[1].maxFill, tanks[0].fill) / 50}, Energiekosten {@code maxPower/20}).
 * Tankgroessen 1:1 aus dem Original (Tower-Variante, nicht die kleinere Extractor-Basis):
 * Wasser 50000mB, Schwerwasser 5000mB.
 * <p>
 * Bewusst NICHT uebernommen: die 1.7.10-Fluid-Pipe-Autoconnect-Logik (dieser Port nutzt Capability-
 * basierte Fluid-Handler statt manueller Nachbarblock-Subscription) und kein eigenes Multiblock-
 * Geruest - Original ist ein {@code BlockDummyable} mit Verbindungspunkten, hier als Einzelblock
 * mit Tank-Kapazitaeten der Turm-Variante nachgebaut (Funktion identisch, ohne die reine Deko-Hoehe).
 */
public class MachineDeuteriumTowerBlockEntity extends BaseMachineBlockEntity {

    public static final int TANK_WATER = 0;
    public static final int TANK_HEAVY_WATER = 1;

    private static final int WATER_CAPACITY = 50_000;
    private static final int HEAVY_WATER_CAPACITY = 5_000;
    private static final int CONVERT_RATIO = 50; // 50mB Wasser -> 1mB Schwerwasser
    private static final long ENERGY_PER_TICK = 5_000L; // getMaxPower()/20 im Original (100_000/20)

    private final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(ModFluids.WATER.getSource(), WATER_CAPACITY),
            new FluidTank(ModFluids.HEAVYWATER.getSource(), HEAVY_WATER_CAPACITY)
    };

    public MachineDeuteriumTowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEUTERIUM_TOWER_BE.get(), pos, state, 0, 100_000L, 10_000L);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return tanks[TANK_WATER].getForgeFluidCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    public static void tick(Level level, BlockPos pos, BlockState state, MachineDeuteriumTowerBlockEntity be) {
        if (level.isClientSide()) return;

        FluidTank water = be.tanks[TANK_WATER];
        FluidTank heavyWater = be.tanks[TANK_HEAVY_WATER];

        boolean dirty = false;
        if (be.getEnergyStored() >= ENERGY_PER_TICK
                && water.getFluidAmountMb() >= 100
                && heavyWater.getFluidAmountMb() < heavyWater.getCapacityMb()) {

            int convert = Math.min(heavyWater.getCapacityMb() - heavyWater.getFluidAmountMb(),
                    water.getFluidAmountMb() / CONVERT_RATIO);

            if (convert > 0) {
                water.drainMb(convert * CONVERT_RATIO);
                heavyWater.fillMb(ModFluids.HEAVYWATER.getSource(), convert);
                be.setEnergyStored(be.getEnergyStored() - ENERGY_PER_TICK);
                dirty = true;
            }
        }

        if (dirty) {
            be.setChanged();
            be.sendUpdateToClient();
        }
    }

    public FluidTank getTank(int index) {
        return tanks[index];
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tanks[TANK_WATER].writeToNBT(tag, "water");
        tanks[TANK_HEAVY_WATER].writeToNBT(tag, "heavyWater");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tanks[TANK_WATER].readFromNBT(tag, "water");
        tanks[TANK_HEAVY_WATER].readFromNBT(tag, "heavyWater");
    }

    @Override public Component getDisplayName() { return Component.translatable("container.hbm_m.deuterium_tower"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return null; }
    @Override protected Component getDefaultName() { return Component.translatable("container.hbm_m.deuterium_tower"); }
    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return false; }
}
