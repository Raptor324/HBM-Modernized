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

    public RBMKTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_TERMINAL_BE.get(), pos, state);
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
                if (arg.isEmpty()) yield "chan: " + channel;
                channel = arg;
                yield "channel set to " + channel;
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
                yield "stopped";
            }
            default -> "unknown command: " + cmd;
        };

        setChanged();
        syncToClient();
        return result;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("cmd") && level != null) {
            eval(level, data.getString("cmd"));
            return; // eval() already calls setChanged()/syncToClient()
        }
        if (data.contains("channel")) channel = data.getString("channel");
        setChanged();
        syncToClient();
    }

    
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putString("channel", channel);
        tag.putBoolean("running", running);
        tag.putString("runningValue", runningValue);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        channel = tag.getString("channel");
        running = tag.getBoolean("running");
        runningValue = tag.contains("runningValue") ? tag.getString("runningValue") : "0";
    }
}
