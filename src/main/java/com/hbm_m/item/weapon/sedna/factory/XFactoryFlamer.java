package com.hbm_m.item.weapon.sedna.factory;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.hbm_m.entity.effect.EntityFireLingering;
import com.hbm_m.entity.projectile.EntityBulletBaseMK4;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.item.weapon.sedna.BulletConfig;
import com.hbm_m.item.weapon.sedna.Crosshair;
import com.hbm_m.item.weapon.sedna.GunConfig;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm_m.item.weapon.sedna.Receiver;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm_m.item.weapon.sedna.impl.ItemGunChemthrower;
import com.hbm_m.item.weapon.sedna.mags.MagazineFluid;
import com.hbm_m.item.weapon.sedna.mags.MagazineFullReload;
import com.hbm_m.particle.helper.FlameCreator;
import com.hbm_m.powerarmor.resist.DamageResistanceHandler.DamageClass;
import com.hbm_m.render.anim.AnimationEnums.GunAnimation;
import com.hbm_m.render.anim.BusAnimation;
import com.hbm_m.render.anim.BusAnimationKeyframe.IType;
import com.hbm_m.render.anim.BusAnimationSequence;
import com.hbm_m.util.ForgeDirection;
import com.hbm_m.util.MovingObjectPosition;
import com.hbm_m.util.MovingObjectPosition.MovingObjectType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** 1:1 {@code XFactoryFlamer}: Flammenwerfer (normal, Topaz, Daybreaker) und Chemiewerfer. */
public class XFactoryFlamer {

    public static BulletConfig flame_nograv;
    public static BulletConfig flame_nograv_bf;

    public static BulletConfig flame_diesel;
    public static BulletConfig flame_gas;
    public static BulletConfig flame_napalm;
    public static BulletConfig flame_balefire;

    public static BulletConfig flame_topaz_diesel;
    public static BulletConfig flame_topaz_gas;
    public static BulletConfig flame_topaz_napalm;
    public static BulletConfig flame_topaz_balefire;

    public static BulletConfig flame_daybreaker_diesel;
    public static BulletConfig flame_daybreaker_gas;
    public static BulletConfig flame_daybreaker_napalm;
    public static BulletConfig flame_daybreaker_balefire;

    public static Consumer<Entity> LAMBDA_FIRE = (bullet) -> {
        if (bullet.level().isClientSide && distToClient(bullet) < 100) FlameCreator.composeEffectClient(bullet.getX(), bullet.getY() - 0.125, bullet.getZ(), FlameCreator.META_FIRE);
    };
    public static Consumer<Entity> LAMBDA_BALEFIRE = (bullet) -> {
        if (bullet.level().isClientSide && distToClient(bullet) < 100) FlameCreator.composeEffectClient(bullet.getX(), bullet.getY() - 0.125, bullet.getZ(), FlameCreator.META_BALEFIRE);
    };
    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_IGNITE_FIRE = (bullet, mop) -> {
        if (mop.entityHit instanceof LivingEntity living) {
            if (HbmLivingProps.getFire(living) < 100) HbmLivingProps.setFire(living, 100);
        }
    };
    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_IGNITE_BALEFIRE = (bullet, mop) -> {
        if (mop.entityHit instanceof LivingEntity living) {
            if (HbmLivingProps.getBalefire(living) < 200) HbmLivingProps.setBalefire(living, 200);
        }
    };
    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_LINGER_DIESEL = (bullet, mop) -> { if (!igniteIfPossible(bullet, mop)) spawnFire(bullet, mop, 2F, 1F, 100, EntityFireLingering.TYPE_DIESEL); };
    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_LINGER_GAS = (bullet, mop) -> { igniteIfPossible(bullet, mop); };
    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_LINGER_NAPALM = (bullet, mop) -> { if (!igniteIfPossible(bullet, mop)) spawnFire(bullet, mop, 2.5F, 1F, 200, EntityFireLingering.TYPE_DIESEL); };
    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_LINGER_BALEFIRE = (bullet, mop) -> { spawnFire(bullet, mop, 3F, 1F, 300, EntityFireLingering.TYPE_BALEFIRE); };

    /** Original {@code MainRegistry.proxy.me().getDistanceToEntity(bullet)} - nur clientseitig aufgerufen. */
    private static double distToClient(Entity bullet) {
        Player me = com.hbm_m.client.weapon.GunClientHooks.clientPlayer();
        return me == null ? Double.MAX_VALUE : me.distanceTo(bullet);
    }

    public static boolean igniteIfPossible(EntityBulletBaseMK4 bullet, MovingObjectPosition mop) {
        if (mop.typeOfHit == MovingObjectType.BLOCK) {
            Level world = bullet.level();
            BlockPos pos = mop.getBlockPos();
            BlockState b = world.getBlockState(pos);
            ForgeDirection dir = ForgeDirection.getOrientation(mop.sideHit);
            if (b.isFlammable(world, pos, dir.getOpposite().toDirection())) {
                BlockPos side = pos.offset(dir.offsetX, dir.offsetY, dir.offsetZ);
                if (world.getBlockState(side).isAir()) {
                    world.setBlockAndUpdate(side, Blocks.FIRE.defaultBlockState());
                    return true;
                }
            }
            bullet.setDead();
        }
        return false;
    }

