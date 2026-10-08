package com.hbm_m.handler.guncfg;

import com.hbm_m.handler.BulletConfiguration;

/**
 * Die vom NPC-Altsystem noch gebrauchten Teile von {@code BulletConfigFactory} (standardBullet/Rocket/Grenade),
 * {@code GunRocketFactory.getRocketConfig} und {@code GunEnergyFactory.getTurbineConfig} - Werte 1:1.
 */
@Deprecated
public final class LegacyBulletConfigFactory {

    public static final float defaultSpread = 0.005F;

    private LegacyBulletConfigFactory() { }

    public static BulletConfiguration standardBulletConfig() {
        BulletConfiguration bullet = new BulletConfiguration();
        bullet.velocity = 5.0F;
        bullet.spread = defaultSpread;
        bullet.bulletsMin = 1;
        bullet.bulletsMax = 1;
        bullet.gravity = 0D;
        bullet.maxAge = 100;
        bullet.doesRicochet = true;
        bullet.ricochetAngle = 5;
        bullet.HBRC = 2;
        bullet.LBRC = 95;
        bullet.bounceMod = 0.8;
        bullet.doesPenetrate = true;
        bullet.doesBreakGlass = true;
        bullet.destroysBlocks = false;
        bullet.style = BulletConfiguration.STYLE_NORMAL;
        bullet.plink = BulletConfiguration.PLINK_BULLET;
        bullet.leadChance = 5;
        return bullet;
    }

    public static BulletConfiguration standardRocketConfig() {
        BulletConfiguration bullet = new BulletConfiguration();
        bullet.velocity = 2.0F;
        bullet.spread = defaultSpread;
        bullet.bulletsMin = 1;
        bullet.bulletsMax = 1;
        bullet.gravity = 0.005D;
        bullet.maxAge = 300;
        bullet.doesRicochet = true;
        bullet.ricochetAngle = 10;
        bullet.HBRC = 2;
        bullet.LBRC = 100;
        bullet.bounceMod = 0.8;
        bullet.doesPenetrate = false;
        bullet.doesBreakGlass = false;
        bullet.explosive = 5.0F;
        bullet.style = BulletConfiguration.STYLE_ROCKET;
        bullet.plink = BulletConfiguration.PLINK_GRENADE;
        bullet.vPFX = "smoke";
        return bullet;
    }

    public static BulletConfiguration standardGrenadeConfig() {
        BulletConfiguration bullet = new BulletConfiguration();
        bullet.velocity = 2.0F;
        bullet.spread = defaultSpread;
        bullet.bulletsMin = 1;
        bullet.bulletsMax = 1;
        bullet.gravity = 0.035D;
        bullet.maxAge = 300;
        bullet.doesRicochet = false;
        bullet.ricochetAngle = 0;
        bullet.HBRC = 0;
        bullet.LBRC = 0;
        bullet.bounceMod = 1.0;
        bullet.doesPenetrate = false;
        bullet.doesBreakGlass = false;
        bullet.explosive = 2.5F;
        bullet.style = BulletConfiguration.STYLE_GRENADE;
        bullet.plink = BulletConfiguration.PLINK_GRENADE;
        bullet.vPFX = "smoke";
        return bullet;
    }

    /** {@code GunRocketFactory.getRocketConfig}. */
    public static BulletConfiguration getRocketConfig() {
        BulletConfiguration bullet = standardRocketConfig();
        bullet.dmgMin = 10;
        bullet.dmgMax = 15;
        bullet.explosive = 4F;
        bullet.trail = 0;
        return bullet;
    }

    /** {@code GunEnergyFactory.getTurbineConfig}: Turbinenblaetter der Turbinen-Rakete. */
    public static BulletConfiguration getTurbineConfig() {
        BulletConfiguration bullet = new BulletConfiguration();
        bullet.dmgMin = 100;
        bullet.dmgMax = 150;
        bullet.velocity = 1F;
        bullet.gravity = 0.0;
        bullet.maxAge = 200;
        bullet.style = BulletConfiguration.STYLE_BLADE;
        bullet.destroysBlocks = true;
        bullet.doesRicochet = false;
        return bullet;
    }
}
