package com.hbm_m.blockentity.machines.rbmk;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.blockentity.network.radio.RTTYNetwork;
import com.hbm_m.sound.HbmSoundsNT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Arrays;

/**
 * 1:1 port of {@code TileEntityRBMKLever}: 2 flip-switches, each broadcasting a fixed
 * on/off command string to its RTTY channel when toggled.
 */
public class RBMKLeverBlockEntity extends RBMKPanelDeviceBlockEntity {

    public static final int UNITS = 2;
    /** LeverUnit.FLIP_SPEED = 1F/10F - a full flip takes 10 ticks (0.5 s), the timing the
     *  arrival broadcast and arc flash are tuned around. */
    private static final double FLIP_SPEED = 0.1;

    public final String[]  channel    = new String[UNITS];
    public final String[]  commandOn  = new String[UNITS];
    public final String[]  commandOff = new String[UNITS];
    public final boolean[] state      = new boolean[UNITS];
    public final double[]  flip       = new double[UNITS];
    /** LeverUnit.polling - while set, the lever re-sends its command every tick at either end. */
    public final boolean[] polling    = new boolean[UNITS];
    /** LeverUnit.prevFlipProgress - previous tick's flip, for render interpolation. */
    public final double[]  prevFlip   = new double[UNITS];

    public RBMKLeverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_LEVER_BE.get(), pos, state);
        Arrays.fill(channel, "");
        Arrays.fill(commandOn, "1");
        Arrays.fill(commandOff, "0");
    }

    @Override
    protected void onPanelTick(Level level, BlockPos pos) {
        for (int i = 0; i < UNITS; i++) {
            // The original snapshots the previous progress before the active check, so an
            // inactive lever still interpolates to a standstill instead of snapping.
            prevFlip[i] = flip[i];
            if (!isUnitActive(i)) continue;

            boolean arcFlash = false;

            // Polling mode re-sends at whichever end the lever is resting at (LeverUnit.update).
            if (polling[i]) {
                if (flip[i] >= 1.0) send(level, i, commandOn[i]);
                if (flip[i] <= 0.0) send(level, i, commandOff[i]);
            }

            // turning on... (1:1 with LeverUnit.update: the non-polling command is broadcast the
            // exact tick the flip animation ARRIVES at the end, not at click time; clicking
            // mid-flip merely reverses direction and sends nothing until arrival)
            if (state[i] && flip[i] < 1.0) {
                flip[i] = Math.min(1.0, flip[i] + FLIP_SPEED);
                if (flip[i] >= 1.0) {
                    if (!polling[i]) send(level, i, commandOn[i]);
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            HbmSoundsNT.get("block.leverstop"), SoundSource.BLOCKS, 0.5F, 1F);
                    arcFlash = true;
                }

            // turning off...
            } else if (!state[i] && flip[i] > 0.0) {
                if (prevFlip[i] >= 1.0) arcFlash = true;
                flip[i] = Math.max(0.0, flip[i] - FLIP_SPEED);
                if (flip[i] <= 0.0) {
                    if (!polling[i]) send(level, i, commandOff[i]);
                    level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            HbmSoundsNT.get("block.leverstop"), SoundSource.BLOCKS, 0.5F, 1F);
                }
            }

            if (arcFlash) {
                // LeverUnit.update: SPARK sound plus 2 "tau" particles (count=5, small) on the
                // panel face, offset perpendicular to the facing per lever index.
                level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        HbmSoundsNT.get("block.spark"), SoundSource.BLOCKS, 1F, 1F);
                Direction dir = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
                Direction rot = dir.getClockWise();

                for (int j = 0; j < 2; j++) {
                    double x = pos.getX() + 0.5 + dir.getStepX() * 0.4 - rot.getStepX() * (i - 0.5) * (j == 0 ? 0.375 : 0.625);
                    double y = pos.getY() + 0.4375 - 0.03125;
                    double z = pos.getZ() + 0.5 + dir.getStepZ() * 0.4 - rot.getStepZ() * (i - 0.5) * (j == 0 ? 0.375 : 0.625);

                    CompoundTag dPart = new CompoundTag();
                    dPart.putString("type", "tau");
                    dPart.putByte("count", (byte) 5);
                    dPart.putBoolean("small", true);
                    if (level instanceof net.minecraft.server.level.ServerLevel server)
                        com.hbm_m.particle.helper.IParticleCreator.sendPacket(server, x, y, z, 25, dPart);
                }
            }
        }
    }

    /** Broadcasts one command on a unit's channel, skipping blanks like the original's canSend. */
    private void send(Level level, int unit, String command) {
        String ch = channel[unit];
        if (ch != null && !ch.isEmpty() && command != null && !command.isEmpty())
            RTTYNetwork.broadcast(level, ch, command);
    }

    /**
     * 1:1 with the original's {@code RBMKLever.onBlockActivated}: the 2 levers split left/right
     * exclusively by the STORED facing meta (meta 2/NORTH &amp;&amp; hitX &lt; 0.5 -&gt; 1;
     * 3/SOUTH &amp;&amp; hitX &gt; 0.5 -&gt; 1; 4/WEST &amp;&amp; hitZ &gt; 0.5 -&gt; 1;
     * 5/EAST &amp;&amp; hitZ &lt; 0.5 -&gt; 1) - the same fixed table as the keypad, independent of
     * which face was actually clicked. Edge hits (fractional X/Z exactly 0/1) and the UP/DOWN
     * faces are excluded. Returns -1 when the hit must not click anything.
     */
    public static int unitFromHit(BlockPos pos, Direction facing, BlockHitResult hit) {
        Direction face = hit.getDirection();
        if (face == Direction.UP || face == Direction.DOWN) return -1;

        double hitX = hit.getLocation().x - pos.getX();
        double hitZ = hit.getLocation().z - pos.getZ();

        // Original guard: hitX != 0 && hitX != 1 && hitZ != 0 && hitZ != 1
        if (hitX == 0 || hitX == 1 || hitZ == 0 || hitZ == 1) return -1;

        int indexHit = 0;

        if (facing == Direction.NORTH && hitX < 0.5) indexHit = 1;
        if (facing == Direction.SOUTH && hitX > 0.5) indexHit = 1;
        if (facing == Direction.WEST  && hitZ > 0.5) indexHit = 1;
        if (facing == Direction.EAST  && hitZ < 0.5) indexHit = 1;

        return indexHit;
    }

    /**
     * 1:1 with {@code LeverUnit.click()}: toggling only flips the target state and plays
     * LEVER_START when clicked at a rest end - the command itself is broadcast by
     * {@link #onPanelTick} when the flip animation ARRIVES (about 0.5 s later, see the original's
     * update() at TileEntityRBMKLever:112-131). A mid-flip click merely reverses direction and
     * sends nothing.
     */
    public void flipLever(Level level, BlockPos pos, Player player, int unit) {
        if (unit < 0 || unit >= UNITS) return;
        if (!isUnitActive(unit)) return;

        if (flip[unit] <= 0.0 || flip[unit] >= 1.0) {
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    HbmSoundsNT.get("block.leverstart"), SoundSource.BLOCKS, 1F, 1F);
        }

        state[unit] = !state[unit];
        setChanged();
        syncToClient();
    }

    /** Original per-unit array size (see the matching *Unit inner class). */
    @Override public int unitCount() { return UNITS; }

    @Override
    public void receiveControl(CompoundTag data) {
        receiveSharedControl(data);
        for (int i = 0; i < UNITS; i++) {
            if (data.contains("channel" + i))    channel[i]    = data.getString("channel" + i);
            if (data.contains("commandOn" + i))  commandOn[i]  = data.getString("commandOn" + i);
            if (data.contains("commandOff" + i)) commandOff[i] = data.getString("commandOff" + i);
            if (data.contains("lpolling" + i))   polling[i]    = data.getBoolean("lpolling" + i);
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
            tag.putString("commandOn" + i, commandOn[i]);
            tag.putString("commandOff" + i, commandOff[i]);
            tag.putBoolean("lpolling" + i, polling[i]);
            // 1:1 with LeverUnit NBT: isTurningOn semantics (mid-flip survives reload) plus the
            // flipProgress the original serializes every tick - without it the in-world lever
            // snapped instantly instead of animating over 0.5 s (baseTick ships this tag to
            // clients every tick, and the renderer lerps prevFlip -> flip). prevFlip ships too:
            // the port's panel tick is server-only, so the client could never advance the
            // interpolation base itself (unlike the original's updateClient).
            tag.putBoolean("state" + i, state[i]);
            tag.putDouble("prevFlip" + i, prevFlip[i]);
            tag.putDouble("flip" + i, flip[i]);
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
            // Old saves had no "flip" key and were forced to the nearest end; anything written
            // by this version carries the exact mid-flip progress.
            flip[i]       = tag.contains("flip" + i) ? tag.getDouble("flip" + i) : (state[i] ? 1.0 : 0.0);
            prevFlip[i]   = tag.contains("prevFlip" + i) ? tag.getDouble("prevFlip" + i) : flip[i];
        }
    }
}
