package com.hbm_m.item.weapon.sedna.factory;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.effect.EntityFireLingering;
import com.hbm_m.entity.logic.EntityC130;
import com.hbm_m.entity.logic.EntityC130.C130PayloadType;
import com.hbm_m.entity.projectile.EntityBulletBaseMK4;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm_m.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.item.weapon.sedna.BulletConfig;
import com.hbm_m.item.weapon.sedna.BulletConfig.EnumCasingType;
import com.hbm_m.item.weapon.sedna.Crosshair;
import com.hbm_m.item.weapon.sedna.GunConfig;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm_m.item.weapon.sedna.Receiver;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm_m.item.weapon.sedna.mags.MagazineFullReload;
import com.hbm_m.item.weapon.sedna.mags.MagazineSingleReload;
import com.hbm_m.particle.SpentCasing;
import com.hbm_m.particle.SpentCasing.CasingType;
import com.hbm_m.powerarmor.resist.DamageResistanceHandler.DamageClass;
import com.hbm_m.render.anim.AnimationEnums.GunAnimation;
import com.hbm_m.render.anim.BusAnimation;
import com.hbm_m.render.anim.BusAnimationKeyframe.IType;
import com.hbm_m.render.anim.BusAnimationSequence;
import com.hbm_m.util.EntityDamageUtil;
import com.hbm_m.util.ForgeDirection;
import com.hbm_m.util.MovingObjectPosition;
import com.hbm_m.util.WorldUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/** 1:1 {@code XFactory40mm}: Signalpistole (mit C-130-Abwurf), Congo Lake und MK108. */
public class XFactory40mm {

    public static BulletConfig g26_flare;
    public static BulletConfig g26_flare_supply;
    public static BulletConfig g26_flare_weapon;