    public static void spawnFire(EntityBulletBaseMK4 bullet, MovingObjectPosition mop, float width, float height, int duration, int type) {
        if (mop.typeOfHit == MovingObjectType.BLOCK) {
            List<EntityFireLingering> fires = bullet.level().getEntitiesOfClass(EntityFireLingering.class,
                    new AABB(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord).inflate(width / 2 + 0.5, height / 2 + 0.5, width / 2 + 0.5));
            if (fires.isEmpty()) {
                EntityFireLingering fire = new EntityFireLingering(bullet.level()).setArea(width, height).setDuration(duration).setType(type);
                fire.setPosition(mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord);
                bullet.level().addFreshEntity(fire);
            }
            bullet.setDead();
        }
    }

    private static ItemStack plateSteel(int n) {
        return new ItemStack(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE), n);
    }

    public static void init() {
        flame_diesel = new BulletConfig().setItem(EnumAmmo.FLAME_DIESEL).setCasing(plateSteel(2), 500).setupDamageClass(DamageClass.FIRE).setLife(100).setVel(1F).setGrav(0.02D).setReloadCount(500).setSelfDamageDelay(20).setKnockback(0F)
                .setOnImpact(LAMBDA_IGNITE_FIRE).setOnUpdate(LAMBDA_FIRE).setOnRicochet(LAMBDA_LINGER_DIESEL);
        flame_gas = new BulletConfig().setItem(EnumAmmo.FLAME_GAS).setCasing(plateSteel(2), 500).setupDamageClass(DamageClass.FIRE).setLife(10).setSpread(0.05F).setVel(1F).setGrav(0.0D).setReloadCount(500).setSelfDamageDelay(20).setKnockback(0F)
                .setOnImpact(LAMBDA_IGNITE_FIRE).setOnUpdate(LAMBDA_FIRE).setOnRicochet(LAMBDA_LINGER_GAS);
        flame_napalm = new BulletConfig().setItem(EnumAmmo.FLAME_NAPALM).setCasing(plateSteel(2), 500).setupDamageClass(DamageClass.FIRE).setLife(200).setVel(1F).setGrav(0.02D).setReloadCount(500).setSelfDamageDelay(20).setKnockback(0F)
                .setOnImpact(LAMBDA_IGNITE_FIRE).setOnUpdate(LAMBDA_FIRE).setOnRicochet(LAMBDA_LINGER_NAPALM);
        flame_balefire = new BulletConfig().setItem(EnumAmmo.FLAME_BALEFIRE).setCasing(plateSteel(2), 500).setupDamageClass(DamageClass.FIRE).setLife(200).setVel(1F).setGrav(0.02D).setReloadCount(500).setSelfDamageDelay(20).setKnockback(0F)
                .setOnImpact(LAMBDA_IGNITE_BALEFIRE).setOnUpdate(LAMBDA_BALEFIRE).setOnRicochet(LAMBDA_LINGER_BALEFIRE);

        flame_nograv = flame_diesel.clone().setGrav(0);
        flame_nograv_bf = flame_balefire.clone().setGrav(0).setLife(100);

        flame_topaz_diesel = flame_diesel		.clone().setProjectiles(2).setSpread(0.05F).setLife(60).setGrav(0.0D);
        flame_topaz_gas = flame_gas				.clone().setProjectiles(2).setSpread(0.05F);
        flame_topaz_napalm = flame_napalm		.clone().setProjectiles(2).setSpread(0.05F).setLife(60).setGrav(0.0D);
        flame_topaz_balefire = flame_balefire	.clone().setProjectiles(2).setSpread(0.05F).setLife(60).setGrav(0.0D);

        flame_daybreaker_diesel = flame_diesel.clone().setLife(200).setVel(2F).setGrav(0.035D)
                .setOnImpact((bullet, mop) -> { Lego.standardExplode(bullet, mop, 5F); spawnFire(bullet, mop, 6F, 2F, 200, EntityFireLingering.TYPE_DIESEL); bullet.setDead(); });
        flame_daybreaker_gas = flame_gas.clone().setLife(200).setVel(2F).setGrav(0.035D)
                .setOnImpact((bullet, mop) -> { Lego.standardExplode(bullet, mop, 5F); bullet.setDead(); });
        flame_daybreaker_napalm = flame_napalm.clone().setLife(200).setVel(2F).setGrav(0.035D)
                .setOnImpact((bullet, mop) -> { Lego.standardExplode(bullet, mop, 7.5F); spawnFire(bullet, mop, 6F, 2F, 300, EntityFireLingering.TYPE_DIESEL); bullet.setDead(); });
        flame_daybreaker_balefire = flame_balefire.clone().setLife(200).setVel(2F).setGrav(0.035D)
                .setOnImpact((bullet, mop) -> { Lego.standardExplode(bullet, mop, 5F); spawnFire(bullet, mop, 7.5F, 2.5F, 400, EntityFireLingering.TYPE_BALEFIRE); bullet.setDead(); });

        GunFactory.reg("gun_flamer", new ItemGunBaseNT(WeaponQuality.A_SIDE, new GunConfig()
                .dura(20_000).draw(10).inspect(17).crosshair(Crosshair.L_CIRCLE)
                .rec(new Receiver(0)
                        .dmg(1F).spreadHipfire(0F).delay(1).auto(true).reload(90).jam(17)
                        .mag(new MagazineFullReload(0, 300).addConfigs(flame_diesel, flame_gas, flame_napalm, flame_balefire))
                        .offset(0.75, -0.0625, -0.25D)
                        .setupStandardFire())
                .setupStandardConfiguration()
                .anim(LAMBDA_FLAMER_ANIMS).orchestra(Orchestras.ORCHESTRA_FLAMER)
                ).setDefaultAmmo(EnumAmmo.FLAME_DIESEL, 1));
        GunFactory.reg("gun_flamer_topaz", new ItemGunBaseNT(WeaponQuality.B_SIDE, new GunConfig()
                .dura(20_000).draw(10).inspect(17).crosshair(Crosshair.L_CIRCLE)
                .rec(new Receiver(0)
                        .dmg(1.5F).spreadHipfire(0F).delay(1).auto(true).reload(90).jam(17)
                        .mag(new MagazineFullReload(0, 500).addConfigs(flame_topaz_diesel, flame_topaz_gas, flame_topaz_napalm, flame_topaz_balefire))
                        .offset(0.75, -0.0625, -0.25D)
                        .setupStandardFire())
                .setupStandardConfiguration()
                .anim(LAMBDA_FLAMER_ANIMS).orchestra(Orchestras.ORCHESTRA_FLAMER)
                ).setDefaultAmmo(EnumAmmo.FLAME_DIESEL, 1));
        GunFactory.reg("gun_flamer_daybreaker", new ItemGunBaseNT(WeaponQuality.LEGENDARY, new GunConfig()
                .dura(20_000).draw(10).inspect(17).crosshair(Crosshair.L_CIRCLE)
                .rec(new Receiver(0)
                        .dmg(25F).spreadHipfire(0F).delay(10).auto(true).reload(90).jam(17).sound("hbm:weapon.fire.blackPowder", 1.0F, 1.0F)
                        .mag(new MagazineFullReload(0, 50).addConfigs(flame_daybreaker_diesel, flame_daybreaker_gas, flame_daybreaker_napalm, flame_daybreaker_balefire))
                        .offset(0.75, -0.0625, -0.25D)
                        .setupStandardFire())
                .setupStandardConfiguration()
                .anim(LAMBDA_FLAMER_ANIMS).orchestra(Orchestras.ORCHESTRA_FLAMER_DAYBREAKER)
                ).setDefaultAmmo(EnumAmmo.FLAME_DIESEL, 1));

        GunFactory.reg("gun_chemthrower", new ItemGunChemthrower(WeaponQuality.A_SIDE, new GunConfig()
                .dura(90_000).draw(10).inspect(17).crosshair(Crosshair.L_CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
                .rec(new Receiver(0)
                        .delay(1).spreadHipfire(0F).auto(true)
                        .mag(new MagazineFluid(0, 3_000))
                        .offset(0.75, -0.0625, -0.25D)
                        .canFire(ItemGunChemthrower.LAMBDA_CAN_FIRE).fire(ItemGunChemthrower.LAMBDA_FIRE))
                .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
                .anim(LAMBDA_CHEMTHROWER_ANIMS).orchestra(Orchestras.ORCHESTRA_CHEMTHROWER)
                ));
    }

    @SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_FLAMER_ANIMS = (stack, type) -> {
        switch (type) {
            case EQUIP: return new BusAnimation()
                    .addBus("EQUIP", new BusAnimationSequence().addPos(-45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
            case RELOAD: return com.hbm_m.client.weapon.WeaponResources.flamethrower_anim.get().get("Reload");
            case INSPECT:
            case JAMMED: return new BusAnimation()
                    .addBus("ROTATE", new BusAnimationSequence().addPos(0, 0, 45, 250, IType.SIN_FULL).addPos(0, 0, 45, 350).addPos(0, 0, -15, 150, IType.SIN_FULL).addPos(0, 0, 0, 100, IType.SIN_FULL));
        }

        return null;
    };

    @SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_CHEMTHROWER_ANIMS = (stack, type) -> {
        switch (type) {
            case EQUIP: return new BusAnimation()
                    .addBus("EQUIP", new BusAnimationSequence().addPos(-45, 0, 0, 0).addPos(0, 0, 0, 500, IType.SIN_DOWN));
        }

        return null;
    };
}
