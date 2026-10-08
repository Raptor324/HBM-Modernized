package com.hbm_m.entity.mob.siege;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.entity.mob.EntityUFOBase;
import com.hbm_m.entity.projectile.EntitySiegeLaser;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.ContaminationUtil;
import com.hbm_m.util.ContaminationUtil.ContaminationType;
import com.hbm_m.util.ContaminationUtil.HazardType;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code com.hbm.entity.mob.siege.EntitySiegeCraft}: Belagerungsschiff (7x1). Feuert alle 30-40 Ticks einen Faecher
 * aus sieben {@link EntitySiegeLaser}n (Farbe nach Restleben) und hat einen 300-Tick-Strahlzyklus: Zielerfassung
 * (120-60), Markierung (100-40), Strahl auf den fixierten Punkt (40-0) mit 1000 Schaden, Feuer und Strahlung.
 * Panzerung/Leben/Schaden kommen aus der {@link SiegeTier}.
 *
 * <p>Im Original ist die Einheit nicht in {@code EntityMappings} eingetragen (nur der Laser) und damit nicht
 * spawnbar; im Port ist sie registriert (z.B. per /summon), ohne Spawn-Ei. Die Bosslebensleiste zeigt das Original
 * nicht an (im Renderer auskommentiert), daher auch hier nicht.</p>
 */
public class EntitySiegeCraft extends EntityUFOBase {

