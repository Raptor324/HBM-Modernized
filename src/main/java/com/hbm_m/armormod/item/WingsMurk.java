package com.hbm_m.armormod.item;

import com.hbm_m.extprop.HbmPlayerProps;
import com.hbm_m.item.ModItems;
import com.hbm_m.util.ArmorUtil;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code com.hbm.items.armor.WingsMurk}: wings_murk (Flug mit Jetpack-Taste) und wings_limp (Gleiten). */
public class WingsMurk extends JetpackBase {

    public WingsMurk(Properties properties) {
        super(properties);
    }

    @Override
    public String getModelTexture() {
        return "hbm_m:textures/armor/wings_murk.png";
    }

    @Override
    public void jetpackTick(Level world, Player player, ItemStack stack) {

        if (player.onGround())
            return;

        ArmorUtil.resetFlightTime(player);

        if (player.fallDistance > 0)
            player.fallDistance = 0;

        Vec3 m = player.getDeltaMovement();
        double mx = m.x, my = m.y, mz = m.z;

        if (this == ModItems.WINGS_LIMP.get()) {

            if (my < -0.4D)
                my = -0.4D;

            if (player.isShiftKeyDown()) {

                if (my < -0.08) {

                    double mo = my * -0.2;
                    my += mo;

                    Vec3 vec = player.getLookAngle().scale(mo);
                    mx += vec.x;
                    my += vec.y;
                    mz += vec.z;
                }
            }
        }

        HbmPlayerProps props = HbmPlayerProps.getData(player);

        if (this == ModItems.WINGS_MURK.get()) {

            if (props.isJetpackActive()) {

                if (player.isShiftKeyDown()) {
                    if (my < -1)
                        my += 0.4D;
                    else if (my < -0.1)
                        my += 0.2D;
                    else if (my < 0)
                        my = 0;
                    else if (my > 1)
                        my -= 0.4D;
                    else if (my > 0.1)
                        my -= 0.2D;
                    else if (my > 0)
                        my = 0;

                } else {
                    if (my < 0.6D)
                        my += 0.2D;
                    else
                        my = 0.8D;
                }

            } else if (props.enableBackpack && !player.isShiftKeyDown()) {

                if (my < -1)
                    my += 0.4D;
                else if (my < -0.1)
                    my += 0.2D;
                else if (my < 0)
                    my = 0;
            }

            if (props.enableBackpack) {

                Vec3 orig = player.getLookAngle();
                Vec3 look = new Vec3(orig.x, 0, orig.z).normalize();
                double mod = player.isSprinting() ? 1D : 0.25D;

                if (player.zza != 0) {
                    mx += look.x * 0.35 * player.zza * mod;
                    mz += look.z * 0.35 * player.zza * mod;
                }

                if (player.xxa != 0) {
                    look = look.yRot((float) Math.PI * 0.5F);
                    mx += look.x * 0.15 * player.xxa * mod;
                    mz += look.z * 0.15 * player.xxa * mod;
                }
            }
        }

        player.setDeltaMovement(mx, my, mz);
    }
}
