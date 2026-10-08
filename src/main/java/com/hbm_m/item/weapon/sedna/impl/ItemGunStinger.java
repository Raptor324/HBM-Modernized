package com.hbm_m.item.weapon.sedna.impl;

import java.util.List;

import com.hbm_m.item.weapon.sedna.GunConfig;
import com.hbm_m.item.weapon.sedna.ItemGunBaseNT;
import com.hbm_m.item.weapon.sedna.factory.Lego;
import com.hbm_m.util.Vec3NT;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code ItemGunStinger}: Zielsuchender Raketenwerfer. Mit gehaltener Sekundaertaste und angelegter Waffe wird
 * das naechste Ziel im Sichtkegel 60 Ticks lang aufgeschaltet. Das HUD (Fadenkreuz + Aufschaltbalken, Original
 * {@code renderHUD}) zeichnet {@code com.hbm_m.client.weapon.StingerClient}.
 */
public class ItemGunStinger extends ItemGunBaseNT {

    public static final String KEY_LOCKINGON = "lockingon";
    public static final String KEY_LOCKONPROGRESS = "lockonprogress";

    public static float prevLockon;
    public static float lockon;

    public ItemGunStinger(WeaponQuality quality, GunConfig... cfg) {
        super(quality, cfg);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean isHeld) {
        super.inventoryTick(stack, world, entity, slot, isHeld);

        if (entity instanceof Player player) {
            isHeld = isHeld && player.getMainHandItem() == stack; // wie ItemGunBaseNT: nur die Haupthand zaehlt
            if (!world.isClientSide && !isHeld && getIsLockingOn(stack)) {
                setIsLockingOn(stack, false);
            }

            prevLockon = lockon;

            if (!world.isClientSide) {
                int prevTarget = getLockonTarget(stack);
                if (isHeld && getIsLockingOn(stack) && getIsAiming(stack) && this.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getAmount(stack, player.getInventory()) > 0) {
                    int newLockonTarget = getLockonTarget(player, 150D, 10D);

                    if (newLockonTarget == -1) {
                        if (!getIsLockedOn(stack)) resetLockon(world, stack);
                    } else {
                        if (!getIsLockedOn(stack) && newLockonTarget != prevTarget) {
                            resetLockon(world, stack);
                            setLockonTarget(stack, newLockonTarget);
                        }
                        progressLockon(world, stack);

                        if (getLockonProgress(stack) >= 60 && !getIsLockedOn(stack)) {
                            Lego.playSound(player, "hbm:item.techBleep", 1F, 1F);
                            setIsLockedOn(stack, true);
                        }
                    }
                } else {
                    resetLockon(world, stack);
                }
            } else {
                if (getLockonProgress(stack) > 1) {
                    lockon += (1F / 60F);
                } else {
                    lockon = 0;
                }
            }
        }
    }

    public void resetLockon(Level world, ItemStack stack) {
        setLockonProgress(stack, 0);
        setIsLockedOn(stack, false);
    }

    public void progressLockon(Level world, ItemStack stack) {
        setLockonProgress(stack, getLockonProgress(stack) + 1);
    }

