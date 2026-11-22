package com.hbm_m.util;

import com.hbm_m.interfaces.ILockable;
import com.hbm_m.main.MainRegistry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Адаптер {@link ILockable} поверх ванильного контейнера (сундук, шалкер, бочка):
 * данные замка лежат в persistent data BlockEntity (теги как у DoorBlockEntity:
 * {@code lock}/{@code locked}/{@code lockMod}/{@code cheesable}). Persistent data
 * сериализуется Forge/NeoForge вместе с BE и переживает выгрузку чанка.
 */
public class VanillaContainerLockable implements ILockable {

    private final BlockEntity be;

    public VanillaContainerLockable(BlockEntity be) {
        this.be = be;
    }

    public BlockEntity getBlockEntity() {
        return be;
    }

    private CompoundTag tag() {
        return be.getPersistentData();
    }

    @Override
    public int getPins() {
        return tag().getInt("lock");
    }

    @Override
    public void setPins(int pins) {
        tag().putInt("lock", pins);
        be.setChanged();
    }

    @Override
    public boolean isLocked() {
        return tag().getBoolean("locked");
    }

    @Override
    public void lock() {
        if (getPins() == 0) {
            MainRegistry.LOGGER.error("Attempted to lock a container with no pins set at {}", be.getBlockPos());
        }
        tag().putBoolean("locked", true);
        be.setChanged();
    }

    @Override
    public void unlock() {
        tag().putBoolean("locked", false);
        be.setChanged();
    }

    @Override
    public double getLockMod() {
        return tag().contains("lockMod") ? tag().getDouble("lockMod") : 0.1D;
    }

    @Override
    public void setLockMod(double mod) {
        tag().putDouble("lockMod", mod);
        be.setChanged();
    }

    @Override
    public boolean isCheesable() {
        return !tag().contains("cheesable") || tag().getBoolean("cheesable");
    }

    @Override
    public void setCheesable(boolean cheesable) {
        tag().putBoolean("cheesable", cheesable);
        be.setChanged();
    }

    @Override
    public net.minecraft.core.BlockPos getLockPos() {
        return be.getBlockPos();
    }

    @Override
    public net.minecraft.world.level.Level getLockLevel() {
        return be.getLevel();
    }
}
