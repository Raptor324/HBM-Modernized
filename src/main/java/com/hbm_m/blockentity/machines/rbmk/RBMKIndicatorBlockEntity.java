package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.radio.RTTYNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

/**
 * 1:1 port of {@code TileEntityRBMKIndicator}: 6 lamps, each lit whenever its polled RTTY value
 * falls inside {@code [min,max]} (or outside, if {@code invert} is set).
 */
public class RBMKIndicatorBlockEntity extends RBMKPanelDeviceBlockEntity {

    public static final int UNITS = 6;
    public final String[]  channel = new String[UNITS];
    public final double[]  min     = new double[UNITS];
    public final double[]  max     = new double[UNITS];
    public final boolean[] invert  = new boolean[UNITS];
    public final boolean[] state   = new boolean[UNITS];
    /**
     * {@code polling}: a polling unit re-reads its channel every tick and falls back to zero when
     * the signal stops, so it reads as live. A non-polling one latches whatever it last saw. The
     * port had no polling flag, so every unit behaved as latching and a dead channel left the
     * readout frozen.
     */
    public final boolean[] polling = new boolean[UNITS];

    public RBMKIndicatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_INDICATOR_BE.get(), pos, state);
        Arrays.fill(channel, "");
        // 1:1 with IndicatorUnit: "public long max = 100" - an unconfigured lamp lights for
        // values 0..100, not just 0..1.
        Arrays.fill(max, 100.0);
        for (int i = 0; i < UNITS; i++) unitLabel[i] = "Indicator " + (i + 1);
    }

    /** CE's per-index lamp colours (IndicatorUnit constructor): alternating red and yellow. */
    @Override
    protected int defaultUnitColor(int index) {
        return index % 2 == 0 ? 0xFF0000 : 0xFFFF00;
    }

    @Override
    protected void onPanelTick(Level level, BlockPos pos) {
        for (int i = 0; i < UNITS; i++) {
            // CE's IndicatorUnit.update bails on an inactive unit or an unset channel and leaves the
            // lamp exactly as it was; the port cleared it instead, so switching a lamp off blanked
            // it rather than freezing its last reading.
            if (!isUnitActive(i)) continue;
            if (channel[i] == null || channel[i].isEmpty()) continue;

            RTTYNetwork.RttyChannel ch = RTTYNetwork.listen(level, channel[i]);
            // 1:1 with IndicatorUnit.update: stale entries (older than broadcast tick + 1) count
            // as "no signal" - a polling lamp re-decides on 0, a latching one keeps its last
            // state.
            if (ch != null && ch.timeStamp < level.getGameTime() - 1) ch = null;
            if (ch != null && ch.signal != null) {
                decideLight(i, parseNum(String.valueOf(ch.signal), 0));
            } else if (polling[i]) {
                decideLight(i, 0);
            }
        }
    }

    /**
     * 1:1 with CE's {@code decideLight}: a {@code max} below {@code min} deliberately inverts the
     * test, so the lamp lights <em>outside</em> the band instead of inside it. That inversion was
     * missing - the port always tested {@code value >= min && value <= max}, which is simply never
     * true once the bounds are crossed, so an inverted indicator stayed dark forever.
     *
     * <p>{@link #invert} is this port's own extra toggle (it is wired up in the panel's config
     * screen) and is applied on top of CE's result.</p>
     */
    private void decideLight(int i, double value) {
        boolean light = (min[i] <= max[i])
                ? (value >= min[i] && value <= max[i])
                : (value < max[i] || value > min[i]);
        state[i] = invert[i] != light;
    }

    /** Original per-unit array size (see the matching *Unit inner class). */
    @Override public int unitCount() { return UNITS; }

    @Override
    public void receiveControl(CompoundTag data) {
        receiveSharedControl(data);
        for (int i = 0; i < UNITS; i++) {
            if (data.contains("channel" + i)) {
                String ch = data.getString("channel" + i);
                channel[i] = ch.length() > 64 ? ch.substring(0, 64) : ch;
            }
            // 1:1 with TileEntityRBMKIndicator.receiveControl: an ABSENT bound resets the band
            // to unbounded (original: Integer.MIN_VALUE / Integer.MAX_VALUE) so the lamp always
            // lights. The GUI only sends the key when its text field actually parses.
            if (data.contains("min" + i)) {
                double v = data.getDouble("min" + i);
                if (Double.isFinite(v)) min[i] = v;
            } else {
                min[i] = -Double.MAX_VALUE;
            }
            if (data.contains("max" + i)) {
                double v = data.getDouble("max" + i);
                if (Double.isFinite(v)) max[i] = v;
            } else {
                max[i] = Double.MAX_VALUE;
            }
            if (data.contains("invert" + i))  invert[i]  = data.getBoolean("invert" + i);
            if (data.contains("polling" + i)) polling[i] = data.getBoolean("polling" + i);
        }
        setChanged();
        syncToClient();
    }

    // Persisted through writeNbtData/readNbtData, NOT saveAdditional/load:
    // BaseHbmBlockEntity builds the CLIENT update tag from writeNbtData alone, so a
    // subclass overriding saveAdditional saves to disk correctly yet sends the client
    // nothing - which is why these readouts stayed blank in world.
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        for (int i = 0; i < UNITS; i++) {
            tag.putString("channel" + i, channel[i]);
            tag.putDouble("min" + i, min[i]);
            tag.putDouble("max" + i, max[i]);
            tag.putBoolean("invert" + i, invert[i]);
            tag.putBoolean("polling" + i, polling[i]);
            // 1:1 with IndicatorUnit ("light" + index): the lamp state is what the whole device
            // is for - without it the in-world lamp never lit (baseTick ships this tag to
            // clients every tick, and the renderer reads be.state[i]).
            tag.putBoolean("light" + i, state[i]);
        }
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        for (int i = 0; i < UNITS; i++) {
            channel[i] = tag.contains("channel" + i) ? tag.getString("channel" + i) : "";
            min[i]     = tag.getDouble("min" + i);
            max[i]     = tag.contains("max" + i) ? tag.getDouble("max" + i) : 100.0;
            invert[i]  = tag.getBoolean("invert" + i);
            polling[i] = tag.getBoolean("polling" + i);
            state[i]   = tag.getBoolean("light" + i);
        }
    }
}
