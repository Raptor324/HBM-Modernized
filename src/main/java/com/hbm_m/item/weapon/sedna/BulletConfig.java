package com.hbm_m.item.weapon.sedna;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.bomb.BlockDetonatable;
import com.hbm_m.entity.projectile.EntityBulletBaseMK4;
import com.hbm_m.entity.projectile.EntityBulletBeamBase;
import com.hbm_m.inventory.ComparableStack;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmoSecret;
import com.hbm_m.particle.SpentCasing;
import com.hbm_m.powerarmor.resist.DamageResistanceHandler.DamageClass;
import com.hbm_m.util.BobMathUtil;
import com.hbm_m.util.EntityDamageUtil;
import com.hbm_m.util.ForgeDirection;
import com.hbm_m.util.MovingObjectPosition;
import com.hbm_m.util.Vec3NT;
import com.hbm_m.util.confetti.ConfettiUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ItemLike;

/**
 * 1:1 {@code BulletConfig}: alles, was ein Geschoss ausmacht - Munition, Huelse, Geschwindigkeit, Streuung,
 * Schadensfaktoren, Durchschlag, Abpraller und die Lambdas fuer Flug, Aufschlag und Darstellung. Jede Konfiguration
 * bekommt beim Erzeugen eine fortlaufende ID (Reihenfolge = {@code GunFactory.init}), die im Magazin-NBT und im
 * Geschoss-Entity gespeichert wird.
 */
public class BulletConfig implements Cloneable {

    public static List<BulletConfig> configs = new ArrayList<>();

    /** Original-Sonderfall in {@code EntityBulletBaseMK4.onUpdate}: der Seilhaken dreht sich clientseitig nicht. */
    @Nullable
    public static Predicate<BulletConfig> clientRotationLocked;

    public int id;

    public ComparableStack ammo;
    public ItemStack casingItem;
    public int casingAmount;
    /** How much ammo is added to a standard mag when loading one item */
    public int ammoReloadCount = 1;
    public float velocity = 10F;
    public float spread = 0F;
    public float wear = 1F;
    public int projectilesMin = 1;
    public int projectilesMax = 1;
    public ProjectileType pType = ProjectileType.BULLET;

    public float damageMult = 1.0F;
    public float armorThresholdNegation = 0.0F;
    public float armorPiercingPercent = 0.0F;
    public float knockbackMult = 0.1F;
    public float headshotMult = 1.25F;

    public DamageClass dmgClass = DamageClass.PHYSICAL;

    public float ricochetAngle = 5F;
    public int maxRicochetCount = 2;
    /** Whether damage dealt to an entity is subtracted from the projectile's damage on penetration */
    public boolean damageFalloffByPen = true;

    public Consumer<Entity> onUpdate;
    public BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> onImpact;
    public BiConsumer<EntityBulletBeamBase, MovingObjectPosition> onImpactBeam; //fuck fuck fuck fuck i should have used a better base class here god dammit
    public BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> onRicochet = LAMBDA_STANDARD_RICOCHET;
    public BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> onEntityHit = LAMBDA_STANDARD_ENTITY_HIT;

    public double gravity = 0;
    public int expires = 30;
    public boolean impactsEntities = true;
    public boolean doesPenetrate = false;
    /** Whether projectiles ignore blocks entirely */
    public boolean isSpectral = false;
    public int selfDamageDelay = 2;

    public boolean blackPowder = false;
    public boolean renderRotations = true;
    public SpentCasing casing;
    /** Clientseitige Darstellung (PoseStack ueber {@code GunGL}); Lambda wird nur auf dem Client aufgerufen. */
    public BiConsumer<EntityBulletBaseMK4, Float> renderer;
    public BiConsumer<EntityBulletBeamBase, Float> rendererBeam;

    public BulletConfig() {
        this.id = configs.size();
        configs.add(this);
    }

    /** Required for the clone() operation to reset the ID, otherwise the ID and config entry will be the same as the original */
    public BulletConfig forceReRegister() {
        this.id = configs.size();
        configs.add(this);
        return this;
    }

