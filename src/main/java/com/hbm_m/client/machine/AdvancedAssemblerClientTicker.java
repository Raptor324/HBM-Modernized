package com.hbm_m.client.machine;

import java.util.Arrays;

import com.hbm_m.blockentity.machines.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.interfaces.IClientTicker;
import com.hbm_m.sound.AdvancedAssemblerSoundInstance;
import com.hbm_m.sound.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
/**
 * Клиентский тикер advanced assembler: вынесен из BlockEntity, чтобы dedicated server
 * не загружал класс с полями клиентских звуков.
 */
@OnlyIn(Dist.CLIENT)
public class AdvancedAssemblerClientTicker implements IClientTicker {

    private final AssemblerArm[] arms = new AssemblerArm[2];
    private float ringAngle;
    private float prevRingAngle;
    private float ringTarget;
    private float ringSpeed;
    private int ringDelay;
    private boolean wasCraftingLastTick = false;
    private AdvancedAssemblerSoundInstance soundInstance;
    /** Bumped whenever any arm angle changed this tick - the O(1) epoch salt
     * consumed by {@code MachineAdvancedAssemblerRenderer.animationEpoch} (it
     * replaces hashing every arm angle per render frame - 3.8% of the frame on
     * the 5000-machine fleet, spark 2026-10-07). Streamed record words derive
     * only from the arm angles, so the checksum covers exactly the inputs the
     * render-side epoch must observe. */
    public long animVersion;
    private int lastAngleHash;

    private final MachineAdvancedAssemblerBlockEntity be;

    public AdvancedAssemblerClientTicker(MachineAdvancedAssemblerBlockEntity be) {
        this.be = be;
        for (int i = 0; i < arms.length; i++) {
            arms[i] = new AssemblerArm();
        }
    }

    public float getRingAngle() {
        return ringAngle;
    }

    public float getPrevRingAngle() {
        return prevRingAngle;
    }

    public float getRingAngle(float partialTick) {
        return Mth.lerp(partialTick, prevRingAngle, ringAngle);
    }

    public AssemblerArm[] getArms() {
        return arms;
    }

    @Override
    public void clientTick() {
        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();
        // World-space anchor: on a Sable ship the BE sits in far-away plot-grid
        // chunks of this level, so every distance gate and every sound anchor
        // must use the projected position (parity with ClientSoundManager.isAudible).
        net.minecraft.world.phys.Vec3 worldCenter = level != null
                ? com.hbm_m.compat.sable.SableCompat.blockCenterInWorld(level, pos)
                : new net.minecraft.world.phys.Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        // Distance gate: on assembler farms thousands of tickers each hit the
        // sound engine every tick (SoundEngine.play stalls the render thread on
        // its audio-pool future). Beyond ~24 blocks the sounds are inaudible
        // anyway - keep the animation, skip all OpenAL traffic.
        boolean inEarshot = isInEarshot(worldCenter);
        updateSound(be, inEarshot);

        this.prevRingAngle = this.ringAngle;
        boolean craftingNow = be.isClientCrafting();
        boolean armAiActive = isWithinDistance(worldCenter, ARM_AI_DISTANCE_SQ);
        if (craftingNow) {
            for (AssemblerArm arm : arms) {
                arm.updateInterp();
                if (armAiActive) {
                    arm.updateArm(level, worldCenter, level.random, inEarshot);
                }
            }
        } else {
            for (AssemblerArm arm : arms) {
                arm.updateInterp();
                if (armAiActive) {
                    arm.returnToNullPos();
                }
            }
        }

        if (craftingNow && !wasCraftingLastTick) {
            this.ringTarget = this.ringAngle + (level.random.nextFloat() * 2 - 1) * 135;
            this.ringSpeed = 10.0F + level.random.nextFloat() * 5.0F;
            this.ringDelay = 0;
            if (inEarshot) {
                level.playLocalSound(worldCenter.x, worldCenter.y, worldCenter.z,
                    ModSounds.ASSEMBLER_START.get(), SoundSource.BLOCKS, 0.5f, 1.0f, false);
            }
        }

        wasCraftingLastTick = craftingNow;

        if (craftingNow) {
            boolean advance = this.ringAngle != this.ringTarget;
            if (!advance && this.ringDelay > 0) {
                this.ringDelay--;
                if (this.ringDelay == 0) {
                    if (inEarshot) {
                        level.playLocalSound(worldCenter.x, worldCenter.y, worldCenter.z,
                            ModSounds.ASSEMBLER_START.get(), SoundSource.BLOCKS, 0.3f, 1.0f, false);
                    }
                    this.ringTarget = this.ringAngle + (level.random.nextFloat() * 2 - 1) * 135;
                    this.ringSpeed = 10.0F + level.random.nextFloat() * 5.0F;
                    advance = true;
                }
            }
            if (advance) {
                if (Math.abs(this.ringTarget - this.ringAngle) <= this.ringSpeed) {
                    this.ringAngle = this.ringTarget;
                } else {
                    this.ringAngle += Math.signum(this.ringTarget - this.ringAngle) * this.ringSpeed;
                }
                if (this.ringAngle == this.ringTarget) {
                    while (this.ringTarget >= 360f) {
                        this.ringTarget -= 360f;
                        this.ringAngle -= 360f;
                        this.prevRingAngle -= 360f;
                    }
                    while (this.ringTarget <= -360f) {
                        this.ringTarget += 360f;
                        this.ringAngle += 360f;
                        this.prevRingAngle += 360f;
                    }
                    this.ringDelay = 20 + level.random.nextInt(21);
                }
            }
        }
        // Not crafting: the ring freezes in place (no return-to-zero unwind).

        // Arm-angle change salt for the render epoch (see animVersion).
        // Bit-mixing hash, NOT an additive sum: two arms moving by CANCELLING
        // deltas kept the old checksum unchanged, the render epoch stayed
        // "stable" and the frozen walk asserted the arms mid-swing - and the
        // freeze then stopped the very changes the sum watched, locking the
        // machine in a frozen pose while the tracked ring extrapolated its
        // stale segment past the target (jerk + snap on every real epoch flip,
        // 2026-10-07). A bitwise mix cannot cancel: any angle bit flip changes
        // the hash deterministically.
        int hash = 0;
        for (AssemblerArm arm : arms) {
            if (arm != null) {
                for (float a : arm.angles) {
                    hash = hash * 31 + Float.floatToRawIntBits(a);
                }
            }
        }
        if (hash != lastAngleHash) {
            lastAngleHash = hash;
            animVersion++;
        }
    }

