package com.hbm_m.explosion;

import java.util.List;

import com.hbm_m.util.ContaminationUtil;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 1:1-Port von {@code com.hbm.explosion.ExplosionHurtUtil}. */
public class ExplosionHurtUtil {

    /**
     * Verstrahlt alle Lebewesen im Umkreis.
     * @param outer kleinste Dosis ganz am Rand
     * @param inner groesste Dosis genau im Zentrum
     */
    public static void doRadiation(Level world, double x, double y, double z, float outer, float inner, double radius) {

        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius));

        for (LivingEntity entity : entities) {

            Vec3 vec = new Vec3(x - entity.getX(), y - entity.getY(), z - entity.getZ());

            double dist = vec.length();

            if (dist > radius)
                continue;

            double interpolation = 1 - (dist / radius);
            float rad = (float) (outer + (inner - outer) * interpolation);

            ContaminationUtil.contaminate(entity, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.CREATIVE, rad);
        }
    }
}
