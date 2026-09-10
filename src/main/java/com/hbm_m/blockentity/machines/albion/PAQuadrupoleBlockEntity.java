package com.hbm_m.blockentity.machines.albion;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.item.machine.ItemPACoil;
import com.hbm_m.item.machine.ItemPACoil.CoilType;

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
 * 1:1-Port von {@code TileEntityPAQuadrupole} (1.7.10): der Quadrupolmagnet buendelt den Strahl.
 *
 * <p>Er nimmt dem Teilchen {@link #FOCUS_GAIN} Streuung ab. Ist der Impuls kleiner als das
 * {@code quadMin} der eingesetzten Spule, arbeitet der Magnet mit dem <b>zehnfachen</b>
 * Energiebedarf; ueber {@code quadMax} verliert er den Strahl ganz.</p>
 * <p><b>Es ist ein Multiblock.</b> Wie im Original steht das Teilchen nie auf dem Kern, sondern
 * auf einer seiner Dummyzellen; der Kern wird von dort aus gesucht. Daraus ergeben sich die
 * Spruenge von zwei bis fuenf Feldern - und damit die tatsaechliche Groesse eines Rings.</p>
 */
public class PAQuadrupoleBlockEntity extends CooledMachineBlockEntity implements IParticleUser {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_COIL = 1;
    public static final int INVENTORY_SIZE = 2;

    /** Original: {@code usage = 100_000}. */
    private static final long USAGE = 100_000L;
    /** Original: {@code focusGain = 100}. */
    private static final int FOCUS_GAIN = 100;
    private static final long MAX_POWER = 1_000_000L;
    /** Original: {@code addDistance(3)}. */
    private static final int DISTANCE = 3;
    /** Kettenbetrieb: einen Block weiter (siehe Klassenkommentar). */
    /** Original: die Eingangszelle liegt ein Feld vor dem Kern. */
    private static final int ENTRY_OFFSET = -1;
    /** Original: {@code offset(beamlineDir, 2)}. */
    private static final int EXIT_OFFSET = 2;

    public PAQuadrupoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PA_QUADRUPOLE_BE.get(), pos, state, INVENTORY_SIZE, MAX_POWER, MAX_POWER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PAQuadrupoleBlockEntity be) {
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
        // 1:1-Port: die Eingangszelle liegt ein Feld vor dem Kern.
        BlockPos input = worldPosition.relative(beamAxis(), ENTRY_OFFSET);
        return input.getX() == x && input.getY() == y && input.getZ() == z && beamAxis() == dir;
    }

    @Override
    public void onEnter(Particle particle, Direction dir) {
        CoilType type = ItemPACoil.typeOf(getInventory().getStackInSlot(SLOT_COIL));

        // Original: unterhalb von quadMin kostet der Magnet das Zehnfache.
        int mult = (type != null && type.quadMin > particle.momentum) ? 10 : 1;

        if (!isCool())                                          particle.crash(PAState.CRASH_NOCOOL);
        if (getEnergyStored() < USAGE * mult)                   particle.crash(PAState.CRASH_NOPOWER);
        if (type == null)                                       particle.crash(PAState.CRASH_NOCOIL);
        if (type != null && type.quadMax < particle.momentum)   particle.crash(PAState.CRASH_OVERSPEED);

        if (particle.invalid) return;

        particle.addDistance(DISTANCE);
        particle.focus(FOCUS_GAIN);
        setEnergyStored(getEnergyStored() - USAGE * mult);
    }

    @Override
    public BlockPos getExitPos(Particle particle) {
        return worldPosition.relative(beamAxis(), EXIT_OFFSET);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_COIL) return stack.getItem() instanceof ItemPACoil;
        if (slot == SLOT_BATTERY) return isEnergyReceiverItem(stack);
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.quadrupole");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new com.hbm_m.inventory.menu.PAQuadrupoleMenu(id, inv, this); // Original: kein eigenes GUI, die Spule wird per Rechtsklick eingesetzt.
    }
}
