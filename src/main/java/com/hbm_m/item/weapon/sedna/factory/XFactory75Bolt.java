package com.hbm_m.item.weapon.sedna.factory;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.hbm_m.entity.projectile.EntityBulletBaseMK4;
import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.item.weapon.sedna.BulletConfig;
import com.hbm_m.item.weapon.sedna.Crosshair;
import com.hbm_m.item.weapon.sedna.GunConfig;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm_m.item.weapon.sedna.Receiver;
import com.hbm_m.item.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm_m.item.weapon.sedna.mags.MagazineFullReload;
import com.hbm_m.particle.SpentCasing;
import com.hbm_m.particle.SpentCasing.CasingType;
import com.hbm_m.render.anim.AnimationEnums.GunAnimation;
import com.hbm_m.render.anim.BusAnimation;
import com.hbm_m.render.anim.BusAnimationSequence;
import com.hbm_m.util.MovingObjectPosition;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code XFactory75Bolt}: Bolter. */
public class XFactory75Bolt {

    public static BulletConfig b75;
    public static BulletConfig b75_inc;
    public static BulletConfig b75_exp;

    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_TINY_EXPLODE = (bullet, mop) -> {
        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && bullet.tickCount < 3 && mop.entityHit == bullet.getThrower()) return;
        Lego.tinyExplode(bullet, mop, 2F); bullet.setDead();
    };
    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_INC = (bullet, mop) -> {
        if (mop.entityHit != null && mop.entityHit instanceof LivingEntity living) {
            if (HbmLivingProps.getPhosphorus(living) < 300) HbmLivingProps.setPhosphorus(living, 300);
        }
    };
    public static BiConsumer<EntityBulletBaseMK4, MovingObjectPosition> LAMBDA_STANDARD_EXPLODE = (bullet, mop) -> {
        Lego.standardExplode(bullet, mop, 5F); bullet.setDead();
    };

    public static void init() {
        SpentCasing casing75 = new SpentCasing(CasingType.STRAIGHT).setColor(SpentCasing.COLOR_CASE_BRASS).setScale(2F, 2F, 1.5F);

        b75 = new BulletConfig().setItem(EnumAmmo.B75)
                .setCasing(casing75.clone().register("b75")).setOnImpact(LAMBDA_TINY_EXPLODE);
        b75_inc = new BulletConfig().setItem(EnumAmmo.B75_INC).setDamage(0.8F).setArmorPiercing(0.1F)
                .setCasing(casing75.clone().register("b75inc")).setOnImpact(LAMBDA_INC);
        b75_exp = new BulletConfig().setItem(EnumAmmo.B75_EXP).setDamage(1.5F).setArmorPiercing(-0.25F)
                .setCasing(casing75.clone().register("b75exp")).setOnImpact(LAMBDA_STANDARD_EXPLODE);

        GunFactory.reg("gun_bolter", new ItemGunBaseNT(WeaponQuality.SPECIAL, new GunConfig()
                .dura(3_000).draw(20).inspect(31).crosshair(Crosshair.L_CIRCLE).smoke(LAMBDA_SMOKE)
                .rec(new Receiver(0)
                        .dmg(15F).delay(2).auto(true).spread(0.005F).reload(40).jam(55).sound("hbm:weapon.fire.blackPowder", 1.0F, 1.0F)
                        .mag(new MagazineFullReload(0, 30).addConfigs(b75, b75_inc, b75_exp))
                        .offset(1, -0.0625 * 2.5, -0.25D)
                        .setupStandardFire().recoil(LAMBDA_RECOIL_BOLT))
                .setupStandardConfiguration()
                .anim(LAMBDA_BOLTER_ANIMS).orchestra(Orchestras.ORCHESTRA_BOLTER)
                ).setDefaultAmmo(EnumAmmo.B75, 15));
    }

    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_SMOKE = (stack, ctx) -> {
        Lego.handleStandardSmoke(ctx.entity, stack, 2000, 0.05D, 1.1D, 0);
    };

    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_BOLT = (stack, ctx) -> {
        ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5), (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
    };

    @SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_BOLTER_ANIMS = (stack, type) -> {
        switch (type) {
            case CYCLE: return new BusAnimation()
                    .addBus("RECOIL", new BusAnimationSequence().addPos(1, 0, 0, 25).addPos(0, 0, 0, 75));
            case RELOAD: return new BusAnimation()
                    .addBus("TILT", new BusAnimationSequence().addPos(1, 0, 0, 250).addPos(1, 0, 0, 1500).addPos(0, 0, 0, 250))
                    .addBus("MAG", new BusAnimationSequence().addPos(0, 0, 1, 500).addPos(1, 0, 1, 500).addPos(0, 0, 0, 500));
            case JAMMED: return new BusAnimation()
                    .addBus("TILT", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(1, 0, 0, 250).addPos(1, 0, 0, 700).addPos(0, 0, 0, 250))
                    .addBus("MAG", new BusAnimationSequence().addPos(0, 0, 0, 750).addPos(0.6, 0, 0, 250).addPos(0, 0, 0, 250));
        }

        return null;
    };
}
