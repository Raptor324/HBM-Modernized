package com.hbm_m.api.block;

import javax.annotation.Nullable;

import com.hbm_m.entity.item.EntityTNTPrimedBase;

import net.minecraft.world.level.Level;

/** 1:1 {@code api.hbm.block.IFuckingExplode}: was ein gezuendeter Block beim Ablauf der Lunte tut. */
public interface IFuckingExplode {

    void explodeEntity(Level world, double x, double y, double z, @Nullable EntityTNTPrimedBase entity);
}
