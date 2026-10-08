package com.hbm_m.entity.logic;

import static com.hbm_m.entity.mob.glyphid.EntityGlyphid.*;

import java.util.List;

import com.hbm_m.config.MobConfig;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.mob.glyphid.EntityGlyphid;
import com.hbm_m.entity.mob.glyphid.EntityGlyphidNuclear;
import com.hbm_m.entity.mob.glyphid.EntityGlyphidScout;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code EntityWaypoint}: unsichtbarer Sammelpunkt der Glyphiden. Glyphiden im Radius uebernehmen alle zwei
 * Sekunden die Aufgabe des Wegpunkts (und erzeugen ggf. den Anschlusswegpunkt); danach verschwindet er, ausser beim
 * Nestbau, wo nur ein Spaeher ihn aufloest. Mit {@code waypointDebug} sieht man ihn als farbige Rauchsaeule.
 */
public class EntityWaypoint extends Entity {

    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(EntityWaypoint.class, EntityDataSerializers.INT);

    public EntityWaypoint(EntityType<? extends EntityWaypoint> type, Level world) {
        super(type, world);
        this.noPhysics = true;
    }

    public EntityWaypoint(Level world) {
        this(ModEntities.WAYPOINT.get(), world);
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        this.entityData.define(TYPE, 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(TYPE, 0);
    }
    *///?}

    @Override
    public boolean fireImmune() {
        return true;
    }

    public int maxAge = 2400;
    public int radius = 3;
    public boolean highPriority = false;
    protected EntityWaypoint additional;

    public void setHighPriority() {
        highPriority = true;
    }

    public int getWaypointType() {
        return this.entityData.get(TYPE);
    }

    public void setAdditionalWaypoint(EntityWaypoint waypoint) {
        additional = waypoint;
    }

    public void setWaypointType(int waypointType) {
        this.entityData.set(TYPE, waypointType);
    }

    boolean hasSpawned = false;

    public int getColor() {
        switch (getWaypointType()) {

            case TASK_RETREAT_FOR_REINFORCEMENTS: return 0x5FA6E8;

            case TASK_BUILD_HIVE:
            case TASK_INITIATE_RETREAT: return 0x127766;

            default: return 0x566573;

        }
    }

    AABB bb;

    @Override
    public void baseTick() {

        if (tickCount >= maxAge) {
            this.discard();
        }

        bb = new AABB(this.getX(), this.getY(), this.getZ(), this.getX(), this.getY(), this.getZ()).inflate(radius);

        if (!level().isClientSide) {

            if (tickCount % 40 == 0) {

                List<Entity> targets = level().getEntities(this, bb);

                for (Entity e : targets) {
                    if (e instanceof EntityGlyphid bug) {

                        if (additional != null && !hasSpawned) {
                            level().addFreshEntity(additional);
                            hasSpawned = true;
                        }

                        boolean exceptions = bug.getWaypoint() != this || e instanceof EntityGlyphidScout || e instanceof EntityGlyphidNuclear;

                        if (!exceptions)
                            bug.setCurrentTask(getWaypointType(), additional);

                        if (getWaypointType() == TASK_BUILD_HIVE) {
                            if (e instanceof EntityGlyphidScout)
                                discard();
                        } else {
                            discard();
                        }

                    }
                }
            }
        } else if (MobConfig.waypointDebug()) {

            double x = bb.minX + (random.nextDouble() - 0.5) * (bb.maxX - bb.minX);
            double y = bb.minY + random.nextDouble() * (bb.maxY - bb.minY);
            double z = bb.minZ + (random.nextDouble() - 0.5) * (bb.maxZ - bb.minZ);

            CompoundTag fx = new CompoundTag();
            fx.putString("type", "tower");
            fx.putFloat("lift", 0.5F);
            fx.putFloat("base", 0.75F);
            fx.putFloat("max", 2F);
            fx.putInt("life", 50 + level().random.nextInt(10));
            fx.putInt("color", getColor());
            fx.putDouble("posX", x);
            fx.putDouble("posY", y);
            fx.putDouble("posZ", z);
            com.hbm_m.particle.helper.ParticleEffectClient.effectNT(fx);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.setWaypointType(nbt.getInt("type"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("type", getWaypointType());
    }
}
