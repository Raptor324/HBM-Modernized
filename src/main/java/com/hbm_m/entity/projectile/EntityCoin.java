package com.hbm_m.entity.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * 1:1 {@code EntityCoin}: geworfene Muenze (N I 4 N I), die von Strahlen getroffen werden kann und den Strahl auf das
 * naechste Ziel umlenkt (siehe {@link EntityBulletBeamBase}). Zerfaellt beim Aufschlag auf einen Block.
 */
public class EntityCoin extends EntityThrowableNT {

    public EntityCoin(EntityType<? extends EntityCoin> type, Level world) {
        super(type, world);
    }

    @Override
    protected void onImpact(HitResult mop) {
        if (mop.getType() == HitResult.Type.BLOCK) this.discard();
    }

    @Override protected float getAirDrag() { return 1F; }
    @Override public double getGravityVelocity() { return 0.02D; }
    @Override public boolean isPickable() { return true; }
    @Override public boolean isAttackable() { return true; }
}