    private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(EntitySiegeCraft.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LOCK_X = SynchedEntityData.defineId(EntitySiegeCraft.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LOCK_Y = SynchedEntityData.defineId(EntitySiegeCraft.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> LOCK_Z = SynchedEntityData.defineId(EntitySiegeCraft.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> BEAM = SynchedEntityData.defineId(EntitySiegeCraft.class, EntityDataSerializers.BOOLEAN);

    private int attackCooldown;
    private int beamCountdown;

    public EntitySiegeCraft(EntityType<? extends EntitySiegeCraft> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return FlyingMob.createMobAttributes();
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    /** {@code EntityLiving.canAttackClass}: alles ausser Creeper und Ghast. */
    protected static boolean canAttackClass(Entity e) {
        return e.getClass() != Creeper.class && e.getClass() != Ghast.class;
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {

        if (this.isInvulnerableTo(source))
            return false;

        SiegeTier tier = this.getTier();

        if (tier.fireProof && source.is(DamageTypeTags.IS_FIRE)) {
            this.clearFire();
            return false;
        }

        // noFF: von anderen Mobs nicht verletzbar
        if (tier.noFriendlyFire && source.getEntity() != null && !(source.getEntity() instanceof Player))
            return false;

        damage -= tier.dt;

        if (damage < 0) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_BREAK, SoundSource.HOSTILE, 5F, 1.0F + random.nextFloat() * 0.5F);
            return false;
        }

        damage *= (1F - tier.dr);

        return super.hurt(source, damage);
    }

    @Override
    protected void tickDeath() {

        this.beamCountdown = 200;
        this.setBeam(false);

        this.setDeltaMovement(this.getDeltaMovement().add(0, -0.05D, 0));

        if (this.deathTime == 19 && !level().isClientSide) {

            CompoundTag data = new CompoundTag();
            data.putString("type", "tinytot");
            IParticleCreator.sendPacket((ServerLevel) level(), getX(), getY() + 0.5, getZ(), 250, data);
            level().playSound(null, getX(), getY(), getZ(), HbmSoundsNT.get("hbm:weapon.mukeExplosion"), SoundSource.HOSTILE, 15.0F, 1.0F);
        }

        super.tickDeath();
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TIER, 0);
        this.entityData.define(LOCK_X, 0F);
        this.entityData.define(LOCK_Y, 0F);
        this.entityData.define(LOCK_Z, 0F);
        this.entityData.define(BEAM, false);
    }
    //?}

    public void setTier(SiegeTier tier) {
        this.entityData.set(TIER, tier.id);

        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(tier.speedMod);
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(tier.health * 25);
        this.setHealth(this.getMaxHealth());
    }

    public SiegeTier getTier() {
        SiegeTier tier = SiegeTier.tiers[this.entityData.get(TIER)];
        return tier != null ? tier : SiegeTier.CLAY;
    }

    public void setBeam(boolean beam) {
        this.entityData.set(BEAM, beam);
    }

    public boolean getBeam() {
        return this.entityData.get(BEAM);
    }

    public void setLockon(double x, double y, double z) {
        this.entityData.set(LOCK_X, (float) x);
        this.entityData.set(LOCK_Y, (float) y);
        this.entityData.set(LOCK_Z, (float) z);
    }

    public Vec3 getLockon() {
        return new Vec3(this.entityData.get(LOCK_X), this.entityData.get(LOCK_Y), this.entityData.get(LOCK_Z));
    }

    @Override
    protected int getScanRange() {
        return 100;
    }

    @Override
    protected int targetHeightOffset() {
        return 7 + random.nextInt(5);
    }

    @Override
    protected int wanderHeightOffset() {
        return 10 + random.nextInt(2);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.isRemoved()) return;

        if (this.courseChangeCooldown > 0) {
            this.courseChangeCooldown--;
        }
        if (this.scanCooldown > 0) {
            this.scanCooldown--;
        }

        if (!level().isClientSide) {
            ServerLevel server = (ServerLevel) level();

            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }
            if (this.beamCountdown > 0) {
                this.beamCountdown--;
            }

            if (random.nextInt(50) == 0) {

                CompoundTag dPart = new CompoundTag();
                dPart.putString("type", "tau");
                dPart.putByte("count", (byte) (2 + random.nextInt(3)));
                IParticleCreator.sendPacket(server, getX() + random.nextGaussian() * 2, getY() + random.nextGaussian(), getZ() + random.nextGaussian() * 2, 50, dPart);
            }

            boolean beam = false;

            if (this.target == null || this.beamCountdown <= 0) {
                this.beamCountdown = 300; //200 - 100: nichts, 100 - 40: Zielerfassung, 40 - 20: fixieren, 20 - 0: Strahl
            } else {

                if (this.beamCountdown >= 60 && this.beamCountdown < 120) {
                    double x = this.target.getX();
                    double y = this.target.getY() + this.target.getBbHeight() * 0.5;
                    double z = this.target.getZ();
                    this.setLockon(x, y, z);

                    if (this.beamCountdown == 110) {
                        level().playSound(null, target.getX(), target.getY(), target.getZ(), HbmSoundsNT.get("hbm:weapon.stingerLockOn"), SoundSource.HOSTILE, 2F, 0.75F);
                    }
                }

                if (this.beamCountdown >= 40 && this.beamCountdown < 100) {

                    Vec3 lockon = this.getLockon();
                    CompoundTag fx = new CompoundTag();
                    fx.putString("type", "vanillaburst");
                    fx.putString("mode", "reddust");
                    fx.putDouble("motion", 0.2D);
                    fx.putInt("count", 5);
                    IParticleCreator.sendPacket(server, lockon.x, lockon.y, lockon.z, 100, fx);
                }

                if (this.beamCountdown < 40) {

                    Vec3 lockon = this.getLockon();

                    if (this.beamCountdown == 39) {
                        level().playSound(null, lockon.x, lockon.y, lockon.z, HbmSoundsNT.get("hbm:entity.ufoBlast"), SoundSource.HOSTILE, 5.0F, 0.9F + level().random.nextFloat() * 0.2F);
                    }

                    List<Entity> entities = level().getEntities(this, new AABB(lockon.x, lockon.y, lockon.z, lockon.x, lockon.y, lockon.z).inflate(2, 2, 2));

                    for (Entity e : entities) {
                        if (canAttackClass(e)) {
                            // Original causeCombineDamage(this, e): direkte Quelle das Schiff, Verursacher das Opfer
                            e.hurt(ModDamageSources.create(e, this, ModDamageTypes.CMB), 1000F);
                            com.hbm_m.platform.PlatformHooks.setSecondsOnFire(e, 5);

                            if (e instanceof LivingEntity living)
                                ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 5F);
                        }
                    }

                    CompoundTag data = new CompoundTag();
                    data.putString("type", "plasmablast");
                    data.putFloat("r", 0.0F);
                    data.putFloat("g", 0.75F);
                    data.putFloat("b", 1.0F);
                    data.putFloat("pitch", -90 + random.nextFloat() * 180);
                    data.putFloat("yaw", random.nextFloat() * 180F);
                    data.putFloat("scale", 5F);
                    IParticleCreator.sendPacket(server, lockon.x, lockon.y, lockon.z, 150, data);
                    beam = true;
                }
            }

            this.setBeam(beam);

            if (this.attackCooldown == 0 && this.target != null) {
                this.attackCooldown = 30 + random.nextInt(10);

                double x = getX();
                double y = getY();
                double z = getZ();

                Vec3 vec = new Vec3(target.getX() - x, target.getY() + target.getBbHeight() * 0.5 - y, target.getZ() - z).normalize();
                SiegeTier tier = this.getTier();

                float health = getHealth() / getMaxHealth();

                int r = (int) (0xff * (1 - health));
                int g = (int) (0xff * health);
                int b = 0;
                int color = (r << 16) | (g << 8) | b;

                for (int i = 0; i < 7; i++) {

                    // Vec3.rotateAroundY der 1.7.10 dreht wie Vec3.yRot
                    Vec3 copy = vec.yRot((float) Math.PI / 180F * (i - 3) * 5F);

                    EntitySiegeLaser laser = new EntitySiegeLaser(level(), this);
                    laser.setPos(x, y, z);
                    laser.shoot(copy.x, copy.y, copy.z, 1F, 0.0F);
                    laser.setColor(color);
                    laser.setDamage(tier.damageMod);
                    laser.setBreakChance(tier.laserBreak * 2);
                    if (tier.laserIncendiary) laser.setIncendiary();
                    level().addFreshEntity(laser);
                }

                level().playSound(null, getX(), getY(), getZ(), HbmSoundsNT.get("hbm:weapon.ballsLaser"), SoundSource.HOSTILE, 2.0F, 1.0F);
            }
        }

        if (this.courseChangeCooldown > 0) {
            approachPosition(this.target == null ? 0.25D : 0.5D + this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1);
        }
    }

    @Override
    protected void setCourseWithoutTaget() {
        int x = (int) Math.floor(getX() + random.nextGaussian() * 15);
        int z = (int) Math.floor(getZ() + random.nextGaussian() * 15);
        this.setWaypoint(x, level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 5 + random.nextInt(6), z);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("siegeTier", this.getTier().id);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        SiegeTier tier = SiegeTier.tiers[nbt.getInt("siegeTier")];
        this.setTier(tier != null ? tier : SiegeTier.CLAY);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        this.setTier(SiegeTier.tiers[random.nextInt(SiegeTier.getLength())]);
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean byPlayer) {

        if (byPlayer) {
            for (Supplier<ItemStack> drop : this.getTier().dropItem) {
                this.spawnAtLocation(drop.get().copy(), 0F);
            }
        }
    }
}
