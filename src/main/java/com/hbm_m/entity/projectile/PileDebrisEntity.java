package com.hbm_m.entity.projectile;

import com.hbm_m.entity.ModEntities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 1:1-Port von {@code TileEntityPileCore.pile_debris}: der brennende Graphitbrocken, den ein
 * durchgehender Uranmeiler beim Zerbersten in die Luft schleudert.
 *
 * <p>Im Original ist das keine eigene Klasse, sondern eine {@code BulletConfig} auf
 * {@code EntityBulletBaseMK4} - dieses Geschosssystem gibt es in diesem Port nicht, die Werte sind
 * darum hier fest eingetragen: {@code setLife(200).setVel(1F).setGrav(0.1D)}, dazu der
 * Aufprallschaden 100 und die Trefferbreite 0,35 aus dem Aufruf in {@code handleMeltdown}.</p>
 *
 * <p>Fuenfzehn davon steigen aus der Mitte der Brennstoffkanaele auf. Jeder brennt im Flug
 * ({@code LAMBDA_FIRE}) und reisst beim Aufschlag ein Loch von fuenf Bloecken
 * ({@code LAMBDA_STANDARD_EXPLODE}) - das ist der Grund, warum ein geschmolzener Meiler nicht nur
 * ein Loch hinterlaesst, sondern eine ganze zerpfluegte Landschaft.</p>
 */
public class PileDebrisEntity extends Entity {

    /** Original: {@code setLife(200)}. */
    public static final int MAX_AGE = 200;
    /** Original: {@code setGrav(0.1D)}. */
    public static final double GRAVITY = 0.1D;
    /** Original: der Schadenswert im Konstruktoraufruf. */
    public static final float DAMAGE = 100F;
    /** Original: {@code LAMBDA_STANDARD_EXPLODE} sprengt mit Staerke 5, ohne Feuer zu legen. */
    public static final float EXPLOSION_STRENGTH = 5F;

    /** Nur zur Anzeige: der Brocken taumelt im Flug. */
    public float rot = 0F;
    public float lastRot = 0F;

    private int age = 0;

    public PileDebrisEntity(EntityType<? extends PileDebrisEntity> type, Level level) {
        super(type, level);
        this.rot = this.random.nextFloat() * 360F;
        this.lastRot = this.rot;
    }

    public static PileDebrisEntity create(Level level, double x, double y, double z,
                                          double mX, double mY, double mZ) {
        PileDebrisEntity debris = new PileDebrisEntity(ModEntities.PILE_DEBRIS.get(), level);
        debris.setPos(x, y, z);
        debris.setDeltaMovement(mX, mY, mZ);
        return debris;
    }

    //? if < 1.21.1 {

    @Override
    protected void defineSynchedData() { }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { }
    *///?}

    @Override
    public void tick() {
        this.lastRot = this.rot;
        this.rot += 15F;
        if (this.rot >= 360F) {
            this.rot -= 360F;
            this.lastRot -= 360F;
        }

        Vec3 motion = getDeltaMovement();
        Vec3 start = position();
        Vec3 end = start.add(motion);

        // Original: LAMBDA_FIRE - der Brocken brennt, solange er fliegt.
        if (level().isClientSide()) {
            level().addParticle(ParticleTypes.FLAME, getX(), getY() - 0.125D, getZ(), 0D, 0D, 0D);
            level().addParticle(ParticleTypes.SMOKE, getX(), getY() - 0.125D, getZ(), 0D, 0D, 0D);
        }

        // Trifft er einen Block, endet der Flug dort.
        BlockHitResult blockHit = level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        if (blockHit.getType() != HitResult.Type.MISS) end = blockHit.getLocation();

        // Und alles Lebende auf dem Weg dorthin bekommt die vollen hundert ab.
        LivingEntity struck = firstEntityOnPath(start, end);

        setPos(end.x, end.y, end.z);

        if (struck != null) {
            //? if < 1.21.1 {
            struck.hurt(damageSources().explosion(this, null), DAMAGE);
            //?} else {
            /*struck.hurt((net.minecraft.server.level.ServerLevel) level(),
                    damageSources().explosion(this, null), DAMAGE);
            *///?}
            impact();
            return;
        }

        if (blockHit.getType() != HitResult.Type.MISS) {
            impact();
            return;
        }

        setDeltaMovement(motion.x, motion.y - GRAVITY, motion.z);

        if (!level().isClientSide()) {
            age++;
            if (age > MAX_AGE) discard();
        }
    }

    /** Der erste Lebende, dessen Trefferkasten die Flugbahn dieses Ticks schneidet. */
    private LivingEntity firstEntityOnPath(Vec3 start, Vec3 end) {
        AABB path = getBoundingBox().expandTowards(end.subtract(start)).inflate(0.35D);
        List<LivingEntity> candidates = level().getEntitiesOfClass(LivingEntity.class, path,
                entity -> entity.isAlive() && entity.isPickable());

        LivingEntity closest = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity entity : candidates) {
            double dist = entity.position().distanceToSqr(start);
            if (dist < closestDist) {
                closest = entity;
                closestDist = dist;
            }
        }
        return closest;
    }

    /** Original: {@code LAMBDA_STANDARD_EXPLODE} - Staerke 5, Bloecke zerbrechen, kein Feuer. */
    private void impact() {
        if (!level().isClientSide()) {
            level().explode(this, getX(), getY(), getZ(), EXPLOSION_STRENGTH, false,
                    Level.ExplosionInteraction.BLOCK);
        }
        discard();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        age = tag.getInt("age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("age", age);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
