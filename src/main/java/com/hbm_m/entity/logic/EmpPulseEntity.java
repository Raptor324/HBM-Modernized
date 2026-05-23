package com.hbm_m.entity.logic;

import com.hbm_m.interfaces.IEnergyReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Импульс EMP: обнуляет энергию машин в радиусе 100 блоков (порт {@code com.hbm.entity.logic.EMP}).
 */
public class EmpPulseEntity extends Entity {

    private static final int LIFE = 10 * 60 * 20;
    private static final int RADIUS = 100;

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

        for (int x = -RADIUS; x <= RADIUS; x++) {
            int x2 = x * x;
            for (int y = -RADIUS; y <= RADIUS; y++) {
                int y2 = x2 + y * y;
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    int z2 = y2 + z * z;
                    if (Math.sqrt(z2) <= RADIUS) {
                        this.tryAdd(center.offset(x, y, z));
                    }
                }
            }
        }
    }

    private void tryAdd(BlockPos pos) {
        BlockEntity be = this.level().getBlockEntity(pos);
        if (be instanceof IEnergyReceiver) {
            this.machines.add(pos.immutable());
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
