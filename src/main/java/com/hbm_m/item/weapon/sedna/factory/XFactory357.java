package com.hbm_m.item.weapon.sedna.factory;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

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
import com.hbm_m.render.anim.AnimationEnums.GunAnimation;
import com.hbm_m.render.anim.BusAnimation;
import com.hbm_m.render.anim.BusAnimationKeyframe.IType;
import com.hbm_m.render.anim.BusAnimationSequence;

import net.minecraft.world.item.ItemStack;

/** 1:1-Port von {@code com.hbm.items.weapon.sedna.factory.XFactory357} (Atlas, Dani). */
public class XFactory357 {

    public static BulletConfig m357_bp;
    public static BulletConfig m357_sp;
    public static BulletConfig m357_fmj;
    public static BulletConfig m357_jhp;
    public static BulletConfig m357_ap;
    public static BulletConfig m357_express;

    /** NTMSounds.GUN_PISTOL_FIRE */
    private static final String GUN_PISTOL_FIRE = "hbm:weapon.fire.pistol";

    public static void init() {
        m357_bp = new BulletConfig().setItem(EnumAmmo.M357_BP).setCasing(EnumCasingType.SMALL, 16).setDamage(0.75F).setBlackPowder(true);
        m357_sp = new BulletConfig().setItem(EnumAmmo.M357_SP).setCasing(EnumCasingType.SMALL, 8);
        m357_fmj = new BulletConfig().setItem(EnumAmmo.M357_FMJ).setCasing(EnumCasingType.SMALL, 8).setDamage(0.8F).setThresholdNegation(2F).setArmorPiercing(0.1F);
        m357_jhp = new BulletConfig().setItem(EnumAmmo.M357_JHP).setCasing(EnumCasingType.SMALL, 8).setDamage(1.5F).setHeadshot(1.5F).setArmorPiercing(-0.25F);
        m357_ap = new BulletConfig().setItem(EnumAmmo.M357_AP).setCasing(EnumCasingType.SMALL_STEEL, 8).setDoesPenetrate(true).setDamageFalloffByPen(false).setDamage(1.25F).setThresholdNegation(5F).setArmorPiercing(0.15F);
        m357_express = new BulletConfig().setItem(EnumAmmo.M357_EXPRESS).setCasing(EnumCasingType.SMALL, 8).setDoesPenetrate(true).setDamage(1.5F).setThresholdNegation(2F).setArmorPiercing(0.1F).setWear(1.5F);

        GunFactory.reg("gun_light_revolver", new ItemGunBaseNT(WeaponQuality.A_SIDE, new GunConfig()
                .dura(300).draw(4).inspect(23).crosshair(Crosshair.CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
                .rec(new Receiver(0)
                        .dmg(7.5F).delay(11).reload(55).jam(45).sound(GUN_PISTOL_FIRE, 1.0F, 1.0F)
                        .mag(new MagazineFullReload(0, 6).addConfigs(m357_bp, m357_sp, m357_fmj, m357_jhp, m357_ap, m357_express))
                        .offset(0.75, -0.0625, -0.3125D)
                        .setupStandardFire().recoil(LAMBDA_RECOIL_ATLAS))
                .setupStandardConfiguration()
                .anim(LAMBDA_ATLAS_ANIMS).orchestra(Orchestras.ORCHESTRA_ATLAS)
                ).setDefaultAmmo(EnumAmmo.M357_SP, 12));
        GunFactory.reg("gun_light_revolver_atlas", new ItemGunBaseNT(WeaponQuality.B_SIDE, new GunConfig()
                .dura(300).draw(4).inspect(23).crosshair(Crosshair.CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
                .rec(new Receiver(0)
                        .dmg(12.5F).delay(11).reload(55).jam(45).sound(GUN_PISTOL_FIRE, 1.0F, 1.0F)
                        .mag(new MagazineFullReload(0, 6).addConfigs(m357_bp, m357_sp, m357_fmj, m357_jhp, m357_ap, m357_express))
                        .offset(0.75, -0.0625, -0.3125D)
                        .setupStandardFire().recoil(LAMBDA_RECOIL_ATLAS))
                .setupStandardConfiguration()
                .anim(LAMBDA_ATLAS_ANIMS).orchestra(Orchestras.ORCHESTRA_ATLAS)
                ).setDefaultAmmo(EnumAmmo.M357_JHP, 12));
        GunFactory.reg("gun_light_revolver_dani", new ItemGunBaseNT(WeaponQuality.LEGENDARY,
                new GunConfig().dura(30_000).draw(20).inspect(23).crosshair(Crosshair.CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
                .rec(new Receiver(0)
                        .dmg(15F).spreadHipfire(0F).delay(11).reload(55).jam(45).sound(GUN_PISTOL_FIRE, 1.0F, 1.1F)
                        .mag(new MagazineFullReload(0, 6).addConfigs(m357_bp, m357_sp, m357_fmj, m357_jhp, m357_ap, m357_express))
                        .offset(0.75, -0.0625, 0.3125D)
                        .setupStandardFire().recoil(LAMBDA_RECOIL_DANI))
                .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
                .anim(LAMBDA_DANI_ANIMS).orchestra(Orchestras.ORCHESTRA_DANI),
                new GunConfig().dura(30_000).draw(20).inspect(23).crosshair(Crosshair.CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
                .rec(new Receiver(0)
                        .dmg(15F).spreadHipfire(0F).delay(11).reload(55).jam(45).sound(GUN_PISTOL_FIRE, 1.0F, 0.9F)
                        .mag(new MagazineFullReload(1, 6).addConfigs(m357_bp, m357_sp, m357_fmj, m357_jhp, m357_ap, m357_express))
                        .offset(0.75, -0.0625, -0.3125D)
                        .setupStandardFire().recoil(LAMBDA_RECOIL_DANI))
                .ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
                .anim(LAMBDA_DANI_ANIMS).orchestra(Orchestras.ORCHESTRA_DANI)
                ).setDefaultAmmo(EnumAmmo.M357_EXPRESS, 24));
    }

    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_ATLAS = (stack, ctx) -> {
        ItemGunBaseNT.setupRecoil(10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
    };

    public static BiConsumer<ItemStack, LambdaContext> LAMBDA_RECOIL_DANI = (stack, ctx) -> {
        ItemGunBaseNT.setupRecoil(5, (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.75));
    };

    @SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_ATLAS_ANIMS = (stack, type) -> {
        switch(type) {
        case EQUIP: return new BusAnimation()
                .addBus("EQUIP", new BusAnimationSequence().addPos(-90, 0, 0, 0).addPos(0, 0, 0, 350, IType.SIN_DOWN));
        case CYCLE: return new BusAnimation()
                .addBus("RECOIL", new BusAnimationSequence().addPos(0, 0, 0, 50).addPos(0, 0, -3, 50).addPos(0, 0, 0, 250))
                .addBus("HAMMER", new BusAnimationSequence().addPos(0, 0, 1, 50).addPos(0, 0, 1, 300).addPos(0, 0, 0, 200))
                .addBus("DRUM", new BusAnimationSequence().addPos(0, 0, 0, 250).addPos(0, 0, 1, 200));
        case CYCLE_DRY: return new BusAnimation()
                .addBus("HAMMER", new BusAnimationSequence().addPos(0, 0, 1, 50).addPos(0, 0, 1, 200).addPos(0, 0, 0, 200))
                .addBus("DRUM", new BusAnimationSequence().addPos(0, 0, 0, 250).addPos(0, 0, 1, 200));
        case RELOAD: return new BusAnimation()
                .addBus("LATCH", new BusAnimationSequence().addPos(0, 0, 90, 300).addPos(0, 0, 90, 2000).addPos(0, 0, 0, 150))
                .addBus("FRONT", new BusAnimationSequence().addPos(0, 0, 0, 200).addPos(0, 0, 45, 150).addPos(0, 0, 45, 2000).addPos(0, 0, 0, 75))
                .addBus("RELOAD_ROT", new BusAnimationSequence().addPos(0, 0, 0, 300).addPos(60, 0, 0, 500).addPos(60, 0, 0, 500).addPos(0, -90, -90, 0).addPos(0, -90, -90, 600).addPos(0, 0, 0, 300).addPos(0, 0, 0, 100).addPos(-45, 0, 0, 50).addPos(-45, 0, 0, 100).addPos(0, 0, 0, 300))
                .addBus("RELOAD_MOVE", new BusAnimationSequence().addPos(0, 0, 0, 300).addPos(0, -15, 0, 1000).addPos(0, 0, 0, 450))
                .addBus("DRUM_PUSH", new BusAnimationSequence().addPos(0, 0, 0, 1600).addPos(0, 0, -5, 0).addPos(0, 0, 0, 300));
        case INSPECT: return new BusAnimation()
                .addBus("LATCH", new BusAnimationSequence().addPos(0, 0, 90, 300).addPos(0, 0, 90, 1000).addPos(0, 0, 0, 150))
                .addBus("FRONT", new BusAnimationSequence().addPos(0, 0, 0, 200).addPos(0, 0, 45, 150).addPos(0, 0, 45, 1000).addPos(0, 0, 0, 75))
                .addBus("RELOAD_ROT", new BusAnimationSequence().addPos(0, 0, 0, 300).addPos(45, 0, 0, 500, IType.SIN_FULL).addPos(45, 0, 0, 500).addPos(-45, 0, 0, 50).addPos(-45, 0, 0, 100).addPos(0, 0, 0, 300))
                .addBus("RELOAD_MOVE", new BusAnimationSequence().addPos(0, 0, 0, 300).addPos(0, -2.5, 0, 500, IType.SIN_FULL).addPos(0, -2.5, 0, 500).addPos(0, 0, 0, 350));
        case JAMMED: return new BusAnimation()
                .addBus("LATCH", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 90, 300).addPos(0, 0, 90, 1000).addPos(0, 0, 0, 150))
                .addBus("FRONT", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 0, 200).addPos(0, 0, 45, 150).addPos(0, 0, 45, 1000).addPos(0, 0, 0, 75))
                .addBus("RELOAD_ROT", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 0, 300).addPos(45, 0, 0, 500, IType.SIN_FULL).addPos(45, 0, 0, 500).addPos(-45, 0, 0, 50).addPos(-45, 0, 0, 100).addPos(0, 0, 0, 300))
                .addBus("RELOAD_MOVE", new BusAnimationSequence().addPos(0, 0, 0, 500).addPos(0, 0, 0, 300).addPos(0, -2.5, 0, 500, IType.SIN_FULL).addPos(0, -2.5, 0, 500).addPos(0, 0, 0, 350));
        }

        return null;
    };

    @SuppressWarnings("incomplete-switch") public static BiFunction<ItemStack, GunAnimation, BusAnimation> LAMBDA_DANI_ANIMS = (stack, type) -> {
        switch(type) {
        case EQUIP: return new BusAnimation().addBus("EQUIP", new BusAnimationSequence().addPos(360 * 3, 0, 0, 1000, IType.SIN_DOWN));
        }

        return LAMBDA_ATLAS_ANIMS.apply(stack, type);
    };
}
