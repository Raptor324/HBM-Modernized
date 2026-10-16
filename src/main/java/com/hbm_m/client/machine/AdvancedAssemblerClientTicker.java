package com.hbm_m.client.machine;

import java.util.Arrays;

import com.hbm_m.blockentity.machines.MachineAdvancedAssemblerBlockEntity;
import com.hbm_m.interfaces.IClientTicker;
import com.hbm_m.sound.AdvancedAssemblerSoundInstance;
import com.hbm_m.sound.ModSounds;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

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

    public AssemblerArm[] getArms() {
        return arms;
    }

    @Override
    public void clientTick() {
        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();
        // Distance gate: on assembler farms thousands of tickers each hit the
        // sound engine every tick (SoundEngine.play stalls the render thread on
        // its audio-pool future). Beyond ~24 blocks the sounds are inaudible
        // anyway - keep the animation, skip all OpenAL traffic.
        boolean inEarshot = isInEarshot(pos);
        updateSound(be, inEarshot);

        this.prevRingAngle = this.ringAngle;
        boolean craftingNow = be.isClientCrafting();

        if (craftingNow) {
            for (AssemblerArm arm : arms) {
                arm.updateInterp();
                arm.updateArm(level, pos, level.random, inEarshot);
            }
        } else {
            for (AssemblerArm arm : arms) {
                arm.updateInterp();
                arm.returnToNullPos();
            }
        }

        if (craftingNow && !wasCraftingLastTick) {
            this.ringTarget += (level.random.nextFloat() * 2 - 1) * 135;
            this.ringSpeed = 10.0F + level.random.nextFloat() * 5.0F;
            this.ringDelay = 0;
            if (inEarshot) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    ModSounds.ASSEMBLER_START.get(), SoundSource.BLOCKS, 0.5f, 1.0f, false);
            }
        }

        wasCraftingLastTick = craftingNow;

        if (craftingNow) {
            if (this.ringAngle != this.ringTarget) {
                // Linear travel toward the (accumulating) target - NO wrapDegrees:
                // wrapping here made the ring take the "short arc" against its travel
                // direction, wander past +-180 and then SNAP to the target when the
                // wrapped delta crossed zero (the visible 360-degree backswing).
                if (Math.abs(this.ringTarget - this.ringAngle) <= this.ringSpeed) {
                    this.ringAngle = this.ringTarget;
                } else {
                    this.ringAngle += Math.signum(this.ringTarget - this.ringAngle) * this.ringSpeed;
                }
                if (this.ringAngle == this.ringTarget) {
                    // Normalize the whole trajectory (target + current + previous) so
                    // the render lerp stays continuous across the +-360 boundary.
                    if (this.ringTarget >= 360f) {
                        this.ringTarget -= 360f;
                        this.ringAngle -= 360f;
                        this.prevRingAngle -= 360f;
                    }
                    if (this.ringTarget <= -360f) {
                        this.ringTarget += 360f;
                        this.ringAngle += 360f;
                        this.prevRingAngle += 360f;
                    }
                    this.ringDelay = 20 + level.random.nextInt(21);
                }
            } else if (this.ringDelay > 0) {
                this.ringDelay--;
                if (this.ringDelay == 0) {
                    if (inEarshot) {
                        level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            ModSounds.ASSEMBLER_START.get(), SoundSource.BLOCKS, 0.3f, 1.0f, false);
                    }
                    this.ringTarget += (level.random.nextFloat() * 2 - 1) * 135;
                    this.ringSpeed = 10.0F + level.random.nextFloat() * 5.0F;
                }
            }
        }
        // Not crafting: the ring freezes in place (no return-to-zero unwind).
    }

    private static boolean isInEarshot(BlockPos pos) {
        net.minecraft.client.player.LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        double dx = (pos.getX() + 0.5) - player.getX();
        double dy = (pos.getY() + 0.5) - player.getY();
        double dz = (pos.getZ() + 0.5) - player.getZ();
        return dx * dx + dy * dy + dz * dz <= EARSHOT_DISTANCE_SQ;
    }

    private static final double EARSHOT_DISTANCE_SQ = 24.0 * 24.0;

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

        public void updateArm(Level level, BlockPos pos, RandomSource random, boolean inEarshot) {
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
                            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
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
