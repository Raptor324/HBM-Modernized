package com.hbm_m.sound;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.hbm_m.blockentity.machines.MachineChungusBlockEntity;
import com.hbm_m.blockentity.machines.MachineIndustrialTurbineBlockEntity;
import com.hbm_m.blockentity.machines.MachineTurbofanBlockEntity;
import com.hbm_m.block.entity.doors.DoorBlockEntity;

/**
 * Мост к клиентскому звуковому менеджеру и loop-фабрикам — единый контракт для
 * ВСЕХ машин: common-код (BlockEntity) вызывает его через
 * {@link ClientSoundBootstrap}, реализация ставится один раз из клиентского
 * сетапа. Заменяет рефлексию Class.forName/getMethod из common-кода (раньше —
 * не кэшированный getMethod на каждый вызов каждой машины).
 * <p>
 * Поставщики звука (Supplier/возвраты) — opaque Object: конкретные типы
 * (AbstractTickableSoundInstance) клиентские, common их не упоминает.
 */
public interface IClientSoundAccess {

    void playOneShotSound(BlockPos pos, SoundEvent sound, float volume);

    void stopSound(BlockPos pos);

    void stopSpecificSound(BlockPos pos, String soundType);

    void updateDoorSoundRaw(BlockPos pos, String soundType, boolean isMoving, Supplier<?> loopSoundSupplier);

    /**
     * Same as {@link #updateSound(BlockEntity, boolean, Supplier)} with an explicit
     * audibility range: while no player is within {@code maxDistance} of the sound
     * position the loop is not created / is stopped. Machines with a non-standard
     * hearing radius (turbofan afterburner) pass their own range.
     */
    void updateSound(BlockEntity be, boolean shouldBePlaying, Supplier<?> soundSupplier, double maxDistance);

    /** {@code soundSupplier} (nullable) создаёт инстанс звука; result opaque. */
    default void updateSound(BlockEntity be, boolean shouldBePlaying, Supplier<?> soundSupplier) {
        updateSound(be, shouldBePlaying, soundSupplier, DEFAULT_AUDIBILITY_RANGE);
    }

    double DEFAULT_AUDIBILITY_RANGE = 32.0D;

    /** Вне зоны слышимости цикл не создаётся (оригинальное поведение). */
    boolean machineLoopInRange(BlockEntity be, double yOffset, double maxDistance);

    Object createMachineLoop(BlockEntity be, SoundEvent sound, double yOffset, double maxDistance,
                             java.util.function.ToDoubleFunction<BlockEntity> speed);

    Object createDoorLoop(DoorBlockEntity be, SoundEvent sound);

    Object createChungusLoop(MachineChungusBlockEntity be, SoundEvent sound);

    Object createTurbineLoop(MachineIndustrialTurbineBlockEntity be, SoundEvent sound);

    Object createTurbofanLoop(MachineTurbofanBlockEntity be);
}
