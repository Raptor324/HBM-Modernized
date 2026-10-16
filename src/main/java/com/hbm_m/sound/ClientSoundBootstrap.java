package com.hbm_m.sound;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.hbm_m.block.entity.doors.DoorBlockEntity;
import com.hbm_m.blockentity.machines.MachineChungusBlockEntity;
import com.hbm_m.blockentity.machines.MachineIndustrialTurbineBlockEntity;
import com.hbm_m.blockentity.machines.MachineTurbofanBlockEntity;

/**
 * Делегирование к клиентскому звуковому мосту ({@link IClientSoundAccess}).
 * Реализация ставится один раз из клиентского сетапа ({@link #install});
 * dedicated server её не загружает — все методы молча выходят по isClientSide.
 * <p>
 * Раньше каждый вызов каждой машины делал рефлексивный
 * {@code Class.forName + getMethod + invoke} (не кэшированный) — теперь прямой
 * invokeinterface через установленный мост.
 */
public final class ClientSoundBootstrap {

    @Nullable
    private static volatile IClientSoundAccess access;

    private ClientSoundBootstrap() {}

    /** Вызывается один раз из клиентского сетапа (ClientSetup). */
    public static void install(IClientSoundAccess bridge) {
        access = bridge;
    }

    @Nullable
    public static IClientSoundAccess peek() {
        return access;
    }

    private static boolean isClientSide(Level level) {
        return level != null && level.isClientSide();
    }

    public static void playOneShotSound(Level level, BlockPos pos, SoundEvent sound, float volume) {
        if (!isClientSide(level) || sound == null || access == null) {
            return;
        }
        access.playOneShotSound(pos, sound, volume);
    }

    public static void stopSound(Level level, BlockPos pos) {
        if (!isClientSide(level) || access == null) {
            return;
        }
        access.stopSound(pos);
    }

    public static void stopSpecificSound(Level level, BlockPos pos, String soundType) {
        if (!isClientSide(level) || access == null) {
            return;
        }
        access.stopSpecificSound(pos, soundType);
    }

    public static void updateDoorSoundRaw(Level level, BlockPos pos, String soundType, boolean isMoving, Supplier<?> loopSoundSupplier) {
        if (!isClientSide(level) || access == null) {
            return;
        }
        access.updateDoorSoundRaw(pos, soundType, isMoving, loopSoundSupplier);
    }

    /**
     * Speed-driven loop ({@code AudioWrapper} in the original): pitch and volume follow
     * {@code speed} (0..1) every tick, {@code <= 0} ends it.
     */
    public static void updateMachineLoop(BlockEntity be, boolean shouldBePlaying, SoundEvent sound, double yOffset,
                                         double maxDistance, java.util.function.ToDoubleFunction<BlockEntity> speed) {
        if (be == null || be.getLevel() == null || !be.getLevel().isClientSide() || access == null) {
            return;
        }
        // Out of range the instance would stop itself on its first tick and be recreated here
        // on the next one; the original does not even start it then.
        boolean playing = shouldBePlaying && access.machineLoopInRange(be, yOffset, maxDistance);
        updateSound(be, playing, () -> access.createMachineLoop(be, sound, yOffset, maxDistance, speed));
    }

    public static void updateSound(BlockEntity be, boolean shouldBePlaying, Supplier<?> soundSupplier) {
        updateSound(be, shouldBePlaying, soundSupplier, IClientSoundAccess.DEFAULT_AUDIBILITY_RANGE);
    }

    /** Distance-gated variant: machines with a custom hearing radius pass their own range. */
    public static void updateSound(BlockEntity be, boolean shouldBePlaying, Supplier<?> soundSupplier, double maxDistance) {
        if (be == null || be.getLevel() == null || !be.getLevel().isClientSide() || access == null) {
            return;
        }
        access.updateSound(be, shouldBePlaying, soundSupplier, maxDistance);
    }

    // ── Loop-фабрики станков (возвращают opaque инстанс звука для Supplier) ──

    public static Object createDoorLoop(DoorBlockEntity be, SoundEvent sound) {
        IClientSoundAccess a = access;
        return a == null ? null : a.createDoorLoop(be, sound);
    }

    public static Object createChungusLoop(MachineChungusBlockEntity be, SoundEvent sound) {
        IClientSoundAccess a = access;
        return a == null ? null : a.createChungusLoop(be, sound);
    }

    public static Object createTurbineLoop(MachineIndustrialTurbineBlockEntity be, SoundEvent sound) {
        IClientSoundAccess a = access;
        return a == null ? null : a.createTurbineLoop(be, sound);
    }

    public static Object createTurbofanLoop(MachineTurbofanBlockEntity be) {
        IClientSoundAccess a = access;
        return a == null ? null : a.createTurbofanLoop(be);
    }
}
