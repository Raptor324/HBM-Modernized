package com.hbm_m.entity.missile;

import java.util.List;

import api.hbm_m.entity.IRadarDetectable;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.explosion.MissileWarheadEffects;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Порт {@code EntityMissileAntiBallistic} из 1.7.10.
 *
 * Поведение:
 *   1. Первые {@link #ACTIVATION_TICKS} тиков — чисто вертикальный старт (motionY = BASE_SPEED).
 *   2. После активации — ищет ближайшую баллистическую ракету (кроме стелс и ABM),
 *      ведёт предиктивное наведение и подрывается в радиусе {@link #PROXIMITY_KILL}.
 *   3. Автоудаление: при posY {@literal >} {@link #MAX_ALTITUDE} или ticksExisted {@literal >}
 *      {@link #MAX_LIFETIME} без цели.
 *
 * В отличие от {@link MissileBaseEntity}, ABM не использует баллистику к targetX/targetZ —
 * он управляется собственным наведением в {@link #serverTickLogic()}.
 */
public class MissileABMEntity extends MissileBaseEntity {

    /** Радиус поиска цели (1.7.10: dist = 1_000 в targetMissile). */
    private static final double TARGET_SEARCH_RADIUS = 1_000.0D;
    /** Дистанция подрыва (1.7.10: delta.lengthVector() < 10). */
    private static final double PROXIMITY_KILL = 10.0D;
    /** Тиков до активации наведения (1.7.10: activationTimer < 40). */
    private static final int ACTIVATION_TICKS = 40;
    /** Базовая скорость (1.7.10: baseSpeed = 1.5D). */
    public static final double BASE_SPEED = 1.5D;
    /** Предел ramp скорости (1.7.10: velocity < 6). */
    private static final double MAX_VELOCITY = 6.0D;
    /** Шаг прироста скорости (1.7.10: velocity += 0.1). */
    private static final double VELOCITY_STEP = 0.1D;
    /** Максимальное время жизни без цели, тиков (1.7.10: ticksExisted > 600). */
    private static final int MAX_LIFETIME = 600;
    /** Высота самоуничтожения без цели (1.7.10: posY > 2000). */
    private static final double MAX_ALTITUDE = 2_000.0D;
    /** Сила взрыва при proximity kill (1.7.10: 15F). */
    private static final float PROXIMITY_BLAST = 15.0F;
    /** Сила взрыва при impact (1.7.10: 20F). */
    private static final float IMPACT_BLAST = 20.0F;

    /** Текущая цель наведения. */
    public Entity tracking;
    // ВАЖНО: поле velocity НЕ переопределяется — используется унаследованное из MissileBaseEntity.
    // Причина: base tick() читает именно то поле (mult = this.velocity) для интеграции позиции.
    // Если его перекрыть здесь, base будет читать свой 0, а ABM — свой ramp, и перехватчик
    // замрёт на месте (motion*0). Это точный порт 1.7.10, где motionMult() возвращает velocity.
    /** Счётчик тиков до активации наведения. */
    protected int activationTimer = 0;

    public MissileABMEntity(EntityType<? extends MissileABMEntity> type, Level level) {
        super(type, level);
        this.setDeltaMovement(this.getDeltaMovement().x, BASE_SPEED, this.getDeltaMovement().z);
    }

    public MissileABMEntity(Level level) {
        this(ModEntities.MISSILE_ABM.get(), level);
    }

    @Override
    protected void serverTickLogic() {
        // 1. Ramp скорости (1.7.10: if (velocity < 6) velocity += 0.1).
        if (this.velocity < MAX_VELOCITY) {
            this.velocity += VELOCITY_STEP;
        }

        // 2. Фаза активации: вертикальный старт (1.7.10: if (activationTimer < 40) motionY = baseSpeed).
        if (this.activationTimer < ACTIVATION_TICKS) {
            this.activationTimer++;
            this.setDeltaMovement(this.getDeltaMovement().x, BASE_SPEED, this.getDeltaMovement().z);
            return;
        }

        // 3. Поиск цели, если её нет или она ушла (1.7.10: if (tracking == null || isDead) targetMissile()).
        Entity prevTracking = this.tracking;
        if (this.tracking == null || this.tracking.isRemoved() || !this.tracking.isAlive()) {
            this.targetMissile();
        }

        // 4. При первом захвате — спавн ударной волны (порт ExplosionLarge.spawnShock 24/3F).
        if (prevTracking == null && this.tracking != null && this.level() instanceof ServerLevel server) {
            com.hbm_m.particle.explosions.basic.ExplosionParticleUtils.spawnAirBombShockwave(
                    server, this.getX(), this.getY(), this.getZ());
        }

        // 5. Наведение или самоуничтожение.
        if (this.tracking != null && this.tracking.isAlive() && !this.tracking.isRemoved()) {
            this.aimAtTarget();
        } else {
            // Без цели — истечение времени жизни (1.7.10: if (ticksExisted > 600) setDead()).
            if (this.tickCount > MAX_LIFETIME) {
                releaseChunkTicket();
                this.discard();
            }
        }

        // 6. Высотное самоуничтожение без цели (1.7.10: posY > 2000 && (tracking == null || isDead)).
        if (this.getY() > MAX_ALTITUDE
                && (this.tracking == null || this.tracking.isRemoved() || !this.tracking.isAlive())) {
            releaseChunkTicket();
            this.discard();
        }
    }

    /**
     * Поиск ближайшей баллистической ракеты в радиусе {@link #TARGET_SEARCH_RADIUS}.
     * Порт {@code EntityMissileAntiBallistic.targetMissile()}.
     *
     * Исключения: стелс (canBeDetectedByRadar()==false) и другие ABM (чтобы перехватчики
     * не гонялись друг за другом).
     */
    protected void targetMissile() {
        Entity closest = null;
        double bestDistSq = TARGET_SEARCH_RADIUS * TARGET_SEARCH_RADIUS;

        AABB area = new AABB(
                this.getX() - TARGET_SEARCH_RADIUS, this.getY() - TARGET_SEARCH_RADIUS, this.getZ() - TARGET_SEARCH_RADIUS,
                this.getX() + TARGET_SEARCH_RADIUS, this.getY() + TARGET_SEARCH_RADIUS, this.getZ() + TARGET_SEARCH_RADIUS);

        List<MissileBaseEntity> candidates = this.level().getEntitiesOfClass(MissileBaseEntity.class, area,
                e -> e != this && e.isAlive() && !e.isRemoved());

        for (MissileBaseEntity e : candidates) {
            // Стелс не захватывается (1.7.10: if (e instanceof EntityMissileStealth) continue).
            // ABM не охотится на ABM (по аналогии с EntityMissileStealth исключением).
            if (!e.canBeDetectedByRadar()) {
                continue;
            }
            if (e instanceof MissileABMEntity) {
                continue;
            }

            double dx = e.getX() - this.getX();
            double dy = e.getY() - this.getY();
            double dz = e.getZ() - this.getZ();
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                closest = e;
            }
        }

        this.tracking = closest;
    }

    /**
     * Предиктивное наведение (порт {@code EntityMissileAntiBallistic.aimAtTarget()}).
     *
     * Алгоритм: предсказываем позицию цели на intercept тиков вперёд по её скорости,
     * затем направляем motion к предсказанной точке. На дистанции {@link #PROXIMITY_KILL}
     * подрываемся.
     */
    protected void aimAtTarget() {
        Entity tgt = this.tracking;
        Vec3 delta = new Vec3(tgt.getX() - this.getX(), tgt.getY() - this.getY(), tgt.getZ() - this.getZ());
        double deltaLen = delta.length();

        // Предикция: за сколько тиков ABM достигнет цели (1.7.10: delta.length() / (baseSpeed * velocity)).
        double effSpeed = BASE_SPEED * Math.max(this.velocity, 1.0D);
        double intercept = effSpeed > 0.0D ? deltaLen / effSpeed : 0.0D;

        // Скорость цели: разница позиции с прошлого тика (1.7.10: pos - lastTickPos).
        Vec3 tgtVel = new Vec3(tgt.getX() - tgt.xOld, tgt.getY() - tgt.yOld, tgt.getZ() - tgt.zOld);
        Vec3 predicted = new Vec3(
                tgt.getX() + tgtVel.x * intercept,
                tgt.getY() + tgtVel.y * intercept,
                tgt.getZ() + tgtVel.z * intercept);

        Vec3 toPredicted = new Vec3(predicted.x - this.getX(), predicted.y - this.getY(), predicted.z - this.getZ());
        if (toPredicted.lengthSqr() <= 1.0E-8D) {
            toPredicted = delta;
        }
        Vec3 motion = toPredicted.normalize().scale(BASE_SPEED);

        // Proximity kill: достаточно близко — подрыв (1.7.10: delta < 10 && activationTimer >= 40).
        if (deltaLen < PROXIMITY_KILL && this.activationTimer >= ACTIVATION_TICKS) {
            this.detonateProximity();
            return;
        }

        this.setDeltaMovement(motion);
    }

    /**
     * Подрыв ABM по близости к цели + кинематографичные эффекты уничтожения.
     * Порт {@code ExplosionLarge.explode(world, posX, posY, posZ, 15F, true, false, false)}.
     *
     * Эффекты спавнятся через {@code ServerExplosionParticles.sendAlwaysVisible} —
     * это «фейковый рендер» (тот же, что у contrail-частиц), позволяющий видеть
     * перехват с земли даже когда ABM летит высоко в неб unloaded-чанках.
     */
    protected void detonateProximity() {
        if (this.level().isClientSide) {
            return;
        }
        if (this.level() instanceof ServerLevel server) {
            double x = this.getX();
            double y = this.getY();
            double z = this.getZ();

            server.explode(this, x, y, z, PROXIMITY_BLAST, Level.ExplosionInteraction.BLOCK);

            com.hbm_m.particle.explosions.basic.ExplosionParticleUtils.spawnAirBombSparks(server, x, y, z);
            if (server.getServer() != null) {
                int tick = server.getServer().getTickCount();
                server.getServer().tell(new net.minecraft.server.TickTask(tick + 2, () ->
                        com.hbm_m.particle.explosions.basic.ExplosionParticleUtils.spawnAirBombShockwave(server, x, y, z)));
                server.getServer().tell(new net.minecraft.server.TickTask(tick + 5, () ->
                        com.hbm_m.particle.explosions.basic.ExplosionParticleUtils.spawnAirBombMushroomCloud(server, x, y, z)));
            }

            Vec3 motion = this.getDeltaMovement();
            MissileWarheadEffects.missileDestroyed(server, this,
                    x, y, z, motion,
                    MissileWarheadEffects.defaultDebrisForTier(0), net.minecraft.world.item.ItemStack.EMPTY);
        }
        releaseChunkTicket();
        this.discard();
    }

    @Override
    protected void onMissileImpact(BlockPos pos) {
        if (this.level().isClientSide) {
            return;
        }

        // Порт onImpact: взрыв только после активации (1.7.10: if (activationTimer >= 40)).
        if (this.activationTimer >= ACTIVATION_TICKS && this.level() instanceof ServerLevel server) {
            server.explode(this,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    IMPACT_BLAST, Level.ExplosionInteraction.BLOCK);
        }
    }

    /**
     * ABM наводится на tracking, а не на targetX/targetZ — поэтому yaw/pitch
     * вычисляем из текущего motion (порт onUpdate строки 100-105 в 1.7.10),
     * а не из цели запуска, как базовая баллистическая ракета.
     */
    @Override
    protected void updateRotationFromMotion() {
        // Жестко фиксируем направление вверх (XRot = 0) на фазе старта.
        // Никакие микро-коллизии со стенами шахты больше не положат её на бок!
        if (this.activationTimer < ACTIVATION_TICKS) {
            this.setXRot(0.0F);
            return;
        }

        Vec3 motion = this.getDeltaMovement();
        // Защита от дрейфа после старта
        if (motion.lengthSqr() < 0.001D) {
            return;
        }

        double f2 = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        this.setXRot((float) (Math.atan2(motion.y, f2) * 180.0D / Math.PI) - 90.0F);
        
        if (f2 > 1.0E-5D) {
            this.setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));
        }
    }

    /**
     * ABM не использует баллистику к targetX/targetZ — это заглушка initLaunch.
     * {@link #serverTickLogic()} полностью управляет движением.
     */
    @Override
    public void initLaunch(double x, double y, double z, int targetX, int targetZ) {
        this.setPos(x, y, z);
        this.startX = (int) x;
        this.startZ = (int) z;
        this.targetX = targetX;
        this.targetZ = targetZ;
        this.setDeltaMovement(0.0D, BASE_SPEED, 0.0D);
        this.setYRot(0.0F);
        this.setXRot(0.0F); // 0.0F - строго носом вверх для модели ракеты
        this.xRotO = 0.0F;
        this.yRotO = 0.0F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        // velocity сохраняется базовым классом как "Velocity" — не дублируем.
        tag.putInt("ABMActivation", this.activationTimer);
        if (this.tracking != null) {
            tag.putInt("ABMTrackingId", this.tracking.getId());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.activationTimer = tag.getInt("ABMActivation");
        // tracking восстанавливается лениво в serverTickLogic, если entityID не валиден.
        if (tag.contains("ABMTrackingId") && this.level() != null) {
            Entity e = this.level().getEntity(tag.getInt("ABMTrackingId"));
            if (e instanceof MissileBaseEntity) {
                this.tracking = e;
            }
        }
    }

    @Override
    public IRadarDetectable.RadarTargetType getTargetType() {
        return IRadarDetectable.RadarTargetType.MISSILE_AB;
    }
}
