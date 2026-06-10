package com.hbm_m.client.sound;

import com.hbm_m.block.entity.doors.DoorBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Клиентский цикл звука двери — вынесен из {@link DoorBlockEntity}, чтобы серверный класс BE
 * не содержал в bytecode ссылок на {@link AbstractTickableSoundInstance}.
 */
public final class DoorLoopSoundFactory {

    private DoorLoopSoundFactory() {}

    public static Object create(DoorBlockEntity door, SoundEvent sound) {
        return new AbstractTickableSoundInstance(sound, SoundSource.BLOCKS, RandomSource.create()) {
            {
                this.x = door.getBlockPos().getX() + 0.5;
                this.y = door.getBlockPos().getY() + 0.5;
                this.z = door.getBlockPos().getZ() + 0.5;
                this.volume = door.getDoorDecl().getSoundVolume();
                this.pitch = 1.0f;
                this.looping = true;
            }

            @Override
            public void tick() {
                Level level = Minecraft.getInstance().level;
                if (level == null) {
                    this.stop();
                    return;
                }

                BlockEntity be = level.getBlockEntity(door.getBlockPos());
                if (!(be instanceof DoorBlockEntity doorBE) || (doorBE.state != 2 && doorBE.state != 3)) {
                    this.stop();
                }
            }
        };
    }
}