    public BulletConfig setBeam() {														this.pType = ProjectileType.BEAM; return this; }
    public BulletConfig setChunkloading() {												this.pType = ProjectileType.BULLET_CHUNKLOADING; return this; }
    public BulletConfig setItem(ItemLike ammo) {										this.ammo = new ComparableStack(ammo); return this; }
    public BulletConfig setItem(Supplier<? extends ItemLike> ammo) {					this.ammo = new ComparableStack(ammo); return this; }
    public BulletConfig setItem(ItemStack ammo) {										this.ammo = new ComparableStack(ammo); return this; }
    public BulletConfig setItem(ComparableStack ammo) {									this.ammo = ammo; return this; }
    public BulletConfig setItem(EnumAmmo ammo) {										this.ammo = new ComparableStack(WeaponItems.AMMO_STANDARD.get(ammo)); return this; }
    public BulletConfig setItem(EnumAmmoSecret ammo) {									this.ammo = new ComparableStack(WeaponItems.AMMO_SECRET.get(ammo)); return this; }
    public BulletConfig setCasing(ItemStack item, int amount) {							this.casingItem = item; this.casingAmount = amount; return this; }
    /** Original {@code setCasing(EnumCasingType, amount)}: Huelsen-Gegenstand {@code casing_<typ>}. */
    public BulletConfig setCasing(EnumCasingType item, int amount) {					this.casingItem = item.stack(); this.casingAmount = amount; return this; }
    public BulletConfig setReloadCount(int ammoReloadCount) {							this.ammoReloadCount = ammoReloadCount; return this; }
    public BulletConfig setVel(float velocity) {										this.velocity = velocity; return this; }
    public BulletConfig setSpread(float spread) {										this.spread = spread; return this; }
    public BulletConfig setWear(float wear) {											this.wear = wear; return this; }
    public BulletConfig setProjectiles(int amount) {									this.projectilesMin = this.projectilesMax = amount; return this; }
    public BulletConfig setProjectiles(int min, int max) {								this.projectilesMin = min; this.projectilesMax = max; return this; }
    public BulletConfig setDamage(float damageMult) {									this.damageMult = damageMult; return this; }
    public BulletConfig setThresholdNegation(float armorThresholdNegation) {			this.armorThresholdNegation = armorThresholdNegation; return this; }
    public BulletConfig setArmorPiercing(float armorPiercingPercent) {					this.armorPiercingPercent = armorPiercingPercent; return this; }
    public BulletConfig setKnockback(float knockbackMult) {								this.knockbackMult = knockbackMult; return this; }
    public BulletConfig setHeadshot(float headshotMult) {								this.headshotMult = headshotMult; return this; }
    public BulletConfig setupDamageClass(DamageClass clazz) {							this.dmgClass = clazz; return this; }
    public BulletConfig setRicochetAngle(float angle) {									this.ricochetAngle = angle; return this; }
    public BulletConfig setRicochetCount(int count) {									this.maxRicochetCount = count; return this; }
    public BulletConfig setDamageFalloffByPen(boolean falloff) {						this.damageFalloffByPen = falloff; return this; }
    public BulletConfig setGrav(double gravity) {										this.gravity = gravity; return this; }
    public BulletConfig setLife(int expires) {											this.expires = expires; return this; }
    public BulletConfig setImpactsEntities(boolean impact) {							this.impactsEntities = impact; return this; }
    public BulletConfig setDoesPenetrate(boolean pen) {									this.doesPenetrate = pen; return this; }
    public BulletConfig setSpectral(boolean spectral) {									this.isSpectral = spectral; return this; }
    public BulletConfig setSelfDamageDelay(int delay) {									this.selfDamageDelay = delay; return this; }
    public BulletConfig setBlackPowder(boolean bp) {									this.blackPowder = bp; return this; }
    public BulletConfig setRenderRotations(boolean rot) {								this.renderRotations = rot; return this; }
    public BulletConfig setCasing(SpentCasing casing) {									this.casing = casing; return this; }

