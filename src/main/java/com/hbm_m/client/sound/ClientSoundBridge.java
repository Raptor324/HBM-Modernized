package com.hbm_m.client.sound;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.hbm_m.block.entity.doors.DoorBlockEntity;
import com.hbm_m.blockentity.machines.MachineChungusBlockEntity;
import com.hbm_m.blockentity.machines.MachineIndustrialTurbineBlockEntity;
import com.hbm_m.blockentity.machines.MachineTurbofanBlockEntity;
import com.hbm_m.sound.ClientSoundManager;
import com.hbm_m.sound.IClientSoundAccess;

/**
 * Клиентская реализация {@link IClientSoundAccess}: прямые вызовы
 * {@link ClientSoundManager} и loop-фабрик вместо рефлексии из common-кода.
 * Ставится один раз в ClientSetup ({@code ClientSoundBootstrap.install}).
 */
public final class ClientSoundBridge implements IClientSoundAccess {

    @Override
    public void playOneShotSound(BlockPos pos, SoundEvent sound, float volume) {
        ClientSoundManager.playOneShotSound(pos, sound, volume);
    }

    @Override
    public void stopSound(BlockPos pos) {
        ClientSoundManager.stopSound(pos);
    }

    @Override
    public void stopSpecificSound(BlockPos pos, String soundType) {
        ClientSoundManager.stopSpecificSound(pos, soundType);
    }

    @Override
    public void updateDoorSoundRaw(BlockPos pos, String soundType, boolean isMoving, Supplier<?> loopSoundSupplier) {
        ClientSoundManager.updateDoorSoundRaw(pos, soundType, isMoving, loopSoundSupplier);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updateSound(BlockEntity be, boolean shouldBePlaying, Supplier<?> soundSupplier) {
        updateSound(be, shouldBePlaying, soundSupplier, IClientSoundAccess.DEFAULT_AUDIBILITY_RANGE);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updateSound(BlockEntity be, boolean shouldBePlaying, Supplier<?> soundSupplier, double maxDistance) {
        Supplier<? extends net.minecraft.client.resources.sounds.AbstractTickableSoundInstance> typed =
                (Supplier<? extends net.minecraft.client.resources.sounds.AbstractTickableSoundInstance>) soundSupplier;
        ClientSoundManager.updateSound(be, shouldBePlaying, typed, maxDistance);
    }

    @Override
    public boolean machineLoopInRange(BlockEntity be, double yOffset, double maxDistance) {
        return MachineLoopSoundFactory.inRange(be, yOffset, maxDistance);
    }

    @Override
    public Object createMachineLoop(BlockEntity be, SoundEvent sound, double yOffset, double maxDistance,
                                    java.util.function.ToDoubleFunction<BlockEntity> speed) {
        return MachineLoopSoundFactory.create(be, sound, yOffset, maxDistance, speed);
    }

    @Override
    public Object createDoorLoop(DoorBlockEntity be, SoundEvent sound) {
        return DoorLoopSoundFactory.create(be, sound);
    }

    @Override
    public Object createChungusLoop(MachineChungusBlockEntity be, SoundEvent sound) {
        return ChungusLoopSoundFactory.create(be, sound);
    }

    @Override
    public Object createTurbineLoop(MachineIndustrialTurbineBlockEntity be, SoundEvent sound) {
        return TurbineLoopSoundFactory.create(be, sound);
    }

    @Override
    public Object createTurbofanLoop(MachineTurbofanBlockEntity be) {
        return TurbofanLoopSoundFactory.create(be);
    }
}
