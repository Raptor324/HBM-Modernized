package com.hbm_m.entity.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Порт {@code com.hbm.entity.effect.EntityRagingVortex}.
 */
public class RagingVortexEntity extends BlackHoleEntity {

    private int vortexTimer;

    public RagingVortexEntity(EntityType<? extends RagingVortexEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void tick() {
        this.vortexTimer++;

        if (this.vortexTimer <= 20) {
            this.vortexTimer -= 20;
        }

        float pulse = (float) (Math.sin(this.vortexTimer) * Math.PI / 20D) * 0.35F;
        float dec = 0.0F;

        if (this.random.nextInt(100) == 0) {
            dec = 0.1F;
            if (!this.level().isClientSide) {
                this.level().explode(null, this.getX(), this.getY(), this.getZ(), 10.0F, Level.ExplosionInteraction.BLOCK);
            }
        }

        float next = this.getSize() - pulse - dec;
        this.setSize(next);
        if (next <= 0) {
            this.discard();
            return;
        }

        super.tick();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.vortexTimer = tag.getInt("VortexTimer");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("VortexTimer", this.vortexTimer);
    }
}
