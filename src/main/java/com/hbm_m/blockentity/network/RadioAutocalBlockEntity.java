package com.hbm_m.blockentity.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.radio.IRadioTorchConfigurable;
import com.hbm_m.blockentity.network.radio.RTTYNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityRadioAUTOCAL} (1.7.10 Original) - a small in-world programmable
 * terminal. Runs a stored line-based script, up to {@link #LINES_PER_TICK} lines per tick, with
 * label/jump control flow, a small integer variable store, redstone output pins, and RTTY channel
 * I/O via the existing {@link RTTYNetwork} bus.
 * <p>
 * SCOPE-Vereinfachung: Das Original benutzt eine eigene bytecode-artige Assemblersprache
 * ("MSES1Ext1", {@code com.hbm.module.ParseMSES1Ext1}) mit einem groesseren Befehlssatz. Ein
 * 1:1-Port dieser bespoke-VM wuerde den Rahmen sprengen; stattdessen implementiert dieser Port eine
 * eigene, kleinere aber real ausfuehrbare Skriptsprache mit demselben Grundprinzip (zeilenweise
 * Befehle, Labels/Sprungmarken, bedingte Sprachsteuerung, Redstone- und RTTY-I/O) - der
 * Kernmechanismus "programmierbares Redstone-/Funkterminal" bleibt erhalten, der genaue Befehlssatz
 * ist neu:
 * <pre>
 * LBL name                  - jump target marker (no-op)
 * SET var value             - set integer variable
 * ADD var value             - add to variable
 * JMP label                 - unconditional jump
 * IFEQ/IFLT/IFGT var value label - conditional jump
 * RS side value             - set redstone output (0..15) on side 0..5 (Direction ordinal)
 * RTTY.SEND channel value   - broadcast a value (literal or $var) on an RTTY channel
 * RTTY.LISTEN channel var   - store latest RTTY signal on a channel into a variable
 * WAIT ticks                - pause execution for N ticks
 * STOP                      - turn the terminal off
 * </pre>
 */
public class RadioAutocalBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements IRadioTorchConfigurable {

    private static final int LINES_PER_TICK = 20;
    private static final int MAX_ITERATIONS = 200;
    private static final int HISTORY_SIZE = 6;

    public final List<String> script = new ArrayList<>();
    public final List<String> history = new ArrayList<>();
    public boolean isOn = false;
    public boolean ignoreError = false;
    public boolean autoReboot = false;

    private final Map<String, Long> vars = new HashMap<>();
    private final Map<String, Integer> labels = new HashMap<>();
    private final int[] redstoneOut = new int[6];

    private int pc = 0;
    private int waitTicks = 0;

    public RadioAutocalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIO_AUTOCAL_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioAutocalBlockEntity be) {
        if (level.isClientSide) return;
        if (!be.isOn) return;

        if (be.waitTicks > 0) {
            be.waitTicks--;
            return;
        }

        int executed = 0;
        int iterations = 0;
        while (be.isOn && executed < LINES_PER_TICK && iterations < MAX_ITERATIONS) {
            iterations++;
            if (be.pc >= be.script.size()) {
                be.isOn = false;
                be.log("END");
                break;
            }

            String line = be.script.get(be.pc).trim();
            be.pc++;
            if (line.isEmpty() || line.startsWith("#")) continue;

            executed++;
            try {
                if (!be.execute(level, line)) break; // WAIT hit, stop this tick
            } catch (Exception ex) {
                be.log("ERROR: " + line);
                if (!be.ignoreError) {
                    be.isOn = false;
                    break;
                }
            }
        }

        be.setChanged();
    }

    /** Returns false if execution should pause for the rest of this tick (WAIT). */
    private boolean execute(Level level, String line) {
        String[] tok = line.split("\\s+");
        String cmd = tok[0].toUpperCase(java.util.Locale.ROOT);

        switch (cmd) {
            case "LBL" -> { }
            case "SET" -> vars.put(tok[1], resolve(tok[2]));
            case "ADD" -> vars.merge(tok[1], resolve(tok[2]), Long::sum);
            case "JMP" -> jumpTo(tok[1]);
            case "IFEQ" -> { if (resolveVar(tok[1]) == resolve(tok[2])) jumpTo(tok[3]); }
            case "IFLT" -> { if (resolveVar(tok[1]) < resolve(tok[2])) jumpTo(tok[3]); }
            case "IFGT" -> { if (resolveVar(tok[1]) > resolve(tok[2])) jumpTo(tok[3]); }
            case "RS" -> {
                int side = Integer.parseInt(tok[1]);
                if (side >= 0 && side < 6) redstoneOut[side] = (int) Math.max(0, Math.min(15, resolve(tok[2])));
            }
            case "RTTY.SEND" -> RTTYNetwork.broadcast(level, tok[1], String.valueOf(resolve(tok[2])));
            case "RTTY.LISTEN" -> {
                RTTYNetwork.RttyChannel sig = RTTYNetwork.listen(level, tok[1]);
                if (sig != null && sig.signal != null) {
                    try { vars.put(tok[2], Long.parseLong(String.valueOf(sig.signal))); } catch (NumberFormatException ignored) {}
                }
            }
            case "WAIT" -> { waitTicks = Math.max(0, (int) resolve(tok[1])); return false; }
            case "STOP" -> isOn = false;
            case "PRINT" -> log(line.substring(Math.min(line.length(), 6)));
            default -> throw new IllegalArgumentException("unknown command: " + cmd);
        }
        return true;
    }

    private long resolve(String token) {
        if (token.startsWith("$")) return resolveVar(token.substring(1));
        try { return Long.parseLong(token); } catch (NumberFormatException e) { return 0L; }
    }

    private long resolveVar(String name) {
        return vars.getOrDefault(name.startsWith("$") ? name.substring(1) : name, 0L);
    }

    private void jumpTo(String label) {
        Integer target = labels.get(label);
        if (target != null) pc = target;
    }

    private void log(String msg) {
        history.add(0, msg);
        while (history.size() > HISTORY_SIZE) history.remove(history.size() - 1);
    }

    public int getRedstoneOutput(int side) {
        return side >= 0 && side < 6 ? redstoneOut[side] : 0;
    }

    private void regenerateLabels() {
        labels.clear();
        for (int i = 0; i < script.size(); i++) {
            String line = script.get(i).trim();
            if (line.toUpperCase(java.util.Locale.ROOT).startsWith("LBL ")) {
                labels.put(line.substring(4).trim(), i);
            }
        }
    }

    public void setScript(List<String> lines) {
        script.clear();
        script.addAll(lines);
        regenerateLabels();
        pc = 0;
        waitTicks = 0;
        setChanged();
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("payload")) {
            String payload = data.getString("payload");
            List<String> lines = new ArrayList<>();
            for (String l : payload.split("\n")) lines.add(l.trim());
            setScript(lines);
        }
        if (data.contains("on")) {
            boolean on = data.getBoolean("on");
            if (on && !isOn) { pc = 0; waitTicks = 0; regenerateLabels(); }
            isOn = on;
        }
        if (data.contains("ignore")) ignoreError = data.getBoolean("ignore");
        if (data.contains("auto")) autoReboot = data.getBoolean("auto");

        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ListTag scriptTag = new ListTag();
        for (String line : script) scriptTag.add(StringTag.valueOf(line));
        tag.put("script", scriptTag);
        tag.putBoolean("isOn", isOn);
        tag.putBoolean("ignoreError", ignoreError);
        tag.putBoolean("autoReboot", autoReboot);
        tag.putInt("pc", pc);
        tag.putInt("waitTicks", waitTicks);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        script.clear();
        ListTag scriptTag = tag.getList("script", 8);
        for (int i = 0; i < scriptTag.size(); i++) script.add(scriptTag.getString(i));
        regenerateLabels();
        isOn = tag.getBoolean("isOn");
        ignoreError = tag.getBoolean("ignoreError");
        autoReboot = tag.getBoolean("autoReboot");
        pc = tag.getInt("pc");
        waitTicks = tag.getInt("waitTicks");
    }
}
