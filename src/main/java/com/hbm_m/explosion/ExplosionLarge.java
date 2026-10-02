package com.hbm_m.explosion;

import java.util.List;

import com.hbm_m.entity.projectile.EntityShrapnel;
import com.hbm_m.entity.projectile.RubbleEntity;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.util.ParticleUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1-Port von {@code com.hbm.explosion.ExplosionLarge} (1.7.10): die Werkzeugkiste fuer
 * "grosse" konventionelle Explosionen - Rauchwolke, radiale Wolke, Schaum, Schockwelle,
 * Flammenkranz, Truemmer, Schrapnell, Leuchtspur, Raketentruemmer, Bunkerbrecher und der
 * Truemmer-{@code jolt}.
 */
public final class ExplosionLarge {

    private ExplosionLarge() {}

    private static final RandomSource rand = RandomSource.create();

    private static void send(Level world, double x, double y, double z, CompoundTag data) {
        if (world instanceof ServerLevel sl) IParticleCreator.sendPacket(sl, x, y, z, 250, data);
    }

    @Deprecated
    public static void spawnParticles(Level world, double x, double y, double z, int count) {
        CompoundTag data = new CompoundTag();
        data.putString("type", "smoke");
        data.putString("mode", "cloud");
        data.putInt("count", count);
        send(world, x, y, z, data);
    }

    public static void spawnParticlesRadial(Level world, double x, double y, double z, int count) {
        CompoundTag data = new CompoundTag();
        data.putString("type", "smoke");
        data.putString("mode", "radial");
        data.putInt("count", count);
        send(world, x, y, z, data);
    }

    public static void spawnFoam(Level world, double x, double y, double z, int count) {
        CompoundTag data = new CompoundTag();
        data.putString("type", "smoke");
        data.putString("mode", "foamSplash");
        data.putInt("count", count);
        send(world, x, y, z, data);
    }

    public static void spawnShock(Level world, double x, double y, double z, int count, double strength) {
        CompoundTag data = new CompoundTag();
        data.putString("type", "smoke");
        data.putString("mode", "shock");
        data.putInt("count", count);
        data.putDouble("strength", strength);
        if (world instanceof ServerLevel sl) IParticleCreator.sendPacket(sl, x, y + 0.5, z, 250, data);
    }

    public static void spawnBurst(Level world, double x, double y, double z, int count, double strength) {
        Vec3 vec = new Vec3(strength, 0, 0).yRot(rand.nextInt(360));
        for (int i = 0; i < count; i++) {
            ParticleUtil.spawnGasFlame(world, x, y, z, vec.x, 0.0, vec.z);
            vec = vec.yRot(360 / count);
        }
    }

    public static void spawnRubble(Level world, double x, double y, double z, int count) {
        for (int i = 0; i < count; i++) {
            RubbleEntity rubble = RubbleEntity.create(world, x, y, z, Blocks.STONE.defaultBlockState());
            rubble.setDeltaMovement(
                    rand.nextGaussian() * 0.75 * (1 + (count / 50)),
                    0.75 * (1 + ((count + rand.nextInt(count * 5))) / 25),
                    rand.nextGaussian() * 0.75 * (1 + (count / 50)));
            world.addFreshEntity(rubble);
        }
    }

    public static void spawnShrapnels(Level world, double x, double y, double z, int count) {
        for (int i = 0; i < count; i++) {
            EntityShrapnel shrapnel = new EntityShrapnel(world);
            shrapnel.setPos(x, y, z);
            shrapnel.setDeltaMovement(
                    rand.nextGaussian() * 1 * (1 + (count / 50)),
                    ((rand.nextFloat() * 0.5) + 0.5) * (1 + (count / (15 + rand.nextInt(21)))) + (rand.nextFloat() / 50 * count),
                    rand.nextGaussian() * 1 * (1 + (count / 50)));
            shrapnel.setTrail(rand.nextInt(3) == 0);
            world.addFreshEntity(shrapnel);
        }
    }

    public static void spawnTracers(Level world, double x, double y, double z, int count) {
        for (int i = 0; i < count; i++) {
            EntityShrapnel shrapnel = new EntityShrapnel(world);
            shrapnel.setPos(x, y, z);
            shrapnel.setDeltaMovement(
                    rand.nextGaussian() * 1 * (1 + (count / 50)) * 0.25F,
                    ((rand.nextFloat() * 0.5) + 0.5) * (1 + (count / (15 + rand.nextInt(21)))) + (rand.nextFloat() / 50 * count) * 0.25F,
                    rand.nextGaussian() * 1 * (1 + (count / 50)) * 0.25F);
            shrapnel.setTrail(true);
            world.addFreshEntity(shrapnel);
        }
    }

    public static void spawnShrapnelShower(Level world, double x, double y, double z, double motionX, double motionY, double motionZ, int count, double deviation) {
        for (int i = 0; i < count; i++) {
            EntityShrapnel shrapnel = new EntityShrapnel(world);
            shrapnel.setPos(x, y, z);
            shrapnel.setDeltaMovement(motionX + rand.nextGaussian() * deviation, motionY + rand.nextGaussian() * deviation, motionZ + rand.nextGaussian() * deviation);
            shrapnel.setTrail(rand.nextInt(3) == 0);
            world.addFreshEntity(shrapnel);
        }
    }

