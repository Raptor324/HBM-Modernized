package com.hbm_m.entity.projectile;

import com.hbm_m.platform.PlatformHooks;

import com.hbm_m.platform.EffectHooks;

import javax.annotation.Nullable;

import com.hbm_m.block.bomb.BlockDetonatable;
import com.hbm_m.effect.ModEffects;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm_m.explosion.vanillant.standard.BlockMutatorFire;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorNoDamage;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm_m.handler.BulletConfigSyncingUtil;
import com.hbm_m.handler.BulletConfiguration;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.BobMathUtil;
import com.hbm_m.util.Vec3NT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityBulletBaseNT} (MK2-Altsystem, entity_bullet_mk2): nur noch fuer die NPC-Geschosse (Maskman,
 * BOT Prime, UFO) und die Turbinenblaetter der Turbinen-Rakete. Konfiguration ueber {@link BulletConfigSyncingUtil},
 * Stil/Spur/Schluessel synchronisiert. Wird wie im Original nicht gespeichert.
 * Port: {@code getEntityData().homingTarget} ist das Feld {@link #homingTarget}.
 */
@Deprecated
public class EntityBulletBaseNT extends EntityThrowableInterp {

    private static final EntityDataAccessor<Byte> STYLE = SynchedEntityData.defineId(EntityBulletBaseNT.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> TRAIL = SynchedEntityData.defineId(EntityBulletBaseNT.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> CONFIG = SynchedEntityData.defineId(EntityBulletBaseNT.class, EntityDataSerializers.INT);

    @Nullable private BulletConfiguration config;
    public float overrideDamage;
    /** Original: {@code getEntityData().getInteger("homingTarget")}. */
    public int homingTarget;

    public EntityBulletBaseNT(EntityType<? extends EntityBulletBaseNT> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    @Nullable
    public BulletConfiguration getConfig() {
        return config;
    }

    private void applyConfig(int key) {
        this.config = BulletConfigSyncingUtil.pullConfig(key);
        this.entityData.set(CONFIG, key);
        if (this.config == null) {
            this.discard();
            return;
        }
        this.entityData.set(STYLE, (byte) this.config.style);
        this.entityData.set(TRAIL, (byte) this.config.trail);
    }

    /** Original {@code EntityBulletBaseNT(World, int)}. */
    public static EntityBulletBaseNT create(Level world, int config) {
        EntityBulletBaseNT b = new EntityBulletBaseNT(ModEntities.BULLET_BASE_NT.get(), world);
        b.applyConfig(config);
        return b;
    }

    /** Original {@code EntityBulletBaseNT(World, int, EntityLivingBase)}: Augenhoehe, seitlicher Versatz 0,16. */
    public static EntityBulletBaseNT create(Level world, int config, LivingEntity entity) {
        EntityBulletBaseNT b = create(world, config);
        b.setThrower(entity);
        float yaw = entity.getYRot(), pitch = entity.getXRot();
        double x = entity.getX(), y = entity.getY() + entity.getEyeHeight(), z = entity.getZ();
        double sideOffset = 0.16D;
        x -= Mth.cos(yaw / 180.0F * (float) Math.PI) * sideOffset;
        y -= 0.1D;
        z -= Mth.sin(yaw / 180.0F * (float) Math.PI) * sideOffset;
        b.moveTo(x, y, z, yaw, pitch);
        double mx = -Mth.sin(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI);
        double mz = Mth.cos(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI);
        double my = -Mth.sin(pitch / 180.0F * (float) Math.PI);
        if (b.config != null) b.setThrowableHeading(mx, my, mz, 1.0F, b.config.spread);
        return b;
    }

    /** Original {@code EntityBulletBaseNT(World, int, shooter, target, motion, deviation)}: auf ein Drittel der Zielhoehe. */
    public static EntityBulletBaseNT create(Level world, int config, LivingEntity entity, LivingEntity target, float motion, float deviation) {
        EntityBulletBaseNT b = create(world, config);
        b.setThrower(entity);
        double posY = entity.getY() + entity.getEyeHeight() - 0.10000000149011612D;
        double d0 = target.getX() - entity.getX();
        double d1 = target.getBoundingBox().minY + target.getBbHeight() / 3.0F - posY;
        double d2 = target.getZ() - entity.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        b.setPos(b.getX(), posY, b.getZ());
        if (d3 >= 1.0E-7D) {
            float f2 = (float) (Math.atan2(d2, d0) * 180.0D / Math.PI) - 90.0F;
            float f3 = (float) (-(Math.atan2(d1, d3) * 180.0D / Math.PI));
            double d4 = d0 / d3;
            double d5 = d2 / d3;
            b.moveTo(entity.getX() + d4, posY, entity.getZ() + d5, f2, f3);
            b.setThrowableHeading(d0, d1, d2, motion, deviation);
        }
        return b;
    }

    //? if < 1.21.1 {
    @Override
    protected void defineExtraData() {
        this.entityData.define(STYLE, (byte) 0);
        this.entityData.define(TRAIL, (byte) 0);
        this.entityData.define(CONFIG, 0);
    }
    //?} else {
    /*@Override
    protected void defineExtraData(SynchedEntityData.Builder builder) {
        builder.define(STYLE, (byte) 0);
        builder.define(TRAIL, (byte) 0);
        builder.define(CONFIG, 0);
    }
    *///?}

    public int getStyle() { return this.entityData.get(STYLE); }
    public int getTrail() { return this.entityData.get(TRAIL); }

    @Override
    public void tick() {
        if (config == null) config = BulletConfigSyncingUtil.pullConfig(this.entityData.get(CONFIG));

        if (config == null) {
            this.discard();
            return;
        }

        if (!level().isClientSide) {
            if (config.maxAge == 0) {
                if (this.config.bntUpdate != null) this.config.bntUpdate.behaveUpdate(this);
                this.discard();
                return;
            }
            if (this.tickCount > config.maxAge) this.discard();
        }

        if (this.config.bntUpdate != null) this.config.bntUpdate.behaveUpdate(this);

        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();

        super.tick();

        if (level().isClientSide && !config.vPFX.isEmpty()) {
            Vec3 vec = new Vec3(getX() - xo, getY() - yo, getZ() - zo);
            double motion = Math.max(vec.length(), 0.1);
            vec = vec.normalize();
            for (double d = 0; d < motion; d += 0.5) {
                CompoundTag nbt = new CompoundTag();
                nbt.putString("type", "vanillaExt");
                nbt.putString("mode", config.vPFX);
                nbt.putDouble("posX", getX() - vec.x * d);
                nbt.putDouble("posY", getY() - vec.y * d);
                nbt.putDouble("posZ", getZ() - vec.z * d);
                com.hbm_m.particle.helper.ParticleEffectClient.effectNT(nbt);
            }
        }
    }

    @Override
    protected void onImpact(HitResult mop) {
        if (config == null) return;

        if (mop instanceof BlockHitResult bhr) {
            boolean hRic = random.nextInt(100) < config.HBRC;
            boolean doesRic = config.doesRicochet && hRic;
            Vec3 hit = bhr.getLocation();
            BlockPos bp = bhr.getBlockPos();
            int side = bhr.getDirection().get3DDataValue();

            if (!doesRic) {
                this.setPos(hit.x, hit.y, hit.z);
                this.onBlockImpact(bp.getX(), bp.getY(), bp.getZ(), side, false);
            }

            if (doesRic) {
                Vec3NT face = switch (bhr.getDirection()) {
                    case DOWN -> new Vec3NT(0, -1, 0);
                    case UP -> new Vec3NT(0, 1, 0);
                    case NORTH -> new Vec3NT(0, 0, 1);
                    case SOUTH -> new Vec3NT(0, 0, -1);
                    case WEST -> new Vec3NT(-1, 0, 0);
                    case EAST -> new Vec3NT(1, 0, 0);
                };

                Vec3 m = getDeltaMovement();
                Vec3NT vel = new Vec3NT(m.x, m.y, m.z);
                boolean lRic = random.nextInt(100) < config.LBRC;
                double angle = Math.abs(BobMathUtil.getCrossAngle(vel, face) - 90);

                if (hRic || (angle <= config.ricochetAngle && lRic)) {
                    double mx = m.x, my = m.y, mz = m.z;
                    switch (bhr.getDirection()) {
                        case DOWN, UP -> my *= -1;
                        case NORTH, SOUTH -> mz *= -1;
                        case WEST, EAST -> mx *= -1;
                    }
                    setDeltaMovement(mx, my, mz);

                    if (config.plink == 1)
                        level().playSound(null, getX(), getY(), getZ(), HbmSoundsNT.get("hbm:weapon.ricochet"), SoundSource.NEUTRAL, 0.25F, 1.0F);
                    if (config.plink == 2)
                        level().playSound(null, getX(), getY(), getZ(), HbmSoundsNT.get("hbm:weapon.gBounce"), SoundSource.NEUTRAL, 1.0F, 1.0F);

                    this.setPos(hit.x, hit.y, hit.z);
                } else {
                    if (!level().isClientSide) {
                        this.setPos(hit.x, hit.y, hit.z);
                        onBlockImpact(bp.getX(), bp.getY(), bp.getZ(), side, false);
                    }
                }

                setDeltaMovement(getDeltaMovement().scale(config.bounceMod));
            }
        }

        if (mop instanceof EntityHitResult ehr) {
            Entity victim = ehr.getEntity();
            DamageSource damagesource = this.config.getDamage(this, this.getThrower());

            if (!config.doesPenetrate) {
                this.setPos(ehr.getLocation().x, ehr.getLocation().y, ehr.getLocation().z);
                onEntityImpact(victim);
            } else {
                onEntityHurt(victim);
            }

            float damage = random.nextFloat() * (config.dmgMax - config.dmgMin) + config.dmgMin;
            if (overrideDamage != 0) damage = overrideDamage;

            boolean headshot = false;
            if (victim instanceof LivingEntity living && this.config.headshotMult > 1F) {
                double head = living.getBbHeight() - living.getEyeHeight();
                if (living.isAlive() && ehr.getLocation().y > (living.getY() + living.getBbHeight() - head * 2)) {
                    damage *= this.config.headshotMult;
                    headshot = true;
                }
            }

            if (!victim.hurt(damagesource, damage)) {
                // Original: lastDamage per Reflexion addieren und erneut versuchen
                try {
                    if (victim instanceof LivingEntity living) {
                        float dmg = damage + getLastHurt(living);
                        if (!victim.hurt(damagesource, dmg)) headshot = false;
                    }
                } catch (Exception x) { }
            }

            if (!level().isClientSide && headshot && victim instanceof LivingEntity living && level() instanceof ServerLevel server) {
                double head = living.getBbHeight() - living.getEyeHeight();
                CompoundTag data = new CompoundTag();
                data.putString("type", "vanillaburst");
                data.putInt("count", 15);
                data.putDouble("motion", 0.1D);
                data.putString("mode", "blockdust");
                data.putInt("block", BuiltInRegistries.BLOCK.getId(Blocks.REDSTONE_BLOCK));
                IParticleCreator.sendPacket(server, living.getX(), living.getY() + living.getBbHeight() - head, living.getZ(), 50, data);
                level().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.NEUTRAL, 1.0F, 0.95F + random.nextFloat() * 0.2F);
            }
        }
    }

    /**
     * Geschoss stirbt am Block (oder -1/-1/-1 beim Entitaetstreffer). Port: Original prueft bY &gt; -1 / == -1 - in 1.20
     * gibt es negative Y-Bloecke, daher der Merker {@code entityImpact}.
     */
    private void onBlockImpact(int bX, int bY, int bZ, int sideHit, boolean entityImpact) {
        BlockPos bp = new BlockPos(bX, bY, bZ);
        BlockState state = level().getBlockState(bp);

        if (config.bntImpact != null) config.bntImpact.behaveBlockHit(this, bX, bY, bZ, sideHit);

        if (!level().isClientSide) {
            if (!entityImpact && !this.inGround) this.discard();
            if (!config.doesPenetrate && entityImpact) this.discard();
        }

        if (config.incendiary > 0 && !level().isClientSide) {
            int px = (int) getX(), py = (int) getY(), pz = (int) getZ();
            int[][] offs = { {0, 0, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1} };
            for (int[] o : offs) {
                BlockPos p = new BlockPos(px + o[0], py + o[1], pz + o[2]);
                if (level().random.nextInt(3) == 0 && level().getBlockState(p).isAir()) level().setBlockAndUpdate(p, Blocks.FIRE.defaultBlockState());
            }
        }

        if (config.explosive > 0 && !level().isClientSide) {
            ExplosionVNT vnt = new ExplosionVNT(level(), getX(), getY(), getZ(), config.explosive, this.getThrower());
            vnt.setBlockAllocator(new BlockAllocatorStandard());
            if (config.blockDamage) vnt.setBlockProcessor(new BlockProcessorStandard().withBlockEffect(config.incendiary > 0 ? new BlockMutatorFire() : null));
            else vnt.setBlockProcessor(new BlockProcessorNoDamage().withBlockEffect(config.incendiary > 0 ? new BlockMutatorFire() : null));
            vnt.setEntityProcessor(new EntityProcessorStandard().allowSelfDamage());
            vnt.setPlayerProcessor(new PlayerProcessorStandard());
            vnt.setSFX(new ExplosionEffectStandard());
            vnt.explode();
        }

        if (entityImpact) return; // Original prueft hier Block (-1,-1,-1) = Luft

        if (config.destroysBlocks && !level().isClientSide) {
            if (state.getDestroySpeed(level(), bp) <= 120 && state.getDestroySpeed(level(), bp) >= 0) level().destroyBlock(bp, false);
        } else if (config.doesBreakGlass && !level().isClientSide) {
            if (state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE) || state.getBlock() instanceof StainedGlassBlock || state.getBlock() instanceof StainedGlassPaneBlock)
                level().destroyBlock(bp, false);
            if (state.getBlock() instanceof BlockDetonatable det) det.onShot(level(), bp);
        }
    }

    private void onEntityImpact(Entity e) {
        onEntityHurt(e);
        onBlockImpact(-1, -1, -1, -1, true);
    }

    private void onEntityHurt(Entity e) {
        if (config.incendiary > 0 && !level().isClientSide) PlatformHooks.setSecondsOnFire(e, config.incendiary);

        if (config.leadChance > 0 && !level().isClientSide && level().random.nextInt(100) < config.leadChance && e instanceof LivingEntity living)
            living.addEffect(new MobEffectInstance(EffectHooks.of(ModEffects.LEAD), 10 * 20, 0));

        if (e instanceof LivingEntity living && config.effects != null && !config.effects.isEmpty() && !level().isClientSide) {
            for (MobEffectInstance effect : config.effects) living.addEffect(new MobEffectInstance(effect));
        }
    }

    private static java.lang.reflect.Field lastHurtField;

    /** Original: {@code ReflectionHelper.findField(EntityLivingBase.class, "lastDamage", "field_110153_bc")}. */
    private static float getLastHurt(LivingEntity living) throws Exception {
        if (lastHurtField == null) {
            java.lang.reflect.Field f;
            try {
                f = LivingEntity.class.getDeclaredField("lastHurt");
            } catch (NoSuchFieldException e) {
                f = LivingEntity.class.getDeclaredField("f_20898_");
            }
            f.setAccessible(true);
            lastHurtField = f;
        }
        return lastHurtField.getFloat(living);
    }

    @Override public boolean doesPenetrate() { return config != null && this.config.doesPenetrate; }
    @Override public int selfDamageDelay() { return config == null ? 5 : this.config.selfDamageDelay; }
    @Override protected double headingForceMult() { return 1D; }
    @Override public double getGravityVelocity() { return config == null ? 0 : this.config.gravity; }
    @Override protected double motionMult() { return config == null ? 1 : this.config.velocity; }
    @Override protected float getAirDrag() { return 1F; }
    @Override protected float getWaterDrag() { return 1F; }

    /** Original writeToNBTOptional = false: wird nie gespeichert. */
    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.discard();
    }

    //? if < 1.21.1 {
    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(this);
    }
    //?}

    public interface IBulletHurtBehaviorNT { void behaveEntityHurt(EntityBulletBaseNT bullet, Entity hit); }
    public interface IBulletHitBehaviorNT { void behaveEntityHit(EntityBulletBaseNT bullet, Entity hit); }
    public interface IBulletRicochetBehaviorNT { void behaveBlockRicochet(EntityBulletBaseNT bullet, int x, int y, int z); }
    public interface IBulletImpactBehaviorNT { void behaveBlockHit(EntityBulletBaseNT bullet, int x, int y, int z, int sideHit); }
    public interface IBulletUpdateBehaviorNT { void behaveUpdate(EntityBulletBaseNT bullet); }
}
