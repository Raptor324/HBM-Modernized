package com.hbm_m.item.weapon.sedna.impl;

import com.hbm_m.entity.projectile.EntityBulletBaseMK4;
import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.handler.EnumKeybind;
import com.hbm_m.item.weapon.sedna.GunConfig;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.factory.XFactoryTool;
import com.hbm_m.util.ArmorUtil;
import com.hbm_m.util.Vec3NT;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code ItemGunChargeThrower}: Ladungswerfer mit Enterhaken (Primaer = einholen, Sekundaer = bremsen, sonst Seil straff halten). */
public class ItemGunChargeThrower extends ItemGunBaseNT {

    public static final String KEY_LASTHOOK = "lasthook";

    public ItemGunChargeThrower(WeaponQuality quality, GunConfig... cfg) {
        super(quality, cfg);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean isHeld) {
        super.inventoryTick(stack, world, entity, slot, isHeld);

        if (getState(stack, 0) == GunState.RELOADING) {
            if (getLastHook(stack) != -1) setLastHook(stack, -1);
        }

        if (isHeld && entity instanceof Player player) {
            Entity e = world.getEntity(getLastHook(stack));
            if (e != null && e.isAlive() && e instanceof EntityBulletBaseMK4 hook && hook.config == XFactoryTool.ct_hook && hook.velocity < 0.01) {
                Vec3NT vec = new Vec3NT(e.getX() - player.getX(), e.getY() - player.getY() - player.getEyeHeight(), e.getZ() - player.getZ());
                double line = vec.lengthVector();
                Vec3 motion = player.getDeltaMovement();
                double motionX = motion.x;
                double motionY = motion.y;
                double motionZ = motion.z;
                if (HbmPlayerProps.getData(player).getKeyPressed(EnumKeybind.GUN_PRIMARY)) {
                    vec = vec.normalize();
                    vec.xCoord *= 0.1; vec.yCoord *= 0.1; vec.zCoord *= 0.1; // Original: normalizeSelf().multiply(0.1)
                    motionX += vec.xCoord;
                    motionY += vec.yCoord + 0.04;
                    motionZ += vec.zCoord;
                    if (!world.isClientSide && line < 2) hook.setDead();
                } else if (!HbmPlayerProps.getData(player).getKeyPressed(EnumKeybind.GUN_SECONDARY)) {
                    Vec3NT nextPos = new Vec3NT(player.getX() + motionX, player.getY() + player.getEyeHeight() + motionY, player.getZ() + motionZ);
                    Vec3NT delta = new Vec3NT(e.getX() - nextPos.xCoord, e.getY() - nextPos.yCoord, e.getZ() - nextPos.zCoord);
                    if (delta.lengthVector() > line) {
                        delta = delta.normalize();
                        delta.xCoord *= line; delta.yCoord *= line; delta.zCoord *= line; // Original: normalizeSelf().multiply(line)
                        Vec3NT newNext = new Vec3NT(e.getX() - delta.xCoord, e.getY() - delta.yCoord, e.getZ() - delta.zCoord);
                        Vec3NT vel = new Vec3NT(newNext.xCoord - player.getX(), newNext.yCoord - player.getY() - player.getEyeHeight(), newNext.zCoord - player.getZ());
                        if (vel.lengthVector() < 3) {
                            motionX = vel.xCoord;
                            motionY = vel.yCoord;
                            motionZ = vel.zCoord;
                        }
                    }
                } else {
                    motionX *= 0.5;
                    motionY *= 0.5;
                    motionZ *= 0.5;
                }
                player.setDeltaMovement(motionX, motionY, motionZ);

                if (motionY > -0.1) player.fallDistance = 0;
                ArmorUtil.resetFlightTime(player);
            }
        } else {
            if (getLastHook(stack) != -1) setLastHook(stack, -1);
        }
    }

    public static int getLastHook(ItemStack stack) { return getValueInt(stack, KEY_LASTHOOK); }
    public static void setLastHook(ItemStack stack, int value) { setValueInt(stack, KEY_LASTHOOK, value); }
}
