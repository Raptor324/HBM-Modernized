package com.hbm_m.entity.missile;

import api.hbm.entity.IRadarDetectable;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class MissileABMEntity extends MissileBaseEntity {

    public MissileABMEntity(EntityType<? extends MissileABMEntity> type, Level level) {
        super(type, level);
    }

    public MissileABMEntity(Level level) {
        this(ModEntities.MISSILE_ABM.get(), level);
    }

    @Override
    protected Item getMissileItem() {
        return ModItems.MISSILE_ABM.get();
    }

    @Override
    protected void onMissileImpact(BlockPos pos) {
        if (this.level().isClientSide) {
            return;
        }

        this.level().explode(
                this,
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                6.0F,
                Level.ExplosionInteraction.BLOCK
        );
    }

    @Override
    public IRadarDetectable.RadarTargetType getTargetType() {
        return IRadarDetectable.RadarTargetType.MISSILE_AB;
    }
}