package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.radio.RTTYNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1-in-spirit port of {@code TileEntityRBMKTerminal}: a free-text command console on the RTTY
 * bus. Supports the original's core command set - {@code chan <name>} sets the active channel,
 * {@code send <value>} broadcasts once, {@code start <value>} begins continuously re-broadcasting
 * that value every tick, {@code stop} ends it.
 * <p>
 * <p>Ueber Redstone-over-Radio laesst sich der Bildschirm auch von aussen beschreiben:
 * {@code clear} leert ihn, {@code write} schiebt eine Zeile oben hinein, {@code set&lt;Zeile&gt;}
 * ueberschreibt eine bestimmte der siebzehn Zeilen, und {@code submit} fuehrt einen Befehl aus, als
 * haette ihn jemand eingetippt. Damit wird das Terminal zur Anzeigetafel einer Anlage, die sich
 * selbst beschriftet.</p>
 *
 * <p><b>Nicht portiert:</b> der OpenComputers-Modus ({@code ocMode}) - fuer OpenComputers gibt es
 * in diesem Port an keiner Stelle eine Anbindung.</p>
 */
public class RBMKTerminalBlockEntity extends RBMKPanelDeviceBlockEntity
        implements com.hbm_m.api.redstoneoverradio.IRORInteractive {

    public String channel = "";
    public boolean running = false;
    // 1:1 with TileEntityRBMKTerminal.repeatCmd: defaults to "" - only a non-empty recorded
    // command repeats (see the onPanelTick gate).
    public String runningValue = "";

    /** TileEntityRBMKTerminal.history - the 17-line scrollback the renderer prints under the
     *  working line. Newest entry first, exactly like the original's shift-and-insert. */
    public final String[] history = new String[17];

    /** TileEntityRBMKTerminal.doesRepeat - drives the terminal text amber instead of green. */
    public boolean doesRepeat = false;

    /**
     * 1:1 with {@code TileEntityRBMKTerminal.push}: the loop runs down to index 1 only (starts
     * at {@code length - 2}), so the bottom (17th) line is never written and the oldest line
     * falls off a row early - the bottom line of the terminal stays permanently blank. That is
     * the original's exact quirk; the port's "fixed" version that filled every slot deviated
     * from 1.7.10.
     */
    public void pushHistory(String msg) {
        for (int i = history.length - 2; i > 0; i--) history[i] = history[i - 1];
        history[0] = msg == null ? "" : msg;
    }

    public RBMKTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_TERMINAL_BE.get(), pos, state);
        java.util.Arrays.fill(history, "");
    }

    @Override
    protected void onPanelTick(Level level, BlockPos pos) {
        // 1:1 with TileEntityRBMKTerminal.updateEntity: the repeat fires every tick only while
        // BOTH channel and repeat command are non-empty (original L44-46) - so a start issued
        // before the channel was set begins repeating the moment "chan" is set later.
        if (running && !channel.isEmpty() && !runningValue.isEmpty()) {
            RTTYNetwork.broadcast(level, channel, runningValue);
        }
    }

    // ── Redstone-over-Radio ──

    /** 1:1 aus {@code TileEntityRBMKTerminal.getFunctionInfo}. */
    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_FUNCTION + "clear",
                PREFIX_FUNCTION + "write" + NAME_SEPARATOR + "text",
                PREFIX_FUNCTION + "set<line#>" + NAME_SEPARATOR + "text",
                PREFIX_FUNCTION + "submit" + NAME_SEPARATOR + "command"
        };
    }

    @Override
    public String runRORFunction(String name, String[] params) {

        if ((PREFIX_FUNCTION + "clear").equals(name)) {
            java.util.Arrays.fill(history, "");
            setChanged();
            syncToClient();
            return null;
        }

        String allParams = String.join(" ", params);

        if ((PREFIX_FUNCTION + "write").equals(name)) {
            pushHistory(allParams);
            setChanged();
            syncToClient();
            return null;
        }

        if (name.startsWith(PREFIX_FUNCTION + "set")) {
            int line = com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(
                    name.substring(PREFIX_FUNCTION.length() + 3), 1, history.length) - 1;
            history[line] = allParams;
            setChanged();
            syncToClient();
            return null;
        }

        if ((PREFIX_FUNCTION + "submit").equals(name) && getLevel() != null) {
            eval(getLevel(), allParams);
            return null;
        }

        return null;
    }

    /** Evaluates a single command line, returning a response string for the GUI's scrollback. */
    public String eval(Level level, String line) {
        if (line == null) return "";
        // 1:1 with TileEntityRBMKTerminal.eval: push(cmd) runs BEFORE the empty check (L61-62),
        // so an empty input echoes as a blank line into the scrollback instead of vanishing.
        pushHistory(line);
        String[] parts = line.trim().split("\\s+", 2);
        if (parts.length == 0 || parts[0].isEmpty()) return "";

        String cmd = parts[0].toLowerCase(java.util.Locale.ROOT);
        String arg = parts.length > 1 ? parts[1] : "";

        String result = switch (cmd) {
            case "chan" -> {
                // CE's bare "chan" clears the channel rather than reporting it - that is the only
                // way to detach a terminal again.
                channel = arg;
                yield "Set channel to " + (channel.isEmpty() ? "<none>" : channel);
            }
            case "send" -> {
                if (channel.isEmpty()) yield "Cannot send - no channel set";
                // 1:1 with TileEntityRBMKTerminal.eval: the payload is broadcast verbatim -
                // "send " with an empty payload transmits "" (downstream numeric listeners parse
                // that as a failure/0), it is never substituted with "1".
                RTTYNetwork.broadcast(level, channel, arg);
                yield "Sent signal on " + channel;
            }
            // 1:1 with TileEntityRBMKTerminal.eval "start": the repeat command is recorded
            // unconditionally (even with no channel set, and even empty) - the terminal only
            // begins repeating once BOTH channel and value are non-empty, so a start issued
            // before setting the channel fires the moment "chan" is set later.
            case "start" -> {
                running = true;
                runningValue = arg;
                yield "Repeating signal on " + channel;
            }
            case "stop" -> {
                running = false;
                yield "Stopping repeat signal";
            }
            // CE's three remaining commands, none of which the port had.
            case "clear" -> {
                java.util.Arrays.fill(history, "");
                yield "";
            }
            case "horse" -> "Horse.";
            // 1:1 with TileEntityRBMKTerminal.eval "selfdestruct": destroy the block without
            // drops, then the no-block-damage ExplosionVNT(5) with piercing entity processor,
            // player processor and weapon SFX (RTTYNetwork.selfDestruct is the already-ported
            // 1:1 equivalent) - the ground stays intact.
            case "selfdestruct" -> {
                RTTYNetwork.selfDestruct(level, getBlockPos());
                yield "";
            }
            default -> "Unrecognized command!";
        };

        // The original pushes the raw command once at the top of eval (see above) and each
        // command handler pushes its own response line (TileEntityRBMKTerminal:66-98).
        if (!"clear".equals(cmd)) {
            if (result != null && !result.isEmpty()) pushHistory(result);
        }
        doesRepeat = running;

        setChanged();
        syncToClient();
        return result;
    }

    /** Original per-unit array size (see the matching *Unit inner class). */
    @Override public int unitCount() { return 0; }

    @Override
    public void receiveControl(CompoundTag data) {
        receiveSharedControl(data);
        if (data.contains("cmd") && level != null) {
            eval(level, data.getString("cmd"));
            return; // eval() already calls setChanged()/syncToClient()
        }
        if (data.contains("channel")) channel = data.getString("channel");
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
        tag.putString("channel", channel);
        tag.putBoolean("running", running);
        tag.putBoolean("doesRepeat", doesRepeat);
        for (int i = 0; i < history.length; i++)
            tag.putString("history" + i, history[i] == null ? "" : history[i]);
        tag.putString("runningValue", runningValue);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        channel = tag.getString("channel");
        running = tag.getBoolean("running");
        doesRepeat = tag.getBoolean("doesRepeat");
        for (int i = 0; i < history.length; i++) history[i] = tag.getString("history" + i);
        runningValue = tag.contains("runningValue") ? tag.getString("runningValue") : "";
    }
}
