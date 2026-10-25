package com.hbm_m.blockentity.machines;

import com.hbm_m.platform.RenderBounds;

import com.hbm_m.api.energy.Nodespace;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.blockentity.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineFENSU} (LEGACY): Batterie mit {@code Long.MAX_VALUE} HE und einem Uebertragungslimit
 * von 1e16 HE je Takt. Anders als die normale Batterie ist sie immer ein Netzknoten, der nur nach unten (unter den
 * Kern) anschliesst; die Betriebsart entscheidet nur, ob sie dort abgibt und/oder aufnimmt. Auf dem Client dreht
 * sich die Scheibe je nach Ladung.
 *
 * <p>GUI, Redstone-Modi, Prioritaet und Mitnahme der Ladung beim Abbau ({@code IPersistentNBT}) erbt sie von
 * {@link MachineBatteryBlockEntity} - wie im Original von {@code TileEntityMachineBattery}.
 * Port-Modi: 0 Puffer, 1 Eingang, 2 Ausgang, 3 aus.</p>
 */
public class MachineFENSUBlockEntity extends MachineBatteryBlockEntity {

    public float prevRotation = 0F;
    public float rotation = 0F;

    public static final long maxTransfer = 10_000_000_000_000_000L;

    public long[] log = new long[20];
    public long delta = 0;

    private long lastNetTick = Long.MIN_VALUE;

    public MachineFENSUBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_FENSU_BE.get(), pos, state, Long.MAX_VALUE, maxTransfer);
    }

    public static void tickFensu(Level level, BlockPos pos, BlockState state, MachineFENSUBlockEntity be) {

        if (!level.isClientSide) {

            long prevPower = be.getEnergyStored();

            // Library.chargeItemsFromTE(slots, 1, ...)
            be.chargeItemInSlot(1);

            // Knoten, tryProvide nach unten, addReceiver/removeReceiver je nach Modus
            be.ensureNetworkInitialized();

            // Library.chargeTEFromItems(slots, 0, ...)
            be.chargeFromBatterySlot(0);

            long power = be.getEnergyStored();
            long avg = (power / 2 + prevPower / 2);
            be.delta = avg - be.log[0];

            for (int i = 1; i < be.log.length; i++) {
                be.log[i - 1] = be.log[i];
            }

            be.log[19] = avg;

            // networkPackNT(20): Ladung fuer die Scheibe
            if (power != prevPower || level.getGameTime() % 20 == 0) {
                be.setChanged();
                be.sendUpdateToClient();
            }
        } else {
            be.prevRotation = be.rotation;
            be.rotation += be.getSpeed();

            if (be.rotation >= 360) {
                be.rotation -= 360;
                be.prevRotation -= 360;
            }
        }
    }

    /**
     * Original-{@code updateEntity}, Netzteil: der eigene Knoten existiert immer (nicht nur im Puffermodus), abgegeben
     * wird ueber {@code tryProvide} nach unten, aufgenommen ueber die eigene Netzanmeldung.
     */
    @Override
    public void ensureNetworkInitialized() {
        if (!(level instanceof ServerLevel world) || isRemoved()) return;

        long now = world.getGameTime();
        if (now == lastNetTick) return; // Eigener Tick und zentraler Treiber rufen beide; nur einmal je Takt
        lastNetTick = now;

        Nodespace.PowerNode node = Nodespace.getNode(world, worldPosition);
        if (node == null || node.expired) {
            node = this.createNode(worldPosition);
            Nodespace.createNode(world, node);
        }

        int mode = getCurrentMode();
        boolean output = mode == 0 || mode == 2;
        boolean input = mode == 0 || mode == 1;

        if (output) {
            this.tryProvide(world, worldPosition.getX(), worldPosition.getY() - 1, worldPosition.getZ(), Direction.DOWN);
        } else {
            if (node.hasValidNet()) node.net.removeProvider(this);
        }

        if (input) {
            if (node.hasValidNet()) node.net.addReceiver(this);
        } else {
            if (node.hasValidNet()) node.net.removeReceiver(this);
        }
    }

    /** {@code createNode}: ein Knoten am Kern mit genau einer Verbindung nach unten. */
    @Override
    public Nodespace.PowerNode createNode(BlockPos pos) {
        return new Nodespace.PowerNode(Nodespace.THE_POWER_PROVIDER, pos)
                .setConnections(new NodeDirPos(pos.getX(), pos.getY() - 1, pos.getZ(), Direction.DOWN));
    }

    public float getSpeed() {
        return (float) Math.pow(Math.log(getEnergyStored() * 0.75 + 1) * 0.05F, 5);
    }

    /** Original {@code delta} gilt fuer 20 Takte, die Port-GUI rechnet je Takt. */
    @Override
    public long getEnergyDelta() {
        return this.delta / 20L;
    }

    @Override
    public long transferPower(long power) {

        long overshoot = 0;

        // if power exceeds our transfer limit, truncate
        if (power > maxTransfer) {
            overshoot += power - maxTransfer;
            power = maxTransfer;
        }

        // this check is in essence the same as the default implementation, but re-arranged to never overflow the int64 range
        // if the remaining power exceeds the power cap, truncate again
        long freespace = this.getMaxEnergyStored() - this.getEnergyStored();

        if (freespace < power) {
            overshoot += power - freespace;
            power = freespace;
        }

        // what remains is sure to not exceed the transfer limit and the power cap (and therefore the int64 range)
        this.setEnergyStored(this.getEnergyStored() + power);

        return overshoot;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return RenderBounds.INFINITE;
    }
}
