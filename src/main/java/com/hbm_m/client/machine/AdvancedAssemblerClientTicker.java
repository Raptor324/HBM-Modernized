package com.hbm_m.client.machine;

import java.util.Arrays;

import com.hbm_m.block.entity.machines.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.sound.AdvancedAssemblerSoundInstance;
import com.hbm_m.sound.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Клиентский тикер advanced assembler: вынесен из BlockEntity, чтобы dedicated server
 * не загружал класс с полями клиентских звуков.
 */
public class AdvancedAssemblerClientTicker {

    private final AssemblerArm[] arms = new AssemblerArm[2];
    private float ringAngle;
    private float prevRingAngle;
    private float ringTarget;
    private float ringSpeed;
    private int ringDelay;
    private boolean wasCraftingLastTick = false;
    private AdvancedAssemblerSoundInstance soundInstance;

    public AdvancedAssemblerClientTicker() {
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

    public AssemblerArm[] getArms() {
        return arms;
    }

    public void clientTick(Level level, BlockPos pos, BlockState state, MachineAdvancedAssemblerBlockEntity entity) {
        updateSound(entity);

        this.prevRingAngle = this.ringAngle;
        boolean craftingNow = entity.isClientCrafting();

        if (craftingNow) {
            for (AssemblerArm arm : arms) {
                arm.updateInterp();
                arm.updateArm(level, pos, level.random);
            }
        } else {
            for (AssemblerArm arm : arms) {
                arm.updateInterp();
                arm.returnToNullPos();
            }
        }

        if (craftingNow && !wasCraftingLastTick) {
            this.ringTarget = (level.random.nextFloat() * 2 - 1) * 135;
            this.ringSpeed = 10.0F + level.random.nextFloat() * 5.0F;
            this.ringDelay = 0;
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                ModSounds.ASSEMBLER_START.get(), SoundSource.BLOCKS, 0.5f, 1.0f, false);
        }

        wasCraftingLastTick = craftingNow;

        if (craftingNow) {
            if (this.ringAngle != this.ringTarget) {
                float ringDelta = Mth.wrapDegrees(this.ringTarget - this.ringAngle);
                if (Math.abs(ringDelta) <= this.ringSpeed) {
                    this.ringAngle = this.ringTarget;
                    this.ringDelay = 20 + level.random.nextInt(21);
                } else {
                    this.ringAngle += Math.signum(ringDelta) * this.ringSpeed;
                }
            } else if (this.ringDelay > 0) {
                this.ringDelay--;
                if (this.ringDelay == 0) {
                    level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        ModSounds.ASSEMBLER_START.get(), SoundSource.BLOCKS, 0.3f, 1.0f, false);
                    this.ringTarget = (level.random.nextFloat() * 2 - 1) * 135;
                    this.ringSpeed = 10.0F + level.random.nextFloat() * 5.0F;
                }
            }
        } else {
            if (Math.abs(this.ringAngle) > 0.1f) {
                this.ringAngle = Mth.lerp(0.1f, this.ringAngle, 0);
            } else {
                this.ringAngle = 0;
            }
        }
    }

    private void updateSound(MachineAdvancedAssemblerBlockEntity entity) {
        boolean isCrafting = entity.isClientCrafting();
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

        public void updateArm(Level level, BlockPos pos, RandomSource random) {
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
                        level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            ModSounds.ASSEMBLER_STRIKE_RANDOM.get(), SoundSource.BLOCKS, 0.5f, 1.0F, false);
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
