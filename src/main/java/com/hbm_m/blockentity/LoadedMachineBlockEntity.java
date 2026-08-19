package com.hbm_m.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Базовый класс для BlockEntity, который отслеживает загрузку чанка
 * и управляет синхронизацией с клиентом.
 *
 * <p>Наследует {@link BaseHbmBlockEntity} : персистенция и клиент-синхронизация
 * идут через writeNbtData/readNbtData/applyClientUpdate — без stonecutter-ветвлений.
 */
public abstract class LoadedMachineBlockEntity extends BaseHbmBlockEntity {
    
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
    //? if forge {
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
    //?}
    
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
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("muffled", muffled);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        this.muffled = tag.getBoolean("muffled");
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        
        if (this.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.getServer().submit(() -> {
                if (this.isRemoved()) return;
                
                BlockState state = this.getBlockState();
                if (state.getBlock() instanceof com.hbm_m.interfaces.IMultiblockController controller) {
                    controller.getStructureHelper().attemptAutoRepair(serverLevel, this.worldPosition, state, controller);
                }
            });
        }
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
