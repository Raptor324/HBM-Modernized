package com.hbm_m.entity.projectile;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.weapon.sedna.BulletConfig;
import com.hbm_m.util.MovingObjectPosition;
import com.hbm_m.util.Vec3NT;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityBulletBeamBase}: Strahlwaffen-Schuss als Sofort-Treffer (Hitscan, 250 Bloecke). Trifft der Strahl
 * eine geworfene Muenze ({@link EntityCoin}), wird er mit 125 % Schaden auf das naechste Ziel umgelenkt - Muenzen vor
 * Spielern vor Monstern vor sonstigen Lebewesen. Das Entity selbst existiert nur fuer die Darstellung.
 */
public class EntityBulletBeamBase extends Entity
        //? if forge {
        implements net.minecraftforge.entity.IEntityAdditionalSpawnData
        //?} elif neoforge {
        /*implements net.neoforged.neoforge.entity.IEntityWithComplexSpawn
        *///?}
{

    private static final EntityDataAccessor<Integer> CONFIG = SynchedEntityData.defineId(EntityBulletBeamBase.class, EntityDataSerializers.INT);

    @Nullable
    public LivingEntity thrower;
    public BulletConfig config;
    public float damage;
    public double headingX;
    public double headingY;
    public double headingZ;
    public double beamLength;

    public EntityBulletBeamBase(EntityType<? extends EntityBulletBeamBase> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    @Nullable
    public LivingEntity getThrower() { return this.thrower; }

    public EntityBulletBeamBase(Level world, BulletConfig config, float baseDamage) {
        this(ModEntities.BULLET_BEAM.get(), world);
        this.setBulletConfig(config);
        this.damage = baseDamage * this.config.damageMult;
    }

    public EntityBulletBeamBase(LivingEntity entity, BulletConfig config, float baseDamage) {
        this(entity.level(), config, baseDamage);
        this.thrower = entity;
    }

    public EntityBulletBeamBase(LivingEntity entity, BulletConfig config, float baseDamage, float angularInaccuracy, double sideOffset, double heightOffset, double frontOffset) {
        this(ModEntities.BULLET_BEAM.get(), entity.level());
        this.thrower = entity;
        this.setBulletConfig(config);
        this.damage = baseDamage * this.config.damageMult;
        float yaw = entity.getYRot() + (float) random.nextGaussian() * angularInaccuracy;
        float pitch = entity.getXRot() + (float) random.nextGaussian() * angularInaccuracy;

        Vec3NT offset = Vec3NT.createVectorHelper(sideOffset, heightOffset, frontOffset);
        offset.rotateAroundX(-pitch / 180F * (float) Math.PI);
        offset.rotateAroundY(-yaw / 180F * (float) Math.PI);
        this.moveTo(entity.getX() + offset.xCoord, entity.getY() + entity.getEyeHeight() + offset.yCoord, entity.getZ() + offset.zCoord, yaw, pitch);

        this.headingX = -Mth.sin(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI);
        this.headingZ = Mth.cos(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI);
        this.headingY = -Mth.sin(pitch / 180.0F * (float) Math.PI);

        double range = 250D;
        this.headingX *= range;
        this.headingY *= range;
        this.headingZ *= range;

        performHitscan();
    }

    public void setRotationsFromVector(Vec3NT delta) {
        float pitch = (float) (-Math.asin(delta.yCoord / delta.lengthVector()) * 180D / Math.PI);
        float yaw = (float) (-Math.atan2(delta.xCoord, delta.zCoord) * 180D / Math.PI);
        this.setXRot(pitch);
        this.setYRot(yaw);
        this.headingX = -Mth.sin(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI);
        this.headingZ = Mth.cos(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI);
        this.headingY = -Mth.sin(pitch / 180.0F * (float) Math.PI);
    }

    public void performHitscanExternal(double range) {
        this.headingX *= range;
        this.headingY *= range;
        this.headingZ *= range;
        performHitscan();
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        this.entityData.define(CONFIG, 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
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
        if (config.onUpdate != null) config.onUpdate.accept(this);
        super.tick();
        if (!level().isClientSide && this.tickCount > config.expires) this.discard();
    }

    protected void performHitscan() {
        Vec3 pos = new Vec3(getX(), getY(), getZ());
        Vec3 nextPos = new Vec3(getX() + this.headingX, getY() + this.headingY, getZ() + this.headingZ);
        MovingObjectPosition mop = null;
        if (!this.isSpectral()) {
            BlockHitResult bhr = level().clip(new ClipContext(pos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (bhr.getType() != HitResult.Type.MISS) mop = MovingObjectPosition.of(bhr);
        }

        if (mop != null) nextPos = mop.hitVec3();

        if (!level().isClientSide && this.doesImpactEntities()) {

            Entity hitEntity = null;
            List<Entity> list = level().getEntities(this, this.getBoundingBox().expandTowards(this.headingX, this.headingY, this.headingZ).inflate(1.0D));
            double nearest = 0.0D;
            Vec3 nonPenImpact = null;
            Vec3 coinHit = null;
            double closestCoin = 0;
            EntityCoin hitCoin = null;

            for (Entity entity : list) {
                if (!entity.isAlive()) continue;
                if (entity instanceof EntityCoin coin) {
                    AABB aabb = entity.getBoundingBox().inflate(0.3D);
                    var hitMop = aabb.clip(pos, nextPos);
                    if (hitMop.isPresent()) {
                        double dist = pos.distanceTo(hitMop.get());
                        if (closestCoin == 0 || dist < closestCoin) {
                            closestCoin = dist;
                            hitCoin = coin;
                            coinHit = hitMop.get();
                        }
                    }
                }
            }

            for (Entity entity : list) {
                if (entity.isPickable() && entity != thrower && entity.isAlive()) {
                    AABB aabb = entity.getBoundingBox().inflate(0.3D);
                    var hitMop = aabb.clip(pos, nextPos);
                    if (hitMop.isPresent()) {
                        double dist = pos.distanceTo(hitMop.get());
                        // if penetration is enabled, run impact for all intersecting entities
                        if (this.doesPenetrate()) {
                            if (hitCoin == null || dist < closestCoin) {
                                this.onImpact(new MovingObjectPosition(entity, new Vec3NT(hitMop.get())));
                            }
                        } else {
                            if (dist < nearest || nearest == 0.0D) {
                                hitEntity = entity;
                                nearest = dist;
                                nonPenImpact = hitMop.get();
                            }
                        }
                    }
                }
            }

            // if not, only run it for the closest MOP
            if (!this.doesPenetrate() && hitEntity != null) {
                mop = new MovingObjectPosition(hitEntity, new Vec3NT(nonPenImpact));
            }

            if (hitCoin != null) {
                Vec3 vec = new Vec3(coinHit.x - getX(), coinHit.y - getY(), coinHit.z - getZ());
                this.beamLength = vec.length();

                double range = 50;
                List<Entity> targets = level().getEntitiesOfClass(Entity.class, new AABB(coinHit, coinHit).inflate(range));
                Entity nearestCoin = null;
                Entity nearestPlayer = null;
                Entity nearestMob = null;
                Entity nearestOther = null;
                double coinDist = 0;
                double playerDist = 0;
                double mobDist = 0;
                double otherDist = 0;

                hitCoin.discard();

                for (Entity entity : targets) {
                    if (entity == this.getThrower()) continue;
                    if (!entity.isAlive()) continue;
                    double dist = entity.distanceTo(hitCoin);
                    if (dist > range) continue;

                    if (entity instanceof EntityCoin) {
                        if (coinDist == 0 || dist < coinDist) { coinDist = dist; nearestCoin = entity; }
                    } else if (entity instanceof Player) {
                        if (playerDist == 0 || dist < playerDist) { playerDist = dist; nearestPlayer = entity; }
                    } else if (entity instanceof Enemy) {
                        if (mobDist == 0 || dist < mobDist) { mobDist = dist; nearestMob = entity; }
                    } else if (entity instanceof LivingEntity) {
                        if (otherDist == 0 || dist < otherDist) { otherDist = dist; nearestOther = entity; }
                    }
                }

                // ternary of shame
                Entity target = nearestCoin != null ? nearestCoin :
                        nearestPlayer != null ? nearestPlayer :
                        nearestMob != null ? nearestMob :
                        nearestOther;

                LivingEntity shooter = hitCoin.getThrower() != null ? hitCoin.getThrower() : this.thrower;
                EntityBulletBeamBase newBeam = shooter != null ? new EntityBulletBeamBase(shooter, this.config, this.damage * 1.25F) : new EntityBulletBeamBase(level(), this.config, this.damage * 1.25F);
                newBeam.setPos(coinHit.x, coinHit.y, coinHit.z);
                if (target != null) {
                    newBeam.setRotationsFromVector(Vec3NT.createVectorHelper(target.getX() - newBeam.getX(), (target.getY() + target.getBbHeight() / 2D) - newBeam.getY(), target.getZ() - newBeam.getZ()));
                } else {
                    newBeam.setRotationsFromVector(Vec3NT.createVectorHelper(random.nextGaussian() * 0.5, -1, random.nextGaussian() * 0.5));
                }
                newBeam.performHitscanExternal(250D);
                level().addFreshEntity(newBeam);

                if (level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.EXPLOSION, coinHit.x, coinHit.y, coinHit.z, 1, 0, 0, 0, 0);
                }
                return;
            }
        }

        if (mop != null) {
            this.onImpact(mop);
            this.beamLength = new Vec3(mop.hitVec.xCoord - getX(), mop.hitVec.yCoord - getY(), mop.hitVec.zCoord - getZ()).length();
        } else {
            this.beamLength = new Vec3(nextPos.x - getX(), nextPos.y - getY(), nextPos.z - getZ()).length();
        }
    }

    protected void onImpact(MovingObjectPosition mop) {
        if (!level().isClientSide) {
            if (this.config.onImpactBeam != null) this.config.onImpactBeam.accept(this, mop);
        }
    }

    public boolean doesImpactEntities() { return this.config.impactsEntities; }
    public boolean doesPenetrate() { return this.config.doesPenetrate; }
    public boolean isSpectral() { return this.config.isSpectral; }

    /** Original {@code setDead()}. */
    public void setDead() { this.discard(); }

    @Override protected void addAdditionalSaveData(CompoundTag nbt) { }
    @Override protected void readAdditionalSaveData(CompoundTag nbt) { this.discard(); }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean displayFireAnimation() { return false; }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return true;
    }

    //? if forge {
    @Override
    public void writeSpawnData(FriendlyByteBuf buf) {
        buf.writeDouble(beamLength);
        buf.writeFloat(getYRot());
        buf.writeFloat(getXRot());
        buf.writeInt(this.config != null ? this.config.id : 0);
        buf.writeInt(this.thrower != null ? this.thrower.getId() : -1);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf buf) {
        this.beamLength = buf.readDouble();
        this.setYRot(buf.readFloat());
        this.setXRot(buf.readFloat());
        this.yRotO = getYRot();
        this.xRotO = getXRot();
        int id = buf.readInt();
        if (id >= 0 && id < BulletConfig.configs.size()) this.config = BulletConfig.configs.get(id);
        Entity e = level().getEntity(buf.readInt());
        if (e instanceof LivingEntity living) this.thrower = living;
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket() {
        return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);
    }
    //?} elif neoforge {
    /*// NeoForge: IEntityWithComplexSpawn (Spawn-Paket wird automatisch erweitert)
    @Override
    public void writeSpawnData(net.minecraft.network.RegistryFriendlyByteBuf buf) {
        buf.writeDouble(beamLength);
        buf.writeFloat(getYRot());
        buf.writeFloat(getXRot());
        buf.writeInt(this.config != null ? this.config.id : 0);
        buf.writeInt(this.thrower != null ? this.thrower.getId() : -1);
    }

    @Override
    public void readSpawnData(net.minecraft.network.RegistryFriendlyByteBuf buf) {
        this.beamLength = buf.readDouble();
        this.setYRot(buf.readFloat());
        this.setXRot(buf.readFloat());
        this.yRotO = getYRot();
        this.xRotO = getXRot();
        int id = buf.readInt();
        if (id >= 0 && id < BulletConfig.configs.size()) this.config = BulletConfig.configs.get(id);
        Entity e = level().getEntity(buf.readInt());
        if (e instanceof LivingEntity living) this.thrower = living;
    }
    *///?}
}
