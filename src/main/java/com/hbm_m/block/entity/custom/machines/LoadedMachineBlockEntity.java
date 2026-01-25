package com.hbm_m.block.entity.custom.machines;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Базовый класс для BlockEntity, который отслеживает загрузку чанка
 * и управляет синхронизацией с клиентом
 */
public abstract class LoadedMachineBlockEntity extends BlockEntity {
    
    protected boolean isLoaded = true;
    protected boolean muffled = false;
    
    public LoadedMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    public boolean isLoaded() {
        return isLoaded;
    }
    
    @Override
    public void setRemoved() {
        super.setRemoved();
        this.isLoaded = false;
    }
    
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        this.isLoaded = false;
    }
    
    @Override
    public void onLoad() {
        super.onLoad();
        this.isLoaded = true;
    }
    
    public boolean isMuffled() {
        return muffled;
    }
    
    public void setMuffled(boolean muffled) {
        this.muffled = muffled;
        setChanged();
    }
    
    public float getVolume(float baseVolume) {
        return muffled ? baseVolume * 0.1F : baseVolume;
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("muffled", muffled);
    }
    
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.muffled = tag.getBoolean("muffled");
    }
    
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }
    
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
    
    /**
     * Отправляет обновление клиенту
     */
    protected void sendUpdateToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    
    /**
     * Отправляет обновление клиенту с указанием дистанции
     */
    protected void sendUpdateToClient(int range) {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            // В 1.20.1 нет прямого аналога networkPackNT с радиусом
            // Используется стандартная синхронизация через getUpdatePacket()
        }
    }
}
