package com.hbm_m.item.weapon.sedna.factory;

import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.hbm_m.entity.projectile.EntityBulletBaseMK4;
import com.hbm_m.entity.projectile.EntityBulletBaseMK4CL;
import com.hbm_m.entity.projectile.EntityBulletBeamBase;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm_m.explosion.vanillant.standard.ExplosionEffectTiny;
import com.hbm_m.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm_m.item.weapon.sedna.BulletConfig;
import com.hbm_m.item.weapon.sedna.BulletConfig.ProjectileType;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.SmokeNode;
import com.hbm_m.item.weapon.sedna.Receiver;
import com.hbm_m.item.weapon.sedna.mags.IMagazine;
import com.hbm_m.particle.helper.BlackPowderCreator;
import com.hbm_m.render.anim.AnimationEnums.GunAnimation;
import com.hbm_m.render.anim.BusAnimation;
import com.hbm_m.render.anim.BusAnimationSequence;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.ForgeDirection;
import com.hbm_m.util.MovingObjectPosition;
import com.hbm_m.util.Vec3NT;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code Lego}: "LEGO" - i.e. standardized building blocks which can be used to set up gun configs easily.
 *
 * small update, 24/11/03: this turned into fucking spaghetti. fuuuuuuuck.
 *
 * @author hbm
 */
public class Lego {

    public static final Random ANIM_RAND = new Random();

