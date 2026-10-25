package com.hbm_m.item.weapon.sedna.factory;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm_m.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.weapon.sedna.BulletConfig;
import com.hbm_m.particle.SpentCasing;
import com.hbm_m.particle.SpentCasing.CasingType;

/**
 * 1:1 {@code XFactoryTurret}: Geschosse der Geschuetztuerme (DGK, 240-mm-Granaten).
 * Portabweichung: {@code ammo_shell.stackFromEnum(Ammo240Shell.X)} ist im Port je Typ ein eigener Gegenstand.
 */
public class XFactoryTurret {

    public static BulletConfig dgk_normal;

    public static SpentCasing CASINNG240MM = new SpentCasing(CasingType.BOTTLENECK).setScale(7.5F).setBounceMotion(0.02F, 0.05F).setColor(SpentCasing.COLOR_CASE_BRASS).setupSmoke(1F, 0.5D, 60, 20);
    public static BulletConfig shell_normal;
    public static BulletConfig shell_explosive;
    public static BulletConfig shell_ap;
    public static BulletConfig shell_du;
    public static BulletConfig shell_w9;

    public static void init() {
        dgk_normal = new BulletConfig().setItem(ModItems.AMMO_DGK);

        shell_normal = new BulletConfig().setItem(ModItems.AMMO_SHELL).setDamage(1F).setCasing(CASINNG240MM.clone().register("240standard")).setOnImpact((bullet, mop) -> {
            Lego.standardExplode(bullet, mop, 10F); bullet.setDead();
        });
        shell_explosive = new BulletConfig().setItem(ModItems.AMMO_SHELL_EXPLOSIVE).setDamage(1.5F).setCasing(CASINNG240MM.clone().register("240ext")).setOnImpact((bullet, mop) -> {
            ExplosionVNT vnt = new ExplosionVNT(bullet.level(), mop.hitVec.xCoord, mop.hitVec.yCoord, mop.hitVec.zCoord, 10F);
            vnt.setBlockAllocator(new BlockAllocatorStandard());
            vnt.setBlockProcessor(new BlockProcessorStandard());
            vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage));
            vnt.setPlayerProcessor(new PlayerProcessorStandard());
            vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
            vnt.explode();
            bullet.setDead();
        });
        shell_ap = new BulletConfig().setItem(ModItems.AMMO_SHELL_APFSDS_T).setDamage(2F).setDoesPenetrate(true).setCasing(CASINNG240MM.clone().register("240w"));
        shell_du = new BulletConfig().setItem(ModItems.AMMO_SHELL_APFSDS_DU).setDamage(2.5F).setDoesPenetrate(true).setDamageFalloffByPen(false).setCasing(CASINNG240MM.clone().register("240u"));
        shell_w9 = new BulletConfig().setItem(ModItems.AMMO_SHELL_W9).setDamage(2.5F).setCasing(CASINNG240MM.clone().register("240n")).setOnImpact(XFactoryCatapult.LAMBDA_NUKE_STANDARD);
    }
}
