package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.radio.RTTYNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

/**
 * 1:1 port of {@code TileEntityRBMKLever}: 2 flip-switches, each broadcasting a fixed
 * on/off command string to its RTTY channel when toggled.
 */
public class RBMKLeverBlockEntity extends RBMKPanelDeviceBlockEntity {

    public static final int UNITS = 2;
    private static final double FLIP_SPEED = 0.2;

    public final String[]  channel    = new String[UNITS];
    public final String[]  commandOn  = new String[UNITS];
    public final String[]  commandOff = new String[UNITS];
    public final boolean[] state      = new boolean[UNITS];
    public final double[]  flip       = new double[UNITS];

    public RBMKLeverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_LEVER_BE.get(), pos, state);
        Arrays.fill(channel, "");
        Arrays.fill(commandOn, "1");
        Arrays.fill(commandOff, "0");
    }

    @Override
    protected void onPanelTick(Level level, BlockPos pos) {
        for (int i = 0; i < UNITS; i++) {
            double target = state[i] ? 1.0 : 0.0;
            if (flip[i] < target) flip[i] = Math.min(target, flip[i] + FLIP_SPEED);
            else if (flip[i] > target) flip[i] = Math.max(target, flip[i] - FLIP_SPEED);
        }
    }

    /** Hit-quadrant → unit index: top half = unit 0, bottom half = unit 1 (2 stacked levers). */
    /**
     * 1:1 with the original's {@code RBMKLever.onBlockActivated}: the original splits its 2
     * levers left/right across whichever face was clicked (using the block's stored facing
     * metadata to know which world axis runs "across" that face), never top/bottom - our
     * previous top/bottom split was wrong. This port has no stored facing metadata at all, so
     * instead of replicating the original's per-facing branch table, the split is derived
     * directly from the actually-clicked face ({@link BlockHitResult#getDirection()}) and the
     * hit position projected onto that face's own clockwise tangent - equivalent in spirit, and
     * correct for whichever side is actually clicked rather than assuming one fixed orientation.
     */
    public static int unitFromHit(BlockPos pos, BlockHitResult hit) {
        Direction face = hit.getDirection();
        if (face.getAxis() == Direction.Axis.Y) return 0; // top/bottom clicks don't split in the original either

        Direction tangent = face.getClockWise();
        Vec3 local = hit.getLocation().subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        double across = local.x * tangent.getStepX() + local.z * tangent.getStepZ();
        return across >= 0 ? 0 : 1;
    }

    public void flipLever(Level level, BlockPos pos, Player player, int unit) {
        if (unit < 0 || unit >= UNITS) return;
        state[unit] = !state[unit];
        setChanged();
        syncToClient();
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5f, state[unit] ? 0.7f : 0.6f);

        String ch = channel[unit];
        if (ch != null && !ch.isEmpty()) {
            String cmd = state[unit] ? commandOn[unit] : commandOff[unit];
            if (cmd != null && !cmd.isEmpty()) RTTYNetwork.broadcast(level, ch, cmd);
        }
    }

    @Override
    public void receiveControl(CompoundTag data) {
        for (int i = 0; i < UNITS; i++) {
            if (data.contains("channel" + i))    channel[i]    = data.getString("channel" + i);
            if (data.contains("commandOn" + i))  commandOn[i]  = data.getString("commandOn" + i);
            if (data.contains("commandOff" + i)) commandOff[i] = data.getString("commandOff" + i);
        }
        setChanged();
        syncToClient();
    }

    
    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        for (int i = 0; i < UNITS; i++) {
        tag.putString("channel" + i, channel[i]);
        tag.putString("commandOn" + i, commandOn[i]);
        tag.putString("commandOff" + i, commandOff[i]);
        tag.putBoolean("state" + i, state[i]);
        }
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        for (int i = 0; i < UNITS; i++) {
        channel[i]    = tag.contains("channel" + i) ? tag.getString("channel" + i) : "";
        commandOn[i]  = tag.contains("commandOn" + i) ? tag.getString("commandOn" + i) : "1";
        commandOff[i] = tag.contains("commandOff" + i) ? tag.getString("commandOff" + i) : "0";
        state[i]      = tag.getBoolean("state" + i);
        flip[i]       = state[i] ? 1.0 : 0.0;
        }
    }
}
