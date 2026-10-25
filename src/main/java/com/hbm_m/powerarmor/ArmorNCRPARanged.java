package com.hbm_m.powerarmor;

import com.hbm_m.entity.projectile.EntityBulletBaseMK4;
import com.hbm_m.item.weapon.sedna.BulletConfig;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.GunState;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm_m.item.weapon.sedna.factory.Lego;
import com.hbm_m.item.weapon.sedna.factory.XFactoryRocket;
import com.hbm_m.item.weapon.sedna.impl.IPARanged;
import com.hbm_m.item.weapon.sedna.mags.MagazineBelt;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code com.hbm.items.armor.ArmorNCRPARanged}: Schulterraketen der NCR-Powerarmor (gelenkt/ungelenkt). */
public class ArmorNCRPARanged implements IPARanged {

    public static MagazineBelt rocketSteerMag = new MagazineBelt();
    public static MagazineBelt rocketMag = new MagazineBelt();

    @Override public void clickPrimary(ItemStack stack, LambdaContext ctx) { fireRocket(stack, ctx, true); }
    @Override public void clickSecondary(ItemStack stack, LambdaContext ctx) { fireRocket(stack, ctx, false); }

    public static void fireRocket(ItemStack stack, LambdaContext ctx, boolean steer) {

        Player player = ctx.getPlayer();
        if (player == null) return;
        GunState state = ItemGunBaseNT.getState(stack, 0);
        MagazineBelt mag = steer ? rocketSteerMag : rocketMag;

        if (state == GunState.IDLE) {
            if (mag.acceptedBullets.isEmpty()) {
                mag.addConfigs(steer ? XFactoryRocket.rocket_ncrpa_steer : XFactoryRocket.rocket_ncrpa);
            }
            BulletConfig cfg = mag.getType(stack, player.getInventory());
            int amount = mag.getAmount(stack, player.getInventory());

            if (amount > 0) {
                mag.useUpAmmo(stack, player.getInventory(), 1);
                EntityBulletBaseMK4 mk4 = new EntityBulletBaseMK4(player, cfg, 25, 0, 0.25F * (player.getRandom().nextBoolean() ? -1 : 1), 0, 0);
                player.level().addFreshEntity(mk4);
                ItemGunBaseNT.setState(stack, 0, GunState.COOLDOWN);
                ItemGunBaseNT.setTimer(stack, 0, 10);
                Lego.playSound(player, "hbm:weapon.rpgShoot", 0.5F, 0.9F + player.getRandom().nextFloat() * 0.2F);
                player.inventoryMenu.broadcastChanges();
            } else {
                ItemGunBaseNT.setState(stack, 0, GunState.COOLDOWN);
                ItemGunBaseNT.setTimer(stack, 0, 10);
                Lego.playSound(player, "hbm:weapon.reload.dryFireClick", 1F, 1F);
            }
        }
    }
}