    public static void spawnMissileDebris(Level world, double x, double y, double z, double motionX, double motionY, double motionZ, double deviation, List<ItemStack> debris, ItemStack rareDrop) {
        if (debris != null) {
            for (ItemStack stack : debris) {
                if (stack == null || stack.isEmpty()) continue;
                int k = rand.nextInt(stack.getCount() + 1);
                for (int j = 0; j < k; j++) {
                    ItemEntity item = new ItemEntity(world, x, y, z, stack.copy());
                    Vec3 m = new Vec3((motionX + rand.nextGaussian() * deviation) * 0.85, (motionY + rand.nextGaussian() * deviation) * 0.85, (motionZ + rand.nextGaussian() * deviation) * 0.85);
                    item.setDeltaMovement(m);
                    item.setPos(item.getX() + m.x * 2, item.getY() + m.y * 2, item.getZ() + m.z * 2);
                    world.addFreshEntity(item);
                }
            }
        }
        if (rareDrop != null && !rareDrop.isEmpty() && rand.nextInt(10) == 0) {
            ItemEntity item = new ItemEntity(world, x, y, z, rareDrop.copy());
            item.setDeltaMovement(motionX + rand.nextGaussian() * deviation * 0.1, motionY + rand.nextGaussian() * deviation * 0.1, motionZ + rand.nextGaussian() * deviation * 0.1);
            world.addFreshEntity(item);
        }
    }

    @Deprecated
    public static void explode(Level world, double x, double y, double z, float strength, boolean cloud, boolean rubble, boolean shrapnel, Entity exploder) {
        world.explode(exploder, x, y, z, strength, Level.ExplosionInteraction.TNT);
        if (cloud) spawnParticles(world, x, y, z, cloudFunction((int) strength));
        if (rubble) spawnRubble(world, x, y, z, rubbleFunction((int) strength));
        if (shrapnel) spawnShrapnels(world, x, y, z, shrapnelFunction((int) strength));
    }

    @Deprecated
    public static void explode(Level world, double x, double y, double z, float strength, boolean cloud, boolean rubble, boolean shrapnel) {
        explode(world, x, y, z, strength, cloud, rubble, shrapnel, null);
    }

    @Deprecated
    public static void explodeFire(Level world, double x, double y, double z, float strength, boolean cloud, boolean rubble, boolean shrapnel) {
        world.explode(null, x, y, z, strength, true, Level.ExplosionInteraction.TNT);
        if (cloud) spawnParticles(world, x, y, z, cloudFunction((int) strength));
        if (rubble) spawnRubble(world, x, y, z, rubbleFunction((int) strength));
        if (shrapnel) spawnShrapnels(world, x, y, z, shrapnelFunction((int) strength));
    }

    public static void buster(Level world, double x, double y, double z, Vec3 vector, float strength, float depth) {
        vector = vector.normalize();
        for (int i = 0; i < depth; i += 2) {
            world.explode(null, x + vector.x * i, y + vector.y * i, z + vector.z * i, strength, Level.ExplosionInteraction.TNT);
        }
    }

    public static void jolt(Level world, double posX, double posY, double posZ, double strength, int count, double vel) {
        for (int j = 0; j < count; j++) {
            double phi = rand.nextDouble() * (Math.PI * 2);
            double costheta = rand.nextDouble() * 2 - 1;
            double theta = Math.acos(costheta);
            double x = Math.sin(theta) * Math.cos(phi);
            double y = Math.sin(theta) * Math.sin(phi);
            double z = Math.cos(theta);
            Vec3 vec = new Vec3(x, y, z);

            for (int i = 0; i < strength; i++) {
                double x0 = posX + (vec.x * i);
                double y0 = posY + (vec.y * i);
                double z0 = posZ + (vec.z * i);
                if (world.isClientSide) continue;
                BlockPos p = new BlockPos((int) x0, (int) y0, (int) z0);
                BlockState state = world.getBlockState(p);
                if (!state.getFluidState().isEmpty()) {
                    world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                    state = Blocks.AIR.defaultBlockState();
                }
                if (!state.isAir()) {
                    if (state.getBlock().getExplosionResistance() > 70) continue;
                    RubbleEntity rubble = RubbleEntity.create(world, x0 + 0.5F, y0 + 0.5F, z0 + 0.5F, state);
                    // Original: vec4.normalize() ohne Zuweisung - der Vektor bleibt unnormiert.
                    Vec3 vec4 = new Vec3(posX - rubble.getX(), posY - rubble.getY(), posZ - rubble.getZ());
                    rubble.setDeltaMovement(vec4.x * vel, vec4.y * vel, vec4.z * vel);
                    world.addFreshEntity(rubble);
                    world.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                    break;
                }
            }
        }
    }

    public static int cloudFunction(int i) {
        return (int) (850 * (1 - Math.pow(Math.E, -i / 15)) + 15);
    }

    public static int rubbleFunction(int i) {
        return i / 10;
    }

    public static int shrapnelFunction(int i) {
        return i / 3;
    }
}
