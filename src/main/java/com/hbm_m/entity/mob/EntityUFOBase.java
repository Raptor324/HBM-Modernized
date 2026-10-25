package com.hbm_m.entity.mob;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityUFOBase}: Flugkoerper mit Wegpunkt (synchronisiert), Spielersuche alle {@link #getScanDelay} Ticks
 * im Radius {@link #getScanRange} und Kursplanung um das Ziel herum. Kindklassen rufen in
 * {@link #customServerAiStep} zuerst {@code super} auf (setzt die Bewegung auf 0 und plant).
 */
public abstract class EntityUFOBase extends FlyingMob implements Enemy {

    private static final EntityDataAccessor<Integer> WX = SynchedEntityData.defineId(EntityUFOBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WY = SynchedEntityData.defineId(EntityUFOBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WZ = SynchedEntityData.defineId(EntityUFOBase.class, EntityDataSerializers.INT);

    protected int scanCooldown;
    protected int courseChangeCooldown;
    @Nullable protected Entity target;

    protected EntityUFOBase(EntityType<? extends EntityUFOBase> type, Level world) {
        super(type, world);
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        //XYZ
        this.entityData.define(WX, 0);
        this.entityData.define(WY, 0);
        this.entityData.define(WZ, 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        //XYZ
        builder.define(WX, 0);
        builder.define(WY, 0);
        builder.define(WZ, 0);
    }
    *///?}

    @Override
    protected void customServerAiStep() {

        if (level().getDifficulty() == Difficulty.PEACEFUL) {
            this.discard();
            return;
        }

        this.setDeltaMovement(Vec3.ZERO);

        if (this.target != null && !this.target.isAlive()) {
            this.target = null;
        }

        scanForTarget();

        if (this.courseChangeCooldown <= 0) {
            this.setCourse();
        }
    }

    /** Naechster Spieler (nicht kreativ, nicht unsichtbar), Pruefung nur alle {@link #getScanDelay} Ticks. */
    protected void scanForTarget() {

        int range = getScanRange();

        if (this.scanCooldown <= 0) {
            List<Entity> entities = level().getEntities(this, this.getBoundingBox().inflate(range, range / 2, range));
            this.target = null;

            for (Entity entity : entities) {

                if (!entity.isAlive())
                    continue;

                if (entity instanceof Player player) {

                    if (player.isCreative() || player.isSpectator())
                        continue;

                    if (player.hasEffect(MobEffects.INVISIBILITY))
                        continue;

                    if (this.target == null) {
                        this.target = entity;
                    } else {
                        if (this.distanceToSqr(entity) < this.distanceToSqr(this.target)) {
                            this.target = entity;
                        }
                    }
                }
            }

            this.scanCooldown = getScanDelay();
        }
    }

    protected int getScanRange() {
        return 50;
    }

    protected int getScanDelay() {
        return 100;
    }

    protected boolean isCourseTraversable(double x, double y, double z, double dist) {

        double d4 = (this.getWaypointX() - this.getX()) / dist;
        double d5 = (this.getWaypointY() - this.getY()) / dist;
        double d6 = (this.getWaypointZ() - this.getZ()) / dist;
        AABB axisalignedbb = this.getBoundingBox();

        for (int i = 1; i < dist; ++i) {
            axisalignedbb = axisalignedbb.move(d4, d5, d6);

            if (!level().noCollision(this, axisalignedbb)) {
                return false;
            }
        }

        return true;
    }

    protected void approachPosition(double speed) {

        double deltaX = this.getWaypointX() - this.getX();
        double deltaY = this.getWaypointY() - this.getY();
        double deltaZ = this.getWaypointZ() - this.getZ();
        Vec3 delta = new Vec3(deltaX, deltaY, deltaZ);
        double len = delta.length();

        if (len > 5) {
            if (this.isCourseTraversable(this.getWaypointX(), this.getWaypointY(), this.getWaypointZ(), len)) {
                this.setDeltaMovement(delta.x * speed / len, delta.y * speed / len, delta.z * speed / len);
            } else {
                this.courseChangeCooldown = 0;
            }
        }
    }

    protected void setCourse() {

        if (this.target != null) {
            this.setCourseForTaget();
            this.courseChangeCooldown = 20 + random.nextInt(20);
        } else {
            this.setCourseWithoutTaget();
            this.courseChangeCooldown = 60 + random.nextInt(20);
        }
    }

    protected void setCourseForTaget() {
        Vec3 vec = new Vec3(this.getX() - this.target.getX(), 0, this.getZ() - this.target.getZ()).yRot((float) Math.PI * 2 * random.nextFloat());

        double length = vec.length();
        double overshoot = 10 + random.nextDouble() * 10;

        int wX = (int) Math.floor(this.target.getX() - vec.x / length * overshoot);
        int wZ = (int) Math.floor(this.target.getZ() - vec.z / length * overshoot);

        this.setWaypoint(wX, Math.max(height(wX, wZ), (int) this.target.getY()) + targetHeightOffset(), wZ);
    }

    protected int targetHeightOffset() {
        return 2 + random.nextInt(2);
    }

    protected int wanderHeightOffset() {
        return 2 + random.nextInt(3);
    }

    protected void setCourseWithoutTaget() {
        int x = (int) Math.floor(getX() + random.nextGaussian() * 5);
        int z = (int) Math.floor(getZ() + random.nextGaussian() * 5);
        this.setWaypoint(x, height(x, z) + wanderHeightOffset(), z);
    }

    private int height(int x, int z) {
        return level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    public void setWaypoint(int x, int y, int z) {
        this.entityData.set(WX, x);
        this.entityData.set(WY, y);
        this.entityData.set(WZ, z);
    }

    public int getWaypointX() {
        return this.entityData.get(WX);
    }

    public int getWaypointY() {
        return this.entityData.get(WY);
    }

    public int getWaypointZ() {
        return this.entityData.get(WZ);
    }
}
