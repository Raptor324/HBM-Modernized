package com.hbm_m.entity.mob;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.entity.IRadiationImmune;
import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.projectile.EntityBullet;
import com.hbm_m.entity.projectile.EntityChopperMine;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityHunterChopper}: der Kampfhubschrauber. Fliegt wie ein Ghast Wegpunkte 10-25 Bloecke ueber dem
 * Boden an, zielt auf das naechste Lebewesen (250 Bloecke), feuert ab Zaehler 120 jeden zweiten Tick
 * Hubschraubergeschosse und wirft Minen. Nur explosionsartiger Schaden ohne Verursacher wirkt (alles mit Entity wird
 * abgewiesen, der Rest auf 10 % gesenkt ausser Splitter/Atom/Schwarzes Loch/Explosion). Toedlicher Schaden laesst ihn
 * brennend abstuerzen; am Boden 15er-Explosion und Kampfschrott.
 */
public class EntityHunterChopper extends FlyingMob implements Enemy, IRadiationImmune {

    private static final EntityDataAccessor<Byte> CHARGING = SynchedEntityData.defineId(EntityHunterChopper.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DYING = SynchedEntityData.defineId(EntityHunterChopper.class, EntityDataSerializers.BYTE);

    public int courseChangeCooldown;
    public double waypointX;
    public double waypointY;
    public double waypointZ;
    private Entity targetedEntity;
    public int prevAttackCounter;
    public int attackCounter;
    public int mineDropCounter;
    public boolean isDying = false;

    public EntityHunterChopper(EntityType<? extends EntityHunterChopper> type, Level world) {
        super(type, world);
        this.xpReward = 500;
        this.noCulling = true;
    }

    public EntityHunterChopper(Level world) {
        this(ModEntities.HUNTER_CHOPPER.get(), world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return FlyingMob.createMobAttributes().add(Attributes.MAX_HEALTH, 750.0D);
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CHARGING, (byte) 0);
        this.entityData.define(DYING, (byte) 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGING, (byte) 0);
        builder.define(DYING, (byte) 0);
    }
    *///?}

    public boolean isCharging() {
        return this.entityData.get(CHARGING) != 0;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {

        if (!(source.is(ModDamageTypes.SHRAPNEL) || source.is(ModDamageTypes.NUCLEAR_BLAST) || source.is(ModDamageTypes.BLACK_HOLE) || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(ModDamageTypes.TAU) || source.getMsgId().startsWith("subAtomic")))
            amount *= 0.1F;

        // Original: "source instanceof EntityDamageSource" - alles, was von einer Entity ausgeht, prallt ab
        boolean entitySource = source.getEntity() != null || (source.getDirectEntity() != null && !source.is(DamageTypeTags.IS_EXPLOSION));

        if (this.isInvulnerableTo(source) || entitySource || this.getHealth() <= 0.1F) {
            return false;
        } else if (amount >= this.getHealth()) {
            this.initDeath();
            this.setIsDying(true);
            this.setHealth(0.1F);
            return false;
        }

        if (random.nextInt(15) == 0) {
            if (!level().isClientSide && !this.isDying) {
                level().explode(this, getX(), getY(), getZ(), 5F, true, Level.ExplosionInteraction.MOB);
                this.dropDamageItem();
            }
        }

        for (int j = 0; j < 3; j++) {
            double d0 = random.nextDouble() / 20 * random.nextInt(2) == 0 ? -1 : 1;
            double d1 = random.nextDouble() / 20 * random.nextInt(2) == 0 ? -1 : 1;
            double d2 = random.nextDouble() / 20 * random.nextInt(2) == 0 ? -1 : 1;

            for (int i = 0; i < 8; i++)
                if (level().isClientSide)
                    level().addParticle(ParticleTypes.FIREWORK, getX(), getY(), getZ(), d0 * i * 0.25, d1 * i * 0.25, d2 * i * 0.25);
        }

        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) com.hbm_m.client.sound.ChopperSoundClient.tickChopper(this);
    }

    @Override
    protected void customServerAiStep() {
        if (level().getDifficulty() == Difficulty.PEACEFUL) {
            this.discard();
        }

        Vec3 m = getDeltaMovement();
        double motionX = m.x, motionY = m.y, motionZ = m.z;

        if (!isDying) {
            playSoundEffect("hbm:misc.nullChopper", 10.0F, 0.5F);

            this.prevAttackCounter = this.attackCounter;
            double d0 = this.waypointX - getX();
            double d1 = this.waypointY - getY();
            double d2 = this.waypointZ - getZ();
            double d3 = d0 * d0 + d1 * d1 + d2 * d2;

            if (d3 < 1.0D || d3 > 3600.0D) {
                if (this.targetedEntity != null) {
                    this.waypointX = targetedEntity.getX() + (this.random.nextFloat() * 2.0F - 1.0F) * 16.0F;
                    this.waypointZ = targetedEntity.getZ() + (this.random.nextFloat() * 2.0F - 1.0F) * 16.0F;
                } else {
                    this.waypointX = getX() + (this.random.nextFloat() * 2.0F - 1.0F) * 16.0F;
                    this.waypointZ = getZ() + (this.random.nextFloat() * 2.0F - 1.0F) * 16.0F;
                }
                this.waypointY = heightValue(waypointX, waypointZ) + 10 + random.nextInt(15);
            }

            if (this.courseChangeCooldown-- <= 0) {
                this.courseChangeCooldown += this.random.nextInt(5) + 2;
                d3 = Math.sqrt(d3);

                if (this.isCourseTraversable(this.waypointX, this.waypointY, this.waypointZ, d3)) {
                    motionX += d0 / d3 * 0.1D;
                    motionY += d1 / d3 * 0.1D;
                    motionZ += d2 / d3 * 0.1D;
                } else {
                    this.waypointX = getX() + (this.random.nextFloat() * 2.0F - 1.0F) * 16.0F;
                    this.waypointZ = getZ() + (this.random.nextFloat() * 2.0F - 1.0F) * 16.0F;
                    this.waypointY = heightValue(waypointX, waypointZ) + 10 + random.nextInt(15);
                }
            }

            if (this.targetedEntity != null && !this.targetedEntity.isAlive()) {
                this.targetedEntity = null;
            }

            if (this.targetedEntity == null || this.attackCounter <= 0) {
                this.targetedEntity = getClosestEntityForChopper(level(), getX(), getY(), getZ(), 250);
            }

            double d4 = 64.0D;

            if (this.targetedEntity != null && this.targetedEntity.distanceToSqr(this) < d4 * d4) {
                double d8 = 2.0D;
                Vec3 vec3 = this.getViewVector(1.0F);
                double xStart = getX() + vec3.x * d8;
                double yStart = getY() - 0.5;
                double zStart = getZ() + vec3.z * d8;
                double d5 = this.targetedEntity.getX() - xStart;
                double d6 = this.targetedEntity.getBoundingBox().minY + this.targetedEntity.getBbHeight() / 2.0F - yStart;
                double d7 = this.targetedEntity.getZ() - zStart;

                ++this.attackCounter;
                if (attackCounter >= 200) {
                    attackCounter -= 200;
                }

                if (this.attackCounter % 2 == 0 && attackCounter >= 120) {
                    // Original: "hbm:weapon.osiprShoot" - diesen Klang gibt es im Original nicht (stumm)
                    EntityBullet entityarrow = new EntityBullet(level(), this, 3.0F, 35, 45, false, "chopper");
                    Vec3 vec2 = new Vec3(d5 - 1 + random.nextInt(3), d6 - 1 + random.nextInt(3), d7 - 1 + random.nextInt(3)).normalize();
                    double motion = 3;
                    entityarrow.setDeltaMovement(vec2.x * motion, vec2.y * motion, vec2.z * motion);
                    entityarrow.setDamage(3 + random.nextInt(5));
                    entityarrow.setPos(xStart, yStart, zStart);
                    level().addFreshEntity(entityarrow);
                }
                if (this.attackCounter == 80) {
                    playSoundAtEntity("hbm:entity.chopperCharge", 5.0F, 1.0F);
                }

                this.mineDropCounter++;
                if (mineDropCounter > 100 && random.nextInt(15) == 0) {
                    playSoundAtEntity("hbm:entity.chopperDrop", 15.0F, 1.0F);
                    EntityChopperMine mine = new EntityChopperMine(level(), getX(), getY() - 0.5, getZ(), 0, -0.3, 0, this);
                    this.mineDropCounter = 0;
                    level().addFreshEntity(mine);

                    if (random.nextInt(3) == 0) {
                        level().addFreshEntity(new EntityChopperMine(level(), getX(), getY() - 0.5, getZ(), 1, -0.3, 0, this));
                        level().addFreshEntity(new EntityChopperMine(level(), getX(), getY() - 0.5, getZ(), 0, -0.3, 1, this));
                        level().addFreshEntity(new EntityChopperMine(level(), getX(), getY() - 0.5, getZ(), -1, -0.3, 0, this));
                        level().addFreshEntity(new EntityChopperMine(level(), getX(), getY() - 0.5, getZ(), 0, -0.3, -1, this));
                    }
                }

            } else {

                if (this.attackCounter > 0) {
                    this.attackCounter = 0;
                }
            }

            byte b1 = this.entityData.get(CHARGING);
            byte b0 = (byte) (this.attackCounter > 10 ? 1 : 0);

            if (b1 != b0) {
                this.entityData.set(CHARGING, b0);
            }
        } else {
            motionY -= 0.08;
            if (Math.sqrt(Math.pow(motionX, 2) + Math.pow(motionZ, 2)) * 1.2 < 1.8) {
                motionX *= 1.2;
                motionZ *= 1.2;
            }

            if (random.nextInt(20) == 0) {
                level().explode(this, getX(), getY(), getZ(), 5F, true, Level.ExplosionInteraction.MOB);
            }

            CompoundTag data = new CompoundTag();
            data.putString("type", "exhaust");
            data.putString("mode", "meteor");
            data.putInt("count", 10);
            data.putDouble("width", 1);
            if (level() instanceof ServerLevel sl) {
                for (net.minecraft.server.level.ServerPlayer p : sl.players()) {
                    if (p.distanceToSqr(getX(), getY(), getZ()) < 100 * 100) com.hbm_m.network.AuxParticlePacket.sendTo(p, data, getX(), getY(), getZ());
                }
            }

            setYRot(getYRot() + 20);

            if (this.onGround()) {
                level().explode(this, getX(), getY(), getZ(), 15F, true, Level.ExplosionInteraction.MOB);
                this.dropItems();
                this.discard();
            }
            if (this.tickCount % 2 == 0)
                playSoundEffect("hbm:misc.nullCrashing", 10.0F, 0.5F);
        }

        this.setDeltaMovement(motionX, motionY, motionZ);

        double tgtYaw = this.targetedEntity == null
                ? Math.atan2(motionX, motionZ) * 180.0D / Math.PI
                : Math.atan2(getX() - targetedEntity.getX(), getZ() - targetedEntity.getZ()) * 180.0D / Math.PI;
        double f3 = Math.sqrt(motionX * motionX + motionZ * motionZ);
        float yaw = getYRot();
        if (yaw - tgtYaw >= 10)
            this.yRotO = yaw = yaw - 10;
        if (yaw - tgtYaw <= -10)
            this.yRotO = yaw = yaw + 10;
        // Original-Bedingung "< 10 && > 10" ist nie erfuellt - bleibt so
        setYRot(yaw);
        float pitch = (float) (Math.atan2(motionY, f3) * 180.0D / Math.PI);
        this.xRotO = pitch;

        if (pitch <= 330 && pitch >= 30) {
            if (pitch < 180)
                pitch = 30;
            if (pitch >= 180)
                pitch = 330;
        }
        setXRot(pitch);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;

        updateBossBar();
    }

    private double heightValue(double x, double z) {
        return level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) x, (int) z);
    }

    private void playSoundEffect(String key, float volume, float pitch) {
        SoundEvent ev = HbmSoundsNT.get(key);
        if (ev != null) level().playSound(null, getX(), getY(), getZ(), ev, SoundSource.HOSTILE, volume, pitch);
    }

    private void playSoundAtEntity(String key, float volume, float pitch) {
        playSoundEffect(key, volume, pitch);
    }

    /** True if the ghast has an unobstructed line of travel to the waypoint. */
    private boolean isCourseTraversable(double x, double y, double z, double dist) {
        double d4 = (this.waypointX - getX()) / dist;
        double d5 = (this.waypointY - getY()) / dist;
        double d6 = (this.waypointZ - getZ()) / dist;
        AABB axisalignedbb = this.getBoundingBox();

        for (int i = 1; i < dist; ++i) {
            axisalignedbb = axisalignedbb.move(d4, d5, d6);

            if (!level().noCollision(this, axisalignedbb)) {
                return false;
            }
        }

        return true;
    }

    /** {@code Library.getClosestEntityForChopper}: naechstes lebendes Wesen (kein Hubschrauber), Schleichen = 80 % Reichweite. */
    @Nullable
    public static LivingEntity getClosestEntityForChopper(Level world, double x, double y, double z, double radius) {
        double d4 = -1.0D;
        LivingEntity result = null;

        for (LivingEntity entity : world.getEntitiesOfClass(LivingEntity.class, new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius))) {
            if (entity instanceof EntityHunterChopper) continue;

            if (entity.isAlive() && !(entity instanceof Player p && p.getAbilities().invulnerable)) {
                double d5 = entity.distanceToSqr(x, y, z);
                double d6 = radius;

                if (entity.isShiftKeyDown()) {
                    d6 = radius * 0.800000011920929D;
                }

                if ((radius < 0.0D || d5 < d6 * d6) && (d4 == -1.0D || d5 < d4)) {
                    d4 = d5;
                    result = entity;
                }
            }
        }

        return result;
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    protected void dropItems() {
        this.spawnAtLocation(new ItemStack(ModMaterialItems.item(ModMaterials.COMBINE_SCRAP, MaterialShape.SCRAP), random.nextInt(8) + 1));
        this.spawnAtLocation(new ItemStack(ModMaterialItems.item(ModMaterials.COMBINE_STEEL, MaterialShape.PLATE), random.nextInt(5) + 1));
    }

    @Override
    protected float getSoundVolume() {
        return 10.0F;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000;
    }

    public void initDeath() {
        level().explode(this, getX(), getY(), getZ(), 10F, true, Level.ExplosionInteraction.MOB);
        if (!this.isDying)
            playSoundAtEntity("hbm:entity.chopperDamage", 10.0F, 1.0F);
        isDying = true;
    }

    public void dropDamageItem() {
        int i = random.nextInt(10);

        if (i < 6)
            this.spawnAtLocation(new ItemStack(ModMaterialItems.item(ModMaterials.COMBINE_SCRAP, MaterialShape.SCRAP), 1));
        else
            this.spawnAtLocation(new ItemStack(ModMaterialItems.item(ModMaterials.COMBINE_STEEL, MaterialShape.PLATE), 1));
    }

    public void setIsDying(boolean b) {
        this.entityData.set(DYING, (byte) (b ? 1 : 0));
    }

    public boolean getIsDying() {
        return this.entityData.get(DYING) == 1;
    }

    // ─── Bossleiste (Original: BossStatus aus dem Renderer) ──────────────────

    private final net.minecraft.server.level.ServerBossEvent bossEvent =
            new net.minecraft.server.level.ServerBossEvent(this.getDisplayName(),
                    net.minecraft.world.BossEvent.BossBarColor.PURPLE,
                    net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);

    @Override
    public void startSeenByPlayer(@NotNull net.minecraft.server.level.ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(@NotNull net.minecraft.server.level.ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable net.minecraft.network.chat.Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    protected void updateBossBar() {
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }
}
