package com.hbm_m.client.weapon.render;

import com.hbm_m.client.render.SimpleObjModel;
import com.hbm_m.client.weapon.GunGL;
import com.hbm_m.client.weapon.WeaponResources;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.render.anim.HbmAnimations;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Clientseitige {@code renderFirstPerson}-Rümpfe von {@code ArmorNCRPAMelee}/{@code ArmorRPAMelee} (1:1), damit die
 * gemeinsamen Komponentenklassen keine Client-Typen enthalten. Zusätzlich die im WeaponResources fehlenden
 * Original-ResourceManager-Einträge {@code armor_remnant} und {@code rpa_arm}.
 */
public final class PAMeleeRenders {

    private PAMeleeRenders() { }

    public static final SimpleObjModel armor_remnant = new SimpleObjModel(ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "models/armor/remnant.obj"));
    public static final ResourceLocation rpa_arm = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/armor/rpa_arm.png");

    /** 1:1 {@code ArmorNCRPAMelee.renderFirstPerson}. */
    public static void renderNCRPA(ItemStack stack) {
        GunGL.bindTexture(WeaponResources.ncrpa_arm);

        GunGL.translate(0, -1.5, 0.5);
        double scale = 0.125D;
        GunGL.scale(scale, scale, scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double swingRight = HbmAnimations.getRelevantTransformation("SWINGRIGHT")[0];
        double swingLeft = HbmAnimations.getRelevantTransformation("SWINGLEFT")[0];
        double sweepTurn = HbmAnimations.getRelevantTransformation("SWEEPTURN")[0];
        double sweepCut = HbmAnimations.getRelevantTransformation("SWEEPCUT")[0];

        double forwardTilt = 60 - 60 * equip[0];
        double offsetOutward = 3;
        double roll = 60;

        GunGL.pushMatrix();

        GunGL.translate(-14 * swingLeft - 4 * sweepTurn, 6 * sweepCut, 2 * swingLeft + 8 * sweepCut);
        GunGL.rotate(forwardTilt + swingRight * 40 - 60 * sweepCut, 1, 0, 0);

        GunGL.translate(offsetOutward, 0, 0);
        GunGL.translate(6, 8, 0);
        GunGL.rotate(90 * swingLeft, 0, 0, 1);
        GunGL.rotate(roll + 30 * swingLeft - 90 * sweepTurn, 0, 1, 0);
        GunGL.translate(-6, -8, 0);
        GunGL.renderPart(WeaponResources.armor_ncr, "LeftArm");
        GunGL.popMatrix();

        GunGL.pushMatrix();

        GunGL.translate(14 * swingRight + 4 * sweepTurn, 6 * sweepCut, 2 * swingRight + 8 * sweepCut);
        GunGL.rotate(forwardTilt + swingLeft * 40 - 60 * sweepCut, 1, 0, 0);

        GunGL.translate(-offsetOutward, 0, 0);
        GunGL.translate(-6, 8, 0);
        GunGL.rotate(-90 * swingRight, 0, 0, 1);
        GunGL.rotate(-roll - 30 * swingRight + 90 * sweepTurn, 0, 1, 0);
        GunGL.translate(6, -8, 0);
        GunGL.renderPart(WeaponResources.armor_ncr, "RightArm");
        GunGL.popMatrix();
    }

    /** 1:1 {@code ArmorRPAMelee.renderFirstPerson}. */
    public static void renderRPA(ItemStack stack) {
        GunGL.bindTexture(rpa_arm);

        GunGL.translate(0, -1.5, 0.5);
        double scale = 0.125D;
        GunGL.scale(scale, scale, scale);

        double[] equip = HbmAnimations.getRelevantTransformation("EQUIP");
        double swingRight = HbmAnimations.getRelevantTransformation("SWINGRIGHT")[0];
        double swingLeft = HbmAnimations.getRelevantTransformation("SWINGLEFT")[0];
        double slapTurn = HbmAnimations.getRelevantTransformation("SLAPTURN")[0];
        double slap = HbmAnimations.getRelevantTransformation("SLAP")[0];

        double forwardTilt = 60 - 60 * equip[0];
        double offsetOutward = 3;
        double roll = 60;

        GunGL.pushMatrix();

        GunGL.translate(-12 * swingLeft + 2 * slapTurn - 5 * slap, 6 * slap, 5 * swingLeft + 8 * slap);
        GunGL.rotate(forwardTilt - swingRight * 20, 1, 0, 0);

        GunGL.translate(offsetOutward, 0, 0);
        GunGL.translate(6, 8, 0);
        GunGL.rotate(60 * swingLeft + 45 * slap, 0, 0, 1);
        GunGL.rotate(roll + 15 * swingLeft + 45 * slapTurn, 0, 1, 0);
        GunGL.translate(-6, -8, 0);
        GunGL.renderPart(armor_remnant, "LeftArm");
        GunGL.popMatrix();

        GunGL.pushMatrix();

        GunGL.translate(12 * swingRight - 2 * slapTurn + 5 * slap, 6 * slap, 5 * swingRight + 8 * slap);
        GunGL.rotate(forwardTilt - swingLeft * 20, 1, 0, 0);

        GunGL.translate(-offsetOutward, 0, 0);
        GunGL.translate(-6, 8, 0);
        GunGL.rotate(-60 * swingRight - 45 * slap, 0, 0, 1);
        GunGL.rotate(-roll - 15 * swingRight - 45 * slapTurn, 0, 1, 0);
        GunGL.translate(6, -8, 0);
        GunGL.renderPart(armor_remnant, "RightArm");
        GunGL.popMatrix();
    }
}
