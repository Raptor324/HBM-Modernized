package com.hbm_m.entity.grenades;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.main.MainRegistry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 1:1 {@code com.hbm.entity.grenade.EntityGrenadeBase} (EntityThrowable): Wurf mit 1.5 Geschwindigkeit, bei jedem
 * Aufprall 0 Wurfschaden am getroffenen Wesen, Protokoll (enableExtendedLogging) und {@link #explode()}.
 */
public abstract class EntityGrenadeBase extends ThrowableProjectile {

    protected EntityGrenadeBase(EntityType<? extends EntityGrenadeBase> type, Level world) {
        super(type, world);
    }

    protected EntityGrenadeBase(EntityType<? extends EntityGrenadeBase> type, Level world, LivingEntity living) {
        super(type, living, world);
        // EntityThrowable(world, living): Blickrichtung, 1.5 Geschwindigkeit, 1.0 Streuung
        this.shootFromRotation(living, living.getXRot(), living.getYRot(), 0.0F, 1.5F, 1.0F);
    }

    protected EntityGrenadeBase(EntityType<? extends EntityGrenadeBase> type, Level world, double x, double y, double z) {
        super(type, x, y, z, world);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (result instanceof EntityHitResult ehr) {
            ehr.getEntity().hurt(damageSources().thrown(this, this.getOwner()), 0);
        }

        if (!level().isClientSide) {
            if (ModClothConfig.get().enableExtendedLogging) {

                String s = "null";

                if (getOwner() instanceof Player player)
                    s = player.getName().getString();

                MainRegistry.LOGGER.info("[GREN] Set off grenade at " + ((int) getX()) + " / " + ((int) getY()) + " / " + ((int) getZ()) + " by " + s + "!");
            }
        }

        this.explode();
    }

    public abstract void explode();
}
