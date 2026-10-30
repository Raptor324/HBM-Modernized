package com.hbm_m.entity.projectile;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.weapon.sedna.BulletConfig;
import com.hbm_m.util.BobMathUtil;
import com.hbm_m.util.MovingObjectPosition;
import com.hbm_m.util.Vec3NT;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityBulletBaseMK4}: Geschoss des SEDNA-Waffensystems. Alles Verhalten kommt aus der
 * {@link BulletConfig} (Geschwindigkeit, Schwerkraft, Lebensdauer, Durchschlag, Abpraller, Treffer-Lambdas); die
 * Konfiguration wird ueber ihre ID synchronisiert, damit der Client sie zeichnen kann. Zielsuchende Geschosse
 * ({@code lockonTarget}) drehen jeden Tick etwas staerker auf ihr Ziel ein.
 */
public class EntityBulletBaseMK4 extends EntityThrowableNT
        //? if forge {
        implements net.minecraftforge.entity.IEntityAdditionalSpawnData
        //?} elif neoforge {
        /*implements net.neoforged.neoforge.entity.IEntityWithComplexSpawn
        *///?}
{

    private static final EntityDataAccessor<Integer> CONFIG = SynchedEntityData.defineId(EntityBulletBaseMK4.class, EntityDataSerializers.INT);

    public BulletConfig config;
    //used for rendering tracers
    public double velocity;
    public double prevVelocity;
    public double accel;
    public float damage;
    public int ricochets = 0;
    @Nullable
    public Entity lockonTarget = null;

    public EntityBulletBaseMK4(EntityType<? extends EntityBulletBaseMK4> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public EntityBulletBaseMK4(Level world) {
        this(ModEntities.BULLET_MK4.get(), world);
    }

    /** For submunitions! */
    public EntityBulletBaseMK4(Level world, @Nullable LivingEntity entity, BulletConfig config, float damage, float gunSpread, double posX, double posY, double posZ, double motionX, double motionY, double motionZ) {
        this(world);
        this.thrower = entity;
        this.setBulletConfig(config);
        this.damage = damage;
        this.moveTo(posX, posY, posZ, 0, 0);
        this.setThrowableHeading(motionX, motionY, motionZ, 1.0F, this.config.spread + gunSpread);
        this.seedVelocity();
    }

    /** For standard guns */
    public EntityBulletBaseMK4(LivingEntity entity, BulletConfig config, float baseDamage, float gunSpread, double sideOffset, double heightOffset, double frontOffset) {
        this(ModEntities.BULLET_MK4.get(), entity, config, baseDamage, gunSpread, sideOffset, heightOffset, frontOffset);
    }

    protected EntityBulletBaseMK4(EntityType<? extends EntityBulletBaseMK4> type, LivingEntity entity, BulletConfig config, float baseDamage, float gunSpread, double sideOffset, double heightOffset, double frontOffset) {
        this(type, entity.level());
        this.thrower = entity;
        this.setBulletConfig(config);
        this.damage = baseDamage * this.config.damageMult;

        float yaw = entity.getYRot();
        float pitch = entity.getXRot();

        // 1. Calculate physical muzzle spawn position (audit DEF-06 / Section 3.3 Patch 1)
        Vec3NT offset = Vec3NT.createVectorHelper(sideOffset, heightOffset, frontOffset);
        offset.rotateAroundX(-pitch / 180F * (float) Math.PI);
        offset.rotateAroundY(-yaw / 180F * (float) Math.PI);
        double spawnX = entity.getX() + offset.xCoord;
        double spawnY = entity.getY() + entity.getEyeHeight() + offset.yCoord;
        double spawnZ = entity.getZ() + offset.zCoord;

        // 2. Compute camera line-of-sight zeroing target (100 blocks or block collider)
        Vec3 eyePos = entity.getEyePosition(1.0F);
        Vec3 lookVec = entity.getViewVector(1.0F);
        double zeroDist = 100.0D;
        Vec3 traceEnd = eyePos.add(lookVec.scale(zeroDist));
        HitResult targetHit = entity.level().clip(new ClipContext(eyePos, traceEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
        Vec3 targetPoint = (targetHit.getType() != HitResult.Type.MISS) ? targetHit.getLocation() : traceEnd;

        // Dynamic entity raycast: ensure convergence on target mobs in close combat
        AABB traceBox = new AABB(eyePos, traceEnd).inflate(1.0D);
        EntityHitResult entityHit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                entity.level(), entity, eyePos, traceEnd, traceBox, e -> !e.isSpectator() && e.isPickable());
        if (entityHit != null) {
            double entityDist = eyePos.distanceTo(entityHit.getLocation());
            if (targetHit.getType() == HitResult.Type.MISS || entityDist < eyePos.distanceTo(targetPoint)) {
                targetPoint = entityHit.getLocation();
            }
        }

        // 3. True firing direction vector from muzzle to target point with point-blank fallback guard
        Vec3 toTarget = targetPoint.subtract(spawnX, spawnY, spawnZ);
        Vec3 fireDir;
        // Directional fallback guard: prevents backward shooting if obstacle/entity is closer than the front offset
        if (toTarget.dot(lookVec) <= 0.05D || eyePos.distanceTo(targetPoint) < 1.0D) {
            fireDir = lookVec;
        } else {
            fireDir = toTarget.normalize();
        }

        // 4. Euler angles in the 1.7.10 Sedna convention (pitch positive up) that BulletRenderers.MK4 rotates by
        float bulletYaw = (float) (Mth.atan2(fireDir.x, fireDir.z) * 180.0D / Math.PI);
        float hyp = (float) Math.sqrt(fireDir.x * fireDir.x + fireDir.z * fireDir.z);
        float bulletPitch = (float) (Mth.atan2(fireDir.y, hyp) * 180.0D / Math.PI);

        this.moveTo(spawnX, spawnY, spawnZ, bulletYaw, bulletPitch);
        this.setThrowableHeading(fireDir.x, fireDir.y, fireDir.z, 1.0F, gunSpread);
        this.seedVelocity();
    }

    /** DEF-10: immediate velocity seeding to eliminate Frame-0 tracer pop-in (velocity was 0.0 on tick 0). */
    private void seedVelocity() {
        Vec3 motion = this.getDeltaMovement();
        this.velocity = motion.length();
        this.prevVelocity = this.velocity;
    }

    /** For turrets - angles are in radians, and pitch is negative! */
    public EntityBulletBaseMK4(Level world, BulletConfig config, float baseDamage, float gunSpread, float yaw, float pitch) {
        this(world);
        this.setBulletConfig(config);
        this.damage = baseDamage * this.config.damageMult;
        float y = yaw * 180F / (float) Math.PI;
        float p = -pitch * 180F / (float) Math.PI;
        this.setYRot(y); this.yRotO = y;
        this.setXRot(p); this.xRotO = p;
        double mx = -Mth.sin(y / 180.0F * (float) Math.PI) * Mth.cos(p / 180.0F * (float) Math.PI);
        double mz = Mth.cos(y / 180.0F * (float) Math.PI) * Mth.cos(p / 180.0F * (float) Math.PI);
        double my = -Mth.sin(p / 180.0F * (float) Math.PI);
        this.setThrowableHeading(mx, my, mz, 1.0F, gunSpread);
        this.seedVelocity();
    }

    //? if < 1.21.1 {
    @Override
    protected void defineExtraData() {
        this.entityData.define(CONFIG, 0);
    }
    //?} else {
    /*@Override
    protected void defineExtraData(SynchedEntityData.Builder builder) {
        builder.define(CONFIG, 0);
    }
    *///?}

    public void setBulletConfig(BulletConfig config) {
        this.config = config;
        this.entityData.set(CONFIG, config.id);
    }

    @Nullable
    public BulletConfig getBulletConfig() {
        int id = this.entityData.get(CONFIG);
        if (id < 0 || id >= BulletConfig.configs.size()) return null;
        return BulletConfig.configs.get(id);
    }

    @Override
    public void tick() {
        if (config == null) config = this.getBulletConfig();
        if (config == null) {
            this.discard();
            return;
        }

        double px = getX(), py = getY(), pz = getZ();
        super.tick();
        double dX = this.getX() - px;
        double dY = this.getY() - py;
        double dZ = this.getZ() - pz;

        if (!this.inGround && this.lockonTarget != null && this.lockonTarget.isAlive()) {
            Vec3 m = getDeltaMovement();
            Vec3NT motion = new Vec3NT(m.x, m.y, m.z);
            double vel = motion.lengthVector();
            Vec3NT delta = new Vec3NT(lockonTarget.getX() - getX(), lockonTarget.getY() + lockonTarget.getBbHeight() / 2D - getY(), lockonTarget.getZ() - getZ());
            float turn = Math.min(0.005F * this.tickCount, 1F);
            Vec3NT newVec = new Vec3NT(
                    BobMathUtil.interp(motion.xCoord, delta.xCoord, turn),
                    BobMathUtil.interp(motion.yCoord, delta.yCoord, turn),
                    BobMathUtil.interp(motion.zCoord, delta.zCoord, turn)).normalize();
            this.setDeltaMovement(newVec.xCoord * vel, newVec.yCoord * vel, newVec.zCoord * vel);
            this.hasImpulse = true;
        }

        this.prevVelocity = this.velocity;
        this.velocity = Math.sqrt(dX * dX + dY * dY + dZ * dZ);

        if (!this.inGround && !this.onGround() && velocity > 0 && (!level().isClientSide || !this.isClientRotationLocked())) {
            float hyp = Mth.sqrt((float) (dX * dX + dZ * dZ));
            // 1.7.10 convention, consistent with setThrowableHeading
            this.setYRot((float) (Math.atan2(dX, dZ) * 180.0D / Math.PI));
            this.setXRot((float) (Math.atan2(dY, hyp) * 180.0D / Math.PI));
            while (this.getXRot() - this.xRotO < -180.0F) this.xRotO -= 360.0F;
            while (this.getXRot() - this.xRotO >= 180.0F) this.xRotO += 360.0F;
            while (this.getYRot() - this.yRotO < -180.0F) this.yRotO -= 360.0F;
            while (this.getYRot() - this.yRotO >= 180.0F) this.yRotO += 360.0F;
        }

        if (!level().isClientSide && this.tickCount > config.expires) this.discard();
        if (this.config.onUpdate != null) this.config.onUpdate.accept(this);
    }

    /** Original: der Haken der Seilwinde ({@code XFactoryTool.ct_hook}) dreht clientseitig nicht selbst. */
    protected boolean isClientRotationLocked() {
        return BulletConfig.clientRotationLocked != null && BulletConfig.clientRotationLocked.test(this.config);
    }

    @Override
    protected void onImpact(HitResult hit) {
        if (!level().isClientSide) {
            MovingObjectPosition mop = MovingObjectPosition.of(hit);
            if (mop == null) return;
            if (this.config.onImpact != null) this.config.onImpact.accept(this, mop);
            if (this.isRemoved() || this.inGround) return;
            if (this.config.onRicochet != null) this.config.onRicochet.accept(this, mop);
            if (this.config.onEntityHit != null) this.config.onEntityHit.accept(this, mop);
        }
    }

    /** Original {@code setPosition} + {@code posX/Y/Z}: Treffer-Lambdas setzen das Geschoss auf den Trefferpunkt. */
    public void setPosition(double x, double y, double z) {
        this.setPos(x, y, z);
    }

    /** Original {@code setDead()}. */
    public void setDead() {
        this.discard();
    }

    public boolean isDead() {
        return this.isRemoved();
    }

    @Override protected double headingForceMult() { return 1D; }
    @Override public double getGravityVelocity() { return this.config.gravity; }
    @Override protected double motionMult() { return this.config.velocity + this.accel; }
    @Override protected float getAirDrag() { return 1F; }
    @Override protected float getWaterDrag() { return 1F; }
    @Override public boolean doesImpactEntities() { return this.config.impactsEntities; }
    @Override public boolean doesPenetrate() { return this.config.doesPenetrate; }
    @Override public boolean isSpectral() { return this.config.isSpectral; }
    @Override public int selfDamageDelay() { return this.config.selfDamageDelay; }
    @Override public boolean displayFireAnimation() { return false; }
    @Override public boolean fireImmune() { return true; }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    //? if forge {
    @Override
    public void writeSpawnData(FriendlyByteBuf buf) {
        buf.writeInt(this.thrower != null ? thrower.getId() : -1);
        buf.writeInt(this.config != null ? this.config.id : 0);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf) {
        Entity e = level().getEntity(buf.readInt());
        if (e instanceof LivingEntity living) this.thrower = living;
        int id = buf.readInt();
        if (id >= 0 && id < BulletConfig.configs.size()) this.config = BulletConfig.configs.get(id);
        // DEF-10: seed tracer velocity immediately so the tracer renders on frame 0
        this.seedVelocity();
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
    //?} elif neoforge {
    /*// NeoForge: IEntityWithComplexSpawn (spawn packet is extended automatically).
    // 1.21.1 vanilla no longer transmits motion in the spawn packet (1.20.1 ClientPacketListener
    // called lerpMotion(Xa/Ya/Za); 1.21.1 recreateFromPacket does not set velocity) — otherwise the
    // client bullet stands still, velocity=0 and the tracer never renders (renderBulletStandard: len<=0).
    @Override
    public void writeSpawnData(net.minecraft.network.RegistryFriendlyByteBuf buf) {
        buf.writeInt(this.thrower != null ? thrower.getId() : -1);
        buf.writeInt(this.config != null ? this.config.id : 0);
        Vec3 m = this.getDeltaMovement();
        buf.writeDouble(m.x);
        buf.writeDouble(m.y);
        buf.writeDouble(m.z);
    }

    @Override
    public void readSpawnData(net.minecraft.network.RegistryFriendlyByteBuf buf) {
        Entity e = level().getEntity(buf.readInt());
        if (e instanceof LivingEntity living) this.thrower = living;
        int id = buf.readInt();
        if (id >= 0 && id < BulletConfig.configs.size()) this.config = BulletConfig.configs.get(id);
        this.lerpMotion(buf.readDouble(), buf.readDouble(), buf.readDouble());
        // DEF-10: seed tracer velocity immediately so the tracer renders on frame 0
        this.seedVelocity();
    }
    *///?}
}
