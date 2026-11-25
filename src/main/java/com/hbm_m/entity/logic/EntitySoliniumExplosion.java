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
    var defs = com.hbm_m.platform.EntityDataHooks.sink(this.entityData);
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    var defs = com.hbm_m.platform.EntityDataHooks.sink(builder);
    *///?}
    }

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

        // 1:1 legacy EntityNukeExplosionMK3.statFacFleija (ориг. строки 210-265): активный
        // возмутитель поля (FieldDisturber) в радиусе 300 отменяет взрыв — дискард + ufoBlast +
        // частицы plasmablast. Оригинал делал это в statFacFleija().makeSol() для солиния тоже.
        java.util.Iterator<java.util.Map.Entry<EntityNukeExplosionMK3.ATEntry, Long>> it =
                EntityNukeExplosionMK3.at.entrySet().iterator();
        while (it.hasNext()) {
            java.util.Map.Entry<EntityNukeExplosionMK3.ATEntry, Long> next = it.next();
            if (next.getValue() < level.getGameTime()) {
                it.remove();
                continue;
            }
            EntityNukeExplosionMK3.ATEntry entry = next.getKey();
            if (!entry.dim().equals(level.dimension().location().toString())) continue;
            double dx = x - entry.x(), dy = y - entry.y(), dz = z - entry.z();
            if (Math.sqrt(dx * dx + dy * dy + dz * dz) < 300) {
                entity.discard();
                if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                    for (int i = 0; i < 2; i++) {
                        double ix = i == 0 ? x : (entry.x() + 0.5);
                        double iy = i == 0 ? y : (entry.y() + 0.5);
                        double iz = i == 0 ? z : (entry.z() + 0.5);
                        level.playSound(null, ix, iy, iz, com.hbm_m.sound.HbmSoundsNT.get("entity.ufoBlast"), net.minecraft.sounds.SoundSource.HOSTILE, 15.0F, 0.7F + level.random.nextFloat() * 0.2F);
                        net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
                        data.putString("type", "plasmablast");
                        data.putFloat("r", 0.0F);
                        data.putFloat("g", 0.75F);
                        data.putFloat("b", 1.0F);
                        data.putFloat("scale", 7.5F);
                        com.hbm_m.particle.helper.IParticleCreator.sendPacket(sl, ix, iy, iz, 150, data);
                    }
                }
                break;
            }
        }
        return entity;
    }
}
