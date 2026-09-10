package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineRadiolysisMenu;
import com.hbm_m.recipe.CrackingTowerRecipe;
import com.hbm_m.recipe.RadiolysisRecipe;

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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Radiolysis Collector: Direktport der Fluid-Crack-Kernmechanik aus
 * {@code TileEntityMachineRadiolysis} (1.7.10 Original).
 * <p>
 * <p><b>Er hat keinen Stromanschluss.</b> Seine ganze Leistung kommt aus zehn
 * {@link com.hbm_m.item.machine.ItemRTGPellet RTG-Pellets}: ihre Waerme wird zu Energie
 * ({@code heat * 10} je Tick) <b>und</b> bestimmt, wie schnell gecrackt wird. Erst ab Hitze 100
 * laeuft ueberhaupt etwas, und je heisser, desto kuerzer der Abstand - von dreissig Ticks
 * herunter auf fuenf. Wer schnell cracken will, muss also teure Pellets verheizen.</p>
 *
 * <p><b>Nicht portiert:</b> das Entseuchen ({@code sterilize}, ab Hitze 200) - dieser Port hat
 * kein Seuchensystem, an dem es etwas zu entfernen gaebe. Die eigentliche Fluid-Crack-Logik (100mB Input -&gt; zwei Output-Fluessigkeiten alle
 * {@code CRACK_INTERVAL} Ticks) ist 1:1 aus dem Original uebernommen, inkl. Wiederverwendung der
 * Cracking-Tower-Rezepttabelle wie im Original: eigene {@link RadiolysisRecipe}-Eintraege zuerst,
 * dann Fallback auf {@link CrackingTowerRecipe}.
 */
