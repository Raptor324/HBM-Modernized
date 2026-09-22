package com.hbm_m.client.sound;

import java.util.function.ToDoubleFunction;

import com.hbm_m.blockentity.LoadedMachineBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Generic port of {@code AudioWrapper} loops driven by a machine speed: the original updates
 * pitch and volume to {@code speed} every tick and drops the loop when the machine stops or the
 * player walks out of range. Reached through {@code ClientSoundBootstrap} by reflection so the
 * block entity classes carry no client types.
 */
public final class MachineLoopSoundFactory {

    private MachineLoopSoundFactory() {}

    /** Sable overrides the player's distance for blocks on ships; a plain AABB distance would not do. */
    public static boolean inRange(BlockEntity be, double yOffset, double maxDistance) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        BlockPos pos = be.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + yOffset, pos.getZ() + 0.5) < maxDistance * maxDistance;
    }

    /**
     * @param speed 0..1 from the live block entity; {@code <= 0} stops the loop. Pitch equals it,
     *              volume too (times the muffler factor of {@link LoadedMachineBlockEntity}).
     */
    public static Object create(BlockEntity be, SoundEvent sound, double yOffset, double maxDistance,
                                ToDoubleFunction<BlockEntity> speed) {
        BlockPos pos = be.getBlockPos();
        Class<?> type = be.getClass();
        return new AbstractTickableSoundInstance(sound, SoundSource.BLOCKS, RandomSource.create()) {
            {
                this.x = pos.getX() + 0.5;
                this.y = pos.getY() + yOffset;
                this.z = pos.getZ() + 0.5;
                this.looping = true;
                this.delay = 0;
                this.attenuation = Attenuation.LINEAR;
                apply(be, (float) speed.applyAsDouble(be));
            }

            private void apply(BlockEntity live, float value) {
                float clamped = Math.max(0F, Math.min(1F, value));
                this.pitch = clamped;
                this.volume = live instanceof LoadedMachineBlockEntity loaded ? loaded.getVolume(clamped) : clamped;
            }

            @Override
            public void tick() {
                Level level = Minecraft.getInstance().level;
                if (level == null) {
                    this.stop();
                    return;
                }
                BlockEntity live = level.getBlockEntity(pos);
                if (live == null || live.getClass() != type) {
                    this.stop();
                    return;
                }
                float value = (float) speed.applyAsDouble(live);
                if (value <= 0F) {
                    this.stop();
                    return;
                }
                if (!inRange(live, yOffset, maxDistance)) {
                    this.stop();
                    return;
                }
                apply(live, value);
            }
        };
    }
}
