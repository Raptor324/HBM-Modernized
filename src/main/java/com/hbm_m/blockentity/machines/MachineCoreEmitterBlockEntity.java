package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineCoreEmitterBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.ILaserable;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 1:1-Port von {@code TileEntityCoreEmitter} (1.7.10): der Strahler des Dunklen Fusionsreaktors.
 *
 * <p>Er zieht Energie aus dem Netz, verwandelt sie in einen Laserstrahl und schickt ihn bis zu
 * {@value #RANGE} Bloecke weit. Wieviel er dabei umsetzt, bestimmt der Wattregler: der Verbrauch
 * ist {@code maxPower * watts / 2000}, der Ertrag {@code watts * 100} - beides linear, der Regler
 * ist also eine reine Drosselung ohne Wirkungsgradverlust.</p>
 *
 * <p><b>Der Strahl geht durch den Kern hindurch.</b> Trifft er einen
 * {@link com.hbm_m.blockentity.machines.dfc.DFCCoreBlockEntity Fusionskern}, verbrennt der die
 * Energie und gibt ein Vielfaches zurueck, mit dem der Strahl weiterlaeuft - daher {@code continue}
 * und nicht {@code break}. Genau daraus entsteht der Ertrag des ganzen Reaktors.</p>
 *
 * <p><b>Der Strahl ist gefaehrlich.</b> Alles Lebende auf seiner Bahn nimmt fuenfzig Schaden und
 * brennt zehn Sekunden. Fluessigkeiten verdampfen mit einem Zischen, feste Bloecke unter
 * {@value #BEAM_BLOCK_RESISTANCE} Sprengwiderstand zerbrechen mit einer Wahrscheinlichkeit von
 * eins zu zwanzig je Tick - eine fehlgeleitete Anlage frisst sich also langsam durch die
 * Landschaft. Nur wirklich harte Bloecke halten dauerhaft stand.</p>
 *
 * <p><b>Und er braucht Kuehlung.</b> Solange gefeuert wird, gehen 20 mB Cryogel je Tick durch;
 * geht der Tank leer, wird der Strahler selbst zu fliessender Lava. Das ist kein Schoenheitsfehler,
 * sondern der eigentliche Grund, warum eine DFC-Anlage eine Kuehlmittelversorgung braucht.</p>
 */
public class MachineCoreEmitterBlockEntity extends BaseMachineBlockEntity
        implements ILaserable, com.hbm_m.api.redstoneoverradio.IRORInteractive {

    /** Original: {@code maxPower = 1000000000L}. */
    public static final long MAX_POWER = 1_000_000_000L;
    /** Original: {@code range = 50}. */
    public static final int RANGE = 50;
    /** Original: {@code tank.setFill(tank.getFill() - 20)}. */
    public static final int COOLANT_PER_TICK_MB = 20;
    public static final int COOLANT_CAPACITY_MB = 64_000;

    /** Original: der Sprengwiderstand, ab dem ein Block dem Strahl standhaelt. */
    public static final float BEAM_BLOCK_RESISTANCE = 6000F;

    private final FluidTank coolantTank = new FluidTank(ModFluids.CRYOGEL.getSource(), COOLANT_CAPACITY_MB);

    /** Original: {@code watts} - 1 bis 100 Prozent. */
    private int watts = 1;
    /** Original: {@code beam} - wie weit der Strahl zuletzt kam. */
    private int beam;
    /** Original: {@code joules} - was in diesem Tick durch den Strahl geht. */
    private long joules;
    /** Original: {@code prev} - der Wert des letzten Ticks, den die Oberflaeche anzeigt. */
    private long prev;
    /** Original: {@code isOn} - der Schalter in der Oberflaeche. */
    private boolean isOn;

    public MachineCoreEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CORE_EMITTER_BE.get(), pos, state, 4, MAX_POWER, MAX_POWER, 0L);
    }

    //? if forge {
    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(
            net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.FLUID_HANDLER) {
            return coolantTank.getForgeFluidCapability().cast();
        }
        return super.getCapability(cap, side);
    }
    //?}

    public FluidTank getCoolantTank() { return coolantTank; }
    public int getBeamLength()        { return beam; }
    public int getWatts()             { return watts; }
    public long getOutput()           { return prev; }
    public boolean isOn()             { return isOn; }

    /** Original: {@code MathHelper.clamp_int(watts, 1, 100)}. */
    public void setWatts(int watts) {
        this.watts = Math.max(1, Math.min(100, watts));
        setChanged();
    }

    public void toggle() {
        this.isOn = !this.isOn;
        setChanged();
    }

    /** Original: {@code demand = maxPower * watts / 2000}. */
    public long getDemand() {
        return MAX_POWER * watts / 2000L;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCoreEmitterBlockEntity be) {
        if (level.isClientSide()) return;

        be.ensureNetworkInitialized();
        be.serverTick(level, pos, state);
    }

    /** 1:1-Port von {@code updateEntity}. */
    private void serverTick(Level level, BlockPos pos, BlockState state) {

        watts = Math.max(1, Math.min(100, watts));
        long demand = getDemand();

        beam = 0;

        // Solange etwas durch den Strahl geht, laeuft die Kuehlung mit - und wenn nicht, gluehen
        // die Spulen durch.
        if (joules > 0 || prev > 0) {
            if (coolantTank.getFill() >= COOLANT_PER_TICK_MB) {
                coolantTank.setFill(coolantTank.getFill() - COOLANT_PER_TICK_MB);
            } else {
                level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
                return;
            }
        }

        if (isOn) {
            // 50.000.000 HE ergeben 10.000 Spk - ein Spk kostet also 5.000 HE.
            if (getEnergyStored() >= demand) {
                setEnergyStored(getEnergyStored() - demand);
                joules += watts * 100L;
            }
            prev = joules;

            if (joules > 0) {
                long out = joules * 95L / 100L;
                Direction dir = state.getValue(MachineCoreEmitterBlock.FACING);

                fireBeam(level, pos, dir, out);
                joules = 0;

                harmAlongBeam(level, pos, dir);
            }
        } else {
            joules = 0;
            prev = 0;
        }

        setChanged();
        sendUpdateToClient();
    }

    /** 1:1-Port der Strahlschleife. */
    private void fireBeam(Level level, BlockPos origin, Direction dir, long out) {

        for (int i = 1; i <= RANGE; i++) {
            beam = i;

            BlockPos at = origin.relative(dir, i);
            if (!level.isLoaded(at)) break;

            BlockEntity te = level.getBlockEntity(at);

            // Der Kern verbrennt die Energie und gibt ein Vielfaches zurueck - der Strahl laeuft
            // damit weiter, statt hier zu enden.
            if (te instanceof com.hbm_m.blockentity.machines.dfc.DFCCoreBlockEntity core) {
                out = core.burn(out);
                continue;
            }

            if (te instanceof ILaserable laserable) {
                laserable.addLaserEnergy(level, at, out, dir);
                break;
            }

            BlockState hit = level.getBlockState(at);
            if (hit.isAir()) continue;

            // Fluessigkeiten verdampfen.
            if (!hit.getFluidState().isEmpty()) {
                level.playSound(null, at, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.removeBlock(at, false);
                break;
            }

            // Alles unter 6000 Sprengwiderstand bricht mit der Zeit weg.
            if (hit.getBlock().getExplosionResistance() < BEAM_BLOCK_RESISTANCE
                    && level.getRandom().nextInt(20) == 0) {
                level.destroyBlock(at, false);
            }

            break;
        }
    }

    /** 1:1-Port der Trefferbox: alles Lebende auf der Bahn nimmt Schaden und brennt. */
    private void harmAlongBeam(Level level, BlockPos pos, Direction dir) {

        double blx = Math.min(pos.getX(), pos.getX() + dir.getStepX() * beam) + 0.2D;
        double bux = Math.max(pos.getX(), pos.getX() + dir.getStepX() * beam) + 0.8D;
        double bly = Math.min(pos.getY(), pos.getY() + dir.getStepY() * beam) + 0.2D;
        double buy = Math.max(pos.getY(), pos.getY() + dir.getStepY() * beam) + 0.8D;
        double blz = Math.min(pos.getZ(), pos.getZ() + dir.getStepZ() * beam) + 0.2D;
        double buz = Math.max(pos.getZ(), pos.getZ() + dir.getStepZ() * beam) + 0.8D;

        List<Entity> list = level.getEntitiesOfClass(Entity.class, new AABB(blx, bly, blz, bux, buy, buz));

        for (Entity e : list) {
            //? if < 1.21.1 {
            e.hurt(com.hbm_m.damagesource.ModDamageSources.amsCore(level), 50F);
            e.setSecondsOnFire(10);
            //?} else {
            /*if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                e.hurt(serverLevel, com.hbm_m.damagesource.ModDamageSources.amsCore(level), 50F);
            }
            e.setRemainingFireTicks(10 * 20);
            *///?}
        }
    }

    /**
     * 1:1-Port von {@code addEnergy}: der Strahler nimmt fremde Strahlen an - aber nicht von vorn,
     * sonst liessen sich zwei Strahler gegeneinander stellen und Energie im Kreis schicken.
     */
    @Override
    public boolean addLaserEnergy(Level level, BlockPos pos, long energy, Direction beamDirection) {
        Direction facing = getBlockState().getValue(MachineCoreEmitterBlock.FACING);
        if (beamDirection.getOpposite() == facing) return false;

        joules += energy;
        setChanged();
        return true;
    }

    // ── Redstone-over-Radio ──

    /** 1:1 aus {@code TileEntityCoreEmitter}. */
    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_FUNCTION + "setpower" + NAME_SEPARATOR + "percent",
                PREFIX_FUNCTION + "toggle",
                PREFIX_FUNCTION + "switch" + NAME_SEPARATOR + "on/off"
        };
    }

    @Override
    public String runRORFunction(String name, String[] params) {

        if ((PREFIX_FUNCTION + "setpower").equals(name) && params.length > 0) {
            // Original erlaubt hier ausdruecklich auch die 0, obwohl der Tick auf 1 hochzieht.
            this.watts = com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(params[0], 0, 100);
            setChanged();
            return null;
        }

        if ((PREFIX_FUNCTION + "toggle").equals(name)) {
            isOn = !isOn;
            setChanged();
            return null;
        }

        if ((PREFIX_FUNCTION + "switch").equals(name) && params.length > 0) {
            if ("on".equals(params[0]))  { isOn = true;  setChanged(); }
            if ("off".equals(params[0])) { isOn = false; setChanged(); }
        }

        return null;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        coolantTank.writeToNBT(tag, "coolant");
        tag.putInt("watts", watts);
        tag.putInt("beam", beam);
        tag.putLong("joules", joules);
        tag.putLong("prev", prev);
        tag.putBoolean("isOn", isOn);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        coolantTank.readFromNBT(tag, "coolant");
        watts = tag.contains("watts") ? tag.getInt("watts") : 1;
        beam = tag.getInt("beam");
        joules = tag.getLong("joules");
        prev = tag.getLong("prev");
        isOn = tag.getBoolean("isOn");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.core_emitter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return com.hbm_m.inventory.menu.MachineCoreEmitterMenu.create(id, inv, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.core_emitter");
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }
}