public class MachineRadiolysisBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    /** Original: {@code slot_rtg = {0..9}} - die zehn Pelletplaetze. */
    public static final int[] SLOTS_RTG = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
    /** Original: Platz 10 und 11 sind Ein- und Ausgabe der Fluidkennung. */
    public static final int SLOT_FLUID_ID = 10;
    public static final int SLOT_FLUID_ID_OUT = 11;
    /** Original: Platz 12 und 13 bestrahlen Gegenstaende - hier ohne Wirkung, siehe Javadoc. */
    public static final int SLOT_IRRADIATE_IN = 12;
    public static final int SLOT_IRRADIATE_OUT = 13;
    /** Original: {@code chargeItemsFromTE(slots, 14, ...)} - hier wird eine Batterie geladen. */
    public static final int SLOT_BATTERY = 14;
    private static final int SLOT_COUNT = 15;

    private static final long MAX_POWER = 1_000_000L;
    /** Original: {@code power += heat * 10}. */
    private static final int ENERGY_PER_HEAT = 10;
    /** Original: erst ab dieser Hitze wird ueberhaupt gecrackt. */
    private static final int CRACK_HEAT_MIN = 100;

    /** Original: {@code heat} - die Summe der Pelletwaerme. */
    private int heat = 0;

    private final FluidTank[] tanks = new FluidTank[3];

    public MachineRadiolysisBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIOLYSIS_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER);
        tanks[0] = new FluidTank(2_000);
        tanks[1] = new FluidTank(2_000);
        tanks[2] = new FluidTank(2_000);
    }

    public FluidTank[] getTanks() { return tanks; }

    /** Die aufsummierte Pelletwaerme - die Oberflaeche zeigt sie an. */
    public int getHeat() { return heat; }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineRadiolysisBlockEntity be) {
        if (level.isClientSide()) return;

        // 1:1: die Pellets liefern Waerme und altern dabei; daraus entsteht die Energie.
        be.heat = com.hbm_m.item.machine.ItemRTGPellet.updateRTGs(be.inventory, SLOTS_RTG);
        be.setEnergyStored(Math.min(be.getMaxEnergyStored(),
                be.getEnergyStored() + (long) be.heat * ENERGY_PER_HEAT));

        // Original: chargeItemsFromTE - der Platz gibt Energie ab, statt welche zu holen.
        be.chargeItemInSlot(SLOT_BATTERY);

        if (level.getGameTime() % 10 == 0) {
            for (Direction dir : Direction.values()) {
                be.trySubscribe(be.tanks[0].getTankType(), level, pos.relative(dir), dir);
                if (be.tanks[1].getFill() > 0) be.tryProvide(be.tanks[1], level, pos.relative(dir), dir);
                if (be.tanks[2].getFill() > 0) be.tryProvide(be.tanks[2], level, pos.relative(dir), dir);
            }
        }

        // 1:1: {@code crackTime = max(-0.1 * (heat - 100) + 30, 5)} - je heisser, desto oefter.
        if (be.heat > CRACK_HEAT_MIN) {
            int crackTime = (int) Math.max(-0.1D * (be.heat - CRACK_HEAT_MIN) + 30D, 5D);
            if (level.getGameTime() % crackTime == 0) be.crack();
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    /** Результат радиолиза/крекинга: (жидкость A, mB, жидкость B, mB) — общий для обоих рецептов. */
    private record CrackResult(net.minecraft.world.level.material.Fluid outA, int amountA,
                               net.minecraft.world.level.material.Fluid outB, int amountB) {}

    /**
     * Data-driven поиск: сначала собственный RadiolysisRecipe (вода → пероксид+водород),
     * затем делегирование в CrackingTowerRecipe — как оригинальный RadiolysisRecipes
     * делегировал в CrackingRecipes.
     */
    @org.jetbrains.annotations.Nullable
    private CrackResult findCrackRecipe() {
        if (level == null) return null;
        RadiolysisRecipe own = RadiolysisRecipe.getRecipe(level, tanks[0].getTankType());
        if (own != null) {
            return new CrackResult(own.getOutputA(), own.getOutputAMb(),
                    own.getOutputB() != null ? own.getOutputB() : own.getOutputA(),
                    own.getOutputBMb());
        }
        CrackingTowerRecipe fallback = CrackingTowerRecipe.getRecipe(level, tanks[0].getTankType());
        if (fallback != null) {
            return new CrackResult(fallback.getOutputA(), fallback.getOutputAMb(),
                    fallback.getOutputB() != null ? fallback.getOutputB() : fallback.getOutputA(),
                    fallback.getOutputBMb());
        }
        return null;
    }

    private void crack() {
        CrackResult recipe = findCrackRecipe();
        if (recipe == null) return;

        int left = recipe.amountA();
        int right = recipe.amountB();

        if (tanks[0].getFill() >= 100 && hasSpace(left, right)) {
            tanks[0].drainMb(100);
            if (left > 0) tanks[1].fillMb(recipe.outA(), left);
            if (right > 0) tanks[2].fillMb(recipe.outB(), right);
        }
    }

    private boolean hasSpace(int left, int right) {
        return tanks[1].getFill() + left <= tanks[1].getMaxFill() && tanks[2].getFill() + right <= tanks[2].getMaxFill();
    }

    // ==================== IFluidUserMK2 / MK2-Netz ====================

    @Override
    public FluidTank[] getAllTanks() { return tanks; }

    @Override
    public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1], tanks[2] }; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null && fromDir != Direction.DOWN;
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tanks[0].writeToNBT(tag, "tank0");
        tanks[1].writeToNBT(tag, "tank1");
        tanks[2].writeToNBT(tag, "tank2");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tanks[0].readFromNBT(tag, "tank0");
        tanks[1].readFromNBT(tag, "tank1");
        tanks[2].readFromNBT(tag, "tank2");
    }

    // ==================== GUI ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.radiolysis");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot >= SLOTS_RTG[0] && slot <= SLOTS_RTG[SLOTS_RTG.length - 1]) {
            return stack.getItem() instanceof com.hbm_m.item.machine.ItemRTGPellet;
        }
        return switch (slot) {
            case SLOT_FLUID_ID -> true;
            // Original: der Ausgabeplatz nimmt nichts an, er gibt nur heraus.
            case SLOT_FLUID_ID_OUT, SLOT_IRRADIATE_OUT -> false;
            case SLOT_IRRADIATE_IN -> true;
            case SLOT_BATTERY -> isEnergyReceiverItem(stack);
            default -> false;
        };
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return MachineRadiolysisMenu.create(id, inv, this);
    }
}