    private static boolean isInEarshot(net.minecraft.world.phys.Vec3 worldCenter) {
        return isWithinDistance(worldCenter, EARSHOT_DISTANCE_SQ);
    }

    private static boolean isWithinDistance(net.minecraft.world.phys.Vec3 worldCenter, double maxDistSq) {
        net.minecraft.client.player.LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        double dx = worldCenter.x - player.getX();
        double dy = worldCenter.y - player.getY();
        double dz = worldCenter.z - player.getZ();
        return dx * dx + dy * dy + dz * dz <= maxDistSq;
    }

    private static final double EARSHOT_DISTANCE_SQ = 24.0 * 24.0;
    /** Beyond this the arm AI (updateArm / returnToNullPos) is skipped - see the gate in clientTick. */
    private static final double ARM_AI_DISTANCE_SQ = 48.0 * 48.0;

    private void updateSound(MachineAdvancedAssemblerBlockEntity entity, boolean inEarshot) {
        boolean isCrafting = entity.isClientCrafting() && inEarshot;
        if (isCrafting && (this.soundInstance == null || this.soundInstance.isStopped())) {
            this.soundInstance = new AdvancedAssemblerSoundInstance(entity.getBlockPos());
            Minecraft.getInstance().getSoundManager().play(this.soundInstance);
        } else if (!isCrafting && this.soundInstance != null && !this.soundInstance.isStopped()) {
            Minecraft.getInstance().getSoundManager().stop(this.soundInstance);
            this.soundInstance = null;
        }
    }

    public void onRemoved() {
        if (this.soundInstance != null) {
            Minecraft.getInstance().getSoundManager().stop(this.soundInstance);
            this.soundInstance = null;
        }
    }

    public static class AssemblerArm {
        public float[] angles = new float[4];
        public float[] prevAngles = new float[4];
        private float[] targetAngles = new float[4];
        private float[] speed = new float[4];
        private ArmActionState state = ArmActionState.ASSUME_POSITION;
        private int actionDelay = 0;

        private enum ArmActionState {
            ASSUME_POSITION, EXTEND_STRIKER, RETRACT_STRIKER
        }

        public AssemblerArm() {
            resetSpeed();
        }

        public void updateInterp() {
            System.arraycopy(angles, 0, prevAngles, 0, angles.length);
        }

        public void returnToNullPos() {
            Arrays.fill(targetAngles, 0);
            speed[0] = speed[1] = speed[2] = 3;
            speed[3] = 0.25f;
            state = ArmActionState.RETRACT_STRIKER;
            move();
        }

        private void resetSpeed() {
            speed[0] = 15;
            speed[1] = 15;
            speed[2] = 15;
            speed[3] = 0.5f;
        }

        public void updateArm(Level level, net.minecraft.world.phys.Vec3 soundPos, RandomSource random, boolean inEarshot) {
            resetSpeed();
            if (actionDelay > 0) {
                actionDelay--;
                return;
            }
            switch (state) {
                case ASSUME_POSITION:
                    if (move()) {
                        actionDelay = 2;
                        state = ArmActionState.EXTEND_STRIKER;
                        targetAngles[3] = -0.75f;
                    }
                    break;
                case EXTEND_STRIKER:
                    if (move()) {
                        if (inEarshot) {
                            level.playLocalSound(soundPos.x, soundPos.y, soundPos.z,
                                ModSounds.ASSEMBLER_STRIKE_RANDOM.get(), SoundSource.BLOCKS, 0.5f, 1.0F, false);
                        }
                        state = ArmActionState.RETRACT_STRIKER;
                        targetAngles[3] = 0f;
                    }
                    break;
                case RETRACT_STRIKER:
                    if (move()) {
                        actionDelay = 2 + random.nextInt(5);
                        chooseNewArmPosition(random);
                        state = ArmActionState.ASSUME_POSITION;
                    }
                    break;
            }
        }

        private static final float[][] POSITIONS = {
            {45, -15, -5}, {15, 15, -15}, {25, 10, -15},
            {30, 0, -10}, {70, -10, -25}
        };

        public void chooseNewArmPosition(RandomSource random) {
            int chosen = random.nextInt(5);
            targetAngles[0] = POSITIONS[chosen][0];
            targetAngles[1] = POSITIONS[chosen][1];
            targetAngles[2] = POSITIONS[chosen][2];
        }

        private boolean move() {
            boolean allReached = true;
            for (int i = 0; i < angles.length; i++) {
                float current = angles[i];
                float target = targetAngles[i];
                if (current == target) {
                    continue;
                }
                allReached = false;
                float delta = target - current;
                float absDelta = Math.abs(delta);
                if (absDelta <= speed[i]) {
                    angles[i] = target;
                } else {
                    angles[i] += Math.signum(delta) * speed[i];
                }
            }
            return allReached;
        }
    }
}
