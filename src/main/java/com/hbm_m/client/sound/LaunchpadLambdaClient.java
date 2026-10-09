package com.hbm_m.client.sound;

import com.hbm_m.blockentity.machines.LaunchpadLambdaBlockEntity;
import com.hbm_m.client.weapon.OrchestrasClient;
import com.hbm_m.client.weapon.OrchestrasClient.LoopSound;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Clientteil von {@code TileEntityLaunchpadLambda#updateEntity}: Dampf aus dem Silo, solange die aufgerichtete Rakete
 * betankt ist, und die Schleifentoene von Silotueren, Aufrichter, Rotor und Countdown-Sirene samt Stopp-Geraeuschen.
 */
public final class LaunchpadLambdaClient {

    private LaunchpadLambdaClient() {}

    public static void tick(LaunchpadLambdaBlockEntity pad) {
        Level world = pad.getLevel();
        Minecraft mc = Minecraft.getInstance();
        if (world == null || mc.player == null) return;
        BlockPos pos = pad.getBlockPos();

        if (pad.erected && pad.hasOxidizer() && mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5) <= 100 * 100) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "tower");
            data.putFloat("lift", 0F);
            data.putFloat("base", 0.5F);
            data.putFloat("max", 2F);
            data.putInt("life", 70 + world.random.nextInt(30));
            data.putDouble("posX", pos.getX() + 0.5 + world.random.nextGaussian() * 0.25);
            data.putDouble("posZ", pos.getZ() + 0.5 + world.random.nextGaussian() * 0.25);
            data.putDouble("posY", pos.getY() + 2);
            data.putBoolean("noWind", true);
            data.putFloat("alpha", 2F);
            data.putFloat("strafe", 0.075F);
            for (int i = 0; i < 3; i++) com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
        }

        float[] sync = pad.syncPositions;
        handleSound(pad, 0, sync[LaunchpadLambdaBlockEntity.INDEX_DOORS] > 0F && sync[LaunchpadLambdaBlockEntity.INDEX_DOORS] < 3F);
        handleSound(pad, 1, !pad.finishedMoving(LaunchpadLambdaBlockEntity.INDEX_ERECTOR) && pad.wasMoving(LaunchpadLambdaBlockEntity.INDEX_ERECTOR));
        handleSound(pad, 2, sync[LaunchpadLambdaBlockEntity.INDEX_ROTOR] > 0F && sync[LaunchpadLambdaBlockEntity.INDEX_ROTOR] < 180F);
        handleSound(pad, 3, pad.countdown > 0);

        if (pad.wasMoving(LaunchpadLambdaBlockEntity.INDEX_CLAMPS) && sync[LaunchpadLambdaBlockEntity.INDEX_CLAMPS] == 90F)
            play(world, pos.getX() + 0.5, pos.getY() + 12, pos.getZ() + 0.5, "hbm:door.sliding_seal_stop", 25F, 0.75F);
    }

    private static void handleSound(LaunchpadLambdaBlockEntity pad, int index, boolean isRunning) {
        Vec3 pos = soundPos(pad, index);

        if (isRunning) {
            LoopSound audio = (LoopSound) pad.audios[index];
            if (audio == null) {
                audio = createSound(index, pos);
                pad.audios[index] = audio;
                audio.startSound();
            } else if (!audio.isPlaying()) {
                audio.stopSound();
                audio = createSound(index, pos);
                pad.audios[index] = audio;
                audio.startSound();
            }
            audio.keepAlive();
            audio.updatePosition((float) pos.x, (float) pos.y, (float) pos.z);

        } else if (pad.audios[index] != null) {
            ((LoopSound) pad.audios[index]).stopSound();
            pad.audios[index] = null;

            int e = LaunchpadLambdaBlockEntity.INDEX_ERECTOR;
            if (Math.abs(pad.positions[e] - pad.prevPositions[e]) > 5) return;

            Level world = pad.getLevel();
            if (index == 0) play(world, pos.x, pos.y, pos.z, "hbm:door.garage_stop", 35F, 1F);
            if (index == 1 && pad.target[e] == 25F) play(world, pos.x, pos.y, pos.z, "hbm:door.wgh_big_stop", 25F, 0.5F);
            if (index == 1 && pad.target[e] != 25F) play(world, pos.x, pos.y, pos.z, "hbm:door.wgh_stop", 25F, 1F);
            if (index == 2) play(world, pos.x, pos.y, pos.z, "hbm:door.wgh_big_stop", 25F, 0.75F);
        }
    }

    private static LoopSound createSound(int index, Vec3 pos) {
        float x = (float) pos.x, y = (float) pos.y, z = (float) pos.z;
        return switch (index) {
            case 0 -> OrchestrasClient.getLoopedSound("hbm:door.garage_move", x, y, z, 2.0F, 35F, 1.0F, 10);
            case 1 -> OrchestrasClient.getLoopedSound("hbm:door.wgh_start", x, y, z, 2.0F, 35F, 1.0F, 10);
            case 2 -> OrchestrasClient.getLoopedSound("hbm:door.wgh_big_start", x, y, z, 2.0F, 35F, 0.75F, 10);
            default -> OrchestrasClient.getLoopedSound("hbm:alarm.regularSiren", x, y, z, 10F, 50F, 1F, 20);
        };
    }

    private static Vec3 soundPos(LaunchpadLambdaBlockEntity pad, int index) {
        BlockPos p = pad.getBlockPos();
        return new Vec3(p.getX() + 0.5, p.getY() + 3 + (index == 2 ? 12 : 0), p.getZ() + 0.5);
    }

    /** Original {@code playSoundClient(x, y, z, sound, volume, pitch)}; die Lautstaerke steht dort fuer die Reichweite. */
    private static void play(Level world, double x, double y, double z, String sound, float volume, float pitch) {
        world.playLocalSound(x, y, z, HbmSoundsNT.get(sound), SoundSource.BLOCKS, volume, pitch, false);
    }
}
