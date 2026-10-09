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
 * <b>Nachgeruestet:</b> der Kern existiert inzwischen
 * ({@link com.hbm_m.blockentity.machines.dfc.DFCCoreBlockEntity}), darum speist der Injektor
 * wieder wie im Original ein - siehe {@link #injectIntoCore}. Befuellen laesst er sich weiter per
 * Pumpe oder Rohr ueber {@link CoreInjectorFluidHandler}.
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
    //?} elif neoforge {
    /*@Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.FLUID_HANDLER) {
            return com.hbm_m.platform.LazyCap.of(() -> new CoreInjectorFluidHandler(this)).cast();
        }
        return super.getHbmCapability(cap, side);
    }
    *///?}

    /** Original: {@code range = 15}. */
    public static final int RANGE = 15;

    /** Wie weit der Strahl zuletzt kam - nur zur Anzeige. */
    private int beam;

    public int getBeam() {
        return beam;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCoreInjectorBlockEntity be) {
        if (level.isClientSide()) {
            return;
        }
        be.ensureNetworkInitialized();
        be.applyIdentifiers();
        be.injectIntoCore(level, pos, state);
        // Original networkPackNT: Strahllaenge und Tanks gehen jeden Tick an den Client (RenderCoreComponent)
        be.sendUpdateToClient();
    }

    /**
     * 1:1-Port der Einspeiseschleife aus {@code TileEntityCoreInjector.updateEntity}: der Injektor
     * sucht in seiner Blickrichtung den ersten Kern und schiebt seinen Brennstoff hinueber.
     *
     * <p>Je Tank gilt: passt der Brennstoff zu dem, was im Kern schon steht, wird aufgefuellt. Ist
     * der Kerntank dagegen <b>leer</b>, uebernimmt er kurzerhand die Sorte des Injektors - so
     * bestimmt man ueber den Injektor, womit der Kern faehrt.</p>
     */
    private void injectIntoCore(Level level, BlockPos pos, BlockState state) {
        beam = 0;

        // Original: ForgeDirection.getOrientation(getBlockMetadata()) - die Blickrichtung des Blocks.
        // Der Port-Block fuehrt dafuer die volle Sechsrichtungs-FACING (DFCOrientation).
        net.minecraft.core.Direction dir;
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            dir = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        } else if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            dir = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        } else {
            dir = net.minecraft.core.Direction.NORTH;
        }

        for (int i = 1; i <= RANGE; i++) {
            BlockPos at = pos.relative(dir, i);
            net.minecraft.world.level.block.entity.BlockEntity te = level.getBlockEntity(at);

            if (te instanceof com.hbm_m.blockentity.machines.dfc.DFCCoreBlockEntity core) {
                FluidTank[] coreTanks = core.getTanks();

                for (int t = 0; t < 2 && t < coreTanks.length; t++) {
                    FluidTank own = tanks[t];
                    FluidTank target = coreTanks[t];

                    if (target.getTankType() != own.getTankType()) {
                        // Nur ein leerer Kerntank laesst sich umwidmen.
                        if (target.getFill() != 0) continue;
                        target.setTankType(own.getTankType());
                    }

                    int moved = Math.min(own.getFill(), target.getMaxFill() - target.getFill());
                    if (moved <= 0) continue;

                    own.setFill(own.getFill() - moved);
                    target.setFill(target.getFill() + moved);
                    core.setChanged();
                }

                beam = i;
                setChanged();
                return;
            }

            // Original: ein fester Block haelt den Strahl auf.
            if (!level.getBlockState(at).isAir()) return;
        }
    }

    /** Original: {@code tanks[0].setType(0, 1, slots); tanks[1].setType(2, 3, slots);} - Fluidkennung ein (0/2), aus (1/3). */
    private void applyIdentifiers() {
        ItemStack[] slots = new ItemStack[4];
        for (int i = 0; i < 4; i++) slots[i] = inventory.getStackInSlot(i);
        boolean changed = tanks[TANK_DEUTERIUM].setType(0, 1, slots);
        changed |= tanks[TANK_TRITIUM].setType(2, 3, slots);
        if (changed) {
            for (int i = 0; i < 4; i++) inventory.setStackInSlot(i, slots[i] == null ? ItemStack.EMPTY : slots[i]);
            setChanged();
        }
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
        tag.putInt("beam", beam);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tanks[TANK_DEUTERIUM].readFromNBT(tag, "deuterium");
        tanks[TANK_TRITIUM].readFromNBT(tag, "tritium");
        beam = tag.getInt("beam");
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
        // Original: Eingaenge 0/2 (Fluidkennung), 1/3 nur entnehmbar (SlotCraftingOutput)
        return slot == 0 || slot == 2;
    }
}
