package com.hbm_m.blockentity.machines.albion;

import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-Port von {@code TileEntityPARFC} (1.7.10): die Beschleunigerzelle (Radio Frequency Cavity).
 *
 * <p>Sie ist das einzige Bauteil, das dem Teilchen Impuls gibt - {@link #MOMENTUM_GAIN} je
 * Durchgang. Dafuer faechert sie den Strahl um denselben Betrag auf, weshalb hinter jeder Zelle
 * genug Quadrupole stehen muessen, sonst reisst der Strahl ab.</p>
 *
 * <p>Mit neun Bloecken Streckengewinn ist sie das ergiebigste Bauteil des Rings.</p>
 * <p><b>Es ist ein Multiblock.</b> Wie im Original steht das Teilchen nie auf dem Kern, sondern
 * auf einer seiner Dummyzellen; der Kern wird von dort aus gesucht. Daraus ergeben sich die
 * Spruenge von zwei bis fuenf Feldern - und damit die tatsaechliche Groesse eines Rings.</p>
 */
public class PARFCBlockEntity extends CooledMachineBlockEntity implements IParticleUser {

    public static final int SLOT_BATTERY = 0;
    public static final int INVENTORY_SIZE = 1;

    /** Original: {@code usage = 250_000}. */
    private static final long USAGE = 250_000L;
    /** Original: {@code momentumGain = 100}. */
    private static final int MOMENTUM_GAIN = 100;
    /** Original: {@code defocusGain = 100}. */
    private static final int DEFOCUS_GAIN = 100;
    private static final long MAX_POWER = 2_500_000L;
    /** Original: {@code addDistance(9)}. */
    private static final int DISTANCE = 9;
    /** Kettenbetrieb: einen Block weiter (siehe Klassenkommentar). */
    /** Original: {@code offset(rfcDir, -4)} - die Kammer ist neun Felder lang. */
    private static final int ENTRY_OFFSET = -4;
    /** Original: {@code offset(beamlineDir, 5)}. */
    private static final int EXIT_OFFSET = 5;

    public PARFCBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PA_RFC_BE.get(), pos, state, INVENTORY_SIZE, MAX_POWER, MAX_POWER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PARFCBlockEntity be) {
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
        // 1:1-Port: die Eingangszelle liegt vier Felder vor dem Kern.
        BlockPos input = worldPosition.relative(beamAxis(), ENTRY_OFFSET);
        return input.getX() == x && input.getY() == y && input.getZ() == z && beamAxis() == dir;
    }

    @Override
    public void onEnter(Particle particle, Direction dir) {
        if (!isCool())                     particle.crash(PAState.CRASH_NOCOOL);
        if (getEnergyStored() < USAGE)     particle.crash(PAState.CRASH_NOPOWER);

        if (particle.invalid) return;

        particle.addDistance(DISTANCE);
        particle.momentum += MOMENTUM_GAIN;
        particle.defocus(DEFOCUS_GAIN);
        setEnergyStored(getEnergyStored() - USAGE);
    }

    @Override
    public BlockPos getExitPos(Particle particle) {
        return worldPosition.relative(beamAxis(), EXIT_OFFSET);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == SLOT_BATTERY && isEnergyReceiverItem(stack);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.rfc");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new com.hbm_m.inventory.menu.PARFCMenu(id, inv, this);
    }
}
