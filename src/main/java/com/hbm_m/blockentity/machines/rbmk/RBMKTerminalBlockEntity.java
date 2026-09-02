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
 * SCOPE-Vereinfachung: the original's OpenComputers "OC mode", Redstone-over-Radio
 * {@code IRORInteractive} hook, and the {@code selfdestruct} easter-egg command are not ported -
 * OC has no equivalent integration anywhere else in this mod, RoR devices aren't ported, and
 * selfdestruct is a pure joke command with no functional purpose.
 */
public class RBMKTerminalBlockEntity extends RBMKPanelDeviceBlockEntity {

    public String channel = "";
    public boolean running = false;
    public String runningValue = "0";

    /** TileEntityRBMKTerminal.history - the 17-line scrollback the renderer prints under the
     *  working line. Newest entry first, exactly like the original's shift-and-insert. */
    public final String[] history = new String[17];

    /** TileEntityRBMKTerminal.doesRepeat - drives the terminal text amber instead of green. */
    public boolean doesRepeat = false;

    /**
     * Shift everything down one slot and put the new message at the top.
     *
     * <p>The loop started at {@code length - 2}, so the last slot was never written and the oldest
     * line fell off the list a row early - the bottom line of the terminal stayed permanently
     * blank. CE starts at {@code length - 1}.</p>
     */
    public void pushHistory(String msg) {
        for (int i = history.length - 1; i > 0; i--) history[i] = history[i - 1];
        history[0] = msg == null ? "" : msg;
    }

    public RBMKTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_TERMINAL_BE.get(), pos, state);
        java.util.Arrays.fill(history, "");
    }

    @Override
    protected void onPanelTick(Level level, BlockPos pos) {
        if (running && !channel.isEmpty()) {
            RTTYNetwork.broadcast(level, channel, runningValue);
        }
    }

    /** Evaluates a single command line, returning a response string for the GUI's scrollback. */
    public String eval(Level level, String line) {
        if (line == null) return "";
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
                if (channel.isEmpty()) yield "no channel set";
                RTTYNetwork.broadcast(level, channel, arg.isEmpty() ? "1" : arg);
                yield "sent '" + arg + "' on " + channel;
            }
            case "start" -> {
                if (channel.isEmpty()) yield "no channel set";
                running = true;
                runningValue = arg.isEmpty() ? "1" : arg;
                yield "started continuous send: " + runningValue;
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
            case "selfdestruct" -> {
                level.destroyBlock(getBlockPos(), false);
                level.explode(null, getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5,
                        getBlockPos().getZ() + 0.5, 5.0F, Level.ExplosionInteraction.BLOCK);
                yield "";
            }
            default -> "Unrecognized command!";
        };

        // The original echoes both the command and its response into the panel's scrollback
        // (TileEntityRBMKTerminal:126-129); history[0] is the newest line.
        if (!"clear".equals(cmd)) {
            pushHistory(line.trim());
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

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("channel", channel);
        tag.putBoolean("running", running);
        tag.putBoolean("doesRepeat", doesRepeat);
        for (int i = 0; i < history.length; i++)
            tag.putString("history" + i, history[i] == null ? "" : history[i]);
        tag.putString("runningValue", runningValue);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        channel = tag.getString("channel");
        running = tag.getBoolean("running");
        doesRepeat = tag.getBoolean("doesRepeat");
        for (int i = 0; i < history.length; i++) history[i] = tag.getString("history" + i);
        runningValue = tag.contains("runningValue") ? tag.getString("runningValue") : "0";
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("channel", channel);
        tag.putBoolean("running", running);
        tag.putBoolean("doesRepeat", doesRepeat);
        for (int i = 0; i < history.length; i++)
            tag.putString("history" + i, history[i] == null ? "" : history[i]);
        tag.putString("runningValue", runningValue);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        channel = tag.getString("channel");
        running = tag.getBoolean("running");
        doesRepeat = tag.getBoolean("doesRepeat");
        for (int i = 0; i < history.length; i++) history[i] = tag.getString("history" + i);
        runningValue = tag.contains("runningValue") ? tag.getString("runningValue") : "0";
    }
    *///?}
}
