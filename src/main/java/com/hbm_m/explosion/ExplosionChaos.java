package com.hbm_m.explosion;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.projectile.ClusterRocketEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Вспомогательные эффекты взрывов (поджог, кластер) по образцу {@code com.hbm.explosion.ExplosionChaos}.
 */
public final class ExplosionChaos {

    private ExplosionChaos() {
    }

    /** Поджигает горючие блоки в сфере (flameDeath). */
    /** 1:1 ExplosionChaos.igniteAllBlocks: Feuer auf jeden nicht leeren Block in der Halbkugel r*r/2. */
    public static void igniteAllBlocks(Level world, int x, int y, int z, int bound) {

        int r = bound;
        int r2 = r * r;
        int r22 = r2 / 2;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        for (int xx = -r; xx < r; xx++) {
            int X = xx + x;
            int XX = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int Y = yy + y;
                int YY = XX + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    int Z = zz + z;
                    int ZZ = YY + zz * zz;
                    if (ZZ < r22) {
                        pos.set(X, Y, Z);
                        above.set(X, Y + 1, Z);
                        var up = world.getBlockState(above);
                        if ((up.isAir() || up.is(Blocks.SNOW)) && !world.getBlockState(pos).isAir()) {
                            world.setBlock(above, Blocks.FIRE.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    public static void flameDeath(Level level, int x, int y, int z, int bound) {
        int r = bound;
        int r2 = r * r;
        int r22 = r2 / 2;
        for (int xx = -r; xx < r; xx++) {
            int X = xx + x;
            int XX = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int Y = yy + y;
                int YY = XX + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    int Z = zz + z;
                    int ZZ = YY + zz * zz;
                    if (ZZ < r22) {
                        BlockPos pos = new BlockPos(X, Y, Z);
                        if (level.getBlockState(pos).isFlammable(level, pos, Direction.UP)
                                && level.getBlockState(pos.above()).isAir()) {
                            level.setBlock(pos.above(), Blocks.FIRE.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    /** Ставит огонь над непустыми блоками в сфере (burn). */
    public static void burn(Level level, int x, int y, int z, int bound) {
        int r = bound;
        int r2 = r * r;
        int r22 = r2 / 2;
        for (int xx = -r; xx < r; xx++) {
            int X = xx + x;
            int XX = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int Y = yy + y;
                int YY = XX + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    int Z = zz + z;
                    int ZZ = YY + zz * zz;
                    if (ZZ < r22) {
                        BlockPos pos = new BlockPos(X, Y, Z);
                        BlockPos above = pos.above();
                        if ((level.getBlockState(above).isAir() || level.getBlockState(above).is(Blocks.SNOW))
                                && !level.getBlockState(pos).isAir()) {
                            level.setBlock(above, Blocks.FIRE.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    /**
     * Разбрасывает суббоеприпасы по направлению полёта ракеты
     * (аналог {@code ExplosionChaos.cluster} + {@code EntityBulletBaseMK4} в 1.7.10).
     */
    public static void cluster(Level level, double x, double y, double z, int count,
                               float yaw, float pitch, float yawRand, float pitchRand, float speed) {
        if (level.isClientSide) {
            return;
        }

        for (int i = 0; i < count; i++) {
            float yawRad = yaw + (float) (yawRand * level.random.nextGaussian());
            float pitchRad = pitch + (float) (pitchRand * level.random.nextGaussian());

            float yawDeg = yawRad * 180.0F / (float) Math.PI;
            float pitchDeg = -pitchRad * 180.0F / (float) Math.PI;

            double motionX = -Mth.sin(yawDeg * ((float) Math.PI / 180.0F))
                    * Mth.cos(pitchDeg * ((float) Math.PI / 180.0F));
            double motionZ = Mth.cos(yawDeg * ((float) Math.PI / 180.0F))
                    * Mth.cos(pitchDeg * ((float) Math.PI / 180.0F));
            double motionY = -Mth.sin(pitchDeg * ((float) Math.PI / 180.0F));

            ClusterRocketEntity fragment = new ClusterRocketEntity(ModEntities.CLUSTER_ROCKET.get(), level);
            fragment.setPos(x, y, z);
            fragment.setDeltaMovement(motionX * speed, motionY * speed, motionZ * speed);
            level.addFreshEntity(fragment);
        }
    }

    /** 1:1 {@code ExplosionChaos.floater}: hebt eine Kugel (Radius^2/2) um {@code height} Bloecke an. */
    public static void floater(Level world, int x, int y, int z, int radi, int height) {
        int r = radi;
        int r2 = r * r;
        int r22 = r2 / 2;
        for (int xx = -r; xx < r; xx++) {
            int X = xx + x;
            int XX = xx * xx;
            for (int yy = -r; yy < r; yy++) {
                int Y = yy + y;
                int YY = XX + yy * yy;
                for (int zz = -r; zz < r; zz++) {
                    int Z = zz + z;
                    int ZZ = YY + zz * zz;
                    if (ZZ < r22) {
                        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(X, Y, Z);
                        net.minecraft.world.level.block.state.BlockState save = world.getBlockState(pos);
                        world.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                        if (!save.isAir()) {
                            world.setBlock(pos.above(height), save, 2);
                        }
                    }
                }
            }
        }
    }

    /** 1:1 {@code ExplosionChaos.move}: benennt Lebewesen um (Dinnerbone/Grumm/jeb_) und verschiebt alles im Radius. */
    public static void move(Level world, int x, int y, int z, int radius, int a, int b, int c) {
        double wat = radius;

        radius *= 2.0F;
        int i = net.minecraft.util.Mth.floor(x - wat - 1.0D);
        int j = net.minecraft.util.Mth.floor(x + wat + 1.0D);
        int k = net.minecraft.util.Mth.floor(y - wat - 1.0D);
        int i2 = net.minecraft.util.Mth.floor(y + wat + 1.0D);
        int l = net.minecraft.util.Mth.floor(z - wat - 1.0D);
        int j2 = net.minecraft.util.Mth.floor(z + wat + 1.0D);
        java.util.List<net.minecraft.world.entity.Entity> list = world.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(i, k, l, j, i2, j2));

        for (net.minecraft.world.entity.Entity entity : list) {
            double d4 = Math.sqrt(entity.distanceToSqr(x, y, z)) / radius;

            if (d4 <= 1.0D) {
                double d5 = entity.getX() - x;
                double d6 = entity.getEyeY() - y;
                double d7 = entity.getZ() - z;
                if (entity instanceof net.minecraft.world.entity.Mob && !(entity instanceof net.minecraft.world.entity.animal.Sheep)) {
                    entity.setCustomName(net.minecraft.network.chat.Component.literal(world.random.nextInt(2) == 0 ? "Dinnerbone" : "Grumm"));
                }

                if (entity instanceof net.minecraft.world.entity.animal.Sheep) {
                    entity.setCustomName(net.minecraft.network.chat.Component.literal("jeb_"));
                }

                double d9 = Math.sqrt(d5 * d5 + d6 * d6 + d7 * d7);
                if (d9 < wat) {
                    entity.setPos(entity.getX() + a, entity.getY() + b, entity.getZ() + c);
                }
            }
        }
    }

    // ================================================================================================
    // Gaswolken (EntityModFX)

    private static java.util.List<net.minecraft.world.entity.LivingEntity> gasTargets(Level world, double x, double y, double z, double range) {
        java.util.List<net.minecraft.world.entity.LivingEntity> out = new java.util.ArrayList<>();
        for (net.minecraft.world.entity.LivingEntity e : world.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                new net.minecraft.world.phys.AABB(x - range, y - range, z - range, x + range, y + range, z + range))) {
            if (Math.sqrt(e.distanceToSqr(x, y, z)) <= range) out.add(e);
        }
        return out;
    }

    /** 1:1 {@code ExplosionChaos.poison}: Chlor / Agent Orange - Gasmaske (Lunge oder Blasen) verbraucht Filter, sonst Giftcocktail. */
    public static void poison(Level world, double x, double y, double z, double range) {
        for (net.minecraft.world.entity.LivingEntity entity : gasTargets(world, x, y, z, range)) {
            if (com.hbm_m.handler.ArmorRegistry.hasProtection(entity, 3, com.hbm_m.handler.HazardClass.GAS_LUNG)
                    || com.hbm_m.handler.ArmorRegistry.hasProtection(entity, 3, com.hbm_m.handler.HazardClass.GAS_BLISTERING)) {
                com.hbm_m.util.ArmorUtil.damageGasMaskFilter(entity, 1);
            } else {
                entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 5 * 20, 0));
                entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 20 * 20, 2));
                entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 1 * 20, 1));
                entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 30 * 20, 1));
                entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN, 30 * 20, 2));
            }
        }
    }

    private static void damageWholeSuit(net.minecraft.world.entity.LivingEntity entity, int amount) {
        for (net.minecraft.world.entity.EquipmentSlot s : new net.minecraft.world.entity.EquipmentSlot[] {
                net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET })
            com.hbm_m.util.ArmorUtil.damageSuit(entity, s, amount);
    }

    /** 1:1 {@code ExplosionChaos.pc}: Pink Cloud - frisst Ruestung (25 je Teil) und macht 5 Schaden. */
    public static void pc(Level world, double x, double y, double z, double range) {
        for (net.minecraft.world.entity.LivingEntity entity : gasTargets(world, x, y, z, range)) {
            damageWholeSuit(entity, 25);
            entity.hurt(com.hbm_m.damagesource.ModDamageSources.pc(world), 5);
        }
    }

    /** 1:1 {@code ExplosionChaos.c}: Wolke - frisst Ruestung, Hazmat schuetzt, Taint wird zu Mutation, sonst 5 Schaden. */
    public static void c(Level world, double x, double y, double z, double range) {
        for (net.minecraft.world.entity.LivingEntity entity : gasTargets(world, x, y, z, range)) {
            damageWholeSuit(entity, 25);
            if (com.hbm_m.util.ArmorUtil.checkForHazmat(entity)) continue;
            if (entity.hasEffect(com.hbm_m.effect.ModEffects.TAINT.get())) {
                entity.removeEffect(com.hbm_m.effect.ModEffects.TAINT.get());
                entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(com.hbm_m.effect.ModEffects.MUTATION.get(), 1 * 60 * 60 * 20, 0, false, true));
            }
            entity.hurt(com.hbm_m.damagesource.ModDamageSources.cloud(world), 5);
        }
    }

    /** 1:1 {@code ExplosionChaos.spawnPoisonCloud}: 1 = Wolke, 2 = Pink Cloud, sonst Agent Orange. */
    public static void spawnPoisonCloud(Level world, double x, double y, double z, int count, double speed, int type) {
        for (int i = 0; i < count; i++) {
            com.hbm_m.entity.effect.EntityModFX fx;
            if (type == 1) fx = new com.hbm_m.entity.effect.EntityModFX.Cloud(world, x, y, z, 0.0, 0.0, 0.0);
            else if (type == 2) fx = new com.hbm_m.entity.effect.EntityModFX.PinkCloud(world, x, y, z, 0.0, 0.0, 0.0);
            else fx = new com.hbm_m.entity.effect.EntityModFX.Orange(world, x, y, z, 0.0, 0.0, 0.0);
            fx.setDeltaMovement(world.random.nextGaussian() * speed, world.random.nextGaussian() * speed, world.random.nextGaussian() * speed);
            world.addFreshEntity(fx);
        }
    }
}
