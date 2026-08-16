package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.radio.RTTYNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Arrays;

/**
 * 1:1 port of {@code TileEntityRBMKKeyPad}: 4 buttons, each broadcasting its command to its RTTY
 * channel. {@code polling[i]=true} re-broadcasts every tick the button is held; {@code false}
 * sends once per click - matches the original's "hold to repeat" vs "single press" modes.
 */
public class RBMKKeyPadBlockEntity extends RBMKPanelDeviceBlockEntity {

    public static final int UNITS = 4;

    public final String[]  channel = new String[UNITS];
    public final String[]  command = new String[UNITS];
    public final boolean[] polling = new boolean[UNITS];

    /**
     * Which unit (if any) is currently repeating, for polling mode. -1 = none.
     * <p>
     * SCOPE-Vereinfachung: the original repeats a polling button's broadcast every tick it's
     * physically held down by the player. A block right-click in this port is a single discrete
     * event with no "held" signal, so polling mode here is click-to-start / click-again-to-stop
     * instead - functionally equivalent for automation use (a continuous signal on the channel)
     * without needing a dedicated continuous-hold packet just for this one button type.
     */
    private int heldUnit = -1;

    public RBMKKeyPadBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_KEYPAD_BE.get(), pos, state);
        Arrays.fill(channel, "");
        Arrays.fill(command, "1");
    }

    @Override
    protected void onPanelTick(Level level, BlockPos pos) {
        if (heldUnit >= 0) broadcast(level, heldUnit);
    }

    /** Quadrant hit-test matching the original's 2x2 grid layout. */
    public static int unitFromHit(BlockHitResult hit) {
        double x = hit.getLocation().x - Math.floor(hit.getLocation().x);
        double y = hit.getLocation().y - Math.floor(hit.getLocation().y);
        int col = x >= 0.5 ? 1 : 0;
        int row = y >= 0.5 ? 0 : 1;
        return row * 2 + col;
    }

    public void click(Level level, BlockPos pos, Player player, int unit) {
        if (unit < 0 || unit >= UNITS) return;
        level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5f, 1.2f);

        if (polling[unit]) {
            heldUnit = (heldUnit == unit) ? -1 : unit;
            if (heldUnit == unit) broadcast(level, unit);
        } else {
            broadcast(level, unit);
        }
    }

    private void broadcast(Level level, int unit) {
        String ch = channel[unit];
        String cmd = command[unit];
        if (ch != null && !ch.isEmpty() && cmd != null && !cmd.isEmpty()) {
            RTTYNetwork.broadcast(level, ch, cmd);
        }
    }

    @Override
    public void receiveControl(CompoundTag data) {
        for (int i = 0; i < UNITS; i++) {
            if (data.contains("channel" + i)) channel[i] = data.getString("channel" + i);
            if (data.contains("command" + i)) command[i] = data.getString("command" + i);
            if (data.contains("polling" + i)) polling[i] = data.getBoolean("polling" + i);
        }
        setChanged();
        syncToClient();
    }

    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (int i = 0; i < UNITS; i++) {
            tag.putString("channel" + i, channel[i]);
            tag.putString("command" + i, command[i]);
            tag.putBoolean("polling" + i, polling[i]);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < UNITS; i++) {
            channel[i] = tag.contains("channel" + i) ? tag.getString("channel" + i) : "";
            command[i] = tag.contains("command" + i) ? tag.getString("command" + i) : "1";
            polling[i] = tag.getBoolean("polling" + i);
        }
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < UNITS; i++) {
            tag.putString("channel" + i, channel[i]);
            tag.putString("command" + i, command[i]);
            tag.putBoolean("polling" + i, polling[i]);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < UNITS; i++) {
            channel[i] = tag.contains("channel" + i) ? tag.getString("channel" + i) : "";
            command[i] = tag.contains("command" + i) ? tag.getString("command" + i) : "1";
            polling[i] = tag.getBoolean("polling" + i);
        }
    }
    *///?}
}
