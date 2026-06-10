package com.hbm_m.entity.logic;

import com.hbm_m.interfaces.IEnergyReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;

/**
 * Импульс EMP: обнуляет энергию машин в радиусе 100 блоков (порт {@code com.hbm.entity.logic.EntityEMP}).
 * Сканирует только block entity в загруженных чанках — не перебирает каждый блок сферы.
 */
public class EmpPulseEntity extends Entity {

    private static final int LIFE = 10 * 60 * 20;
    private static final int RADIUS = 100;
    private static final long RADIUS_SQR = (long) RADIUS * RADIUS;

    private List<BlockPos> machines;

    public EmpPulseEntity(EntityType<? extends EmpPulseEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            if (this.machines == null) {
                this.allocate();
            } else {
                this.shock();
            }

            if (this.tickCount > LIFE) {
                this.discard();
            }
        }
    }

    private void allocate() {
        this.machines = new ArrayList<>();
        BlockPos center = this.blockPosition();

        int minChunkX = (center.getX() - RADIUS) >> 4;
        int maxChunkX = (center.getX() + RADIUS) >> 4;
        int minChunkZ = (center.getZ() - RADIUS) >> 4;
        int maxChunkZ = (center.getZ() + RADIUS) >> 4;

        Level level = this.level();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos pos = be.getBlockPos();
                    if (be instanceof IEnergyReceiver && center.distSqr(pos) <= RADIUS_SQR) {
                        this.machines.add(pos.immutable());
                    }
                }
            }
        }
    }

    private void shock() {
        for (BlockPos pos : this.machines) {
            BlockEntity be = this.level().getBlockEntity(pos);
            if (be instanceof IEnergyReceiver receiver) {
                receiver.setEnergyStored(0);
            }
        }
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
