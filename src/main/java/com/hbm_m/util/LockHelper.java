package com.hbm_m.util;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.interfaces.ILockable;
import com.hbm_m.interfaces.IMultiblockPart;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;

/**
 * Резолвер {@link ILockable} по BlockEntity. Порт семантики
 * {@code CompatExternal.getCoreFromPos} (1.7.10): замок вешается на контроллер
 * мультиблока, кликать можно по любой части.
 *
 * <p>Ванильные контейнеры (сундуки, шалкеры, бочки) не имеют наших полей —
 * для них данные замка хранятся в {@code getPersistentData()} через
 * {@link VanillaContainerLockable} (Forge/NeoForge сериализуют persistent data на диск).
 */
public final class LockHelper {

    private LockHelper() {}

    @Nullable
    public static ILockable resolve(@Nullable BlockEntity be) {
        if (be == null) return null;
        // Мультиблоки: замок живёт на контроллере
        if (be instanceof IMultiblockPart part && part.getControllerPos() != null && be.getLevel() != null) {
            BlockEntity core = be.getLevel().getBlockEntity(part.getControllerPos());
            if (core instanceof ILockable lockable) return lockable;
        }
        if (be instanceof ILockable lockable) return lockable;
        // Ванильные контейнеры (ChestBlockEntity, TrappedChestBlockEntity, ShulkerBoxBlockEntity, BarrelBlockEntity)
        if (be instanceof RandomizableContainerBlockEntity) {
            return new VanillaContainerLockable(be);
        }
        return null;
    }
}
