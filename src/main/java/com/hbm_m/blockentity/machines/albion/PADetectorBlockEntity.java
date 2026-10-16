package com.hbm_m.blockentity.machines.albion;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.recipe.ParticleAcceleratorRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code TileEntityPADetector} (1.7.10): das Ende der Strahlstrecke.
 *
 * <p>Hier wird abgerechnet. Der Detektor nimmt das Teilchen an und prueft der Reihe nach:
 * ist der Strahl <b>vollkommen</b> gebuendelt (Streuung genau null), ist Energie da, ist er kalt
 * genug? Danach sucht er das Rezept zu den beiden mitgereisten Stoffen und vergleicht den Impuls.
 * Passt alles, landen die Produkte in den Ausgabefaechern und der Lauf endet mit
 * {@link PAState#SUCCESS}.</p>
 *
 * <p>Ein erfolgreicher Lauf meldet sich nach oben: an den
 * {@link com.hbm_m.satellite.DetectorEvents Detektor} als mittelstarker Ausbruch und an den
 * {@link com.hbm_m.satellite.RayScanEvents Strahlenscanner} als Teilchenquelle, die noch eine halbe
 * Minute nachleuchtet. Wer einen Beschleuniger betreibt, ist damit ortbar.</p>
 *
 * <p><b>Abweichungen:</b> Die Behaelterrueckgabe des Originals ({@code getContainerItem}, Slots 1
 * und 2) entfaellt - keines der portierten Rezepte nutzt Behaelter. Der Omega-12-Erfolg beim
 * Digamma-Teilchen fehlt, weil dieser Port keine Erfolge dieser Art kennt.</p>
 */
public class PADetectorBlockEntity extends CooledMachineBlockEntity implements IParticleUser {

    public static final int SLOT_BATTERY = 0;
    /** Original: Slots 3 und 4 sind die Ausgaben. */
    public static final int SLOT_OUTPUT_A = 1;
    public static final int SLOT_OUTPUT_B = 2;
    public static final int INVENTORY_SIZE = 3;

    /** Original: {@code usage = 100_000}. */
    private static final long USAGE = 100_000L;
    private static final long MAX_POWER = 1_000_000L;


    public PADetectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PA_DETECTOR_BE.get(), pos, state, INVENTORY_SIZE, MAX_POWER, MAX_POWER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PADetectorBlockEntity be) {
        if (level.isClientSide()) return;

        be.ensureNetworkInitialized();
        be.chargeFromBatterySlot(SLOT_BATTERY);
        be.tickCooling(level, pos);

        be.setChanged();
        be.sendUpdateToClient();
    }

    private Direction beamAxis() {
        return PAOrientation.beamAxis(getBlockState());
    }

    @Override
    public boolean canParticleEnter(Particle particle, Direction dir, int x, int y, int z) {
        // 1:1-Port: {@code offset(detectorDir, -4)} - der Detektor ist neun Felder lang.
        BlockPos input = worldPosition.relative(beamAxis(), -4);
        return input.getX() == x && input.getY() == y && input.getZ() == z && beamAxis() == dir;
    }

    @Override
    public void onEnter(Particle particle, Direction dir) {
        // Der Lauf endet hier in jedem Fall.
        particle.invalid = true;

        // Original: der Strahl muss vollkommen gebuendelt ankommen.
        if (particle.defocus > 0)          { particle.crash(PAState.CRASH_DEFOCUS); return; }
        if (getEnergyStored() < USAGE)     { particle.crash(PAState.CRASH_NOPOWER); return; }
        if (!isCool())                     { particle.crash(PAState.CRASH_NOCOOL);  return; }

        setEnergyStored(getEnergyStored() - USAGE);

        ParticleAcceleratorRecipe recipe =
                ParticleAcceleratorRecipe.find(level, particle.input1, particle.input2);

        if (recipe == null) {
            particle.crash(PAState.CRASH_NORECIPE);
            return;
        }

        if (particle.momentum < recipe.getMomentum()) {
            particle.crash(PAState.CRASH_UNDERSPEED);
            return;
        }

        if (canAccept(recipe)) {
            insert(SLOT_OUTPUT_A, recipe.getOutputA());
            if (recipe.hasOutputB()) insert(SLOT_OUTPUT_B, recipe.getOutputB());
        }

        if (getLevel() != null) {
            com.hbm_m.satellite.RayScanEvents.reportEvent(getLevel(), worldPosition, com.hbm_m.satellite.RayScanEvents.INFO_PARTICLE, 600);
            com.hbm_m.satellite.DetectorEvents.reportEvent(getLevel(), com.hbm_m.satellite.DetectorEvents.DURATION_MEDIUM,
                    com.hbm_m.satellite.DetectorEvents.BurstIntensity.MEDIUM, worldPosition.getX(), worldPosition.getZ());
        }

        particle.crash(PAState.SUCCESS);
    }

    /** Original: {@code canAccept} - beide Ausgaben muessen Platz haben. */
    private boolean canAccept(ParticleAcceleratorRecipe recipe) {
        return fits(SLOT_OUTPUT_A, recipe.getOutputA())
                && (!recipe.hasOutputB() || fits(SLOT_OUTPUT_B, recipe.getOutputB()));
    }

    private boolean fits(int slot, ItemStack output) {
        if (output == null || output.isEmpty()) return true;

        ItemStack present = getInventory().getStackInSlot(slot);
        if (present.isEmpty()) return true;

        return com.hbm_m.platform.PlatformHooks.isSameItemSameTags(present, output)
                && present.getCount() + output.getCount() <= output.getMaxStackSize();
    }

    private void insert(int slot, ItemStack output) {
        if (output == null || output.isEmpty()) return;

        ItemStack present = getInventory().getStackInSlot(slot);
        if (present.isEmpty()) {
            getInventory().setStackInSlot(slot, output.copy());
        } else {
            present.grow(output.getCount());
        }
    }

    @Override
    @Nullable
    public BlockPos getExitPos(Particle particle) {
        return null; // Original: das Teilchen verlaesst den Detektor nicht.
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == SLOT_BATTERY && isEnergyReceiverItem(stack);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.pa_detector");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new com.hbm_m.inventory.menu.PADetectorMenu(id, inv, this);
    }
}