    public BulletConfig setRenderer(BiConsumer<EntityBulletBaseMK4, Float> renderer) {		this.renderer = renderer; return this; }
    public BulletConfig setRendererBeam(BiConsumer<EntityBulletBeamBase, Float> renderer) {	this.rendererBeam = renderer; return this; }

    public BulletConfig setOnUpdate(Consumer<Entity> lambda) {												this.onUpdate = lambda; return this; }
    public BulletConfig setOnRicochet(BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> lambda) {		this.onRicochet = lambda; return this; }
    public BulletConfig setOnImpact(BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> lambda) {			this.onImpact = lambda; return this; }
    public BulletConfig setOnBeamImpact(BiConsumer<EntityBulletBeamBase, MovingObjectPosition> lambda) {	this.onImpactBeam = lambda; return this; }
    public BulletConfig setOnEntityHit(BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> lambda) {		this.onEntityHit = lambda; return this; }

    public static enum ProjectileType {
        BULLET,
        BULLET_CHUNKLOADING,
        BEAM
    }

    /** {@code ItemEnums.EnumCasingType}: die Huelsen-Gegenstaende {@code casing_<typ>}. */
    public static enum EnumCasingType {
        SMALL, LARGE, SMALL_STEEL, LARGE_STEEL, SHOTSHELL, BUCKSHOT, BUCKSHOT_ADVANCED;

        public ItemStack stack() {
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hbm_m", "casing_" + name().toLowerCase(java.util.Locale.US)));
            return new ItemStack(item);
        }
    }

