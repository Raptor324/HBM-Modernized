package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.item.ModItems;
import com.hbm_m.radiation.ChunkRadiationManager;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityStorageDrum} (1.7.10 Original) - a 24-slot nuclear-waste store: each
 * slot has a small random per-tick chance to decay into its "depleted" form, releasing liquid/gas
 * waste byproduct into two internal tanks (16,000mB each) and irradiating the surroundings.
 * <p>
 * SCOPE-Vereinfachung: Das Original berechnet den Fluessig-/Gas-Ertrag pro Zerfall anhand einer
 * meta-abhaengigen {@code WasteClass}-Tabelle (unterschiedliche Ertragsmengen je nach Abfallklasse)
 * und strahlt Entities per Raytrace-Streuung entlang der Sichtlinie individuell an. Hier: fester
 * Ertrag pro Zerfallsart (voll/tiny) und einfache {@link ChunkRadiationManager}-Erhoehung statt
 * Entity-Raytracing - der Kernmechanismus (Abfall zerfaellt langsam zu Fluessigkeit/Gas plus
 * Strahlung) bleibt vollstaendig erhalten, nur die genaue Ertragstabelle/Streuphysik entfaellt.
 * Ebenso entfaellt die MK2-Rohrnetzwerk-Anbindung (kein Auto-Push, nur passive Kapazitaet) - siehe
 * dieselbe Vereinfachung bei der Drone-Crate-Fluessigkeitsvariante.
 */
public class MachineStorageDrumBlockEntity extends BaseMachineBlockEntity {

    public static final int INVENTORY_SIZE = 24;
    private static final int DECAY_CHANCE = 6000;
    private static final int TINY_DECAY_CHANCE = 600;
    private static final int FULL_YIELD_MB = 200;
    private static final int TINY_YIELD_MB = 20;

    private final FluidTank liquidTank = new FluidTank(ModFluids.WASTEFLUID.getSource(), 16_000);
    private final FluidTank gasTank = new FluidTank(ModFluids.WASTEGAS.getSource(), 16_000);

    public MachineStorageDrumBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_STORAGE_DRUM_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    /** CE: {@code decayRate = 0.9965402628F} - a ten-second half-life on stored activation. */
    private static final float DECAY_RATE = 0.9965402628F;

    public static void tick(Level level, BlockPos pos, BlockState state, MachineStorageDrumBlockEntity be) {
        if (level.isClientSide) return;

        int liquid = 0;
        int gas = 0;
        float rad = 0;

        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack stack = be.inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();

            if (item == ModItems.NUCLEAR_WASTE_LONG.get() && level.random.nextInt(DECAY_CHANCE) == 0) {
                liquid += FULL_YIELD_MB;
                gas += FULL_YIELD_MB;
                be.inventory.setStackInSlot(i, new ItemStack(ModItems.NUCLEAR_WASTE_LONG_DEPLETED.get()));
            } else if (item == ModItems.NUCLEAR_WASTE_LONG_TINY.get() && level.random.nextInt(TINY_DECAY_CHANCE) == 0) {
                liquid += TINY_YIELD_MB;
                gas += TINY_YIELD_MB;
                be.inventory.setStackInSlot(i, new ItemStack(ModItems.NUCLEAR_WASTE_LONG_DEPLETED_TINY.get()));
            } else if (item == ModItems.NUCLEAR_WASTE_SHORT.get() && level.random.nextInt(DECAY_CHANCE) == 0) {
                liquid += FULL_YIELD_MB;
                gas += FULL_YIELD_MB;
                be.inventory.setStackInSlot(i, new ItemStack(ModItems.NUCLEAR_WASTE_SHORT_DEPLETED.get()));
            } else if (item == ModItems.NUCLEAR_WASTE_SHORT_TINY.get() && level.random.nextInt(TINY_DECAY_CHANCE) == 0) {
                liquid += TINY_YIELD_MB;
                gas += TINY_YIELD_MB;
                be.inventory.setStackInSlot(i, new ItemStack(ModItems.NUCLEAR_WASTE_SHORT_DEPLETED_TINY.get()));
            }

            // Anything in the drum that has no waste recipe of its own instead has its induced
            // neutron activation bled off - CE's drum is the intended way to cool down something
            // you left sitting in an outgasser. DECAY_RATE is a ten-second half-life.
            com.hbm_m.util.ContaminationUtil.neutronActivateItem(stack, 0.0F, DECAY_RATE);

            if (!stack.isEmpty()) rad += 0.1F;
        }

        if (liquid > 0) be.liquidTank.fill(Math.min(be.liquidTank.getMaxFill(), be.liquidTank.getFill() + liquid));
        if (gas > 0) be.gasTank.fill(Math.min(be.gasTank.getMaxFill(), be.gasTank.getFill() + gas));

        if (rad > 0 && level.getGameTime() % 20 == 0) {
            ChunkRadiationManager.incrementRad(level, pos.getX(), pos.getY(), pos.getZ(), rad);
        }

        be.setChanged();
    }

    public FluidTank getLiquidTank() { return liquidTank; }
    public FluidTank getGasTank() { return gasTank; }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        Item item = stack.getItem();
        return item == ModItems.NUCLEAR_WASTE_LONG.get() || item == ModItems.NUCLEAR_WASTE_LONG_TINY.get()
                || item == ModItems.NUCLEAR_WASTE_SHORT.get() || item == ModItems.NUCLEAR_WASTE_SHORT_TINY.get();
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.put("liquidTank", liquidTank.writeNBT(new CompoundTag()));
        tag.put("gasTank", gasTank.writeNBT(new CompoundTag()));
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        if (tag.contains("liquidTank")) liquidTank.readNBT(tag.getCompound("liquidTank"));
        if (tag.contains("gasTank")) gasTank.readNBT(tag.getCompound("gasTank"));
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_storage_drum");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return com.hbm_m.inventory.menu.MachineStorageDrumMenu.create(id, inventory, this);
    }
}
