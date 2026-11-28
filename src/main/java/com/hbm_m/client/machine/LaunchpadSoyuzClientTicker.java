package com.hbm_m.client.machine;

import com.hbm_m.blockentity.machines.LaunchpadSoyuzBlockEntity;
import com.hbm_m.interfaces.IClientTicker;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client ticker of the Soyuz launch pad: the three looped sounds of the original
 * {@code AudioWrapper[3]} ({@code handleSound}/{@code createSound}/{@code getSoundPosition}),
 * driven by the synced pad state. Lives next to the common BE so the dedicated
 * server never loads client sound classes.
 */
@OnlyIn(Dist.CLIENT)
public class LaunchpadSoyuzClientTicker implements IClientTicker {

    private final LaunchpadSoyuzBlockEntity be;
    private final PadLoopSound[] audios = new PadLoopSound[3];

    public LaunchpadSoyuzClientTicker(LaunchpadSoyuzBlockEntity be) {
        this.be = be;
    }

    @Override
    public void clientTick() {
        // Original handleSound conditions (0: LAUNCHING, 1: carriage moving, 2: rotor moving)
        handleSound(0, be.soyuzStatus == LaunchpadSoyuzBlockEntity.SoyuzStatus.LAUNCHING);
        boolean powered = be.getEnergyStored() >= LaunchpadSoyuzBlockEntity.CONSUMPTION;
        handleSound(1, powered
                && be.positions[LaunchpadSoyuzBlockEntity.INDEX_CARRIAGE] > 0
                && be.positions[LaunchpadSoyuzBlockEntity.INDEX_CARRIAGE] < 1);
        handleSound(2, powered
                && be.positions[LaunchpadSoyuzBlockEntity.INDEX_ROTOR] > 0
                && be.positions[LaunchpadSoyuzBlockEntity.INDEX_ROTOR] < 1);
    }

    /** Original {@code handleSound(index, isRunning)}. */
    private void handleSound(int index, boolean isRunning) {
        Minecraft mc = Minecraft.getInstance();

        if (isRunning) {
            if (audios[index] == null) {
                audios[index] = createSound(index);
                mc.getSoundManager().play(audios[index]);

            } else if (audios[index].isStopped() || !mc.getSoundManager().isActive(audios[index])) {
                // Original: stopSound + recreate + startSound
                audios[index] = createSound(index);
                mc.getSoundManager().play(audios[index]);
            }

        } else {
            if (audios[index] != null) {
                mc.getSoundManager().stop(audios[index]);
                audios[index] = null;

                // Original one-shots when the loop ends
                Vec3 pos = getSoundPosition(index);
                if (index == 1) {
                    playAt(HbmSoundsNT.get("hbm:door.garage_stop"), pos, 35F, 1.0F);
                }
                if (index == 2) {
                    playAt(HbmSoundsNT.get("hbm:door.wgh_big_stop"), pos, 35F, 0.75F);
                }
            }
        }
    }

    /** Original {@code createSound(index)}: soyuzReady/garage_move/wgh_big_start. */
    private PadLoopSound createSound(int index) {
        SoundEvent sound;
        float baseVolume;
        float range;
        float pitch;

        if (index == 0) {
            sound = HbmSoundsNT.get("hbm:block.soyuzReady");
            baseVolume = 2.0F;
            range = 100F;
            pitch = 1.0F;
        } else if (index == 1) {
            sound = HbmSoundsNT.get("hbm:door.garage_move");
            baseVolume = 2.0F;
            range = 35F;
            pitch = 1.0F;
        } else if (index == 2) {
            sound = HbmSoundsNT.get("hbm:door.wgh_big_start");
            baseVolume = 2.0F;
            range = 50F;
            pitch = 0.75F;
        } else {
            return null;
        }

        if (sound == null) return null;
        Vec3 pos = getSoundPosition(index);
        return new PadLoopSound(sound, SoundSource.BLOCKS, baseVolume, range, pitch,
                pos.x, pos.y, pos.z, be, index);
    }

