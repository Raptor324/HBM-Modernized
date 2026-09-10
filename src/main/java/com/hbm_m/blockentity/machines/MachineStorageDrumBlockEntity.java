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
 * <p>Wieviel dabei anfaellt, haengt wie im Original an der <b>Abfallklasse</b> des Muells
 * ({@link com.hbm_m.item.special.ItemWasteLong.WasteClass} /
 * {@link com.hbm_m.item.special.ItemWasteShort.WasteClass}): Thorium-Muell gibt praktisch nichts
 * ab, Plutonium-241 und aufwaerts je einen vollen Eimer Gas pro Zerfall. Ein Fasslager laesst sich
 * also nicht pauschal auslegen - man dimensioniert es nach dem schmutzigsten Brennstoff, den man
 * fahren will. Die Klasse bleibt beim Zerfall erhalten, das abgereicherte Stueck traegt sie weiter.
 * Kleine Brocken liefern ein Zehntel, ebenfalls wie im Original.</p>
 *
 * <p>SCOPE-Vereinfachung: Das Original strahlt Entities per Raytrace-Streuung entlang der
 * Sichtlinie individuell an; hier eine einfache {@link ChunkRadiationManager}-Erhoehung. Ebenso
 * entfaellt die MK2-Rohrnetzwerk-Anbindung (kein Auto-Push, nur passive Kapazitaet) - siehe
 * dieselbe Vereinfachung bei der Drone-Crate-Fluessigkeitsvariante.
 */
public class MachineStorageDrumBlockEntity extends BaseMachineBlockEntity {

    public static final int INVENTORY_SIZE = 24;
    private static final int DECAY_CHANCE = 6000;
    private static final int TINY_DECAY_CHANCE = 600;

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
        // Der Gasanteil wandert ueber ein Feld, damit der Zerfall beides in einem Rutsch liefern kann.
        be.pendingGas = 0;
        float rad = 0;

        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack stack = be.inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            // 1:1: der Ertrag kommt aus der Abfallklasse des jeweiligen Stapels.
            liquid += be.tryDecay(level, i, stack);

            // Nach einem Zerfall liegt hier bereits das abgereicherte Stueck.
            stack = be.inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            // Anything in the drum that has no waste recipe of its own instead has its induced
            // neutron activation bled off - CE's drum is the intended way to cool down something
            // you left sitting in an outgasser. DECAY_RATE is a ten-second half-life.
            com.hbm_m.util.ContaminationUtil.neutronActivateItem(stack, 0.0F, DECAY_RATE);

            rad += 0.1F;
        }

        if (liquid > 0) be.liquidTank.fill(Math.min(be.liquidTank.getMaxFill(), be.liquidTank.getFill() + liquid));
        if (be.pendingGas > 0) be.gasTank.fill(Math.min(be.gasTank.getMaxFill(), be.gasTank.getFill() + be.pendingGas));

        if (rad > 0 && level.getGameTime() % 20 == 0) {
            ChunkRadiationManager.incrementRad(level, pos.getX(), pos.getY(), pos.getZ(), rad);
        }

        be.setChanged();
    }

    /** Zwischenspeicher fuer das Gas eines Ticks - siehe {@link #tryDecay}. */
    private int pendingGas = 0;

    /**
     * Prueft die vier Zerfallspfade des Originals fuer einen Stapel. Trifft einer zu, wird der
     * Stapel durch sein abgereichertes Gegenstueck derselben Abfallklasse ersetzt; der Gasanteil
     * landet in {@link #pendingGas}, der Fluessiganteil ist der Rueckgabewert.
     */
    private int tryDecay(Level level, int slot, ItemStack stack) {

        for (var path : com.hbm_m.item.special.WasteClasses.LONG_PATHS) {
            int index = path.indexOf(stack);
            if (index < 0) continue;
            // Original: kleine Brocken zerfallen zehnmal so oft.
            if (level.random.nextInt(path.tiny() ? TINY_DECAY_CHANCE : DECAY_CHANCE) != 0) return 0;

            inventory.setStackInSlot(slot, path.spentStack(index));
            pendingGas += path.gasAt(index);
            return path.liquidAt(index);
        }

        for (var path : com.hbm_m.item.special.WasteClasses.SHORT_PATHS) {
            int index = path.indexOf(stack);
            if (index < 0) continue;
            if (level.random.nextInt(path.tiny() ? TINY_DECAY_CHANCE : DECAY_CHANCE) != 0) return 0;

            inventory.setStackInSlot(slot, path.spentStack(index));
            pendingGas += path.gasAt(index);
            return path.liquidAt(index);
        }

        return 0;
    }

    public FluidTank getLiquidTank() { return liquidTank; }
    public FluidTank getGasTank() { return gasTank; }

    /** In das Fass passt jeder Muell, der einen Zerfallspfad hat - also jede Abfallklasse. */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        for (var path : com.hbm_m.item.special.WasteClasses.LONG_PATHS) {
            if (path.indexOf(stack) >= 0) return true;
        }
        for (var path : com.hbm_m.item.special.WasteClasses.SHORT_PATHS) {
            if (path.indexOf(stack) >= 0) return true;
        }
        return false;
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