    /** Original {@code worldObj.playSoundEffect(x, y, z, "hbm:...", vol, pitch)} - Schluessel wie im Original. */
    public static void playSound(Entity entity, String sound, float volume, float pitch) {
        SoundEvent ev = HbmSoundsNT.get(sound);
        if (ev != null) entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), ev, SoundSource.PLAYERS, volume, pitch);
    }

    /**
     * If IDLE and the mag of receiver 0 can be loaded, set state to RELOADING. Used by keybinds. */
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_RELOAD = (stack, ctx) -> {

        Player player = ctx.getPlayer();
        Receiver rec = ctx.config.getReceivers(stack)[0];
        GunState state = ItemGunBaseNT.getState(stack, ctx.configIndex);

        if (state == GunState.IDLE) {

            ItemGunBaseNT.setIsAiming(stack, false);
            IMagazine<?> mag = rec.getMagazine(stack);

            if (mag.canReload(stack, ctx.inventory)) {
                int loaded = mag.getAmount(stack, ctx.inventory);
                mag.setAmountBeforeReload(stack, loaded);
                ItemGunBaseNT.setState(stack, ctx.configIndex, GunState.RELOADING);
                ItemGunBaseNT.setTimer(stack, ctx.configIndex, rec.getReloadBeginDuration(stack) + (loaded <= 0 ? rec.getReloadCockOnEmptyPre(stack) : 0));
                ItemGunBaseNT.playAnimation(player, stack, GunAnimation.RELOAD, ctx.configIndex);
                if (ctx.config.getReloadChangesType(stack)) mag.initNewType(stack, ctx.inventory);
            } else {
                ItemGunBaseNT.playAnimation(player, stack, GunAnimation.INSPECT, ctx.configIndex);
                if (!ctx.config.getInspectCancel(stack)) {
                    ItemGunBaseNT.setState(stack, ctx.configIndex, GunState.DRAWING);
                    ItemGunBaseNT.setTimer(stack, ctx.configIndex, ctx.config.getInspectDuration(stack));
                }
            }
        }
    };

    /** If IDLE and ammo is loaded, fire and set to JUST_FIRED. */
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_CLICK_PRIMARY = (stack, ctx) -> { clickReceiver(stack, ctx, 0); };

    public static void clickReceiver(ItemStack stack, LambdaContext ctx, int receiver) {

        LivingEntity entity = ctx.entity;
        Player player = ctx.getPlayer();
        Receiver rec = ctx.config.getReceivers(stack)[receiver];
        int index = ctx.configIndex;
        GunState state = ItemGunBaseNT.getState(stack, index);

        if (state == GunState.IDLE) {

            if (rec.getCanFire(stack).apply(stack, ctx)) {
                rec.getOnFire(stack).accept(stack, ctx);

                if (rec.getFireSound(stack) != null) playSound(entity, rec.getFireSound(stack), rec.getFireVolume(stack), rec.getFirePitch(stack));

                int remaining = rec.getRoundsPerCycle(stack) - 1;
                for (int i = 0; i < remaining; i++) if (rec.getCanFire(stack).apply(stack, ctx)) rec.getOnFire(stack).accept(stack, ctx);

                ItemGunBaseNT.setState(stack, index, GunState.COOLDOWN);
                ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterFire(stack));
            } else {

                if (rec.getDoesDryFire(stack)) {
                    ItemGunBaseNT.playAnimation(player, stack, GunAnimation.CYCLE_DRY, index);
                    ItemGunBaseNT.setState(stack, index, rec.getRefireAfterDry(stack) ? GunState.COOLDOWN : GunState.DRAWING);
                    ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterDryFire(stack));
                }
            }
        }

        if (state == GunState.RELOADING) {
            ItemGunBaseNT.setReloadCancel(stack, true);
        }
    }

    /** If IDLE, switch mode between 0 and 1. */
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_CLICK_SECONDARY = (stack, ctx) -> {

        LivingEntity entity = ctx.entity;
        int index = ctx.configIndex;
        GunState state = ItemGunBaseNT.getState(stack, index);

        if (state == GunState.IDLE) {
            int mode = ItemGunBaseNT.getMode(stack, 0);
            ItemGunBaseNT.setMode(stack, index, 1 - mode);
            if (mode == 0)
                playSound(entity, "hbm:weapon.switchmode1", 1F, 1F);
            else
                playSound(entity, "hbm:weapon.switchmode2", 1F, 1F);
        }
    };

    /** Default smoke. */
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_SMOKE = (stack, ctx) -> {
        handleStandardSmoke(ctx.entity, stack, 2000, 0.025D, 1.15D, ctx.configIndex);
    };

    public static void handleStandardSmoke(LivingEntity entity, ItemStack stack, int smokeDuration, double alphaDecay, double widthGrowth, int index) {
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        long lastShot = gun.lastShot[index];
        List<SmokeNode> smokeNodes = gun.getConfig(stack, index).smokeNodes;

        boolean smoking = lastShot + smokeDuration > System.currentTimeMillis();
        if (!smoking && !smokeNodes.isEmpty()) smokeNodes.clear();

        if (smoking) {
            Vec3 m = entity.getDeltaMovement();
            Vec3NT prev = Vec3NT.createVectorHelper(-m.x, -m.y, -m.z);
            prev.rotateAroundY((float) (entity.getYRot() * Math.PI / 180D));
            double accel = 15D;
            double side = (entity.getYRot() - entity.yHeadRotO) * 0.1D;
            double waggle = 0.025D;

            for (SmokeNode node : smokeNodes) {
                node.forward += -prev.zCoord * accel + entity.level().random.nextGaussian() * waggle;
                node.lift += prev.yCoord + 1.5D;
                node.side += prev.xCoord * accel + entity.level().random.nextGaussian() * waggle + side;
                if (node.alpha > 0) node.alpha -= alphaDecay;
                node.width *= widthGrowth;
            }

            double alpha = (System.currentTimeMillis() - lastShot) / (double) smokeDuration;
            alpha = (1 - alpha) * 0.5D;

            if (ItemGunBaseNT.getState(stack, index) == GunState.RELOADING || smokeNodes.size() == 0) alpha = 0;
            smokeNodes.add(new SmokeNode(alpha));
        }
    }

    /** Toggles isAiming. Used by keybinds. */
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_TOGGLE_AIM = (stack, ctx) -> { ItemGunBaseNT.setIsAiming(stack, !ItemGunBaseNT.getIsAiming(stack)); };

    /** Returns true if the mag has ammo in it. Used by keybind functions on whether to fire, and deciders on whether to trigger a refire. */
    public static BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_STANDARD_CAN_FIRE = (stack, ctx) -> { return ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmount(stack, ctx.inventory) > 0; };
    public static BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_SECOND_CAN_FIRE = (stack, ctx) -> { return ctx.config.getReceivers(stack)[1].getMagazine(stack).getAmount(stack, ctx.inventory) > 0; };

    /** Returns true if the mag has ammo in it, and the gun is in the locked on state */
    public static BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_LOCKON_CAN_FIRE = (stack, ctx) -> { return ctx.config.getReceivers(stack)[0].getMagazine(stack).getAmount(stack, ctx.inventory) > 0 && ItemGunBaseNT.getIsLockedOn(stack); };

    /** JUMPER - bypasses mag testing and just allows constant fire */
    public static BiFunction<ItemStack, LambdaContext, Boolean> LAMBDA_DEBUG_CAN_FIRE = (stack, ctx) -> { return true; };

    /** Spawns an EntityBulletBaseMK4 with the loaded bulletcfg */
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_STANDARD_FIRE = (stack, ctx) -> {
        doStandardFire(stack, ctx, GunAnimation.CYCLE, 0, true);
    };
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SECOND_FIRE = (stack, ctx) -> {
        doStandardFire(stack, ctx, GunAnimation.CYCLE, 1, true);
    };
    /** Spawns an EntityBulletBaseMK4 with the loaded bulletcfg, ignores wear */
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_NOWEAR_FIRE = (stack, ctx) -> {
        doStandardFire(stack, ctx, GunAnimation.CYCLE, 0, false);
    };
    /** Spawns an EntityBulletBaseMK4 with the loaded bulletcfg, then resets lockon progress */
    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_LOCKON_FIRE = (stack, ctx) -> {
        doStandardFire(stack, ctx, GunAnimation.CYCLE, 0, true);
        ItemGunBaseNT.setIsLockedOn(stack, false);
    };

    public static void doStandardFire(ItemStack stack, LambdaContext ctx, GunAnimation anim, int receiver, boolean calcWear) {
        LivingEntity entity = ctx.entity;
        Player player = ctx.getPlayer();
        int index = ctx.configIndex;
        if (anim != null) ItemGunBaseNT.playAnimation(player, stack, anim, ctx.configIndex);

        boolean aim = ItemGunBaseNT.getIsAiming(stack);
        Receiver primary = ctx.config.getReceivers(stack)[receiver];
        IMagazine<?> mag = primary.getMagazine(stack);
        BulletConfig config = (BulletConfig) mag.getType(stack, ctx.inventory);

        Vec3NT offset = ItemGunBaseNT.getIsAiming(stack) ? primary.getProjectileOffsetScoped(stack) : primary.getProjectileOffset(stack);
        double forwardOffset = offset.xCoord;
        double heightOffset = offset.yCoord;
        double sideOffset = offset.zCoord;

        int projectiles = config.projectilesMin;
        if (config.projectilesMax > config.projectilesMin) projectiles += entity.getRandom().nextInt(config.projectilesMax - config.projectilesMin + 1);
        projectiles = (int) (projectiles * primary.getSplitProjectiles(stack));

        for (int i = 0; i < projectiles; i++) {
            float damage = calcDamage(ctx, stack, primary, calcWear, index);
            float spread = calcSpread(ctx, stack, primary, config, calcWear, index, aim);

            if (config.pType == ProjectileType.BULLET) {
                EntityBulletBaseMK4 mk4 = new EntityBulletBaseMK4(entity, config, damage, spread, sideOffset, heightOffset, forwardOffset);
                if (ItemGunBaseNT.getIsLockedOn(stack)) mk4.lockonTarget = entity.level().getEntity(ItemGunBaseNT.getLockonTarget(stack));
                if (i == 0 && config.blackPowder && entity.level() instanceof ServerLevel sl) { Vec3 mm = mk4.getDeltaMovement(); BlackPowderCreator.composeEffect(sl, mk4.getX(), mk4.getY(), mk4.getZ(), mm.x, mm.y, mm.z, 10, 0.25F, 0.5F, 10, 0.25F); }
                entity.level().addFreshEntity(mk4);
            } else if (config.pType == ProjectileType.BULLET_CHUNKLOADING) {
                EntityBulletBaseMK4CL mk4 = new EntityBulletBaseMK4CL(entity, config, damage, spread, sideOffset, heightOffset, forwardOffset);
                if (ItemGunBaseNT.getIsLockedOn(stack)) mk4.lockonTarget = entity.level().getEntity(ItemGunBaseNT.getLockonTarget(stack));
                if (i == 0 && config.blackPowder && entity.level() instanceof ServerLevel sl) { Vec3 mm = mk4.getDeltaMovement(); BlackPowderCreator.composeEffect(sl, mk4.getX(), mk4.getY(), mk4.getZ(), mm.x, mm.y, mm.z, 10, 0.25F, 0.5F, 10, 0.25F); }
                entity.level().addFreshEntity(mk4);
            } else if (config.pType == ProjectileType.BEAM) {
                EntityBulletBeamBase mk4 = new EntityBulletBeamBase(entity, config, damage, spread, sideOffset, heightOffset, forwardOffset);
                entity.level().addFreshEntity(mk4);
            }
        }

        if (player != null) player.awardStat(com.hbm_m.advancement.ModStats.BULLETS.get());
        mag.useUpAmmo(stack, ctx.inventory, 1);
        if (calcWear) ItemGunBaseNT.setWear(stack, index, Math.min(ItemGunBaseNT.getWear(stack, index) + config.wear, ctx.config.getDurability(stack)));
    }

    public static float getStandardWearSpread(ItemStack stack, com.hbm_m.item.weapon.sedna.GunConfig config, int index) {
        float percent = ItemGunBaseNT.getWear(stack, index) / config.getDurability(stack);
        if (percent < 0.5F) return 0F;
        return (percent - 0.5F) * 2F;
    }

    /** Returns the standard multiplier for damage based on wear */
    public static float getStandardWearDamage(ItemStack stack, com.hbm_m.item.weapon.sedna.GunConfig config, int index) {
        float percent = ItemGunBaseNT.getWear(stack, index) / config.getDurability(stack);
        if (percent < 0.75F) return 1F;
        return 1F - (percent - 0.75F) * 2F;
    }

    /** Returns the full calculated damage based on guncfg and wear */
    public static float calcDamage(LambdaContext ctx, ItemStack stack, Receiver primary, boolean calcWear, int index) {
        return primary.getBaseDamage(stack) * (calcWear ? getStandardWearDamage(stack, ctx.config, index) : 1);
    }

    public static float calcSpread(LambdaContext ctx, ItemStack stack, Receiver primary, BulletConfig config, boolean calcWear, int index, boolean aim) {
        // the gun's innate spread, SMGs will have poor accuracy no matter what
        float spreadInnate = primary.getInnateSpread(stack);
        // the ammo's spread (for example for buckshot) multiplied with the gun's ammo modifier (choke or sawed off barrel)
        float spreadAmmo = config.spread * primary.getAmmoSpread(stack);
        // hipfire penalty, i.e. extra spread when not aiming
        float spreadHipfire = aim ? 0F : primary.getHipfireSpread(stack);
        // extra spread caused by weapon durability, [0;0.125] by default
        float spreadWear = !calcWear ? 0F : (getStandardWearSpread(stack, ctx.config, index) * primary.getDurabilitySpread(stack));

        return spreadInnate + spreadAmmo + spreadHipfire + spreadWear;
    }

    public static void standardExplode(EntityBulletBaseMK4 bullet, MovingObjectPosition mop, float range) { standardExplode(bullet, mop, range, 1F); }
    public static void standardExplode(EntityBulletBaseMK4 bullet, MovingObjectPosition mop, float range, float damageMod) {
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, range, bullet.getThrower());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage * damageMod).setupPiercing(bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
    }

    public static void tinyExplode(EntityBulletBaseMK4 bullet, MovingObjectPosition mop, float range) { tinyExplode(bullet, mop, range, 1F); }
    public static void tinyExplode(EntityBulletBaseMK4 bullet, MovingObjectPosition mop, float range, float damageMod) {
        ForgeDirection dir = ForgeDirection.getOrientation(mop.sideHit);
        double x = mop.hitVec.xCoord + dir.offsetX * 0.25D;
        double y = mop.hitVec.yCoord + dir.offsetY * 0.25D;
        double z = mop.hitVec.zCoord + dir.offsetZ * 0.25D;
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), x, y, z, range, bullet.getThrower());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(0.5, bullet.damage * damageMod)
                .setupPiercing(bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent).setKnockback(0.25D));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectTiny());
        vnt.explode();
    }

    /** anims for the DEBUG revolver, mostly a copy of the li'lpip but with some fixes regarding the cylinder movement */
    public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_DEBUG_ANIMS = (stack, type) -> {
        switch (type) {
            case CYCLE: return new BusAnimation()
                    .addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, 0, 50).addPos(0, 0, -3, 50).addPos(0, 0, 0, 250))
                    .addBus("HAMMER", new BusAnimationSequence().addPos(0, 0, 1, 50).addPos(0, 0, 1, 400).addPos(0, 0, 0, 200))
                    .addBus("DRUM", new BusAnimationSequence().addPos(0, 0, 0, 450).addPos(0, 0, 1, 200));
            case CYCLE_DRY: return new BusAnimation()
                    .addBus("HAMMER", new BusAnimationSequence().addPos(0, 0, 1, 50).addPos(0, 0, 1, 300 + 100).addPos(0, 0, 0, 200))
                    .addBus("DRUM", new BusAnimationSequence().addPos(0, 0, 0, 450).addPos(0, 0, 1, 200));
            case EQUIP: return new BusAnimation().addBus("ROTATE", new BusAnimationSequence().addPos(-360, 0, 0, 350));
            case RELOAD: return new BusAnimation()
                    .addBus("RELAOD_TILT", new BusAnimationSequence().addPos(-15, 0, 0, 100).addPos(65, 0, 0, 100).addPos(45, 0, 0, 50).addPos(0, 0, 0, 200).addPos(0, 0, 0, 1450).addPos(-80, 0, 0, 100).addPos(-80, 0, 0, 100).addPos(0, 0, 0, 200))
                    .addBus("RELOAD_CYLINDER", new BusAnimationSequence().addPos(0, 0, 0, 200).addPos(90, 0, 0, 100).addPos(90, 0, 0, 1700).addPos(0, 0, 0, 70))
                    .addBus("RELOAD_LIFT", new BusAnimationSequence().addPos(0, 0, 0, 350).addPos(-45, 0, 0, 250).addPos(-45, 0, 0, 350).addPos(-15, 0, 0, 200).addPos(-15, 0, 0, 1050).addPos(0, 0, 0, 100))
                    .addBus("RELOAD_JOLT", new BusAnimationSequence().addPos(0, 0, 0, 600).addPos(2, 0, 0, 50).addPos(0, 0, 0, 100))
                    .addBus("RELOAD_BULLETS", new BusAnimationSequence().addPos(0, 0, 0, 650).addPos(10, 0, 0, 300).addPos(10, 0, 0, 200).addPos(0, 0, 0, 700))
                    .addBus("RELOAD_BULLETS_CON", new BusAnimationSequence().addPos(1, 0, 0, 0).addPos(1, 0, 0, 950).addPos(0, 0, 0, 1));
            case INSPECT:
            case JAMMED: return new BusAnimation()
                    .addBus("RELAOD_TILT", new BusAnimationSequence().addPos(-15, 0, 0, 100).addPos(65, 0, 0, 100).addPos(45, 0, 0, 50).addPos(0, 0, 0, 200).addPos(0, 0, 0, 200).addPos(-80, 0, 0, 100).addPos(-80, 0, 0, 100).addPos(0, 0, 0, 200))
                    .addBus("RELOAD_CYLINDER", new BusAnimationSequence().addPos(0, 0, 0, 200).addPos(90, 0, 0, 100).addPos(90, 0, 0, 450).addPos(0, 0, 0, 70));
            default: return null;
        }
    };
}