    /** Original {@code getSoundPosition(index)}: Basis vor dem Tower, Loops wandern mit der Katze. */
    private Vec3 getSoundPosition(int index) {
        BlockPos p = be.getBlockPos();
        var facing = be.getFacing();
        var rot = facing.getClockWise();

        double x = p.getX() + 0.5 - facing.getStepX() * 4 - rot.getStepX() * 4;
        double y = p.getY() + 4;
        double z = p.getZ() + 0.5 - facing.getStepZ() * 4 - rot.getStepZ() * 4;

        if (index == 1) {
            double dist = 24 + 20.5 * (1 - be.positions[LaunchpadSoyuzBlockEntity.INDEX_CARRIAGE]);
            x -= rot.getStepX() * dist;
            z -= rot.getStepZ() * dist;
        }

        if (index == 2) {
            double dist = 18 + 20.5 * (1 - be.positions[LaunchpadSoyuzBlockEntity.INDEX_CARRIAGE]);
            x -= rot.getStepX() * dist;
            z -= rot.getStepZ() * dist;
        }

        return new Vec3(x, y, z);
    }

    private static void playAt(SoundEvent sound, Vec3 pos, float volume, float pitch) {
        Level level = Minecraft.getInstance().level;
        if (sound == null || level == null) return;
        level.playLocalSound(pos.x, pos.y, pos.z, sound, SoundSource.BLOCKS, volume, pitch, false);
    }

    @Override
    public void onRemoved() {
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < audios.length; i++) {
            if (audios[i] != null) {
                mc.getSoundManager().stop(audios[i]);
                audios[i] = null;
            }
        }
    }

    /** Positionaler Loop: Position/Volumen folgen dem Pad-Zustand (Original AudioWrapper). */
    private static final class PadLoopSound extends AbstractTickableSoundInstance {

        private final LaunchpadSoyuzBlockEntity be;
        private final int index;
        private final float baseVolume;
        private final float range;

        PadLoopSound(SoundEvent sound, SoundSource source, float baseVolume, float range, float pitch,
                     double x, double y, double z, LaunchpadSoyuzBlockEntity be, int index) {
            super(sound, source, RandomSource.create());
            this.be = be;
            this.index = index;
            this.baseVolume = baseVolume;
            this.range = range;
            this.x = x;
            this.y = y;
            this.z = z;
            this.volume = baseVolume;
            this.pitch = pitch;
            this.looping = true;
            this.delay = 0;
            this.attenuation = Attenuation.NONE;
        }

        @Override
        public void tick() {
            Level level = Minecraft.getInstance().level;
            if (level == null || be.isRemoved()) {
                this.stop();
                return;
            }

            // Original getSoundPosition: der Loop wandert mit der Katze
            Vec3 pos = tickerPosition();
            this.x = pos.x;
            this.y = pos.y;
            this.z = pos.z;

            var player = Minecraft.getInstance().player;
            if (player == null) {
                this.stop();
                return;
            }
            double d = Math.sqrt(player.distanceToSqr(this.x, this.y, this.z));
            this.volume = (float) Math.max(0D, baseVolume * (1D - d / range));
        }

        private Vec3 tickerPosition() {
            BlockPos p = be.getBlockPos();
            var facing = be.getFacing();
            var rot = facing.getClockWise();

            double x = p.getX() + 0.5 - facing.getStepX() * 4 - rot.getStepX() * 4;
            double y = p.getY() + 4;
            double z = p.getZ() + 0.5 - facing.getStepZ() * 4 - rot.getStepZ() * 4;

            if (index == 1) {
                double dist = 24 + 20.5 * (1 - be.positions[LaunchpadSoyuzBlockEntity.INDEX_CARRIAGE]);
                x -= rot.getStepX() * dist;
                z -= rot.getStepZ() * dist;
            }
            if (index == 2) {
                double dist = 18 + 20.5 * (1 - be.positions[LaunchpadSoyuzBlockEntity.INDEX_CARRIAGE]);
                x -= rot.getStepX() * dist;
                z -= rot.getStepZ() * dist;
            }
            return new Vec3(x, y, z);
        }
    }
}
