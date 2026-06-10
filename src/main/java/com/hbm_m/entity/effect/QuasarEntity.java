package com.hbm_m.entity.effect;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Порт {@code com.hbm.entity.effect.EntityQuasar} (digamma quasar).
 */
public class QuasarEntity extends BlackHoleEntity {

    public QuasarEntity(EntityType<? extends QuasarEntity> type, Level level) {
        super(type, level);
    }
}