    public static int getLockonTarget(Player player, double distance, double angleThreshold) {

        if (player == null) return -1;

        double x = player.getX();
        double y = player.getY() + player.getEyeHeight();
        double z = player.getZ();

        // Original Vec3NT.multiply/add/rotateAroundYDeg (mutierend) - der Port-Vec3NT hat sie nicht, daher von Hand
        Vec3NT delta = new Vec3NT(player.getViewVector(1F));
        delta.setComponents(delta.xCoord * distance, delta.yCoord * distance, delta.zCoord * distance);
        Vec3NT look = new Vec3NT(delta.xCoord + x, delta.yCoord + y, delta.zCoord + z);
        Vec3NT left = rotateAroundYDeg(new Vec3NT(delta.xCoord + x, delta.yCoord + y, delta.zCoord + z), -angleThreshold);
        left.setComponents(left.xCoord, left.yCoord + 10, left.zCoord);
        Vec3NT right = rotateAroundYDeg(new Vec3NT(delta.xCoord + x, delta.yCoord + y, delta.zCoord + z), angleThreshold);
        right.setComponents(right.xCoord, right.yCoord - 10, right.zCoord);
        Vec3NT pos = new Vec3NT(x, y, z);

        AABB aabb = new AABB(getMinX(look, left, right, pos), getMinY(look, left, right, pos), getMinZ(look, left, right, pos),
                getMaxX(look, left, right, pos), getMaxY(look, left, right, pos), getMaxZ(look, left, right, pos));
        List<Entity> entities = player.level().getEntities(player, aabb);
        Entity closestEntity = null;
        double closestAngle = 360D;

        Vec3NT toEntity = new Vec3NT(0, 0, 0);

        for (Entity entity : entities) {
            if (entity.getBbHeight() < 0.5F || !entity.isPickable()) continue;
            toEntity.setComponents(entity.getX() - x, entity.getY() + entity.getBbHeight() / 2D - y, entity.getZ() - z);

            double vecProd = toEntity.xCoord * delta.xCoord + toEntity.yCoord * delta.yCoord + toEntity.zCoord * delta.zCoord;
            double bot = toEntity.lengthVector() * delta.lengthVector();
            double angle = Math.abs(Math.acos(vecProd / bot) * 180 / Math.PI);

            if (angle < closestAngle && angle < angleThreshold) {
                closestAngle = angle;
                closestEntity = entity;
            }
        }

        return closestEntity == null ? -1 : closestEntity.getId();
    }

    /** Original {@code Vec3NT.rotateAroundYDeg}: dreht diesen Vektor um die Y-Achse (Grad). */
    private static Vec3NT rotateAroundYDeg(Vec3NT v, double alpha) {
        double rad = alpha / 180D * Math.PI;
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double nx = v.xCoord * cos + v.zCoord * sin;
        double ny = v.yCoord;
        double nz = v.zCoord * cos - v.xCoord * sin;
        return v.setComponents(nx, ny, nz);
    }

    private static double getMinX(Vec3NT... vecs) { double min = Double.POSITIVE_INFINITY; for (Vec3NT vec : vecs) if (vec.xCoord < min) min = vec.xCoord; return min; }
    private static double getMinY(Vec3NT... vecs) { double min = Double.POSITIVE_INFINITY; for (Vec3NT vec : vecs) if (vec.yCoord < min) min = vec.yCoord; return min; }
    private static double getMinZ(Vec3NT... vecs) { double min = Double.POSITIVE_INFINITY; for (Vec3NT vec : vecs) if (vec.zCoord < min) min = vec.zCoord; return min; }
    private static double getMaxX(Vec3NT... vecs) { double max = Double.NEGATIVE_INFINITY; for (Vec3NT vec : vecs) if (vec.xCoord > max) max = vec.xCoord; return max; }
    private static double getMaxY(Vec3NT... vecs) { double max = Double.NEGATIVE_INFINITY; for (Vec3NT vec : vecs) if (vec.yCoord > max) max = vec.yCoord; return max; }
    private static double getMaxZ(Vec3NT... vecs) { double max = Double.NEGATIVE_INFINITY; for (Vec3NT vec : vecs) if (vec.zCoord > max) max = vec.zCoord; return max; }

    public static boolean getIsLockingOn(ItemStack stack) { return getValueBool(stack, KEY_LOCKINGON); }
    public static void setIsLockingOn(ItemStack stack, boolean value) { setValueBool(stack, KEY_LOCKINGON, value); }
    public static int getLockonProgress(ItemStack stack) { return getValueInt(stack, KEY_LOCKONPROGRESS); }
    public static void setLockonProgress(ItemStack stack, int value) { setValueInt(stack, KEY_LOCKONPROGRESS, value); }
}
