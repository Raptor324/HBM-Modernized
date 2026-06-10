package com.hbm_m.sound;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Вызовы {@link ClientSoundManager} через reflection, чтобы common-классы (BlockEntity и т.д.)
 * не имели в constant pool ссылок на клиентские типы — иначе dedicated server падает при загрузке класса.
 */
public final class ClientSoundBootstrap {

    private static final String CLIENT_SOUND_MANAGER = "com.hbm_m.sound.ClientSoundManager";

    private ClientSoundBootstrap() {}

    private static Class<?> managerClass() throws ClassNotFoundException {
        return Class.forName(CLIENT_SOUND_MANAGER);
    }

    private static boolean isClientSide(Level level) {
        return level != null && level.isClientSide();
    }

    public static void playOneShotSound(Level level, BlockPos pos, SoundEvent sound, float volume) {
        if (!isClientSide(level) || sound == null) {
            return;
        }
        try {
            Method m = managerClass().getMethod("playOneShotSound", BlockPos.class, SoundEvent.class, float.class);
            m.invoke(null, pos, sound, volume);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void stopSound(Level level, BlockPos pos) {
        if (!isClientSide(level)) {
            return;
        }
        try {
            Method m = managerClass().getMethod("stopSound", BlockPos.class);
            m.invoke(null, pos);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void stopSpecificSound(Level level, BlockPos pos, String soundType) {
        if (!isClientSide(level)) {
            return;
        }
        try {
            Method m = managerClass().getMethod("stopSpecificSound", BlockPos.class, String.class);
            m.invoke(null, pos, soundType);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateDoorSoundRaw(Level level, BlockPos pos, String soundType, boolean isMoving, Supplier<?> loopSoundSupplier) {
        if (!isClientSide(level)) {
            return;
        }
        try {
            Method m = managerClass().getMethod(
                "updateDoorSoundRaw",
                BlockPos.class,
                String.class,
                boolean.class,
                Supplier.class
            );
            m.invoke(null, pos, soundType, isMoving, loopSoundSupplier);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateSound(BlockEntity be, boolean shouldBePlaying, Supplier<?> soundSupplier) {
        if (be == null || be.getLevel() == null || !be.getLevel().isClientSide()) {
            return;
        }
        try {
            Method m = managerClass().getMethod("updateSound", BlockEntity.class, boolean.class, Supplier.class);
            m.invoke(null, be, shouldBePlaying, soundSupplier);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
