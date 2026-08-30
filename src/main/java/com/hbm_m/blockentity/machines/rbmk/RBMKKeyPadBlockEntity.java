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

    /** KeyUnit.isPressed - latched for toggle buttons, momentary while polling. */
    public final boolean[] isPressed = new boolean[UNITS];

    /** KeyUnit's constructor defaults (TileEntityRBMKKeyPad:99-103). */
    @Override
    protected int defaultUnitColor(int index) {
        return switch (index) {
            case 0 -> 0xFF0000;
            case 1 -> 0xFFFF00;
            case 2 -> 0x0080FF;
            case 3 -> 0x00FF00;
            default -> 0x00FF00;
        };
    }

    /**
     * Countdown that keeps a non-polling button visually depressed after a click
     * ({@code KeyUnit.clickTimer}, 7 ticks in the original).
     */
    private final int[] clickTimer = new int[UNITS];

    public RBMKKeyPadBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_KEYPAD_BE.get(), pos, state);
        Arrays.fill(channel, "");
        Arrays.fill(command, "1");
    }

    @Override
    protected void onPanelTick(Level level, BlockPos pos) {
        // KeyUnit.update(): a latched polling button re-broadcasts every tick; a one-shot button
        // just counts its press animation down.
        for (int i = 0; i < UNITS; i++) {
            if (!isUnitActive(i)) continue;

            if (polling[i] && isPressed[i]) {
                broadcast(level, i);
            } else if (!polling[i] && isPressed[i] && --clickTimer[i] <= 0) {
                isPressed[i] = false;
                syncToClient();
            }
        }
    }

    /** Quadrant hit-test matching the original's 2x2 grid layout. */
    public static int unitFromHit(BlockHitResult hit) {
        double x = hit.getLocation().x - Math.floor(hit.getLocation().x);
        double y = hit.getLocation().y - Math.floor(hit.getLocation().y);
        int col = x >= 0.5 ? 1 : 0;
        int row = y >= 0.5 ? 0 : 1;
        return row * 2 + col;
    }

    /**
     * 1:1 with {@code KeyUnit.click()}: a plain button fires once and stays lit for
     * {@code clickTimer} ticks, a polling button latches on and off. The click sound's pitch
     * follows the resulting pressed state, exactly as the original does.
     */
    public void click(Level level, BlockPos pos, Player player, int unit) {
        if (unit < 0 || unit >= UNITS) return;
        if (!isUnitActive(unit)) return;

        if (!polling[unit]) {
            broadcast(level, unit);
            isPressed[unit] = true;
            clickTimer[unit] = 7;
        } else {
            isPressed[unit] = !isPressed[unit];
            setChanged();
        }

        level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS,
                1f, isPressed[unit] ? 1f : 0.75f);
        syncToClient();
    }

    private void broadcast(Level level, int unit) {
        String ch = channel[unit];
        String cmd = command[unit];
        if (ch != null && !ch.isEmpty() && cmd != null && !cmd.isEmpty()) {
            RTTYNetwork.broadcast(level, ch, cmd);
        }
    }

    /** Original per-unit array size (see the matching *Unit inner class). */
    @Override public int unitCount() { return UNITS; }

    @Override
    public void receiveControl(CompoundTag data) {
        receiveSharedControl(data);
        for (int i = 0; i < UNITS; i++) {
            if (data.contains("channel" + i)) channel[i] = data.getString("channel" + i);
            if (data.contains("command" + i)) command[i] = data.getString("command" + i);
            if (data.contains("polling" + i)) polling[i] = data.getBoolean("polling" + i);
        }
        setChanged();
        syncToClient();
    }

    
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        for (int i = 0; i < UNITS; i++) {
        tag.putString("channel" + i, channel[i]);
        tag.putString("command" + i, command[i]);
        tag.putBoolean("polling" + i, polling[i]);
        }
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        for (int i = 0; i < UNITS; i++) {
        channel[i] = tag.contains("channel" + i) ? tag.getString("channel" + i) : "";
        command[i] = tag.contains("command" + i) ? tag.getString("command" + i) : "1";
        polling[i] = tag.getBoolean("polling" + i);
        }
    }
}
