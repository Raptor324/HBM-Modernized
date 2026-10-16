package com.hbm_m.particle.helper;

import com.hbm_m.client.handler.ClientVanishHandler;
import com.hbm_m.particle.nt.ParticleSkeletonNT;
import com.hbm_m.particle.nt.ParticleEngineNT;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Порт {@code SkeletonCreator} из 1.7.10.
 * Спавнит набор «костей» в виде {@link ParticleSkeletonNT} для умершей сущности.
 */
public class SkeletonCreator implements IParticleCreator {

    private static final Map<EntityType<?>, Function<LivingEntity, BoneDefinition[]>> SKULLANIZER = new HashMap<>();

    public static void composeEffect(net.minecraft.world.level.Level level, Entity entity, float brightness) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "skeleton");
            data.putInt("entityID", entity.getId());
            data.putFloat("brightness", brightness);
            IParticleCreator.sendPacket(serverLevel, entity.getX(), entity.getY(), entity.getZ(), 100, data);
        }
    }

    @Override
    public void makeParticle(ClientLevel level, Player player, RandomSource rand, double x, double y, double z, CompoundTag tag) {
        if (SKULLANIZER.isEmpty()) init();

        int entityID = tag.getInt("entityID");
        Entity entity = level.getEntity(entityID);
        if (!(entity instanceof LivingEntity living)) return;

        ClientVanishHandler.vanish(entityID);

        float brightness = tag.getFloat("brightness");
        Function<LivingEntity, BoneDefinition[]> bonealizer = SKULLANIZER.get(living.getType());
        boolean baby = living.isBaby();
        if (bonealizer != null) {
            BoneDefinition[] bones = bonealizer.apply(living);
            for (BoneDefinition bone : bones) {
                // Baby-вариант: вся кость уменьшается вдвое (ванильный масштаб 0.5),
                // но череп остаётся полноразмерным — как в рендере baby-мобов ванили
                // (голова ребёнка вдвое крупнее пропорционально телу).
                float scale = baby ? bone.type.babyScale() : 1.0F;
                // Для baby так же вдвое опускаем высоту спаuna относительно земли.
                double bx = bone.x, by = bone.y, bz = bone.z;
                if (baby) {
                    by = living.getY() + (bone.y - living.getY()) * 0.5;
                }
                ParticleSkeletonNT skeleton = new ParticleSkeletonNT(
                        level, bx, by, bz,
                        brightness, brightness, brightness,
                        bone.type, scale
                );
                skeleton.setRotation(bone.yaw, bone.pitch);
                ParticleEngineNT.INSTANCE.add(skeleton);
            }
        }
    }

    public static class BoneDefinition {
        public final ParticleSkeletonNT.BoneKind type;
        public final float yaw;
        public final float pitch;
        public final double x;
        public final double y;
        public final double z;

        public BoneDefinition(ParticleSkeletonNT.BoneKind type, float yaw, float pitch, double x, double y, double z) {
            this.type = type;
            this.yaw = yaw;
            this.pitch = pitch;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static Function<LivingEntity, BoneDefinition[]> BONES_BIPED = (entity) -> {
        Vec3 leftarm = rotateY(0.375, 0, -entity.yBodyRot);
        Vec3 leftleg = rotateY(0.125, 0, -entity.yBodyRot);
        return new BoneDefinition[]{
                new BoneDefinition(ParticleSkeletonNT.BoneKind.SKULL, -entity.getYHeadRot(), entity.getXRot(), entity.getX(), entity.getY() + 1.75, entity.getZ()),
                new BoneDefinition(ParticleSkeletonNT.BoneKind.TORSO, -entity.yBodyRot, 0, entity.getX(), entity.getY() + 1.125, entity.getZ()),
                new BoneDefinition(ParticleSkeletonNT.BoneKind.LIMB, -entity.yBodyRot, 0, entity.getX() + leftarm.x, entity.getY() + 1.125, entity.getZ() + leftarm.z),
                new BoneDefinition(ParticleSkeletonNT.BoneKind.LIMB, -entity.yBodyRot, 0, entity.getX() - leftarm.x, entity.getY() + 1.125, entity.getZ() - leftarm.z),
                new BoneDefinition(ParticleSkeletonNT.BoneKind.LIMB, -entity.yBodyRot, 0, entity.getX() + leftleg.x, entity.getY() + 0.375, entity.getZ() + leftleg.z),
                new BoneDefinition(ParticleSkeletonNT.BoneKind.LIMB, -entity.yBodyRot, 0, entity.getX() - leftleg.x, entity.getY() + 0.375, entity.getZ() - leftleg.z),
        };
    };

    public static Function<LivingEntity, BoneDefinition[]> BONES_ZOMBIE = (entity) -> {
        Vec3 leftarm = rotateY(0.375, 0, -entity.yBodyRot);
        Vec3 forward = rotateY(0, 0.25, -entity.yBodyRot);
        Vec3 leftleg = rotateY(0.125, 0, -entity.yBodyRot);
        return new BoneDefinition[]{
                new BoneDefinition(ParticleSkeletonNT.BoneKind.SKULL, -entity.getYHeadRot(), entity.getXRot(), entity.getX(), entity.getY() + 1.75, entity.getZ()),
                new BoneDefinition(ParticleSkeletonNT.BoneKind.TORSO, -entity.yBodyRot, 0, entity.getX(), entity.getY() + 1.125, entity.getZ()),
                new BoneDefinition(ParticleSkeletonNT.BoneKind.LIMB, -entity.yBodyRot, -90, entity.getX() + leftarm.x + forward.x, entity.getY() + 1.375, entity.getZ() + leftarm.z + forward.z),
                new BoneDefinition(ParticleSkeletonNT.BoneKind.LIMB, -entity.yBodyRot, -90, entity.getX() - leftarm.x + forward.x, entity.getY() + 1.375, entity.getZ() - leftarm.z + forward.z),
                new BoneDefinition(ParticleSkeletonNT.BoneKind.LIMB, -entity.yBodyRot, 0, entity.getX() + leftleg.x, entity.getY() + 0.375, entity.getZ() + leftleg.z),
                new BoneDefinition(ParticleSkeletonNT.BoneKind.LIMB, -entity.yBodyRot, 0, entity.getX() - leftleg.x, entity.getY() + 0.375, entity.getZ() - leftleg.z),
        };
    };

    public static Function<LivingEntity, BoneDefinition[]> BONES_VILLAGER = (entity) -> {
        Vec3 leftarm = rotateY(0.375, 0, -entity.yBodyRot);
        Vec3 forward = rotateY(0, 0.25, -entity.yBodyRot);
        Vec3 leftleg = rotateY(0.125, 0, -entity.yBodyRot);
        return new BoneDefinition[]{
                new BoneDefinition(ParticleSkeletonNT.BoneKind.SKULL_VILLAGER, -entity.getYHeadRot(), entity.getXRot(), entity.getX(), entity.getY() + 1.6875, entity.getZ()),
                new BoneDefinition(ParticleSkeletonNT.BoneKind.TORSO, -entity.yBodyRot, 0, entity.getX(), entity.getY() + 1.0, entity.getZ()),
                new BoneDefinition(ParticleSkeletonNT.BoneKind.LIMB, -entity.yBodyRot, -45, entity.getX() + leftarm.x + forward.x, entity.getY() + 1.125, entity.getZ() + leftarm.z + forward.z),
                new BoneDefinition(ParticleSkeletonNT.BoneKind.LIMB, -entity.yBodyRot, -45, entity.getX() - leftarm.x + forward.x, entity.getY() + 1.125, entity.getZ() - leftarm.z + forward.z),
                new BoneDefinition(ParticleSkeletonNT.BoneKind.LIMB, -entity.yBodyRot, 0, entity.getX() + leftleg.x, entity.getY() + 0.375, entity.getZ() + leftleg.z),
                new BoneDefinition(ParticleSkeletonNT.BoneKind.LIMB, -entity.yBodyRot, 0, entity.getX() - leftleg.x, entity.getY() + 0.375, entity.getZ() - leftleg.z),
        };
    };

    private static Vec3 rotateY(double x, double z, float yawDeg) {
        // Iterates on XZ-plane, mimics Vec3NT.rotateAroundYDeg
        double angleRad = Math.toRadians(+yawDeg);
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        double nx = x * cos - z * sin;
        double nz = x * sin + z * cos;
        return new Vec3(nx, 0, nz);
    }

    private static void init() {
        SKULLANIZER.put(EntityType.PLAYER, BONES_BIPED);

        SKULLANIZER.put(EntityType.ZOMBIE, BONES_ZOMBIE);
        SKULLANIZER.put(EntityType.GIANT, BONES_ZOMBIE);
        SKULLANIZER.put(EntityType.ZOMBIE_VILLAGER, BONES_VILLAGER);
        SKULLANIZER.put(EntityType.HUSK, BONES_ZOMBIE);
        SKULLANIZER.put(EntityType.DROWNED, BONES_ZOMBIE);
        SKULLANIZER.put(EntityType.SKELETON, BONES_ZOMBIE);
        SKULLANIZER.put(EntityType.STRAY, BONES_ZOMBIE);
        SKULLANIZER.put(EntityType.WITHER_SKELETON, BONES_ZOMBIE);
        SKULLANIZER.put(EntityType.PIGLIN, BONES_ZOMBIE);
        SKULLANIZER.put(EntityType.PIGLIN_BRUTE, BONES_ZOMBIE);
        SKULLANIZER.put(EntityType.ZOMBIFIED_PIGLIN, BONES_ZOMBIE);

        SKULLANIZER.put(EntityType.VILLAGER, BONES_VILLAGER);
        SKULLANIZER.put(EntityType.PILLAGER, BONES_VILLAGER);
        SKULLANIZER.put(EntityType.EVOKER, BONES_VILLAGER);
        SKULLANIZER.put(EntityType.ILLUSIONER, BONES_VILLAGER);
        SKULLANIZER.put(EntityType.VINDICATOR, BONES_VILLAGER);
        SKULLANIZER.put(EntityType.WANDERING_TRADER, BONES_VILLAGER);
        SKULLANIZER.put(EntityType.WITCH, BONES_VILLAGER);
    }
}
