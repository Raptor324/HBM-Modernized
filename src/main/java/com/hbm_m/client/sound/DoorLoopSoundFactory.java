package com.hbm_m.client.sound;

import com.hbm_m.block.entity.doors.DoorBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
/**
 * Клиентский цикл звука двери — вынесен из {@link DoorBlockEntity}, чтобы серверный класс BE
 * не содержал в bytecode ссылок на {@link AbstractTickableSoundInstance}.
 */
@OnlyIn(Dist.CLIENT)
public final class DoorLoopSoundFactory {

    private DoorLoopSoundFactory() {}

    public static Object create(DoorBlockEntity door, SoundEvent sound) {
        return new AbstractTickableSoundInstance(sound, SoundSource.BLOCKS, RandomSource.create()) {
            {
                this.volume = door.getDoorDecl().getSoundVolume();
                this.pitch = 1.0f;
                this.looping = true;
                updatePosition();
            }

            /** Sable ships move - re-anchor in world space every tick (raw plot-grid coords mute the loop). */
            private void updatePosition() {
                net.minecraft.world.phys.Vec3 world = com.hbm_m.sound.SableSoundPosition.project(
                        door.getBlockPos(), 0.5D);
                this.x = world.x;
                this.y = world.y;
                this.z = world.z;
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
                updatePosition();
            }
        };
    }
}
