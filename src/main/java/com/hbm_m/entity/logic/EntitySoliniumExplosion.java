package com.hbm_m.entity.logic;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.explosion.ExplosionSolinium;
import com.hbm_m.explosion.ExplosionNukeGeneric;
import com.hbm_m.util.ContaminationUtil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Взрыв солиния («синяя стирка»): органика удаляется, всё живое получает
 * колоссальную дозу радиации каждый тик спирали.
 */
public class EntitySoliniumExplosion extends EntityExplosionChunkloading {

    public int destructionRange;
    public ExplosionSolinium expl;
    public int speed = 1;
    public boolean did;

    public EntitySoliniumExplosion(EntityType<? extends EntitySoliniumExplosion> type, Level level) {
        super(type, level);
    }

    @Override
    protected int getChunkLoadRadius() {
        if (this.destructionRange <= 0) {
            return super.getChunkLoadRadius();
        }
        return Math.min(12, Math.max(super.getChunkLoadRadius(), (this.destructionRange + 15) >> 4) + 1);
    }

    //? if < 1.21.1 {

    @Override
    protected void defineSynchedData() {
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }
    *///?}

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            updateChunkTicket();
        }

        if (!this.did) {
            this.expl = new ExplosionSolinium(
                    (int) getX(), (int) getY(), (int) getZ(),
                    level(), this.destructionRange, 1.0F, 1.0F);
            this.did = true;
        }

        this.speed += 1;

        if (!level().isClientSide) {
            for (int i = 0; i < this.speed; i++) {
                if (this.expl.update()) {
                    clearChunkTicket();
                    this.discard();
                    break;
                }
            }
        }

        if (!level().isClientSide) {
            level().playSound(null, getX(), getY(), getZ(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS,
                    10000.0F, 0.8F + level().random.nextFloat() * 0.2F);
            doRadiation();
        }
    }

    /** Линейная доза от 15000 (край) до 250000 (центр), в обход защиты костюма. */
    private void doRadiation() {
        double range = this.destructionRange;
        var entities = level().getEntitiesOfClass(LivingEntity.class,
                new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(range));
        for (LivingEntity entity : entities) {
            Vec3 vector = new Vec3(getX() - entity.getX(), getY() - entity.getY(), getZ() - entity.getZ());
            double distance = vector.length();
            if (distance > range) continue;
            float rad = (float) (15000.0D + (250000.0D - 15000.0D) * (1.0D - distance / range));
            ContaminationUtil.contaminate(entity,
                    ContaminationUtil.HazardType.RADIATION,
                    ContaminationUtil.ContaminationType.RAD_BYPASS,
                    rad);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        clearChunkTicket();
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.tickCount = tag.getInt("age");
        destructionRange = tag.getInt("destructionRange");
        speed = tag.getInt("speed");
        did = tag.getBoolean("did");

        long time = tag.getLong("milliTime");
        int lifespan;
        try {
            lifespan = ModClothConfig.get().limitExplosionLifespan;
        } catch (Exception e) {
            lifespan = 0;
        }
        if (lifespan > 0 && System.currentTimeMillis() - time > lifespan * 1000L) {
            discard();
            return;
        }

        if (did) {
            expl = new ExplosionSolinium(
                    (int) getX(), (int) getY(), (int) getZ(),
                    level(), destructionRange, 1.0F, 1.0F);
            expl.readFromNbt(tag, "expl_");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("age", this.tickCount);
        tag.putInt("destructionRange", destructionRange);
        tag.putInt("speed", speed);
        tag.putBoolean("did", did);
        tag.putLong("milliTime", System.currentTimeMillis());
        if (expl != null) {
            expl.saveToNbt(tag, "expl_");
        }
    }

    public static EntitySoliniumExplosion statFac(Level level, double x, double y, double z, int range) {
        EntitySoliniumExplosion entity = new EntitySoliniumExplosion(ModEntities.SOLINIUM_EXPLOSION.get(), level);
        entity.setPos(x, y, z);
        entity.destructionRange = range;
        entity.speed = ModClothConfig.get().blastSpeed;
        return entity;
    }
}