    public static BulletConfig g40_he;
    public static BulletConfig g40_heat;
    public static BulletConfig g40_demo;
    public static BulletConfig g40_inc;
    public static BulletConfig g40_phosphorus;

    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_STANDARD_IGNITE = (bullet, mop) -> {
        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            if (mop.entityHit instanceof LivingEntity living) {
                HbmLivingProps.setFire(living, HbmLivingProps.getFire(living) + 200);
            }
        }
    };
    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_STANDARD_EXPLODE = (bullet, mop) -> {
        Lego.standardExplode(bullet, mop, 5F); bullet.setDead();
    };
    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_STANDARD_EXPLODE_HEAT = (bullet, mop) -> {
        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && bullet.tickCount < 3 && mop.entityHit == bullet.getThrower()) return;
        Lego.standardExplode(bullet, mop, 3.5F); bullet.setDead();
        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mop.entityHit instanceof LivingEntity living) {
            EntityDamageUtil.attackEntityFromNT(living, BulletConfig.getDamage(bullet, bullet.getThrower(), DamageClass.EXPLOSIVE), bullet.damage * 3F, true, true, 0.5F, 3F, 0.15F);
        } else if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            mop.entityHit.hurt(BulletConfig.getDamage(bullet, bullet.getThrower(), DamageClass.EXPLOSIVE), bullet.damage * 3F);
        }
    };
    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_STANDARD_EXPLODE_DEMO = (bullet, mop) -> {
        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && bullet.tickCount < 3 && mop.entityHit == bullet.getThrower()) return;
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, 5F, bullet.getThrower());
        vnt.setBlockAllocator(new BlockAllocatorStandard());
        vnt.setBlockProcessor(new BlockProcessorStandard());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
        bullet.setDead();
    };
    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_STANDARD_EXPLODE_INC = (bullet, mop) -> {
        spawnFire(bullet, mop, false, 200);
    };
    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_STANDARD_EXPLODE_PHOSPHORUS = (bullet, mop) -> {
        spawnFire(bullet, mop, true, 400);
    };

    public static void spawnFire(EntityBulletBaseMK4 bullet, MovingObjectPosition mop, boolean phosphorus, int duration) {
        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && bullet.tickCount < 3 && mop.entityHit == bullet.getThrower()) return;
        Level world = bullet.level();
        Lego.standardExplode(bullet, mop, 3F);
        EntityFireLingering fire = new EntityFireLingering(world).setArea(5, 2).setDuration(duration).setType(phosphorus ? EntityFireLingering.TYPE_PHOSPHORUS : EntityFireLingering.TYPE_DIESEL);
        fire.setPosition(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
        world.addFreshEntity(fire);
        bullet.setDead();
        placeFire(world, mop, 1);
    }

    /** Original-Schleife: luftige Bloecke neben brennbaren Bloecken in Brand setzen. */
    public static void placeFire(Level world, MovingObjectPosition mop, int range) {
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    int x = (int) Math.floor(mop.hitVec.xCoord) + dx;
                    int y = (int) Math.floor(mop.hitVec.yCoord) + dy;
                    int z = (int) Math.floor(mop.hitVec.zCoord) + dz;
                    BlockPos pos = new BlockPos(x, y, z);
                    if (world.getBlockState(pos).isAir()) for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                        BlockPos n = pos.offset(dir.offsetX, dir.offsetY, dir.offsetZ);
                        if (world.getBlockState(n).isFlammable(world, n, dir.getOpposite().toDirection())) {
                            world.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                            break;
                        }
                    }
                }
            }
        }
    }

    public static Consumer<Entity> LAMBDA_SPAWN_C130_SUPPLIESS = (entity) -> { spawnPlane(entity, C130PayloadType.SUPPLIES); };
    public static Consumer<Entity> LAMBDA_SPAWN_C130_WEAPONS = (entity) -> { spawnPlane(entity, C130PayloadType.WEAPONS); };

    public static void spawnPlane(Entity entity, C130PayloadType payload) {
        if (!entity.level().isClientSide && entity.tickCount == 40) {
            EntityBulletBaseMK4 bullet = (EntityBulletBaseMK4) entity;
            if (bullet.getThrower() != null) Lego.playSound(bullet.getThrower(), "hbm:item.techBleep", 1.0F, 1.0F);
            EntityC130 c130 = new EntityC130(ModEntities.C130.get(), bullet.level());
            int x = (int) Math.floor(bullet.getX());
            int z = (int) Math.floor(bullet.getZ());
            int y = bullet.level().getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            c130.fac(bullet.level(), x, y, z, payload);
            WorldUtil.loadAndSpawnEntityInWorld(c130);
            // Original TrackerUtil.setTrackingRange(250): der Entity-Typ C130 hat bereits Tracking 1000
        }
    }

    public static void init() {

        g26_flare = new BulletConfig().setItem(EnumAmmo.G26_FLARE).setCasing(EnumCasingType.LARGE, 4).setLife(100).setVel(2F).setGrav(0.015D).setRenderRotations(false).setOnImpact(LAMBDA_STANDARD_IGNITE).setCasing(new SpentCasing(CasingType.STRAIGHT).setColor(0x9E1616).setScale(2F).register("g26Flare"));
        g26_flare_supply = new BulletConfig().setItem(EnumAmmo.G26_FLARE_SUPPLY).setCasing(EnumCasingType.LARGE, 4).setLife(100).setVel(2F).setGrav(0.015D).setRenderRotations(false).setOnImpact(LAMBDA_STANDARD_IGNITE).setOnUpdate(LAMBDA_SPAWN_C130_SUPPLIESS).setCasing(new SpentCasing(CasingType.STRAIGHT).setColor(0x3C80F0).setScale(2F).register("g26FlareSupply"));
        g26_flare_weapon = new BulletConfig().setItem(EnumAmmo.G26_FLARE_WEAPON).setCasing(EnumCasingType.LARGE, 4).setLife(100).setVel(2F).setGrav(0.015D).setRenderRotations(false).setOnImpact(LAMBDA_STANDARD_IGNITE).setOnUpdate(LAMBDA_SPAWN_C130_WEAPONS).setCasing(new SpentCasing(CasingType.STRAIGHT).setColor(0x278400).setScale(2F).register("g26FlareWeapon"));

        BulletConfig g40_base = new BulletConfig().setLife(200).setVel(2F).setGrav(0.035D);
        g40_he = g40_base.clone().setItem(EnumAmmo.G40_HE).setCasing(EnumCasingType.LARGE, 4).setOnImpact(LAMBDA_STANDARD_EXPLODE).setCasing(new SpentCasing(CasingType.STRAIGHT).setColor(0x777777).setScale(2, 2F, 1.5F).register("g40"));
        g40_heat = g40_base.clone().setItem(EnumAmmo.G40_HEAT).setCasing(EnumCasingType.LARGE, 4).setOnImpact(LAMBDA_STANDARD_EXPLODE_HEAT).setDamage(0.5F).setCasing(new SpentCasing(CasingType.STRAIGHT).setColor(0x5E6854).setScale(2, 2F, 1.5F).register("g40heat"));
        g40_demo = g40_base.clone().setItem(EnumAmmo.G40_DEMO).setCasing(EnumCasingType.LARGE, 4).setOnImpact(LAMBDA_STANDARD_EXPLODE_DEMO).setDamage(0.75F).setCasing(new SpentCasing(CasingType.STRAIGHT).setColor(0xE30000).setScale(2, 2F, 1.5F).register("g40demo"));
        g40_inc = g40_base.clone().setItem(EnumAmmo.G40_INC).setCasing(EnumCasingType.LARGE, 4).setOnImpact(LAMBDA_STANDARD_EXPLODE_INC).setDamage(0.75F).setCasing(new SpentCasing(CasingType.STRAIGHT).setColor(0xE86F20).setScale(2, 2F, 1.5F).register("g40inc"));
        g40_phosphorus = g40_base.clone().setItem(EnumAmmo.G40_PHOSPHORUS).setCasing(EnumCasingType.LARGE, 4).setOnImpact(LAMBDA_STANDARD_EXPLODE_PHOSPHORUS).setDamage(0.75F).setCasing(new SpentCasing(CasingType.STRAIGHT).setColor(0xC8C8C8).setScale(2, 2F, 1.5F).register("g40phos"));

        GunFactory.reg("gun_flaregun", new ItemGunBaseNT(WeaponQuality.A_SIDE, new GunConfig()
                .dura(100).draw(7).inspect(39).crosshair(Crosshair.L_CIRCUMFLEX).smoke(LAMBDA_SMOKE)
                .rec(new Receiver(0)
                        .dmg(15F).delay(20).reload(28).jam(33).sound("hbm:weapon.hkShoot", 1.0F, 1.0F)
                        .mag(new MagazineSingleReload(0, 1).addConfigs(g26_flare, g26_flare_supply, g26_flare_weapon))
                        .offset(0.75, -0.0625, -0.1875D)
                        .setupStandardFire().recoil(LAMBDA_RECOIL_GL))
                .setupStandardConfiguration()
                .anim(LAMBDA_FLAREGUN_ANIMS).orchestra(Orchestras.ORCHESTRA_FLAREGUN)
                ).setDefaultAmmo(EnumAmmo.G26_FLARE, 3));

        GunFactory.reg("gun_congolake", new ItemGunBaseNT(WeaponQuality.A_SIDE, new GunConfig()
                .dura(400).draw(7).inspect(39).reloadSequential(true).reloadChangeType(true).crosshair(Crosshair.L_CIRCUMFLEX).smoke(LAMBDA_SMOKE)
                .rec(new Receiver(0)
                        .dmg(20F).delay(24).reload(16, 16, 16, 0).jam(0).sound("hbm:weapon.glShoot", 1.0F, 1.0F)
                        .mag(new MagazineSingleReload(0, 4).addConfigs(g40_he, g40_heat, g40_demo, g40_inc, g40_phosphorus))
                        .offset(0.75, -0.0625, -0.1875D)
                        .setupStandardFire().recoil(LAMBDA_RECOIL_GL))
                .setupStandardConfiguration()
                .anim(LAMBDA_CONGOLAKE_ANIMS).orchestra(Orchestras.ORCHESTRA_CONGOLAKE)
                ).setDefaultAmmo(EnumAmmo.G40_HE, 8));

        GunFactory.reg("gun_mk108", new ItemGunBaseNT(WeaponQuality.A_SIDE, new GunConfig()
                .dura(5_000).draw(20).inspect(65).crosshair(Crosshair.L_CIRCUMFLEX).hideCrosshair(false)
                .rec(new Receiver(0)
                        .dmg(25F).delay(10).auto(true).dryfireAfterAuto(true).reload(135).jam(25).sound("hbm:weapon.fire.mk108", 1.0F, 1.0F)
                        .mag(new MagazineFullReload(0, 30).addConfigs(g40_he, g40_heat, g40_demo, g40_inc, g40_phosphorus))
                        .offset(0.75, -0.125, -0.125)
                        .setupStandardFire().recoil(LAMBDA_RECOIL_MK108))
                .setupStandardConfiguration()
                .anim(LAMBDA_MK108_ANIMS).orchestra(Orchestras.ORCHESTRA_MK108)
                ).setDefaultAmmo(EnumAmmo.G40_HE, 50));
    }

    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SMOKE = (stack, ctx) -> {
        Lego.handleStandardSmoke(ctx.entity, stack, 1500, 0.025D, 1.05D, 0);
    };

    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_GL = (stack, ctx) -> {
        ItemGunBaseNT.setupRecoil(10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
    };

    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_MK108 = (stack, ctx) -> {
        ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 1.0) + 1F, (float) (ctx.getPlayer().getRandom().nextGaussian()));
    };

    @SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_FLAREGUN_ANIMS = (stack, type) -> {
        switch (type) {
            case EQUIP: return new BusAnimation()
                    .addBus("EQUIP", new BusAnimationSequence().addPos(-90, 0, 0, 0).addPos(0, 0, 0, 350, IType.SIN_DOWN));
            case CYCLE: return new BusAnimation()
                    .addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, 0, 50).addPos(0, 0, -3, 50).addPos(0, 0, 0, 250))
                    .addBus("HAMMER", new BusAnimationSequence().addPos(15, 0, 0, 50).addPos(15, 0, 0, 550).addPos(0, 0, 0, 100));
            case CYCLE_DRY: return new BusAnimation()
                    .addBus("HAMMER", new BusAnimationSequence().addPos(15, 0, 0, 50).addPos(15, 0, 0, 550).addPos(0, 0, 0, 100));
            case RELOAD: return new BusAnimation()
                    .addBus("OPEN", new BusAnimationSequence().addPos(45, 0, 0, 200, IType.SIN_FULL).addPos(45, 0, 0, 750).addPos(0, 0, 0, 200, IType.SIN_UP))
                    .addBus("SHELL", new BusAnimationSequence().addPos(4, -8, -4, 0).addPos(4, -8, -4, 200).addPos(0, 0, -5, 500, IType.SIN_DOWN).addPos(0, 0, 0, 200, IType.SIN_UP))
                    .addBus("FLIP", new BusAnimationSequence().addPos(0, 0, 0, 200).addPos(25, 0, 0, 200, IType.SIN_DOWN).addPos(25, 0, 0, 800).addPos(0, 0, 0, 200, IType.SIN_DOWN));
            case JAMMED: return new BusAnimation()
                    .addBus("OPEN", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(45, 0, 0, 200, IType.SIN_FULL).addPos(45, 0, 0, 500).addPos(0, 0, 0, 200, IType.SIN_UP))
                    .addBus("FLIP", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 0, 200).addPos(25, 0, 0, 200, IType.SIN_DOWN).addPos(25, 0, 0, 550).addPos(0, 0, 0, 200, IType.SIN_DOWN));
            case INSPECT: return new BusAnimation()
                    .addBus("FLIP", new BusAnimationSequence().addPos(-360 * 3, 0, 0, 1500, IType.SIN_FULL));
        }

        return null;
    };

    @SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_CONGOLAKE_ANIMS = (stack, type) -> {
        int ammo = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, XFactory12ga.clientInventory());
        switch (type) {
            case EQUIP: return com.hbm_m.client.weapon.WeaponResources.congolake_anim.get().get("Equip");
            case CYCLE: return com.hbm_m.client.weapon.WeaponResources.congolake_anim.get().get(ammo <= 1 ? "FireEmpty" : "Fire");
            case RELOAD: return com.hbm_m.client.weapon.WeaponResources.congolake_anim.get().get(ammo == 0 ? "ReloadEmpty" : "ReloadStart");
            case RELOAD_CYCLE: return com.hbm_m.client.weapon.WeaponResources.congolake_anim.get().get("Reload");
            case RELOAD_END: return com.hbm_m.client.weapon.WeaponResources.congolake_anim.get().get("ReloadEnd");
            case JAMMED: return com.hbm_m.client.weapon.WeaponResources.congolake_anim.get().get("Jammed");
            case INSPECT: return com.hbm_m.client.weapon.WeaponResources.congolake_anim.get().get("Inspect");
        }

        return null;
    };

    @SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_MK108_ANIMS = (stack, type) -> {
        switch (type) {
            case EQUIP: return new BusAnimation()
                    .addBus("EQUIP", new BusAnimationSequence().setPos(45, 0, 0).addPos(0, 0, 0, 1000, IType.SIN_DOWN));
            case CYCLE:
                int amount = ((ItemGunBaseNT) stack.getItem()).getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, null);
                return new BusAnimation()
                    .addBus("RECOIL", new BusAnimationSequence().hold(50).addPos(0, 0, -0.25, 100, IType.SIN_DOWN).addPos(0, 0, 0, 150, IType.SIN_FULL))
                    .addBus("BARREL", new BusAnimationSequence().addPos(0, 0, -1, 100, IType.SIN_DOWN).addPos(0, 0, 0, 250, IType.SIN_FULL))
                    .addBus("CYCLE", new BusAnimationSequence().addPos(0, 0, 0, 100).addPos(1, 0, 0, 150))
                    .addBus("SHELLS", new BusAnimationSequence().setPos(amount - 1, 0, 0));
            case CYCLE_DRY: return new BusAnimation()
                    .addBus("HAMMER", new BusAnimationSequence().addPos(15, 0, 0, 50).addPos(15, 0, 0, 550).addPos(0, 0, 0, 100));
            case RELOAD: return new BusAnimation()
                    .addBus("LIFT", new BusAnimationSequence().addPos(10, 0, 0, 500, IType.SIN_FULL).holdUntil(1250).addPos(-50, 0, 0, 750, IType.SIN_FULL).holdUntil(5500).addPos(0, 0, 0, 500, IType.SIN_FULL).hold(500).addPos(1, 0, 0, 100, IType.SIN_UP).addPos(0, 0, 0, 150, IType.SIN_FULL))
                    .addBus("LID", new BusAnimationSequence().addPos(60, 0, 0, 500, IType.SIN_FULL).holdUntil(6000).addPos(0, 0, 0, 500, IType.SIN_UP))
                    .addBus("BELT", new BusAnimationSequence().setPos(1, 0, 0).hold(500).addPos(0, 0, 0, 750, IType.SIN_UP).holdUntil(4500).addPos(1, 0, 0, 750, IType.SIN_UP))
                    .addBus("DRUM", new BusAnimationSequence().hold(2000).addPos(2.5, 0, 0, 500, IType.SIN_DOWN).addPos(2.5, -2, -8, 500, IType.SIN_UP).setPos(4, -3, -8).addPos(2.5, 0, 0, 1000, IType.SIN_FULL).addPos(0, 0, 0, 500, IType.SIN_UP));
            case JAMMED: return new BusAnimation()
                    .addBus("LID", new BusAnimationSequence().hold(250).addPos(45, 0, 0, 500, IType.SIN_FULL).addPos(0, 0, 0, 250, IType.SIN_UP))
                    .addBus("LIFT", new BusAnimationSequence().hold(1000).addPos(1, 0, 0, 100, IType.SIN_UP).addPos(0, 0, 0, 150, IType.SIN_FULL));
            case INSPECT:
                int yeetHorizontal = 750;
                int untilImpact = yeetHorizontal * 9 / 15;
                int delay = 250;
                int height = 6;
                int arcUp = untilImpact * 5 / 8;
                int arcDown = untilImpact * 3 / 8;
                return new BusAnimation()
                        .addBus("LIFT", new BusAnimationSequence().hold(untilImpact).addPos(1, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_FULL).hold(delay - 150).addPos(1, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_FULL).hold(delay - 150).addPos(1, 0, 0, 50, IType.SIN_UP).addPos(0, 0, 0, 100, IType.SIN_FULL))
                        .addBus("GRENH1", new BusAnimationSequence().setPos(9, 0, 0).addPos(-6, 0, 0, yeetHorizontal))
                        .addBus("GRENV1", new BusAnimationSequence().setPos(0, -2, 0).addPos(0, height, 0, arcUp, IType.SIN_DOWN).addPos(0, 2, 0, arcDown, IType.SIN_UP).addPos(0, 3, 0, yeetHorizontal - untilImpact, IType.SIN_DOWN))
                        .addBus("GRENS1", new BusAnimationSequence().addPos(360 * 2, 0, 0, untilImpact).setPos(0, 0, 0).addPos(360 * 1, 0, 0, yeetHorizontal - untilImpact))
                        .addBus("GRENH2", new BusAnimationSequence().setPos(9, 0, 0).hold(delay).addPos(-6, 0, 0, yeetHorizontal))
                        .addBus("GRENV2", new BusAnimationSequence().setPos(0, -2, 0).hold(delay).addPos(0, height, 0, arcUp, IType.SIN_DOWN).addPos(0, 2, 0, arcDown, IType.SIN_UP).addPos(0, 3, 0, yeetHorizontal - untilImpact, IType.SIN_DOWN))
                        .addBus("GRENS2", new BusAnimationSequence().hold(delay).addPos(360 * 2, 0, 0, untilImpact).setPos(0, 0, 0).addPos(360 * 1, 0, 0, yeetHorizontal - untilImpact))
                        .addBus("GRENH3", new BusAnimationSequence().setPos(9, 0, 0).hold(delay * 2).addPos(-6, 0, 0, yeetHorizontal))
                        .addBus("GRENV3", new BusAnimationSequence().setPos(0, -2, 0).hold(delay * 2).addPos(0, height, 0, arcUp, IType.SIN_DOWN).addPos(0, 2, 0, arcDown, IType.SIN_UP).addPos(0, 3, 0, yeetHorizontal - untilImpact, IType.SIN_DOWN))
                        .addBus("GRENS3", new BusAnimationSequence().hold(delay * 2).addPos(360 * 2, 0, 0, untilImpact).setPos(0, 0, 0).addPos(360 * 1, 0, 0, yeetHorizontal - untilImpact));
        }
        return null;
    };
}
