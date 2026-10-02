package com.hbm_m.item.weapon;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm_m.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/** 1:1 {@code com.hbm.items.weapon.ItemGrenadeDynamite} ({@code stick_dynamite}). */
public class ItemGrenadeDynamite extends ItemGenericGrenade {

    public ItemGrenadeDynamite(int fuse, Properties properties) {
        super(fuse, properties);
    }

    @Override
    public void explode(Entity grenade, LivingEntity thrower, Level world, double x, double y, double z) {
        ExplosionVNT vnt = new ExplosionVNT(grenade.level(), grenade.getX(), grenade.getY(), grenade.getZ(), 5, thrower);
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 15));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
    }
}