    /** Original {@code getDamage}: SEDNA-Schadensquelle je Schadensklasse, mit oder ohne Schuetzen. */
    public static DamageSource getDamage(Entity projectile, @Nullable LivingEntity shooter, DamageClass dmgClass) {
        return SednaDamage.create(projectile, shooter, dmgClass);
    }

    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_STANDARD_RICOCHET = (bullet, mop) -> {

        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {

            BlockPos pos = mop.getBlockPos();
            BlockState state = bullet.level().getBlockState(pos);
            Block b = state.getBlock();
            //? if < 1.21.1 {
            if (state.getSoundType() == net.minecraft.world.level.block.SoundType.GLASS || state.is(net.minecraftforge.common.Tags.Blocks.GLASS) || state.is(net.minecraftforge.common.Tags.Blocks.GLASS_PANES)) {
            //?} else {
            /*if (state.getSoundType() == net.minecraft.world.level.block.SoundType.GLASS || state.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_BLOCKS) || state.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_PANES)) {
            *///?}
                bullet.level().destroyBlock(pos, false);
                bullet.setPosition(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
                return;
            }
            if (b instanceof BlockDetonatable det) {
                det.onShot(bullet.level(), pos);
            }
            if (BulletConfig.crtShotHandler != null) BulletConfig.crtShotHandler.accept(bullet.level(), pos);

            ForgeDirection dir = ForgeDirection.getOrientation(mop.sideHit);
            Vec3NT face = Vec3NT.createVectorHelper(dir.offsetX, dir.offsetY, dir.offsetZ);
            var m = bullet.getDeltaMovement();
            Vec3NT vel = Vec3NT.createVectorHelper(m.x, m.y, m.z).normalize();

            double angle = Math.abs(BobMathUtil.getCrossAngle(vel, face) - 90);

            if (angle <= bullet.config.ricochetAngle) {

                bullet.ricochets++;
                if (bullet.ricochets > bullet.config.maxRicochetCount) {
                    bullet.setPosition(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
                    bullet.setDead();
                }

                double mx = m.x, my = m.y, mz = m.z;
                switch (mop.sideHit) {
                    case 0, 1 -> my *= -1;
                    case 2, 3 -> mz *= -1;
                    case 4, 5 -> mx *= -1;
                    default -> { }
                }
                bullet.setDeltaMovement(mx, my, mz);
                bullet.level().playSound(null, bullet.getX(), bullet.getY(), bullet.getZ(), com.hbm_m.sound.HbmSoundsNT.get("hbm:weapon.ricochet"), SoundSource.PLAYERS, 0.25F, 1.0F);
                bullet.setPosition(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
                //send a teleport so the ricochet is more accurate instead of the interp smoothing fucking everything up
                bullet.hasImpulse = true;
                return;

            } else {
                bullet.setPosition(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
                bullet.setDead();
            }
        }
    };

    /** Original-Sonderfall {@code deco_crt}: getroffene Bildschirme gehen kaputt (wird vom Block eingetragen). */
    @Nullable
    public static BiConsumer<net.minecraft.world.level.Level, BlockPos> crtShotHandler;

    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_STANDARD_ENTITY_HIT = (bullet, mop) -> {

        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            Entity entity = mop.entityHit;

            if (entity == bullet.getThrower() && bullet.tickCount < bullet.selfDamageDelay()) return;
            if (entity instanceof LivingEntity l && l.getHealth() <= 0) return;

            DamageSource source = getDamage(bullet, bullet.getThrower(), bullet.config.dmgClass);
            float intendedDamage = bullet.damage;

            if (!(entity instanceof LivingEntity living)) {
                EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, source, bullet.damage);
                return;
            }

            if (bullet.config.headshotMult > 1F) {
                double head = living.getBbHeight() - living.getEyeHeight();
                if (living.isAlive() && mop.hitVec != null && mop.hitVec.yCoord > (living.getY() + living.getBbHeight() - head * 2)) {
                    intendedDamage *= bullet.config.headshotMult;
                }
            }

            float prevHealth = living.getHealth();

            EntityDamageUtil.attackEntityFromNT(living, source, intendedDamage, true, true, bullet.config.knockbackMult, bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent);

            float newHealth = living.getHealth();

            if (bullet.config.damageFalloffByPen) bullet.damage -= Math.max(prevHealth - newHealth, 0) * 0.5;
            if (!bullet.doesPenetrate() || bullet.damage < 0) {
                bullet.setPosition(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
                bullet.setDead();
            }

            if (!living.isAlive()) ConfettiUtil.decideConfetti(living, source);
        }
    };

    public static BiConsumer<EntityBulletBeamBase, MovingObjectPosition> LAMBDA_STANDARD_BEAM_HIT = (bullet, mop) -> {

        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            Entity entity = mop.entityHit;

            if (entity instanceof LivingEntity l && l.getHealth() <= 0) return;

            DamageSource source = getDamage(bullet, bullet.getThrower(), bullet.config.dmgClass);

            if (!(entity instanceof LivingEntity living)) {
                EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, source, bullet.damage);
                return;
            }

            EntityDamageUtil.attackEntityFromNT(living, source, bullet.damage, true, true, bullet.config.knockbackMult, bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent);
            if (!living.isAlive()) ConfettiUtil.decideConfetti(living, source);
        }
    };

    public static BiConsumer<EntityBulletBeamBase, MovingObjectPosition> LAMBDA_BEAM_HIT = (beam, mop) -> {

        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            Entity entity = mop.entityHit;

            if (entity instanceof LivingEntity l && l.getHealth() <= 0) return;

            DamageSource source = getDamage(beam, beam.thrower, beam.config.dmgClass);

            if (!(entity instanceof LivingEntity living)) {
                EntityDamageUtil.attackEntityFromIgnoreIFrame(entity, source, beam.damage);
                return;
            }

            EntityDamageUtil.attackEntityFromNT(living, source, beam.damage, true, false, beam.config.knockbackMult, beam.config.armorThresholdNegation, beam.config.armorPiercingPercent);
        }
    };

    @Override
    public BulletConfig clone() {
        try {
            BulletConfig clone = (BulletConfig) super.clone();
            clone.forceReRegister();
            return clone;
        } catch (CloneNotSupportedException e) { }
        return null;
    }
}
