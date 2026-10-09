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
import net.minecraft.world.level.Level;
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

        Vec3NT offset = Vec3NT.createVectorHelper(sideOffset, heightOffset, frontOffset);
        offset.rotateAroundX(-pitch / 180F * (float) Math.PI);
        offset.rotateAroundY(-yaw / 180F * (float) Math.PI);
        this.moveTo(entity.getX() + offset.xCoord, entity.getY() + entity.getEyeHeight() + offset.yCoord, entity.getZ() + offset.zCoord, yaw, pitch);

        double mx = -Mth.sin(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI);
        double mz = Mth.cos(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI);
        double my = -Mth.sin(pitch / 180.0F * (float) Math.PI);
        this.setThrowableHeading(mx, my, mz, 1.0F, gunSpread);
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
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
    //?} elif neoforge {
    /*// NeoForge: IEntityWithComplexSpawn (Spawn-Paket wird automatisch erweitert)
    @Override
    public void writeSpawnData(net.minecraft.network.RegistryFriendlyByteBuf buf) {
        buf.writeInt(this.thrower != null ? thrower.getId() : -1);
        buf.writeInt(this.config != null ? this.config.id : 0);
    }

    @Override
    public void readSpawnData(net.minecraft.network.RegistryFriendlyByteBuf buf) {
        Entity e = level().getEntity(buf.readInt());
        if (e instanceof LivingEntity living) this.thrower = living;
        int id = buf.readInt();
        if (id >= 0 && id < BulletConfig.configs.size()) this.config = BulletConfig.configs.get(id);
    }
    *///?}
}
